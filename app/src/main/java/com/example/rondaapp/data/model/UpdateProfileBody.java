package com.example.rondaapp.data.model;

/**
 * Body de PUT users/{id}: los datos personales editables del perfil.
 */
public class UpdateProfileBody {

    private final String name;
    private final String email;
    private final String phone;
    private final String zone;

    public UpdateProfileBody(String name, String email, String phone, String zone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.zone = zone;
    }
}
