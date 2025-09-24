package com.example.healthflow.model;

import java.time.OffsetDateTime;

public class Appointment {
    private Long id;
    private Long doctorId;
    private Long patientId;
    private OffsetDateTime appointmentDate;
    private int durationMinutes = 30;
    private ApptStatus status = ApptStatus.PENDING;
    private String location;
    private Long createdBy; // users.id
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Appointment(){}

    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Long getDoctorId(){return doctorId;}
    public void setDoctorId(Long doctorId){this.doctorId=doctorId;}
    public Long getPatientId(){return patientId;}
    public void setPatientId(Long patientId){this.patientId=patientId;}
    public OffsetDateTime getAppointmentDate(){return appointmentDate;}
    public void setAppointmentDate(OffsetDateTime appointmentDate){this.appointmentDate=appointmentDate;}
    public int getDurationMinutes(){return durationMinutes;}
    public void setDurationMinutes(int durationMinutes){this.durationMinutes=durationMinutes;}
    public ApptStatus getStatus(){return status;}
    public void setStatus(ApptStatus status){this.status=status;}
    public String getLocation(){return location;}
    public void setLocation(String location){this.location=location;}
    public Long getCreatedBy(){return createdBy;}
    public void setCreatedBy(Long createdBy){this.createdBy=createdBy;}
    public OffsetDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(OffsetDateTime createdAt){this.createdAt=createdAt;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(OffsetDateTime updatedAt){this.updatedAt=updatedAt;}
}