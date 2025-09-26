module com.example.healthflow {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.zaxxer.hikari;
    requires java.sql;
    requires org.postgresql.jdbc;
    requires org.slf4j;
    requires org.slf4j.simple;
    requires jbcrypt;
    requires java.net.http;
    requires java.desktop;

    // Ikonli
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;


    // Open controllers package for JavaFX FXML
    opens com.example.healthflow.controllers to javafx.fxml;

    // Export packages containing public classes
    opens com.example.healthflow to javafx.fxml;
    exports com.example.healthflow;
    exports com.example.healthflow.controllers;
}
