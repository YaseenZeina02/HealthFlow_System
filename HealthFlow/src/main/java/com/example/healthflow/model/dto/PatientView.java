package com.example.healthflow.model.dto;

import java.time.LocalDate;

public record PatientView(
        long patientId,
        long userId,
        String fullName,
        String nationalId,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String medicalHistory
) {}