package com.example.healthflow.controllers;

import com.example.healthflow.dao.DoctorDAO;
import com.example.healthflow.db.Database;
import com.example.healthflow.model.Role;
import com.example.healthflow.model.User;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.service.AuthService.Session;
import com.example.healthflow.service.DoctorDashboardService;
import com.example.healthflow.service.DoctorDashboardService.Appt;
import com.example.healthflow.service.DoctorDashboardService.Stats;
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
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DoctorController {

    /* ====== Cards / header / nav ====== */
    @FXML private AnchorPane Appointments;
    @FXML private Label TotalAppointments;

    @FXML private AnchorPane Appointments21;
    @FXML private Label TotalAppointments21;

    @FXML private AnchorPane Appointments2;
    @FXML private Label TotalAppointments2;

    @FXML private AnchorPane Appointments22;
    @FXML private Label TotalAppointments22;

    @FXML private Button BackButton;
    @FXML private Button DachboardButton;
    @FXML private Button PatientsButton;

    @FXML private AnchorPane CenterAnchorPane;
    @FXML private AnchorPane DashboardAnchorPane;
    @FXML private AnchorPane PatientAnchorPane;

    @FXML private Label DateOfDay;
    @FXML private Label time;
    @FXML private Label welcomeUser;
    @FXML private Label UsernameLabel;
    @FXML private Label UserIdLabel;

    @FXML private Circle ActiveStatus;

    /* ====== Dashboard table (appointments) ====== */
    @FXML private TableView<AppointmentRow> AppointmentsTable;
    @FXML private TableColumn<AppointmentRow, String>    colPatientName;
    @FXML private TableColumn<AppointmentRow, LocalDate> colDate;
    @FXML private TableColumn<AppointmentRow, String>    colTime;
    @FXML private TableColumn<AppointmentRow, String>    colStatus;
    @FXML private TableColumn<AppointmentRow, AppointmentRow> colAction;

    /* ====== Patients tab ====== */
    @FXML private TableView<PatientRow> patientTable;
    @FXML private TableColumn<PatientRow, String>  colNationalId;
    @FXML private TableColumn<PatientRow, String>  colName;
    @FXML private TableColumn<PatientRow, String>  colGender;
    @FXML private TableColumn<PatientRow, Integer> colDob; // age
    @FXML private TableColumn<PatientRow, String>  colMedicalHistory;
    @FXML private TableColumn<PatientRow, PatientRow> colAction2;
    @FXML private TextField search;      // patients search (future)
    @FXML private TextField searchLabel; // appointments search (future)
    private FilteredList<PatientRow> filtered;
    private SortedList<PatientRow> sorted;

    @FXML private VBox rootPane;

    /* ====== Nav highlight ====== */
    private static final String ACTIVE_CLASS = "current";
    private void markNavActive(Button active) {
        Button[] all = {DachboardButton, PatientsButton};
        for (Button b : all) {
            b.getStyleClass().remove(ACTIVE_CLASS);
            if (!b.getStyleClass().contains("nav-btn")) b.getStyleClass().add("nav-btn");
        }
        if (active != null && !active.getStyleClass().contains(ACTIVE_CLASS)) {
            active.getStyleClass().add(ACTIVE_CLASS);
        }
    }

    /* ====== Services / state ====== */
    private final ConnectivityMonitor monitor;
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final DoctorDashboardService svc = new DoctorDashboardService();

    private final ObservableList<AppointmentRow> apptData = FXCollections.observableArrayList();
    private final ObservableList<PatientRow> patientData = FXCollections.observableArrayList();

    public DoctorController(ConnectivityMonitor monitor) { this.monitor = monitor; }
    public DoctorController() { this(new ConnectivityMonitor()); }

    /* ================= INIT ================= */
    @FXML
    private void initialize() {
        monitor.start();
        showDashboardPane();

        if (rootPane != null) {
            ConnectivityBanner banner = new ConnectivityBanner(monitor);
            rootPane.getChildren().add(0, banner);
        }

        startClock();

        DachboardButton.setOnAction(e -> showDashboardPane());
        PatientsButton.setOnAction(e -> showPatientsPane());
        BackButton.setOnAction(e -> goBackToLogin());

        try { OnlineBindings.disableWhenOffline(monitor, DachboardButton, PatientsButton); } catch (Throwable ignored) {}

        wireAppointmentsTable();
        wirePatientsTable();
        wireSearch();

        if (loadUserAndEnsureDoctorProfile()) {
            reloadAll();
        }
    }

    /* ================= Header time & date (12h) ================= */
    private void startClock() {
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, e -> time.setText(java.time.LocalTime.now().format(tf))),
                new KeyFrame(Duration.seconds(1))
        );
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();

        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateOfDay.setText(LocalDate.now().format(df));
    }

    /* ================= Navigation ================= */
    private void showDashboardPane() {
        DashboardAnchorPane.setVisible(true);
        PatientAnchorPane.setVisible(false);
        markNavActive(DachboardButton);
    }
    private void showPatientsPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(true);
        markNavActive(PatientsButton);
    }
    private void goBackToLogin() {
        Stage stage = (Stage) BackButton.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(new Navigation().Login_Fxml));
            loader.setControllerFactory(type ->
                    type == LoginController.class ? new LoginController(monitor) : null
            );
            Parent root = loader.load();
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

        if (u.getRole() != Role.DOCTOR) {
            showWarn("Role", "This user is not a doctor.");
            return false;
        }

        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                // ✅ تعتمد على DoctorDAO الذي أرسلته لك (يوفّر هذه الدالة)
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
    private void reloadAll() {
        loadTodayStatsAsync();
        loadTodayAppointmentsAsync();
        loadPatientsAsync();
    }

    private void loadTodayStatsAsync() {
        var u = Session.get(); if (u == null) return;
        new Thread(() -> {
            try {
                Stats s = svc.loadTodayStats(u.getId(), LocalDate.now());
                Platform.runLater(() -> {
                    TotalAppointments.setText("Today's Appointments: " + s.total());
                    TotalAppointments2.setText("Completed: " + s.completed());
                    TotalAppointments22.setText("Remaining: " + s.remaining());
                    TotalAppointments21.setText("Today's Patients: " + s.total());
                });
            } catch (Exception e) {
                Platform.runLater(() -> showWarn("Stats", "Failed to load today's stats. Please try again later."));
            }
        }, "doc-stats").start();
    }

    private void loadTodayAppointmentsAsync() {
        var u = Session.get(); if (u == null) return;
        new Thread(() -> {
            try {
                List<Appt> list = svc.listTodayAppointments(u.getId(), LocalDate.now());
                Platform.runLater(() -> {
                    apptData.clear();
                    for (Appt a : list) apptData.add(AppointmentRow.of(a));
                });
            } catch (Exception e) {
                Platform.runLater(() -> showWarn("Appointments", "Failed to load today's appointments."));
            }
        }, "doc-appts").start();
    }

    private void loadPatientsAsync() {
        new Thread(() -> {
            try {
                List<com.example.healthflow.model.dto.PatientView> list = svc.listDoctorPatients();
                Platform.runLater(() -> {
                    patientData.clear();
                    for (com.example.healthflow.model.dto.PatientView pv : list) {
                        patientData.add(new PatientRow(
                                pv.nationalId(),
                                pv.fullName(),
                                pv.gender(),
                                ageFromDob(pv.dateOfBirth()),
                                pv.medicalHistory()
                        ));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showWarn("Patients", "Failed to load patients."));
            }
        }, "doc-patients").start();
    }

    /* ================= Tables wiring ================= */
    private void wireAppointmentsTable() {
        if (AppointmentsTable == null) return;

        if (colPatientName != null) colPatientName.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        if (colDate != null)        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        if (colTime != null)        colTime.setCellValueFactory(new PropertyValueFactory<>("timeStr"));
        if (colStatus != null)      colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (colAction != null) {
            colAction.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
            colAction.setCellFactory(col -> new TableCell<>() {
                private final Button btnView = new Button("View");
                private final Button btnDone = new Button("Done");
                private final Button btnPresc = new Button("Prescription");
                private final HBox box = new HBox(6, btnView, btnDone, btnPresc);
                {
                    box.setPadding(new Insets(0,0,0,0));
                    btnDone.disableProperty().bind(monitor.onlineProperty().not());
                    btnPresc.disableProperty().bind(monitor.onlineProperty().not());

                    btnView.setOnAction(e -> {
                        AppointmentRow row = getItem();
                        if (row != null) showPatientDetails(row);
                    });
                    btnDone.setOnAction(e -> {
                        AppointmentRow row = getItem();
                        if (row != null) completeAppointment(row);
                    });
                    btnPresc.setOnAction(e -> {
                        AppointmentRow row = getItem();
                        if (row != null) openPrescription(row);
                    });
                }
                @Override protected void updateItem(AppointmentRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }
        AppointmentsTable.setItems(apptData);
    }

    private void wirePatientsTable() {
        if (patientTable == null) return;

        if (colNationalId != null)     colNationalId.setCellValueFactory(new PropertyValueFactory<>("nationalId"));
        if (colName != null)           colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        if (colGender != null)         colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        if (colDob != null)            colDob.setCellValueFactory(new PropertyValueFactory<>("age"));
        if (colMedicalHistory != null) colMedicalHistory.setCellValueFactory(new PropertyValueFactory<>("medicalHistory"));
        if (colAction2 != null) {
            colAction2.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
            colAction2.setCellFactory(col -> new TableCell<>() {
                private final Button btnView = new Button("View");
                { btnView.setOnAction(e -> {
                    PatientRow row = getItem();
                    if (row != null) showPatientDetails(row.getFullName(), row.getMedicalHistory());
                });
                }
                @Override protected void updateItem(PatientRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setGraphic(empty ? null : btnView);
                }
            });
        }
        patientTable.setItems(patientData);
    }

    private void wireSearch() {
        filtered = new FilteredList<>(patientData, p -> true);
        if (searchLabel != null) {
            searchLabel.textProperty().addListener((obs, old, q) -> {
                String s = (q == null) ? "" : q.trim().toLowerCase();
                filtered.setPredicate(p -> {
                    if (s.isEmpty()) return true;
                    if (contains(p.getFullName(), s))        return true;
                    if (contains(p.getGender(), s))          return true;
                    if (contains(p.getNationalId(), s))      return true;
                    if (contains(p.getMedicalHistory(), s))  return true;
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
                    loadTodayStatsAsync();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showWarn("Update", "Could not mark as completed. Please try again later."));
            }
        }, "doc-complete").start();
    }

    private void openPrescription(AppointmentRow row) {
        showInfo("Prescription", "Open prescription composer for: " + row.getPatientName() +
                "\n(Status will be PENDING for pharmacy).");
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
    private static String safe(String s) { return s == null ? "" : s; }

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

        public long getId() { return id.get(); }
        public void setId(long v) { id.set(v); }
        public LongProperty idProperty() { return id; }

        public String getPatientName() { return patientName.get(); }
        public void setPatientName(String v) { patientName.set(v); }
        public StringProperty patientNameProperty() { return patientName; }

        public String getNationalId() { return nationalId.get(); }
        public void setNationalId(String v) { nationalId.set(v); }
        public StringProperty nationalIdProperty() { return nationalId; }

        public LocalDate getDate() { return date.get(); }
        public void setDate(LocalDate v) { date.set(v); }
        public ObjectProperty<LocalDate> dateProperty() { return date; }

        public String getTimeStr() { return timeStr.get(); }
        public void setTimeStr(String v) { timeStr.set(v); }
        public StringProperty timeStrProperty() { return timeStr; }

        public String getStatus() { return status.get(); }
        public void setStatus(String v) { status.set(v); }
        public StringProperty statusProperty() { return status; }

        public long getPatientUserId() { return patientUserId.get(); }
        public void setPatientUserId(long v) { patientUserId.set(v); }

        public String getMedicalHistory() { return medicalHistory.get(); }
        public void setMedicalHistory(String v) { medicalHistory.set(v); }
        public StringProperty medicalHistoryProperty() { return medicalHistory; }
    }

    public static class PatientRow {
        private final StringProperty nationalId = new SimpleStringProperty();
        private final StringProperty fullName = new SimpleStringProperty();
        private final StringProperty gender = new SimpleStringProperty();
        private final IntegerProperty age = new SimpleIntegerProperty();
        private final StringProperty medicalHistory = new SimpleStringProperty();

        public PatientRow(String nid, String name, String gender, int age, String history) {
            setNationalId(nid);
            setFullName(name);
            setGender(gender);
            setAge(age);
            setMedicalHistory(history);
        }

        public String getNationalId() { return nationalId.get(); }
        public void setNationalId(String v) { nationalId.set(v); }
        public StringProperty nationalIdProperty() { return nationalId; }

        public String getFullName() { return fullName.get(); }
        public void setFullName(String v) { fullName.set(v); }
        public StringProperty fullNameProperty() { return fullName; }

        public String getGender() { return gender.get(); }
        public void setGender(String v) { gender.set(v); }
        public StringProperty genderProperty() { return gender; }

        public int getAge() { return age.get(); }
        public void setAge(int v) { age.set(v); }
        public IntegerProperty ageProperty() { return age; }

        public String getMedicalHistory() { return medicalHistory.get(); }
        public void setMedicalHistory(String v) { medicalHistory.set(v); }
        public StringProperty medicalHistoryProperty() { return medicalHistory; }
    }
}


//package com.example.healthflow.controllers;
//
//import com.example.healthflow.dao.DoctorDAO;
//import com.example.healthflow.db.Database;
//import com.example.healthflow.model.Role;
//import com.example.healthflow.model.User;
//import com.example.healthflow.net.ConnectivityMonitor;
//import com.example.healthflow.service.AuthService.Session;
//import com.example.healthflow.service.DoctorDashboardService;
//import com.example.healthflow.service.DoctorDashboardService.Appt;
//import com.example.healthflow.service.DoctorDashboardService.Stats;
//import com.example.healthflow.ui.ConnectivityBanner;
//import com.example.healthflow.ui.OnlineBindings;
//import javafx.animation.KeyFrame;
//import javafx.animation.Timeline;
//import javafx.application.Platform;
//import javafx.beans.property.*;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.collections.transformation.FilteredList;
//import javafx.collections.transformation.SortedList;
//import javafx.fxml.FXML;
//import javafx.fxml.FXMLLoader;
//import javafx.geometry.Insets;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.scene.control.*;
//import javafx.scene.control.cell.PropertyValueFactory;
//import javafx.scene.layout.AnchorPane;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.scene.shape.Circle;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//
//import java.io.IOException;
//import java.sql.Connection;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//
//public class DoctorController {
//
//    /* ====== Cards / header / nav ====== */
//    @FXML private AnchorPane Appointments;
//    @FXML private Label TotalAppointments;
//
//    @FXML private AnchorPane Appointments21;
//    @FXML private Label TotalAppointments21;
//
//    @FXML private AnchorPane Appointments2;
//    @FXML private Label TotalAppointments2;
//
//    @FXML private AnchorPane Appointments22;
//    @FXML private Label TotalAppointments22;
//
//    @FXML private Button BackButton;
//    @FXML private Button DachboardButton;
//    @FXML private Button PatientsButton;
//
//    @FXML private AnchorPane CenterAnchorPane;
//    @FXML private AnchorPane DashboardAnchorPane;
//    @FXML private AnchorPane PatientAnchorPane;
//
//    @FXML private Label DateOfDay;
//    @FXML private Label time;
//    @FXML private Label welcomeUser;
//    @FXML private Label UsernameLabel;
//    @FXML private Label UserIdLabel;
//
//    @FXML
//    private Circle ActiveStatus;
//
//    /* ====== Dashboard table (appointments) ====== */
//    @FXML private TableView<AppointmentRow> AppointmentsTable;
//    @FXML private TableColumn<AppointmentRow, String> colPatientName;
//    @FXML private TableColumn<AppointmentRow, LocalDate> colDate;
//    @FXML private TableColumn<AppointmentRow, String> colTime;
//    @FXML private TableColumn<AppointmentRow, String> colStatus;
//    @FXML private TableColumn<AppointmentRow, AppointmentRow> colAction;
//
//    /* ====== Patients tab ====== */
//    @FXML private TableView<PatientRow> patientTable;
//    @FXML private TableColumn<PatientRow, String> colNationalId;
//    @FXML private TableColumn<PatientRow, String> colName;
//    @FXML private TableColumn<PatientRow, String> colGender;
//    @FXML private TableColumn<PatientRow, Integer> colDob; // age
//    @FXML private TableColumn<PatientRow, String> colMedicalHistory;
//    @FXML private TableColumn<PatientRow, PatientRow> colAction2;
//    @FXML private TextField search;      // patients search (future)
//    @FXML private TextField searchLabel; // appointments search (future)
//    private FilteredList<PatientRow> filtered;
//    private SortedList<PatientRow> sorted;
//    @FXML private VBox rootPane;
//
//
//    private static final String ACTIVE_CLASS = "current";
//
//    // To color current btn page
//    private void markNavActive(Button active) {
//        Button[] all = {DachboardButton, PatientsButton};
//        for (Button b : all) {
//            b.getStyleClass().remove(ACTIVE_CLASS);
//            // تأكد الزرار عليه class nav-btn (لو مش حاططها في الـ FXML)
//            if (!b.getStyleClass().contains("nav-btn")) b.getStyleClass().add("nav-btn");
//        }
//        if (active != null && !active.getStyleClass().contains(ACTIVE_CLASS)) {
//            active.getStyleClass().add(ACTIVE_CLASS);
//        }
//    }
//
//    /* ====== Services / state ====== */
//    private final ConnectivityMonitor monitor;
//    private final DoctorDAO doctorDAO = new DoctorDAO();
//    private final DoctorDashboardService svc = new DoctorDashboardService();
//
//    private final ObservableList<AppointmentRow> apptData = FXCollections.observableArrayList();
//    private final ObservableList<PatientRow> patientData = FXCollections.observableArrayList();
//
//    public DoctorController(ConnectivityMonitor monitor) { this.monitor = monitor; }
//    public DoctorController() { this(new ConnectivityMonitor()); }
//
//    /* ================= INIT ================= */
//    @FXML
//    private void initialize() {
//        // connectivity + banner
//        monitor.start();
//        showDashboardPane();
//        if (rootPane != null) {
//            ConnectivityBanner banner = new ConnectivityBanner(monitor);
//            rootPane.getChildren().add(0, banner);
//        }
//
//        // clock/date
//        startClock();
//
//        // nav
//        DachboardButton.setOnAction(e -> showDashboardPane());
//        PatientsButton.setOnAction(e -> showPatientsPane());
//        BackButton.setOnAction(e -> goBackToLogin());
////        wireSearch();
//        // disable left buttons when offline (if OnlineBindings exists)
//        try { OnlineBindings.disableWhenOffline(monitor, DachboardButton, PatientsButton); } catch (Throwable ignored) {}
//
//        // tables wiring
//        wireAppointmentsTable();
//        wirePatientsTable();
//        wireSearch();
//
//        if (loadUserAndEnsureDoctorProfile()) {
//            reloadAll();
//        }
//    }
//    /* ================= Header time & date (12h) ================= */
//    private void startClock() {
//        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a");
//        Timeline tl = new Timeline(
//                new KeyFrame(Duration.ZERO, e -> time.setText(java.time.LocalTime.now().format(tf))),
//                new KeyFrame(Duration.seconds(1))
//        );
//        tl.setCycleCount(Timeline.INDEFINITE);
//        tl.play();
//
//        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd/yyyy");
//        DateOfDay.setText(LocalDate.now().format(df));
//    }
//
//    /* ================= Navigation ================= */
//    private void showDashboardPane() {
//        DashboardAnchorPane.setVisible(true);
//        PatientAnchorPane.setVisible(false);
//        markNavActive(DachboardButton);
//
//    }
//    private void showPatientsPane() {
//        DashboardAnchorPane.setVisible(false);
//        PatientAnchorPane.setVisible(true);
//        markNavActive(PatientsButton);
//    }
//    private void goBackToLogin() {
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
//        } catch (IOException e) {
//            showError("Navigation", e);
//        }
//    }
//
//    /* ================= User & role ================= */
//    private boolean loadUserAndEnsureDoctorProfile() {
//        User u = Session.get();
//        if (u == null) return false;
//
//        UsernameLabel.setText(u.getFullName());
//        UserIdLabel.setText(String.valueOf(u.getId()));
//        welcomeUser.setText(firstName(u.getFullName()));
//
//        if (u.getRole() != Role.DOCTOR) {
//            showWarn("Role", "This user is not a doctor.");
//            return false;
//        }
//
//        try (Connection c = Database.get()) {
//            c.setAutoCommit(false);
//            try {
//                doctorDAO.ensureProfileForUser(c, u.getId()); // create if missing
//                c.commit();
//            } catch (Exception ex) {
//                c.rollback();
//                showWarn("Doctor Profile", "Could not ensure doctor profile. Please try again later.");
//                return false;
//            } finally {
//                c.setAutoCommit(true);
//            }
//        } catch (Exception ex) {
//            showWarn("Connection", "Database connection failed. Please try again later.");
//            return false;
//        }
//
//        return true;
//    }
//
//    /* ================= Data loads ================= */
//    private void reloadAll() {
//        loadTodayStatsAsync();
//        loadTodayAppointmentsAsync();
//        loadPatientsAsync();
//    }
//
//    private void loadTodayStatsAsync() {
//        var u = Session.get(); if (u == null) return;
//        new Thread(() -> {
//            try {
//                Stats s = svc.loadTodayStats(u.getId(), LocalDate.now());
//                Platform.runLater(() -> {
//                    TotalAppointments.setText("Today's Appointments: " + s.total());
//                    TotalAppointments2.setText("Completed: " + s.completed());
//                    TotalAppointments22.setText("Remaining: " + s.remaining());
//                    TotalAppointments21.setText("Today's Patients: " + s.total()); // initial = total
//                });
//            } catch (Exception e) {
//                Platform.runLater(() -> showWarn("Stats", "Failed to load today's stats. Please try again later."));
//            }
//        }, "doc-stats").start();
//    }
//
//    private void loadTodayAppointmentsAsync() {
//        var u = Session.get(); if (u == null) return;
//        new Thread(() -> {
//            try {
//                List<Appt> list = svc.listTodayAppointments(u.getId(), LocalDate.now());
//                Platform.runLater(() -> {
//                    apptData.clear();
//                    for (Appt a : list) apptData.add(AppointmentRow.of(a));
//                });
//            } catch (Exception e) {
//                Platform.runLater(() -> showWarn("Appointments", "Failed to load today's appointments."));
//            }
//        }, "doc-appts").start();
//    }
//
//    private void loadPatientsAsync() {
//        new Thread(() -> {
//            try {
//                List<com.example.healthflow.model.dto.PatientView> list = svc.listDoctorPatients();
//                Platform.runLater(() -> {
//                    patientData.clear();
//                    for (com.example.healthflow.model.dto.PatientView pv : list) {
//                        patientData.add(new PatientRow(
//                                pv.nationalId(),
//                                pv.fullName(),
//                                pv.gender(),
//                                ageFromDob(pv.dateOfBirth()),
//                                pv.medicalHistory()
//                        ));
//                    }
//                });
//            } catch (Exception e) {
//                Platform.runLater(() -> showWarn("Patients", "Failed to load patients."));
//            }
//        }, "doc-patients").start();
//    }
//
//    /* ================= Tables wiring ================= */
//    private void wireAppointmentsTable() {
//        if (colPatientName != null) colPatientName.setCellValueFactory(new PropertyValueFactory<>("patientName"));
//        if (colDate != null)        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
//        if (colTime != null)        colTime.setCellValueFactory(new PropertyValueFactory<>("timeStr"));
//        if (colStatus != null)      colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
//        if (colAction != null) {
//            colAction.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
//            colAction.setCellFactory(col -> new TableCell<>() {
//                private final Button btnView = new Button("View");
//                private final Button btnDone = new Button("Done");
//                private final Button btnPresc = new Button("Prescription");
//                private final HBox box = new HBox(6, btnView, btnDone, btnPresc);
//                {
//                    box.setPadding(new Insets(0,0,0,0));
//                    // disable when offline
//                    btnDone.disableProperty().bind(monitor.onlineProperty().not());
//                    btnPresc.disableProperty().bind(monitor.onlineProperty().not());
//
//                    btnView.setOnAction(e -> {
//                        AppointmentRow row = getItem();
//                        if (row != null) showPatientDetails(row);
//                    });
//                    btnDone.setOnAction(e -> {
//                        AppointmentRow row = getItem();
//                        if (row != null) completeAppointment(row);
//                    });
//                    btnPresc.setOnAction(e -> {
//                        AppointmentRow row = getItem();
//                        if (row != null) openPrescription(row);
//                    });
//                }
//                @Override
//                protected void updateItem(AppointmentRow row, boolean empty) {
//                    super.updateItem(row, empty);
//                    setGraphic(empty ? null : box);
//                }
//            });
//        }
//        AppointmentsTable.setItems(apptData);
//    }
//
//    private void wirePatientsTable() {
//        if (colNationalId != null)     colNationalId.setCellValueFactory(new PropertyValueFactory<>("nationalId"));
//        if (colName != null)           colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
//        if (colGender != null)         colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
//        if (colDob != null)            colDob.setCellValueFactory(new PropertyValueFactory<>("age"));
//        if (colMedicalHistory != null) colMedicalHistory.setCellValueFactory(new PropertyValueFactory<>("medicalHistory"));
//        if (colAction2 != null) {
//            colAction2.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
//            colAction2.setCellFactory(col -> new TableCell<>() {
//                private final Button btnView = new Button("View");
//                {
//                    btnView.setOnAction(e -> {
//                        PatientRow row = getItem();
//                        if (row != null) {
//                            showPatientDetails(row.getFullName(), row.getMedicalHistory());
//                        }
//
//                    });
//                }
//                @Override
//                protected void updateItem(PatientRow row, boolean empty) {
//                    super.updateItem(row, empty);
//                    setGraphic(empty ? null : btnView);
//                }
//            });
//        }
//        patientTable.setItems(patientData);
//    }
//
//    private void wireSearch() {
//        // لو عندك patientTable.setItems(...) في مكان آخر احذفه، لأننا سنضبط الـ items هنا
//        filtered = new FilteredList<>(patientData, p -> true);
//
//        searchLabel.textProperty().addListener((obs, old, q) -> {
//            String s = (q == null) ? "" : q.trim().toLowerCase();
//
//            filtered.setPredicate(p -> {
//                if (s.isEmpty()) return true;
//
//                // مطابقات النص على الحقول المعروضة
//                if (contains(p.getFullName(), s))        return true;
//                if (contains(p.getGender(), s))          return true;
//                if (contains(p.getNationalId(), s))      return true;
//                if (contains(p.getMedicalHistory(), s))  return true;
//
//                // العمر كرقم
//                if (String.valueOf(p.getAge()).contains(s)) return true;
//
//                // === لو عندك هذه الحقول فعلاً في PatientRow فعّل السطور التالية ===
//                // if (contains(p.getPhone(), s))              return true;
//                // if (p.getDateOfBirth() != null && p.getDateOfBirth().toString().toLowerCase().contains(s)) return true;
//
//                return false;
//            });
//        });
//
//        sorted = new SortedList<>(filtered);
//        sorted.comparatorProperty().bind(patientTable.comparatorProperty());
//        patientTable.setItems(sorted);
//    }
//
//    // مساعد case-insensitive + null-safe
//    private static boolean contains(String value, String q) {
//        return value != null && value.toLowerCase().contains(q);
//    }
//
//    /* ================= Actions ================= */
//    private void showPatientDetails(AppointmentRow row) {
//        showInfo("Patient details",
//                "Name: " + row.getPatientName() +
//                        "\nNational ID: " + safe(row.getNationalId()) +
//                        "\nMedical history:\n" + safe(row.getMedicalHistory()));
//    }
//
//    private void showPatientDetails(String name, String history) {
//        showInfo("Patient details",
//                "Name: " + safe(name) + "\n\nMedical history:\n" + safe(history));
//    }
//
//    private void completeAppointment(AppointmentRow row) {
//        if (!monitor.isOnline()) {
//            showWarn("Offline", "You are offline. Please reconnect and try again.");
//            return;
//        }
//        new Thread(() -> {
//            try {
//                svc.markCompleted(row.getId());
//                Platform.runLater(() -> {
//                    row.setStatus("COMPLETED");
//                    AppointmentsTable.refresh();
//                    loadTodayStatsAsync();
//                });
//            } catch (Exception e) {
//                Platform.runLater(() -> showWarn("Update", "Could not mark as completed. Please try again later."));
//            }
//        }, "doc-complete").start();
//    }
//
//    private void openPrescription(AppointmentRow row) {
//        // Placeholder — prescription UI will be added later
//        showInfo("Prescription", "Open prescription composer for: " + row.getPatientName() + "\n(Status will be PENDING for pharmacy).");
//    }
//
//    /* ================= Helpers ================= */
//    private String firstName(String full) {
//        if (full == null || full.isBlank()) return "user";
//        return full.trim().split("\\s+")[0];
//    }
//
//    private static int ageFromDob(java.time.LocalDate dob) {
//        if (dob == null) return 0;
//        return java.time.Period.between(dob, LocalDate.now()).getYears();
//    }
//
//    private static String safe(String s) { return s == null ? "" : s; }
//
//    private void showError(String title, Exception ex) {
//        ex.printStackTrace();
//        Alert a = new Alert(Alert.AlertType.ERROR);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(ex.getMessage());
//        a.showAndWait();
//    }
//    private void showWarn(String title, String msg) {
//        Alert a = new Alert(Alert.AlertType.WARNING);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(msg);
//        a.showAndWait();
//    }
//    private void showInfo(String title, String msg) {
//        Alert a = new Alert(Alert.AlertType.INFORMATION);
//        a.setTitle(title);
//        a.setHeaderText(null);
//        a.setContentText(msg);
//        a.showAndWait();
//    }
//
//    /* ================= Row models ================= */
//    public static class AppointmentRow {
//        private final LongProperty id = new SimpleLongProperty();
//        private final StringProperty patientName = new SimpleStringProperty();
//        private final StringProperty nationalId = new SimpleStringProperty();
//        private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
//        private final StringProperty timeStr = new SimpleStringProperty();
//        private final StringProperty status = new SimpleStringProperty();
//        private final LongProperty patientUserId = new SimpleLongProperty();
//        private final StringProperty medicalHistory = new SimpleStringProperty();
//
//        public static AppointmentRow of(Appt a) {
//            AppointmentRow r = new AppointmentRow();
//            r.setId(a.id);
//            r.setPatientName(a.patientName);
//            r.setNationalId(a.patientNationalId);
//            r.setDate(a.date);
//            r.setTimeStr(a.time == null ? "" : a.time.toString());
//            r.setStatus(a.status);
//            r.setPatientUserId(a.patientUserId);
//            r.setMedicalHistory(a.medicalHistory);
//            return r;
//        }
//
//        public long getId() { return id.get(); }
//        public void setId(long v) { id.set(v); }
//        public LongProperty idProperty() { return id; }
//
//        public String getPatientName() { return patientName.get(); }
//        public void setPatientName(String v) { patientName.set(v); }
//        public StringProperty patientNameProperty() { return patientName; }
//
//        public String getNationalId() { return nationalId.get(); }
//        public void setNationalId(String v) { nationalId.set(v); }
//        public StringProperty nationalIdProperty() { return nationalId; }
//
//        public LocalDate getDate() { return date.get(); }
//        public void setDate(LocalDate v) { date.set(v); }
//        public ObjectProperty<LocalDate> dateProperty() { return date; }
//
//        public String getTimeStr() { return timeStr.get(); }
//        public void setTimeStr(String v) { timeStr.set(v); }
//        public StringProperty timeStrProperty() { return timeStr; }
//
//        public String getStatus() { return status.get(); }
//        public void setStatus(String v) { status.set(v); }
//        public StringProperty statusProperty() { return status; }
//
//        public long getPatientUserId() { return patientUserId.get(); }
//        public void setPatientUserId(long v) { patientUserId.set(v); }
//
//        public String getMedicalHistory() { return medicalHistory.get(); }
//        public void setMedicalHistory(String v) { medicalHistory.set(v); }
//        public StringProperty medicalHistoryProperty() { return medicalHistory; }
//    }
//
//    public static class PatientRow {
//        private final StringProperty nationalId = new SimpleStringProperty();
//        private final StringProperty fullName = new SimpleStringProperty();
//        private final StringProperty gender = new SimpleStringProperty();
//        private final IntegerProperty age = new SimpleIntegerProperty();
//        private final StringProperty medicalHistory = new SimpleStringProperty();
//
//        public PatientRow(String nid, String name, String gender, int age, String history) {
//            setNationalId(nid);
//            setFullName(name);
//            setGender(gender);
//            setAge(age);
//            setMedicalHistory(history);
//        }
//
//        public String getNationalId() { return nationalId.get(); }
//        public void setNationalId(String v) { nationalId.set(v); }
//        public StringProperty nationalIdProperty() { return nationalId; }
//
//        public String getFullName() { return fullName.get(); }
//        public void setFullName(String v) { fullName.set(v); }
//        public StringProperty fullNameProperty() { return fullName; }
//
//        public String getGender() { return gender.get(); }
//        public void setGender(String v) { gender.set(v); }
//        public StringProperty genderProperty() { return gender; }
//
//        public int getAge() { return age.get(); }
//        public void setAge(int v) { age.set(v); }
//        public IntegerProperty ageProperty() { return age; }
//
//        public String getMedicalHistory() { return medicalHistory.get(); }
//        public void setMedicalHistory(String v) { medicalHistory.set(v); }
//        public StringProperty medicalHistoryProperty() { return medicalHistory; }
//    }
//}
