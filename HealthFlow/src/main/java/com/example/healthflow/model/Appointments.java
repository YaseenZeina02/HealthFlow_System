package com.example.healthflow.model;

import java.time.LocalTime;
import java.time.LocalDate;
public class Appointments {
    private int appointmentId;
    private int userId;
    private int patientId;
    private LocalDate date;
    private LocalTime time;
    private String notes;
    private AppointmentStatus status;

    public enum AppointmentStatus {
        PENDING, CONFIRMED, CANCELED;
    }
    public Appointments(int appointmentId, int userId, int patientId, LocalDate date, LocalTime time, String notes, AppointmentStatus status) {
        this.appointmentId = appointmentId;
        this.userId = userId;
        this.patientId = patientId;
        this.date = date;
        this.time = time;
        this.notes = notes;
        this.status = status;

    }

    public int getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(int appointmentId) {
        this.appointmentId = appointmentId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
}
