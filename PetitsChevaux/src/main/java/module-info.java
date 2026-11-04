module fr.utbm.petitschevaux {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    // Base de données des scores : JDBC et le pilote H2 (chargé par DriverManager).
    requires java.sql;
    requires com.h2database;

    // Les contrôleurs sont instanciés et injectés (@FXML) par réflexion par FXMLLoader.
    opens fr.utbm.petitschevaux.controller to javafx.fxml;

    exports fr.utbm.petitschevaux;
    exports fr.utbm.petitschevaux.model;
    exports fr.utbm.petitschevaux.view;
    exports fr.utbm.petitschevaux.controller;
    exports fr.utbm.petitschevaux.network;
    exports fr.utbm.petitschevaux.persistence;
}
