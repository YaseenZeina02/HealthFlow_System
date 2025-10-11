package com.example.healthflow.service;

import com.example.healthflow.db.Database;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Minimal Appointment CRUD service that matches our DB.sql schema:
 *  - doctor_id BIGINT
 *  - patient_id BIGINT
 *  - appointment_date TIMESTAMPTZ
 *  - duration_minutes INT
 *  - status appt_status
 */
public class AppointmentService {

    public long create(long doctorId, long patientId, LocalDate date, LocalTime time,
                       int durationMinutes, String status) throws SQLException {
        LocalDateTime start = LocalDateTime.of(date, time);
        final String sql = """
                INSERT INTO appointments(doctor_id, patient_id, appointment_date, duration_minutes, status)
                VALUES (?,?,?,?,?)
                RETURNING id
                """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            ps.setLong(2, patientId);
            ps.setTimestamp(3, Timestamp.valueOf(start));
            ps.setInt(4, durationMinutes);
            ps.setString(5, status == null ? "SCHEDULED" : status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
                throw new SQLException("Insert failed, no id returned.");
            }
        }
    }

    public void updateTimeAndDoctor(long id, long doctorId, LocalDate date, LocalTime time,
                                    int durationMinutes) throws SQLException {
        LocalDateTime start = LocalDateTime.of(date, time);
        final String sql = """
                UPDATE appointments
                   SET doctor_id = ?, appointment_date = ?, duration_minutes = ?
                 WHERE id = ?
                """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setInt(3, durationMinutes);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    public void updateStatus(long id, String status) throws SQLException {
        final String sql = "UPDATE appointments SET status = ? WHERE id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        final String sql = "DELETE FROM appointments WHERE id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<Appt> findById(long id) throws SQLException {
        final String sql = """
                SELECT id, doctor_id, patient_id, appointment_date, duration_minutes, status::text AS status
                FROM appointments WHERE id = ?
                """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Appt(
                            rs.getLong("id"),
                            rs.getLong("doctor_id"),
                            rs.getLong("patient_id"),
                            rs.getTimestamp("appointment_date").toLocalDateTime(),
                            rs.getInt("duration_minutes"),
                            rs.getString("status")
                    ));
                }
                return Optional.empty();
            }
        }
    }

    /** Simple DTO for quick transfers. */
    public record Appt(long id, long doctorId, long patientId, LocalDateTime start,
                       int durationMinutes, String status) {
        public LocalDate date() { return start.toLocalDate(); }
        public LocalTime time() { return start.toLocalTime(); }
        public Duration duration() { return Duration.ofMinutes(durationMinutes); }
    }
}
