package com.example.healthflow.dao;
import com.example.healthflow.model.Medicine;
import com.example.healthflow.model.MedicineBatch;
import com.example.healthflow.model.InventoryTransaction;

import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


public class InventoryTransactionDAO {
//    public InventoryTransaction post(Connection c, Long medicineId, Long batchId,
//                                     int qtyChange, String reason, String refType, Long refId) throws SQLException {
//        final String sql = """
//                INSERT INTO inventory_transactions (medicine_id, batch_id, qty_change, reason, ref_type, ref_id)
//                VALUES (?, ?, ?, ?, ?, ?)
//                RETURNING *
//                """;
//        try (PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setLong(1, medicineId);
//            if (batchId == null) ps.setNull(2, Types.BIGINT);
//            else ps.setLong(2, batchId);
//            ps.setInt(3, qtyChange);
//            ps.setString(4, reason);
//            ps.setString(5, refType);
//            if (refId == null) ps.setNull(6, Types.BIGINT);
//            else ps.setLong(6, refId);
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    InventoryTransaction t = new InventoryTransaction();
//                    t.setId(rs.getLong("id"));
//                    t.setMedicineId(rs.getLong("medicine_id"));
//                    t.setBatchId((Long) rs.getObject("batch_id"));
//                    t.setQtyChange(rs.getInt("qty_change"));
//                    t.setReason(rs.getString("reason"));
//                    t.setRefType(rs.getString("ref_type"));
//                    t.setRefId((Long) rs.getObject("ref_id"));
//                    t.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
//                    return t;
//                }
//            }
//        }
//        throw new SQLException("Failed to post inventory transaction");
//    }


}