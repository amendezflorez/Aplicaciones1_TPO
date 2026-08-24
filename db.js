const Database = require('better-sqlite3');
const path = require('path');

// El archivo ronda.db se crea solo, en esta misma carpeta, la primera vez que corrés el server.
const db = new Database(path.join(__dirname, 'ronda.db'));

db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    email TEXT UNIQUE,
    username TEXT UNIQUE,
    password TEXT,
    name TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
  );

  CREATE TABLE IF NOT EXISTS otp_codes (
    email TEXT PRIMARY KEY,
    code TEXT NOT NULL,
    expires_at INTEGER NOT NULL
  );
`);

module.exports = db;
