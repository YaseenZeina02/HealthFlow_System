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
            ps.setString(3, medicineName);
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
}