package com.example.healthflow.core.time;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class AgeText {
    private AgeText() {}

    /**
     * تنسيق العمر حسب القاعدة الجديدة:
     * إذا كان العمر سنة أو أكثر → عرض بالسنوات فقط (مثال: "5y")
     * إذا كان أقل من سنة وأكبر أو يساوي شهر → عرض بالأشهر فقط (مثال: "10m")
     * إذا كان أقل من شهر → عرض بالأيام فقط (مثال: "15d")
     */
    public static String format(LocalDate dob) {
        if (dob == null) return "—";
        LocalDate now = LocalDate.now();
        if (dob.isAfter(now)) return "—";

        long years = ChronoUnit.YEARS.between(dob, now);
        if (years >= 1) {
            return years + " y";
        }

        long months = ChronoUnit.MONTHS.between(dob, now);
        if (months >= 1) {
            return months + " m";
        }

        long days = ChronoUnit.DAYS.between(dob, now);
        return days + " d";
    }
}