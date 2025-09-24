package com.example.healthflow.ui;

import com.example.healthflow.net.ConnectivityMonitor;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** شريط تحذير يظهر فقط عندما لا يوجد اتصال إنترنت. */
public class ConnectivityBanner extends HBox {

    public ConnectivityBanner(ConnectivityMonitor monitor) {
        Label msg = new Label("⚠️ No internet connection");
        Button retry = new Button("Retry");
        retry.setOnAction(e -> monitor.checkNow());

        setSpacing(10);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #FFE7A3; -fx-border-color: #E2C46E; -fx-border-width: 0 0 1 0;");

        getChildren().addAll(msg, retry);

        // يظهر فقط عند الانقطاع
        visibleProperty().bind(monitor.onlineProperty().not());
        managedProperty().bind(visibleProperty());
    }

    /** مساعد صغير لشدّ عناصر إضافية لليمين إذا احتجت */
    public void appendRight(Node node) {
        getChildren().add(node);
    }
}