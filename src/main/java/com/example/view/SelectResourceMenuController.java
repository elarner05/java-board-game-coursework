package com.example.view;

import com.example.viewmodel.GameViewModel;
import com.example.viewmodel.TurnState;
import com.example.viewmodel.viewstates.PlayerViewState;
import com.example.viewmodel.viewstates.ResourceViewState;
import com.example.model.config.LangManager;
import com.example.model.config.ResourceConfig;
import com.example.viewmodel.viewstates.BankViewState;
import com.example.viewmodel.viewstates.GameUIState;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class SelectResourceMenuController {

    @FXML
    private HBox resourceButtonsBox;
    @FXML
    private Button selectResourceButton;
    @FXML
    private Label tradingMenuLabel;

    private GameViewModel viewModel;
    private ToggleGroup resourceToggleGroup = new ToggleGroup();

    @FXML
    public void initialize() {
        tradingMenuLabel.setText(LangManager.get("tradingMenuLabel"));
        selectResourceButton.setText(LangManager.get("selectResourceButton"));
    }

    public void bind(GameViewModel viewModel) {
        // Clear existing buttons
        this.viewModel = viewModel;
        resourceButtonsBox.getChildren().clear();
        viewModel.turnStateProperty().addListener((obs, old, type) -> {
            if (type == TurnState.MONOPOLY) {
                buildMonoplyUI();
            } else if (type == TurnState.TRADE_FRENZY) {
                buildTradeFrenzyUI();
            } else {
                resourceButtonsBox.getChildren().clear();
            }
        });
        viewModel.bankStateProperty().addListener((obs, oldBank, newBank) -> {
            TurnState type = viewModel.turnStateProperty().get();
            if (type == TurnState.MONOPOLY) {
                buildMonoplyUI();
            } else if (type == TurnState.TRADE_FRENZY) {
                buildTradeFrenzyUI();
            } else {
                resourceButtonsBox.getChildren().clear();
            }
        });

        selectResourceButton.disableProperty().bind(resourceToggleGroup.selectedToggleProperty().isNull());

    }

    private void buildMonoplyUI() {
        ObjectProperty<BankViewState> bank = viewModel.bankStateProperty();
        bank.get().getResources().forEach(resourceViewState -> {
            VBox resourceSelector = createResourceSelector(resourceViewState, resourceToggleGroup,
                    resourceViewState.configProperty().get().maxQuantity, true);
            resourceButtonsBox.getChildren().add(resourceSelector);
        });
    }

    private void buildTradeFrenzyUI() {
        ObjectProperty<BankViewState> bank = viewModel.bankStateProperty();
        bank.get().getResources().forEach(resourceViewState -> {
            VBox resourceSelector = createResourceSelector(resourceViewState, resourceToggleGroup, 3, false);
            resourceButtonsBox.getChildren().add(resourceSelector);
        });
    }

    private VBox createResourceSelector(ResourceViewState resourceViewState, ToggleGroup toggleGroup,
            int requiredAmount, boolean lessThan) {

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
            if (lessThan) {
                radioButton.disableProperty().bind(
                        resourceViewState.countProperty().greaterThanOrEqualTo(requiredAmount));
            } else {
                radioButton.disableProperty().bind(
                        resourceViewState.countProperty().lessThan(requiredAmount));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return root;
    }

    @FXML
    private void handleSelectResourcesButton() {
        Toggle selectedToggle = resourceToggleGroup.getSelectedToggle();
        if (selectedToggle != null) {
            ResourceConfig selectedResource = (ResourceConfig) selectedToggle.getUserData();
            viewModel.onResourceTypeSelected(selectedResource);
        }
        selectedToggle.setSelected(false);
        GameUIState.selectResourceMenuVisible.set(false);
        GameUIState.popupVisible.set(false);

    }
}
