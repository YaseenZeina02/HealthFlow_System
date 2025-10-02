package com.example.healthflow.model.dto;

import com.example.healthflow.model.User;

import java.sql.Connection;
import java.sql.SQLException;

public interface UserDAO {
    long insert(String nationalId, String fullName, String email, String passwordHash,
                String phone, String role, Connection c) throws SQLException;

    void update(long id, String fullName, String phone, String nationalId, Connection c) throws SQLException;

    int delete(long id, Connection c) throws SQLException;

    UserDTO findById(long id) throws SQLException;

    User findByEmail(String email) throws SQLException;

    // ميثود اختياري لإدخال User كامل (لو بدك)
    User insert(User u);
}
