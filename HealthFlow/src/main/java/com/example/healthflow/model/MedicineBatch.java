package com.example.healthflow.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class MedicineBatch {
    private Long id;
    private Long medicineId;
    private String batchNo;
    private LocalDate expiryDate;
    private int quantity;
    private OffsetDateTime receivedAt;

    public MedicineBatch(){}

    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Long getMedicineId(){return medicineId;}
    public void setMedicineId(Long medicineId){this.medicineId=medicineId;}
    public String getBatchNo(){return batchNo;}
    public void setBatchNo(String batchNo){this.batchNo=batchNo;}
    public LocalDate getExpiryDate(){return expiryDate;}
    public void setExpiryDate(LocalDate expiryDate){this.expiryDate=expiryDate;}
    public int getQuantity(){return quantity;}
    public void setQuantity(int quantity){this.quantity=quantity;}
    public OffsetDateTime getReceivedAt(){return receivedAt;}
    public void setReceivedAt(OffsetDateTime receivedAt){this.receivedAt=receivedAt;}
}