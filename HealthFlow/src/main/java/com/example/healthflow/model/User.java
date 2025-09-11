package com.example.healthflow.model;
import java.time.OffsetDateTime;

public class User {
    private Long id;
    private String nationalId;   // 9 digits, nullable
    private String fullName;
    private String email;
    private String passwordHash;
    private String role;         // ADMIN, DOCTOR, RECEPTIONIST, PHARMACIST, PATIENT
    private String phone;
    private boolean isActive;
    private OffsetDateTime lastLogin;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public User() {}

    public User(Long id, String nationalId, String fullName, String email,
                String passwordHash, String role, String phone,
                boolean isActive, OffsetDateTime lastLogin,
                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.nationalId = nationalId;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.phone = phone;
        this.isActive = isActive;
        this.lastLogin = lastLogin;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }

    /**
     * Sets the user role, validating that it's one of the allowed values.
     * @param role The role to set (ADMIN, DOCTOR, RECEPTIONIST, PHARMACIST, PATIENT)
     * @throws IllegalArgumentException if the role is not valid
     */
    public void setRole(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        String upperRole = role.toUpperCase();
        if (!upperRole.equals("ADMIN") && 
            !upperRole.equals("DOCTOR") && 
            !upperRole.equals("RECEPTIONIST") && 
            !upperRole.equals("PHARMACIST") && 
            !upperRole.equals("PATIENT")) {
            throw new IllegalArgumentException("Invalid role: " + role + 
                ". Must be one of: ADMIN, DOCTOR, RECEPTIONIST, PHARMACIST, PATIENT");
        }

        this.role = upperRole; // Store in uppercase for consistency
    }

    public String getPhone() { return phone; }

    /**
     * Sets the phone number, validating that it's a valid format.
     * Accepts formats like: +1234567890, 123-456-7890, (123) 456-7890, etc.
     * @param phone The phone number to set
     * @throws IllegalArgumentException if the phone number is not valid
     */
    public void setPhone(String phone) {
        if (phone != null && !phone.matches("^(\\+\\d{1,3}( )?)?((\\(\\d{1,3}\\))|\\d{1,3})[- .]?\\d{3,4}[- .]?\\d{4}$")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
        this.phone = phone;
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public OffsetDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(OffsetDateTime lastLogin) { this.lastLogin = lastLogin; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
