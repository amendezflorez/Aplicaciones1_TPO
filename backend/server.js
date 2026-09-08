require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { randomUUID } = require('crypto');
const nodemailer = require('nodemailer');
const db = require('./db');
const { zonasCercanas } = require('./zones');

const app = express();
app.use(cors());
// Las fotos viajan en base64 dentro del JSON, y el limite por defecto de
// express es 100kb: con una sola foto ya se pasa.
app.use(express.json({ limit: '12mb' }));

const PORT = process.env.PORT || 8080;
const OTP_EXPIRY_MS = 5 * 60 * 1000; // 5 minutos de validez

function generateOtp() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

// Transporter de Gmail: la conexión que despacha los mails.
// Las credenciales vienen del .env, nunca escritas en el código.
const mailTransporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: process.env.GMAIL_USER,
    pass: process.env.GMAIL_PASS,
  },
});

// Manda el código OTP por mail. Es async pero no bloquea la respuesta:
// si el mail falla, el código igual quedó guardado y se ve en consola.
async function enviarOtpPorMail(email, code) {
  try {
    await mailTransporter.sendMail({
      from: process.env.MAIL_FROM,
      to: email,
      subject: 'Tu código de acceso a Ronda',
      text: `Tu código de verificación es: ${code}\n\nVence en 5 minutos.`,
    });
    console.log(`✅ Mail con OTP enviado a ${email}`);
  } catch (error) {
    console.error(`❌ No se pudo enviar el mail a ${email}:`, error.message);
  }
}

// Log simple de solicitudes entrantes
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl}`);
  next();
});

// Endpoint de salud
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', server: 'Ronda Backend Unificado', port: PORT });
});

// ==========================================
// 1. ENDPOINTS DE AUTENTICACIÓN
// ==========================================

// Login con usuario y contraseña
app.post('/api/auth/login', async (req, res) => {
  const { username, password } = req.body;

  if (!username || !password) {
    return res.status(400).json({ success: false, message: 'Faltan username o password' });
  }

  try {
    let user = await db.get('SELECT * FROM users WHERE username = ?', [username]);

    if (!user) {
      const id = randomUUID();
      await db.run(
        'INSERT INTO users (id, username, password, name) VALUES (?, ?, ?, ?)',
        [id, username, password, username]
      );
      user = { id, username, password, name: username, email: null };
      console.log(`[LOGIN] Usuario nuevo creado: ${username}`);
    } else if (user.password !== password) {
      return res.status(401).json({ success: false, message: 'Contraseña incorrecta' });
    }

    const token = randomUUID();
    res.json({
      token,
      userId: user.id,
      email: user.email,
      name: user.name,
      // La zona viaja en el login para que el filtro de cercania del Home
      // funcione sin tener que pedir el perfil aparte.
      zone: user.zone || null,
    });
  } catch (error) {
    console.error('Error en /api/auth/login:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Solicitar código OTP
app.post('/api/auth/otp/request', async (req, res) => {
  const { email } = req.body;
  if (!email) return res.status(400).json({ success: false, message: 'Falta email' });

  const code = generateOtp();
  const expiresAt = Date.now() + OTP_EXPIRY_MS;

  try {
    await db.run(`
      INSERT INTO otp_codes (email, code, expires_at) VALUES (?, ?, ?)
      ON CONFLICT(email) DO UPDATE SET code = excluded.code, expires_at = excluded.expires_at
    `, [email, code, expiresAt]);

    console.log(`\n📩 Código OTP para ${email}: ${code}\n`);
    enviarOtpPorMail(email, code);
    res.json({ success: true, message: 'Código enviado' });
  } catch (error) {
    console.error('Error en /api/auth/otp/request:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Reenviar código OTP
app.post('/api/auth/otp/resend', async (req, res) => {
  const { email } = req.body;
  if (!email) return res.status(400).json({ success: false, message: 'Falta email' });

  const code = generateOtp();
  const expiresAt = Date.now() + OTP_EXPIRY_MS;

  try {
    await db.run(`
      INSERT INTO otp_codes (email, code, expires_at) VALUES (?, ?, ?)
      ON CONFLICT(email) DO UPDATE SET code = excluded.code, expires_at = excluded.expires_at
    `, [email, code, expiresAt]);

    console.log(`\n📩 Código OTP reenviado para ${email}: ${code}\n`);
    enviarOtpPorMail(email, code);
    res.json({ success: true, message: 'Código reenviado' });
  } catch (error) {
    console.error('Error en /api/auth/otp/resend:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Verificar código OTP
app.post('/api/auth/otp/verify', async (req, res) => {
  const { email, code } = req.body;
  if (!email || !code) return res.status(400).json({ success: false, message: 'Faltan datos' });

  try {
    const record = await db.get('SELECT * FROM otp_codes WHERE email = ?', [email]);

    if (!record) {
      return res.status(400).json({ success: false, message: 'No se solicitó un código para este email' });
    }
    if (Date.now() > record.expires_at) {
      return res.status(400).json({ success: false, message: 'El código expiró' });
    }
    if (record.code !== code) {
      return res.status(400).json({ success: false, message: 'Código incorrecto' });
    }

    let user = await db.get('SELECT * FROM users WHERE email = ?', [email]);
    if (!user) {
      const id = randomUUID();
      const name = email.split('@')[0];
      await db.run('INSERT INTO users (id, email, name) VALUES (?, ?, ?)', [id, email, name]);
      user = { id, email, name };
      console.log(`[OTP] Usuario nuevo creado: ${email}`);
    }

    await db.run('DELETE FROM otp_codes WHERE email = ?', [email]);

    const token = randomUUID();
    res.json({
      token,
      userId: user.id,
      email: user.email,
      name: user.name,
      // La zona viaja en el login para que el filtro de cercania del Home
      // funcione sin tener que pedir el perfil aparte.
      zone: user.zone || null,
    });
  } catch (error) {
    console.error('Error en /api/auth/otp/verify:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// ==========================================
// 2. ENDPOINTS DE PUBLICACIONES (HOME)
// ==========================================

app.get('/api/publications', async (req, res) => {
  const {
    search,
    category,
    condition,
    minPrice,
    maxPrice,
    zone,
    nearZone,
    sortBy,
    page = 1,
    limit = 10
  } = req.query;

  // El WHERE se arma una sola vez y se reutiliza para el COUNT del total,
  // asi el cliente sabe cuando dejar de pedir paginas. Las columnas van
  // calificadas con "p." porque el JOIN con users trae otra columna "zone".
  let whereClause = " WHERE p.status = 'activa'";
  const filterParams = [];

  if (search) {
    whereClause += ' AND (p.title LIKE ? OR p.description LIKE ?)';
    filterParams.push(`%${search}%`, `%${search}%`);
  }
  if (category) {
    whereClause += ' AND p.category = ?';
    filterParams.push(category);
  }
  if (condition) {
    whereClause += ' AND p.condition = ?';
    filterParams.push(condition);
  }
  if (zone) {
    whereClause += ' AND p.zone = ?';
    filterParams.push(zone);
  }
  // Punto 3: "cercanía a la zona del usuario". No es igualdad: se expande la
  // zona propia a ella misma más sus barrios linderos. Si el usuario tiene
  // seteada una zona que no está en la tabla, zonasCercanas() devuelve solo
  // esa y el filtro degrada a igualdad exacta.
  if (nearZone) {
    const cercanas = zonasCercanas(nearZone);
    if (cercanas.length > 0) {
      const placeholders = cercanas.map(() => '?').join(', ');
      whereClause += ` AND p.zone IN (${placeholders})`;
      filterParams.push(...cercanas);
    }
  }
  if (minPrice) {
    whereClause += ' AND p.price >= ?';
    filterParams.push(Number(minPrice));
  }
  if (maxPrice) {
    whereClause += ' AND p.price <= ?';
    filterParams.push(Number(maxPrice));
  }

  const sortMap = {
    'price_asc': 'p.price ASC',
    'price_desc': 'p.price DESC',
    'recent': 'p.created_at DESC'
  };
  const orderByClause = sortMap[sortBy] || 'p.created_at DESC';

  const limitNum = Math.max(1, parseInt(limit, 10) || 10);
  const pageNum = Math.max(1, parseInt(page, 10) || 1);
  const offset = (pageNum - 1) * limitNum;

  // seller_name viaja en el listado para poder abrir el perfil publico del
  // vendedor desde la tarjeta (punto 2: consultar a la otra parte antes de operar).
  const listQuery = `SELECT p.*, u.name AS seller_name,
                            (SELECT COUNT(*) FROM publication_photos ph
                              WHERE ph.publication_id = p.id) AS photo_count
                     FROM publications p
                     LEFT JOIN users u ON u.id = p.user_id${whereClause}
                     ORDER BY ${orderByClause} LIMIT ? OFFSET ?`;
  const countQuery = `SELECT COUNT(*) AS total FROM publications p${whereClause}`;

  try {
    const rows = await db.all(listQuery, [...filterParams, limitNum, offset]);
    const countRow = await db.get(countQuery, filterParams);

    res.json({
      data: rows,
      page: pageNum,
      limit: limitNum,
      total: countRow ? countRow.total : rows.length
    });
  } catch (error) {
    console.error('Error al consultar publicaciones:', error);
    res.status(500).json({ error: error.message });
  }
});

// ==========================================
// 2b. PUBLICAR Y GESTIONAR PUBLICACIONES (PUNTO 5)
// ==========================================

const ESTADOS_VALIDOS = ['activa', 'pausada', 'vendida'];
const CONDICIONES_VALIDAS = ['nuevo', 'como nuevo', 'usado'];
const MAX_FOTOS = 5;

// Crear una publicacion, con sus fotos.
app.post('/api/publications', async (req, res) => {
  const { userId, title, description, price, condition, category, zone, photos } = req.body;

  if (!userId) {
    return res.status(400).json({ success: false, message: 'Falta el usuario que publica' });
  }
  if (!title || !title.trim()) {
    return res.status(400).json({ success: false, message: 'El título es obligatorio' });
  }
  const precio = Number(price);
  if (!Number.isFinite(precio) || precio < 0) {
    return res.status(400).json({ success: false, message: 'El precio no es válido' });
  }
  if (!CONDICIONES_VALIDAS.includes(condition)) {
    return res.status(400).json({ success: false, message: 'El estado del artículo no es válido' });
  }
  const fotos = Array.isArray(photos) ? photos : [];
  if (fotos.length > MAX_FOTOS) {
    return res.status(400).json({ success: false, message: `Como máximo ${MAX_FOTOS} fotos` });
  }

  try {
    const user = await db.get('SELECT id FROM users WHERE id = ?', [userId]);
    if (!user) {
      return res.status(404).json({ success: false, message: 'Usuario no encontrado' });
    }

    const insert = await db.run(
      `INSERT INTO publications (title, description, price, condition, category, zone, user_id, status)
       VALUES (?, ?, ?, ?, ?, ?, ?, 'activa')`,
      [title.trim(), description || null, precio, condition, category || null, zone || null, userId]
    );

    for (let i = 0; i < fotos.length; i++) {
      await db.run(
        'INSERT INTO publication_photos (publication_id, data, position) VALUES (?, ?, ?)',
        [insert.lastID, fotos[i], i]
      );
    }

    const creada = await db.get('SELECT * FROM publications WHERE id = ?', [insert.lastID]);
    res.status(201).json({ ...creada, photo_count: fotos.length });
  } catch (error) {
    console.error('Error en POST /api/publications', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// "Mis publicaciones": todas las del usuario, en cualquier estado.
// Se diferencia del perfil publico, que solo lista las activas.
app.get('/api/users/:id/publications', async (req, res) => {
  try {
    const rows = await db.all(
      `SELECT p.*, (SELECT COUNT(*) FROM publication_photos ph
                     WHERE ph.publication_id = p.id) AS photo_count
         FROM publications p
        WHERE p.user_id = ?
        ORDER BY p.created_at DESC`,
      [req.params.id]
    );
    res.json({ data: rows, total: rows.length });
  } catch (error) {
    console.error('Error en GET /api/users/:id/publications', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Pausar / reactivar / marcar vendida.
app.patch('/api/publications/:id/status', async (req, res) => {
  const { status } = req.body;

  if (!ESTADOS_VALIDOS.includes(status)) {
    return res.status(400).json({
      success: false,
      message: `El estado debe ser uno de: ${ESTADOS_VALIDOS.join(', ')}`
    });
  }

  try {
    const publicacion = await db.get('SELECT * FROM publications WHERE id = ?', [req.params.id]);
    if (!publicacion) {
      return res.status(404).json({ success: false, message: 'Publicación no encontrada' });
    }

    await db.run('UPDATE publications SET status = ? WHERE id = ?', [status, req.params.id]);
    res.json({ ...publicacion, status });
  } catch (error) {
    console.error('Error en PATCH /api/publications/:id/status', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Fotos de una publicacion. Endpoint aparte a proposito: los listados no
// arrastran base64. El detalle del punto 4 consume este mismo endpoint.
app.get('/api/publications/:id/photos', async (req, res) => {
  try {
    const rows = await db.all(
      'SELECT id, data, position FROM publication_photos WHERE publication_id = ? ORDER BY position',
      [req.params.id]
    );
    res.json({ data: rows, total: rows.length });
  } catch (error) {
    console.error('Error en GET /api/publications/:id/photos', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// ==========================================
// 3. ENDPOINTS DE PERFIL Y REPUTACION
// ==========================================

/**
 * Reputacion de un usuario, derivada de las calificaciones recibidas:
 * promedio de estrellas y cantidad de operaciones concretadas en cada rol.
 */
async function getReputation(userId) {
  const row = await db.get(
    `SELECT COUNT(*) AS totalRatings,
            AVG(stars) AS average,
            SUM(CASE WHEN role = 'vendedor'  THEN 1 ELSE 0 END) AS salesCount,
            SUM(CASE WHEN role = 'comprador' THEN 1 ELSE 0 END) AS purchasesCount
       FROM ratings
      WHERE rated_user_id = ?`,
    [userId]
  );

  return {
    // Se redondea a un decimal para que el cliente muestre "4.5" sin hacer cuentas.
    average: row && row.average ? Math.round(row.average * 10) / 10 : 0,
    totalRatings: (row && row.totalRatings) || 0,
    salesCount: (row && row.salesCount) || 0,
    purchasesCount: (row && row.purchasesCount) || 0
  };
}

// Perfil de un usuario: datos personales + reputacion + publicaciones activas.
app.get('/api/users/:id', async (req, res) => {
  const { id } = req.params;

  try {
    const user = await db.get(
      'SELECT id, name, email, phone, zone, created_at FROM users WHERE id = ?',
      [id]
    );
    if (!user) {
      return res.status(404).json({ success: false, message: 'Usuario no encontrado' });
    }

    const reputation = await getReputation(id);
    const activePublications = await db.all(
      `SELECT * FROM publications
        WHERE user_id = ? AND status = 'activa'
        ORDER BY created_at DESC`,
      [id]
    );

    res.json({
      id: user.id,
      name: user.name,
      email: user.email,
      phone: user.phone,
      zone: user.zone,
      // "Antiguedad en la plataforma" se calcula en el cliente a partir de esta fecha.
      createdAt: user.created_at,
      reputation,
      activePublications
    });
  } catch (error) {
    console.error('Error en GET /api/users/:id', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Editar los datos personales del perfil.
app.put('/api/users/:id', async (req, res) => {
  const { id } = req.params;
  const { name, email, phone, zone } = req.body;

  if (!name || !name.trim()) {
    return res.status(400).json({ success: false, message: 'El nombre no puede quedar vacío' });
  }
  if (email && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
    return res.status(400).json({ success: false, message: 'El email no es válido' });
  }

  try {
    const user = await db.get('SELECT id FROM users WHERE id = ?', [id]);
    if (!user) {
      return res.status(404).json({ success: false, message: 'Usuario no encontrado' });
    }

    // El email es UNIQUE: se avisa con 409 en vez de dejar explotar el constraint.
    if (email) {
      const enUso = await db.get('SELECT id FROM users WHERE email = ? AND id <> ?', [email, id]);
      if (enUso) {
        return res.status(409).json({ success: false, message: 'Ese email ya está en uso' });
      }
    }

    await db.run(
      'UPDATE users SET name = ?, email = ?, phone = ?, zone = ? WHERE id = ?',
      [name.trim(), email || null, phone || null, zone || null, id]
    );

    const actualizado = await db.get(
      'SELECT id, name, email, phone, zone, created_at FROM users WHERE id = ?',
      [id]
    );

    res.json({
      id: actualizado.id,
      name: actualizado.name,
      email: actualizado.email,
      phone: actualizado.phone,
      zone: actualizado.zone,
      createdAt: actualizado.created_at,
      reputation: await getReputation(id),
      activePublications: []
    });
  } catch (error) {
    console.error('Error en PUT /api/users/:id', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Calificar a un usuario. La reputacion sale de aca; emitir la calificacion al
// cerrar una operacion es parte del flujo de los puntos 4 y 5.
app.post('/api/users/:id/ratings', async (req, res) => {
  const { id } = req.params;
  const { stars, role, comment, raterUserId } = req.body;

  const estrellas = parseInt(stars, 10);
  if (!Number.isInteger(estrellas) || estrellas < 1 || estrellas > 5) {
    return res.status(400).json({ success: false, message: 'Las estrellas deben ir de 1 a 5' });
  }
  if (role !== 'vendedor' && role !== 'comprador') {
    return res.status(400).json({ success: false, message: "El rol debe ser 'vendedor' o 'comprador'" });
  }

  try {
    const user = await db.get('SELECT id FROM users WHERE id = ?', [id]);
    if (!user) {
      return res.status(404).json({ success: false, message: 'Usuario no encontrado' });
    }

    await db.run(
      'INSERT INTO ratings (rated_user_id, rater_user_id, stars, role, comment) VALUES (?, ?, ?, ?, ?)',
      [id, raterUserId || null, estrellas, role, comment || null]
    );

    res.status(201).json({ success: true, reputation: await getReputation(id) });
  } catch (error) {
    console.error('Error en POST /api/users/:id/ratings', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Se espera a que el esquema termine de migrar antes de atender pedidos.
db.ready
  .then(() => {
    app.listen(PORT, () => {
      console.log(`===================================================`);
      console.log(`✅ Backend Unificado de Ronda corriendo en http://localhost:${PORT}`);
      console.log(`📲 Base URL para Android Emulator: http://10.0.2.2:${PORT}/api/`);
      console.log(`===================================================`);
    });
  })
  .catch((err) => {
    console.error('❌ No se pudo inicializar la base, el servidor no arranca:', err.message);
    process.exit(1);
  });
