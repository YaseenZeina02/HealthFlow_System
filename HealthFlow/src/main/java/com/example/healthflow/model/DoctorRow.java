package com.example.healthflow.model;

import javafx.beans.property.*;

import java.time.OffsetDateTime;

public class DoctorRow {
    private final LongProperty doctorId = new SimpleLongProperty();
    private final StringProperty fullName = new SimpleStringProperty();
    private final StringProperty gender = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty specialty = new SimpleStringProperty();
    private final StringProperty bio = new SimpleStringProperty();
    private final StringProperty statusText = new SimpleStringProperty();
    private final BooleanProperty available = new SimpleBooleanProperty(false);
    private final StringProperty roomNumber = new SimpleStringProperty();



    public DoctorRow(){}

    public DoctorRow(long doctorId, String fullName, String gender, String phone,
                     String specialty, String bio, String statusText, String roomNumber) {
        setDoctorId(doctorId);
        setFullName(fullName);
        setGender(gender);
        setPhone(phone);
        setSpecialty(specialty);
        setBio(bio);
        setStatusText(statusText);
        setAvailable("AVAILABLE".equalsIgnoreCase(statusText));
        setRoomNumber(roomNumber); // ✅ إضافة الغرفة هنا
    }

    public long getDoctorId() { return doctorId.get(); }
    public void setDoctorId(long v) { doctorId.set(v); }
    public LongProperty doctorIdProperty() { return doctorId; }

    public String getFullName() { return fullName.get(); }
    public void setFullName(String v) { fullName.set(v); }
    public StringProperty fullNameProperty() { return fullName; }

    public String getGender() { return gender.get(); }
    public void setGender(String v) { gender.set(v); }
    public StringProperty genderProperty() { return gender; }

    public String getPhone() { return phone.get(); }
    public void setPhone(String v) { phone.set(v); }
    public StringProperty phoneProperty() { return phone; }

    public String getSpecialty() { return specialty.get(); }
    public void setSpecialty(String v) { specialty.set(v); }
    public StringProperty specialtyProperty() { return specialty; }

    public String getBio() { return bio.get(); }
    public void setBio(String v) { bio.set(v); }
    public StringProperty bioProperty() { return bio; }

    public String getStatusText() { return statusText.get(); }
    public void setStatusText(String v) { statusText.set(v); }
    public StringProperty statusTextProperty() { return statusText; }

    public boolean isAvailable() { return available.get(); }
    public void setAvailable(boolean v) { available.set(v); }
    public BooleanProperty availableProperty() { return available; }

    public String getRoomNumber() {
        return roomNumber.get();
    }
    public void setRoomNumber(String room) {
        this.roomNumber.set(room);
    }

    public StringProperty roomNumberProperty() {
        return roomNumber;
    }
}