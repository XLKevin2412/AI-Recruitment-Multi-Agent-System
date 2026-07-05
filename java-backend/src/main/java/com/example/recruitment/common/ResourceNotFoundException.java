package com.example.recruitment.common;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String id) {
        super(resourceName + " not found: " + id);
    }
}
