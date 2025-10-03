package com.example.healthflow.dao;

import com.example.healthflow.db.Database;
import com.example.healthflow.model.dto.DoctorApptRow;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentJdbcDAO {

    public List<DoctorApptRow> listTodayByDoctor(long doctorId) throws SQLException {
        final String sql = """
            SELECT a.id as appointment_id,
                   p.id as patient_id,
                   u.id as user_id,
                   u.full_name as patient_name,
                   u.national_id,
                   a.appointment_date as appt_at,
                   a.duration_minutes,
                   a.status,
                   p.medical_history
            FROM appointments a
            JOIN patients p ON p.id = a.patient_id
            JOIN users u ON u.id = p.user_id
            WHERE a.doctor_id = ?
              AND a.appointment_date::date = CURRENT_DATE
            ORDER BY a.appointment_date
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                List<DoctorApptRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new DoctorApptRow(
                            rs.getLong("appointment_id"),
                            rs.getLong("patient_id"),
                            rs.getLong("user_id"),
                            rs.getString("patient_name"),
                            rs.getString("national_id"),
                            rs.getObject("appt_at", OffsetDateTime.class),
                            rs.getInt("duration_minutes"),
                            rs.getString("status"),
                            rs.getString("medical_history")
                    ));
                }
                return out;
            }
        }
    }

    public int markCompleted(long appointmentId) throws SQLException {
        final String sql = "UPDATE appointments SET status='COMPLETED', updated_at=NOW() WHERE id=? AND status <> 'COMPLETED'";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, appointmentId);
            return ps.executeUpdate();
        }
    }

    public Counts todayCounts(long doctorId) throws SQLException {
        final String sql = """
            SELECT
              COUNT(*) FILTER (WHERE appointment_date::date = CURRENT_DATE) AS total_today,
              COUNT(*) FILTER (WHERE appointment_date::date = CURRENT_DATE AND status='COMPLETED') AS completed_today,
              COUNT(*) FILTER (WHERE appointment_date::date = CURRENT_DATE AND status IN ('PENDING','SCHEDULED')) AS remaining_today
            FROM appointments
            WHERE doctor_id = ?
        """;
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Counts(
                        rs.getInt("total_today"),
                        rs.getInt("completed_today"),
                        rs.getInt("remaining_today")
                );
            }
        }
    }

    public record Counts(int totalToday, int completedToday, int remainingToday) {}
}