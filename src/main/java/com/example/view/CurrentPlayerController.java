package com.example.view;

import java.io.IOException;

import com.example.model.config.LangManager;
import com.example.viewmodel.GameViewModel;
import com.example.viewmodel.TurnState;
import com.example.viewmodel.viewstates.GameUIState;
import com.example.viewmodel.viewstates.PlayerViewState;
import com.example.viewmodel.viewstates.ResourceViewState;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class CurrentPlayerController {

    @FXML
    private Rectangle bottomBackground;
    @FXML
    private Label currentPlayerDisplay;
    @FXML
    private Label scoreValue;
    @FXML

    private Label turnHintLabel;
    @FXML

    private VBox resourcesBox, devCardBox;

    @FXML
    private Button buildSettlementButton;
    @FXML
    private Button buildCityButton;
    @FXML
    private Button buildRoadButton;
    @FXML
    private Button buildDevCardButton;
    @FXML
    private Button repairTileButton;
    @FXML
    private Button rollDiceButton;
    @FXML
    private Button buildButton;
    @FXML
    private Button tradeButton;
    @FXML
    private Button endTurnButton;
    @FXML
    private Label currentPlayerScoreLabel;
    @FXML  
    private Label diceLabel;
    @FXML
    private Label resourcesLabel;
    @FXML
    private Label currentPlayerDevCardsLabel;

    // @FXML
    // private Label die1Value;
    // @FXML
    // private Label die2Value;

    private GameViewModel viewModel;

    public void initialize() {
        currentPlayerScoreLabel.setText(LangManager.get("currentPlayerScoreLabel"));
        diceLabel.setText(LangManager.get("diceLabel"));
        resourcesLabel.setText(LangManager.get("resourcesLabel"));
        currentPlayerDevCardsLabel.setText(LangManager.get("currentPlayerDevCardsLabel"));
        endTurnButton.setText(LangManager.get("endTurnButton"));
        tradeButton.setText(LangManager.get("tradeButton"));
        buildButton.setText(LangManager.get("buildButton"));
        rollDiceButton.setText(LangManager.get("rollDiceButton"));
        repairTileButton.setText(LangManager.get("repairTileButton"));
        buildDevCardButton.setText(LangManager.get("buildDevCardButton"));
        buildCityButton.setText(LangManager.get("buildCityButton"));
        buildRoadButton.setText(LangManager.get("buildRoadButton"));
        buildSettlementButton.setText(LangManager.get("buildSettlementButton"));
    }

    public void bindCurrentPlayer(GameViewModel viewModel) {

        // Initialize Static Text Method HERE

        this.viewModel = viewModel;
        turnHintLabel.textProperty().bind(viewModel.turnHintTextProperty());

        ObjectProperty<PlayerViewState> currentPlayer = viewModel.currentPlayerProperty();

        // Name
        currentPlayerDisplay.textProperty().bind(
                Bindings.selectString(currentPlayer, "name"));

        // real score
        scoreValue.textProperty().bind(
                Bindings.selectString(currentPlayer, "realScore"));

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
        populateDevCards();

        // Buttons
        buildSettlementButton.disableProperty().bind(
                Bindings.selectBoolean(currentPlayer, "canBuildSettlement").not());
        buildCityButton.disableProperty().bind(
                Bindings.selectBoolean(currentPlayer, "canBuildCity").not());
        buildRoadButton.disableProperty().bind(
                Bindings.selectBoolean(currentPlayer, "canBuildRoad").not());
        buildDevCardButton.disableProperty().bind(
                Bindings.selectBoolean(currentPlayer, "canBuildDevCard").not());
        repairTileButton.disableProperty().bind(
                Bindings.selectBoolean(currentPlayer, "canRepairTile").not());
        rollDiceButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.DICE_ROLL));
        buildRoadButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.BUILD));
        buildSettlementButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.BUILD));
        buildCityButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.BUILD));
        buildDevCardButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.BUILD));
        buildButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.TRADE));
        endTurnButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.BUILD));
        tradeButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.TRADE));
        repairTileButton.visibleProperty().bind(
                viewModel.turnStateProperty().isEqualTo(TurnState.BUILD));
    }

    private void populateDevCards() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/devCardBox.fxml"));
            Node node = loader.load();

            DevCardBoxController ctrl = loader.getController();
            ctrl.bindDevCards(viewModel);
            devCardBox.getChildren().add(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        for (int i = 0; i < size; i++) {
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

    public void endTurn() {
        viewModel.endTurn();
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
        GameUIState.tradingMenuVisible.set(true);

        System.out.println(viewModel.turnStateProperty().get());
    }

    @FXML
    private void buildDevCard() {
        viewModel.buildDevCard();
    }

    @FXML
    private void repairTile() {
        viewModel.switchToRepairTileState();
    }
}
