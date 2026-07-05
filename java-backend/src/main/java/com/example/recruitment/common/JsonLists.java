package com.example.recruitment.common;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonLists {

    private JsonLists() {
    }

    public static List<String> readStringList(ObjectMapper objectMapper, String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of(value);
        }
    }

    public static String writeStringList(ObjectMapper objectMapper, List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception ex) {
            return "[]";
        }
    }
}
