package com.example.healthflow.model;

public enum ItemStatus {
    PENDING,
    APPROVED,
    COMPLETED,
    CANCELLED;

    public static ItemStatus fromString(String v) {
        if (v == null) throw new IllegalArgumentException("status is null");
        return ItemStatus.valueOf(v.trim().toUpperCase());
    }
}