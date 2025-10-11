package com.example.healthflow.service;

import com.example.healthflow.dao.PatientDAO;
import com.example.healthflow.dao.UserDAO;
import com.example.healthflow.dao.PatientJdbcDAO;
import com.example.healthflow.dao.UserJdbcDAO;
import com.example.healthflow.db.Database;
import com.example.healthflow.model.PatientRow;
import com.example.healthflow.model.Role;
//import com.example.healthflow.model.dto.PatientView;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PatientService {

    private final UserDAO userDAO = new UserJdbcDAO();
    private final PatientDAO patientDAO = new PatientJdbcDAO();

    private static final String DEFAULT_PATIENT_PASSWORD = "patient@123";

    /** إضافة مريض جديد */
    public PatientRow createPatient(String fullName, String nid, String phone,
                                     LocalDate dob, String gender, String history) throws SQLException {

        String hash = org.mindrot.jbcrypt.BCrypt.hashpw(
                DEFAULT_PATIENT_PASSWORD, org.mindrot.jbcrypt.BCrypt.gensalt()
        );

        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                // 1) users: الآن gender هنا
                long userId = userDAO.insertWithGender(
                        nid, fullName, null, hash, phone, Role.PATIENT.name(), gender, c);

                // 2) patients: بدون gender بعد الترحيل
                long patientId = patientDAO.insert(userId, dob, history, c);

                c.commit();

                return new PatientRow(
                        patientId, userId, fullName, nid, phone, dob, gender, history
                );
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
                // users: حدّث الاسم/الهاتف/الرقم الوطني + gender
                userDAO.updateWithGender(userId, fullName, phone, nid, gender, c);

                // patients: حدّث التاريخ والسجل الطبي فقط
                patientDAO.update(patientId, dob, history, c);

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
            userDAO.delete(userId, c); // FK CASCADE سيحذف من patients
        }
    }

    /** قراءة كل المرضى */
    public List<PatientRow> listPatients() throws SQLException {
        // مهم: DAO لازم يعمل JOIN على users ويرجع u.gender AS gender
        return patientDAO.findAll();
    }
}