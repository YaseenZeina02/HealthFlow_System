package com.example.healthflow.dao;

import com.example.healthflow.db.Database;
import com.example.healthflow.model.*;

import java.sql.*;
import java.time.OffsetDateTime;
import java.time.LocalDate;
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
            if (appointmentId == null) ps.setNull(1, Types.BIGINT); else ps.setLong(1, appointmentId);
            ps.setLong(2, doctorId);
            ps.setLong(3, patientId);
            ps.setString(4, notes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        throw new SQLException("Failed to create prescription");
    }

    /** Count all prescriptions created on a specific calendar date (by created_at::date). */
    public int countTotalOnDate(Connection c, LocalDate day) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM prescriptions WHERE created_at::date = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, day);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Count PENDING prescriptions (waiting review) created on a given date. */
    public int countPendingOnDate(Connection c, LocalDate day) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM prescriptions WHERE created_at::date = ? AND status = 'PENDING'::prescription_status";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, day);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Count completed (processed by pharmacist) prescriptions created on a given date.
     * Completed = any status other than PENDING (APPROVED/REJECTED/DISPENSED). */
    public int countCompletedOnDate(Connection c, LocalDate day) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM prescriptions WHERE created_at::date = ? AND status <> 'PENDING'::prescription_status";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, day);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // Convenience overloads that manage their own connection
    public int countTotalOnDate(LocalDate day) throws SQLException {
        try (Connection c = Database.get()) { return countTotalOnDate(c, day); }
    }
    public int countPendingOnDate(LocalDate day) throws SQLException {
        try (Connection c = Database.get()) { return countPendingOnDate(c, day); }
    }
    public int countCompletedOnDate(LocalDate day) throws SQLException {
        try (Connection c = Database.get()) { return countCompletedOnDate(c, day); }
    }

    /** Create a prescription (optionally without appointment) and insert items in one transaction. */
    public Prescription createAndItems(Connection c,
                                       Long appointmentId, Long doctorId, Long patientId, String notes,
                                       List<PrescriptionItem> items,
                                       PrescriptionItemDAO itemDao) throws SQLException {
        boolean oldAuto = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            Prescription p = create(c, appointmentId, doctorId, patientId, notes);
            if (items != null && !items.isEmpty()) {
                itemDao.addItems(c, p.getId(), items);
            }
            c.commit();
            return p;
        } catch (SQLException ex) {
            c.rollback();
            throw ex;
        } finally {
            c.setAutoCommit(oldAuto);
        }
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

    /** Prescriptions visible to pharmacy queue by status. */
    public List<Prescription> listByStatus(Connection c, PrescriptionStatus status, int limit) throws SQLException {
        final String sql = "SELECT * FROM prescriptions WHERE status = ?::prescription_status ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<Prescription> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }
    /**
     * Ensure there is a prescription linked to the given appointment. If one exists, return its id;
     * otherwise create a new PENDING prescription and return the new id.
     */
    public long ensurePrescriptionForAppointment(Connection c, long appointmentId, long doctorId, long patientId) throws SQLException {
        // 1) Try to find existing prescription for this appointment
        final String q = "SELECT id FROM prescriptions WHERE appointment_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(q)) {
            ps.setLong(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        // 2) Create a new one (defaults to PENDING status per schema)
        Prescription p = create(c, appointmentId, doctorId, patientId, null);
        return p.getId();
    }

    /**
     * Overload that manages its own connection.
     */
    public long ensurePrescriptionForAppointment(long appointmentId, long doctorId, long patientId) throws SQLException {
        try (Connection c = Database.get()) {
            return ensurePrescriptionForAppointment(c, appointmentId, doctorId, patientId);
        }
    }
}