package com.example.rondaapp.data.model;

import java.util.List;

public class QuestionsResponse {

    private List<Question> data;
    private int total;

    public List<Question> getData() { return data; }
    public int getTotal() { return total; }
}
