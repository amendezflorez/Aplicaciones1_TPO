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
    )`,
  `CREATE TABLE IF NOT EXISTS favorites (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      userId TEXT NOT NULL,
      publicationId INTEGER NOT NULL,
      savedPrice REAL NOT NULL,
      savedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
      UNIQUE(userId, publicationId),
      FOREIGN KEY (publicationId) REFERENCES publications(id)
    )`,

  `CREATE TABLE IF NOT EXISTS saved_searches (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      userId TEXT NOT NULL,
      searchTerm TEXT NOT NULL,
      filters TEXT,
      savedAt DATETIME DEFAULT CURRENT_TIMESTAMP
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

// Password fija para poder loguearse como estos usuarios y ver las ofertas
// de ejemplo desde la app. El backend no se evalua ni se defiende, asi que no
// hace falta nada mas elaborado.
const SEED_PASSWORD = 'ronda123';

const SEED_USERS = [
  { id: 'seed-ana', name: 'Ana Gómez', email: 'ana@ronda.test', username: 'ana', phone: '11-5555-0101', zone: 'Palermo' },
  { id: 'seed-bruno', name: 'Bruno Díaz', email: 'bruno@ronda.test', username: 'bruno', phone: '11-5555-0202', zone: 'Belgrano' },
  { id: 'seed-carla', name: 'Carla Ruiz', email: 'carla@ronda.test', username: 'carla', phone: '11-5555-0303', zone: 'Caballito' }
];

// Punto 7: 48hs de vigencia para que una oferta siga "pendiente". Duplicada a
// mano aqui (server.js tiene la misma constante, OFFER_EXPIRY_MS) porque db.js
// no puede importar server.js sin generar una dependencia circular.
const OFFER_EXPIRY_MS = 48 * 60 * 60 * 1000;

// [comprador, indice en SEED_PUBLICATIONS, monto, mensaje, vencida ya]
// La primera y la segunda oferta caen sobre la misma publicacion (indice 0,
// de Ana) para poder probar en el momento que aceptar una rechaza la otra.
// La tercera se siembra ya vencida, para ver el lazy-expiry apenas se lee.
const SEED_OFFERS = [
  ['seed-carla', 0, 140000, '¿Lo dejarías en $140.000? Puedo pasar a buscarlo esta semana.', false],
  ['seed-bruno', 0, 130000, null, false],
  ['seed-ana', 1, 40000, 'Te lo compro ya si me lo dejás en $40.000', true]
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

// address/lat/lng: puente al punto 8 (coordinacion de entrega). Se siembran
// ya en esta feature porque el gate de visibilidad de la direccion (punto 7)
// necesita datos reales para poder probarse.
const SEED_PUBLICATIONS = [
  ['Bicicleta Mountain Bike', 'Rodado 29 en excelente estado, frenos a disco', 150000, 'usado', 'Deportes', 'Palermo', 'Av. Santa Fe 4200, Palermo, CABA', -34.5889, -58.4298],
  ['Teclado Mecánico RGB', 'Nuevo en caja sellada con switches blue intercambiables', 45000, 'nuevo', 'Tecnología', 'Belgrano', 'Av. Cabildo 2000, Belgrano, CABA', -34.5627, -58.4583],
  ['Silla Ergonómica de Oficina', 'Poco uso, soporte lumbar y apoyabrazos regulables', 80000, 'como nuevo', 'Hogar', 'Caballito', 'Av. Rivadavia 5200, Caballito, CABA', -34.6178, -58.4436],
  ['Cámara Mirrorless 4K', 'Incluye lente 18-55mm, bolso y dos baterías', 320000, 'usado', 'Tecnología', 'Recoleta', 'Av. Las Heras 2000, Recoleta, CABA', -34.5875, -58.3974],
  ['Guitarra Criolla de Estudio', 'Excelente sonido para principiantes y avanzados', 65000, 'como nuevo', 'Música', 'Almagro', 'Av. Rivadavia 4200, Almagro, CABA', -34.6083, -58.4205]
];

async function seedPublications() {
  const row = await dbAsync.get('SELECT COUNT(*) AS count FROM publications');
  if (row && row.count > 0) return;

  for (const item of SEED_PUBLICATIONS) {
    await dbAsync.run(
      `INSERT INTO publications (title, description, price, condition, category, zone, address, lat, lng)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      item
    );
  }
  console.log('[DB] Se insertaron datos iniciales de prueba en la tabla "publications".');
}

async function seedUsersAndRatings() {
  for (const u of SEED_USERS) {
    await dbAsync.run(
      `INSERT OR IGNORE INTO users (id, email, username, password, name, phone, zone)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [u.id, u.email, u.username, SEED_PASSWORD, u.name, u.phone, u.zone]
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
 * Punto 7: ofertas de ejemplo para poder probar el flujo de entrada, sin
 * arrancar de una base vacia. Depende de que SEED_PUBLICATIONS ya haya
 * insertado sus filas en orden (los ids autoincrementales empiezan en 1).
 */
async function seedOffers() {
  const row = await dbAsync.get('SELECT COUNT(*) AS count FROM offers');
  if (row && row.count > 0) return;

  const ahora = Date.now();
  for (const [buyerId, publicacionIndex, amount, message, yaVencida] of SEED_OFFERS) {
    const expiresAt = yaVencida ? ahora - 60 * 60 * 1000 : ahora + OFFER_EXPIRY_MS;
    await dbAsync.run(
      `INSERT INTO offers (publication_id, user_id, amount, message, expires_at)
       VALUES (?, ?, ?, ?, ?)`,
      [publicacionIndex + 1, buyerId, amount, message, expiresAt]
    );
  }
  console.log('[DB] Se insertaron ofertas de prueba en la tabla "offers".');
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

  // Punto 11: desde cuando contar "publicaciones nuevas" de una busqueda
  // guardada. Se actualiza al ejecutarla; mientras sea NULL cuenta desde que se
  // guardo (savedAt). camelCase como el resto de la tabla.
  await ensureColumn('saved_searches', 'lastSeenAt', 'DATETIME');

  // Coordinación de la Entrega: punto de entrega acordado
  await ensureColumn('offers', 'delivery_point', 'TEXT');
  await ensureColumn('publications', 'delivery_point', 'TEXT');

  // Punto 7: direccion exacta de entrega. Vive en la publicacion (es el punto
  // de encuentro fijo que define el vendedor), no en la oferta. El endpoint de
  // detalle recien la manda si quien pregunta es el dueno o tiene una oferta
  // aceptada sobre esta publicacion (ver GET /api/publications/:id).
  await ensureColumn('publications', 'address', 'TEXT');
  await ensureColumn('publications', 'lat', 'REAL');
  await ensureColumn('publications', 'lng', 'REAL');

  // Punto 7: mensaje opcional del comprador y vencimiento de la oferta.
  await ensureColumn('offers', 'message', 'TEXT');
  await ensureColumn('offers', 'expires_at', 'INTEGER');

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
  await seedOffers();
  await seedOperacionesAceptadas();
}

/**
 * Punto 10: Sembrar operaciones aceptadas para probar el historial.
 * Usamos la primera oferta de SEED_OFFERS y la marcamos como aceptada.
 */
async function seedOperacionesAceptadas() {
  const row = await dbAsync.get("SELECT COUNT(*) as count FROM offers WHERE status = 'aceptada'");
  if (row && row.count > 0) return;

  // Ana es vendedora (seed-ana), Carla es compradora (seed-carla).
  // La primera publicación es la bici.
  // Ponemos una fecha de hace 2 días para que el botón "Calificar" aparezca (dentro de los 7 días).
  const haceDosDias = new Date();
  haceDosDias.setDate(haceDosDias.getDate() - 2);
  const fechaStr = haceDosDias.toISOString().split('T')[0]; // YYYY-MM-DD

  await dbAsync.run(
    `UPDATE offers SET status = 'aceptada', delivery_point = ? WHERE id = 1`,
    [fechaStr]
  );

  // También creamos una venta para Ana (ella vendió a Bruno)
  await dbAsync.run(
    `UPDATE offers SET status = 'aceptada', delivery_point = ? WHERE id = 2`,
    [fechaStr]
  );

  console.log('[DB] Se sembraron operaciones aceptadas para el historial.');
}

/** El server espera esta promesa antes de escuchar, para no atender con el esquema a medio migrar. */
dbAsync.ready = init().catch((err) => {
  console.error('❌ Error inicializando la base:', err.message);
  throw err;
});

module.exports = dbAsync;
