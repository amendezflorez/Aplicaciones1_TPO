# Ronda Backend (simulado)

Backend mínimo para probar el flujo de autenticación (login + OTP) de la app Ronda.
No es evaluado en el TP, es solo para poder probar la conexión real desde la app.

## Requisitos

- Node.js instalado (18+)

## Cómo correrlo

```
npm install
npm start
```

Esto levanta el server en `http://localhost:8080`. Desde el emulador de Android, la app le habla
a través de `http://10.0.2.2:8080/api/` (ya está configurado así en `RetrofitClient.java`).

## Cómo probar el flujo OTP

1. En la app, tocá "Ingresar con código por email" y metés cualquier email.
2. Mirá la terminal donde corre este server: ahí se imprime el código de 6 dígitos
   (simula el envío real de un email).
3. Volvés a la app y metés ese código en la pantalla de verificación.

## Datos

Se guardan en un archivo `ronda.db` (SQLite) que se crea solo la primera vez que corrés
el server, en esta misma carpeta. Si querés arrancar de cero, borrá ese archivo y reiniciá el server.

## Endpoints

- `POST /api/auth/login` — body: `{ username, password }`. Si el usuario no existe, se crea al vuelo (simplificado a propósito, no es evaluado).
- `POST /api/auth/otp/request` — body: `{ email }`. Genera un código y lo imprime en la consola del server.
- `POST /api/auth/otp/resend` — igual al anterior, genera un código nuevo.
- `POST /api/auth/otp/verify` — body: `{ email, code }`. Si coincide, crea sesión y devuelve el usuario.
