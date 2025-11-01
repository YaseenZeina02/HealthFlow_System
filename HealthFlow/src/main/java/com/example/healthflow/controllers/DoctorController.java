package com.example.healthflow.controllers;

import com.example.healthflow.dao.DoctorDAO;
import com.example.healthflow.db.Database;
import com.example.healthflow.model.Role;
import com.example.healthflow.model.User;
import com.example.healthflow.model.dto.MedicineRow;
import com.example.healthflow.model.dto.PrescItemRow;
import com.example.healthflow.dao.PrescriptionItemDAO;
import com.example.healthflow.dao.PrescriptionDAO;
import com.example.healthflow.model.PrescriptionItem;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.service.AuthService.Session;
import com.example.healthflow.service.DoctorDashboardService;
import com.example.healthflow.service.DoctorDashboardService.Appt;
import com.example.healthflow.service.DoctorDashboardService.Stats;
import com.example.healthflow.ui.ComboAnimations;
import com.example.healthflow.ui.ConnectivityBanner;
import javafx.animation.ScaleTransition;
import com.example.healthflow.ui.OnlineBindings;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;


import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DoctorController {

    /* ====== Cards / header / nav ====== */
    @FXML
    private AnchorPane Appointments;
    @FXML
    private Label TotalAppointmentsNum;

    //    @FXML private AnchorPane Appointments21;
    @FXML
    private Label PatientsNumberWithSpecificDoctor;

    //    @FXML private AnchorPane Appointments2;
    @FXML
    private Label AppointmentCompletedWithSpecificDoctor;

    //    @FXML private AnchorPane Appointments22;
    @FXML
    private Label AppointmentRemainingWithSpecificDoctor;

    @FXML
    private Button BackButton;
    @FXML
    private Button DachboardButton;
    @FXML
    private Button PatientsButton;
    @FXML
    private Button PrescriptionButton;
    @FXML
    private Button Add_Medication;
    @FXML
    private Button cancelAddMedication;
    @FXML
    private Button InsertMedicine; // to insert medicine to database
    @FXML
    private Button sendToPharmacy;

    @FXML
    private Button Update_Medication;
//    @FXML private Button Delete_Medication;

    @FXML
    private TextField medicineField;


    @FXML
    private AnchorPane CenterAnchorPane;
    @FXML
    private AnchorPane DashboardAnchorPane;
    @FXML
    private AnchorPane PatientAnchorPane;
    @FXML
    private AnchorPane PrescriptionAnchorPane;
    @FXML
    private AnchorPane PrescriptionMedicationAnchorPane;
    @FXML
    private AnchorPane AddMedicationAnchorPane;


    @FXML
    private Label DateOfDay;
    @FXML
    private Label time;
    @FXML
    private Label welcomeUser;
    @FXML
    private Label UsernameLabel;
    @FXML
    private Label UserIdLabel;
    @FXML
    private Label alertLabel;


    @FXML
    private Circle ActiveStatus;

    @FXML
    private DatePicker datePickerPatientsWithDoctorDash;

    /* ====== Dashboard table (appointments) ====== */
    @FXML
    private TableView<AppointmentRow> AppointmentsTable;
    @FXML
    private TableColumn<AppointmentRow, String> colSerialNumber;
    @FXML
    private TableColumn<AppointmentRow, String> colDoctorName;
    @FXML
    private TableColumn<AppointmentRow, String> colPatientName;
    @FXML
    private TableColumn<AppointmentRow, LocalDate> colDate;
    @FXML
    private TableColumn<AppointmentRow, String> colTime;
    @FXML
    private TableColumn<AppointmentRow, String> colStatus;
    @FXML
    private TableColumn<AppointmentRow, AppointmentRow> colAction;

    /* ====== Patients tab ====== */
    @FXML
    private TableView<PatientRow> patientTable;
    @FXML
    private TableColumn<PatientRow, String> colNationalId;
    @FXML
    private TableColumn<PatientRow, String> colName;
    @FXML
    private TableColumn<PatientRow, String> colGender;
    @FXML
    private TableColumn<PatientRow, Integer> colDob; // age
    @FXML
    private TableColumn<PatientRow, String> colMedicalHistory;
    @FXML
    private TableColumn<PatientRow, PatientRow> colAction2;
    @FXML
    private TextField search;      // patients search (future)
    @FXML
    private TextField searchLabel; // appointments search (future)
    @FXML
    private DatePicker datePickerPatientsWithDoctor;
    private FilteredList<PatientRow> filtered;
    private SortedList<PatientRow> sorted;


    /* ====== Medicine tab ====== */
    @FXML
    private TableView<PrescItemRow> TablePrescriptionItems;
    @FXML
    private TableColumn<PrescItemRow, Number> colIdx;
    @FXML
    private TableColumn<PrescItemRow, String> colMedicineName;
    @FXML
    private TableColumn<PrescItemRow, String> colDosage;
    @FXML
    private TableColumn<PrescItemRow, Integer> colDuration;
    @FXML
    private TableColumn<PrescItemRow, String> colQuantity;
    @FXML
    private TableColumn<PrescItemRow, String> colPack;

    //    @FXML private TableColumn<PrescItemRow, String>  colDiagnosis;
    @FXML
    private TableColumn<PrescItemRow, PrescItemRow> colPresesAction;
    @FXML
    private TableColumn<PrescItemRow, Integer> colDose;
    @FXML
    private TableColumn<PrescItemRow, Integer> colFreqPerDay;
    @FXML
    private TableColumn<PrescItemRow, String> colStrength;
    @FXML
    private TableColumn<PrescItemRow, String> colForm;
    @FXML
    private TableColumn<PrescItemRow, String> colRoute;

    @FXML private Button clearSelectionDach;
    @FXML private Button clearSelectionPatient;


    @FXML
    private TableColumn<PrescItemRow, String> colPresesStatus;

    // In-memory draft prescription items (source list for the table)
    private final ObservableList<PrescItemRow> prescItemsEditable = FXCollections.observableArrayList();

    // Medicine autocomplete suggestions

    // Tracks whether we're editing an existing row from the table
    private PrescItemRow editingRow = null;
    private Long currentPrescriptionId = null;
    // Current patient/doctor context for prescription
    private Long selectedPatientUserId = null;
    private String selectedPatientName = null;
    private String selectedPatientNationalId = null;
    private Long currentDoctorUserId = null;
    private String currentDoctorFullName = null;
    // Live DB notifications (appointments/patients) for real-time refresh
    private com.example.healthflow.db.notify.DbNotifications dbn;

    @FXML
    private AnchorPane rootPane;

    @FXML
    private TextArea DiagnosisTF;

    @FXML
    private Label DoctorNameLabel;

    @FXML
    private Label Dose;
    @FXML
    private TextField PatientNameTF;


    @FXML
    private Label dateWithTimePres;


    @FXML
    private TextField doseText;

    @FXML
    private TextField duration;

    @FXML
    private ComboBox<String> formCombo;

    @FXML
    private TextField freq_day;

    @FXML
    private TextArea nots_Pre;

    @FXML private Label qtyLabelCulc;


    @FXML
    private ComboBox<String> medicineName_combo;

    @FXML
    private ComboBox<String> routeCombo;

    @FXML
    private ComboBox<String> strength_combo;

    @FXML
    private Label userStatus;


    /* ====== Services / state ====== */
    private final ConnectivityMonitor monitor;
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final DoctorDashboardService svc = new DoctorDashboardService();
    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();

    private final ObservableList<AppointmentRow> apptData = FXCollections.observableArrayList();
    private FilteredList<AppointmentRow> apptFiltered = new FilteredList<>(apptData, r -> true);
    private SortedList<AppointmentRow> apptSorted = new SortedList<>(apptFiltered);
    private org.controlsfx.control.textfield.AutoCompletionBinding<String> apptSearchBinding;

    private final ObservableList<PatientRow> patientData = FXCollections.observableArrayList();

    // Refit callback for AppointmentsTable last column (Action)
    private Runnable appointmentsRefit = null;


    public DoctorController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }

    public DoctorController() {
        this(new ConnectivityMonitor());
    }


    /* ====== Nav highlight ====== */
    private static final String ACTIVE_CLASS = "current";

    private void markNavActive(Button active) {
        Button[] all = {DachboardButton, PatientsButton, PrescriptionButton};
        for (Button b : all) {
            b.getStyleClass().remove(ACTIVE_CLASS);
            if (!b.getStyleClass().contains("nav-btn")) b.getStyleClass().add("nav-btn");
        }
        if (active != null && !active.getStyleClass().contains(ACTIVE_CLASS)) {
            active.getStyleClass().add(ACTIVE_CLASS);
        }
    }

    /* ================= INIT ================= */
    @FXML
    private void initialize() {
        monitor.start();
        showDashboardPane();

        if (rootPane != null) {
            ConnectivityBanner banner = new ConnectivityBanner(monitor);
            AnchorPane.setTopAnchor(banner, 0.0);
            AnchorPane.setLeftAnchor(banner, 0.0);
            AnchorPane.setRightAnchor(banner, 0.0);
            rootPane.getChildren().add(banner);
        }

        startClock();

        DachboardButton.setOnAction(e -> showDashboardPane());
        PatientsButton.setOnAction(e -> showPatientsPane());
        PrescriptionButton.setOnAction(e -> {
            // Reset context for "prescription without appointment"
            selectedPatientUserId = null;
            selectedPatientName = null;
            selectedPatientNationalId = null;
            currentPrescriptionId = null;

            showPrescriptionPane();
            // Make PatientNameTF editable with placeholder so user can type "name • NID"
            setPatientHeader("", null, true);
        });
        Add_Medication.setOnAction(e -> showPrescriptionPaneToAddMedication());

        Add_Medication.setOnAction(e -> {
            System.out.println("[ADD_MEDICATION BUTTON] Current patient context -> userId="
                    + selectedPatientUserId + ", name=" + selectedPatientName
                    + ", NID=" + selectedPatientNationalId);
            showPrescriptionPaneToAddMedication();
        });
        cancelAddMedication.setOnAction(e -> showPrescriptionPane());


        BackButton.setOnAction(e -> goBackToLogin());

        try {
            OnlineBindings.disableWhenOffline(monitor, DachboardButton, PatientsButton);
        } catch (Throwable ignored) {
        }

        wireAppointmentsTable();
        wirePatientsTable();
        wireSearch();

        buildAppointmentSearchIndex(); // تجهيز ال-AutoComplete من أول تحميل

        wirePrescriptionItemsTable();

        ComboAnimations.applySmoothSelect(medicineName_combo, s -> s);
        ComboAnimations.delayHideOnSelect(medicineName_combo, Duration.seconds(0.1));

        ComboAnimations.applySmoothSelect(strength_combo, s -> s);
        ComboAnimations.delayHideOnSelect(strength_combo, Duration.seconds(0.1));

        ComboAnimations.applySmoothSelect(formCombo, s -> s);
        ComboAnimations.delayHideOnSelect(formCombo, Duration.seconds(0.1));

        ComboAnimations.applySmoothSelect(routeCombo, s -> s);
        ComboAnimations.delayHideOnSelect(routeCombo, Duration.seconds(0.1));
        strength_combo.setVisibleRowCount(6);   // أي ComboBox عندك
        formCombo.setVisibleRowCount(6);
        routeCombo.setVisibleRowCount(6);


        if (clearSelectionDach != null) {
            clearSelectionDach.setOnAction(e -> {
                if (AppointmentsTable != null) {
                    AppointmentsTable.getSelectionModel().clearSelection();
                }
            });
        }

        if (clearSelectionPatient != null) {
            clearSelectionPatient.setOnAction(e -> {
                if (patientTable != null) {
                    patientTable.getSelectionModel().clearSelection();
                }
            });
        }

        // === Dashboard-by-date: bind DatePicker to table + counters (no extra filtering) ===
        if (datePickerPatientsWithDoctorDash != null) {
            var todayGazaDash = java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
            datePickerPatientsWithDoctorDash.setValue(todayGazaDash);

            // Initial load (today)
            loadStatsForDateAsync(todayGazaDash);
            loadAppointmentsForDateAsync(todayGazaDash);

            // Reload whenever the date changes
            datePickerPatientsWithDoctorDash.valueProperty().addListener((obs, ov, nv) -> {
                if (nv != null) {
                    loadStatsForDateAsync(nv);
                    loadAppointmentsForDateAsync(nv);
                }
            });
        }


        // === Patients-by-date filter ===
        if (datePickerPatientsWithDoctor != null) {
            var todayGaza = java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
            datePickerPatientsWithDoctor.setValue(todayGaza);
            datePickerPatientsWithDoctor.valueProperty().addListener((obs, ov, nv) -> {
                if (nv != null) loadPatientsForDateAsync(nv);
            });
            loadPatientsForDateAsync(todayGaza);
        }


        populateMedicineCombos();
        loadMedicinesIntoCombo();


        if (PatientNameTF != null) {
            PatientNameTF.textProperty().addListener((obs, oldV, newV) -> {
                String t = (newV == null) ? "" : newV.trim();
                // استخرج كل الأرقام كهوية وطنية
                String digits = t.replaceAll("[^0-9]", "");
                selectedPatientNationalId = digits.isBlank() ? null : digits;

                // الاسم = النص بدون " • أرقام" في آخره إن وُجدت
                String nameOnly = t;
                int dot = t.lastIndexOf('•');
                if (dot >= 0) {
                    nameOnly = t.substring(0, dot).trim();
                }
                selectedPatientName = nameOnly.isBlank() ? null : nameOnly;
                // ملاحظة: selectedPatientUserId يُترك null — رح نحلّه لاحقًا إذا توفّر NID
            });
        }

        if (TablePrescriptionItems != null) {
            TablePrescriptionItems.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                boolean has = n != null;
                if (Update_Medication != null) Update_Medication.setDisable(!has);
            });
            if (Update_Medication != null) Update_Medication.setDisable(true);
        }

        // Add / Save in the Add-Medicine pane
        if (InsertMedicine != null) {
            InsertMedicine.setOnAction(e -> {

                addMedicineFromDialog();
            });
        }
        if (Update_Medication != null) {
            Update_Medication.setOnAction(e -> openEditSelectedItem());
        }

        // When doctor finishes composing the draft and wants to send to pharmacy:
        if (sendToPharmacy != null) {
            sendToPharmacy.setOnAction(e -> handleSendToPharmacy());
        }


        if (loadUserAndEnsureDoctorProfile()) {
            reloadAll();
        }
    }

    /* ================= Header time & date (12h) ================= */
    private static final java.time.ZoneId APP_TZ = java.time.ZoneId.of("Asia/Gaza");

    private void startClock() {
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, e -> time.setText(java.time.ZonedDateTime.now(APP_TZ).format(tf))),
                new KeyFrame(Duration.seconds(1))
        );
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();

        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        var todayGaza = java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
        DateOfDay.setText(todayGaza.format(df));
    }

    /* ================= Navigation ================= */
    private void showDashboardPane() {
        DashboardAnchorPane.setVisible(true);
        PatientAnchorPane.setVisible(false);
        PrescriptionAnchorPane.setVisible(false);

        markNavActive(DachboardButton);
        refitAppointmentsColumnsLater();
    }

    // Helper: schedule a refit of AppointmentsTable columns if possible
    private void refitAppointmentsColumnsLater() {
        if (appointmentsRefit != null) Platform.runLater(appointmentsRefit);
    }

    private void showPatientsPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(true);
        PrescriptionAnchorPane.setVisible(false);
        markNavActive(PatientsButton);
    }

    private void showPrescriptionPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(false);
        PrescriptionAnchorPane.setVisible(true);
        PrescriptionMedicationAnchorPane.setVisible(true);
        AddMedicationAnchorPane.setVisible(false);
//        markNavActive(InsertButton2);     //
    }

    private void showPrescriptionPaneToAddMedication() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(false);
        PrescriptionAnchorPane.setVisible(true);
        PrescriptionMedicationAnchorPane.setVisible(false);
        AddMedicationAnchorPane.setVisible(true);
//        markNavActive(InsertButton2);     //
    }

    /**
     * Clear the draft prescription items table and return to Dashboard.
     * Triggered by the doctor pressing the "sendToPharmacy" button.
     */
    private void handleSendToPharmacy() {
        try {
            // Clear the TableView content (visible rows)
            if (TablePrescriptionItems != null) {
                TablePrescriptionItems.getItems().clear();
                TablePrescriptionItems.getSelectionModel().clearSelection();
            }
            // Clear the in-memory draft list (data source)
            if (prescItemsEditable != null) {
                prescItemsEditable.clear();
            }
            // Reset transient editing context
            editingRow = null;
            currentPrescriptionId = null;
            selectedPatientUserId = null;
            selectedPatientName = null;
            selectedPatientNationalId = null;
        } catch (Exception ex) {
            // Non-fatal: show a small warning and continue to dashboard
            showWarn("Prescription", "Failed to reset draft items: " + ex.getMessage());
        }
        // Navigate back to the dashboard view
        showDashboardPane();
    }

    private void goBackToLogin() {
        Stage stage = (Stage) BackButton.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(new Navigation().Login_Fxml));
            loader.setControllerFactory(type ->
                    type == LoginController.class ? new LoginController(monitor) : null
            );

            Parent loginRoot = loader.load();

            ConnectivityBanner banner = new ConnectivityBanner(monitor);
            AnchorPane.setTopAnchor(banner, 0.0);
            AnchorPane container = new AnchorPane(loginRoot);
            container.setPrefSize(900, 600);
            AnchorPane.setTopAnchor(loginRoot, 0.0);
            AnchorPane.setRightAnchor(loginRoot, 0.0);
            AnchorPane.setBottomAnchor(loginRoot, 0.0);
            AnchorPane.setLeftAnchor(loginRoot, 0.0);

            AnchorPane root = new AnchorPane();
            AnchorPane.setTopAnchor(container, 0.0);
            AnchorPane.setRightAnchor(container, 0.0);
            AnchorPane.setBottomAnchor(container, 0.0);
            AnchorPane.setLeftAnchor(container, 0.0);
            root.getChildren().addAll(container, banner);

            stage.setTitle("HealthFlow");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            showError("Navigation", e);
        }
    }

    /* ================= User & role ================= */
    private boolean loadUserAndEnsureDoctorProfile() {
        User u = Session.get();
        if (u == null) return false;

        UsernameLabel.setText(u.getFullName());
        UserIdLabel.setText(String.valueOf(u.getId()));
        welcomeUser.setText(firstName(u.getFullName()));
        // Cache doctor info for prescription header
        currentDoctorUserId = u.getId();
        currentDoctorFullName = u.getFullName();
        if (DoctorNameLabel != null) {
            DoctorNameLabel.setText(currentDoctorFullName);
        }

        if (u.getRole() != Role.DOCTOR) {
            showWarn("Role", "This user is not a doctor.");
            return false;
        }

        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                doctorDAO.ensureProfileForUser(c, u.getId());
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                showWarn("Doctor Profile", "Could not ensure doctor profile. Please try again later.");
                return false;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (Exception ex) {
            showWarn("Connection", "Database connection failed. Please try again later.");
            return false;
        }

        return true;
    }

    /* ================= Data loads ================= */

    /**
     * Load medicine names from DB into the medicineName_combo combobox.
     */
    private void loadMedicinesIntoCombo() {
        if (medicineName_combo == null) return;
        medicineName_combo.setDisable(true);
        new Thread(() -> {
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            Exception err = null;
            try (Connection c = Database.get();
                 java.sql.PreparedStatement ps = c.prepareStatement(
                         "SELECT name FROM medicines ORDER BY name ASC");
                 java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String n = rs.getString(1);
                    if (n != null && !n.isBlank()) names.add(n);
                }
            } catch (Exception ex) {
                err = ex;
            }

            Exception finalErr = err;
            Platform.runLater(() -> {
                try {
                    if (!names.isEmpty()) {
                        medicineName_combo.getItems().setAll(names);
                    } else if (finalErr != null) {
                        toast("Failed to load medicines: " + finalErr.getMessage(), "warn");
                    }
                } finally {
                    medicineName_combo.setDisable(false);
                }
            });
        }, "load-meds-combo").start();
    }

    private void reloadAll() {
        var _dashDate = (datePickerPatientsWithDoctorDash != null && datePickerPatientsWithDoctorDash.getValue() != null)
                ? datePickerPatientsWithDoctorDash.getValue()
                : java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
        loadStatsForDateAsync(_dashDate);
        loadAppointmentsForDateAsync(_dashDate);

        var d = (datePickerPatientsWithDoctor != null && datePickerPatientsWithDoctor.getValue() != null)
                ? datePickerPatientsWithDoctor.getValue()
                : java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
        loadPatientsForDateAsync(d);
    }

    /**
     * Load dashboard counters (total/completed/remaining/unique patients) for the logged-in doctor on a specific date.
     */
    private void loadStatsForDateAsync(LocalDate date) {
        var u = Session.get();
        if (u == null || date == null) return;

        new Thread(() -> {
            int total = 0, completed = 0, remaining = 0, patientsToday = 0;
            Exception err = null;
            try (Connection c = Database.get()) {
                String sql = """
                        SELECT
                            COUNT(*)                                                              AS total,
                            COUNT(*) FILTER (WHERE a.status = 'COMPLETED')                        AS completed,
                            COUNT(DISTINCT p.user_id)                                             AS patients_today,
                            COUNT(*) FILTER (WHERE a.status NOT IN ('COMPLETED','CANCELLED','NO_SHOW')) AS remaining
                        FROM appointments a
                        JOIN doctors d  ON d.id = a.doctor_id
                        JOIN patients p ON p.id = a.patient_id
                        WHERE d.user_id = ? AND a.appointment_date::date = ?
                        """;
                try (var ps = c.prepareStatement(sql)) {
                    ps.setLong(1, u.getId());
                    ps.setDate(2, java.sql.Date.valueOf(date));
                    try (var rs = ps.executeQuery()) {
                        if (rs.next()) {
                            total = rs.getInt("total");
                            completed = rs.getInt("completed");
                            patientsToday = rs.getInt("patients_today");
                            remaining = rs.getInt("remaining");
                        }
                    }
                }
            } catch (Exception ex) {
                err = ex;
            }

            final int fTotal = total;
            final int fCompleted = completed;
            final int fRemaining = remaining;
            final int fPatients = patientsToday;
            final Exception fErr = err;

            System.out.printf("[Stats] date=%s userId=%d -> total=%d completed=%d remaining=%d patients=%d\n",
                    String.valueOf(date), u.getId(), fTotal, fCompleted, fRemaining, fPatients);

            Platform.runLater(() -> {
                setTextSafe(TotalAppointmentsNum, String.valueOf(fTotal));
                setTextSafe(AppointmentCompletedWithSpecificDoctor, String.valueOf(fCompleted));
                setTextSafe(AppointmentRemainingWithSpecificDoctor, String.valueOf(fRemaining));
                setTextSafe(PatientsNumberWithSpecificDoctor, String.valueOf(fPatients));

                if (fErr != null) {
                    showWarn("Stats", "Failed to load stats from DB: " + fErr.getMessage());
                }
            });
        }, "doc-stats-by-date").start();
    }

    private void loadAppointmentsForDateTableAsync(java.time.LocalDate date) {
        // استخدم اللودر الموحّد المتوافق مع AppointmentRow.of(Appt)
        loadAppointmentsForDateAsync(date);
    }

    /**
     * Build the search suggestions for the Appointments table and wire the live filter.
     */
    private void buildAppointmentSearchIndex() {
        if (searchLabel == null) return;

        java.util.LinkedHashSet<String> suggestions = new java.util.LinkedHashSet<>();
        for (AppointmentRow r : apptData) {
            if (r == null) continue;
            String idStr = String.valueOf(r.getId());
            String name = nullToEmpty(r.getPatientName());
            String nid = nullToEmpty(r.getNationalId());
            String status = nullToEmpty(r.getStatus());
            String dateStr = (r.getDate() == null) ? "" : r.getDate().toString();
            String timeStr = nullToEmpty(r.getTimeStr());
            for (String p : java.util.Arrays.asList(idStr, name, nid, status, dateStr, timeStr)) {
                String t = nullToEmpty(p).trim();
                if (!t.isEmpty()) suggestions.add(t);
            }
        }

        try {
            if (apptSearchBinding != null) apptSearchBinding.dispose();
            apptSearchBinding = org.controlsfx.control.textfield.TextFields.bindAutoCompletion(searchLabel, suggestions);
        } catch (IllegalAccessError err) {
            apptSearchBinding = null;
            System.out.println("[AutoComplete] Disabled due to module access error: " + err.getMessage());
        } catch (Throwable t) {
            // Any other runtime issue with ControlsFX; keep the app running.
            apptSearchBinding = null;
            System.out.println("[AutoComplete] Disabled: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }

        searchLabel.textProperty().removeListener(searchAppointmentsListener);
        searchLabel.textProperty().addListener(searchAppointmentsListener);
    }

    // listener لفلترة جدول المواعيد حسب النص
    private final javafx.beans.value.ChangeListener<String> searchAppointmentsListener = (obs, ov, nv) -> {
        String q = (nv == null) ? "" : nv.trim().toLowerCase();
        if (q.isEmpty()) {
            apptFiltered.setPredicate(r -> true);
            return;
        }
        apptFiltered.setPredicate(r -> {
            if (r == null) return false;
            String idStr = String.valueOf(r.getId());
            String name = nullToEmpty(r.getPatientName()).toLowerCase();
            String nid = nullToEmpty(r.getNationalId()).toLowerCase();
            String status = nullToEmpty(r.getStatus()).toLowerCase();
            String dateStr = (r.getDate() == null) ? "" : r.getDate().toString().toLowerCase();
            String timeStr = nullToEmpty(r.getTimeStr()).toLowerCase();
            return idStr.contains(q)
                    || name.contains(q)
                    || nid.contains(q)
                    || status.contains(q)
                    || dateStr.contains(q)
                    || timeStr.contains(q);
        });
    };

    private static boolean safeContains(String s, String q) {
        return s != null && q != null && s.toLowerCase().contains(q);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }


    private void loadPatientsAsync() { // kept for compatibility in other calls
        var d = (datePickerPatientsWithDoctor != null && datePickerPatientsWithDoctor.getValue() != null)
                ? datePickerPatientsWithDoctor.getValue()
                : java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
        loadPatientsForDateAsync(d);
    }

    /**
     * Load patients who have an appointment with the logged-in doctor on the given date.
     */
    private void loadPatientsForDateAsync(java.time.LocalDate date) {
        var u = Session.get();
        if (u == null || date == null) return;

        new Thread(() -> {
            try (Connection c = Database.get()) {
                String sql = """
                        SELECT DISTINCT
                             pu.national_id,
                             pu.full_name AS patient_name,
                             pu.gender,            -- من users
                             p.date_of_birth,      -- من patients
                             p.medical_history,
                             p.user_id AS patient_user_id
                         FROM appointments a
                         JOIN doctors d  ON d.id = a.doctor_id
                         JOIN users   du ON du.id = d.user_id          -- doctor user
                         JOIN patients p ON p.id = a.patient_id
                         JOIN users   pu ON pu.id = p.user_id          -- patient user
                         WHERE du.id = ?                                -- (user_id للدكتور المسجّل)
                           AND a.appointment_date::date = ?            -- التاريخ المختار من الـ DatePicker
                         ORDER BY patient_name;
                        """;
                java.util.ArrayList<PatientRow> rows = new java.util.ArrayList<>();
                try (var ps = c.prepareStatement(sql)) {
                    ps.setLong(1, u.getId());
                    ps.setDate(2, java.sql.Date.valueOf(date));
                    try (var rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String nid = rs.getString("national_id");
                            String name = rs.getString("patient_name");
                            String gender = rs.getString("gender");
                            java.sql.Date dob = rs.getDate("date_of_birth");
                            String mh = rs.getString("medical_history");
                            int age = (dob == null) ? 0 : ageFromDob(dob.toLocalDate());
                            PatientRow pr = new PatientRow(nid, name, gender, age, mh);
                            long uid = rs.getLong("patient_user_id");
                            if (uid > 0) pr.setUserId(uid);
                            rows.add(pr);
                        }
                    }
                }
                Platform.runLater(() -> patientData.setAll(rows));
            } catch (Exception ex) {
                System.out.println("[PatientsByDate] primary query failed: " + ex.getMessage());
                // Fallback by doctor email
                try (Connection c2 = Database.get()) {
                    String email = (u.getEmail() == null) ? "" : u.getEmail().trim().toLowerCase();
                    String sql2 = """
                            SELECT DISTINCT pu.national_id,
                                            pu.full_name          AS patient_name,
                                            pu.gender,
                                            pu.date_of_birth,
                                            p.medical_history,
                                            p.user_id             AS patient_user_id
                            FROM appointments a
                            JOIN doctors d  ON d.id = a.doctor_id
                            JOIN users   du ON du.id = d.user_id   -- doctor user
                            JOIN patients p ON p.id = a.patient_id
                            JOIN users   pu ON pu.id = p.user_id   -- patient user
                            WHERE lower(du.email) = ? AND a.appointment_date::date = ?
                            ORDER BY patient_name
                            """;
                    java.util.ArrayList<PatientRow> rows2 = new java.util.ArrayList<>();
                    try (var ps = c2.prepareStatement(sql2)) {
                        ps.setString(1, email);
                        ps.setDate(2, java.sql.Date.valueOf(date));
                        try (var rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String nid = rs.getString("national_id");
                                String name = rs.getString("patient_name");
                                String gender = rs.getString("gender");
                                java.sql.Date dob = rs.getDate("date_of_birth");
                                String mh = rs.getString("medical_history");
                                int age = (dob == null) ? 0 : ageFromDob(dob.toLocalDate());
                                PatientRow pr = new PatientRow(nid, name, gender, age, mh);
                                long uid = rs.getLong("patient_user_id");
                                if (uid > 0) pr.setUserId(uid);
                                rows2.add(pr);
                            }
                        }
                    }
                    Platform.runLater(() -> patientData.setAll(rows2));
                } catch (Exception ex2) {
                    System.out.println("[PatientsByDate] email fallback failed: " + ex2.getMessage());
                    Platform.runLater(() -> showWarn("Patients", "Failed to load patients for the selected date."));
                }
            }
        }, "doc-patients-by-date").start();
    }

    // --- Live refresh (doctor page) ---
    private final java.util.concurrent.ScheduledExecutorService doctorAutoExec =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> { var t = new Thread(r, "doc-autorefresh"); t.setDaemon(true); return t; });
    private final java.util.concurrent.atomic.AtomicBoolean docRefreshPending = new java.util.concurrent.atomic.AtomicBoolean(false);

    // تغيير آخر تحديث (fallback polling)
    private volatile java.sql.Timestamp lastApptTs = null;
    private volatile java.sql.Timestamp lastPatientTs = null;

    // Coalesced & debounced refresh for the doctor dashboard
    private void scheduleDoctorRefresh() {
        if (!docRefreshPending.compareAndSet(false, true)) return; // already queued
        doctorAutoExec.schedule(() -> {
            javafx.application.Platform.runLater(() -> {
                try { loadTodayAppointmentsAsync(); }
                catch (Throwable t) { System.err.println("[DoctorController] loadTodayAppointmentsAsync error: " + t); }
                try {
                    java.time.LocalDate day = (datePickerPatientsWithDoctorDash != null && datePickerPatientsWithDoctorDash.getValue() != null)
                            ? datePickerPatientsWithDoctorDash.getValue()
                            : java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
                    loadStatsForDateAsync(day);
                } catch (Throwable t) {
                    System.err.println("[DoctorController] loadStatsForDateAsync error: " + t);
                }
                try {
                    if (AppointmentsTable != null) AppointmentsTable.refresh();
                    if (patientTable != null) patientTable.refresh();
                } catch (Throwable ignore) { }
                docRefreshPending.set(false);
            });
        }, 250, java.util.concurrent.TimeUnit.MILLISECONDS); // 250ms debounce
    }

    /* ================= Tables wiring ================= */

    /**
     * Backward-compatible wrapper: loads appointments for the date selected in
     * datePickerPatientsWithDoctorDash (defaults to today in APP_TZ).
     */
    private void loadTodayAppointmentsAsync() {
        LocalDate date = (datePickerPatientsWithDoctorDash != null && datePickerPatientsWithDoctorDash.getValue() != null)
                ? datePickerPatientsWithDoctorDash.getValue()
                : java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
        loadAppointmentsForDateAsync(date);
    }
    // Coalesced UI refresh when DB notifies about appointments/patients changes
//    private void onDbEventRefreshDoctor(String payload) {
//        javafx.application.Platform.runLater(() -> {
//            try {
//                // 1)Appointments: إعادة تحميل لليوم الحالي/المختار
//                loadTodayAppointmentsAsync();
//            } catch (Throwable t) {
//                System.err.println("[DoctorController] loadTodayAppointmentsAsync error: " + t);
//            }
//            try {
//                // 2) إحصائيات/رسم اليوم
//                java.time.LocalDate day = (datePickerPatientsWithDoctorDash != null &&
//                        datePickerPatientsWithDoctorDash.getValue() != null)
//                        ? datePickerPatientsWithDoctorDash.getValue()
//                        : java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
//                loadStatsForDateAsync(day);
//            } catch (Throwable t) {
//                System.err.println("[DoctorController] loadStatsForDateAsync error: " + t);
//            }
//            try {
//                if (AppointmentsTable != null) AppointmentsTable.refresh();
//                if (patientTable != null) patientTable.refresh();
//            } catch (Throwable ignore) { }
//        });
//    }

    private void onDbEventRefreshDoctor(String payload) {
        scheduleDoctorRefresh();
    }
    // Start ultra-light polling so UI stays live even if NOTIFY is missed
    private void startLightweightPollingDoctor() {
        doctorAutoExec.scheduleAtFixedRate(() -> {
            try {
                java.time.LocalDate day = (datePickerPatientsWithDoctorDash != null && datePickerPatientsWithDoctorDash.getValue() != null)
                        ? datePickerPatientsWithDoctorDash.getValue()
                        : java.time.ZonedDateTime.now(APP_TZ).toLocalDate();

                java.sql.Timestamp ts1 = fetchMaxApptUpdatedAtForDoctor(day);
                if ((lastApptTs == null && ts1 != null) || (ts1 != null && lastApptTs != null && ts1.after(lastApptTs))) {
                    lastApptTs = ts1;
                    scheduleDoctorRefresh();
                }

                java.sql.Timestamp ts2 = fetchMaxPatientUpdatedAt();
                if ((lastPatientTs == null && ts2 != null) || (ts2 != null && lastPatientTs != null && ts2.after(lastPatientTs))) {
                    lastPatientTs = ts2;
                    scheduleDoctorRefresh();
                }
            } catch (Throwable t) {
                System.err.println("[DoctorController] polling error: " + t);
            }
        }, 1200, 3000, java.util.concurrent.TimeUnit.MILLISECONDS); // يبدأ بعد 1.2s ثم كل 3s
    }

    private java.sql.Timestamp fetchMaxApptUpdatedAtForDoctor(java.time.LocalDate day) {
        final String sql = """
        SELECT MAX(a.updated_at)
        FROM appointments a
        JOIN doctors d ON d.id = a.doctor_id
        JOIN users   du ON du.id = d.user_id
        WHERE a.appointment_date::date = ? AND du.id = ?
        """;

        var u = com.example.healthflow.service.AuthService.Session.get();
        if (u == null || day == null) return null;

        try (java.sql.Connection c = com.example.healthflow.db.Database.get();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(day)); // أدق من setObject مع LocalDate
            ps.setLong(2, u.getId());

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getTimestamp(1) : null;
            }
        } catch (java.sql.SQLException e) {
            System.err.println("[DoctorController] fetchMaxApptUpdatedAtForDoctor SQL error: " + e.getMessage());
            return null;
        }
    }
    private java.sql.Timestamp fetchMaxPatientUpdatedAt() {
        final String sql = """
        SELECT MAX(GREATEST(u.updated_at, p.updated_at))
        FROM users u
        JOIN patients p ON p.user_id = u.id
        """;

        try (java.sql.Connection c = com.example.healthflow.db.Database.get();
             java.sql.PreparedStatement ps = c.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getTimestamp(1) : null;
        } catch (java.sql.SQLException e) {
            System.err.println("[DoctorController] fetchMaxPatientUpdatedAt SQL error: " + e.getMessage());
            return null;
        }
    }

    /** Start durable LISTEN/NOTIFY for doctor page (safe, idempotent). */
    private void startDbNotificationsForDoctor() {
        try {
            if (dbn != null) return;
            dbn = new com.example.healthflow.db.notify.DbNotifications();
            // أي تغيّر على المواعيد أو المرضى يهم شاشة الدكتور
            dbn.listen("appointments_changed", this::onDbEventRefreshDoctor);
            dbn.listen("patients_changed",     this::onDbEventRefreshDoctor);
            System.out.println("[DoctorController] DbNotifications wired.");
        } catch (Throwable t) {
            System.err.println("[DoctorController] DbNotifications init error: " + t);
        }
    }

    private void loadAppointmentsForDateAsync(LocalDate date) {
        var u = Session.get();
        if (u == null || date == null) return;
        new Thread(() -> {
            List<Appt> list = null;
            Exception lastErr = null;
            try {
                // Preferred service call with explicit date
                list = svc.listTodayAppointments(u.getId(), date);
            } catch (Exception ex) {
                lastErr = ex;
                System.out.println("[Appointments] service call failed: " + ex.getMessage());
                ex.printStackTrace();
            }

            // Fallback: query by doctor email + specific date
            if (list == null || list.isEmpty()) {
                String email = (u.getEmail() == null) ? "" : u.getEmail().trim().toLowerCase();
                try (Connection c = Database.get()) {
                    String sql = """
                            SELECT a.id,
                                   pu.full_name              AS patient_name,
                                   pu.national_id            AS patient_nid,
                                   a.appointment_date        AS ts,
                                   a.status,
                                   p.user_id                 AS patient_user_id,
                                   p.medical_history         AS medical_history
                            FROM appointments a
                            JOIN doctors d  ON d.id = a.doctor_id
                            JOIN users   du ON du.id = d.user_id        -- doctor user
                            JOIN patients p ON p.id = a.patient_id
                            JOIN users   pu ON pu.id = p.user_id        -- patient user
                            WHERE lower(du.email) = ? AND a.appointment_date::date = ?
                            ORDER BY a.appointment_date
                            """;
                    try (var ps = c.prepareStatement(sql)) {
                        ps.setString(1, email);
                        ps.setDate(2, java.sql.Date.valueOf(date));
                        try (var rs = ps.executeQuery()) {
                            java.util.ArrayList<Appt> fb = new java.util.ArrayList<>();
                            while (rs.next()) {
                                Appt a = new Appt();
                                a.id = rs.getLong("id");
                                a.patientName = rs.getString("patient_name");
                                a.patientNationalId = rs.getString("patient_nid");
                                java.sql.Timestamp ts = rs.getTimestamp("ts");
                                if (ts != null) {
                                    var zdt = ts.toInstant().atZone(APP_TZ);
                                    a.date = zdt.toLocalDate();
                                    a.time = zdt.toLocalTime();
                                }
                                a.status = rs.getString("status");
                                a.patientUserId = rs.getLong("patient_user_id");
                                a.medicalHistory = rs.getString("medical_history");
                                fb.add(a);
                            }
                            System.out.println("[Appointments] email fallback succeeded using appointment_date casts.");
                            list = fb;
                        }
                    }
                } catch (SQLException e) {
                    lastErr = (lastErr == null) ? e : lastErr;
                }
            }

            // Update UI on FX thread
            List<Appt> finalList = list;
            Exception finalErr = lastErr;
            Platform.runLater(() -> {
                apptData.clear();
                if (finalList != null && !finalList.isEmpty()) {
                    for (Appt a : finalList) apptData.add(AppointmentRow.of(a));
                } else if (finalErr != null) {
                    String msg = "Failed to load appointments for the selected date.";
                    if (finalErr.getMessage() != null && !finalErr.getMessage().isBlank()) {
                        msg += " (" + finalErr.getMessage() + ")";
                    }
                    showWarn("Appointments", msg);
                }
                refitAppointmentsColumnsLater();
            });
        }, "doc-appts-by-date").start();
    }

    private void wireAppointmentsTable() {
        if (AppointmentsTable == null) return;
//        AppointmentsTable
        // أعمدة البيانات
        if (colPatientName != null) colPatientName.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        if (colDate != null) colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        if (colTime != null) colTime.setCellValueFactory(new PropertyValueFactory<>("timeStr"));
        if (colStatus != null) colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Doctor name column: always show current doctor full name
        if (colDoctorName != null) colDoctorName.setCellValueFactory(v ->
                new ReadOnlyStringWrapper(currentDoctorFullName != null ? currentDoctorFullName
                        : (Session.get() != null ? Session.get().getFullName() : "")));

        // Serial number column: running index (unsortable)
        if (colSerialNumber != null) {
            colSerialNumber.setResizable(true);
            colSerialNumber.setSortable(false);
            colSerialNumber.setCellFactory(col -> new TableCell<AppointmentRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : String.valueOf(getIndex() + 1));
                }
            });
        }

        // Preferred widths so H-scroll appears when total exceeds viewport
        if (colSerialNumber != null) colSerialNumber.setPrefWidth(60);
        if (colDoctorName != null) colDoctorName.setPrefWidth(140);
        if (colPatientName != null) colPatientName.setPrefWidth(220);
        if (colDate != null) colDate.setPrefWidth(140);
        if (colTime != null) colTime.setPrefWidth(110);
        if (colStatus != null) colStatus.setPrefWidth(140);
        if (colAction != null) {
            colAction.setPrefWidth(220);
            colAction.setMinWidth(180);
        }

        // --- Make the last column (Action) flex to fill remaining space when available ---
        final double actionBaseWidth = (colAction == null) ? 0 : Math.max(180, colAction.getPrefWidth());

        appointmentsRefit = () -> {
            if (AppointmentsTable == null || colAction == null) return;
            double used = 0;
            if (colSerialNumber != null) used += colSerialNumber.getWidth();
            if (colDoctorName != null) used += colDoctorName.getWidth();
            if (colPatientName != null) used += colPatientName.getWidth();
            if (colDate != null) used += colDate.getWidth();
            if (colTime != null) used += colTime.getWidth();
            if (colStatus != null) used += colStatus.getWidth();
            double totalTableWidth = AppointmentsTable.getWidth();
            double padding = 14; // scrollbar/skin padding guard
            double remaining = totalTableWidth - used - padding;
            double newW = Math.max(actionBaseWidth, remaining);
            colAction.prefWidthProperty().unbind();
            colAction.setPrefWidth(newW);
        };
        // run once after layout
        Platform.runLater(appointmentsRefit);

        // Re-fit when table or columns change size
        ChangeListener<Number> _refit = (obs, ov, nv) -> {
            if (appointmentsRefit != null) appointmentsRefit.run();
        };
        AppointmentsTable.widthProperty().addListener(_refit);
        if (colSerialNumber != null) colSerialNumber.widthProperty().addListener(_refit);
        if (colDoctorName != null) colDoctorName.widthProperty().addListener(_refit);
        if (colPatientName != null) colPatientName.widthProperty().addListener(_refit);
        if (colDate != null) colDate.widthProperty().addListener(_refit);
        if (colTime != null) colTime.widthProperty().addListener(_refit);
        if (colStatus != null) colStatus.widthProperty().addListener(_refit);


        // عمود الأكشن: حد أدنى ومعطّل تغيير الحجم والفرز
//        colAction.setMinWidth(260);
        colDoctorName.setResizable(true);


        colAction.setResizable(true);
        colAction.setSortable(false);
        if (colAction != null) {
            colAction.setResizable(true);
            colAction.setMinWidth(180);
        }

        // ✨ خلية الأكشن: زر Cancel و Edit
        colAction.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAction.setCellFactory(tc -> new TableCell<AppointmentRow, AppointmentRow>() {
            private final Button btnDone = new Button();
            private final Button btnCancel = new Button();
            private final Button btnPresc = new Button("Prescription");
            private final HBox box = new HBox(8, btnDone, btnCancel, btnPresc);

            {
                box.setAlignment(Pos.CENTER_LEFT);

                // نصوص بسيطة بدل أيقونات خارجية
                btnDone.setText("✔");
                btnCancel.setText("✖");

                btnDone.getStyleClass().addAll("btn", "btn-complete", "table-action");
                btnCancel.getStyleClass().addAll("btn", "btn-danger", "table-action");
                btnPresc.getStyleClass().addAll("btn", "btn-complete");

                btnDone.setOnAction(e -> {
                    playPressAnim(btnDone);
                    AppointmentRow r = getItem();
                    if (r == null) return;
                    // دالتك الحالية لتكميل الموعد
                    completeAppointmentDb(r);
                    // عطّل ✔ و ✖ بعد الإكمال
                    btnDone.setDisable(true);
                    btnCancel.setDisable(true);
                });

                btnCancel.setOnAction(e -> {
                    playPressAnim(btnCancel);
                    AppointmentRow r = getItem();
                    if (r == null) return;
                    // دالة الإلغاء التي عندك أسفل الملف
                    cancelAppointment(r);
                    // عطّل ✔ و ✖ بعد الإلغاء
                    btnDone.setDisable(true);
                    btnCancel.setDisable(true);
                });

                btnPresc.setOnAction(e -> {
                    playPressAnim(btnPresc);
                    AppointmentRow r = getItem();
                    if (r == null) return;
                    // يفتح واجهة الوصفة ويعرض الأصناف إن وُجدت
                    openPrescriptionForAppointment(r);
//                    openPrescription(r);
                });
            }

            @Override
            protected void updateItem(AppointmentRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                String st = (row.getStatus() == null) ? "" : row.getStatus().trim().toUpperCase();
                boolean terminal = "COMPLETED".equals(st) || "CANCELLED".equals(st);
                // عطّل ✔ و ✖ لما تكون الحالة نهائية؛ Prescription يظل شغّال
                btnDone.setDisable(terminal);
                btnCancel.setDisable(terminal);
                setGraphic(box);
            }
        });

        // ارتفاع صف متغيّر (السكرول سيظهر تلقائياً عندما لا تتسع المساحة)
        AppointmentsTable.setFixedCellSize(-1);

        // أخيراً البيانات
        AppointmentsTable.setItems(apptData);
        // سياسة القياس: سياسة غير مقيدة لتمكين سكرول أفقي عند تجاوز العرض
        AppointmentsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        startDbNotificationsForDoctor();
        startLightweightPollingDoctor();


    }

    // === Appointment actions: Complete, Cancel, Edit ===
    private void completeAppointmentDb(AppointmentRow row) {
        if (row == null) return;
        try {
            boolean ok = doctorDAO.markAppointmentCompleted(row.getId());
            if (ok) {
                row.setStatus("COMPLETED");
                // Reload table for the currently selected dashboard date
                loadTodayAppointmentsAsync();
                if (AppointmentsTable != null) AppointmentsTable.refresh();
                toast("Appointment COMPLETED.", "ok");
                var _dashDate = (datePickerPatientsWithDoctorDash != null && datePickerPatientsWithDoctorDash.getValue() != null)
                        ? datePickerPatientsWithDoctorDash.getValue()
                        : java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
                loadStatsForDateAsync(_dashDate);
            } else {
                showWarn("Complete", "Could not mark as COMPLETED.");
            }
        } catch (Exception ex) {
            showWarn("Complete", "Failed to update DB: " + ex.getMessage());
        }
    }

    //    private void cancelAppointment(AppointmentRow row) {
    //        if (row == null) return;
    //        try {
    //            boolean ok = doctorDAO.cancelAppointment(row.getId());
    //            if (ok) {
    //                // remove from UI list entirely as requested
    //                apptData.remove(row);
    //                // Reload table for the currently selected dashboard date
    //                loadTodayAppointmentsAsync();
    //                if (AppointmentsTable != null) AppointmentsTable.refresh();
    //                toast("Appointment cancelled.", "ok");
    //                var _dashDate = (datePickerPatientsWithDoctorDash != null && datePickerPatientsWithDoctorDash.getValue() != null)
    //                        ? datePickerPatientsWithDoctorDash.getValue()
    //                        : java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
    //                loadStatsForDateAsync(_dashDate);
    //            } else {
    //                showWarn("Cancel", "Could not cancel appointment.");
    //            }
    //        } catch (Exception ex) {
    //            showWarn("Cancel", "Failed to update DB: " + ex.getMessage());
    //        }
    //    }

    /**
     * Cancel appointment in DB then update UI; leaves Prescription enabled.
     */
    private void cancelAppointment(AppointmentRow row) {
        if (row == null) return;
        new Thread(() -> {
            boolean ok = false;
            try (Connection c = Database.get();
                 java.sql.PreparedStatement ps = c.prepareStatement(
                         "UPDATE appointments SET status = 'CANCELLED', updated_at = NOW() WHERE id = ?")) {
                ps.setLong(1, row.getId());
                ok = (ps.executeUpdate() == 1);
            } catch (Exception ignore) {
                ok = false;
            }
            final boolean success = ok;
            Platform.runLater(() -> {
                if (success) {
                    row.setStatus("CANCELLED");
                    if (AppointmentsTable != null) AppointmentsTable.refresh();
                    var _dashDate = (datePickerPatientsWithDoctorDash != null && datePickerPatientsWithDoctorDash.getValue() != null)
                            ? datePickerPatientsWithDoctorDash.getValue()
                            : java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
                    loadStatsForDateAsync(_dashDate);
                } else {
                    showWarn("Update", "Could not cancel the appointment. Please try again later.");
                }
            });
        }, "doc-cancel").start();
    }

    private void editAppointment(AppointmentRow row) {
        if (row == null) return;
        showInfo("Edit Appointment", "Open edit dialog for appointment #" + row.getId()
                + "\nPatient: " + safe(row.getPatientName()));
    }

    /**
     * Open prescription page for the selected appointment: ensures prescription exists and fills header.
     */
    private void openPrescriptionForAppointment(AppointmentRow row) {
        if (row == null) return;
        try (Connection c = Database.get()) {
            // Resolve doctor id (doctors.id) from current user
            var u = com.example.healthflow.service.AuthService.Session.get();
            if (u == null) {
                showWarn("Prescription", "No logged-in user.");
                return;
            }
            Long doctorId = doctorDAO.findDoctorIdByUserId(c, u.getId());
            if (doctorId == null) {
                doctorDAO.ensureProfileForUser(c, u.getId());
                doctorId = doctorDAO.findDoctorIdByUserId(c, u.getId());
            }
            if (doctorId == null) {
                showWarn("Prescription", "Doctor profile missing.");
                return;
            }

            // Translate patient user id -> patients.id; fallback by national id if user id is missing
            Long patientUserId = (row.getPatientUserId() > 0) ? row.getPatientUserId() : null;
            if (patientUserId == null && row.getNationalId() != null && !row.getNationalId().isBlank()) {
                patientUserId = doctorDAO.findPatientUserIdByNationalId(c, row.getNationalId());
            }
            if (patientUserId == null) {
                showWarn("Prescription", "Patient user id not found for this appointment.");
                return;
            }
            Long patientId = doctorDAO.findPatientIdByUserId(c, patientUserId);
            if (patientId == null) {
                showWarn("Prescription", "Patient profile not found.");
                return;
            }

            // Ensure prescription for this appointment, then cache context and navigate to the pane
            long prescId = new PrescriptionDAO().ensurePrescriptionForAppointment(c, row.getId(), doctorId, patientId);
            this.currentPrescriptionId = prescId;
            this.selectedPatientUserId = patientUserId;
            this.selectedPatientName = row.getPatientName();
            this.selectedPatientNationalId = row.getNationalId();

            // Ensure pack suggestions exist before loading items so colPack won't show "—"
            try (Connection c2 = Database.get()) {
                c2.setAutoCommit(true);
                new com.example.healthflow.dao.PrescriptionItemDAO().backfillSuggestions(c2, this.currentPrescriptionId);
            } catch (Exception ignore) {
            }

            showPrescriptionPane();
            loadPrescriptionItemsFromDb(currentPrescriptionId);
            Platform.runLater(() -> setPatientHeader(row.getPatientName(), row.getNationalId(), false));
//            toast("Prescription #" + prescId + " ready.", "ok");
        } catch (Exception ex) {
            showError("Open Prescription", ex);
        }
    }

    /**
     * Load items for a given prescription and push into the items table.
     */
    private void loadPrescriptionItemsFromDb(long prescId) {
        if (prescId <= 0) return;
        // امسح الداتا الحالية في الجدول/اللستة
        try {
            if (TablePrescriptionItems != null) {
                TablePrescriptionItems.getItems().clear();
                TablePrescriptionItems.getSelectionModel().clearSelection();
            }
            if (prescItemsEditable != null) prescItemsEditable.clear();
        } catch (Throwable ignore) {
        }

        new Thread(() -> {
            java.util.List<PrescItemRow> rows = new java.util.ArrayList<>();
            Exception err = null;
            try (Connection c = Database.get();
                 java.sql.PreparedStatement ps = c.prepareStatement(
                         "SELECT id, medicine_id, medicine_name, " +
                         "       COALESCE(dosage_text, dosage) AS dosage_display, " +
                         "       quantity, status, batch_id, dose, freq_per_day, duration_days, strength, form, route, notes, " +
                         "       qty_units_requested, suggested_unit::text AS suggested_unit, suggested_count, suggested_units_total, " +
                         "       approved_unit::text  AS approved_unit,  approved_count,   approved_units_total " +
                         "FROM prescription_items WHERE prescription_id = ? ORDER BY id")) {
                ps.setLong(1, prescId);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PrescItemRow r = new PrescItemRow();
                        r.setId(rs.getLong("id"));
                        long mid = rs.getLong("medicine_id");
                        if (!rs.wasNull()) {
                            r.setMedicineId(mid);
                        }
                        r.setMedicineName(rs.getString("medicine_name"));
                        r.setDosageText(rs.getString("dosage_display"));
                        r.setQuantity(rs.getInt("quantity"));
                        String st = rs.getString("status");
                        if (st != null) r.setStatus(st);
                        long b = rs.getLong("batch_id");
                        if (!rs.wasNull()) {
                            r.setBatchId(b);
                        }
                        Integer doseObj = (Integer) rs.getObject("dose");
                        if (doseObj != null) r.setDose(doseObj);
                        Integer freqObj = (Integer) rs.getObject("freq_per_day");
                        if (freqObj != null) r.setFreqPerDay(freqObj);
                        Integer durObj = (Integer) rs.getObject("duration_days");
                        if (durObj != null) r.setDurationDays(durObj);
                        String strength = rs.getString("strength");
                        if (strength != null) r.setStrength(strength);
                        String form = rs.getString("form");
                        if (form != null) r.setForm(form);
                        String route = rs.getString("route");
                        if (route != null) r.setRoute(route);
                        String notes = rs.getString("notes");
                        if (notes != null) r.setNotes(notes);
                        // Pack suggestion / approval fields
                        Integer qtyReq = (Integer) rs.getObject("qty_units_requested");
                        if (qtyReq != null) r.setQtyUnitsRequested(qtyReq);

                        String suggUnit = rs.getString("suggested_unit");
                        if (suggUnit != null && !suggUnit.isBlank()) {
                            try {
                                r.setSuggestedUnit(com.example.healthflow.core.packaging.PackUnit.valueOf(suggUnit));
                            } catch (IllegalArgumentException ex) {
                                r.setSuggestedUnit(null);
                            }
                        } else {
                            r.setSuggestedUnit(null);
                        }

                        Integer suggCount = (Integer) rs.getObject("suggested_count");
                        if (suggCount != null) r.setSuggestedCount(suggCount);
                        Integer suggTotal = (Integer) rs.getObject("suggested_units_total");
                        if (suggTotal != null) r.setSuggestedUnitsTotal(suggTotal);


                        String apprUnit = rs.getString("approved_unit");
                        if (apprUnit != null && !apprUnit.isBlank()) {
                            try {
                                r.setApprovedUnit(com.example.healthflow.core.packaging.PackUnit.valueOf(apprUnit));
                            } catch (IllegalArgumentException ex) {
                                r.setApprovedUnit(null);
                            }
                        } else {
                            r.setApprovedUnit(null);
                        }

                        Integer apprCount = (Integer) rs.getObject("approved_count");
                        if (apprCount != null) r.setApprovedCount(apprCount);
                        Integer apprTotal = (Integer) rs.getObject("approved_units_total");
                        if (apprTotal != null) r.setApprovedUnitsTotal(apprTotal);
                        rows.add(r);
                    }
                }
            } catch (Exception ex) {
                err = ex;
            }

            Exception fErr = err;
            Platform.runLater(() -> {
                if (fErr != null) {
                    showWarn("Prescription Items", "Failed to load items: " + fErr.getMessage());
                    return;
                }
                // ادفع البيانات للـ ObservableList المربوطة بالجدول
                if (prescItemsEditable != null) {
                    prescItemsEditable.addAll(rows);
                } else if (TablePrescriptionItems != null) {
                    // fallback لو الجدول غير مربوط بلستة خارجية
//                    TablePrescriptionItems.getItems().addAll(rows);
                    Platform.runLater(this::reloadPrescriptionItemsFromDb);
                }
                if (TablePrescriptionItems != null) TablePrescriptionItems.refresh();
            });
        }, "doc-load-presc-items").start();
        TablePrescriptionItems.setItems(prescItemsEditable);
    }

    private void setPatientHeader(String name, String nid, boolean editable) {
        if (PatientNameTF != null) {
            String txt = (name == null ? "" : name.trim());
            if (nid != null && !nid.isBlank()) txt += " \u2022 " + nid;
            PatientNameTF.setText(txt);
            PatientNameTF.setEditable(editable);
            PatientNameTF.setPromptText(editable ? "Patient name \u2022 National ID (optional)" : null);
        }
        if (DoctorNameLabel != null) {
            DoctorNameLabel.setText(
                    currentDoctorFullName != null ? currentDoctorFullName :
                            (Session.get() != null ? Session.get().getFullName() : "")
            );
        }
        // خزّن السياق الحالي
        selectedPatientName = name;
        selectedPatientNationalId = nid;
    }


    private void wirePatientsTable() {
        if (patientTable == null) return;

        if (colNationalId != null) colNationalId.setCellValueFactory(new PropertyValueFactory<>("nationalId"));
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        if (colGender != null) colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        if (colDob != null) colDob.setCellValueFactory(new PropertyValueFactory<>("age"));
        if (colMedicalHistory != null)
            colMedicalHistory.setCellValueFactory(new PropertyValueFactory<>("medicalHistory"));
        if (colAction2 != null) {
            colAction2.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
            colAction2.setCellFactory(col -> new TableCell<PatientRow, PatientRow>() {
                private final Button btnDone = new Button();
                private final Button btnPresc = new Button("Prescription");
                private final HBox box = new HBox(8, btnDone, btnPresc);

                {
                    // Icon + tooltip for "Complete"
                    btnDone.setGraphic(new org.kordamp.ikonli.javafx.FontIcon("fas-check-circle"));
                    btnDone.setTooltip(new Tooltip("Complete Appointment"));
                    btnDone.getStyleClass().addAll("btn", "btn-complete");
                    btnPresc.getStyleClass().addAll("btn", "btn-complete");
                    box.getStyleClass().add("table-actions");

                    // Compact sizes
                    btnDone.setMinWidth(36);
                    btnDone.setMaxWidth(Region.USE_PREF_SIZE);
                    btnPresc.setMinWidth(78);
                    btnPresc.setMaxWidth(Region.USE_PREF_SIZE);

                    // Disable when offline
                    btnDone.disableProperty().bind(monitor.onlineProperty().not());
                    btnPresc.disableProperty().bind(monitor.onlineProperty().not());

                    // Complete the selected appointment if it belongs to this patient;
                    // otherwise, complete the first active appointment for this patient.
                    btnDone.setOnAction(e -> {
                        PatientRow prow = getItem();
                        if (prow == null) return;

                        AppointmentRow target = null;

                        // 1) Prefer the currently selected appointment in the AppointmentsTable
                        if (AppointmentsTable != null) {
                            AppointmentRow sel = AppointmentsTable.getSelectionModel().getSelectedItem();
                            if (sel != null) {
                                boolean matchByUser = (prow.getUserId() > 0) && (sel.getPatientUserId() == prow.getUserId());
                                boolean matchByNid = (prow.getNationalId() != null) && prow.getNationalId().equals(sel.getNationalId());
                                boolean notCompleted = sel.getStatus() == null || !sel.getStatus().equalsIgnoreCase("COMPLETED");
                                if ((matchByUser || matchByNid) && notCompleted) {
                                    target = sel;
                                }
                            }
                        }

                        // 2) If no suitable selection, fallback to first active appt for this patient
                        if (target == null) {
                            for (AppointmentRow ar : apptData) {
                                boolean matchByUser = (prow.getUserId() > 0) && (ar.getPatientUserId() == prow.getUserId());
                                boolean matchByNid = (prow.getNationalId() != null) && prow.getNationalId().equals(ar.getNationalId());
                                boolean notCompleted = ar.getStatus() == null || !ar.getStatus().equalsIgnoreCase("COMPLETED");
                                if ((matchByUser || matchByNid) && notCompleted) {
                                    target = ar;
                                    break;
                                }
                            }
                        }

                        if (target != null) {
                            completeAppointmentDb(target);
                        } else {
                            // 3) DB fallback: try to locate today's active appointment for this patient & current doctor
                            var u = Session.get();
                            if (u == null) {
                                toast("No logged-in user.", "warn");
                                return;
                            }
                            Long doctorId = null;
                            try (Connection c = Database.get()) {
                                // resolve doctor_id from current user once
                                doctorId = doctorDAO.findDoctorIdByUserId(c, u.getId());
                                if (doctorId == null) {
                                    doctorDAO.ensureProfileForUser(c, u.getId());
                                    doctorId = doctorDAO.findDoctorIdByUserId(c, u.getId());
                                }
                                if (doctorId == null) {
                                    toast("Doctor profile missing.", "warn");
                                    return;
                                }

                                Long apptId = null;
                                // Prefer matching by patient user id if available
                                if (prow.getUserId() > 0) {
                                    try (var ps = c.prepareStatement(
                                            "SELECT a.id FROM appointments a " +
                                                    "JOIN patients p ON p.id = a.patient_id " +
                                                    "WHERE a.doctor_id = ? AND p.user_id = ? " +
                                                    "AND a.appointment_date::date = CURRENT_DATE " +
                                                    "AND a.status NOT IN ('COMPLETED','CANCELLED') " +
                                                    "ORDER BY a.appointment_date LIMIT 1")) {
                                        ps.setLong(1, doctorId);
                                        ps.setLong(2, prow.getUserId());
                                        try (var rs = ps.executeQuery()) {
                                            if (rs.next()) apptId = rs.getLong(1);
                                        }
                                    }
                                }
                                // If still not found and we have a National ID, try via NID
                                if (apptId == null && prow.getNationalId() != null && !prow.getNationalId().isBlank()) {
                                    try (var ps = c.prepareStatement(
                                            "SELECT a.id FROM appointments a " +
                                                    "JOIN patients p ON p.id = a.patient_id " +
                                                    "JOIN users pu ON pu.id = p.user_id " +
                                                    "WHERE a.doctor_id = ? AND pu.national_id = ? " +
                                                    "AND a.appointment_date::date = CURRENT_DATE " +
                                                    "AND a.status NOT IN ('COMPLETED','CANCELLED') " +
                                                    "ORDER BY a.appointment_date LIMIT 1")) {
                                        ps.setLong(1, doctorId);
                                        ps.setString(2, prow.getNationalId());
                                        try (var rs = ps.executeQuery()) {
                                            if (rs.next()) apptId = rs.getLong(1);
                                        }
                                    }
                                }

                                if (apptId != null) {
                                    // Update status in DB and refresh UI
                                    boolean ok = doctorDAO.markAppointmentCompleted(apptId);
                                    if (ok) {
                                        toast("Appointment marked COMPLETED.", "ok");
                                        loadTodayAppointmentsAsync();
                                        var _dashDate = (datePickerPatientsWithDoctorDash != null && datePickerPatientsWithDoctorDash.getValue() != null)
                                                ? datePickerPatientsWithDoctorDash.getValue()
                                                : java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
                                        loadStatsForDateAsync(_dashDate);
                                    } else {
                                        toast("Could not mark appointment as COMPLETED.", "warn");
                                    }
                                } else {
                                    toast("No active appointment found for this patient today.", "warn");
                                }
                            } catch (Exception ex) {
                                showWarn("Complete", "Failed to update DB: " + ex.getMessage());
                            }
                        }
                    });

                    btnPresc.setOnAction(e -> {
                        PatientRow row = getItem();
                        if (row == null) return;
                        // persist selection
                        Long pid = null;
                        if (row.getUserId() > 0) {
                            pid = Long.valueOf(row.getUserId());
                        } else {
                            pid = fallbackResolvePatientIdFromAppointments(row);
                        }
                        selectedPatientUserId = pid; // may be null -> handled later in ensureDraftPrescription()
                        selectedPatientName = row.getFullName();
                        selectedPatientNationalId = row.getNationalId();

//                        System.out.println("[PRESCRIPTION BUTTON] Stored -> userId=" + selectedPatientUserId
//                                + ", name=" + selectedPatientName
//                                + ", NID=" + selectedPatientNationalId);

                        // If userId is still null, try DB lookup by national id immediately
                        if (selectedPatientUserId == null && selectedPatientNationalId != null && !selectedPatientNationalId.isBlank()) {
                            try (Connection c = Database.get()) {
                                Long uid = doctorDAO.findPatientUserIdByNationalId(c, selectedPatientNationalId);
                                if (uid != null && uid > 0) {
                                    selectedPatientUserId = uid;
//                                    System.out.println("[PRESCRIPTION BUTTON] DB lookup by NID succeeded -> userId=" + selectedPatientUserId);
                                } else {
//                                    System.out.println("[PRESCRIPTION BUTTON] DB lookup by NID failed (no match).");
                                }
                            } catch (Exception ex) {
//                                System.out.println("[PRESCRIPTION BUTTON] DB lookup by NID error: " + ex.getMessage());
                            }
                        }

                        // reset any previous draft so a new one is created for this patient
                        currentPrescriptionId = null;
                        // افتح واجهة الوصفة عبر الدالة التي تعمل backfill قبل التحميل
                        openPrescriptionFromItems();
                        // ثم حدّث الهيدر بعد العرض
                        Platform.runLater(() -> {
                            setPatientHeader(row.getFullName(), row.getNationalId(), false);
                            if (PatientNameTF != null) {
                                PatientNameTF.positionCaret(PatientNameTF.getText().length());
                            }
                        });
                        if (selectedPatientUserId == null) {
                            toast("Patient selected (no user id yet). If add fails, choose from Appointments or type National ID in header.", "warn");
                        }
                    });
                }

                @Override
                protected void updateItem(PatientRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(null);
                    setGraphic(empty ? null : box);
                }
            });
        }
        // توزيع أبعاد الأعمدة كنِسَب من عرض الجدول ليتكيّف مع الشاشة
//        if (patientTable != null) {
//            var w2 = patientTable.widthProperty().subtract(15);
//            if (colNationalId != null)     colNationalId.prefWidthProperty().bind(w2.multiply(0.1));
//            if (colName    != null)        colName.prefWidthProperty().bind(w2.multiply(0.16));
//            if (colGender  != null)        colGender.prefWidthProperty().bind(w2.multiply(0.6));
//            if (colDob     != null)        colDob.prefWidthProperty().bind(w2.multiply(0.10));
//            if (colMedicalHistory != null) colMedicalHistory.prefWidthProperty().bind(w2.multiply(0.22));
//            if (colAction2 != null) {
//                colAction2.prefWidthProperty().bind(w2.multiply(0.08));
//                colAction2.setResizable(true);
//                colAction2.setSortable(true);
//                colAction2.setMinWidth(90);
//            }
//        }
        // سياسة توزيع الأعمدة بحيث تملأ العرض وتُفَعِّل سكرول تلقائي
        patientTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // اربط حجم الجدول بحجم حاويته (AnchorPane) ليتوفر "منفذ عرض" صغير بما يكفي لظهور السكروول عند كثرة الصفوف
        if (PatientAnchorPane != null) {
            patientTable.prefWidthProperty().bind(PatientAnchorPane.widthProperty());
            // اطرح هامشًا بسيطًا من الارتفاع لو عندك هيدر/تولبار أعلى الجدول
            patientTable.prefHeightProperty().bind(PatientAnchorPane.heightProperty().subtract(40));
        }

        // تحديد ارتفاع صف ثابت يُحسِّن الأداء ويضمن ظهور السكروول عند تجاوز عدد الصفوف للارتفاع المتاح
        patientTable.setFixedCellSize(36);   // ارتفاع الصف ~36px
        patientTable.setMinHeight(120);      // حد أدنى حتى لا يتمدّد بلا داعٍ

        patientTable.setItems(patientData);
    }

    private void wireSearch() {
        filtered = new FilteredList<>(patientData, p -> true);
        if (searchLabel != null) {
            searchLabel.textProperty().addListener((obs, old, q) -> {
                String s = (q == null) ? "" : q.trim().toLowerCase();
                filtered.setPredicate(p -> {
                    if (s.isEmpty()) return true;
                    if (contains(p.getFullName(), s)) return true;
                    if (contains(p.getGender(), s)) return true;
                    if (contains(p.getNationalId(), s)) return true;
                    if (contains(p.getMedicalHistory(), s)) return true;
                    return String.valueOf(p.getAge()).contains(s);
                });
            });
        }
        sorted = new SortedList<>(filtered);
        if (patientTable != null) sorted.comparatorProperty().bind(patientTable.comparatorProperty());
        if (patientTable != null) patientTable.setItems(sorted);
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    /* ================= Prescription Items table wiring ================= */
private void wirePrescriptionItemsTable() {
    if (TablePrescriptionItems == null) return;

    TablePrescriptionItems.setItems(prescItemsEditable);
    TablePrescriptionItems.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

    final double actionBase = (colPresesAction == null) ? 0 : Math.max(180, colPresesAction.getPrefWidth());
    final double actionMax  = 240; // cap expansion so we don't kill H-scroll

    java.util.function.ToDoubleFunction<TableColumn<?, ?>> cw = (tc) -> {
        if (tc == null) return 0.0;
        double w = tc.getWidth();
        return (w > 0.0 ? w : Math.max(tc.getPrefWidth(), tc.getMinWidth()));
    };

    Runnable fitLastColumn = () -> {
        if (TablePrescriptionItems == null || colPresesAction == null) return;
        double used = 0;
        used += cw.applyAsDouble(colIdx);
        used += cw.applyAsDouble(colMedicineName);
        used += cw.applyAsDouble(colStrength);
        used += cw.applyAsDouble(colForm);
        used += cw.applyAsDouble(colDose);
        used += cw.applyAsDouble(colDosage);
        used += cw.applyAsDouble(colFreqPerDay);
        used += cw.applyAsDouble(colDuration);
        used += cw.applyAsDouble(colRoute);
        used += cw.applyAsDouble(colQuantity);
        used += cw.applyAsDouble(colPresesStatus);

        double total = TablePrescriptionItems.getWidth();
        double padding = 14;
        double remaining = total - used - padding;
        double newW = Math.max(actionBase, Math.min(remaining, actionMax));
        colPresesAction.prefWidthProperty().unbind();
        colPresesAction.setPrefWidth(newW);
    };

    if (colIdx != null) {
        colIdx.setSortable(false);
        colIdx.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(
                TablePrescriptionItems.getItems().indexOf(cd.getValue()) + 1));
    }
    if (colMedicineName != null) colMedicineName.setCellValueFactory(cd -> cd.getValue().medicineNameProperty());
    if (colDosage != null)       colDosage.setCellValueFactory(new PropertyValueFactory<>("dosageText"));
    if (colDuration != null)     colDuration.setCellValueFactory(cd -> cd.getValue().durationDaysProperty().asObject());

    if (colQuantity != null) {
        colQuantity.setCellValueFactory(cd -> {
            var it = cd.getValue();
            Integer u = it.getQtyUnitsRequested();
            int d = Math.max(0, it.getDose());
            int f = Math.max(0, it.getFreqPerDay());
            int g = Math.max(0, it.getDurationDays());
            if ((u == null || u <= 0) && d > 0 && f > 0 && g > 0) u = d * f * g;

            String form = it.getForm();
            String unitShort = (form != null && form.equalsIgnoreCase("Tablet")) ? "tabs"
                                : (form != null && form.equalsIgnoreCase("Capsule")) ? "caps"
                                : "units";
            String text = (u != null && u > 0) ? (u + " " + unitShort) : "—";
            return new ReadOnlyStringWrapper(text);
        });

        colQuantity.setCellFactory(col -> new TableCell<PrescItemRow, String>() {
            @Override protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (empty || text == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(text);
                    PrescItemRow row = (getTableRow() == null) ? null : (PrescItemRow) getTableRow().getItem();
                    if (row != null) {
                        int d = row.getDose(), f = row.getFreqPerDay(), days = row.getDurationDays();
                        String tt = "Qty = Dose × Freq/day × Duration\n= "
                                + (d == 0 ? "?" : d) + " × "
                                + (f == 0 ? "?" : f) + " × "
                                + (days == 0 ? "?" : days);
                        if (row.getSuggestedUnit() != null && row.getSuggestedCount() != null && row.getSuggestedUnitsTotal() != null) {
                            tt += "\nSuggested: " + row.getSuggestedCount() + " " + row.getSuggestedUnit() + " (" + row.getSuggestedUnitsTotal() + ")";
                        }
                        setTooltip(new Tooltip(tt));
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });
    }

    // === Pack column (single binding) ===
    if (colPack != null) {
        colPack.setText("Pack");
        colPack.setPrefWidth(160);
        colPack.setCellValueFactory(cd -> cd.getValue().packProperty());        colPack.setCellFactory(col -> new TableCell<PrescItemRow, String>() {
            @Override protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (empty) { setText(null); setTooltip(null); return; }
                setText((text == null || text.isBlank()) ? "—" : text);
                setTooltip((text == null || text.isBlank()) ? null : new Tooltip(text));
            }
        });
    }

    if (colPresesStatus != null) colPresesStatus.setCellValueFactory(cd -> cd.getValue().statusProperty());
    if (colFreqPerDay != null)   { colFreqPerDay.setCellValueFactory(new PropertyValueFactory<>("freqPerDay")); colFreqPerDay.setPrefWidth(120); }
    if (colStrength  != null)    { colStrength.setCellValueFactory(new PropertyValueFactory<>("strength"));   colStrength.setPrefWidth(80); }
    if (colForm      != null)    { colForm.setCellValueFactory(new PropertyValueFactory<>("form"));           colForm.setPrefWidth(100); }
    if (colDose      != null)    { colDose.setCellValueFactory(new PropertyValueFactory<>("dose"));           colDose.setPrefWidth(90); }
    if (colRoute     != null)    { colRoute.setCellValueFactory(new PropertyValueFactory<>("route"));         colRoute.setPrefWidth(100); }

    if (colMedicineName != null) colMedicineName.setPrefWidth(100);
    if (colDosage != null)       colDosage.setPrefWidth(310);
    if (colStrength != null)     colStrength.setPrefWidth(80);
    if (colForm != null)         colForm.setPrefWidth(80);
    if (colDose != null)         colDose.setPrefWidth(60);
    if (colFreqPerDay != null)   colFreqPerDay.setPrefWidth(70);
    if (colDuration != null)     colDuration.setPrefWidth(70);
    if (colRoute != null)        colRoute.setPrefWidth(100);
    if (colQuantity != null)     colQuantity.setPrefWidth(80);

    ChangeListener<Number> _refit = (obs, ov, nv) -> fitLastColumn.run();
    TablePrescriptionItems.widthProperty().addListener(_refit);
    if (colIdx != null)           colIdx.widthProperty().addListener(_refit);
    if (colMedicineName != null)  colMedicineName.widthProperty().addListener(_refit);
    if (colStrength != null)      colStrength.widthProperty().addListener(_refit);
    if (colForm != null)          colForm.widthProperty().addListener(_refit);
    if (colDosage != null)        colDosage.widthProperty().addListener(_refit);
    if (colFreqPerDay != null)    colFreqPerDay.widthProperty().addListener(_refit);
    if (colDuration != null)      colDuration.widthProperty().addListener(_refit);
    if (colRoute != null)         colRoute.widthProperty().addListener(_refit);
    if (colQuantity != null)      colQuantity.widthProperty().addListener(_refit);
    if (colPresesStatus != null)  colPresesStatus.widthProperty().addListener(_refit);
    if (colPresesAction != null)  colPresesAction.widthProperty().addListener(_refit);

    Platform.runLater(fitLastColumn);

    if (colPresesAction != null) {
        colPresesAction.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colPresesAction.setSortable(false);
        colPresesAction.setResizable(false);
        colPresesAction.setCellFactory(tc -> new TableCell<PrescItemRow, PrescItemRow>() {
            private final Button btnDel = new Button("Delete");
            private final HBox box = new HBox(8, btnDel);
            {
                btnDel.getStyleClass().addAll("btn", "btn-danger");
                box.setAlignment(Pos.CENTER_LEFT);
                btnDel.setOnAction(e -> {
                    PrescItemRow row = getItem();
                    if (row == null) return;
                    if (row.getId() > 0) {
                        btnDel.setDisable(true);
                        new Thread(() -> {
                            try (Connection c = Database.get()) {
                                c.setAutoCommit(true);
                                new PrescriptionItemDAO().deleteById(c, row.getId());
                                Platform.runLater(() -> { prescItemsEditable.remove(row); toast("Delete medication.", "ok"); btnDel.setDisable(false); });
                            } catch (Exception ex) {
                                Platform.runLater(() -> { toast("DB delete failed. Row kept.", "warn"); btnDel.setDisable(false); });
                            }
                        }, "delete-presc-item").start();
                    } else {
                        if (prescItemsEditable.remove(row)) toast("Row removed (not saved in DB).", "ok");
                        else toast("Could not remove the row.", "warn");
                    }
                });
            }
            @Override protected void updateItem(PrescItemRow row, boolean empty) {
                super.updateItem(row, empty);
                setText(null);
                setGraphic(empty ? null : box);
            }
        });
    }
}

    /**
     * Resolve patient id for the current prescription context.
     * Priority:
     * 1) Cached selectedPatientUserId
     * 2) Selected row from Appointments table
     * 3) DB lookup by selectedPatientNationalId (set from Patients table action)
     * 4) Extract national id from PatientNameLabel and lookup in DB
     * 5) Scan today's appointments to match name/NID, then return user id or lookup by that NID
     */
    private Long resolveCurrentPatientId() {
        // 1) If we already cached it, return it
        if (selectedPatientUserId != null) return selectedPatientUserId;

        // 2) From currently selected appointment (fast path when doctor works from Appointments tab)
        if (AppointmentsTable != null) {
            AppointmentRow sel = AppointmentsTable.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getPatientUserId() > 0) return sel.getPatientUserId();
        }

        // Helper to query DB by national id and cache if found
        java.util.function.Function<String, Long> findByNid = (nid) -> {
            if (nid == null || nid.isBlank()) return null;
            try (Connection c = Database.get()) {
                Long uid = doctorDAO.findPatientUserIdByNationalId(c, nid);
                if (uid != null && uid > 0) {
                    selectedPatientUserId = uid; // cache
                    return uid;
                }
            } catch (Exception ignored) {
            }
            return null;
        };

        // 3) DB fallback using national id captured when clicking Prescription in Patients tab
        Long viaSelectedNid = findByNid.apply(selectedPatientNationalId);
        if (viaSelectedNid != null) return viaSelectedNid;

        // 4) Try to parse national id from the header label "PatientNameTF"
        if (PatientNameTF != null && PatientNameTF.getText() != null) {
            String txt = PatientNameTF.getText();
            // Expect format: "<name> • <nid>" → خُذ كل الأرقام المتتالية كهوية
            String digits = txt.replaceAll("[^0-9]", "");
            Long viaHeader = findByNid.apply(digits);
            if (viaHeader != null) return viaHeader;
        }

        // 5) Last resort: scan today's appointments and try to match name/NID,
        //    then use the user id directly or lookup by the matched NID.
        if (!apptData.isEmpty()) {
            String headerName = (PatientNameTF != null) ? PatientNameTF.getText() : null;
            String headerNid = null;
            if (headerName != null) {
                String digits = headerName.replaceAll("[^0-9]", "");
                headerNid = (digits == null || digits.isBlank()) ? null : digits;
                // قص الاسم قبل "•" إن وجد
                if (headerName.contains("•")) headerName = headerName.split("•")[0].trim();
            }

            for (AppointmentRow ar : apptData) {
                // exact nid match
                if (headerNid != null && headerNid.equals(ar.getNationalId())) {
                    if (ar.getPatientUserId() > 0) return ar.getPatientUserId();
                    Long viaApptNid = findByNid.apply(ar.getNationalId());
                    if (viaApptNid != null) return viaApptNid;
                }
                // exact name match (case-insensitive)
                if (headerName != null && headerName.equalsIgnoreCase(ar.getPatientName())) {
                    if (ar.getPatientUserId() > 0) return ar.getPatientUserId();
                    Long viaApptNid2 = findByNid.apply(ar.getNationalId());
                    if (viaApptNid2 != null) return viaApptNid2;
                }
            }
        }

        // Not resolved
        return null;
    }

    /**
     * Try to infer patient user id from current appointments by matching national id or name.
     */
    private Long fallbackResolvePatientIdFromAppointments(PatientRow row) {
        if (row == null) return null;
        // try by national id first
        for (AppointmentRow ar : apptData) {
            if (ar.getPatientUserId() > 0) {
                if (row.getNationalId() != null && row.getNationalId().equals(ar.getNationalId())) {
                    return ar.getPatientUserId();
                }
                if (row.getFullName() != null && row.getFullName().equalsIgnoreCase(ar.getPatientName())) {
                    return ar.getPatientUserId();
                }
            }
        }
        return null;
    }

    private PrescItemRow toRow(com.example.healthflow.model.PrescriptionItem it) {
        PrescItemRow r = new PrescItemRow();

        // --- الحقول الأساسية ---
        r.setMedicineName(it.getMedicineName());
        r.setStrength(it.getStrength());
        r.setForm(it.getForm());
        r.setDose(it.getDose() == null ? 0 : it.getDose());
        r.setFreqPerDay(it.getFreqPerDay() == null ? 0 : it.getFreqPerDay());
        r.setDurationDays(it.getDurationDays() == null ? 0 : it.getDurationDays());
        r.setRoute(it.getRoute());
//        r.setQuantity(Math.max(0, it.getQuantity())); // كمية من الداتابيز (int primitive، ما فيه null)

        Integer qObj = null;
        try {
            qObj = it.getQuantity();
        } catch (Throwable ignore) {
        }
        r.setQuantity(qObj == null ? 0 : Math.max(0, qObj));

        // --- حقول التغليف الجديدة ---
        try {
            // عدد الوحدات المطلوبة (الكمية بالوحدات الصغرى)
            r.setQtyUnitsRequested(it.getQtyUnitsRequested());

            // suggested_unit من DAO جاي String → نحوله إلى Enum
            String su = it.getSuggestedUnit();
            if (su != null && !su.isBlank()) {
                try {
                    r.setSuggestedUnit(com.example.healthflow.core.packaging.PackUnit.valueOf(su));
                } catch (IllegalArgumentException ex) {
                    r.setSuggestedUnit(null); // تجاهل النص الغير معروف
                }
            }

            // باقي حقول الاقتراح
            r.setSuggestedCount(it.getSuggestedCount());
            r.setSuggestedUnitsTotal(it.getSuggestedUnitsTotal());
        } catch (Throwable ignore) {
            // لو نسخة قديمة من الموديل لا تحتوي accessor → تجاهل بدون كسر الواجهة
        }

        // ---- Guard against unit/form mismatches (prevents "BOTTLE" with tablets, etc.) ----
        try {
            String f = (r.getForm() == null) ? "" : r.getForm().trim().toLowerCase();
            boolean isSolid = f.equals("tablet") || f.equals("tab") || f.equals("tablets")
                    || f.equals("capsule") || f.equals("capsules");
            boolean isLiquid = f.equals("syrup") || f.equals("suspension") || f.equals("injection") || f.equals("drops");
            boolean isSemi = f.equals("cream") || f.equals("ointment");

            // suggested_*
            if (r.getSuggestedUnit() != null) {
                String u = r.getSuggestedUnit().name();
                boolean ok = (isSolid && (u.equals("BLISTER") || u.equals("BOX") || u.equals("UNIT")))
                        || (isLiquid && u.equals("BOTTLE"))
                        || (isSemi && u.equals("TUBE"));
                if (!ok) {
                    r.setSuggestedUnit(null);
                    r.setSuggestedCount(null);
                    r.setSuggestedUnitsTotal(null);
                }
            }

            // approved_* (same guard)
            if (r.getApprovedUnit() != null) {
                String u2 = r.getApprovedUnit().name();
                boolean ok2 = (isSolid && (u2.equals("BLISTER") || u2.equals("BOX") || u2.equals("UNIT")))
                        || (isLiquid && u2.equals("BOTTLE"))
                        || (isSemi && u2.equals("TUBE"));
                if (!ok2) {
                    r.setApprovedUnit(null);
                    r.setApprovedCount(null);
                    r.setApprovedUnitsTotal(null);
                }
            }
        } catch (Throwable ignore) { /* keep UI resilient */ }

        return r;
    }

    private void reloadPrescriptionItemsFromDb() {
        if (currentPrescriptionId == null || currentPrescriptionId <= 0) return;
        try (java.sql.Connection conn = Database.get()) {
            // Backfill suggestions so Pack values are available even when opening from Prescription button
            try {
                conn.setAutoCommit(true);
                new com.example.healthflow.dao.PrescriptionItemDAO().backfillSuggestions(conn, currentPrescriptionId);
            } catch (Throwable ignore) { /* non-fatal; continue loading */ }
            java.util.List<com.example.healthflow.model.PrescriptionItem> fresh =
                    new com.example.healthflow.dao.PrescriptionItemDAO().listByPrescription(conn, currentPrescriptionId);
            javafx.collections.ObservableList<PrescItemRow> rows = javafx.collections.FXCollections.observableArrayList();
            for (var it : fresh) {
                PrescItemRow r = toRow(it);    // تحويل موحّد
                rows.add(r);
            }
            if (TablePrescriptionItems != null) TablePrescriptionItems.setItems(rows);
        } catch (Exception ex) {
            toast("Failed to reload items: " + ex.getMessage(), "warn");
        }
    }

    private Long ensurePatientFromHeaderIfPossible(Connection c) {
        try {
            if (PatientNameTF == null || PatientNameTF.getText() == null) return null;
            String header = PatientNameTF.getText().trim();
            if (header.isEmpty()) return null;
            // Extract NID (digits) and name (text before '•' if present)
            String digits = header.replaceAll("[^0-9]", "");
            String name = header;
            int dot = header.lastIndexOf('•');
            if (dot >= 0) name = header.substring(0, dot).trim();
            if (name == null || name.isBlank() || digits == null || digits.isBlank()) return null;

            // 1) If exists → return
            Long uid = doctorDAO.findPatientUserIdByNationalId(c, digits);
            if (uid != null && uid > 0) {
                selectedPatientUserId = uid;
                selectedPatientName = name;
                selectedPatientNationalId = digits;
//                System.out.println("[ENSURE_PATIENT_FROM_HEADER] Existing by NID -> userId=" + uid);
                return uid;
            }

            // 2) Create minimal user + patient, with a temporary email to satisfy DB trigger
            // prepare a unique, temporary email to satisfy "at least one contact" DB rule
            String tempEmailBase = "guest+" + digits + "@temp.local";
            String tempEmail = tempEmailBase;
            // attempt insert; if unique violation on email happens, append a millis suffix
            try (var psU = c.prepareStatement(
                    "INSERT INTO users (full_name, national_id, role, email) VALUES (?, ?, 'PATIENT', ?) RETURNING id");
                 var psP = c.prepareStatement(
                         "INSERT INTO patients (user_id) VALUES (?) RETURNING id")) {
                psU.setString(1, name);
                psU.setString(2, digits);
                psU.setString(3, tempEmail);
                try (var rsU = psU.executeQuery()) {
                    if (rsU.next()) {
                        long newUserId = rsU.getLong(1);
                        psP.setLong(1, newUserId);
                        try (var rsP = psP.executeQuery()) {
                            if (rsP.next()) {
                                long newPatientId = rsP.getLong(1);
//                                System.out.println("[ENSURE_PATIENT_FROM_HEADER] Created userId=" + newUserId + ", patientId=" + newPatientId);
                                selectedPatientUserId = newUserId;
                                selectedPatientName = name;
                                selectedPatientNationalId = digits;
                                // Lock header to avoid accidental edits after creation
                                setPatientHeader(name, digits, false);
                                return newUserId;
                            }
                        }
                    }
                }
            } catch (java.sql.SQLException ex) {
                // if email unique constraint fails, retry once with suffix
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("unique")
                        && ex.getMessage().toLowerCase().contains("email")) {
                    String retryEmail = "guest+" + digits + "+" + System.currentTimeMillis() + "@temp.local";
                    try (var psU2 = c.prepareStatement(
                            "INSERT INTO users (full_name, national_id, role, email) VALUES (?, ?, 'PATIENT', ?) RETURNING id");
                         var psP2 = c.prepareStatement(
                                 "INSERT INTO patients (user_id) VALUES (?) RETURNING id")) {
                        psU2.setString(1, name);
                        psU2.setString(2, digits);
                        psU2.setString(3, retryEmail);
                        try (var rsU2 = psU2.executeQuery()) {
                            if (rsU2.next()) {
                                long newUserId = rsU2.getLong(1);
                                psP2.setLong(1, newUserId);
                                try (var rsP2 = psP2.executeQuery()) {
                                    if (rsP2.next()) {
                                        long newPatientId = rsP2.getLong(1);
//                                        System.out.println("[ENSURE_PATIENT_FROM_HEADER] Created (retry) userId=" + newUserId + ", patientId=" + newPatientId);
                                        selectedPatientUserId = newUserId;
                                        selectedPatientName = name;
                                        selectedPatientNationalId = digits;
                                        setPatientHeader(name, digits, false);
                                        return newUserId;
                                    }
                                }
                            }
                        }
                    } catch (Exception ex2) {
                        System.out.println("[ENSURE_PATIENT_FROM_HEADER] Retry failed: " + ex2.getMessage());
                    }
                } else {
                    System.out.println("[ENSURE_PATIENT_FROM_HEADER] Insert failed: " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            System.out.println("[ENSURE_PATIENT_FROM_HEADER] Failed: " + ex.getMessage());
        }
        return null;
    }

    /**
     * Ensure draft prescription exists in DB and return its id (creates one if missing).
     */
    private Long ensureDraftPrescription() throws Exception {
        if (currentPrescriptionId != null) return currentPrescriptionId;

        var u = Session.get();
        if (u == null) throw new IllegalStateException("No logged-in user.");
        Long patientId = resolveCurrentPatientId();

        // If not resolved, try to create a minimal patient from header "name • NID"
        if (patientId == null) {
            Long maybeCreatedUserId;
            try {
                maybeCreatedUserId = ensurePatientFromHeaderIfPossible(Database.get());
            } catch (Exception e) {
                maybeCreatedUserId = null;
            }
            if (maybeCreatedUserId != null) {
                patientId = maybeCreatedUserId;
            }
        }

        try (Connection c = Database.get()) {
            c.setAutoCommit(true);
            PrescriptionDAO pDao = new PrescriptionDAO();
            DoctorDAO dDao = new DoctorDAO();

            // resolve doctors.id (NOT users.id). Create profile if missing.
            Long doctorId = dDao.findDoctorIdByUserId(c, u.getId());
            if (doctorId == null) {
                dDao.ensureProfileForUser(c, u.getId());
                doctorId = dDao.findDoctorIdByUserId(c, u.getId());
            }
            if (doctorId == null) {
                throw new IllegalStateException("Doctor profile not found/created for user " + u.getId());
            }

            // translate patient user id -> patients.id
            if (patientId == null) {
                toast("Enter patient National ID in the header field or choose a patient from the list.", "warn");
                throw new IllegalStateException("No patient selected.");
            }
            Long realPatientId = dDao.findPatientIdByUserId(c, patientId);
            if (realPatientId == null) {
                throw new IllegalStateException("Patient profile not found for userId=" + patientId + " (patients.id lookup failed)");
            }

            String diagnosisText = (DiagnosisTF != null) ? DiagnosisTF.getText() : null;

            // pass doctors.id and patients.id (FK-safe)
            var p = pDao.create(c, null, doctorId, realPatientId, diagnosisText);
            currentPrescriptionId = p.getId();
            toast("Draft prescription #" + currentPrescriptionId + " created.", "ok");
            return currentPrescriptionId;
        }

    }

    /**
     * Lookup medicines.id by name (case-insensitive, trimmed). Returns null if not found.
     */
    private Long findMedicineIdByName(Connection c, String name) throws SQLException {
        if (name == null) return null;
        String q = name.trim();
        if (q.isEmpty()) return null;
        try (java.sql.PreparedStatement ps = c.prepareStatement(
                "SELECT id, name FROM medicines WHERE LOWER(name) = LOWER(?) LIMIT 1")) {
            ps.setString(1, q);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // (اختياري) ثبّت قيمة الكومبو على الصياغة الرسمية من الداتابيز
                    String canon = rs.getString("name");
                    if (medicineName_combo != null && canon != null && !canon.isBlank()) {
                        final String canonFinal = canon;
                        Platform.runLater(() -> medicineName_combo.setValue(canonFinal));
                    }
                    return rs.getLong("id");
                }
            }
        }
        return null;
    }

    /* ================= Add Medicine dialog -> add row into table ================= */
    private void addMedicineFromDialog() {
        try {
            Long prescId = ensureDraftPrescription(); // تأكد من وجود وصفة في DB


            // 1) medicine id/name
            String medNameText = (medicineName_combo != null && getSelectedMedicineName() != null)
                    ? getSelectedMedicineName() : "";
            String medName = medNameText;   // اسم من الكومبو
            String diagnosisTextUi = (DiagnosisTF != null && DiagnosisTF.getText() != null)
                    ? DiagnosisTF.getText().trim() : "";
            Long medId;
            try (Connection cLookup = Database.get()) {
                medId = findMedicineIdByName(cLookup, medName);  // يجيب id لو موجود
            }

            if (medName == null || medName.isBlank()) {
                toast("Please choose a medicine.", "warn");
                return;
            }

            // 2) dosage parts
            String dose = (doseText != null && doseText.getText() != null) ? doseText.getText().trim() : "";
            String freqStr = (freq_day != null && freq_day.getText() != null) ? freq_day.getText().trim() : "";
            String durStr = (duration != null && duration.getText() != null) ? duration.getText().trim() : "";
            int freqPerDay = safeParseInt(freqStr, 0);
            int days = safeParseInt(durStr, 0);
            Integer doseInt = null;
            try {
                doseInt = (dose.isBlank() ? null : Integer.valueOf(dose));
            } catch (Exception ignored) {
            }
            if (doseInt == null || doseInt <= 0) {
                toast("Enter a positive Dose.", "warn");
                return;
            }
            if (freqPerDay <= 0 || days <= 0) {
                toast("Enter positive numbers for Freq/day and Duration.", "warn");
                return;
            }

            String form = (formCombo != null && formCombo.getValue() != null) ? String.valueOf(formCombo.getValue()) : "";
            String route = (routeCombo != null && routeCombo.getValue() != null) ? String.valueOf(routeCombo.getValue()) : "";
            String strength = (strength_combo != null && strength_combo.getValue() != null) ? String.valueOf(strength_combo.getValue()) : "";
            String notesStr = (nots_Pre != null && nots_Pre.getText() != null) ? nots_Pre.getText().trim() : "";

            // 3) compose dosage text
            StringBuilder ds = new StringBuilder();
            if (!strength.isBlank()) ds.append(strength).append(" ");
            if (!form.isBlank()) ds.append(form).append(" \u2022 ");
            if (!dose.isBlank()) ds.append(dose).append(" \u2022 ");
            ds.append(freqPerDay).append("x/day \u2022 ").append(days).append("d");
            if (!route.isBlank()) ds.append(" \u2022 ").append(route);
            if (!notesStr.isBlank()) ds.append(" \u2022 ").append(notesStr);

            // 4) quantity: dose * freq/day * days
            int doseCount = Math.max(1, doseInt);
            int qty = Math.max(1, doseCount * Math.max(1, freqPerDay) * Math.max(1, days));

            try (Connection c = Database.get()) {
                c.setAutoCommit(true);
                PrescriptionItemDAO dao = new PrescriptionItemDAO();

                if (editingRow == null || editingRow.getId() <= 0) {
                    // INSERT إلى الداتابيز أولاً
                    PrescriptionItem db = dao.addItem(
                            c, prescId, medId, medName,
                            doseInt,
                            (freqPerDay > 0 ? freqPerDay : null),
                            (days > 0 ? days : null),
                            strength.isBlank() ? null : strength,
                            form.isBlank() ? null : form,
                            route.isBlank() ? null : route,
                            notesStr.isBlank() ? null : notesStr,
                            qty
                    );

                    try {
                        new PrescriptionItemDAO().backfillSuggestions(c, prescId);
                    } catch (Exception ignore) {
                    }

                    // أعرض في الجدول بناءً على البيانات الراجعة (الاسم قد يتظبط بالتريغر)
                    PrescItemRow row = new PrescItemRow();
                    row.setId(db.getId());
                    row.setMedicineId(db.getMedicineId() == null ? 0 : db.getMedicineId());
                    row.setMedicineName(db.getMedicineName());
                    row.setDose(doseInt == null ? 0 : doseInt);
                    row.setFreqPerDay(freqPerDay);
                    row.setDurationDays(days);
                    row.setStrength(strength);
                    row.setForm(form);
                    row.setRoute(route);
                    row.setNotes(notesStr);
                    row.setDosageText(db.getDosageText());
                    row.setQuantity(db.getQuantity());
                    row.setDiagnosis(diagnosisTextUi);
                    row.setStatus(db.getStatus() == null ? "PENDING" : db.getStatus().name());

//                    prescItemsEditable.add(row);
                    Platform.runLater(this::reloadPrescriptionItemsFromDb);

                    toast("The medicine has been added. ", "ok");
                } else {
                    // UPDATE في الداتابيز
                    PrescriptionItem db = dao.updateItem(
                            c, editingRow.getId(), medId, medName,
                            doseInt,
                            (freqPerDay > 0 ? freqPerDay : null),
                            (days > 0 ? days : null),
                            strength.isBlank() ? null : strength,
                            form.isBlank() ? null : form,
                            route.isBlank() ? null : route,
                            notesStr.isBlank() ? null : notesStr,
                            qty
                    );

                    // عكس التحديث على UI
                    editingRow.setMedicineId(db.getMedicineId() == null ? 0 : db.getMedicineId());
                    editingRow.setMedicineName(db.getMedicineName());
                    editingRow.setDose(doseInt == null ? 0 : doseInt);
                    editingRow.setFreqPerDay(freqPerDay);
                    editingRow.setDurationDays(days);
                    editingRow.setStrength(strength);
                    editingRow.setForm(form);
                    editingRow.setRoute(route);
                    editingRow.setNotes(notesStr);
                    editingRow.setDosageText(db.getDosageText());
                    editingRow.setQuantity(db.getQuantity());
                    editingRow.setDiagnosis(diagnosisTextUi);
                    if (TablePrescriptionItems != null) TablePrescriptionItems.refresh();
                    toast("Medication updated in DB.", "ok");
                    editingRow = null;
                    if (InsertMedicine != null) InsertMedicine.setText("Add");
                }
            }

            clearAddForm();
            showPrescriptionPane();
        } catch (Exception ex) {
            showError("Add/Save Medicine", ex);
        }
    }

    /**
     * Open Add pane with the selected row prefilled to edit.
     */
    private void openEditSelectedItem() {
        PrescItemRow sel = (TablePrescriptionItems == null)
                ? null : TablePrescriptionItems.getSelectionModel().getSelectedItem();
        if (sel == null) {
            toast("Select a row to edit.", "warn");
            return;
        }
        editingRow = sel;

        // نملأ ما نقدر نستعيده بثقة
        if (medicineName_combo != null) medicineName_combo.setValue(sel.getMedicineName());
        if (duration != null) duration.setText(String.valueOf(Math.max(0, sel.getDurationDays())));

        // تقدير freq/day = quantity / duration إن أمكن (قسم صحيح)
        if (freq_day != null) {
            int days = Math.max(1, sel.getDurationDays());
            int q = Math.max(0, sel.getQuantity());
            int freq = (q % days == 0 && days > 0) ? (q / days) : 0;
            freq_day.setText(freq > 0 ? String.valueOf(freq) : "");
        }

        // املأ الحقول القابلة للاسترجاع من الصف المحدد
        if (doseText != null) doseText.setText(sel.getDose() > 0 ? String.valueOf(sel.getDose()) : "");
        if (strength_combo != null) {
            if (sel.getStrength() != null && !sel.getStrength().isBlank())
                strength_combo.getSelectionModel().select(sel.getStrength());
            else strength_combo.getSelectionModel().clearSelection();
        }
        if (formCombo != null) {
            if (sel.getForm() != null && !sel.getForm().isBlank()) formCombo.getSelectionModel().select(sel.getForm());
            else formCombo.getSelectionModel().clearSelection();
        }
        if (routeCombo != null) {
            if (sel.getRoute() != null && !sel.getRoute().isBlank())
                routeCombo.getSelectionModel().select(sel.getRoute());
            else routeCombo.getSelectionModel().clearSelection();
        }
        if (nots_Pre != null) nots_Pre.setText(sel.getNotes() == null ? "" : sel.getNotes());

        if (InsertMedicine != null) InsertMedicine.setText("Save");
        showPrescriptionPaneToAddMedication();
        setupQtyLiveLabel();

        // أخفِ قائمة الاقتراحات لو كانت ظاهرة
// ركّز على الكومبو الحديثة بدل الحقل القديم
        if (medicineName_combo != null) medicineName_combo.requestFocus();
    }

    /**
     * Delete selected row from the UI table and database, regardless of which list backs the table.
     */
    private void deleteSelectedItem() {
        PrescItemRow sel = (TablePrescriptionItems == null)
                ? null : TablePrescriptionItems.getSelectionModel().getSelectedItem();
        if (sel == null) {
            toast("Select a row to delete.", "warn");
            return;
        }

        // 1) Remove from the table's backing list (fallback to prescItemsEditable if needed)
        boolean removed = false;
        try {
            if (TablePrescriptionItems != null && TablePrescriptionItems.getItems() != null) {
                removed = TablePrescriptionItems.getItems().remove(sel);
            }
            if (!removed && prescItemsEditable != null) {
                removed = prescItemsEditable.remove(sel);
            }
        } catch (Throwable ignore) { /* keep going */ }

        if (!removed) {
            // If not removed from a local list, still continue with DB delete (source of truth)
            toast("Removing from table failed; proceeding to delete from database...", "warn");
        }

        // 2) Delete from DB if the row exists there
        final long idToDelete = sel.getId();
        if (idToDelete > 0) {
            new Thread(() -> {
                try (Connection c = Database.get()) {
                    c.setAutoCommit(true);
                    new com.example.healthflow.dao.PrescriptionItemDAO().deleteById(c, idToDelete);
                    Platform.runLater(() -> {
                        toast("Row deleted from database.", "ok");
                        // Reload authoritative data from DB to keep UI consistent
                        reloadPrescriptionItemsFromDb();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> toast("DB delete failed: " + ex.getMessage(), "err"));
                }
            }, "delete-presc-item").start();
        } else {
            // Local-only row; just refresh UI
            if (TablePrescriptionItems != null) TablePrescriptionItems.refresh();
            toast("Row removed.", "ok");
        }
    }

    /**
     * Clear Add-Medicine form fields.
     */
    private void clearAddForm() {
        if (medicineName_combo != null) medicineName_combo.setValue(null);
        if (doseText != null) doseText.clear();
        if (freq_day != null) freq_day.clear();
        if (duration != null) duration.clear();
        if (nots_Pre != null) nots_Pre.clear();
        if (formCombo != null) formCombo.getSelectionModel().clearSelection();
        if (routeCombo != null) routeCombo.getSelectionModel().clearSelection();
        if (strength_combo != null) strength_combo.getSelectionModel().clearSelection();
    }

    /**
     * Helper: Apply a selected medicine to the form and hide suggestions.
     */
    private void applySelectedMedicine(MedicineRow m) {
        if (m == null) return;
        try {
            if (medicineName_combo != null) medicineName_combo.setValue(m.getName());
        } catch (Throwable ignored) {
        }
        // Optional: fill other fields if present on MedicineRow
        try {
            if (strength_combo != null && m.getStrength() != null) {
                strength_combo.getSelectionModel().select(m.getStrength());
            }
        } catch (Throwable ignored) {
        }
        try {
            if (formCombo != null && m.getForm() != null) {
                formCombo.getSelectionModel().select(m.getForm());
            }
        } catch (Throwable ignored) {
        }
        try {
            if (routeCombo != null && m.getRoute() != null) {
                routeCombo.getSelectionModel().select(m.getRoute());
            }
        } catch (Throwable ignored) {
        }


    }

    private void updateSelectedItemFromEditors() {
        PrescItemRow target = (TablePrescriptionItems != null)
                ? TablePrescriptionItems.getSelectionModel().getSelectedItem()
                : null;
        if (target == null && editingRow != null) target = editingRow;
        if (target == null) {
            toast("Select a medicine row to update.", "warn");
            return;
        }

        // اقرأ قيم المحرّرات
        String medName = getSelectedMedicineName();
        String form = (formCombo != null) ? formCombo.getValue() : null;
        String route = (routeCombo != null) ? routeCombo.getValue() : null;
        String strength = (strength_combo != null) ? strength_combo.getValue() : null;
        String notes = (nots_Pre != null && nots_Pre.getText() != null) ? nots_Pre.getText().trim() : null;
        String diagnosisTextUi = (DiagnosisTF != null && DiagnosisTF.getText() != null)
                ? DiagnosisTF.getText().trim() : "";
        // Diagnosis يبقى على مستوى الهيدر/الوصفة وليس عنصر الدواء

        int dose = 0, freq = 0, dur = 0;
        try {
            if (doseText != null && !doseText.getText().isBlank()) dose = Integer.parseInt(doseText.getText().trim());
        } catch (Exception ignored) {
        }
        try {
            if (freq_day != null && !freq_day.getText().isBlank()) freq = Integer.parseInt(freq_day.getText().trim());
        } catch (Exception ignored) {
        }
        try {
            if (duration != null && !duration.getText().isBlank()) dur = Integer.parseInt(duration.getText().trim());
        } catch (Exception ignored) {
        }
        int qty = (dose > 0 && freq > 0 && dur > 0) ? (dose * freq * dur) : target.getQuantity();
        // طبّق القيم على الصف (تجاهل لو Setter مش موجود)
        try {
            target.setMedicineName(medName);
        } catch (Throwable ignored) {
        }
        try {
            target.setDosage(String.valueOf(dose));
        } catch (Throwable ignored) {
        }
        try {
            target.setDurationDays(dur);
        } catch (Throwable ignored) {
        }
        try {
            target.setQuantity(qty);
        } catch (Throwable ignored) {
        }
        try {
            target.setNotes(notes);
        } catch (Throwable ignored) {
        }
        try {
            target.setDiagnosis(diagnosisTextUi);
        } catch (Throwable ignored) {
        }

        if (TablePrescriptionItems != null) TablePrescriptionItems.refresh();
        toast("Item updated.", "info");
    }


    // ================== ComboBox population for medication ==================
    // ====== FXML TableColumn declarations for prescription items ======

    private void populateMedicineCombos() {
        // Populate combo boxes with appropriate options
        if (strength_combo != null) {
            strength_combo.getItems().setAll(
                    "125 mg", "250 mg", "500 mg", "750 mg", "1 g",
                    "2 g", "5 mg/ml", "10 mg/ml", "20 mg/ml"
            );
        }

        if (formCombo != null) {
            formCombo.getItems().setAll(
                    "Tablet", "Capsule", "Syrup", "Injection", "Suspension",
                    "Ointment", "Cream", "Drops", "Inhaler", "Powder"
            );
        }

        if (routeCombo != null) {
            routeCombo.getItems().setAll(
                    "Oral", "Intravenous (IV)", "Intramuscular (IM)", "Subcutaneous (SC)",
                    "Topical", "Inhalation", "Ophthalmic", "Otic", "Rectal", "Vaginal"
            );
        }
        setupQtyLiveLabel();
    }

    // ==== Tiny toast on alertLabel (auto-hide) ====
    private Timeline toastTimeline;

    private void toast(String msg, String type) {
        if (alertLabel == null) return;
        Platform.runLater(() -> {
            alertLabel.setText(msg);
            alertLabel.setVisible(true);
            // simple styling by type
            String base = "-fx-background-radius: 6; -fx-padding: 6 10; -fx-text-fill: white;";
            switch (type == null ? "" : type) {
                case "ok" -> alertLabel.setStyle(base + "-fx-background-color: #28a745;");
                case "warn" -> alertLabel.setStyle(base + "-fx-background-color: #ffc107; -fx-text-fill: #222;");
                case "err" -> alertLabel.setStyle(base + "-fx-background-color: #dc3545;");
                default -> alertLabel.setStyle(base + "-fx-background-color: #17a2b8;");
            }
            if (toastTimeline != null) toastTimeline.stop();
            alertLabel.setOpacity(1.0);
            toastTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(2.5), ev -> {
                        // fade out
                        Timeline fade = new Timeline(
                                new KeyFrame(Duration.millis(0), e -> alertLabel.setOpacity(1)),
                                new KeyFrame(Duration.millis(500), e -> alertLabel.setOpacity(0))
                        );
                        fade.setOnFinished(e -> alertLabel.setVisible(false));
                        fade.play();
                    })
            );
            toastTimeline.play();
        });
    }

    /**
     * Tiny press animation (visual feedback) for table action buttons.
     */
    private void playPressAnim(javafx.scene.control.Button b) {
        if (b == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(120), b);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(0.92);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    private String getSelectedMedicineName() {
        String v = (medicineName_combo == null) ? null : medicineName_combo.getValue();
        return (v == null) ? "" : v.trim();
    }

    private static int safeParseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    /* ================= Actions ================= */

    private void showPatientDetails(AppointmentRow row) {
        showInfo("Patient details",
                "Name: " + row.getPatientName() +
                        "\nNational ID: " + safe(row.getNationalId()) +
                        "\nMedical history:\n" + safe(row.getMedicalHistory()));
    }


    private void showPatientDetails(String name, String history) {
        showInfo("Patient details",
                "Name: " + safe(name) + "\n\nMedical history:\n" + safe(history));
    }

    private void completeAppointment(AppointmentRow row) {
        if (!monitor.isOnline()) {
            showWarn("Offline", "You are offline. Please reconnect and try again.");
            return;
        }
        new Thread(() -> {
            try {
                svc.markCompleted(row.getId());
                Platform.runLater(() -> {
                    row.setStatus("COMPLETED");
                    if (AppointmentsTable != null) AppointmentsTable.refresh();
                    var _dashDate = (datePickerPatientsWithDoctorDash != null && datePickerPatientsWithDoctorDash.getValue() != null)
                            ? datePickerPatientsWithDoctorDash.getValue()
                            : java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
                    loadStatsForDateAsync(_dashDate);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showWarn("Update", "Could not mark as completed. Please try again later."));
            }
        }, "doc-complete").start();
    }


    private void openPrescription(AppointmentRow row) {
        try {
            // 0) حضّر هيدر المريض (للاحتياط) حتى لو ما كان مُختار من قبل
            try {
                if (row != null) {
                    String nid = row.getNationalId();
                    String name = row.getPatientName();
                    // استخدم دالة المساعدة مباشرة (تتحقق داخليًا من PatientNameTF)
                    setPatientHeader(name, nid, true);
                }
            } catch (Throwable ignore) {}

            // 1) تأكد من وجود وصفة مسودة في الداتابيز، واربطها كـ currentPrescriptionId
            currentPrescriptionId = null; // أعد التقييم دائمًا بناءً على المريض الحالي
            Long prescId = ensureDraftPrescription();
            this.currentPrescriptionId = prescId;

            // 2) Backfill اقتراحات التغليف قبل تحميل العناصر → يضمن أن عمود Pack لا يظهر "—"
            try (Connection c = Database.get()) {
                c.setAutoCommit(true);
                new com.example.healthflow.dao.PrescriptionItemDAO().backfillSuggestions(c, prescId);
            } catch (Exception ignored) {}

            // 3) حمّل عناصر الوصفة واعرض شاشة الوصفة
//            reloadPrescriptionItemsFromDb();
            loadPrescriptionItemsFromDb(prescId);
            showPrescriptionPane();
//            reloadPrescriptionItemsFromDb();
        } catch (Exception ex) {
            showError("Open Prescription", ex);
        }
    }

    /** Open prescription pane from the items table action.
     *  Ensures backfill of suggestions so colPack shows values immediately. */
    public void openPrescriptionFromItems() {
        try {
            Long prescId = (currentPrescriptionId == null || currentPrescriptionId <= 0)
                    ? ensureDraftPrescription() : currentPrescriptionId;
            try (java.sql.Connection c = Database.get()) {
                c.setAutoCommit(true);
                new com.example.healthflow.dao.PrescriptionItemDAO().backfillSuggestions(c, prescId);
            } catch (Exception ignored) {}
            reloadPrescriptionItemsFromDb();
            showPrescriptionPane();
        } catch (Exception ex) {
            showError("Open Prescription", ex);
        }
    }

    /* ================= Helpers ================= */
    private String firstName(String full) {
        if (full == null || full.isBlank()) return "user";
        return full.trim().split("\\s+")[0];
    }

    private static int ageFromDob(LocalDate dob) {
        if (dob == null) return 0;
        return java.time.Period.between(dob, LocalDate.now()).getYears();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private void showError(String title, Exception ex) {
        ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(ex.getMessage());
        a.showAndWait();
    }

    private void showWarn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static void setTextSafe(Label label, String text) {
        if (label != null) label.setText(text);
    }

    /* ================= Row models ================= */
    public static class AppointmentRow {
        private final LongProperty id = new SimpleLongProperty();
        private final StringProperty patientName = new SimpleStringProperty();
        private final StringProperty nationalId = new SimpleStringProperty();
        private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
        private final StringProperty timeStr = new SimpleStringProperty();
        private final StringProperty status = new SimpleStringProperty();
        private final LongProperty patientUserId = new SimpleLongProperty();
        private final StringProperty medicalHistory = new SimpleStringProperty();

        public static AppointmentRow of(Appt a) {
            AppointmentRow r = new AppointmentRow();
            r.setId(a.id);
            r.setPatientName(a.patientName);
            r.setNationalId(a.patientNationalId);
            r.setDate(a.date);
            r.setTimeStr(a.time == null ? "" : a.time.toString());
            r.setStatus(a.status);
            r.setPatientUserId(a.patientUserId);
            r.setMedicalHistory(a.medicalHistory);
            return r;
        }

        public long getId() {
            return id.get();
        }

        public void setId(long v) {
            id.set(v);
        }

        public LongProperty idProperty() {
            return id;
        }

        public String getPatientName() {
            return patientName.get();
        }

        public void setPatientName(String v) {
            patientName.set(v);
        }

        public StringProperty patientNameProperty() {
            return patientName;
        }

        public String getNationalId() {
            return nationalId.get();
        }

        public void setNationalId(String v) {
            nationalId.set(v);
        }

        public StringProperty nationalIdProperty() {
            return nationalId;
        }

        public LocalDate getDate() {
            return date.get();
        }

        public void setDate(LocalDate v) {
            date.set(v);
        }

        public ObjectProperty<LocalDate> dateProperty() {
            return date;
        }

        public String getTimeStr() {
            return timeStr.get();
        }

        public void setTimeStr(String v) {
            timeStr.set(v);
        }

        public StringProperty timeStrProperty() {
            return timeStr;
        }

        public String getStatus() {
            return status.get();
        }

        public void setStatus(String v) {
            status.set(v);
        }

        public StringProperty statusProperty() {
            return status;
        }

        public long getPatientUserId() {
            return patientUserId.get();
        }

        public void setPatientUserId(long v) {
            patientUserId.set(v);
        }

        public String getMedicalHistory() {
            return medicalHistory.get();
        }

        public void setMedicalHistory(String v) {
            medicalHistory.set(v);
        }

        public StringProperty medicalHistoryProperty() {
            return medicalHistory;
        }
    }

    public static class PatientRow {
        private final StringProperty nationalId = new SimpleStringProperty();
        private final StringProperty fullName = new SimpleStringProperty();
        private final StringProperty gender = new SimpleStringProperty();
        private final IntegerProperty age = new SimpleIntegerProperty();
        private final StringProperty medicalHistory = new SimpleStringProperty();
        private final LongProperty userId = new SimpleLongProperty(0);

        public PatientRow(String nid, String name, String gender, int age, String history) {
            setNationalId(nid);
            setFullName(name);
            setGender(gender);
            setAge(age);
            setMedicalHistory(history);
        }

        public String getNationalId() {
            return nationalId.get();
        }

        public void setNationalId(String v) {
            nationalId.set(v);
        }

        public StringProperty nationalIdProperty() {
            return nationalId;
        }

        public String getFullName() {
            return fullName.get();
        }

        public void setFullName(String v) {
            fullName.set(v);
        }

        public StringProperty fullNameProperty() {
            return fullName;
        }

        public String getGender() {
            return gender.get();
        }

        public void setGender(String v) {
            gender.set(v);
        }

        public StringProperty genderProperty() {
            return gender;
        }

        public int getAge() {
            return age.get();
        }

        public void setAge(int v) {
            age.set(v);
        }

        public IntegerProperty ageProperty() {
            return age;
        }

        public String getMedicalHistory() {
            return medicalHistory.get();
        }

        public void setMedicalHistory(String v) {
            medicalHistory.set(v);
        }

        public StringProperty medicalHistoryProperty() {
            return medicalHistory;
        }

        public long getUserId() {
            return userId.get();
        }

        public void setUserId(long v) {
            userId.set(v);
        }

        public LongProperty userIdProperty() {
            return userId;
        }
    }


    // ===== Helper for live calculation of quantity label =====
    private void setupQtyLiveLabel() {
        System.out.println("[qty] label=" + (qtyLabelCulc != null) +
                ", dose=" + (doseText != null) +
                ", freq=" + (freq_day != null) +
                ", dur=" + (duration != null) +
                ", form=" + (formCombo != null));
        if (qtyLabelCulc == null) return;
        // Update now and on changes
        Runnable updater = this::updateQtyLabel;
        if (doseText != null) doseText.textProperty().addListener((obs, o, n) -> updater.run());
        if (freq_day != null) freq_day.textProperty().addListener((obs, o, n) -> updater.run());
        if (duration != null) duration.textProperty().addListener((obs, o, n) -> updater.run());
        if (formCombo != null) formCombo.valueProperty().addListener((obs, o, n) -> updater.run());
        updater.run();

    }

    private void updateQtyLabel() {
        if (qtyLabelCulc == null) return;
        int d = (doseText != null && doseText.getText() != null) ? safeParseInt(doseText.getText(), 0) : 0;
        int f = (freq_day != null && freq_day.getText() != null) ? safeParseInt(freq_day.getText(), 0) : 0;
        int g = (duration != null && duration.getText() != null) ? safeParseInt(duration.getText(), 0) : 0;
        int total = (d > 0 && f > 0 && g > 0) ? d * f * g : 0;
        String formVal = (formCombo != null && formCombo.getValue() != null) ? String.valueOf(formCombo.getValue()) : null;
        String unit = unitFromForm(formVal);
        StringBuilder sb = new StringBuilder("");
        if (total > 0) {
            sb.append(" = ").append(total).append(" ").append(unit);
        }
        qtyLabelCulc.setText(sb.toString());
    }

    private String unitFromForm(String form) {
        if (form == null) return "units";
        String f = form.trim().toLowerCase();
        if (f.equals("tablet") || f.equals("tab") || f.equals("tablets")) return "tabs";
        if (f.equals("capsule") || f.equals("capsules") || f.equals("cap")) return "caps";
        if (f.equals("syrup") || f.equals("suspension") || f.equals("injection") || f.equals("drops")) return "ml";
        if (f.equals("cream") || f.equals("ointment")) return "g";
        return "units";
    }



}