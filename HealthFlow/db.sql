-- =========================
--  Extensions
-- =========================
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =========================
--  Enums
-- =========================
CREATE TYPE role_type AS ENUM ('ADMIN','DOCTOR','RECEPTIONIST','PHARMACIST','PATIENT');
CREATE TYPE appt_status AS ENUM ('PENDING','SCHEDULED','COMPLETED','CANCELLED','NO_SHOW');
CREATE TYPE prescription_status AS ENUM ('PENDING','APPROVED','REJECTED','DISPENSED');
CREATE TYPE item_status2 AS ENUM ('PENDING','PARTIAL','DISPENSED','CANCELLED');  -- عدلت عليها بحيث بدلنا PARTIAL بCOMPLETED التعديل في السيرفر وفي اسف الملف للتنبيه فقط
CREATE TYPE gender_type AS ENUM ('MALE','FEMALE');

-- =========================
--  Utility functions
-- =========================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END $$;

CREATE OR REPLACE FUNCTION ensure_user_role(expected role_type, uid BIGINT)
RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE r role_type;
BEGIN
SELECT role INTO r FROM users WHERE id = uid;
IF r IS NULL OR r <> expected THEN
    RAISE EXCEPTION 'User % must have role %', uid, expected;
END IF;
END $$;

CREATE OR REPLACE FUNCTION mask_nid(nid TEXT)
RETURNS TEXT LANGUAGE sql IMMUTABLE AS $$
SELECT CASE WHEN nid IS NULL THEN NULL
            ELSE SUBSTRING(nid,1,2) || '*****' || SUBSTRING(nid,8,2) END
           $$;

-- require national_id for selected roles
CREATE OR REPLACE FUNCTION ensure_national_id_for_roles()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.role IN ('DOCTOR','PATIENT') AND NEW.national_id IS NULL THEN
    RAISE EXCEPTION 'national_id is required for role %', NEW.role;
END IF;
  IF NEW.national_id IS NOT NULL AND NEW.national_id !~ '^\d{9}$' THEN
    RAISE EXCEPTION 'national_id must be 9 digits';
END IF;
RETURN NEW;
END $$;

-- =========================
--  Core users
-- =========================
CREATE TABLE users (
                       id            BIGSERIAL   PRIMARY KEY,
                       national_id   CHAR(9),                                     -- optional globally; required by trigger for some roles
                       full_name     VARCHAR(100) NOT NULL,
                       email         CITEXT       NOT NULL UNIQUE,
                       password_hash TEXT         NOT NULL,
                       role          role_type    NOT NULL,
                       phone         VARCHAR(20),
                       is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
                       last_login    TIMESTAMPTZ,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
    -- CONSTRAINT national_id_digits_chk CHECK (national_id IS NULL OR national_id ~ '^\d{9}$')
);

-- unique when present
CREATE UNIQUE INDEX uniq_users_nid_nonnull ON users (national_id) WHERE national_id IS NOT NULL;

CREATE TRIGGER users_need_nid
    BEFORE INSERT OR UPDATE ON users
                         FOR EACH ROW EXECUTE FUNCTION ensure_national_id_for_roles();

CREATE TRIGGER users_set_updated_at
    BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =========================
--  Subtype tables (1:1 with users)
-- =========================
CREATE TABLE doctors (
                         id           BIGSERIAL PRIMARY KEY,
                         user_id      BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                         specialty    VARCHAR(100) NOT NULL,
                         bio          TEXT,
                         updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE patients (
                          id              BIGSERIAL PRIMARY KEY,
                          user_id         BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                          date_of_birth   DATE   NOT NULL CHECK (date_of_birth <= CURRENT_DATE),
                          gender          gender_type NOT NULL,
                          medical_history TEXT,
                          updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE receptionists (
                               id      BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE pharmacists (
                             id      BIGSERIAL PRIMARY KEY,
                             user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE
);

-- Guardrails: user role must match subtype
CREATE OR REPLACE FUNCTION trg_need_doctor()       RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN PERFORM ensure_user_role('DOCTOR',       NEW.user_id); RETURN NEW; END $$;
CREATE OR REPLACE FUNCTION trg_need_patient()      RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN PERFORM ensure_user_role('PATIENT',      NEW.user_id); RETURN NEW; END $$;
CREATE OR REPLACE FUNCTION trg_need_receptionist() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN PERFORM ensure_user_role('RECEPTIONIST', NEW.user_id); RETURN NEW; END $$;
CREATE OR REPLACE FUNCTION trg_need_pharmacist()   RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN PERFORM ensure_user_role('PHARMACIST',   NEW.user_id); RETURN NEW; END $$;

CREATE TRIGGER doctors_role_chk       BEFORE INSERT OR UPDATE ON doctors       FOR EACH ROW EXECUTE FUNCTION trg_need_doctor();
CREATE TRIGGER patients_role_chk      BEFORE INSERT OR UPDATE ON patients      FOR EACH ROW EXECUTE FUNCTION trg_need_patient();
CREATE TRIGGER receptionists_role_chk BEFORE INSERT OR UPDATE ON receptionists FOR EACH ROW EXECUTE FUNCTION trg_need_receptionist();
CREATE TRIGGER pharmacists_role_chk   BEFORE INSERT OR UPDATE ON pharmacists   FOR EACH ROW EXECUTE FUNCTION trg_need_pharmacist();

CREATE TRIGGER doctors_set_updated_at  BEFORE UPDATE ON doctors  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER patients_set_updated_at BEFORE UPDATE ON patients FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =========================
--  Medicines & Inventory
-- =========================
CREATE TABLE medicines (
                           id                 BIGSERIAL PRIMARY KEY,
                           name               VARCHAR(100) NOT NULL UNIQUE,
                           description        TEXT,
                           available_quantity INT NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
                           created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER meds_set_updated_at BEFORE UPDATE ON medicines FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_med_name_trgm ON medicines USING gin (name gin_trgm_ops);

CREATE TABLE medicine_batches (
                                  id           BIGSERIAL PRIMARY KEY,
                                  medicine_id  BIGINT NOT NULL REFERENCES medicines(id) ON DELETE CASCADE,
                                  batch_no     VARCHAR(50) NOT NULL,
                                  expiry_date  DATE NOT NULL,
                                  quantity     INT  NOT NULL CHECK (quantity >= 0),
                                  received_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  UNIQUE (medicine_id, batch_no)
);

CREATE TABLE inventory_transactions (
                                        id           BIGSERIAL PRIMARY KEY,
                                        medicine_id  BIGINT NOT NULL REFERENCES medicines(id) ON DELETE CASCADE,
                                        batch_id     BIGINT REFERENCES medicine_batches(id) ON DELETE SET NULL,
                                        qty_change   INT NOT NULL,
                                        reason       TEXT NOT NULL,
                                        ref_type     TEXT,
                                        ref_id       BIGINT,
                                        created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_inv_tx_med_time ON inventory_transactions(medicine_id, created_at);

-- keep medicines.available_quantity synced from ledger
CREATE OR REPLACE FUNCTION refresh_medicine_available_qty(p_medicine_id BIGINT)
RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
UPDATE medicines m
SET available_quantity =
        COALESCE((SELECT SUM(qty_change) FROM inventory_transactions it
                  WHERE it.medicine_id = p_medicine_id), 0),
    updated_at = NOW()
WHERE m.id = p_medicine_id;
END $$;

CREATE OR REPLACE FUNCTION inv_tx_after_change()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  PERFORM refresh_medicine_available_qty(NEW.medicine_id);
RETURN NEW;
END $$;

CREATE TRIGGER inv_tx_after_ins
    AFTER INSERT ON inventory_transactions
    FOR EACH ROW EXECUTE FUNCTION inv_tx_after_change();

-- =========================
--  Appointments (overlap guard)
-- =========================
CREATE TABLE appointments (
                              id               BIGSERIAL PRIMARY KEY,
                              doctor_id        BIGINT NOT NULL REFERENCES doctors(id)   ON DELETE RESTRICT,
                              patient_id       BIGINT NOT NULL REFERENCES patients(id)  ON DELETE RESTRICT,
                              appointment_date TIMESTAMPTZ NOT NULL,
                              duration_minutes INT NOT NULL DEFAULT 30,
                              status           appt_status NOT NULL DEFAULT 'PENDING',
                              location         VARCHAR(100),
                              created_by       BIGINT REFERENCES users(id) ON DELETE SET NULL,
                              created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              appt_range       tstzrange
);

CREATE OR REPLACE FUNCTION update_appt_range()
RETURNS TRIGGER AS $$
BEGIN
  NEW.appt_range := tstzrange(NEW.appointment_date, NEW.appointment_date + (NEW.duration_minutes || ' minutes')::interval, '[)');
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_appt_range
    BEFORE INSERT OR UPDATE ON appointments
                         FOR EACH ROW EXECUTE FUNCTION update_appt_range();
CREATE TRIGGER appt_set_updated_at BEFORE UPDATE ON appointments FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_appt_range_doctor ON appointments USING gist (doctor_id, appt_range);
ALTER TABLE appointments
    ADD CONSTRAINT no_doctor_overlap EXCLUDE USING gist (doctor_id WITH =, appt_range WITH &&);

CREATE INDEX idx_appt_doctor_time  ON appointments(doctor_id, appointment_date);
CREATE INDEX idx_appt_patient_time ON appointments(patient_id, appointment_date);
CREATE INDEX idx_appt_status_time  ON appointments(status, appointment_date);

-- =========================
--  Prescriptions (+ state rules)
-- =========================
CREATE TABLE prescriptions (
                               id             BIGSERIAL PRIMARY KEY,
                               appointment_id BIGINT NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
                               doctor_id      BIGINT NOT NULL REFERENCES doctors(id)      ON DELETE RESTRICT,
                               patient_id     BIGINT NOT NULL REFERENCES patients(id)     ON DELETE RESTRICT,
                               pharmacist_id  BIGINT REFERENCES pharmacists(id)           ON DELETE RESTRICT,
                               status         prescription_status NOT NULL DEFAULT 'PENDING',
                               decision_at    TIMESTAMPTZ,
                               decision_note  TEXT,
                               notes          TEXT,
                               created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               approved_at    TIMESTAMPTZ,
                               dispensed_at   TIMESTAMPTZ,
                               approved_by    BIGINT REFERENCES pharmacists(id),
                               dispensed_by   BIGINT REFERENCES pharmacists(id),
                               CONSTRAINT presc_decision_guard CHECK (
                                   (status = 'PENDING')
                                       OR (status IN ('APPROVED','REJECTED','DISPENSED') AND pharmacist_id IS NOT NULL AND decision_at IS NOT NULL)
                                   )
);
CREATE INDEX idx_presc_patient_time    ON prescriptions(patient_id, created_at);
CREATE INDEX idx_presc_status_decision ON prescriptions(status, decision_at);

-- doctor/patient must match appointment
CREATE OR REPLACE FUNCTION presc_match_appt()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE a_doctor BIGINT; a_patient BIGINT;
BEGIN
SELECT doctor_id, patient_id INTO a_doctor, a_patient FROM appointments WHERE id = NEW.appointment_id;
IF a_doctor IS NULL THEN RAISE EXCEPTION 'Invalid appointment %', NEW.appointment_id; END IF;
  IF NEW.doctor_id <> a_doctor OR NEW.patient_id <> a_patient THEN
    RAISE EXCEPTION 'Prescription doctor/patient must match appointment';
END IF;
RETURN NEW;
END $$;

CREATE TRIGGER prescriptions_match_appt
    BEFORE INSERT OR UPDATE ON prescriptions
                         FOR EACH ROW EXECUTE FUNCTION presc_match_appt();

-- state timestamps/actors
CREATE OR REPLACE FUNCTION presc_state_enforcer()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.status IN ('APPROVED','REJECTED') THEN
    IF NEW.pharmacist_id IS NULL THEN RAISE EXCEPTION 'pharmacist_id required to %', NEW.status; END IF;
    IF NEW.decision_at IS NULL THEN NEW.decision_at = NOW(); END IF;
    IF NEW.status = 'APPROVED' AND NEW.approved_at IS NULL THEN NEW.approved_at = NEW.decision_at; END IF;
    IF NEW.status = 'APPROVED' AND NEW.approved_by IS NULL THEN NEW.approved_by = NEW.pharmacist_id; END IF;
  ELSIF NEW.status = 'DISPENSED' THEN
    IF NEW.pharmacist_id IS NULL THEN RAISE EXCEPTION 'pharmacist_id required to DISPENSE'; END IF;
    IF NEW.dispensed_at IS NULL THEN NEW.dispensed_at = NOW(); END IF;
    IF NEW.dispensed_by IS NULL THEN NEW.dispensed_by = NEW.pharmacist_id; END IF;
END IF;
RETURN NEW;
END $$;

CREATE TRIGGER prescriptions_state
    BEFORE INSERT OR UPDATE ON prescriptions
                         FOR EACH ROW EXECUTE FUNCTION presc_state_enforcer();

-- =========================
--  Prescription Items
-- =========================
CREATE TABLE prescription_items (
                                    id               BIGSERIAL PRIMARY KEY,
                                    prescription_id  BIGINT NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
                                    medicine_id      BIGINT REFERENCES medicines(id) ON DELETE SET NULL,
                                    medicine_name    VARCHAR(100) NOT NULL,
                                    dosage           VARCHAR(50)  NOT NULL,
                                    quantity         INT NOT NULL CHECK (quantity > 0),
                                    qty_dispensed    INT NOT NULL DEFAULT 0 CHECK (qty_dispensed >= 0),
                                    status           item_status2 NOT NULL DEFAULT 'PENDING',
                                    batch_id         BIGINT REFERENCES medicine_batches(id) ON DELETE SET NULL
);
CREATE INDEX idx_items_by_prescription ON prescription_items(prescription_id);
CREATE INDEX idx_items_medicine       ON prescription_items(medicine_id);

-- =========================
--  Activity / Audit
-- =========================
CREATE TABLE activity_logs (
                               id          BIGSERIAL PRIMARY KEY,
                               user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
                               action      TEXT NOT NULL,
                               entity_type VARCHAR(50),
                               entity_id   BIGINT,
                               metadata    JSONB,
                               created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_logs_user_time  ON activity_logs(user_id, created_at);
CREATE INDEX idx_logs_entity     ON activity_logs(entity_type, entity_id);
CREATE INDEX idx_logs_meta_gin   ON activity_logs USING gin (metadata);

-- =========================
--  Views (mask national id)
-- =========================
CREATE OR REPLACE VIEW v_doctors AS
SELECT d.id, d.user_id, d.specialty, d.bio,
       mask_nid(u.national_id) AS national_id_masked
FROM doctors d JOIN users u ON u.id = d.user_id;

CREATE OR REPLACE VIEW v_patients AS
SELECT p.id, p.user_id, p.date_of_birth, p.gender, p.medical_history,
       mask_nid(u.national_id) AS national_id_masked
FROM patients p JOIN users u ON u.id = p.user_id;


-- 1) Allow NULL emails
ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL;

-- (Postgres UNIQUE allows many NULLs, so your existing UNIQUE is fine.)

-- 2) Make phone unique when present (optional but useful)
CREATE UNIQUE INDEX IF NOT EXISTS uniq_users_phone_nonnull
    ON users (phone)
    WHERE phone IS NOT NULL;

-- 3) Enforce role-based rules: staff must have email; patients need email or phone
CREATE OR REPLACE FUNCTION ensure_contact_by_role()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  -- staff must have email
  IF NEW.role IN ('ADMIN','RECEPTIONIST','PHARMACIST','DOCTOR') THEN
    IF NEW.email IS NULL OR btrim(NEW.email) = '' THEN
      RAISE EXCEPTION 'email is required for role %', NEW.role;
END IF;
END IF;

  -- patients need at least one contact
  IF NEW.role = 'PATIENT' THEN
    IF (NEW.email IS NULL OR btrim(NEW.email) = '')
       AND (NEW.phone IS NULL OR btrim(NEW.phone) = '') THEN
      RAISE EXCEPTION 'patient must have at least one contact: email or phone';
END IF;
END IF;

RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS users_need_email ON users;
CREATE TRIGGER users_need_email
    BEFORE INSERT OR UPDATE ON users
                         FOR EACH ROW EXECUTE FUNCTION ensure_contact_by_role();

-- 1) أضِف gender إلى users (مبدئيًا NULL عشان نعبّي البيانات القديمة)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS gender gender_type;

-- 2) تعبئة أولية من المرضى
UPDATE users u
SET gender = p.gender
    FROM patients p
WHERE p.user_id = u.id
  AND u.gender IS NULL;

-- 3) (اختياري لكن مُستحسن) إلزام توفر gender لأدوار معيّنة
--    لو بدك تلزمه للجميع: شغّل السطر SET NOT NULL
--    لو بدك تلزمه لأدوار محددة: استخدم التريغر بالأسفل
-- ALTER TABLE users ALTER COLUMN gender SET NOT NULL;

-- 3-b) تريغر: يلزم gender لبعض الأدوار (مثلاً لكل المستخدمين)
CREATE OR REPLACE FUNCTION ensure_gender_present()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  -- غيّر الشرط لو بدك تلزمه لأدوار معينة فقط
  IF NEW.gender IS NULL THEN
    RAISE EXCEPTION 'gender is required';
END IF;
RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS users_need_gender ON users;
CREATE TRIGGER users_need_gender
    BEFORE INSERT OR UPDATE ON users
                         FOR EACH ROW EXECUTE FUNCTION ensure_gender_present();

-- 4) حدّث الـ VIEW اللي كانت تقرأ من patients.gender
CREATE OR REPLACE VIEW v_patients AS
SELECT p.id,
       p.user_id,
       p.date_of_birth,
       u.gender,               -- <-- بدل p.gender
       p.medical_history,
       mask_nid(u.national_id) AS national_id_masked
FROM patients p
         JOIN users u ON u.id = p.user_id;

-- 5) احذف عمود gender من patients بعد ما كل شيء صار جاهز
ALTER TABLE patients
DROP COLUMN IF EXISTS gender;

-- (لو عندك أي فهارس/أكواد كانت تشير إلى patients.gender، احذفها قبل هذا السطر)


--      اضافة حالة الدكتور
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'doctor_status') THEN
CREATE TYPE doctor_status AS ENUM (
      'AVAILABLE',
      'IN_APPOINTMENT',
      'ON_BREAK'
    );
END IF;
END $$;

-- 2) أضف العمود للجدول
ALTER TABLE doctors
    ADD COLUMN IF NOT EXISTS availability_status doctor_status NOT NULL DEFAULT 'AVAILABLE';

-- 3) أضف فهرس للبحث السريع
CREATE INDEX IF NOT EXISTS idx_doctor_availability
    ON doctors(availability_status);



------------------------------------------------------------------
-- دالة الاشعار في الداتا بيز انه صار عملية اضافة موعد
-- 1) دالة ترسل إشعارًا عند أي تغيير على المواعيد
CREATE OR REPLACE FUNCTION notify_appointments_changed()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  -- نرسل معرف الموعد كـ payload (إن وُجد)
  PERFORM pg_notify('appointments_changed',
                    COALESCE(NEW.id::text, OLD.id::text, ''));
RETURN NULL; -- AFTER triggers لا تحتاج إرجاع الصف
END;
$$;


-- 2) تريغر بعد الإدراج
DROP TRIGGER IF EXISTS trg_appt_notify_insert ON appointments;
CREATE TRIGGER trg_appt_notify_insert
    AFTER INSERT ON appointments
    FOR EACH ROW
    EXECUTE FUNCTION notify_appointments_changed();

-- 3) تريغر بعد التعديل
DROP TRIGGER IF EXISTS trg_appt_notify_update ON appointments;
CREATE TRIGGER trg_appt_notify_update
    AFTER UPDATE ON appointments
    FOR EACH ROW
    EXECUTE FUNCTION notify_appointments_changed();

-- 4) تريغر بعد الحذف
DROP TRIGGER IF EXISTS trg_appt_notify_delete ON appointments;
CREATE TRIGGER trg_appt_notify_delete
    AFTER DELETE ON appointments
    FOR EACH ROW
    EXECUTE FUNCTION notify_appointments_changed();


-- في حال بدي اشيلها
-- DROP TRIGGER IF EXISTS trg_appt_notify_insert ON appointments;
-- DROP TRIGGER IF EXISTS trg_appt_notify_update ON appointments;
-- DROP TRIGGER IF EXISTS trg_appt_notify_delete ON appointments;
-- DROP FUNCTION IF EXISTS notify_appointments_changed();

------------------------------------------------------------------
-- قناة الإشعارات
-- سيصلك payload بسيط (JSON) لكن احنا بس بنستخدم الإشعار نفسه

-- 1) دالة ترسل إشعارًا عند أي تغيير على جدول المرضى

CREATE OR REPLACE FUNCTION notify_patients_changed() RETURNS trigger AS $$
BEGIN
  PERFORM pg_notify(
    'patients_changed',
    json_build_object(
      'op', TG_OP,
      'patient_id', COALESCE(NEW.id, OLD.id),
      'user_id',    COALESCE(NEW.user_id, OLD.user_id)
    )::text
  );
RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_patients_notify_insupddel ON patients;
CREATE TRIGGER trg_patients_notify_insupddel
    AFTER INSERT OR UPDATE OR DELETE ON patients
    FOR EACH ROW
    EXECUTE FUNCTION notify_patients_changed();


-- لو تم تعديل بيانات المستخدم (full_name, phone, ...الخ)
-- المرتبط بمريض، برضه نبعث إشعار.
CREATE OR REPLACE FUNCTION notify_users_patient_changed() RETURNS trigger AS $$
BEGIN
  IF EXISTS (SELECT 1 FROM patients p WHERE p.user_id = COALESCE(NEW.id, OLD.id)) THEN
    PERFORM pg_notify(
      'patients_changed',
      json_build_object(
        'op', TG_OP,
        'user_id', COALESCE(NEW.id, OLD.id)
      )::text
    );
END IF;
RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_notify_for_patients ON users;
CREATE TRIGGER trg_users_notify_for_patients
    AFTER UPDATE OR DELETE ON users
FOR EACH ROW
EXECUTE FUNCTION notify_users_patient_changed();


-- مثال منع تداخل لنفس الطبيب
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE appointments
    ADD CONSTRAINT no_overlap_per_doctor
    EXCLUDE USING gist (doctor_id WITH =, appt_range WITH &&);



-- 1) أضف العمود للأطباء (لو مش موجود):
ALTER TABLE doctors
    ADD COLUMN IF NOT EXISTS room_number VARCHAR(100) UNIQUE;

-- 2) تعبئة أولية لرقم الغرفة من آخر موعد معروف لكل دكتور (إن وُجدت غرف محفوظة سابقًا):
WITH last_rooms AS (
    SELECT a.doctor_id,
           (ARRAY_AGG(a.location ORDER BY a.appointment_date DESC))[1] AS room
FROM appointments a
WHERE a.location IS NOT NULL
GROUP BY a.doctor_id
    )
UPDATE doctors d
SET room_number = lr.room
    FROM last_rooms lr
WHERE lr.doctor_id = d.id
  AND d.room_number IS NULL;

-- 3) احذف العمود من appointments:
ALTER TABLE appointments DROP COLUMN IF EXISTS location;


// رجعت location على جدول المواعيد
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS location varchar(100);
-- افتراضيًا عند الإدخال: خذ غرفة الدكتور
CREATE OR REPLACE FUNCTION appt_default_room()
RETURNS trigger AS $$
BEGIN
  IF NEW.location IS NULL THEN
SELECT room_number INTO NEW.location FROM doctors WHERE id = NEW.doctor_id;
END IF;
RETURN NEW;
END; $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_appt_default_room ON appointments;
CREATE TRIGGER trg_appt_default_room
    BEFORE INSERT ON appointments
    FOR EACH ROW
    EXECUTE FUNCTION appt_default_room();

-- (اختياري) منع تعارضات دقيقة على نفس الغرفة ونفس الـ start:
-- مبدئيًا على نفس لحظة البدء (بدون مدة/تداخل):
CREATE UNIQUE INDEX IF NOT EXISTS uq_room_start
    ON appointments (location, appointment_date)
    WHERE location IS NOT NULL;

-- (اختياري) منع تعارض الدكتور:
CREATE UNIQUE INDEX IF NOT EXISTS uq_doctor_start
    ON appointments (doctor_id, appointment_date);

-- اخر تعديلات الداتابيز



-- 0) إصلاح ازدواج قيود التداخل (اختر واحدًا فقط)
-- إن كنت راضيًا عن الاسم الأول "no_doctor_overlap"، احذف الثاني:
ALTER TABLE appointments
DROP CONSTRAINT IF EXISTS no_overlap_per_doctor;

-- 1) السماح بوصفة دون موعد (اختياري حسب رغبتك)
-- اجعل appointment_id قابلاً لأن يكون NULL
ALTER TABLE prescriptions
    ALTER COLUMN appointment_id DROP NOT NULL;

-- عدّل presc_match_appt ليعمل فقط إذا appointment_id ليس NULL
CREATE OR REPLACE FUNCTION presc_match_appt()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE a_doctor BIGINT; a_patient BIGINT;
BEGIN
  IF NEW.appointment_id IS NOT NULL THEN
SELECT doctor_id, patient_id INTO a_doctor, a_patient
FROM appointments WHERE id = NEW.appointment_id;
IF a_doctor IS NULL THEN
      RAISE EXCEPTION 'Invalid appointment %', NEW.appointment_id;
END IF;
    IF NEW.doctor_id <> a_doctor OR NEW.patient_id <> a_patient THEN
      RAISE EXCEPTION 'Prescription doctor/patient must match appointment';
END IF;
END IF;
RETURN NEW;
END $$;

-- 2) قيود تكامل الدواء/الدفعة في عناصر الوصفة (بدون Subqueries داخل CHECK)
-- PostgreSQL لا يسمح بـ subqueries داخل CHECK بشكل يعتمد عليه،
-- لذلك نستبدلها بـ TRIGGER يفرض التكامل ويعاير الاسم تلقائياً.

CREATE OR REPLACE FUNCTION enforce_item_med_integrity()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
v_med_name  VARCHAR(100);
  v_batch_med BIGINT;
BEGIN
  -- لازم يتوفر على الأقل واحد: medicine_id أو medicine_name
  IF (NEW.medicine_id IS NULL AND (NEW.medicine_name IS NULL OR btrim(NEW.medicine_name) = '')) THEN
    RAISE EXCEPTION 'Either medicine_id or medicine_name must be provided';
END IF;

  -- لو medicine_id موجود، تحقّق من صحته وعدّل الاسم لاسم الدواء الرسمي
  IF NEW.medicine_id IS NOT NULL THEN
SELECT m.name INTO v_med_name FROM medicines m WHERE m.id = NEW.medicine_id;
IF v_med_name IS NULL THEN
      RAISE EXCEPTION 'Invalid medicine_id: %', NEW.medicine_id;
END IF;
    IF NEW.medicine_name IS DISTINCT FROM v_med_name THEN
      NEW.medicine_name := v_med_name;
END IF;
END IF;

  -- لو batch_id موجود، لازم ينتمي لنفس الدواء
  IF NEW.batch_id IS NOT NULL THEN
SELECT mb.medicine_id INTO v_batch_med FROM medicine_batches mb WHERE mb.id = NEW.batch_id;
IF v_batch_med IS NULL THEN
      RAISE EXCEPTION 'Invalid batch_id: %', NEW.batch_id;
END IF;

    -- لو medicine_id غير موجود، خد قيمته من الدفعة وسوّي الاسم
    IF NEW.medicine_id IS NULL THEN
      NEW.medicine_id := v_batch_med;
SELECT m.name INTO v_med_name FROM medicines m WHERE m.id = NEW.medicine_id;
IF NEW.medicine_name IS DISTINCT FROM v_med_name THEN
        NEW.medicine_name := v_med_name;
END IF;

    -- لو موجود لكنه لا يطابق دواء الدفعة → خطأ
    ELSIF v_batch_med IS DISTINCT FROM NEW.medicine_id THEN
      RAISE EXCEPTION 'Batch % belongs to medicine %, but row has medicine_id %',
                      NEW.batch_id, v_batch_med, NEW.medicine_id;
END IF;
END IF;

RETURN NEW;
END
$$;

DROP TRIGGER IF EXISTS tri_item_med_integrity ON prescription_items;
CREATE TRIGGER tri_item_med_integrity
    BEFORE INSERT OR UPDATE ON prescription_items
                         FOR EACH ROW
                         EXECUTE FUNCTION enforce_item_med_integrity();


ALTER TABLE prescription_items
    ADD COLUMN IF NOT EXISTS dose INT,
    ADD COLUMN IF NOT EXISTS freq_per_day INT,
    ADD COLUMN IF NOT EXISTS duration_days INT,
    ADD COLUMN IF NOT EXISTS strength VARCHAR(50),
    ADD COLUMN IF NOT EXISTS form VARCHAR(30),
    ADD COLUMN IF NOT EXISTS route VARCHAR(30),
    ADD COLUMN IF NOT EXISTS notes TEXT;

-- 2) لو بدك تغيّر dosage لنص عرض فقط
ALTER TABLE prescription_items
ALTER COLUMN dosage TYPE VARCHAR(255); -- أو TEXT


DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'prescription_items'
      AND column_name = 'dosage_text'
  ) THEN
ALTER TABLE prescription_items
    ADD COLUMN dosage_text TEXT
        GENERATED ALWAYS AS (
            btrim(
                /* نجمع بس العناصر غير الفارغة بأسلوب immutable بالكامل */
                    regexp_replace(
                        /* نُدخل الفواصل " • " يدويًا، ثم نحذف المكرّر/الزائد لاحقًا */
                            COALESCE(NULLIF(strength,''), '') ||
                            CASE WHEN strength IS NOT NULL AND btrim(strength) <> '' AND form IS NOT NULL AND btrim(form) <> '' THEN ' • ' ELSE '' END ||
                            COALESCE(NULLIF(form,''), '') ||
                            CASE WHEN ( (strength IS NOT NULL AND btrim(strength) <> '') OR (form IS NOT NULL AND btrim(form) <> '') )
                                AND (dose IS NOT NULL OR (freq_per_day IS NOT NULL AND duration_days IS NOT NULL) OR (route IS NOT NULL AND btrim(route) <> '') OR (notes IS NOT NULL AND btrim(notes) <> '') )
                                     THEN ' • ' ELSE '' END ||
                            COALESCE(dose::text, '') ||
                            CASE WHEN dose IS NOT NULL AND (freq_per_day IS NOT NULL AND duration_days IS NOT NULL) THEN ' • ' ELSE '' END ||
                            COALESCE(
                                    CASE WHEN freq_per_day IS NOT NULL AND duration_days IS NOT NULL
                                             THEN (freq_per_day::text || 'x/day · ' || duration_days::text || 'd')
                                        END, ''
                            ) ||
                            CASE WHEN ( (dose IS NOT NULL) OR (freq_per_day IS NOT NULL AND duration_days IS NOT NULL) )
                                AND (route IS NOT NULL AND btrim(route) <> '') THEN ' • ' ELSE '' END ||
                            COALESCE(NULLIF(route,''), '') ||
                            CASE WHEN ( (dose IS NOT NULL) OR (freq_per_day IS NOT NULL AND duration_days IS NOT NULL) OR (route IS NOT NULL AND btrim(route) <> '') )
                                AND (notes IS NOT NULL AND btrim(notes) <> '') THEN ' • ' ELSE '' END ||
                            COALESCE(NULLIF(notes,''), ''),
                            '(^(\s*•\s*)+)|(•\s*•)',  -- يشيل نقاط "•" البادئة أو المكرّرة
                            'g'
                    )
            )
            ) STORED;
END IF;
END $$;


ALTER TABLE prescription_items
    ALTER COLUMN dosage DROP NOT NULL;

// to make it easiy to get the data in fast way
CREATE TRIGGER inv_tx_after_upd
    AFTER UPDATE ON inventory_transactions
    FOR EACH ROW EXECUTE FUNCTION inv_tx_after_change();

CREATE TRIGGER inv_tx_after_del
    AFTER DELETE ON inventory_transactions
    FOR EACH ROW EXECUTE FUNCTION inv_tx_after_change();



SELECT b.id, b.medicine_id AS batch_med, 1 AS tx_med
FROM medicine_batches b
WHERE b.id = 55;

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
WHERE balance >= 30                 -- يكفي للصرف
  AND expiry_date >= CURRENT_DATE   -- غير منتهية
ORDER BY expiry_date ASC
    LIMIT 1;


ALTER TYPE item_status2 RENAME VALUE 'PARTIAL' TO 'COMPLETED';

ALTER TYPE item_status2 RENAME VALUE 'DISPENSED' TO 'APPROVED';


-- 1) نوع وحدة الأساس للدواء
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'med_unit') THEN
CREATE TYPE med_unit AS ENUM ('TABLET','CAPSULE','SYRUP','SUSPENSION','INJECTION','CREAM','OINTMENT','DROPS');
END IF;
END $$;

-- 2) أعمدة التغليف على medicines (كلها NULLABLE وآمنة)
ALTER TABLE medicines
    ADD COLUMN IF NOT EXISTS base_unit med_unit,
    ADD COLUMN IF NOT EXISTS tablets_per_blister INT,
    ADD COLUMN IF NOT EXISTS blisters_per_box   INT,
    ADD COLUMN IF NOT EXISTS ml_per_bottle      INT,
    ADD COLUMN IF NOT EXISTS grams_per_tube     INT,
    ADD COLUMN IF NOT EXISTS split_allowed      BOOLEAN;

-- (اختياري) حدود منطقية ومنع القيم السالبة
ALTER TABLE medicines
    ADD CONSTRAINT IF NOT EXISTS chk_meds_pack_positive
    CHECK (
    (tablets_per_blister IS NULL OR tablets_per_blister > 0) AND
    (blisters_per_box   IS NULL OR blisters_per_box   > 0) AND
    (ml_per_bottle      IS NULL OR ml_per_bottle      > 0) AND
    (grams_per_tube     IS NULL OR grams_per_tube     > 0)
    );


-- 3) نوع وحدات التغليف المقترحة/المعتمدة
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'pack_unit') THEN
CREATE TYPE pack_unit AS ENUM ('UNIT','BLISTER','BOX','BOTTLE','TUBE');
END IF;
END $$;

-- 4) أعمدة الاقتراح والاعتماد على prescription_items (NULLABLE وآمنة)
ALTER TABLE prescription_items
    ADD COLUMN IF NOT EXISTS qty_units_requested     INT,
    ADD COLUMN IF NOT EXISTS suggested_unit          pack_unit,
    ADD COLUMN IF NOT EXISTS suggested_count         INT,
    ADD COLUMN IF NOT EXISTS suggested_units_total   INT,
    ADD COLUMN IF NOT EXISTS approved_unit           pack_unit,
    ADD COLUMN IF NOT EXISTS approved_count          INT,
    ADD COLUMN IF NOT EXISTS approved_units_total    INT;


ALTER TABLE patients ENABLE TRIGGER trg_patients_notify_insupddel;
