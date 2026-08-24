**Esta rama cubre el punto 1 del TP: Autenticación y Registro de Usuarios.**

**Implementado:**
- Login con usuario y contraseña.
- Login alternativo por mail + codigo OTP (con reenvio y cooldown de 60s).
- Navegacion con Fragments + Navigation Component.
- Sesion persistida en SharedPreferences (Session Manager).
- Capa de red con Retrofit (data/network)

**Backend:**
Falta conexion al backend, por lo que las pantallas de Login y OTP nunca reciben respuesta exitosa del servidor -> no se puede ir de Login a Home (cualquier cosa agregar boton SKIP).
Los endpoints en ApiService.java: 
- auth/login
- auth/otp/request
- auth/otp/verify
- auth/otp/resend
son una propuesta para simular las llamadas.

Cuando este disponible el back, ajustar: 
- BASE_URL en RetrofitClient.java.
- Paths de los endpoints en ApiService.java.
- Nombres de campos JSON en data/model/ si no coinciden con el real.
