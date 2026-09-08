package com.example.rondaapp.data.model;

/**
 * Reputación de un usuario, construida por el backend a partir de las
 * calificaciones recibidas (punto 2 del TP).
 */
public class Reputation {

    private double average;
    private int totalRatings;
    private int salesCount;
    private int purchasesCount;

    /** Promedio de estrellas, ya redondeado a un decimal por el backend. */
    public double getAverage() { return average; }

    public int getTotalRatings() { return totalRatings; }

    /** Operaciones concretadas como vendedor. */
    public int getSalesCount() { return salesCount; }

    /** Operaciones concretadas como comprador. */
    public int getPurchasesCount() { return purchasesCount; }

    public boolean tieneCalificaciones() { return totalRatings > 0; }
}
