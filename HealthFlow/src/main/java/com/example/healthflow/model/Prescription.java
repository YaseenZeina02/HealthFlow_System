package com.example.healthflow.model;

import java.time.OffsetDateTime;

public class Prescription {
    private Long id;
    private Long appointmentId;
    private Long doctorId;
    private Long patientId;
    private Long pharmacistId; // nullable
    private PrescriptionStatus status = PrescriptionStatus.PENDING;
    private OffsetDateTime decisionAt; // nullable
    private String decisionNote;       // nullable
    private String notes;              // nullable
    private OffsetDateTime createdAt;
    private OffsetDateTime approvedAt;   // nullable
    private OffsetDateTime dispensedAt;  // nullable
    private Long approvedBy;             // nullable (pharmacists.id)
    private Long dispensedBy;            // nullable (pharmacists.id)

    public Prescription(){}

    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Long getAppointmentId(){return appointmentId;}
    public void setAppointmentId(Long appointmentId){this.appointmentId=appointmentId;}
    public Long getDoctorId(){return doctorId;}
    public void setDoctorId(Long doctorId){this.doctorId=doctorId;}
    public Long getPatientId(){return patientId;}
    public void setPatientId(Long patientId){this.patientId=patientId;}
    public Long getPharmacistId(){return pharmacistId;}
    public void setPharmacistId(Long pharmacistId){this.pharmacistId=pharmacistId;}
    public PrescriptionStatus getStatus(){return status;}
    public void setStatus(PrescriptionStatus status){this.status=status;}
    public OffsetDateTime getDecisionAt(){return decisionAt;}
    public void setDecisionAt(OffsetDateTime decisionAt){this.decisionAt=decisionAt;}
    public String getDecisionNote(){return decisionNote;}
    public void setDecisionNote(String decisionNote){this.decisionNote=decisionNote;}
    public String getNotes(){return notes;}
    public void setNotes(String notes){this.notes=notes;}
    public OffsetDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(OffsetDateTime createdAt){this.createdAt=createdAt;}
    public OffsetDateTime getApprovedAt(){return approvedAt;}
    public void setApprovedAt(OffsetDateTime approvedAt){this.approvedAt=approvedAt;}
    public OffsetDateTime getDispensedAt(){return dispensedAt;}
    public void setDispensedAt(OffsetDateTime dispensedAt){this.dispensedAt=dispensedAt;}
    public Long getApprovedBy(){return approvedBy;}
    public void setApprovedBy(Long approvedBy){this.approvedBy=approvedBy;}
    public Long getDispensedBy(){return dispensedBy;}
    public void setDispensedBy(Long dispensedBy){this.dispensedBy=dispensedBy;}
}