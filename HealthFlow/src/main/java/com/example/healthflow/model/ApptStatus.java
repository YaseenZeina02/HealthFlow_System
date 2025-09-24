package com.example.healthflow.model;

public enum ApptStatus {
    PENDING, SCHEDULED, COMPLETED, CANCELLED, NO_SHOW;

    public static ApptStatus fromString(String v) {
        if (v == null) throw new IllegalArgumentException("status is null");
        return ApptStatus.valueOf(v.trim().toUpperCase());
    }
}