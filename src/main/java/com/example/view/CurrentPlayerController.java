package com.example.view;

import java.io.IOException;

import com.example.viewmodel.TurnState;
import com.example.viewmodel.viewstates.GameUIState;
import com.example.viewmodel.viewstates.PlayerViewState;
import com.example.viewmodel.viewstates.ResourceViewState;
import com.example.viewmodel.GameViewModel;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public class CurrentPlayerController {

    @FXML
    private Rectangle bottomBackground;
    @FXML
    private Label currentPlayerDisplay;
    @FXML
    private VBox resourcesBox;

    @FXML
    private Button buildSettlementButton;
    @FXML
    private Button buildCityButton;
    @FXML
    private Button buildRoadButton;
    @FXML
    private Button rollDiceButton;
    @FXML
    private Button buildButton;
    @FXML
    private Button tradeButton;
    @FXML
    private Button endTurnButton;
    @FXML
    private Polygon smallTriangle;

    // @FXML
    // private Label die1Value;
    // @FXML
    // private Label die2Value;

    private GameViewModel viewModel;

    public void bindCurrentPlayer(GameViewModel viewModel) {

        //Initialize Static Text Method HERE

        this.viewModel = viewModel;

        ObjectProperty<PlayerViewState> currentPlayer = viewModel.currentPlayerProperty();

        // Name
        currentPlayerDisplay.textProperty().bind(
                Bindings.selectString(currentPlayer, "name"));

        // Background color
        bottomBackground.fillProperty().bind(
                Bindings.select(currentPlayer, "color"));

        // Resources
        // 🔹 RESOURCES 🔹
        currentPlayer.addListener((obs, oldPlayer, newPlayer) -> {
            populateResources(newPlayer);
        });

        // initialize
        populateResources(currentPlayer.get());
        
        // Buttons
        buildSettlementButton.disableProperty().bind(
                Bindings.selectBoolean(currentPlayer, "canBuildSettlement").not());
        buildCityButton.disableProperty().bind(
                Bindings.selectBoolean(currentPlayer, "canBuildCity").not());
        buildRoadButton.disableProperty().bind(
                Bindings.selectBoolean(currentPlayer, "canBuildRoad").not());

        // die1Value.textProperty().bind(
        //         Bindings.selectString(viewModel.diceRollProperty(), "dice1"));
        // die2Value.textProperty().bind(
        //         Bindings.selectString(viewModel.diceRollProperty(), "dice2"));

        rollDiceButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.DICE_ROLL));
        buildRoadButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.BUILD));
        buildSettlementButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.BUILD));
        buildCityButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.BUILD));
        buildButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.TRADE));
        endTurnButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.BUILD));
        tradeButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.TRADE));
    }

    private void populateResources(PlayerViewState player) {
        resourcesBox.getChildren().clear();

        if (player == null) {
            return;
        }

        for (ResourceViewState resource : player.getResources()) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/resourceBox.fxml"));
                Node node = loader.load();

                ResourceBoxController ctrl = loader.getController();
                ctrl.bind(resource);
                node.getStyleClass().add("resource-track-cont");
                node.setStyle("-fx-stroke-width: 4;");
                resourcesBox.getChildren().add(node);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        setResourceIndents();
    }

    private void setResourceIndents() {

        int size = resourcesBox.getChildren().size();
        for(int i = 0; i < size; i++)
        {
            Node row = resourcesBox.getChildren().get(size - i - 1);

            row.setTranslateX(0);
            row.setTranslateX(30 * i);
        }
    }

    public void switchToBUILDSETTLEMENTPHASE() {
        viewModel.switchToBuildSettlementState();
        System.out.println(viewModel.turnStateProperty().get());
    }

    public void switchToBUILDROADPHASE() {
        viewModel.switchToBuildRoadState();
        System.out.println(viewModel.turnStateProperty().get());
    }

    public void switchToBUILDCITYPHASE() {
        viewModel.switchToBuildCityState();
        System.out.println(viewModel.turnStateProperty().get());
    }

    public void switchToROLLDICEPHASE() {
        viewModel.switchToRollDiceState();
        System.out.println(viewModel.turnStateProperty().get());
    }

    public void switchToBUILDINGPHASE() {
        viewModel.switchToBuildState();
        System.out.println(viewModel.turnStateProperty().get());
    }

    public void nextPlayer() {
        viewModel.nextPlayer();

        System.out.println("Next player: " +
            viewModel.getCurrentPlayer().nameProperty().get());
    }

    public void rollDice() {
        viewModel.rollDice();
        System.out.println(viewModel.turnStateProperty().get());
    }

    public void showTradingMenu() {
        GameUIState.popupVisible.set(true);
        System.out.println(viewModel.turnStateProperty().get());
    }
}
