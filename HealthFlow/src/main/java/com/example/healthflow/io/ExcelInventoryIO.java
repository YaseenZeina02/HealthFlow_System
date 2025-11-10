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
        DESCRIPTION(7, "Description (optional)"),
        TABLETS_PER_BLISTER(8, "Tablets/Blister (optional)"),
        BLISTERS_PER_BOX(9, "Blisters/Box (optional)"),
        ML_PER_BOTTLE(10, "mL/Bottle (optional)"),
        GRAMS_PER_TUBE(11, "g/Tube (optional)"),
        SPLIT_ALLOWED(12, "Split Allowed (true/false)"),
        REORDER_THRESHOLD(13, "Reorder Threshold (optional)");

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
        private final Integer tabletsPerBlister;
        private final Integer blistersPerBox;
        private final Integer mlPerBottle;
        private final Integer gramsPerTube;
        private final Boolean splitAllowed;
        private final Integer reorderThreshold;

        // يُملأ لاحقاً أثناء الـ commit لو قدرنا نحلّه
        private Long medicineId;              // اختياري

        public ReceiveRow(String medicineName, String strength, String form, String baseUnit,
                          Integer quantity, String batchNo, LocalDate expiry, String description,
                          Integer tabletsPerBlister, Integer blistersPerBox, Integer mlPerBottle,
                          Integer gramsPerTube, Boolean splitAllowed, Integer reorderThreshold) {
            this.medicineName = medicineName;
            this.strength = strength;
            this.form = form;
            this.baseUnit = baseUnit;
            this.quantity = quantity;
            this.batchNo = batchNo;
            this.expiry = expiry;
            this.description = description;
            this.tabletsPerBlister = tabletsPerBlister;
            this.blistersPerBox = blistersPerBox;
            this.mlPerBottle = mlPerBottle;
            this.gramsPerTube = gramsPerTube;
            this.splitAllowed = splitAllowed;
            this.reorderThreshold = reorderThreshold;
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

        public Integer getTabletsPerBlister() { return tabletsPerBlister; }
        public Integer getBlistersPerBox()    { return blistersPerBox; }
        public Integer getMlPerBottle()       { return mlPerBottle; }
        public Integer getGramsPerTube()      { return gramsPerTube; }
        public Boolean getSplitAllowed()      { return splitAllowed; }
        public Integer getReorderThreshold()  { return reorderThreshold; }

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
            ex.createCell(Col.TABLETS_PER_BLISTER.idx).setCellValue(10);
            ex.createCell(Col.BLISTERS_PER_BOX.idx).setCellValue(2);
            ex.createCell(Col.ML_PER_BOTTLE.idx).setCellValue(0);
            ex.createCell(Col.GRAMS_PER_TUBE.idx).setCellValue(0);
            ex.createCell(Col.SPLIT_ALLOWED.idx).setCellValue(true);
            ex.createCell(Col.REORDER_THRESHOLD.idx).setCellValue(20);

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
                Row row = sh.getRow(i);
                if (row == null) continue;

                String medicine = getString(row, Col.MEDICINE_NAME.idx);
                String strength = getString(row, Col.STRENGTH.idx);
                String form     = getString(row, Col.FORM.idx);
                String base     = getString(row, Col.BASE_UNIT.idx);
                Integer qty     = getInteger(row, Col.QUANTITY.idx);
                String batch    = getString(row, Col.BATCH_NO.idx);
                Integer tpb  = getInteger(row, Col.TABLETS_PER_BLISTER.idx);
                Integer bpb  = getInteger(row, Col.BLISTERS_PER_BOX.idx);
                Integer mlb  = getInteger(row, Col.ML_PER_BOTTLE.idx);
                Integer gpt  = getInteger(row, Col.GRAMS_PER_TUBE.idx);
                Boolean split= getBoolean(row, Col.SPLIT_ALLOWED.idx);
                Integer thr  = getInteger(row, Col.REORDER_THRESHOLD.idx);

                // ✅ معالجة التاريخ بمرونة عالية
                LocalDate expiry = null;
                try {
                    Cell expiryCell = row.getCell(Col.EXPIRY.idx);
                    if (expiryCell != null) {
                        expiry = parseExcelDate(expiryCell); // يحاول كل أنواع الخلايا
                        if (expiry == null) {
                            // fallback للنصوص المكتوبة يدويًا مثل "31/12/2026" أو "2026-12-31"
                            String expTxt = getString(row, Col.EXPIRY.idx);
                            expiry = parseDateString(expTxt);
                        }
                    }
                } catch (Exception ignore) {
                    // fallback إضافي لو فشل الكل
                    String expTxt = getString(row, Col.EXPIRY.idx);
                    expiry = parseDateString(expTxt);
                }

                String desc = getString(row, Col.DESCRIPTION.idx);

                // تخطي الصفوف الفارغة كليّاً
                boolean allBlank = (isBlank(medicine) && qty == null && isBlank(batch) && expiry == null
                        && isBlank(strength) && isBlank(form) && isBlank(base) && isBlank(desc)
                        && tpb == null && bpb == null && mlb == null && gpt == null && split == null && thr == null);
                if (allBlank) continue;

                // تحقق من الحقول المطلوبة
                List<String> rowErr = new ArrayList<>();
                if (isBlank(medicine)) rowErr.add("Medicine Name is required");
                if (qty == null || qty <= 0) rowErr.add("Quantity must be positive");
                if (expiry == null) rowErr.add("Expiry Date is required or invalid format (e.g. 2026-12-31)");

                if (!rowErr.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": " + String.join(", ", rowErr));
                    continue;
                }

                rows.add(new ReceiveRow(medicine, strength, form, base, qty, batch, expiry, desc,
                        tpb, bpb, mlb, gpt, split, thr));            }
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

    private static Boolean getBoolean(Row r, int idx){
        Cell c = r.getCell(idx); if (c == null) return null;
        if (c.getCellType() == CellType.BOOLEAN) return c.getBooleanCellValue();
        if (c.getCellType() == CellType.STRING) {
            String s = c.getStringCellValue().trim().toLowerCase(Locale.ROOT);
            if (s.isEmpty()) return null;
            if (s.equals("true") || s.equals("yes") || s.equals("1")) return true;
            if (s.equals("false") || s.equals("no") || s.equals("0")) return false;
        }
        if (c.getCellType() == CellType.NUMERIC) {
            return Math.round(c.getNumericCellValue()) != 0;
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

    // في ExcelInventoryIO.java (داخل الكلاس)
    private static final java.time.format.DateTimeFormatter[] FLEX_DATE_FORMATS = new java.time.format.DateTimeFormatter[] {
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("MM.dd.yyyy")
    };

    private static java.time.LocalDate parseDateString(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return java.time.LocalDate.parse(s.trim()); } catch (Exception ignore) {}
        for (var f : FLEX_DATE_FORMATS) {
            try { return java.time.LocalDate.parse(s.trim(), f); } catch (Exception ignore) {}
        }
        return null;
    }

    private static java.time.LocalDate parseExcelDate(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return null;
        var t = cell.getCellType();
        if (t == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
            if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                var d = cell.getDateCellValue().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                return d;
            } else {
                // serial بدون فورمات
                try {
                    var d = org.apache.poi.ss.usermodel.DateUtil.getJavaDate(cell.getNumericCellValue(), false)
                            .toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    return d;
                } catch (Exception ignore) {}
            }
        } else if (t == org.apache.poi.ss.usermodel.CellType.STRING) {
            return parseDateString(cell.getStringCellValue());
        } else if (t == org.apache.poi.ss.usermodel.CellType.FORMULA) {
            try {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    var d = cell.getDateCellValue().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    return d;
                } else {
                    var d = org.apache.poi.ss.usermodel.DateUtil.getJavaDate(cell.getNumericCellValue(), false)
                            .toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    return d;
                }
            } catch (Exception ignore) {
                try { return parseDateString(cell.getStringCellValue()); } catch (Exception ignore2) {}
            }
        }
        return null;
    }

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