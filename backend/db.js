const sqlite3 = require('sqlite3').verbose();
const path = require('path');

const dbPath = path.join(__dirname, 'ronda.db');
const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error('❌ Error al conectar con la base de datos SQLite:', err.message);
  } else {
    console.log('📦 Conectado a la base de datos SQLite (ronda.db)');
  }
});

const dbAsync = {
  get(sql, params = []) {
    return new Promise((resolve, reject) => {
      db.get(sql, params, (err, row) => {
        if (err) reject(err);
        else resolve(row);
      });
    });
  },
  all(sql, params = []) {
    return new Promise((resolve, reject) => {
      db.all(sql, params, (err, rows) => {
        if (err) reject(err);
        else resolve(rows || []);
      });
    });
  },
  run(sql, params = []) {
    return new Promise((resolve, reject) => {
      db.run(sql, params, function (err) {
        if (err) reject(err);
        else resolve({ lastID: this.lastID, changes: this.changes });
      });
    });
  },
  raw: db
};

db.serialize(() => {
  db.run(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      email TEXT UNIQUE,
      username TEXT UNIQUE,
      password TEXT,
      name TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS otp_codes (
      email TEXT PRIMARY KEY,
      code TEXT NOT NULL,
      expires_at INTEGER NOT NULL
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS publications (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      title TEXT NOT NULL,
      description TEXT,
      price REAL NOT NULL,
      condition TEXT,
      category TEXT,
      zone TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);

  db.get('SELECT COUNT(*) as count FROM publications', (err, row) => {
    if (!err && row && row.count === 0) {
      const stmt = db.prepare(`
        INSERT INTO publications (title, description, price, condition, category, zone)
        VALUES (?, ?, ?, ?, ?, ?)
      `);

      const seedPublications = [
        ['Bicicleta Mountain Bike', 'Rodado 29 en excelente estado, frenos a disco', 150000, 'usado', 'Deportes', 'Palermo'],
        ['Teclado Mecánico RGB', 'Nuevo en caja sellada con switches blue intercambiables', 45000, 'nuevo', 'Tecnología', 'Belgrano'],
        ['Silla Ergonómica de Oficina', 'Poco uso, soporte lumbar y apoyabrazos regulables', 80000, 'como nuevo', 'Hogar', 'Caballito'],
        ['Cámara Mirrorless 4K', 'Incluye lente 18-55mm, bolso y dos baterías', 320000, 'usado', 'Tecnología', 'Recoleta'],
        ['Guitarra Criolla de Estudio', 'Excelente sonido para principiantes y avanzados', 65000, 'como nuevo', 'Música', 'Almagro']
      ];

      for (const item of seedPublications) {
        stmt.run(...item);
      }
      stmt.finalize();
      console.log('[DB] Se insertaron datos iniciales de prueba en la tabla "publications".');
    }
  });
});

module.exports = dbAsync;
