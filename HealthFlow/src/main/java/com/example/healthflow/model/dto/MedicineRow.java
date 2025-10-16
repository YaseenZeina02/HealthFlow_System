package com.example.healthflow.model.dto;

import javafx.beans.property.*;

public class MedicineRow {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty form = new SimpleStringProperty();       // Tablet / Capsule / Syrup ...
    private final StringProperty strength = new SimpleStringProperty();   // e.g. 500 mg
    private final StringProperty route = new SimpleStringProperty();      // Oral / IV / IM ...
    private final IntegerProperty availableQuantity = new SimpleIntegerProperty(0);

    public MedicineRow() {}

    public MedicineRow(long id, String name) {
        setId(id);
        setName(name);
    }

    public MedicineRow(long id, String name, String form, String strength, String route, int availableQuantity) {
        setId(id);
        setName(name);
        setForm(form);
        setStrength(strength);
        setRoute(route);
        setAvailableQuantity(availableQuantity);
    }

    // ===== id =====
    public long getId() { return id.get(); }
    public void setId(long v) { id.set(v); }
    public LongProperty idProperty() { return id; }

    // ===== name =====
    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
    public StringProperty nameProperty() { return name; }

    // ===== form =====
    public String getForm() { return form.get(); }
    public void setForm(String v) { form.set(v); }
    public StringProperty formProperty() { return form; }

    // ===== strength =====
    public String getStrength() { return strength.get(); }
    public void setStrength(String v) { strength.set(v); }
    public StringProperty strengthProperty() { return strength; }

    // ===== route =====
    public String getRoute() { return route.get(); }
    public void setRoute(String v) { route.set(v); }
    public StringProperty routeProperty() { return route; }

    // ===== availableQuantity =====
    public int getAvailableQuantity() { return availableQuantity.get(); }
    public void setAvailableQuantity(int v) { availableQuantity.set(v); }
    public IntegerProperty availableQuantityProperty() { return availableQuantity; }
}