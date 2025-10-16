package com.example.healthflow.controllers;

import com.example.healthflow.dao.DoctorDAO;
import com.example.healthflow.db.Database;
import com.example.healthflow.model.Role;
import com.example.healthflow.model.User;
import com.example.healthflow.model.dto.MedicineRow;
import com.example.healthflow.model.dto.PrescItemRow;
import com.example.healthflow.dao.PrescriptionItemDAO;
import com.example.healthflow.dao.PrescriptionDAO;
import com.example.healthflow.model.Prescription;
import com.example.healthflow.model.PrescriptionItem;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.service.AuthService.Session;
import com.example.healthflow.service.DoctorDashboardService;
import com.example.healthflow.service.DoctorDashboardService.Appt;
import com.example.healthflow.service.DoctorDashboardService.Stats;
import com.example.healthflow.ui.ConnectivityBanner;
import com.example.healthflow.ui.OnlineBindings;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.KeyValue;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
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
    @FXML private Button PrescriptionButton;
    @FXML private Button Add_Medication;
    @FXML private Button cancelAddMedication;
    @FXML private Button InsertMedicine; // to insert medicine to database
    @FXML private Button sendToPharmacy;

    @FXML private Button Update_Medication;
    @FXML private Button Delete_Medication;

    @FXML private TextField medicineField;
    @FXML private TableView<MedicineRow> medicineSuggestTable;  // not visible in the current time
    @FXML private TableColumn<MedicineRow, String> colMedName;

    @FXML private AnchorPane CenterAnchorPane;
    @FXML private AnchorPane DashboardAnchorPane;
    @FXML private AnchorPane PatientAnchorPane;
    @FXML private AnchorPane PrescriptionAnchorPane;
    @FXML private AnchorPane PrescriptionMedicationAnchorPane;
    @FXML private AnchorPane AddMedicationAnchorPane;


    @FXML private Label DateOfDay;
    @FXML private Label time;
    @FXML private Label welcomeUser;
    @FXML private Label UsernameLabel;
    @FXML private Label UserIdLabel;
    @FXML private Label alertLabel;


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

    /* ====== Medicine tab ====== */
    @FXML private TableView<PrescItemRow> TablePrescriptionItems;
    @FXML private TableColumn<PrescItemRow, Number>  colIdx;
    @FXML private TableColumn<PrescItemRow, String>  colMedicineName;
    @FXML private TableColumn<PrescItemRow, String>  colDosage;
    @FXML private TableColumn<PrescItemRow, Integer> colDuration;
    @FXML private TableColumn<PrescItemRow, Integer> colQuantity;
    @FXML private TableColumn<PrescItemRow, Integer> colDispensed;
    @FXML private TableColumn<PrescItemRow, PrescItemRow> colPresesAction;
    @FXML private TableColumn<PrescItemRow, String>  colPresesStatus;

    // In-memory draft prescription items (source list for the table)
    private final ObservableList<PrescItemRow> prescItemsEditable = FXCollections.observableArrayList();

    // Tracks whether we're editing an existing row from the table
    private PrescItemRow editingRow = null;
    private Long currentPrescriptionId = null;

    @FXML private AnchorPane rootPane;


//    ------------------------------

    @FXML
    private TextArea DiagnosisTF;

    @FXML
    private Label DoctorNameLabel;

    @FXML
    private Label Dose;

    @FXML
    private Label PatientNameLabel;

    @FXML
    private TableColumn<?, ?> colFreqPerDay;

    @FXML
    private Label dateWithTimePres;

    @FXML
    private TextField doseText;

    @FXML
    private TextField duration;

    @FXML
    private ComboBox<?> formCombo;

    @FXML
    private TextField freq_day;

    @FXML
    private TextArea medicalHistory1;

    @FXML
    private TextField medicineName;

    @FXML
    private ComboBox<?> routeCombo;


    @FXML
    private ComboBox<?> strength_combo;


    @FXML
    private Label userStatus;




    /* ====== Services / state ====== */
    private final ConnectivityMonitor monitor;
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final DoctorDashboardService svc = new DoctorDashboardService();

    private final ObservableList<AppointmentRow> apptData = FXCollections.observableArrayList();
    private final ObservableList<PatientRow> patientData = FXCollections.observableArrayList();



    public DoctorController(ConnectivityMonitor monitor) { this.monitor = monitor; }
    public DoctorController() { this(new ConnectivityMonitor()); }


    /* ====== Nav highlight ====== */
    private static final String ACTIVE_CLASS = "current";
    private void markNavActive(Button active) {
        Button[] all = {DachboardButton, PatientsButton ,PrescriptionButton};
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
        PrescriptionButton.setOnAction(e -> showPrescriptionPane());
        Add_Medication.setOnAction(e -> showPrescriptionPaneToAddMedication());
        cancelAddMedication.setOnAction(e -> showPrescriptionPane());


        BackButton.setOnAction(e -> goBackToLogin());

        try { OnlineBindings.disableWhenOffline(monitor, DachboardButton, PatientsButton); } catch (Throwable ignored) {}

        wireAppointmentsTable();
        wirePatientsTable();
        wireSearch();

        wirePrescriptionItemsTable();
        if (TablePrescriptionItems != null) {
            TablePrescriptionItems.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                boolean has = n != null;
                if (Update_Medication != null) Update_Medication.setDisable(!has);
                if (Delete_Medication != null) Delete_Medication.setDisable(!has);
            });
            if (Update_Medication != null) Update_Medication.setDisable(true);
            if (Delete_Medication != null) Delete_Medication.setDisable(true);
        }

        // Add / Save in the Add-Medicine pane
        if (InsertMedicine != null) {
            InsertMedicine.setOnAction(e -> addMedicineFromDialog());
        }
            // Edit: open Add pane prefilled with selected row
        if (Update_Medication != null) {
            Update_Medication.setOnAction(e -> openEditSelectedItem());
        }
            // Delete selected (from table; if has DB id -> delete from DB too)
        if (Delete_Medication != null) {
            Delete_Medication.setOnAction(e -> deleteSelectedItem());
        }



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
        PrescriptionAnchorPane.setVisible(false);

        markNavActive(DachboardButton);
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
    private void reloadAll() {
        loadTodayStatsAsync();
        loadTodayAppointmentsAsync();
        loadPatientsAsync(); // الآن تُحمِّل فقط مرضى هذا الدكتور (اليوم)
    }

    private void loadTodayStatsAsync() {
        var u = Session.get(); if (u == null) return;
        new Thread(() -> {
            try {
                Stats s = svc.loadTodayStats(u.getId(), LocalDate.now());
                Platform.runLater(() -> {
                    setTextSafe(TotalAppointments,  "Today's Appointments: " + s.total());
                    setTextSafe(TotalAppointments2,  "Completed: " + s.completed());
                    setTextSafe(TotalAppointments22, "Remaining: " + s.remaining());
                    setTextSafe(TotalAppointments21, "Today's Patients: " + s.total());
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

    /**
     * Loads all patients who have any appointment with the currently logged-in doctor (no date filter).
     * Computes the age from patients.date_of_birth.
     */
    private void loadPatientsAsync() {
        var u = Session.get();
        if (u == null) return;

        new Thread(() -> {
            try {
                // اجلب كل مرضى هذا الطبيب (حسب user_id) من الـ DAO
                java.util.List<com.example.healthflow.dao.DoctorDAO.PatientWithAppt> list =
                        doctorDAO.listPatientsWithAppointmentsForDoctor(u.getId());

                var rows = FXCollections.<PatientRow>observableArrayList();
                for (var r : list) {
                    rows.add(new PatientRow(
                            r.nationalId,
                            r.patientName,
                            r.gender,
                            ageFromDob(r.dateOfBirth),
                            r.medicalHistory
                    ));
                }
                Platform.runLater(() -> patientData.setAll(rows));
            } catch (Exception e) {
                Platform.runLater(() -> showWarn("Patients", "Failed to load patients for this doctor."));
            }
        }, "doc-patients").start();
    }

    /* ================= Tables wiring ================= */

    private void wireAppointmentsTable() {
        if (AppointmentsTable == null) return;

        // أعمدة البيانات
        if (colPatientName != null) colPatientName.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        if (colDate != null)        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        if (colTime != null)        colTime.setCellValueFactory(new PropertyValueFactory<>("timeStr"));
        if (colStatus != null)      colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // توزيع بعرض الجدول (نِسَب مئوية) ليتكيّف مع صِغر/كبر الشاشة
        // نطرح ~15px لعرض شريط التمرير عند الحاجة
        if (AppointmentsTable != null) {
            var w = AppointmentsTable.widthProperty().subtract(15);
            if (colPatientName != null) colPatientName.prefWidthProperty().bind(w.multiply(0.28));
            if (colDate != null)        colDate.prefWidthProperty().bind(w.multiply(0.15));
            if (colTime != null)        colTime.prefWidthProperty().bind(w.multiply(0.15));
            if (colStatus != null)      colStatus.prefWidthProperty().bind(w.multiply(0.18));
            if (colAction != null)      colAction.prefWidthProperty().bind(w.multiply(0.24));
        }

        // عمود الأكشن: حد أدنى ومعطّل تغيير الحجم والفرز
        colAction.setMinWidth(260);
        colAction.setResizable(false);
        colAction.setSortable(false);

        // ✨ خلية الأكشن باستخدام FlowPane (يضمن ترتيب أفقي، ولو ضاق يلف بدون تكسير)
        colAction.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAction.setCellFactory(tc -> new TableCell<AppointmentRow, AppointmentRow>() {
//            private final Button btnView  = new Button("View");
            private final Button btnDone  = new Button("Done");
            private final Button btnPresc = new Button("Prescription");
            private final HBox box = new HBox(8,btnDone, btnPresc); // مسافة 8مسافة px بين الأزرار
//            private final FlowPane pane   = new FlowPane();

            {
                // أبعاد الأزرار (عشان ما تنضغط لأحجام غريبة)
//                btnView.setMinWidth(80);   btnView.setMaxWidth(Region.USE_PREF_SIZE);
                btnDone.setMinWidth(80);   btnDone.setMaxWidth(Region.USE_PREF_SIZE);
                btnPresc.setMinWidth(120); btnPresc.setMaxWidth(Region.USE_PREF_SIZE);

                // تعطيل عند الأوفلاين
                btnDone.disableProperty().bind(monitor.onlineProperty().not());
                btnPresc.disableProperty().bind(monitor.onlineProperty().not());

                // الأحداث
//                btnView.setOnAction(e -> { var row = getItem(); if (row != null) showPatientDetails(row); });
                btnDone.setOnAction(e -> { var row = getItem(); if (row != null) completeAppointment(row); });
                btnPresc.setOnAction(e -> { var row = getItem(); if (row != null) openPrescription(row); });


                box.setAlignment(Pos.CENTER_LEFT);
                // أمثلة للأحداث
                btnPresc.setOnAction(e -> {
                    var row = getItem();
                    if (row != null) showPrescriptionPane();
                });
                btnDone.setOnAction(e -> {
                    var row = getItem();
                    if (row != null) completeAppointment(row);
                });

            }

            @Override
            protected void updateItem(AppointmentRow row, boolean empty) {
                super.updateItem(row, empty);
                setText(null);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
//                setGraphic(empty ? null : pane);
            }
        });

        // سياسة القياس: توزيع مُقيد يملأ عرض الجدول ويُظهر سكرول تلقائياً عند الحاجة
        AppointmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // ارتفاع صف متغيّر (السكرول سيظهر تلقائياً عندما لا تتسع المساحة)
        AppointmentsTable.setFixedCellSize(-1);

        // أخيراً البيانات
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

            if (colAction2 != null) {
                colAction2.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
                colAction2.setCellFactory(col -> new TableCell<PatientRow, PatientRow>() {
//                    private final Button btnView   = new Button("View");
                    private final Button btnDone  = new Button("Done");
                    private final Button btnPresc = new Button("Prescription");
                    private final HBox box = new HBox(8,btnDone, btnPresc);

                    {
                        // classes للزرار – هنستعملها في الـ CSS
//                        btnView.getStyleClass().addAll("btn", "btn--info");
                        btnDone.getStyleClass().addAll("btn", "btn-complete");
                        btnPresc.getStyleClass().addAll("btn", "btn-complete");
                        box.getStyleClass().add("table-actions");

                        // قياسات مريحة
//                        btnView.setMinWidth(74);   btnView.setMaxWidth(Region.USE_PREF_SIZE);
                        btnDone.setMinWidth(90); btnDone.setMaxWidth(Region.USE_PREF_SIZE);
                        btnPresc.setMinWidth(78);   btnPresc.setMaxWidth(Region.USE_PREF_SIZE);

                        // أفعال
                        btnDone.setOnAction(e -> {
                            PatientRow row = getItem();
                            if (row != null) showPatientDetails(row.getFullName(), row.getMedicalHistory());
                        });

                        btnPresc.setOnAction(e -> showPrescriptionPane());

                    }

                    @Override protected void updateItem(PatientRow row, boolean empty) {
                        super.updateItem(row, empty);
                        setText(null);
                        setGraphic(empty ? null : box);
                    }
                });
            }
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

    /* ================= Prescription Items table wiring ================= */
    private void wirePrescriptionItemsTable() {
        if (TablePrescriptionItems == null) return;


        TablePrescriptionItems.setItems(prescItemsEditable);
//        TablePrescriptionItems.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TablePrescriptionItems.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            //  خلي آخر عمود (Action) ياكل الفراغ الفاضي بدون ما يلغيلك الـ H-Scroll
        final double actionBase = (colPresesAction == null) ? 0 : colPresesAction.getPrefWidth();

        Runnable fitLastColumn = () -> {
            if (TablePrescriptionItems == null || colPresesAction == null) return;

            double used = 0;
            if (colIdx != null)           used += colIdx.getWidth();
            if (colMedicineName != null)  used += colMedicineName.getWidth();
            if (colDosage != null)        used += colDosage.getWidth();
            if (colDuration != null)      used += colDuration.getWidth();
            if (colQuantity != null)      used += colQuantity.getWidth();
            if (colDispensed != null)     used += colDispensed.getWidth();
            if (colPresesStatus != null)  used += colPresesStatus.getWidth();
            // ما نضيف عرض الـ Action هنا – بنحسبه بعدين

            double total = TablePrescriptionItems.getWidth();
            double padding = 14; // هامش/سكين
            double remaining = total - used - padding;

            // لو الأعمدة أكبر من الجدول → remaining سالب/صفر → نخلي الـ Action على عرضه الأساسي
            double newW = Math.max(actionBase, remaining);
            colPresesAction.setPrefWidth(newW);
        };

        Platform.runLater(fitLastColumn);

        if (colIdx != null) {
            colIdx.setSortable(false);
            colIdx.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(
                    TablePrescriptionItems.getItems().indexOf(cd.getValue()) + 1));
        }
        if (colMedicineName != null)  colMedicineName.setCellValueFactory(cd -> cd.getValue().medicineNameProperty());
        if (colDosage != null)        colDosage.setCellValueFactory(cd -> cd.getValue().dosageProperty());
        if (colDuration != null)      colDuration.setCellValueFactory(cd -> cd.getValue().durationDaysProperty().asObject());
        if (colQuantity != null)      colQuantity.setCellValueFactory(cd -> cd.getValue().quantityProperty().asObject());
        if (colDispensed != null)     colDispensed.setCellValueFactory(cd -> cd.getValue().qtyDispensedProperty().asObject());
        if (colPresesStatus != null)  if (colPresesStatus != null) colPresesStatus.setCellValueFactory(cd -> cd.getValue().statusProperty());

        if (colPresesAction != null) {
            colPresesAction.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
            colPresesAction.setSortable(false);
            colPresesAction.setResizable(false);
            colPresesAction.setCellFactory(tc -> new TableCell<PrescItemRow, PrescItemRow>() {
                private final Button btnDel = new Button("Delete");
                private final HBox box = new HBox(8, btnDel);
                {
                    btnDel.getStyleClass().addAll("btn", "btn-danger");
                    btnDel.setOnAction(e -> {
                        PrescItemRow row = getItem();
                        if (row != null) prescItemsEditable.remove(row); // remove from SOURCE
                    });
                    box.setAlignment(Pos.CENTER_LEFT);
                }
                @Override protected void updateItem(PrescItemRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(null);
                    setGraphic(empty ? null : box);
                }
            });


        }

        // Optional: width percentages
        var w = TablePrescriptionItems.widthProperty().subtract(15);
        if (colIdx != null)          colIdx.prefWidthProperty().bind(w.multiply(0.07));
        if (colMedicineName != null) colMedicineName.prefWidthProperty().bind(w.multiply(0.22));
        if (colDosage != null)       colDosage.prefWidthProperty().bind(w.multiply(0.30));
        if (colDuration != null)     colDuration.prefWidthProperty().bind(w.multiply(0.10));
        if (colQuantity != null)     colQuantity.prefWidthProperty().bind(w.multiply(0.10));
        if (colDispensed != null)    colDispensed.prefWidthProperty().bind(w.multiply(0.08));
        if (colStatus != null)       colStatus.prefWidthProperty().bind(w.multiply(0.08));
        if (colPresesAction != null) colPresesAction.prefWidthProperty().bind(w.multiply(0.05));
    }

    /** Resolve patient id. الأفضل من جدول المواعيد لأنه يحوي patientUserId. */
    private Long resolveCurrentPatientId() {
        if (AppointmentsTable != null) {
            AppointmentRow sel = AppointmentsTable.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getPatientUserId() > 0) return sel.getPatientUserId();
        }
        return null; // لو بدك، لاحقًا نجيب من patientTable عبر DAO
    }

    /** Ensure draft prescription exists in DB and return its id (creates one if missing). */
    private Long ensureDraftPrescription() throws Exception {
        if (currentPrescriptionId != null) return currentPrescriptionId;

        var u = Session.get();
        if (u == null) throw new IllegalStateException("No logged-in user.");
        Long patientId = resolveCurrentPatientId();
        if (patientId == null) {
            toast("Select an appointment/patient first.", "warn");
            throw new IllegalStateException("No patient selected.");
        }

        try (Connection c = Database.get()) {
            c.setAutoCommit(true);
            PrescriptionDAO pDao = new PrescriptionDAO();
            // appointment_id = NULL (بدون موعد)، الملاحظات من DiagnosisTF إن وُجدت
            Prescription p = pDao.create(c, null, u.getId(), patientId, DiagnosisTF != null ? DiagnosisTF.getText() : null);
            currentPrescriptionId = p.getId();
            toast("Draft prescription #" + currentPrescriptionId + " created.", "ok");
            return currentPrescriptionId;
        }
    }

    /* ================= Add Medicine dialog -> add row into table ================= */
    private void addMedicineFromDialog() {
        try {
            Long prescId = ensureDraftPrescription(); // تأكد من وجود وصفة في DB

            // 1) medicine id/name
            MedicineRow sel = (medicineSuggestTable == null) ? null : medicineSuggestTable.getSelectionModel().getSelectedItem();
            String medNameText = (medicineName != null && medicineName.getText() != null) ? medicineName.getText().trim() : "";
            String medName = (sel != null) ? sel.getName() : medNameText;
            Long medId = (sel != null) ? sel.getId() : null;

            if (medName == null || medName.isBlank()) {
                toast("Please choose a medicine.", "warn");
                return;
            }

            // 2) dosage parts
            String dose = (doseText != null && doseText.getText()!=null) ? doseText.getText().trim() : "";
            String freqStr = (freq_day != null && freq_day.getText()!=null) ? freq_day.getText().trim() : "";
            String durStr  = (duration != null && duration.getText()!=null) ? duration.getText().trim() : "";
            int freqPerDay = safeParseInt(freqStr, 0);
            int days       = safeParseInt(durStr, 0);
            if (freqPerDay <= 0 || days <= 0) {
                toast("Enter positive numbers for Freq/day and Duration.", "warn");
                return;
            }

            String form  = (formCombo != null && formCombo.getValue()!=null) ? String.valueOf(formCombo.getValue()) : "";
            String route = (routeCombo != null && routeCombo.getValue()!=null) ? String.valueOf(routeCombo.getValue()) : "";
            String strength = (strength_combo != null && strength_combo.getValue()!=null) ? String.valueOf(strength_combo.getValue()) : "";
            String notesStr = (medicalHistory1 != null && medicalHistory1.getText()!=null) ? medicalHistory1.getText().trim() : "";

            // 3) compose dosage text
            StringBuilder ds = new StringBuilder();
            if (!strength.isBlank()) ds.append(strength).append(" ");
            if (!form.isBlank())     ds.append(form).append(" \u2022 ");
            if (!dose.isBlank())     ds.append(dose).append(" \u2022 ");
            ds.append(freqPerDay).append("x/day \u2022 ").append(days).append("d");
            if (!route.isBlank())    ds.append(" \u2022 ").append(route);
            if (!notesStr.isBlank()) ds.append(" \u2022 ").append(notesStr);

            // 4) quantity (solid forms): freq/day * days
            int qty = Math.max(1, freqPerDay * days);

            try (Connection c = Database.get()) {
                c.setAutoCommit(true);
                PrescriptionItemDAO dao = new PrescriptionItemDAO();

                if (editingRow == null || editingRow.getId() <= 0) {
                    // INSERT إلى الداتابيز أولاً
                    PrescriptionItem db = dao.addItem(c, prescId, medId, medName, ds.toString(), qty);

                    // أعرض في الجدول بناءً على البيانات الراجعة (الاسم قد يتظبط بالتريغر)
                    PrescItemRow row = new PrescItemRow();
                    row.setId(db.getId());
                    row.setMedicineId(db.getMedicineId() == null ? 0 : db.getMedicineId());
                    row.setMedicineName(db.getMedicineName());
                    row.setDosage(db.getDosage());
                    row.setDurationDays(days); // UI-only
                    row.setQuantity(db.getQuantity());
                    row.setQtyDispensed(db.getQtyDispensed());
                    row.setStatus(db.getStatus() == null ? "PENDING" : db.getStatus().name());
                    row.setNotes(notesStr);

                    prescItemsEditable.add(row);
                    toast("Medication added (DB) to prescription #" + prescId + ".", "ok");
                } else {
                    // UPDATE في الداتابيز
                    PrescriptionItem db = dao.updateItem(c, editingRow.getId(), medId, medName, ds.toString(), qty);

                    // عكس التحديث على UI
                    editingRow.setMedicineId(db.getMedicineId() == null ? 0 : db.getMedicineId());
                    editingRow.setMedicineName(db.getMedicineName());
                    editingRow.setDosage(db.getDosage());
                    editingRow.setDurationDays(days);
                    editingRow.setQuantity(db.getQuantity());
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

    /** Open Add pane with the selected row prefilled to edit. */
    private void openEditSelectedItem() {
        PrescItemRow sel = (TablePrescriptionItems == null) ? null : TablePrescriptionItems.getSelectionModel().getSelectedItem();
        if (sel == null) {
            toast("Select a row to edit.", "warn");
            return;
        }
        editingRow = sel;
        // Prefill fields we can reliably map
        if (medicineName != null) medicineName.setText(sel.getMedicineName());
        if (duration != null)     duration.setText(String.valueOf(Math.max(0, sel.getDurationDays())));
        // Try estimate freq/day if possible: qty / days (integer)
        if (freq_day != null) {
            int days = Math.max(1, sel.getDurationDays());
            int q = Math.max(0, sel.getQuantity());
            int freq = (q % days == 0) ? (q / days) : 0;
            freq_day.setText(freq > 0 ? String.valueOf(freq) : "");
        }
        // doseText/strength/form/route cannot be reconstructed strictly from 'dosage' string reliably
        // leave them to user if needed.

        if (InsertMedicine != null) InsertMedicine.setText("Save");
        showPrescriptionPaneToAddMedication();
    }

    /** Delete selected row; if it has a DB id (>0), delete from DB too. */
    /** Delete selected row; if it has a DB id (>0), delete from DB too. */
    private void deleteSelectedItem() {
        PrescItemRow sel = (TablePrescriptionItems == null) ? null : TablePrescriptionItems.getSelectionModel().getSelectedItem();
        if (sel == null) {
            toast("Select a row to delete.", "warn");
            return;
        }
        boolean removed = prescItemsEditable.remove(sel);
        if (!removed) {
            toast("Could not remove the row.", "warn");
            return;
        }
        if (sel.getId() > 0) {
            new Thread(() -> {
                try (Connection c = Database.get()) {
                    c.setAutoCommit(true);
                    new PrescriptionItemDAO().deleteById(c, sel.getId());
                    Platform.runLater(() -> {
                        sel.setId(0); // صار مش محفوظ
                        toast("Row deleted from database.", "ok");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> toast("Deleted locally but DB delete failed.", "warn"));
                }
            }, "delete-presc-item").start();
        } else {
            toast("Row removed.", "ok");
        }
    }
    /** Clear Add-Medicine form fields. */
    private void clearAddForm() {
        if (medicineName != null) medicineName.clear();
        if (doseText != null) doseText.clear();
        if (freq_day != null) freq_day.clear();
        if (duration != null) duration.clear();
        if (medicalHistory1 != null) medicalHistory1.clear();
        if (formCombo != null) formCombo.getSelectionModel().clearSelection();
        if (routeCombo != null) routeCombo.getSelectionModel().clearSelection();
        if (strength_combo != null) strength_combo.getSelectionModel().clearSelection();
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
                case "ok"   -> alertLabel.setStyle(base + "-fx-background-color: #28a745;");
                case "warn" -> alertLabel.setStyle(base + "-fx-background-color: #ffc107; -fx-text-fill: #222;");
                case "err"  -> alertLabel.setStyle(base + "-fx-background-color: #dc3545;");
                default     -> alertLabel.setStyle(base + "-fx-background-color: #17a2b8;");
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

    private static int safeParseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
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
