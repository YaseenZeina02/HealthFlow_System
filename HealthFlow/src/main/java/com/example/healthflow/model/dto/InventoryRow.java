package com.example.healthflow.model.dto;

public class InventoryRow {
    private final long medicineId;
    private final String displayName;
    private final String form;
    private final String baseUnit;
    private final int availableQuantity;
    private final String nextBatchNo;
    private final Integer nextBatchQty;
    private final java.time.LocalDate nextExpiry;
    private final String receivedBy;
    private final String receivedAtText;

    public InventoryRow(long medicineId, String displayName, String form, String baseUnit,
                        int availableQuantity,
                        String nextBatchNo, Integer nextBatchQty, java.time.LocalDate nextExpiry,
                        String receivedBy, String receivedAtText) {
        this.medicineId = medicineId;
        this.displayName = displayName;
        this.form = form;
        this.baseUnit = baseUnit;
        this.availableQuantity = availableQuantity;
        this.nextBatchNo = nextBatchNo;
        this.nextBatchQty = nextBatchQty;
        this.nextExpiry = nextExpiry;
        this.receivedBy = receivedBy;
        this.receivedAtText = receivedAtText;
    }
    public long getMedicineId() { return medicineId; }
    public String getDisplayName() { return displayName; }
    public String getForm() { return form; }
    public String getBaseUnit() { return baseUnit; }
    public int getAvailableQuantity() { return availableQuantity; }
    public String getNextBatchNo() { return nextBatchNo; }
    public Integer getNextBatchQty() { return nextBatchQty; }
    public java.time.LocalDate getNextExpiry() { return nextExpiry; }
    public String getReceivedBy() { return receivedBy; }
    public String getReceivedAt() { return receivedAtText; }
}