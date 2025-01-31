module com.example.trylma2 {
    requires javafx.controls;
    requires javafx.fxml;

    requires spring.context;
    requires spring.beans;
    requires spring.core;
    requires spring.jdbc;
    requires java.sql;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    //opens com.example.trylma2 to javafx.fxml;
    opens com.example.trylma2 to spring.core, spring.beans, javafx.fxml;
    exports com.example.trylma2;
}

