require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { randomUUID } = require('crypto');
const nodemailer = require('nodemailer');
const jwt = require('jsonwebtoken');
const db = require('./db');
const { zonasCercanas } = require('./zones');

const app = express();
app.use(cors());
// Las fotos viajan en base64 dentro del JSON, y el limite por defecto de
// express es 100kb: con una sola foto ya se pasa.
app.use(express.json({ limit: '12mb' }));

const PORT = process.env.PORT || 8080;
const OTP_EXPIRY_MS = 5 * 60 * 1000; // 5 minutos de validez
const OFFER_EXPIRY_MS = 48 * 60 * 60 * 1000; // Punto 7: 48hs de vigencia de una oferta

function generateOtp() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

// ==========================================
// AUTENTICACION CON JWT
// ==========================================

const JWT_SECRET = process.env.JWT_SECRET;
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '7d';

// Sin secreto no se arranca. Poner uno por defecto en el codigo seria peor que
// no tener JWT: cualquiera que lea el repo podria firmar tokens validos.
if (!JWT_SECRET) {
  console.error('❌ Falta JWT_SECRET en el .env. Ver backend/README.md.');
  process.exit(1);
}

/**
 * Emite el token de sesion como JWT firmado.
 *
 * El token es autocontenido: lleva el id del usuario en "sub" y su vencimiento
 * en "exp", asi que validarlo no requiere ir a la base.
 */
function crearSesion(userId) {
  return jwt.sign({ sub: userId }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });
}

/**
 * Exige el header "Authorization: Bearer <token>" que manda la app y verifica
 * la firma del JWT. Cuelga el dueno en req.userId para que la ruta sepa quien
 * esta llamando.
 *
 * No se aplica a /api/health ni a /api/auth/*: son justamente las rutas que se
 * usan cuando todavia no hay sesion.
 */
function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice('Bearer '.length).trim() : null;

  if (!token) {
    return res.status(401).json({ success: false, message: 'Falta el token de sesion' });
  }

  try {
    const payload = jwt.verify(token, JWT_SECRET);
    req.userId = payload.sub;
    next();
  } catch (error) {
    // Se distingue vencido de invalido para que el cliente pueda decidir si
    // reintentar el login o avisar que algo raro pasa. Ambos son 401.
    const vencido = error.name === 'TokenExpiredError';
    return res.status(401).json({
      success: false,
      expired: vencido,
      message: vencido ? 'La sesion expiro, volve a iniciar sesion' : 'Token invalido'
    });
  }
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

    const token = crearSesion(user.id);
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

    if (process.env.NODE_ENV !== 'production') {
      console.log(`\n📩 Código OTP para ${email}: ${code}\n`);
    }
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

    const token = crearSesion(user.id);
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

/**
 * Arma el WHERE de las publicaciones activas a partir de los filtros del Home.
 *
 * Lo comparten el listado y el contador de novedades de las busquedas
 * guardadas (punto 11): "coincide con la busqueda" tiene que significar lo
 * mismo en los dos lados, y con el filtro copiado en dos lugares tarde o
 * temprano dejarian de coincidir. Las columnas van calificadas con "p."
 * porque el listado hace JOIN con users, que trae otra columna "zone".
 */
function filtrosDePublicaciones({ search, category, condition, zone, nearZone, minPrice, maxPrice }) {
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

  return { whereClause, filterParams };
}

app.get('/api/publications', requireAuth, async (req, res) => {
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
  // asi el cliente sabe cuando dejar de pedir paginas.
  const { whereClause, filterParams } = filtrosDePublicaciones({
    search, category, condition, zone, nearZone, minPrice, maxPrice
  });

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

    // Punto 7: el listado nunca muestra la direccion exacta. Eso recien se
    // revela en el detalle, y solo si corresponde (ver GET /api/publications/:id).
    rows.forEach((r) => { delete r.address; delete r.lat; delete r.lng; });

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
app.post('/api/publications', requireAuth, async (req, res) => {
  const { title, description, price, condition, category, zone, photos } = req.body;

  // El autor sale del token. Tomarlo del body dejaba publicar a nombre de otro
  // con solo mandar su id, que es peor que leer datos ajenos: falsifica autoria.
  const userId = req.userId;

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
    // El id ya no lo elige el cliente, asi que si el usuario no esta es que el
    // token sobrevivio a su dueno: eso es una sesion muerta, no un 404.
    const user = await db.get('SELECT id FROM users WHERE id = ?', [userId]);
    if (!user) {
      return res.status(401).json({ success: false, message: 'La sesion ya no es valida' });
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
app.get('/api/users/:id/publications', requireAuth, async (req, res) => {
  // Punto 7: esta lista incluye la direccion exacta (es panel de gestion del
  // propio dueno, a diferencia del perfil publico que solo trae las activas
  // sin direccion). Sin este chequeo, cualquiera podia leer la direccion de
  // publicaciones ajenas pasando el id de otro usuario en la URL.
  if (req.params.id !== req.userId) {
    return res.status(403).json({ success: false, message: 'Solo podes ver tus propias publicaciones' });
  }
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
app.patch('/api/publications/:id/status', requireAuth, async (req, res) => {
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
    // Pausar o marcar como vendida es gestion de la propia publicacion, igual
    // que ver las ofertas: solo el vendedor.
    if (publicacion.user_id !== req.userId) {
      return res.status(403).json({ success: false, message: 'Solo el vendedor cambia el estado de la publicacion' });
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
// ==========================================
// 2.b DETALLE DE LA PUBLICACION (PUNTO 4)
// ==========================================

/** Trae la publicacion con su vendedor, o null si no existe. */
async function getPublicacionConVendedor(id) {
  const publication = await db.get(
    `SELECT p.*,
            u.name AS seller_name,
            (SELECT COUNT(*) FROM publication_photos WHERE publication_id = p.id) AS photo_count
       FROM publications p
       LEFT JOIN users u ON u.id = p.user_id
      WHERE p.id = ?`,
    [id]
  );
  return publication || null;
}

// Detalle completo: la publicacion mas los datos del vendedor con su reputacion,
// que es lo que permite decidir si conviene operar sin salir de la pantalla.
app.get('/api/publications/:id', requireAuth, async (req, res) => {
  try {
    const publication = await getPublicacionConVendedor(req.params.id);
    if (!publication) {
      return res.status(404).json({ success: false, message: 'Publicacion no encontrada' });
    }

    let seller = null;
    if (publication.user_id) {
      const usuario = await db.get(
        'SELECT id, name, zone, created_at FROM users WHERE id = ?',
        [publication.user_id]
      );
      if (usuario) {
        seller = { ...usuario, reputation: await getReputation(usuario.id) };
      }
    }

    // Puente al punto 8 (coordinacion de entrega): la direccion exacta solo
    // viaja si quien pregunta es el dueno o tiene una oferta aceptada sobre
    // esta publicacion. El articulo se puede ver siempre; donde retirarlo, no.
    const esDueno = publication.user_id === req.userId;
    let puedeVerDireccion = esDueno;
    if (!puedeVerDireccion) {
      const ofertaAceptada = await db.get(
        `SELECT id FROM offers WHERE publication_id = ? AND user_id = ? AND status = 'aceptada'`,
        [publication.id, req.userId]
      );
      puedeVerDireccion = !!ofertaAceptada;
    }
    if (!puedeVerDireccion) {
      delete publication.address;
      delete publication.lat;
      delete publication.lng;
    }

    res.json({ publication, seller });
  } catch (error) {
    console.error('Error en GET /api/publications/:id', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Preguntas de la publicacion. Las ve cualquiera que mire el detalle: son
// publicas, como en cualquier marketplace.
app.get('/api/publications/:id/questions', requireAuth, async (req, res) => {
  try {
    const rows = await db.all(
      `SELECT q.id, q.text, q.created_at, q.user_id, u.name AS user_name
         FROM questions q
         LEFT JOIN users u ON u.id = q.user_id
        WHERE q.publication_id = ?
        ORDER BY q.created_at DESC`,
      [req.params.id]
    );
    res.json({ data: rows, total: rows.length });
  } catch (error) {
    console.error('Error en GET /api/publications/:id/questions', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

app.post('/api/publications/:id/questions', requireAuth, async (req, res) => {
  const texto = (req.body.text || '').trim();
  if (!texto) {
    return res.status(400).json({ success: false, message: 'La pregunta no puede estar vacia' });
  }

  try {
    const publication = await getPublicacionConVendedor(req.params.id);
    if (!publication) {
      return res.status(404).json({ success: false, message: 'Publicacion no encontrada' });
    }
    // El duenio gestiona su publicacion; preguntar es la accion del interesado.
    if (publication.user_id === req.userId) {
      return res.status(400).json({ success: false, message: 'No podes preguntar en tu propia publicacion' });
    }

    const { lastID } = await db.run(
      'INSERT INTO questions (publication_id, user_id, text) VALUES (?, ?, ?)',
      [publication.id, req.userId, texto]
    );
    const creada = await db.get(
      `SELECT q.id, q.text, q.created_at, q.user_id, u.name AS user_name
         FROM questions q LEFT JOIN users u ON u.id = q.user_id
        WHERE q.id = ?`,
      [lastID]
    );
    res.status(201).json(creada);
  } catch (error) {
    console.error('Error en POST /api/publications/:id/questions', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

const ACCIONES_OFERTA = ['aceptar', 'rechazar', 'contraofertar'];

/**
 * Punto 7: la caducidad es lazy, sin cron. Una oferta "en juego" (pendiente, o
 * contraofertada y esperando la respuesta del comprador) cuyo plazo ya paso se
 * persiste como "vencida" en el momento en que se lee (no cuando vence de
 * verdad), asi nunca se muestra activa con el plazo cumplido. La contraoferta
 * reusa el mismo expires_at de la oferta original: no se reinicia el reloj.
 */
async function expirarSiCorresponde(oferta) {
  const enJuego = oferta.status === 'pendiente' || oferta.status === 'contraofertada';
  if (enJuego && oferta.expires_at && Date.now() > oferta.expires_at) {
    await db.run("UPDATE offers SET status = 'vencida' WHERE id = ?", [oferta.id]);
    oferta.status = 'vencida';
  }
  return oferta;
}

// Las ofertas las ve solo el vendedor: son parte de la gestion de su publicacion.
app.get('/api/publications/:id/offers', requireAuth, async (req, res) => {
  try {
    const publication = await getPublicacionConVendedor(req.params.id);
    if (!publication) {
      return res.status(404).json({ success: false, message: 'Publicacion no encontrada' });
    }
    if (publication.user_id !== req.userId) {
      return res.status(403).json({ success: false, message: 'Solo el vendedor ve las ofertas' });
    }

    const rows = await db.all(
      `SELECT o.id, o.amount, o.status, o.message, o.delivery_point, o.expires_at, o.created_at, o.user_id, u.name AS user_name
         FROM offers o
         LEFT JOIN users u ON u.id = o.user_id
        WHERE o.publication_id = ?
        ORDER BY o.amount DESC, o.created_at DESC`,
      [req.params.id]
    );
    const data = [];
    for (const row of rows) data.push(await expirarSiCorresponde(row));

    res.json({ data, total: data.length });
  } catch (error) {
    console.error('Error en GET /api/publications/:id/offers', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Coordinación de la Entrega: consulta de la oferta realizada por el usuario actual en la publicación
app.get('/api/publications/:id/my-offer', requireAuth, async (req, res) => {
  try {
    const offer = await db.get(
      `SELECT o.id, o.amount, o.status, o.delivery_point, o.created_at, o.user_id, u.name AS user_name
         FROM offers o
         LEFT JOIN users u ON u.id = o.user_id
        WHERE o.publication_id = ? AND o.user_id = ?
        ORDER BY o.created_at DESC
        LIMIT 1`,
      [req.params.id, req.userId]
    );
    res.json(offer || null);
  } catch (error) {
    console.error('Error en GET /api/publications/:id/my-offer', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Coordinación de la Entrega: el vendedor acepta una oferta y confirma el punto de entrega
app.patch('/api/offers/:id/accept', requireAuth, async (req, res) => {
  try {
    const offer = await db.get(
      `SELECT o.*, p.user_id AS seller_id, p.zone AS publication_zone, p.delivery_point AS publication_delivery_point
         FROM offers o
         JOIN publications p ON p.id = o.publication_id
        WHERE o.id = ?`,
      [req.params.id]
    );

    if (!offer) {
      return res.status(404).json({ success: false, message: 'Oferta no encontrada' });
    }
    if (offer.seller_id !== req.userId) {
      return res.status(403).json({ success: false, message: 'Solo el vendedor puede aceptar la oferta' });
    }

    const deliveryPoint = (req.body && req.body.delivery_point && req.body.delivery_point.trim())
      ? req.body.delivery_point.trim()
      : (offer.publication_delivery_point || offer.publication_zone || 'Punto a convenir');

    await db.run(
      "UPDATE offers SET status = 'aceptada', delivery_point = ? WHERE id = ?",
      [deliveryPoint, offer.id]
    );

    const updated = await db.get(
      `SELECT o.id, o.amount, o.status, o.delivery_point, o.created_at, o.user_id, u.name AS user_name
         FROM offers o
         LEFT JOIN users u ON u.id = o.user_id
        WHERE o.id = ?`,
      [offer.id]
    );
    res.json(updated);
  } catch (error) {
    console.error('Error en PATCH /api/offers/:id/accept', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

app.post('/api/publications/:id/offers', requireAuth, async (req, res) => {
  const monto = Number(req.body.amount);
  const mensaje = (req.body.message || '').trim();
  if (!Number.isFinite(monto) || monto <= 0) {
    return res.status(400).json({ success: false, message: 'El monto ofertado no es valido' });
  }

  try {
    const publication = await getPublicacionConVendedor(req.params.id);
    if (!publication) {
      return res.status(404).json({ success: false, message: 'Publicacion no encontrada' });
    }
    if (publication.user_id === req.userId) {
      return res.status(400).json({ success: false, message: 'No podes ofertar en tu propia publicacion' });
    }
    if (publication.status !== 'activa') {
      return res.status(400).json({ success: false, message: 'La publicacion no esta activa' });
    }

    const { lastID } = await db.run(
      'INSERT INTO offers (publication_id, user_id, amount, message, expires_at) VALUES (?, ?, ?, ?, ?)',
      [publication.id, req.userId, monto, mensaje || null, Date.now() + OFFER_EXPIRY_MS]
    );
    const creada = await db.get(
      `SELECT o.id, o.amount, o.status, o.message, o.delivery_point, o.expires_at, o.created_at, o.user_id, u.name AS user_name
         FROM offers o LEFT JOIN users u ON u.id = o.user_id
        WHERE o.id = ?`,
      [lastID]
    );
    res.status(201).json(creada);
  } catch (error) {
    console.error('Error en POST /api/publications/:id/offers', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

/**
 * Punto 7: cambiar el estado de una oferta. Una sola ruta para las tres
 * acciones (aceptar / rechazar / contraofertar) porque las tres comparten la
 * misma validacion de "quien puede tocar esta oferta en este estado".
 *
 * Reglas de transicion:
 * - 'pendiente' -> el VENDEDOR puede aceptar, rechazar o contraofertar.
 * - 'contraofertada' -> la pelota paso al COMPRADOR, que puede aceptar o
 *   rechazar la contraoferta (no hay una segunda vuelta: sin parent_id no se
 *   arma historial encadenado, ver nota en el reporte).
 * - Cualquier otro estado (aceptada / rechazada / vencida) ya esta resuelto:
 *   ninguna accion nueva se acepta sobre ella.
 * - Aceptar (en cualquiera de los dos casos de arriba) rechaza automaticamente
 *   las demas ofertas en danza (pendiente o contraofertada) de esa publicacion.
 */
app.patch('/api/publications/:id/offers/:offerId', requireAuth, async (req, res) => {
  const { action, amount } = req.body;
  if (!ACCIONES_OFERTA.includes(action)) {
    return res.status(400).json({
      success: false,
      message: `La accion debe ser una de: ${ACCIONES_OFERTA.join(', ')}`
    });
  }

  try {
    const publication = await getPublicacionConVendedor(req.params.id);
    if (!publication) {
      return res.status(404).json({ success: false, message: 'Publicacion no encontrada' });
    }

    let oferta = await db.get(
      'SELECT * FROM offers WHERE id = ? AND publication_id = ?',
      [req.params.offerId, publication.id]
    );
    if (!oferta) {
      return res.status(404).json({ success: false, message: 'Oferta no encontrada' });
    }
    oferta = await expirarSiCorresponde(oferta);

    const esVendedor = publication.user_id === req.userId;
    const esComprador = oferta.user_id === req.userId;

    if (action === 'contraofertar') {
      if (!esVendedor) {
        return res.status(403).json({ success: false, message: 'Solo el vendedor puede contraofertar' });
      }
      if (oferta.status !== 'pendiente') {
        return res.status(400).json({ success: false, message: 'Esa oferta ya no esta pendiente' });
      }
      const nuevoMonto = Number(amount);
      if (!Number.isFinite(nuevoMonto) || nuevoMonto <= 0) {
        return res.status(400).json({ success: false, message: 'El monto de la contraoferta no es valido' });
      }
      await db.run("UPDATE offers SET amount = ?, status = 'contraofertada' WHERE id = ?", [nuevoMonto, oferta.id]);
    } else {
      const nuevoEstado = action === 'aceptar' ? 'aceptada' : 'rechazada';
      const puedeVendedor = esVendedor && oferta.status === 'pendiente';
      const puedeComprador = esComprador && oferta.status === 'contraofertada';

      if (!puedeVendedor && !puedeComprador) {
        return res.status(403).json({ success: false, message: 'No podes cambiar el estado de esta oferta' });
      }

      await db.run('UPDATE offers SET status = ? WHERE id = ?', [nuevoEstado, oferta.id]);

      if (nuevoEstado === 'aceptada') {
        // Solo un comprador se queda con el punto de entrega: las demas ofertas
        // en danza de esta publicacion (de este u otros compradores) se cierran.
        await db.run(
          `UPDATE offers SET status = 'rechazada'
             WHERE publication_id = ? AND id != ? AND status IN ('pendiente', 'contraofertada')`,
          [publication.id, oferta.id]
        );
        // La publicacion se cierra a nuevas ofertas: POST .../offers ya exige
        // status = 'activa', asi que esto la bloquea sola (punto 5).
        await db.run("UPDATE publications SET status = 'vendida' WHERE id = ?", [publication.id]);
      }
    }

    const actualizada = await db.get(
      `SELECT o.id, o.amount, o.status, o.message, o.expires_at, o.created_at, o.user_id, u.name AS user_name
         FROM offers o LEFT JOIN users u ON u.id = o.user_id
        WHERE o.id = ?`,
      [oferta.id]
    );
    res.json(actualizada);
  } catch (error) {
    console.error('Error en PATCH /api/publications/:id/offers/:offerId', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// "Mis ofertas": las que mande como comprador y las que recibi como vendedor,
// desde la misma tabla segun el rol. El vencimiento se recalcula en cada
// lectura porque la caducidad es lazy (ver expirarSiCorresponde).
app.get('/api/offers/mine', requireAuth, async (req, res) => {
  try {
    const enviadasRaw = await db.all(
      `SELECT o.id, o.publication_id, o.amount, o.status, o.message, o.expires_at, o.created_at,
              p.title AS publication_title, p.price AS publication_price,
              p.user_id AS seller_id, su.name AS seller_name
         FROM offers o
         JOIN publications p ON p.id = o.publication_id
         LEFT JOIN users su ON su.id = p.user_id
        WHERE o.user_id = ?
        ORDER BY o.created_at DESC`,
      [req.userId]
    );
    const recibidasRaw = await db.all(
      `SELECT o.id, o.publication_id, o.amount, o.status, o.message, o.expires_at, o.created_at,
              p.title AS publication_title, p.price AS publication_price,
              o.user_id AS buyer_id, u.name AS buyer_name
         FROM offers o
         JOIN publications p ON p.id = o.publication_id
         LEFT JOIN users u ON u.id = o.user_id
        WHERE p.user_id = ?
        ORDER BY o.created_at DESC`,
      [req.userId]
    );

    const enviadas = [];
    for (const row of enviadasRaw) enviadas.push(await expirarSiCorresponde(row));
    const recibidas = [];
    for (const row of recibidasRaw) recibidas.push(await expirarSiCorresponde(row));

    res.json({ enviadas, recibidas });
  } catch (error) {
    console.error('Error en GET /api/offers/mine', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

app.get('/api/publications/:id/photos', requireAuth, async (req, res) => {
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
app.get('/api/users/:id', requireAuth, async (req, res) => {
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
    // Punto 7: el perfil publico de otro usuario tampoco muestra la direccion
    // exacta de sus publicaciones (misma regla que el listado del Home).
    activePublications.forEach((p) => { delete p.address; delete p.lat; delete p.lng; });

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
app.put('/api/users/:id', requireAuth, async (req, res) => {
  const { id } = req.params;
  const { name, email, phone, zone } = req.body;

  // El token dice quien llama, pero no que pueda tocar este perfil: sin esta
  // comparacion cualquier usuario logueado le reescribe los datos a otro.
  if (id !== req.userId) {
    return res.status(403).json({ success: false, message: 'Solo se puede editar el perfil propio' });
  }

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
app.post('/api/users/:id/ratings', requireAuth, async (req, res) => {
  const { id } = req.params;
  const { stars, role, comment } = req.body;

  // Quien califica sale del token, igual que el autor de una publicacion.
  // Tomarlo del body dejaba firmar una calificacion a nombre de cualquiera.
  const raterUserId = req.userId;

  // La reputacion del punto 2 es lo que opinan los demas: calificarse a uno
  // mismo es inflarla.
  if (id === raterUserId) {
    return res.status(400).json({ success: false, message: 'No podes calificarte a vos mismo' });
  }

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
      [id, raterUserId, estrellas, role, comment || null]
    );

    res.status(201).json({ success: true, reputation: await getReputation(id) });
  } catch (error) {
    console.error('Error en POST /api/users/:id/ratings', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// ==========================================
// 1. ENDPOINTS DE FAVORITOS - PUNTO 11
// ==========================================

// --- Endpoints de Favoritos (Punto 11) ---
app.post('/api/favorites', requireAuth, async (req, res) => {
  const { publicationId, savedPrice } = req.body;
  const userId = req.userId;

  if (!publicationId || savedPrice === undefined) {
    return res.status(400).json({ message: 'Faltan publicationId o savedPrice' });
  }

  try {
    const result = await db.run(
      'INSERT INTO favorites (userId, publicationId, savedPrice) VALUES (?, ?, ?)',
      [userId, publicationId, savedPrice]
    );

    const fav = await db.get(
      `SELECT f.id, f.userId, f.publicationId, f.savedPrice, f.savedAt,
              p.id as p_id, p.title, p.description, p.price, p.condition,
              p.category, p.zone
       FROM favorites f JOIN publications p ON f.publicationId = p.id
       WHERE f.id = ?`,
      [result.lastID]
    );

    res.status(201).json({
      id: fav.id,
      userId: fav.userId,
      publicationId: fav.publicationId,
      savedPrice: fav.savedPrice,
      savedAt: fav.savedAt,
      publication: {
        id: fav.p_id,
        title: fav.title,
        description: fav.description,
        price: fav.price,
        condition: fav.condition,
        category: fav.category,
        zone: fav.zone,
      },
    });
  } catch (error) {
    if (error.code === 'SQLITE_CONSTRAINT') {
      return res.status(409).json({ message: 'Esta publicación ya está en tus favoritos' });
    }
    console.error('Error en POST /api/favorites:', error);
    res.status(500).json({ message: error.message });
  }
});

app.get('/api/favorites', requireAuth, async (req, res) => {
  // El duenio de la lista sale del token, nunca del query: aceptar ?userId=
  // dejaba que cualquier usuario logueado leyera la lista de otro.
  const userId = req.userId;
  try {
    const rows = await db.all(
      `SELECT f.id, f.userId, f.publicationId, f.savedPrice, f.savedAt,
              p.id as p_id, p.title, p.description, p.price, p.condition,
              p.category, p.zone
       FROM favorites f JOIN publications p ON f.publicationId = p.id
       WHERE f.userId = ?
       ORDER BY f.savedAt DESC`,
      [userId]
    );

    const data = rows.map((r) => ({
      id: r.id,
      userId: r.userId,
      publicationId: r.publicationId,
      savedPrice: r.savedPrice,
      savedAt: r.savedAt,
      publication: {
        id: r.p_id,
        title: r.title,
        description: r.description,
        price: r.price,
        condition: r.condition,
        category: r.category,
        zone: r.zone,
      },
    }));

    res.json({ success: true, data });
  } catch (error) {
    console.error('Error en GET /api/favorites:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

app.delete('/api/favorites/:id', requireAuth, async (req, res) => {
  try {
    await db.run('DELETE FROM favorites WHERE id = ? AND userId = ?', [req.params.id, req.userId]);
    res.json({ success: true, message: 'Favorito eliminado' });
  } catch (error) {
    console.error('Error en DELETE /api/favorites/:id:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// --- Endpoints de Búsquedas Guardadas (Punto 11) ---
app.post('/api/saved-searches', requireAuth, async (req, res) => {
  const { searchTerm, category, minPrice, maxPrice, condition, zone, sort } = req.body;
  const userId = req.userId;

  if (!searchTerm) {
    return res.status(400).json({ message: 'Falta searchTerm' });
  }

  const filters = JSON.stringify({ category, minPrice, maxPrice, condition, zone, sort });

  try {
    const result = await db.run(
      'INSERT INTO saved_searches (userId, searchTerm, filters) VALUES (?, ?, ?)',
      [userId, searchTerm, filters]
    );

    res.status(201).json({
      id: result.lastID,
      userId,
      searchTerm,
      category: category || null,
      minPrice: minPrice != null ? minPrice : null,
      maxPrice: maxPrice != null ? maxPrice : null,
      condition: condition || null,
      zone: zone || null,
      sort: sort || 'recent',
      createdAt: new Date().toISOString(),
    });
  } catch (error) {
    console.error('Error en POST /api/saved-searches:', error);
    res.status(500).json({ message: error.message });
  }
});

app.get('/api/saved-searches', requireAuth, async (req, res) => {
  // El duenio de la lista sale del token, nunca del query: aceptar ?userId=
  // dejaba que cualquier usuario logueado leyera la lista de otro.
  const userId = req.userId;
  try {
    const rows = await db.all(
      'SELECT * FROM saved_searches WHERE userId = ? ORDER BY savedAt DESC',
      [userId]
    );

    const data = [];
    for (const r of rows) {
      const f = JSON.parse(r.filters || '{}');
      data.push({
        id: r.id,
        userId: r.userId,
        searchTerm: r.searchTerm,
        category: f.category || null,
        minPrice: f.minPrice != null ? f.minPrice : null,
        maxPrice: f.maxPrice != null ? f.maxPrice : null,
        condition: f.condition || null,
        zone: f.zone || null,
        sort: f.sort || 'recent',
        createdAt: r.savedAt,
        lastSeenAt: r.lastSeenAt || r.savedAt,
        newCount: await contarNovedades(r, f, userId),
      });
    }

    res.json({ success: true, data });
  } catch (error) {
    console.error('Error en GET /api/saved-searches:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

/**
 * Punto 11: cuantas publicaciones que coinciden con la busqueda aparecieron
 * desde la ultima vez que la persona la ejecuto (o desde que la guardo).
 *
 * Usa el mismo WHERE que el listado del Home, asi que "coincide" significa lo
 * mismo que veria al ejecutarla. No cuenta las publicaciones propias: que tu
 * propio articulo coincida con tu busqueda no es una novedad para vos.
 */
async function contarNovedades(busqueda, filtros, userId) {
  const { whereClause, filterParams } = filtrosDePublicaciones({
    search: busqueda.searchTerm,
    category: filtros.category,
    condition: filtros.condition,
    zone: filtros.zone,
    minPrice: filtros.minPrice,
    maxPrice: filtros.maxPrice,
  });
  // created_at y la marca estan los dos en el formato de SQLite
  // ("YYYY-MM-DD HH:MM:SS", UTC), asi que se comparan como texto sin convertir.
  const row = await db.get(
    `SELECT COUNT(*) AS total FROM publications p${whereClause}
        AND p.created_at > ?
        AND (p.user_id IS NULL OR p.user_id != ?)`,
    [...filterParams, busqueda.lastSeenAt || busqueda.savedAt, userId]
  );
  return row ? row.total : 0;
}

// Marca la busqueda como vista: las novedades se vuelven a contar desde ahora.
// La app lo llama al ejecutar la busqueda, que es cuando la persona ve el resultado.
app.patch('/api/saved-searches/:id/seen', requireAuth, async (req, res) => {
  try {
    // datetime('now') y no la hora de JS: tiene que quedar en el mismo formato
    // que publications.created_at para que la comparacion de arriba funcione.
    const { changes } = await db.run(
      "UPDATE saved_searches SET lastSeenAt = datetime('now') WHERE id = ? AND userId = ?",
      [req.params.id, req.userId]
    );
    if (changes === 0) {
      return res.status(404).json({ success: false, message: 'Busqueda no encontrada' });
    }
    res.json({ success: true, message: 'Busqueda marcada como vista' });
  } catch (error) {
    console.error('Error en PATCH /api/saved-searches/:id/seen:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

app.delete('/api/saved-searches/:id', requireAuth, async (req, res) => {
  try {
    await db.run('DELETE FROM saved_searches WHERE id = ? AND userId = ?', [req.params.id, req.userId]);
    res.json({ success: true, message: 'Búsqueda eliminada' });
  } catch (error) {
    console.error('Error en DELETE /api/saved-searches/:id:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// ==========================================
// 4. ENDPOINTS DE HISTORIAL Y CALIFICACIONES
// ==========================================

app.get('/api/operaciones', requireAuth, async (req, res) => {
  const { tipo, fechaInicio, fechaFin } = req.query;
  const userId = req.userId;
  console.log(`[HISTORIAL] Consultando para userId: ${userId}, tipo: ${tipo}`);

  try {
    // Buscamos ofertas aceptadas donde el usuario sea comprador o vendedor
    let sql = `
      SELECT
        o.id,
        p.title AS articuloNombre,
        o.amount AS montoFinal,
        o.created_at AS fecha,
        o.delivery_point AS fechaEntrega, -- Usamos delivery_point como placeholder de fecha entrega si no hay
        CASE WHEN o.user_id = ? THEN 'COMPRA' ELSE 'VENTA' END AS tipo,
        CASE WHEN o.user_id = ? THEN p.user_id ELSE o.user_id END AS contraparteId,
        CASE WHEN o.user_id = ? THEN su.name ELSE bu.name END AS contraparteNombre,
        EXISTS(SELECT 1 FROM ratings r WHERE r.rater_user_id = ? AND r.comment LIKE '%' || o.id || '%') as calificada -- heuristica simple
      FROM offers o
      JOIN publications p ON p.id = o.publication_id
      LEFT JOIN users su ON su.id = p.user_id
      LEFT JOIN users bu ON bu.id = o.user_id
      WHERE o.status = 'aceptada' AND (o.user_id = ? OR p.user_id = ?)
    `;
    const params = [userId, userId, userId, userId, userId, userId];

    if (tipo && tipo !== 'TODOS') {
      sql += " AND (CASE WHEN o.user_id = ? THEN 'COMPRA' ELSE 'VENTA' END) = ?";
      params.push(userId, tipo);
    }

    if (fechaInicio) {
      sql += " AND o.created_at >= ?";
      params.push(fechaInicio);
    }
    if (fechaFin) {
      sql += " AND o.created_at <= ?";
      params.push(fechaFin);
    }

    sql += " ORDER BY o.created_at DESC";

    const rows = await db.all(sql, params);
    // Convertir 0/1 de SQLite a booleanos reales para que GSON no explote
    const mappedRows = rows.map(row => ({
      ...row,
      calificada: !!row.calificada
    }));
    res.json(mappedRows);
  } catch (error) {
    console.error('Error en GET /api/operaciones:', error);
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/operaciones/:id/calificar', requireAuth, async (req, res) => {
  const { stars, comment } = req.body;
  const operacionId = req.params.id;
  const raterId = req.userId;

  try {
    const operacion = await db.get(
      `SELECT o.*, p.user_id as seller_id FROM offers o
       JOIN publications p ON p.id = o.publication_id
       WHERE o.id = ? AND o.status = 'aceptada'`,
      [operacionId]
    );

    if (!operacion) return res.status(404).json({ message: 'Operación no encontrada' });

    // Determinar a quién calificar y qué rol tiene el calificado
    let ratedUserId, role;
    if (operacion.user_id === raterId) {
       // Soy el comprador, califico al vendedor
       ratedUserId = operacion.seller_id;
       role = 'vendedor';
    } else if (operacion.seller_id === raterId) {
       // Soy el vendedor, califico al comprador
       ratedUserId = operacion.user_id;
       role = 'comprador';
    } else {
       return res.status(403).json({ message: 'No participaste en esta operación' });
    }

    await db.run(
      'INSERT INTO ratings (rated_user_id, rater_user_id, stars, role, comment) VALUES (?, ?, ?, ?, ?)',
      [ratedUserId, raterId, stars, role, comment + " (Op #" + operacionId + ")"]
    );

    res.status(201).send();
  } catch (error) {
    res.status(500).json({ error: error.message });
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
