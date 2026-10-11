module fr.utbm.petitschevaux {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;


    opens fr.utbm.petitschevaux to javafx.fxml;
    exports fr.utbm.petitschevaux;
}