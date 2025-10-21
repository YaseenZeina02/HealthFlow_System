package com.example.healthflow.service;

import com.example.healthflow.dao.AppointmentJdbcDAO;
import com.example.healthflow.dao.PatientDAO;
import com.example.healthflow.dao.PatientJdbcDAO;
import com.example.healthflow.model.PatientRow;
import com.example.healthflow.model.dto.DoctorApptRow;
//import com.example.healthflow.model.dto.PatientView;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class DoctorDashboardService {

    private final AppointmentJdbcDAO apptDAO = new AppointmentJdbcDAO();
    private final PatientDAO patientDAO = new PatientJdbcDAO();
    private static final java.time.ZoneId APP_TZ = java.time.ZoneId.of("Asia/Gaza");

    public Stats loadTodayStats(long doctorId, LocalDate now) throws Exception {
        var rows = apptDAO.listTodayByDoctor(doctorId);
        int total = 0;
        int completed = 0;
        int remaining = 0;
        java.util.HashSet<Long> distinctPatients = new java.util.HashSet<>();

        for (com.example.healthflow.model.dto.DoctorApptRow r : rows) {
            total++;
            if (r.getUserId() != 0) distinctPatients.add(r.getUserId());
            String st = r.getStatus();
            if (st != null && st.equalsIgnoreCase("COMPLETED")) {
                completed++;
            } else if (st == null || st.equalsIgnoreCase("SCHEDULED") || st.equalsIgnoreCase("PENDING") || st.equalsIgnoreCase("CONFIRMED")) {
                remaining++;
            }
        }
        if (total == 0) {
            try (var c = com.example.healthflow.db.Database.get();
                 var ps = c.prepareStatement(
                         "SELECT count(*) AS total,\n" +
                         "       count(*) FILTER (WHERE status = 'COMPLETED') AS completed,\n" +
                         "       count(*) FILTER (WHERE status IS NULL OR status IN ('SCHEDULED','PENDING','CONFIRMED')) AS remaining,\n" +
                         "       count(DISTINCT p.user_id) AS patients_today\n" +
                         "FROM appointments a\n" +
                         "JOIN doctors d  ON d.id = a.doctor_id\n" +
                         "JOIN users   du ON du.id = d.user_id\n" +
                         "JOIN patients p ON p.id = a.patient_id\n" +
                         "WHERE du.id = ? AND a.appointment_date::date = CURRENT_DATE") ) {
                ps.setLong(1, doctorId);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getInt("total");
                        completed = rs.getInt("completed");
                        remaining = rs.getInt("remaining");
                        distinctPatients.clear();
                        int patientsToday = rs.getInt("patients_today");
                        return new Stats(total, completed, remaining, patientsToday);
                    }
                }
            } catch (Exception ignored) {}
        }
        return new Stats(total, completed, remaining, distinctPatients.size());
    }

    public List<Appt> listTodayAppointments(long doctorId, LocalDate now) throws Exception {
        var rows = apptDAO.listTodayByDoctor(doctorId);
        List<Appt> out = new ArrayList<>(rows.size());
        for (DoctorApptRow r : rows) {
            Appt a = new Appt();
            a.id = r.getAppointmentId();
            a.patientUserId = r.getUserId();
            a.patientName = r.getPatientName();
            a.patientNationalId = r.getNationalId();
            if (r.getApptAt() != null) {
                var ldt = r.getApptAt();
                var zdt = ldt.atZoneSameInstant(APP_TZ);
                a.date = zdt.toLocalDate();
                a.time = zdt.toLocalTime();
            }
            a.status = r.getStatus();
            a.medicalHistory = r.getMedicalHistory();
            out.add(a);
        }
        return out;
    }

    public boolean markCompleted(long appointmentId) throws Exception {
        return apptDAO.markCompleted(appointmentId) == 1;
    }

    public List<PatientRow> listDoctorPatients() throws SQLException {
        return patientDAO.findAll(); // simple version for now
    }

    public record Stats(int total, int completed, int remaining, int patientsToday) {}

    public static final class Appt {
        public long id;
        public long patientUserId;
        public String patientName;
        public String patientNationalId;
        public java.time.LocalDate date;
        public java.time.LocalTime time;
        public String status;
        public String medicalHistory;
    }
}