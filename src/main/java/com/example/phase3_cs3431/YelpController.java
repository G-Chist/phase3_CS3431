package com.example.phase3_cs3431;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class YelpController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}