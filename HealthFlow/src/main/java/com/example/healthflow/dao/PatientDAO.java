package com.example.healthflow.dao;

import com.example.healthflow.model.dto.PatientView;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface PatientDAO {

    /** ✅ جديد: بدون gender */
    long insert(long userId, LocalDate dob, String history, Connection c) throws SQLException;

    /** ✅ جديد: بدون gender */
    void update(long patientId, LocalDate dob, String history, Connection c) throws SQLException;

    List<PatientView> findAll() throws SQLException;
}


//package com.example.healthflow.dao;
//
//
//import com.example.healthflow.model.dto.PatientView;
//
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.time.LocalDate;
//import java.util.List;
//
//public interface PatientDAO {
//    long insert(long userId, LocalDate dob, String gender, String history, Connection c) throws SQLException;
//    void update(long patientId, LocalDate dob, String gender, String history, Connection c) throws SQLException;
//    List<PatientView> findAll() throws SQLException;
//}
