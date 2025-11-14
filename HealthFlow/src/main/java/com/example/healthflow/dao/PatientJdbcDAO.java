package com.example.healthflow.dao;

import com.example.healthflow.db.Database;
import com.example.healthflow.model.PatientRow;
//import com.example.healthflow.model.dto.PatientView;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PatientJdbcDAO implements PatientDAO {

    @Override
    public long insert(long userId, LocalDate dob, String history, Connection c) throws SQLException {
        String sql = """
            INSERT INTO patients (user_id, date_of_birth, medical_history)
            VALUES (?,?,?) RETURNING id
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setObject(2, dob);
            ps.setString(3, history);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong("id"); }
        }
    }

    @Override
    public void update(long patientId, LocalDate dob, String history, Connection c) throws SQLException {
        String sql = """
            UPDATE patients
               SET date_of_birth=?, medical_history=?, updated_at=NOW()
             WHERE id=?
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, dob);
            ps.setString(2, history);
            ps.setLong(3, patientId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<PatientRow> findAll() throws SQLException {
        String sql = """
            SELECT p.id AS patient_id,
                   u.id AS user_id,
                   u.full_name,
                   u.national_id,
                   u.phone,
                   p.date_of_birth,
                   u.gender          AS gender,
                   p.medical_history
              FROM patients p
              JOIN users u ON u.id = p.user_id
             ORDER BY p.id DESC
             LIMIT 10
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<PatientRow> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new PatientRow(
                        rs.getLong("patient_id"),
                        rs.getLong("user_id"),
                        rs.getString("full_name"),
                        rs.getString("national_id"),
                        rs.getString("phone"),
                        rs.getObject("date_of_birth", java.time.LocalDate.class),
                        rs.getString("gender"),
                        rs.getString("medical_history")
                ));
            }
            return list;
        }
    }

    public List<PatientRow> searchByQuery(String query, int limit) throws SQLException {
        String q = (query == null ? "" : query.trim());
        if (q.length() < 1) return List.of();

        String sql = """
        SELECT p.id AS patient_id,
               u.id AS user_id,
               u.full_name,
               u.national_id,
               u.phone,
               p.date_of_birth,
               u.gender,         -- gender now from users
               p.medical_history
          FROM patients p
          JOIN users u ON u.id = p.user_id
         WHERE (u.full_name ILIKE ? OR u.national_id ILIKE ? OR u.phone ILIKE ?)
         ORDER BY u.full_name ASC
         LIMIT ?
    """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + q + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setInt(4, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<PatientRow> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new PatientRow(
                            rs.getLong("patient_id"),
                            rs.getLong("user_id"),
                            rs.getString("full_name"),
                            rs.getString("national_id"),
                            rs.getString("phone"),
                            rs.getObject("date_of_birth", java.time.LocalDate.class),
                            rs.getString("gender"),
                            rs.getString("medical_history")
                    ));
                }
                return list;
            }
        }
    }


    @Override
    public List<PatientRow> searchPatientsByKeyword(String keyword, int limit) throws SQLException {
        String q = (keyword == null) ? "" : keyword.trim().toLowerCase();
        if (q.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        String sql = """
        SELECT p.id AS patient_id,
               u.id AS user_id,
               u.full_name,
               u.national_id,
               u.phone,
               p.date_of_birth,
               u.gender AS gender,
               p.medical_history
        FROM patients p
        JOIN users u ON u.id = p.user_id
        WHERE
              LOWER(u.full_name) LIKE ?
           OR LOWER(u.national_id) LIKE ?
           OR LOWER(COALESCE(u.phone, '')) LIKE ?
           OR LOWER(COALESCE(p.medical_history, '')) LIKE ?
        ORDER BY p.id DESC
        LIMIT ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String like = "%" + q + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setInt(5, limit);

            try (ResultSet rs = ps.executeQuery()) {
                List<PatientRow> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new PatientRow(
                            rs.getLong("patient_id"),
                            rs.getLong("user_id"),
                            rs.getString("full_name"),
                            rs.getString("national_id"),
                            rs.getString("phone"),
                            rs.getObject("date_of_birth", java.time.LocalDate.class),
                            rs.getString("gender"),
                            rs.getString("medical_history")
                    ));
                }
                return list;
            }
        }
    }
}
