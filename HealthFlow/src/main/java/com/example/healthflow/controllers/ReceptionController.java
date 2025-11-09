package com.example.healthflow.controllers;

import com.example.healthflow.dao.AppointmentJdbcDAO;
import com.example.healthflow.db.Database;
import com.example.healthflow.dao.DoctorDAO;
import com.example.healthflow.model.Appointment;
import com.example.healthflow.model.DoctorRow;
import com.example.healthflow.model.PatientRow;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.service.AuthService.Session;
import com.example.healthflow.model.Appointment.ApptRow;
import com.example.healthflow.service.PatientService;
import com.example.healthflow.ui.ConfirmDialog;
import com.example.healthflow.ui.ConnectivityBanner;
import com.example.healthflow.ui.OnlineBindings;

import com.example.healthflow.db.notify.DbNotifications;
import com.example.healthflow.ui.fx.RefreshScheduler;
import com.example.healthflow.ui.fx.TableUtils;
import static com.example.healthflow.ui.base.Dialogs.error;
//import static jdk.internal.org.commonmark.text.Characters.isBlank;

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
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import com.example.healthflow.ui.ComboAnimations;

import javafx.scene.control.TextField;


import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TablePosition;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;



public class ReceptionController {
    /* ============ UI ============ */
    @FXML private Button LogOutBtn;


    @FXML private AnchorPane DashboardAnchorPane;
    @FXML private AnchorPane PatientAnchorPane;
    @FXML private AnchorPane AppointmentsAnchorPane;
    @FXML private AnchorPane DoctorAnchorPane;
    @FXML private StackPane rootPane;
    @FXML private Button DachboardButton;
    @FXML private Button PatientsButton;
    @FXML private Button AppointmentsButton;
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
    @FXML private TextArea medicalHistory;


    @FXML private Button InsertButton;
    @FXML private Button UpdateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearBtn;


    @FXML private TextField search;
    @FXML private Button clearSelectionPatient;
    @FXML private TableView<PatientRow> patientTable;
    @FXML private TableColumn<PatientRow, Integer> colPatientsSerial;
    @FXML private TableColumn<PatientRow, String> colNationalId;
    @FXML private TableColumn<PatientRow, String> colName;
    @FXML private TableColumn<PatientRow, String> colGender;
    @FXML private TableColumn<PatientRow, LocalDate> colDob;
    @FXML private TableColumn<PatientRow, String> colPhoneNumber;
    @FXML private TableColumn<PatientRow, String> colMedicalHistory;

    @FXML private Label NumberOfTotalAppointments;
    @FXML private Label NumberOfTotalDoctors;
    @FXML private Label NumberOfTotalPatients;
    @FXML private Label patientCompleteNum;
    @FXML private Label RemainingNum;



    @FXML private Circle ActiveStatus;
    @FXML private AnchorPane Appointments;
    @FXML private AnchorPane CenterAnchorPane;
    @FXML private AnchorPane Doctors;
    @FXML private AnchorPane Patients;
    @FXML private AnchorPane reportAnchor;

    @FXML private Label TotalAppointments;
    @FXML private Label TotalDoctors;

    @FXML private TableView<DoctorDAO.AppointmentRow> TableAppInDashboard;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, Number> colAppointmentID;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, Void> colActionDash;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, LocalDate> colAppintementDateDash;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, String> colAppintementTimeDash;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, String> colDoctorNameDash;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, String> colPatientNameDash;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, String> colSpecialtyDash;
    @FXML private TableColumn<DoctorDAO.AppointmentRow, String> colRoomDash;

    @FXML private DatePicker dataPickerDashboard;
    @FXML private BarChart<String, Number> appointmentStatusChart;
    @FXML private Button clearSelectionDach;
    @FXML private TextField searchAppointmentDach;
    @FXML private TextField searchDoctor;
    @FXML private Button clearSelectionDoctor;
    @FXML private Button insertAppointments;
    @FXML private Label TotalPatients;
    @FXML private Button BookAppointmentFromPateint;
    @FXML private Button updateAppointments;

    @FXML private ComboBox<String> DoctorspecialtyApp;             // list of specialties
    @FXML private ComboBox<DoctorDAO.DoctorOption> avilabelDoctorApp; // available doctors for selected specialty
    @FXML private Button clear_Appointments;
    @FXML private Button deleteAppointments;

    @FXML private DatePicker dataPickerAppointment;
    @FXML private TableView<ApptRow> TableINAppointment;
    @FXML private TableColumn<ApptRow, Number> colAppointmentSerialNUm;
    @FXML private TableColumn<ApptRow, LocalDate> colDateAppointment;
    @FXML private TableColumn<ApptRow, String> colDoctorNameAppointment;
    @FXML private TableColumn<ApptRow, String> colPatientNameAppointment;
    @FXML private TableColumn<ApptRow, String> colSpecialty;
    @FXML private TableColumn<ApptRow, String> colStatusAppointment;
    @FXML private TableColumn<ApptRow, String> colStartTime;
    @FXML private TableColumn<ApptRow, Number> colSessionTime;
    @FXML private TableColumn<ApptRow, String> colRoomNumber;

    @FXML private Label LabelToAlert;

    @FXML private Label getPatientName;
    @FXML private Label getPatientID;

    // ===== Doctors table =====
    @FXML private TableView<DoctorRow> DocTable_Recption;

    @FXML private TableColumn<DoctorRow, Integer> colDoctorSerial;
    @FXML private TableColumn<DoctorRow, String> colDoctor_name;
    @FXML private TableColumn<DoctorRow, String> colDoctor_Gender;
    @FXML private TableColumn<DoctorRow, String> colDoctor_Phone;
    @FXML private TableColumn<DoctorRow, String> colDoctor_Specialty;
    @FXML private TableColumn<DoctorRow, String> colDoctor_bio;
    @FXML private TableColumn<DoctorRow, String> colDoctor_Status;
    @FXML private TableColumn<DoctorRow, String> colDocRoomNumber;
    @FXML private TableColumn<DoctorRow, Boolean> colDoctor_available;

    @FXML
    private DatePicker AppointmentDate;
    @FXML
    private TextField appointmentSetTime;
    @FXML
    private TextField PatientNameForAppointment;
    @FXML
    private TextField PatientIDForAppointment;
    @FXML
    private ComboBox<DoctorDAO.Slot> cmbSlots;
    @FXML private Label AppointmentDateDetailes;

    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> statusFilterDashboard; // لو معرف في FXML تجاهل هذا السطر

    private javafx.collections.ObservableList<com.example.healthflow.dao.DoctorDAO.AppointmentRow> dashBase =
            javafx.collections.FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<com.example.healthflow.dao.DoctorDAO.AppointmentRow> dashFiltered =
            new javafx.collections.transformation.FilteredList<>(dashBase, r -> true);
    private javafx.collections.transformation.SortedList<com.example.healthflow.dao.DoctorDAO.AppointmentRow> dashSorted =
            new javafx.collections.transformation.SortedList<>(dashFiltered);

    private final ObservableList<DoctorDAO.AppointmentRow> apptData = FXCollections.observableArrayList();

    // Dashboard filtering helpers
    private FilteredList<DoctorDAO.AppointmentRow> filteredDash = new FilteredList<>(apptData, r -> true);
    private SortedList<DoctorDAO.AppointmentRow> sortedDash = new SortedList<>(filteredDash);

    // Editable list for the appointments table in the Appointment pane
    private ObservableList<ApptRow> apptEditable = FXCollections.observableArrayList();
    // Filtering helpers for the Appointments table (Appointments pane)
    private FilteredList<ApptRow> filteredAppt = new FilteredList<>(apptEditable, r -> true);
    private SortedList<ApptRow> sortedAppt = new SortedList<>(filteredAppt);
    // Caches/edit choices for in-row editors
    private final ObservableList<String> specialtyChoices = FXCollections.observableArrayList();
    private final Map<String, ObservableList<DoctorDAO.DoctorOption>> doctorsBySpec = new ConcurrentHashMap<>();
    private static final DateTimeFormatter DATE_FMT_HUMAN = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy");
    private static final java.time.ZoneId APP_ZONE = java.time.ZoneId.of("Asia/Gaza");
    private static final java.time.ZoneId APP_TZ = java.time.ZoneId.of("Asia/Gaza");
    // --- Lightweight change tracking (fallback if NOTIFY is missed) ---
    private volatile java.sql.Timestamp lastApptTs = null;
    private volatile java.sql.Timestamp lastPatientTs = null;
    // Cache: appointment.id -> patient's national_id (or fallback patient id)
    private final java.util.concurrent.ConcurrentHashMap<Long, String> apptPatientIdCache
            = new java.util.concurrent.ConcurrentHashMap<>();

    private volatile boolean apptLoading = false;
    private static final java.time.format.DateTimeFormatter UI_DATE =
            java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");


    private final AtomicBoolean refreshBusy = new AtomicBoolean(false);

    public static final int DEFAULT_SESSION_MIN = 20;
    // --- Coalesced UI refresh + DB NOTIFY ---
    private final RefreshScheduler uiRefresh = new RefreshScheduler(600);
    private DbNotifications apptDbListener;

    // To color current nav button
    private static final String ACTIVE_CLASS = "current";
    private static final DateTimeFormatter SLOT_FMT_12H = DateTimeFormatter.ofPattern("hh:mm a");

    /* ============ Types ============ */
    public enum Gender {MALE, FEMALE}

    /* ============ State ============ */
    private final ObservableList<PatientRow> patientData = FXCollections.observableArrayList();
    private FilteredList<PatientRow> filtered;

    private final ObservableList<DoctorRow> doctorData = FXCollections.observableArrayList();
    private FilteredList<DoctorRow> doctorFiltered;

    private final PatientService patientService = new PatientService();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final String cssPath = "/com/example/healthflow/Design/ReceptionDesign.css";

    // When booking from patient, auto-pick nearest future slot once
    private volatile boolean selectNearestSlotOnNextRefresh = false;

    /* ============ Connectivity ============ */
    private final ConnectivityMonitor monitor;
    private static volatile boolean listenerRegistered = false;
    private static volatile Boolean lastNotifiedOnline = null;

    // Cache currently-selected patient to survive pane switches / refreshes
    private PatientRow selectedPatient;
    private boolean patientSelHooked = false;

    private final Deque<AnchorPane> navigationHistory = new ArrayDeque<>();

    // Gate for AppointmentsButton (avoid setDisable on a bound property)
    private javafx.beans.property.BooleanProperty appointmentsAccess =
            new javafx.beans.property.SimpleBooleanProperty(false);

    // helpers:
    private static java.time.OffsetDateTime toAppOffset(java.time.LocalDate d, java.time.LocalTime t) {
        return java.time.ZonedDateTime.of(d, t, APP_ZONE).toOffsetDateTime();
    }

    private static java.time.LocalDateTime toLocal(java.time.OffsetDateTime odt) {
        return odt == null ? null : odt.atZoneSameInstant(APP_ZONE).toLocalDateTime();
    }

    // ===== Transient banner on LabelToAlert (with fade) =====
    private void showToast(String kind, String msg) {
        Platform.runLater(() -> {
            if (LabelToAlert == null) return;
            String icon, style;
            switch (kind == null ? "info" : kind.toLowerCase()) {
                case "error":
                    icon = "❌ ";
                    style = "-fx-background-color:#ffe6e6; -fx-text-fill:#b00020; -fx-border-color:#b00020; "
                            + "-fx-border-radius:6; -fx-background-radius:6; -fx-padding:6 10; -fx-font-weight:bold;";
                    break;
                case "warn":
                    icon = "⚠️ ";
                    style = "-fx-background-color:#fff4e5; -fx-text-fill:#8a6d3b; -fx-border-color:#f0ad4e; "
                            + "-fx-border-radius:6; -fx-background-radius:6; -fx-padding:6 10; -fx-font-weight:bold;";
                    break;
                case "success":
                    icon = "✅ ";
                    style = "-fx-background-color:#e8f5e9; -fx-text-fill:#2e7d32; -fx-border-color:#66bb6a; "
                            + "-fx-border-radius:6; -fx-background-radius:6; -fx-padding:6 10; -fx-font-weight:bold;";
                    break;
                default:
                    icon = "ℹ️ ";
                    style = "-fx-background-color:#e6f0ff; -fx-text-fill:#1a4fb3; -fx-border-color:#1a4fb3; "
                            + "-fx-border-radius:6; -fx-background-radius:6; -fx-padding:6 10; -fx-font-weight:bold;";
            }
            LabelToAlert.setStyle(style);
            LabelToAlert.setText(icon + (msg == null ? "" : msg));
            LabelToAlert.setOpacity(0);
            LabelToAlert.setVisible(true);

            var fadeIn = new javafx.animation.FadeTransition(Duration.millis(250), LabelToAlert);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            var pause = new javafx.animation.PauseTransition(Duration.seconds(4));
            pause.setOnFinished(ev -> {
                var fadeOut = new javafx.animation.FadeTransition(Duration.millis(300), LabelToAlert);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(__ -> {
                    LabelToAlert.setVisible(false);
                    LabelToAlert.setText("");
                    LabelToAlert.setOpacity(1);
                });
                fadeOut.play();
            });
            pause.play();
        });
    }

    private boolean isPastDate(LocalDate d) {
        return d != null && d.isBefore(LocalDate.now());
    }

    // enforce UI rules when date changes (disable time controls for past dates)
    private void enforceDateRules() {
        LocalDate day = (AppointmentDate == null) ? null : AppointmentDate.getValue();
        boolean past = isPastDate(day);
        if (cmbSlots != null) {
            cmbSlots.setDisable(past);
            if (past) cmbSlots.getItems().clear();
        }
        if (past) {
            showToast("error", "The selected date is in the past. Please choose today or a future date.");
        }
        if (TableINAppointment != null) TableINAppointment.refresh();
    }
    private final ScheduledExecutorService autoRefreshExec =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ui-auto-refresh");
                t.setDaemon(true);
                return t;
            });

    private void startDbNotificationsSafe() {
        try {
            // Recreate listener to avoid stale/closed connections
            stopDbNotifications();
            apptDbListener = new DbNotifications();

            // --- Appointments channel ---
            apptDbListener.listen("appointments_changed", payload -> {
                System.out.println("[ReceptionController] NOTIFY appointments_changed payload=" + payload);
                slotCache.clear();
                Platform.runLater(() -> scheduleCoalescedRefresh());
            });

            // --- Patients channel ---
            apptDbListener.listen("patients_changed", payload -> {
                System.out.println("[ReceptionController] NOTIFY patients_changed payload=" + payload);
                Platform.runLater(() -> loadPatientsBG());
                uiRefresh.request(() -> {
//                    loadPatientsBG();
                    ensureTableBindings();
                    scheduleCoalescedRefresh();
                });
            });

            // Some implementations auto-start on first listen; no explicit start() available
            System.out.println("[ReceptionController] DbNotifications wired.");
        } catch (Throwable t) {
            System.err.println("[ReceptionController] startDbNotificationsSafe error: " + t);
        }

        // Ensure we only refresh on NOTIFY (no timers)
        disableAutoRefreshPeriodic();
    }

    private void onAppointmentsDbEvent(String payload) {
        // Unified, debounced refresh
        scheduleCoalescedRefresh();
    }


    private void stopDbNotifications() {
        try {
            if (apptDbListener != null) {
                apptDbListener.close();
                apptDbListener = null;
            }
        } catch (Exception ignore) {
        }
    }


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


    // Cache for free slots per doctor/day to make row selection instant
    private final Map<Long, Map<LocalDate, ObservableList<DoctorDAO.Slot>>> slotCache = new ConcurrentHashMap<>();


    // Best-effort: cancel pending debounced refresh if the implementation exposes such method
    private void cancelPendingUiRefresh() {
        if (uiRefresh == null) return;
        try {
            var m = uiRefresh.getClass().getMethod("cancelPending");
            m.invoke(uiRefresh);
        } catch (Exception ignore) {
            // no-op if not supported
        }
    }

    // ========== DASHBOARD TABLE RELOAD ==========
    // This is the method referenced in the instruction (see log message)


    /* ============ slot load ============ */

    // إرجاع الغرف المتاحة (Room 1..Room 9) مع استبعاد المحجوزة
    private List<String> listAvailableRooms(long doctorId, LocalDate date, LocalTime time) throws SQLException {
        final int TOTAL_ROOMS = 9;
        // كل الغرف الافتراضية
        List<String> all = new ArrayList<>();
        for (int i = 1; i <= TOTAL_ROOMS; i++) all.add("Room " + i);

        if (doctorId <= 0 || date == null || time == null) return all;

//        long doctorId = doctorDAO.findIdByName(doctorName);

        // الغرف المحجوزة لهذه اللحظة لهذا الطبيب
        final String sql = """
            SELECT COALESCE(location,'') AS loc
            FROM appointments
            WHERE doctor_id = ?
              AND appointment_date::date = ?
              AND appointment_date::time = ?
        """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, doctorId);   // لا تستخدم الاسم هنا
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(time));

            try (ResultSet rs = ps.executeQuery()) {
                Set<String> taken = new HashSet<>();
                while (rs.next()) {
                    String loc = rs.getString("loc");
                    if (loc != null && !loc.isBlank()) taken.add(loc);
                }
                all.removeAll(taken);
                return all;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return all; // fallback لو صار خطأ
        }
    }





    public ReceptionController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }

    public ReceptionController() {
        this(new ConnectivityMonitor());
    }

    private void updateAppointmentDetailsLabel(Appointment.ApptRow row) {
        if (AppointmentDateDetailes == null) return;
        if (row == null || row.getDate() == null || row.getTime() == null) {
            AppointmentDateDetailes.setText("");
            AppointmentDateDetailes.setStyle("");
            return;
        }
        int mins = row.getSessionTime() > 0 ? row.getSessionTime() : DEFAULT_SESSION_MIN;
        LocalDate d = row.getDate();
        LocalTime start = row.getTime();
        LocalTime end = start.plusMinutes(mins);
        String txt = d.format(DATE_FMT_HUMAN) + " — " +
                start.format(SLOT_FMT_12H) + " \u2192 " + end.format(SLOT_FMT_12H) +
                " (" + mins + " min)";
        AppointmentDateDetailes.setText(txt);
        AppointmentDateDetailes.setStyle(
                "-fx-background-color:#e6f0ff; -fx-text-fill:#1a4fb3; " +
                        "-fx-border-color:#1a4fb3; -fx-border-radius:6; -fx-background-radius:6; " +
                        "-fx-padding:4 8; -fx-font-weight:bold;"
        );
    }

    // داخل ReceptionController (أو المكان اللي مخصص لتهيئة الأعمدة)
    private TableCell<Appointment.ApptRow, LocalDate> datePickerCell() {
        return new TableCell<Appointment.ApptRow, LocalDate>() {
            private final DatePicker picker = new DatePicker();

            {
                // شكليّات + إصلاحات
                picker.setEditable(true);
                picker.setPromptText("yyyy-MM-dd");
                picker.getStyleClass().addAll("table-cell","box");


                // فور اختيار تاريخ جديد
                picker.setOnAction(e -> {
                    var rowItem = getTableRow() != null ? getTableRow().getItem() : null;
                    LocalDate d = picker.getValue();
                    if (rowItem == null || d == null) return;

                    // حدّث الموديل
                    rowItem.setDate(d);

                    // لو فيه وقت محدد، حدّث الـ start في الداتابيز
                    if (rowItem.getTime() != null && rowItem.getId() > 0) {
                        try {
                            updateAppointmentStartAt(rowItem.getId(), d, rowItem.getTime());
                        } catch (Exception ex) {
                            showError("Update date/time", ex);
                        }
                    }

                    // ريفرش جدول/أوقات
                    commitEdit(d);
                    if (TableINAppointment != null) TableINAppointment.refresh();
                    updateAppointmentDetailsLabel(rowItem);  // أو المتغير المحلي للصف
//                    updateDirtyAlert();
                });
                // افتح الـ DatePicker عند بداية التحرير
                this.setOnMouseClicked(me -> {
                    if (!isEmpty() && me.getClickCount() == 1) {
                        startEdit();
                        picker.show();
                    }
                });
            }
            @Override
            public void startEdit() {
                super.startEdit();
                picker.setValue(getItem());
                setGraphic(picker);
                setText(null);
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setGraphic(null);
                setText(format(getItem()));
            }

            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else if (isEditing()) {
                    picker.setValue(item);
                    setGraphic(picker);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(format(item));
                }
            }

            private String format(LocalDate d) {
                return (d == null) ? "" : d.toString(); // بدك فورمات معيّن؟ استعمل DateTimeFormatter
            }
        };
    }

    private TableCell<Appointment.ApptRow, String> doctorComboCell() {
        return new TableCell<Appointment.ApptRow, String>() {
            private final ComboBox<DoctorDAO.DoctorOption> combo = new ComboBox<>();

            {
                combo.setVisibleRowCount(8);
                combo.setPromptText("Select doctor");
                combo.setCellFactory(list -> new ListCell<>() {
                    @Override
                    protected void updateItem(DoctorDAO.DoctorOption item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.fullName + "  (Room: " + item.roomNumber + ")");
                    }
                });
                combo.setButtonCell(new ListCell<>() {
                    @Override
                    protected void updateItem(DoctorDAO.DoctorOption item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.fullName + "  (Room: " + item.roomNumber + ")");
                    }
                });
                setOnMouseClicked(e -> {
                    if (!isEmpty() && getTableRow() != null && getTableRow().isSelected()) {
                        startEdit();
                        combo.show();
                    }
                });
                combo.setOnShown(e -> {
                    var rowItem = (getTableRow() == null) ? null : getTableRow().getItem();
                    if (rowItem == null) return;
                    String spec = rowItem.getSpecialty();
                    java.util.List<DoctorDAO.DoctorOption> opts;
                    try {
                        opts = doctorDAO.listAvailableBySpecialty(spec);
                    } catch (Exception ex) {
                        opts = java.util.Collections.emptyList();
                    }
                    combo.setItems(FXCollections.observableArrayList(opts));
                    // اختَر الحالي إن كان مضبوطًا
                    if (rowItem.getDoctorId() > 0) {
                        for (var o : opts)
                            if (o.doctorId == rowItem.getDoctorId()) {
                                combo.getSelectionModel().select(o);
                                break;
                            }
                    }
                });
                combo.setOnAction(e -> {
                    var rowItem = (getTableRow() == null) ? null : getTableRow().getItem();
                    var opt = combo.getValue();
                    if (rowItem == null || opt == null) return;
                    rowItem.setDoctorId(opt.doctorId);
                    rowItem.setDoctorName(opt.fullName);
                    rowItem.setRoomNumber(opt.roomNumber);
                    rowItem.setDirty(true);
                    commitEdit(opt.fullName);
                    if (TableINAppointment != null) TableINAppointment.refresh();
                    updateAppointmentDetailsLabel(rowItem);
                });
            }
            @Override
            public void startEdit() {
                super.startEdit();
                setGraphic(combo);
                setText(null);
            }
            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setGraphic(null);
                setText(getItem());
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean showEditor = isEditing() && getTableRow() != null && getTableRow().isSelected();
                if (showEditor) {
                    setGraphic(combo);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(item);
                }
            }
        };
    }


    private void refreshSlots() {
        if (cmbSlots == null) return;

        var doc = (avilabelDoctorApp == null) ? null : avilabelDoctorApp.getValue();
        var day = (AppointmentDate == null) ? null : AppointmentDate.getValue();

        // لا طبيب/لا تاريخ → افرغ وأخرج
        if (doc == null || day == null) {
            cmbSlots.getItems().clear();
            cmbSlots.getSelectionModel().clearSelection();
            cmbSlots.setValue(null);
            cmbSlots.setPromptText("Select time");
            return;
        }

        // تاريخ ماضي → عطل التحكم وافرغ
        if (isPastDate(day)) {
            cmbSlots.setDisable(true);
            cmbSlots.getItems().clear();
            cmbSlots.getSelectionModel().clearSelection();
            cmbSlots.setValue(null);
            showToast("error", "The selected date is in the past. Please choose today or a future date.");
            return;
        }

        final LocalTime open  = LocalTime.of(9, 0);
        final LocalTime close = LocalTime.of(15, 0);
        final int slotMinutes = DEFAULT_SESSION_MIN; // 20 min

        new Thread(() -> {
            try {
                var slots = doctorDAO.listFreeSlots(doc.doctorId, day, open, close, slotMinutes);

                // أمان إضافي: استبعد أي فتحة تبدأ عند/بعد الإغلاق أو تنتهي عند/بعد الإغلاق
                slots.removeIf(s -> {
                    LocalTime fromT = s.from().toLocalTime();
                    LocalTime toT   = s.to().toLocalTime();
                    return !fromT.isBefore(close) || !toT.isBefore(close);
                });

                // لليوم الحالي فقط بحسب منطقة التطبيق
                if (day.equals(LocalDate.now(APP_TZ))) {
                    var nowZ = java.time.ZonedDateTime.now(APP_TZ).withSecond(0).withNano(0);
                    var now  = nowZ.toLocalDateTime();
                    int mod  = now.getMinute() % slotMinutes;
                    var cutoff = (mod == 0) ? now : now.plusMinutes(slotMinutes - mod);
                    slots.removeIf(s -> s.from().isBefore(cutoff));

                    // لو الدوام خلص اليوم
                    if (now.toLocalTime().isAfter(close)) {
                        Platform.runLater(() -> {
                            cmbSlots.getItems().clear();
                            cmbSlots.getSelectionModel().clearSelection();
                            cmbSlots.setValue(null);
                            cmbSlots.setPromptText("Working hours are over");
                            showToast("info", "Clinic working hours are over for today.");
                        });
                        return; // أوقف المعالجة لليوم الحالي
                    }
                }

                var data = FXCollections.observableArrayList(slots);
                Platform.runLater(() -> {
                    cmbSlots.setDisable(false);
                    cmbSlots.setItems(data);

                    if (selectNearestSlotOnNextRefresh && !data.isEmpty()) {
                        // في مسار "Book from Patient" فقط
                        cmbSlots.getSelectionModel().select(0);
                        selectNearestSlotOnNextRefresh = false; // one-shot
                    } else {
                        // لا اختيار افتراضي
                        cmbSlots.getSelectionModel().clearSelection();
                        cmbSlots.getSelectionModel().select(-1); // defensive
                        cmbSlots.setValue(null);
                        cmbSlots.setPromptText(data.isEmpty() ? "No available times" : "Select time");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showWarn("Slots", "Failed to load free slots: " + e.getMessage()));
            }
        }, "load-slots").start();
    }

    private static String fmt12(LocalTime t) {
        return t == null ? "" : t.format(SLOT_FMT_12H);
    }

    /* ============ Clock (12h) ============ */
    private void startClock() {
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        Timeline tl = new Timeline(new KeyFrame(Duration.ZERO, e -> time.setText(LocalTime.now().format(tf))),
                new KeyFrame(Duration.seconds(1)));
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


    /* ============ Panes ============ */
    private void showDashboardPane() {
        disableAppointmentsAccess();
        DashboardAnchorPane.setVisible(true);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(false);
        DoctorAnchorPane.setVisible(false);
        markNavActive(DachboardButton);
    }

    private void showDoctorPane() {
        disableAppointmentsAccess();
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(false);
        DoctorAnchorPane.setVisible(true);
        markNavActive(DoctorsButton);
    }

    private void showPatientsPane() {
        disableAppointmentsAccess();
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(true);
        AppointmentsAnchorPane.setVisible(false);
        DoctorAnchorPane.setVisible(false);
        markNavActive(PatientsButton);
    }
    @FXML
    private void showPatientsPaneAction() {
        disableAppointmentsAccess();
        switchPane(PatientAnchorPane);
        markNavActive(PatientsButton);
    }

    private void showAppointmentPane() {
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(true);
        DoctorAnchorPane.setVisible(false);
        enableAppointmentsAccess();
        markNavActive(AppointmentsButton);

    }

    /* ============ Patients: table & search ============ */
    private void wirePatientTable() {
        colPatientsSerial.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<Integer>(patientTable.getItems().indexOf(cd.getValue()) + 1)
        );
        colNationalId.setCellValueFactory(cd -> cd.getValue().nationalIdProperty());
        colName.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
        colGender.setCellValueFactory(cd -> cd.getValue().genderProperty());
        colDob.setCellValueFactory(cd -> cd.getValue().dateOfBirthProperty());
        colPhoneNumber.setCellValueFactory(cd -> cd.getValue().phoneProperty());
        colMedicalHistory.setCellValueFactory(cd -> cd.getValue().medicalHistoryProperty());
        patientTable.setItems(patientData);
//        patientTable.setEditable(true);
        setupPatientInlineEditing();

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
                    contains(p.getFullName(), s) || contains(p.getGender(), s) ||
                            contains(p.getPhone(), s) || contains(p.getNationalId(), s) ||
                            contains(p.getMedicalHistory(), s) ||
                            (p.getDateOfBirth() != null && p.getDateOfBirth().toString().toLowerCase().contains(s)));
        });
        SortedList<PatientRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(patientTable.comparatorProperty());
        patientTable.setItems(sorted);
    }

    private void setupPatientInlineEditing() {
        // Full Name -> users.full_name
        colName.setCellFactory(TextFieldTableCell.forTableColumn());
        colName.setOnEditCommit(ev -> {
            PatientRow row = ev.getRowValue();
            String v = safe(ev.getNewValue());
            if (v == null) return;
            row.setFullName(v);
            updateUserText(row.getUserId(), "full_name", v);
            notifyPatientsChanged();
        });

        // Phone -> users.phone
        colPhoneNumber.setCellFactory(TextFieldTableCell.forTableColumn());
        colPhoneNumber.setOnEditCommit(ev -> {
            PatientRow row = ev.getRowValue();
            String v = safe(ev.getNewValue());
            if (v == null) return;
            row.setPhone(v);
            updateUserText(row.getUserId(), "phone", v);
            notifyPatientsChanged();
        });

        // Gender -> users.gender (ComboBox MALE/FEMALE)
        colGender.setCellFactory(ComboBoxTableCell.forTableColumn(
                FXCollections.observableArrayList("MALE", "FEMALE")
        ));
        colGender.setOnEditCommit(ev -> {
            PatientRow row = ev.getRowValue();
            String v = safe(ev.getNewValue());
            if (v == null) return;
            row.setGender(v);
            updateUserText(row.getUserId(), "gender", v);
            notifyPatientsChanged();
        });

        // National ID -> patients.national_id
        colNationalId.setCellFactory(TextFieldTableCell.forTableColumn());
        colNationalId.setOnEditCommit(ev -> {
            PatientRow row = ev.getRowValue();
            String v = safe(ev.getNewValue());
            if (v == null) return;
            row.setNationalId(v);
            updatePatientText(row.getPatientId(), "national_id", v);
            notifyPatientsChanged();
        });

        // Date of Birth -> patients.date_of_birth (نستخدم TextFieldTableCell مع محوّل بسيط yyyy-MM-dd)
        colDob.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : d.toString();
            }

            @Override
            public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) return null;
                return LocalDate.parse(s.trim()); // صيغة: 2025-10-10
            }
        }));
        colDob.setOnEditCommit(ev -> {
            PatientRow row = ev.getRowValue();
            LocalDate d = ev.getNewValue();
            if (d == null) return;
            row.setDateOfBirth(d);
            updatePatientDate(row.getPatientId(), "date_of_birth", d);
            notifyPatientsChanged();
        });

        // Medical History -> patients.medical_history
        colMedicalHistory.setCellFactory(TextFieldTableCell.forTableColumn());
        colMedicalHistory.setOnEditCommit(ev -> {
            PatientRow row = ev.getRowValue();
            String v = safe(ev.getNewValue());
            if (v == null) v = ""; // نسمح بقيمة فارغة
            row.setMedicalHistory(v);
            updatePatientText(row.getPatientId(), "medical_history", v);
            notifyPatientsChanged();
        });
    }

    private void updateUserText(long userId, String column, String value) {
        final String sql;
        if ("gender".equalsIgnoreCase(column)) {
            sql = "UPDATE users SET gender = ?::gender_type WHERE id = ?";
        } else {
            sql = "UPDATE users SET " + column + " = ? WHERE id = ?";
        }
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            showError("Update user." + column, e);
        }
    }

    private void updatePatientText(long patientId, String column, String value) {
        final String sql = "UPDATE patients SET " + column + " = ? WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setLong(2, patientId);
            ps.executeUpdate();
        } catch (SQLException e) {
            showError("Update patient." + column, e);
        }
    }

    private void updatePatientDate(long patientId, String column, LocalDate d) {
        final String sql = "UPDATE patients SET " + column + " = ? WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(d));
            ps.setLong(2, patientId);
            ps.executeUpdate();
        } catch (SQLException e) {
            showError("Update patient." + column, e);
        }
    }

    private void notifyPatientsChanged() {
        try (Connection c = Database.get();
             PreparedStatement nps = c.prepareStatement("SELECT pg_notify('patients_changed','update')")) {
            nps.execute();
        } catch (SQLException e) {
            // مش حرجة لو فشلت النوتيفاي، بس نطبع للتشخيص
            e.printStackTrace();
        }
    }


    private String safe(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean contains(String v, String q) {
        return v != null && v.toLowerCase().contains(q);
    }

    // === Time validation (Asia/Gaza) ===
    private boolean isPastStart(LocalDate d, LocalTime t) {
        if (d == null || t == null) return false;
        var now = java.time.ZonedDateTime.now(APP_TZ).withSecond(0).withNano(0);
        var chosen = java.time.ZonedDateTime.of(d, t, APP_TZ);
        return chosen.isBefore(now);
    }


    private void loadPatientsBG() {
        try {
            var list = patientService.listPatients(); // List<PatientRow>
            Platform.runLater(() -> {
                patientData.clear();
                patientData.addAll(list);
            });
        } catch (Exception ex) {
            Platform.runLater(() -> showError("Load Patients", ex));
        }
    }

    /* ============ Doctors: table, search, load ============ */
    private void wireDoctorTable() {
        colDoctorSerial.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<Integer>(DocTable_Recption.getItems().indexOf(cd.getValue()) + 1)
        );
        if (colDoctor_name != null) colDoctor_name.setCellValueFactory(cd -> cd.getValue().fullNameProperty());
        if (colDoctor_Gender != null) colDoctor_Gender.setCellValueFactory(cd -> cd.getValue().genderProperty());
        if (colDoctor_Phone != null) colDoctor_Phone.setCellValueFactory(cd -> cd.getValue().phoneProperty());
        if (colDoctor_Specialty != null)
            colDoctor_Specialty.setCellValueFactory(cd -> cd.getValue().specialtyProperty());
        if (colDoctor_bio != null) colDoctor_bio.setCellValueFactory(cd -> cd.getValue().bioProperty());
        if (colDoctor_Status != null) colDoctor_Status.setCellValueFactory(cd -> cd.getValue().statusTextProperty());
        if (colDoctor_available != null)
            colDoctor_available.setCellValueFactory(cd -> cd.getValue().availableProperty());
        if (colDocRoomNumber != null) colDocRoomNumber.setCellValueFactory(cd -> cd.getValue().roomNumberProperty());
        if (DocTable_Recption != null) DocTable_Recption.setItems(doctorData);
    }

    private void wireSearchDoctors() {
        doctorFiltered = new FilteredList<>(doctorData, d -> true);
        if (searchDoctor != null) {
            searchDoctor.textProperty().addListener((obs, old, q) -> {
                String s = (q == null) ? "" : q.trim().toLowerCase();
                if (s.isEmpty()) doctorFiltered.setPredicate(d -> true);
                else doctorFiltered.setPredicate(d ->
                        contains(d.getFullName(), s) || contains(d.getGender(), s) ||
                                contains(d.getPhone(), s) || contains(d.getSpecialty(), s) ||
                                contains(d.getBio(), s) || contains(d.getStatusText(), s));
            });
        }
        if (DocTable_Recption != null) {
            SortedList<DoctorRow> sorted = new SortedList<>(doctorFiltered);
            sorted.comparatorProperty().bind(DocTable_Recption.comparatorProperty());
            DocTable_Recption.setItems(sorted);
        }
    }

    private void setupDoctorFilters() {
        if (DoctorspecialtyApp != null) {
            DoctorspecialtyApp.setPromptText("Select specialty");
            loadSpecialtiesAsync();
            DoctorspecialtyApp.valueProperty().addListener((obs, old, sp) -> loadAvailableDoctorsForSpecialty(sp));
        }
        if (avilabelDoctorApp != null) {
            avilabelDoctorApp.setPromptText("Available doctor");
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

    private void loadSpecialtiesAsync() {
        if (DoctorspecialtyApp == null) return;
        new Thread(() -> {
            try {
                var available = doctorDAO.listAvailableBySpecialty((String) null);
                Set<String> specs = new TreeSet<>();
                for (var opt : available) if (opt != null && opt.specialty != null) specs.add(opt.specialty);
                Platform.runLater(() -> DoctorspecialtyApp.setItems(FXCollections.observableArrayList(specs)));
            } catch (Exception e) {
                Platform.runLater(() -> showWarn("Doctors", "Failed to load specialties (available only)."));
            }
        }, "recp-specialties").start();
    }

    private void loadAvailableDoctorsForSpecialty(String specialty) {
        if (avilabelDoctorApp == null) return;
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
                    if (!list.isEmpty()) avilabelDoctorApp.getSelectionModel().select(0);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showWarn("Doctors", "Failed to load available doctors."));
            }
        }, "recp-avail-docs").start();
    }

    private void ensureSpecialtiesLoadedAsync() {
        if (!specialtyChoices.isEmpty()) return;
        new Thread(() -> {
            try {
                var all = doctorDAO.listAvailableBySpecialty((String) null);
                Set<String> sp = new TreeSet<>();
                for (var d : all) if (d != null && d.specialty != null) sp.add(d.specialty);
                Platform.runLater(() -> specialtyChoices.setAll(sp));
            } catch (Exception ignored) {
            }
        }, "load-spec-cache").start();
    }

    private void ensureDoctorsForSpecAsync(String spec) {
        if (spec == null || spec.isBlank()) return;
        if (doctorsBySpec.containsKey(spec)) return;
        doctorsBySpec.put(spec, FXCollections.observableArrayList());
        new Thread(() -> {
            try {
                var list = doctorDAO.listAvailableBySpecialty(spec);
                Platform.runLater(() -> doctorsBySpec.get(spec).setAll(list));
            } catch (Exception ignored) {
            }
        }, "load-docs-" + spec).start();
    }

    /**
     * استماع لقناة DB NOTIFY
     */
    private void startDbNotifications() {
        startDbNotificationsSafe();
    }



    /* ===== Helpers (alerts & online guard wrapper) ===== */
    private boolean ensureOnlineOrAlert() {
        if (monitor != null && !monitor.isOnline()) {
            showWarn("Offline", "You're offline. Please reconnect and try again.");
            return false;
        }
        return true;
    }

    private void showWarn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        Stage stage = (Stage) rootPane.getScene().getWindow();
        a.initOwner(stage);
        a.initModality(Modality.WINDOW_MODAL);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        Stage stage = (Stage) rootPane.getScene().getWindow();
        a.initOwner(stage);
        a.initModality(Modality.WINDOW_MODAL);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String showError(String title, Exception ex) {
        if (ex != null) ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        Stage stage = (Stage) rootPane.getScene().getWindow();
        a.initOwner(stage);
        a.initModality(Modality.WINDOW_MODAL);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(ex == null ? title : ex.getMessage());
        a.showAndWait();
        showToast("error", a.getContentText());
        return ex == null ? title : ex.getMessage();
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        Stage stage = (Stage) rootPane.getScene().getWindow();
        a.initOwner(stage);
        a.initModality(Modality.WINDOW_MODAL);

        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    /* ===== Patients CRUD (restored) ===== */
    private void doInsertPatient() {
        String fullName = trimOrNull(FullNameTextField.getText());
        String nid = trimOrNull(PatientIdTextField.getText());
        Gender gender = GenderComboBox.getValue();
        LocalDate dob = DateOfBirthPicker.getValue();
        String phone = trimOrNull(PhoneTextField.getText());
        String history = trimOrNull(medicalHistory.getText());

        if (fullName == null) {
            showWarn("Validation", "Full name is required.");
            return;
        }
        if (dob == null) {
            showWarn("Validation", "Date of birth is required.");
            return;
        }
        if (gender == null) {
            showWarn("Validation", "Gender is required.");
            return;
        }
        if (phone == null) {
            showWarn("Validation", "Patient must have a phone number.");
            return;
        }

        // Validate National ID: exactly 9 digits (client-side guard)
        if (nid == null || !nid.matches("\\d{9}")) {
            showWarn("National ID", "National ID must be exactly 9 digits.");
            return;
        }

        try {
            patientService.createPatient(fullName, nid, phone, dob, gender.name(), history);
            loadPatientsBG();
            clearForm();
            showInfo("Insert", "Patient inserted successfully.");
        } catch (org.postgresql.util.PSQLException e) {
            String msg = (e.getMessage() == null) ? "" : e.getMessage();
            String sqlState = e.getSQLState(); // '23505' for unique-violation in PG
            String constraint = null;
            try {
                var sev = e.getServerErrorMessage();
                if (sev != null) constraint = sev.getConstraint();
            } catch (Throwable ignore) { }

            // 1) طول الهوية
            if (msg.contains("value too long for type character(9)") ||
                msg.toLowerCase().contains("national_id must be 9 digits")) {
                showWarn("National ID", "National ID must be exactly 9 digits.");
            }
            // 2) تكرار الهوية (unique violation)
            else if ("23505".equals(sqlState) ||
                     (constraint != null && constraint.toLowerCase().contains("uniq_users_nid_nonnull")) ||
                     msg.toLowerCase().contains("duplicate key value") && msg.toLowerCase().contains("national_id")) {
                showWarn("Duplicate", "A patient with this National ID already exists. Please use Update instead.");
            }
            else {
                showError("Insert Patient", e);
            }
            return;
        } catch (Exception ex) {
            showError("Insert Patient", ex);
            return;
        }
        // إشعار قنوات الـ DB NOTIFY (غير حرِج لو فشل)
        try (Connection c = Database.get();
             PreparedStatement nps = c.prepareStatement("SELECT pg_notify('patients_changed','insert')")) {
            nps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void doUpdatePatient() {
        PatientRow row = (patientTable == null) ? null : patientTable.getSelectionModel().getSelectedItem();
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
        // Validate National ID on update as well
        if (nid != null && !nid.isBlank() && !nid.matches("\\d{9}")) {
            showWarn("National ID", "National ID must be exactly 9 digits.");
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
            if (patientTable != null) patientTable.refresh();
            showInfo("Update", "Patient updated successfully.");
        } catch (org.postgresql.util.PSQLException e) {
            if (e.getMessage() != null && (
                    e.getMessage().contains("value too long for type character(9)") ||
                    e.getMessage().toLowerCase().contains("national_id must be 9 digits")
            )) {
                showWarn("National ID", "National ID must be exactly 9 digits.");
            } else {
                showError("Update Patient", e);
            }
            return;
        } catch (Exception ex) {
            showError("Update Patient", ex);
            return;
        }
        try (Connection c = Database.get();
             PreparedStatement nps = c.prepareStatement("SELECT pg_notify('patients_changed','update')")) {
            nps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void doDeletePatient() {
        PatientRow row = (patientTable == null) ? null : patientTable.getSelectionModel().getSelectedItem();
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

        try (Connection c = Database.get();
             PreparedStatement nps = c.prepareStatement("SELECT pg_notify('patients_changed','delete')")) {
            nps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void clearForm() {
        if (FullNameTextField != null) FullNameTextField.clear();
        if (PatientIdTextField != null) PatientIdTextField.clear();
        if (PhoneTextField != null) PhoneTextField.clear();
        if (medicalHistory != null) medicalHistory.clear();
        if (GenderComboBox != null) GenderComboBox.setValue(Gender.MALE);
        if (DateOfBirthPicker != null) DateOfBirthPicker.setValue(null);
        if (patientTable != null) patientTable.getSelectionModel().clearSelection();
        if (search != null) search.clear();
        if (patientTable != null) patientTable.refresh();

    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // remember original widths per column (weak so columns can GC)
    private final java.util.Map<TableColumn<?, ?>, Double> _origColWidth = new java.util.WeakHashMap<>();

    /* ===== Appointments table wiring & search (minimal) ===== */
    private void wireAppointmentsTables() {
        if (TableINAppointment == null) return;
        //TableINAppointment.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        //TableINAppointment.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableINAppointment.setFixedCellSize(-1);

        enableExpandAutoWidth(TableINAppointment, colPatientNameAppointment, 5000);
        enableExpandAutoWidth(TableINAppointment, colSpecialty, 5000);
        enableExpandAutoWidth(TableINAppointment, colDoctorNameAppointment, 5000);

        TableINAppointment.setItems(sortedAppt);
        sortedAppt.comparatorProperty().bind(TableINAppointment.comparatorProperty());
        // === تفعيل التحرير داخل جدول المواعيد ===
        TableINAppointment.setEditable(true);
        ensureAppointmentBindings();
        colDateAppointment.setEditable(true);
//        colSpecialty.setEditable(true);

        setupInlineEditing();
        if (colDateAppointment != null) colDateAppointment.setCellValueFactory(cd -> cd.getValue().dateProperty());
        if (colDoctorNameAppointment != null)
            colDoctorNameAppointment.setCellValueFactory(cd -> cd.getValue().doctorNameProperty());
        if (colPatientNameAppointment != null)
            colPatientNameAppointment.setCellValueFactory(cd -> cd.getValue().patientNameProperty());
        if (colSpecialty != null) colSpecialty.setCellValueFactory(cd -> cd.getValue().specialtyProperty());
        if (colStatusAppointment != null)
            colStatusAppointment.setCellValueFactory(cd -> cd.getValue().statusProperty());
        TableINAppointment.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            updatePatientFieldsFromAppt(newRow);
            updateAppointmentDetailsLabel(newRow);
        });
        Platform.runLater(() ->
                updateAppointmentDetailsLabel(TableINAppointment.getSelectionModel().getSelectedItem())
        );

        // -------- Serial Number Column (#) --------
        if (colAppointmentSerialNUm != null) {
            colAppointmentSerialNUm.setStyle("-fx-alignment: CENTER;");
            /*
            colAppointmentSerialNUm.setMinWidth(60);
            colAppointmentSerialNUm.setPrefWidth(70);
            colAppointmentSerialNUm.setMaxWidth(100);
            */
            // لا نحتاج Data من الموديل؛ نستخدم خلية تعرض getIndex()+1
            colAppointmentSerialNUm.setCellFactory(col -> new TableCell<ApptRow, Number>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : String.valueOf(getIndex() + 1));
                }
            });

            // valueFactory وهمي فقط لإرضاء الـ TableColumn<ApptRow, Number>
            colAppointmentSerialNUm.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(0));

            // ممنوع الفرز/السحب على عمود الترقيم
            colAppointmentSerialNUm.setSortable(false);
            colAppointmentSerialNUm.setReorderable(false);
        }

        if (colStartTime != null) {
            // عرض للقراءة فقط بصيغة 12h
            colStartTime.setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(fmt12(cd.getValue().getTime()))
            );

            // محرر ComboBox يعرض فقط الفتحات الحرة للصف (doctor/date)
            colStartTime.setCellFactory(col -> new TableCell<ApptRow, String>() {
                private final ComboBox<DoctorDAO.Slot> combo = new ComboBox<>();

                {
                    combo.setVisibleRowCount(10);
                    combo.setPromptText("Select time");

                    // Render slots as 12h start-time text
                    combo.setCellFactory(list -> new ListCell<>() {
                        @Override
                        protected void updateItem(DoctorDAO.Slot item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? null : fmt12(item.from().toLocalTime()));
                        }
                    });
                    combo.setButtonCell(new ListCell<>() {
                        @Override
                        protected void updateItem(DoctorDAO.Slot item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? null : fmt12(item.from().toLocalTime()));
                        }
                    });

                    setOnMouseClicked(e -> {
                        if (!isEmpty() && getTableRow() != null && getTableRow().isSelected()) {
                            var rowItem = (getTableRow() == null) ? null : (ApptRow) getTableRow().getItem();
                            if (rowItem != null && isPastDate(rowItem.getDate())) {
                                showToast("error", "You cannot select a time for a past date.");
                                return;
                            }
                            startEdit();
                            combo.show();
                        }
                    });

                    // Guard: when opening the popup, keep it unselected unless the row already has a time
                    combo.setOnShowing(ev -> {
                        var rowItem = (getTableRow() == null) ? null : (ApptRow) getTableRow().getItem();
                        if (rowItem == null || rowItem.getTime() == null) {
                            combo.getSelectionModel().clearSelection();
                            combo.setValue(null);
                            combo.setPromptText("Select time");
                        }
                    });

                    // Populate only FREE slots for the row's doctor/date
                    combo.setOnShown(e -> {
                        var rowItem = (getTableRow() == null) ? null : (ApptRow) getTableRow().getItem();
                        if (rowItem == null) return;

                        LocalDate day = rowItem.getDate();
                        long docId = rowItem.getDoctorId();
                        if (day == null) {
                            showToast("warn", "Select a date first.");
                            combo.hide();
                            return;
                        }
                        if (docId <= 0) {
                            showToast("warn", "Select a doctor first.");
                            combo.hide();
                            return;
                        }

                        final LocalTime open = LocalTime.of(9, 0);
                        final LocalTime close = LocalTime.of(15, 0);
                        final int step = (rowItem.getSessionTime() > 0) ? rowItem.getSessionTime() : DEFAULT_SESSION_MIN; // 20 min default

                        java.util.List<DoctorDAO.Slot> slots;
                        try {
                            slots = doctorDAO.listFreeSlots(docId, day, open, close, step);
                        } catch (Exception ex) {
                            showWarn("Slots", "Failed to load free slots: " + ex.getMessage());
                            return;
                        }

                        // === Prune 1: never show past times for TODAY (use APP_TZ) ===
                        if (day.equals(LocalDate.now(APP_TZ))) {
                            LocalDateTime now = java.time.ZonedDateTime.now(APP_TZ)
                                    .toLocalDateTime().withSecond(0).withNano(0);
                            int mod = now.getMinute() % step;
                            LocalTime cutoffT = (mod == 0) ? now.toLocalTime() : now.toLocalTime().plusMinutes(step - mod);
                            slots.removeIf(s -> s.from().toLocalTime().isBefore(cutoffT));
                        }

                        // === Prune 2: enforce clinic close boundary ===
                        slots.removeIf(s -> !s.from().toLocalTime().isBefore(close));

                        // === Prune 3: remove overlaps with busy rows for same doctor/day (excluding current row) ===
                        if (TableINAppointment != null && apptEditable != null) {
                            java.util.List<java.time.LocalTime[]> busy = new java.util.ArrayList<>();
                            for (ApptRow r : apptEditable) {
                                if (r == null || r == rowItem) continue;
                                if (r.getDoctorId() != docId) continue;
                                if (day.equals(r.getDate()) && r.getTime() != null) {
                                    java.time.LocalTime st = r.getTime();
                                    int dur = (r.getSessionTime() > 0) ? r.getSessionTime() : DEFAULT_SESSION_MIN;
                                    java.time.LocalTime et = st.plusMinutes(dur);
                                    busy.add(new java.time.LocalTime[]{st, et});
                                }
                            }
                            slots.removeIf(s -> {
                                java.time.LocalTime st = s.from().toLocalTime();
                                java.time.LocalTime et = st.plusMinutes(step);
                                for (java.time.LocalTime[] b : busy) {
                                    java.time.LocalTime bst = b[0], bet = b[1];
                                    boolean overlap = !et.isBefore(bst) && !st.isAfter(bet.minusNanos(1));
                                    if (overlap) return true;
                                }
                                return false;
                            });
                        }

                        // Keep row's current time selectable even if busy (so user can keep it)
                        if (rowItem.getTime() != null) {
                            boolean present = false;
                            for (DoctorDAO.Slot s : slots) {
                                if (s.from().toLocalTime().equals(rowItem.getTime())) { present = true; break; }
                            }
                            if (!present) {
                                java.time.LocalDateTime from = java.time.LocalDateTime.of(day, rowItem.getTime());
                                java.time.LocalDateTime to = from.plusMinutes(step);
                                slots.add(new DoctorDAO.Slot(from, to));
                                slots.sort(java.util.Comparator.comparing(DoctorDAO.Slot::from));
                            }
                        }

                        combo.setItems(FXCollections.observableArrayList(slots));

                        // Neutralize JavaFX default selection after setting items
                        combo.getSelectionModel().clearSelection();
                        combo.setValue(null);

                        // === Selection policy (table): never auto-select ===
                        if (rowItem.getTime() == null) {
                            combo.getSelectionModel().clearSelection();
                            combo.setValue(null);           // keep value null so prompt shows
                            combo.setPromptText("Select time");
                        } else {
                            // preselect the row's current time if it's still present in the list
                            boolean matched = false;
                            for (DoctorDAO.Slot s : slots) {
                                if (s.from().toLocalTime().equals(rowItem.getTime())) {
                                    combo.getSelectionModel().select(s);
                                    matched = true;
                                    break;
                                }
                            }
                            if (!matched) {
                                // leave it unselected; user will choose a new free slot
                                combo.getSelectionModel().clearSelection();
                                combo.setValue(null);
                                combo.setPromptText("Select time");
                            }
                        }
                    });

                    combo.setOnAction(e -> {
                        var rowItem = (getTableRow() == null) ? null : (ApptRow) getTableRow().getItem();
                        DoctorDAO.Slot sel = combo.getValue();
                        if (rowItem == null || sel == null) return;
                        try {
                            LocalTime nt = sel.from().toLocalTime();
                            // 🚫 لا تسمح بوقت ماضي
                            if (isPastStart(rowItem.getDate(), nt)) {
                                showToast("error", "Selected time is in the past. Choose a future time.");
                                combo.getSelectionModel().clearSelection();
                                cancelEdit();
                                return;
                            }
                            rowItem.setTime(nt);
                            rowItem.setDirty(true);
                            if (rowItem.getId() > 0 && rowItem.getDate() != null) {
                                updateAppointmentStartAt(rowItem.getId(), rowItem.getDate(), nt);
                            }
                            commitEdit(fmt12(nt));
                            if (TableINAppointment != null) TableINAppointment.refresh();
                        } catch (Exception ex) {
                            showError("Invalid time", new RuntimeException("Unexpected time selection"));
                        }
                    });
                }

                @Override
                public void startEdit() {
                    super.startEdit();
                    setGraphic(combo);
                    setText(null);
                    combo.setPromptText("Select time");
                    var rowItem = (getTableRow() == null) ? null : (ApptRow) getTableRow().getItem();
                    if (rowItem == null || rowItem.getTime() == null) {
                        combo.getSelectionModel().clearSelection();
                        combo.setValue(null);
                    }
                }

                @Override
                public void cancelEdit() {
                    super.cancelEdit();
                    setGraphic(null);
                    setText(getItem());
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    boolean showEditor = isEditing() && getTableRow() != null && getTableRow().isSelected();
                    if (showEditor) {
                        setGraphic(combo);
                        setText(null);
                    } else {
                        setGraphic(null);
                        setText(item);
                    }
                }
            });
        }


        if (colRoomNumber != null) {
            colRoomNumber.setEditable(false);
            colRoomNumber.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    cd.getValue().getRoomNumber() == null ? "" : cd.getValue().getRoomNumber()
            ));
        }

        // Date as DatePicker
        if (colDateAppointment != null) {
            colDateAppointment.setCellFactory(col -> datePickerCell());
        }
        if (colSessionTime != null) {
            colSessionTime.setEditable(false); // read-only
            colSessionTime.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(
                    cd.getValue().getSessionTime() > 0 ? cd.getValue().getSessionTime() : DEFAULT_SESSION_MIN
            ));
            // simple non-editable rendering
            colSessionTime.setCellFactory(col -> new TableCell<ApptRow, Number>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.valueOf(item.intValue()));
                }
            });
        }


        if (TableINAppointment != null) TableINAppointment.refresh();
    }

    // Ensure FilteredList/SortedList are wired to the table items before applying predicates
    private void ensureAppointmentBindings() {
        if (TableINAppointment == null) return;
        // initialize filteredAppt/sortedAppt if not already correct
        if (filteredAppt == null || sortedAppt == null || TableINAppointment.getItems() != sortedAppt) {
            if (filteredAppt == null) {
                // fall back to current table items if present, else use apptEditable
                javafx.collections.ObservableList<ApptRow> base = (apptEditable != null) ? apptEditable : javafx.collections.FXCollections.observableArrayList();
                filteredAppt = new javafx.collections.transformation.FilteredList<>(base, r -> true);
            }
            if (sortedAppt == null || sortedAppt.getSource() != filteredAppt) {
                sortedAppt = new javafx.collections.transformation.SortedList<>(filteredAppt);
            }
            sortedAppt.comparatorProperty().bind(TableINAppointment.comparatorProperty());
            TableINAppointment.setItems(sortedAppt);
        }
    }

    // --- Date filter for Appointments table (like dataPickerDashboard) ---
    private void wireAppointmentDateFilter() {
        // default to today if not set
        if (dataPickerAppointment != null && dataPickerAppointment.getValue() == null) {
            dataPickerAppointment.setValue(LocalDate.now());
        }

        if (dataPickerAppointment != null) {
            dataPickerAppointment.valueProperty().addListener((obs, oldD, newD) -> {
                // 1) طبّق الفلاتر أولاً
                applyAppointmentFilters();

            });
        }

        // تشغيل أولي للفلاتر عند فتح الصفحة
        applyAppointmentFilters();
    }

    private void updateNoAppointmentsBanner() {
        if (LabelToAlert == null) return;
        if (apptLoading) return; // لا تعرض رسالة أثناء التحميل

        LocalDate sel = (dataPickerAppointment != null && dataPickerAppointment.getValue() != null)
                ? dataPickerAppointment.getValue() : LocalDate.now();

        // كم عنصر ظاهر فعلاً؟
        int visible = 0;
        if (filteredAppt != null) {
            visible = filteredAppt.size();
        } else if (TableINAppointment != null && TableINAppointment.getItems() != null) {
            visible = TableINAppointment.getItems().size();
        }

        if (visible == 0) {
            LabelToAlert.setVisible(true);
            LabelToAlert.setManaged(true);
            // نص أجمل مع تاريخ لطيف
            String nice = sel.format(UI_DATE);
            LabelToAlert.setText("ℹ No appointments on " + nice + ".");
            // كلاس ستايل
            if (!LabelToAlert.getStyleClass().contains("info-banner")) {
                LabelToAlert.getStyleClass().add("info-banner");
            }
        } else {
            LabelToAlert.setVisible(false);
            LabelToAlert.setManaged(false);
            LabelToAlert.setText("");
            LabelToAlert.getStyleClass().remove("info-banner");
        }
    }

    private void applyAppointmentFilters() {
        ensureAppointmentBindings();

        final LocalDate selDate = (dataPickerAppointment == null) ? null : dataPickerAppointment.getValue();
        final String q = (searchAppointmentDach == null || searchAppointmentDach.getText() == null)
                ? "" : searchAppointmentDach.getText().trim().toLowerCase();

        // 👈 المفتاح: ALL = لا فلترة حالة
        final String statusSel = (statusFilter == null || statusFilter.getValue() == null)
                ? "ALL"
                : statusFilter.getValue().toUpperCase();

        filteredAppt.setPredicate(r -> {
            // صفوف تعبئة/فراغ (لو موجودة) لا تدخل المنطق
            if (r == null) return false;

            // 1) فلتر التاريخ
            if (selDate != null && !selDate.equals(r.getDate())) return false;

            // 2) فلتر الحالة (ALL = مرّر)
            if (!"ALL".equals(statusSel)) {
                String st = (r.getStatus() == null) ? "" : r.getStatus().toUpperCase();
                if (!st.equals(statusSel)) return false;
            }

            // 3) البحث النصي (اختياري)
            if (!q.isEmpty()) {
                return (r.getPatientName() != null && r.getPatientName().toLowerCase().contains(q))
                        || (r.getDoctorName() != null && r.getDoctorName().toLowerCase().contains(q))
                        || (r.getSpecialty() != null && r.getSpecialty().toLowerCase().contains(q))
                        || String.valueOf(r.getId()).contains(q);
            }
            return true;
        });

        if (TableINAppointment != null) TableINAppointment.refresh();
    }

    @FXML private void onClearDashboardFilters() {
        if (searchAppointmentDach != null) searchAppointmentDach.clear();
        if (statusFilter != null) statusFilter.getSelectionModel().select("ALL");
        applyAppointmentFilters();
    }


    // توليد قائمة الأوقات بصيغة 12h وفق دوام العيادة وبخطوة مدة الجلسة
    private java.util.List<String> generateClinicTimes(java.time.LocalDate date) {
        java.util.List<String> res = new java.util.ArrayList<>();
        java.time.LocalTime open = java.time.LocalTime.of(9, 0);
        java.time.LocalTime close = java.time.LocalTime.of(15, 0);
        int step = DEFAULT_SESSION_MIN; // 20 دقيقة

        for (java.time.LocalTime t = open; t.isBefore(close); t = t.plusMinutes(step)) {
            res.add(t.format(SLOT_FMT_12H));
        }

        if (date != null && date.equals(java.time.LocalDate.now(APP_TZ))) {
            java.time.LocalDateTime now = java.time.ZonedDateTime.now(APP_TZ)
                    .toLocalDateTime().withSecond(0).withNano(0);
            int mod = now.getMinute() % step;
            java.time.LocalTime cutoff = (mod == 0)
                    ? now.toLocalTime()
                    : now.toLocalTime().plusMinutes(step - mod);
            res.removeIf(s -> java.time.LocalTime.parse(s, SLOT_FMT_12H).isBefore(cutoff));
            res.removeIf(s -> java.time.LocalTime.parse(s, SLOT_FMT_12H).compareTo(close) >= 0);
        }

        return res;
    }

    private void setupInlineEditing() {
        if (TableINAppointment == null) return;

        // Patient name inline (kept local only)
        if (colPatientNameAppointment != null) {
            colPatientNameAppointment.setEditable(true);
            colPatientNameAppointment.setCellFactory(TextFieldTableCell.forTableColumn());
            colPatientNameAppointment.setOnEditCommit(ev -> {
                ApptRow row = ev.getRowValue();
                String v = safe(ev.getNewValue());
                if (row == null || v == null) return;
                row.setPatientName(v);
                row.setDirty(true);
            });
        }
        if (colSpecialty != null) {
            ensureSpecialtiesLoadedAsync();
            colSpecialty.setEditable(true);
            colSpecialty.setCellFactory(ComboBoxTableCell.forTableColumn(specialtyChoices));
            colSpecialty.setOnEditCommit(ev -> {
                ApptRow row = ev.getRowValue();
                String sp = ev.getNewValue();
                if (row == null || sp == null) return;
                // حدّث التخصص
                row.setSpecialty(sp);
                // Specialty تغيّر ⇒ لازم نلغي أي اختيار دكتور وغرفة
                row.setDoctorId(0);
                row.setDoctorName(null);
                row.setRoomNumber(null);
                row.setDirty(true);
//                updateDirtyAlert();
                ensureDoctorsForSpecAsync(sp);  // حمّل قائمة أطباء التخصص الجديد
                if (TableINAppointment != null) TableINAppointment.refresh();
            });
        }

        // Doctor column uses our custom cell that only shows the ComboBox while editing/selected
        if (colDoctorNameAppointment != null) {
            colDoctorNameAppointment.setEditable(true);
            colDoctorNameAppointment.setCellFactory(col -> doctorComboCell());
        }


        // Status column as ComboBox
        if (colStatusAppointment != null) {
            ObservableList<String> statuses = FXCollections.observableArrayList("SCHEDULED", "COMPLETED", "CANCELLED");
            colStatusAppointment.setEditable(true);
            colStatusAppointment.setCellFactory(ComboBoxTableCell.forTableColumn(statuses));
            colStatusAppointment.setOnEditCommit(ev -> {
                ApptRow row = ev.getRowValue();
                String st = ev.getNewValue();
                if (row == null || st == null) return;
                row.setStatus(st);
                row.setDirty(true);
            });
        }
    }

    // Add or focus a draft appointment row for a selected patient
    private void addOrFocusDraftForPatient(PatientRow p) {
        if (p == null || TableINAppointment == null) return;
        for (ApptRow r : apptEditable) {
            if (p.getFullName().equals(r.getPatientName()) && r.isNew()) {
                TableINAppointment.getSelectionModel().select(r);
                TableINAppointment.scrollTo(r);
                return;
            }
        }
        ApptRow r = new ApptRow();
        r.setId(0);
        r.setNew(true);
        r.setDirty(true);
        r.setPatientName(p.getFullName());
        r.setStatus("SCHEDULED");
        r.setTime(null);
        r.setDate(AppointmentDate != null && AppointmentDate.getValue() != null ? AppointmentDate.getValue() : LocalDate.now());

        apptEditable.add(0, r);
        TableINAppointment.getSelectionModel().select(r);
        TableINAppointment.scrollTo(r);
        if (getPatientName != null) getPatientName.setText(p.getFullName());
        if (getPatientID != null) getPatientID.setText(String.valueOf(p.getPatientId()));
    }

    // Add a blank draft row from the + button
    private void addBlankDraftRow() {
        if (TableINAppointment == null) return;

        // Try to prefill from selected patient row
        String name = null;
        Long pid = null;
        try {
            PatientRow sel = (patientTable == null) ? null : patientTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                name = sel.getFullName();
                pid = sel.getPatientId();
            }
        } catch (Exception ignore) {
        }

        // Fallback to labels if set
        if ((name == null || name.isBlank()) && getPatientName != null) name = getPatientName.getText();
        if ((pid == null || pid <= 0) && getPatientID != null) {
            String txt = getPatientID.getText();
            if (txt != null && txt.trim().matches("\\d+")) pid = Long.parseLong(txt.trim());
        }

        ApptRow r = new ApptRow();
        r.setId(0);              // draft (hidden by cell factory)
        r.setNew(true);
        r.setDirty(true);
        r.setStatus("SCHEDULED");
        r.setDate((dataPickerAppointment != null && dataPickerAppointment.getValue() != null)
                ? dataPickerAppointment.getValue()
                : LocalDate.now());
        if (name != null) r.setPatientName(name);
        // doctor/time/room left empty until user chooses

        apptEditable.add(0, r);
        TableINAppointment.getSelectionModel().select(r);
        TableINAppointment.scrollTo(r);

        // Reflect to top labels too (so resolvePatientId() can work on insert)
        if (getPatientName != null && name != null) getPatientName.setText(name);
        if (getPatientID != null) getPatientID.setText(pid != null ? String.valueOf(pid) : "");
    }

    private void doClearAppointmentForm() {
        // Only clear the table selection; do not modify any DB state
        if (TableINAppointment != null) {
            TableINAppointment.getSelectionModel().clearSelection();
            try {
                TableINAppointment.getFocusModel().focus(-1);
            } catch (Exception ignore) {
            }
        }
        // Clear the top patient info labels and the date/time summary label
        if (getPatientName != null) getPatientName.setText("");
        if (getPatientID != null) getPatientID.setText("");
        updateAppointmentDetailsLabel(null);
        showToast("info", "Selection cleared.");
        onClearDashboardFilters();
    }

    // Load appointments table once (used at init)
    private void loadAppointmentsTable() {
        try {
            var apptRows = AppointmentJdbcDAO.listScheduledAppointments();
            var mapped = FXCollections.<ApptRow>observableArrayList();
            for (var r : apptRows) {
                ApptRow ar = new ApptRow();
                ar.setId(r.id);
                ar.setDoctorId(r.doctorId);
                ar.setDoctorName(r.doctorName);
                ar.setPatientName(r.patientName);
                ar.setSpecialty(r.specialty);
                ar.setStatus(r.status);
                java.time.LocalDateTime ldt2 = toLocal(r.startAt);
                if (ldt2 != null) {
                    ar.setDate(ldt2.toLocalDate());
                    ar.setTime(ldt2.toLocalTime());
                }
                ar.setRoomNumber(r.location);
                ar.setNew(false);
                ar.setDirty(false);
                mapped.add(ar);
            }
            Platform.runLater(() -> TableUtils.applyDelta(apptEditable, mapped, ApptRow::getId));
            Platform.runLater(() -> updatePatientFieldsFromAppt((TableINAppointment == null) ? null : TableINAppointment.getSelectionModel().getSelectedItem()));
        } catch (Exception ex) {
            Platform.runLater(() -> showError("Load Appointments", ex));
        }
    }

    private void updateAppointmentCounters() {
        java.time.LocalDate sel = (dataPickerDashboard != null && dataPickerDashboard.getValue() != null)
                ? dataPickerDashboard.getValue()
                : java.time.LocalDate.now();
        try {
            int doctors = com.example.healthflow.dao.AppointmentJdbcDAO.countDoctorsOnDate(sel);
            int appts = com.example.healthflow.dao.AppointmentJdbcDAO.countAppointmentsOnDate(sel);
            int patients = com.example.healthflow.dao.AppointmentJdbcDAO.countPatientsOnDate(sel);
            int completed = com.example.healthflow.dao.AppointmentJdbcDAO.countCompletedAppointmentsOnDate(sel);
            int remaining = com.example.healthflow.dao.AppointmentJdbcDAO.countRemainingAppointmentsOnDate(sel);

            Platform.runLater(() -> {
                if (NumberOfTotalDoctors != null) NumberOfTotalDoctors.setText(String.valueOf(doctors));
                if (NumberOfTotalAppointments != null) NumberOfTotalAppointments.setText(String.valueOf(appts));
                if (NumberOfTotalPatients != null) NumberOfTotalPatients.setText(String.valueOf(patients));
                if (patientCompleteNum != null) patientCompleteNum.setText(String.valueOf(completed));
                if (RemainingNum != null) RemainingNum.setText(String.valueOf(remaining));
                updatePatientDetailsChart(); // تبقى كما هي
            });
        } catch (Exception ex) {
            Platform.runLater(() -> showError("Counters (by date)", ex));
        }
    }

    // Update start_at field for an appointment
    private void updateAppointmentStartAt(long id, LocalDate d, LocalTime t) {
        // 1) أغلق أي تحرير جارٍ في الجدول حتى تُحفظ القيم داخل ApptRow
        if (TableINAppointment != null && TableINAppointment.getEditingCell() != null) {
            TableINAppointment.edit(-1, null);
        }

        // 2) تحقق مبدئي
        if (id <= 0 || d == null || t == null) return;

        // 3) لا ترمِ استثناءً للمستخدم – أعرض رسالة لطيفة فقط
        if (isPastStart(d, t)) {
            showToast("error", "Past time not allowed for today's date");
            return;
        }

        final String sql = "UPDATE appointments SET appointment_date = ?, updated_at = now() WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            java.time.OffsetDateTime startAt = toAppOffset(d, t); // Asia/Gaza
            ps.setObject(1, startAt); // write timestamptz correctly
            ps.setLong(2, id);
            ps.executeUpdate();

            try (PreparedStatement n = c.prepareStatement("SELECT pg_notify('appointments_changed','update')")) {
                n.execute();
            }
            lastApptTs = new java.sql.Timestamp(System.currentTimeMillis());

            // 4) تحديث تفاؤلي للـ UI
            if (TableINAppointment != null) {
                ApptRow sel = TableINAppointment.getItems().stream()
                        .filter(r -> r != null && r.getId() == id)
                        .findFirst().orElse(null);
                if (sel != null) {
                    sel.setDate(d);
                    sel.setTime(t);
                    sel.setDirty(false);
                    TableINAppointment.refresh();
                }
            }
        } catch (SQLException e) {
            Platform.runLater(() -> showError("Update appointment_date", e));
        }
    }

    // Delete currently selected appointment
    private void doDeleteAppointment() {
        // 1) أغلق أي تحرير جارٍ
        if (TableINAppointment != null && TableINAppointment.getEditingCell() != null) {
            TableINAppointment.edit(-1, null);
        }

        var row = (TableINAppointment == null) ? null : TableINAppointment.getSelectionModel().getSelectedItem();
        if (row == null) {
            showWarn("Delete", "Select an appointment row first.");
            return;
        }

        // 2) لو مسودة (id = 0) احذف محليًا فقط
        if (row.getId() <= 0) {
            apptEditable.remove(row);
            showToast("info", "Draft row removed.");
            return;
        }

        if (!confirm("Delete", "Delete appointment #" + row.getId() + "?")) return;

        try (Connection c = Database.get()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM appointments WHERE id = ?")) {
                ps.setLong(1, row.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement n = c.prepareStatement("SELECT pg_notify('appointments_changed','delete')")) {
                n.execute();
            }

            // 3) حدث الواجهة فورًا
            apptEditable.remove(row);
            if (TableINAppointment != null) {
                TableINAppointment.getSelectionModel().clearSelection();
                TableINAppointment.refresh();
            }

            showInfo("Delete", "Appointment deleted.");
        } catch (Exception e) {
            showError("Delete Appointment", e);
        }
    }

    private void doInsertAppointment() {
        try {
            // --- 0) Commit any in-cell edits first so row values are up-to-date ---
            if (TableINAppointment != null && TableINAppointment.getEditingCell() != null) {
                TableINAppointment.edit(-1, null); // forces commit/cancel → updates ApptRow model
            }

            Long doctorId = null;
            LocalDate day = null;
            LocalTime time = null;
            Integer duration = null;
            String location = null;

            // ===== Path A: from mini booking form (preferred) =====
            var formDoc  = (avilabelDoctorApp == null) ? null : avilabelDoctorApp.getValue();
            var formDay  = (AppointmentDate   == null) ? null : AppointmentDate.getValue();
            var formSlot = (cmbSlots           == null) ? null : cmbSlots.getValue();
            if (formDoc != null && formDay != null && formSlot != null) {
                doctorId = formDoc.doctorId;
                day      = formDay;
                time     = formSlot.from().toLocalTime();
                duration = (int) java.time.Duration.between(formSlot.from(), formSlot.to()).toMinutes();
            }

            // ===== Path B: from selected table row (draft row) =====
            if (doctorId == null || day == null || time == null) {
                ApptRow row = (TableINAppointment == null) ? null : TableINAppointment.getSelectionModel().getSelectedItem();
                if (row != null) {
                    // date/time/room taken directly from row (after commit above)
                    if (row.getDate() != null) day = row.getDate();
                    if (row.getTime() != null) time = row.getTime();
                    if (row.getRoomNumber() != null && !row.getRoomNumber().isBlank()) location = row.getRoomNumber();
                    duration = (row.getSessionTime() > 0) ? row.getSessionTime() : DEFAULT_SESSION_MIN;

                    // doctor id: use explicit id if already set; otherwise resolve by doctor name (+ optional specialty)
                    if (row.getDoctorId() > 0) {
                        doctorId = row.getDoctorId();
                    } else {
                        doctorId = resolveDoctorIdForRow(row);
                    }
                }
            }

            // ===== Validation =====
            if (doctorId == null || day == null || time == null) {
                if (TableINAppointment != null) {
                    TableINAppointment.requestFocus();
                }
                showWarn("Insert Appointment", "Select specialty, doctor and time slot.");
                return;
            }

            Long patientId = resolvePatientId();
            if (patientId == null) {
                showWarn("Insert Appointment", "Invalid Patient. Select a patient from the table or enter a valid Patient ID / National ID.");
                return;
            }
            if (isPastStart(day, time)) {
                showToast("error", "Cannot insert an appointment in the past. Choose a future time.");
                return;
            }

            if (duration == null || duration <= 0) duration = DEFAULT_SESSION_MIN;
            java.time.OffsetDateTime startAt = toAppOffset(day, time);

            final String sql = """
                INSERT INTO appointments
                  (doctor_id, patient_id, appointment_date, duration_minutes, status, location, created_by, created_at, updated_at)
                VALUES
                  (?, ?, ?, ?, 'SCHEDULED'::appt_status, COALESCE(?, (SELECT room_number FROM doctors WHERE id=?)), ?, now(), now())
                RETURNING id, doctor_id, patient_id, appointment_date, duration_minutes, status, location
            """;

            ApptRow draft = (TableINAppointment == null) ? null : TableINAppointment.getSelectionModel().getSelectedItem();
            try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, doctorId);
                ps.setLong(2, patientId);
                ps.setObject(3, startAt);
                ps.setInt(4, duration);
                if (location != null && !location.isBlank()) ps.setString(5, location);
                else ps.setNull(5, Types.VARCHAR);
                ps.setLong(6, doctorId); // for COALESCE subselect
                ps.setLong(7, Session.get().getId());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ApptRow ar = new ApptRow();
                        ar.setId(rs.getLong("id"));
                        ar.setDoctorId(rs.getLong("doctor_id"));
                        ar.setDoctorName((formDoc != null) ? formDoc.fullName : (draft != null ? draft.getDoctorName() : null));
                        ar.setPatientName((draft != null) ? draft.getPatientName() : null);
                        ar.setSpecialty((draft != null) ? draft.getSpecialty() : null);
                        ar.setStatus(rs.getString("status"));

                        java.time.OffsetDateTime odt = rs.getObject("appointment_date", java.time.OffsetDateTime.class);
                        java.time.LocalDateTime ldt = toLocal(odt);
                        if (ldt != null) {
                            ar.setDate(ldt.toLocalDate());
                            ar.setTime(ldt.toLocalTime());
                        }
                        String loc = rs.getString("location");
                        ar.setRoomNumber(loc != null ? loc : (draft != null ? draft.getRoomNumber() : null));
                        ar.setNew(false);
                        ar.setDirty(false);

                        if (draft != null && apptEditable.contains(draft)) {
                            int idx = apptEditable.indexOf(draft);
                            apptEditable.set(idx, ar);
                        } else {
                            apptEditable.add(0, ar);
                        }
                        if (TableINAppointment != null) {
                            TableINAppointment.getSelectionModel().select(ar);
                            TableINAppointment.scrollTo(ar);
                        }
                    }
                }

                try (PreparedStatement n = c.prepareStatement("SELECT pg_notify('appointments_changed','insert')")) {
                    n.execute();
                }


            } catch (org.postgresql.util.PSQLException e) {
                String constraint = null;
                try {
                    var sev = e.getServerErrorMessage();
                    if (sev != null) constraint = sev.getConstraint();
                } catch (Throwable ignore) { }

                if (constraint != null && constraint.toLowerCase().contains("no_doctor_overlap")) {
                    String from = (time == null) ? "" : fmt12(time);
                    String to   = (time == null) ? "" : fmt12(time.plusMinutes((duration != null && duration > 0) ? duration : DEFAULT_SESSION_MIN));
                    String niceDate = (day == null) ? "" : day.format(UI_DATE);

                    showWarn(
                            "Time slot unavailable",
                            "That time slot is already booked for this doctor on " + niceDate +
                                    (from.isEmpty() ? "." : (" (" + from + "–" + to + ").")) +
                                    "\nPlease choose another available time."
                    );
                    return; // نوقف الإدخال بهدوء بدون stacktrace للمستخدم
                }

                // أي خطأ PG آخر → اتركه يعامل بالهاندلر الافتراضي
                throw e;

            }
            showDashboardPane();

            showInfo("Insert", "Appointment created.");
        } catch (Exception e) {
            if (e instanceof java.sql.SQLException se && "23505".equals(se.getSQLState())) {
                showWarn("Insert Appointment", "Conflict: another appointment exists for the same doctor or room at this start time.");
                return;
            }
            showError("Insert Appointment", e);
        }
    }

    /** Resolve doctor_id from the displayed doctor name in the row.
     *  Tolerates "Dr." prefix and "(Room ...)" suffix; filters by specialty if present.
     */
    private Long resolveDoctorIdForRow(ApptRow row) {
        if (row == null) return null;
        if (row.getDoctorId() > 0) return row.getDoctorId();

        String disp = row.getDoctorName();
        if (disp == null || disp.isBlank()) return null;

        // strip "Dr." و أي لاحقة أقواس مثل (Room: …)
        String name = disp
                .replaceAll("(?i)^\\s*dr\\.?\\s*", "")
                .replaceAll("\\(.*$", "")
                .trim();
        if (name.isEmpty()) return null;

        final String sql =
                "SELECT d.id " +
                        "FROM doctors d JOIN users u ON u.id = d.user_id " +
                        "WHERE lower(u.full_name) LIKE lower(?) " +
                        "AND ( ? IS NULL OR d.specialty = ? ) " +
                        "ORDER BY u.full_name ASC " +
                        "LIMIT 1";

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + name + "%");
            ps.setString(2, (row.getSpecialty() == null ? null : row.getSpecialty()));
            ps.setString(3, (row.getSpecialty() == null ? null : row.getSpecialty()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException ignore) { }
        return null;
    }

    private void doUpdateAppointment() {
        // 0) Commit any active cell edit to ensure model is up-to-date
        if (TableINAppointment != null && TableINAppointment.getEditingCell() != null) {
            TableINAppointment.edit(-1, null);
        }

        // 1) Re-read selection after committing edits
        ApptRow row = (TableINAppointment == null) ? null : TableINAppointment.getSelectionModel().getSelectedItem();
        if (row == null) {
            showWarn("Update", "Select an appointment row first.");
            return;
        }
        if (row.getId() <= 0) {
            showWarn("Update", "Please save this draft as a new appointment first.");
            return;
        }

        try (Connection c = Database.get()) {
            String sql = "UPDATE appointments SET doctor_id=?, appointment_date=?, duration_minutes=?, " +
                    "location = COALESCE(?, (SELECT room_number FROM doctors WHERE id=?)), " +
                    "status=?::appt_status, updated_at=now() WHERE id=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                LocalDate date = row.getDate();
                LocalTime time = row.getTime();
                // 🚫 Prevent moving appointment to the past
                if (date != null && time != null && isPastStart(date, time)) {
                    showToast("error", "Cannot update appointment to a past time.");
                    return;
                }
                java.time.OffsetDateTime startAt = (date != null && time != null)
                        ? toAppOffset(date, time)
                        : null;
                int duration = (row.getSessionTime() > 0) ? row.getSessionTime() : DEFAULT_SESSION_MIN;

                // Resolve doctorId if not set (tolerate edits in combo before commit)
                long doctorId = row.getDoctorId() > 0 ? row.getDoctorId() : (Optional.ofNullable(resolveDoctorIdForRow(row)).orElse(0L));
                if (doctorId <= 0) {
                    showWarn("Update Appointment", "Select a valid doctor for this appointment.");
                    return;
                }

                ps.setLong(1, doctorId);
                if (startAt != null) ps.setObject(2, startAt);
                else ps.setNull(2, java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
                ps.setInt(3, duration);

                if (row.getRoomNumber() != null && !row.getRoomNumber().isBlank())
                    ps.setString(4, row.getRoomNumber());
                else
                    ps.setNull(4, Types.VARCHAR);

                ps.setLong(5, doctorId); // for COALESCE subselect

                if (row.getStatus() != null && !row.getStatus().isBlank())
                    ps.setString(6, row.getStatus());
                else
                    ps.setNull(6, Types.OTHER);

                ps.setLong(7, row.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement n = c.prepareStatement("SELECT pg_notify('appointments_changed','update')")) {
                n.execute();
            }
            lastApptTs = new java.sql.Timestamp(System.currentTimeMillis());
            showInfo("Update", "Appointment updated.");
        } catch (Exception e) {
            if (e instanceof java.sql.SQLException se && "23505".equals(se.getSQLState())) {
                showWarn("Update Appointment", "Conflict: another appointment exists for the same doctor or room at this start time.");
                return;
            }
            showError("Update Appointment", e);
        }
    }

//    private void reloadDashboardAppointments() {
//        if (TableAppInDashboard == null) return;
//
//        java.time.LocalDate sel = (dataPickerDashboard != null && dataPickerDashboard.getValue() != null)
//                ? dataPickerDashboard.getValue()
//                : java.time.LocalDate.now();
//
//        // اجلب البيانات بالخلفية حتى ما يعلق الـ UI
//        new Thread(() -> {
//            try {
//                var rows = com.example.healthflow.dao.AppointmentJdbcDAO.listByDateAll(sel);
//
//                // كل تفاعل مع الواجهة يتم داخل FX thread
//                Platform.runLater(() -> {
//                    if (rows != null && !rows.isEmpty()) {
//                        dashBase.setAll(rows);
//                        TableAppInDashboard.setPlaceholder(new Label(""));
//                    } else {
//                        System.out.println("[Dashboard] no appointments found, keeping previous rows temporarily");
//                        TableAppInDashboard.setPlaceholder(new Label("No appointments on " + sel));
//                    }
//
//                    applyDashboardFilters();
//                    TableAppInDashboard.refresh();
//                    System.out.println("[ReceptionController] reloadDashboardAppointments sel=" + sel + " rows=" + (rows == null ? 0 : rows.size()));
//                });
//
//            } catch (Exception ex) {
//                System.err.println("[ReceptionController] reloadDashboardAppointments error: " + ex);
//                Platform.runLater(() -> {
//                    if (TableAppInDashboard != null)
//                        TableAppInDashboard.setPlaceholder(new Label("Failed to load"));
//                });
//            }
//        }, "reload-dashboard").start();
//    }


    // ========== DASHBOARD TABLE RELOAD ==========
    // This is the method referenced in the instruction (see log message)
    private void reloadDashboardAppointments() {
        final LocalDate day = (dataPickerDashboard == null || dataPickerDashboard.getValue() == null)
                ? java.time.LocalDate.now()
                : dataPickerDashboard.getValue();

        new Thread(() -> {
            java.util.List<DoctorDAO.AppointmentRow> rows;
            try {
                rows = doctorDAO.listDashboardAppointments(day);
            } catch (Exception e) {
                e.printStackTrace();
                final String msg = e.getMessage();
                Platform.runLater(() -> showWarn("Dashboard", "Failed to load dashboard appointments: " + msg));
                return;
            }

            final java.util.List<DoctorDAO.AppointmentRow> finalRows = rows;
            Platform.runLater(() -> {
                System.out.println("[ReceptionController] reloadDashboardAppointments sel=" + day + " rows=" + (finalRows == null ? 0 : finalRows.size()));

                // --- Apply rows to dashboard table (clear when empty) ---
                if (finalRows == null || finalRows.isEmpty()) {
                    try { dashBase.clear(); } catch (Throwable ignore) {}
                    try {
                        if (TableAppInDashboard != null) {
                            TableAppInDashboard.getSelectionModel().clearSelection();
                            if (TableAppInDashboard.getItems() != dashSorted) {
                                TableAppInDashboard.setItems(dashSorted); // fix: use dashSorted consistently
                            }
                            TableAppInDashboard.setPlaceholder(new Label(
                                    (day == null ? "No appointments" : ("No appointments for " + day))
                            ));
                            TableAppInDashboard.refresh();
                        }
                    } catch (Throwable ignore) {}
                    System.out.println("[ReceptionController] reloadDashboardAppointments: no rows → cleared table");
                    return; // stop here; nothing more to populate
                }

                // If we have rows → show them
                dashBase.setAll(finalRows);
                try {
                    if (TableAppInDashboard != null && TableAppInDashboard.getItems() != dashSorted) {
                        TableAppInDashboard.setItems(dashSorted);
                    }
                } catch (Throwable ignore) {}
            });
        }, "reload-dashboard").start();
    }


    // -- Dashboard DatePicker wiring: today by default + reload on change
    private void wireDashboardDatePicker() {
        if (dataPickerDashboard == null) return;

        // Set default without relying on action handlers
        if (dataPickerDashboard.getValue() == null) {
            dataPickerDashboard.setValue(java.time.LocalDate.now());
        }

        // Single source of truth: value listener only (avoid duplicate firing with setOnAction)
        dataPickerDashboard.valueProperty().addListener((obs, oldD, newD) -> {
            refreshDashboardAll();
        });

        // Initial populate once
        refreshDashboardAll();
    }

    private void refreshDashboardAll() {
        reloadDashboardAppointments();
        updateAppointmentCounters();
        updatePatientDetailsChart();
        applyDashboardFilters();
    }


    private void wireDashboardAppointmentsSearch() {
        if (searchAppointmentDach == null) return;

        final javafx.animation.PauseTransition debounce =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));

        searchAppointmentDach.textProperty().addListener((obs, old, q) -> {
            debounce.stop();
            debounce.setOnFinished(ev -> applyDashboardFilters());
            debounce.playFromStart();
        });

        // أول تطبيق
        applyDashboardFilters();
        if (TableAppInDashboard != null && TableAppInDashboard.getItems() != dashSorted) {
            TableAppInDashboard.setItems(dashSorted);
        }

    }

    // يحدّث الاسم + رقم الهوية بدون حجب واجهة المستخدم
    private void updatePatientFieldsFromAppt(ApptRow row) {
        if (getPatientName == null && getPatientID == null) return;

        // الاسم (خفيف)
        String name = (row == null || row.getPatientName() == null) ? "" : row.getPatientName();
        if (getPatientName != null) getPatientName.setText(name);

        // إن ما في صف صالح
        if (row == null || row.getId() <= 0) {
            if (getPatientID != null) getPatientID.setText("");
            return;
        }

        long apptId = row.getId();

        // جرّب الكاش أولاً
        String cached = apptPatientIdCache.get(apptId);
        if (cached != null) {
            if (getPatientID != null) getPatientID.setText(cached);
            return;
        }

        // مؤقتاً: إشارة خفيفة أنه لسه بتحميل
        if (getPatientID != null) getPatientID.setText("…");

        // حمّل في الخلفية
        new Thread(() -> {
            final String sql = "SELECT p.id AS pid, u.national_id AS nid " +
                    "FROM appointments a " +
                    "JOIN patients p ON p.id = a.patient_id " +
                    "JOIN users u ON u.id = p.user_id " +
                    "WHERE a.id = ?";
            String display = "";
            try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, apptId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long pid = rs.getLong("pid");
                        String nid = rs.getString("nid");
                        display = (nid != null && !nid.isBlank()) ? nid : Long.toString(pid);
                    }
                }
            } catch (SQLException ignored) {
                // نتجنب التوستات المتكررة هنا حتى ما نبطّئ التنقل
            }
            final String toShow = display;
            if (toShow != null && !toShow.isBlank()) {
                apptPatientIdCache.put(apptId, toShow);
            }
            Platform.runLater(() -> {
                if (getPatientID != null) getPatientID.setText(toShow == null ? "" : toShow);
            });
        }, "load-appt-nid-" + apptId).start();
    }

    private void updatePatientDetailsChart() {
        LocalDate sel = (dataPickerDashboard != null && dataPickerDashboard.getValue() != null)
                ? dataPickerDashboard.getValue()
                : LocalDate.now();
        refreshAppointmentStatusChart(sel);
    }


    // Graceful shutdown for listeners/executors
    void shutdown() {
        try {
            if (apptDbListener != null) apptDbListener.close();
        } catch (Exception ignore) {
        }
        try {
            autoRefreshExec.shutdownNow();
        } catch (Exception ignore) {
        }
        try {
            if (monitor != null) monitor.stop();
        } catch (Exception ignore) {
        }
    }

    /**
     * Resolve patient_id from UI: try selected patient row; then try numeric id; then fallback to national_id (9-digit).
     */
    private Long resolvePatientId() {
        // 1) From selected patient row in patients table (most reliable)
        try {
            PatientRow sel = (patientTable == null) ? null : patientTable.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getPatientId() > 0) return sel.getPatientId();
        } catch (Exception ignore) {
        }

        // 2) From text field (could be internal id or national id)
        String raw = (getPatientID == null) ? null : getPatientID.getText();
        if (raw == null) raw = (PatientIdTextField == null) ? null : PatientIdTextField.getText();
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        // 2a) If numeric and not 9 digits, treat as internal patients.id
        if (s.matches("\\d+") && s.length() != 9) {
            long id = Long.parseLong(s);
            if (patientExistsById(id)) return id;
        }

        // 2b) If 9-digit, treat as national_id and lookup patients.id via users
        if (s.matches("\\d{9}")) {
            Long pid = findPatientIdByNationalId(s);
            if (pid != null) return pid;
        }

        // 2c) Last chance: numeric direct id (even if 9 digits) – verify existence
        if (s.matches("\\d+")) {
            long id = Long.parseLong(s);
            if (patientExistsById(id)) return id;
        }

        return null;
    }

    private boolean patientExistsById(long id) {
        final String sql = "SELECT 1 FROM patients WHERE id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private Long findPatientIdByNationalId(String nid) {
        final String sql = "SELECT p.id FROM patients p JOIN users u ON u.id = p.user_id WHERE u.national_id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException ignore) {
        }
        return null;
    }

    // ===== Dashboard table: columns & actions =====
    private void wireDashboardTable() {
        if (TableAppInDashboard == null) return;

        // --- 0) Ensure pipeline exists: dashBase -> dashFiltered -> dashSorted ---
        if (dashBase == null) {
            dashBase = javafx.collections.FXCollections.observableArrayList();
        }
        if (dashFiltered == null) {
            dashFiltered = new javafx.collections.transformation.FilteredList<>(dashBase, r -> true);
        }
        if (dashSorted == null) {
            dashSorted = new javafx.collections.transformation.SortedList<>(dashFiltered);
        }

        // --- 1) Table wiring (idempotent) ---
        //TableAppInDashboard.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        try { dashSorted.comparatorProperty().bind(TableAppInDashboard.comparatorProperty()); } catch (Throwable ignore) {}
        if (TableAppInDashboard.getItems() != dashSorted) {
            TableAppInDashboard.setItems(dashSorted);
        }
        if (TableAppInDashboard.getPlaceholder() == null) {
            TableAppInDashboard.setPlaceholder(new Label("Loading..."));
        }

        // -------- Appointment ID (index shown 1..n) --------
        if (colAppointmentID != null) {
            colAppointmentID.setStyle("-fx-alignment: CENTER;");
            /*
            colAppointmentID.setMinWidth(10);
            colAppointmentID.setPrefWidth(30);
            colAppointmentID.setMaxWidth(30);

             */
            colAppointmentID.setCellFactory(col -> new TableCell<DoctorDAO.AppointmentRow, Number>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : String.valueOf(getIndex() + 1));
                }
            });
            colAppointmentID.setCellValueFactory(cd -> new SimpleIntegerProperty(getSafeIndexOf(cd.getValue()) + 1));
        }

        // -------- Patient Name --------
        if (colPatientNameDash != null) {
            colPatientNameDash.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().patientName));
            /*
            colPatientNameDash.setMinWidth(160);
            colPatientNameDash.setPrefWidth(160);
            //colPatientNameDash.setMaxWidth(250);

             */
        }

        // -------- Doctor Name --------
        if (colDoctorNameDash != null) {
            colDoctorNameDash.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().doctorName));
            /*
            colDoctorNameDash.setMinWidth(160);
            colDoctorNameDash.setPrefWidth(160);
            //colDoctorNameDash.setMaxWidth(250);

             */
        }

        // -------- Specialty --------
        if (colSpecialtyDash != null) {
            colSpecialtyDash.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().specialty));
            /*
            colSpecialtyDash.setMinWidth(120);
            colSpecialtyDash.setPrefWidth(120);
            //colSpecialtyDash.setMaxWidth(140);

             */
        }

        // -------- Date --------
        if (colAppintementDateDash != null) {
            colAppintementDateDash.setCellValueFactory(cd -> {
                LocalDateTime ldt = toLocal(cd.getValue().startAt);
                return new SimpleObjectProperty<>(ldt == null ? null : ldt.toLocalDate());
            });
            colAppintementDateDash.setEditable(false);
            colAppintementDateDash.setStyle("-fx-alignment: CENTER;");
            /*
            colAppintementDateDash.setMinWidth(100);
            colAppintementDateDash.setPrefWidth(100);
            //colAppintementDateDash.setMaxWidth(120);

             */
        }

        // -------- Time --------
        if (colAppintementTimeDash != null) {
            colAppintementTimeDash.setCellValueFactory(cd -> {
                LocalDateTime ldt = toLocal(cd.getValue().startAt);
                if (ldt == null) return new SimpleStringProperty("");
                LocalTime from = ldt.toLocalTime();
                LocalTime to = from.plusMinutes(DEFAULT_SESSION_MIN);
                return new SimpleStringProperty(from.format(SLOT_FMT_12H) + " \u2192 " + to.format(SLOT_FMT_12H));
            });
            colAppintementTimeDash.setStyle("-fx-alignment: CENTER;");
            /*
            colAppintementTimeDash.setMinWidth(180);
            colAppintementTimeDash.setPrefWidth(180);
            //colAppintementTimeDash.setMaxWidth(260);

             */

        }

        // -------- Room --------
        if (colRoomDash != null) {
            colRoomDash.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().location));
            colRoomDash.setStyle("-fx-alignment: CENTER;");
            /*
            colRoomDash.setMinWidth(100);
            colRoomDash.setPrefWidth(100);

             */
        }

        // -------- Action (button) --------
        if (colActionDash != null) {
            colActionDash.setStyle("-fx-alignment: CENTER;");
            /*
            colActionDash.setMinWidth(100);
            colActionDash.setPrefWidth(100);
            //colActionDash.setMaxWidth(100);
            colActionDash.setResizable(true);

             */

            colActionDash.setCellFactory(col -> new TableCell<DoctorDAO.AppointmentRow, Void>() {
                private final Button btn = new Button("Open");
                { btn.getStyleClass().add("action-btn"); btn.setFocusTraversable(false); }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    btn.setOnAction(e -> {
                        DoctorDAO.AppointmentRow r = getTableView().getItems().get(getIndex());
                        if (r == null) return;
                        showAppointmentPane();
                        if (TableINAppointment != null) {
                            ApptRow match = TableINAppointment.getItems().stream()
                                    .filter(a -> a.getId() == r.id)
                                    .findFirst().orElse(null);
                            if (match != null) {
                                TableINAppointment.getSelectionModel().select(match);
                                TableINAppointment.scrollTo(match);
                            } else {
                                TableINAppointment.getItems().stream()
                                        .filter(a -> java.util.Objects.equals(a.getPatientName(), r.patientName))
                                        .findFirst().ifPresent(a -> {
                                            TableINAppointment.getSelectionModel().select(a);
                                            TableINAppointment.scrollTo(a);
                                        });
                            }
                        }
                    });
                    setGraphic(btn);
                }
            });
        }

        // ختامًا: أول تحميل + فلترة (خفيفة جدًا)
        reloadDashboardAppointments();
        applyDashboardFilters();
    }


    private int getSafeIndexOf(DoctorDAO.AppointmentRow v) {
        if (v == null || TableAppInDashboard == null) return -1;
        return TableAppInDashboard.getItems().indexOf(v);
    }

    private void wireDashboardAppointmentsSearchDP() {
        if (searchAppointmentDach != null) {
            searchAppointmentDach.textProperty().addListener((obs, old, q) -> applyDashboardFilters());
        }
        reloadDashboardAppointments();
        applyDashboardFilters();
    }

    //    private void applyDashboardFilters() {
    //        String q = (searchAppointmentDach == null || searchAppointmentDach.getText() == null)
    //                ? "" : searchAppointmentDach.getText().trim().toLowerCase();
    //        LocalDate sel = (dataPickerDashboard == null) ? null : dataPickerDashboard.getValue();
    //
    //        dashFiltered.setPredicate(r -> {            // date filter
    //            LocalDateTime ldt = toLocal(r.startAt);
    //            if (sel != null) {
    //                if (ldt == null || !ldt.toLocalDate().equals(sel)) return false;
    //            }
    //            // search filter across multiple fields
    //            if (q.isEmpty()) return true;
    //            return (r.patientName != null && r.patientName.toLowerCase().contains(q)) ||
    //                    (r.doctorName != null && r.doctorName.toLowerCase().contains(q)) ||
    //                    (r.specialty != null && r.specialty.toLowerCase().contains(q)) ||
    //                    (r.location != null && r.location.toLowerCase().contains(q)) ||
    //                    (String.valueOf(r.id).contains(q));
    //        });
    //
    //        if (TableAppInDashboard != null) {
    //            TableAppInDashboard.refresh();
    //            if (sel != null && TableAppInDashboard.getItems().isEmpty()) {
    //                // messages handled on date listener
    //            }
    //        }
    //    }

    private void applyDashboardFilters() {
        String q = (searchAppointmentDach == null || searchAppointmentDach.getText() == null)
                ? "" : searchAppointmentDach.getText().trim().toLowerCase();

        // ✅ فلترة عامة (بدون فلترة تاريخ، لأنها موجودة في reloadDashboardAppointments)
        java.util.function.Predicate<DoctorDAO.AppointmentRow> predicate = r -> {
            if (r == null) return false;
            if (q.isEmpty()) return true;
            return (r.patientName != null && r.patientName.toLowerCase().contains(q))
                    || (r.doctorName != null && r.doctorName.toLowerCase().contains(q))
                    || (r.specialty != null && r.specialty.toLowerCase().contains(q))
                    || (r.location != null && r.location.toLowerCase().contains(q))
                    || (String.valueOf(r.id).contains(q));
        };

        if (dashFiltered != null) dashFiltered.setPredicate(predicate);
        if (filteredDash != null) filteredDash.setPredicate(predicate);

        if (TableAppInDashboard != null) {
            TableAppInDashboard.refresh();
        }
    }

    /**
     * تحديث مخطط حالات المواعيد حسب تاريخ محدد (BarChart)
     */
    private void refreshAppointmentStatusChart(LocalDate day) {
        if (appointmentStatusChart == null) return;
        if (day == null) {
            day = (dataPickerDashboard != null && dataPickerDashboard.getValue() != null)
                    ? dataPickerDashboard.getValue()
                    : LocalDate.now();
        }

        final LocalDate dayFinal = day;
        new Thread(() -> {
            java.util.Map<String, Integer> counts = java.util.Collections.emptyMap();
            try {
                counts = com.example.healthflow.dao.AppointmentJdbcDAO.countByStatusOnDate(dayFinal);
            } catch (Exception e) {
                e.printStackTrace();
            }
            final java.util.Map<String, Integer> dataMap = counts;

            Platform.runLater(() -> {
                // Reset & disable animations to avoid label glitches
                appointmentStatusChart.setAnimated(false);
                appointmentStatusChart.getData().clear();
                appointmentStatusChart.setLegendVisible(false); // save space

                // ---- X Axis (Category) – use short labels to fit space ----
                final String[] ORDER_FULL = {"SCHEDULED", "COMPLETED", "CANCELLED"};
                final String[] ORDER_SHORT = {"SCHED.", "COMP.", "CANCEL."};

                if (appointmentStatusChart.getXAxis() instanceof CategoryAxis cat) {
                    cat.setAnimated(false);
                    cat.setAutoRanging(false);
                    cat.setCategories(FXCollections.observableArrayList(ORDER_SHORT));
                    cat.setTickLabelRotation(0);
                    cat.setTickLabelGap(2);
                    cat.setStartMargin(8);
                    cat.setEndMargin(8);
                    cat.setStyle("-fx-tick-label-font-size: 12px;");
                }

                // ---- Y Axis (Number) – integer counts from 0 .. max ----
                int maxVal = Math.max(
                        Math.max(dataMap.getOrDefault("SCHEDULED", 0),
                                dataMap.getOrDefault("COMPLETED", 0)),
                        dataMap.getOrDefault("CANCELLED", 0)
                );
                int upper = Math.max(1, maxVal); // keep baseline visible when all zeros
                if (appointmentStatusChart.getYAxis() instanceof javafx.scene.chart.NumberAxis y) {
                    y.setAnimated(false);
                    y.setAutoRanging(false);
                    y.setLowerBound(0);
                    y.setUpperBound(upper);
                    y.setTickUnit(1);
                    y.setForceZeroInRange(true);
                }

                // ---- Build the series (map full keys -> short labels) ----
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(dayFinal.toString());

                for (int i = 0; i < ORDER_FULL.length; i++) {
                    String full = ORDER_FULL[i];
                    String shortL = ORDER_SHORT[i];
                    int v = dataMap.getOrDefault(full, 0);

                    XYChart.Data<String, Number> d = new XYChart.Data<>(shortL, v);
                    series.getData().add(d);

                    // Add tooltip when Node becomes available
                    d.nodeProperty().addListener((o, oldNode, newNode) -> {
                        if (newNode != null) {
                            Tooltip.install(newNode, new Tooltip(
                                    (full.charAt(0) + full.substring(1).toLowerCase()) + ": " + v
                            ));
                        }
                    });
                }

                appointmentStatusChart.getData().add(series);
            });

        }, "appt-status-chart").start();
    }



    // Ensure tables are bound to their proper Sorted/Filtered lists (idempotent)
    private void ensureTableBindings() {
        try {
            if (TableAppInDashboard != null && TableAppInDashboard.getItems() != dashSorted) {
                dashSorted.comparatorProperty().bind(TableAppInDashboard.comparatorProperty());
                TableAppInDashboard.setItems(dashSorted);
            }
        } catch (Throwable ignore) {
        }
        try {
            if (TableINAppointment != null && TableINAppointment.getItems() != sortedAppt) {
                sortedAppt.comparatorProperty().bind(TableINAppointment.comparatorProperty());
                TableINAppointment.setItems(sortedAppt);
            }
        } catch (Throwable ignore) {
        }
        try {
            if (DocTable_Recption != null && DocTable_Recption.getItems() != (doctorFiltered == null ? doctorData : doctorFiltered)) {
                if (doctorFiltered == null) doctorFiltered = new FilteredList<>(doctorData, r -> true);
                var sorted = new SortedList<>(doctorFiltered);
                sorted.comparatorProperty().bind(DocTable_Recption.comparatorProperty());
                DocTable_Recption.setItems(sorted);
            }
        } catch (Throwable ignore) {
        }
        try {
            if (patientTable != null && patientTable.getItems() != (filtered == null ? patientData : filtered)) {
                if (filtered == null) filtered = new FilteredList<>(patientData, r -> true);
                var sorted = new SortedList<>(filtered);
                sorted.comparatorProperty().bind(patientTable.comparatorProperty());
                patientTable.setItems(sorted);
            }
        } catch (Throwable ignore) {
        }
    }
    /** Ensure we remember patient selection even if items refresh or pane changes. */
    private void ensurePatientSelectionHook() {
        if (patientSelHooked) return;
        if (patientTable == null) return;
        patientSelHooked = true;

        var sm = patientTable.getSelectionModel();
        if (sm != null) {
            sm.setSelectionMode(SelectionMode.SINGLE);
            sm.selectedItemProperty().addListener((obs, ov, nv) -> {
                if (nv != null) selectedPatient = nv;
            });
        }

        // في حال حصل Click بدون firing للـ selectedItem لسبب ما
        patientTable.setOnMouseClicked(e -> {
            var cur = (patientTable.getSelectionModel() == null)
                    ? null : patientTable.getSelectionModel().getSelectedItem();
            if (cur != null) selectedPatient = cur;
        });
    }
    private PatientRow getSelectedPatientOrNull() {
        ensurePatientSelectionHook();
        if (selectedPatient != null) return selectedPatient;
        if (patientTable != null && patientTable.getSelectionModel() != null) {
            return patientTable.getSelectionModel().getSelectedItem();
        }
        return null;
    }

    // Unified, debounced UI refresh pipeline used by DB NOTIFY and manual triggers
    private void scheduleCoalescedRefresh() {
        if (uiRefresh == null) return;
        uiRefresh.request(() -> {
            // 0) Ensure tables are wired to the right backing lists (fixes empty tables like TableINAppointment)
            ensureTableBindings();

            // 1) Dashboard appointments (date picker respected inside)
            try {
                reloadDashboardAppointments();
            } catch (Throwable t) {
                System.err.println("[UI-Refresh] reloadDashboardAppointments: " + t);
            }
            try {
                applyDashboardFilters();
            } catch (Throwable t) {
                System.err.println("[UI-Refresh] applyDashboardFilters: " + t);
            }

            // 2) Chart & counters
            try {
                LocalDate day = (dataPickerDashboard == null) ? LocalDate.now() : dataPickerDashboard.getValue();
                if (appointmentStatusChart != null) refreshAppointmentStatusChart(day);
            } catch (Throwable t) {
                System.err.println("[UI-Refresh] refreshAppointmentStatusChart: " + t);
            }
            try {
                updateAppointmentCounters();
            } catch (Throwable t) {
                System.err.println("[UI-Refresh] updateAppointmentCounters: " + t);
            }

            // 3) Patients/Doctors datasets (background fetches but UI swap on FX thread)
            try {
                loadPatientsBG();
            } catch (Throwable t) {
                System.err.println("[UI-Refresh] loadPatientsBG: " + t);
            }
            try {
                new Thread(() -> {
                    try {
                        var list = DoctorDAO.loadDoctorsBG();
                        Platform.runLater(() -> {
                            doctorData.setAll(list);
                            ensureTableBindings();
                        });
                    } catch (Throwable ignore) {
                    }
                }, "doctors-reload").start();
            } catch (Throwable t) {
                System.err.println("[UI-Refresh] reload doctors: " + t);
            }

            // 4) Appointments pane table (if visible)
            try {
                loadAppointmentsTable();
                if (TableINAppointment != null) TableINAppointment.refresh();
            } catch (Throwable t) {
                System.err.println("[UI-Refresh] loadAppointmentsTable: " + t);
            }

            // 5) Final touch: refresh visuals
            try {
                if (TableAppInDashboard != null) TableAppInDashboard.refresh();
                if (DocTable_Recption != null) DocTable_Recption.refresh();
                if (patientTable != null) patientTable.refresh();
            } catch (Throwable ignore) {
            }
        });
    }

    private java.sql.Timestamp fetchMaxApptUpdatedAt(java.time.LocalDate day) {
        final String sql = "SELECT MAX(updated_at) FROM appointments WHERE appointment_date::date = ?";
        try (java.sql.Connection c = com.example.healthflow.db.Database.get();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, day);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getTimestamp(1) : null;
            }
        } catch (java.sql.SQLException e) {
            return null;
        }
    }

    private java.sql.Timestamp fetchMaxPatientUpdatedAt() {
        final String sql = "SELECT MAX(GREATEST(u.updated_at, p.updated_at)) " +
                "FROM users u JOIN patients p ON p.user_id = u.id";
        try (java.sql.Connection c = com.example.healthflow.db.Database.get();
             java.sql.PreparedStatement ps = c.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getTimestamp(1) : null;
        } catch (java.sql.SQLException e) {
            return null;
        }
    }


    /* ============ Init ============ */
    @FXML
    private void initialize() {
        // CSS attach (safe if scene null at init)
        if (rootPane != null) {
            var cssUrl = getClass().getResource("/com/example/healthflow/Design/ReceptionDesign.css");
            attachComboCss(rootPane);
            if (cssUrl != null) {
                String css = cssUrl.toExternalForm();
                if (rootPane.getScene() != null) {
                    if (!rootPane.getScene().getStylesheets().contains(css))
                        rootPane.getScene().getStylesheets().add(css);
                } else {
                    rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                        if (newScene != null && !newScene.getStylesheets().contains(css)) {
                            newScene.getStylesheets().add(css);
                        }
                    });
                }
            } else {
                System.err.println("Reception CSS not found at /com/example/healthflow/Design/ReceptionDesign.css");
            }
        }

        monitor.start();
        if (rootPane != null) {
            ConnectivityBanner banner = new ConnectivityBanner(monitor);
            rootPane.getChildren().add(0, banner);
            banner.prefWidthProperty().bind(rootPane.widthProperty());
        }
        OnlineBindings.disableWhenOffline(monitor,
                InsertButton, UpdateButton, deleteButton, clearBtn,
                DachboardButton, PatientsButton, AppointmentsButton, DoctorsButton);

        if (!listenerRegistered) {
            listenerRegistered = true;
            final boolean[] firstEmissionHandled = {false};
            monitor.onlineProperty().addListener((obs, wasOnline, isOnline) -> {
                if (!firstEmissionHandled[0]) {
                    firstEmissionHandled[0] = true;
                    lastNotifiedOnline = isOnline;
                    return;
                }
                if (lastNotifiedOnline != null && lastNotifiedOnline == isOnline) return;
                lastNotifiedOnline = isOnline;
            });
        }

        DachboardButton.setOnAction(e -> showDashboardPane());
        PatientsButton.setOnAction(e -> showPatientsPaneAction());
        AppointmentsButton.setOnAction(e -> showAppointmentPane());
        DoctorsButton.setOnAction(e -> showDoctorPane());

        if (AppointmentsButton != null) {
            appointmentsAccess = new javafx.beans.property.SimpleBooleanProperty(false);
            AppointmentsButton.disableProperty().bind(appointmentsAccess.not());

            appointmentsAccess.addListener((obs, oldVal, enabled) -> {
                try {
                    AppointmentsButton.setOpacity(enabled ? 1.0 : 0.6);
                    var css = AppointmentsButton.getStyleClass();
                    if (enabled) {
                        css.remove("btn-disabled");
                        if (!css.contains("btn-enabled")) css.add("btn-enabled");
                    } else {
                        css.remove("btn-enabled");
                        if (!css.contains("btn-disabled")) css.add("btn-disabled");
                    }
                } catch (Throwable ignore) {}
            });
        }


        disableAppointmentsAccess(); //for disable btn
        startClock();

        GenderComboBox.setItems(FXCollections.observableArrayList(Gender.values()));
        GenderComboBox.setValue(Gender.MALE);
        DateOfBirthPicker.setValue(null);

        if (AppointmentDate != null && AppointmentDate.getValue() == null) {
            AppointmentDate.setValue(LocalDate.now());
        }

        wirePatientTable();
        ensurePatientSelectionHook();
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
        clearBtn.setOnAction(e -> {
            selectedPatient = null;
            clearForm();
        });

        BookAppointmentFromPateint.setOnAction(e -> {
            // --- تشخيص سريع ---
            ensurePatientSelectionHook();
            PatientRow cached = selectedPatient;
            PatientRow direct = (patientTable != null && patientTable.getSelectionModel() != null)
                    ? patientTable.getSelectionModel().getSelectedItem() : null;
            int fIdx = (patientTable != null && patientTable.getFocusModel() != null)
                    ? patientTable.getFocusModel().getFocusedIndex() : -1;
            String formNid  = (getPatientID   != null && getPatientID.getText()   != null) ? getPatientID.getText().trim()   : "";
            String formName = (getPatientName != null && getPatientName.getText() != null) ? getPatientName.getText().trim() : "";
            System.out.println("[BookBtn] cached=" + (cached==null?"null":cached.getFullName())
                    + " direct=" + (direct==null?"null":direct.getFullName())
                    + " focusIdx=" + fIdx + " formNid=" + formNid + " formName=" + formName);

            // --- حسم المريض من 3 مصادر + الفورم كـ fallback ---
            PatientRow p = (cached != null) ? cached : direct;

            if (p == null && patientTable != null && fIdx >= 0 && fIdx < patientTable.getItems().size()) {
                Object o = patientTable.getItems().get(fIdx);
                if (o instanceof PatientRow pr) p = pr;
            }

            if (p == null && (!formNid.isEmpty() || !formName.isEmpty()) && patientTable != null && patientTable.getItems() != null) {
                for (Object o : patientTable.getItems()) {
                    if (!(o instanceof PatientRow pr)) continue;
                    boolean nidMatch  = !formNid.isEmpty()  && formNid.equalsIgnoreCase(String.valueOf(pr.getNationalId()));
                    boolean nameMatch = !formName.isEmpty() && formName.equalsIgnoreCase(String.valueOf(pr.getFullName()));
                    if (nidMatch || nameMatch) { p = pr; break; }
                }
            }

            if (p == null) {
                // آخر فallback: لو الفورم مليان، كمّل التنقل واملأ الحقول (بدون مسودة) عشان ما توقف شغلك
                if (!formName.isEmpty() || !formNid.isEmpty()) {
                    enableAppointmentsAccess();
                    showAppointmentPane();
                    if (getPatientName != null) getPatientName.setText(formName);
                    if (getPatientID   != null) getPatientID.setText(formNid);
                    selectNearestSlotOnNextRefresh = true;
                    Platform.runLater(this::applyAppointmentFilters);
                    showToast("warn", "Using form values (no table selection).");
                    return; // لاحقًا نكمّل إضافة draft لما نربط الـ NID بـ patient_id من الداتابيز
                }

                Alert a = new Alert(Alert.AlertType.WARNING);
                Stage stage = (Stage) rootPane.getScene().getWindow();
                a.initOwner(stage);
                a.initModality(Modality.WINDOW_MODAL);

                a.setTitle("Select a patient");
                a.setHeaderText(null);
                a.setContentText("Please select a patient from the table first.");
                a.showAndWait();
                showToast("warn", "Please select a patient from the table first.");
                return;
            }

            System.out.println("BookAppointmentFromPateint: " + p.getFullName() + "  Age :" + p.getAge());

            enableAppointmentsAccess();
            showAppointmentPane();

            if (getPatientName != null) getPatientName.setText(p.getFullName());
            if (getPatientID   != null) getPatientID.setText(p.getNationalId());

            if (DoctorspecialtyApp != null &&
                    (DoctorspecialtyApp.getItems() == null || DoctorspecialtyApp.getItems().isEmpty())) {
                loadSpecialtiesAsync();
            }

            addOrFocusDraftForPatient(p);
            selectNearestSlotOnNextRefresh = true;
            Platform.runLater(this::applyAppointmentFilters);
        });
        if (AppointmentsAnchorPane != null) {
            AppointmentsAnchorPane.visibleProperty().addListener((o, was, isNow) -> {
                if (Boolean.FALSE.equals(isNow)) {
                    disableAppointmentsAccess();
                }
            });
        }

        Platform.runLater(() -> {
            new Thread(() -> {
                try {
                    loadHeaderUser();
                } catch (Exception ignored) {
                }
            }, "hdr-user-load").start();
            new Thread(this::loadPatientsBG, "patients-load").start();
            new Thread(() -> {
                var list = DoctorDAO.loadDoctorsBG();
                Platform.runLater(() -> doctorData.setAll(list));
            }, "doctors-load").start();
        });
        if (cmbSlots != null) {
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
                    setText(empty || s == null ? "Select time"
                            : s.from().toLocalTime().format(SLOT_FMT_12H) + " \u2192 " +
                            s.to().toLocalTime().format(SLOT_FMT_12H));
                }
            });
            cmbSlots.setOnShown(e -> refreshSlots());
        } else {
            System.out.println("cmbSlots is null: Reception.fxml likely doesn't have fx:id=\"cmbSlots\" on a ComboBox");
        }

        if (AppointmentDate != null) AppointmentDate.valueProperty().addListener((o, a, b) -> refreshSlots());
        if (avilabelDoctorApp != null) avilabelDoctorApp.valueProperty().addListener((o, a, b) -> refreshSlots());

        showDashboardPane();

        // appointments wiring + load
        wireAppointmentsTables();
//        setupAppointmentSlotsListener();
        wireDashboardAppointmentsSearch();
        wireDashboardAppointmentsSearchDP();
        wireAppointmentDateFilter();      // لاستخدام datePiker مهم
        wireDashboardTable();
//
        wireAppointmentStatusFilter();  // ثبّت الافتراضي (SCHEDULED) أولًا
        wireAppointmentFilters();       // بعدين أربط باقي الفلاتر (التاريخ/البحث)
        loadFilteredAppointments();     // أخيرًا حمّل البيانات أول مرة

        ComboAnimations.applySmoothSelect(statusFilter, s -> s);
//        ComboAnimations.enableSlidingSelection(statusFilter, Duration.millis(260));
        ComboAnimations.delayHideOnSelect(statusFilter, Duration.seconds(0.1));
        if (filteredAppt != null) {
            filteredAppt.addListener((javafx.collections.ListChangeListener<ApptRow>) c -> updateNoAppointmentsBanner());
        }

        if (dataPickerDashboard != null && dataPickerDashboard.getValue() == null) {
            dataPickerDashboard.setValue(LocalDate.now());
        }

        if (dataPickerDashboard != null) {
            dataPickerDashboard.valueProperty().addListener((obs, oldD, newD) -> {
                reloadDashboardAppointments(); // عشان يعرض بيانات اليوم المختار
                applyDashboardFilters();
                refreshAppointmentStatusChart(newD);
                updateAppointmentCounters();
                if (newD != null) {
                    if (newD.isAfter(LocalDate.now())) {
                        showToast("warn", "The selected date is in the future. No appointments to show yet.");
                    } else if (filteredDash.getPredicate() != null && TableAppInDashboard != null && TableAppInDashboard.getItems().isEmpty()) {
                        showToast("info", "No appointments on this date.");
                    }
                }
            });
        }
        // Initial dashboard load (single consolidated call)
        refreshDashboardAll();

        if (clearSelectionDach != null) clearSelectionDach.setOnAction(e -> {
            if (TableAppInDashboard != null) TableAppInDashboard.getSelectionModel().clearSelection();
            if (searchAppointmentDach != null) searchAppointmentDach.clear();
            // إعادة التركيز لحقل البحث بعد المسح (اختياري)
            if (searchAppointmentDach != null)
                searchAppointmentDach.requestFocus();
        });
        if (clearSelectionDoctor != null) {
            clearSelectionDoctor.setOnAction(e -> {
                // إزالة التحديد من جدول الأطباء
                if (DocTable_Recption != null)
                    DocTable_Recption.getSelectionModel().clearSelection();

                // مسح النص داخل مربع البحث
                if (searchDoctor != null)
                    searchDoctor.clear();

                // إعادة التركيز لحقل البحث بعد المسح (اختياري)
                if (searchDoctor != null)
                    searchDoctor.requestFocus();
            });
        }
        if (clearSelectionPatient != null) {
            clearSelectionPatient.setOnAction(e -> {
                // 1) Clear selection in the patients table
                if (patientTable != null) {
                    patientTable.getSelectionModel().clearSelection();
                }

                // 2) Reset any cached/selected model object
                try { selectedPatient = null; } catch (Throwable ignore) {}

                // 3) Clear form fields explicitly (defensive null checks)
                try { if (getPatientID != null)   getPatientID.setText("");} catch (Throwable ignore) {}
                try { if (getPatientName != null) getPatientName.setText(""); } catch (Throwable ignore) {}
                try { if (PatientIdTextField != null) PatientIdTextField.clear(); } catch (Throwable ignore) {}
                try { if (FullNameTextField != null) FullNameTextField.clear(); } catch (Throwable ignore) {}
                try { if (PhoneTextField != null) PhoneTextField.clear(); } catch (Throwable ignore) {}
                try { if (medicalHistory != null) medicalHistory.clear(); } catch (Throwable ignore) {}
                try { if (GenderComboBox != null) GenderComboBox.getSelectionModel().clearSelection(); } catch (Throwable ignore) {}
                try { if (DateOfBirthPicker != null) DateOfBirthPicker.setValue(null); } catch (Throwable ignore) {}

                // 4) Clear the search box and return focus to it (optional UX)
                try {
                    if (search != null) {
                        search.clear();
                        search.requestFocus();
                    }
                } catch (Throwable ignore) {}

                // 5) If a general clearForm() exists, call it to keep logic centralized
                try { clearForm(); } catch (Throwable ignore) {}

                // 6) Refresh table visuals in case any cell depends on selection-bound properties
                try { if (patientTable != null) patientTable.refresh(); } catch (Throwable ignore) {}

                // Optional: toast feedback
                try { showToast("info", "Patient selection and form cleared."); } catch (Throwable ignore) {}
            });
        }

        // CRUD buttons
        if (insertAppointments != null) insertAppointments.setOnAction(e -> doInsertAppointment());
        if (updateAppointments != null) updateAppointments.setOnAction(e -> doUpdateAppointment());
        if (deleteAppointments != null) deleteAppointments.setOnAction(e -> doDeleteAppointment());
        if (clear_Appointments != null) clear_Appointments.setOnAction(e -> doClearAppointmentForm());
        if (AppointmentDate != null)
            AppointmentDate.valueProperty().addListener((o, a, b) -> {
                refreshSlots();
                enforceDateRules();
            });

        // initial data loads
        new Thread(this::loadAppointmentsTable, "appt-load").start();
        new Thread(this::updateAppointmentCounters, "appt-counts").start();

        // === التحديث اللحظي + تهيئة أولية ===

        startDbNotifications();
        scheduleCoalescedRefresh();  // تعبئة أولية
//        startLightweightPolling();

        Platform.runLater(() -> {
            var url = getClass().getResource(cssPath);
            javafx.scene.Node hook = (TableAppInDashboard != null) ? TableAppInDashboard : TableINAppointment;
            if (url != null && hook != null && hook.getScene() != null) {
                String css = url.toExternalForm();
                if (!hook.getScene().getStylesheets().contains(css)) {
                    hook.getScene().getStylesheets().add(css);
                }
            } else {
            }
        });

        Platform.runLater(() -> {
            // اختَر Node أكيد ملتصق بالمشهد
            javafx.scene.Node hook =
                    (rootPane != null) ? rootPane
                            : (TableAppInDashboard != null ? TableAppInDashboard : TableINAppointment);

            if (hook != null && hook.getScene() != null) {
                javafx.stage.Window win = hook.getScene().getWindow();
                if (win != null) {
                    win.addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, e -> shutdown());
                    win.addEventHandler(javafx.stage.WindowEvent.WINDOW_HIDDEN, e -> shutdown());
                }
            }
        });


        try {
            try {
                TableUtils.optOutCopy(colSpecialty, colDoctorNameAppointment, colStatusAppointment, colStartTime);
            } catch (Throwable ignore) {}

            TableUtils.applyUnifiedTableStyle(
                    rootPane,
                    TableINAppointment,
                    TableAppInDashboard,
                    patientTable,
                    DocTable_Recption
            );
            if (TableINAppointment != null) TableINAppointment.setEditable(true);
            setupInlineEditing();

            // Attach lightweight copy menu to ComboBox columns (non-intrusive)
            try {
                TableUtils.addCopyMenuNonIntrusive(colSpecialty, colDoctorNameAppointment, colStatusAppointment, colRoomNumber);
            } catch (Throwable ignore) {}



        } catch (Throwable ignore) {}

    }

    private void wireAppointmentFilters() {
        if (dataPickerAppointment != null) {
            dataPickerAppointment.setValue(LocalDate.now());
            dataPickerAppointment.valueProperty().addListener((obs, oldV, newV) -> {
                loadFilteredAppointments();
            });
        }
        if (statusFilter != null) {
            statusFilter.valueProperty().addListener((obs, oldV, newV) -> {
                loadFilteredAppointments();
            });
        }
    }
    private void wireAppointmentStatusFilter() {
        if (statusFilter == null) return;
        statusFilter.setItems(FXCollections.observableArrayList("ALL","SCHEDULED","COMPLETED","CANCELLED"));
        statusFilter.getSelectionModel().select("SCHEDULED"); // ← الافتراضي المتّسق مع التحميل الحالي
        statusFilter.valueProperty().addListener((obs, ov, nv) -> applyAppointmentFilters());
    }

    private void loadFilteredAppointments() {
        if (TableINAppointment == null) return;

        final LocalDate day = (dataPickerAppointment != null && dataPickerAppointment.getValue() != null)
                ? dataPickerAppointment.getValue()
                : LocalDate.now();

        final String status = (statusFilter != null && statusFilter.getValue() != null)
                ? statusFilter.getValue()
                : "ALL";

        new Thread(() -> {
            apptLoading = true;
            final java.util.List<ApptRow> rows = new java.util.ArrayList<>();

            final String sqlBase = """
            SELECT a.id, a.status, a.appointment_date, a.duration_minutes,
                   d.id AS doctor_id, du.full_name AS doctor_name,
                   p.id AS patient_id, pu.full_name AS patient_name,
                   d.specialty, COALESCE(a.location, d.room_number) AS room
            FROM appointments a
            JOIN doctors d ON d.id = a.doctor_id
            JOIN users du ON du.id = d.user_id
            JOIN patients p ON p.id = a.patient_id
            JOIN users pu ON pu.id = p.user_id
            WHERE a.appointment_date::date = ?
        """;

            final String sql = "ALL".equals(status)
                    ? sqlBase + " ORDER BY a.appointment_date"
                    : sqlBase + " AND a.status = ?::appt_status ORDER BY a.appointment_date";

            try (java.sql.Connection c = com.example.healthflow.db.Database.get();
                 java.sql.PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setDate(1, java.sql.Date.valueOf(day));
                if (!"ALL".equals(status)) ps.setString(2, status);

                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ApptRow r = new ApptRow();
                        r.setId(rs.getLong("id"));
                        r.setStatus(rs.getString("status"));
                        r.setDoctorName(rs.getString("doctor_name"));
                        r.setPatientName(rs.getString("patient_name"));
                        r.setSpecialty(rs.getString("specialty"));
                        r.setRoomNumber(rs.getString("room"));

                        java.sql.Timestamp ts = rs.getTimestamp("appointment_date");
                        if (ts != null) {
                            java.time.LocalDateTime dt = ts.toLocalDateTime();
                            r.setDate(dt.toLocalDate());
                            r.setTime(dt.toLocalTime());
                        }
                        r.setSessionTime(rs.getInt("duration_minutes"));
                        rows.add(r);
                    }
                }
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                // تأكد من الربط الصحيح
                ensureTableBindings();

                // إذا ما كان عندك مصدر مهيأ، عالطائر أنشئ واحد وربطه
                if (apptEditable == null) {
                    apptEditable = javafx.collections.FXCollections.observableArrayList();
                    // لو عندك filteredAppt/sortedAppt جهزهُم هنا إن لزم
                    if (filteredAppt == null) filteredAppt = new javafx.collections.transformation.FilteredList<>(apptEditable, r -> true);
                    if (sortedAppt == null) {
                        sortedAppt = new javafx.collections.transformation.SortedList<>(filteredAppt);
                        sortedAppt.comparatorProperty().bind(TableINAppointment.comparatorProperty());
                    }
                    TableINAppointment.setItems(sortedAppt);
                }

                // ضخ البيانات في المصدر (أفضل شيء لتجنب الوميض وكسر الفرز/الفلترة)
                apptEditable.setAll(rows);

                // لمسة تحديث خفيفة
                TableINAppointment.refresh();

                // أنهِ التحميل ثم حدّث اللافتة
                apptLoading = false;
                updateNoAppointmentsBanner();
            });
        }, "load-appts-filtered").start();
    }

    // --- Gate Appointments navigation ---
    private void enableAppointmentsAccess()  { appointmentsAccess.set(true);  }
    private void disableAppointmentsAccess() { appointmentsAccess.set(false); }


    /**
     * Attach /com/example/healthflow/Design/combobox.css to the current scene
     * and add the "hf-combo" style class to all ComboBox controls (including those inside tables).
     */
    private void attachComboCss(javafx.scene.Parent root) {
        if (root == null) return;
        final String cssPath = "/com/example/healthflow/Design/combobox.css";
        var url = getClass().getResource(cssPath);
        if (url != null) {
            String css = url.toExternalForm();
            if (root.getScene() != null) {
                var list = root.getScene().getStylesheets();
                if (!list.contains(css)) list.add(css);
            } else {
                root.sceneProperty().addListener((obs, oldS, newS) -> {
                    if (newS != null && !newS.getStylesheets().contains(css)) {
                        newS.getStylesheets().add(css);
                    }
                });
            }
        }
        // وسّم كل الـ ComboBox بـ "hf-combo" (الحاليّة والتي ستُنشأ لاحقًا في خلايا الجداول)
        javafx.application.Platform.runLater(() -> {
            try {
                for (javafx.scene.Node n : root.lookupAll(".combo-box")) {
                    if (n.getStyleClass() != null && !n.getStyleClass().contains("hf-combo")) {
                        n.getStyleClass().add("hf-combo");
                    }
                }
                // كرر الوسم بعد كل layout pulse (للكومبوبوكس المتولدة من CellFactory لاحقًا)
                root.sceneProperty().addListener((o, oldS, newS) -> {
                    if (newS == null) return;
                    newS.addPostLayoutPulseListener(() -> {
                        for (javafx.scene.Node n : root.lookupAll(".combo-box")) {
                            if (n.getStyleClass() != null && !n.getStyleClass().contains("hf-combo")) {
                                n.getStyleClass().add("hf-combo");
                            }
                        }
                    });
                });
            } catch (Throwable ignore) { }
        });
    }

    /**
     * Enables hover/selection-based column width expansion for a TableColumn, preserving original cellFactory logic.
     * If the column already has a cellFactory (e.g., for copyable or editable cells), it wraps it instead of replacing.
     */

    // لحتى الان مش راضية تشتغل صح فيها مشكلة
    private <R> void enableExpandAutoWidth(TableView<R> table, TableColumn<R, String> column, double maxWidth) {
        // keep any existing factory (copyable cells, inline editors, etc.)
        javafx.util.Callback<TableColumn<R, String>, TableCell<R, String>> original = column.getCellFactory();
        if (original == null) {
            original = tc -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };
        }

        javafx.util.Callback<TableColumn<R, String>, TableCell<R, String>> finalOriginal = original;
        column.setCellFactory(tc -> {
            TableCell<R, String> cell = finalOriginal.call(tc);

            // handlers to expand/restore
            Runnable expand = () -> {
                String txt = cell.getItem();
                if (txt == null || txt.isEmpty()) return;
                javafx.scene.text.Text t = new javafx.scene.text.Text(txt);
                t.setFont(cell.getFont());
                double needed = t.getLayoutBounds().getWidth() + 28; // padding
                double cur = column.getWidth();
                needed = Math.min(Math.max(needed, cur), maxWidth);

                _origColWidth.putIfAbsent(column, cur);
                table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
                column.setPrefWidth(needed);

                cell.setWrapText(false);
                cell.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
            };

            Runnable restore = () -> {
                Double orig = _origColWidth.get(column);
                if (orig != null) column.setPrefWidth(orig);
                cell.setWrapText(false);
                cell.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            };

            cell.setOnMouseEntered(e -> expand.run());
            cell.setOnMouseExited(e -> restore.run());
            cell.selectedProperty().addListener((o, ov, nv) -> { if (nv) expand.run(); else restore.run(); });
            cell.focusedProperty().addListener((o, ov, nv) -> { if (nv) expand.run(); else restore.run(); });

            return cell; // keep original graphic/text behavior
        });
    }

    // Helper: resolve the current Stage safely from rootPane or logout button
    // To Turn on LogOut Btn

    private Stage getCurrentStageSafely() {
        try {
            if (rootPane != null && rootPane.getScene() != null) {
                return (Stage) rootPane.getScene().getWindow();
            }
        } catch (Throwable ignore) {}

        try {
            if (LogOutBtn != null && LogOutBtn.getScene() != null) {
                return (Stage) LogOutBtn.getScene().getWindow();
            }
        } catch (Throwable ignore) {}

        return null;
    }

    // ---- Logout (single confirmation attached to this window) ----
    @FXML
    private void handleLogoutAction() {
        Stage stage = getCurrentStageSafely();

        // Single app-attached confirmation (no duplicate prompts)
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        alert.setTitle("Confirm Logout");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to log out?");
        if (stage != null) {
            alert.initOwner(stage);
            alert.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return; // user cancelled
        }

        // Proceed with logout and return to fixed-size Login
        doLogout(stage);
    }

    private void doLogout(Stage stage) {
        try {
            shutdown(); // graceful cleanup

            if (stage == null) {
                stage = getCurrentStageSafely();
            }

            // Navigate back to Login with fixed size (handled by Navigation)
            new Navigation().showLoginFixed(stage, monitor);

        } catch (Exception e) {
            showError("Logout", e);
        }
    }

    /** تنفيذ الخروج بدون أي حوارات إضافية */

    @FXML
    private void handleBack() {
        if (!navigationHistory.isEmpty()) {
            AnchorPane previous = navigationHistory.pop();
            // أخفِ الكل
            DashboardAnchorPane.setVisible(false);
            PatientAnchorPane.setVisible(false);
            AppointmentsAnchorPane.setVisible(false);
            DoctorAnchorPane.setVisible(false);

            // أظهر السابقة
            previous.setVisible(true);
        }
    }
    private AnchorPane getCurrentPane() {
        if (DashboardAnchorPane.isVisible()) return DashboardAnchorPane;
        if (PatientAnchorPane.isVisible()) return PatientAnchorPane;
        if (AppointmentsAnchorPane.isVisible()) return AppointmentsAnchorPane;
        if (DoctorAnchorPane.isVisible()) return DoctorAnchorPane;
        return null;
    }
    private void switchPane(AnchorPane paneToShow) {
        // خزن الصفحة الحالية قبل التبديل
        if (getCurrentPane() != paneToShow) {
            navigationHistory.push(getCurrentPane());
        }

        // أخفِ الكل
        DashboardAnchorPane.setVisible(false);
        PatientAnchorPane.setVisible(false);
        AppointmentsAnchorPane.setVisible(false);
        DoctorAnchorPane.setVisible(false);

        // أظهر الصفحة الجديدة
        paneToShow.setVisible(true);

    }

    // Disable any periodic UI refresh; rely only on DB NOTIFY
    private void disableAutoRefreshPeriodic() {
        try {
            if (autoRefreshExec != null && !autoRefreshExec.isShutdown()) {
                autoRefreshExec.shutdownNow();
            }
        } catch (Throwable ignore) {}
    }

}