package fr.utbm.petitschevaux;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class gti HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}
