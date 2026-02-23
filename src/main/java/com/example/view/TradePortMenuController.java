package com.example.view;

import java.util.Objects;

import com.example.model.config.LangManager;
import com.example.model.config.PortConfig;
import com.example.model.config.ResourceConfig;
import com.example.viewmodel.GameViewModel;
import com.example.viewmodel.viewstates.BankViewState;
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
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class TradePortMenuController {
    private GameViewModel viewModel;

    @FXML
    private Label portTradeTitleLabel;
    @FXML
    private Button confirmTradeButton;

    @FXML
    private HBox giveResourceBox;
    @FXML
    private HBox receiveResourceBox;
    @FXML
    private HBox selectPortBox;
    private ToggleGroup giveToggleGroup = new ToggleGroup();
    private ToggleGroup receiveToggleGroup = new ToggleGroup();
    private ToggleGroup portToggleGroup = new ToggleGroup();

    public void bind(GameViewModel viewModel) {
        this.viewModel = viewModel;
        viewModel.currentPlayerProperty().addListener((obs, oldPlayer, newPlayer) -> {
            updateResourceBoxes(viewModel);
        });
        viewModel.bankStateProperty().addListener((obs, oldBank, newBank) -> {
            updateResourceBoxes(viewModel);
        });
        updateResourceBoxes(viewModel);

        GameUIState.popupVisible.addListener((obs, oldValue, newValue) -> {
            giveToggleGroup.selectToggle(null);
            receiveToggleGroup.selectToggle(null);
            portToggleGroup.selectToggle(null);
        });

        confirmTradeButton.disableProperty().bind(
                Bindings.or(
                        giveToggleGroup.selectedToggleProperty().isNull(),
                        Bindings.or(
                                receiveToggleGroup.selectedToggleProperty().isNull(),
                                portToggleGroup.selectedToggleProperty().isNull())));
    }

    public void initialize() {
        portTradeTitleLabel.setText(LangManager.get("portTradeTitleLabel"));
        confirmTradeButton.setText(LangManager.get("confirmTradeButton"));

        giveResourceBox.setSpacing(100);
        receiveResourceBox.setSpacing(100);
        selectPortBox.setSpacing(20);   // optional
    }

    private void updateResourceBoxes(GameViewModel viewModel) {
        giveResourceBox.getChildren().clear();
        receiveResourceBox.getChildren().clear();
        selectPortBox.getChildren().clear();
        ObjectProperty<PlayerViewState> currentPlayer = viewModel.currentPlayerProperty();
        currentPlayer.get().getResources().forEach(resourceViewState -> {
            VBox resourceSelector = createGiveResourceSelector(resourceViewState, giveToggleGroup, receiveToggleGroup,
                    portToggleGroup);
            giveResourceBox.getChildren().add(resourceSelector);
        });

        currentPlayer.get().getPorts().forEach(portConfig -> {
            VBox portSelector = createPortSelector(portConfig, portToggleGroup);
            selectPortBox.getChildren().add(portSelector);
        });

        BankViewState bankViewState = viewModel.bankStateProperty().get();
        bankViewState.getResources().forEach(resourceViewState -> {
            VBox resourceSelector = createReceiveResourceSelector(resourceViewState, receiveToggleGroup,
                    giveToggleGroup, portToggleGroup);
            receiveResourceBox.getChildren().add(resourceSelector);
        });

        portToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            giveToggleGroup.selectToggle(null);
            receiveToggleGroup.selectToggle(null);
        });
    }

    private VBox createGiveResourceSelector(ResourceViewState resourceViewState, ToggleGroup toggleGroup,
            ToggleGroup sourceGroup, ToggleGroup portGroup) {

        VBox root = new VBox();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/resourceBox.fxml"));
            Node resourceBox = loader.load();
            ResourceBoxController resourceBoxController = loader.getController();
            resourceBoxController.bind(resourceViewState);
            root.getChildren().add(resourceBox);

            RadioButton radioButton = new RadioButton();
            radioButton.setToggleGroup(toggleGroup);
            radioButton.setUserData(resourceViewState.configProperty().get());
            root.getChildren().add(radioButton);
            radioButton.disableProperty().bind(
                    Bindings.createBooleanBinding(
                            () -> {
                                Toggle selectedPort = portGroup.getSelectedToggle();
                                if (selectedPort == null) {
                                    return true; // disable if no port selected
                                }
                                PortConfig portConfig = (PortConfig) selectedPort.getUserData();
                                boolean notEnough = resourceViewState.countProperty().get() < portConfig.giveQuantity;
                                boolean notRightResource = !resourceViewState.configProperty().get().id
                                        .equals(portConfig.resourceID);
                                boolean wildCardPort = portConfig.resourceID.equals("");
                                return notEnough || (notRightResource && !wildCardPort);
                            },
                            resourceViewState.countProperty(),
                            portGroup.selectedToggleProperty()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return root;
    }

    private VBox createReceiveResourceSelector(ResourceViewState resourceViewState, ToggleGroup toggleGroup,
            ToggleGroup sourceGroup, ToggleGroup portGroup) {

        VBox root = new VBox();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/resourceBox.fxml"));
            Node resourceBox = loader.load();
            ResourceBoxController resourceBoxController = loader.getController();
            resourceBoxController.bind(resourceViewState);
            root.getChildren().add(resourceBox);

            RadioButton radioButton = new RadioButton();
            radioButton.setToggleGroup(toggleGroup);
            radioButton.setUserData(resourceViewState.configProperty().get());
            root.getChildren().add(radioButton);
            radioButton.disableProperty().bind(
                    resourceViewState.countProperty().lessThan(1)
                            .or(
                                    Bindings.createBooleanBinding(
                                            () -> {
                                                Toggle selectedPort = portGroup.getSelectedToggle();
                                                if (selectedPort == null) {
                                                    return true; // disable if no port selected
                                                }
                                                Toggle selected = sourceGroup.getSelectedToggle();
                                                return selected != null
                                                        && Objects.equals(
                                                                selected.getUserData(),
                                                                radioButton.getUserData());
                                            },
                                            resourceViewState.countProperty(),
                                            portGroup.selectedToggleProperty(),
                                            sourceGroup.selectedToggleProperty())));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return root;
    }

    private VBox createPortSelector(PortConfig portConfig, ToggleGroup toggleGroup) {

        VBox root = new VBox();
        Label portLabel = new Label(LangManager.get(portConfig.id + ".name"));
        root.getChildren().add(portLabel);
        RadioButton radioButton = new RadioButton();
        radioButton.setToggleGroup(toggleGroup);
        radioButton.setUserData(portConfig);
        root.getChildren().add(radioButton);

        return root;
    }

    @FXML
    private void handleConfirmTrade() {
        System.out.println("Confirming bank trade...");
        System.out.println("Selected give resource: " + giveToggleGroup.getSelectedToggle().getUserData());
        System.out.println("Selected receive resource: " + receiveToggleGroup.getSelectedToggle().getUserData());
        PortConfig portinUse = (PortConfig) portToggleGroup.getSelectedToggle().getUserData();
        ResourceConfig receiveResource = (ResourceConfig) receiveToggleGroup.getSelectedToggle().getUserData();
        viewModel.setPortTrade(portinUse, receiveResource);

        giveToggleGroup.getSelectedToggle().setSelected(false);
        receiveToggleGroup.getSelectedToggle().setSelected(false);
    }
}
