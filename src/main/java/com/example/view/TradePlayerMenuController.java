package com.example.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;

import com.example.model.config.LangManager;
import com.example.model.config.PortConfig;
import com.example.model.config.ResourceConfig;
import com.example.viewmodel.GameViewModel;
import com.example.viewmodel.viewstates.BankViewState;
import com.example.viewmodel.viewstates.GameUIState;
import com.example.viewmodel.viewstates.PlayerViewState;
import com.example.viewmodel.viewstates.ResourceViewState;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;

public class TradePlayerMenuController {
    private GameViewModel viewModel;

    @FXML
    private Label playerTradeTitleLabel;
    @FXML
    private Button proposeTradeButton;

    @FXML
    private HBox giveResourceBox;
    @FXML
    private HBox receiveResourceBox;
    @FXML
    private HBox playersAcceptBox;

    private ObservableMap<ResourceConfig, Integer> selectedGiveResources = FXCollections.observableHashMap();
    private ObservableMap<ResourceConfig, Integer> selectedReceiveResources = FXCollections.observableHashMap();
    private BooleanProperty tradeProposed = new SimpleBooleanProperty(false);
    private IntegerProperty selectedPlayerID = new SimpleIntegerProperty(-1);
    private ObservableList<Integer> rejectedPlayerIDs = FXCollections.observableArrayList();
    private ObservableList<Integer> playerAcceptOrderStack = FXCollections.observableArrayList();

    public void bind(GameViewModel viewModel) {
        this.viewModel = viewModel;
        viewModel.currentPlayerProperty().addListener((obs, oldPlayer, newPlayer) -> {
            updateResourceBoxes(viewModel);
        });
        updateResourceBoxes(viewModel);

        GameUIState.popupVisible.addListener((obs, oldValue, newValue) -> {
            if (!newValue) { // popup hidden
                tradeProposed.set(false);
                selectedGiveResources.clear();
                selectedReceiveResources.clear();
                rejectedPlayerIDs.clear();
                playerAcceptOrderStack.clear();
                updateResourceBoxes(viewModel);
            }
        });
    }

    public void initialize() {
        playerTradeTitleLabel.setText(LangManager.get("playerTradeTitleLabel"));
        proposeTradeButton.setText(LangManager.get("proposeTradeButton"));
    }

    private void updateResourceBoxes(GameViewModel viewModel) {
        giveResourceBox.getChildren().clear();
        receiveResourceBox.getChildren().clear();
        playersAcceptBox.getChildren().clear();

        ObjectProperty<PlayerViewState> currentPlayer = viewModel.currentPlayerProperty();
        currentPlayer.get().getResources().forEach(resourceViewState -> {
            VBox resourceSelector = createResourceGiveSelector(resourceViewState);
            giveResourceBox.getChildren().add(resourceSelector);
        });

        currentPlayer.get().getResources().forEach(resourceViewState -> {
            VBox resourceSelector = createResourceReceiveSelector(resourceViewState,
                    viewModel.playersProperty());
            receiveResourceBox.getChildren().add(resourceSelector);
        });

        playerAcceptOrderStack.clear();
        List<PlayerViewState> otherPlayers = viewModel.playersProperty();
        for (PlayerViewState player : otherPlayers) {
            if (player.idProperty().get() != currentPlayer.get().idProperty().get()) {
                playerAcceptOrderStack.add(player.idProperty().get());
                VBox acceptBox = createPlayerAcceptBox(player);
                playersAcceptBox.getChildren().add(acceptBox);
            }
        }
        System.out.println(playerAcceptOrderStack);

    }

    private VBox createResourceGiveSelector(ResourceViewState resourceViewState) {
        IntegerSpinnerValueFactory valueFactory = new IntegerSpinnerValueFactory(0,
                resourceViewState.countProperty().get(), 0);

        VBox selectBox = new VBox();
        selectBox.setStyle("-fx-border-color: black; -fx-background-color: "
                + resourceViewState.configProperty().get().colorHex + "; -fx-padding: 10; -fx-spacing: 10;");
        selectBox.getStyleClass().setAll("trading-resource-box");
        Label nameLabel = new Label(
            LangManager.get(resourceViewState.configProperty().get().id + ".name")
            + "    "
            + resourceViewState.configProperty().get().symbol
        );        nameLabel.getStyleClass().setAll("trading-resource-title");
        Spinner<Integer> spinner = new Spinner<>(valueFactory);
        spinner.setEditable(true);
        spinner.getStyleClass().setAll("spinner", "trading-resource-title");

        // Bind max to the current resource count dynamically
        valueFactory.maxProperty().bind(resourceViewState.countProperty());

        // Optional: clamp value to current max
        resourceViewState.countProperty().addListener((obs, oldVal, newVal) -> {
            if (spinner.getValue() > newVal.intValue()) {
                spinner.getValueFactory().setValue(newVal.intValue());
            }
        });

        spinner.disableProperty().bind(tradeProposed);
        spinner.setUserData(resourceViewState.configProperty().get());
        selectBox.getChildren().addAll(nameLabel, spinner);
        return selectBox;
    }

    private VBox createResourceReceiveSelector(ResourceViewState resourceViewState,
            ObservableList<PlayerViewState> players) {

        VBox selectBox = new VBox();
        selectBox.setStyle("-fx-border-color: black; -fx-background-color: "
                + resourceViewState.configProperty().get().colorHex + "; -fx-padding: 10; -fx-spacing: 10");
        selectBox.getStyleClass().setAll("trading-resource-box");
        Label nameLabel = new Label(
            LangManager.get(resourceViewState.configProperty().get().id + ".name")
            + "    "
            + resourceViewState.configProperty().get().symbol
        );
        nameLabel.getStyleClass().setAll("trading-resource-title");
        int max = players.stream()
                .mapToInt(player -> player.getResources().stream()
                        .filter(r -> r.configProperty().get().equals(
                                resourceViewState.configProperty().get()))
                        .mapToInt(r -> r.countProperty().get())
                        .sum())
                .max()
                .orElse(0);
        Spinner<Integer> spinner = new Spinner<>(0, max, 0);
        spinner.setEditable(true);
        spinner.getStyleClass().setAll("spinner", "trading-resource-title");
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {

            HashMap<ResourceConfig, Integer> selected = getSelectedResources(receiveResourceBox);

            if (!canAnyPlayerProvide(selected, players)) {
                // revert change
                spinner.getValueFactory().setValue(oldValue);
            }
        });
        spinner.disableProperty().bind(tradeProposed);
        spinner.setUserData(resourceViewState.configProperty().get());
        selectBox.getChildren().addAll(nameLabel, spinner);
        return selectBox;
    }

    private HashMap<ResourceConfig, Integer> getSelectedResources(HBox selectionBox) {
        HashMap<ResourceConfig, Integer> selection = new HashMap<>();

        for (var node : selectionBox.getChildren()) {
            if (node instanceof VBox vbox) {
                for (var child : vbox.getChildren()) {
                    if (child instanceof Spinner<?> spinner) {
                        ResourceConfig resource = (ResourceConfig) spinner.getUserData();
                        selection.put(resource, ((Spinner<Integer>) spinner).getValue());
                    }
                }
            }
        }

        System.out.println("Selected resources: " + selection);
        return selection;
    }

    private boolean canAnyPlayerProvide(
            HashMap<ResourceConfig, Integer> required,
            ObservableList<PlayerViewState> players) {

        return players.stream().anyMatch(player -> {

            return required.entrySet().stream().allMatch(entry -> {

                ResourceConfig resource = entry.getKey();
                int requiredAmount = entry.getValue();

                int playerAmount = player.getResources().stream()
                        .filter(r -> r.configProperty().get().equals(resource))
                        .mapToInt(r -> r.countProperty().get())
                        .sum();

                return playerAmount >= requiredAmount;
            });

        });
    }

    private VBox createPlayerAcceptBox(PlayerViewState player) {
        VBox acceptBox = new VBox();
        System.out.println(player.colorProperty().get().toString());
        Color fxColor = player.colorProperty().get();
        String cssColor = String.format(
                "#%02X%02X%02X",
                (int) (fxColor.getRed() * 255),
                (int) (fxColor.getGreen() * 255),
                (int) (fxColor.getBlue() * 255));
        acceptBox.setStyle("-fx-border-color: black; -fx-padding: 15; -fx-spacing: 5; -fx-background-color: " + cssColor + ";");
        Label nameLabel = new Label(player.nameProperty().get());
        Button acceptButton = new Button("✓ - Accept");
        acceptButton.setPrefWidth(80);
        acceptBox.setAlignment(Pos.CENTER);
        acceptButton.setUserData(player.idProperty().get());
        acceptButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> {
                            int playerId = player.idProperty().get();
                            boolean rejected = rejectedPlayerIDs.contains(playerId);
                            int idx = playerAcceptOrderStack.indexOf(playerId);
                            boolean notFirst = (idx != 0);
                            boolean notProposed = !tradeProposed.get();
                            boolean canProvide = canThisPlayerAccept(selectedReceiveResources, player);
                            return rejected || notFirst || notProposed || !canProvide;
                        },
                        tradeProposed,
                        rejectedPlayerIDs,
                        playerAcceptOrderStack,
                        selectedReceiveResources,
                        player.idProperty()));

        acceptButton.setOnAction(e -> {
            selectedPlayerID.set((Integer) acceptButton.getUserData());
            handleConfirmTrade();
        });
        Button rejectButton = new Button("X - Reject");
        rejectButton.setPrefWidth(80);
        rejectButton.setAlignment(Pos.CENTER);
        rejectButton.setUserData(player.idProperty().get());
        rejectButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> {
                            int playerId = player.idProperty().get();
                            boolean rejected = rejectedPlayerIDs.contains(playerId);
                            int idx = playerAcceptOrderStack.indexOf(playerId);
                            boolean notFirst = (idx != 0);
                            boolean notProposed = !tradeProposed.get();
                            return rejected || notFirst || notProposed;
                        },
                        tradeProposed,
                        rejectedPlayerIDs,
                        playerAcceptOrderStack,
                        player.idProperty()));
        rejectButton.setOnAction(e -> {
            int playerId = player.idProperty().get();
            if (!rejectedPlayerIDs.contains(playerId)) {
                rejectedPlayerIDs.add(playerId);
            }
            playerAcceptOrderStack.remove((Integer) playerId);
        });

        acceptBox.getStyleClass().setAll("trading-resource-box");
        acceptButton.getStyleClass().setAll("action-button", "action-button-accept");
        rejectButton.getStyleClass().setAll("action-button", "action-button-reject");


        acceptBox.getChildren().addAll(nameLabel, acceptButton, rejectButton);
        return acceptBox;
    }

    private boolean canThisPlayerAccept(ObservableMap<ResourceConfig, Integer> required, PlayerViewState player) {

        return required.entrySet().stream().allMatch(entry -> {

            ResourceConfig resource = entry.getKey();
            int requiredAmount = entry.getValue();

            int playerAmount = player.getResources().stream()
                    .filter(r -> r.configProperty().get().equals(resource))
                    .mapToInt(r -> r.countProperty().get())
                    .sum();

            return playerAmount >= requiredAmount;
        });

    }

    @FXML
    private void handleProposeTrade() {
        System.out.println("Proposing player trade...");
        System.out.print(playerAcceptOrderStack);

        selectedGiveResources.clear();
        selectedReceiveResources.clear();
        selectedGiveResources.putAll(getSelectedResources(giveResourceBox));
        selectedReceiveResources.putAll(getSelectedResources(receiveResourceBox));
        tradeProposed.set(true);
    }

    private void handleConfirmTrade() {
        System.out.println("Confirming bank trade...");
        viewModel.setPlayerTrade(
                selectedPlayerID.get(),
                new HashMap<>(selectedGiveResources),
                new HashMap<>(selectedReceiveResources));
        tradeProposed.set(false);
        selectedPlayerID.set(-1);
        rejectedPlayerIDs.clear();
        playerAcceptOrderStack.clear();
        updateResourceBoxes(viewModel);
    }
}
