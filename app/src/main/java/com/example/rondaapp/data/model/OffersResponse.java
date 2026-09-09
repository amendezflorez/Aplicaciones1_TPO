package com.example.rondaapp.data.model;

import java.util.List;

public class OffersResponse {

    private List<Offer> data;
    private int total;

    public List<Offer> getData() { return data; }
    public int getTotal() { return total; }
}
