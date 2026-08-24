package com.ronda.app.data.model;

/**
 * Respuesta genérica para endpoints que solo confirman una acción,
 * como el envío o reenvío del código OTP.
 */
public class SimpleResponse {
    private boolean success;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
