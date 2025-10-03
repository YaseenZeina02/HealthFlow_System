package com.example.healthflow.service;

import com.example.healthflow.dao.AppointmentJdbcDAO;
import com.example.healthflow.dao.PatientDAO;
import com.example.healthflow.dao.PatientJdbcDAO;
import com.example.healthflow.model.dto.DoctorApptRow;
import com.example.healthflow.model.dto.PatientView;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class DoctorDashboardService {

    private final AppointmentJdbcDAO apptDAO = new AppointmentJdbcDAO();
    private final PatientDAO patientDAO = new PatientJdbcDAO();

    public Stats loadTodayStats(long doctorId, LocalDate now) throws Exception {
        var c = apptDAO.todayCounts(doctorId);
        return new Stats(c.totalToday(), c.completedToday(), c.remainingToday());
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
                a.date = r.getApptAt().toLocalDate();
                a.time = r.getApptAt().toLocalTime();
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

    public List<PatientView> listDoctorPatients() throws SQLException {
        return patientDAO.findAll(); // simple version for now
    }

    public record Stats(int total, int completed, int remaining) {}

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