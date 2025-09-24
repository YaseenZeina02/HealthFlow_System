package com.example.healthflow.model;

import java.time.OffsetDateTime;

public class InventoryTransaction {
    private Long id;
    private Long medicineId;
    private Long batchId;         // nullable
    private int qtyChange;        // موجب/سالب
    private String reason;        // NOT NULL
    private String refType;       // nullable
    private Long refId;           // nullable
    private OffsetDateTime createdAt;

    public InventoryTransaction(){}

    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Long getMedicineId(){return medicineId;}
    public void setMedicineId(Long medicineId){this.medicineId=medicineId;}
    public Long getBatchId(){return batchId;}
    public void setBatchId(Long batchId){this.batchId=batchId;}
    public int getQtyChange(){return qtyChange;}
    public void setQtyChange(int qtyChange){this.qtyChange=qtyChange;}
    public String getReason(){return reason;}
    public void setReason(String reason){this.reason=reason;}
    public String getRefType(){return refType;}
    public void setRefType(String refType){this.refType=refType;}
    public Long getRefId(){return refId;}
    public void setRefId(Long refId){this.refId=refId;}
    public OffsetDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(OffsetDateTime createdAt){this.createdAt=createdAt;}
}