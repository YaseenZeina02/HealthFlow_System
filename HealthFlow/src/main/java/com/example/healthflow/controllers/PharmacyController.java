
package com.example.healthflow.controllers;

import com.example.healthflow.core.inventory.DeductSupport;
import com.example.healthflow.dao.PrescriptionDAO;
import com.example.healthflow.dao.PrescriptionDAO.DashboardRow;
//import com.example.healthflow.dao.PrescriptionDAO.DeductSuppport.DeductRow;
import com.example.healthflow.service.AuthService.Session;
import com.example.healthflow.dao.PrescriptionItemDAO;
import com.example.healthflow.db.Database;
import com.example.healthflow.model.PrescriptionItem;
import com.example.healthflow.model.ItemStatus;
import com.example.healthflow.model.dto.PrescItemRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.example.healthflow.ui.fx.TableUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import com.example.healthflow.core.packaging.PackagingSupport;
import com.example.healthflow.core.packaging.PackagingSupport.PackagingInfo;
import com.example.healthflow.core.packaging.PackagingSupport.PackSuggestion;
import javafx.concurrent.Task; // (لو مش موجود بس)
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.ui.ConnectivityBanner;
import com.example.healthflow.ui.OnlineBindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import org.controlsfx.control.SegmentedButton;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.input.MouseEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import com.example.healthflow.model.dto.InventoryRow;
import com.example.healthflow.dao.PharmacyQueries;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import static com.example.healthflow.db.Database.shutdown;

public class PharmacyController {

    @FXML private Button LogOutBtn;

    private final ConnectivityMonitor monitor;
    // Controller state
    private DashboardRow selectedRow;
    private final ObservableList<DashboardRow> dashboardRows = FXCollections.observableArrayList();
    private final ObservableList<PrescItemRow> itemRows = FXCollections.observableArrayList();

    /* ====== Utilities / constants (match DoctorController style) ====== */
    private static final java.time.ZoneId APP_TZ = java.time.ZoneId.of("Asia/Gaza");
    private static final String ACTIVE_CLASS = "current";

    // ===== Dashboard loading state & debounce =====
    private volatile boolean dashboardLoading = false;
    private LocalDate lastLoadedDate = null;
    private PauseTransition dpDebounce;

    private final PharmacyQueries queries = new PharmacyQueries();
    // ===== Inventory summary thresholds =====

    private static final int LOW_STOCK_THRESHOLD_UNITS = 20;   // المخزون المنخفض: ≤ 20 وحدة
    private static final int EXPIRY_SOON_DAYS = 30;            // قريب الانتهاء: خلال 30 يومًا

    // Remember last folder used for Excel import/export
    private java.nio.file.Path lastExcelDir = null;

    @FXML
    private VBox rootPane;


    @FXML
    private Label AppointmentDate;

    @FXML
    private Label ApprovedNumber;

    @FXML
    private Button BackButton;

    @FXML
    private AnchorPane CenterAnchorPane;

    @FXML
    private Button DashboardButton;

    @FXML
    private Label DateOfDay;

    @FXML
    private Label DiagnosisView;

    @FXML
    private Label DoctorNameLabel;

    @FXML
    private Label ExpiringSoonMedicine;

    @FXML
    private DatePicker ExpiryDate;

    @FXML
    private Button Finish_Prescription;

    @FXML
    private AnchorPane InventoryAnchorPane;

    @FXML
    private Button InventoryButton;

    @FXML
    private Label LowStockMedicine;

    @FXML
    private TextField MedicineNameRecive;

    @FXML
    private TextField PatientNameTF;

    @FXML
    private Label PendingNumber;

    @FXML
    private Label PharmacistNameLabel;

    @FXML
    private TableView<DashboardRow> PresciptionsTable;

    @FXML
    private AnchorPane PrescriptionAnchorPane;

    @FXML
    private AnchorPane PrescriptionMedicationAnchorPane;

    @FXML
    private Button PrescriptionsButton;

    @FXML
    private Label PrescriptionsCompleteNum;

    @FXML
    private Label PrescriptionsTodayTotal;

    @FXML
    private Label PrescriptionsWatingNum;
    @FXML
    private Label Prescription_number;


    @FXML
    private AnchorPane ReceivePane;


    @FXML
    private Label RejectedNumber;

    @FXML
    private Button ReportsButton;



    @FXML
    private TableView<PrescItemRow> TablePrescriptionItems;

    @FXML
    private DatePicker PrescriptionDatePicker;




    @FXML
    private Label TitleBar;

    @FXML
    private Label TotalAppointments2;

    @FXML
    private Label TotalAppointments21;

    @FXML
    private Label TotalAppointments22;

    @FXML
    private Label TotalItemsNumber;

    @FXML
    private Label TotalmedicinesNumber;

    @FXML
    private Label UserIdLabel;

    @FXML
    private Label UsernameLabel;

    @FXML
    private Label alertLabel;

    @FXML
    private Label MedicineLabelDt;

    @FXML
    private FontIcon back;

    @FXML
    private TextField batchNum;

    @FXML
    private ToggleButton btnDeduct;

    @FXML
    private ToggleButton btnReceive;

    @FXML
    private TableColumn<DashboardRow, Void> colActionPhDashbord;

    @FXML
    private TableColumn<DashboardRow, String> colDate_Time;

    @FXML
    private TableColumn<PrescItemRow, Number> colStock;

    @FXML
    private TableColumn<PrescItemRow, String> colItemStatus;


    @FXML
    private TableColumn<DashboardRow, String> colDoctorName;

    @FXML
    private TableColumn<PrescItemRow, String> colDosage;



    @FXML
    private TableColumn<PrescItemRow, Number> colDose;

    @FXML
    private TableColumn<PrescItemRow, String> colForm;

    @FXML
    private TableColumn<PrescItemRow, Number> colIdx;



    @FXML
    private TableColumn<PrescItemRow, String> colMedicineName;

    @FXML private TableView<InventoryRow> TableMedicinesInventory;
    @FXML private TableColumn<InventoryRow, Number>   colSerialInventory;
    @FXML private TableColumn<InventoryRow, String>   colMedicineNameInventory; // اسم الدواء
    @FXML private TableColumn<InventoryRow, String>   colMedicineForm;
    @FXML private TableColumn<InventoryRow, String>   colMedicineBase;
    @FXML private TableColumn<InventoryRow, Number>   colQuantityInventory;
    @FXML private TableColumn<InventoryRow, String>   colMedicineBatchNextNumber;  // رقم الدفعة الأقدم
    @FXML private TableColumn<InventoryRow, Number>   colQtyNext;                  // كمية تلك الدفعة
    @FXML private TableColumn<InventoryRow, java.time.LocalDate> colExpiryNext;    // تاريخ انتهاء الدفعة
    @FXML private TableColumn<InventoryRow, String>   colReceivedBy;
    @FXML private TableColumn<InventoryRow, String>   colReceivedAt;

    // Backing data + filters/sorters for Inventory table
    private final ObservableList<InventoryRow> inventoryRows = FXCollections.observableArrayList();
    private FilteredList<InventoryRow> invFiltered;
    private SortedList<InventoryRow>   invSorted;

    @FXML
    private TableColumn<DashboardRow, String> colPatientName;

    @FXML
    private TableColumn<PrescItemRow, Void> colPresesItemAction;
    @FXML
    private TableColumn<PrescItemRow, Number> colQuantity;
    @FXML
    private TableColumn<PrescItemRow, Number> colSuggestionQty;

    private static final String SUGG_PLACEHOLDER = "—";



    @FXML
    private TableColumn<DashboardRow, Number> colSerialPhDashboard;

    @FXML
    private TableColumn<PrescItemRow, String> colStrength;

    @FXML
    private TableColumn<DashboardRow, String> colprescriptionStutus;



    @FXML
    private AnchorPane deductPane;

    @FXML
    private AnchorPane pharmacyDashboardAnchorPane;

    @FXML
    private TextField quantity;


    @FXML private Button saveBtnReceive;
    @FXML private Button downloadTemp;
    @FXML private Button importExcelFile;
    @FXML private Label labelFileDetails;

    @FXML
    private TextField searchDashbord;

    @FXML
    private TextField searchItems;

    @FXML
    private TextField searchOnInventory;

    @FXML
    private TextArea descriptionTf;

    @FXML
    private SegmentedButton segInv;
    @FXML private AnchorPane suggestPane;
    @FXML private TableView<MedicineSuggestion> tblMedSuggest;
    @FXML private TableColumn<MedicineSuggestion, String> colSuggName;
    @FXML private TableColumn<MedicineSuggestion, String> colSuggStrength;

    private final ObservableList<MedicineSuggestion> medSuggestions = FXCollections.observableArrayList();
    private Long selectedMedicineId = null;
    private final PauseTransition medDebounce = new PauseTransition(Duration.millis(200));
    private boolean sidebarGuardWired = false;

    @FXML
    private Label time;

    @FXML
    private Label userStatus;

    @FXML
    private Label welcomeUser;


    /*  Deduct Part  */
    @FXML private ComboBox<String> cmboTypeOfDeduct;
    @FXML private TableView<DeductSupport.DeductRow> TableToShowMedicineByBatchNumber;
    @FXML private TableColumn<DeductSupport.DeductRow,Number> colSerialDeduct ;
    @FXML private TableColumn<DeductSupport.DeductRow,String> colMedicineNameInventoryDeduct ;
    @FXML private TableColumn<DeductSupport.DeductRow,Number> colStockQtyInventoryDeduct ;
    @FXML private TableColumn<DeductSupport.DeductRow,String> colExpiryQtyInventoryDeduct ;
    @FXML private TextField deductBatchNumber_MN;
    @FXML private TextArea ReasonOfDeduct;
    @FXML private TextField quantityToDeduct;
    @FXML private Button saveBtnDeduct;
    @FXML private Button plusToQuantity;
    @FXML private Button minusToQuantity;
    @FXML private Label sugestMedicinesItem;

    private DeductSupport deductSupport; // from the redy class




    private volatile java.time.Instant lastPrescItemsFp = null;   // fingerprint لعناصر الوصفة المفتوحة
    private volatile java.time.Instant lastInventoryFp  = null;   // fingerprint للمخزون


    private com.example.healthflow.db.notify.DbNotifications dbn;
    private final javafx.animation.PauseTransition dashCoalesce  = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
    private final javafx.animation.PauseTransition itemsCoalesce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
    private final javafx.animation.PauseTransition invCoalesce   = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
    private volatile javafx.concurrent.Task<java.util.List<MedicineSuggestion>> currentMedTask;

    private static final class DashboardData {
        int total;
        int waiting;
        int completed;
        java.util.List<PrescriptionDAO.DashboardRow> rows;
    }

    private void setVisibleManaged(javafx.scene.Node node, boolean value) {
        if (node != null) {
            node.setVisible(value);
            node.setManaged(value);
        }
    }

    private void markNavActive(Button active) {
        Button[] all = {DashboardButton, PrescriptionsButton, InventoryButton};
        for (Button b : all) {
            if (b == null) continue;
            b.getStyleClass().remove(ACTIVE_CLASS);
            if (!b.getStyleClass().contains("nav-btn")) b.getStyleClass().add("nav-btn");
        }
        if (active != null && !active.getStyleClass().contains(ACTIVE_CLASS)) {
            active.getStyleClass().add(ACTIVE_CLASS);
        }
    }
    /** One-time setup for Inventory table columns and row factory. */
    private void initInventoryTableIfNeeded() {
        if (TableMedicinesInventory == null) return;

        if (TableMedicinesInventory.getItems() == null) {
            TableMedicinesInventory.setItems(inventoryRows);
        }

        // Serial #
        if (colSerialInventory != null) {
            colSerialInventory.setCellValueFactory(cf ->
                    new javafx.beans.property.SimpleIntegerProperty(
                            cf.getTableView().getItems().indexOf(cf.getValue()) + 1));
            colSerialInventory.setStyle("-fx-alignment: CENTER;");
        }

        // Value factories
        if (colMedicineNameInventory != null)
            colMedicineNameInventory.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        if (colMedicineForm != null)
            colMedicineForm.setCellValueFactory(new PropertyValueFactory<>("form"));
        if (colMedicineBase != null)
            colMedicineBase.setCellValueFactory(new PropertyValueFactory<>("baseUnit"));
        if (colQuantityInventory != null)
            colQuantityInventory.setCellValueFactory(new PropertyValueFactory<>("availableQuantity"));
        if (colMedicineBatchNextNumber != null)
            colMedicineBatchNextNumber.setCellValueFactory(new PropertyValueFactory<>("nextBatchNo"));
        if (colQtyNext != null)
            colQtyNext.setCellValueFactory(new PropertyValueFactory<>("nextBatchQty"));
        if (colExpiryNext != null)
            colExpiryNext.setCellValueFactory(new PropertyValueFactory<>("nextExpiry"));
        if (colReceivedBy != null)
            colReceivedBy.setCellValueFactory(new PropertyValueFactory<>("receivedBy"));
        if (colReceivedAt != null)
            colReceivedAt.setCellValueFactory(new PropertyValueFactory<>("receivedAt"));

        // لف نص اسم الدواء
        if (colMedicineNameInventory != null) {
            colMedicineNameInventory.setCellFactory(tc -> new TableCell<>() {
                private final Label lbl = new Label();
                { lbl.setWrapText(true); }
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    lbl.setText(item);
                    setGraphic(lbl);
                }
            });
        }

        // تلوين تاريخ الانتهاء
        if (colExpiryNext != null) {
            colExpiryNext.setCellFactory(tc -> new TableCell<>() {
                @Override protected void updateItem(java.time.LocalDate d, boolean empty) {
                    super.updateItem(d, empty);
                    if (empty || d == null) { setText(null); setStyle(""); return; }
                    setText(d.toString());
                    long days = java.time.temporal.ChronoUnit.DAYS
                            .between(java.time.LocalDate.now(APP_TZ), d);
                    if (days <= 0) setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:bold;"); // منتهي
                    else if (days <= 30) setStyle("-fx-text-fill:#b45309;");                 // قريب الانتهاء
                    else setStyle("");
                }
            });
        }

        if (TableMedicinesInventory != null) {
            TableMedicinesInventory.setRowFactory(tv -> {
                TableRow<InventoryRow> row = new TableRow<>();
                row.setOnMouseClicked(ev -> {
                    if (ev.getClickCount() == 2 && !row.isEmpty()) {
                        showInventoryDetailsDialog(row.getItem());
                    }
                });
                return row;
            });
        }

        if (TableMedicinesInventory.getPlaceholder() == null) {
            TableMedicinesInventory.setPlaceholder(new Label("No content in table"));
        }

        // البحث الحيّ (مرة واحدة)
        if (searchOnInventory != null && invFiltered == null) {
            invFiltered = new FilteredList<>(inventoryRows, r -> true);
            invSorted   = new SortedList<>(invFiltered);
            invSorted.comparatorProperty().bind(TableMedicinesInventory.comparatorProperty());
            TableMedicinesInventory.setItems(invSorted);

            searchOnInventory.textProperty().addListener((obs, a, b) -> {
                String q = (b == null ? "" : b.trim().toLowerCase());
                invFiltered.setPredicate(r -> {
                    if (q.isEmpty()) return true;
                    return (r.getDisplayName() != null && r.getDisplayName().toLowerCase().contains(q))
                            || (r.getForm() != null && r.getForm().toLowerCase().contains(q))
                            || (r.getBaseUnit() != null && r.getBaseUnit().toLowerCase().contains(q))
                            || (r.getNextBatchNo() != null && r.getNextBatchNo().toLowerCase().contains(q))
                            || (r.getReceivedBy() != null && r.getReceivedBy().toLowerCase().contains(q))
                            || (r.getReceivedAt() != null && r.getReceivedAt().toLowerCase().contains(q));
                });
            });
        }
        applyInventoryNullPlaceholders();
    }

    private void showInventoryQuickDetails(InventoryRow r) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Medicine Details");
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        GridPane gp = new GridPane(); gp.setHgap(12); gp.setVgap(8);
        int i = 0;
        gp.addRow(i++, new Label("Name:"),  new Label(r.getDisplayName()));
        gp.addRow(i++, new Label("Form:"),  new Label(r.getForm()==null? "" : r.getForm()));
        gp.addRow(i++, new Label("Base:"),  new Label(r.getBaseUnit()==null? "" : r.getBaseUnit()));
        gp.addRow(i++, new Label("Available:"), new Label(String.valueOf(r.getAvailableQuantity())));
        gp.addRow(i++, new Label("Next Batch:"), new Label(r.getNextBatchNo()==null? "—" : r.getNextBatchNo()));
        gp.addRow(i++, new Label("Qty (Next):"), new Label(r.getNextBatchQty()==null? "—" : String.valueOf(r.getNextBatchQty())));
        gp.addRow(i++, new Label("Expiry (Next):"), new Label(r.getNextExpiry()==null? "—" : r.getNextExpiry().toString()));
        gp.addRow(i++, new Label("Received By:"), new Label(r.getReceivedBy()==null? "—" : r.getReceivedBy()));
        gp.addRow(i++, new Label("Received At:"), new Label(r.getReceivedAt()==null? "—" : r.getReceivedAt()));
        dlg.getDialogPane().setContent(gp);
        dlg.showAndWait();
    }


    private void startClock() {
        if (time != null) {
            var tf = java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss a");
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO, e -> time.setText(java.time.ZonedDateTime.now(APP_TZ).format(tf))),
                    new KeyFrame(Duration.seconds(1))
            );
            tl.setCycleCount(Timeline.INDEFINITE);
            tl.play();
        }
        if (DateOfDay != null) {
            var df = java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy");
            var todayGaza = java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
            DateOfDay.setText(todayGaza.format(df));
        }
    }

    // Resolve pharmacist/user display names and support both users.id and pharmacists.id in UserIdLabel.
    // Resolve pharmacist/user labels from the authenticated session FIRST; then fall back to labels/DB.
    private void resolveLoggedInUserLabels() {
        try {
            String full = null;
            Long userId = null;

            // --- 0) Primary source: Auth session (set by LoginController) ---
            try {
                var u = Session.get();
                if (u != null) {
                    userId = u.getId();
                    full = u.getFullName();
                }
            } catch (Throwable ignored) {}

            // --- 1) If session missing, try reading users.id from UserIdLabel and fetch full_name ---
            if (full == null || full.isBlank()) {
                Long rawId = null;
                if (UserIdLabel != null) {
                    String uidTxt = UserIdLabel.getText();
                    if (uidTxt != null) {
                        uidTxt = uidTxt.replaceAll("[^0-9]", "").trim();
                        if (!uidTxt.isEmpty()) {
                            try { rawId = Long.parseLong(uidTxt); } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                if (rawId != null) {
                    try (Connection c = Database.get();
                         PreparedStatement ps = c.prepareStatement("SELECT id, full_name FROM users WHERE id = ?")) {
                        ps.setLong(1, rawId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                userId = rs.getLong(1);
                                full = rs.getString(2);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[PharmacyController] resolveLoggedInUserLabels users.id lookup failed: " + e.getMessage());
                    }
                }
            }

            // --- 2) Final fallback: use UsernameLabel text if it looks real ---
            if ((full == null || full.isBlank()) && UsernameLabel != null) {
                String txt = UsernameLabel.getText();
                if (txt != null && !txt.isBlank() && !txt.equalsIgnoreCase("user name")) {
                    full = txt.trim();
                }
            }

            // --- Apply to UI ---
            if (full != null && !full.isBlank()) {
                String first = full.trim().split("\\s+")[0];
                if (welcomeUser != null) welcomeUser.setText(first);
                if (PharmacistNameLabel != null) PharmacistNameLabel.setText(full);
                if (UsernameLabel != null) UsernameLabel.setText(full);
            } else {
                System.out.println("[PharmacyController] resolveLoggedInUserLabels: full name not resolved; check Session and labels.");
            }

            if (UserIdLabel != null && userId != null) {
                UserIdLabel.setText(String.valueOf(userId));
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] resolveLoggedInUserLabels: " + ex);
        }
    }
    // Helper to get selected date or today (Asia/Gaza)
    private LocalDate getSelectedDateOrToday() {
        try {
            LocalDate picked = (PrescriptionDatePicker != null) ? PrescriptionDatePicker.getValue() : null;
            if (picked != null) return picked;
        } catch (Throwable ignored) {}
        return java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
    }

    // Helper: is the currently opened prescription read-only (completed)?
    private boolean isCurrentPrescriptionReadOnly() {
        return selectedRow != null && selectedRow.status != null && "DISPENSED".equalsIgnoreCase(selectedRow.status.name());
    }

    private Long requireCurrentPharmacistId() {
        Long userId = null;
        String email = null;
        String role  = null;

        // 1) Session
        try {
            var u = Session.get();
            if (u != null) { userId = u.getId(); email = u.getEmail(); }
        } catch (Throwable ignored) {}

        if (userId == null) {
            new Alert(Alert.AlertType.ERROR, "No logged-in user. Please log in again.").showAndWait();
            return null;
        }

        try (Connection c = Database.get()) {
            // 2) Resolve role + email (fallback)
            try (PreparedStatement ps = c.prepareStatement("SELECT role, email FROM users WHERE id = ?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        role  = rs.getString("role");
                        if (email == null) email = rs.getString("email");
                    }
                }
            }

            if (role == null || !"PHARMACIST".equalsIgnoreCase(role)) {
                new Alert(Alert.AlertType.ERROR,
                        "You are not authorized as a pharmacist.\n(Your role: " + String.valueOf(role) + ")")
                        .showAndWait();
                return null;
            }

            // 3) Try by user_id
            Long pharmId = null;
            try (PreparedStatement ps = c.prepareStatement("SELECT id FROM pharmacists WHERE user_id = ?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) pharmId = rs.getLong(1); }
            }
            if (pharmId != null) return pharmId;

            // 4) Fallback by email (legacy/migrated)
            if (email != null && !email.isBlank()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT p.id FROM pharmacists p JOIN users u ON u.id = p.user_id " +
                                "WHERE LOWER(u.email)=LOWER(?) LIMIT 1")) {
                    ps.setString(1, email.trim());
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) pharmId = rs.getLong(1); }
                }
                if (pharmId != null) return pharmId;
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] requireCurrentPharmacistId error: " + ex);
            new Alert(Alert.AlertType.ERROR, "Failed to resolve pharmacist.\n" + ex.getMessage()).showAndWait();
            return null;
        }

        new Alert(Alert.AlertType.ERROR,
                "Your account has pharmacist role but is not linked in the pharmacists table.\n" +
                        "Please contact the administrator to link your account.")
                .showAndWait();
        return null;
    }

    private java.time.OffsetDateTime fetchAppointmentDateById(Long apptId) {
        if (apptId == null) return null;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT appointment_date FROM appointments WHERE id = ?")) {
            ps.setLong(1, apptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp(1);
                    if (ts != null) return ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
                }
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] fetchAppointmentDateById: " + ex);
        }
        return null;
    }

    private void refreshPharmacyDashboardCounts() {
        if (PrescriptionsTodayTotal == null && PrescriptionsWatingNum == null && PrescriptionsCompleteNum == null) return;
        LocalDate day = getSelectedDateOrToday();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("""
             SELECT
                 COUNT(*) AS total,
                 COUNT(*) FILTER (WHERE status = 'PENDING') AS waiting,
                 COUNT(*) FILTER (WHERE status IN ('APPROVED','DISPENSED')) AS completed
             FROM prescriptions
             WHERE created_at::date = ?
         """)) {
            ps.setDate(1, java.sql.Date.valueOf(day));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (PrescriptionsTodayTotal != null) PrescriptionsTodayTotal.setText(String.valueOf(rs.getInt("total")));
                    if (PrescriptionsWatingNum != null) PrescriptionsWatingNum.setText(String.valueOf(rs.getInt("waiting")));
                    if (PrescriptionsCompleteNum != null) PrescriptionsCompleteNum.setText(String.valueOf(rs.getInt("completed")));
                }
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] refreshPharmacyDashboardCounts: " + ex);
            if (alertLabel != null) alertLabel.setText("Failed to refresh counts: " + ex.getMessage());
        }
    }

    /* ====== Diagnostics for FXML wiring ====== */
    private boolean warnIfMissing() {
        StringBuilder sb = new StringBuilder();
        if (pharmacyDashboardAnchorPane == null) sb.append("pharmacyDashboardAnchorPane, ");
        if (PrescriptionAnchorPane == null)     sb.append("PrescriptionAnchorPane, ");
        if (InventoryAnchorPane == null)        sb.append("InventoryAnchorPane, ");
        if (DashboardButton == null)            sb.append("DashboardButton, ");
        if (PrescriptionsButton == null)        sb.append("PrescriptionsButton, ");
        if (InventoryButton == null)            sb.append("InventoryButton, ");
        if (BackButton == null)                 sb.append("BackButton, ");
        if (btnReceive == null)                 sb.append("btnReceive, ");
        if (btnDeduct == null)                  sb.append("btnDeduct, ");
        if (segInv == null)                     sb.append("segInv, ");
        if (sb.length() > 0) {
            String miss = sb.substring(0, sb.length() - 2);
            System.out.println("[PharmacyController] Missing fx:id(s): " + miss);
            if (alertLabel != null) {
                alertLabel.setText("Missing fx:id(s): " + miss);
            }
            return true;
        }
        return false;
    }

    /* ====== Section switching (Dashboard / Prescriptions / Inventory) ====== */
    private void showDashboardPane() {
        setPrescriptionSidebarState(false, false);
        wireSidebarGuardsIfNeeded();
        setVisibleManaged(pharmacyDashboardAnchorPane, true);
        setVisibleManaged(PrescriptionAnchorPane, false);
        setVisibleManaged(InventoryAnchorPane, false);
        markNavActive(DashboardButton);
        System.out.println("[PharmacyController] showDashboardPane -> ensure listeners running");
        startPharmacyDbNotifications();
        loadDashboardAsync(false);
    }

    private void loadDashboardTable() {
        loadDashboardAsync(true);
    }
    private void loadDashboardAsync(boolean force) {
        LocalDate day = getSelectedDateOrToday();
        if (!force && lastLoadedDate != null && lastLoadedDate.equals(day) && !dashboardRows.isEmpty()) {
            return; // already loaded for this day
        }
        if (dashboardLoading) return;
        dashboardLoading = true;
        lastLoadedDate = day;

        if (alertLabel != null) alertLabel.setText("Loading " + day + " ...");
        System.out.println("[PharmacyController] loadDashboardAsync start day=" + day);
        if (PresciptionsTable != null) PresciptionsTable.setPlaceholder(new Label("Loading..."));

        // Busy UI hint
        if (rootPane != null) rootPane.setCursor(Cursor.WAIT);
        if (DashboardButton != null && !DashboardButton.disableProperty().isBound()) {
            DashboardButton.setDisable(true);
        }

        Task<DashboardData> task = new Task<>() {
            @Override protected DashboardData call() throws Exception {
                DashboardData data = new DashboardData();
                try (Connection c = Database.get()) {
                    PrescriptionDAO dao = new PrescriptionDAO();
                    data.total     = dao.countTotalOnDate(c, day);
                    data.waiting   = dao.countPendingOnDate(c, day);
                    data.completed = dao.countCompletedOnDate(c, day);
                    data.rows      = dao.listDashboardRowsByDateAndStatus(c, day, null);
                }
                return data;
            }
        };

        task.setOnSucceeded(ev -> {
            DashboardData d = task.getValue();
            System.out.println("[PharmacyController] loadDashboardAsync SUCCESS day=" + lastLoadedDate
                    + " rows=" + (d == null || d.rows == null ? 0 : d.rows.size()));

            // Update counts
            if (d != null) {
                if (PrescriptionsTodayTotal != null)   PrescriptionsTodayTotal.setText(String.valueOf(d.total));
                if (PrescriptionsWatingNum != null)    PrescriptionsWatingNum.setText(String.valueOf(d.waiting));
                if (PrescriptionsCompleteNum != null)  PrescriptionsCompleteNum.setText(String.valueOf(d.completed));
                // Update table
                dashboardRows.setAll(d.rows == null ? java.util.Collections.emptyList() : d.rows);
            }
            if (PresciptionsTable != null) PresciptionsTable.setPlaceholder(new Label(dashboardRows.isEmpty() ? "No data" : ""));

            // Finalize UI state
            dashboardLoading = false;
            if (rootPane != null) rootPane.setCursor(Cursor.DEFAULT);
            if (DashboardButton != null && !DashboardButton.disableProperty().isBound()) {
                DashboardButton.setDisable(false);
            }
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            System.err.println("[PharmacyController] loadDashboardAsync FAILED day=" + lastLoadedDate + " ex=" + ex);
            if (alertLabel != null) alertLabel.setText("Failed to load dashboard: " + (ex == null ? "Unknown error" : ex.getMessage()));
            if (PresciptionsTable != null) PresciptionsTable.setPlaceholder(new Label("Failed to load"));

            // Finalize UI state
            dashboardLoading = false;
            if (rootPane != null) rootPane.setCursor(Cursor.DEFAULT);
            if (DashboardButton != null && !DashboardButton.disableProperty().isBound()) {
                DashboardButton.setDisable(false);
            }
        });

        Thread th = new Thread(task, "pharm-dashboard-loader");
        th.setDaemon(true);
        th.start();
    }


    // ---- Auto-refresh (polling-based) for the pharmacy dashboard ----
    private final java.util.concurrent.ScheduledExecutorService pharmAutoRefresher =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "pharm-auto-refresh");
                t.setDaemon(true);
                return t;
            });
    private volatile java.time.Instant lastDashFingerprint = null;
    private volatile boolean autoRefreshStarted = false;


    // ===== Inventory: loader =====
    private void reloadInventoryTable() {
        if (TableMedicinesInventory == null) return;
        initInventoryTableIfNeeded();

        Task<ObservableList<InventoryRow>> task = new Task<>() {
            @Override protected ObservableList<InventoryRow> call() throws Exception {
                ObservableList<InventoryRow> rows = FXCollections.observableArrayList();
                try (Connection c = Database.get()) {
                    rows.addAll(queries.getInventoryOverview(c, APP_TZ));
                }
                return rows;
            }
        };
        task.setOnSucceeded(ev -> inventoryRows.setAll(task.getValue()));
        updateInventorySummary();

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            System.err.println("[PharmacyController] reloadInventoryTable FAILED: " + ex);
            if (TableMedicinesInventory != null)
                TableMedicinesInventory.setPlaceholder(new Label("Failed to load"));
        });
        Thread th = new Thread(task, "inv-overview-loader");
        th.setDaemon(true);
        th.start();
    }

    private boolean isDashboardVisible() {
        return pharmacyDashboardAnchorPane != null && pharmacyDashboardAnchorPane.isVisible();
    }

    private boolean isPrescriptionVisible() {
        return PrescriptionAnchorPane != null && PrescriptionAnchorPane.isVisible();
    }

    private boolean isInventoryVisible() {
        return InventoryAnchorPane != null && InventoryAnchorPane.isVisible();
    }

    /** يعيد تحميل عناصر الوصفة المفتوحة (إن وُجدت). */
    private void reloadCurrentPrescriptionItems() {
        if (selectedRow != null && selectedRow.prescriptionId > 0) {
            loadPrescriptionItems(selectedRow.prescriptionId);
            // بعد ما نحمّل العناصر، حدّث العدّادات
            refreshPrescriptionItemCounts(selectedRow.prescriptionId);
        }
    }


    /** حوار صرف ذكي: عبوات/أشرطة أو وحدات (فراطة) مع حساب تلقائي وإشارة إن كانت كاملة أو فراطة */
    private DispenseDecision showPackAwareDispenseDialog(PrescItemRow r) {
        int prescribed = Math.max(0, r.getQuantity());
        int inStock    = Math.max(0, r.getStockAvailable());
        int requested  = prescribed;

        PackagingSupport.PackagingInfo p =
                (r.getMedicineId() > 0) ? fetchPackaging(r.getMedicineId()) : null;

        // Suggested default from helper
        PackagingSupport.PackSuggestion sugg = PackagingSupport.suggestPackFor(requested, p);

        Dialog<DispenseDecision> dlg = new Dialog<>();
        dlg.setTitle("Dispense quantity");
        dlg.setHeaderText("How do you want to dispense? (packs or units)");

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, okType);

        // Modes
        ToggleGroup mode = new ToggleGroup();
        RadioButton byPack  = new RadioButton("By pack");
        RadioButton byUnits = new RadioButton("By units");
        byPack.setToggleGroup(mode);
        byUnits.setToggleGroup(mode);
        byPack.setSelected(true);

        // Limits line
        Label limits = new Label("Prescribed: " + prescribed + "  |  In stock: " + inStock);

        // Pack choice
        ChoiceBox<String> unitChoice = new ChoiceBox<>();
        java.util.List<String> opts = new java.util.ArrayList<>();
        if (p != null) {
            if (p.mlPerBottle != null)       opts.add("BOTTLE");
            else if (p.gramsPerTube != null) opts.add("TUBE");
            else {
                if (p.blistersPerBox != null && p.tabletsPerBlister != null) opts.add("BOX");
                if (p.tabletsPerBlister != null)                              opts.add("BLISTER");
            }
        }
        opts.add("UNIT");
        unitChoice.setItems(FXCollections.observableArrayList(opts));
        String defaultUnit = (sugg != null && opts.contains(sugg.unit)) ? sugg.unit : opts.get(0);
        unitChoice.getSelectionModel().select(defaultUnit);

        // Spinner (we'll clamp later based on unit)
        Spinner<Integer> packCount = new Spinner<>();
        packCount.setEditable(true);

        // Helper: units per selected pack
        java.util.function.Function<String, Integer> unitsPerPack = u -> {
            if (p == null) return 1;
            if ("BOX".equals(u) && p.tabletsPerBlister != null && p.blistersPerBox != null)
                return p.tabletsPerBlister * p.blistersPerBox;
            if ("BLISTER".equals(u) && p.tabletsPerBlister != null)
                return p.tabletsPerBlister;
            if ("BOTTLE".equals(u) || "TUBE".equals(u))
                return 1;
            return 1; // UNIT
        };

        // Set spinner bounds/initial value based on current unit + limits
        Runnable initPackSpinner = () -> {
            String u0 = unitChoice.getSelectionModel().getSelectedItem();
            int perPack = Math.max(1, unitsPerPack.apply(u0));
            int maxByPrescribed = (prescribed <= 0) ? 10_000 : Math.max(1, prescribed / perPack);
            int maxByStock      = (inStock    <= 0) ? 10_000 : Math.max(1, inStock    / perPack);
            int max             = Math.max(1, Math.min(maxByPrescribed, maxByStock));

            int suggested = (sugg != null && u0 != null && u0.equals(sugg.unit)) ? Math.max(1, sugg.count) : 1;
            int defVal    = Math.min(suggested, max); // no arbitrary default like 5

            SpinnerValueFactory.IntegerSpinnerValueFactory vf =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, defVal);
            packCount.setValueFactory(vf);
        };

        // Breakdown lines
        Label breakdown = new Label(); breakdown.setWrapText(true);
        Label countHint = new Label(); countHint.setWrapText(true); // e.g., "= 3 × 10 = 30 units"

        // Units free entry (+/-)
        TextField unitsField = new TextField(String.valueOf(requested));
        Button btnMinus = new Button("–");
        Button btnPlus  = new Button("+");
        unitsField.setDisable(true); btnMinus.setDisable(true); btnPlus.setDisable(true);
        HBox unitsRow = new HBox(6, new Label("Units:"), btnMinus, unitsField, btnPlus);
        unitsRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Results and warnings
        Label reverse = new Label(); reverse.setWrapText(true);  // e.g., "42 units ≈ 4 BLISTER + 2 UNIT"
        Label calc = new Label();    calc.setWrapText(true);
        Label warn = new Label();    warn.getStyleClass().add("text-danger");

        VBox box = new VBox(8,
                limits,
                new HBox(10, byPack, byUnits),
                new HBox(10, new Label("Pack:"), unitChoice, new Label("Count:"), packCount),
                breakdown,
                countHint,
                unitsRow,
                reverse,
                calc,
                warn
        );
        dlg.getDialogPane().setContent(box);

        final Button okBtn = (Button) dlg.getDialogPane().lookupButton(okType);

        // Compute reverse (units -> packs)
        java.util.function.Function<Integer, String> reversePacks = (Integer units) -> {
            if (p == null || units == null) return "";
            Integer perBlister = p.tabletsPerBlister;
            Integer blistersPerBox = p.blistersPerBox;
            if (perBlister == null && blistersPerBox == null) return "";
            int u = Math.max(0, units);
            StringBuilder sb = new StringBuilder("≈ ");
            boolean any = false;
            if (perBlister != null && blistersPerBox != null) {
                int perBox = perBlister * blistersPerBox;
                int nBox = (perBox > 0) ? (u / perBox) : 0; u = (perBox > 0) ? (u % perBox) : u;
                if (nBox > 0) { sb.append(nBox).append(" BOX"); any = true; }
            }
            if (perBlister != null) {
                int nBl = (perBlister > 0) ? (u / perBlister) : 0; u = (perBlister > 0) ? (u % perBlister) : u;
                if (any && nBl > 0) sb.append(" + ");
                if (nBl > 0) { sb.append(nBl).append(" BLISTER"); any = true; }
            }
            if (u > 0) { if (any) sb.append(" + "); sb.append(u).append(" UNIT"); any = true; }
            return any ? sb.toString() : "";
        };

        Runnable recompute = () -> {
            boolean free = byUnits.isSelected();
            unitsField.setDisable(!free);
            btnMinus.setDisable(!free);
            btnPlus.setDisable(!free);
            unitChoice.setDisable(free);
            packCount.setDisable(free);

            String u = unitChoice.getSelectionModel().getSelectedItem();
            String btxt = "";
            if (p != null) {
                if ("BOX".equals(u) && p.tabletsPerBlister != null && p.blistersPerBox != null) {
                    btxt = "1 BOX = " + p.blistersPerBox + " BLISTER × " + p.tabletsPerBlister + " UNIT = "
                            + (p.blistersPerBox * p.tabletsPerBlister) + " units";
                } else if ("BLISTER".equals(u) && p.tabletsPerBlister != null) {
                    btxt = "1 BLISTER = " + p.tabletsPerBlister + " UNIT";
                } else if ("BOTTLE".equals(u) && p.mlPerBottle != null) {
                    btxt = "1 BOTTLE = " + p.mlPerBottle + " ml";
                } else if ("TUBE".equals(u) && p.gramsPerTube != null) {
                    btxt = "1 TUBE = " + p.gramsPerTube + " g";
                } else if ("UNIT".equals(u)) {
                    btxt = "UNIT = single piece";
                }
            }
            breakdown.setText(btxt);

            int units;
            if (free) {
                try { units = Math.max(0, Integer.parseInt(unitsField.getText().trim())); }
                catch (Exception e) { units = 0; }
                countHint.setText("");
            } else {
                int cnt = safeSpinnerInt(packCount);
                int per = unitsPerPack.apply(u);
                units = cnt * Math.max(1, per);
                countHint.setText("= " + cnt + " × " + Math.max(1, per) + " = " + units + " units");
            }

            // Reverse hint when typing free units
            reverse.setText(free ? reversePacks.apply(units) : "");

            String note;
            if (!free && p != null && p.tabletsPerBlister != null) {
                int perBlister = Math.max(1, p.tabletsPerBlister);
                note = (units % perBlister == 0) ? "Full pack" : "فراطة (not a full blister)";
            } else {
                note = "Units";
            }
            calc.setText("Will dispense " + units + " units — " + note);

            String w = ""; boolean ok = true;
            if (units > prescribed) { w = "Exceeds prescribed (" + prescribed + ")."; ok = false; }
            if (units > inStock)     { w = (w.isEmpty()? "" : w + " ") + "Exceeds stock (" + inStock + ")."; ok = false; }
            warn.setText(w);
            okBtn.setDisable(!ok);

            final int unitsFinal = units;
            dlg.setResultConverter(btn -> (btn == okType)
                    ? new DispenseDecision(
                            free ? null : u,
                            free ? 0    : safeSpinnerInt(packCount),
                            unitsFinal,
                            free)
                    : null);
        };

        unitChoice.getSelectionModel().selectedItemProperty().addListener((obs,a,b) -> { initPackSpinner.run(); recompute.run(); });
        packCount.valueProperty().addListener((obs,a,b) -> recompute.run());
        unitsField.textProperty().addListener((obs,a,b) -> recompute.run());
        byPack.setOnAction(e -> recompute.run());
        byUnits.setOnAction(e -> recompute.run());

        btnMinus.setOnAction(e -> { try { int v = Integer.parseInt(unitsField.getText().trim()); if (v > 0) unitsField.setText(String.valueOf(v - 1)); } catch (Exception ignored) {} });
        btnPlus.setOnAction(e -> { try { int v = Integer.parseInt(unitsField.getText().trim()); unitsField.setText(String.valueOf(v + 1)); } catch (Exception ignored) {} });

        // Initialize spinner and compute defaults
        initPackSpinner.run();
        recompute.run();
        return dlg.showAndWait().orElse(null);
    }




    private static int safeSpinnerInt(Spinner<Integer> sp) {
        if (sp == null) return 1;
        try { sp.commitValue(); } catch (Throwable ignored) { }
        Integer v = null;
        try { v = sp.getValue(); } catch (Throwable ignored) { }
        if (v == null && sp.getValueFactory() != null) {
            try { v = sp.getValueFactory().getValue(); } catch (Throwable ignored) { }
        }
        if (v == null && sp.getValueFactory() instanceof SpinnerValueFactory.IntegerSpinnerValueFactory vf) {
            try { v = vf.getMin(); } catch (Throwable ignored) { }
        }
        return v == null ? 1 : v;
    }

    /** بديل بسيط للاستخدام داخل زر Accept */
    private int promptDispenseQuantity(PrescItemRow r) {
        DispenseDecision d = showPackAwareDispenseDialog(r);
        return (d == null) ? -1 : d.unitsTotal;
    }

    /** Update Total/Approved/Rejected/Pending counters for a given prescription. */
    private void refreshPrescriptionItemCounts(long prescId) {
        int total = 0, approved = 0, rejected = 0, pending = 0;

        try (java.sql.Connection c = com.example.healthflow.db.Database.get();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     """
                     SELECT
                         COUNT(*)                                     AS total,
                         COUNT(*) FILTER (WHERE status = 'APPROVED')  AS approved,
                         COUNT(*) FILTER (WHERE status = 'CANCELLED') AS rejected,
                         COUNT(*) FILTER (WHERE status = 'PENDING')   AS pending
                     FROM prescription_items
                     WHERE prescription_id = ?
                     """
             )) {
            ps.setLong(1, prescId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total    = rs.getInt("total");
                    approved = rs.getInt("approved");
                    rejected = rs.getInt("rejected");
                    pending  = rs.getInt("pending");
                }
            }
        } catch (Exception e) {
            System.err.println("[PharmacyController] refreshPrescriptionItemCounts error: " + e);
        }

        if (TotalItemsNumber != null) TotalItemsNumber.setText(String.valueOf(total));
        if (ApprovedNumber   != null) ApprovedNumber.setText(String.valueOf(approved));
        if (RejectedNumber   != null) RejectedNumber.setText(String.valueOf(rejected));
        if (PendingNumber    != null) PendingNumber.setText(String.valueOf(pending));
    }

    /** Query packaging for a given medicine id. Safe to call per-row; tiny select. */
    private PackagingInfo fetchPackaging(long medId) {
        try (java.sql.Connection c = com.example.healthflow.db.Database.get();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "SELECT base_unit::text, tablets_per_blister, blisters_per_box, ml_per_bottle, grams_per_tube, split_allowed " +
                             "FROM medicines WHERE id = ?")) {
            ps.setLong(1, medId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PackagingInfo(
                            rs.getString(1),
                            (Integer)rs.getObject(2),
                            (Integer)rs.getObject(3),
                            (Integer)rs.getObject(4),
                            (Integer)rs.getObject(5),
                            (Boolean)rs.getObject(6)
                    );
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    // --- ضع هاتين الدالتين داخل PharmacyController ---

    private static final class DispenseDecision {
        final String approvedUnit;   // UNIT/BLISTER/BOX/BOTTLE/TUBE أو null لو "فراطة"
        final int approvedCount;     // عدد العبوات لو بالعبوات
        final int unitsTotal;        // إجمالي الوحدات الفعلية للصرف
        final boolean isFreeUnits;   // فراطة؟
        DispenseDecision(String unit, int count, int unitsTotal, boolean free) {
            this.approvedUnit = unit; this.approvedCount = count; this.unitsTotal = unitsTotal; this.isFreeUnits = free;
        }
    }

    private void setupDashboardTableColumns() {
        // --- Suggested pack/quantity column (renders text with tooltip, stays read-only) ---
        if (colSuggestionQty != null) {
            colSuggestionQty.setCellFactory(col -> new TableCell<PrescItemRow, Number>() {
                private final Tooltip tip = new Tooltip();
                @Override protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setText(null); setTooltip(null); return; }
                    PrescItemRow r = getTableView().getItems().get(getIndex());
                    int req = Math.max(0, r.getQuantity());
                    long medId = r.getMedicineId();
                    PackagingInfo p = (medId > 0) ? fetchPackaging(medId) : null;
                    PackSuggestion s = PackagingSupport.suggestPackFor(req, p);
                    String txt = PackagingSupport.formatSuggestionText(s);
                    setText(txt);
                    tip.setText(txt);
                    setTooltip(tip);
                }
            });
            colSuggestionQty.setStyle("-fx-alignment: CENTER-LEFT;");
            colSuggestionQty.setPrefWidth(180);
        }
        if (PresciptionsTable == null) return;
//        PresciptionsTable.setColumnResizePolicy(javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Serial #
        // -------- Serial # --------
        if (colSerialPhDashboard != null) {
            colSerialPhDashboard.setCellFactory((TableColumn<DashboardRow, Number> col) ->
                    new TableCell<DashboardRow, Number>() {
                        @Override
                        protected void updateItem(Number item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setText(null);
                            } else {
                                // الترقيم بحسب الترتيب الحالي للجدول
                                setText(Integer.toString(getIndex() + 1));
                            }
                        }
                    }
            );
            // (اختياري) عرض صغير وثابت
            colSerialPhDashboard.setStyle("-fx-alignment: CENTER;");
            colSerialPhDashboard.setMinWidth(60);
            colSerialPhDashboard.setPrefWidth(70);
            colSerialPhDashboard.setMaxWidth(100);
        }
        // Names
        if (colPatientName != null) colPatientName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().patientName));
        if (colDoctorName  != null) colDoctorName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().doctorName));
        // Date/Time formatted: appt date (if exists) + created time
        if (colDate_Time != null) {
            var dateFmt  = java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");
            var timeFmt  = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
            colDate_Time.setCellValueFactory(cd -> {
                var appt    = cd.getValue().appointmentDateTime;
                var created = cd.getValue().createdAt;
                String left  = (appt    != null) ? appt.atZoneSameInstant(APP_TZ).toLocalDateTime().format(dateFmt) : "";
                String right = (created != null) ? created.atZoneSameInstant(APP_TZ).toLocalDateTime().format(timeFmt) : "";
                String txt = left;
                if (!left.isEmpty() && !right.isEmpty()) txt += "  |  " + right;
                else if (left.isEmpty()) txt = right; // fallback
                return new javafx.beans.property.SimpleStringProperty(txt);
            });
        }
        // Status beautified
        if (colprescriptionStutus != null) {
            colprescriptionStutus.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                    cd.getValue().status == null ? "" : switch (cd.getValue().status) {
                        case DRAFT -> "DRAFT";
                        case PENDING -> "PENDING";
                        case APPROVED -> "APPROVED";
                        case REJECTED -> "REJECTED";
                        case DISPENSED -> "DISPENSED";
                    }
            ));
        }
        // Action column (View)
        if (colActionPhDashbord != null) {
            colActionPhDashbord.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                private final Button btn = new Button("View Prescription");
                {
                    btn.getStyleClass().addAll("btn", "btn-primary", "btn--deep", "btn--compact");
                    btn.setOnAction(e -> {
                        DashboardRow row = getTableView().getItems().get(getIndex());
                        showPrescriptionDetails(row);
                    });
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });
        }
    }

    private void showPrescriptionDetails(DashboardRow row) {
        if (row == null) return;
        this.selectedRow = row;
        if (Prescription_number != null) Prescription_number.setText("");

        // Switch to prescription pane
        showPrescriptionsPane();

        setPrescriptionSidebarState(true, true);
        wireSidebarGuardsIfNeeded();

        // Enable/disable Finish button based on prescription status
        // === Finish_Prescription ===
        if (Finish_Prescription != null) {
            Finish_Prescription.setOnAction(e -> {
                startBtnBusy(Finish_Prescription, "Finishing …");
                javafx.animation.PauseTransition pt =
                        new javafx.animation.PauseTransition(javafx.util.Duration.millis(140));
                pt.setOnFinished(ev -> javafx.application.Platform.runLater(() -> {
                    try {
                        onFinishPrescription(); // الدالة الموجودة أصلاً لإنهاء الوصفة
                    } finally {
                        stopBtnBusy(Finish_Prescription);
                    }
                }));
                pt.play();
            });
        }

        // Patient name + NID (read-only)
        if (PatientNameTF != null) {
            String nid = row.patientNid == null ? "" : (" (" + row.patientNid + ")");
            PatientNameTF.setText(row.patientName + nid);
            PatientNameTF.setEditable(false);
        }

        // Doctor name
        if (DoctorNameLabel != null) {
            DoctorNameLabel.setText(row.doctorName);
        }

        // Prescription number (ID)
        if (Prescription_number != null) {
            if (row.prescriptionId > 0) {
                Prescription_number.setText(String.valueOf(row.prescriptionId));
            } else if (row.appointmentId != null) {
                // Optional fallback: try to resolve prescription id by appointment id if needed
                Prescription_number.setText("–");
            } else {
                Prescription_number.setText("–");
            }
        }

        // Pharmacist full name + Welcome first name
        resolveLoggedInUserLabels();

        // Appointment date/time: day + date + time, include ID if available, with DB fallback
        if (AppointmentDate != null) {
            var dateFmt = java.time.format.DateTimeFormatter.ofPattern("EEE MM/dd/yyyy");
            var timeFmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
            var when = (row.appointmentDateTime != null)
                    ? row.appointmentDateTime
                    : fetchAppointmentDateById(row.appointmentId);

            if (when != null) {
                var zdt = when.atZoneSameInstant(APP_TZ).toLocalDateTime();
                String txt = zdt.format(dateFmt) + "  |  " + zdt.format(timeFmt);
                if (row.appointmentId != null) {
                    txt += "  (ID: " + row.appointmentId + ")";
                }
                AppointmentDate.setText(txt);
            } else {
                AppointmentDate.setText("—");
            }
        }

        // Diagnosis
        if (DiagnosisView != null) {
            DiagnosisView.setText(row.diagnosisNote == null ? "" : row.diagnosisNote);
        }

        // Load items
        loadPrescriptionItems(row.prescriptionId);
    }

    private void loadPrescriptionItems(long prescId) {
        itemRows.clear();

        // 1) Load items for the prescription (current behavior)
        java.util.List<PrescriptionItem> items = java.util.Collections.emptyList();
        try (Connection c = Database.get()) {
            PrescriptionItemDAO itemDao = new PrescriptionItemDAO();
            items = itemDao.listByPrescription(c, prescId);
        } catch (Exception ex) {
            System.err.println("[PharmacyController] loadPrescriptionItems (fetch items) error: " + ex);
        }

        if (items == null || items.isEmpty()) {
            if (TablePrescriptionItems != null) TablePrescriptionItems.setItems(itemRows);
            refreshPrescriptionItemCounts(prescId);

            return;
        }

        // 2) Pre-collect medicine ids and names to lookup stock in one go
        java.util.Set<Long> medIds = new java.util.LinkedHashSet<>();
        java.util.Set<String> medNames = new java.util.LinkedHashSet<>();
        for (PrescriptionItem it : items) {
            if (it.getMedicineId() != null) medIds.add(it.getMedicineId());
            else if (it.getMedicineName() != null && !it.getMedicineName().isBlank()) medNames.add(it.getMedicineName().trim());
        }

        // 3) Build maps id->available and lower(name)->available
        java.util.Map<Long, Integer> stockById = new java.util.HashMap<>();
        java.util.Map<String, Integer> stockByLowerName = new java.util.HashMap<>();

        try (Connection c = Database.get()) {
            // 3.a) Lookup by ids
            if (!medIds.isEmpty()) {
                String in = medIds.stream().map(x -> "?").collect(java.util.stream.Collectors.joining(","));
                String sql = "SELECT id, available_quantity FROM medicines WHERE id IN (" + in + ")";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    int i = 1; for (Long id : medIds) ps.setLong(i++, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            stockById.put(rs.getLong(1), rs.getInt(2));
                        }
                    }
                }
            }
            // 3.b) Lookup by names (case-insensitive)
            if (!medNames.isEmpty()) {
                String in = medNames.stream().map(x -> "?").collect(java.util.stream.Collectors.joining(","));
                String sql = "SELECT LOWER(name), available_quantity FROM medicines WHERE LOWER(name) IN (" + in + ")";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    int i = 1; for (String nm : medNames) ps.setString(i++, nm.toLowerCase());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            stockByLowerName.put(rs.getString(1), rs.getInt(2));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] loadPrescriptionItems (fetch stock) error: " + ex);
        }

        // 4) Build UI rows and include stockAvailable
        for (PrescriptionItem it : items) {
            PrescItemRow r = new PrescItemRow();
            r.setId(it.getId());
            if (it.getMedicineId() != null) r.setMedicineId(it.getMedicineId());
            r.setMedicineName(it.getMedicineName());
            r.setStrength(it.getStrength());
            r.setForm(it.getForm());
            if (it.getDose() != null) r.setDose(it.getDose());
            if (it.getDurationDays() != null) r.setDurationDays(it.getDurationDays());
            if (it.getFreqPerDay() != null) r.setFreqPerDay(it.getFreqPerDay());
            r.setDosage(it.getDosage());
            r.setDosageText(it.getDosageText());
            r.setQuantity(it.getQuantity());
            r.setQtyDispensed(it.getQtyDispensed());
            r.setStatus(it.getStatus().name());

            // Resolve stock: prefer by id, otherwise by lower(name)
            Integer stock = null;
            if (it.getMedicineId() != null) stock = stockById.get(it.getMedicineId());
            if (stock == null && it.getMedicineName() != null) stock = stockByLowerName.get(it.getMedicineName().toLowerCase());
            r.setStockAvailable(stock == null ? 0 : stock);

            itemRows.add(r);
        }

        if (TablePrescriptionItems != null) TablePrescriptionItems.setItems(itemRows);
        refreshPrescriptionItemCounts(prescId);    }

    private void setupItemsTableColumns() {
        if (TablePrescriptionItems == null) return;
        if (colIdx != null) {
            colIdx.setCellFactory(col -> new TableCell<PrescItemRow, Number>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : Integer.toString(getIndex() + 1));
                }
            });
        }        if (colMedicineName != null) colMedicineName.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        if (colStrength != null) colStrength.setCellValueFactory(new PropertyValueFactory<>("strength"));
        if (colForm != null) colForm.setCellValueFactory(new PropertyValueFactory<>("form"));
        if (colDose != null) colDose.setCellValueFactory(new PropertyValueFactory<>("dose"));
        if (colDosage != null) {
            colDosage.setCellValueFactory(cd -> {
                String txt = cd.getValue().getDosageText();
                if (txt == null || txt.isBlank()) txt = cd.getValue().getDosage();
                return new javafx.beans.property.SimpleStringProperty(txt == null ? "" : txt);
            });
        }
        if (colQuantity != null) colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        if (colStock != null) colStock.setCellValueFactory(new PropertyValueFactory<>("stockAvailable"));
        if (colItemStatus != null) colItemStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (colPresesItemAction != null) {
            colPresesItemAction.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                private final Button approve = new Button("Accept");
                private final Button reject  = new Button("Reject");
                {
                    approve.getStyleClass().add("btn-success");
                    reject.getStyleClass().add("btn-danger");
                    // Inline fallback styles if CSS classes are not present
                    approve.setStyle("-fx-background-color: #298e40; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
                    reject.setStyle("-fx-background-color: #b3081a; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
                    approve.setOnAction(e -> onApproveItem(getIndex()));
                    reject.setOnAction(e -> onRejectItem(getIndex()));
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    PrescItemRow row = getTableView().getItems().get(getIndex());
                    boolean ro = isCurrentPrescriptionReadOnly();
                    boolean pending = row != null && row.getStatus() != null && row.getStatus().equalsIgnoreCase("PENDING");
                    approve.setDisable(ro || !pending);
                    reject.setDisable(ro || !pending);
                    var box = new javafx.scene.layout.HBox(6, approve, reject);
                    setGraphic(box);
                }
            });
        }


        if (colIdx != null) colIdx.setPrefWidth(50);
        if (colMedicineName != null) colMedicineName.setPrefWidth(180);
        if (colStrength != null) colStrength.setPrefWidth(120);
        if (colForm != null) colForm.setPrefWidth(120);
        if (colDose != null) colDose.setPrefWidth(80);
        if (colDosage != null) colDosage.setPrefWidth(360);
        if (colQuantity != null) colQuantity.setPrefWidth(100);
        if (colStock != null) colStock.setPrefWidth(120);
        if (colItemStatus != null) colItemStatus.setPrefWidth(120);
        if (colPresesItemAction != null) colPresesItemAction.setPrefWidth(160);
    }

    private void onApproveItem(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= itemRows.size() || selectedRow == null) return;
        PrescItemRow row = itemRows.get(rowIndex);

        // 1) الحوار
        DispenseDecision d = showPackAwareDispenseDialog(row);
        if (d == null) return;

        int prescribed = Math.max(0, row.getQuantity());
        int inStock    = Math.max(0, row.getStockAvailable());
        int unitsToDispense = Math.max(0, d.unitsTotal);

        if (unitsToDispense == 0) {
            new Alert(Alert.AlertType.INFORMATION, "No units selected to dispense.").showAndWait();
            return;
        }
        if (unitsToDispense > prescribed) {
            new Alert(Alert.AlertType.ERROR, "Cannot dispense more than prescribed (" + prescribed + ")").showAndWait();
            return;
        }
        if (unitsToDispense > inStock) {
            new Alert(Alert.AlertType.ERROR, "Not enough stock (available: " + inStock + ")").showAndWait();
            return;
        }

        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                // Resolve medicine_id (fallback by name)
                Long medicineId = row.getMedicineId();
                if (medicineId == null || medicineId <= 0) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT id FROM medicines WHERE LOWER(name)=LOWER(?) LIMIT 1")) {
                        ps.setString(1, row.getMedicineName());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) medicineId = rs.getLong(1);
                        }
                    }
                }
                if (medicineId == null || medicineId <= 0)
                    throw new IllegalStateException("Cannot resolve medicine id for '" + row.getMedicineName() + "'.");

                // Resolve batch (treat 0 as null)
                Long batchId = row.getBatchId();
                if (batchId == null || batchId <= 0) {
                    batchId = queries.fifoBatchId(c, medicineId, unitsToDispense);
                }
                if (batchId == null || batchId <= 0) {
                    c.rollback();
                    new Alert(Alert.AlertType.ERROR,
                            "No suitable batch with enough balance.\nPlease select a batch from inventory.")
                            .showAndWait();
                    return;
                }

                // Pharmacist
                Long pharmacistId = requireCurrentPharmacistId();
                if (pharmacistId == null) { c.rollback(); return; }

                // Update item
                new PrescriptionItemDAO().updateDispensed(c, row.getId(), unitsToDispense, ItemStatus.APPROVED, null);

                // Insert inventory transaction (STRICT: must have batch)
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO inventory_transactions " +
                                "(medicine_id, batch_id, qty_change, reason, ref_type, ref_id, pharmacist_id, created_at) " +
                                "VALUES (?,?,?,?,?,?,?, NOW())")) {
                    ps.setLong(1, medicineId);
                    ps.setLong(2, batchId);
                    ps.setInt(3, -Math.abs(unitsToDispense));
                    ps.setString(4, "Prescription dispense");
                    ps.setString(5, "PRESCRIPTION_ITEM");
                    ps.setLong(6, row.getId());
                    ps.setLong(7, pharmacistId);
                    ps.executeUpdate();
                }

                c.commit();

                // UI
                row.setQtyDispensed(unitsToDispense);
                row.setStatus(ItemStatus.APPROVED.name());
                row.setBatchId(batchId);
                row.setStockAvailable(Math.max(0, inStock - unitsToDispense));
                if (TablePrescriptionItems != null) TablePrescriptionItems.refresh();
                refreshPharmacyDashboardCounts();
                refreshPharmacyTables();

            } catch (Exception tx) {
                try { c.rollback(); } catch (Exception ignored) {}
                throw tx;
            } finally { try { c.setAutoCommit(true); } catch (Exception ignored) {} }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] onApproveItem error: " + ex);
            new Alert(Alert.AlertType.ERROR, "Failed to approve item: " + ex.getMessage()).showAndWait();
        }
    }


    public void onApproveItem(PrescItemRow row) {
        if (row == null) return;
        int idx = itemRows.indexOf(row);
        onApproveItem(idx);
    }

    // Refresh dashboard + current prescription items and side counters safely
    private void refreshPharmacyTables() {
        try {
            // Dashboard list
            loadDashboardTable();
        } catch (Throwable ignored) {}

        try {
            // Current prescription items (if a prescription is open)
            if (selectedRow != null && selectedRow.prescriptionId > 0) {
                loadPrescriptionItems(selectedRow.prescriptionId);
            }
        } catch (Throwable ignored) {}

        try {
            // Summary counters
            refreshPharmacyDashboardCounts();
        } catch (Throwable ignored) {}
    }

    private void onRejectItem(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= itemRows.size() || selectedRow == null) return;
        PrescItemRow r = itemRows.get(rowIndex);
        try (Connection c = Database.get()) {
            PrescriptionItemDAO dao = new PrescriptionItemDAO();
            dao.updateDispensed(c, r.getId(), 0, ItemStatus.CANCELLED, null);
            r.setStatus("CANCELLED");
            r.setQtyDispensed(0);
            TablePrescriptionItems.refresh();
        } catch (Exception ex) {
            System.err.println("[PharmacyController] reject item error: " + ex);
        }
    }

    // Handler: Mark prescription as DISPENSED (completed) and prevent further edits
    private void onFinishPrescription() {
        if (selectedRow == null) return;
        long prescId = selectedRow.prescriptionId;
        if (prescId <= 0) return;

        // Strictly require pharmacist mapping (no auto-create)
        Long pharmacistId = requireCurrentPharmacistId();
        if (pharmacistId == null) return;

        // Mark prescription as DISPENSED (completed)
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE prescriptions\n" +
                             "SET status = 'DISPENSED',\n" +
                             "    pharmacist_id = ?,\n" +
                             "    decision_at   = COALESCE(decision_at, NOW()),\n" +
                             "    approved_at   = COALESCE(approved_at, COALESCE(decision_at, NOW())),\n" + // ✅
                             "    approved_by   = COALESCE(approved_by, ?),\n" +
                             "    dispensed_at  = NOW(),\n" +
                             "    dispensed_by  = ?\n" +
                             "WHERE id = ? AND status <> 'DISPENSED'")) {
            ps.setLong(1, pharmacistId);  // pharmacist_id
            ps.setLong(2, pharmacistId);  // approved_by (fallback if null)
            ps.setLong(3, pharmacistId);  // dispensed_by
            ps.setLong(4, prescId);       // WHERE id = ?
            int changed = ps.executeUpdate();

            if (changed > 0) {
                selectedRow.status = com.example.healthflow.model.PrescriptionStatus.DISPENSED;
                if (Finish_Prescription != null) Finish_Prescription.setDisable(true);
                if (TablePrescriptionItems != null) TablePrescriptionItems.refresh();
                refreshPharmacyDashboardCounts();
                loadDashboardTable();

                // ✅ unified alert
                showOk("Finish Prescription", "The prescription has been successfully.");
            } else {
                showOk("Finish Prescription", "This prescription is already marked as DISPENSED.");
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] finish prescription error: " + ex);
            showError("Finish Prescription", "Failed to complete prescription:\n" + ex.getMessage());
        }

        setPrescriptionSidebarState(false, false);
        showDashboardPane();
    }
    // --- Unified alerts (same look across app) ---
    private static void showOk(String title, String message) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null); // like other screens (no header line)
        a.setContentText(message);
        a.showAndWait();
    }

    private static void showError(String title, String message, Throwable ex) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null); // keep consistent
        a.setContentText(message + (ex != null ? "\n" + ex.getMessage() : ""));
        a.showAndWait();
    }

    private void showPrescriptionsPane() {
        setVisibleManaged(pharmacyDashboardAnchorPane, false);
        setVisibleManaged(PrescriptionAnchorPane, true);
        setVisibleManaged(InventoryAnchorPane, false);
        markNavActive(PrescriptionsButton);
        // Show the details area only when a prescription is selected later
        if (PrescriptionMedicationAnchorPane != null){
            setVisibleManaged(PrescriptionMedicationAnchorPane, true);
        }
        startPharmacyDbNotifications();
    }

    private void showInventoryPane() {
        setPrescriptionSidebarState(false, false);
        setVisibleManaged(pharmacyDashboardAnchorPane, false);
        setVisibleManaged(PrescriptionAnchorPane, false);
        setVisibleManaged(InventoryAnchorPane, true);
        markNavActive(InventoryButton);
        startPharmacyDbNotifications();
        wireInventoryToggles();
    }

    /* ====== Inventory Receive vs Deduct (like segmented behavior) ====== */
    private ToggleGroup invToggleGroup;
    private ToggleButton lastSelectedInvBtn;
    @FXML private ToggleButton btnInventory;
    @FXML private AnchorPane InventoryPane;
//    @FXML private AnchorPane AdjustPane;






    private void showInventoryReceiveMode() {
        if (ReceivePane != null) ReceivePane.setVisible(true);
        if (deductPane  != null) deductPane.setVisible(false);
        if (InventoryPane != null) InventoryPane.setVisible(false);
        hideSuggest();
        if (MedicineNameRecive != null) {
            String t = MedicineNameRecive.getText();
            if (t != null && t.trim().length() >= 2) queryMedSuggestions(t);
        }
    }

    private void showInventoryDeductMode() {
        if (ReceivePane != null) ReceivePane.setVisible(false);
        if (deductPane  != null) deductPane.setVisible(true);
        if (InventoryPane != null) InventoryPane.setVisible(false);
    }

    private void showInventoryMainMode() { // الزر الافتراضي: btnInventory
        if (ReceivePane != null) ReceivePane.setVisible(false);
        if (deductPane  != null) deductPane.setVisible(false);
        if (InventoryPane != null) InventoryPane.setVisible(true);
    }

    private void wireInventoryToggles() {
        invToggleGroup = new ToggleGroup();
        if (btnReceive != null) btnReceive.setToggleGroup(invToggleGroup);
        if (btnDeduct != null)  btnDeduct.setToggleGroup(invToggleGroup);
        if (btnInventory != null)  btnInventory.setToggleGroup(invToggleGroup);

        // امنع إلغاء التحديد بالضغط على الزر المحدد
        if (btnReceive != null) {
            btnReceive.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                if (btnReceive.isSelected()) e.consume();
            });
        }
        if (btnDeduct != null) {
            btnDeduct.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                if (btnDeduct.isSelected()) e.consume();
            });
        }
        if (btnInventory != null) {
            btnInventory.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                if (btnInventory.isSelected()) e.consume();
            });
        }

        // تأكد أن الـ SegmentedButton يحتوي أزرارنا
        if (btnInventory == null) btnInventory = new ToggleButton("Inventory");
        if (segInv != null) {
            if (btnInventory    != null && !segInv.getButtons().contains(btnInventory))    segInv.getButtons().add(btnInventory);
            if (btnReceive   != null && !segInv.getButtons().contains(btnReceive))   segInv.getButtons().add(btnReceive);
            if (btnDeduct    != null && !segInv.getButtons().contains(btnDeduct))    segInv.getButtons().add(btnDeduct);
        }

        // الوضع الافتراضي = btnInventory
        if (btnReceive != null) btnReceive.setSelected(false);
        if (btnDeduct  != null) btnDeduct.setSelected(false);
        if (btnInventory  != null) btnInventory.setSelected(true);
        lastSelectedInvBtn = (btnInventory != null) ? btnInventory : (btnReceive != null ? btnReceive : btnDeduct);

        // أظهر البانل المناسب بدايةً
        if (btnReceive != null && btnReceive.isSelected()) {
            showInventoryReceiveMode();
        } else if (btnDeduct != null && btnDeduct.isSelected()) {
            showInventoryDeductMode();
        } else if (btnInventory != null && btnInventory.isSelected()) {
            showInventoryMainMode();
        }

        // بدّل البانلز عند تغيير الاختيار + لا تسمح بـ null
        if (invToggleGroup != null) {
            invToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                if (newT == null) {
                    if (lastSelectedInvBtn != null) invToggleGroup.selectToggle(lastSelectedInvBtn);
                    return;
                }
                if (newT == btnReceive) {
                    showInventoryReceiveMode();
                } else if (newT == btnDeduct) {
                    showInventoryDeductMode();
                } else if (newT == btnInventory) {
                    showInventoryMainMode();
                }
                if (newT instanceof ToggleButton tb) {
                    lastSelectedInvBtn = tb;
                }
            });
        }
    }

    public PharmacyController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }

    // Default constructor for FXML loader
    public PharmacyController() {
        this(new ConnectivityMonitor());
    }

    private void ensureConnectivityBannerOnce() {
        if (rootPane == null) return;
        // لا تكرار: افحص إن كان مضاف مسبقًا
        for (javafx.scene.Node n : rootPane.getChildren()) {
            if (n instanceof ConnectivityBanner) {
                return; // موجود بالفعل
            }
        }
        ConnectivityBanner banner = new ConnectivityBanner(monitor);
        rootPane.getChildren().add(0, banner);
    }

    @FXML
    private void initialize() {
        resolveLoggedInUserLabels();

        if (rootPane != null) {
            ensureConnectivityBannerOnce();

            // attach shared CSS defensively
            try {
                var cssUrl = getClass().getResource("/Design/ReceptionDesign.css");
                if (cssUrl != null && !rootPane.getStylesheets().contains(cssUrl.toExternalForm())) {
                    rootPane.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Throwable ignored) {}
        }
        // Defensive: hide all sections first; we'll explicitly show one below
        btnReceive.setOnAction(e -> showInventoryReceiveMode());
        btnDeduct.setOnAction(e -> showInventoryDeductMode());
        if (btnInventory != null) btnInventory.setOnAction(e -> showInventoryMainMode());

        // Quick sanity check: if IDs are not wired, navigation won't work
        boolean missing = warnIfMissing();

        // Start connectivity monitor
        monitor.start();


        // Start header clock & date (12h, Asia/Gaza)
        startClock();

        if (PrescriptionDatePicker != null) {
            PrescriptionDatePicker.setValue(java.time.ZonedDateTime.now(APP_TZ).toLocalDate());
            // Debounce: تجميع تغييرات التاريخ 250ms قبل التحميل
            dpDebounce = new PauseTransition(Duration.millis(250));
            dpDebounce.setOnFinished(e2 -> {
                loadDashboardAsync(true);
            });
            PrescriptionDatePicker.valueProperty().addListener((obs, oldV, newV) -> {
                if (dpDebounce != null) {
                    dpDebounce.stop();
                    dpDebounce.playFromStart();
                } else {
                    loadDashboardAsync(true);
                }
            });
            // بعض المنصات تطلق فقط ActionEvent عند اختيار اليوم
            PrescriptionDatePicker.setOnAction(e -> {
                if (dpDebounce != null) {
                    dpDebounce.stop();
                    dpDebounce.playFromStart();
                } else {
                    loadDashboardAsync(true);
                }
            });
        }

        // ===== Top navigation buttons =====
        if (DashboardButton != null) {
            DashboardButton.setOnAction(e -> showDashboardPane());
        }
        if (PrescriptionsButton != null) {
            PrescriptionsButton.setOnAction(e -> showPrescriptionsPane());
        }
        if (InventoryButton != null) {
            InventoryButton.setOnAction(e -> showInventoryPane());
        }
        // Wire Finish button to handler
        if (Finish_Prescription != null) {
            Finish_Prescription.setOnAction(e -> onFinishPrescription());
        }

        // Back button: keep it simple for now — return to Dashboard


        if (missing) {
            System.out.println("[PharmacyController] Some fx:id are missing. Verify FXML ids match controller fields.");
        }

        // Disable actions when offline (if OnlineBindings present)
        try { OnlineBindings.disableWhenOffline(monitor, DashboardButton, InventoryButton, saveBtnReceive, saveBtnDeduct); } catch (Throwable ignored) {}
        // Prevent multiple async loads via rapid clicks (if button stays enabled from bindings)
        if (DashboardButton != null) {
            DashboardButton.setOnAction(e -> {
                if (!dashboardLoading) showDashboardPane();
            });
        }

        // Inventory toggles
        wireInventoryToggles();

        // Initial section
        showDashboardPane();
        System.out.println("[PharmacyController] init -> startPharmacyDbNotifications()");
        startPharmacyDbNotifications();

        // Hard fallback: enforce Dashboard default if panes exist
        if (pharmacyDashboardAnchorPane != null && InventoryAnchorPane != null) {
            if (!pharmacyDashboardAnchorPane.isVisible() && InventoryAnchorPane.isVisible()) {
                setVisibleManaged(InventoryAnchorPane, false);
                setVisibleManaged(pharmacyDashboardAnchorPane, true);
            }
        }
        refreshPharmacyDashboardCounts();
        setupDashboardTableColumns();
        if (PresciptionsTable != null) PresciptionsTable.setItems(dashboardRows);

        setupItemsTableColumns();
        if (TablePrescriptionItems != null) {
            TablePrescriptionItems.setColumnResizePolicy(
                    javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY
            );
        }
        loadDashboardTable();

    // This in Inventory
        try {
            initInventoryTableIfNeeded();
            reloadInventoryTable();
            updateInventorySummary();
        } catch (Throwable ignored) {}

        if (MedicineLabelDt != null) MedicineLabelDt.setWrapText(true);
        if (descriptionTf != null) descriptionTf.setWrapText(true);
    // Recive
        if (tblMedSuggest != null) {
            colSuggName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().name));
            colSuggStrength.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().strength));
            tblMedSuggest.setItems(medSuggestions);

            tblMedSuggest.setOnMouseClicked(e -> { if (e.getClickCount()==2) pickSelectedMedicine(); });
            tblMedSuggest.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case ENTER -> { pickSelectedMedicine(); e.consume(); }
                    case ESCAPE -> hideSuggest();
                }
            });
        }
        // Ensure the suggestions table has a visible height
        if (tblMedSuggest != null) {
            tblMedSuggest.setPrefHeight(180);
            tblMedSuggest.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            // If inside AnchorPane, anchor to all sides (safe even if not AnchorPane)
            try {
                javafx.scene.layout.AnchorPane.setTopAnchor(tblMedSuggest, 0.0);
                javafx.scene.layout.AnchorPane.setLeftAnchor(tblMedSuggest, 0.0);
                javafx.scene.layout.AnchorPane.setRightAnchor(tblMedSuggest, 0.0);
                javafx.scene.layout.AnchorPane.setBottomAnchor(tblMedSuggest, 0.0);
            } catch (Throwable ignored) {}
        }

        if (tblMedSuggest != null) {
            colSuggName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name));
            colSuggStrength.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().strength));
            tblMedSuggest.setItems(medSuggestions);
        }
        hideSuggest(); // البداية مخفي

    // إظهار/إخفاء الجدول بحسب النص
        if (MedicineNameRecive != null) {
            MedicineNameRecive.textProperty().addListener((obs, oldText, newText) -> {
                String t = (newText == null) ? "" : newText.trim();

                if (t.length() >= 1) {
                    // أظهر القائمة فورًا
                    showSuggest();
                    // صف مؤقّت في الأعلى لحين رجوع نتائج الداتابيز
                    java.util.ArrayList<MedicineSuggestion> pre = new java.util.ArrayList<>();
                    pre.add(new MedicineSuggestion(-1, "➕ Add new: " + t, "", true));
                    medSuggestions.setAll(pre);
                } else {
                    medSuggestions.clear();
                    hideSuggest();
                }

                // شغّل البحث المؤجل (debounce)
                medDebounce.stop();
                medDebounce.setOnFinished(ev -> queryMedSuggestions(t));
                medDebounce.playFromStart();
            });

            // اختصارات الكيبورد
            MedicineNameRecive.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case DOWN -> {
                        showSuggest();
                        if (tblMedSuggest != null) {
                            tblMedSuggest.requestFocus();
                            tblMedSuggest.getSelectionModel().selectFirst();
                        }
                    }
                    case ENTER -> {
                        if (tblMedSuggest != null && !medSuggestions.isEmpty()) {
                            tblMedSuggest.getSelectionModel().selectFirst();
                            pickSelectedMedicine();
                            e.consume();
                        }
                    }
                }
            });
        }

        if (descriptionTf != null) descriptionTf.clear();

        // Setup wrapping for MedicineLabelDt
        if (MedicineLabelDt != null) {
            MedicineLabelDt.setWrapText(true);
            try {
                var p = MedicineLabelDt.getParent();
                if (p instanceof javafx.scene.layout.Region reg) {
                    MedicineLabelDt.maxWidthProperty().bind(reg.widthProperty().subtract(24));
                } else if (rootPane != null) {
                    MedicineLabelDt.maxWidthProperty().bind(rootPane.widthProperty().subtract(80));
                }
            } catch (Throwable ignored) {}
        }



        try {
            TableUtils.applyUnifiedTableStyle(
                    rootPane,
                    TableToShowMedicineByBatchNumber,
                    PresciptionsTable,
                    TableMedicinesInventory,
                    TablePrescriptionItems,
                    tblMedSuggest
            );
        } catch (Throwable ignore) {}



        // === saveBtnReceive ===
        if (saveBtnReceive != null) {
            saveBtnReceive.setOnAction(e -> {
                startBtnBusy(saveBtnReceive, "Receiving …");
                javafx.animation.PauseTransition pt =
                        new javafx.animation.PauseTransition(javafx.util.Duration.millis(140));
                pt.setOnFinished(ev -> javafx.application.Platform.runLater(() -> {
                    try {
                        onSaveReceive();
                    } finally {
                        stopBtnBusy(saveBtnReceive);
                    }
                }));
                pt.play();
            });
        }

        deductSupport = new DeductSupport(
                cmboTypeOfDeduct,
                TableToShowMedicineByBatchNumber,
                colSerialDeduct,
                colMedicineNameInventoryDeduct,
                colStockQtyInventoryDeduct,
                colExpiryQtyInventoryDeduct,
                deductBatchNumber_MN,
                quantityToDeduct,
                ReasonOfDeduct,
                plusToQuantity,
                minusToQuantity,
                saveBtnDeduct,
                sugestMedicinesItem
        );
        deductSupport.setOnSaveCallback(this::reloadInventoryTable);
        deductSupport.init();


        // === saveBtnDeduct ===
        if (saveBtnDeduct != null) {
            saveBtnDeduct.setOnAction(e -> {
                startBtnBusy(saveBtnDeduct, "Deducting …");
                javafx.animation.PauseTransition pt =
                        new javafx.animation.PauseTransition(javafx.util.Duration.millis(140));
                pt.setOnFinished(ev -> javafx.application.Platform.runLater(() -> {
                    try {
                        applyInventoryNullPlaceholders();
                        if (deductSupport != null) {
                            deductSupport.triggerSave();   // <-- نفّذ الحفظ بدل إعادة الإنشاء
                        }
                    } finally {
                        stopBtnBusy(saveBtnDeduct);
                    }
                }));
                pt.play();
            });
        }


    }
    private void startPharmacyDbNotifications() {
        if (dbn != null) return; // already started
        System.out.println("[PharmacyController] starting DbNotifications listeners...");
        dbn = new com.example.healthflow.db.notify.DbNotifications();

        dashCoalesce.setOnFinished(e  -> { System.out.println("[Pharm] dashCoalesce -> loadDashboardAsync(true)"); loadDashboardAsync(true); });
        itemsCoalesce.setOnFinished(e -> { System.out.println("[Pharm] itemsCoalesce -> reloadCurrentPrescriptionItems()"); reloadCurrentPrescriptionItems(); });
        invCoalesce.setOnFinished(e   -> { System.out.println("[Pharm] invCoalesce   -> reloadInventoryTable()"); reloadInventoryTable(); });

        dbn.listen("prescriptions_changed", p -> Platform.runLater(() -> {
            dashCoalesce.playFromStart();
            if (isPrescriptionVisible()) itemsCoalesce.playFromStart();
        }));

        dbn.listen("prescription_items_changed", p -> Platform.runLater(() -> {
            itemsCoalesce.playFromStart();
            dashCoalesce.playFromStart();
        }));

        dbn.listen("inventory_changed", p -> Platform.runLater(() -> {
            invCoalesce.playFromStart();
            itemsCoalesce.playFromStart();
            dashCoalesce.playFromStart();
        }));


        // أغلق المستمع عند إغلاق النافذة
        javafx.application.Platform.runLater(() -> {
            var hook = (rootPane != null) ? rootPane : pharmacyDashboardAnchorPane;
            if (hook != null && hook.getScene() != null) {
                var win = hook.getScene().getWindow();
                if (win != null) {
                    win.addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, e -> stopPharmacyDbNotifications());
                    win.addEventHandler(javafx.stage.WindowEvent.WINDOW_HIDDEN, e -> stopPharmacyDbNotifications());
                }
            }
        });
    }

    private void stopPharmacyDbNotifications() {
        try { if (dbn != null) dbn.close(); } catch (Exception ignore) {}
        dbn = null;
//        System.out.println("[PharmacyController] DbNotifications stopped.");
    }

    private void showSuggest()  {
        if (suggestPane != null) {
            suggestPane.setVisible(true);
            suggestPane.setManaged(true);
            suggestPane.toFront();
            // Do NOT request focus to tblMedSuggest here (so typing doesn't stop)
        }
    }
    private void hideSuggest()  {
        if (suggestPane != null) {
            suggestPane.setVisible(false);
            suggestPane.setManaged(false);
        }
    }

    private void pickSelectedMedicine() {
        MedicineSuggestion s = tblMedSuggest.getSelectionModel().getSelectedItem();
        if (s == null) return;

        if (s.addNew) {
            hideSuggest();
            openAddMedicineDialog(MedicineNameRecive.getText().trim());
            return;
        }

        selectedMedicineId = s.id;
        MedicineNameRecive.setText(s.display());
        hideSuggest();

    }

    // استعلام الاقتراحات (Async)
    // يُنادى عليها عند الكتابة في MedicineNameRecive (مع debounce)
    private void queryMedSuggestions(String q) {
        if (q == null) q = "";
        final String query = q.trim();
        if (query.length() < 1) { medSuggestions.clear(); hideSuggest(); return; }

        final String batchFilter = (batchNum != null && batchNum.getText()!=null) ? batchNum.getText().trim() : "";

        // ألغِ أي مهمة سابقة لتفادي اللّاج على الكتابة السريعة
        if (currentMedTask != null && currentMedTask.isRunning()) {
            currentMedTask.cancel();
        }

        javafx.concurrent.Task<java.util.List<MedicineSuggestion>> task = new javafx.concurrent.Task<>() {
            @Override protected java.util.List<MedicineSuggestion> call() throws Exception {
                java.util.List<MedicineSuggestion> list = new java.util.ArrayList<>();
                String like = "%" + query + "%";

                boolean useBatch = !batchFilter.isBlank();
                String sql = useBatch ? """
                SELECT DISTINCT m.id,
                       COALESCE(m.display_name, m.name) AS dn,
                       COALESCE(m.strength,'') AS st
                FROM medicines m
                JOIN medicine_batches b ON b.medicine_id = m.id
                WHERE (COALESCE(m.display_name, m.name) ILIKE ? OR m.name ILIKE ?)
                  AND b.batch_no ILIKE ?
                ORDER BY
                  POSITION(LOWER(?) IN LOWER(COALESCE(m.display_name,m.name))) NULLS LAST,
                  LENGTH(COALESCE(m.display_name,m.name)),
                  COALESCE(m.display_name,m.name)
                LIMIT 20
            """ : """
                SELECT m.id,
                       COALESCE(m.display_name, m.name) AS dn,
                       COALESCE(m.strength,'') AS st
                FROM medicines m
                WHERE (COALESCE(m.display_name, m.name) ILIKE ? OR m.name ILIKE ?)
                ORDER BY
                  POSITION(LOWER(?) IN LOWER(COALESCE(m.display_name,m.name))) NULLS LAST,
                  LENGTH(COALESCE(m.display_name,m.name)),
                  COALESCE(m.display_name,m.name)
                LIMIT 20
            """;

                try (Connection c = Database.get();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    if (useBatch) {
                        ps.setString(1, like);
                        ps.setString(2, like);
                        ps.setString(3, "%" + batchFilter + "%");
                        ps.setString(4, query);
                    } else {
                        ps.setString(1, like);
                        ps.setString(2, like);
                        ps.setString(3, query);
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (!isCancelled() && rs.next()) {
                            list.add(new MedicineSuggestion(
                                    rs.getLong("id"),
                                    rs.getString("dn"),
                                    rs.getString("st"),
                                    false
                            ));
                        }
                    }
                }

                // ثبت "Add new" كأول صف دائمًا
                java.util.ArrayList<MedicineSuggestion> out = new java.util.ArrayList<>();
                out.add(new MedicineSuggestion(-1, "➕ Add new: " + query, "", true));
                out.addAll(list);
                return out;
            }
        };

        currentMedTask = task;

        task.setOnSucceeded(ev -> {
            if (task.isCancelled()) return;
            medSuggestions.setAll(task.getValue());
            showSuggest(); // يظهر من أول حرف
            if (!medSuggestions.isEmpty()) {
                tblMedSuggest.getSelectionModel().selectFirst();
            }
        });
        task.setOnFailed(ev -> {
            if (task.isCancelled()) return;
            medSuggestions.clear();
            // حتى لو فشل، خلّي "Add new" متاح بالأعلى
            medSuggestions.add(new MedicineSuggestion(-1, "➕ Add new: " + query, "", true));
            showSuggest();
        });

        new Thread(task, "med-suggest").start();
    }

    private void openAddMedicineDialog(String prefill) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Add Medicine");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        // ---- Prefill dialog from last pending draft (if any) ----
        // If user previously entered values (but hasn't saved), reuse them
        if (pendingNewMedicine != null) {
            // Name: prefer typed 'prefill' if provided, otherwise keep previous
            if (prefill != null && !prefill.isBlank()) {
                // keep provided prefill
            } else if (pendingNewMedicine.name != null && !pendingNewMedicine.name.isBlank()) {
                prefill = pendingNewMedicine.name;
            }
        }

        TextField name      = new TextField(prefill);
        TextField strength  = new TextField();
        javafx.scene.control.ComboBox<String> formCb = new javafx.scene.control.ComboBox<>();
        formCb.setEditable(true);

        ChoiceBox<String> baseUnit = new ChoiceBox<>(FXCollections.observableArrayList(
                "TABLET","CAPSULE","SYRUP","SUSPENSION","INJECTION","CREAM","OINTMENT","DROPS","SPRAY"
        ));
        baseUnit.getSelectionModel().select("TABLET");

        // Packaging controls
        Spinner<Integer> tabletsPerBlister = new Spinner<>(1, 1000, 10);
        Spinner<Integer> blistersPerBox    = new Spinner<>(1, 1000, 1);
        Spinner<Integer> mlPerBottle       = new Spinner<>(1, 5000, 100);
        Spinner<Integer> gramsPerTube      = new Spinner<>(1, 1000, 20);
        CheckBox splitAllowed              = new CheckBox("Split allowed");

        // Enable/disable depending on base unit
        Runnable updatePackaging = () -> {
            String u = baseUnit.getSelectionModel().getSelectedItem();
            boolean isTabOrCap = "TABLET".equals(u) || "CAPSULE".equals(u);
            boolean isSyr      = "SYRUP".equals(u) || "SUSPENSION".equals(u) || "DROPS".equals(u);
            boolean isCream    = "CREAM".equals(u) || "OINTMENT".equals(u) || "SPRAY".equals(u);

            tabletsPerBlister.setDisable(!isTabOrCap);
            blistersPerBox.setDisable(!isTabOrCap);
            splitAllowed.setDisable(!isTabOrCap);

            mlPerBottle.setDisable(!isSyr);
            gramsPerTube.setDisable(!isCream);
        };

        // Suggested "Form" options per base unit (editable; DB column is free-text)
        java.util.Map<String, java.util.List<String>> FORM_OPTIONS = new java.util.HashMap<>();
        FORM_OPTIONS.put("TABLET",     java.util.List.of("Oral", "Chewable", "Sublingual"));
        FORM_OPTIONS.put("CAPSULE",    java.util.List.of("Oral"));
        FORM_OPTIONS.put("SYRUP",      java.util.List.of("Oral"));
        FORM_OPTIONS.put("SUSPENSION", java.util.List.of("Oral"));
        FORM_OPTIONS.put("DROPS",      java.util.List.of("Oral", "Eye", "Ear", "Nasal"));
        FORM_OPTIONS.put("INJECTION",  java.util.List.of("IV", "IM", "SC"));
        FORM_OPTIONS.put("CREAM",      java.util.List.of("Topical"));
        FORM_OPTIONS.put("OINTMENT",   java.util.List.of("Topical"));
        FORM_OPTIONS.put("SPRAY",      java.util.List.of("Topical", "Nasal"));

        Runnable refreshFormOptions = () -> {
            String u = baseUnit.getSelectionModel().getSelectedItem();
            java.util.List<String> opts = FORM_OPTIONS.getOrDefault(u, java.util.List.of());
            formCb.getItems().setAll(opts);
            // Preserve previously typed value if any
            String current = (formCb.getEditor() != null) ? formCb.getEditor().getText() : null;
            if (current != null && !current.isBlank() && !opts.contains(current)) {
                // keep the custom text
                formCb.getEditor().setText(current);
            } else if (!opts.isEmpty()) {
                // default to first option if nothing typed/selected
                if (formCb.getValue() == null || formCb.getValue().isBlank()) {
                    formCb.setValue(opts.get(0));
                }
            }
        };

        // If a previous draft exists, prefill the rest of fields
        if (pendingNewMedicine != null) {
            if (pendingNewMedicine.strength != null) strength.setText(pendingNewMedicine.strength);
            if (pendingNewMedicine.baseUnit != null) baseUnit.getSelectionModel().select(pendingNewMedicine.baseUnit);
            // Apply enable/disable according to selected base unit
            updatePackaging.run();
            refreshFormOptions.run();
            if (pendingNewMedicine.form != null && !pendingNewMedicine.form.isBlank()) {
                if (formCb.isEditable()) {
                    formCb.getEditor().setText(pendingNewMedicine.form);
                }
                if (!formCb.getItems().contains(pendingNewMedicine.form)) {
                    formCb.getItems().add(pendingNewMedicine.form);
                }
                formCb.setValue(pendingNewMedicine.form);
            }
            if (pendingNewMedicine.tabletsPerBlister != null) tabletsPerBlister.getValueFactory().setValue(pendingNewMedicine.tabletsPerBlister);
            if (pendingNewMedicine.blistersPerBox    != null) blistersPerBox.getValueFactory().setValue(pendingNewMedicine.blistersPerBox);
            if (pendingNewMedicine.mlPerBottle       != null) mlPerBottle.getValueFactory().setValue(pendingNewMedicine.mlPerBottle);
            if (pendingNewMedicine.gramsPerTube      != null) gramsPerTube.getValueFactory().setValue(pendingNewMedicine.gramsPerTube);
            if (pendingNewMedicine.splitAllowed      != null) splitAllowed.setSelected(Boolean.TRUE.equals(pendingNewMedicine.splitAllowed));
        }
        baseUnit.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            updatePackaging.run();
            refreshFormOptions.run();
        });
        updatePackaging.run();
        refreshFormOptions.run();

        GridPane gp = new GridPane();
        gp.setHgap(8); gp.setVgap(8);
        int r = 0;
        gp.addRow(r++, new Label("Name:"),     name);
        gp.addRow(r++, new Label("Strength:"), strength);
        gp.addRow(r++, new Label("Form:"),     formCb);
        gp.addRow(r++, new Label("Base Unit:"),baseUnit);
        gp.addRow(r++, new Label("Tablets/Blister:"), tabletsPerBlister);
        gp.addRow(r++, new Label("Blisters/Box:"),    blistersPerBox);
        gp.addRow(r++, new Label("mL/Bottle:"),       mlPerBottle);
        gp.addRow(r++, new Label("g/Tube:"),          gramsPerTube);
        gp.add(splitAllowed, 1, r++);

        dlg.getDialogPane().setContent(gp);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.disableProperty().bind(name.textProperty().isEmpty());

        dlg.setResultConverter(button -> button);
        var res = dlg.showAndWait();
        // If user pressed Cancel or closed the dialog, exit early
        if (res.isEmpty() || res.get() == ButtonType.CANCEL) {
            return;
        }

        // Prepare draft without touching DB
        NewMedicineDraft draft = new NewMedicineDraft();
        draft.name = name.getText().trim();
        draft.strength = strength.getText().trim();
        String formVal = (formCb.isEditable() ? formCb.getEditor().getText() : formCb.getValue());
        draft.form = (formVal == null || formVal.isBlank()) ? "" : formVal.trim();
        draft.baseUnit = baseUnit.getValue();
        draft.tabletsPerBlister = tabletsPerBlister.isDisabled()? null : tabletsPerBlister.getValue();
        draft.blistersPerBox    = blistersPerBox.isDisabled()?    null : blistersPerBox.getValue();
        draft.mlPerBottle       = mlPerBottle.isDisabled()?       null : mlPerBottle.getValue();
        draft.gramsPerTube      = gramsPerTube.isDisabled()?      null : gramsPerTube.getValue();
        draft.splitAllowed      = splitAllowed.isDisabled()?      null : splitAllowed.isSelected();

        final String dialogName = name.getText().trim();
        if (MedicineNameRecive != null) {
            MedicineNameRecive.setText(dialogName);
        }

// 2) ابنِ سطر تفاصيل مختصر واعرضه في MedicineLabelDt
        StringBuilder details = new StringBuilder();

// أضف form ثم strength لو موجودين
        if (draft.form != null && !draft.form.isBlank()) {
            details.append(draft.form.trim());
        }
        if (draft.strength != null && !draft.strength.isBlank()) {
            if (details.length() > 0) details.append(" • ");
            details.append(draft.strength.trim());
        }

// أضف وحدة الأساس
        if (draft.baseUnit != null) {
            if (details.length() > 0) details.append(" • ");
            details.append(draft.baseUnit);
        }

// أضف بيانات التغليف حسب النوع
        if ("TABLET".equals(draft.baseUnit) || "CAPSULE".equals(draft.baseUnit)) {
            if (draft.tabletsPerBlister != null && draft.blistersPerBox != null) {
                details.append(" • ")
                        .append(draft.tabletsPerBlister).append("/blister × ")
                        .append(draft.blistersPerBox).append(" blisters");
            }
            if (Boolean.TRUE.equals(draft.splitAllowed)) {
                details.append(" • split allowed");
            }
        } else if ("SYRUP".equals(draft.baseUnit) || "SUSPENSION".equals(draft.baseUnit) || "DROPS".equals(draft.baseUnit)) {
            if (draft.mlPerBottle != null) {
                details.append(" • ").append(draft.mlPerBottle).append(" mL/bottle");
            }
        } else if ("CREAM".equals(draft.baseUnit) || "OINTMENT".equals(draft.baseUnit) || "SPRAY".equals(draft.baseUnit)) {
            if (draft.gramsPerTube != null) {
                details.append(" • ").append(draft.gramsPerTube).append(" g/tube");
            }
        }

// ادفع النص للّيبِل الخارجي
        if (MedicineLabelDt != null) {
            MedicineLabelDt.setText(details.toString());
        }

        if (descriptionTf != null) {
            String desc = descriptionTf.getText();
            draft.description = (desc == null || desc.isBlank()) ? null : desc.trim();
        }

        pendingNewMedicine = draft;          // mark as pending create
        selectedMedicineId = null;           // ensure we create on Save
        // Fill the text box for user clarity
//        String disp = draft.name
//                + (draft.strength == null || draft.strength.isBlank()? "" : " " + draft.strength)
//                + (draft.form == null || draft.form.isBlank()? "" : " " + draft.form);
//        if (MedicineNameRecive != null) MedicineNameRecive.setText(disp);
        if (MedicineNameRecive != null) MedicineNameRecive.setText(dialogName);

        String disp = draft.name
                + (draft.strength == null || draft.strength.isBlank()? "" : " " + draft.strength)
                + (draft.form == null || draft.form.isBlank()? "" : " " + draft.form);
        hideSuggest();
        showInfo("Medicine details captured. It will be saved together with the batch.");
    }

    /**
     * Save the new medicine to the medicines table, including the description.
     */
    private void saveMedicineToDb(String name, String strength, String form, String baseUnit,
                                  Integer tabletsPerBlister, Integer blistersPerBox,
                                  Integer mlPerBottle, Integer gramsPerTube,
                                  String description) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO medicines " +
                             "(name, strength, form, base_unit, tablets_per_blister, blisters_per_box, ml_per_bottle, grams_per_tube, description) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, name);
            ps.setString(2, strength);
            ps.setString(3, form);
            ps.setString(4, baseUnit);
            if (tabletsPerBlister != null) ps.setInt(5, tabletsPerBlister); else ps.setNull(5, java.sql.Types.INTEGER);
            if (blistersPerBox != null) ps.setInt(6, blistersPerBox); else ps.setNull(6, java.sql.Types.INTEGER);
            if (mlPerBottle != null) ps.setInt(7, mlPerBottle); else ps.setNull(7, java.sql.Types.INTEGER);
            if (gramsPerTube != null) ps.setInt(8, gramsPerTube); else ps.setNull(8, java.sql.Types.INTEGER);
            ps.setString(9, description);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
            if (alertLabel != null) alertLabel.setText("Failed to add medicine: " + ex.getMessage());
        }
    }

    /**
     * Try to resolve the currently logged-in user id without compile-time dependencies.
     * Looks for common session holders via reflection:
     *  - com.example.healthflow.auth.Session#getCurrentUserId()
     *  - com.example.healthflow.controllers.LoginController#getLoggedInUserId()
     *  - com.example.healthflow.core.AppSession#getUserId()
     * Returns null if not found.
     */
    private Long tryResolveCurrentUserId() {
        String[] candidates = new String[] {
                "com.example.healthflow.auth.Session#getCurrentUserId",
                "com.example.healthflow.controllers.LoginController#getLoggedInUserId",
                "com.example.healthflow.core.AppSession#getUserId"
        };
        for (String sig : candidates) {
            try {
                String cls = sig.substring(0, sig.indexOf('#'));
                String mth = sig.substring(sig.indexOf('#') + 1);
                Class<?> c = Class.forName(cls);
                java.lang.reflect.Method m = c.getDeclaredMethod(mth);
                Object v = m.invoke(null);
                if (v instanceof Number n) return n.longValue();
                if (v != null) return Long.valueOf(v.toString());
            } catch (Throwable ignore) {}
        }
        return null;
    }

    /**
     * Map a user_id to pharmacist_id.
     */
    private Long findPharmacistIdByUser(Long userId) {
        if (userId == null) return null;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT id FROM pharmacists WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception ignore) {}
        return null;
    }

    @FXML
    private void onSaveReceive() {
    // If an Excel import is pending, commit it and skip manual validations
        if (lastImportResult != null && lastImportResult.rows != null && !lastImportResult.rows.isEmpty()) {
            commitImportedRowsToDb();
            return;
        }

        final String medText = (MedicineNameRecive != null) ? MedicineNameRecive.getText().trim() : "";
        if ((selectedMedicineId == null) && medText.isBlank()) {
            showWarn("Receive", "Select a medicine or type its name.");
            return;
        }

        Integer qty = null;
        try { qty = Integer.valueOf(quantity.getText().trim()); } catch (Exception ignore) {}
        if (qty == null || qty <= 0) {
            showWarn("Receive", "Quantity must be a positive integer.");
            return;
        }

        final LocalDate exp = (ExpiryDate != null) ? ExpiryDate.getValue() : null;
        if (exp == null) {
            showWarn("Receive", "Expiry date is required.");
            return;
        }

        String batch = (batchNum != null && batchNum.getText() != null) ? batchNum.getText().trim() : "";
        if (batch.isBlank()) {
            batch = "AUTO-" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        }

        final Long currentPharmacistId = requireCurrentPharmacistId();
        if (currentPharmacistId == null) return;

        Long medId = selectedMedicineId;
        if (medId == null && pendingNewMedicine != null) {
            try (Connection c = Database.get();
                 PreparedStatement ps = c.prepareStatement("""
                INSERT INTO medicines
                  (name, strength, form, base_unit,
                   tablets_per_blister, blisters_per_box, ml_per_bottle, grams_per_tube, split_allowed, description)
                VALUES
                  (?, NULLIF(?,''), NULLIF(?,''), ?::med_unit,
                   ?, ?, ?, ?, ?, NULLIF(?, ''))
                ON CONFLICT DO NOTHING
                RETURNING id
            """)) {
                ps.setString(1, pendingNewMedicine.name);
                ps.setString(2, pendingNewMedicine.strength);
                ps.setString(3, pendingNewMedicine.form);
                ps.setString(4, pendingNewMedicine.baseUnit);
                ps.setObject(5, pendingNewMedicine.tabletsPerBlister);
                ps.setObject(6, pendingNewMedicine.blistersPerBox);
                ps.setObject(7, pendingNewMedicine.mlPerBottle);
                ps.setObject(8, pendingNewMedicine.gramsPerTube);
                ps.setObject(9, pendingNewMedicine.splitAllowed);
                ps.setObject(10, pendingNewMedicine.description);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) medId = rs.getLong(1);
                }
            } catch (Exception ex) {
                showError("Failed to create medicine: " + ex.getMessage());
                return;
            }

            if (medId == null) {
                medId = resolveMedicineIdByDisplayOrName(
                        MedicineNameRecive != null ? MedicineNameRecive.getText().trim() : pendingNewMedicine.name);
            }
            selectedMedicineId = medId;
            pendingNewMedicine = null;
        }

        if (medId == null) medId = resolveMedicineIdByDisplayOrName(medText);
        if (medId == null) {
            showWarn("Receive", "Please select/add a medicine.");
            return;
        }

        try (Connection c = Database.get()) {
            c.setAutoCommit(false);

            long batchId;

            // ✅ الآن نضيف quantity لأن العمود NOT NULL
            final String upsertBatch = """
            INSERT INTO medicine_batches (medicine_id, batch_no, expiry_date, quantity)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (medicine_id, batch_no) DO UPDATE
               SET expiry_date = LEAST(medicine_batches.expiry_date, EXCLUDED.expiry_date),
                   quantity = medicine_batches.quantity + EXCLUDED.quantity
            RETURNING id
        """;
            try (PreparedStatement ps = c.prepareStatement(upsertBatch)) {
                ps.setLong(1, medId);
                ps.setString(2, batch);
                ps.setDate(3, java.sql.Date.valueOf(exp));
                ps.setInt(4, qty);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); batchId = rs.getLong(1); }
            }

            final String txSql = """
            INSERT INTO inventory_transactions (medicine_id, batch_id, qty_change, reason, ref_type, pharmacist_id)
            VALUES (?, ?, ?, 'RECEIVE', 'manual_receive', ?)
        """;
            try (PreparedStatement ps = c.prepareStatement(txSql)) {
                ps.setLong(1, medId);
                ps.setLong(2, batchId);
                ps.setInt(3, qty);
                if (currentPharmacistId == null)
                    ps.setNull(4, java.sql.Types.BIGINT);
                else
                    ps.setLong(4, currentPharmacistId);
                ps.executeUpdate();
            }

            c.commit();
            showInfo("Batch received successfully.");

            selectedMedicineId = null;
            if (MedicineNameRecive != null) MedicineNameRecive.clear();
            if (batchNum != null) batchNum.clear();
            if (quantity != null) quantity.clear();
            if (ExpiryDate != null) ExpiryDate.setValue(null);
            if (MedicineLabelDt != null) MedicineLabelDt.setText("");
            hideSuggest();

            showInventoryMainMode();
            btnInventory.setSelected(true);
            btnReceive.setSelected(false);
            reloadInventoryTable();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Failed to receive batch: " + ex.getMessage());
        }


    }
    // --- Draft holder used when user adds a medicine but wants to save it together with the batch later
    private static final class NewMedicineDraft {
        String name;
        String strength;
        String form;
        String baseUnit; // matches med_unit enum text
        Integer tabletsPerBlister;
        Integer blistersPerBox;
        Integer mlPerBottle;
        Integer gramsPerTube;
        Boolean splitAllowed;
        String description;
    }
    private NewMedicineDraft pendingNewMedicine; // not-null means: create medicine at Save
    private Long resolveMedicineIdByDisplayOrName(String text) {
        if (text == null || text.isBlank()) return null;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("""
            SELECT id FROM medicines
            WHERE lower(COALESCE(display_name, name)) = lower(?)
               OR COALESCE(display_name, name) ILIKE ?
            ORDER BY CASE WHEN lower(COALESCE(display_name,name)) = lower(?) THEN 0 ELSE 1 END,
                     similarity(COALESCE(display_name,name), ?) DESC
            LIMIT 1
         """)) {
            ps.setString(1, text);
            ps.setString(2, "%" + text + "%");
            ps.setString(3, text);
            ps.setString(4, text);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getLong(1); }
        } catch (Exception ignore) {}
        return null;
    }

    // --- Utility alert methods ---
    private void showWarn(String title, String msg) {
        javafx.application.Platform.runLater(() -> {
            var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private void showInfo(String msg) {
        javafx.application.Platform.runLater(() -> {
            var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Info");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private void showError(String title, Exception ex) {
        ex.printStackTrace();
        javafx.application.Platform.runLater(() -> {
            var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        });
    }

    // Overloads to support calls that pass only a message, or (title, message)
    private void showError(String msg) {
        javafx.application.Platform.runLater(() -> {
            var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private void showError(String title, String msg) {
        javafx.application.Platform.runLater(() -> {
            var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    /**
     * Apply '—' placeholder for empty/null inventory cells to keep the grid tidy.
     */
    private void applyInventoryNullPlaceholders() {
        if (colMedicineNameInventory != null) {
            colMedicineNameInventory.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (item == null || item.isBlank() ? "—" : item));
                }
            });
        }
        if (colMedicineForm != null) {
            colMedicineForm.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (item == null || item.isBlank() ? "—" : item));
                }
            });
        }
        if (colMedicineBase != null) {
            colMedicineBase.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (item == null || item.isBlank() ? "—" : item));
                }
            });
        }
        if (colQuantityInventory != null) {
            colQuantityInventory.setCellFactory(col -> new javafx.scene.control.TableCell<InventoryRow, Number>() {
                @Override protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (item == null ? "—" : item.toString()));
                }
            });
        }
        if (colMedicineBatchNextNumber != null) {
            colMedicineBatchNextNumber.setCellFactory(col -> new javafx.scene.control.TableCell<InventoryRow, String>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (item == null || item.isBlank() ? "—" : item));
                }
            });
        }
        if (colQtyNext != null) {
            colQtyNext.setCellFactory(col -> new javafx.scene.control.TableCell<InventoryRow, Number>() {
                @Override protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (item == null ? "—" : item.toString()));
                }
            });
        }
        if (colExpiryNext != null) {
            colExpiryNext.setCellFactory(col -> new javafx.scene.control.TableCell<InventoryRow, java.time.LocalDate>() {
                @Override protected void updateItem(java.time.LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setText(null); return; }
                    setText(item == null ? "—" : item.toString());
                }
            });
        }
        if (colReceivedBy != null) {
            colReceivedBy.setCellFactory(col -> new javafx.scene.control.TableCell<InventoryRow, String>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (item == null || item.isBlank() ? "—" : item));
                }
            });
        }
        if (colReceivedAt != null) {
            colReceivedAt.setCellFactory(col -> new javafx.scene.control.TableCell<InventoryRow, String>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (item == null || item.isBlank() ? "—" : item));
                }
            });
        }
    }


    public static final class MedicineSuggestion {
        public final long id;           // -1 = Add new
        public final String name;
        public final String strength;
        public final boolean addNew;
        public MedicineSuggestion(long id, String name, String strength, boolean addNew) {
            this.id=id; this.name=name; this.strength=strength; this.addNew=addNew;
        }
        public String display() {
            return name + (strength==null||strength.isBlank()? "" : " " + strength);
        }
    }

    /** يحسب ملخّص الجرد (إجمالي/منخفض/قريب الانتهاء) ويعرضه في الكروت. */
    private void updateInventorySummary() {
        int total = (inventoryRows == null) ? 0 : inventoryRows.size();

        int low = 0;
        int soon = 0;
        java.time.LocalDate today = java.time.ZonedDateTime.now(APP_TZ).toLocalDate();
        java.time.LocalDate soonLimit = today.plusDays(EXPIRY_SOON_DAYS);

        if (inventoryRows != null) {
            for (InventoryRow r : inventoryRows) {
                if (r == null) continue;

                // استخدم عتبة الدواء الخاصة (reorder_threshold) إن وجدت، وإلا استخدم القيمة العامة
                int threshold = (r.getReorderThreshold() == null)
                        ? LOW_STOCK_THRESHOLD_UNITS
                        : r.getReorderThreshold();

                // Low stock check
                if (r.getAvailableQuantity() <= threshold) {
                    low++;
                }

                // Expiring soon (يقترب تاريخ الانتهاء)
                java.time.LocalDate exp = r.getNextExpiry();
                if (exp != null && !exp.isBefore(today) && !exp.isAfter(soonLimit)) {
                    soon++;
                }
            }
        }

        if (TotalmedicinesNumber != null)
            TotalmedicinesNumber.setText(String.valueOf(total));
        if (LowStockMedicine != null)
            LowStockMedicine.setText(String.valueOf(low));
        if (ExpiringSoonMedicine != null)
            ExpiringSoonMedicine.setText(String.valueOf(soon));
    }


    /** Show details of a selected inventory row and allow editing its reorder threshold. */
    private void showInventoryDetailsDialog(InventoryRow row) {
        if (row == null) return;

        GridPane gp = new GridPane();
        gp.setHgap(12);
        gp.setVgap(8);

        int r = 0;
        gp.addRow(r++, new Label("Name:"),        new Label(row.getDisplayName()));
        gp.addRow(r++, new Label("Form:"),        new Label(row.getForm() == null ? "—" : row.getForm()));
        gp.addRow(r++, new Label("Base:"),        new Label(row.getBaseUnit() == null ? "—" : row.getBaseUnit()));
        gp.addRow(r++, new Label("Available:"),   new Label(String.valueOf(row.getAvailableQuantity())));
        gp.addRow(r++, new Label("Next Batch:"),  new Label(row.getNextBatchNo() == null ? "—" : row.getNextBatchNo()));
        gp.addRow(r++, new Label("Qty (Next):"),  new Label(row.getNextBatchQty() == null ? "—" : row.getNextBatchQty().toString()));
        gp.addRow(r++, new Label("Expiry (Next):"),
                new Label(row.getNextExpiry() == null ? "—" : row.getNextExpiry().toString()));
        gp.addRow(r++, new Label("Received By:"), new Label(row.getReceivedBy() == null ? "—" : row.getReceivedBy()));
        gp.addRow(r++, new Label("Received At:"), new Label(row.getReceivedAt() == null ? "—" : row.getReceivedAt()));

        // threshold الحالي (لو null يستخدم الديفولت العام)
        final int currentThr = (row.getReorderThreshold() == null)
                ? LOW_STOCK_THRESHOLD_UNITS
                : row.getReorderThreshold();

        // خانة إدخال قابلة للتعديل
        Spinner<Integer> thrSpinner = new Spinner<>(0, Integer.MAX_VALUE, currentThr);
        thrSpinner.setEditable(true);
        gp.addRow(r++, new Label("Reorder threshold:"), thrSpinner);

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Medicine Details");
        dlg.getDialogPane().setContent(gp);

        ButtonType BTN_SAVE = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().setAll(BTN_SAVE, ButtonType.CLOSE);

        // تصحيح الإدخال عند الكتابة في محرر السبنر
        final TextField editor = thrSpinner.getEditor();
        editor.textProperty().addListener((obs, oldV, newV) -> {
            try {
                int v = Integer.parseInt(newV.trim());
                if (v < 0) throw new NumberFormatException();
                thrSpinner.getValueFactory().setValue(v);
            } catch (Exception ignore) { /* نترك القيمة السابقة لحين الضغط Save */ }
        });

        dlg.setResultConverter(bt -> bt);
        var res = dlg.showAndWait();
        if (res.isEmpty() || res.get() != BTN_SAVE) return;

        int newThr = thrSpinner.getValue();
        if (newThr < 0) {
            showWarn("Invalid value", "Please enter a non-negative integer.");
            return;
        }

        try {
            queries.updateReorderThreshold(row.getMedicineId(), newThr);
            reloadInventoryTable();
            showInfo("Reorder threshold updated.");
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Failed to update threshold: " + ex.getMessage());
        }
    }

    // ===== Excel: Download Template =====
    @FXML
    private void onDownloadTemplate() {
        try {
            javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
            dc.setTitle("Choose folder to save template");
            if (lastExcelDir != null && java.nio.file.Files.isDirectory(lastExcelDir)) {
                dc.setInitialDirectory(lastExcelDir.toFile());
            }
            var window = (rootPane == null || rootPane.getScene() == null) ? null : rootPane.getScene().getWindow();
            java.io.File dir = dc.showDialog(window);
            if (dir == null) return;

            // generate template
            java.nio.file.Path out = com.example.healthflow.io.ExcelInventoryIO.writeReceiveTemplate(dir.toPath());

            lastExcelDir = dir.toPath(); // remember

            if (labelFileDetails != null) {
                labelFileDetails.setText("Template saved: " + out.toAbsolutePath());
            }
            showInfo("Template created:\n" + out.toAbsolutePath());
        } catch (Throwable ex) {
            ex.printStackTrace();
            showError("Failed to create template: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        }
    }

    // ===== Excel: Import (.xlsx) =====
    @FXML
    private void onImportExcelFile() {



        try {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Select Receive Excel (.xlsx)");
            fc.getExtensionFilters().setAll(
                    new javafx.stage.FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
            if (lastExcelDir != null && java.nio.file.Files.isDirectory(lastExcelDir)) {
                fc.setInitialDirectory(lastExcelDir.toFile());
            }
            var window = (rootPane == null || rootPane.getScene() == null) ? null : rootPane.getScene().getWindow();
            java.io.File file = fc.showOpenDialog(window);
            if (file == null) return;

            if (!file.getName().toLowerCase().endsWith(".xlsx")) {
                showError("Invalid file format. Please select a valid Excel (.xlsx) file.");
                return;
            }

            lastExcelDir = file.getParentFile().toPath();

            var task = new javafx.concurrent.Task<com.example.healthflow.io.ExcelInventoryIO.Result>() {
                @Override
                protected com.example.healthflow.io.ExcelInventoryIO.Result call() throws Exception {
                    return com.example.healthflow.io.ExcelInventoryIO.readReceiveFile(file);
                }
            };

            task.setOnRunning(e -> {
                setBusy(true, "Reading Excel…");
            });
            task.setOnSucceeded(e -> {
                setBusy(false, null);
                var result = task.getValue();
                lastImportResult = result;

                int ok = (result == null) ? 0 : result.okCount();
                int err = (result != null && result.hasErrors()) ? result.errors.size() : 0;

                StringBuilder sb = new StringBuilder();
                sb.append("File: ").append(file.getName())
                        .append("\nRows OK: ").append(ok);
                if (err > 0) {
                    sb.append("\nErrors: ").append(err);
                    int limit = Math.min(5, err);
                    for (int i = 0; i < limit; i++) sb.append("\n - ").append(result.errors.get(i));
                    if (err > 5) sb.append("\n ... and ").append(err - 5).append(" more");
                }

                if (labelFileDetails != null) {
                    labelFileDetails.setWrapText(true);
                    labelFileDetails.setText(sb.toString());
                }

                if (saveBtnReceive != null) {
                    // The button might be bound in FXML; unbind before setting state.
                    if (saveBtnReceive.disableProperty().isBound()) {
                        saveBtnReceive.disableProperty().unbind();
                    }
                    boolean noOk = (ok == 0);
                    saveBtnReceive.setDisable(noOk);
                    saveBtnReceive.setText(noOk ? "Save" : ("Save (" + ok + " imported)"));
                }

                showInfo(sb.toString());
            });
            task.setOnFailed(e -> {
                setBusy(false, "Failed to import file");
                Throwable ex = task.getException();
                ex.printStackTrace();
                showError("Failed to import file: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
            });

            new Thread(task, "excel-read").start();

        } catch (Throwable ex) {
            setBusy(false, null);
            ex.printStackTrace();
            showError("Failed to import file: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        }
    }

    // احفظ نتيجة آخر استيراد لحد ما نضغط Save
    private com.example.healthflow.io.ExcelInventoryIO.Result lastImportResult;

    private void commitImportedRowsToDb() {
        if (lastImportResult == null || lastImportResult.rows == null || lastImportResult.rows.isEmpty()) {
            showWarn("Import", "No imported data available.");
            return;
        }
        final var rows = lastImportResult.rows;

        var task = new javafx.concurrent.Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                return commitImportedRowsToDbInternal(rows);
            }
        };

        task.setOnRunning(e -> setBusy(true, "Saving imported rows..."));
        task.setOnSucceeded(e -> {
            setBusy(false, null);
            int saved = task.getValue();
            showInfo("Imported rows committed successfully. Saved: " + saved);
            lastImportResult = null;
            if (saveBtnReceive != null) {
                if (saveBtnReceive.disableProperty().isBound()) {
                    saveBtnReceive.disableProperty().unbind();
                }
                saveBtnReceive.setText("Save");
                saveBtnReceive.setDisable(false);
            }
            reloadInventoryTable();
        });
        task.setOnFailed(e -> {
            setBusy(false, null);
            Throwable ex = task.getException();
            ex.printStackTrace();
            showError("Commit failed: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        });

        new Thread(task, "excel-commit").start();
    }
    private int commitImportedRowsToDbInternal(
            java.util.List<com.example.healthflow.io.ExcelInventoryIO.ReceiveRow> rows) throws Exception {

        if (rows == null || rows.isEmpty()) return 0;

        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            final Long pharmacistId = requireCurrentPharmacistId(); // ممكن يكون null

            final String upsertBatchSql = """
            INSERT INTO medicine_batches (medicine_id, batch_no, expiry_date, quantity)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (medicine_id, batch_no) DO UPDATE
               SET expiry_date = LEAST(medicine_batches.expiry_date, EXCLUDED.expiry_date),
                   quantity    = medicine_batches.quantity + EXCLUDED.quantity
            RETURNING id
        """;

            final String insertTxSql = """
            INSERT INTO inventory_transactions (medicine_id, batch_id, qty_change, reason, ref_type, pharmacist_id)
            VALUES (?, ?, ?, 'RECEIVE', 'excel_import', ?)
        """;

            final String createMedSql = """
            INSERT INTO medicines
              (name, strength, form, base_unit, description)
            VALUES
              (?, NULLIF(?,''), NULLIF(?,''), NULLIF(?, '')::med_unit, NULLIF(?, ''))
            ON CONFLICT DO NOTHING
            RETURNING id
        """;

            int ok = 0;

            try (PreparedStatement psBatch = c.prepareStatement(upsertBatchSql);
                 PreparedStatement psTx    = c.prepareStatement(insertTxSql);
                 PreparedStatement psMed   = c.prepareStatement(createMedSql)) {

                for (var row : rows) {
                    if (row == null) continue;

                    Long   medId     = row.getMedicineId();
                    String name      = safeTrim(row.getMedicineName());
                    String strength  = safeTrim(row.getStrength());
                    String form      = safeTrim(row.getForm());
                    String baseUnit  = safeTrim(row.getBaseUnit());
                    Integer qty      = row.getQuantity();
                    String batchNo   = safeTrim(row.getBatchNo());
                    // خُذ التاريخ من أي حقل موجود
                    java.time.LocalDate expiry = (row.getExpiryDate() != null ? row.getExpiryDate()
                            : row.getExpiry());
                    String desc = safeTrim(row.getDescription());

                    // تحقق صلاحية الصف
                    if (qty == null || qty <= 0 || batchNo == null || batchNo.isBlank() || expiry == null) {
                        continue;
                    }

                    // حلّ هوية الدواء إن كانت مفقودة
                    if (medId == null) {
                        // حاول باسم العرض (name + strength + form)
                        String displayTry = ((name == null) ? "" : name)
                                + ((strength == null || strength.isBlank()) ? "" : " " + strength)
                                + ((form == null || form.isBlank()) ? "" : " " + form);

                        medId = resolveMedicineIdByDisplayOrName(displayTry);

                        // أنشئ الدواء إذا ما لقيناه وكان عندنا اسم
                        if (medId == null && name != null && !name.isBlank()) {
                            psMed.clearParameters();
                            psMed.setString(1, name);
                            psMed.setString(2, strength);
                            psMed.setString(3, form);
                            psMed.setString(4, baseUnit);
                            psMed.setString(5, desc);
                            try (ResultSet rs = psMed.executeQuery()) {
                                if (rs.next()) medId = rs.getLong(1);
                            }
                            // لو ما رجع id (بسبب CONFLICT)، حاول نحلّه مجددًا بالاسم الصافي
                            if (medId == null) {
                                medId = resolveMedicineIdByDisplayOrName(name);
                            }
                        }
                    }
                    if (medId == null) continue; // ما نقدر نكمل بدون دواء

                    // UPSERT لدفعة المخزون
                    psBatch.clearParameters();
                    psBatch.setLong(1, medId);
                    psBatch.setString(2, batchNo);
                    psBatch.setDate(3, java.sql.Date.valueOf(expiry));
                    psBatch.setInt(4, qty);

                    long batchId;
                    try (ResultSet rs = psBatch.executeQuery()) {
                        rs.next();
                        batchId = rs.getLong(1);
                    }

                    // سجل الحركة
                    psTx.clearParameters();
                    psTx.setLong(1, medId);
                    psTx.setLong(2, batchId);
                    psTx.setInt(3,  qty);
                    if (pharmacistId == null) psTx.setNull(4, java.sql.Types.BIGINT);
                    else                      psTx.setLong(4, pharmacistId);
                    psTx.executeUpdate();

                    ok++;
                }
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            }

            c.commit();
            return ok;
        }
    }

    private static String safeTrim(String s) {
        return (s == null ? null : s.trim());
    }


    // (overlay no longer used; keeping fields for now)
    private StackPane busyOverlay;
    private Label busyText;

    private void ensureBusyOverlay() {
        if (rootPane == null) return;
        if (busyOverlay != null) return;

        busyOverlay = new StackPane();
        busyOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.35);");
        ProgressIndicator pi = new ProgressIndicator();
        busyText = new Label("Loading...");
        VBox box = new VBox(12, pi, busyText);
        box.setAlignment(Pos.CENTER);
        busyOverlay.getChildren().add(box);
        busyOverlay.setVisible(false);

        // rootPane عندك StackPane، فنقدر نركّب الطبقة فوقه
        rootPane.getChildren().add(busyOverlay);
        StackPane.setAlignment(busyOverlay, Pos.CENTER);
    }

    private void setBusy(boolean on, String msg) {
        javafx.application.Platform.runLater(() -> {
            // Show status in the footer label instead of dimming the whole UI.
            if (labelFileDetails != null) {
                String prefix = on ? "⏳ " : "✔ ";
                if (msg != null && !msg.isBlank()) {
                    labelFileDetails.setText(prefix + msg);
                } else if (!on) {
                    // keep the previous details text if we're just clearing busy state
                }
            }
            // Disable the Save button while busy (after unbinding if needed).
            if (saveBtnReceive != null) {
                if (saveBtnReceive.disableProperty().isBound()) {
                    saveBtnReceive.disableProperty().unbind();
                }
                saveBtnReceive.setDisable(on);
            }
        });
    }

    // reverce Date in Excel to add medicene
    private LocalDate parseFlexibleDate(String text) {
        if (text == null || text.isBlank()) return null;

        // جرّب أولاً الصيغة الصحيحة ISO (yyyy-MM-dd)
        try {
            return LocalDate.parse(text.trim());
        } catch (Exception ignore) {}

        // لو فشلت، جرّب الصيغ المعكوسة المحتملة
        DateTimeFormatter[] formats = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("MM-dd-yyyy")
        };

        for (DateTimeFormatter fmt : formats) {
            try {
                return LocalDate.parse(text.trim(), fmt);
            } catch (Exception ignore) {}
        }

        // لو ولا صيغة اشتغلت، رجّع null
        return null;
    }


    @FXML
    private void onSaveReceiveBtn() {
        // 1) أولاً: لو في استيراد من إكسل جاهز → نُجري الكومِت ونخرج
        if (lastImportResult != null
                && lastImportResult.rows != null
                && !lastImportResult.rows.isEmpty()) {
            commitImportedRowsToDb();
            return;
        }

        // 2) خلاف ذلك: مسار الحفظ اليدوي
        // نطبّع (normalize) تاريخ الانتهاء من محرر الـ DatePicker لو كان مكتوبًا نصيًا
        if (ExpiryDate != null && ExpiryDate.getValue() == null && ExpiryDate.getEditor() != null) {
            String txt = ExpiryDate.getEditor().getText();
            LocalDate exp = parseFlexibleDate(txt);
            if (exp != null) {
                // نثبّت التاريخ في الـ DatePicker حتى تستخدمه onSaveReceive()
                ExpiryDate.setValue(exp);
            }
        }

        // 3) نفوّض لباقي التحقق/الحفظ اليدوي الموجود عندك
        onSaveReceive();
    }

    // --- Make inventory table rows open the details dialog with editable threshold ---
    {
        javafx.application.Platform.runLater(() -> {
            if (saveBtnReceive != null) {
                // always route Save to the unified handler
                saveBtnReceive.setOnAction(ev -> onSaveReceiveBtn());
            }

            if (TableMedicinesInventory != null) {
                TableMedicinesInventory.setRowFactory(tv -> {
                    TableRow<InventoryRow> row = new TableRow<>();
                    row.setOnMouseClicked(ev -> {
                        if (ev.getClickCount() == 2 && !row.isEmpty()) {
                            showInventoryDetailsDialog(row.getItem());
                        }
                    });
                    return row;
                });
            }
        });
    }

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

    /** Login-like busy state for any Button: spinner on the left + text swap. */
    private void startBtnBusy(javafx.scene.control.Button b, String busyText) {
        if (b == null) return;
        try {
            if (!b.getProperties().containsKey("orig-text")) {
                b.getProperties().put("orig-text", b.getText());
            }
            // لا نعطّل الزر بالكامل حتى ما يخفت شكله؛ فقط امنع النقرات
            b.setMouseTransparent(true);

            javafx.scene.control.ProgressIndicator pi = new javafx.scene.control.ProgressIndicator();
            pi.setPrefSize(16, 16);
            pi.setMaxSize(16, 16);
            pi.setProgress(javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS);
            pi.setVisible(true);
            pi.setManaged(true);

            // لو أزرارك داكنة، خلّي المؤشر أبيض ليوضح (احذف السطر لو الخلفية فاتحة)
            pi.setStyle("-fx-progress-color: white;");

            b.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            b.setGraphicTextGap(8);
            b.setGraphic(pi);

            if (busyText != null && !busyText.isBlank()) {
                b.setText(busyText);
            }
        } catch (Throwable ignored) {}
    }

    private void stopBtnBusy(javafx.scene.control.Button b) {
        if (b == null) return;
        try {
            Object prev = b.getProperties().get("orig-text");
            if (prev instanceof String) b.setText((String) prev);
            b.getProperties().remove("orig-text");
            b.setGraphic(null);
            b.setMouseTransparent(false);
        } catch (Throwable ignored) {}
    }


    /** Enable/disable and visually mark the sidebar Prescriptions button */
    private void setPrescriptionSidebarState(boolean enabled, boolean active) {
        if (PrescriptionsButton == null) return;

        // فك أي binding موجود لتجنّب: "A bound value cannot be set"
        try { PrescriptionsButton.disableProperty().unbind(); } catch (Throwable ignored) {}

        PrescriptionsButton.setDisable(!enabled);

        var css = PrescriptionsButton.getStyleClass();
        css.remove("active");
        css.remove("nav-active");
        if (active) {
            if (!css.contains("active")) css.add("active");
            if (!css.contains("nav-active")) css.add("nav-active");
        }
    }

    /** مرّة واحدة: اربط أزرار الشريط الجانبي لتُعطّل زر الوصفات عند استخدامها */
    private void wireSidebarGuardsIfNeeded() {
        if (sidebarGuardWired) return;
        sidebarGuardWired = true;

        if (DashboardButton != null) {
            DashboardButton.addEventHandler(ActionEvent.ACTION,
                    e -> setPrescriptionSidebarState(false, false));
        }
        if (InventoryButton != null) {
            InventoryButton.addEventHandler(ActionEvent.ACTION,
                    e -> setPrescriptionSidebarState(false, false));
        }
        if (ReportsButton != null) {
            ReportsButton.addEventHandler(ActionEvent.ACTION,
                    e -> setPrescriptionSidebarState(false, false));
        }
        // لو عندك زر Back داخل واجهة الوصفة
        if (BackButton != null) {
            BackButton.addEventHandler(ActionEvent.ACTION,
                    e -> setPrescriptionSidebarState(false, false));
        }
    }
}
