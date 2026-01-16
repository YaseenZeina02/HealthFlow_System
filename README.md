# HealthFlow

HealthFlow is a desktop application for managing daily operations in local health centers and pharmacies. It provides patient management, appointments scheduling, doctor dashboards, and pharmacy inventory — with emphasis on a smooth JavaFX UI and secure data handling.

## Features
- Patient management: register, search, and filter patients
- Appointments: create, edit, cancel; filter by date/status/doctor
- Doctor dashboard: daily schedule, quick filters, live counters
- Pharmacy: inventory and dispensing workflows
- Secure copy/export: CSV/XLSX export with formula-injection protection and control-char stripping
- Consistent table UX: unified styles and non-intrusive copy menus via TableUtils

## Tech Stack
- Java 17+
- JavaFX 17+
- Maven
- PostgreSQL
- Optional: Apache POI (for .xlsx export)

## Getting Started
1. Requirements
   - JDK 17+
   - Maven 3.9+
   - PostgreSQL 13+
2. Configuration
   - Create a database and apply db.sql (found at HealthFlow/db.sql)
   - Configure DB connection in `src/main/java/com/example/healthflow/db/Database.java`
3. Build and Run
   - Build: `mvn -q -DskipTests package`
   - Run: `mvn -q javafx:run`

## Appointments (Reception)
- Insert: choose Specialty → Doctor → Time slot from the form, or fill a draft row in the table and select it, then click Insert.
  - The app commits any in-cell edit before inserting. Past-time slots are rejected.
- Update: select a saved appointment row, edit the fields (doctor/date/time/status/location), then click Update.
  - The app commits any active edit and validates values before applying the update. It will also resolve the doctor from the displayed name if the ID hasn’t been set yet.
- Delete: select a saved appointment row and click Delete.
  - If a draft row (unsaved, id<=0) is selected, it is removed from the table only.

## Security and Data Protection
- Clipboard/export hardening in TableUtils:
  - All exported text is sanitized to strip control characters.
  - Leading = + - @ are prefixed with an apostrophe to neutralize CSV/Excel formula execution.
- Non-intrusive copy behavior respects editable and ComboBox table columns and does not override their cell factories.

## Troubleshooting
- “Select specialty, doctor and time slot.” when inserting:
  - Ensure a doctor, date, and time slot are chosen in the form, or a draft row is selected with those values filled. Click outside any editing cell to commit the edit, then click Insert again.
- “Select an appointment row first.” when updating/deleting:
  - Make sure a row is actually selected in the table. If you were editing a cell, click Update/Delete again; the controller now commits the edit before reading selection.
- ComboBox columns not showing values:
  - TableUtils avoids overriding ComboBox/ChoiceBox or editable columns; ensure your controller sets cellFactory/cellValueFactory correctly.

## Contributing
- Use minimal, focused changes for fixes.
- Prefer utilities in `TableUtils` for consistent behavior.
- Follow JavaFX threading rules: only touch UI on the FX Application Thread.

## License
Proprietary/Academic use for Graduation Project. Consult project maintainers for reuse.

## 📸 Screenshots

### 🔐 Authentication
![Login Screen](docs/images/Login.png)

---

### 🧑‍💼 Reception Module

| Dashboard | Patients | Doctors |
|----------|----------|---------|
| ![](docs/images/Reception_Dashboard.png) | ![](docs/images/Reception_Patients.png) | ![](docs/images/Reception_Doctor.png) |

---

### 👨‍⚕️ Doctor Module

| Appointments | Patients | Prescriptions |
|--------------|----------|---------------|
| ![](docs/images/Doctor_Appointments.png) | ![](docs/images/Doctor_Patients.png) | ![](docs/images/Doctor_Prescription.png) |

---

### 💊 Pharmacy Module

| Dashboard | Inventory |
|----------|-----------|
| ![](docs/images/Pharmacy_Dashboard.png) | ![](docs/images/Pharmacy_Inventory.png) |

| Prescriptions | Reports |
|---------------|---------|
| ![](docs/images/Pharmacy_Prescriptions.png) | ![](docs/images/Pharmacy_Reports.png) |