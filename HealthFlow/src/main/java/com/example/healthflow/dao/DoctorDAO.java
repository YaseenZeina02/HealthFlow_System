package com.example.healthflow.dao;

import com.example.healthflow.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO للدكاترة مع دعم حالة التوفر + جلب الأطباء المتاحين حسب التخصص.
 * يعتمد على جدول doctors وعمود availability_status من النوع ENUM doctor_status.
 */
public class DoctorDAO {

    // حالة الطبيب (مطابقة لقيم ENUM في قاعدة البيانات)
    public enum DoctorAvailability {
        AVAILABLE, IN_APPOINTMENT, ON_BREAK
    }

    /** عنصر للعرض في ComboBox: يحمل doctor_id و user_id والاسم والتخصص */
    public static final class DoctorOption {
        public final long doctorId;
        public final long userId;
        public final String fullName;
        public final String specialty;

        public DoctorOption(long doctorId, long userId, String fullName, String specialty) {
            this.doctorId = doctorId;
            this.userId = userId;
            this.fullName = fullName;
            this.specialty = specialty;
        }

        @Override
        public String toString() {
            return fullName + " (" + specialty + ")";
        }
    }

    /* ==================== حالة التوفر ==================== */

    public void setAvailability(long doctorUserId, DoctorAvailability status) throws Exception {
        try (Connection c = Database.get()) {
            setAvailability(c, doctorUserId, status);
        }
    }

    public void setAvailability(Connection c, long doctorUserId, DoctorAvailability status) throws Exception {
        String sql = "UPDATE doctors SET availability_status = ? WHERE user_id = ?";
        try (var ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, doctorUserId);
            ps.executeUpdate();
        }
    }

    public DoctorAvailability getAvailability(long doctorUserId) throws Exception {
        try (Connection c = Database.get()) {
            return getAvailability(c, doctorUserId);
        }
    }

    public DoctorAvailability getAvailability(Connection c, long doctorUserId) throws Exception {
        String sql = "SELECT availability_status FROM doctors WHERE user_id = ?";
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorUserId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return DoctorAvailability.valueOf(rs.getString(1));
            }
        }
    }

    /* ==================== التخصصات ==================== */

    /** يعيد قائمة بالتخصصات الموجودة */
    public List<String> listSpecialties() throws Exception {
        try (Connection c = Database.get()) {
            return listSpecialties(c);
        }
    }

    public List<String> listSpecialties(Connection c) throws Exception {
        String sql = "SELECT DISTINCT specialty FROM doctors ORDER BY specialty";
        try (var ps = c.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            List<String> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getString(1));
            return list;
        }
    }

    /* ==================== الأطباء المتاحون ==================== */

    /** جميع الأطباء المتاحين (إن مررت specialty=null يعيد كل المتاحين) */
    public List<DoctorOption> listAvailableBySpecialty(String specialty) throws Exception {
        try (Connection c = Database.get()) {
            return listAvailableBySpecialty(c, specialty);
        }
    }



    public List<DoctorOption> listAvailableBySpecialty(Connection c, String specialty) throws Exception {
        String sql = """
            SELECT d.id, d.user_id, u.full_name, d.specialty
            FROM doctors d
            JOIN users u ON u.id = d.user_id
            WHERE d.availability_status = 'AVAILABLE'
              AND (? IS NULL OR d.specialty = ?)
            ORDER BY u.full_name
            """;
        try (var ps = c.prepareStatement(sql)) {
            if (specialty == null || specialty.isBlank()) {
                ps.setNull(1, java.sql.Types.VARCHAR);
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(1, specialty);
                ps.setString(2, specialty);
            }
            try (var rs = ps.executeQuery()) {
                List<DoctorOption> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new DoctorOption(
                            rs.getLong(1),
                            rs.getLong(2),
                            rs.getString(3),
                            rs.getString(4)
                    ));
                }
                return list;
            }
        }
    }
    public void ensureProfileForUser(Connection c, long userId) throws SQLException {
        final String sql = """
            INSERT INTO doctors (user_id, specialty)
            VALUES (?, ?)
            ON CONFLICT (user_id) DO NOTHING
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, "GENERAL"); // اختر أي قيمة افتراضية مناسبة عندك
            ps.executeUpdate();
        }
        // لو عندك أعمدة أخرى NOT NULL لازم قيم افتراضية لها، أضفها هنا.
    }

    /**
     * نسخة مريحة بدون تمرير Connection (تفتح/تغلق الاتصال داخليًا).
     */
    public void ensureProfileForUser(long userId) throws SQLException {
        try (Connection c = Database.get()) {
            boolean old = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                ensureProfileForUser(c, userId);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(old);
            }
        }
    }

    /** حزمة بيانات خفيفة لعرض مرضى الطبيب (اليوم) */
    public static final class PatientLite {
        public final String nationalId;
        public final String fullName;
        public final String gender;           // MALE / FEMALE كنص
        public final java.time.LocalDate dateOfBirth;
        public final String medicalHistory;

        public PatientLite(String nationalId,
                           String fullName,
                           String gender,
                           java.time.LocalDate dateOfBirth,
                           String medicalHistory) {
            this.nationalId = nationalId;
            this.fullName = fullName;
            this.gender = gender;
            this.dateOfBirth = dateOfBirth;
            this.medicalHistory = medicalHistory;
        }
    }

    /** حزمة بيانات لعرض مرضى الطبيب مع معلومات الموعد */
    public static final class PatientWithAppt {
        public final long patientId;
        public final String patientName;
        public final String nationalId;
        public final String gender;
        public final String phone;
        public final String medicalHistory;
        public final java.time.LocalDate dateOfBirth;
        public final long appointmentId;
        public final java.time.OffsetDateTime appointmentDate;
        public final String status;

        public PatientWithAppt(long patientId,
                               String patientName,
                               String nationalId,
                               String gender,
                               String phone,
                               String medicalHistory,
                               java.time.LocalDate dateOfBirth,
                               long appointmentId,
                               java.time.OffsetDateTime appointmentDate,
                               String status) {
            this.patientId = patientId;
            this.patientName = patientName;
            this.nationalId = nationalId;
            this.gender = gender;
            this.phone = phone;
            this.medicalHistory = medicalHistory;
            this.dateOfBirth = dateOfBirth;
            this.appointmentId = appointmentId;
            this.appointmentDate = appointmentDate;
            this.status = status;
        }
    }

    /** جميع المرضى الذين لديهم مواعيد مع هذا الطبيب (حسب user_id للطبيب) */
    public java.util.List<PatientWithAppt> listPatientsWithAppointmentsForDoctor(long doctorUserId) throws Exception {
        try (java.sql.Connection c = com.example.healthflow.db.Database.get()) {
            return listPatientsWithAppointmentsForDoctor(c, doctorUserId);
        }
    }

    /** نسخة مع تمرير Connection (للاستخدام داخل معاملات) */
    public java.util.List<PatientWithAppt> listPatientsWithAppointmentsForDoctor(java.sql.Connection c, long doctorUserId) throws Exception {
        final String sql = """
            SELECT 
                p.id AS patient_id,
                u.full_name AS patient_name,
                u.national_id,
                u.gender::text AS gender,
                u.phone,
                p.medical_history,
                p.date_of_birth,
                a.id AS appointment_id,
                a.appointment_date,
                a.status
            FROM appointments a
            JOIN patients p ON a.patient_id = p.id
            JOIN users u ON p.user_id = u.id
            JOIN doctors d ON d.id = a.doctor_id
            WHERE d.user_id = ?
            ORDER BY a.appointment_date DESC
        """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorUserId);
            try (var rs = ps.executeQuery()) {
                java.util.ArrayList<PatientWithAppt> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    out.add(new PatientWithAppt(
                            rs.getLong("patient_id"),
                            rs.getString("patient_name"),
                            rs.getString("national_id"),
                            rs.getString("gender"),
                            rs.getString("phone"),
                            rs.getString("medical_history"),
                            rs.getObject("date_of_birth", java.time.LocalDate.class),
                            rs.getLong("appointment_id"),
                            rs.getObject("appointment_date", java.time.OffsetDateTime.class),
                            rs.getString("status")
                    ));
                }
                return out;
            }
        }
    }

    /**
     * يعيد مرضى الطبيب (حسب user_id للطبيب) الذين لديهم موعد "اليوم" فقط.
     * يعتمد على الجداول: appointments, doctors, patients, users.
     */
    public java.util.List<PatientLite> listTodaysPatientsForDoctor(long doctorUserId) throws Exception {
        try (java.sql.Connection c = com.example.healthflow.db.Database.get()) {
            return listTodaysPatientsForDoctor(c, doctorUserId);
        }
    }

    public java.util.List<PatientLite> listTodaysPatientsForDoctor(java.sql.Connection c, long doctorUserId) throws Exception {
        final String sql = """
            SELECT DISTINCT ON (u2.id)
                   u2.national_id,
                   u2.full_name,
                   u2.gender::text AS gender,
                   p.date_of_birth,
                   p.medical_history,
                   a.appointment_date
            FROM appointments a
            JOIN patients p ON a.patient_id = p.id
            JOIN users u2   ON p.user_id   = u2.id
            WHERE a.doctor_id = (SELECT id FROM doctors WHERE user_id = ?)
            ORDER BY u2.id, a.appointment_date DESC
        """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorUserId);
            try (var rs = ps.executeQuery()) {
                java.util.ArrayList<PatientLite> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    out.add(new PatientLite(
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
