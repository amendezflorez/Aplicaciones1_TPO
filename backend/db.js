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

const SEED_PASSWORD = 'ronda123';

const SEED_USERS = [
  { id: 'seed-ana', name: 'Ana Gómez', email: 'ana@ronda.test', username: 'ana', phone: '11-5555-0101', zone: 'Palermo' },
  { id: 'seed-bruno', name: 'Bruno Díaz', email: 'bruno@ronda.test', username: 'bruno', phone: '11-5555-0202', zone: 'Belgrano' },
  { id: 'seed-carla', name: 'Carla Ruiz', email: 'carla@ronda.test', username: 'carla', phone: '11-5555-0303', zone: 'Caballito' }
];

const OFFER_EXPIRY_MS = 48 * 60 * 60 * 1000;

const SEED_OFFERS = [
  ['seed-carla', 0, 140000, '¿Lo dejarías en $140.000? Puedo pasar a buscarlo esta semana.', false],
  ['seed-bruno', 0, 130000, null, false],
  ['seed-ana', 1, 40000, 'Te lo compro ya si me lo dejás en $40.000', true]
];

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

async function seedDemoHistory() {
  console.log('🌱 Poblando datos para el historial de juan@hotmail.com...');

  const JUAN_ID = '0b4800dd-69f1-4cfa-9ecd-bd73fed782f3'; // ID detectado en la DB

  // 1. Asegurar que Juan existe con datos Senior
  await dbAsync.run(`
    INSERT INTO users (id, email, username, password, name, zone)
    VALUES (?, 'juan@hotmail.com', 'juan@hotmail.com', 'juan123', 'Juan Pérez', 'Palermo')
    ON CONFLICT(id) DO UPDATE SET name='Juan Pérez', zone='Palermo'
  `, [JUAN_ID]);

  // 2. Otros usuarios para interactuar
  const uMaria = 'u-maria', uTech = 'u-tech', uCarlos = 'u-carlos';
  await dbAsync.run(`INSERT OR IGNORE INTO users (id, name, email, zone) VALUES
    ('${uMaria}', 'María López', 'maria@test.com', 'Belgrano'),
    ('${uTech}', 'Tech Store SRL', 'ventas@techstore.com', 'Centro'),
    ('${uCarlos}', 'Carlos Gómez', 'carlos@test.com', 'Caballito')`);

  const getFechaStr = (diasAtras) => {
    const d = new Date();
    d.setDate(d.getDate() - diasAtras);
    return d.toISOString().split('T')[0];
  };

  // 3. Limpiar operaciones viejas de Juan para que la demo sea limpia
  await dbAsync.run("DELETE FROM offers WHERE user_id = ? OR publication_id IN (SELECT id FROM publications WHERE user_id = ?)", [JUAN_ID, JUAN_ID]);
  await dbAsync.run("DELETE FROM ratings WHERE rater_user_id = ? OR rated_user_id = ?", [JUAN_ID, JUAN_ID]);

  // 4. COMPRAS de Juan (Él es comprador)

  // Op 1: PS5 - Comprada hace 2 días (En Septiembre 2026)
  const pub1 = await dbAsync.run("INSERT INTO publications (title, price, user_id, status) VALUES ('PlayStation 5', 850000, ?, 'vendida')", [uMaria]);
  await dbAsync.run("INSERT INTO offers (publication_id, user_id, amount, status, delivery_point, created_at) VALUES (?, ?, 850000, 'aceptada', ?, '2026-09-18')", [pub1.lastID, JUAN_ID, getFechaStr(2)]);

  // Op 2: Monitor - Comprado en Agosto 2026
  const pub2 = await dbAsync.run("INSERT INTO publications (title, price, user_id, status) VALUES ('Monitor 24 pulgadas', 120000, ?, 'vendida')", [uTech]);
  await dbAsync.run("INSERT INTO offers (publication_id, user_id, amount, status, delivery_point, created_at) VALUES (?, ?, 115000, 'aceptada', ?, '2026-08-15')", [pub2.lastID, JUAN_ID, getFechaStr(15)]);

  // Op 3: Teclado - Comprado en Julio 2026 (YA CALIFICADO)
  const pub3 = await dbAsync.run("INSERT INTO publications (title, price, user_id, status) VALUES ('Teclado Mecánico', 45000, ?, 'vendida')", [uCarlos]);
  const off3 = await dbAsync.run("INSERT INTO offers (publication_id, user_id, amount, status, delivery_point, created_at) VALUES (?, ?, 40000, 'aceptada', ?, '2026-07-20')", [pub3.lastID, JUAN_ID, getFechaStr(3)]);
  await dbAsync.run("INSERT INTO ratings (rated_user_id, rater_user_id, stars, role, comment) VALUES (?, ?, 5, 'vendedor', 'Excelente producto Op #' || ?)", [uCarlos, JUAN_ID, off3.lastID]);

  // Op 6: Smart TV - Comprada en Diciembre 2025
  const pub6 = await dbAsync.run("INSERT INTO publications (title, price, user_id, status) VALUES ('Smart TV 50 pulgadas', 450000, ?, 'vendida')", [uTech]);
  await dbAsync.run("INSERT INTO offers (publication_id, user_id, amount, status, delivery_point, created_at) VALUES (?, ?, 450000, 'aceptada', ?, '2025-12-10')", [pub6.lastID, JUAN_ID, getFechaStr(300)]);

  // Op 7: Mesa de Comedor - Comprada hace 10 días
  const pub7 = await dbAsync.run("INSERT INTO publications (title, price, user_id, status) VALUES ('Mesa de Comedor', 95000, ?, 'vendida')", [uMaria]);
  await dbAsync.run("INSERT INTO offers (publication_id, user_id, amount, status, delivery_point, created_at) VALUES (?, ?, 90000, 'aceptada', ?, ?)", [pub7.lastID, JUAN_ID, getFechaStr(10), getFechaStr(10)]);

  // 5. VENTAS de Juan (Él es vendedor)

  // Op 4: Bici - Vendida en Junio 2026
  const pub4 = await dbAsync.run("INSERT INTO publications (title, price, user_id, status) VALUES ('Bicicleta Mountain Bike', 150000, ?, 'vendida')", [JUAN_ID]);
  await dbAsync.run("INSERT INTO offers (publication_id, user_id, amount, status, delivery_point, created_at) VALUES (?, ?, 145000, 'aceptada', ?, '2026-06-10')", [pub4.lastID, uMaria, getFechaStr(4)]);

  // Op 5: Lámpara - Vendida en Enero 2026
  const pub5 = await dbAsync.run("INSERT INTO publications (title, price, user_id, status) VALUES ('Lámpara de Escritorio', 15000, ?, 'vendida')", [JUAN_ID]);
  await dbAsync.run("INSERT INTO offers (publication_id, user_id, amount, status, delivery_point, created_at) VALUES (?, ?, 15000, 'aceptada', ?, '2026-01-05')", [pub5.lastID, uTech, getFechaStr(40)]);

  // Op 8: iPhone 13 - Vendida hace 1 día (YA CALIFICADA por el comprador)
  const pub8 = await dbAsync.run("INSERT INTO publications (title, price, user_id, status) VALUES ('iPhone 13 128GB', 750000, ?, 'vendida')", [JUAN_ID]);
  const off8 = await dbAsync.run("INSERT INTO offers (publication_id, user_id, amount, status, delivery_point, created_at) VALUES (?, ?, 750000, 'aceptada', ?, ?)", [pub8.lastID, uCarlos, getFechaStr(1), getFechaStr(1)]);
  await dbAsync.run("INSERT INTO ratings (rated_user_id, rater_user_id, stars, role, comment) VALUES (?, ?, 5, 'vendedor', 'Vendedor excelente Op #' || ?)", [JUAN_ID, uCarlos, off8.lastID]);

  // Op 9: Auriculares Sony - Vendida hace 5 días
  const pub9 = await dbAsync.run("INSERT INTO publications (title, price, user_id, status) VALUES ('Auriculares Sony WH-1000XM4', 350000, ?, 'vendida')", [JUAN_ID]);
  await dbAsync.run("INSERT INTO offers (publication_id, user_id, amount, status, delivery_point, created_at) VALUES (?, ?, 340000, 'aceptada', ?, ?)", [pub9.lastID, uMaria, getFechaStr(5), getFechaStr(5)]);

  // Op 10: Libro de Cocina - Vendida en 2024
  const pub10 = await dbAsync.run("INSERT INTO publications (title, price, user_id, status) VALUES ('Libro de Cocina Italiana', 5000, ?, 'vendida')", [JUAN_ID]);
  await dbAsync.run("INSERT INTO offers (publication_id, user_id, amount, status, delivery_point, created_at) VALUES (?, ?, 5000, 'aceptada', ?, '2024-05-20')", [pub10.lastID, uTech, getFechaStr(800)]);

  // 6. Reputación de Juan (4.2 promedio inicial con estas 4)
  await dbAsync.run("INSERT INTO ratings (rated_user_id, stars, role, comment) VALUES (?, 5, 'vendedor', 'Todo ok'), (?, 4, 'vendedor', 'Puntual'), (?, 4, 'comprador', 'Correcto'), (?, 4, 'comprador', 'Buen trato')", [JUAN_ID, JUAN_ID, JUAN_ID, JUAN_ID]);

  // 7. Reputación para los otros usuarios (para testear el cambio)

  // María López (u-maria): Empieza con 3.0 (una de 3)
  await dbAsync.run("INSERT INTO ratings (rated_user_id, stars, role, comment) VALUES ('u-maria', 3, 'vendedor', 'Vendedora aceptable')");

  // Tech Store (u-tech): Empieza con 4.5 (5 y 4)
  await dbAsync.run("INSERT INTO ratings (rated_user_id, stars, role, comment) VALUES ('u-tech', 5, 'vendedor', 'Excelente servicio'), ('u-tech', 4, 'vendedor', 'Muy profesionales')");

  // Carlos Gómez (u-carlos): Empieza con 5.0 (una de 5)
  await dbAsync.run("INSERT INTO ratings (rated_user_id, stars, role, comment) VALUES ('u-carlos', 5, 'vendedor', 'El mejor de la zona')");

  console.log('✅ Datos de demo cargados con éxito para Juan y contrapartes.');
}

async function seedPhotos() {
  // Limpiamos las fotos anteriores para asegurarnos de que se aplique la nueva imagen
  await dbAsync.run('DELETE FROM publication_photos');

  // Imagen real de la raqueta que pasó el usuario
  const raquetaImg = 'data:image/webp;base64,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';

  // Placeholder: Un cuadro gris de 100x100 en Base64
  const placeholder = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAYAAABw4pVUAAAAL0lEQVR4nO3BAQ0AAADCoPdPbQ8HFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOA9DAUAAAF7Yt30AAAAAElFTkSuQmCC';

  const publications = await dbAsync.all('SELECT id, title FROM publications');
  for (const pub of publications) {
    const img = pub.title.toLowerCase().includes('raqueta') ? raquetaImg : placeholder;
    await dbAsync.run(
      'INSERT INTO publication_photos (publication_id, data, position) VALUES (?, ?, ?)',
      [pub.id, img, 0]
    );
  }
  console.log('[DB] Se insertaron fotos de prueba (incluyendo la raqueta real).');
}

async function init() {
  for (const sql of CREATE_TABLES) {
    await dbAsync.run(sql);
  }

  await ensureColumn('users', 'phone', 'TEXT');
  await ensureColumn('users', 'zone', 'TEXT');
  await ensureColumn('publications', 'user_id', 'TEXT');
  await ensureColumn('publications', 'status', "TEXT NOT NULL DEFAULT 'activa'");
  await ensureColumn('saved_searches', 'lastSeenAt', 'DATETIME');
  await ensureColumn('offers', 'delivery_point', 'TEXT');
  await ensureColumn('publications', 'delivery_point', 'TEXT');
  await ensureColumn('publications', 'address', 'TEXT');
  await ensureColumn('publications', 'lat', 'REAL');
  await ensureColumn('publications', 'lng', 'REAL');
  await ensureColumn('offers', 'message', 'TEXT');
  await ensureColumn('offers', 'expires_at', 'INTEGER');

  await dbAsync.run('CREATE INDEX IF NOT EXISTS idx_photos_publication ON publication_photos (publication_id, position)');
  await dbAsync.run('CREATE INDEX IF NOT EXISTS idx_questions_publication ON questions (publication_id, created_at)');
  await dbAsync.run('CREATE INDEX IF NOT EXISTS idx_offers_publication ON offers (publication_id, created_at)');
  await dbAsync.run('DROP TABLE IF EXISTS sessions');

  await seedPublications();
  await seedUsersAndRatings();
  await assignOwnerlessPublications();
  await seedOffers();
  await seedDemoHistory();
  await seedPhotos(); // Agregado para las imágenes
}

dbAsync.ready = init().catch((err) => {
  console.error('❌ Error inicializando la base:', err.message);
  throw err;
});

module.exports = dbAsync;
