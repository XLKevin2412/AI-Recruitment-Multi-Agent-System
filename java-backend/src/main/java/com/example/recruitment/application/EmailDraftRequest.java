package com.example.recruitment.application;

import jakarta.validation.constraints.NotBlank;

public class EmailDraftRequest {

    @NotBlank
    private String emailType;

    public String getEmailType() {
        return emailType;
    }

    public void setEmailType(String emailType) {
        this.emailType = emailType;
    }
}
