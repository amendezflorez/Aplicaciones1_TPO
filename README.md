# Ronda - App Android (feature_1)

Esta rama cubre el punto 1 del TP: Autenticación y Registro de Usuarios.

## Implementado

- Login con usuario y contraseña.
- Login alternativo por mail + código OTP (con reenvío y cooldown de 60s).
- Navegación con Fragments + Navigation Component.
- Sesión persistida en SharedPreferences (SessionManager).
- Capa de red con Retrofit (data/network).

## Backend

Ya hay un backend funcional para este punto (Login + OTP), en la rama `db` del
repo (Node + Express + SQLite). Instrucciones para correrlo en el README de esa rama.

Los endpoints en `ApiService.java`:
- `auth/login`
- `auth/otp/request`
- `auth/otp/verify`
- `auth/otp/resend`

ya están conectados a ese backend de prueba. A medida que se sumen los demás puntos
del TP (Home, publicaciones, etc.), hay que ir agregando los endpoints correspondientes
en el backend (rama `db`) y actualizando `ApiService.java` acá.

Nota: `BASE_URL` en `RetrofitClient.java` apunta a `10.0.2.2`, que solo funciona
desde el emulador. Para probar en un celular real hay que cambiarla por la IP de
la PC en la red, o por la URL final si el backend se despliega en algún servidor.
