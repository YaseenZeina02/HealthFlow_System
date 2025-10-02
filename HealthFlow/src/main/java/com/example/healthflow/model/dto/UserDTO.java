package com.example.healthflow.model.dto;

import java.time.OffsetDateTime;

public class UserDTO {
    private final long id;
    private final String nationalId;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String role;
    private final boolean isActive;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public UserDTO(long id, String nationalId, String fullName, String email,
                   String phone, String role, boolean isActive,
                   OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.nationalId = nationalId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public String getNationalId() { return nationalId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public boolean isActive() { return isActive; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}