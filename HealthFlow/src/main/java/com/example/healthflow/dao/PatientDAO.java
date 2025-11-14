package com.example.healthflow.dao;

import com.example.healthflow.model.PatientRow;
//import com.example.healthflow.model.dto.PatientView;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface PatientDAO {

    /** ✅ جديد: بدون gender */
    long insert(long userId, LocalDate dob, String history, Connection c) throws SQLException;

    /** ✅ جديد: بدون gender */
    void update(long patientId, LocalDate dob, String history, Connection c) throws SQLException;

    List<PatientRow> findAll() throws SQLException;
    List<PatientRow> searchPatientsByKeyword(String keyword, int limit) throws SQLException;

}