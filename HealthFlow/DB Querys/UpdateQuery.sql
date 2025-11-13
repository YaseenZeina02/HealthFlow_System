•	تعديل بيانات المستخدم (اسم، جوال، جنس…)
•	تفعيل/تعطيل المستخدم is_active
•	تغيير كلمة المرور password_hash
•	تعديل بروفايل الدكتور والـ availability_status
•	تعديل بيانات المريض
•	تعديل بيانات الدواء + التغليف + حد إعادة الطلب
•	تعديل بيانات batch
•	تعديل الموعد (تاريخ/وقت/حالة)
•	إغلاق المواعيد القديمة NO_SHOW
•	تغيير حالة الوصفة (DRAFT → PENDING → APPROVED → DISPENSED → REJECTED)
•	تعديل عنصر وصفة (جرعة، batch، كميات…)
•	تصحيح حركة مخزون استثنائية
•	تعليم كود استعادة كلمة السر كـ used

-- =============================================================
--  HealthFlow – Update Queries Reference
--  هذا الملف يحتوي أمثلة مهيكلة لأهم جمل UPDATE التي نستخدمها
--  في النظام، مع شرح مختصر لوظيفتها ومتى نستخدمها.
-- =============================================================


-- =============================================================
-- 1) Update basic user info (البيانات الأساسية للمستخدم)
--    مثال: تعديل رقم الجوال والاسم للبروفايل.
--    التريغرات:
--      * users_set_updated_at → يحدّث updated_at تلقائيًا
--      * ensure_contact_by_role → يتأكد من وجود email/phone حسب الدور
--      * ensure_gender_present → يلزم وجود gender
-- =============================================================
UPDATE users
SET full_name = 'New Full Name',
    phone     = '0599000111',
    gender    = 'FEMALE',
    updated_at = NOW()          -- اختياري؛ التريغر يقوم به تلقائيًا
WHERE id = 122;                 -- user_id المطلوب تعديله


-- =============================================================
-- 2) Toggle user active flag (تفعيل/تعطيل المستخدم)
--    تُستخدم لشاشة الإدارة لتعطيل حساب بدون حذفه.
-- =============================================================
UPDATE users
SET is_active = FALSE
WHERE id = 122;

-- لإعادة التفعيل:
UPDATE users
SET is_active = TRUE
WHERE id = 122;


-- =============================================================
-- 3) Change user password hash (تغيير كلمة المرور)
--    الاستعمال: شاشة "Reset/Change password".
--    الملاحظة: نخزّن الـ hash فقط، وليس كلمة المرور نفسها.
-- =============================================================
UPDATE users
SET password_hash = '$2a$10$some-bcrypt-hash-here',
    updated_at    = NOW()
WHERE id = 122;


-- =============================================================
-- 4) Update doctor profile (التخصص، النبذة، رقم الغرفة)
--    التريغرات:
--      * doctors_role_chk → يتأكد أن المستخدم فعلاً DOCTOR
--      * doctors_set_updated_at → يحدّث updated_at
-- =============================================================
UPDATE doctors
SET specialty  = 'CARDIOLOGY',
    bio        = 'Consultant cardiologist.',
    room_number = 'Room 5'
WHERE id = 11;   -- doctor.id


-- =============================================================
-- 5) Update doctor availability_status (حالة توفر الدكتور)
--    القيم: 'AVAILABLE','IN_APPOINTMENT','ON_BREAK'
--    تُستخدم في الداشبورد لعرض الأطباء المتوفرين.
-- =============================================================
UPDATE doctors
SET availability_status = 'IN_APPOINTMENT'
WHERE id = 11;


-- =============================================================
-- 6) Update patient info (تعديل بيانات المريض)
--    يتم عادةً من شاشة الريسبشن.
--    انتبه أن gender أصبح موجودًا على users وليس patients.
-- =============================================================
UPDATE patients
SET date_of_birth   = DATE '1998-07-27',
    medical_history = 'Updated history: hypertension.'
WHERE id = 47;   -- patient.id

-- لو احتجنا تعديل gender أو الهاتف، يكون عبر جدول users:
UPDATE users
SET gender = 'MALE',
    phone  = '0599888777'
WHERE id = 122;      -- user_id المرتبط بالمريض


-- =============================================================
-- 7) Update medicine master data (بيانات دواء)
--    مثل تعديل الاسم، التركيز، الشكل الدوائي أو حد إعادة الطلب.
--    التريغر meds_set_updated_at يحدّث updated_at تلقائيًا.
-- =============================================================
UPDATE medicines
SET name               = 'Paracetamol',
    strength           = '500 mg',
    form               = 'TABLET',
    base_unit          = 'TABLET',
    tablets_per_blister = 10,
    blisters_per_box    = 10,
    reorder_threshold   = 50
WHERE id = 1;


-- =============================================================
-- 8) Update medicine batch metadata (بيانات دفعة)
--    لا نعدّل quantity يدويًا؛ يتم تحديثها من ledger (inventory_transactions).
--    هنا مثال لتعديل تاريخ الانتهاء أو السبب.
-- =============================================================
UPDATE medicine_batches
SET expiry_date = DATE '2027-01-31',
    reason      = 'Supplier correction',
    type        = 'PURCHASE'
WHERE id = 55;


-- =============================================================
-- 9) Update appointment date/time or status (تعديل موعد)
--    أمثلة:
--      * إعادة جدولة الموعد
--      * تغيير الحالة إلى COMPLETED/CANCELLED/NO_SHOW
--    التريغرات:
--      * set_appt_range → يحدّث appt_range للحماية من التداخل
--      * appt_set_updated_at → يحدّث updated_at
-- =============================================================
-- (أ) إعادة جدولة الموعد:
UPDATE appointments
SET appointment_date = TIMESTAMP '2025-11-14 15:00:00',
    duration_minutes = 20
WHERE id = 200;   -- appointment_id

-- (ب) إنهاء الموعد كـ COMPLETED:
UPDATE appointments
SET status = 'COMPLETED'
WHERE id = 200;

-- (ج) إلغاء الموعد:
UPDATE appointments
SET status = 'CANCELLED'
WHERE id = 200;


-- =============================================================
-- 10) Bulk close past appointments as NO_SHOW (عملية مجمّعة)
--     هذا نفس منطق دالة close_past_appointments() الموجودة في db.sql
-- =============================================================
UPDATE appointments
SET status = 'NO_SHOW'
WHERE status = 'SCHEDULED'
  AND appointment_date::date < (NOW() AT TIME ZONE 'Asia/Gaza')::date;


-- =============================================================
-- 11) Update prescription status (حالة الوصفة)
--     حالات الوصفة: 'DRAFT','PENDING','APPROVED','REJECTED','DISPENSED'
--     التريغرات:
--       * presc_state_enforcer → يضبط decision_at / approved_at / dispensed_at
--       * presc_require_items_on_pending → يمنع PENDING بدون عناصر
-- =============================================================
-- (أ) تحويل الوصفة من DRAFT إلى PENDING (بعد إضافة أدوية):
UPDATE prescriptions
SET status = 'PENDING'
WHERE id = 300;   -- prescription_id

-- (ب) موافقة الصيدلي على الوصفة:
UPDATE prescriptions
SET status        = 'APPROVED',
    pharmacist_id = 5   -- pharmacist.id
WHERE id = 300;

-- (ج) رفض الوصفة:
UPDATE prescriptions
SET status        = 'REJECTED',
    pharmacist_id = 5,
    diagnosis     = 'Not appropriate for patient age.'
WHERE id = 300;

-- (د) صرف الوصفة بالكامل:
UPDATE prescriptions
SET status        = 'DISPENSED',
    pharmacist_id = 5
WHERE id = 300;


-- =============================================================
-- 12) Update prescription item (عنصر الوصفة)
--     أمثلة:
--       * تعديل الكمية المطلوبة أو الجرعة
--       * ربط العنصر بدفعة معينة
--       * تحديث الكمية المصروفة والحالة إلى APPROVED/COMPLETED
--     التريغرات:
--       * tri_item_med_integrity → يضمن تطابق medicine/batch والاسم
-- =============================================================
-- (أ) تعديل الجرعة وبعض الحقول التفصيلية:
UPDATE prescription_items
SET dose          = 1,
    freq_per_day  = 3,
    duration_days = 5,
    strength      = '500 mg',
    form          = 'TABLET',
    route         = 'PO',
    notes         = 'After meals'
WHERE id = 1000;  -- item_id

-- (ب) ربط العنصر بدفعة محددة:
UPDATE prescription_items
SET batch_id = 55
WHERE id = 1000;

-- (ج) تحديث الكمية المصروفة والحالة:
UPDATE prescription_items
SET qty_dispensed = 15,
    status        = 'APPROVED'
WHERE id = 1000;


-- =============================================================
-- 13) Adjust inventory_transactions (تصحيح حركة مخزون استثنائية)
--     بشكل عام نفضّل إدخال حركة جديدة للتصحيح بدلاً من UPDATE،
--     لكن أحيانًا في بيئة الاختبار نحتاج تصحيح سبب أو ref_type فقط.
-- =============================================================
UPDATE inventory_transactions
SET reason   = 'Opening balance correction',
    ref_type = 'ADJUSTMENT'
WHERE id = 500;   -- transaction id


-- =============================================================
-- 14) Mark password reset token as used (تفعيل كود الاستعادة)
--     يُستدعى بعد نجاح المستخدم في إدخال الـ OTP الصحيح.
-- =============================================================
UPDATE password_reset_tokens
SET used = TRUE
WHERE id = 10;   -- أو WHERE user_id = ? AND otp_hash = ?


-- =============================================================
-- 15) Generic pattern for soft data fixes (تصحيحات بيانات عامة)
--     مثال عام يمكن نسخه وتعديله عند الحاجة.
-- =============================================================
-- مثال: نقل كل المرضى الذين لا يملكون رقم جوال إلى قيمة افتراضية
--      فقط في بيئة الاختبار، وليس الإنتاج.
UPDATE users
SET phone = '000000000'
WHERE role = 'PATIENT'
  AND (phone IS NULL OR btrim(phone) = '');


-- نهاية ملف UpdateQuery.sql – مرجع لجمل UPDATE الأساسية في HealthFlow