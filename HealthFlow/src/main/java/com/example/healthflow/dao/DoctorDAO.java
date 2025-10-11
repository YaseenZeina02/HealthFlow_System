package com.example.healthflow.dao;

import com.example.healthflow.db.Database;
import com.example.healthflow.model.DoctorRow;
import javafx.application.Platform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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

    /** عنصر للعرض في ComboBox: يحمل doctor_id و user_id والاسم والتخصص ورقم الغرفة */
    public static final class DoctorOption {
        public final long doctorId;
        public final long userId;
        public final String fullName;
        public final String specialty;
        public final String roomNumber; // NEW

        public DoctorOption(long doctorId, long userId, String fullName, String specialty, String roomNumber) {
            this.doctorId = doctorId;
            this.userId = userId;
            this.fullName = fullName;
            this.specialty = specialty;
            this.roomNumber = roomNumber;
        }

        @Override
        public String toString() {
            return roomNumber == null || roomNumber.isBlank()
                    ? fullName + " (" + specialty + ")"
                    : fullName + " (" + specialty + ")  — Room: " + roomNumber;
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

    public static List<DoctorRow> loadDoctorsBG() {
        final String sql = """
        SELECT d.id,
               u.full_name,
               u.gender::text AS gender,
               COALESCE(u.phone,'') AS phone,
               d.specialty,
               COALESCE(d.bio,'') AS bio,
               d.availability_status::text AS status,
               d.room_number
        FROM doctors d
        JOIN users u ON u.id = d.user_id
        ORDER BY u.full_name
    """;
        List<DoctorRow> list = new ArrayList<>();
        try (var c = Database.get(); var ps = c.prepareStatement(sql); var rs = ps.executeQuery()) {
            while (rs.next()) {
                DoctorRow r = new DoctorRow();
                r.setFullName(rs.getString("full_name"));
                r.setGender(rs.getString("gender"));
                r.setPhone(rs.getString("phone"));
                r.setSpecialty(rs.getString("specialty"));
                r.setBio(rs.getString("bio"));
                String st = rs.getString("status");
                r.setStatusText(st);
                r.setAvailable("AVAILABLE".equalsIgnoreCase(st));
                r.setRoomNumber(rs.getString("room_number"));
                list.add(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<DoctorOption> listAvailableBySpecialty(Connection c, String specialty) throws Exception {
        String sql = """
            SELECT d.id, d.user_id, u.full_name, d.specialty, d.room_number
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
                            rs.getString(4),
                            rs.getString(5)
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

    public record Slot(LocalDateTime from, LocalDateTime to) {
        private static final DateTimeFormatter TIME_12 = DateTimeFormatter.ofPattern("hh:mm a");
        @Override public String toString() {
            return from.toLocalTime().format(TIME_12) + " \u2192 " + to.toLocalTime().format(TIME_12);
        }
    }


    // تعرض الأوقات المتاحة للدكتور المختار
    public List<Slot> listFreeSlots(long doctorId, LocalDate day,
                                    LocalTime open, LocalTime close, int slotMin) throws SQLException {
        String sql = """
            WITH params AS (
              SELECT
                ?::bigint     AS doc_id,     -- 1: doctorId
                ?::date       AS d,          -- 2: day
                ?::timestamp  AS open_at,    -- 3: day + open
                ?::timestamp  AS close_at,   -- 4: day + close
                ?::int        AS slot_min    -- 5: slotMin
            ),
            series AS (
              SELECT
                generate_series(
                  p.open_at,
                  p.close_at - (p.slot_min || ' min')::interval,
                  (p.slot_min || ' min')::interval
                ) AS slot_start,
                p.slot_min,
                p.d       AS day,
                p.doc_id  AS doc_id,
                /* أول فتحة اليوم (لو اليوم = تاريخ النظام نقرّب للسلوت التالي) */
                CASE
                  WHEN p.d = CURRENT_DATE THEN
                    GREATEST(
                      p.open_at,
                      date_trunc('minute', NOW())
                      + make_interval(mins => (p.slot_min - ((EXTRACT(EPOCH FROM NOW())/60)::int % p.slot_min)) % p.slot_min)
                    )
                  ELSE p.open_at
                END AS first_slot
              FROM params p
            ),
            booked AS (
              SELECT
                a.doctor_id,
                a.appointment_date::date AS day,
                a.appointment_date AS start_at,
                a.appointment_date + (a.duration_minutes || ' min')::interval AS end_at
              FROM appointments a
            )
            SELECT
              s.slot_start                                    AS free_from,
              s.slot_start + (s.slot_min || ' min')::interval AS free_to,
              to_char(s.slot_start::timestamptz, 'HH12:MI AM') AS free_from_12,
              to_char((s.slot_start + (s.slot_min || ' min')::interval)::timestamptz, 'HH12:MI AM') AS free_to_12
            FROM series s
            WHERE s.slot_start >= s.first_slot
              AND NOT EXISTS (
                SELECT 1
                FROM booked b
                WHERE b.doctor_id = s.doc_id
                  AND b.day       = s.day
                  AND tstzrange(b.start_at, b.end_at, '[)')
                      && tstzrange(
                            s.slot_start::timestamptz,
                            (s.slot_start + (s.slot_min || ' min')::interval)::timestamptz,
                            '[)'
                         )
              )
            ORDER BY free_from;
        """;


        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {

            ps.setLong(1, doctorId);
            ps.setObject(2, day); // ::date
            ps.setObject(3, LocalDateTime.of(day, open));  // ::timestamp
            ps.setObject(4, LocalDateTime.of(day, close)); // ::timestamp
            ps.setInt(5, slotMin);                         // ::int

            try (var rs = ps.executeQuery()) {
                var list = new ArrayList<Slot>();
                while (rs.next()) {
                    list.add(new Slot(
                            rs.getTimestamp("free_from").toLocalDateTime(),
                            rs.getTimestamp("free_to").toLocalDateTime()
                    ));
                }
                return list;
            }
        }
    }
    /**
     * صفّ مواعيد جاهز للعرض في الجداول (داشبورد/شاشة المواعيد).
     */
    public static final class AppointmentRow {
        public final long id;
        public final long doctorId;   // NEW
        public final long patientId;
        public final java.time.OffsetDateTime startAt;
        public final String doctorName;
        public final String patientName;
        public final String specialty;
        public final String status;
        public String location;

        public AppointmentRow(long id, long doctorId, long patientId,
                              java.time.OffsetDateTime startAt,
                              String doctorName,
                              String patientName,
                              String specialty,
                              String status) {
            this.id = id;
            this.doctorId = doctorId;
            this.patientId = patientId;
            this.startAt = startAt;
            this.doctorName = doctorName;
            this.patientName = patientName;
            this.specialty = specialty;
            this.status = status;
        }
    }

    /**
     * يعيد كل المواعيد مع اسم الدكتور/المريض والتخصص والحالة.
     */


    public java.util.List<AppointmentRow> listAppointments() throws SQLException {
        final String sql = """
        SELECT
            a.id,
            a.doctor_id,
            a.patient_id,
            a.appointment_date AS start_at,
            du.full_name AS doctor_name,
            pu.full_name AS patient_name,
            d.specialty,
            a.status::text AS status,
            a.location AS location
        FROM appointments a
        JOIN doctors d  ON d.id = a.doctor_id
        JOIN users  du  ON du.id = d.user_id
        JOIN patients p ON p.id = a.patient_id
        JOIN users  pu  ON pu.id = p.user_id
        ORDER BY a.appointment_date DESC
    """;
        try (var c = Database.get();
             var ps = c.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            var out = new java.util.ArrayList<AppointmentRow>();
            while (rs.next()) {
                AppointmentRow row = new AppointmentRow(
                        rs.getLong("id"),
                        rs.getLong("doctor_id"),
                        rs.getLong("patient_id"),
                        rs.getObject("start_at", OffsetDateTime.class),
                        rs.getString("doctor_name"),
                        rs.getString("patient_name"),
                        rs.getString("specialty"),
                        rs.getString("status")
                );
                row.location = rs.getString("location"); // <-- مهم
                out.add(row);
            }
            return out;
        }
    }


    /**
     * Search appointments by free-text over doctor/patient names or status.
     * If query is null/blank, returns all (same as listAppointments()).
     */
    public java.util.List<AppointmentRow> searchAppointments(String query) throws SQLException {
        boolean hasQ = query != null && !query.trim().isEmpty();
        String base = """
        SELECT
            a.id,
            a.doctor_id,
            a.patient_id,
            a.appointment_date AS start_at,
            du.full_name AS doctor_name,
            pu.full_name AS patient_name,
            d.specialty,
            a.status::text AS status,
            a.location AS location
        FROM appointments a
        JOIN doctors d  ON d.id = a.doctor_id
        JOIN users  du  ON du.id = d.user_id
        JOIN patients p ON p.id = a.patient_id
        JOIN users  pu  ON pu.id = p.user_id
        WHERE 1=1
    """;
        String where = hasQ
                ? " AND (du.full_name ILIKE ? OR pu.full_name ILIKE ? OR d.specialty ILIKE ? OR a.status::text ILIKE ?)"
                : "";
        String sql = base + where + " ORDER BY a.appointment_date DESC";

        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            if (hasQ) {
                String q = "%" + query.trim() + "%";
                ps.setString(1, q);
                ps.setString(2, q);
                ps.setString(3, q);
                ps.setString(4, q);
            }
            try (var rs = ps.executeQuery()) {
                var out = new ArrayList<AppointmentRow>();
                while (rs.next()) {
                    AppointmentRow row = new AppointmentRow(
                            rs.getLong("id"),
                            rs.getLong("doctor_id"),
                            rs.getLong("patient_id"),
                            rs.getObject("start_at", OffsetDateTime.class),
                            rs.getString("doctor_name"),
                            rs.getString("patient_name"),
                            rs.getString("specialty"),
                            rs.getString("status")
                    );
                    row.location = rs.getString("location");
                    out.add(row);
                }
                return out;
            }
        }
    }
    /* ==================== Counters ==================== */




    /** إيجاد patient.id من national_id (users.national_id) */
    public Long findPatientIdByNationalId(String nationalId) throws SQLException {
        final String sql = """
            SELECT p.id
            FROM patients p
            JOIN users u ON u.id = p.user_id
            WHERE u.national_id = ?
        """;
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, nationalId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    public long findIdByName(String fullName) throws SQLException {
        String sql = "SELECT d.id FROM doctors d JOIN users u ON u.id = d.user_id WHERE u.full_name = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Doctor not found: " + fullName);
    }



}
