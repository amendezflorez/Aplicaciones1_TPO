package com.example.rondaapp.data.model;

/** Body de POST publications/{id}/questions. El autor sale del token. */
public class QuestionBody {

    private final String text;

    public QuestionBody(String text) {
        this.text = text;
    }
}
