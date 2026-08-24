package com.ronda.app.data.model;

public class OtpVerifyBody {
    private String email;
    private String code;

    public OtpVerifyBody(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }
}
