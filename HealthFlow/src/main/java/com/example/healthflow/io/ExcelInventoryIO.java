package com.example.healthflow.io;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * قراءة/كتابة ملفات إكسل الخاصة بالاستلامات (Receive Medicines).
 * الكلاس مستقل ويمكن استخدامه لاحقاً للتصدير والاستيراد من أي شاشة.
 */
public final class ExcelInventoryIO {

    private ExcelInventoryIO() {}

    /** ترتيب الأعمدة في القالب */
    public enum Col {
        MEDICINE_NAME(0, "Medicine Name*"),
        STRENGTH(1, "Strength (optional)"),
        FORM(2, "Form (optional)"),
        BASE_UNIT(3, "Base Unit (optional)"),
        QUANTITY(4, "Quantity*"),
        BATCH_NO(5, "Batch Number (optional)"),
        EXPIRY(6, "Expiry Date* (YYYY-MM-DD)"),
        DESCRIPTION(7, "Description (optional)");

        public final int idx;
        public final String header;
        Col(int idx, String header){ this.idx = idx; this.header = header; }
    }

    public static final class ReceiveRow {
        // من الملف (أو من الواجهة)
        private final String medicineName;
        private final String strength;
        private final String form;
        private final String baseUnit;
        private final Integer quantity;
        private final String batchNo;
        private final LocalDate expiry;       // نحتفظ به كما هو
        private final String description;

        // يُملأ لاحقاً أثناء الـ commit لو قدرنا نحلّه
        private Long medicineId;              // اختياري

        public ReceiveRow(String medicineName, String strength, String form, String baseUnit,
                          Integer quantity, String batchNo, LocalDate expiry, String description) {
            this.medicineName = medicineName;
            this.strength = strength;
            this.form = form;
            this.baseUnit = baseUnit;
            this.quantity = quantity;
            this.batchNo = batchNo;
            this.expiry = expiry;
            this.description = description;
        }

        public String getMedicineName()   { return medicineName; }
        public String getStrength()       { return strength; }
        public String getForm()           { return form; }
        public String getBaseUnit()       { return baseUnit; }
        public Integer getQuantity()      { return quantity; }
        public String getBatchNo()        { return batchNo; }
        public LocalDate getExpiry()      { return expiry; }
        public LocalDate getExpiryDate()  { return expiry; } // alias لاستخدام موحّد
        public String getDescription()    { return description; }

        public Long  getMedicineId()      { return medicineId; }
        public void  setMedicineId(Long v){ this.medicineId = v; }

        @Override
        public String toString() {
            return "ReceiveRow{" +
                    "medicineName='" + medicineName + '\'' +
                    ", strength='" + strength + '\'' +
                    ", form='" + form + '\'' +
                    ", baseUnit='" + baseUnit + '\'' +
                    ", quantity=" + quantity +
                    ", batchNo='" + batchNo + '\'' +
                    ", expiry=" + expiry +
                    ", description='" + description + '\'' +
                    ", medicineId=" + medicineId +
                    '}';
        }
    }


    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** يكتب ملف القالب في المسار المحدد (ينشئ المجلد إن لزم). */
    public static Path writeReceiveTemplate(Path directory) throws IOException {
        if (directory == null) throw new IllegalArgumentException("directory is null");
        Files.createDirectories(directory);

        Path out = directory.resolve("HealthFlow_ReceiveTemplate.xlsx");

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Receive");

            // نمط العنوان
            CellStyle hdr = wb.createCellStyle();
            var font = wb.createFont();
            font.setBold(true);
            hdr.setFont(font);

            // الصف 0: العناوين
            Row head = sh.createRow(0);
            for (Col c : Col.values()) {
                Cell cell = head.createCell(c.idx);
                cell.setCellValue(c.header);
                cell.setCellStyle(hdr);
                sh.setColumnWidth(c.idx, 26 * 256); // عرض لطيف
            }

            // صف توضيحي (مثال)
            Row ex = sh.createRow(1);
            ex.createCell(Col.MEDICINE_NAME.idx).setCellValue("Paracetamol");
            ex.createCell(Col.STRENGTH.idx).setCellValue("500 mg");
            ex.createCell(Col.FORM.idx).setCellValue("TABLET");
            ex.createCell(Col.BASE_UNIT.idx).setCellValue("TABLET");
            ex.createCell(Col.QUANTITY.idx).setCellValue(500);
            ex.createCell(Col.BATCH_NO.idx).setCellValue("AUTO-20250101-1");
            ex.createCell(Col.EXPIRY.idx).setCellValue("2026-12-31");
            ex.createCell(Col.DESCRIPTION.idx).setCellValue("donation");

            try (OutputStream os = Files.newOutputStream(out)) {
                wb.write(os);
            }
        }

        return out;
    }

    /** يقرأ ملف الإكسل ويعيد الصفوف الصالحة + يجمع الأخطاء بالتوازي. */
    public static Result readReceiveFile(File file) throws IOException {
        if (file == null) throw new IllegalArgumentException("file is null");
        List<ReceiveRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (InputStream in = new FileInputStream(file);
             Workbook wb = new XSSFWorkbook(in)) {

            Sheet sh = wb.getSheetAt(0);
            int last = sh.getLastRowNum();

            // بدءاً من الصف 1 (بعد العناوين)
            for (int i = 1; i <= last; i++) {
                Row r = sh.getRow(i);
                if (r == null) continue;

                String medicine = getString(r, Col.MEDICINE_NAME.idx);
                String strength = getString(r, Col.STRENGTH.idx);
                String form     = getString(r, Col.FORM.idx);
                String base     = getString(r, Col.BASE_UNIT.idx);
                Integer qty     = getInteger(r, Col.QUANTITY.idx);
                String batch    = getString(r, Col.BATCH_NO.idx);
                LocalDate exp   = getDate(r, Col.EXPIRY.idx);
                String desc     = getString(r, Col.DESCRIPTION.idx);

                // تخطي الصفوف الفارغة كليّاً
                boolean allBlank = (isBlank(medicine) && qty == null && isBlank(batch) && exp == null
                        && isBlank(strength) && isBlank(form) && isBlank(base) && isBlank(desc));
                if (allBlank) continue;

                // تحقق و تجميع رسائل الخطأ
                List<String> rowErr = new ArrayList<>();
                if (isBlank(medicine)) rowErr.add("Medicine Name is required");
                if (qty == null || qty <= 0) rowErr.add("Quantity must be positive");
                if (exp == null) rowErr.add("Expiry Date is required (YYYY-MM-DD)");

                if (!rowErr.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": " + String.join(", ", rowErr));
                    continue;
                }

                rows.add(new ReceiveRow(medicine, strength, form, base, qty, batch, exp, desc));
            }
        }

        return new Result(rows, errors);
    }

    // نتيجة القراءة: صفوف + أخطاء
    public static final class Result {
        public final List<ReceiveRow> rows;
        public final List<String> errors;
        public Result(List<ReceiveRow> rows, List<String> errors) {
            this.rows = rows; this.errors = errors;
        }
        public int okCount() { return rows == null ? 0 : rows.size(); }
        public boolean hasErrors() { return errors != null && !errors.isEmpty(); }
    }

    // ---------- Helpers ----------
    private static boolean isBlank(String s){ return s == null || s.trim().isEmpty(); }

    private static String getString(Row r, int idx){
        Cell c = r.getCell(idx); if (c == null) return null;
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue().trim();
        if (c.getCellType() == CellType.NUMERIC) {
            // لو كانت رقمية (مثلاً batch كأرقام) نحولها لنص بدون كسر
            double d = c.getNumericCellValue();
            long asLong = (long) d;
            if (Math.abs(d - asLong) < 1e-9) return Long.toString(asLong);
            return Double.toString(d);
        }
        if (c.getCellType() == CellType.BOOLEAN) return Boolean.toString(c.getBooleanCellValue());
        return null;
    }

    private static Integer getInteger(Row r, int idx){
        Cell c = r.getCell(idx); if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC) return (int)Math.round(c.getNumericCellValue());
        if (c.getCellType() == CellType.STRING) {
            try { return Integer.parseInt(c.getStringCellValue().trim()); } catch (Exception ignore) {}
        }
        return null;
    }

    private static LocalDate getDate(Row r, int idx){
        Cell c = r.getCell(idx); if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return c.getLocalDateTimeCellValue().toLocalDate();
        }
        if (c.getCellType() == CellType.STRING) {
            String s = c.getStringCellValue().trim();
            try { return LocalDate.parse(s, DF); } catch (Exception ignore) {}
        }
        return null;
    }

    private static final java.time.format.DateTimeFormatter[] DATE_PATTERNS = new java.time.format.DateTimeFormatter[] {
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,            // 2026-12-31
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),   // 31/12/2026
            java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"),     // 1/2/2026
            java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy")    // 12/31/2026
    };

//    private static java.time.LocalDate readExpiry(org.apache.poi.ss.usermodel.Cell c) {
//        if (c == null) return null;
//        switch (c.getCellType()) {
//            case NUMERIC:
//                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(c)) {
//                    return c.getLocalDateTimeCellValue().toLocalDate();
//                } else {
//                    // أحيانًا يكون التاريخ رقم Serial من إكسل بدون formatting
//                    double dv = c.getNumericCellValue();
//                    java.util.Date d = org.apache.poi.ss.usermodel.DateUtil.getJavaDate(dv);
//                    return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
//                }
//            case STRING:
//                String s = c.getStringCellValue();
//                if (s != null) {
//                    s = s.trim();
//                    if (!s.isEmpty()) {
//                        for (var fmt : DATE_PATTERNS) {
//                            try { return java.time.LocalDate.parse(s, fmt); } catch (Exception ignore) {}
//                        }
//                    }
//                }
//                return null;
//            default:
//                return null;
//        }
//    }

    private static LocalDate readExpiry(Cell cell) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String v = cell.getStringCellValue().trim();
            if (v.isEmpty()) return null;

            // ✅ جرّب أكثر من تنسيق مقبول
            DateTimeFormatter[] fmts = new DateTimeFormatter[] {
                    DateTimeFormatter.ISO_LOCAL_DATE,                 // 2026-12-31
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),        // 31/12/2026
                    DateTimeFormatter.ofPattern("d/M/yyyy"),          // 1/2/2026
                    DateTimeFormatter.ofPattern("yyyy/MM/dd")         // 2026/12/31
            };

            for (DateTimeFormatter fmt : fmts) {
                try { return LocalDate.parse(v, fmt); } catch (Exception ignore) {}
            }

        } catch (Exception ignore) {}
        return null;
    }
}