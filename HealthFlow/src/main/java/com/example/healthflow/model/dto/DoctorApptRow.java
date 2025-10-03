package com.example.healthflow.model.dto;

import java.time.OffsetDateTime;

public class DoctorApptRow {
    private final long appointmentId;
    private final long patientId;
    private final long userId;
    private final String patientName;
    private final String nationalId;
    private final OffsetDateTime apptAt;
    private final int durationMinutes;
    private final String status;
    private final String medicalHistory;

    public DoctorApptRow(long appointmentId, long patientId, long userId,
                         String patientName, String nationalId,
                         OffsetDateTime apptAt, int durationMinutes,
                         String status, String medicalHistory) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.userId = userId;
        this.patientName = patientName;
        this.nationalId = nationalId;
        this.apptAt = apptAt;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.medicalHistory = medicalHistory;
    }

    public long getAppointmentId() { return appointmentId; }
    public long getPatientId() { return patientId; }
    public long getUserId() { return userId; }
    public String getPatientName() { return patientName; }
    public String getNationalId() { return nationalId; }
    public OffsetDateTime getApptAt() { return apptAt; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getStatus() { return status; }
    public String getMedicalHistory() { return medicalHistory; }
}