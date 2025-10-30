package com.example.healthflow.core.packaging;

public class MedicinePackaging {
    public MedUnit baseUnit;           // TABLET/CAPSULE/...
    public Integer tabletsPerBlister;  // قد تكون null
    public Integer blistersPerBox;     // قد تكون null
    public Integer mlPerBottle;
    public Integer gramsPerTube;
    public Boolean splitAllowed;
}
