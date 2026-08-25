```markdown
# Ronda - Aplicación Móvil de Compra y Venta entre Personas 🛒📱

Proyecto desarrollado para la materia **Desarrollo de Aplicaciones 1**. Este repositorio contiene la implementación nativa para Android (Java) y la API REST de backend construida con Node.js y SQLite.

---

## 📌 Contexto y Objetivo
**Ronda** es una plataforma orientada a la compraventa de artículos entre particulares, permitiendo publicar productos en desuso, negociar precios y coordinar entregas de forma segura.

Este repositorio documenta e implementa el **Punto 3: Explorar Publicaciones (Home)** del Trabajo Práctico Obligatorio (TPO).

---

## 🚀 Alcance de la Feature: Explorar Publicaciones (Home)
El módulo principal de exploración incluye:
* **Listado paginado de publicaciones**: Visualización de tarjetas con título, precio, estado del artículo (`nuevo`, `como nuevo`, `usado`), categoría y zona geográfica del vendedor.
* **Buscador de texto libre**: Filtro dinámico sobre el título y la descripción del producto.
* **Filtros combinados**:
  * Categoría del producto.
  * Rango de precios (`minPrice` / `maxPrice`).
  * Estado de conservación del artículo.
  * Zona o cercanía.
* **Criterios de ordenamiento**:
  * Más recientes.
  * Menor precio (`price_asc`).
  * Mayor precio (`price_desc`).

---

## 🛠 Tecnologías y Arquitectura

### 📱 Android (Frontend Nativo)
* **Lenguaje:** Java
* **Patrón de Arquitectura:** Single Activity Architecture con Jetpack Navigation Component.
* **UI Components:** `Fragment`, `RecyclerView`, `CardView`, `SearchView`, `Spinner`.
* **Networking & Serialización:** Retrofit 2 + Gson Converter.
* **Gestión de Dependencias:** Gradle con Version Catalog (`libs.versions.toml`).

### 🌐 Backend & Persistencia
* **Runtime:** Node.js + Express.
* **Base de Datos:** SQLite3 (base de datos relacional embebida/local).
* **CORS:** Middleware habilitado para pruebas desde emulador y dispositivos locales.

---

## 📂 Estructura del Proyecto

```text
ronda-project/
├── backend/
│   ├── package.json
│   ├── server.js              # Servidor Express + Endpoints + Seed SQLite
│   └── ronda.db               # Base de datos SQLite generada automáticamente
│
└── android/
    ├── app/
    │   ├── src/
    │   │   └── main/
    │   │       ├── java/com/ejemplo/ronda/
    │   │       │   ├── data/
    │   │       │   │   ├── model/       # Publication, PublicationResponse
    │   │       │   │   └── network/     # ApiService, RetrofitClient
    │   │       │   └── ui/
    │   │       │       ├── home/        # HomeFragment, PublicationAdapter
    │   │       │       └── main/        # MainActivity
    │   │       └── res/
    │   │           ├── layout/          # activity_main.xml, fragment_home.xml, item_publication.xml
    │   │           └── navigation/      # nav_graph.xml
    │   └── build.gradle.kts
    └── gradle/
        └── libs.versions.toml