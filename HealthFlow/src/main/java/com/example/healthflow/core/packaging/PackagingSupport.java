package com.example.healthflow.core.packaging;

public final class PackagingSupport {

    /** معلومات التغليف القادمة من جدول medicines */
//    public static final class PackagingInfo {
//        public String  baseUnit;           // TABLET/CAPSULE/SYRUP/...
//        public Integer tabletsPerBlister;  // قد تكون NULL
//        public Integer blistersPerBox;     // قد تكون NULL
//        public Integer mlPerBottle;        // قد تكون NULL
//        public Integer gramsPerTube;       // قد تكون NULL
//        public Boolean splitAllowed;       // NULL => true للوحدات الصلبة
//        public Integer reorderThreshold;   // الحد الأدنى لإعادة الطلب
//        public PackagingInfo(String baseUnit,
//                             Integer tabletsPerBlister,
//                             Integer blistersPerBox,
//                             Integer mlPerBottle,
//                             Integer gramsPerTube,
//                             Boolean splitAllowed,
//                             Integer reorderThreshold) {
//            this.baseUnit = baseUnit;
//            this.tabletsPerBlister = tabletsPerBlister;
//            this.blistersPerBox = blistersPerBox;
//            this.mlPerBottle = mlPerBottle;
//            this.gramsPerTube = gramsPerTube;
//            this.splitAllowed = splitAllowed;
//            this.reorderThreshold = reorderThreshold;
//        }
//
//        // Backward compatible (calls that still pass 6 args)
//        public PackagingInfo(String baseUnit,
//                             Integer tabletsPerBlister,
//                             Integer blistersPerBox,
//                             Integer mlPerBottle,
//                             Integer gramsPerTube,
//                             Boolean splitAllowed) {
//            this(baseUnit, tabletsPerBlister, blistersPerBox, mlPerBottle, gramsPerTube, splitAllowed, null);
//        }
//    }
    public static class PackagingInfo {
        public String  baseUnit;             // TABLET/CAPSULE/SYRUP/... إلخ
        public Integer tabletsPerBlister;
        public Integer blistersPerBox;
        public Integer mlPerBottle;
        public Integer gramsPerTube;
        public Boolean splitAllowed;
        public Integer reorderThreshold;     // <— الحقل الجديد

        // 0-arg: نحتاجه عندما ننشئ الكائن ثم نملأ الحقول يدويًا من الـDialog
        public PackagingInfo() { }

        // 6-arg: توافقًا مع الاستدعاءات القديمة (قبل إضافة reorderThreshold)
        public PackagingInfo(String baseUnit,
                             Integer tabletsPerBlister,
                             Integer blistersPerBox,
                             Integer mlPerBottle,
                             Integer gramsPerTube,
                             Boolean splitAllowed) {
            this(baseUnit, tabletsPerBlister, blistersPerBox, mlPerBottle, gramsPerTube, splitAllowed, null);
        }

        // 7-arg: التوقيع الكامل بعد إضافة reorderThreshold
        public PackagingInfo(String baseUnit,
                             Integer tabletsPerBlister,
                             Integer blistersPerBox,
                             Integer mlPerBottle,
                             Integer gramsPerTube,
                             Boolean splitAllowed,
                             Integer reorderThreshold) {
            this.baseUnit = baseUnit;
            this.tabletsPerBlister = tabletsPerBlister;
            this.blistersPerBox = blistersPerBox;
            this.mlPerBottle = mlPerBottle;
            this.gramsPerTube = gramsPerTube;
            this.splitAllowed = splitAllowed;
            this.reorderThreshold = reorderThreshold;
        }
    }

    /** اقتراح التغليف النهائي (وحدة + عدد + ما يعادلها وحدات) */
    public static final class PackSuggestion {
        public final String unit;     // UNIT/BLISTER/BOX/BOTTLE/TUBE
        public final int    count;    // عدد العبوات/الأشرطة المقترحة
        public final int    unitsTotal; // ما يعادلها بوحدات مفردة
        public PackSuggestion(String unit, int count, int unitsTotal) {
            this.unit = unit; this.count = count; this.unitsTotal = unitsTotal;
        }
    }

    /** حساب أفضل اقتراح اعتمادًا على الكمية المطلوبة والتغليف */
    public static PackSuggestion suggestPackFor(int requestedUnits, PackagingInfo p) {
        if (requestedUnits <= 0 || p == null)
            return new PackSuggestion("UNIT", Math.max(requestedUnits, 0), Math.max(requestedUnits, 0));

        // السوائل / العبوات غير القابلة للتجزئة
        if (p.mlPerBottle != null || p.gramsPerTube != null) {
            String unit = (p.mlPerBottle != null) ? "BOTTLE" : "TUBE";
            // كل عبوة تعتبر كاملة (لا نقسمها)
            return new PackSuggestion(unit, 1, Math.max(requestedUnits, 0));
        }

        int perBlister     = (p.tabletsPerBlister == null || p.tabletsPerBlister <= 0) ? 0 : p.tabletsPerBlister;
        int blistersPerBox = (p.blistersPerBox   == null || p.blistersPerBox   <= 0) ? 0 : p.blistersPerBox;
        boolean allowSplit = (p.splitAllowed == null) ? true : p.splitAllowed;

        if (perBlister > 0 && blistersPerBox > 0) {
            int perBox = perBlister * blistersPerBox;
            if (!allowSplit) {
                if (requestedUnits <= perBlister) return new PackSuggestion("BLISTER", 1, perBlister);
                if (requestedUnits <= perBox)     return new PackSuggestion("BOX",     1, perBox);
                int boxes = (int)Math.ceil(requestedUnits / (double)perBox);
                return new PackSuggestion("BOX", boxes, boxes * perBox);
            }
            if (requestedUnits % perBlister == 0) {
                int bl = requestedUnits / perBlister;
                return new PackSuggestion("BLISTER", bl, bl * perBlister);
            }
            int up = ((requestedUnits + perBlister - 1) / perBlister) * perBlister;
            if (up - requestedUnits <= Math.max(1, (int)(0.2 * requestedUnits))) {
                int bl = up / perBlister;
                return new PackSuggestion("BLISTER", bl, bl * perBlister);
            }
            return new PackSuggestion("UNIT", requestedUnits, requestedUnits);
        }

        if (perBlister > 0) {
            if (!allowSplit) {
                int bl = (int)Math.ceil(requestedUnits / (double)perBlister);
                return new PackSuggestion("BLISTER", bl, bl * perBlister);
            }
            if (requestedUnits % perBlister == 0) {
                int bl = requestedUnits / perBlister;
                return new PackSuggestion("BLISTER", bl, bl * perBlister);
            }
            return new PackSuggestion("UNIT", requestedUnits, requestedUnits);
        }

        return new PackSuggestion("UNIT", requestedUnits, requestedUnits);
    }

    /** تنسيق نص الاقتراح للعرض في الجدول */
    public static String formatSuggestionText(PackSuggestion s) {
        if (s == null) return "Sugg: —";
        String unitLabel = switch (s.unit) {
            case "BLISTER" -> "BLISTER";
            case "BOX"     -> "BOX";
            case "BOTTLE"  -> "BOTTLE";
            case "TUBE"    -> "TUBE";
            default        -> "UNIT";
        };
        return "Sugg: " + s.count + " " + unitLabel + " = " + s.unitsTotal;
    }
}