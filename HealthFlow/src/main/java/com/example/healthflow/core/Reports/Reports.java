package com.example.healthflow.core.Reports;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public final class Reports {

    private Reports() {}

    // ---------- DTOs ----------
    public record NearExpiryRow(String medicine, String batchNo, int qty, LocalDate expiry, int daysLeft) {}
    public record LowStockRow(String medicine, int available, int threshold) {}

    private static final ZoneId APP_TZ = ZoneId.systemDefault();

    // ---------- Loads: Tables ----------
    /** أدوية تنتهي خلال X يوم (دفعات ذات رصيد فعلي > 0) */
    public static ObservableList<NearExpiryRow> loadNearExpiry(Connection c, int daysAhead) throws SQLException {
        String sql =
                "WITH bal AS (" +
                        "  SELECT b.id, b.batch_no, b.expiry_date, m.display_name," +
                        "         COALESCE(SUM(t.qty_change),0) AS balance " +
                        "  FROM medicine_batches b " +
                        "  JOIN medicines m ON m.id = b.medicine_id " +
                        "  LEFT JOIN inventory_transactions t ON t.batch_id = b.id " +
                        "  GROUP BY b.id, b.batch_no, b.expiry_date, m.display_name" +
                        ") " +
                        "SELECT display_name, batch_no, balance, expiry_date " +
                        "FROM bal " +
                        "WHERE expiry_date >= CURRENT_DATE " +
                        "  AND expiry_date <= CURRENT_DATE + (? || ' days')::interval " +
                        "  AND balance > 0 " +
                        "ORDER BY expiry_date ASC";

        ObservableList<NearExpiryRow> rows = FXCollections.observableArrayList();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, daysAhead);
            try (ResultSet rs = ps.executeQuery()) {
                LocalDate today = LocalDate.now(APP_TZ);
                while (rs.next()) {
                    String med = rs.getString(1);
                    String batch = rs.getString(2);
                    int qty = rs.getInt(3);
                    LocalDate expiry = rs.getDate(4).toLocalDate();
                    int daysLeft = (int) ChronoUnit.DAYS.between(today, expiry);
                    rows.add(new NearExpiryRow(med, batch, qty, expiry, daysLeft));
                }
            }
        }
        return rows;
    }

    /** أدوية رصيدها ≤ حد إعادة الطلب */
    public static ObservableList<LowStockRow> loadLowStock(Connection c) throws SQLException {
        String sql =
                "WITH inv AS (" +
                        "  SELECT m.display_name, m.reorder_threshold, " +
                        "         COALESCE((SELECT SUM(t.qty_change) FROM inventory_transactions t WHERE t.medicine_id = m.id),0) AS available " +
                        "  FROM medicines m" +
                        ") " +
                        "SELECT display_name, available, reorder_threshold " +
                        "FROM inv " +
                        "WHERE available <= reorder_threshold " +
                        "ORDER BY available ASC, display_name ASC";

        ObservableList<LowStockRow> rows = FXCollections.observableArrayList();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new LowStockRow(rs.getString(1), rs.getInt(2), rs.getInt(3)));
            }
        }
        return rows;
    }

    // ---------- Loads: Charts ----------
    /** وحدات مصروفة أسبوعيًا في نطاق شهر محدد */
    public static XYChart.Series<String, Number> loadDispensedPerWeek(Connection c, LocalDate monthStart) throws SQLException {
        LocalDate from = monthStart.withDayOfMonth(1);
        LocalDate to = from.plusMonths(1);

        String sql =
                "SELECT (1 + ((EXTRACT(DAY FROM p.dispensed_at)::int - 1)/7)) AS wk_in_month, " +
                        "       COALESCE(SUM(i.qty_dispensed),0) AS units " +
                        "FROM prescriptions p " +
                        "JOIN prescription_items i ON i.prescription_id = p.id " +
                        "WHERE p.dispensed_at >= ? AND p.dispensed_at < ? " +   // حدود الشهر [start, nextMonth)
                        "GROUP BY wk_in_month " +
                        "ORDER BY wk_in_month";
//                "SELECT to_char(p.dispensed_at, 'IW') AS wk, COALESCE(SUM(i.qty_dispensed),0) AS units " +
//                        "FROM prescriptions p " +
//                        "JOIN prescription_items i ON i.prescription_id = p.id " +
//                        "WHERE p.dispensed_at >= ? AND p.dispensed_at < ? " +
//                        "GROUP BY wk ORDER BY wk";

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String label = "W" + rs.getInt(1);     // 1..5 داخل الشهر
                    s.getData().add(new XYChart.Data<>(label, rs.getInt(2)));
                }
            }
        }
        return s;
    }

    /** مرضى جدد أسبوعيًا في نطاق شهر محدد */
    public static XYChart.Series<String, Number> loadNewPatientsPerWeek(Connection c, LocalDate monthStart) throws SQLException {
        LocalDate from = monthStart.withDayOfMonth(1);
        LocalDate to = from.plusMonths(1);

//        String sql =
//                "SELECT to_char(appointment_date, 'IW') AS wk, COUNT(DISTINCT patient_id) AS c " +
//                        "FROM appointments " +
//                        "WHERE appointment_date >= ? AND appointment_date < ? " +
//                        "GROUP BY wk ORDER BY wk";

        String sql =
                "SELECT (1 + ((EXTRACT(DAY FROM appointment_date)::int - 1)/7)) AS wk_in_month, " +
                        "       COUNT(DISTINCT patient_id) AS c " +
                        "FROM appointments " +
                        "WHERE appointment_date >= ? AND appointment_date < ? " +
                        "GROUP BY wk_in_month " +
                        "ORDER BY wk_in_month";

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
//                while (rs.next()) s.getData().add(new XYChart.Data<>("W"+rs.getString(1), rs.getInt(2)));
                while (rs.next()) {
                    String label = "W" + rs.getInt(1);
                    s.getData().add(new XYChart.Data<>(label, rs.getInt(2)));
                }
            }
        }
        return s;
    }

    // ---------- Export ----------
    /** تصدير محتوى الجدولين إلى CSV بسيط */
    public static Path exportCsv(Path dir, ObservableList<NearExpiryRow> near, ObservableList<LowStockRow> low) throws Exception {
        if (dir == null) dir = Path.of(System.getProperty("user.home"));
        Path file = dir.resolve("pharmacy-reports-" + LocalDate.now(APP_TZ) + ".csv");
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("=== Medicines Near Expiry (<=30 days) ===\n");
            w.write("Medicine,Batch,Qty,Expiry,DaysLeft\n");
            for (NearExpiryRow r: near)
                w.write(escape(r.medicine()) + "," + escape(r.batchNo()) + "," + r.qty() + "," + r.expiry() + "," + r.daysLeft() + "\n");

            w.write("\n=== Low Stock (at/below reorder) ===\n");
            w.write("Medicine,Available,ReorderThreshold\n");
            for (LowStockRow r: low)
                w.write(escape(r.medicine()) + "," + r.available() + "," + r.threshold() + "\n");
        }
        return file;
    }

    private static String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"","\"\"") + "\"";
        }
        return s;
    }

    /**
     * يصدّر تقريرين إلى ملف Excel منسّق (ورقتان):
     *  - Near Expiry
     *  - Low Stock
     * يعيد المسار الناتج (اسم تلقائي بالوقت).
     */
    public static java.nio.file.Path exportXlsx(java.nio.file.Path dir,
                                                java.util.List<NearExpiryRow> near,
                                                java.util.List<LowStockRow> low) throws Exception {
        String base = "pharmacy_reports_" + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        java.nio.file.Path out = dir.resolve(base + ".xlsx");

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            // ===== Styles =====
            org.apache.poi.ss.usermodel.Font titleFont = wb.createFont();
            titleFont.setBold(true); titleFont.setFontHeightInPoints((short)12);
            org.apache.poi.ss.usermodel.CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);

            org.apache.poi.ss.usermodel.Font headFont = wb.createFont();
            headFont.setBold(true);
            org.apache.poi.ss.usermodel.CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            org.apache.poi.ss.usermodel.CellStyle cellStyle = wb.createCellStyle();
            cellStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            cellStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            cellStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            cellStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            org.apache.poi.ss.usermodel.CellStyle centerStyle = wb.createCellStyle();
            centerStyle.cloneStyleFrom(cellStyle);
            centerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            // ===== Sheet 1: Near Expiry =====
            org.apache.poi.ss.usermodel.Sheet s1 = wb.createSheet("Near Expiry");
            int r = 0;
            org.apache.poi.ss.usermodel.Row t1 = s1.createRow(r++);
            org.apache.poi.ss.usermodel.Cell t1c = t1.createCell(0);
            t1c.setCellValue("=== Medicines Near Expiry (<=30 days) ===");
            t1c.setCellStyle(titleStyle);

            String[] h1Cols = {"Medicine","Batch","Qty","Expiry","DaysLeft"};
            org.apache.poi.ss.usermodel.Row h1 = s1.createRow(r++);
            for (int i = 0; i < h1Cols.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = h1.createCell(i);
                c.setCellValue(h1Cols[i]);
                c.setCellStyle(headerStyle);
            }
            if (near != null) {
                for (NearExpiryRow row : near) {
                    org.apache.poi.ss.usermodel.Row rr = s1.createRow(r++);
                    int c = 0;
                    org.apache.poi.ss.usermodel.Cell c0 = rr.createCell(c++); c0.setCellValue(nz(row.medicine())); c0.setCellStyle(cellStyle);
                    org.apache.poi.ss.usermodel.Cell c1 = rr.createCell(c++); c1.setCellValue(nz(row.batchNo()));  c1.setCellStyle(cellStyle);
                    org.apache.poi.ss.usermodel.Cell c2 = rr.createCell(c++); c2.setCellValue(row.qty());          c2.setCellStyle(centerStyle);
                    org.apache.poi.ss.usermodel.Cell c3 = rr.createCell(c++); c3.setCellValue(row.expiry()==null?"":row.expiry().toString()); c3.setCellStyle(centerStyle);
                    org.apache.poi.ss.usermodel.Cell c4 = rr.createCell(c++); c4.setCellValue(row.daysLeft());     c4.setCellStyle(centerStyle);
                }
            }
            for (int i = 0; i < h1Cols.length; i++) s1.autoSizeColumn(i);

            // ===== Sheet 2: Low Stock =====
            org.apache.poi.ss.usermodel.Sheet s2 = wb.createSheet("Low Stock");
            r = 0;
            org.apache.poi.ss.usermodel.Row t2 = s2.createRow(r++);
            org.apache.poi.ss.usermodel.Cell t2c = t2.createCell(0);
            t2c.setCellValue("=== Low Stock (at/below reorder) ===");
            t2c.setCellStyle(titleStyle);

            String[] h2Cols = {"Medicine","Available","ReorderThreshold"};
            org.apache.poi.ss.usermodel.Row h2 = s2.createRow(r++);
            for (int i = 0; i < h2Cols.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = h2.createCell(i);
                c.setCellValue(h2Cols[i]);
                c.setCellStyle(headerStyle);
            }
            if (low != null) {
                for (LowStockRow row : low) {
                    org.apache.poi.ss.usermodel.Row rr = s2.createRow(r++);
                    int c = 0;
                    org.apache.poi.ss.usermodel.Cell c0 = rr.createCell(c++); c0.setCellValue(nz(row.medicine())); c0.setCellStyle(cellStyle);
                    org.apache.poi.ss.usermodel.Cell c1 = rr.createCell(c++); c1.setCellValue(row.available());    c1.setCellStyle(centerStyle);
                    org.apache.poi.ss.usermodel.Cell c2 = rr.createCell(c++); c2.setCellValue(row.threshold());    c2.setCellStyle(centerStyle);
                }
            }
            for (int i = 0; i < h2Cols.length; i++) s2.autoSizeColumn(i);

            // اكتب الملف
            try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(out)) {
                wb.write(os);
            }
        }
        return out;
    }

    private static String nz(String s) { return (s == null ? "" : s); }
}