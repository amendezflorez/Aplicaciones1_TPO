/**
 * Mapa de barrios linderos de CABA, para el filtro de "cercanía a la zona del
 * usuario" del punto 3.
 *
 * El TP pide cercanía, no igualdad: filtrar solo por el barrio propio dejaría
 * afuera una publicación a diez cuadras que está del otro lado de una avenida.
 * Sin coordenadas por publicación, la aproximación honesta es una tabla de
 * adyacencia curada: "cerca" = tu barrio + los que lo lindan.
 *
 * La tabla se declara en un solo sentido y se completa simétrica al cargar el
 * módulo, así no hay que acordarse de escribir cada par dos veces.
 */

const LINDEROS_DECLARADOS = {
  'Palermo': ['Recoleta', 'Belgrano', 'Almagro', 'Villa Crespo', 'Colegiales', 'Chacarita'],
  'Belgrano': ['Colegiales', 'Núñez', 'Villa Urquiza', 'Coghlan'],
  'Recoleta': ['Almagro', 'Balvanera', 'Retiro', 'San Nicolás'],
  'Caballito': ['Almagro', 'Flores', 'Boedo', 'Villa Crespo', 'Parque Chacabuco'],
  'Almagro': ['Balvanera', 'Villa Crespo', 'Boedo'],
  'Villa Crespo': ['Chacarita', 'Villa Ortúzar'],
  'Villa Urquiza': ['Coghlan', 'Villa Pueyrredón', 'Saavedra', 'Villa Ortúzar'],
  'Núñez': ['Saavedra'],
  'Colegiales': ['Chacarita'],
  'Chacarita': ['Paternal', 'Villa Ortúzar'],
  'Flores': ['Floresta', 'Parque Chacabuco', 'Villa Crespo'],
  'Boedo': ['San Cristóbal', 'Parque Chacabuco'],
  'Balvanera': ['San Cristóbal', 'San Nicolás', 'Once'],
  'Saavedra': ['Coghlan'],
  'Parque Chacabuco': ['Flores']
};

/** Quita acentos y pasa a minúsculas, para que "núñez" y "Nunez" caigan en la misma clave. */
function normalizar(zona) {
  return String(zona)
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '') // marcas de acento, ya separadas por NFD
    .trim()
    .toLowerCase();
}

// Índice normalizado y simétrico: clave normalizada -> Set de nombres canónicos.
const INDICE = (() => {
  const indice = new Map();

  const agregar = (a, b) => {
    const clave = normalizar(a);
    if (!indice.has(clave)) indice.set(clave, new Set([a]));
    indice.get(clave).add(b);
  };

  for (const [barrio, linderos] of Object.entries(LINDEROS_DECLARADOS)) {
    agregar(barrio, barrio);
    for (const lindero of linderos) {
      agregar(barrio, lindero);
      agregar(lindero, barrio); // simetría
    }
  }
  return indice;
})();

/**
 * Devuelve los nombres de barrio considerados "cerca" de la zona dada,
 * incluyéndola a ella misma.
 *
 * Si la zona no figura en la tabla (el usuario escribió algo que no es un
 * barrio conocido) se devuelve solo esa zona: el filtro degrada a igualdad
 * exacta en vez de fallar o devolver todo.
 */
function zonasCercanas(zona) {
  if (!zona || !String(zona).trim()) return [];

  const encontradas = INDICE.get(normalizar(zona));
  return encontradas ? Array.from(encontradas) : [String(zona).trim()];
}

module.exports = { zonasCercanas, normalizar };
