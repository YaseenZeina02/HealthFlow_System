// dao/DoctorPatientJdbcDAO.java
package com.example.healthflow.dao;

import com.example.healthflow.db.Database;
import com.example.healthflow.model.dto.DoctorPatientRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorPatientJdbcDAO {

    // كل المرضى الذين لديهم أي موعد مع هذا الدكتور (ممكن تقصرها على اليوم لو بدك)
    public List<DoctorPatientRow> listPatientsOfDoctor(long doctorId) throws SQLException {
        final String sql = """
            SELECT DISTINCT p.id AS patient_id,
                   u.id AS user_id,
                   u.national_id,
                   u.full_name,
                   u.gender,                 -- لو كانت في users
                   p.date_of_birth,
                   p.medical_history
            FROM appointments a
            JOIN patients p ON p.id = a.patient_id
            JOIN users u     ON u.id = p.user_id
            WHERE a.doctor_id = ?
            ORDER BY u.full_name
        """;
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                List<DoctorPatientRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new DoctorPatientRow(
                            rs.getLong("patient_id"),
                            rs.getLong("user_id"),
                            rs.getString("national_id"),
                            rs.getString("full_name"),
                            rs.getString("gender"),
                            rs.getObject("date_of_birth", java.time.LocalDate.class),
                            rs.getString("medical_history")
                    ));
                }
                return out;
            }
        }
    }
}