# Backend Ronda

Backend unificado (Auth + OTP por mail + Publicaciones + Perfil) en Node.js + Express + SQLite.

## Puesta en marcha

Desde la carpeta `backend`:

```bash
npm install
npm start
```

El servidor arranca en `http://localhost:8080`.
Base URL para el emulador de Android: `http://10.0.2.2:8080/api/`.

## Configuración del `.env` (obligatorio para el envío de OTP)

El login por OTP manda el código por mail usando una cuenta de Gmail. Las
credenciales **no se suben al repo** (el `.env` está en el `.gitignore`), así que
cada integrante tiene que crear el suyo.

1. Crear un archivo llamado `.env` dentro de la carpeta `backend` (al lado de `server.js`).
2. Pegar estas tres líneas y completar con datos propios:

```
GMAIL_USER=tumail@gmail.com
GMAIL_PASS=las16letrassinespacios
MAIL_FROM=Ronda <tumail@gmail.com>
```

- `GMAIL_USER`: la dirección de Gmail completa.
- `GMAIL_PASS`: la contraseña de aplicación de 16 caracteres (ver abajo cómo obtenerla), **sin espacios**.
- `MAIL_FROM`: lo que ve el usuario como remitente. Se puede dejar "Ronda".

> Sin comillas y sin espacios alrededor del `=`.

Si el `.env` no está o le falta la contraseña, el servidor igual arranca y el
código OTP se sigue viendo en la consola, pero no se manda el mail.

## Cómo obtener el `GMAIL_PASS` (contraseña de aplicación)

No es la contraseña normal de Gmail: es una "contraseña de aplicación" que Google
genera aparte. Requiere tener la verificación en 2 pasos activada.

1. Activar la verificación en 2 pasos (si no la tenés):
   https://myaccount.google.com/security
2. Generar la contraseña de aplicación:
   https://myaccount.google.com/apppasswords
3. Ponerle un nombre cualquiera (ej. "ronda-backend") y crear.
4. Google muestra 16 letras en 4 grupos (ej. `abcd efgh ijkl mnop`).
   Copiarlas y pegarlas en `GMAIL_PASS` **todas juntas, sin espacios**
   (`abcdefghijklmnop`).

> La contraseña se muestra una sola vez. Si se cierra la ventana sin copiarla,
> hay que borrarla y generar una nueva.

## Notas

- El código OTP vence a los 5 minutos.
- El OTP siempre se imprime en la consola además de mandarse por mail, para
  poder probar sin depender del correo.
- La base SQLite (`*.db`) y `node_modules` están ignorados por git.