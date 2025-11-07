package com.example.healthflow.model;

import javafx.beans.property.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

// ===== Row models (re-added) =====
public class PatientRow {
    private final LongProperty patientId = new SimpleLongProperty();
    private final LongProperty userId = new SimpleLongProperty();
    private final StringProperty fullName = new SimpleStringProperty();
    private final StringProperty nationalId = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> dateOfBirth = new SimpleObjectProperty<>();
    private final StringProperty gender = new SimpleStringProperty();
    private final StringProperty medicalHistory = new SimpleStringProperty();
    private final IntegerProperty age = new SimpleIntegerProperty();

    public int getAge() {
        return age.get();
    }
    private void setAge(int age) {
        this.age.set(Math.max(0, age)); // يمنع القيم السالبة
    }

    public IntegerProperty ageProperty() {
        return age;
    }

    public PatientRow(Long patientId, Long userId, String fullName, String nationalId,
                      String phone, LocalDate dob, String gender, String medicalHistory) {
        setPatientId(patientId);
        setUserId(userId);
        setFullName(fullName);
        setNationalId(nationalId);
        setPhone(phone);
        setDateOfBirth(dob);
        setGender(gender);
        setMedicalHistory(medicalHistory);
    }

    public PatientRow(String nid, String name, String gender, int age, String history) {
            setNationalId(nid);
            setFullName(name);
            setGender(gender);
            setAge(age);
            setMedicalHistory(history);
        }



    public long getPatientId() { return patientId.get(); }
    public void setPatientId(long v) { patientId.set(v); }
    public LongProperty patientIdProperty() { return patientId; }

    public long getUserId() { return userId.get(); }
    public void setUserId(long v) { userId.set(v); }
    public LongProperty userIdProperty() { return userId; }

    public String getFullName() { return fullName.get(); }
    public void setFullName(String v) { fullName.set(v); }
    public StringProperty fullNameProperty() { return fullName; }

    public String getNationalId() { return nationalId.get(); }
    public void setNationalId(String v) { nationalId.set(v); }
    public StringProperty nationalIdProperty() { return nationalId; }

    public String getPhone() { return phone.get(); }
    public void setPhone(String v) { phone.set(v); }
    public StringProperty phoneProperty() { return phone; }

    public LocalDate getDateOfBirth() { return dateOfBirth.get(); }
    public void setDateOfBirth(LocalDate v) { dateOfBirth.set(v); }
    public ObjectProperty<LocalDate> dateOfBirthProperty() { return dateOfBirth; }

    public String getGender() { return gender.get(); }
    public void setGender(String v) { gender.set(v); }
    public StringProperty genderProperty() { return gender; }

    public String getMedicalHistory() { return medicalHistory.get(); }
    public void setMedicalHistory(String v) { medicalHistory.set(v); }
    public StringProperty medicalHistoryProperty() { return medicalHistory; }

//    for alias used by som UIs
    public long getId() { return getPatientId(); }
}