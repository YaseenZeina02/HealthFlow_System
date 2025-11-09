package com.example.healthflow.dao;

import com.example.healthflow.model.ApptStatus;
import com.example.healthflow.model.Appointment;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {

    /* ================= CRUD الأساسية ================= */

    public Appointment create(Connection c, Long doctorId, Long patientId,
                              OffsetDateTime at, int durationMin,
                              ApptStatus status, String location, Long createdBy) throws SQLException {
        final String sql = """
            INSERT INTO appointments (doctor_id, patient_id, appointment_date, duration_minutes, status, location, created_by)
            VALUES (?, ?, ?, ?, ?::appt_status, ?, ?)
            RETURNING *
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            ps.setLong(2, patientId);
            ps.setObject(3, at);
            ps.setInt(4, durationMin);
            ps.setString(5, status.name());
            ps.setString(6, location);
            if (createdBy == null) ps.setNull(7, Types.BIGINT); else ps.setLong(7, createdBy);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException ex) {
            // Exclusion constraint for overlapping ranges (if you have it)
            if ("23P01".equals(ex.getSQLState())) {
                throw new SQLException("Doctor has an overlapping appointment in this time range", ex);
            }
            throw ex;
        }
        throw new SQLException("Failed to create appointment");
    }

    public Appointment findById(Connection c, Long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM appointments WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Appointment> listForDoctor(Connection c, Long doctorId, int limit, int offset) throws SQLException {
        final String sql = """
            SELECT * FROM appointments
             WHERE doctor_id = ?
             ORDER BY appointment_date
             LIMIT ? OFFSET ?
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            ps.setInt(2, Math.max(1, limit));
            ps.setInt(3, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                List<Appointment> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public boolean updateStatus(Connection c, Long apptId, ApptStatus status) throws SQLException {
        final String sql = "UPDATE appointments SET status = ?::appt_status, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, apptId);
            return ps.executeUpdate() == 1;
        }
    }

    /* ================= دوال لوحة الطبيب (اليوم الحالي) ================= */

    /** يحسب عدد مواعيد الطبيب في يوم محدد. إن مرّرت status=null يحسب الكل. */
    public int countForDoctorOnDate(Connection c, long doctorId, LocalDate day, ApptStatus statusOrNull) throws SQLException {
        final String base = """
                SELECT COUNT(*) AS cnt
                  FROM appointments a
                 WHERE a.doctor_id = ?
                   AND a.appointment_date::date = ?
                """;
        final String sql = (statusOrNull == null) ? base : base + " AND a.status = ?::appt_status";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            ps.setObject(2, day);
            if (statusOrNull != null) ps.setString(3, statusOrNull.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        }
    }

    /**
     * يجلب مواعيد اليوم للطبيب مع بيانات المريض (name/nid/history) وتقسيم التاريخ/الوقت.
     * ملاحظة: ::date و ::time يستخدمان توقيت السيرفر. اضبط TimeZone للـ DB إن لزم.
     */
    public List<DoctorDashboardAppt> listForDoctorOnDate(Connection c, long doctorId, LocalDate day) throws SQLException {
        final String sql = """
            SELECT a.id,
                   a.patient_id,
                   u.full_name      AS patient_name,
                   u.national_id    AS patient_nid,
                   a.appointment_date::date AS appt_date,
                   a.appointment_date::time AS appt_time,
                   a.status,
                   p.medical_history
              FROM appointments a
              JOIN users     u ON u.id = a.patient_id
         LEFT JOIN patients  p ON p.user_id = a.patient_id
             WHERE a.doctor_id = ?
               AND a.appointment_date::date = ?
          ORDER BY appt_time ASC NULLS LAST, a.id ASC
        """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            ps.setObject(2, day);
            try (ResultSet rs = ps.executeQuery()) {
                List<DoctorDashboardAppt> out = new ArrayList<>();
                while (rs.next()) {
                    DoctorDashboardAppt row = new DoctorDashboardAppt();
                    row.id = rs.getLong("id");
                    row.patientId = rs.getLong("patient_id");
                    row.patientName = rs.getString("patient_name");
                    row.patientNationalId = rs.getString("patient_nid");
                    row.date = rs.getObject("appt_date", LocalDate.class);
                    row.time = rs.getObject("appt_time", LocalTime.class);
                    row.status = ApptStatus.fromString(rs.getString("status"));
                    row.medicalHistory = rs.getString("medical_history");
                    out.add(row);
                }
                return out;
            }
        }
    }

    /* ================= Mapping ================= */

    private Appointment map(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setId(rs.getLong("id"));
        a.setDoctorId(rs.getLong("doctor_id"));
        a.setPatientId(rs.getLong("patient_id"));
        a.setAppointmentDate(rs.getObject("appointment_date", OffsetDateTime.class));
        a.setDurationMinutes(rs.getInt("duration_minutes"));
        a.setStatus(ApptStatus.fromString(rs.getString("status")));
        a.setLocation(rs.getString("location"));
        a.setCreatedBy((Long) rs.getObject("created_by"));
        a.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        a.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        return a;
    }

    /* ================= DTO للجدول ================= */
    public static class DoctorDashboardAppt {
        public long id;
        public long patientId;
        public String patientName;
        public String patientNationalId;
        public LocalDate date;
        public LocalTime time;
        public ApptStatus status;
        public String medicalHistory;
    }
}