package com.example.healthflow.dao;

import com.example.healthflow.model.ItemStatus;
import com.example.healthflow.model.PrescriptionItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionItemDAO {

    public PrescriptionItem addItem(Connection c, Long prescriptionId, Long medicineId,
                                    String medicineName, String dosage, int qty) throws SQLException {
        final String sql = """
            INSERT INTO prescription_items (prescription_id, medicine_id, medicine_name, dosage, quantity)
            VALUES (?, ?, ?, ?, ?)
            RETURNING *
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, prescriptionId);
            if (medicineId == null) ps.setNull(2, Types.BIGINT); else ps.setLong(2, medicineId);

            // allow DB trigger enforce_item_med_integrity() to backfill name from medicine_id
            if (medicineName == null || medicineName.isBlank()) ps.setNull(3, Types.VARCHAR); else ps.setString(3, medicineName);

            ps.setString(4, dosage);
            ps.setInt(5, qty);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        throw new SQLException("Failed to add item");
    }

    public boolean updateDispensed(Connection c, Long itemId, int qtyDispensed, ItemStatus status, Long batchId) throws SQLException {
        final String sql = """
            UPDATE prescription_items
               SET qty_dispensed = ?, status = ?::item_status2, batch_id = ?
             WHERE id = ?
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, qtyDispensed);
            ps.setString(2, status.name());
            if (batchId == null) ps.setNull(3, Types.BIGINT); else ps.setLong(3, batchId);
            ps.setLong(4, itemId);
            return ps.executeUpdate() == 1;
        }
    }

    public List<PrescriptionItem> listByPrescription(Connection c, Long prescriptionId) throws SQLException {
        final String sql = "SELECT * FROM prescription_items WHERE prescription_id = ? ORDER BY id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, prescriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<PrescriptionItem> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    private PrescriptionItem map(ResultSet rs) throws SQLException {
        PrescriptionItem it = new PrescriptionItem();
        it.setId(rs.getLong("id"));
        it.setPrescriptionId(rs.getLong("prescription_id"));
        it.setMedicineId((Long) rs.getObject("medicine_id"));
        it.setMedicineName(rs.getString("medicine_name"));
        it.setDosage(rs.getString("dosage"));
        it.setQuantity(rs.getInt("quantity"));
        it.setQtyDispensed(rs.getInt("qty_dispensed"));
        it.setStatus(ItemStatus.fromString(rs.getString("status")));
        it.setBatchId((Long) rs.getObject("batch_id"));
        return it;
    }
    /** Insert many items (loops with RETURNING to preserve mapping). */
    public List<PrescriptionItem> addItems(Connection c, Long prescriptionId, List<PrescriptionItem> items) throws SQLException {
        List<PrescriptionItem> out = new ArrayList<>();
        if (items == null || items.isEmpty()) return out;
        for (PrescriptionItem it : items) {
            out.add(addItem(c, prescriptionId,
                    it.getMedicineId(),
                    it.getMedicineName(),
                    it.getDosage(),
                    it.getQuantity()));
        }
        return out;
    }

    /** Delete one item by id. */
    public boolean deleteById(Connection c, Long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM prescription_items WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    /** Delete all items for a prescription (useful when doctor edits draft before sending). */
    public int deleteByPrescription(Connection c, Long prescriptionId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM prescription_items WHERE prescription_id = ?")) {
            ps.setLong(1, prescriptionId);
            return ps.executeUpdate();
        }
    }

    /** Items + current stock (read-only helper for doctor view). */
    public List<PrescriptionItem> listByPrescriptionWithStock(Connection c, Long prescriptionId) throws SQLException {
        final String sql = """
                SELECT i.*, m.available_quantity
                FROM prescription_items i
                LEFT JOIN medicines m ON m.id = i.medicine_id
                WHERE i.prescription_id = ?
                ORDER BY i.id
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, prescriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<PrescriptionItem> out = new ArrayList<>();
                while (rs.next()) {
                    PrescriptionItem it = map(rs);
                    // if your model has a setter for stock, set it; otherwise ignore
                    try {
                        var fld = PrescriptionItem.class.getMethod("setStockAvailable", int.class);
                        fld.invoke(it, rs.getInt("available_quantity"));
                    } catch (Throwable ignore) {}
                    out.add(it);
                }
                return out;
            }
        }
    }

    /** Update core editable fields of a prescription item and return the updated row. */
    public PrescriptionItem updateItem(Connection c, long id, Long medicineId,
                                       String medicineName, String dosage, int qty) throws SQLException {
        final String sql = """
        UPDATE prescription_items
        SET medicine_id = ?, medicine_name = ?, dosage = ?, quantity = ?
        WHERE id = ?
        RETURNING *
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (medicineId == null) ps.setNull(1, Types.BIGINT); else ps.setLong(1, medicineId);
            if (medicineName == null || medicineName.isBlank()) ps.setNull(2, Types.VARCHAR); else ps.setString(2, medicineName);
            ps.setString(3, dosage);
            ps.setInt(4, qty);
            ps.setLong(5, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        throw new SQLException("Failed to update prescription item id=" + id);
    }
}