package com.example.healthflow.dao;
import com.example.healthflow.core.packaging.PackagingSupport;
import com.example.healthflow.db.Database;
import com.example.healthflow.model.Medicine;
import com.example.healthflow.model.MedicineBatch;
import com.example.healthflow.model.InventoryTransaction;

import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class MedicineBatchDAO {
    public MedicineBatch insert(Connection c, Long medicineId, String batchNo, LocalDate expiry, int qty) throws SQLException {
        final String sql = """
            INSERT INTO medicine_batches (medicine_id, batch_no, expiry_date, quantity)
            VALUES (?, ?, ?, ?)
            RETURNING *
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, medicineId);
            ps.setString(2, batchNo);
            ps.setObject(3, expiry);
            ps.setInt(4, qty);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    MedicineBatch b = new MedicineBatch();
                    b.setId(rs.getLong("id"));
                    b.setMedicineId(rs.getLong("medicine_id"));
                    b.setBatchNo(rs.getString("batch_no"));
                    b.setExpiryDate(rs.getObject("expiry_date", LocalDate.class));
                    b.setQuantity(rs.getInt("quantity"));
                    b.setReceivedAt(rs.getObject("received_at", OffsetDateTime.class));
                    return b;
                }
            }
        }
        throw new SQLException("Failed to create batch");
    }

    public PackagingSupport.PackagingInfo fetchPackaging(long medId) {
        final String sql = """
        SELECT base_unit, tablets_per_blister, blisters_per_box,
               ml_per_bottle, grams_per_tube, split_allowed
        FROM medicines WHERE id=?
    """;
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setLong(1, medId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new PackagingSupport.PackagingInfo(
                        rs.getString("base_unit"),
                        (Integer)rs.getObject("tablets_per_blister"),
                        (Integer)rs.getObject("blisters_per_box"),
                        (Integer)rs.getObject("ml_per_bottle"),
                        (Integer)rs.getObject("grams_per_tube"),
                        (Boolean)rs.getObject("split_allowed")
                );
            }
        } catch (Exception e) { return null; }
    }
}

