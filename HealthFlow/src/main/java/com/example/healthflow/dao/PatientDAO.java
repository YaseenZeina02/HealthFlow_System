package com.example.healthflow.dao;

import com.example.healthflow.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    /** إنشاء user(role=PATIENT) + patient في ترانزاكشن واحدة */
    public Patient createWithUser(Connection c,
                                  String fullName,
                                  String email,        // nullable
                                  String passwordHash, // منشأ خارجياً (BCrypt)
                                  String nationalId,   // nullable
                                  String phone,        // nullable (لكن DB يطلب email أو phone على الأقل)
                                  LocalDate dob,
                                  Gender gender,
                                  String medicalHistory) throws SQLException {

        Long userId = null;
        // 1) users
        final String insUser = """
            INSERT INTO users (national_id, full_name, email, password_hash, role, phone)
            VALUES (?,?,?,?, ?::role_type, ?)
            RETURNING id
            """;
        try (PreparedStatement ps = c.prepareStatement(insUser)) {
            ps.setString(1, nationalId);
            ps.setString(2, fullName);
            ps.setString(3, (email == null || email.isBlank()) ? null : email);
            ps.setString(4, passwordHash);
            ps.setString(5, Role.PATIENT.name());
            ps.setString(6, (phone == null || phone.isBlank()) ? null : phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) userId = rs.getLong("id");
            }
        }

        // 2) patients
        final String insPatient = """
            INSERT INTO patients (user_id, date_of_birth, gender, medical_history)
            VALUES (?, ?, ?::gender_type, ?)
            RETURNING id, user_id, date_of_birth, gender, medical_history, updated_at
            """;
        try (PreparedStatement ps = c.prepareStatement(insPatient)) {
            ps.setLong(1, userId);
            ps.setObject(2, dob);
            ps.setString(3, gender.name());
            ps.setString(4, medicalHistory);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Patient p = new Patient();
                    p.setId(rs.getLong("id"));
                    p.setUserId(rs.getLong("user_id"));
                    p.setDateOfBirth(rs.getObject("date_of_birth", LocalDate.class));
                    p.setGender(Gender.fromString(rs.getString("gender")));
                    p.setMedicalHistory(rs.getString("medical_history"));
                    p.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
                    return p;
                }
            }
        }
        throw new SQLException("Failed to create patient");
    }

    public Patient findById(Connection c, Long id) throws SQLException {
        final String sql = "SELECT * FROM patients WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Patient p = new Patient();
                p.setId(rs.getLong("id"));
                p.setUserId(rs.getLong("user_id"));
                p.setDateOfBirth(rs.getObject("date_of_birth", LocalDate.class));
                p.setGender(Gender.fromString(rs.getString("gender")));
                p.setMedicalHistory(rs.getString("medical_history"));
                p.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
                return p;
            }
        }
    }

    public List<Patient> list(Connection c, int limit, int offset) throws SQLException {
        final String sql = "SELECT * FROM patients ORDER BY id LIMIT ? OFFSET ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            ps.setInt(2, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                List<Patient> out = new ArrayList<>();
                while (rs.next()) {
                    Patient p = new Patient();
                    p.setId(rs.getLong("id"));
                    p.setUserId(rs.getLong("user_id"));
                    p.setDateOfBirth(rs.getObject("date_of_birth", LocalDate.class));
                    p.setGender(Gender.fromString(rs.getString("gender")));
                    p.setMedicalHistory(rs.getString("medical_history"));
                    p.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
                    out.add(p);
                }
                return out;
            }
        }
    }

    public boolean updateMedicalHistory(Connection c, Long patientId, String history) throws SQLException {
        final String sql = "UPDATE patients SET medical_history = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, history);
            ps.setLong(2, patientId);
            return ps.executeUpdate() == 1;
        }
    }
}