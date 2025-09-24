package com.example.healthflow.dao;

import com.example.healthflow.model.ApptStatus;
import com.example.healthflow.model.Appointment;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {

    public Appointment create(Connection c, Long doctorId, Long patientId,
                              OffsetDateTime at, int durationMin,
                              ApptStatus status, String location, Long createdBy) throws SQLException {
        final String sql = """
            INSERT INTO appointments (doctor_id, patient_id, appointment_date, duration_minutes, status, location, created_by)
            VALUES (?, ?, ?, ?, ?::appt_status, ?, ?)
            RETURNING *
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            ps.setLong(2, patientId);
            ps.setObject(3, at);
            ps.setInt(4, durationMin);
            ps.setString(5, status.name());
            ps.setString(6, location);
            if (createdBy == null) ps.setNull(7, Types.BIGINT); else ps.setLong(7, createdBy);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException ex) {
            // تضارب نطاق الموعد (EXCLUSION) -> 23P01
            if ("23P01".equals(ex.getSQLState())) {
                throw new SQLException("Doctor has an overlapping appointment in this time range", ex);
            }
            throw ex;
        }
        throw new SQLException("Failed to create appointment");
    }

    public Appointment findById(Connection c, Long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM appointments WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Appointment> listForDoctor(Connection c, Long doctorId, int limit, int offset) throws SQLException {
        final String sql = """
            SELECT * FROM appointments
             WHERE doctor_id = ?
             ORDER BY appointment_date
             LIMIT ? OFFSET ?
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            ps.setInt(2, Math.max(1, limit));
            ps.setInt(3, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                List<Appointment> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public boolean updateStatus(Connection c, Long apptId, ApptStatus status) throws SQLException {
        final String sql = "UPDATE appointments SET status = ?::appt_status, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, apptId);
            return ps.executeUpdate() == 1;
        }
    }

    private Appointment map(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setId(rs.getLong("id"));
        a.setDoctorId(rs.getLong("doctor_id"));
        a.setPatientId(rs.getLong("patient_id"));
        a.setAppointmentDate(rs.getObject("appointment_date", OffsetDateTime.class));
        a.setDurationMinutes(rs.getInt("duration_minutes"));
        a.setStatus(ApptStatus.fromString(rs.getString("status")));
        a.setLocation(rs.getString("location"));
        a.setCreatedBy((Long) rs.getObject("created_by"));
        a.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        a.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        return a;
    }
}