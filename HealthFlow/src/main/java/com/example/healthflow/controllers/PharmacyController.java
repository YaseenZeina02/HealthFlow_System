package com.example.healthflow.controllers;

import com.example.healthflow.dao.PrescriptionDAO;
import com.example.healthflow.dao.PrescriptionDAO.DashboardRow;
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

import com.example.healthflow.net.ConnectivityMonitor;
import com.example.healthflow.ui.ConnectivityBanner;
import com.example.healthflow.ui.OnlineBindings;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.control.ToggleGroup;
import javafx.beans.value.ChangeListener;
import org.controlsfx.control.SegmentedButton;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;

public class PharmacyController {

    private final ConnectivityMonitor monitor;

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
    private TextArea ReasonOfDeduct;

    @FXML
    private AnchorPane ReceivePane;

    @FXML
    private Label RejectedNumber;

    @FXML
    private Button ReportsButton;

    @FXML
    private TableView<?> TableMedicinesInentory;

    @FXML
    private TableView<PrescItemRow> TablePrescriptionItems;

    @FXML
    private DatePicker PrescriptionDatePicker;


    @FXML
    private TableView<?> TableToShowMedicineByBatchNumber;

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
    private TableColumn<?, ?> colDiagnosisInentory;

    @FXML
    private TableColumn<DashboardRow, String> colDoctorName;

    @FXML
    private TableColumn<PrescItemRow, String> colDosage;

    @FXML
    private TableColumn<?, ?> colDosageInentory;

    @FXML
    private TableColumn<PrescItemRow, Number> colDose;

    @FXML
    private TableColumn<PrescItemRow, String> colForm;

    @FXML
    private TableColumn<PrescItemRow, Number> colIdx;

    @FXML
    private TableColumn<?, ?> colIdx11;

    @FXML
    private TableColumn<PrescItemRow, String> colMedicineName;

    @FXML
    private TableColumn<?, ?> colMedicineNameInentory;

    @FXML
    private TableColumn<?, ?> colMedicineNameInentoryByBatchNumber;

    @FXML
    private TableColumn<DashboardRow, String> colPatientName;

    @FXML
    private TableColumn<PrescItemRow, Void> colPresesItemAction;

    @FXML
    private TableColumn<?, ?> colPresesActionInentory;

    @FXML
    private TableColumn<PrescItemRow, Number> colQuantity;

    @FXML
    private TableColumn<?, ?> colQuantityInentory;

    @FXML
    private TableColumn<DashboardRow, Number> colSerialPhDashboard;

    @FXML
    private TableColumn<PrescItemRow, String> colStrength;

    @FXML
    private TableColumn<DashboardRow, String> colprescriptionStutus;

    // Controller state
    private DashboardRow selectedRow;
    private final ObservableList<DashboardRow> dashboardRows = FXCollections.observableArrayList();
    private final ObservableList<PrescItemRow> itemRows = FXCollections.observableArrayList();

    @FXML
    private TextField deductBatchNumber;

    @FXML
    private AnchorPane deductPane;

    @FXML
    private TextField guantityToDeduct;

    @FXML
    private Label lableToShowMedicinesName;

    @FXML
    private AnchorPane pharmacyDashboardAnchorPane;

    @FXML
    private TextField quantity;

    @FXML
    private Button saveBtnDeduct;

    @FXML
    private Button saveBtnReceive;

    @FXML
    private TextField searchDashbord;

    @FXML
    private TextField searchItems;

    @FXML
    private TextField searchOnInentory;

    @FXML
    private SegmentedButton segInv;

    @FXML
    private Label time;

    @FXML
    private Label userStatus;

    @FXML
    private Label welcomeUser;


    /* ====== Utilities / constants (match DoctorController style) ====== */
    private static final java.time.ZoneId APP_TZ = java.time.ZoneId.of("Asia/Gaza");
    private static final String ACTIVE_CLASS = "current";

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
        try (Connection c = Database.get()) {
            PrescriptionDAO dao = new PrescriptionDAO();
            int total = dao.countTotalOnDate(c, day);
            int waiting = dao.countPendingOnDate(c, day);
            int completed = dao.countCompletedOnDate(c, day);
            if (PrescriptionsTodayTotal != null) PrescriptionsTodayTotal.setText(String.valueOf(total));
            if (PrescriptionsWatingNum != null) PrescriptionsWatingNum.setText(String.valueOf(waiting));
            if (PrescriptionsCompleteNum != null) PrescriptionsCompleteNum.setText(String.valueOf(completed));
        } catch (Exception ex) {
            if (alertLabel != null) alertLabel.setText("Failed to load counts: " + ex.getMessage());
            System.err.println("[PharmacyController] refresh counts error: " + ex);
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
        setVisibleManaged(pharmacyDashboardAnchorPane, true);
        setVisibleManaged(PrescriptionAnchorPane, false);
        setVisibleManaged(InventoryAnchorPane, false);
        if (DashboardButton != null) markNavActive(DashboardButton);
        refreshPharmacyDashboardCounts();
        loadDashboardTable();
    }
    private void loadDashboardTable() {
        if (PresciptionsTable == null) return;
        dashboardRows.clear();
        LocalDate day = getSelectedDateOrToday();
        try (Connection c = Database.get()) {
            PrescriptionDAO dao = new PrescriptionDAO();
            dashboardRows.addAll(dao.listDashboardRowsByDate(c, day));
        } catch (Exception ex) {
            System.err.println("[PharmacyController] loadDashboardTable error: " + ex);
        }
        PresciptionsTable.setItems(dashboardRows);
    }

    private void setupDashboardTableColumns() {
        if (PresciptionsTable == null) return;
        // Serial #
        if (colSerialPhDashboard != null) {
            colSerialPhDashboard.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(PresciptionsTable.getItems().indexOf(cd.getValue()) + 1));
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
                        case PENDING -> "PENDING";
                        case APPROVED -> "Approved";
                        case REJECTED -> "Rejected";
                        case DISPENSED -> "Completed";
                    }
            ));
        }
        // Action column (View)
        if (colActionPhDashbord != null) {
            colActionPhDashbord.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                private final Button btn = new Button("View");
                {
//                    btn.getStyleClass().add("btn-primary");
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

//    private void showPrescriptionDetails(DashboardRow row) {
//        if (row == null) return;
//        this.selectedRow = row;
//        // Switch to prescription pane
//        showPrescriptionsPane();
//        // Header labels
//        if (PatientNameTF != null) {
//            String nid = row.patientNid == null ? "" : (" (" + row.patientNid + ")");
//            PatientNameTF.setText(row.patientName + nid);
//            PatientNameTF.setEditable(false);
//        }
//        if (DoctorNameLabel != null) DoctorNameLabel.setText(row.doctorName);
//        if (PharmacistNameLabel != null) PharmacistNameLabel.setText(UsernameLabel != null ? UsernameLabel.getText() : "");
//        if (AppointmentDate != null) {
//            var df = java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");
//            String appt = row.appointmentDateTime == null ? "—" : row.appointmentDateTime.atZoneSameInstant(APP_TZ).toLocalDateTime().format(df);
//            AppointmentDate.setText(appt);
//        }
//        if (DiagnosisView != null) DiagnosisView.setText(row.diagnosisNote == null ? "" : row.diagnosisNote);
//        // Load items
//        loadPrescriptionItems(row.prescriptionId);
//    }
    private void showPrescriptionDetails(DashboardRow row) {
        if (row == null) return;
        this.selectedRow = row;

        // Switch to prescription pane
        showPrescriptionsPane();

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
        try (Connection c = Database.get()) {
            PrescriptionItemDAO itemDao = new PrescriptionItemDAO();
            var items = itemDao.listByPrescription(c, prescId);
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
                itemRows.add(r);
            }
        } catch (Exception ex) {
            System.err.println("[PharmacyController] loadPrescriptionItems error: " + ex);
        }
        if (TablePrescriptionItems != null) TablePrescriptionItems.setItems(itemRows);
    }

    private void setupItemsTableColumns() {
        if (TablePrescriptionItems == null) return;
        if (colIdx != null) colIdx.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(TablePrescriptionItems.getItems().indexOf(cd.getValue()) + 1));
        if (colMedicineName != null) colMedicineName.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        if (colStrength != null) colStrength.setCellValueFactory(new PropertyValueFactory<>("strength"));
        if (colForm != null) colForm.setCellValueFactory(new PropertyValueFactory<>("form"));
        if (colDose != null) colDose.setCellValueFactory(new PropertyValueFactory<>("dose"));
        if (colDosage != null) {
            colDosage.setCellValueFactory(cd -> {
                String txt = cd.getValue().getDosageText();
                if (txt == null || txt.isBlank()) txt = cd.getValue().getDosage();
                return new javafx.beans.property.SimpleStringProperty(txt == null ? "" : txt);
            });
        }        if (colQuantity != null) colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
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
        PrescItemRow r = itemRows.get(rowIndex);
        try (Connection c = Database.get()) {
            PrescriptionItemDAO dao = new PrescriptionItemDAO();
            int qty = Math.max(0, r.getQuantity());
            dao.updateDispensed(c, r.getId(), qty, ItemStatus.APPROVED, null);
            r.setStatus("APPROVED");
            r.setQtyDispensed(qty);
            TablePrescriptionItems.refresh();
        } catch (Exception ex) {
            System.err.println("[PharmacyController] approve item error: " + ex);
        }
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

    private void showPrescriptionsPane() {
        setVisibleManaged(pharmacyDashboardAnchorPane, false);
        setVisibleManaged(PrescriptionAnchorPane, true);
        setVisibleManaged(InventoryAnchorPane, false);
        markNavActive(PrescriptionsButton);
        // Show the details area only when a prescription is selected later
        if (PrescriptionMedicationAnchorPane != null)
            setVisibleManaged(PrescriptionMedicationAnchorPane, true);
    }

    private void showInventoryPane() {
        setVisibleManaged(pharmacyDashboardAnchorPane, false);
        setVisibleManaged(PrescriptionAnchorPane, false);
        setVisibleManaged(InventoryAnchorPane, true);
        markNavActive(InventoryButton);
        // Ensure a default inventory mode
        if (btnReceive != null) {
            btnReceive.setSelected(true);
//            showInventoryMode(true);
            lastSelectedInvBtn = btnReceive;
        }
        if (segInv != null) {
            // ensure the segmented control actually contains our toggles
            if (!segInv.getButtons().contains(btnReceive) && btnReceive != null) segInv.getButtons().add(btnReceive);
            if (!segInv.getButtons().contains(btnDeduct)  && btnDeduct  != null) segInv.getButtons().add(btnDeduct);
        }
    }

    /* ====== Inventory Receive vs Deduct (like segmented behavior) ====== */
    private ToggleGroup invToggleGroup;
    private ToggleButton lastSelectedInvBtn;


    private void showInventoryReceiveMode() {
        ReceivePane.setVisible(true);
        deductPane.setVisible(false);
    }

    private void showInventoryDeductMode() {
        ReceivePane.setVisible(false);
        deductPane.setVisible(true);
    }


    private void wireInventoryToggles() {
        invToggleGroup = new ToggleGroup();
        if (btnReceive != null) btnReceive.setToggleGroup(invToggleGroup);
        if (btnDeduct != null)  btnDeduct.setToggleGroup(invToggleGroup);

        // Prevent deselection of the currently selected toggle by clicking it again
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

        // default & listener
        if (btnReceive != null) {
            btnReceive.setSelected(true);
//            showInventoryMode(true);
        }

        if (btnDeduct != null) {
            btnDeduct.setSelected(true);
//            showInventoryMode(false);
        }

//
        // Remember last selected; if selection becomes null, restore last
        if (invToggleGroup != null) {
            lastSelectedInvBtn = btnReceive != null ? btnReceive : btnDeduct;
            invToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                if (newT == null) {
                    if (lastSelectedInvBtn != null) invToggleGroup.selectToggle(lastSelectedInvBtn);
                    return;
                }
                if (newT instanceof ToggleButton tb) {
                    lastSelectedInvBtn = tb;
                }
            });
        }
        if (segInv != null) {
            if (btnReceive != null && !segInv.getButtons().contains(btnReceive)) segInv.getButtons().add(btnReceive);
            if (btnDeduct  != null && !segInv.getButtons().contains(btnDeduct))  segInv.getButtons().add(btnDeduct);
        }
    }

    public PharmacyController(ConnectivityMonitor monitor) {
        this.monitor = monitor;
    }

    // Default constructor for FXML loader
    public PharmacyController() {
        this(new ConnectivityMonitor());
    }

    @FXML
    private void initialize() {
        resolveLoggedInUserLabels();

        if (rootPane != null) {
            ConnectivityBanner banner = new ConnectivityBanner(monitor);
            rootPane.getChildren().add(0, banner);

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

        // Quick sanity check: if IDs are not wired, navigation won't work
        boolean missing = warnIfMissing();

        // Start connectivity monitor
        monitor.start();

        // Add connectivity banner at the top of the UI
        if (rootPane != null) {
            ConnectivityBanner banner = new ConnectivityBanner(monitor);
            rootPane.getChildren().add(0, banner);
        }

        // Start header clock & date (12h, Asia/Gaza)
        startClock();


        // Default the date picker to today (Asia/Gaza) and listen for changes
        if (PrescriptionDatePicker != null) {
            PrescriptionDatePicker.setValue(java.time.ZonedDateTime.now(APP_TZ).toLocalDate());
            PrescriptionDatePicker.valueProperty().addListener((obs, oldV, newV) -> {
                refreshPharmacyDashboardCounts();
                loadDashboardTable();
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

        // Back button: keep it simple for now — return to Dashboard
        if (BackButton != null) {
            BackButton.setOnAction(e -> showDashboardPane());
        }

        if (missing) {
            System.out.println("[PharmacyController] Some fx:id are missing. Verify FXML ids match controller fields.");
        }

        // Disable actions when offline (if OnlineBindings present)
        try { OnlineBindings.disableWhenOffline(monitor, DashboardButton, PrescriptionsButton, InventoryButton, saveBtnReceive, saveBtnDeduct); } catch (Throwable ignored) {}

        // Inventory toggles
        wireInventoryToggles();

        // Initial section
        showDashboardPane();

        // Hard fallback: enforce Dashboard default if panes exist
        if (pharmacyDashboardAnchorPane != null && InventoryAnchorPane != null) {
            if (!pharmacyDashboardAnchorPane.isVisible() && InventoryAnchorPane.isVisible()) {
                setVisibleManaged(InventoryAnchorPane, false);
                setVisibleManaged(pharmacyDashboardAnchorPane, true);
            }
        }
        refreshPharmacyDashboardCounts();
        setupDashboardTableColumns();
        setupItemsTableColumns();
        if (TablePrescriptionItems != null) {
            TablePrescriptionItems.setColumnResizePolicy(
                    javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY
            );
        }
        loadDashboardTable();
    }
}
