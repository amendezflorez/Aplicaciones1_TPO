const db = require('./db');

async function check() {
  await db.ready;
  const JUAN_ID = '0b4800dd-69f1-4cfa-9ecd-bd73fed782f3';
  const rows = await db.all(`
      SELECT
        o.id,
        p.title AS articuloNombre,
        o.amount AS montoFinal,
        o.created_at AS fecha,
        o.delivery_point AS fechaEntrega,
        CASE WHEN o.user_id = ? THEN 'COMPRA' ELSE 'VENTA' END AS tipo,
        'Contraparte' AS contraparteNombre,
        EXISTS(SELECT 1 FROM ratings r WHERE r.rater_user_id = ? AND r.comment LIKE '%' || o.id || '%') as calificada
      FROM offers o
      JOIN publications p ON p.id = o.publication_id
      WHERE o.status = 'aceptada' AND (o.user_id = ? OR p.user_id = ?)
  `, [JUAN_ID, JUAN_ID, JUAN_ID, JUAN_ID]);

  console.log(JSON.stringify(rows, null, 2));
  process.exit(0);
}

check();
