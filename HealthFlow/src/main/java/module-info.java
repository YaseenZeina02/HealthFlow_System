module com.example.healthflow {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires com.zaxxer.hikari;
    requires java.sql;
    requires org.postgresql.jdbc;
    requires org.slf4j;
    requires org.slf4j.simple;
    requires jbcrypt;
    requires java.net.http;
    requires java.desktop;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires javafx.base;
    requires org.jetbrains.annotations;
    requires org.controlsfx.controls;
//    requires jdk.internal.md;

    opens com.example.healthflow.model to javafx.base;
    opens com.example.healthflow.controllers to javafx.fxml, javafx.base;

    // Export packages containing public classes
    opens com.example.healthflow to javafx.fxml;
    exports com.example.healthflow;
    exports com.example.healthflow.controllers to javafx.fxml;
}
