package com.example.rondaapp.data.model;

import java.util.List;

public class PhotosResponse {

    private List<PublicationPhoto> data;
    private int total;

    public List<PublicationPhoto> getData() { return data; }
    public int getTotal() { return total; }
}
