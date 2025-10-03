// model/dto/DoctorPatientRow.java
package com.example.healthflow.model.dto;

import java.time.LocalDate;

public record DoctorPatientRow(
        long patientId,
        long userId,
        String nationalId,
        String fullName,
        String gender,      // من users.gender (إن كنت ناقلها هناك)
        LocalDate dateOfBirth,
        String medicalHistory
) {}