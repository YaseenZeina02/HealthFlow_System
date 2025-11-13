1.	Reception
•	جدول المرضى PatientsTable
•	جدول الأطباء TableDoctors_Recption
•	جدول المواعيد الرئيسي AppointmentsTable
•	جدول مواعيد الداشبورد TableAppInDashboard

2.	Doctor
•	مواعيد اليوم للدكتور
•	تاريخ مواعيد مريض معيّن (في ملف المريض)
•	جدول الوصفات عند الدكتور (حسب المريض / حسب الدكتور)

3.	Pharmacy
•	جدول الوصفات الـ PENDING
•	جدول عناصر الوصفة (ItemsTable)

4.	Inventory
•	جدول الأدوية
•	جدول الدفعات لكل دواء
•	جدول حركات المخزون (transactions)

5.	Reports / Logs
•	جدول سجلات النشاط ActivityLogs

وكل استعلام عليه تعليق بالعربي يوضح:
•	في أي واجهة يُستخدم
•	مع أي جدول JavaFX مرتبط
•	وأين تغيّر doctor_id, patient_id, التاريخ… إلخ.


-- =============================================================
--  HealthFlow – Table Queries (SELECT for UI Tables)
--  هذا الملف يجمع أهم الاستعلامات التي نستخدمها لعرض الجداول
--  في واجهات النظام (Reception / Doctor / Pharmacy / Inventory / Reports).
--  الهدف: التوثيق + إمكانية التجربة في DBeaver / Neon.
-- =============================================================


-- =============================================================
-- 1) RECEPTION – PATIENTS TABLE
--    جدول المرضى في واجهة الريسبشن (PatientsTable)
-- =============================================================

-- قائمة المرضى مع أخفاء رقم الهوية (باستخدام v_patients)
SELECT
    p.id                    AS patient_id,
    u.full_name,
    u.gender,
    u.phone,
    p.date_of_birth,
    p.medical_history,
--     vp.national_id_masked
FROM patients p
JOIN users      u  ON u.id = p.user_id
JOIN v_patients vp ON vp.id = p.id
ORDER BY u.full_name;

-- ملاحظة:
--  الواجهة تستخدم FilteredList/SortedList للبحث بالاسم/الجوال/الهوية.


-- =============================================================
-- 2) RECEPTION – DOCTORS TABLE
--    جدول الأطباء في واجهة الريسبشن (TableDoctors_Recption)
-- =============================================================

-- قائمة الأطباء مع بيانات المستخدم وحالة التوفر والغرفة
SELECT
    d.id               AS doctor_id,
    u.full_name,
    u.gender,
    u.phone,
    d.specialty,
    d.bio,
    d.availability_status,
    d.room_number,
    u.is_active
FROM doctors d
JOIN users  u ON u.id = d.user_id
ORDER BY u.full_name;


-- =============================================================
-- 3) RECEPTION – APPOINTMENTS TABLE (MAIN SCHEDULE)
--    جدول المواعيد الرئيسي في الريسبشن (AppointmentsTable)
-- =============================================================

-- مواعيد يوم معيّن لكل الأطباء، مع اسم المريض والطبيب والحالة
SELECT
    a.id,
    a.appointment_date,
    a.status,
    a.location,
    a.duration_minutes,
    pu.full_name AS patient_name,
    du.full_name AS doctor_name,
    d.specialty
FROM appointments a
JOIN doctors   d  ON d.id = a.doctor_id
JOIN users     du ON du.id = d.user_id     -- doctor user
JOIN patients  p  ON p.id = a.patient_id
JOIN users     pu ON pu.id = p.user_id     -- patient user
WHERE a.appointment_date::date = DATE '2025-11-14'  -- يستبدل بتاريخ اليوم/المحدد في الواجهة
ORDER BY a.appointment_date;

-- ملاحظة:
--  في الكود، التاريخ يُمرّر كـ Parameter (LocalDate) بدل القيمة الثابتة.


-- =============================================================
-- 4) RECEPTION – DASHBOARD APPOINTMENTS TABLE
--    جدول المواعيد الصغير في الـ Dashboard (TableAppInDashboard)
-- =============================================================

-- مواعيد اليوم (أو تاريخ محدد) مع معلومات مختصرة للعرض في الداشبورد
SELECT
    a.id,
    a.appointment_date,
    a.status,
    pu.full_name AS patient_name,
    du.full_name AS doctor_name,
    d.specialty,
    a.location
FROM appointments a
JOIN doctors   d  ON d.id = a.doctor_id
JOIN users     du ON du.id = d.user_id
JOIN patients  p  ON p.id = a.patient_id
JOIN users     pu ON pu.id = p.user_id
WHERE a.appointment_date::date = DATE '2025-11-14'
ORDER BY a.appointment_date;

-- ملاحظة:
--  هذا الاستعلام شبيه بما يستخدم في listDashboardAppointments(day)
--  ويُستخدم أيضًا لتشغيل أصوات التنبيه عند تغيّر الحالة.


-- =============================================================
-- 5) DOCTOR – TODAY’S APPOINTMENTS TABLE
--    جدول مواعيد اليوم عند الدكتور (AppointmentsTableDoctor)
-- =============================================================

-- مواعيد دكتور معيّن في يوم معيّن (واجهة الدكتور)
SELECT
    a.id,
    a.appointment_date,
    a.status,
    a.location,
    pu.full_name AS patient_name,
    p.id         AS patient_id
FROM appointments a
JOIN patients p  ON p.id = a.patient_id
JOIN users   pu  ON pu.id = p.user_id
WHERE a.doctor_id = 11                                -- doctor_id الحالي (من Session)
  AND a.appointment_date::date = DATE '2025-11-14'    -- تاريخ مختار في الواجهة
ORDER BY a.appointment_date;


-- =============================================================
-- 6) DOCTOR – PATIENT HISTORY TABLE
--    جدول مواعيد مريض معيّن في ملف المريض عند الدكتور
-- =============================================================

SELECT
    a.id,
    a.appointment_date,
    a.status,
    du.full_name AS doctor_name,
    d.specialty,
    a.location
FROM appointments a
JOIN doctors d  ON d.id = a.doctor_id
JOIN users   du ON du.id = d.user_id
WHERE a.patient_id = 47                               -- patient_id من الصف المحدد
ORDER BY a.appointment_date DESC;


-- =============================================================
-- 7) DOCTOR – PRESCRIPTIONS TABLE
--    جدول الوصفات التابعة للمريض (أو للدكتور) في واجهة الدكتور
-- =============================================================

-- وصفات مريض معيّن (تُعرض عادةً في تبويب Patient Prescriptions)
SELECT
    pr.id          AS prescription_id,
    pr.created_at,
    pr.status,
    pr.diagnosis,
    du.full_name   AS doctor_name,
    pu.full_name   AS patient_name
FROM prescriptions pr
JOIN doctors d   ON d.id = pr.doctor_id
JOIN users   du  ON du.id = d.user_id
JOIN patients p  ON p.id = pr.patient_id
JOIN users   pu  ON pu.id = p.user_id
WHERE pr.patient_id = 47
ORDER BY pr.created_at DESC;

-- وصفات دكتور معيّن (مثلاً تبويب Doctor Prescriptions)
SELECT
    pr.id        AS prescription_id,
    pr.created_at,
    pr.status,
    pr.diagnosis,
    pu.full_name AS patient_name
FROM prescriptions pr
JOIN patients p  ON p.id = pr.patient_id
JOIN users   pu  ON pu.id = p.user_id
WHERE pr.doctor_id = 11
ORDER BY pr.created_at DESC;


-- =============================================================
-- 8) PHARMACY – PENDING PRESCRIPTIONS TABLE
--    جدول الوصفات بانتظار الصيدلي (PendingTable)
-- =============================================================

SELECT
    pr.id           AS prescription_id,
    pr.created_at,
    pr.status,
    du.full_name    AS doctor_name,
    pu.full_name    AS patient_name
FROM prescriptions pr
JOIN doctors d   ON d.id = pr.doctor_id
JOIN users   du  ON du.id = d.user_id
JOIN patients p  ON p.id = pr.patient_id
JOIN users   pu  ON pu.id = p.user_id
WHERE pr.status = 'PENDING'
ORDER BY pr.created_at;

-- ملاحظة:
--  في واجهة الصيدلي، يتم ربط هذا الاستعلام بدالة loadPendingPrescriptions
--  مع إعادة التحميل عند إشعار NOTIFY 'prescriptions_changed'.


-- =============================================================
-- 9) PHARMACY – PRESCRIPTION ITEMS TABLE
--    جدول عناصر الوصفة للصيدلي (ItemsTable)
-- =============================================================

SELECT
    it.id                 AS item_id,
    it.medicine_id,
    it.medicine_name,
    it.dosage_text,
    it.qty_units_requested,
    it.suggested_unit,
    it.suggested_count,
    it.approved_unit,
    it.approved_count,
    it.approved_units_total,
    it.qty_dispensed,
    it.status            AS item_status,
    it.batch_id
FROM prescription_items it
WHERE it.prescription_id = 300      -- prescription_id للصف المحدد في جدول الوصفات
ORDER BY it.id;


-- =============================================================
-- 10) INVENTORY – MEDICINES TABLE
--     جدول الأدوية في واجهة الجرد/الصيدلية
-- =============================================================

SELECT
    m.id,
    m.display_name,
    m.name,
    m.strength,
    m.form,
    m.available_quantity,
    m.reorder_threshold,
    m.base_unit,
    m.tablets_per_blister,
    m.blisters_per_box,
    m.ml_per_bottle,
    m.grams_per_tube,
    m.split_allowed
FROM medicines m
ORDER BY m.display_name;

-- ملاحظة:
--  هذا الجدول يُستخدم في شاشة المخزون وفي شاشة اختيار الدواء في الوصفة.


-- =============================================================
-- 11) INVENTORY – BATCHES TABLE
--     جدول دفعات دواء معيّن (BatchesTable)
-- =============================================================

SELECT
    b.id           AS batch_id,
    b.medicine_id,
    b.batch_no,
    b.expiry_date,
    b.quantity,
    b.received_at,
    b.type,
    b.reason
FROM medicine_batches b
WHERE b.medicine_id = 1
ORDER BY b.expiry_date;


-- =============================================================
-- 12) INVENTORY – TRANSACTIONS TABLE
--     جدول حركات المخزون (InventoryTransactionsTable)
-- =============================================================

SELECT
    t.id,
    t.medicine_id,
    m.display_name AS medicine_name,
    t.batch_id,
    t.qty_change,
    t.reason,
    t.ref_type,
    t.ref_id,
    t.pharmacist_id,
    t.created_at
FROM inventory_transactions t
LEFT JOIN medicines m ON m.id = t.medicine_id
WHERE t.medicine_id = 1                  -- أو حسب الدواء المحدد في واجهة المخزون
ORDER BY t.created_at DESC;


-- =============================================================
-- 13) REPORTS / LOGS – ACTIVITY LOGS TABLE
--     جدول سجلات النشاط (ActivityLogsTable)
-- =============================================================

SELECT
    l.id,
    l.user_id,
    u.full_name AS user_name,
    l.action,
    l.entity_type,
    l.entity_id,
    l.metadata,
    l.created_at
FROM activity_logs l
LEFT JOIN users u ON u.id = l.user_id
ORDER BY l.created_at DESC
LIMIT 200;

-- ملاحظة:
--  يمكن تغيير LIMIT حسب الحاجة في التقارير.


-- =============================================================
-- ملاحظات عامة:
-- 1) التواريخ والأرقام (doctor_id, patient_id, medicine_id, prescription_id)
--    تُمرّر من الكود كـ Parameters، هنا وضعنا قيمًا كمثال فقط.
-- 2) هذه الاستعلامات تمثّل شكل SELECT المستخدم في النظام لعرض الجداول،
--    مع joins ضرورية للحصول على الأسماء/الحقول المقروءة للمستخدم.
-- 3) يمكنك نسخ أي استعلام منها مباشرة إلى DBeaver أو وحدة SQL في Neon
--    لتجربته على قاعدة البيانات الفعلية.
-- نهاية ملف tabelsQuerys.sql – Table Queries Reference