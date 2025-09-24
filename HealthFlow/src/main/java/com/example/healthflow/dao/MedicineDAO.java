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
    public Medicine insert(Connection c, String name, String description) throws SQLException {
        final String sql = """
            INSERT INTO medicines (name, description) VALUES (?, ?)
            RETURNING *
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Medicine m = new Medicine();
                    m.setId(rs.getLong("id"));
                    m.setName(rs.getString("name"));
                    m.setDescription(rs.getString("description"));
                    m.setAvailableQuantity(rs.getInt("available_quantity"));
                    m.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    m.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return m;
                }
            }
        }
        throw new SQLException("Failed to create medicine");
    }

    public Medicine findById(Connection c, Long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM medicines WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Medicine m = new Medicine();
                m.setId(rs.getLong("id"));
                m.setName(rs.getString("name"));
                m.setDescription(rs.getString("description"));
                m.setAvailableQuantity(rs.getInt("available_quantity"));
                m.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                m.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                return m;
            }
        }
    }
}
