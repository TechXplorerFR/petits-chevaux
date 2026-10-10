module fr.utbm.petitschevaux {
    requires javafx.controls;
    requires javafx.fxml;


    opens fr.utbm.petitschevaux to javafx.fxml;
    exports fr.utbm.petitschevaux;
}