module main.java.example.wireplan {
    requires javafx.controls;
    requires javafx.fxml;


    opens main.java.example.wireplan to javafx.fxml;
    exports main.java.example.wireplan;
}