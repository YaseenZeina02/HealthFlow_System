package com.example.healthflow.model.dto;

import com.example.healthflow.core.packaging.PackUnit;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

public class PrescItemRow {
    private final LongProperty id = new SimpleLongProperty(0);               // 0 before DB insert
    private final LongProperty medicineId = new SimpleLongProperty();
    private final StringProperty medicineName = new SimpleStringProperty();
    private final StringProperty dosage = new SimpleStringProperty();         // e.g. "500 mg Tablet • 1 tab • BID • 5d • Oral"
    // ---- Structured dosage fields (DB-backed) ----
    private final IntegerProperty dose = new SimpleIntegerProperty();
    private final IntegerProperty freqPerDay = new SimpleIntegerProperty();
    private final StringProperty strength = new SimpleStringProperty();
    private final StringProperty form = new SimpleStringProperty();
    private final StringProperty route = new SimpleStringProperty();
    private final StringProperty dosageText = new SimpleStringProperty();
    private final IntegerProperty durationDays = new SimpleIntegerProperty(0);
    private final IntegerProperty quantity = new SimpleIntegerProperty(1);
    private final IntegerProperty qtyDispensed = new SimpleIntegerProperty(0);
    private final StringProperty status = new SimpleStringProperty("PENDING");
    private final StringProperty notes = new SimpleStringProperty();
    private final IntegerProperty stockAvailable = new SimpleIntegerProperty(0);
    private final StringProperty diagnosis = new SimpleStringProperty();
    private final LongProperty batchId = new SimpleLongProperty();
//    private Integer qtyUnitsRequested;     // الناتج الخام (مثلاً 14 قرص)
//    private PackUnit suggestedUnit;        // BLISTER / BOX / BOTTLE / TUBE / UNIT
//    private Integer suggestedCount;        // عدد العبوات المقترح
//    private Integer suggestedUnitsTotal;
    private final ObjectProperty<Integer> qtyUnitsRequested = new SimpleObjectProperty<>();
    private final ObjectProperty<PackUnit> suggestedUnit = new SimpleObjectProperty<>();
    private final ObjectProperty<Integer> suggestedCount = new SimpleObjectProperty<>();
    private final ObjectProperty<Integer> suggestedUnitsTotal = new SimpleObjectProperty<>();

    // Display-only: computed text for Pack column (Suggested/Approved)
    private final StringProperty pack = new SimpleStringProperty("—");

    private void recomputePack() {
        StringBuilder sb = new StringBuilder();
        // Suggested
        if (getSuggestedUnit() != null && getSuggestedCount() != null && getSuggestedUnitsTotal() != null) {
            sb.append("Sugg: ")
              .append(getSuggestedCount()).append(' ')
              .append(String.valueOf(getSuggestedUnit()))
              .append(" = ").append(getSuggestedUnitsTotal());
        }
        // Approved
        if (getApprovedUnit() != null && getApprovedCount() != null && getApprovedUnitsTotal() != null) {
            if (sb.length() > 0) sb.append("  |  ");
            sb.append("Appr: ")
              .append(getApprovedCount()).append(' ')
              .append(String.valueOf(getApprovedUnit()))
              .append(" = ").append(getApprovedUnitsTotal());
        }
        pack.set(sb.length() == 0 ? "—" : sb.toString());
    }

    public String getPack() { return pack.get(); }
    public StringProperty packProperty() { return pack; }


    // Approved packaging by pharmacist (what will be actually dispensed)
    private PackUnit approvedUnit;        // BLISTER / BOX / BOTTLE / TUBE / UNIT
    private Integer approvedCount;        // number of packs approved
    private Integer approvedUnitsTotal;   // total base units covered by the approved packs

    public LongProperty batchIdProperty() { return batchId; }
    public long getBatchId() { return batchId.get(); }
    public void setBatchId(long v) { batchId.set(v); }

    public PrescItemRow() {}

    public long getId() { return id.get(); }
    public void setId(long v) { id.set(v); }
    public LongProperty idProperty() { return id; }

    public long getMedicineId() { return medicineId.get(); }
    public void setMedicineId(long v) { medicineId.set(v); }
    public LongProperty medicineIdProperty() { return medicineId; }

    public String getMedicineName() { return medicineName.get(); }
    public void setMedicineName(String v) { medicineName.set(v); }
    public StringProperty medicineNameProperty() { return medicineName; }

    public String getDosage() { return dosage.get(); }
    public void setDosage(String v) { dosage.set(v); }
    public StringProperty dosageProperty() { return dosage; }

    public int getDurationDays() { return durationDays.get(); }
    public void setDurationDays(int v) { durationDays.set(v); }
    public IntegerProperty durationDaysProperty() { return durationDays; }

    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int v) { quantity.set(v); }
    public IntegerProperty quantityProperty() { return quantity; }

    public int getQtyDispensed() { return qtyDispensed.get(); }
    public void setQtyDispensed(int v) { qtyDispensed.set(v); }
    public IntegerProperty qtyDispensedProperty() { return qtyDispensed; }

    public String getStatus() { return status.get(); }
    public void setStatus(String v) { status.set(v); }
    public StringProperty statusProperty() { return status; }

    public String getNotes() { return notes.get(); }
    public void setNotes(String v) { notes.set(v); }
    public StringProperty notesProperty() { return notes; }

    public int getStockAvailable() { return stockAvailable.get(); }
    public void setStockAvailable(int v) { stockAvailable.set(v); }
    public IntegerProperty stockAvailableProperty() { return stockAvailable; }

    public StringProperty diagnosisProperty() { return diagnosis; }
    public String getDiagnosis() { return diagnosis.get(); }
    public void setDiagnosis(String v) { diagnosis.set(v); }

    // ===== New structured fields (DB-backed) =====
    public IntegerProperty doseProperty() { return dose; }
    public int getDose() { return dose.get(); }
    public void setDose(int v) { dose.set(v); }

    public IntegerProperty freqPerDayProperty() { return freqPerDay; }
    public int getFreqPerDay() { return freqPerDay.get(); }
    public void setFreqPerDay(int v) { freqPerDay.set(v); }

    public StringProperty strengthProperty() { return strength; }
    public String getStrength() { return strength.get(); }
    public void setStrength(String v) { strength.set(v); }

    public StringProperty formProperty() { return form; }
    public String getForm() { return form.get(); }
    public void setForm(String v) { form.set(v); }

    public StringProperty routeProperty() { return route; }
    public String getRoute() { return route.get(); }
    public void setRoute(String v) { route.set(v); }

    public StringProperty dosageTextProperty() { return dosageText; }
    public String getDosageText() { return dosageText.get(); }
    public void setDosageText(String v) { dosageText.set(v); }

//    public Integer getQtyUnitsRequested() { return qtyUnitsRequested; }
//    public void setQtyUnitsRequested(Integer v) { this.qtyUnitsRequested = v; }
//
//    public PackUnit getSuggestedUnit() { return suggestedUnit; }
//    public void setSuggestedUnit(PackUnit v) { this.suggestedUnit = v; }
//
//    public Integer getSuggestedCount() { return suggestedCount; }
//    public void setSuggestedCount(Integer v) { this.suggestedCount = v; }
//
//    public Integer getSuggestedUnitsTotal() { return suggestedUnitsTotal; }
//    public void setSuggestedUnitsTotal(Integer v) { this.suggestedUnitsTotal = v; }

    public Integer getQtyUnitsRequested() { return qtyUnitsRequested.get(); }
    public void setQtyUnitsRequested(Integer v) { qtyUnitsRequested.set(v); }
    public ObjectProperty<Integer> qtyUnitsRequestedProperty() { return qtyUnitsRequested; }

    public PackUnit getSuggestedUnit() { return suggestedUnit.get(); }
    public void setSuggestedUnit(PackUnit v) { suggestedUnit.set(v); recomputePack(); }
    public ObjectProperty<PackUnit> suggestedUnitProperty() { return suggestedUnit; }

    public Integer getSuggestedCount() { return suggestedCount.get(); }
    public void setSuggestedCount(Integer v) { suggestedCount.set(v); recomputePack(); }
    public ObjectProperty<Integer> suggestedCountProperty() { return suggestedCount; }

    public Integer getSuggestedUnitsTotal() { return suggestedUnitsTotal.get(); }
    public void setSuggestedUnitsTotal(Integer v) { suggestedUnitsTotal.set(v); recomputePack(); }
    public ObjectProperty<Integer> suggestedUnitsTotalProperty() { return suggestedUnitsTotal; }

    // ===== Approved packaging (set by Pharmacist) =====
    public PackUnit getApprovedUnit() { return approvedUnit; }
    public void setApprovedUnit(PackUnit v) { this.approvedUnit = v; recomputePack(); }

    public Integer getApprovedCount() { return approvedCount; }
    public void setApprovedCount(Integer v) { this.approvedCount = v; recomputePack(); }

    public Integer getApprovedUnitsTotal() { return approvedUnitsTotal; }
    public void setApprovedUnitsTotal(Integer v) { this.approvedUnitsTotal = v; recomputePack(); }

}