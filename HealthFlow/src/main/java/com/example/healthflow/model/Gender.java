package com.example.healthflow.model;

public enum Gender {
    MALE, FEMALE;

    public static Gender fromString(String v) {
        if (v == null) throw new IllegalArgumentException("gender is null");
        return Gender.valueOf(v.trim().toUpperCase());
    }
}