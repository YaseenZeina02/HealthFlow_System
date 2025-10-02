package com.example.healthflow.service;

import com.example.healthflow.dao.PatientDAO;
import com.example.healthflow.dao.UserDAO;
import com.example.healthflow.dao.PatientJdbcDAO;
import com.example.healthflow.dao.UserJdbcDAO;
import com.example.healthflow.db.Database;
import com.example.healthflow.model.dto.PatientView;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PatientService {

    private final UserDAO userDAO = new UserJdbcDAO();
    private final PatientDAO patientDAO = new PatientJdbcDAO();

    private static final String DEFAULT_PATIENT_PASSWORD = "patient@123";

    /** إضافة مريض جديد */
    public PatientView createPatient(String fullName, String nid, String phone,
                                     LocalDate dob, String gender, String history) throws SQLException {
        String hash = org.mindrot.jbcrypt.BCrypt.hashpw(
                DEFAULT_PATIENT_PASSWORD, org.mindrot.jbcrypt.BCrypt.gensalt()
        );

        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                long userId = userDAO.insert(nid, fullName, null, hash, phone, com.example.healthflow.model.Role.PATIENT.name(), c);
                long patientId = patientDAO.insert(userId, dob, gender, history, c);
                c.commit();
                return new PatientView(patientId, userId, fullName, nid, phone, dob, gender, history);
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** تحديث بيانات مريض */
    public void updatePatient(long userId, long patientId, String fullName, String nid,
                              String phone, LocalDate dob, String gender, String history) throws SQLException {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                userDAO.update(userId, fullName, phone, nid, c);
                patientDAO.update(patientId, dob, gender, history, c);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** حذف مريض */
    public void deletePatientByUserId(long userId) throws SQLException {
        try (Connection c = Database.get()) {
            userDAO.delete(userId, c); // لو FK ON DELETE CASCADE موجود هيحذف المريض كمان
        }
    }

    /** قراءة كل المرضى */
    public List<PatientView> listPatients() throws SQLException {
        return patientDAO.findAll();
    }
}
