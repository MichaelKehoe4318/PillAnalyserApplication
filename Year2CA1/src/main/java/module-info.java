module com.example.year2ca1 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.year2ca1 to javafx.fxml;
    exports com.example.year2ca1;
}