package com.example.rondaapp.data.model;

public class OtpRequestBody {
    private String email;

    public OtpRequestBody(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
