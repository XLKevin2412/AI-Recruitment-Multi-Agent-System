package com.example.recruitment.template;

import java.util.List;

public record JobTemplate(
        String code,
        String title,
        String department,
        List<String> requiredSkills,
        String description,
        String requirements) {
}
