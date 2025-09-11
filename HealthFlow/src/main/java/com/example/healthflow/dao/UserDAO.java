package com.example.healthflow.dao;

import com.example.healthflow.db.Database;
import com.example.healthflow.model.User;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /** 
     * Create a new user row and return the populated object (id + timestamps).
     * @param u The user to insert
     * @return The inserted user with ID and timestamps populated
     * @throws SQLException If a database error occurs
     * @throws IllegalArgumentException If user validation fails
     */
    public User insert(User u) throws SQLException, IllegalArgumentException {
        // Validate user fields before inserting
        validateUserFields(u);

        final String sql = """
            INSERT INTO users (national_id, full_name, email, password, role, phone)
            VALUES (?,?,?,?,?,?)
            RETURNING id, created_at, updated_at
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, u.getNationalId());
            ps.setString(2, u.getFullName());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPasswordHash()); // Maps to 'password' column in DB
            ps.setString(5, u.getRole());
            ps.setString(6, u.getPhone());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    u.setId(rs.getLong("id"));
                    u.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    u.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                }
            }
            return u;
        }
    }

    /** Find a user by primary key. */
//    public User findById(Long id) throws SQLException {
//        final String sql = "SELECT * FROM users WHERE id = ?";
//        try (Connection c = Database.get();
//             PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setLong(1, id);
//            try (ResultSet rs = ps.executeQuery()) {
//                return rs.next() ? map(rs) : null;
//            }
//        }
//    }

    public User findById(Long id) throws SQLException {
        final String sql = "SELECT * FROM users WHERE id = ?";
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Find a user by email (case-insensitive because of CITEXT). */
    public User findByEmail(String email) throws SQLException {
        final String sql = "SELECT * FROM users WHERE email = ?";
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Update last_login to NOW() after successful authentication. */
    public void updateLastLogin(Long userId) throws SQLException {
        final String sql = "UPDATE users SET last_login = NOW() WHERE id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    /** Activate/deactivate an account. Returns true if a row was updated. */
    public boolean setActive(Long userId, boolean active) throws SQLException {
        final String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    /** 
     * Update basic profile fields (safe: does not change email/role/password here).
     * @param userId The ID of the user to update
     * @param fullName The new full name
     * @param phone The new phone number
     * @param nationalId The new national ID
     * @return true if the update was successful
     * @throws SQLException If a database error occurs
     * @throws IllegalArgumentException If validation fails
     */
    public boolean updateBasicInfo(Long userId, String fullName, String phone, String nationalId) 
            throws SQLException, IllegalArgumentException {
        // Create a temporary user to validate the fields
        User tempUser = new User();
        tempUser.setPhone(phone);
        tempUser.setNationalId(nationalId);

        final String sql = """
            UPDATE users
               SET full_name = ?, phone = ?, national_id = ?, updated_at = NOW()
             WHERE id = ?
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setString(3, nationalId);
            ps.setLong(4, userId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Update a user's password. Returns true if successful. */
    public boolean updatePassword(Long userId, String newPasswordHash) throws SQLException {
        final String sql = """
            UPDATE users
               SET password = ?, updated_at = NOW()
             WHERE id = ?
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setLong(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    /** List users by role with simple pagination. */
    public List<User> listByRole(String role, int limit, int offset) throws SQLException {
        final String sql = """
            SELECT * FROM users
             WHERE role = ?
             ORDER BY id
             LIMIT ? OFFSET ?
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, Math.max(1, limit));
            ps.setInt(3, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                List<User> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    /* -------------------- helpers -------------------- */

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setNationalId(rs.getString("national_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password")); // DB column 'password' maps to passwordHash field
        u.setRole(rs.getString("role"));
        u.setPhone(rs.getString("phone"));
        u.setActive(rs.getBoolean("is_active"));
        u.setLastLogin(rs.getObject("last_login", OffsetDateTime.class));
        u.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        u.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        return u;
    }

    /**
     * Validates all user fields to ensure they meet requirements.
     * @param u The user to validate
     * @throws IllegalArgumentException If validation fails
     */
    private void validateUserFields(User u) throws IllegalArgumentException {
        if (u == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // These setter methods will throw IllegalArgumentException if validation fails
        if (u.getNationalId() != null) {
            u.setNationalId(u.getNationalId()); // Trigger validation
        }

        if (u.getEmail() != null) {
            u.setEmail(u.getEmail()); // Trigger validation
        }

        if (u.getRole() != null) {
            u.setRole(u.getRole()); // Trigger validation
        }

        if (u.getPhone() != null) {
            u.setPhone(u.getPhone()); // Trigger validation
        }
    }
}
