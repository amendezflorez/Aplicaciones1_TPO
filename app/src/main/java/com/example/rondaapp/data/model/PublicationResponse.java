package com.example.rondaapp.data.model;

import java.util.List;
public class PublicationResponse {
    private List<Publication> data;
    private int page;
    private int limit;

    public List<Publication> getData() { return data; }
    public int getPage() { return page; }
    public int getLimit() { return limit; }
}
