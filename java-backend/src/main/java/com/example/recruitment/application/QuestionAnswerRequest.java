package com.example.recruitment.application;

import jakarta.validation.constraints.NotBlank;

public class QuestionAnswerRequest {

    @NotBlank
    private String question;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
