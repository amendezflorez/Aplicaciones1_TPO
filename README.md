# Ronda - Aplicación Móvil Android & Backend Unificado 🛒📱

Proyecto desarrollado para la materia **Desarrollo de Aplicaciones 1**. Contiene la aplicación nativa para Android (Java) bajo el principio de **Single Activity Architecture** y la **API REST unificada** en Node.js con SQLite.

---

## 🚀 Funcionalidades Integradas

1. **Autenticación (Feature 1)**:
   - Login con usuario y contraseña (creación automática si no existe para pruebas).
   - Login y registro con código OTP de 6 dígitos enviado por email (simulado en consola).
   - Reenvío de código con cooldown de 60 segundos.
   - Sesión persistida con `SessionManager` en `SharedPreferences`.

2. **Explorar Publicaciones / Home (Feature 3)**:
   - Listado de productos en tarjetas (`CardView` + `RecyclerView`).
   - Buscador de texto libre en tiempo real (`SearchView`).
   - Selector de ordenamiento (`Spinner`): Más recientes, Menor precio, Mayor precio.
   - Diálogo de filtros avanzados (`AlertDialog`): Categoría, Condición (`nuevo`, `como nuevo`, `usado`), Zona geográfica y Rango de precios (`minPrice` / `maxPrice`).
   - Botón de **Cerrar Sesión** en la barra superior.

3. **Detalle de Publicación (Feature 4)**:
   - Galería con todas las fotos del artículo, descripción completa, categoría, estado, precio y fecha de publicación.
   - • Datos del vendedor con su reputación y acceso a su perfil público.
   - • Acciones disponibles según quién esté mirando: si es un interesado, puede preguntar, ofertar y guardar la publicación; si es el propio vendedor, accede a la gestión de su publicación.

---

## 🛠️ Cómo Iniciar el Backend Unificado

1. Abrí tu terminal en la carpeta `backend/`:
   ```bash
   cd backend
   ```
2. Instalá las dependencias (solo la primera vez):
   ```bash
   npm install
   ```
3. Iniciá el servidor:
   ```bash
   npm start
   ```

El servidor iniciará en `http://localhost:8080`.
Desde el **emulador de Android Studio**, la aplicación se comunicará a través de:
```
http://10.0.2.2:8080/api/
```

> **Base de Datos**: Los datos se almacenan en `ronda.db` (SQLite) con publicaciones semilla precargadas automáticamente.

---

## 📱 Estructura Android (Single Activity Architecture)

* **Actividad Principal**: `MainActivity.java` como host único.
* **Navegación**: `nav_graph.xml` que incluye:
  * `auth_nav_graph.xml`: `LoginFragment`, `EmailAuthFragment`, `OtpVerificationFragment`.
  * `home_nav_graph.xml`: `HomeFragment`, `DetallePublicacionFragment`.
* **Red**: `RetrofitClient.java` y `ApiService.java` apuntando a `http://10.0.2.2:8080/api/`.
* **Seguridad**: `android:usesCleartextTraffic="true"` habilitado en `AndroidManifest.xml`.
