package com.example.healthflow.dao;

import com.example.healthflow.model.User;
import com.example.healthflow.model.dto.UserDTO;

import java.sql.Connection;
import java.sql.SQLException;

public interface UserDAO {

    /** ⛔️ قديم: بدون gender – خليه إذا في أماكن قديمة تستخدمه */
    long insert(String nationalId, String fullName, String email, String passwordHash,
                String phone, String role, Connection c) throws SQLException;

    /** ✅ جديد: إدراج مع gender */
    long insertWithGender(String nationalId, String fullName, String email, String passwordHash,
                          String phone, String role, String gender, Connection c) throws SQLException;

    /** ⛔️ قديم: بدون gender */
    void update(long id, String fullName, String phone, String nationalId, Connection c) throws SQLException;

    /** ✅ جديد: تحديث مع gender */
    void updateWithGender(long id, String fullName, String phone, String nationalId, String gender, Connection c) throws SQLException;

    int delete(long id, Connection c) throws SQLException;

    UserDTO findById(long id) throws SQLException;

    User findByEmail(String email) throws SQLException;

    // مختصر لإدخال User خارج الترانزاكشن
    User insert(User u);

    void updateLastLogin(Long id);
}

