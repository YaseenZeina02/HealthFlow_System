package com.example.healthflow.dao;

import com.example.healthflow.db.Database;
import com.example.healthflow.model.dto.InventoryRow;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class PharmacyQueries {

    public static final class CountSummary {
        public final int total, waiting, completed;
        public CountSummary(int total, int waiting, int completed) {
            this.total = total; this.waiting = waiting; this.completed = completed;
        }
    }

    // --- Appointments ---
    public OffsetDateTime getAppointmentDateById(long apptId) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT appointment_date FROM appointments WHERE id = ?")) {
            ps.setLong(1, apptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
                }
            }
        }
        return null;
    }

    // --- Dashboard counters ---
    public CountSummary getCountsForDate(LocalDate day) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("""
                 SELECT
                     COUNT(*) AS total,
                     COUNT(*) FILTER (WHERE status = 'PENDING') AS waiting,
                     COUNT(*) FILTER (WHERE status IN ('APPROVED','DISPENSED')) AS completed
                 FROM prescriptions
                 WHERE created_at::date = ?
             """)) {
            ps.setDate(1, Date.valueOf(day));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CountSummary(
                            rs.getInt("total"),
                            rs.getInt("waiting"),
                            rs.getInt("completed")
                    );
                }
            }
        }
        return new CountSummary(0,0,0);
    }

    // --- Fingerprints ---
    public Instant getDashboardFingerprint(LocalDate day) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT MAX(COALESCE(p.dispensed_at, p.approved_at, p.decision_at, p.created_at)) " +
                             "FROM prescriptions p " +
                             "WHERE (p.created_at::date = ? OR p.decision_at::date = ? OR p.approved_at::date = ? OR p.dispensed_at::date = ?)")) {
            Date d = Date.valueOf(day);
            ps.setDate(1, d); ps.setDate(2, d); ps.setDate(3, d); ps.setDate(4, d);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    return ts == null ? null : ts.toInstant();
                }
            }
        }
        return null;
    }

    public Instant getPrescriptionFingerprint(long pid) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT MAX(v) FROM ( " +
                             "  SELECT COALESCE(p.dispensed_at, p.approved_at, p.decision_at, p.created_at) AS v " +
                             "  FROM prescriptions p WHERE p.id = ? " +
                             "  UNION ALL " +
                             "  SELECT NOW() - make_interval(secs => (SELECT COUNT(*) FROM prescription_items i WHERE i.prescription_id = ?)) " +
                             "  UNION ALL " +
                             "  SELECT NOW() - make_interval(secs => (SELECT COALESCE(SUM(i.qty_dispensed),0) FROM prescription_items i WHERE i.prescription_id = ?)) " +
                             ") t")) {
            ps.setLong(1, pid);
            ps.setLong(2, pid);
            ps.setLong(3, pid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    return ts == null ? null : ts.toInstant();
                }
            }
        }
        return null;
    }

    public Instant getInventoryFingerprint() throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT GREATEST( " +
                             "  COALESCE((SELECT MAX(created_at) FROM inventory_transactions), 'epoch'), " +
                             "  COALESCE((SELECT MAX(updated_at) FROM medicines), 'epoch') " +
                             ")")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    return ts == null ? null : ts.toInstant();
                }
            }
        }
        return null;
    }

    // --- Users/Pharmacists helpers ---
    public String getUserRole(long userId) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT role FROM users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    public String getUserEmail(long userId) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT email FROM users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    public String getUserFullName(long userId) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT full_name FROM users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    public Long getPharmacistIdByUserId(long userId) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT id FROM pharmacists WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : null; }
        }
    }

    public Long getPharmacistIdByEmail(String email) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT p.id FROM pharmacists p JOIN users u ON u.id = p.user_id WHERE LOWER(u.email)=LOWER(?) LIMIT 1")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : null; }
        }
    }

    public List<InventoryRow> getInventoryOverview(Connection c, ZoneId tz) throws SQLException {
        String sql = """
        SELECT
            m.id,
            COALESCE(m.display_name, m.name) AS display_name,
            m.form,
            (m.base_unit)::text AS base_unit,
            m.available_quantity,
            m.reorder_threshold,
            nb.batch_no,
            nb.qty,
            nb.expiry_date,
            li.received_by_name,
            to_char(li.created_at AT TIME ZONE ?, 'YYYY-MM-DD HH24:MI') AS received_at_text
        FROM medicines m
        LEFT JOIN LATERAL (
            SELECT
                b.batch_no,
                b.expiry_date,
                COALESCE((SELECT SUM(t.qty_change) FROM inventory_transactions t WHERE t.batch_id = b.id), 0)::int AS qty
            FROM medicine_batches b
            WHERE b.medicine_id = m.id
            ORDER BY b.expiry_date ASC
            LIMIT 1
        ) nb ON TRUE
        LEFT JOIN LATERAL (
            SELECT it.created_at,
                   u.full_name AS received_by_name
            FROM inventory_transactions it
            LEFT JOIN pharmacists p ON p.id = it.pharmacist_id
            LEFT JOIN users u       ON u.id = p.user_id
            WHERE it.medicine_id = m.id AND it.qty_change > 0
            ORDER BY it.created_at DESC
            LIMIT 1
        ) li ON TRUE
        ORDER BY LOWER(COALESCE(m.display_name, m.name))
        """;

        List<InventoryRow> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tz == null ? "UTC" : tz.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long medId = rs.getLong("id");
                    String disp = rs.getString("display_name");
                    String form = rs.getString("form");
                    String base = rs.getString("base_unit");
                    int avail   = rs.getInt("available_quantity");
                    String nextBatch = rs.getString("batch_no");
                    int qtyTmp = rs.getInt("qty");
                    Integer nextQty = rs.wasNull() ? null : Integer.valueOf(qtyTmp);
                    java.sql.Date d  = rs.getDate("expiry_date");
                    java.time.LocalDate nextExp = (d == null) ? null : d.toLocalDate();
                    String receivedBy = rs.getString("received_by_name");
                    String receivedAt  = rs.getString("received_at_text");
                    Integer reorderThr = (Integer) rs.getObject("reorder_threshold");

                    out.add(new InventoryRow(
                            medId, disp, form, base, avail,
                            nextBatch, nextQty, nextExp,
                            receivedBy, receivedAt, reorderThr
                    ));
                }
            }
        }
        return out;
    }
    /** Update the per‑medicine low‑stock threshold (reorder point). */
    public void updateReorderThreshold(long medicineId, int threshold) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE medicines SET reorder_threshold = ? WHERE id = ?")) {
            ps.setInt(1, threshold);
            ps.setLong(2, medicineId);
            ps.executeUpdate();
        }
    }
}