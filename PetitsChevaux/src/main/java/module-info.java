module fr.utbm.petitschevaux {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    // Base de données des scores : JDBC et le pilote H2 (chargé par DriverManager).
    requires java.sql;
    requires com.h2database;

    opens fr.utbm.petitschevaux to javafx.fxml;
    exports fr.utbm.petitschevaux;
}
