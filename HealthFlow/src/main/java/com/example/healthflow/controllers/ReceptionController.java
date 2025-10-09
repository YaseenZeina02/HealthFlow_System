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
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import javafx.beans.property.ReadOnlyObjectWrapper;

import javafx.scene.control.cell.ComboBoxTableCell;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReceptionController {

    /* ============ UI ============ */
    @FXML
    private AnchorPane DashboardAnchorPane;
    @FXML
    private AnchorPane PatientAnchorPane;
    @FXML
    private AnchorPane AppointmentsAnchorPane;
    @FXML
    private AnchorPane DoctorAnchorPane;
    @FXML
    private StackPane rootPane;

    @FXML
    private Button DachboardButton;
    @FXML
    private Button PatientsButton;
    @FXML
    private Button AppointmentsButton;
    @FXML
    private Button BackButton;
    @FXML
    private Button DoctorsButton;

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

    // ===== Patients form =====
    @FXML
    private TextField FullNameTextField;
    @FXML
    private TextField PatientIdTextField;   // National Id
    @FXML
    private ComboBox<Gender> GenderComboBox;
    @FXML
    private DatePicker DateOfBirthPicker;
    @FXML
    private TextField PhoneTextField;
    @FXML
    private TextArea medicalHistory;

    @FXML
    private Button InsertButton;
    @FXML
    private Button UpdateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearBtn;

    @FXML
    private TextField search;

    @FXML
    private TableView<PatientRow> patientTable;
    @FXML
    private TableColumn<PatientRow, String> colNationalId;
    @FXML
    private TableColumn<PatientRow, String> colName;
    @FXML
    private TableColumn<PatientRow, String> colGender;
    @FXML
    private TableColumn<PatientRow, LocalDate> colDob;
    @FXML
    private TableColumn<PatientRow, String> colPhoneNumber;
    @FXML
    private TableColumn<PatientRow, String> colMedicalHistory;

    @FXML
    private Label NumberOfTotalAppointments;
    @FXML
    private Label NumberOfTotalDoctors;
    @FXML
    private Label NumberOfTotalPatients;
    @FXML
    private Label patientCompleteNum;
    @FXML
    private Label RemainingNum;
    private final ObservableList<DoctorDAO.AppointmentRow> apptData = FXCollections.observableArrayList();

    // Caches/edit choices for in-row editors
    private final ObservableList<String> specialtyChoices = FXCollections.observableArrayList();
    private final Map<String, ObservableList<DoctorDAO.DoctorOption>> doctorsBySpec = new ConcurrentHashMap<>();

    @FXML
    private Circle ActiveStatus;

    @FXML
    private TableColumn<?, ?> AppointmentIdColumn;
    @FXML
    private AnchorPane Appointments;
    @FXML
    private AnchorPane CenterAnchorPane;
    @FXML
    private AnchorPane Doctors;
    @FXML
    private AnchorPane Patients;

    @FXML
    private Label TotalAppointments;
    @FXML
    private Label TotalDoctors;

    @FXML private TableView<DoctorDAO.AppointmentRow> TableAppInDashboard;

    @FXML private TableColumn<DoctorDAO.AppointmentRow, Number>      colAppointmentID;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, Void>      colActionDash;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, LocalDate>  colAppintementDateDash;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, String>     colAppintementTimeDash;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, String>     colDoctorNameDash;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, String>     colPatientNameDash;
    @FXML
    private BarChart<String, Number> appointmentStatusChart;
    @FXML
    private Button clearSelectionDach;

    @FXML
    private TextField searchAppointmentDach;
    @FXML
    private TextField searchDoctor;

    @FXML
    private Button insertAppointments;
    @FXML
    private Button addNewRow;
    @FXML
    private Label TotalPatients;
    @FXML
    private Button BookAppointmentFromPateint;
    @FXML
    private Button updateAppointments;
    @FXML
    private ComboBox<String> DoctorspecialtyApp;           // list of specialties
    @FXML
    private ComboBox<DoctorDAO.DoctorOption> avilabelDoctorApp; // available doctors for selected specialty
    @FXML
    private Button clear_Appointments;
    @FXML
    private Button deleteAppointments;

    // In Appointment Anchorpane
//    @FXML
//    private TableView<?> TableINAppointment;
//    @FXML
//    private TableColumn<?, ?> colAppointmentIDAppointment;
//    @FXML
//    private TableColumn<?, ?> colDateAppointment;
//    @FXML
//    private TableColumn<?, ?> colDoctorNameAppointment;
//    @FXML
//    private TableColumn<?, ?> colPatientNameAppointment;
//    @FXML
//    private TableColumn<?, ?> colSpecialty;
//    @FXML
//    private TableColumn<?, ?> colStatusAppointment;
//    @FXML
//    private TableColumn<?, ?> colTimeAppointment;

    @FXML private TableView<ApptRow> TableINAppointment;
    @FXML private TableColumn<ApptRow, Number> colAppointmentIDAppointment;
    @FXML private TableColumn<ApptRow, LocalDate> colDateAppointment;
    @FXML private TableColumn<ApptRow, String> colDoctorNameAppointment;
    @FXML private TableColumn<ApptRow, String> colPatientNameAppointment;
    @FXML private TableColumn<ApptRow, String> colSpecialty;
    @FXML private TableColumn<ApptRow, String> colStatusAppointment;
    @FXML private TableColumn<ApptRow, String> colTimeAppointment;

    @FXML
    private Button deleteButtonAppointment;
    @FXML
    private Label getPatientName;
    @FXML
    private Label getPatientID;

    // ===== Doctors table (تأكّد من fx:id في FXML) =====
    @FXML
    private TableView<DoctorRow> DocTable_Recption;
    @FXML
    private TableColumn<DoctorRow, String> colDoctor_name;
    @FXML
    private TableColumn<DoctorRow, String> colDoctor_Gender;
    @FXML
    private TableColumn<DoctorRow, String> colDoctor_Phone;
    @FXML
    private TableColumn<DoctorRow, String> colDoctor_Specialty;
    @FXML
    private TableColumn<DoctorRow, String> colDoctor_bio;
    @FXML
    private TableColumn<DoctorRow, String> colDoctor_Status;
    @FXML
    private TableColumn<DoctorRow, Boolean> colDoctor_available;


    @FXML
    private DatePicker AppointmentDate;
//    -----


    @FXML
    private TextField appointmentSetTime;

    @FXML
    private TextField PatientNameForAppointment;

    @FXML
    private TextField PatientIDForAppointment;


    //    @FXML
    //    private DatePicker setAppointmentDate;
    @FXML
    private ComboBox<DoctorDAO.Slot> cmbSlots;

    // --- Auto refresh infrastructure ---
    private final ScheduledExecutorService autoRefreshExec =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ui-auto-refresh");
                t.setDaemon(true);
                return t;
            });
    private final AtomicBoolean refreshBusy = new AtomicBoolean(false);


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
    public enum Gender {MALE, FEMALE}

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

    public ReceptionController() {
        this(new ConnectivityMonitor());
    }

    /* ============ Init ============ */
    @FXML
    private void initialize() {
        // Attach CSS when the Scene becomes available (avoid NPE if scene is null during initialize)
        if (rootPane != null) {
            String receptionCss;
            var cssUrl = getClass().getResource("/com/example/healthflow/Design/ReceptionDesign.css"); // absolute path
            if (cssUrl != null) {
                receptionCss = cssUrl.toExternalForm();
            } else {
                receptionCss = null;
                System.err.println("Reception CSS not found at /com/example/healthflow/Design/ReceptionDesign.css");
            }

            if (receptionCss != null) {
                if (rootPane.getScene() != null) {
                    if (!rootPane.getScene().getStylesheets().contains(receptionCss)) {
                        rootPane.getScene().getStylesheets().add(receptionCss);
                    }
                } else {
                    rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                        if (newScene != null && !newScene.getStylesheets().contains(receptionCss)) {
                            newScene.getStylesheets().add(receptionCss);
                        }
                    });
                }
            }
        }

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
            final boolean[] firstEmissionHandled = {false};
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

        InsertButton.setOnAction(e -> {
            if (ensureOnlineOrAlert()) doInsertPatient();
        });
        UpdateButton.setOnAction(e -> {
            if (ensureOnlineOrAlert()) doUpdatePatient();
        });
        deleteButton.setOnAction(e -> {
            if (ensureOnlineOrAlert()) doDeletePatient();
        });
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
            if (getPatientID != null) getPatientID.setText(row.getNationalId());
            // Navigate to the appointment pane
            showAppointmentPane();
            if (DoctorspecialtyApp != null && DoctorspecialtyApp.getItems().isEmpty()) loadSpecialtiesAsync();
            // جرّب تركيز/إضافة سطر مسودة للمريض المختار
            addOrFocusDraftForPatient(row);
        });

        Platform.runLater(() -> {
            new Thread(() -> {
                try {
                    loadHeaderUser();
                } catch (Exception ignored) {
                }
            }, "hdr-user-load").start();
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

        // appointments wiring + load
        wireAppointmentsTables();
        wireDashboardAppointmentsSearch();
        if (clearSelectionDach != null) clearSelectionDach.setOnAction(e -> {
            if (TableAppInDashboard != null)
                TableAppInDashboard.getSelectionModel().clearSelection();
            if (appointmentStatusChart != null)
                appointmentStatusChart.getData().clear(); // يمسح البار-تشارت
            if (searchAppointmentDach != null)
                searchAppointmentDach.clear();    // يمسح البحث
        });

        // CRUD buttons
        if (insertAppointments != null) insertAppointments.setOnAction(e -> doInsertAppointment());
        if (updateAppointments != null) updateAppointments.setOnAction(e -> doUpdateAppointment());
        if (deleteAppointments != null) deleteAppointments.setOnAction(e -> doDeleteAppointment());
        if (clear_Appointments != null) clear_Appointments.setOnAction(e -> doClearAppointmentForm());
        if (addNewRow != null) addNewRow.setOnAction(e -> addBlankDraftRow());

        // initial data loads
        new Thread(this::loadAppointmentsTable, "appt-load").start();
        new Thread(this::updateAppointmentCounters, "appt-counts").start();
        startAutoRefresh();
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

        final LocalTime open = LocalTime.of(9, 0);   // بداية الدوام
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

    private static String fmt12(LocalTime t) {
        return t == null ? "" : t.format(SLOT_FMT_12H);
    }

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
        var u = Session.get();
        if (u == null) return;
        String sql = "SELECT id, full_name FROM users WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, u.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    String fullName = rs.getString("full_name");
                    Platform.runLater(() -> {
                        UsernameLabel.setText(fullName);
                        UserIdLabel.setText(Long.toString(id));
                        welcomeUser.setText(firstName(fullName));
                    });
                    return;
                }
            }
        } catch (SQLException ignored) {
        }
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

    private boolean contains(String v, String q) {
        return v != null && v.toLowerCase().contains(q);
    }

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
        if (colDoctor_name != null) colDoctor_name.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
        if (colDoctor_Gender != null) colDoctor_Gender.setCellValueFactory(cd -> cd.getValue().genderProperty());
        if (colDoctor_Phone != null) colDoctor_Phone.setCellValueFactory(cd -> cd.getValue().phoneProperty());
        if (colDoctor_Specialty != null)
            colDoctor_Specialty.setCellValueFactory(cd -> cd.getValue().specialtyProperty());
        if (colDoctor_bio != null) colDoctor_bio.setCellValueFactory(cd -> cd.getValue().bioProperty());
        if (colDoctor_Status != null) colDoctor_Status.setCellValueFactory(cd -> cd.getValue().statusTextProperty());
        if (colDoctor_available != null)
            colDoctor_available.setCellValueFactory(cd -> cd.getValue().availableProperty());

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

    /**
     * Load specialties into DoctorspecialtyApp and react to changes to fill avilabelDoctorApp.
     */
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
                @Override
                protected void updateItem(DoctorDAO.DoctorOption item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.fullName + "  (id: " + item.doctorId + ")");
                }
            });
            avilabelDoctorApp.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(DoctorDAO.DoctorOption item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.fullName);
                }
            });
        }
    }

    /**
     * Async: fetch distinct specialties (with available doctors only) and populate DoctorspecialtyApp.
     */
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

    /**
     * Async: fetch available doctors for a given specialty (null = all).
     */
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

    /** Ensure specialties cache is filled (available only). */
    private void ensureSpecialtiesLoadedAsync() {
        if (!specialtyChoices.isEmpty()) return;
        new Thread(() -> {
            try {
                var all = doctorDAO.listAvailableBySpecialty((String) null);
                Set<String> sp = new TreeSet<>();
                for (var d : all) if (d != null && d.specialty != null) sp.add(d.specialty);
                Platform.runLater(() -> specialtyChoices.setAll(sp));
            } catch (Exception ignored) {}
        }, "load-spec-cache").start();
    }

    /** Load doctors for a specialty into cache if missing. */
    private void ensureDoctorsForSpecAsync(String spec) {
        if (spec == null || spec.isBlank()) return;
        if (doctorsBySpec.containsKey(spec)) return;
        doctorsBySpec.put(spec, FXCollections.observableArrayList());
        new Thread(() -> {
            try {
                var list = doctorDAO.listAvailableBySpecialty(spec);
                Platform.runLater(() -> doctorsBySpec.get(spec).setAll(list));
            } catch (Exception ignored) {}
        }, "load-docs-" + spec).start();
    }

    /**
     * يسحب جديد الداتا ويحدث الواجهة دورياً:
     * - المواعيد والعدادات والتشارت كل 1ث
     * - المرضى/الأطباء كل 10ث
     * - السلوطات حسب الطبيب/التاريخ المختارَين
     */
    private void startAutoRefresh() {
        // تحديث سريع كل ثانية
        autoRefreshExec.scheduleAtFixedRate(() -> {
            if (!refreshBusy.compareAndSet(false, true)) return;
            try {
                // 1) جدول المواعيد الرئيسي (TableINAppointment) — مجدولة فقط
                var apptRows = doctorDAO.listScheduledAppointments();
                var mapped = javafx.collections.FXCollections.<ApptRow>observableArrayList();
                for (var r : apptRows) {
                    ApptRow ar = new ApptRow();
                    ar.setId(r.id);
                    ar.setDoctorId(r.id);      // ← انتبه: doctorId الصحيح
                    ar.setDoctorName(r.doctorName);
                    ar.setPatientName(r.patientName);
                    ar.setSpecialty(r.specialty);
                    ar.setStatus(r.status);
                    ar.setDate(r.startAt.toLocalDate());
                    ar.setTime(r.startAt.toLocalTime());
                    ar.setNew(false);
                    ar.setDirty(false);
                    mapped.add(ar);
                }
                javafx.application.Platform.runLater(() -> apptEditable.setAll(mapped));

                // 2) جدول الداشبورد (يحترم البحث إن وُجد)
                if (TableAppInDashboard != null) {
                    String q = (searchAppointmentDach != null) ? searchAppointmentDach.getText() : null;
                    var dashRows = (q == null || q.isBlank())
                            ? apptRows
                            : doctorDAO.searchScheduledAppointments(q);
                    javafx.application.Platform.runLater(() -> apptData.setAll(dashRows));
                }

                // 3) العدادّات + تشارت الحالة
                int doctors   = doctorDAO.countAvailableDoctors();
                int appts     = doctorDAO.countAppointments();
                int patients  = doctorDAO.countPatients();
                int completed = doctorDAO.countCompletedAppointments();
                int scheduled = doctorDAO.countScheduledAppointments();
                javafx.application.Platform.runLater(() -> {
                    if (NumberOfTotalDoctors != null)      NumberOfTotalDoctors.setText(String.valueOf(doctors));
                    if (NumberOfTotalAppointments != null) NumberOfTotalAppointments.setText(String.valueOf(appts));
                    if (NumberOfTotalPatients != null)     NumberOfTotalPatients.setText(String.valueOf(patients));
                    if (patientCompleteNum != null)        patientCompleteNum.setText(String.valueOf(completed));
                    if (RemainingNum != null)              RemainingNum.setText(String.valueOf(scheduled));
                    updatePatientDetailsChart();
                });

                // 4) السلوطات (لو الطبيب/التاريخ محددين)
                if (cmbSlots != null && avilabelDoctorApp != null && AppointmentDate != null
                        && avilabelDoctorApp.getValue() != null && AppointmentDate.getValue() != null) {
                    try {
                        final java.time.LocalTime open  = java.time.LocalTime.of(9, 0);
                        final java.time.LocalTime close = java.time.LocalTime.of(15, 0);
                        final int slotMin = 20;
                        var doc = avilabelDoctorApp.getValue();
                        var day = AppointmentDate.getValue();
                        var slots = doctorDAO.listFreeSlots(doc.doctorId, day, open, close, slotMin);

                        // قص الماضي لليوم الحالي + إنتهاء الدوام
                        if (day.equals(java.time.LocalDate.now())) {
                            var now = java.time.LocalDateTime.now().withSecond(0).withNano(0);
                            int mod = now.getMinute() % slotMin;
                            var cutoff = (mod == 0) ? now : now.plusMinutes(slotMin - mod);
                            slots.removeIf(s -> s.from().isBefore(cutoff));
                            if (now.toLocalTime().isAfter(close)) slots.clear();
                        }

                        javafx.application.Platform.runLater(() -> {
                            var selected = cmbSlots.getValue();
                            cmbSlots.setItems(javafx.collections.FXCollections.observableArrayList(slots));
                            // حافظ على الاختيار السابق إن بقي متاحًا
                            if (selected != null && slots.stream().anyMatch(s ->
                                    s.from().equals(selected.from()) && s.to().equals(selected.to()))) {
                                cmbSlots.getSelectionModel().select(selected);
                            }
                        });
                    } catch (Exception ignore) {}
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                refreshBusy.set(false);
            }
        }, 0, 5, java.util.concurrent.TimeUnit.SECONDS);

        // تحديث أبطأ كل 10 ثوانٍ لقوائم المرضى والأطباء
        autoRefreshExec.scheduleAtFixedRate(() -> {
            try {
                loadPatientsBG(); // هذه أصلاً تحدث على FX thread داخليًا
                loadDoctorsBG();
            } catch (Exception ignore) {}
        }, 0, 10, java.util.concurrent.TimeUnit.SECONDS);
    }
    /**
     * تحميل كل الدكاترة مع حالتهم
     */
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
        String nid = trimOrNull(PatientIdTextField.getText());
        Gender gender = GenderComboBox.getValue();
        LocalDate dob = DateOfBirthPicker.getValue();
        String phone = trimOrNull(PhoneTextField.getText());
        String history = trimOrNull(medicalHistory.getText());

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
        } catch (Exception ex) {
            showError("Insert Patient", ex);
        }
    }

    private void doUpdatePatient() {
        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            showWarn("Update", "Select a patient row first.");
            return;
        }

        String fullName = trimOrNull(FullNameTextField.getText());
        String nid = trimOrNull(PatientIdTextField.getText());
        String phone = trimOrNull(PhoneTextField.getText());
        String history = trimOrNull(medicalHistory.getText());
        Gender gender = GenderComboBox.getValue();
        LocalDate dob = DateOfBirthPicker.getValue();

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
        } catch (Exception ex) {
            showError("Update Patient", ex);
        }
    }

    private void doDeletePatient() {
        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            showWarn("Delete", "Select a patient row first.");
            return;
        }
        if (!confirm("Delete", "Are you sure you want to delete this patient?")) return;

        try {
            patientService.deletePatientByUserId(row.getUserId());
            patientData.remove(row);
            clearForm();
            showInfo("Delete", "Patient deleted.");
        } catch (Exception e) {
            showError("Delete Patient", e);
        }
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
        private final LongProperty userId = new SimpleLongProperty();
        private final StringProperty fullName = new SimpleStringProperty();
        private final StringProperty nationalId = new SimpleStringProperty();
        private final StringProperty phone = new SimpleStringProperty();
        private final ObjectProperty<LocalDate> dateOfBirth = new SimpleObjectProperty<>();
        private final StringProperty gender = new SimpleStringProperty();
        private final StringProperty medicalHistory = new SimpleStringProperty();

        public PatientRow(Long patientId, Long userId, String fullName, String nationalId,
                          String phone, LocalDate dob, String gender, String medicalHistory) {
            setPatientId(patientId);
            setUserId(userId);
            setFullName(fullName);
            setNationalId(nationalId);
            setPhone(phone);
            setDateOfBirth(dob);
            setGender(gender);
            setMedicalHistory(medicalHistory);
        }

        public long getPatientId() {
            return patientId.get();
        }

        public void setPatientId(long v) {
            patientId.set(v);
        }

        public LongProperty patientIdProperty() {
            return patientId;
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

        public String getFullName() {
            return fullName.get();
        }

        public void setFullName(String v) {
            fullName.set(v);
        }

        public StringProperty fullNameProperty() {
            return fullName;
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

        public String getPhone() {
            return phone.get();
        }

        public void setPhone(String v) {
            phone.set(v);
        }

        public StringProperty phoneProperty() {
            return phone;
        }

        public LocalDate getDateOfBirth() {
            return dateOfBirth.get();
        }

        public void setDateOfBirth(LocalDate v) {
            dateOfBirth.set(v);
        }

        public ObjectProperty<LocalDate> dateOfBirthProperty() {
            return dateOfBirth;
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

    /**
     * صفّ عرض للدكتور
     */
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

        public long getDoctorId() {
            return doctorId.get();
        }

        public void setDoctorId(long v) {
            doctorId.set(v);
        }

        public LongProperty doctorIdProperty() {
            return doctorId;
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

        public String getPhone() {
            return phone.get();
        }

        public void setPhone(String v) {
            phone.set(v);
        }

        public StringProperty phoneProperty() {
            return phone;
        }

        public String getSpecialty() {
            return specialty.get();
        }

        public void setSpecialty(String v) {
            specialty.set(v);
        }

        public StringProperty specialtyProperty() {
            return specialty;
        }

        public String getBio() {
            return bio.get();
        }

        public void setBio(String v) {
            bio.set(v);
        }

        public StringProperty bioProperty() {
            return bio;
        }

        public String getStatusText() {
            return statusText.get();
        }

        public void setStatusText(String v) {
            statusText.set(v);
        }

        public StringProperty statusTextProperty() {
            return statusText;
        }

        public boolean isAvailable() {
            return available.get();
        }

        public void setAvailable(boolean v) {
            available.set(v);
        }

        public BooleanProperty availableProperty() {
            return available;
        }
    }

    // ===== Editable appointment row used by TableINAppointment =====
    public static class ApptRow {
        private final LongProperty id = new SimpleLongProperty(0);       // 0 = not yet persisted
        private final LongProperty doctorId = new SimpleLongProperty();
        private final LongProperty patientId = new SimpleLongProperty();
        private final StringProperty doctorName = new SimpleStringProperty();
        private final StringProperty patientName = new SimpleStringProperty();
        private final StringProperty specialty = new SimpleStringProperty();
        private final StringProperty status = new SimpleStringProperty("PENDING");
        private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now());
        private final ObjectProperty<LocalTime> time = new SimpleObjectProperty<>(LocalTime.of(9,0));
        private final BooleanProperty isNew = new SimpleBooleanProperty(true);
        private final BooleanProperty dirty = new SimpleBooleanProperty(false);

        public long getId() { return id.get(); }
        public void setId(long v) { id.set(v); }
        public LongProperty idProperty() { return id; }

        public long getDoctorId() { return doctorId.get(); }
        public void setDoctorId(long v) { doctorId.set(v); dirty.set(true); }
        public LongProperty doctorIdProperty() { return doctorId; }

        public long getPatientId() { return patientId.get(); }
        public void setPatientId(long v) { patientId.set(v); dirty.set(true); }
        public LongProperty patientIdProperty() { return patientId; }

        public String getDoctorName() { return doctorName.get(); }
        public void setDoctorName(String v) { doctorName.set(v); }
        public StringProperty doctorNameProperty() { return doctorName; }

        public String getPatientName() { return patientName.get(); }
        public void setPatientName(String v) { patientName.set(v); }
        public StringProperty patientNameProperty() { return patientName; }

        public String getSpecialty() { return specialty.get(); }
        public void setSpecialty(String v) { specialty.set(v); }
        public StringProperty specialtyProperty() { return specialty; }

        public String getStatus() { return status.get(); }
        public void setStatus(String v) { status.set(v); }
        public StringProperty statusProperty() { return status; }

        public LocalDate getDate() { return date.get(); }
        public void setDate(LocalDate v) { date.set(v); dirty.set(true); }
        public ObjectProperty<LocalDate> dateProperty() { return date; }

        public LocalTime getTime() { return time.get(); }
        public void setTime(LocalTime v) { time.set(v); dirty.set(true); }
        public ObjectProperty<LocalTime> timeProperty() { return time; }

        public boolean isNew() { return isNew.get(); }
        public void setNew(boolean v) { isNew.set(v); }
        public BooleanProperty isNewProperty() { return isNew; }

        public boolean isDirty() { return dirty.get(); }
        public void setDirty(boolean v) { dirty.set(v); }
        public BooleanProperty dirtyProperty() { return dirty; }
    }

    // backing list for editable table (TableINAppointment)
    private final ObservableList<ApptRow> apptEditable = FXCollections.observableArrayList();

    private static final DateTimeFormatter TIME_12 = DateTimeFormatter.ofPattern("hh:mm a");


    /**
     * Wire both appointments tables (main & dashboard) to display AppointmentRow.
     */
    private void wireAppointmentsTables() {
        // Appointments main table
        if (TableINAppointment != null) {
            TableINAppointment.setEditable(true);

            // Use unconstrained resize policy to allow horizontal scrolling
            TableINAppointment.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

            // Set fixed preferred widths for columns (horizontal scrollbar enabled if total width exceeds table)
            colAppointmentIDAppointment.setPrefWidth(90);     // ID
            colPatientNameAppointment.setPrefWidth(220);      // Patient Name
            colSpecialty.setPrefWidth(170);                   // Specialty
            colDoctorNameAppointment.setPrefWidth(200);       // Doctor Name
            colDateAppointment.setPrefWidth(150);             // Date
            colTimeAppointment.setPrefWidth(160);             // Time
            colStatusAppointment.setPrefWidth(130);           // Status

            // Keep minWidth for usability
            colAppointmentIDAppointment.setMinWidth(60);
            colPatientNameAppointment.setMinWidth(160);
            colSpecialty.setMinWidth(140);
            colDoctorNameAppointment.setMinWidth(160);
            colDateAppointment.setMinWidth(120);
            colTimeAppointment.setMinWidth(130);
            colStatusAppointment.setMinWidth(100);

            colAppointmentIDAppointment.setCellValueFactory(cd -> cd.getValue().idProperty());
            colDateAppointment.setCellValueFactory(cd -> cd.getValue().dateProperty());
            colTimeAppointment.setCellValueFactory(cd -> new SimpleStringProperty(fmt12(cd.getValue().getTime())));
            colDoctorNameAppointment.setCellValueFactory(cd -> cd.getValue().doctorNameProperty());
            colPatientNameAppointment.setCellValueFactory(cd -> cd.getValue().patientNameProperty());
            colSpecialty.setCellValueFactory(cd -> cd.getValue().specialtyProperty());
            colStatusAppointment.setCellValueFactory(cd -> cd.getValue().statusProperty());

            // --- Specialty editor: ComboBox in-row ---
            ensureSpecialtiesLoadedAsync();
            colSpecialty.setCellFactory(col -> new TableCell<>() {
                private final ComboBox<String> cb = new ComboBox<>(specialtyChoices);
                {
                    cb.getStyleClass().add("box");                 // ← مهم
                    cb.setMaxWidth(Double.MAX_VALUE);
                    cb.setPromptText("Specialty");
                    cb.setOnAction(e -> {
                        ApptRow r = getTableView().getItems().get(getIndex());
                        if (r == null) return;
                        String sp = cb.getValue();
                        r.setSpecialty(sp);
                        r.setDoctorId(0);
                        r.setDoctorName(null);
                        r.setTime(null);
                        ensureDoctorsForSpecAsync(sp);
                        getTableView().refresh();
                    });
                }
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); setText(null); }
                    else { cb.setValue(item); setGraphic(cb); setText(null); }
                }
            });
//            colDoctorNameAppointment.setCellFactory(col -> new TableCell<>() {
//                private final ComboBox<DoctorDAO.DoctorOption> cb = new ComboBox<>();
//                {
//                    cb.getStyleClass().add("box");                 // ← مهم
//                    cb.setMaxWidth(Double.MAX_VALUE);
//                    cb.setConverter(new StringConverter<>() {
//                        @Override public String toString(DoctorDAO.DoctorOption d) { return d == null ? "" : d.fullName; }
//                        @Override public DoctorDAO.DoctorOption fromString(String s) { return null; }
//                    });
//                    cb.setOnShowing(e -> {
//                        ApptRow r = getTableView().getItems().get(getIndex());
//                        if (r == null) return;
//                        ensureDoctorsForSpecAsync(r.getSpecialty());
//                        var list = doctorsBySpec.getOrDefault(r.getSpecialty(), FXCollections.observableArrayList());
//                        cb.setItems(list);
//                    });
//                    cb.setOnAction(e -> {
//                        ApptRow r = getTableView().getItems().get(getIndex());
//                        var d = cb.getValue();
//                        if (r == null) return;
//                        if (d != null) { r.setDoctorId(d.doctorId); r.setDoctorName(d.fullName); }
//                        else { r.setDoctorId(0); r.setDoctorName(null); }
//                        r.setTime(null);
//                        getTableView().refresh();
//                    });
//                }
//                @Override protected void updateItem(String item, boolean empty) {
//                    super.updateItem(item, empty);
//                    if (empty) { setGraphic(null); setText(null); }
//                    else {
//                        ApptRow r = getTableView().getItems().get(getIndex());
//                        if (r != null && r.getDoctorName() != null) {
//                            var list = doctorsBySpec.getOrDefault(r.getSpecialty(), FXCollections.observableArrayList());
//                            cb.setItems(list);
//                            cb.getSelectionModel().select(
//                                    list.stream().filter(o -> o.doctorId == r.getDoctorId()).findFirst().orElse(null)
//                            );
//                        }
//                        setGraphic(cb); setText(null);
//                    }
//                }
//            });

            colDoctorNameAppointment.setCellFactory(col -> new TableCell<ApptRow, String>() {
                private final ComboBox<DoctorDAO.DoctorOption> cb = new ComboBox<>();
                {
                    cb.getStyleClass().add("box");
                    cb.setMaxWidth(Double.MAX_VALUE);
                    cb.setVisibleRowCount(10);
                    cb.setConverter(new StringConverter<>() {
                        @Override public String toString(DoctorDAO.DoctorOption d){ return d==null? "" : d.fullName; }
                        @Override public DoctorDAO.DoctorOption fromString(String s){ return null; }
                    });
                    // لو أول كليك والقائمة لسه فاضية: حمّل ثم افتح مباشرة
                    cb.setOnShowing(e -> {
                        ApptRow r = getTableView().getItems().get(getIndex());
                        if (r == null) return;
                        ensureDoctorsForSpecAsync(r.getSpecialty());
                        var list = doctorsBySpec.getOrDefault(r.getSpecialty(), FXCollections.observableArrayList());
                        cb.setItems(list);
                    });

                    cb.setOnMousePressed(e -> {
                        if (cb.getItems().isEmpty()) {
                            ApptRow r = getRowSafely();
                            if (r != null) { preloadDoctors(r); Platform.runLater(cb::show); }
                        }
                    });
                    cb.setOnAction(e -> {
                        ApptRow r = getRowSafely();
                        var d = cb.getValue();
                        if (r == null) return;
                        if (d != null) { r.setDoctorId(d.doctorId); r.setDoctorName(d.fullName); }
                        else           { r.setDoctorId(0);          r.setDoctorName(null);      }
                        r.setTime(null); // إعادة اختيار الوقت
                        getTableView().refresh();
                    });

                }
                private ApptRow getRowSafely() {
                    int i = getIndex();
                    return (i < 0 || i >= getTableView().getItems().size()) ? null : getTableView().getItems().get(i);
                }
                private void preloadDoctors(ApptRow r) {
                    String spec = r.getSpecialty();
                    if (spec == null || spec.isBlank()) { cb.getItems().clear(); return; }
                    ensureDoctorsForSpecAsync(spec);
                    cb.setItems(doctorsBySpec.getOrDefault(spec, FXCollections.observableArrayList()));
                }
                @Override protected void updateItem(String itm, boolean empty) {
                    super.updateItem(itm, empty);
                    if (empty) { setGraphic(null); setText(null); return; }
                    ApptRow r = getRowSafely();
                    preloadDoctors(r); // **تجهيز مسبق**
                    if (r != null && r.getDoctorId() != 0) {
                        cb.getSelectionModel().select(
                                cb.getItems().stream().filter(o -> o.doctorId == r.getDoctorId()).findFirst().orElse(null)
                        );
                    } else cb.getSelectionModel().clearSelection();
                    setGraphic(cb); setText(null);
                }
            });

//            colTimeAppointment.setCellFactory(col -> new TableCell<>() {
//                private final ComboBox<DoctorDAO.Slot> cb = new ComboBox<>();
//                {
//                    cb.getStyleClass().add("box");
//                    cb.setMaxWidth(Double.MAX_VALUE);
//                    cb.setConverter(new StringConverter<>() {
//                        @Override public String toString(DoctorDAO.Slot s) {
//                            if (s == null) return "";
//                            return s.from().toLocalTime().format(SLOT_FMT_12H) + " \u2192 " +
//                                    s.to().toLocalTime().format(SLOT_FMT_12H);
//                        }
//                        @Override public DoctorDAO.Slot fromString(String s) { return null; }
//                    });
//                    cb.setOnShowing(e -> {
//                        ApptRow r = getTableView().getItems().get(getIndex());
//                        if (r == null || r.getDoctorId() == 0 || r.getDate() == null) {
//                            cb.getItems().clear();
//                            return;
//                        }
//                        new Thread(() -> {
//                            try {
//                                final LocalTime open  = LocalTime.of(9, 0),
//                                        close = LocalTime.of(15, 0); // ← العيادة تنتهي 3PM
//                                final int slotMinutes = 20;
//
//                                var slots = doctorDAO.listFreeSlots(r.getDoctorId(), r.getDate(), open, close, slotMinutes);
//
//                                if (r.getDate().equals(LocalDate.now())) {
//                                    LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
//                                    int mod = now.getMinute() % slotMinutes;
//                                    LocalDateTime cutoff = (mod == 0) ? now : now.plusMinutes(slotMinutes - mod);
//                                    slots.removeIf(s -> s.from().isBefore(cutoff));
//                                }
//
//                                Platform.runLater(() -> {
//                                    if (slots.isEmpty()) {
//                                        cb.getItems().clear();
//                                        cb.setPromptText("Out of working hours");
//                                        cb.setDisable(true);
//                                    } else {
//                                        cb.setDisable(false);
//                                        cb.setItems(FXCollections.observableArrayList(slots));
//                                    }
//                                });
//                            } catch (Exception ex) {
//                                Platform.runLater(() -> {
//                                    cb.getItems().clear();
//                                    cb.setPromptText("Out of working hours");
//                                    cb.setDisable(true);
//                                });
//                            }
//                        }, "row-slots").start();
//                    });
//                    cb.setOnAction(e -> {
//                        ApptRow r = getTableView().getItems().get(getIndex());
//                        var s = cb.getValue();
//                        if (r != null && s != null) r.setTime(s.from().toLocalTime());
//                        getTableView().refresh();
//                    });
//                }
//                @Override protected void updateItem(String item, boolean empty) {
//                    super.updateItem(item, empty);
//                    if (empty) { setGraphic(null); setText(null); }
//                    else {
//                        ApptRow r = getTableView().getItems().get(getIndex());
//                        if (cb.isDisable()) {
//                            cb.setPromptText("Out of working hours");
//                        } else {
//                            cb.setPromptText(r != null && r.getTime() != null ? fmt12(r.getTime()) : "Select time");
//                        }
//                        setGraphic(cb); setText(null);
//                    }
//                }
//            });
            colTimeAppointment.setCellFactory(col -> new TableCell<ApptRow, String>() {
                private final ComboBox<DoctorDAO.Slot> cb = new ComboBox<>();
                {
                    cb.getStyleClass().add("box");
                    cb.setMaxWidth(Double.MAX_VALUE);
                    cb.setVisibleRowCount(12);
                    cb.setConverter(new StringConverter<>() {
                        @Override public String toString(DoctorDAO.Slot s){
                            if (s==null) return "";
                            return s.from().toLocalTime().format(SLOT_FMT_12H) + " \u2192 " +
                                    s.to().toLocalTime().format(SLOT_FMT_12H);
                        }
                        @Override public DoctorDAO.Slot fromString(String s){ return null; }
                    });
                    cb.setOnMousePressed(e -> {
                        if (cb.getItems().isEmpty()) {
                            ApptRow r = getRowSafely();
                            if (r != null) { loadSlots(r); Platform.runLater(cb::show); }
                        }
                    });
                    cb.setOnAction(e -> {
                        ApptRow r = getRowSafely();
                        var s = cb.getValue();
                        if (r != null && s != null) r.setTime(s.from().toLocalTime());
                        getTableView().refresh();
                    });
                }
                private ApptRow getRowSafely() {
                    int i = getIndex();
                    return (i < 0 || i >= getTableView().getItems().size()) ? null : getTableView().getItems().get(i);
                }
                private void loadSlots(ApptRow r) {
                    cb.setDisable(false); cb.setPromptText("Loading..."); cb.getItems().clear();
                    if (r.getDoctorId() == 0 || r.getDate() == null) {
                        cb.setPromptText("Select doctor/date"); cb.setDisable(true); return;
                    }
                    new Thread(() -> {
                        try {
                            final LocalTime open = LocalTime.of(9,0), close = LocalTime.of(15,0);
                            final int slotMin = 20;
                            var slots = doctorDAO.listFreeSlots(r.getDoctorId(), r.getDate(), open, close, slotMin);
                            if (r.getDate().equals(LocalDate.now())) {
                                LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
                                int mod = now.getMinute() % slotMin;
                                LocalDateTime cutoff = (mod==0) ? now : now.plusMinutes(slotMin - mod);
                                slots.removeIf(s -> s.from().isBefore(cutoff));
                                if (now.toLocalTime().isAfter(close)) slots.clear();
                            }
                            Platform.runLater(() -> {
                                if (slots.isEmpty()) {
                                    cb.getItems().clear();
                                    cb.setPromptText("Out of working hours");
                                    cb.setDisable(true);
                                } else {
                                    cb.setDisable(false);
                                    cb.setItems(FXCollections.observableArrayList(slots));
                                    cb.setPromptText("Select time");
                                }
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> {
                                cb.getItems().clear();
                                cb.setPromptText("Out of working hours");
                                cb.setDisable(true);
                            });
                        }
                    }, "slots-preload").start();
                }
                @Override protected void updateItem(String itm, boolean empty) {
                    super.updateItem(itm, empty);
                    if (empty) { setGraphic(null); setText(null); return; }
                    ApptRow r = getRowSafely();
                    loadSlots(r); // **تجهيز مسبق**
                    cb.setPromptText(r != null && r.getTime()!=null ? fmt12(r.getTime()) : "Select time");
                    setGraphic(cb); setText(null);
                }
            });

//            colTimeAppointment.setCellFactory(col -> new TableCell<>() {
//                private final ComboBox<DoctorDAO.Slot> cb = new ComboBox<>();
//                {
//                    cb.getStyleClass().add("box");                 // ← مهم
//                    cb.setMaxWidth(Double.MAX_VALUE);
//                    cb.setConverter(new StringConverter<>() {
//                        @Override public String toString(DoctorDAO.Slot s) {
//                            if (s == null) return "";
//                            return s.from().toLocalTime().format(SLOT_FMT_12H) + " \u2192 " + s.to().toLocalTime().format(SLOT_FMT_12H);
//                        }
//                        @Override public DoctorDAO.Slot fromString(String s) { return null; }
//                    });
//                    cb.setOnShowing(e -> {
//                        ApptRow r = getTableView().getItems().get(getIndex());
//                        if (r == null || r.getDoctorId() == 0 || r.getDate() == null) { cb.getItems().clear(); return; }
//                        new Thread(() -> {
//                            try {
//                                final LocalTime open = LocalTime.of(9,0),
//                                               close = LocalTime.of(17,0);
//                                final int slotMinutes = 20;
//                                var slots = doctorDAO.listFreeSlots(r.getDoctorId(), r.getDate(), open, close, slotMinutes);
//                                if (r.getDate().equals(LocalDate.now())) {
//                                    LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
//                                    int mod = now.getMinute() % slotMinutes;
//                                    LocalDateTime cutoff = (mod == 0) ? now : now.plusMinutes(slotMinutes - mod);
//                                    slots.removeIf(s -> s.from().isBefore(cutoff));
//                                }
//                                Platform.runLater(() -> cb.setItems(FXCollections.observableArrayList(slots)));
//                            } catch (Exception ex) {
//                                Platform.runLater(cb.getItems()::clear);
//                            }
//                        }, "row-slots").start();
//                    });
//                    cb.setOnAction(e -> {
//                        ApptRow r = getTableView().getItems().get(getIndex());
//                        var s = cb.getValue();
//                        if (r != null && s != null) r.setTime(s.from().toLocalTime());
//                        getTableView().refresh();
//                    });
//                }
//                @Override protected void updateItem(String item, boolean empty) {
//                    super.updateItem(item, empty);
//                    if (empty) { setGraphic(null); setText(null); }
//                    else {
//                        ApptRow r = getTableView().getItems().get(getIndex());
//                        cb.setPromptText(r != null && r.getTime() != null ? fmt12(r.getTime()) : "Select time");
//                        setGraphic(cb); setText(null);
//                    }
//                }
//            });

            // --- Date editable via DatePicker ---
            colDateAppointment.setCellFactory(col -> new TableCell<ApptRow, LocalDate>() {
                private final DatePicker picker = new DatePicker();
                {
                    picker.getStyleClass().add("box");             // ← مهم
                    picker.setOnAction(e -> {
                        ApptRow r = getTableView().getItems().get(getIndex());
                        if (r != null) { r.setDate(picker.getValue()); r.setTime(null); }
                    });
                }
                @Override protected void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); setText(null); }
                    else { picker.setValue(item); setGraphic(picker); setText(null); }
                }
            });

            // When date changes, clear chosen time (must re-pick a valid slot)
            colDateAppointment.setOnEditCommit(ev -> {
                ApptRow r = ev.getRowValue();
                if (r != null) { r.setDate(ev.getNewValue()); r.setTime(null); }
                TableINAppointment.refresh();
            });

            TableINAppointment.setItems(apptEditable);
        }

        // (Removed duplicate/override block for TableINAppointment columns)


        // Dashboard table
        if (TableAppInDashboard != null) {
//            if (colAppointmentID != null) {
                colAppointmentID.setCellValueFactory(cd ->
                        new ReadOnlyObjectWrapper<>(cd.getValue().id));
//            }
            colAppintementDateDash.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().startAt.toLocalDate()));
            colAppintementTimeDash.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().startAt.toLocalTime().format(SLOT_FMT_12H)));
            colDoctorNameDash.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().doctorName));
            colPatientNameDash.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().patientName));

            if (colActionDash != null) {
                colActionDash.setCellFactory(col -> new TableCell<DoctorDAO.AppointmentRow, Void>() {
                    final Button btn = new Button("Complete");
                    {
                        btn.getStyleClass().setAll("button", "btn-complete");                        btn.setFocusTraversable(false);
                        btn.setOnAction(e -> {
                            DoctorDAO.AppointmentRow row =
                                    getTableView().getItems().get(getIndex());
                            if (row == null) return;

                            if (!ReceptionController.this.confirm("Complete Appointment",
                                    "Mark this appointment as completed?")) return;

                            new Thread(() -> {
                                try {
                                    // 1) حدّث الحالة في الداتابيز
                                    doctorDAO.markAppointmentCompleted(row.id);

                                    new Thread(ReceptionController.this::updateAppointmentCounters, "appt-counts").start();
                                    Platform.runLater(ReceptionController.this::updatePatientDetailsChart);

                                    // 2) شيل السطر من الجدول وحدّث العدّادات والرسم
                                    Platform.runLater(() -> {
                                        apptData.remove(row);
                                        ReceptionController.this.updateAppointmentCounters();
                                        if (appointmentStatusChart != null) appointmentStatusChart.getData().clear();
                                    });
                                } catch (Exception ex) {
                                    Platform.runLater(() -> ReceptionController.this.showError("Complete Appointment", ex));
                                }
                            }, "appt-complete").start();
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : btn);
                    }
                });
            }

            TableAppInDashboard.setItems(apptData);
            TableAppInDashboard.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> ReceptionController.this.updatePatientDetailsChart());
        }
    }

//        if (patientDetails == null) return;
//        patientDetails.getData().clear();
//        var series = new javafx.scene.chart.XYChart.Series<String, Number>();
//        series.setName("Selected");
//        // Example: show 1 bar for specialty and 1 for status length (purely illustrative)
//        series.getData().add(new javafx.scene.chart.XYChart.Data<>(row.specialty, 1));
//        series.getData().add(new javafx.scene.chart.XYChart.Data<>(row.status, 1));
//        //noinspection unchecked
//        ((BarChart<String, Number>) (BarChart<?, ?>) patientDetails).getData().add(series);
//    }

    private void updatePatientDetailsChart() {
        if (appointmentStatusChart == null) return;

        Platform.runLater(() -> {
            appointmentStatusChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Appointments");

            try {
                int completed = doctorDAO.countCompletedAppointments();
                int scheduled = doctorDAO.countScheduledAppointments();

                series.getData().add(new XYChart.Data<>("Completed", completed));
                series.getData().add(new XYChart.Data<>("Scheduled", scheduled));

            } catch (SQLException e) {
                showError("Chart Load", e);
            }

            appointmentStatusChart.getData().setAll(series);
        });
    }



    /**
     * إذا كان للمريض المختار صف مسودة في جدول TableINAppointment فحدده،
     * وإلا أضِف صفًا جديدًا مع قيم افتراضية قابلة للتعديل.
     */
    private void addOrFocusDraftForPatient(PatientRow p) {
        if (p == null || TableINAppointment == null) return;

        // 1) ابحث عن صف مسودة موجود لنفس المريض
        for (ApptRow r : apptEditable) {
            if (r.getPatientId() == p.getPatientId() && (r.isNew() || r.getId() == 0)) {
                TableINAppointment.getSelectionModel().select(r);
                TableINAppointment.scrollTo(r);
                return;
            }
        }

        // 2) أضِف صف مسودة جديد
        ApptRow draft = new ApptRow();
        draft.setNew(true);
        draft.setPatientId(p.getPatientId());
        draft.setPatientName(p.getFullName());
        draft.setStatus("PENDING");
        // عيّن تاريخ اليوم أو تاريخ الـ DatePicker إن وُجد
        LocalDate d = (AppointmentDate != null && AppointmentDate.getValue() != null)
                ? AppointmentDate.getValue() : LocalDate.now();
        draft.setDate(d);
        draft.setTime(LocalTime.of(9, 0));
        // إن كان هناك طبيب محدد في الكمبو، استخدمه كسياق افتراضي
        if (avilabelDoctorApp != null && avilabelDoctorApp.getValue() != null) {
            var doc = avilabelDoctorApp.getValue();
            draft.setDoctorId(doc.doctorId);
            draft.setDoctorName(doc.fullName);
            draft.setSpecialty(doc.specialty);
        }
        apptEditable.add(0, draft);
        TableINAppointment.getSelectionModel().select(draft);
        TableINAppointment.scrollTo(draft);
    }

    /** أضف صفًا فارغًا (مسودة) يدويًا من زر addNewRow */
    private void addBlankDraftRow() {
        if (TableINAppointment == null) return;
        ApptRow draft = new ApptRow();
        draft.setNew(true);
        draft.setStatus("PENDING");
        draft.setDate((AppointmentDate != null && AppointmentDate.getValue() != null)
                ? AppointmentDate.getValue() : LocalDate.now());
        draft.setTime(LocalTime.of(9, 0));
        // حاول تعبئة المريض من الحقول/الليبلز إن كانت موجودة
        try {
            String nid = (getPatientID != null) ? getPatientID.getText() : null;
            if (nid != null && !nid.isBlank()) {
                Long pid = doctorDAO.findPatientIdByNationalId(nid.trim());
                if (pid != null) {
                    draft.setPatientId(pid);
                    if (getPatientName != null) draft.setPatientName(getPatientName.getText());
                }
            }
        } catch (Exception ignored) { }
        // الطبيب المختار (إن وُجد)
        if (avilabelDoctorApp != null && avilabelDoctorApp.getValue() != null) {
            var doc = avilabelDoctorApp.getValue();
            draft.setDoctorId(doc.doctorId);
            draft.setDoctorName(doc.fullName);
            draft.setSpecialty(doc.specialty);
        }
        apptEditable.add(0, draft);
        TableINAppointment.getSelectionModel().select(draft);
        TableINAppointment.scrollTo(draft);
    }

    private void loadAppointmentsTable() {
        try {
            var rows = doctorDAO.listScheduledAppointments(); // فقط الـ SCHEDULED
            ObservableList<ApptRow> mapped = FXCollections.observableArrayList();
            for (var r : rows) {
                ApptRow ar = new ApptRow();
                ar.setId(r.id);
                ar.setDoctorId(r.id);
                ar.setDoctorName(r.doctorName);
                ar.setPatientName(r.patientName);
                ar.setSpecialty(r.specialty);
                ar.setStatus(r.status);
                ar.setDate(r.startAt.toLocalDate());
                ar.setTime(r.startAt.toLocalTime());
                ar.setNew(false);
                ar.setDirty(false);
                mapped.add(ar);
            }
            Platform.runLater(() -> apptEditable.setAll(mapped));
        } catch (Exception e) {
            showError("Load Appointments", e);
        }
    }

    private void updateAppointmentCounters() {
        try {
            int doctors = doctorDAO.countAvailableDoctors();
            int appts = doctorDAO.countAppointments();
            int patients = doctorDAO.countPatients();
            int completed = doctorDAO.countCompletedAppointments();
            int scheduled = doctorDAO.countScheduledAppointments();

            Platform.runLater(() -> {
                if (NumberOfTotalDoctors != null) NumberOfTotalDoctors.setText(String.valueOf(doctors));
                if (NumberOfTotalAppointments != null) NumberOfTotalAppointments.setText(String.valueOf(appts));
                if (NumberOfTotalPatients != null) NumberOfTotalPatients.setText(String.valueOf(patients));
                if (patientCompleteNum != null) patientCompleteNum.setText(String.valueOf(completed));
                if (RemainingNum != null) RemainingNum.setText(String.valueOf(scheduled));
            });
        } catch (Exception e) {
            showError("Counters", e);
        }
    }

//    private void wireDashboardAppointmentsSearch() {
//        if (searchAppointmentDach == null || TableAppInDashboard == null) return;
//
//        // filtered/sorted view
//        FilteredList<DoctorDAO.AppointmentRow> filteredAppts = new FilteredList<>(apptData, a -> true);
//        searchAppointmentDach.textProperty().addListener((obs, old, q) -> {
//            String s = (q == null) ? "" : q.trim().toLowerCase();
//            if (s.isEmpty()) {
//                filteredAppts.setPredicate(a -> true);
//            } else {
//                filteredAppts.setPredicate(a ->
//                        a.doctorName.toLowerCase().contains(s) ||
//                                a.patientName.toLowerCase().contains(s) ||
//                                a.specialty.toLowerCase().contains(s) ||
//                                a.status.toLowerCase().contains(s) ||
//                                a.startAt.toLocalDate().toString().contains(s)
//                );
//            }
//        });
//        SortedList<DoctorDAO.AppointmentRow> sorted = new SortedList<>(filteredAppts);
//        sorted.comparatorProperty().bind(TableAppInDashboard.comparatorProperty());
//        TableAppInDashboard.setItems(sorted);
//    }
    // هذه بعرض المواعيد المجدولة فقط
    private void wireDashboardAppointmentsSearch() {
        if (searchAppointmentDach == null || TableAppInDashboard == null) return;

        searchAppointmentDach.textProperty().addListener((obs, old, q) -> {
            new Thread(() -> {
                try {
                    var rows = (q == null || q.isBlank())
                            ? doctorDAO.listScheduledAppointments()
                            : doctorDAO.searchScheduledAppointments(q);
                    Platform.runLater(() -> apptData.setAll(rows));
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Search Appointments", e));
                }
            }, "search-appts").start();
        });
    }

    private void doInsertAppointment() {
        try {
            var doc = (avilabelDoctorApp != null) ? avilabelDoctorApp.getValue() : null;
            if (doc == null) {
                showWarn("Validation", "Select a doctor.");
                return;
            }

            String nid = (getPatientID != null) ? getPatientID.getText() : null;
            if (nid == null || nid.isBlank()) {
                showWarn("Validation", "Select a patient first.");
                return;
            }

            var patientId = doctorDAO.findPatientIdByNationalId(nid);
            if (patientId == null) {
                showWarn("Validation", "Patient not found.");
                return;
            }

            var day = (AppointmentDate != null) ? AppointmentDate.getValue() : null;
            var slot = (cmbSlots != null) ? cmbSlots.getValue() : null;
            if (day == null || slot == null) {
                showWarn("Validation", "Select date and slot.");
                return;
            }

            var startAt = slot.from().atOffset(java.time.ZoneOffset.UTC);
            Long createdBy = null;
            if (UserIdLabel != null && !UserIdLabel.getText().isBlank()) {
                try {
                    createdBy = Long.parseLong(UserIdLabel.getText().trim());
                } catch (Exception ignored) {
                }
            }

            doctorDAO.insertAppointment(doc.doctorId, patientId, startAt, 20, createdBy);
            showInfo("Insert", "Appointment inserted successfully!");
            loadAppointmentsTable();
            updateAppointmentCounters();
            refreshSlots(); // حتى يختفي الوقت الذي حُجز للتو
        } catch (Exception e) {
            showError("Insert Appointment", e);
        }
    }

    private void doUpdateAppointment() {
        if (TableINAppointment == null) {
            showWarn("Update", "Appointments table not available.");
            return;
        }
        //noinspection unchecked
        var row = (DoctorDAO.AppointmentRow) ((TableView<?>) TableINAppointment).getSelectionModel().getSelectedItem();
        if (row == null) {
            showWarn("Update", "Select an appointment.");
            return;
        }
        var slot = (cmbSlots != null) ? cmbSlots.getValue() : null;
        if (slot == null) {
            showWarn("Update", "Select new slot.");
            return;
        }

        try {
            doctorDAO.updateAppointmentTime(row.id, slot.from().atOffset(java.time.ZoneOffset.UTC), 20);
            showInfo("Update", "Appointment updated.");
            loadAppointmentsTable();
            refreshSlots();
        } catch (Exception e) {
            showError("Update Appointment", e);
        }
    }

    private void doDeleteAppointment() {
        if (TableINAppointment == null) {
            showWarn("Delete", "Appointments table not available.");
            return;
        }
        //noinspection unchecked
        var row = (DoctorDAO.AppointmentRow) ((TableView<?>) TableINAppointment).getSelectionModel().getSelectedItem();
        if (row == null) {
            showWarn("Delete", "Select an appointment.");
            return;
        }

        try {
            doctorDAO.deleteAppointment(row.id);
            showInfo("Delete", "Appointment deleted.");
            loadAppointmentsTable();
            updateAppointmentCounters();
            refreshSlots();
        } catch (Exception e) {
            showError("Delete Appointment", e);
        }
    }

    private void doClearAppointmentForm() {
        if (PatientNameForAppointment != null) PatientNameForAppointment.clear();
        if (PatientIDForAppointment != null) PatientIDForAppointment.clear();
        if (AppointmentDate != null) AppointmentDate.setValue(LocalDate.now());
        if (cmbSlots != null) cmbSlots.getItems().clear();
        if (DoctorspecialtyApp != null) DoctorspecialtyApp.getSelectionModel().clearSelection();
        if (avilabelDoctorApp != null) avilabelDoctorApp.getItems().clear();
    }

    // استدعِ هذه مثلاً في onCloseRequest أو أثناء خروج المستخدم
    public void shutdown() {
        if (autoRefreshExec != null && !autoRefreshExec.isShutdown()) {
            autoRefreshExec.shutdownNow();
        }
    }
}