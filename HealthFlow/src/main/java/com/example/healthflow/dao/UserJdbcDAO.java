package com.example.healthflow.dao;

import com.example.healthflow.db.Database;
import com.example.healthflow.model.Role;
import com.example.healthflow.model.User;
import com.example.healthflow.model.dto.UserDTO;

import java.sql.*;

public class UserJdbcDAO implements UserDAO {

    @Override
    public long insert(String nid, String fullName, String email, String hash, String phone, String role, Connection c) throws SQLException {
        String sql = """
            INSERT INTO users (national_id, full_name, email, password_hash, role, phone)
            VALUES (?,?,?,?,?::role_type,?) RETURNING id
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nid);
            ps.setString(2, fullName);
            ps.setString(3, email);
            ps.setString(4, hash);
            ps.setString(5, role);
            ps.setString(6, phone);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("id");
            }
        }
    }

    @Override
    public void update(long id, String fullName, String phone, String nid, Connection c) throws SQLException {
        String sql = "UPDATE users SET full_name=?, phone=?, national_id=?, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setString(3, nid);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    @Override
    public int delete(long id, Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    @Override
    public UserDTO findById(long id) throws SQLException {
        String sql = """
            SELECT id, national_id, full_name, email, phone, role, is_active, created_at, updated_at
            FROM users WHERE id=?
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new UserDTO(
                        rs.getLong("id"),
                        rs.getString("national_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("role"),
                        rs.getBoolean("is_active"),
                        rs.getObject("created_at", java.time.OffsetDateTime.class),
                        rs.getObject("updated_at", java.time.OffsetDateTime.class)
                );
            }
        }
    }

    @Override
    public User findByEmail(String email) throws SQLException {
        String sql = """
            SELECT id, national_id, full_name, email, password_hash, role, phone, is_active, created_at, updated_at
            FROM users WHERE email=?
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                User u = new User();
                u.setId(rs.getLong("id"));
                u.setNationalId(rs.getString("national_id"));
                u.setFullName(rs.getString("full_name"));
                u.setEmail(rs.getString("email"));
                u.setPasswordHash(rs.getString("password_hash"));
                u.setRole(Role.valueOf(rs.getString("role")));
                u.setPhone(rs.getString("phone"));
                u.setActive(rs.getBoolean("is_active"));
                u.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
                u.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
                return u;
            }
        }
    }

//    @Override
//    public User findByEmail(String email) throws SQLException {
//        String sql = """
//        SELECT id, national_id, full_name, email, password_hash, role, phone, is_active, created_at, updated_at
//        FROM users WHERE email=?
//    """;
//        try (Connection c = Database.get();
//             PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setString(1, email);
//            try (ResultSet rs = ps.executeQuery()) {
//                if (!rs.next()) return null;
//                User u = new User();
//                u.setId(rs.getLong("id"));
//                u.setNationalId(rs.getString("national_id"));
//                u.setFullName(rs.getString("full_name"));
//                u.setEmail(rs.getString("email"));
//                u.setPasswordHash(rs.getString("password_hash"));
//                u.setRole(Role.valueOf(rs.getString("role")));
//                u.setPhone(rs.getString("phone"));
//                u.setActive(rs.getBoolean("is_active"));
//                u.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
//                u.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
//                return u;
//            }
//        } catch (RuntimeException re) {
//            if (re instanceof Database.DbUnavailableException) {
//                // لفّها كـ SQLException مفهومة للطبقات الأعلى
//                throw new SQLException("DB unavailable: " + ((Database.DbUnavailableException) re).status, re);
//            }
//            throw re;
//        }
//    }

    @Override
    public User insert(User u) {
        try (Connection c = Database.get()) {
            long id = insert(u.getNationalId(), u.getFullName(), u.getEmail(),
                    u.getPasswordHash(), u.getPhone(), u.getRole().name(), c);
            u.setId(id);
            return u;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateLastLogin(Long id) {
        String sql = "UPDATE users SET last_login = NOW() WHERE id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
