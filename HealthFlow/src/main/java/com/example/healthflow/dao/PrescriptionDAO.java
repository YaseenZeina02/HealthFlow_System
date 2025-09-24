package com.example.healthflow.dao;

import com.example.healthflow.model.*;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionDAO {

    public Prescription create(Connection c, Long appointmentId, Long doctorId, Long patientId, String notes) throws SQLException {
        final String sql = """
            INSERT INTO prescriptions (appointment_id, doctor_id, patient_id, notes)
            VALUES (?, ?, ?, ?)
            RETURNING *
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, appointmentId);
            ps.setLong(2, doctorId);
            ps.setLong(3, patientId);
            ps.setString(4, notes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        throw new SQLException("Failed to create prescription");
    }

    public Prescription findById(Connection c, Long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM prescriptions WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public boolean approve(Connection c, Long id, Long pharmacistId, String decisionNote) throws SQLException {
        final String sql = """
            UPDATE prescriptions
               SET status = 'APPROVED'::prescription_status,
                   pharmacist_id = ?, decision_note = ?,
                   decision_at = NOW()
             WHERE id = ?
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, pharmacistId);
            ps.setString(2, decisionNote);
            ps.setLong(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean reject(Connection c, Long id, Long pharmacistId, String decisionNote) throws SQLException {
        final String sql = """
            UPDATE prescriptions
               SET status = 'REJECTED'::prescription_status,
                   pharmacist_id = ?, decision_note = ?,
                   decision_at = NOW()
             WHERE id = ?
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, pharmacistId);
            ps.setString(2, decisionNote);
            ps.setLong(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean dispense(Connection c, Long id, Long pharmacistId) throws SQLException {
        final String sql = """
            UPDATE prescriptions
               SET status = 'DISPENSED'::prescription_status,
                   pharmacist_id = ?
             WHERE id = ?
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, pharmacistId);
            ps.setLong(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    private Prescription map(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.setId(rs.getLong("id"));
        p.setAppointmentId(rs.getLong("appointment_id"));
        p.setDoctorId(rs.getLong("doctor_id"));
        p.setPatientId(rs.getLong("patient_id"));
        p.setPharmacistId((Long) rs.getObject("pharmacist_id"));
        p.setStatus(PrescriptionStatus.fromString(rs.getString("status")));
        p.setDecisionAt(rs.getObject("decision_at", OffsetDateTime.class));
        p.setDecisionNote(rs.getString("decision_note"));
        p.setNotes(rs.getString("notes"));
        p.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        p.setApprovedAt(rs.getObject("approved_at", OffsetDateTime.class));
        p.setDispensedAt(rs.getObject("dispensed_at", OffsetDateTime.class));
        p.setApprovedBy((Long) rs.getObject("approved_by"));
        p.setDispensedBy((Long) rs.getObject("dispensed_by"));
        return p;
    }
}