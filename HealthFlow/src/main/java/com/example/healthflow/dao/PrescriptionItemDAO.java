package com.example.healthflow.dao;

import com.example.healthflow.core.packaging.MedicinePackaging;
import com.example.healthflow.core.packaging.PackagingCalculator;
import com.example.healthflow.core.packaging.PackagingSuggestion;
import com.example.healthflow.model.ItemStatus;
import com.example.healthflow.model.PrescriptionItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.example.healthflow.core.packaging.MedUnit;

public class PrescriptionItemDAO {

    public PrescriptionItem addItem(Connection c, Long prescriptionId, Long medicineId,
                                    String medicineName, Integer dose, Integer freqPerDay,
                                    Integer durationDays, String strength, String form,
                                    String route, String notes, int qty) throws SQLException
    {
        final String sql = """
        INSERT INTO prescription_items (
            prescription_id, medicine_id, medicine_name,
            quantity, dose, freq_per_day, duration_days,
            strength, form, route, notes, qty_units_requested,
            suggested_unit, suggested_count, suggested_units_total
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::pack_unit, ?, ?)
        RETURNING *
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, prescriptionId);
            if (medicineId == null) ps.setNull(2, Types.BIGINT); else ps.setLong(2, medicineId);
            if (medicineName == null || medicineName.isBlank()) ps.setNull(3, Types.VARCHAR); else ps.setString(3, medicineName);
            ps.setInt(4, qty);
            if (dose == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, dose);
            if (freqPerDay == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, freqPerDay);
            if (durationDays == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, durationDays);
            if (strength == null || strength.isBlank()) ps.setNull(8, Types.VARCHAR); else ps.setString(8, strength);
            if (form == null || form.isBlank()) ps.setNull(9, Types.VARCHAR); else ps.setString(9, form);
            if (route == null || route.isBlank()) ps.setNull(10, Types.VARCHAR); else ps.setString(10, route);
            if (notes == null || notes.isBlank()) ps.setNull(11, Types.VARCHAR); else ps.setString(11, notes);

            // qty_units_requested = dose * freq/day * duration (nullable-safe)
            Integer unitsRequested = null;
            if (dose != null && freqPerDay != null && durationDays != null) {
                long v = 1L * dose * freqPerDay * durationDays;
                if (v > 0L && v <= Integer.MAX_VALUE) unitsRequested = (int) v;
            }
            if (unitsRequested == null) ps.setNull(12, Types.INTEGER); else ps.setInt(12, unitsRequested);

            // Packaging suggestion (pkg, units)
            PackagingSuggestion sug = null;
            if (medicineId != null && unitsRequested != null) {
                MedicinePackaging pkg = loadPackaging(c, medicineId);
                if (pkg != null && pkg.baseUnit != null) {            // <-- أضف هذا الشرط
                    sug = PackagingCalculator.suggest(pkg, unitsRequested);
                }
            }

            if (sug == null || sug.unit == null) {
                ps.setNull(13, java.sql.Types.VARCHAR);
                ps.setNull(14, java.sql.Types.INTEGER);
                ps.setNull(15, java.sql.Types.INTEGER);
            } else {
                final String unitName = String.valueOf(sug.unit);
                ps.setString(13, unitName);
                ps.setInt(14, sug.count);
                ps.setInt(15, sug.unitsTotal);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        throw new SQLException("Failed to add item");
    }
    public boolean updateDispensed(Connection c, Long itemId, int qtyDispensed, ItemStatus status, Long batchId) throws SQLException {
        final String sql = """
        UPDATE prescription_items
           SET qty_dispensed = ?, status = ?::item_status2, batch_id = ?
         WHERE id = ?
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, qtyDispensed);
            ps.setString(2, status.name());
            if (batchId == null) ps.setNull(3, Types.BIGINT); else ps.setLong(3, batchId);
            ps.setLong(4, itemId);
            return ps.executeUpdate() == 1;
        }
    }
    public List<PrescriptionItem> listByPrescription(Connection c, Long prescriptionId) throws SQLException {
        final String sql = """
            SELECT id, prescription_id, medicine_id, medicine_name,
                          dosage,
                          quantity, qty_dispensed, status, batch_id,
                          dose, freq_per_day, duration_days, strength, form, route, notes, dosage_text,
                          qty_units_requested,
                          suggested_unit::text   AS suggested_unit,
                          suggested_count,
                          suggested_units_total,
                          approved_unit::text    AS approved_unit,
                          approved_count,
                          approved_units_total
                   FROM prescription_items
                   WHERE prescription_id = ?
                   ORDER BY id
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, prescriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<PrescriptionItem> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }



    private PrescriptionItem map(ResultSet rs) throws SQLException {
        PrescriptionItem it = new PrescriptionItem();
        it.setId(rs.getLong("id"));
        it.setPrescriptionId(rs.getLong("prescription_id"));
        it.setMedicineId((Long) rs.getObject("medicine_id"));
        it.setMedicineName(rs.getString("medicine_name"));
        it.setDosage(rs.getString("dosage"));
        it.setQuantity(rs.getInt("quantity"));
        it.setQtyDispensed(rs.getInt("qty_dispensed"));
        it.setStatus(ItemStatus.fromString(rs.getString("status")));
        it.setBatchId((Long) rs.getObject("batch_id"));
        it.setDose((Integer) rs.getObject("dose"));
        it.setFreqPerDay((Integer) rs.getObject("freq_per_day"));
        it.setDurationDays((Integer) rs.getObject("duration_days"));
        it.setStrength(rs.getString("strength"));
        it.setForm(rs.getString("form"));
        it.setRoute(rs.getString("route"));
        it.setNotes(rs.getString("notes"));
        it.setDosageText(rs.getString("dosage_text"));
        if (hasColumn(rs, "qty_units_requested"))
            it.setQtyUnitsRequested(getIntOrNull(rs, "qty_units_requested"));
        it.setSuggestedUnit(getStrOrNull(rs, "suggested_unit"));
        it.setSuggestedCount(getIntOrNull(rs, "suggested_count"));
        it.setSuggestedUnitsTotal(getIntOrNull(rs, "suggested_units_total"));
        it.setApprovedUnit(getStrOrNull(rs, "approved_unit"));
        it.setApprovedCount(getIntOrNull(rs, "approved_count"));
        it.setApprovedUnitsTotal(getIntOrNull(rs, "approved_units_total"));
        return it;
    }

    private static boolean hasColumn(ResultSet rs, String name) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (name.equalsIgnoreCase(md.getColumnLabel(i))) return true;
        }
        return false;
    }
    private static Integer getIntOrNull(ResultSet rs, String col) {
        try { Object o = rs.getObject(col); return (o == null) ? null : ((Number)o).intValue(); }
        catch (SQLException e) { return null; }
    }
    private static String getStrOrNull(ResultSet rs, String col) {
        try { return hasColumn(rs, col) ? rs.getString(col) : null; }
        catch (SQLException e) { return null; }
    }

    /** Insert many items (loops with RETURNING to preserve mapping). */
    public List<PrescriptionItem> addItems(Connection c, Long prescriptionId, List<PrescriptionItem> items) throws SQLException {
        List<PrescriptionItem> out = new ArrayList<>();
        if (items == null || items.isEmpty()) return out;
        for (PrescriptionItem it : items) {
            out.add(addItem(c, prescriptionId,
                    it.getMedicineId(),
                    it.getMedicineName(),
                    it.getDose(),
                    it.getFreqPerDay(),
                    it.getDurationDays(),
                    it.getStrength(),
                    it.getForm(),
                    it.getRoute(),
                    it.getNotes(),
                    it.getQuantity()
            ));
        }
        return out;
    }

    /** Delete one item by id. */
    public boolean deleteById(Connection c, Long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM prescription_items WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    /** Delete all items for a prescription (useful when doctor edits draft before sending). */
    public int deleteByPrescription(Connection c, Long prescriptionId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM prescription_items WHERE prescription_id = ?")) {
            ps.setLong(1, prescriptionId);
            return ps.executeUpdate();
        }
    }

    /** Items + current stock (read-only helper for doctor view). */
    public List<PrescriptionItem> listByPrescriptionWithStock(Connection c, Long prescriptionId) throws SQLException {
        final String sql = """
                SELECT i.*, m.available_quantity
                FROM prescription_items i
                LEFT JOIN medicines m ON m.id = i.medicine_id
                WHERE i.prescription_id = ?
                ORDER BY i.id
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, prescriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<PrescriptionItem> out = new ArrayList<>();
                while (rs.next()) {
                    PrescriptionItem it = map(rs);
                    // if your model has a setter for stock, set it; otherwise ignore
                    try {
                        var fld = PrescriptionItem.class.getMethod("setStockAvailable", int.class);
                        fld.invoke(it, rs.getInt("available_quantity"));
                    } catch (Throwable ignore) {}
                    out.add(it);
                }
                return out;
            }
        }
    }

    /** Update core editable fields of a prescription item and return the updated row. */
    public PrescriptionItem updateItem(Connection c, long id, Long medicineId,
                                       String medicineName, Integer dose, Integer freqPerDay,
                                       Integer durationDays, String strength, String form,
                                       String route, String notes, int qty) throws SQLException {
        final String sql = """
        UPDATE prescription_items
        SET medicine_id = ?, medicine_name = ?,
            dose = ?, freq_per_day = ?, duration_days = ?,
            strength = ?, form = ?, route = ?, notes = ?,
            quantity = ?, qty_units_requested = ?,
            suggested_unit = ?::pack_unit, suggested_count = ?, suggested_units_total = ?
        WHERE id = ?
        RETURNING *
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (medicineId == null) ps.setNull(1, Types.BIGINT); else ps.setLong(1, medicineId);
            if (medicineName == null || medicineName.isBlank()) ps.setNull(2, Types.VARCHAR); else ps.setString(2, medicineName);
            if (dose == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, dose);
            if (freqPerDay == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, freqPerDay);
            if (durationDays == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, durationDays);
            if (strength == null || strength.isBlank()) ps.setNull(6, Types.VARCHAR); else ps.setString(6, strength);
            if (form == null || form.isBlank()) ps.setNull(7, Types.VARCHAR); else ps.setString(7, form);
            if (route == null || route.isBlank()) ps.setNull(8, Types.VARCHAR); else ps.setString(8, route);
            if (notes == null || notes.isBlank()) ps.setNull(9, Types.VARCHAR); else ps.setString(9, notes);
            ps.setInt(10, qty);

            Integer unitsRequested = null;
            if (dose != null && freqPerDay != null && durationDays != null) {
                long v = 1L * dose * freqPerDay * durationDays;
                if (v > 0L && v <= Integer.MAX_VALUE) unitsRequested = (int) v;
            }
            if (unitsRequested == null) ps.setNull(11, Types.INTEGER); else ps.setInt(11, unitsRequested);

            PackagingSuggestion sug = null;
            if (medicineId != null && unitsRequested != null) {
                MedicinePackaging pkg = loadPackaging(c, medicineId);
                if (pkg != null && pkg.baseUnit != null) {            // <-- أضف هذا الشرط
                    sug = PackagingCalculator.suggest(pkg, unitsRequested);
                }
            }

            if (sug == null || sug.unit == null) {
                ps.setNull(12, java.sql.Types.VARCHAR);
                ps.setNull(13, java.sql.Types.INTEGER);
                ps.setNull(14, java.sql.Types.INTEGER);
            } else {
                final String unitName = String.valueOf(sug.unit);
                ps.setString(12, unitName);
                ps.setInt(13, sug.count);
                ps.setInt(14, sug.unitsTotal);
            }

            ps.setLong(15, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        throw new SQLException("Failed to update prescription item id=" + id);
    }

    /** Load packaging info from medicines for a given medicine id. */
    private MedicinePackaging loadPackaging(Connection c, long medicineId) throws SQLException {
        final String q = """
            SELECT base_unit::text AS base_unit,
                   tablets_per_blister,
                   blisters_per_box,
                   ml_per_bottle,
                   grams_per_tube,
                   split_allowed
            FROM medicines
            WHERE id = ?
            """;
        try (PreparedStatement ps = c.prepareStatement(q)) {
            ps.setLong(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                MedicinePackaging p = new MedicinePackaging();
                String bu = rs.getString("base_unit");
                p.baseUnit = (bu == null || bu.isBlank()) ? null : MedUnit.valueOf(bu);
                p.tabletsPerBlister = (Integer) rs.getObject("tablets_per_blister");
                p.blistersPerBox    = (Integer) rs.getObject("blisters_per_box");
                p.mlPerBottle       = (Integer) rs.getObject("ml_per_bottle");
                p.gramsPerTube      = (Integer) rs.getObject("grams_per_tube");
                p.splitAllowed      = (Boolean) rs.getObject("split_allowed");
                return p;
            }
        }
    }

    /** Optional: backfill suggestions for existing rows where suggestion is null. */
    public int backfillSuggestions(Connection c, long prescriptionId) throws SQLException {
        final String sel = """
        SELECT id, medicine_id, qty_units_requested
        FROM prescription_items
        WHERE prescription_id = ?
          AND suggested_unit IS NULL
          AND medicine_id IS NOT NULL
          AND qty_units_requested IS NOT NULL
        """;
        final String upd = """
        UPDATE prescription_items
           SET suggested_unit = ?::pack_unit, suggested_count = ?, suggested_units_total = ?
         WHERE id = ?
        """;
        int updated = 0;
        try (PreparedStatement ps = c.prepareStatement(sel);
             PreparedStatement pu = c.prepareStatement(upd)) {
            ps.setLong(1, prescriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    Long medId = (Long) rs.getObject("medicine_id");
                    Integer units = (Integer) rs.getObject("qty_units_requested");
                    if (medId == null || units == null) continue;
                    MedicinePackaging pkg = loadPackaging(c, medId);
                    if (pkg == null || pkg.baseUnit == null) continue;       // <-- تجاهَل لو ناقص
                    PackagingSuggestion sug = PackagingCalculator.suggest(pkg, units);
                    if (sug != null && sug.unit != null) {
                        final String unitName = String.valueOf(sug.unit);
                        pu.setString(1, unitName);
                        pu.setInt(2, sug.count);
                        pu.setInt(3, sug.unitsTotal);
                        pu.setLong(4, id);
                        updated += pu.executeUpdate();
                    }
                }
            }
        }

        return updated;
    }
}