package com.example.healthflow.model;


public enum Role {
    ADMIN, DOCTOR, RECEPTIONIST, PHARMACIST, PATIENT;

    public static Role fromString(String v) {
        if (v == null) throw new IllegalArgumentException("role is null");
        return Role.valueOf(v.trim().toUpperCase());
    }
}