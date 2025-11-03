package com.example.healthflow.ui.fx;

import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.TableView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseButton;
import javafx.scene.Cursor;

/**
 * Utilities for safe table updates without losing focus/editing.
 */
public final class TableUtils {
    private TableUtils(){}

    private static final PseudoClass PC_COPY_ACTIVE = PseudoClass.getPseudoClass("copy-active");
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

    /** Context menu: Copy Cell / Row / Column / Table (TSV) */
    public static void installTableCopyContextMenu(TableView<?> table) {
        if (table == null) return;
        ContextMenu cm = new ContextMenu();

        MenuItem miCell = new MenuItem("Copy cell");
        miCell.setOnAction(e -> copyFocusedCell(table));

        MenuItem miRow = new MenuItem("Copy row");
        miRow.setOnAction(e -> copyFocusedRow(table));

        MenuItem miCol = new MenuItem("Copy column");
        miCol.setOnAction(e -> copyFocusedColumn(table));

        MenuItem miAll = new MenuItem("Copy table (TSV)");
        miAll.setOnAction(e -> copyWholeTable(table));

        cm.getItems().addAll(miCell, miRow, miCol, new SeparatorMenuItem(), miAll);
        table.setContextMenu(cm);
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
}