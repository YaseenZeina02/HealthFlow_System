
•	INSERT INTO users (نموذج عام لأي مستخدم – دكتور، مريض، صيدلي…)
•	INSERT INTO doctors لربط الدكتور بجدول الأطباء مع التخصص والغرفة.
•	INSERT INTO users + INSERT INTO patients لإنشاء مريض جديد (User + Patient).
•	INSERT INTO receptionists و INSERT INTO pharmacists بعد إنشاء Users خاصة بهم.
•	INSERT INTO medicines لإضافة دواء جديد مع بيانات التغليف (strength, form, base_unit…).
•	INSERT INTO medicine_batches لإنشاء دفعة مخزون جديدة (batch).
•	INSERT INTO inventory_transactions لكل حركة مخزون (إضافة/خصم، مرتبطة بوصفة أو غيرها).
•	INSERT INTO appointments لحجز موعد جديد (مع شرح أن الـ location تُملأ من room_number تلقائيًا).
•	INSERT INTO prescriptions لإنشاء وصفة مرتبطة بموعد (مثال على حالة PENDING).
•	INSERT INTO prescription_items لإضافة دواء داخل الوصفة مع الجرعة التفصيلية والـ requested units.
•	INSERT INTO activity_logs لتسجيل نشاط (مثلاً إنشاء موعد).
•	INSERT INTO password_reset_tokens لإنشاء كود استعادة كلمة المرور.



-- =============================================================
-- 1) Insert core user (generic)
--    تُستخدم عندما نريد إضافة مستخدم جديد من أي نوع (دكتور، مريض، صيدلي...)
--    ملاحظة: يجب توفير email و password_hash حقيقيين في النظام الفعلي.
-- =============================================================
INSERT INTO users (full_name, email, password_hash, role, phone, gender, national_id)
VALUES ('Dr. Ahmed Example', 'dr.ahmed@example.com', 'bcrypt-hash-here', 'DOCTOR',
        '0599000000', 'MALE', '123456789');


-- =============================================================
-- 2) Insert doctor (بعد إنشاء user بدور DOCTOR)
--    يربط المستخدم بجدول الأطباء ويحدد التخصص ورقم الغرفة.
-- =============================================================
INSERT INTO doctors (user_id, specialty, bio, room_number)
VALUES (/* user_id للدكتور */ 45,
        'GENERAL',
        'General practitioner / family doctor.',
        'Room 9');


-- =============================================================
-- 3) Insert patient user + patient record (نمط مبسّط)
--    الخطوة الأولى: إضافة مستخدم بدور PATIENT مع رقم هوية ورقم جوال.
-- =============================================================
INSERT INTO users (full_name, email, password_hash, role, phone, gender, national_id)
VALUES ('Basil Alsers', NULL, 'bcrypt-hash-here', 'PATIENT',
        '0599111222', 'MALE', '987654321')
RETURNING id AS patient_user_id;

-- بعد الحصول على patient_user_id من السطر السابق، نستخدمه هنا:
INSERT INTO patients (user_id, date_of_birth, medical_history)
VALUES (/* patient_user_id */ 120,
        DATE '1998-07-27',
        'No significant past medical history.');


-- =============================================================
-- 4) Insert receptionist / pharmacist (نفس الفكرة مع user)
-- =============================================================
INSERT INTO users (full_name, email, password_hash, role, phone, gender)
VALUES ('Rec. Salem', 'rec.salem@example.com', 'bcrypt-hash-here', 'RECEPTIONIST',
        '0599333444', 'FEMALE');

INSERT INTO receptionists (user_id)
VALUES (/* user_id للريسيبشن */ 60);

INSERT INTO users (full_name, email, password_hash, role, phone, gender)
VALUES ('PH. Mohammed', 'ph.mohammed@example.com', 'bcrypt-hash-here', 'PHARMACIST',
        '0599555666', 'MALE');

INSERT INTO pharmacists (user_id)
VALUES (/* user_id للصيدلي */ 70);


-- =============================================================
-- 5) Insert medicine (دواء جديد في قائمة الأدوية)
--    مع بعض بيانات التغليف الأساسية لربطها مع المخزون / الوصفة.
-- =============================================================
INSERT INTO medicines (name, strength, form, base_unit,
                       tablets_per_blister, blisters_per_box,
                       ml_per_bottle, grams_per_tube,
                       split_allowed, reorder_threshold)
VALUES ('Paracetamol', '500 mg', 'TABLET', 'TABLET',
        10, 10,
        NULL, NULL,
        TRUE, 50);


-- =============================================================
-- 6) Insert medicine batch (دفعة جديدة في المخزن)
-- =============================================================
INSERT INTO medicine_batches (medicine_id, batch_no, expiry_date, quantity, received_at, reason, type)
VALUES (/* medicine_id لـ Paracetamol */ 1,
        'PCT-2025-001',
        DATE '2026-12-31',
        1000,
        NOW(),
        'Initial stock',
        'PURCHASE');


-- =============================================================
-- 7) Insert inventory transaction (حركة مخزون: إضافة أو خصم)
--    qty_change موجب = إضافة للمخزون، سالب = خصم من المخزون.
-- =============================================================
INSERT INTO inventory_transactions (medicine_id, batch_id, qty_change,
                                    reason, ref_type, ref_id,
                                    pharmacist_id)
VALUES (1,           -- medicine_id
        10,          -- batch_id
        -30,         -- صرف 30 وحدة للمريض
        'Dispense to patient prescription #123',
        'PRESCRIPTION',
        123,         -- prescription_id
        5);          -- pharmacist_id (من جدول pharmacists)


-- =============================================================
-- 8) Insert appointment (حجز موعد جديد بين مريض وطبيب)
--    location يتم تعبئتها تلقائيًا من room_number للطبيب إذا تركناها NULL.
-- =============================================================
INSERT INTO appointments (doctor_id, patient_id, appointment_date, duration_minutes, status)
VALUES (11,                       -- doctor_id (من doctors.id)
        47,                       -- patient_id (من patients.id)
        TIMESTAMP '2025-11-14 13:00:00',
        20,
        'SCHEDULED')
RETURNING id AS appointment_id;


-- =============================================================
-- 9) Insert prescription (وصفة مرتبطة بموعد معيّن)
--    في النظام الفعلي الافتراضي يكون DRAFT، هنا مثال على PENDING.
-- =============================================================
INSERT INTO prescriptions (appointment_id, doctor_id, patient_id, status, notes)
VALUES (/* appointment_id */ 200,
        11,       -- doctor_id
        47,       -- patient_id
        'PENDING',
        'Take medications as prescribed.')
RETURNING id AS prescription_id;


-- =============================================================
-- 10) Insert prescription item (دواء ضمن الوصفة)
--      يستخدم أعمدة الـ dosage التفصيلية مع الجرعة النصية.
-- =============================================================
INSERT INTO prescription_items (prescription_id, medicine_id, medicine_name,
                                dosage, dose, freq_per_day, duration_days,
                                strength, form, route, notes,
                                qty_units_requested, suggested_unit, suggested_count)
VALUES (/* prescription_id */ 300,
        1,               -- Paracetamol
        'Paracetamol 500 mg tablets',
        '1 tab 3 times daily for 5 days',
        1,               -- dose (عدد الوحدات في كل جرعة)
        3,               -- freq_per_day
        5,               -- duration_days
        '500 mg',
        'TABLET',
        'PO',            -- route: عن طريق الفم
        'After meals',
        15,              -- عدد الوحدات المطلوبة
        'BLISTER',       -- الوحدة المقترحة
        2);              -- عدد الشرائط المقترحة


-- =============================================================
-- 11) Insert activity log (تسجيل نشاط المستخدم)
-- =============================================================
INSERT INTO activity_logs (user_id, action, entity_type, entity_id, metadata)
VALUES (45,
        'CREATE_APPOINTMENT',
        'APPOINTMENT',
        200,
        '{"source":"Reception UI","notes":"Booked from dashboard"}'::jsonb);


-- =============================================================
-- 12) Insert password reset token (كود استعادة كلمة المرور)
-- =============================================================
INSERT INTO password_reset_tokens (user_id, otp_hash, expires_at, request_info)
VALUES (45,
        'sha256-hash-of-otp-here',
        NOW() + INTERVAL '10 minutes',
        'Requested via forgot-password screen');




-- =============================================================
-- 13) insert appointment for a new patient with an existing doctor
--      يستخدم أعمدة الـ dosage التفصيلية مع الجرعة النصية.
-- =============================================================

WITH doc AS (
    SELECT d.id AS doctor_id
    FROM doctors d
    WHERE d.user_id = 45      -- رقم المستخدم للدكتور
    LIMIT 1
    ),
    new_user AS (
INSERT INTO users (full_name, phone, national_id, role, gender, password_hash)
VALUES ('Test Patient2', '0590059999', '120006789', 'PATIENT', 'MALE',
    'dummy-hash-for-test-only')
    RETURNING id
    ),
    new_patient AS (
INSERT INTO patients (user_id, date_of_birth, medical_history)
SELECT id, DATE '2000-01-01', 'No history'
FROM new_user
    RETURNING id
    )
INSERT INTO appointments (doctor_id, patient_id, appointment_date, status)
SELECT
    doc.doctor_id,                               -- من doctors.id
    new_patient.id,
    (CURRENT_DATE + INTERVAL '1 day')::date
        + TIME '13:40',
    'SCHEDULED'
FROM new_patient, doc
    RETURNING id AS appointment_id;


-- نهاية ملف InsertQuery.sql: أمثلة جاهزة لكل عمليات الإدخال الرئيسية في النظام