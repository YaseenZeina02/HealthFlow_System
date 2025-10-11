package com.example.healthflow.model;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static com.example.healthflow.controllers.ReceptionController.DEFAULT_SESSION_MIN;

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

    // ===== Nested row model used by UI tables =====
    public static class ApptRow {
        private final LongProperty id = new SimpleLongProperty(0);       // 0 = not yet persisted
        private final LongProperty doctorId = new SimpleLongProperty();
        private final LongProperty patientId = new SimpleLongProperty();
        private final StringProperty doctorName = new SimpleStringProperty();
        private final StringProperty patientName = new SimpleStringProperty();
        private final StringProperty specialty = new SimpleStringProperty();
        private final StringProperty status = new SimpleStringProperty("PENDING");
        private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now());
        private final ObjectProperty<LocalTime> time = new SimpleObjectProperty<>(LocalTime.of(9, 0));
        private final BooleanProperty isNew = new SimpleBooleanProperty(true);
        private final BooleanProperty dirty = new SimpleBooleanProperty(false);
        private final StringProperty roomNumber = new SimpleStringProperty();
        private final StringProperty location = new SimpleStringProperty();

        public long getId() { return id.get(); }
        public void setId(long v) { id.set(v); }
        public LongProperty idProperty() { return id; }

        public long getDoctorId() { return doctorId.get(); }
        public void setDoctorId(long v) { doctorId.set(v); dirty.set(true); }
        public LongProperty doctorIdProperty() { return doctorId; }

        public long getPatientId() { return patientId.get(); }
        public void setPatientId(long v) { patientId.set(v); dirty.set(true); }
        public LongProperty patientIdProperty() { return patientId; }

        public String getDoctorName() { return doctorName.get(); }
        public void setDoctorName(String v) { doctorName.set(v); }
        public StringProperty doctorNameProperty() { return doctorName; }

        public String getPatientName() { return patientName.get(); }
        public void setPatientName(String v) { patientName.set(v); }
        public StringProperty patientNameProperty() { return patientName; }

        public String getSpecialty() { return specialty.get(); }
        public void setSpecialty(String v) { specialty.set(v); }
        public StringProperty specialtyProperty() { return specialty; }

        public String getStatus() { return status.get(); }
        public void setStatus(String v) { status.set(v); }
        public StringProperty statusProperty() { return status; }

        public LocalDate getDate() { return date.get(); }
        public void setDate(LocalDate v) { date.set(v); dirty.set(true); }
        public ObjectProperty<LocalDate> dateProperty() { return date; }

        public LocalTime getTime() { return time.get(); }
        public void setTime(LocalTime v) { time.set(v); dirty.set(true); }
        public ObjectProperty<LocalTime> timeProperty() { return time; }

        public boolean isNew() { return isNew.get(); }
        public void setNew(boolean v) { isNew.set(v); }
        public BooleanProperty isNewProperty() { return isNew; }

        public boolean isDirty() { return dirty.get(); }
        public void setDirty(boolean v) { dirty.set(v); }
        public BooleanProperty dirtyProperty() { return dirty; }

        public String getRoomNumber() { return roomNumber.get(); }
        public void setRoomNumber(String v) { roomNumber.set(v); dirty.set(true); }
        public StringProperty roomNumberProperty() { return roomNumber; }

        public String getLocation() { return location.get(); }
        public void setLocation(String v) { location.set(v); dirty.set(true); }
        public StringProperty locationProperty() { return location; }

        private IntegerProperty sessionTime = new SimpleIntegerProperty(DEFAULT_SESSION_MIN);
        public int getSessionTime() {
            return sessionTime.get();
        }

        public void setSessionTime(int minutes) {
            this.sessionTime.set(minutes);
        }

        public IntegerProperty sessionTimeProperty() {
            return sessionTime;
        }

    }
}