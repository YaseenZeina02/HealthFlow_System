package com.example.healthflow.core.inventory;

import com.example.healthflow.db.Database;
import javafx.animation.PauseTransition;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Handles Deduct operations for Pharmacy inventory.
 * Used in PharmacyController.
 */
public class DeductSupport {

    /** Data holder for batch search results. */
    public static final class DeductRow {
        public final long batchId;
        public final long medicineId;
        public final String medicineName;
        public final String batchNo;
        public final LocalDate expiry;
        public final int stock;

        // packaging fields (from medicines)
        public final String baseUnit;                // e.g., TABLET, SYRUP, SPRAY...
        public final Integer tabletsPerBlister;
        public final Integer blistersPerBox;
        public final Integer mlPerBottle;
        public final Integer gramsPerTube;
        public final Boolean splitAllowed;

        public DeductRow(long batchId, long medicineId, String medicineName,
                         String batchNo, LocalDate expiry, int stock,
                         String baseUnit, Integer tabletsPerBlister, Integer blistersPerBox,
                         Integer mlPerBottle, Integer gramsPerTube, Boolean splitAllowed) {
            this.batchId = batchId;
            this.medicineId = medicineId;
            this.medicineName = medicineName;
            this.batchNo = batchNo;
            this.expiry = expiry;
            this.stock = stock;

            this.baseUnit = baseUnit;
            this.tabletsPerBlister = tabletsPerBlister;
            this.blistersPerBox = blistersPerBox;
            this.mlPerBottle = mlPerBottle;
            this.gramsPerTube = gramsPerTube;
            this.splitAllowed = splitAllowed;
        }
    }

    // ---------------- Instance Fields ----------------
    private final ComboBox<String> cmbType;
    private final TableView<DeductRow> tbl;
    private final TableColumn<DeductRow, Number> colSerial;
    private final TableColumn<DeductRow, String> colName;
    private final TableColumn<DeductRow, Number> colStock;
    private final TableColumn<DeductRow, String> colExpiry;
    private final TextField txtSearch;
    private final TextField txtQty;
    private final TextArea txtReason;
    private final Button btnPlus, btnMinus, btnSave;
    private final Label lblSummary;
    private TextFormatter<Integer> qtyFormatter;

    private final ObservableList<DeductRow> rows = FXCollections.observableArrayList();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(250));

    private Long selBatchId, selMedicineId;
    private Integer selStock;
    private LocalDate selExpiry;

    // packaging details for the selected medicine
    private String selBaseUnit;
    private Integer selTabletsPerBlister;
    private Integer selBlistersPerBox;
    private Integer selMlPerBottle;
    private Integer selGramsPerTube;
    private Boolean selSplitAllowed;

    // guard to avoid clearing the table when we programmatically change the search field
    private boolean suppressSearchOnSelection = false;

    // Reactive properties for binding
    private final ObjectProperty<DeductRow> pickedBatchProperty = new SimpleObjectProperty<>();
    private final IntegerProperty deductQty = new SimpleIntegerProperty(1);
    private final StringProperty reasonText = new SimpleStringProperty("");

    // Optional callback when data updated (to refresh main inventory table)
    private Runnable onSaveCallback;

    // ---------------- Constructor ----------------
    public DeductSupport(ComboBox<String> cmbType,
                         TableView<DeductRow> tbl,
                         TableColumn<DeductRow, Number> colSerial,
                         TableColumn<DeductRow, String> colName,
                         TableColumn<DeductRow, Number> colStock,
                         TableColumn<DeductRow, String> colExpiry,
                         TextField txtSearch,
                         TextField txtQty,
                         TextArea txtReason,
                         Button btnPlus,
                         Button btnMinus,
                         Button btnSave,
                         Label lblSummary) {
        this.cmbType = cmbType;
        this.tbl = tbl;
        this.colSerial = colSerial;
        this.colName = colName;
        this.colStock = colStock;
        this.colExpiry = colExpiry;
        this.txtSearch = txtSearch;
        this.txtQty = txtQty;
        this.txtReason = txtReason;
        this.btnPlus = btnPlus;
        this.btnMinus = btnMinus;
        this.btnSave = btnSave;
        this.lblSummary = lblSummary;
    }

    // ---------------- Initialization ----------------
    public void init() {
        // Combo setup
        cmbType.getItems().setAll("Damage / Spoiled", "Expired");
        cmbType.setPromptText("Select type");
        cmbType.valueProperty().addListener((o, oldV, newV) -> {
            if (txtReason != null) {
                // keep it always editable; prefill a simple default if empty
                txtReason.setEditable(true);
                if ("Expired".equals(newV)) {
                    if (txtReason.getText() == null || txtReason.getText().isBlank()) {
                        txtReason.setText("Expired batch");
                    }
                } else if ("Damage / Spoiled".equals(newV)) {
                    if (txtReason.getText() == null || txtReason.getText().isBlank()) {
                        txtReason.setText("Damaged / spoiled");
                    }
                }
            }
            updateSaveState();
        });

        // Make the summary label wrap to multiple lines if needed
        lblSummary.setWrapText(true);

        // Table setup
        tbl.setItems(rows);
        setupColumns();
        tbl.setRowFactory(tv -> {
            TableRow<DeductRow> r = new TableRow<>();
            r.setOnMouseClicked(e -> {
                if (!r.isEmpty() && e.getClickCount() == 1) applySelection(r.getItem());
            });
            return r;
        });

        // Search field debounce
        debounce.setOnFinished(e -> search(txtSearch.getText()));
        txtSearch.textProperty().addListener((o, ov, nv) -> {
            if (suppressSearchOnSelection) {
                return; // ignore programmatic changes from selection
            }
            debounce.stop();
            if (nv == null || nv.isBlank()) {
                // clear table and current selection if the search box is empty
                rows.clear();
                pickedBatchProperty.set(null);
                selBatchId = null; selMedicineId = null; selStock = null; selExpiry = null;
                selBaseUnit = null; selTabletsPerBlister = null; selBlistersPerBox = null;
                selMlPerBottle = null; selGramsPerTube = null; selSplitAllowed = null;
                updateSummary();
            } else {
                debounce.playFromStart();
            }
        });

        // Quantity buttons
        btnPlus.setOnAction(e -> changeQty(+1));
        btnMinus.setOnAction(e -> changeQty(-1));

        // Save button
        btnSave.setOnAction(e -> save());

        // Bind reason text
        if (txtReason != null) {
            reasonText.unbind();
            reasonText.bind(txtReason.textProperty());
        }
        // Numeric TextFormatter for txtQty and bind to deductQty property
        qtyFormatter = new TextFormatter<>(
                new IntegerStringConverter(),
                1,
                c -> c.getControlNewText().matches("\\d{0,9}") ? c : null
        );
        txtQty.setTextFormatter(qtyFormatter);
        deductQty.unbind();
        IntegerProperty qtyValue = IntegerProperty.integerProperty(qtyFormatter.valueProperty());
        deductQty.bindBidirectional(qtyValue);
        qtyFormatter.setValue(1);

        final BooleanBinding saveDisabledBinding =
                pickedBatchProperty.isNull()
                        .or(deductQty.lessThanOrEqualTo(0))
                        .or(reasonText.isEmpty());

        // IMPORTANT: اقفل أي binding سابق واعمل binding واحد فقط
        btnSave.disableProperty().unbind();
        btnSave.disableProperty().bind(saveDisabledBinding);
        updateSaveState();
    }

    private void setupColumns() {
        colSerial.setCellValueFactory(cf ->
                new SimpleIntegerProperty(cf.getTableView().getItems().indexOf(cf.getValue()) + 1));
        colSerial.setStyle("-fx-alignment: CENTER;");

        colName.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().medicineName + " (" + cd.getValue().batchNo + ")"));

        colStock.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().stock));
        colStock.setStyle("-fx-alignment: CENTER;");

        colExpiry.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().expiry == null ? "—" : cd.getValue().expiry.toString()));
        colExpiry.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setStyle("");
                if (!empty && item != null && !"—".equals(item)) {
                    try {
                        LocalDate d = LocalDate.parse(item);
                        long days = ChronoUnit.DAYS.between(LocalDate.now(), d);
                        if (days <= 0)
                            setStyle("-fx-text-fill:#b91c1c; -fx-font-weight:bold;");
                        else if (days <= 30)
                            setStyle("-fx-text-fill:#b45309;");
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    // ---------------- Search Logic ----------------
    private void search(String raw) {
        if (raw == null || raw.isBlank()) { rows.clear(); return; }
        String q = (raw == null ? "" : raw.trim().toLowerCase());
        if (q.length() < 2) { rows.clear(); return; }
        Task<ObservableList<DeductRow>> t = new Task<>() {
            @Override
            protected ObservableList<DeductRow> call() throws Exception {
                ObservableList<DeductRow> list = FXCollections.observableArrayList();
                try (Connection c = Database.get();
                     PreparedStatement ps = c.prepareStatement(
                             "WITH bal AS (" +
                                     "  SELECT b.id, b.medicine_id, b.batch_no, b.expiry_date, " +
                                     "         COALESCE(SUM(t.qty_change),0) AS balance " +
                                     "  FROM medicine_batches b " +
                                     "  LEFT JOIN inventory_transactions t ON t.batch_id = b.id " +
                                     "  GROUP BY b.id, b.medicine_id, b.batch_no, b.expiry_date " +
                                     ") " +
                                     "SELECT bal.id, m.id, m.display_name, bal.batch_no, bal.expiry_date, bal.balance, " +
                                     "       m.base_unit::text, m.tablets_per_blister, m.blisters_per_box, " +
                                     "       m.ml_per_bottle, m.grams_per_tube, m.split_allowed " +
                                     "FROM bal JOIN medicines m ON m.id = bal.medicine_id " +
                                     "WHERE (LOWER(m.display_name) LIKE ? OR LOWER(bal.batch_no) LIKE ?) AND bal.balance > 0 " +
                                     "ORDER BY m.display_name ASC, bal.expiry_date ASC LIMIT 100")) {
                    String like = "%" + q + "%";
                    ps.setString(1, like);
                    ps.setString(2, like);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(new DeductRow(
                                    rs.getLong(1), rs.getLong(2),
                                    rs.getString(3), rs.getString(4),
                                    rs.getDate(5) == null ? null : rs.getDate(5).toLocalDate(),
                                    rs.getInt(6),
                                    rs.getString(7),
                                    (Integer) rs.getObject(8),
                                    (Integer) rs.getObject(9),
                                    (Integer) rs.getObject(10),
                                    (Integer) rs.getObject(11),
                                    (Boolean) rs.getObject(12)
                            ));
                        }
                    }
                }
                return list;
            }
        };
        t.setOnSucceeded(ev -> rows.setAll(t.getValue()));
        t.setOnFailed(ev -> System.err.println("Deduct search failed: " + t.getException()));
        new Thread(t, "deduct-search").start();
    }

    // ---------------- UI Logic ----------------
    private void applySelection(DeductRow r) {
        selBatchId = r.batchId;
        selMedicineId = r.medicineId;
        selStock = r.stock;
        selExpiry = r.expiry;
        pickedBatchProperty.set(r);
        // store packaging details
        selBaseUnit = r.baseUnit;
        selTabletsPerBlister = r.tabletsPerBlister;
        selBlistersPerBox = r.blistersPerBox;
        selMlPerBottle = r.mlPerBottle;
        selGramsPerTube = r.gramsPerTube;
        selSplitAllowed = r.splitAllowed;

        // show a friendly label in the search box but don't refetch/clear the table
        suppressSearchOnSelection = true;
        try {
            txtSearch.setText(r.batchNo + " — " + r.medicineName);
        } finally {
            suppressSearchOnSelection = false;
        }
        if (deductQty.get() <= 0) deductQty.set(1);
        updateSummary();
        updateSaveState();
    }


    private void changeQty(int delta) {
        int val = deductQty.get();
        if (val < 0) val = 0;
        val += delta;
        if (val < 1) val = 1;
        if (selStock != null && val > selStock) val = selStock;
        // update property and the text field via formatter to guarantee UI refresh
        deductQty.set(val);
        if (qtyFormatter != null && (qtyFormatter.getValue() == null || !qtyFormatter.getValue().equals(val))) {
            qtyFormatter.setValue(val);
        }
        updateSummary();
        updateSaveState();
    }

    /** Create a human-friendly breakdown of the quantity using packaging info (if available). */
    private String breakdownText(int qty) {
        if (qty <= 0) return "";
        // Tablets/Capsules with blister/box logic
        if (selBaseUnit != null &&
                (selBaseUnit.equalsIgnoreCase("TABLET") || selBaseUnit.equalsIgnoreCase("CAPSULE")) &&
                selTabletsPerBlister != null && selTabletsPerBlister > 0) {
            int perBlister = selTabletsPerBlister;
            int perBox = (selBlistersPerBox != null && selBlistersPerBox > 0) ? (selBlistersPerBox * perBlister) : -1;
            int boxes = 0, blisters = 0, units = qty;
            if (perBox > 0 && units >= perBox) { boxes = units / perBox; units = units % perBox; }
            if (units >= perBlister) { blisters = units / perBlister; units = units % perBlister; }
            StringBuilder sb = new StringBuilder();
            if (boxes   > 0) sb.append(boxes).append(boxes == 1 ? " box"     : " boxes");
            if (blisters> 0) { if (sb.length()>0) sb.append(" + "); sb.append(blisters).append(blisters==1?" blister":" blisters"); }
            if (units   > 0) { if (sb.length()>0) sb.append(" + "); sb.append(units).append(units==1? " unit"   : " units"); }
            return sb.toString();
        }
        // Liquids (SYRUP/SUSPENSION/DROPS/SPRAY) using ml_per_bottle
        if (selMlPerBottle != null && selMlPerBottle > 0) {
            int mlPerBottle = selMlPerBottle;
            // If splitting allowed, interpret qty as milliliters (ml); otherwise as bottles
            if (Boolean.TRUE.equals(selSplitAllowed)) {
                int bottles = qty / mlPerBottle;
                int mls = qty % mlPerBottle;
                StringBuilder sb = new StringBuilder();
                if (bottles > 0) sb.append(bottles).append(bottles==1 ? " bottle" : " bottles");
                if (mls > 0) { if (sb.length()>0) sb.append(" + "); sb.append(mls).append(" mL"); }
                return sb.toString();
            } else {
                return qty + (qty == 1 ? " bottle" : " bottles");
            }
        }
        // Creams/Ointments/Tubes using grams_per_tube
        if (selGramsPerTube != null && selGramsPerTube > 0) {
            int gPerTube = selGramsPerTube;
            if (Boolean.TRUE.equals(selSplitAllowed)) {
                int tubes = qty / gPerTube;
                int grams = qty % gPerTube;
                StringBuilder sb = new StringBuilder();
                if (tubes > 0) sb.append(tubes).append(tubes==1 ? " tube" : " tubes");
                if (grams > 0) { if (sb.length()>0) sb.append(" + "); sb.append(grams).append(" g"); }
                return sb.toString();
            } else {
                return qty + (qty == 1 ? " tube" : " tubes");
            }
        }
        // Fallback: show units only
        return qty + (qty == 1 ? " unit" : " units");
    }

    private void updateSummary() {
        if (pickedBatchProperty.get() == null || selStock == null) {
            lblSummary.setText("");
            return;
        }
        int q = Math.max(0, deductQty.get());
        String breakdown = breakdownText(q);
        lblSummary.setText("Stock: " + selStock + " | Qty: " + q + (breakdown.isEmpty() ? "" : " (" + breakdown + ")"));
    }

    private void updateSaveState() {
        if (btnSave == null) return;
        if (btnSave.disableProperty().isBound()) return; // binding is authoritative
        boolean disabled = pickedBatchProperty.get() == null
                || deductQty.get() <= 0
                || reasonText.get() == null || reasonText.get().trim().isEmpty();
        btnSave.setDisable(disabled);
    }

    // ---------------- Save Logic ----------------
    private void save() {
        if (btnSave.isDisabled()) return;
        if (selBatchId == null || selMedicineId == null) {
            new Alert(Alert.AlertType.WARNING, "Please pick a batch first.").showAndWait();
            return;
        }
        int qty;
        try { qty = Integer.parseInt(txtQty.getText().trim()); } catch (Exception e) { return; }

        String reason = txtReason.getText();
        String type = cmbType.getValue();

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO inventory_transactions " +
                             "(medicine_id, batch_id, qty_change, reason, ref_type) " +
                             "VALUES (?, ?, ?, ?, ?)")) {
            ps.setLong(1, selMedicineId);
            ps.setLong(2, selBatchId);
            ps.setInt(3, -qty);
            ps.setString(4, reason == null || reason.isBlank() ? type : reason);
            ps.setString(5, type);
            ps.executeUpdate();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
            return;
        }

        new Alert(Alert.AlertType.INFORMATION, "Deduction saved.").showAndWait();
        selStock = selStock - qty;
        updateSummary();
        suppressSearchOnSelection = true;
        try {
            // refresh results but keep the table content; reuse the same query
            search(txtSearch.getText());
        } finally {
            suppressSearchOnSelection = false;
        }
        if (onSaveCallback != null) onSaveCallback.run();

        // Clear fields after save
        txtSearch.clear();
        txtQty.setText("1");
        txtReason.clear();
        rows.clear();
        pickedBatchProperty.set(null);
        selBatchId = null;
        selMedicineId = null;
        selStock = null;
        selExpiry = null;
        selBaseUnit = null;
        selTabletsPerBlister = null;
        selBlistersPerBox = null;
        selMlPerBottle = null;
        selGramsPerTube = null;
        selSplitAllowed = null;
        updateSummary();
        updateSaveState();
    }

    public void setOnSaveCallback(Runnable cb) {
        this.onSaveCallback = cb;
    }
}