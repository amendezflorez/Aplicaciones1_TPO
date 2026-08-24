package com.ronda.app.data.model;

/**
 * Body usado tanto para solicitar el envío del código OTP
 * como para reenviarlo (mismo payload: el email destino).
 */
public class OtpRequestBody {
    private String email;

    public OtpRequestBody(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
