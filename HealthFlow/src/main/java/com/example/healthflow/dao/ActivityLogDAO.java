package com.example.healthflow.dao;

import com.example.healthflow.model.ActivityLog;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {

    public ActivityLog log(Connection c, Long userId, String action, String entityType, Long entityId, String metadataJson) throws SQLException {
        final String sql = """
            INSERT INTO activity_logs (user_id, action, entity_type, entity_id, metadata)
            VALUES (?, ?, ?, ?, CAST(? AS JSONB))
            RETURNING *
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (userId == null) ps.setNull(1, Types.BIGINT); else ps.setLong(1, userId);
            ps.setString(2, action);
            ps.setString(3, entityType);
            if (entityId == null) ps.setNull(4, Types.BIGINT); else ps.setLong(4, entityId);
            ps.setString(5, metadataJson);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ActivityLog al = new ActivityLog();
                    al.setId(rs.getLong("id"));
                    al.setUserId((Long) rs.getObject("user_id"));
                    al.setAction(rs.getString("action"));
                    al.setEntityType(rs.getString("entity_type"));
                    al.setEntityId((Long) rs.getObject("entity_id"));
                    al.setMetadataJson(rs.getString("metadata"));
                    al.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    return al;
                }
            }
        }
        throw new SQLException("Failed to write activity log");
    }

    public List<ActivityLog> recentForUser(Connection c, Long userId, int limit) throws SQLException {
        final String sql = "SELECT * FROM activity_logs WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<ActivityLog> out = new ArrayList<>();
                while (rs.next()) {
                    ActivityLog al = new ActivityLog();
                    al.setId(rs.getLong("id"));
                    al.setUserId((Long) rs.getObject("user_id"));
                    al.setAction(rs.getString("action"));
                    al.setEntityType(rs.getString("entity_type"));
                    al.setEntityId((Long) rs.getObject("entity_id"));
                    al.setMetadataJson(rs.getString("metadata"));
                    al.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
                    out.add(al);
                }
                return out;
            }
        }
    }
}