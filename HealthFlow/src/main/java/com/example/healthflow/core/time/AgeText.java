package com.example.healthflow.core.time;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class AgeText {
    private AgeText() {}

    /** تنسيق عمر عملي:
     *  0 يوم  -> "Newborn"
     *  < 7 أيام -> "Xd"
     *  < 2 أشهر -> "Ww Dd" (أسابيع + أيام)
     *  < 24 شهرًا -> "Xm Yd" (أشهر + أيام)
     *  < 18 سنة  -> "Yy Mm" (سنوات + أشهر)
     *  خلاف ذلك  -> "Yy"
     */
    public static String format(LocalDate dob) {
        if (dob == null) return "—";
        LocalDate now = LocalDate.now();
        if (dob.isAfter(now)) return "—";

        long days = ChronoUnit.DAYS.between(dob, now);
        if (days == 0) return "Newborn";
        if (days < 7)  return days + "d";

        if (days < 62) { // تقريبًا أقل من شهرين
            long weeks = days / 7;
            long rem   = days % 7;
            return rem > 0 ? (weeks + "w " + rem + "d") : (weeks + " Week");
        }

        long months = ChronoUnit.MONTHS.between(dob, now);
        if (months < 24) {
            long remDays = ChronoUnit.DAYS.between(dob.plusMonths(months), now);
            return remDays > 0 ? (months + "m " + remDays + "d") : (months + "m");
        }

        long years = ChronoUnit.YEARS.between(dob, now);
        long remMonths = ChronoUnit.MONTHS.between(dob.plusYears(years), now);
        if (years < 18 && remMonths > 0) {
            return years + "y " + remMonths + "m";
        }
        return years + "y";
    }
}