package com.example.healthflow.ui;

import com.example.healthflow.net.ConnectivityMonitor;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/** شريط تحذير يظهر فقط عندما لا يوجد اتصال إنترنت (بشكل جميل + أنيميشن). */
public class ConnectivityBanner extends HBox {

    private static final Duration FADE_DURATION = Duration.millis(220);

    public ConnectivityBanner(ConnectivityMonitor monitor) {
        // عناصر الواجهة
        Label icon = new Label("⚠");
        Label msg  = new Label("No internet connection");
        Button retry = new Button("Retry");

        // تفعيل إعادة الفحص
        retry.setOnAction(e -> monitor.checkNow());

        // تنسيق عام
        setSpacing(10);
        setPadding(new Insets(8));

        // ====== النمط الداكن الأنيق ======
        setStyle("""
            -fx-background-color: linear-gradient(to right, #2d3436, #636e72);
            -fx-border-color: #222;
            -fx-border-width: 0 0 1 0;
            -fx-alignment: CENTER_LEFT;
        """);

        icon.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 14px;");
        msg.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 13px;");
        retry.setStyle("""
            -fx-background-color: #0984e3;
            -fx-text-fill: white;
            -fx-border-radius: 5;
            -fx-background-radius: 5;
            -fx-font-size: 12px;
            -fx-padding: 4 10 4 10;
        """);

        // فاصل يتمدّد لدفع زر Retry لليمين
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().setAll(icon, msg, spacer, retry);

        // تجاهل أول إشارة من monitor لتجنّب الوميض المؤقّت عند الإقلاع
        final boolean[] firstEmissionHandled = {false};
        monitor.onlineProperty().addListener((obs, wasOnline, isOnline) -> {
            Platform.runLater(() -> {
                if (!firstEmissionHandled[0]) {
                    firstEmissionHandled[0] = true;
                    // لا نعرض شيء مبدئيًا (سنعرض فقط عند الانقطاع التالي)
                    setVisible(false);
                    setManaged(false);
                    setOpacity(0);
                    return;
                }
                if (!isOnline) {
                    showWithFade();
                } else {
                    hideWithFade();
                }
            });
        });

        // الحالة المبدئية
        setVisible(false);
        setManaged(false);
        setOpacity(0);
    }

    /** إظهار البانر بأنيميشن لطيف */
    private void showWithFade() {
        if (isVisible() && getOpacity() >= 1) return;
        setVisible(true);
        setManaged(true);
        FadeTransition ft = new FadeTransition(FADE_DURATION, this);
        ft.setFromValue(Math.max(0, getOpacity()));
        ft.setToValue(1.0);
        ft.play();
    }

    /** إخفاء البانر بأنيميشن لطيف */
    private void hideWithFade() {
        if (!isVisible()) return;
        FadeTransition ft = new FadeTransition(FADE_DURATION, this);
        ft.setFromValue(getOpacity());
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            setVisible(false);
            setManaged(false);
        });
        ft.play();
    }

    /** مساعد صغير لشدّ عناصر إضافية لليمين إذا احتجت */
    public void appendRight(Node node) {
        getChildren().add(node);
    }
}

//package com.example.healthflow.ui;
//
//import com.example.healthflow.net.ConnectivityMonitor;
//import javafx.application.Platform;
//import javafx.geometry.Insets;
//import javafx.scene.Node;
//import javafx.scene.control.Button;
//import javafx.scene.control.Label;
//import javafx.scene.layout.HBox;
//
///** شريط تحذير يظهر فقط عندما لا يوجد اتصال إنترنت. */
//public class ConnectivityBanner extends HBox {
//
//    public ConnectivityBanner(ConnectivityMonitor monitor) {
//        Label msg = new Label("⚠️ No internet connection");
//        Button retry = new Button("Retry");
//        retry.setOnAction(e -> monitor.checkNow());
//
//        setSpacing(10);
//        setPadding(new Insets(8));
////        setStyle("-fx-background-color: #FFE7A3; -fx-border-color: #E2C46E; -fx-border-width: 0 0 1 0;");
////        getChildren().addAll(msg, retry);
//        setStyle("""
//    -fx-background-color: linear-gradient(to right, #ffe0a3, #ffd280);
//    -fx-border-color: #e0b96b;
//    -fx-border-width: 0 0 1 0;
//    -fx-alignment: CENTER_LEFT;
//""");
//
//        Label icon = new Label("\uD83D\uDCF6"); // رمز الواي فاي
//        icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #d17b00;");
//        msg.setStyle("-fx-text-fill: #333; -fx-font-size: 13px;");
//        retry.setStyle("""
//    -fx-background-color: #fff3;
//    -fx-border-color: #dba400;
//    -fx-border-radius: 6;
//    -fx-background-radius: 6;
//    -fx-font-size: 12px;
//""");
//
//        getChildren().setAll(icon, msg, retry);
//
//        // تجاهل أول إشارة من monitor لتجنب الوميض المؤقت عند الإقلاع
//        final boolean[] firstEmissionHandled = {false};
//        monitor.onlineProperty().addListener((obs, wasOnline, isOnline) -> {
//            if (!firstEmissionHandled[0]) {
//                firstEmissionHandled[0] = true;
//                // خفي البانر مبدئيًا (ما يطلع التنبيه المؤقت)
//                Platform.runLater(() -> {
//                    setVisible(false);
//                    setManaged(false);
//                });
//                return;
//            }
//            Platform.runLater(() -> {
//                setVisible(!isOnline);
//                setManaged(!isOnline);
//            });
//        });
//
//        // الحالة المبدئية
//        setVisible(false);
//        setManaged(false);
//    }
//
//    /** مساعد صغير لشدّ عناصر إضافية لليمين إذا احتجت */
//    public void appendRight(Node node) {
//        getChildren().add(node);
//    }
//}