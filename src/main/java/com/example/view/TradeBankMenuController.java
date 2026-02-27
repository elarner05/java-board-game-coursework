package com.example.view;

import java.util.Objects;

import com.example.model.config.LangManager;
import com.example.model.config.ResourceConfig;
import com.example.viewmodel.GameViewModel;
import com.example.viewmodel.viewstates.BankViewState;
import com.example.viewmodel.viewstates.GameUIState;
import com.example.viewmodel.viewstates.PlayerViewState;
import com.example.viewmodel.viewstates.ResourceViewState;

import javafx.scene.Node;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class TradeBankMenuController {

    private GameViewModel viewModel;

    @FXML
    private Label bankTradeTitleLabel;
    @FXML
    private Button confirmTradeButton;

    @FXML
    private HBox giveResourceBox;
    @FXML
    private HBox receiveResourceBox;
    private ToggleGroup giveToggleGroup = new ToggleGroup();
    private ToggleGroup receiveToggleGroup = new ToggleGroup();

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
        });

        confirmTradeButton.disableProperty().bind(
                Bindings.or(
                        giveToggleGroup.selectedToggleProperty().isNull(),
                        receiveToggleGroup.selectedToggleProperty().isNull()));

    }

    public void initialize() {
        bankTradeTitleLabel.setText(LangManager.get("bankTradeTitleLabel"));
        confirmTradeButton.setText(LangManager.get("confirmTradeButton"));

        giveResourceBox.setSpacing(150);
        receiveResourceBox.setSpacing(150);
    }

    private void updateResourceBoxes(GameViewModel viewModel) {
        giveResourceBox.getChildren().clear();
        receiveResourceBox.getChildren().clear();
        

        ObjectProperty<PlayerViewState> currentPlayer = viewModel.currentPlayerProperty();
        currentPlayer.get().getResources().forEach(resourceViewState -> {
            VBox resourceSelector = createGiveResourceSelector(resourceViewState, giveToggleGroup, receiveToggleGroup);
            giveResourceBox.getChildren().add(resourceSelector);
        });

        BankViewState bankViewState = viewModel.bankStateProperty().get();
        bankViewState.getResources().forEach(resourceViewState -> {
            VBox resourceSelector = createReceiveResourceSelector(resourceViewState, receiveToggleGroup,
                    giveToggleGroup);
            receiveResourceBox.getChildren().add(resourceSelector);
        });
    }

    private VBox createGiveResourceSelector(ResourceViewState resourceViewState, ToggleGroup toggleGroup,
            ToggleGroup sourceGroup) {

        return createResourceSelector(resourceViewState, toggleGroup, sourceGroup, 4);
    }

    private VBox createReceiveResourceSelector(ResourceViewState resourceViewState, ToggleGroup toggleGroup,
            ToggleGroup sourceGroup) {

        return createResourceSelector(resourceViewState, toggleGroup, sourceGroup, 1);
    }

    private VBox createResourceSelector(ResourceViewState resourceViewState, ToggleGroup toggleGroup,
            ToggleGroup sourceGroup, int requiredAmount) {

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
            radioButton.getStyleClass().add("radial-radio");
            root.getChildren().add(radioButton);
            radioButton.disableProperty().bind(
                    resourceViewState.countProperty().lessThan(requiredAmount)
                            .or(
                                    Bindings.createBooleanBinding(
                                            () -> {
                                                Toggle selected = sourceGroup.getSelectedToggle();
                                                return selected != null
                                                        && Objects.equals(
                                                                selected.getUserData(),
                                                                radioButton.getUserData());
                                            },
                                            sourceGroup.selectedToggleProperty(),
                                            resourceViewState.countProperty())));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return root;
    }

    @FXML
    private void handleConfirmTrade() {
        System.out.println("Confirming bank trade...");
        System.out.println("Selected give resource: " + giveToggleGroup.getSelectedToggle().getUserData());
        System.out.println("Selected receive resource: " + receiveToggleGroup.getSelectedToggle().getUserData());
        ResourceConfig giveResource = (ResourceConfig) giveToggleGroup.getSelectedToggle().getUserData();
        ResourceConfig receiveResource = (ResourceConfig) receiveToggleGroup.getSelectedToggle().getUserData();
        viewModel.setBankTrade(giveResource, receiveResource);

        giveToggleGroup.getSelectedToggle().setSelected(false);
        receiveToggleGroup.getSelectedToggle().setSelected(false);
    }
}
