const express = require('express');
const cors = require('cors');
const { randomUUID } = require('crypto');
const db = require('./db');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 8080;
const OTP_EXPIRY_MS = 5 * 60 * 1000; // 5 minutos de validez

function generateOtp() {
  return Math.floor(100000 + Math.random() * 900000).toString();
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
    sortBy,
    page = 1,
    limit = 10
  } = req.query;

  let query = 'SELECT * FROM publications WHERE 1=1';
  const params = [];

  if (search) {
    query += ' AND (title LIKE ? OR description LIKE ?)';
    params.push(`%${search}%`, `%${search}%`);
  }
  if (category) {
    query += ' AND category = ?';
    params.push(category);
  }
  if (condition) {
    query += ' AND condition = ?';
    params.push(condition);
  }
  if (zone) {
    query += ' AND zone = ?';
    params.push(zone);
  }
  if (minPrice) {
    query += ' AND price >= ?';
    params.push(Number(minPrice));
  }
  if (maxPrice) {
    query += ' AND price <= ?';
    params.push(Number(maxPrice));
  }

  const sortMap = {
    'price_asc': 'price ASC',
    'price_desc': 'price DESC',
    'recent': 'created_at DESC'
  };
  const orderByClause = sortMap[sortBy] || 'created_at DESC';
  query += ` ORDER BY ${orderByClause}`;

  const limitNum = Math.max(1, parseInt(limit, 10) || 10);
  const pageNum = Math.max(1, parseInt(page, 10) || 1);
  const offset = (pageNum - 1) * limitNum;

  query += ' LIMIT ? OFFSET ?';
  params.push(limitNum, offset);

  try {
    const rows = await db.all(query, params);
    res.json({
      data: rows,
      page: pageNum,
      limit: limitNum
    });
  } catch (error) {
    console.error('Error al consultar publicaciones:', error);
    res.status(500).json({ error: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`===================================================`);
  console.log(`✅ Backend Unificado de Ronda corriendo en http://localhost:${PORT}`);
  console.log(`📲 Base URL para Android Emulator: http://10.0.2.2:${PORT}/api/`);
  console.log(`===================================================`);
});
