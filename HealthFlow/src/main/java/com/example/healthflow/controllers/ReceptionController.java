package com.example.healthflow.controllers;

import com.example.healthflow.db.Database;
import com.example.healthflow.dao.DoctorDAO;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.service.AuthService.Session;
import com.example.healthflow.model.dto.PatientView;
import com.example.healthflow.service.PatientService;
import com.example.healthflow.ui.ConnectivityBanner;
import com.example.healthflow.ui.OnlineBindings;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;


import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class ReceptionController {

    /* ============ UI ============ */
    @FXML private AnchorPane DashboardAnchorPane;
    @FXML private AnchorPane PatientAnchorPane;
    @FXML private AnchorPane AppointmentsAnchorPane;
    @FXML private AnchorPane DoctorAnchorPane;
    @FXML private StackPane rootPane;

    @FXML private Button DachboardButton;
    @FXML private Button PatientsButton;
    @FXML private Button AppointmentsButton;
    @FXML private Button BackButton;
    @FXML private Button DoctorsButton;

    @FXML private Label DateOfDay;
    @FXML private Label time;
    @FXML private Label welcomeUser;

    @FXML private Label UsernameLabel;
    @FXML private Label UserIdLabel;

    // ===== Patients form =====
    @FXML private TextField FullNameTextField;
    @FXML private TextField PatientIdTextField;   // National Id
    @FXML private ComboBox<Gender> GenderComboBox;
    @FXML private DatePicker DateOfBirthPicker;
    @FXML private TextField PhoneTextField;
    @FXML private TextArea  medicalHistory;

    @FXML private Button InsertButton;
    @FXML private Button UpdateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearBtn;

    @FXML private TextField search;

    @FXML private TableView<PatientRow> patientTable;
    @FXML private TableColumn<PatientRow, String>    colNationalId;
    @FXML private TableColumn<PatientRow, String>    colName;
    @FXML private TableColumn<PatientRow, String>    colGender;
    @FXML private TableColumn<PatientRow, LocalDate> colDob;
    @FXML private TableColumn<PatientRow, String>    colPhoneNumber;
    @FXML private TableColumn<PatientRow, String>    colMedicalHistory;

    @FXML private Label NumberOfTotalAppointments;
    @FXML private Label NumberOfTotalDoctors;
    @FXML private Label NumberOfTotalPatients;

    @FXML private Circle ActiveStatus;

    @FXML private TableColumn<?, ?> AppointmentIdColumn;
    @FXML private AnchorPane Appointments;
    @FXML private AnchorPane CenterAnchorPane;
    @FXML private AnchorPane Doctors;
    @FXML private AnchorPane Patients;

    @FXML private Label TotalAppointments;
    @FXML private Label TotalDoctors;

    @FXML private TableColumn<?, ?> colActionDash;
    @FXML private TableColumn<?, ?> colAppintementDateDash;
    @FXML private TableColumn<?, ?> colAppintementTimeDash;
    @FXML private TableColumn<?, ?> colDoctorNameDash;
    @FXML private TableColumn<?, ?> colPatientNameDash;
    @FXML private BarChart<?, ?> patientDetails;

    @FXML private TextField searchAppointmentDach;
    @FXML private TextField searchDoctor;

    @FXML private Button insertAppointments;
    @FXML private Label TotalPatients;
    @FXML private Button BookAppointmentFromPateint;
    @FXML private Button updateAppointments;
    @FXML private ComboBox<String> DoctorspecialtyApp;           // list of specialties
    @FXML private ComboBox<DoctorDAO.DoctorOption> avilabelDoctorApp; // available doctors for selected specialty
    @FXML private Button clear_Appointments;
    @FXML private Button deleteAppointments;

    @FXML private TableColumn<?, ?> colAppointmentIDAppointemnt;
    @FXML private TableColumn<?, ?> colDateAppointemnt;
    @FXML private TableColumn<?, ?> colDoctorNameAppointemnt;
    @FXML private TableColumn<?, ?> colPatientNameAppointemnt;
    @FXML private TableColumn<?, ?> colSpecialty;
    @FXML private TableColumn<?, ?> colStatusAppointemnt;
    @FXML private TableColumn<?, ?> colTimeAppointemnt;
    @FXML private Button deleteButtonAppointemnt;
    @FXML private Label getPatientName;
    @FXML private Label getPatientID;

    // ===== Doctors table (تأكّد من fx:id في FXML) =====
    @FXML private TableView<DoctorRow> DocTable_Recption;
    @FXML private TableColumn<DoctorRow, String>  colDoctor_name;
    @FXML private TableColumn<DoctorRow, String>  colDoctor_Gender;
    @FXML private TableColumn<DoctorRow, String>  colDoctor_Phone;
    @FXML private TableColumn<DoctorRow, String>  colDoctor_Specialty;
    @FXML private TableColumn<DoctorRow, String>  colDoctor_bio;
    @FXML private TableColumn<DoctorRow, String>  colDoctor_Status;
    @FXML private TableColumn<DoctorRow, Boolean> colDoctor_available;


    @FXML
    private DatePicker AppointmentDate;
//    -----



    @FXML
    private TextField appointmentSetTime;


    //    @FXML
    //    private DatePicker setAppointmentDate;
    @FXML private ComboBox<DoctorDAO.Slot> cmbSlots;






    // To color current nav button
    private static final String ACTIVE_CLASS = "current";
    private static final java.time.format.DateTimeFormatter SLOT_FMT_12H =
            java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
    private void markNavActive(Button active) {
        Button[] all = {DachboardButton, DoctorsButton, PatientsButton, AppointmentsButton};
        for (Button b : all) {
            b.getStyleClass().remove(ACTIVE_CLASS);
            if (!b.getStyleClass().contains("nav-btn")) b.getStyleClass().add("nav-btn");
        }
        if (active != null && !active.getStyleClass().contains(ACTIVE_CLASS)) {
            active.getStyleClass().add(ACTIVE_CLASS);
        }
    }

    /* ============ Types ============ */
    public enum Gender { MALE, FEMALE }

    /* ============ State ============ */
    private final ObservableList<PatientRow> patientData = FXCollections.observableArrayList();
    private FilteredList<PatientRow> filtered;

    private final ObservableList<DoctorRow> doctorData = FXCollections.observableArrayList();
    private FilteredList<DoctorRow> doctorFiltered;

    private final Navigation navigation = new Navigation();
    private final PatientService patientService = new PatientService();
    private final DoctorDAO doctorDAO = new DoctorDAO();

    /* ============ Connectivity ============ */
    private final ConnectivityMonitor monitor;

    private static volatile boolean listenerRegistered = false;
    private static volatile Boolean lastNotifiedOnline = null;

    public ReceptionController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }
    public ReceptionController() { this(new ConnectivityMonitor()); }

    /* ============ Init ============ */
    @FXML
    private void initialize() {

        monitor.start();

        if (rootPane != null) {
            ConnectivityBanner banner = new ConnectivityBanner(monitor);
            rootPane.getChildren().add(0, banner);
            banner.prefWidthProperty().bind(rootPane.widthProperty());
        }

        OnlineBindings.disableWhenOffline(
                monitor,
                InsertButton, UpdateButton, deleteButton, clearBtn,
                DachboardButton, PatientsButton, AppointmentsButton, DoctorsButton
        );

        if (!listenerRegistered) {
            listenerRegistered = true;
            final boolean[] firstEmissionHandled = { false };
            monitor.onlineProperty().addListener((obs, wasOnline, isOnline) -> {
                if (!firstEmissionHandled[0]) {
                    firstEmissionHandled[0] = true;
                    lastNotifiedOnline = isOnline;
                    return;
                }
                // Skip duplicate notifications; UI reacts via bindings and banner.
                if (lastNotifiedOnline != null && lastNotifiedOnline == isOnline) return;
                lastNotifiedOnline = isOnline;
                // No alerts here — the ConnectivityBanner and OnlineBindings handle UX.
            });
        }

        DachboardButton.setOnAction(e -> showDashboardPane());
        PatientsButton.setOnAction(e -> showPatientsPane());
        AppointmentsButton.setOnAction(e -> showAppointmentPane());
        DoctorsButton.setOnAction(e -> showDoctorPane());
        BackButton.setOnAction(e -> BackAction());

        startClock();

        GenderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
        GenderComboBox.setValue(Gender.MALE);
        DateOfBirthPicker.setValue(null);

        // Default appointment date pickers to today (can still be changed by the user)
        if (AppointmentDate != null && AppointmentDate.getValue() == null) {
            AppointmentDate.setValue(java.time.LocalDate.now());
        }
//        if (setAppointmentDate != null && setAppointmentDate.getValue() == null) {
//            setAppointmentDate.setValue(java.time.LocalDate.now());
//        }

        wirePatientTable();
        wireDoctorTable();
        wireSearchPatients();
        wireSearchDoctors();
        setupDoctorFilters();

        InsertButton.setOnAction(e -> { if (ensureOnlineOrAlert()) doInsertPatient(); });
        UpdateButton.setOnAction(e -> { if (ensureOnlineOrAlert()) doUpdatePatient(); });
        deleteButton.setOnAction(e -> { if (ensureOnlineOrAlert()) doDeletePatient(); });
        clearBtn.setOnAction(e -> clearForm());
        BookAppointmentFromPateint.setOnAction(e -> {
            PatientRow row = (patientTable == null) ? null : patientTable.getSelectionModel().getSelectedItem();
            if (row == null) {
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("Select a patient");
                a.setHeaderText(null);
                a.setContentText("Please select a patient from the table first.");
                a.showAndWait();
                return;
            }
            // Pre-fill appointment panel labels (if present)
            if (getPatientName != null) getPatientName.setText(row.getFullName());
            if (getPatientID != null)   getPatientID.setText(row.getNationalId());
            // Navigate to the appointment pane
            showAppointmentPane();
            if (DoctorspecialtyApp != null && DoctorspecialtyApp.getItems().isEmpty()) loadSpecialtiesAsync();
        });

        Platform.runLater(() -> {
            new Thread(() -> { try { loadHeaderUser(); } catch (Exception ignored) {} }, "hdr-user-load").start();
            new Thread(this::loadPatientsBG, "patients-load").start();
            new Thread(this::loadDoctorsBG, "doctors-load").start();
        });

//        ------------------------
        cmbSlots.setCellFactory(cb -> new ListCell<DoctorDAO.Slot>() {
            @Override
            protected void updateItem(DoctorDAO.Slot s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null :
                        s.from().toLocalTime().format(SLOT_FMT_12H) + " \u2192 " +
                        s.to().toLocalTime().format(SLOT_FMT_12H));
            }
        });
        cmbSlots.setButtonCell(new ListCell<DoctorDAO.Slot>() {
            @Override
            protected void updateItem(DoctorDAO.Slot s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "Select a slot"
                        : s.from().toLocalTime().format(SLOT_FMT_12H) + " \u2192 " +
                          s.to().toLocalTime().format(SLOT_FMT_12H));
            }
        });

        if (AppointmentDate != null) {
            AppointmentDate.valueProperty().addListener((o, a, b) -> refreshSlots());
        }
        if (avilabelDoctorApp != null) {
            avilabelDoctorApp.valueProperty().addListener((o, a, b) -> refreshSlots());
        }
        showDashboardPane();
    }

//    private void refreshSlots() {
//        if (cmbSlots == null) return;
//
//        var doc = (avilabelDoctorApp == null) ? null : avilabelDoctorApp.getValue();
//        var day = (AppointmentDate == null) ? null : AppointmentDate.getValue();
//
//        if (doc == null || day == null) {
//            cmbSlots.setItems(FXCollections.observableArrayList());
//            return;
//        }
//
//        final LocalTime open  = LocalTime.of(9, 0);   // دوام العيادة
//        final LocalTime close = LocalTime.of(15, 0);  // انتهاء الدواء الدوام الساعة 3
//        final int slotMinutes = 20;
//
//        new Thread(() -> {
//            try {
//                var slots = doctorDAO.listFreeSlots(doc.doctorId, day, open, close, slotMinutes);
//
//                // فلترة أوقات الماضي لو اليوم = اليوم الحالي
//                if (day.equals(LocalDate.now())) {
//                    LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
//                    int mod = now.getMinute() % slotMinutes;
//                    // قرب للسلوت القادم
//                    LocalDateTime cutoff = (mod == 0) ? now : now.plusMinutes(slotMinutes - mod);
//
//                    final LocalDateTime cutoffFinal = cutoff; // لازم يكون effectively final للامبدا
//                    slots.removeIf(s -> s.from().isBefore(cutoffFinal));
//                }
//
//                var data = FXCollections.observableArrayList(slots);
//                Platform.runLater(() -> cmbSlots.setItems(data));
//            } catch (Exception e) {
//                e.printStackTrace();
//                Platform.runLater(() -> showWarn("Slots", "Failed to load free slots: " + e.getMessage()));
//            }
//        }, "load-slots").start();
//    }

    private void refreshSlots() {
        if (cmbSlots == null) return;

        var doc = (avilabelDoctorApp == null) ? null : avilabelDoctorApp.getValue();
        var day = (AppointmentDate == null) ? null : AppointmentDate.getValue();

        if (doc == null || day == null) {
            cmbSlots.setItems(FXCollections.observableArrayList());
            return;
        }

        final LocalTime open  = LocalTime.of(9, 0);   // بداية الدوام
        final LocalTime close = LocalTime.of(15, 0);  // نهاية الدوام
        final int slotMinutes = 20;

        new Thread(() -> {
            try {
                var slots = doctorDAO.listFreeSlots(doc.doctorId, day, open, close, slotMinutes);

                // فلترة أوقات الماضي لو اليوم = اليوم الحالي
                if (day.equals(LocalDate.now())) {
                    LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
                    int mod = now.getMinute() % slotMinutes;
                    // نقرب للسلوت القادم
                    LocalDateTime cutoff = (mod == 0) ? now : now.plusMinutes(slotMinutes - mod);

                    final LocalDateTime cutoffFinal = cutoff;
                    slots.removeIf(s -> s.from().isBefore(cutoffFinal));

                    // إذا انتهى الدوام (الآن بعد آخر وقت)
                    if (now.toLocalTime().isAfter(close)) {
                        Platform.runLater(() -> {
                            cmbSlots.getItems().clear();
                            showInfo("Working Hours", "Clinic working hours are over for today.");
                        });
                        return;
                    }
                }

                var data = FXCollections.observableArrayList(slots);
                Platform.runLater(() -> cmbSlots.setItems(data));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showWarn("Slots", "Failed to load free slots: " + e.getMessage()));
            }
        }, "load-slots").start();
    }

//    private void refreshSlots() {
//        if (cmbSlots == null) return;
//
//        var doc = (avilabelDoctorApp == null) ? null : avilabelDoctorApp.getValue();
//        var day = (AppointmentDate == null) ? null : AppointmentDate.getValue();
//
//        if (doc == null || day == null) {
//            cmbSlots.setItems(FXCollections.observableArrayList());
//            return;
//        }
//
//        var open  = LocalTime.of(9, 0);
//        var close = LocalTime.of(15, 0);
//        var slotMinutes = 20;
//
//        new Thread(() -> {
//            try {
//                var slots = doctorDAO.listFreeSlots(doc.doctorId, day, open, close, slotMinutes);
//                Platform.runLater(() -> cmbSlots.setItems(FXCollections.observableArrayList(slots)));
//            } catch (Exception e) {
//                e.printStackTrace();
//                Platform.runLater(() -> showWarn("Slots", "Failed to load free slots: " + e.getMessage()));
//            }
//        }, "load-slots").start();
//    }

    /* ============ Clock (12h) ============ */
    private void startClock() {
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, e -> time.setText(LocalTime.now().format(tf))),
                new KeyFrame(Duration.seconds(1))
        );
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();

        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateOfDay.setText(LocalDate.now().format(df));
    }

    /* ============ Load header user ============ */
    private void loadHeaderUser() {
        var u = Session.get(); if (u == null) return;
        String sql = "SELECT id, full_name FROM users WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, u.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id"); String fullName = rs.getString("full_name");
                    Platform.runLater(() -> {
                        UsernameLabel.setText(fullName);
                        UserIdLabel.setText(Long.toString(id));
                        welcomeUser.setText(firstName(fullName));
                    });
                    return;
                }
            }
        } catch (SQLException ignored) {}
        Platform.runLater(() -> {
            UsernameLabel.setText(u.getFullName());
            UserIdLabel.setText(String.valueOf(u.getId()));
            welcomeUser.setText(firstName(u.getFullName()));
        });
    }
    private String firstName(String full) {
        if (full == null || full.isBlank()) return "";
        return full.trim().split("\\s+")[0];
    }

    /* ============ Navigation ============ */
//    @FXML
//    private void BackAction() {
//        Stage stage = (Stage) BackButton.getScene().getWindow();
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource(new Navigation().Login_Fxml));
//            loader.setControllerFactory(type ->
//                    type == LoginController.class ? new LoginController(monitor) : null
//            );
//            Parent root = loader.load();
//            stage.setScene(new Scene(root));
//            stage.setResizable(false);
//            stage.show();
//        } catch (IOException e) { showError("Navigation", e); }
//    }
    @FXML
    private void BackAction() {
        Stage stage = (Stage) BackButton.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(new Navigation().Login_Fxml));
            loader.setControllerFactory(type ->
                    type == LoginController.class ? new LoginController(monitor) : null
            );
            Parent loginRoot = loader.load();

            // لفّ loginRoot ببوردر بان ومعاه البانر
            var banner = new com.example.healthflow.ui.ConnectivityBanner(monitor);
            javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
            root.setTop(banner);
            root.setCenter(loginRoot);

            stage.setScene(new Scene(root));
            stage.setTitle("HealthFlow");   // ← غيّر العنوان
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            showError("Navigation", e);
        }
    }

    /* ============ Panes ============ */
    private void showDashboardPane() {
        DashboardAnchorPane.setVisible(true);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(false);
        DoctorAnchorPane.setVisible(false);
        markNavActive(DachboardButton);
    }
    private void showDoctorPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(false);
        DoctorAnchorPane.setVisible(true);
        markNavActive(DoctorsButton);
    }
    private void showPatientsPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(true);
        AppointmentsAnchorPane.setVisible(false);
        DoctorAnchorPane.setVisible(false);
        markNavActive(PatientsButton);
    }
    private void showAppointmentPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(true);
        DoctorAnchorPane.setVisible(false);
        markNavActive(AppointmentsButton);
    }

    /* ============ Patients: table & search ============ */
    private void wirePatientTable() {
        colNationalId.setCellValueFactory(cd -> cd.getValue().nationalIdProperty());
        colName.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
        colGender.setCellValueFactory(cd -> cd.getValue().genderProperty());
        colDob.setCellValueFactory(cd -> cd.getValue().dateOfBirthProperty());
        colPhoneNumber.setCellValueFactory(cd -> cd.getValue().phoneProperty());
        colMedicalHistory.setCellValueFactory(cd -> cd.getValue().medicalHistoryProperty());

        patientTable.setItems(patientData);

        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row == null) return;
            FullNameTextField.setText(row.getFullName());
            PatientIdTextField.setText(row.getNationalId());
            PhoneTextField.setText(row.getPhone());
            medicalHistory.setText(row.getMedicalHistory());
            DateOfBirthPicker.setValue(row.getDateOfBirth());
            GenderComboBox.setValue("MALE".equals(row.getGender()) ? Gender.MALE : Gender.FEMALE);
        });
    }

    private void wireSearchPatients() {
        filtered = new FilteredList<>(patientData, p -> true);
        search.textProperty().addListener((obs, old, q) -> {
            String s = (q == null) ? "" : q.trim().toLowerCase();
            if (s.isEmpty()) filtered.setPredicate(p -> true);
            else filtered.setPredicate(p ->
                    contains(p.getFullName(), s) ||
                            contains(p.getGender(), s) ||
                            contains(p.getPhone(), s) ||
                            contains(p.getNationalId(), s) ||
                            contains(p.getMedicalHistory(), s) ||
                            (p.getDateOfBirth() != null && p.getDateOfBirth().toString().toLowerCase().contains(s))
            );
        });
        SortedList<PatientRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(patientTable.comparatorProperty());
        patientTable.setItems(sorted);
    }

    private boolean contains(String v, String q) { return v != null && v.toLowerCase().contains(q); }

    private void loadPatientsBG() {
        try {
            var list = patientService.listPatients();
            Platform.runLater(() -> {
                patientData.clear();
                for (PatientView pv : list) {
                    patientData.add(new PatientRow(
                            pv.patientId(), pv.userId(), pv.fullName(), pv.nationalId(),
                            pv.phone(), pv.dateOfBirth(), pv.gender(), pv.medicalHistory()
                    ));
                }
            });
        } catch (Exception ex) {
            Platform.runLater(() -> showError("Load Patients", ex));
        }
    }

    /* ============ Doctors: table, search, load ============ */
    private void wireDoctorTable() {
        // ربط الأعمدة
        if (colDoctor_name != null)       colDoctor_name.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
        if (colDoctor_Gender != null)     colDoctor_Gender.setCellValueFactory(cd -> cd.getValue().genderProperty());
        if (colDoctor_Phone != null)      colDoctor_Phone.setCellValueFactory(cd -> cd.getValue().phoneProperty());
        if (colDoctor_Specialty != null)  colDoctor_Specialty.setCellValueFactory(cd -> cd.getValue().specialtyProperty());
        if (colDoctor_bio != null)        colDoctor_bio.setCellValueFactory(cd -> cd.getValue().bioProperty());
        if (colDoctor_Status != null)     colDoctor_Status.setCellValueFactory(cd -> cd.getValue().statusTextProperty());
        if (colDoctor_available != null)  colDoctor_available.setCellValueFactory(cd -> cd.getValue().availableProperty());

        if (DocTable_Recption != null) DocTable_Recption.setItems(doctorData);
    }

    private void wireSearchDoctors() {
        doctorFiltered = new FilteredList<>(doctorData, d -> true);
        if (searchDoctor != null) {
            searchDoctor.textProperty().addListener((obs, old, q) -> {
                String s = (q == null) ? "" : q.trim().toLowerCase();
                if (s.isEmpty()) doctorFiltered.setPredicate(d -> true);
                else doctorFiltered.setPredicate(d ->
                        contains(d.getFullName(), s) ||
                                contains(d.getGender(), s) ||
                                contains(d.getPhone(), s) ||
                                contains(d.getSpecialty(), s) ||
                                contains(d.getBio(), s) ||
                                contains(d.getStatusText(), s)
                );
            });
        }
        if (DocTable_Recption != null) {
            SortedList<DoctorRow> sorted = new SortedList<>(doctorFiltered);
            sorted.comparatorProperty().bind(DocTable_Recption.comparatorProperty());
            DocTable_Recption.setItems(sorted);
        }
    }

    /** Load specialties into DoctorspecialtyApp and react to changes to fill avilabelDoctorApp. */
    private void setupDoctorFilters() {
        // Guard if FXML nodes are absent in this view
        if (DoctorspecialtyApp != null) {
            DoctorspecialtyApp.setPromptText("Select specialty");
            loadSpecialtiesAsync();
            DoctorspecialtyApp.valueProperty().addListener((obs, old, sp) -> {
                loadAvailableDoctorsForSpecialty(sp);
            });
        }

        if (avilabelDoctorApp != null) {
            avilabelDoctorApp.setPromptText("Available doctor");
            // Render doctor nicely in drop-down
            avilabelDoctorApp.setCellFactory(list -> new ListCell<>() {
                @Override protected void updateItem(DoctorDAO.DoctorOption item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.fullName + "  (id: " + item.doctorId+")");
                }
            });
            avilabelDoctorApp.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(DoctorDAO.DoctorOption item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.fullName);
                }
            });
        }
    }

    /** Async: fetch distinct specialties (with available doctors only) and populate DoctorspecialtyApp. */
    private void loadSpecialtiesAsync() {
        if (DoctorspecialtyApp == null) return;
        new Thread(() -> {
            try {
                // احصل على كل الأطباء المتاحين (بدون فلترة تخصص) ثم استخرج تخصصاتهم المميّزة
                var available = doctorDAO.listAvailableBySpecialty((String) null);
                Set<String> specs = new TreeSet<>(); // مرتّبة أبجديًا بدون تكرار
                for (var opt : available) {
                    if (opt != null && opt.specialty != null) {
                        specs.add(opt.specialty);
                    }
                }
                Platform.runLater(() -> DoctorspecialtyApp.setItems(FXCollections.observableArrayList(specs)));
            } catch (Exception e) {
                Platform.runLater(() -> showWarn("Doctors", "Failed to load specialties (available only)."));
            }
        }, "recp-specialties").start();
    }

    /** Async: fetch available doctors for a given specialty (null = all). */
    private void loadAvailableDoctorsForSpecialty(String specialty) {
        if (avilabelDoctorApp == null) return;
        // If no specialty selected, clear list (or you can load all available doctors)
        if (specialty == null || specialty.isBlank()) {
            avilabelDoctorApp.getItems().clear();
            avilabelDoctorApp.setValue(null);
            return;
        }
        new Thread(() -> {
            try {
                var list = doctorDAO.listAvailableBySpecialty(specialty);
                Platform.runLater(() -> {
                    avilabelDoctorApp.setItems(FXCollections.observableArrayList(list));
                    // select first by default (optional)
                    if (!list.isEmpty()) avilabelDoctorApp.getSelectionModel().select(0);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showWarn("Doctors", "Failed to load available doctors."));
            }
        }, "recp-avail-docs").start();
    }


    /** تحميل كل الدكاترة مع حالتهم */
    private void loadDoctorsBG() {
        final String sql = """
            SELECT d.id AS doctor_id,
                   u.full_name,
                   u.gender::text AS gender,
                   u.phone,
                   d.specialty,
                   COALESCE(d.bio, '') AS bio,
                   d.availability_status::text AS status
            FROM doctors d
            JOIN users u ON u.id = d.user_id
            ORDER BY u.full_name
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            ObservableList<DoctorRow> list = FXCollections.observableArrayList();
            while (rs.next()) {
                list.add(new DoctorRow(
                        rs.getLong("doctor_id"),
                        rs.getString("full_name"),
                        rs.getString("gender"),
                        rs.getString("phone"),
                        rs.getString("specialty"),
                        rs.getString("bio"),
                        rs.getString("status")
                ));
            }
            Platform.runLater(() -> {
                doctorData.setAll(list);
                // أرقام أعلى الداشبورد (اختياري)
                if (NumberOfTotalDoctors != null) NumberOfTotalDoctors.setText(String.valueOf(list.size()));
            });
        } catch (Exception ex) {
            Platform.runLater(() -> showError("Load Doctors", ex));
        }
    }

    /* ============ CRUD via Service (patients) ============ */
    private void doInsertPatient() {
        String fullName = trimOrNull(FullNameTextField.getText());
        String nid      = trimOrNull(PatientIdTextField.getText());
        Gender gender   = GenderComboBox.getValue();
        LocalDate dob   = DateOfBirthPicker.getValue();
        String phone    = trimOrNull(PhoneTextField.getText());
        String history  = trimOrNull(medicalHistory.getText());

        if (fullName == null || dob == null || gender == null) {
            showWarn("Validation", "Full name, gender and date of birth are required.");
            return;
        }
        if (phone == null) {
            showWarn("Validation", "Patient must have a phone number.");
            return;
        }

        try {
            var pv = patientService.createPatient(fullName, nid, phone, dob, gender.name(), history);
            patientData.add(new PatientRow(
                    pv.patientId(), pv.userId(), pv.fullName(), pv.nationalId(),
                    pv.phone(), pv.dateOfBirth(), pv.gender(), pv.medicalHistory()
            ));
            clearForm();
            showInfo("Insert", "Patient inserted successfully.");
        } catch (Exception ex) { showError("Insert Patient", ex); }
    }

    private void doUpdatePatient() {
        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
        if (row == null) { showWarn("Update", "Select a patient row first."); return; }

        String fullName = trimOrNull(FullNameTextField.getText());
        String nid      = trimOrNull(PatientIdTextField.getText());
        String phone    = trimOrNull(PhoneTextField.getText());
        String history  = trimOrNull(medicalHistory.getText());
        Gender gender   = GenderComboBox.getValue();
        LocalDate dob   = DateOfBirthPicker.getValue();

        if (fullName == null || dob == null || gender == null) {
            showWarn("Validation", "Full name, gender and date of birth are required.");
            return;
        }

        try {
            patientService.updatePatient(row.getUserId(), row.getPatientId(),
                    fullName, nid, phone, dob, gender.name(), history);

            row.setFullName(fullName);
            row.setNationalId(nid);
            row.setPhone(phone);
            row.setDateOfBirth(dob);
            row.setGender(gender.name());
            row.setMedicalHistory(history);
            patientTable.refresh();

            showInfo("Update", "Patient updated successfully.");
        } catch (Exception ex) { showError("Update Patient", ex); }
    }

    private void doDeletePatient() {
        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
        if (row == null) { showWarn("Delete", "Select a patient row first."); return; }
        if (!confirm("Delete", "Are you sure you want to delete this patient?")) return;

        try {
            patientService.deletePatientByUserId(row.getUserId());
            patientData.remove(row);
            clearForm();
            showInfo("Delete", "Patient deleted.");
        } catch (Exception e) { showError("Delete Patient", e); }
    }

    private void clearForm() {
        FullNameTextField.clear();
        PatientIdTextField.clear();
        PhoneTextField.clear();
        medicalHistory.clear();
        GenderComboBox.setValue(Gender.MALE);
        DateOfBirthPicker.setValue(null);
        patientTable.getSelectionModel().clearSelection();
    }

    /* ============ Helpers ============ */
    private boolean ensureOnlineOrAlert() {
        if (monitor != null && !monitor.isOnline()) {
            showWarn("Offline", "You're offline. Please reconnect and try again.");
            return false;
        }
        return true;
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String showError(String title, Exception ex) {
        if (ex instanceof SQLException sqlEx && "23514".equals(sqlEx.getSQLState())) {
            // 23514 = check_violation
            // مثال على رسالة مخصّصة
        } else {
            ex.printStackTrace();
        }
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(ex.getMessage());
        a.showAndWait();
        return ex.getMessage();
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

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    /* ============ Row models ============ */
    public static class PatientRow {
        private final LongProperty patientId = new SimpleLongProperty();
        private final LongProperty userId    = new SimpleLongProperty();
        private final StringProperty fullName = new SimpleStringProperty();
        private final StringProperty nationalId = new SimpleStringProperty();
        private final StringProperty phone = new SimpleStringProperty();
        private final ObjectProperty<LocalDate> dateOfBirth = new SimpleObjectProperty<>();
        private final StringProperty gender = new SimpleStringProperty();
        private final StringProperty medicalHistory = new SimpleStringProperty();

        public PatientRow(Long patientId, Long userId, String fullName, String nationalId,
                          String phone, LocalDate dob, String gender, String medicalHistory) {
            setPatientId(patientId); setUserId(userId); setFullName(fullName);
            setNationalId(nationalId); setPhone(phone); setDateOfBirth(dob);
            setGender(gender); setMedicalHistory(medicalHistory);
        }

        public long getPatientId() { return patientId.get(); }
        public void setPatientId(long v) { patientId.set(v); }
        public LongProperty patientIdProperty() { return patientId; }

        public long getUserId() { return userId.get(); }
        public void setUserId(long v) { userId.set(v); }
        public LongProperty userIdProperty() { return userId; }

        public String getFullName() { return fullName.get(); }
        public void setFullName(String v) { fullName.set(v); }
        public StringProperty fullNameProperty() { return fullName; }

        public String getNationalId() { return nationalId.get(); }
        public void setNationalId(String v) { nationalId.set(v); }
        public StringProperty nationalIdProperty() { return nationalId; }

        public String getPhone() { return phone.get(); }
        public void setPhone(String v) { phone.set(v); }
        public StringProperty phoneProperty() { return phone; }

        public LocalDate getDateOfBirth() { return dateOfBirth.get(); }
        public void setDateOfBirth(LocalDate v) { dateOfBirth.set(v); }
        public ObjectProperty<LocalDate> dateOfBirthProperty() { return dateOfBirth; }

        public String getGender() { return gender.get(); }
        public void setGender(String v) { gender.set(v); }
        public StringProperty genderProperty() { return gender; }

        public String getMedicalHistory() { return medicalHistory.get(); }
        public void setMedicalHistory(String v) { medicalHistory.set(v); }
        public StringProperty medicalHistoryProperty() { return medicalHistory; }
    }

    /** صفّ عرض للدكتور */
    public static class DoctorRow {
        private final LongProperty doctorId = new SimpleLongProperty();
        private final StringProperty fullName = new SimpleStringProperty();
        private final StringProperty gender = new SimpleStringProperty();
        private final StringProperty phone = new SimpleStringProperty();
        private final StringProperty specialty = new SimpleStringProperty();
        private final StringProperty bio = new SimpleStringProperty();
        private final StringProperty statusText = new SimpleStringProperty();
        private final BooleanProperty available = new SimpleBooleanProperty(false);

        public DoctorRow(long doctorId, String fullName, String gender, String phone,
                         String specialty, String bio, String statusText) {
            setDoctorId(doctorId);
            setFullName(fullName);
            setGender(gender);
            setPhone(phone);
            setSpecialty(specialty);
            setBio(bio);
            setStatusText(statusText);
            setAvailable("AVAILABLE".equalsIgnoreCase(statusText));
        }

        public long getDoctorId() { return doctorId.get(); }
        public void setDoctorId(long v) { doctorId.set(v); }
        public LongProperty doctorIdProperty() { return doctorId; }

        public String getFullName() { return fullName.get(); }
        public void setFullName(String v) { fullName.set(v); }
        public StringProperty fullNameProperty() { return fullName; }

        public String getGender() { return gender.get(); }
        public void setGender(String v) { gender.set(v); }
        public StringProperty genderProperty() { return gender; }

        public String getPhone() { return phone.get(); }
        public void setPhone(String v) { phone.set(v); }
        public StringProperty phoneProperty() { return phone; }

        public String getSpecialty() { return specialty.get(); }
        public void setSpecialty(String v) { specialty.set(v); }
        public StringProperty specialtyProperty() { return specialty; }

        public String getBio() { return bio.get(); }
        public void setBio(String v) { bio.set(v); }
        public StringProperty bioProperty() { return bio; }

        public String getStatusText() { return statusText.get(); }
        public void setStatusText(String v) { statusText.set(v); }
        public StringProperty statusTextProperty() { return statusText; }

        public boolean isAvailable() { return available.get(); }
        public void setAvailable(boolean v) { available.set(v); }
        public BooleanProperty availableProperty() { return available; }
    }
}
