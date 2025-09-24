package com.example.healthflow.model;

public class PrescriptionItem {
    private Long id;
    private Long prescriptionId;
    private Long medicineId;     // nullable (اسم الدواء محفوظ أيضًا)
    private String medicineName; // NOT NULL
    private String dosage;       // NOT NULL
    private int quantity;        // > 0
    private int qtyDispensed;    // >= 0
    private ItemStatus status = ItemStatus.PENDING;
    private Long batchId;        // nullable

    public PrescriptionItem(){}

    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Long getPrescriptionId(){return prescriptionId;}
    public void setPrescriptionId(Long prescriptionId){this.prescriptionId=prescriptionId;}
    public Long getMedicineId(){return medicineId;}
    public void setMedicineId(Long medicineId){this.medicineId=medicineId;}
    public String getMedicineName(){return medicineName;}
    public void setMedicineName(String medicineName){this.medicineName=medicineName;}
    public String getDosage(){return dosage;}
    public void setDosage(String dosage){this.dosage=dosage;}
    public int getQuantity(){return quantity;}
    public void setQuantity(int quantity){this.quantity=quantity;}
    public int getQtyDispensed(){return qtyDispensed;}
    public void setQtyDispensed(int qtyDispensed){this.qtyDispensed=qtyDispensed;}
    public ItemStatus getStatus(){return status;}
    public void setStatus(ItemStatus status){this.status=status;}
    public Long getBatchId(){return batchId;}
    public void setBatchId(Long batchId){this.batchId=batchId;}
}