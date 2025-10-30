package com.example.healthflow.dao;

import com.example.healthflow.model.Medicine;
import com.example.healthflow.model.MedicineBatch;
import com.example.healthflow.model.InventoryTransaction;

import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class MedicineDAO {
    public com.example.healthflow.core.packaging.MedicinePackaging getPackaging(Connection c, long medicineId) throws SQLException {
        String sql = """
            SELECT base_unit::text AS base_unit,
                   tablets_per_blister, blisters_per_box,
                   ml_per_bottle, grams_per_tube, split_allowed
            FROM medicines WHERE id = ?
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                var p = new com.example.healthflow.core.packaging.MedicinePackaging();
                p.baseUnit = rs.getString("base_unit") == null ? null
                        : com.example.healthflow.core.packaging.MedUnit.valueOf(rs.getString("base_unit"));
                p.tabletsPerBlister = (Integer) rs.getObject("tablets_per_blister");
                p.blistersPerBox    = (Integer) rs.getObject("blisters_per_box");
                p.mlPerBottle       = (Integer) rs.getObject("ml_per_bottle");
                p.gramsPerTube      = (Integer) rs.getObject("grams_per_tube");
                p.splitAllowed      = (Boolean) rs.getObject("split_allowed");
                return p;
            }
        }
    }
}