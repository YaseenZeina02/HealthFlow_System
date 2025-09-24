package com.example.healthflow.model;

public enum PrescriptionStatus {
    PENDING, APPROVED, REJECTED, DISPENSED;

    public static PrescriptionStatus fromString(String v) {
        if (v == null) throw new IllegalArgumentException("status is null");
        return PrescriptionStatus.valueOf(v.trim().toUpperCase());
    }
}