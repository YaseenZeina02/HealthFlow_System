package com.example.healthflow.ui.fx;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TableView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Utilities for safe table updates without losing focus/editing.
 */
public final class TableUtils {
    private TableUtils(){}

    /** Apply a delta update by key, avoiding setItems(newList). */
    public static <T, K> void applyDelta(ObservableList<T> target, List<T> fresh, Function<T, K> keyExtractor) {
        Map<K, T> byKey = new HashMap<>();
        for (T f : fresh) byKey.put(keyExtractor.apply(f), f);

        // update existing
        for (int i = 0; i < target.size(); i++) {
            T t = target.get(i);
            T f = byKey.remove(keyExtractor.apply(t));
            if (f != null) target.set(i, f);
        }
        // add remaining new
        target.addAll(byKey.values());
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
}