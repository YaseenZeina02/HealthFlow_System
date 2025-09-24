package com.example.healthflow.dao;

import com.example.healthflow.model.Doctor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDAO {

    public Doctor insert(Connection c, Long userId, String specialty, String bio) throws SQLException {
        final String sql = """
            INSERT INTO doctors (user_id, specialty, bio)
            VALUES (?, ?, ?)
            RETURNING id, user_id, specialty, bio, updated_at
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, specialty);
            ps.setString(3, bio);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Doctor d = new Doctor();
                    d.setId(rs.getLong("id"));
                    d.setUserId(rs.getLong("user_id"));
                    d.setSpecialty(rs.getString("specialty"));
                    d.setBio(rs.getString("bio"));
                    d.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
                    return d;
                }
            }
        }
        throw new SQLException("Failed to create doctor");
    }

    public Doctor findById(Connection c, Long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM doctors WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Doctor d = new Doctor();
                d.setId(rs.getLong("id"));
                d.setUserId(rs.getLong("user_id"));
                d.setSpecialty(rs.getString("specialty"));
                d.setBio(rs.getString("bio"));
                d.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
                return d;
            }
        }
    }

    public List<Doctor> list(Connection c, int limit, int offset) throws SQLException {
        final String sql = "SELECT * FROM doctors ORDER BY id LIMIT ? OFFSET ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            ps.setInt(2, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                List<Doctor> out = new ArrayList<>();
                while (rs.next()) {
                    Doctor d = new Doctor();
                    d.setId(rs.getLong("id"));
                    d.setUserId(rs.getLong("user_id"));
                    d.setSpecialty(rs.getString("specialty"));
                    d.setBio(rs.getString("bio"));
                    d.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
                    out.add(d);
                }
                return out;
            }
        }
    }

    public boolean updateProfile(Connection c, Long id, String specialty, String bio) throws SQLException {
        final String sql = "UPDATE doctors SET specialty = ?, bio = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, specialty);
            ps.setString(2, bio);
            ps.setLong(3, id);
            return ps.executeUpdate() == 1;
        }
    }
}