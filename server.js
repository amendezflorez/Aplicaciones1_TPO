const express = require('express');
const cors = require('cors');
const { v4: uuidv4 } = require('uuid');
const db = require('./db');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = 8080;
const OTP_EXPIRY_MS = 5 * 60 * 1000; // el código vale por 5 minutos

function generateOtp() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

// ---------- LOGIN CON USUARIO Y CONTRASEÑA ----------
// Simplificado a propósito (no es evaluado): si el usuario no existe, se crea al vuelo.
app.post('/api/auth/login', (req, res) => {
  const { username, password } = req.body;

  if (!username || !password) {
    return res.status(400).json({ success: false, message: 'Faltan username o password' });
  }

  let user = db.prepare('SELECT * FROM users WHERE username = ?').get(username);

  if (!user) {
    const id = uuidv4();
    db.prepare('INSERT INTO users (id, username, password, name) VALUES (?, ?, ?, ?)')
      .run(id, username, password, username);
    user = { id, username, password, name: username, email: null };
    console.log(`[LOGIN] Usuario nuevo creado: ${username}`);
  } else if (user.password !== password) {
    return res.status(401).json({ success: false, message: 'Contraseña incorrecta' });
  }

  const token = uuidv4();
  res.json({
    token,
    userId: user.id,
    email: user.email,
    name: user.name,
  });
});

// ---------- SOLICITAR CÓDIGO OTP ----------
app.post('/api/auth/otp/request', (req, res) => {
  const { email } = req.body;
  if (!email) return res.status(400).json({ success: false, message: 'Falta email' });

  const code = generateOtp();
  const expiresAt = Date.now() + OTP_EXPIRY_MS;

  db.prepare(`
    INSERT INTO otp_codes (email, code, expires_at) VALUES (?, ?, ?)
    ON CONFLICT(email) DO UPDATE SET code = excluded.code, expires_at = excluded.expires_at
  `).run(email, code, expiresAt);

  console.log(`\n📩 Código OTP para ${email}: ${code}\n`);

  res.json({ success: true, message: 'Código enviado' });
});

// ---------- REENVIAR CÓDIGO OTP ----------
app.post('/api/auth/otp/resend', (req, res) => {
  const { email } = req.body;
  if (!email) return res.status(400).json({ success: false, message: 'Falta email' });

  const code = generateOtp();
  const expiresAt = Date.now() + OTP_EXPIRY_MS;

  db.prepare(`
    INSERT INTO otp_codes (email, code, expires_at) VALUES (?, ?, ?)
    ON CONFLICT(email) DO UPDATE SET code = excluded.code, expires_at = excluded.expires_at
  `).run(email, code, expiresAt);

  console.log(`\n📩 Código OTP reenviado para ${email}: ${code}\n`);

  res.json({ success: true, message: 'Código reenviado' });
});

// ---------- VERIFICAR CÓDIGO OTP ----------
app.post('/api/auth/otp/verify', (req, res) => {
  const { email, code } = req.body;
  if (!email || !code) return res.status(400).json({ success: false, message: 'Faltan datos' });

  const record = db.prepare('SELECT * FROM otp_codes WHERE email = ?').get(email);

  if (!record) {
    return res.status(400).json({ success: false, message: 'No se solicitó un código para este email' });
  }
  if (Date.now() > record.expires_at) {
    return res.status(400).json({ success: false, message: 'El código expiró' });
  }
  if (record.code !== code) {
    return res.status(400).json({ success: false, message: 'Código incorrecto' });
  }

  // Código válido: buscar o crear el usuario por email
  let user = db.prepare('SELECT * FROM users WHERE email = ?').get(email);
  if (!user) {
    const id = uuidv4();
    const name = email.split('@')[0];
    db.prepare('INSERT INTO users (id, email, name) VALUES (?, ?, ?)').run(id, email, name);
    user = { id, email, name };
    console.log(`[OTP] Usuario nuevo creado: ${email}`);
  }

  // El código ya se usó, se borra
  db.prepare('DELETE FROM otp_codes WHERE email = ?').run(email);

  const token = uuidv4();
  res.json({
    token,
    userId: user.id,
    email: user.email,
    name: user.name,
  });
});

app.listen(PORT, () => {
  console.log(`✅ Backend de Ronda corriendo en http://localhost:${PORT}`);
  console.log(`   Desde el emulador de Android, la app le habla a http://10.0.2.2:${PORT}/api/`);
});
