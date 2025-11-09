package com.example.healthflow.model;

public enum PrescriptionStatus {
    DRAFT,PENDING, APPROVED, REJECTED, DISPENSED;

    public static PrescriptionStatus fromString(String v) {
        if (v == null) throw new IllegalArgumentException("status is null");
        return PrescriptionStatus.valueOf(v.trim().toUpperCase());
    }

    public static PrescriptionStatus fromDb(String s) {
        if (s == null || s.isBlank()) return PENDING;
        try {
            return PrescriptionStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PENDING; // fallback
        }
    }
}