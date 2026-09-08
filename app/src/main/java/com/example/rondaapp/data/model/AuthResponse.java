package com.example.rondaapp.data.model;

/**
 * Respuesta del backend al hacer login (con contraseña) o al
 * verificar correctamente el código OTP.
 */
public class AuthResponse {
    private String token;
    private String userId;
    private String email;
    private String name;
    private String zone;

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    /** Zona del usuario, para anclar el filtro de cercania sin pedir el perfil. */
    public String getZone() {
        return zone;
    }
}
