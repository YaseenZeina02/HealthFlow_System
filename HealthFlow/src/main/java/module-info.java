module com.example.healthflow {
    requires javafx.controls;
    requires javafx.fxml;
    // افتح حزمة الـ controllers لـ JavaFX FXML
    opens com.example.healthflow.controllers to javafx.fxml;

    // صدّر الحزم التي تحتوي على الكلاسات العامة
    opens com.example.healthflow to javafx.fxml;
    exports com.example.healthflow;
}
