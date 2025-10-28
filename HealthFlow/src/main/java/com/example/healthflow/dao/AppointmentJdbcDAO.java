package com.example.healthflow.dao;

import com.example.healthflow.db.Database;
import com.example.healthflow.model.dto.DoctorApptRow;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentJdbcDAO {




    public List<DoctorApptRow> listTodayByDoctor(long doctorId) throws SQLException {
        final String sql = """
            SELECT a.id as appointment_id,
                   p.id as patient_id,
                   u.id as user_id,
                   u.full_name as patient_name,
                   u.national_id,
                   a.appointment_date as appt_at,
                   a.duration_minutes,
                   a.status::text AS status,
                   p.medical_history
            FROM appointments a
            JOIN patients p ON p.id = a.patient_id
            JOIN users u ON u.id = p.user_id
            WHERE a.doctor_id = ?
              AND a.appointment_date >= (CURRENT_DATE AT TIME ZONE 'Asia/Gaza')
              AND a.appointment_date <  ((CURRENT_DATE + 1) AT TIME ZONE 'Asia/Gaza')
            ORDER BY a.appointment_date
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                List<DoctorApptRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new DoctorApptRow(
                            rs.getLong("appointment_id"),
                            rs.getLong("patient_id"),
                            rs.getLong("user_id"),
                            rs.getString("patient_name"),
                            rs.getString("national_id"),
                            rs.getObject("appt_at", OffsetDateTime.class),
                            rs.getInt("duration_minutes"),
                            rs.getString("status"),
                            rs.getString("medical_history")
                    ));
                }
                return out;
            }
        }
    }

    public int markCompleted(long appointmentId) throws SQLException {
        final String sql = "UPDATE appointments SET status='COMPLETED'::appt_status, updated_at=now() WHERE id=? AND status <> 'COMPLETED'::appt_status";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, appointmentId);
            return ps.executeUpdate();
        }
    }

    public Counts todayCounts(long doctorId) throws SQLException {
        final String sql = """
            SELECT
              COUNT(*) FILTER (
                WHERE appointment_date >= (CURRENT_DATE AT TIME ZONE 'Asia/Gaza')
                  AND appointment_date <  ((CURRENT_DATE + 1) AT TIME ZONE 'Asia/Gaza')
              ) AS total_today,
              COUNT(*) FILTER (
                WHERE appointment_date >= (CURRENT_DATE AT TIME ZONE 'Asia/Gaza')
                  AND appointment_date <  ((CURRENT_DATE + 1) AT TIME ZONE 'Asia/Gaza')
                  AND status = 'COMPLETED'::appt_status
              ) AS completed_today,
              COUNT(*) FILTER (
                WHERE appointment_date >= (CURRENT_DATE AT TIME ZONE 'Asia/Gaza')
                  AND appointment_date <  ((CURRENT_DATE + 1) AT TIME ZONE 'Asia/Gaza')
                  AND status IN ('PENDING'::appt_status,'SCHEDULED'::appt_status)
              ) AS remaining_today
            FROM appointments
            WHERE doctor_id = ?
        """;
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Counts(
                        rs.getInt("total_today"),
                        rs.getInt("completed_today"),
                        rs.getInt("remaining_today")
                );
            }
        }
    }

    public record Counts(int totalToday, int completedToday, int remainingToday) {}


    public static List<DoctorDAO.AppointmentRow> listScheduledAppointments() throws SQLException {
        final String sql = """
            SELECT 
                a.id,
                a.doctor_id,
                a.patient_id,
                a.appointment_date AS start_at,
                udoc.full_name  AS doctor_name,
                up.full_name    AS patient_name,
                a.status::text  AS status,
                d.specialty     AS specialty,
                a.location      AS location
            FROM appointments a
            JOIN doctors d  ON d.id = a.doctor_id
            JOIN users udoc ON udoc.id = d.user_id
            JOIN patients p ON p.id = a.patient_id
            JOIN users up   ON up.id = p.user_id
            WHERE a.status = 'SCHEDULED'::appt_status
            ORDER BY a.appointment_date
        """;

        try (var c = Database.get();
             var ps = c.prepareStatement(sql);
             var rs = ps.executeQuery()) {

            var rows = new ArrayList<DoctorDAO.AppointmentRow>();
            while (rs.next()) {
                DoctorDAO.AppointmentRow r = new DoctorDAO.AppointmentRow(
                        rs.getLong("id"),
                        rs.getLong("doctor_id"),
                        rs.getLong("patient_id"),
                        rs.getObject("start_at", java.time.OffsetDateTime.class),
                        rs.getString("doctor_name"),
                        rs.getString("patient_name"),
                        rs.getString("specialty"),
                        rs.getString("status")
                );
                r.location = rs.getString("location");
                rows.add(r);
            }
            return rows;
        }
    }

    //Room
    public static String findDoctorRoom(long doctorId) {
        final String sql = "SELECT room_number FROM doctors WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException ignored) {}
        return null;
    }

    public static List<DoctorDAO.AppointmentRow> searchScheduledAppointments(String query) throws SQLException {
        boolean hasQ = query != null && !query.isBlank();
        String base = """
            SELECT a.id,
                   a.doctor_id,
                   a.patient_id,
                   a.appointment_date AS start_at,
                   udoc.full_name  AS doctor_name,
                   up.full_name    AS patient_name,
                   a.status::text  AS status,
                   d.specialty     AS specialty
            FROM appointments a
            JOIN doctors d  ON d.id = a.doctor_id
            JOIN users udoc ON udoc.id = d.user_id
            JOIN patients p ON p.id = a.patient_id
            JOIN users up   ON up.id = p.user_id
            WHERE a.status = 'SCHEDULED'
        """;
        String where = hasQ
                ? " AND (udoc.full_name ILIKE ? OR up.full_name ILIKE ? OR d.specialty ILIKE ?)"
                : "";
        String sql = base + where + " ORDER BY a.appointment_date";

        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            if (hasQ) {
                String q = "%" + query.trim() + "%";
                ps.setString(1, q);
                ps.setString(2, q);
                ps.setString(3, q);
            }
            try (var rs = ps.executeQuery()) {
                var out = new ArrayList<DoctorDAO.AppointmentRow>();
                while (rs.next()) {
                    out.add(new DoctorDAO.AppointmentRow(
                            rs.getLong("id"),
                            rs.getLong("doctor_id"),
                            rs.getLong("patient_id"),
                            rs.getObject("start_at", OffsetDateTime.class),
                            rs.getString("doctor_name"),
                            rs.getString("patient_name"),
                            rs.getString("specialty"),
                            rs.getString("status")
                    ));
                }
                return out;
            }
        }

    }

    public void markAppointmentCompleted(long apptId) throws SQLException {
        String sql = "UPDATE appointments SET status = 'COMPLETED', updated_at = NOW() WHERE id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, apptId);
            ps.executeUpdate();
        }
    }

    public static int countAvailableDoctors() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM doctors WHERE availability_status = 'AVAILABLE'";
        try (var c = Database.get(); var ps = c.prepareStatement(sql); var rs = ps.executeQuery()) {
            var out = new ArrayList<DoctorDAO.AppointmentRow>();
            rs.next(); return rs.getInt(1);
        }
    }

    public static int countPatients() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM patients";
        try (var c = Database.get(); var ps = c.prepareStatement(sql); var rs = ps.executeQuery()) {
            rs.next(); return rs.getInt(1);
        }
    }

    public static int countAppointments() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM appointments";
        try (var c = Database.get(); var ps = c.prepareStatement(sql); var rs = ps.executeQuery()) {
            rs.next(); return rs.getInt(1);
        }
    }

    public static int countCompletedAppointments() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM appointments WHERE status = 'COMPLETED'";
        try (var c = Database.get(); var ps = c.prepareStatement(sql); var rs = ps.executeQuery()) {
            rs.next(); return rs.getInt(1);
        }
    }

    public int countPendingAppointments() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM appointments WHERE status = 'PENDING'";
        try (var c = Database.get(); var ps = c.prepareStatement(sql); var rs = ps.executeQuery()) {
            rs.next(); return rs.getInt(1);
        }
    }

    public static int countScheduledAppointments() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM appointments WHERE status = 'SCHEDULED'";
        try (var c = Database.get(); var ps = c.prepareStatement(sql); var rs = ps.executeQuery()) {
            rs.next(); return rs.getInt(1);
        }
    }
    /** يحصي المواعيد حسب الحالة في يوم معين (على مستوى جميع الأطباء). */
    public static java.util.Map<String, Integer> countByStatusOnDate(java.time.LocalDate day) throws SQLException {
        final String sql = """
                SELECT status::text AS status, COUNT(*) AS cnt
                FROM appointments
                WHERE (appointment_date AT TIME ZONE 'Asia/Gaza')::date = ?
                GROUP BY status::text
                """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, day);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
                while (rs.next()) {
                    map.put(rs.getString("status"), rs.getInt("cnt"));
                }
                // اضمن وجود مفاتيح للحالات الشائعة حتى لو كانت صفر (اختياري)
                String[] expected = {"SCHEDULED", "CONFIRMED", "PENDING", "COMPLETED", "CANCELLED"};
                for (String k : expected) map.putIfAbsent(k, 0);
                return map;
            }
        }
    }

    /** All appointments (any status) for a given calendar day, Asia/Gaza local date, for Reception dashboard. */
    public static java.util.List<com.example.healthflow.dao.DoctorDAO.AppointmentRow>
    listByDateAll(java.time.LocalDate day) throws java.sql.SQLException {
        final String sql = """
        SELECT 
            a.id,
            a.doctor_id,
            a.patient_id,
            a.appointment_date AS start_at,
            udoc.full_name  AS doctor_name,
            up.full_name    AS patient_name,
            a.status::text  AS status,
            d.specialty     AS specialty,
            a.location      AS location
        FROM appointments a
        JOIN doctors d  ON d.id = a.doctor_id
        JOIN users udoc ON udoc.id = d.user_id
        JOIN patients p ON p.id = a.patient_id
        JOIN users up   ON up.id = p.user_id
        WHERE (a.appointment_date AT TIME ZONE 'Asia/Gaza')::date = ?
        ORDER BY a.appointment_date
    """;
        try (var c = com.example.healthflow.db.Database.get();
             var ps = c.prepareStatement(sql)) {
            ps.setObject(1, day);
            try (var rs = ps.executeQuery()) {
                var rows = new java.util.ArrayList<com.example.healthflow.dao.DoctorDAO.AppointmentRow>();
                while (rs.next()) {
                    var r = new com.example.healthflow.dao.DoctorDAO.AppointmentRow(
                            rs.getLong("id"),
                            rs.getLong("doctor_id"),
                            rs.getLong("patient_id"),
                            rs.getObject("start_at", java.time.OffsetDateTime.class),
                            rs.getString("doctor_name"),
                            rs.getString("patient_name"),
                            rs.getString("specialty"),
                            rs.getString("status")
                    );
                    r.location = rs.getString("location");
                    rows.add(r);
                }
                return rows;
            }
        }
    }

    /** عدد الأطباء الذين لديهم مواعيد في تاريخ محدد */
    public static int countDoctorsOnDate(java.time.LocalDate day) throws SQLException {
        final String sql = "SELECT COUNT(DISTINCT doctor_id) FROM appointments WHERE appointment_date::date = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, day);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** عدد المرضى الذين لديهم مواعيد في تاريخ محدد */
    public static int countPatientsOnDate(java.time.LocalDate day) throws SQLException {
        final String sql = "SELECT COUNT(DISTINCT patient_id) FROM appointments WHERE appointment_date::date = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, day);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** عدد المواعيد الكلي في تاريخ محدد */
    public static int countAppointmentsOnDate(java.time.LocalDate day) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM appointments WHERE appointment_date::date = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, day);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** عدد المواعيد المكتملة في تاريخ محدد */
    public static int countCompletedAppointmentsOnDate(java.time.LocalDate day) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM appointments WHERE appointment_date::date = ? AND status = 'COMPLETED'::appt_status";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, day);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** عدد المواعيد المتبقية (Pending + Scheduled) في تاريخ محدد */
    public static int countRemainingAppointmentsOnDate(java.time.LocalDate day) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM appointments WHERE appointment_date::date = ? AND status IN ('PENDING'::appt_status,'SCHEDULED'::appt_status)";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, day);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }


}