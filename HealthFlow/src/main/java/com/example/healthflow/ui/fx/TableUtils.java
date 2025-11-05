package com.example.healthflow.ui.fx;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TableView;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.function.Function;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseButton;
import javafx.scene.Cursor;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.util.Callback;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Utilities for safe table updates without losing focus/editing.
 */
public final class TableUtils {
    private TableUtils(){}

    private static final PseudoClass PC_COPY_ACTIVE = PseudoClass.getPseudoClass("copy-active");
    private static boolean isClassPresent(String fqcn) {
        try {
            Class.forName(fqcn);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    /** Apply a delta update by key, avoiding setItems(newList). */
    public static <T, K> void applyDelta(ObservableList<T> target, List<T> fresh, Function<T, K> keyExtractor) {
        if (target == null || fresh == null) return;
        if (!javafx.application.Platform.isFxApplicationThread()) {
            javafx.application.Platform.runLater(() -> target.setAll(fresh));
            return;
        }
        target.setAll(fresh);
    }

    /** Remember the current focus owner to restore it later. */
    public static Node rememberFocus(Node anyNodeInScene) {
        if (anyNodeInScene == null || anyNodeInScene.getScene() == null) return null;
        return anyNodeInScene.getScene().getFocusOwner();
    }

    public static void restoreFocus(Node node) {
        if (node != null) node.requestFocus();
    }

    /** Convenience: refresh table without losing selection. */
    public static <T> void safeRefresh(TableView<T> table) {
        if (table == null) return;
        var sel = table.getSelectionModel().getSelectedItem();
        table.refresh();
        if (sel != null) table.getSelectionModel().select(sel);
    }

    // =========================
// Copy helpers (universal)
// =========================
    public static <R> void makeAllStringColumnsCopyable(TableView<R> table) {
        if (table == null) return;
        for (TableColumn<R, ?> c : table.getColumns()) {
            applyCopyableToColumnRec(c);
        }
        enableTableCopyShortcut(table);
        installTableCopyContextMenu(table);
        enhanceSelectionBehavior((TableView<Object>) table);
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private static <R> void applyCopyableToColumnRec(TableColumn<R, ?> col) {
        if (col == null) return;
        if (col.getColumns() != null && !col.getColumns().isEmpty()) {
            for (TableColumn<R, ?> child : col.getColumns()) applyCopyableToColumnRec(child);
            return;
        }
        try {
            TableColumn tc = col;
            if (tc.getTableView() != null && !tc.getTableView().getItems().isEmpty()) {
                Object probe = tc.getCellData(0);
                if (probe instanceof String) {
                    makeCopyable((TableColumn<R, String>) tc);
                }
            }
        } catch (Throwable ignore) {
            // تجاهل الأعمدة غير النصية (مثل أزرار الأكشن)
        }
    }

    /** Install: (1) clear selection when clicking outside any row;
     *  (2) soft hover styling on rows (via style-class). */
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void enhanceSelectionBehavior(TableView<Object> table) {
        if (table == null) return;

        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE); // صف واحد فقط
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getSelectionModel().setCellSelectionEnabled(false);     // صفوف فقط
        if (!table.getStyleClass().contains("hf-select")) {
            table.getStyleClass().add("hf-select");                  // فعّل ستايل التحديد
        }

        String css = TableUtils.class.getResource("/com/example/healthflow/Design/TableUtils.css").toExternalForm();
        if (table.getScene() != null && !table.getScene().getStylesheets().contains(css)) {
            table.getScene().getStylesheets().add(css);
        } else {
            table.sceneProperty().addListener((o, oldScene, newScene) -> {
                if (newScene != null && !newScene.getStylesheets().contains(css)) {
                    newScene.getStylesheets().add(css);
                }
            });
        }

        // Row factory: نعطي كل صف ستايل كلاس للـ hover, ونمسح التحديد لو الضغط كان على صف فارغ
        table.setRowFactory(new Callback<>() {
            @Override
            public TableRow<Object> call(TableView<Object> tv) {
                TableRow<Object> row = new TableRow<>();
                row.getStyleClass().add("hf-row");

                row.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                    if (row.getItem() == _EmptyRowMarker.INSTANCE) {
                        table.getSelectionModel().clearSelection();
                        e.consume();
                        return;
                    }
                    if (!row.isEmpty()) {
                        int idx = row.getIndex();
                        if (!table.getSelectionModel().isSelected(idx)) {
                            table.getSelectionModel().clearSelection();
                            table.getSelectionModel().select(idx);
                        }
                    } else {
                        table.getSelectionModel().clearSelection();
                        e.consume();
                    }
                });

                table.sceneProperty().addListener((o,os,ns)-> {
                    System.out.println("[DEBUG] table style classes: " + table.getStyleClass());
                });

                row.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                    if (!row.isEmpty()) {
                        int idx = row.getIndex();
                        if (!table.getSelectionModel().isSelected(idx)) {
                            table.getSelectionModel().clearSelection();
                            table.getSelectionModel().select(idx);
                        }
                    } else {
                        table.getSelectionModel().clearSelection();
                        e.consume();
                    }
                });

                return row;
            }
        });

        // لما تتغيّر الـ Scene نسجّل فلتر يراقب الضغطات خارج الصفوف/خارج الجدول
        table.sceneProperty().addListener((obs, oldScene, newScene) -> {
            // فك الحارس القديم إن وُجد
            if (oldScene != null) {
                Object h = table.getProperties().remove("hf.clearSelHandler");
                if (h instanceof EventHandler) {
                    oldScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, (EventHandler<MouseEvent>) h);
                }
            }
            if (newScene == null) return;

            EventHandler<MouseEvent> handler = e -> {
                if (e.getButton() != MouseButton.PRIMARY) return; // ما نتدخل بالضغطة اليمين

                Node target = (Node) e.getTarget();
                boolean insideTable = isDescendantOf(target, table);

                if (!insideTable) {
                    // ضغطة خارج الجدول كله
                    table.getSelectionModel().clearSelection();
                    return;
                }

                // ضغطة داخل الجدول: هل هي فوق TableRow فعلي؟
                TableRow<?> row = findAncestor(target, TableRow.class);
                if (row == null || row.isEmpty() || row.getIndex() < 0) {
                    // داخل الجدول لكن خارج أي صف (مساحة فارغة/هيدر/سيرفس)
                    table.getSelectionModel().clearSelection();
                }
                // لو فوق صف غير فارغ, السلوك الافتراضي بينقل التحديد
            };

            newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, handler);
            table.getProperties().put("hf.clearSelHandler", handler);
        });
        if (table.getScene() != null) {
            System.out.println("[DEBUG-now] table already in scene, classes: " + table.getStyleClass());
        } else {
            table.sceneProperty().addListener((o, os, ns) -> {
                if (ns != null)
                    System.out.println("[DEBUG-later] table added to scene, classes: " + table.getStyleClass());
            });
        }
    }
    /** Returns true if node n is a descendant of ancestor (inclusive). */
    private static boolean isDescendantOf(Node n, Node ancestor) {
        for (Node cur = n; cur != null; cur = cur.getParent()) {
            if (cur == ancestor) return true;
        }
        return false;
    }

    /** Walk up the parent chain to find the first ancestor of the requested type. */
    @SuppressWarnings("unchecked")
    private static <T> T findAncestor(Node n, Class<T> type) {
        for (Node cur = n; cur != null; cur = cur.getParent()) {
            if (type.isInstance(cur)) return (T) cur;
        }
        return null;
    }

    /** Render cells as non-editable, selectable TextField with copy menu */
    /** Render cells as non-editable, selectable TextField with copy menu */
    public static <R> void makeCopyable(TableColumn<R, String> column) {
        column.setCellFactory(col -> new TableCell<R, String>() {
            private final TextField tf = new TextField();
            // helper: toggle both pseudo and style class
            private void setActive(boolean active) {
                pseudoClassStateChanged(PC_COPY_ACTIVE, active);
                if (active) {
                    if (!getStyleClass().contains("copyable-active")) getStyleClass().add("copyable-active");
                    if (!tf.getStyleClass().contains("copyable-active")) tf.getStyleClass().add("copyable-active");
                } else {
                    getStyleClass().remove("copyable-active");
                    tf.getStyleClass().remove("copyable-active");
                }
            }

            {
                tf.setEditable(false);
                tf.setFocusTraversable(true);
                tf.setBackground(Background.EMPTY);
                tf.setBorder(null);
                tf.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0 6 0 6; -fx-text-fill: -fx-text-base-color;");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(tf);

                // hook for CSS
                getStyleClass().add("copyable-cell");
                tf.getStyleClass().add("copyable-inner");
        // Cursor + focus handling on the inner TextField
                tf.setCursor(Cursor.TEXT); // خلي المؤشر I-beam دائمًا داخل الحقل
                tf.setOnMouseEntered(e -> tf.setCursor(Cursor.TEXT));
                tf.setOnMouseExited(e -> tf.setCursor(Cursor.TEXT)); // خليه I-beam حتى لما يطلع من الحقل داخل الخلية

        // امنع TableRow من سرقة الحدث وخلي الفوكس داخل الحقل
                tf.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                    if (!isEmpty()) {
                        getTableView().getSelectionModel().select(getIndex());
                        tf.requestFocus(); // إدخال المؤشر داخل الحقل
                    }
                    e.consume(); // مهم: ما تخليش الحدث يطلع للـ Row
                });

                // أثناء السحب لتحديد النص
                tf.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
                    tf.requestFocus(); // ضمن يظل الفوكس داخل الحقل أثناء السحب
                });
                // Hover: I-beam cursor + soft highlight
                setOnMouseEntered(e -> {
                    setCursor(Cursor.TEXT);
                    setActive(true);
                });
                setOnMouseExited(e -> {
                    setCursor(Cursor.DEFAULT);
                    boolean active = isFocused() || isSelected() || tf.isFocused();
                    setActive(active);
                });

                // حافظ على الـ highlight أثناء الفوكس/التحديد
                selectedProperty().addListener((o, ov, nv) -> setActive(nv || isFocused() || tf.isFocused()));
                focusedProperty().addListener((o, ov, nv) -> setActive(nv || isSelected() || tf.isFocused()));
                tf.focusedProperty().addListener((o, ov, nv) -> setActive(nv || isSelected() || isFocused()));

                // Right-click: Copy
                ContextMenu cm = new ContextMenu();
                MenuItem mi = new MenuItem("Copy");
                mi.setOnAction(e -> {
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(tf.getText() == null ? "" : tf.getText());
                    Clipboard.getSystemClipboard().setContent(cc);
                });
                cm.getItems().add(mi);
                MenuItem miSel = new MenuItem("Copy selection");
                miSel.setOnAction(e -> {
                    String s = tf.getSelectedText();
                    if (s == null || s.isEmpty()) s = tf.getText();
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(s == null ? "" : s);
                    Clipboard.getSystemClipboard().setContent(cc);
                });
                cm.getItems().add(miSel);
                tf.setContextMenu(cm);

                // Click: focus only (allow drag-selection); Double-click: select all
                addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                    if (!isEmpty()) {
                        getTableView().getSelectionModel().select(getIndex());
                        tf.requestFocus();
                    }
                });
                addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                    if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() >= 2 && !isEmpty()) {
                        tf.selectAll();
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setActive(false);
                } else {
                    tf.setText(item);
                    setGraphic(tf);
                    setActive(isSelected() || isFocused() || tf.isFocused());
                }
            }
        });
    }
//    public static <T> Callback<TableColumn.CellDataFeatures<T, String>, ObservableValue<String>>
//    nullSafeString(Function<T, StringProperty> getter) {
//        return c -> {
//            T row = c.getValue();
//            return (row == null) ? new ReadOnlyStringWrapper("") : getter.apply(row);
//        };
//    }

    // يجعل كل أعمدة الجدول null-safe مرة واحدة (بما فيها الأعمدة المتداخلة)
    public static <R> void makeColumnsNullSafe(TableView<R> table) {
        if (table == null) return;
        for (TableColumn<R, ?> col : table.getColumns()) {
            wrapColumnNullSafe(col);
        }
    }

    // لفّ الـ valueFactory الحالي (مهما كان نوعه) بحيث لو الصف null يرجّع ObservableValue null
    @SuppressWarnings({"unchecked","rawtypes"})
    private static <R, T> void wrapColumnNullSafe(TableColumn<R, T> col) {
        var original = (Callback<TableColumn.CellDataFeatures<R, T>, ObservableValue<T>>) col.getCellValueFactory();

        col.setCellValueFactory(cdf -> {
            R row = cdf.getValue();
            if (row == null) {
                // خلية لصف تعبئة → ارجع null آمن (الخلايا بتظهر فاضية)
                return new ReadOnlyObjectWrapper<>(null);
            }
            return (original == null) ? new ReadOnlyObjectWrapper<>(null) : original.call(cdf);
        });

        // أعمدة متداخلة
        for (TableColumn<R, ?> child : col.getColumns()) {
            wrapColumnNullSafe((TableColumn) child);
        }
    }


    /** Cmd/Ctrl+C to copy focused cell text */
    public static void enableTableCopyShortcut(TableView<?> table) {
        if (table == null) return;
        // If a TextField/TextArea is currently focused, let it handle Cmd/Ctrl+C natively
        table.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isShortcutDown() && e.getCode() == KeyCode.C) {
                var fo = (table.getScene() == null) ? null : table.getScene().getFocusOwner();
                if (fo instanceof TextInputControl) {
                    // لا تتدخل: خليه ينسخ التحديد الجزئي داخل الخلية
                    return;
                }
            }
        });
        table.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            boolean copyKey = e.isShortcutDown() && e.getCode() == KeyCode.C;
            if (!copyKey) return;

            var fo = (table.getScene() == null) ? null : table.getScene().getFocusOwner();
            if (fo instanceof TextInputControl) {
                // let TextField/TextArea perform the copy of the selected substring
                return;
            }

            var fm = table.getFocusModel();
            if (fm == null) return;
            var pos = fm.getFocusedCell();
            if (pos == null || pos.getTableColumn() == null) return;

            int rowIndex = pos.getRow();
            if (rowIndex < 0 || rowIndex >= table.getItems().size()) return;
            Object rowObj = table.getItems().get(rowIndex);

            @SuppressWarnings("unchecked")
            TableColumn<Object, ?> col = (TableColumn<Object, ?>) pos.getTableColumn();
            var obs = col.getCellObservableValue(rowObj);
            String text = (obs == null || obs.getValue() == null) ? "" : String.valueOf(obs.getValue());

            ClipboardContent cc = new ClipboardContent();
            cc.putString(text);
            Clipboard.getSystemClipboard().setContent(cc);
            e.consume();
        });
    }

    /**
     * Context menu: Copy Cell / Row / Column / Table (TSV) and Export options.
     * Styled with icons and subtle hints.
     */

    public static void installTableCopyContextMenu(TableView<?> table) {
        if (table == null) return;

        ContextMenu cm = new ContextMenu();
        cm.getStyleClass().add("hf-ctx");
        cm.setStyle(
                "-fx-background-color: linear-gradient(to bottom, white, #f7f9fb);" +
                        "-fx-background-insets: 0;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 8;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.20), 18, 0, 0, 6);" +
                        "-fx-border-color: rgba(0,0,0,0.08);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;"
        );

        // ===== Clipboard =====
        cm.getItems().add(sectionHeader("Clipboard"));

        MenuItem miCell = prettyItem("fas-copy", "Copy cell", "");
        miCell.setOnAction(e -> copyFocusedCell(table));

        MenuItem miRow  = prettyItem("fas-align-justify", "Copy row", "");
        miRow.setOnAction(e -> copyFocusedRow(table));

        MenuItem miCol  = prettyItem("fas-columns", "Copy column", "");
        miCol.setOnAction(e -> copyFocusedColumn(table));

        MenuItem miAll  = prettyItem("fas-table", "Copy table (TSV)", "");
        miAll.setOnAction(e -> copyWholeTable(table));

        cm.getItems().addAll(miCell, miRow, miCol, new SeparatorMenuItem(), miAll);

        // خط فاصل خفيف بين القسمين (اختياري)
        cm.getItems().add(new SeparatorMenuItem());

        // ===== Export =====
        cm.getItems().add(sectionHeader("Export"));

        final boolean poiAvailable = isClassPresent("org.apache.poi.xssf.usermodel.XSSFWorkbook");

        MenuItem miExportXlsx = prettyItem("fas-file-export", "Export to Excel (.xlsx)", "⌘E / Ctrl+E");
        miExportXlsx.setDisable(!poiAvailable);
        miExportXlsx.setOnAction(e -> exportTableToExcel(
                table,
                table.getScene() == null ? null : table.getScene().getWindow(),
                true
        ));

        MenuItem miExportCsv  = prettyItem("fas-file-code",
                poiAvailable ? "Export to CSV (.csv)" : "Export (Excel-compatible .csv)", "");
        miExportCsv.setOnAction(e -> exportTableToExcel(
                table,
                table.getScene() == null ? null : table.getScene().getWindow(),
                false
        ));

        cm.getItems().addAll(miExportXlsx, miExportCsv);

        // تنسيق عناصر القائمة فقط (بدون تأثير على عناوين الأقسام)
        cm.setOnShowing(ev -> {
            for (var it : cm.getItems()) {
                if (it instanceof CustomMenuItem cmi && cmi.getContent() instanceof HBox hb) {
                    if (!cmi.getStyleClass().contains("hf-ctx-header")) {
                        hb.setStyle("-fx-background-radius:8; -fx-padding:10 14; -fx-background-insets:0;");
                    }
                }
            }
        });

        // اختصار ⌘E / Ctrl+E للتصدير إلى Excel مباشرة
        table.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isShortcutDown() && e.getCode() == KeyCode.E) {
                Window owner = (table.getScene() == null) ? null : table.getScene().getWindow();
                exportTableToExcel(table, owner, true);
                e.consume();
            }
        });

        table.setContextMenu(cm);
    }

    // Header بلا hover: CustomMenuItem معطّل وشفّاف (يظهر كعنوان فقط)
    private static MenuItem sectionHeader(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size:12px; -fx-font-weight:bold;");

        HBox box = new HBox(lbl);
        box.setPadding(new Insets(8, 14, 8, 14));
        box.setStyle("-fx-background-color:#0e5159; -fx-background-radius:8;");
        box.setMouseTransparent(true); // ما ياخذ فوكس ولا أحداث

        CustomMenuItem cmi = new CustomMenuItem(box, false);
        cmi.getStyleClass().add("hf-ctx-header");
        cmi.setHideOnClick(false);
        cmi.setDisable(true); // مهم: لمنع حالة :armed (ما في hover)
        cmi.setStyle("-fx-background-color: transparent; -fx-opacity: 1;"); // يظل واضح
        cmi.getStyleClass().add("hf-ctx-item");
        return cmi;
    }
    // Helper to build a pretty menu row with icon + text + (optional) right hint
    private static MenuItem prettyItem(String iconLiteral, String text, String hint) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(6, 10, 6, 6));
        FontIcon ic = new FontIcon(iconLiteral); // e.g., "fas-copy"
        ic.setIconSize(14);
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px;");
        HBox spacer = new HBox();
        spacer.setMinWidth(10);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label rhint = new Label(hint == null ? "" : hint);
        rhint.setStyle("-fx-text-fill: #7a869a; -fx-font-size: 11px;");
        row.getChildren().addAll(ic, lbl, spacer, rhint);
        CustomMenuItem cmi = new CustomMenuItem(row, true);
        cmi.getStyleClass().add("hf-ctx-item");
        cmi.setMnemonicParsing(false);
        return cmi;
    }

    private static void copyFocusedCell(TableView<?> table) {
        var fm = table.getFocusModel();
        if (fm == null) return;
        TablePosition<?, ?> pos = fm.getFocusedCell();
        if (pos == null || pos.getTableColumn() == null) return;
        int row = pos.getRow();
        if (row < 0 || row >= table.getItems().size()) return;
        Object rowObj = table.getItems().get(row);
        @SuppressWarnings("unchecked")
        TableColumn<Object, ?> col = (TableColumn<Object, ?>) pos.getTableColumn();
        var obs = col.getCellObservableValue(rowObj);
        String text = (obs == null || obs.getValue() == null) ? "" : String.valueOf(obs.getValue());
        ClipboardContent cc = new ClipboardContent();
        cc.putString(text);
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private static void copyFocusedRow(TableView<?> table) {
        var fm = table.getFocusModel();
        if (fm == null) return;
        int row = fm.getFocusedIndex();
        if (row < 0 || row >= table.getItems().size()) return;
        StringBuilder sb = new StringBuilder();
        for (TableColumn<?, ?> c : table.getVisibleLeafColumns()) {
            Object v = getCellValue(table, row, c);
            if (sb.length() > 0) sb.append('\t');
            sb.append(v == null ? "" : v);
        }
        ClipboardContent cc = new ClipboardContent();
        cc.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private static void copyFocusedColumn(TableView<?> table) {
        var fm = table.getFocusModel();
        if (fm == null) return;
        var pos = fm.getFocusedCell();
        if (pos == null || pos.getTableColumn() == null) return;
        TableColumn<?, ?> col = pos.getTableColumn();
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < table.getItems().size(); r++) {
            Object v = getCellValue(table, r, col);
            if (r > 0) sb.append('\n');
            sb.append(v == null ? "" : v);
        }
        ClipboardContent cc = new ClipboardContent();
        cc.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private static void copyWholeTable(TableView<?> table) {
        StringBuilder sb = new StringBuilder();

        // helper: make any cell value single-line & tab-safe
        java.util.function.Function<Object, String> fmt = v -> {
            if (v == null) return "";
            String s = String.valueOf(v);
            // استبدل أي أسطر جديدة أو Tabs بمسافة حتى يبقى الصف على سطر واحد
            s = s.replace("\r\n", " ")
                    .replace('\n', ' ')
                    .replace('\r', ' ')
                    .replace('\t', ' ')
                    .trim();
            // دمج المسافات المتتابعة (اختياري)
            s = s.replaceAll(" {2,}", " ");
            return s;
        };

        // ===== headers (سطر واحد) =====
        boolean first = true;
        for (TableColumn<?, ?> c : table.getVisibleLeafColumns()) {
            if (!first) sb.append('\t');
            first = false;
            sb.append(fmt.apply(c.getText()));
        }
        sb.append('\n');

        // ===== rows (كل صف على سطر واحد) =====
        for (int r = 0; r < table.getItems().size(); r++) {
            boolean firstCell = true;
            for (TableColumn<?, ?> c : table.getVisibleLeafColumns()) {
                if (!firstCell) sb.append('\t');
                firstCell = false;
                Object v = getCellValue(table, r, c);
                sb.append(fmt.apply(v));
            }
            if (r < table.getItems().size() - 1) sb.append('\n'); // سطر جديد لكل صف فقط
        }

        ClipboardContent cc = new ClipboardContent();
        cc.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private static Object getCellValue(TableView<?> table, int rowIndex, TableColumn<?, ?> column) {
        try {
            Object rowObj = table.getItems().get(rowIndex);
            @SuppressWarnings("unchecked")
            TableColumn<Object, ?> col = (TableColumn<Object, ?>) column;
            var obs = col.getCellObservableValue(rowObj);
            return (obs == null) ? null : obs.getValue();
        } catch (Throwable t) {
            return null;
        }
    }

    /** Export the given TableView to either XLSX (via Apache POI if present) or CSV. */
    private static void exportTableToExcel(TableView<?> table, Window owner, boolean preferXlsx) {
        if (table == null || table.getItems() == null) return;

        String baseName = (table.getId() != null && !table.getId().isBlank())
                ? table.getId()
                : "table";
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());

        boolean poiAvailable = isClassPresent("org.apache.poi.xssf.usermodel.XSSFWorkbook");
        boolean doXlsx = preferXlsx && poiAvailable;

        FileChooser fc = new FileChooser();
        fc.setTitle(doXlsx ? "Save Excel File" : "Save CSV File");
        fc.setInitialFileName(baseName + "_" + timestamp + (doXlsx ? ".xlsx" : ".csv"));
        if (doXlsx) {
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        }
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (Comma-Separated) (*.csv)", "*.csv"));

        File out = fc.showSaveDialog(owner);
        if (out == null) return;

        try {
            if (doXlsx && out.getName().toLowerCase().endsWith(".xlsx")) {
                writeXlsxViaPoiReflection(table, out);
            } else {
                // force CSV if extension is .csv or POI not available
                writeCsv(table, out);
            }
        } catch (Exception ex) {
            // كـ fallback: لو فشل إنشاء XLSX لأي سبب، جرب CSV بنفس الاسم لكن امتداد .csv
            try {
                File alt = out.getName().toLowerCase().endsWith(".csv")
                        ? out
                        : new File(out.getParentFile(), out.getName().replaceAll("\\.xlsx?$", "") + ".csv");
                writeCsv(table, alt);
            } catch (Exception ignored) {
                // بصمت: ما بنظهر Dialog هنا لأن util عام؛ اترك مسؤولية التوست/التنبيه للطبقة المستدعية لو رغبت مستقبلاً
            }
        }
    }

    /** Write CSV with headers, Excel-compatible UTF-8 BOM. Each row on one line. */
    private static void writeCsv(TableView<?> table, File out) throws IOException {
        StringBuilder sb = new StringBuilder();

        // headers
        boolean first = true;
        for (TableColumn<?, ?> c : table.getVisibleLeafColumns()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(escapeCsv(c.getText()));
        }
        sb.append('\n');

        // rows
        for (int r = 0; r < table.getItems().size(); r++) {
            boolean firstCell = true;
            for (TableColumn<?, ?> c : table.getVisibleLeafColumns()) {
                if (!firstCell) sb.append(',');
                firstCell = false;
                Object v = getCellValue(table, r, c);
                sb.append(escapeCsv(v));
            }
            if (r < table.getItems().size() - 1) sb.append('\n');
        }

        // Write with BOM so Excel opens UTF-8 correctly
        try (BufferedWriter w = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
            // UTF-8 BOM
            w.write('\uFEFF');
            w.write(sb.toString());
        }
    }

    /** CSV escaping per RFC4180 */
    private static String escapeCsv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        boolean needQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (needQuotes) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }

    // Optional properties supported:
    // export.receptionist, export.doctor, export.pharmacist  -> explicit names for each role if different from current user
    /**
     * Create an .xlsx via reflection against Apache POI (if present) to avoid hard dependency.
     * Minimal sheet with headers + rows.
     */
    @SuppressWarnings("unchecked")
    private static void writeXlsxViaPoiReflection(TableView<?> table, File out) throws Exception {
        // Classes
        Class<?> wbClz = Class.forName("org.apache.poi.xssf.usermodel.XSSFWorkbook");
        Class<?> sheetClz = Class.forName("org.apache.poi.ss.usermodel.Sheet");
        Class<?> rowClz = Class.forName("org.apache.poi.ss.usermodel.Row");
        Class<?> cellClz = Class.forName("org.apache.poi.ss.usermodel.Cell");
        Class<?> wbIfcClz = Class.forName("org.apache.poi.ss.usermodel.Workbook");
        Class<?> cellStyleClz = Class.forName("org.apache.poi.ss.usermodel.CellStyle");
        Class<?> fontClz = Class.forName("org.apache.poi.ss.usermodel.Font");
        Class<?> indexedColorsClz = Class.forName("org.apache.poi.ss.usermodel.IndexedColors");
        Class<?> borderStyleClz = Class.forName("org.apache.poi.ss.usermodel.BorderStyle");
        Class<?> horizontalAlignClz = Class.forName("org.apache.poi.ss.usermodel.HorizontalAlignment");
        Class<?> fillPatternClz = Class.forName("org.apache.poi.ss.usermodel.FillPatternType");
        Class<?> regionClz = Class.forName("org.apache.poi.ss.util.CellRangeAddress");

        Object wb = wbClz.getConstructor().newInstance();
        Object sheet = wbIfcClz.getMethod("createSheet", String.class).invoke(wb, "Data");
        // default width for readability
        try { sheetClz.getMethod("setDefaultColumnWidth", int.class).invoke(sheet, 20); } catch (Throwable ignore) {}

        int cols = Math.max(1, table.getVisibleLeafColumns().size());

        // ===== Title row (merged & colored) =====
        String baseName = (table.getId() != null && !table.getId().isBlank()) ? table.getId() : "Table";
        String title = String.valueOf(table.getProperties().getOrDefault("export.title", baseName));
        Object titleRow = sheetClz.getMethod("createRow", int.class).invoke(sheet, 0);
        Object titleCell = rowClz.getMethod("createCell", int.class).invoke(titleRow, 0);
        cellClz.getMethod("setCellValue", String.class).invoke(titleCell, title);

        Object titleStyle = wbIfcClz.getMethod("createCellStyle").invoke(wb);
        Object titleFont  = wbIfcClz.getMethod("createFont").invoke(wb);
        fontClz.getMethod("setBold", boolean.class).invoke(titleFont, true);
        try { fontClz.getMethod("setFontHeightInPoints", short.class).invoke(titleFont, (short)16); } catch (Throwable ignore) {}
        // white text on teal fill (more pleasant)
        try {
            Object white = Enum.valueOf((Class<Enum>) indexedColorsClz, "WHITE");
            short whiteIdx = (short) indexedColorsClz.getMethod("getIndex").invoke(white);
            Object teal = Enum.valueOf((Class<Enum>) indexedColorsClz, "TEAL");
            short tealIdx = (short) indexedColorsClz.getMethod("getIndex").invoke(teal);
            fontClz.getMethod("setColor", short.class).invoke(titleFont, whiteIdx);
            cellStyleClz.getMethod("setFillForegroundColor", short.class).invoke(titleStyle, tealIdx);
            Object solid = Enum.valueOf((Class<Enum>) fillPatternClz, "SOLID_FOREGROUND");
            cellStyleClz.getMethod("setFillPattern", fillPatternClz).invoke(titleStyle, solid);
        } catch (Throwable ignore) {}
        cellStyleClz.getMethod("setFont", fontClz).invoke(titleStyle, titleFont);
        try {
            Object center = Enum.valueOf((Class<Enum>) horizontalAlignClz, "CENTER");
            cellStyleClz.getMethod("setAlignment", horizontalAlignClz).invoke(titleStyle, center);
        } catch (Throwable ignore) {}
        try {
            Object thin = Enum.valueOf((Class<Enum>) borderStyleClz, "THIN");
            cellStyleClz.getMethod("setBorderBottom", borderStyleClz).invoke(titleStyle, thin);
        } catch (Throwable ignore) {}
        try {
            Object mergedTitle = regionClz.getConstructor(int.class, int.class, int.class, int.class)
                    .newInstance(0, 0, 0, cols - 1);
            sheetClz.getMethod("addMergedRegion", regionClz).invoke(sheet, mergedTitle);
        } catch (Throwable ignore) {}
        try { cellClz.getMethod("setCellStyle", cellStyleClz).invoke(titleCell, titleStyle); } catch (Throwable ignore) {}

        // ===== Header row (styled) at row 1 =====
        Object headerRow = sheetClz.getMethod("createRow", int.class).invoke(sheet, 1);
        Object headerStyle = wbIfcClz.getMethod("createCellStyle").invoke(wb);
        Object headerFont  = wbIfcClz.getMethod("createFont").invoke(wb);
        fontClz.getMethod("setBold", boolean.class).invoke(headerFont, true);
        cellStyleClz.getMethod("setFont", fontClz).invoke(headerStyle, headerFont);
        try {
            Object lightBlue = Enum.valueOf((Class<Enum>) indexedColorsClz, "LIGHT_CORNFLOWER_BLUE");
            short lightBlueIdx = (short) indexedColorsClz.getMethod("getIndex").invoke(lightBlue);
            cellStyleClz.getMethod("setFillForegroundColor", short.class).invoke(headerStyle, lightBlueIdx);
            Object solid = Enum.valueOf((Class<Enum>) fillPatternClz, "SOLID_FOREGROUND");
            cellStyleClz.getMethod("setFillPattern", fillPatternClz).invoke(headerStyle, solid);
            Object thin = Enum.valueOf((Class<Enum>) borderStyleClz, "THIN");
            cellStyleClz.getMethod("setBorderBottom", borderStyleClz).invoke(headerStyle, thin);
            cellStyleClz.getMethod("setBorderTop",    borderStyleClz).invoke(headerStyle, thin);
            cellStyleClz.getMethod("setBorderLeft",   borderStyleClz).invoke(headerStyle, thin);
            cellStyleClz.getMethod("setBorderRight",  borderStyleClz).invoke(headerStyle, thin);
            Object center = Enum.valueOf((Class<Enum>) horizontalAlignClz, "CENTER");
            cellStyleClz.getMethod("setAlignment", horizontalAlignClz).invoke(headerStyle, center);
        } catch (Throwable ignore) {}

        // Data zebra style (very light blue) for alternate rows
        Object dataAltStyle = wbIfcClz.getMethod("createCellStyle").invoke(wb);
        try {
            Object pale = Enum.valueOf((Class<Enum>) indexedColorsClz, "PALE_BLUE");
            short paleIdx = (short) indexedColorsClz.getMethod("getIndex").invoke(pale);
            cellStyleClz.getMethod("setFillForegroundColor", short.class).invoke(dataAltStyle, paleIdx);
            Object solid = Enum.valueOf((Class<Enum>) fillPatternClz, "SOLID_FOREGROUND");
            cellStyleClz.getMethod("setFillPattern", fillPatternClz).invoke(dataAltStyle, solid);
        } catch (Throwable ignore) {}

        int cidx = 0;
        for (TableColumn<?, ?> c : table.getVisibleLeafColumns()) {
            Object cell = rowClz.getMethod("createCell", int.class).invoke(headerRow, cidx++);
            cellClz.getMethod("setCellValue", String.class).invoke(cell, c.getText() == null ? "" : c.getText());
            try { cellClz.getMethod("setCellStyle", cellStyleClz).invoke(cell, headerStyle); } catch (Throwable ignore) {}
        }

        // ===== Data rows start at row 2 =====
        for (int r = 0; r < table.getItems().size(); r++) {
            Object row = sheetClz.getMethod("createRow", int.class).invoke(sheet, r + 2);
            int x = 0;
            for (TableColumn<?, ?> c : table.getVisibleLeafColumns()) {
                Object cell = rowClz.getMethod("createCell", int.class).invoke(row, x++);
                // Serial column support
                String headerTxt = c.getText() == null ? "" : c.getText().trim();
                Object v;
                if ("#".equals(headerTxt) || "Serial".equalsIgnoreCase(headerTxt) || "S/N".equalsIgnoreCase(headerTxt)) {
                    v = r + 1;
                } else {
                    v = getCellValue(table, r, c);
                }
                if (v == null) {
                    cellClz.getMethod("setCellValue", String.class).invoke(cell, "");
                } else if (v instanceof Number) {
                    try {
                        cellClz.getMethod("setCellValue", double.class).invoke(cell, ((Number) v).doubleValue());
                    } catch (Throwable ignore) {
                        cellClz.getMethod("setCellValue", String.class).invoke(cell, String.valueOf(v));
                    }
                } else {
                    cellClz.getMethod("setCellValue", String.class).invoke(cell, String.valueOf(v));
                }
                // apply alternate row shading
                if ((r % 2) == 1) {
                    try { cellClz.getMethod("setCellStyle", cellStyleClz).invoke(cell, dataAltStyle); } catch (Throwable ignore) {}
                }
            }
        }

        int lastDataRow = table.getItems().size() + 1; // header at row 1

        // Freeze pane under header
        try { sheetClz.getMethod("createFreezePane", int.class, int.class).invoke(sheet, 0, 2); } catch (Throwable ignore) {}
        // Auto filter on header row
        try {
            Object filterRegion = regionClz.getConstructor(int.class, int.class, int.class, int.class)
                    .newInstance(1, 1, 0, cols - 1);
            sheetClz.getMethod("setAutoFilter", regionClz).invoke(sheet, filterRegion);
        } catch (Throwable ignore) {}

        // Auto-size columns
        try {
            for (int i = 0; i < cols; i++) {
                sheetClz.getMethod("autoSizeColumn", int.class).invoke(sheet, i);
            }
        } catch (Throwable ignore) {}

        // ===== Footer: only Date/Time, merged across all columns =====
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String day = java.time.format.DateTimeFormatter.ofPattern("EEEE").format(now).toUpperCase();
        int footerRowIdx = lastDataRow + 2; // one blank row then footer
        Object footerRow = sheetClz.getMethod("createRow", int.class).invoke(sheet, footerRowIdx);
        Object footerCell = rowClz.getMethod("createCell", int.class).invoke(footerRow, 0);
        cellClz.getMethod("setCellValue", String.class).invoke(footerCell, "Generated: " + fmt.format(now) + " (" + day + ")");

        Object footerStyle = wbIfcClz.getMethod("createCellStyle").invoke(wb);
        Object footerFont  = wbIfcClz.getMethod("createFont").invoke(wb);
        try { fontClz.getMethod("setItalic", boolean.class).invoke(footerFont, true); } catch (Throwable ignore) {}
        cellStyleClz.getMethod("setFont", fontClz).invoke(footerStyle, footerFont);
        try {
            Object center = Enum.valueOf((Class<Enum>) horizontalAlignClz, "CENTER");
            cellStyleClz.getMethod("setAlignment", horizontalAlignClz).invoke(footerStyle, center);
            Object ltTurq = Enum.valueOf((Class<Enum>) indexedColorsClz, "LIGHT_TURQUOISE");
            short ltTurqIdx = (short) indexedColorsClz.getMethod("getIndex").invoke(ltTurq);
            cellStyleClz.getMethod("setFillForegroundColor", short.class).invoke(footerStyle, ltTurqIdx);
            Object solid = Enum.valueOf((Class<Enum>) fillPatternClz, "SOLID_FOREGROUND");
            cellStyleClz.getMethod("setFillPattern", fillPatternClz).invoke(footerStyle, solid);
            Object thin = Enum.valueOf((Class<Enum>) borderStyleClz, "THIN");
            cellStyleClz.getMethod("setBorderTop", borderStyleClz).invoke(footerStyle, thin);
        } catch (Throwable ignore) {}
        try { cellClz.getMethod("setCellStyle", cellStyleClz).invoke(footerCell, footerStyle); } catch (Throwable ignore) {}

        try {
            Object footerRegion = regionClz.getConstructor(int.class, int.class, int.class, int.class)
                    .newInstance(footerRowIdx, footerRowIdx, 0, cols - 1);
            sheetClz.getMethod("addMergedRegion", regionClz).invoke(sheet, footerRegion);
        } catch (Throwable ignore) {}

        // write file
        try (java.io.OutputStream os = new java.io.FileOutputStream(out)) {
            wbIfcClz.getMethod("write", java.io.OutputStream.class).invoke(wb, os);
        }
        // close workbook
        try { wbIfcClz.getMethod("close").invoke(wb); } catch (Throwable ignore) {}
    }
    public static void forceIBeamOnCopyableCells(TableView<?> table) {
        if (table == null) return;
        table.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            Node n = e.getPickResult() == null ? null : e.getPickResult().getIntersectedNode();
            boolean inCopy = false;
            for (Node cur = n; cur != null && cur != table; cur = cur.getParent()) {
                var clz = cur.getStyleClass();
                if (clz != null && (clz.contains("copyable-cell") || clz.contains("copyable-inner"))) {
                    inCopy = true; break;
                }
            }
            table.setCursor(inCopy ? Cursor.TEXT : Cursor.DEFAULT);
        });
        table.addEventFilter(MouseEvent.MOUSE_EXITED, e -> table.setCursor(Cursor.DEFAULT));
    }

    // ——— markers & css ———
    private static final class _EmptyRowMarker {
        static final _EmptyRowMarker INSTANCE = new _EmptyRowMarker();
        private _EmptyRowMarker() {}
    }
    private static final String CSS_EMPTY_ROW = "hf-empty-row";

    /** أضِف/أزل صفوف تعبئة إذا كانت القائمة فارغة. استدعِها بعد كل تحميل بيانات. */
    @SuppressWarnings("unchecked")
    public static <T> void ensureBlankRows(TableView<T> table, int minRowsWhenEmpty) {
        if (table == null) return;
        if (minRowsWhenEmpty < 1) minRowsWhenEmpty = 8;

        // 1) ألغِ رسالة الـ placeholder نهائيًا
        table.setPlaceholder(new Pane()); // لا تتحقق من النوع – غيّرها دائمًا

        // 2) اشتغل على المصدر الحقيقي (وليس Sorted/Filtered)
        ObservableList<T> items = table.getItems();
        if (items == null) return;

        // انزل للـ source إن كانت SortedList أو FilteredList
        Object target = items;
        try {
            // SortedList
            if (target instanceof javafx.collections.transformation.SortedList<?> s) {
                target = s.getSource();
            }
            // FilteredList
            if (target instanceof javafx.collections.transformation.FilteredList<?> f) {
                target = f.getSource();
            }
        } catch (Throwable ignore) { }

        if (!(target instanceof ObservableList<?> raw)) return;
        @SuppressWarnings("unchecked")
        ObservableList<T> src = (ObservableList<T>) raw;

        // 3) نظّف أي تعبئة قديمة: nulls أو الماركَر
        src.removeIf(v -> v == null || v == _EmptyRowMarker.INSTANCE);

        // 4) لو فاضي – ضيف صفوف تعبئة
        if (src.isEmpty()) {
            for (int i = 0; i < minRowsWhenEmpty; i++) {
                // استخدام null آمن: الأعمدة بتتعامل معه كـ empty row
                src.add(null);
            }
        }

        // 5) rowFactory: منع التحديد على الصفوف الفارغة/التعبئة
        Callback<TableView<T>, TableRow<T>> oldFactory = (Callback<TableView<T>, TableRow<T>>) table.getRowFactory();
        table.setRowFactory(tv -> {
            TableRow<T> row = (oldFactory != null) ? oldFactory.call(tv) : new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                if (row.isEmpty() || row.getItem() == null || row.getItem() == _EmptyRowMarker.INSTANCE) {
                    tv.getSelectionModel().clearSelection();
                    e.consume();
                }
            });
            return row;
        });
    }

    /** هل هذا الصف صف تعبئة داخلي؟ (مفيد لتجاهله عند النسخ/التصدير إن رغبت) */
    private static boolean isScaffoldRow(Object item) {
        return item == _EmptyRowMarker.INSTANCE;
    }
}