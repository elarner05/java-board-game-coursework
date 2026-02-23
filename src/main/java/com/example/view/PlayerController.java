package com.example.view;

import com.example.viewmodel.viewstates.PlayerViewState;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.shape.Polygon;

public class PlayerController {

    @FXML private Label nameLabel;
    @FXML private Label scoreLabel;
    @FXML private Label longestRoadLabel;
    @FXML private Label cleanestEnvironmentLabel;
    @FXML private Polygon colorBox;
    @FXML private Polygon longestRoadBox;
    @FXML private Polygon cleanestEnvironmentBox;
    @FXML private Polygon scoreBox;

    public void bind(PlayerViewState player) {
        nameLabel.textProperty().bind(player.nameProperty());
        scoreLabel.textProperty().bind(player.knownScoreProperty().asString());
        longestRoadLabel.visibleProperty().bind(player.longestRoadProperty());
        cleanestEnvironmentLabel.visibleProperty().bind(player.cleanestEnvironmentProperty());
        colorBox.fillProperty().bind(player.colorProperty());
        longestRoadBox.fillProperty().bind(player.colorProperty());
        cleanestEnvironmentBox.fillProperty().bind(player.colorProperty());
        scoreBox.fillProperty().bind(player.colorProperty());
    }
}
