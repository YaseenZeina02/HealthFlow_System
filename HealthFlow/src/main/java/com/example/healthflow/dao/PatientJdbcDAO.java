package com.example.healthflow.dao;

import com.example.healthflow.db.Database;
import com.example.healthflow.model.dto.PatientView;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PatientJdbcDAO implements PatientDAO {

    @Override
    public long insert(long userId, LocalDate dob, String history, Connection c) throws SQLException {
        String sql = """
            INSERT INTO patients (user_id, date_of_birth, medical_history)
            VALUES (?,?,?) RETURNING id
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setObject(2, dob);
            ps.setString(3, history);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong("id"); }
        }
    }

    @Override
    public void update(long patientId, LocalDate dob, String history, Connection c) throws SQLException {
        String sql = """
            UPDATE patients
               SET date_of_birth=?, medical_history=?, updated_at=NOW()
             WHERE id=?
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, dob);
            ps.setString(2, history);
            ps.setLong(3, patientId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<PatientView> findAll() throws SQLException {
        String sql = """
            SELECT p.id AS patient_id,
                   u.id AS user_id,
                   u.full_name,
                   u.national_id,
                   u.phone,
                   p.date_of_birth,
                   u.gender          AS gender,         -- الآن من users
                   p.medical_history
              FROM patients p
              JOIN users u ON u.id = p.user_id
             ORDER BY p.id
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<PatientView> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new PatientView(
                        rs.getLong("patient_id"),
                        rs.getLong("user_id"),
                        rs.getString("full_name"),
                        rs.getString("national_id"),
                        rs.getString("phone"),
                        rs.getObject("date_of_birth", java.time.LocalDate.class),
                        rs.getString("gender"),
                        rs.getString("medical_history")
                ));
            }
            return list;
        }
    }
}

//package com.example.healthflow.dao;
//
//import com.example.healthflow.db.Database;
//import com.example.healthflow.model.dto.PatientView;
//
//import java.sql.*;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//
//public class PatientJdbcDAO implements PatientDAO {
//
//    @Override
//    public long insert(long userId, LocalDate dob, String gender, String history, Connection c) throws SQLException {
//        String sql = """
//            INSERT INTO patients (user_id, date_of_birth, gender, medical_history)
//            VALUES (?,?,?::gender_type,?) RETURNING id
//        """;
//        try (PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setLong(1, userId);
//            ps.setObject(2, dob);
//            ps.setString(3, gender);
//            ps.setString(4, history);
//            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong("id"); }
//        }
//    }
//
//    @Override
//    public void update(long patientId, LocalDate dob, String gender, String history, Connection c) throws SQLException {
//        String sql = """
//            UPDATE patients
//               SET date_of_birth=?, gender=?::gender_type, medical_history=?, updated_at=NOW()
//             WHERE id=?
//        """;
//        try (PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setObject(1, dob);
//            ps.setString(2, gender);
//            ps.setString(3, history);
//            ps.setLong(4, patientId);
//            ps.executeUpdate();
//        }
//    }
//
//    @Override
//    public List<PatientView> findAll() throws SQLException {
//        String sql = """
//            SELECT p.id AS patient_id, u.id AS user_id, u.full_name, u.national_id, u.phone,
//                   p.date_of_birth, p.gender, p.medical_history
//              FROM patients p
//              JOIN users u ON u.id = p.user_id
//             ORDER BY p.id
//        """;
//        try (Connection c = Database.get();
//             PreparedStatement ps = c.prepareStatement(sql);
//             ResultSet rs = ps.executeQuery()) {
//
//            List<PatientView> list = new ArrayList<>();
//            while (rs.next()) {
//                list.add(new PatientView(
//                        rs.getLong("patient_id"),
//                        rs.getLong("user_id"),
//                        rs.getString("full_name"),
//                        rs.getString("national_id"),
//                        rs.getString("phone"),
//                        rs.getObject("date_of_birth", java.time.LocalDate.class),
//                        rs.getString("gender"),
//                        rs.getString("medical_history")
//                ));
//            }
//            return list;
//        }
//    }
//}