package com.example.rondaapp.data.model;

import java.util.List;

/**
 * Perfil de un usuario. Sirve tanto para el perfil propio (editable) como
 * para el perfil público de la otra parte antes de operar.
 */
public class UserProfile {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String zone;
    private String createdAt;
    private Reputation reputation;
    private List<Publication> activePublications;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getZone() { return zone; }

    /** Fecha de alta en formato "yyyy-MM-dd HH:mm:ss"; de acá sale la antigüedad. */
    public String getCreatedAt() { return createdAt; }

    public Reputation getReputation() { return reputation; }

    public List<Publication> getActivePublications() { return activePublications; }
}
