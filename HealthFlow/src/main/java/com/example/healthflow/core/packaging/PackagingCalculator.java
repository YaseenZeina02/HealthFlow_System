package com.example.healthflow.core.packaging;

public final class PackagingCalculator {
    private PackagingCalculator() {}

    /** جرعة × مرات/اليوم × مدة (أيام) */
    public static int computeUnitsRequested(int dose, int freqPerDay, int durationDays) {
        long v = (long) dose * (long) freqPerDay * (long) durationDays;
        return (v <= 0) ? 0 : (v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) v);
    }

    /** يقترح عبوة مناسبة حسب بيانات التغليف. قد يعيد null لو لا تتوفر معلومات كافية. */
    public static PackagingSuggestion suggest(MedicinePackaging p, int unitsRequested) {
        if (p == null || unitsRequested <= 0) return null;
        PackagingSuggestion s = new PackagingSuggestion();

        switch (p.baseUnit) {
            case TABLET, CAPSULE -> {
                Integer perBlister = p.tabletsPerBlister;
                Integer perBox = (p.tabletsPerBlister != null && p.blistersPerBox != null)
                        ? p.tabletsPerBlister * p.blistersPerBox : null;

                if (Boolean.TRUE.equals(p.splitAllowed) && perBlister != null) {
                    int count = (int) Math.ceil(unitsRequested / (double) perBlister);
                    s.unit = PackUnit.BLISTER; s.count = count; s.unitsTotal = count * perBlister;
                    s.label = count + " BLISTER = " + s.unitsTotal;
                    return s;
                }
                if (perBox != null) {
                    int count = (int) Math.ceil(unitsRequested / (double) perBox);
                    s.unit = PackUnit.BOX; s.count = count; s.unitsTotal = count * perBox;
                    s.label = count + " BOX = " + s.unitsTotal;
                    return s;
                }
                // fallback
                s.unit = PackUnit.UNIT; s.count = unitsRequested; s.unitsTotal = unitsRequested;
                s.label = unitsRequested + " UNIT";
                return s;
            }
            case SYRUP, SUSPENSION -> {
                if (p.mlPerBottle != null) {
                    int count = (int) Math.ceil(unitsRequested / (double) p.mlPerBottle);
                    s.unit = PackUnit.BOTTLE; s.count = count; s.unitsTotal = count * p.mlPerBottle;
                    s.label = count + " BOTTLE = " + s.unitsTotal + " ml";
                    return s;
                }
            }
            case CREAM, OINTMENT -> {
                if (p.gramsPerTube != null) {
                    int count = (int) Math.ceil(unitsRequested / (double) p.gramsPerTube);
                    s.unit = PackUnit.TUBE; s.count = count; s.unitsTotal = count * p.gramsPerTube;
                    s.label = count + " TUBE = " + s.unitsTotal + " g";
                    return s;
                }
            }
            default -> { /* INJECTION/DROPS… لاحقًا */ }
        }
        return null;
    }

    /** نص مختصر لعمود Quantity: "14 (2 BLISTER = 20)" */
    public static String formatQuantityCell(int unitsRequested, PackagingSuggestion s) {
        if (unitsRequested <= 0) return "0";
        return (s == null) ? String.valueOf(unitsRequested)
                : unitsRequested + " (" + s.label + ")";
    }
}