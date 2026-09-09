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

// ==========================================
// ESQUEMA
// ==========================================

const CREATE_TABLES = [
  `CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      email TEXT UNIQUE,
      username TEXT UNIQUE,
      password TEXT,
      name TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )`,

  `CREATE TABLE IF NOT EXISTS otp_codes (
      email TEXT PRIMARY KEY,
      code TEXT NOT NULL,
      expires_at INTEGER NOT NULL
    )`,

  `CREATE TABLE IF NOT EXISTS publications (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      title TEXT NOT NULL,
      description TEXT,
      price REAL NOT NULL,
      condition TEXT,
      category TEXT,
      zone TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )`,

  // Punto 2: la reputacion se construye a partir de las calificaciones recibidas.
  // "role" es el rol que cumplio el usuario CALIFICADO en la operacion, y es lo
  // que permite contar operaciones concretadas como comprador y como vendedor.
  `CREATE TABLE IF NOT EXISTS ratings (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      rated_user_id TEXT NOT NULL,
      rater_user_id TEXT,
      stars INTEGER NOT NULL CHECK (stars BETWEEN 1 AND 5),
      role TEXT NOT NULL CHECK (role IN ('vendedor', 'comprador')),
      comment TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (rated_user_id) REFERENCES users (id)
    )`,

  // Punto 4: las preguntas que deja un interesado en la publicacion. El TP
  // pide que el interesado pueda "preguntar"; responder queda para el vendedor
  // desde la gestion de su publicacion.
  `CREATE TABLE IF NOT EXISTS questions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      publication_id INTEGER NOT NULL,
      user_id TEXT NOT NULL,
      text TEXT NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (publication_id) REFERENCES publications (id) ON DELETE CASCADE,
      FOREIGN KEY (user_id) REFERENCES users (id)
    )`,

  // Punto 4: las ofertas de precio. "status" queda listo para aceptar/rechazar,
  // que el TP no pide todavia pero es la continuacion natural.
  `CREATE TABLE IF NOT EXISTS offers (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      publication_id INTEGER NOT NULL,
      user_id TEXT NOT NULL,
      amount REAL NOT NULL,
      status TEXT NOT NULL DEFAULT 'pendiente',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (publication_id) REFERENCES publications (id) ON DELETE CASCADE,
      FOREIGN KEY (user_id) REFERENCES users (id)
    )`,

  // Punto 5: las fotos del articulo. Se guardan como data URI en base64 para
  // no necesitar un servidor de archivos aparte; por eso NUNCA se devuelven
  // en los listados, solo por GET /api/publications/:id/photos.
  `CREATE TABLE IF NOT EXISTS publication_photos (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      publication_id INTEGER NOT NULL,
      data TEXT NOT NULL,
      position INTEGER NOT NULL DEFAULT 0,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (publication_id) REFERENCES publications (id) ON DELETE CASCADE
    )`
];

/**
 * ALTER TABLE ADD COLUMN es idempotente a mano: SQLite falla si la columna ya
 * existe, asi que se consulta el PRAGMA antes. Necesario porque la base de
 * desarrollo ya esta creada y no se quiere perder lo que tiene.
 */
async function ensureColumn(table, column, definition) {
  const columnas = await dbAsync.all(`PRAGMA table_info(${table})`);
  if (columnas.some((c) => c.name === column)) return false;

  await dbAsync.run(`ALTER TABLE ${table} ADD COLUMN ${column} ${definition}`);
  console.log(`[DB] Migracion aplicada: ${table}.${column}`);
  return true;
}

// ==========================================
// DATOS DE PRUEBA
// ==========================================

const SEED_USERS = [
  { id: 'seed-ana', name: 'Ana Gómez', email: 'ana@ronda.test', username: 'ana', phone: '11-5555-0101', zone: 'Palermo' },
  { id: 'seed-bruno', name: 'Bruno Díaz', email: 'bruno@ronda.test', username: 'bruno', phone: '11-5555-0202', zone: 'Belgrano' },
  { id: 'seed-carla', name: 'Carla Ruiz', email: 'carla@ronda.test', username: 'carla', phone: '11-5555-0303', zone: 'Caballito' }
];

// [usuario calificado, estrellas, rol del calificado, comentario]
const SEED_RATINGS = [
  ['seed-ana', 5, 'vendedor', 'Entrega puntual y el artículo tal cual la publicación'],
  ['seed-ana', 4, 'vendedor', 'Todo bien, respondió rápido'],
  ['seed-ana', 5, 'vendedor', 'Excelente vendedora'],
  ['seed-ana', 4, 'comprador', 'Compradora seria, coordinamos sin problema'],
  ['seed-bruno', 3, 'vendedor', 'El artículo tenía más uso del descrito'],
  ['seed-bruno', 4, 'comprador', 'Puntual'],
  ['seed-carla', 5, 'comprador', 'Muy buena onda para coordinar'],
  ['seed-carla', 5, 'vendedor', 'Impecable']
];

const SEED_PUBLICATIONS = [
  ['Bicicleta Mountain Bike', 'Rodado 29 en excelente estado, frenos a disco', 150000, 'usado', 'Deportes', 'Palermo'],
  ['Teclado Mecánico RGB', 'Nuevo en caja sellada con switches blue intercambiables', 45000, 'nuevo', 'Tecnología', 'Belgrano'],
  ['Silla Ergonómica de Oficina', 'Poco uso, soporte lumbar y apoyabrazos regulables', 80000, 'como nuevo', 'Hogar', 'Caballito'],
  ['Cámara Mirrorless 4K', 'Incluye lente 18-55mm, bolso y dos baterías', 320000, 'usado', 'Tecnología', 'Recoleta'],
  ['Guitarra Criolla de Estudio', 'Excelente sonido para principiantes y avanzados', 65000, 'como nuevo', 'Música', 'Almagro']
];

async function seedPublications() {
  const row = await dbAsync.get('SELECT COUNT(*) AS count FROM publications');
  if (row && row.count > 0) return;

  for (const item of SEED_PUBLICATIONS) {
    await dbAsync.run(
      `INSERT INTO publications (title, description, price, condition, category, zone)
       VALUES (?, ?, ?, ?, ?, ?)`,
      item
    );
  }
  console.log('[DB] Se insertaron datos iniciales de prueba en la tabla "publications".');
}

async function seedUsersAndRatings() {
  for (const u of SEED_USERS) {
    await dbAsync.run(
      `INSERT OR IGNORE INTO users (id, email, username, name, phone, zone)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [u.id, u.email, u.username, u.name, u.phone, u.zone]
    );
  }

  const row = await dbAsync.get('SELECT COUNT(*) AS count FROM ratings');
  if (row && row.count > 0) return;

  for (const [ratedUserId, stars, role, comment] of SEED_RATINGS) {
    await dbAsync.run(
      'INSERT INTO ratings (rated_user_id, stars, role, comment) VALUES (?, ?, ?, ?)',
      [ratedUserId, stars, role, comment]
    );
  }
  console.log('[DB] Se insertaron calificaciones de prueba en la tabla "ratings".');
}

/**
 * Las publicaciones creadas antes del punto 2 no tienen duenio. Sin duenio el
 * perfil publico no puede listar "sus publicaciones activas", asi que se les
 * reparte uno de los usuarios semilla.
 */
async function assignOwnerlessPublications() {
  const huerfanas = await dbAsync.all(
    'SELECT id FROM publications WHERE user_id IS NULL ORDER BY id'
  );
  if (huerfanas.length === 0) return;

  for (let i = 0; i < huerfanas.length; i++) {
    const duenio = SEED_USERS[i % SEED_USERS.length].id;
    await dbAsync.run('UPDATE publications SET user_id = ? WHERE id = ?', [duenio, huerfanas[i].id]);
  }
  console.log(`[DB] Se asignó vendedor a ${huerfanas.length} publicaciones sin dueño.`);
}

async function init() {
  for (const sql of CREATE_TABLES) {
    await dbAsync.run(sql);
  }

  // Punto 2: datos personales editables del perfil.
  await ensureColumn('users', 'phone', 'TEXT');
  await ensureColumn('users', 'zone', 'TEXT');

  // Punto 2: el perfil publico muestra las publicaciones activas del vendedor.
  // "status" lo consume tambien el punto 5 (activa / pausada / vendida).
  await ensureColumn('publications', 'user_id', 'TEXT');
  await ensureColumn('publications', 'status', "TEXT NOT NULL DEFAULT 'activa'");

  await dbAsync.run(
    'CREATE INDEX IF NOT EXISTS idx_photos_publication ON publication_photos (publication_id, position)'
  );

  await dbAsync.run(
    'CREATE INDEX IF NOT EXISTS idx_questions_publication ON questions (publication_id, created_at)'
  );
  await dbAsync.run(
    'CREATE INDEX IF NOT EXISTS idx_offers_publication ON offers (publication_id, created_at)'
  );

  // El token paso de ser un UUID guardado en "sessions" a un JWT firmado, que
  // se valida por firma sin tocar la base. La tabla quedo sin uso: se descarta
  // para que no queden dos mecanismos de sesion conviviendo.
  await dbAsync.run('DROP TABLE IF EXISTS sessions');

  await seedPublications();
  await seedUsersAndRatings();
  await assignOwnerlessPublications();
}

/** El server espera esta promesa antes de escuchar, para no atender con el esquema a medio migrar. */
dbAsync.ready = init().catch((err) => {
  console.error('❌ Error inicializando la base:', err.message);
  throw err;
});

module.exports = dbAsync;
