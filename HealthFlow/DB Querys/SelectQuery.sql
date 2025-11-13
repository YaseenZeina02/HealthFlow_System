1.	USERS & ROLES – عرض المستخدمين، الطاقم، والبحث بالبريد/الجوال
	2.	DOCTORS – قائمة الأطباء، الحالة، رقم الغرفة
	3.	PATIENTS – المرضى مع national_id_masked من v_patients
	4.	APPOINTMENTS –
	•	مواعيد يوم معيّن (داشبورد الريسبشن)
•	مواعيد دكتور في يوم معيّن
•	نفس فكرة الـ [Stats] total/completed/remaining/patients
•	مواعيد مريض معيّن
	5.	PRESCRIPTIONS –
	•	وصفات مريض
•	وصفة معيّنة مع عناصرها (للدكتور/الصيدلي)
•	وصفات بحالة PENDING للصيدلي
	6.	MEDICINES & INVENTORY – المخزون، الدفعات، FIFO batch
	7.	ACTIVITY_LOGS – آخر نشاطات مستخدم / كيان معيّن
	8.	REPORTING – شوية تقارير بسيطة (عدد مواعيد باليوم، أكثر أدوية صرفًا، عدد وصفات لكل دكتور)


-- =============================================================
--  HealthFlow – SELECT Queries Reference
--  هذا الملف يحتوي أهم أمثلة استعلامات SELECT المستخدمة
--  في النظام (للعرض، الداشبورد، والتقارير).
--  يمكنك استخدامه كمرجع في التقرير أو في DBeaver/Neon للتجربة.
-- =============================================================


-- =============================================================
-- 1) USERS & ROLES – عرض المستخدمين حسب الدور والحالة
-- =============================================================

-- 1.1) كل المستخدمين مع أهم الحقول
SELECT id,
       full_name,
       email,
       role,
       phone,
       gender,
       is_active,
       created_at,
       updated_at
FROM users
ORDER BY created_at DESC;

-- 1.2) كل الطاقم (بدون المرضى)
SELECT id,
       full_name,
       role,
       email,
       phone,
       is_active
FROM users
WHERE role IN ('ADMIN','DOCTOR','RECEPTIONIST','PHARMACIST')
ORDER BY role, full_name;

-- 1.3) البحث عن مستخدم بالبريد أو رقم الجوال (يستخدم في شاشة تسجيل الدخول/البحث)
SELECT id,
       full_name,
       email,
       phone,
       role,
       is_active
FROM users
WHERE LOWER(email) = LOWER('dc@hf.app')
   OR phone = '0599000111';


-- =============================================================
-- 2) DOCTORS – قائمة الأطباء + الحالة + رقم الغرفة
--    مشابه لما يستخدمه الريسيبشن وواجهة اختيار الطبيب.
-- =============================================================

-- 2.1) قائمة الأطباء مع معلومات المستخدم
SELECT d.id           AS doctor_id,
       u.id           AS user_id,
       u.full_name,
       u.gender,
       u.phone,
       d.specialty,
       d.bio,
       d.availability_status,
       d.room_number,
       u.is_active
FROM doctors d
JOIN users u ON u.id = d.user_id
ORDER BY u.full_name;

-- 2.2) الأطباء المتوفرون حاليًا (للحجز السريع)
SELECT d.id           AS doctor_id,
       u.full_name,
       d.specialty,
       d.room_number
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE d.availability_status = 'AVAILABLE'
  AND u.is_active = TRUE
ORDER BY u.full_name;


-- =============================================================
-- 3) PATIENTS – عرض المرضى مع إخفاء رقم الهوية
--    نستخدم v_patients + users للحصول على الاسم والجنس والجوال.
-- =============================================================

-- 3.1) قائمة المرضى مع الهوية المخفية
SELECT p.id                    AS patient_id,
       u.full_name,
       u.gender,
       u.phone,
       p.date_of_birth,
       p.medical_history,
       vp.national_id_masked
FROM patients p
JOIN users u     ON u.id = p.user_id
JOIN v_patients vp ON vp.id = p.id
ORDER BY u.full_name;

-- 3.2) البحث عن مريض بالاسم/الجوال/الهوية (للاستخدام في شاشة الريسبشن)
SELECT p.id                    AS patient_id,
       u.full_name,
       u.phone,
       vp.national_id_masked,
       p.date_of_birth,
       p.medical_history
FROM patients p
JOIN users u     ON u.id = p.user_id
JOIN v_patients vp ON vp.id = p.id
WHERE  (u.full_name ILIKE '%' || 'basil' || '%')
    OR (u.phone      ILIKE '%' || '599'  || '%')
    OR (vp.national_id_masked ILIKE '%' || '12'   || '%')
ORDER BY u.full_name;


-- =============================================================
-- 4) APPOINTMENTS – استعلامات المواعيد للريسيبشن والدكتور
-- =============================================================

-- 4.1) مواعيد يوم معيّن (داشبورد الريسيبشن)
--      تعرض اسم المريض، الطبيب، الحالة، الغرفة، والوقت.
SELECT a.id,
       a.appointment_date,
       a.status,
       a.location,
       pu.full_name AS patient_name,
       du.full_name AS doctor_name,
       d.specialty,
       a.duration_minutes
FROM appointments a
JOIN doctors d   ON d.id = a.doctor_id
JOIN users   du  ON du.id = d.user_id     -- doctor user
JOIN patients p  ON p.id = a.patient_id
JOIN users   pu  ON pu.id = p.user_id     -- patient user
WHERE a.appointment_date::date = DATE '2025-11-14'
ORDER BY a.appointment_date;

-- 4.2) مواعيد دكتور معيّن في يوم معيّن (واجهة الدكتور)
SELECT a.id,
       a.appointment_date,
       a.status,
       a.location,
       pu.full_name AS patient_name,
       p.id         AS patient_id
FROM appointments a
JOIN patients p  ON p.id = a.patient_id
JOIN users   pu  ON pu.id = p.user_id
WHERE a.doctor_id = 11
  AND a.appointment_date::date = DATE '2025-11-14'
ORDER BY a.appointment_date;

-- 4.3) ملخّص إحصائي لمواعيد دكتور في يوم معيّن (نفس فكرة [Stats] في اللوغز)
SELECT a.appointment_date::date                          AS day,
       a.doctor_id,
       COUNT(*)                                          AS total,
       COUNT(*) FILTER (WHERE a.status = 'COMPLETED')    AS completed,
       COUNT(*) FILTER (WHERE a.status IN ('SCHEDULED','PENDING')) AS remaining,
       COUNT(DISTINCT a.patient_id)                      AS patients
FROM appointments a
WHERE a.doctor_id = 11
  AND a.appointment_date::date = DATE '2025-11-14'
GROUP BY a.appointment_date::date, a.doctor_id;

-- 4.4) جميع مواعيد مريض معيّن (للاستخدام داخل ملف المريض)
SELECT a.id,
       a.appointment_date,
       a.status,
       du.full_name AS doctor_name,
       d.specialty,
       a.location
FROM appointments a
JOIN doctors d  ON d.id = a.doctor_id
JOIN users   du ON du.id = d.user_id
WHERE a.patient_id = 47
ORDER BY a.appointment_date DESC;


-- =============================================================
-- 5) PRESCRIPTIONS – عرض الوصفات وعناصر الوصفة
-- =============================================================

-- 5.1) كل وصفات مريض معيّن
SELECT pr.id              AS prescription_id,
       pr.created_at,
       pr.status,
       pr.diagnosis,
       du.full_name       AS doctor_name,
       pu.full_name       AS patient_name
FROM prescriptions pr
JOIN doctors d   ON d.id = pr.doctor_id
JOIN users   du  ON du.id = d.user_id
JOIN patients p  ON p.id = pr.patient_id
JOIN users   pu  ON pu.id = p.user_id
WHERE pr.patient_id = 47
ORDER BY pr.created_at DESC;

-- 5.2) وصفة معيّنة مع عناصرها (لواجهتي الدكتور والصيدلي)
SELECT pr.id                        AS prescription_id,
       pr.status,
       pr.diagnosis,
       pr.created_at,
       du.full_name                 AS doctor_name,
       pu.full_name                 AS patient_name,
       it.id                        AS item_id,
       it.medicine_id,
       it.medicine_name,
       it.dosage_text,
       it.qty_units_requested,
       it.suggested_unit,
       it.suggested_count,
       it.approved_unit,
       it.approved_count,
       it.qty_dispensed,
       it.status                    AS item_status
FROM prescriptions pr
JOIN doctors d   ON d.id = pr.doctor_id
JOIN users   du  ON du.id = d.user_id
JOIN patients p  ON p.id = pr.patient_id
JOIN users   pu  ON pu.id = p.user_id
LEFT JOIN prescription_items it ON it.prescription_id = pr.id
WHERE pr.id = 300
ORDER BY it.id;

-- 5.3) وصفات بانتظار الصيدلي (حالة PENDING)
SELECT pr.id           AS prescription_id,
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


-- =============================================================
-- 6) MEDICINES & INVENTORY – المخزون والدفعات
-- =============================================================

-- 6.1) مخزون الأدوية (جدول medicines فقط)
SELECT m.id,
       m.display_name,
       m.strength,
       m.form,
       m.available_quantity,
       m.reorder_threshold,
       CASE WHEN m.available_quantity <= m.reorder_threshold
            THEN TRUE ELSE FALSE END AS needs_reorder
FROM medicines m
ORDER BY m.display_name;

-- 6.2) دفعات دواء معيّن مع الرصيد الحالي (باستخدام ledger)
WITH bal AS (
    SELECT b.id AS batch_id,
           b.medicine_id,
           b.batch_no,
           b.expiry_date,
           COALESCE(SUM(t.qty_change),0) AS balance
    FROM medicine_batches b
    LEFT JOIN inventory_transactions t ON t.batch_id = b.id
    WHERE b.medicine_id = 1
    GROUP BY b.id, b.medicine_id, b.batch_no, b.expiry_date
)
SELECT batch_id,
       batch_no,
       expiry_date,
       balance
FROM bal
ORDER BY expiry_date;

-- 6.3) اختيار أقدم دفعة تكفي لصرف كمية معينة (منطق FIFO)
WITH bal AS (
    SELECT b.id AS batch_id,
           b.medicine_id,
           b.expiry_date,
           COALESCE(SUM(t.qty_change),0) AS balance
    FROM medicine_batches b
    LEFT JOIN inventory_transactions t ON t.batch_id = b.id
    WHERE b.medicine_id = 1
    GROUP BY b.id, b.medicine_id, b.expiry_date
)
SELECT batch_id
FROM bal
WHERE balance      >= 30                 -- الكمية المطلوبة
  AND expiry_date  >= CURRENT_DATE       -- غير منتهية
ORDER BY expiry_date ASC
LIMIT 1;


-- =============================================================
-- 7) ACTIVITY LOGS – سجلات النشاط
-- =============================================================

-- 7.1) آخر أنشطة مستخدم معيّن
SELECT id,
       user_id,
       action,
       entity_type,
       entity_id,
       metadata,
       created_at
FROM activity_logs
WHERE user_id = 45
ORDER BY created_at DESC
LIMIT 50;

-- 7.2) كل العمليات المتعلقة بموعد معيّن
SELECT id,
       user_id,
       action,
       metadata,
       created_at
FROM activity_logs
WHERE entity_type = 'APPOINTMENT'
  AND entity_id   = 200
ORDER BY created_at;


-- =============================================================
-- 8) REPORTING EXAMPLES – أمثلة تقارير بسيطة
-- =============================================================

-- 8.1) عدد المواعيد لكل حالة في يوم معيّن (تقرير إداري)
SELECT appointment_date::date AS day,
       status,
       COUNT(*)                AS count
FROM appointments
WHERE appointment_date::date = DATE '2025-11-14'
GROUP BY appointment_date::date, status
ORDER BY status;

-- 8.2) أكثر الأدوية صرفًا (عن طريق حركات المخزون المرتبطة بالوصفات)
SELECT m.id,
       m.display_name,
       SUM(CASE WHEN it.qty_dispensed > 0 THEN it.qty_dispensed ELSE 0 END) AS total_units
FROM prescription_items it
LEFT JOIN medicines m ON m.id = it.medicine_id
GROUP BY m.id, m.display_name
HAVING SUM(CASE WHEN it.qty_dispensed > 0 THEN it.qty_dispensed ELSE 0 END) > 0
ORDER BY total_units DESC
LIMIT 20;

-- 8.3) عدد الوصفات لكل دكتور في فترة زمنية
SELECT d.id         AS doctor_id,
       du.full_name AS doctor_name,
       COUNT(*)     AS presc_count
FROM prescriptions pr
JOIN doctors d  ON d.id = pr.doctor_id
JOIN users   du ON du.id = d.user_id
WHERE pr.created_at::date BETWEEN DATE '2025-11-01' AND DATE '2025-11-30'
GROUP BY d.id, du.full_name
ORDER BY presc_count DESC;


-- نهاية ملف SelectQuery.sql – أمثلة جاهزة لاستعلامات العرض والتقارير الرئيسية في HealthFlow