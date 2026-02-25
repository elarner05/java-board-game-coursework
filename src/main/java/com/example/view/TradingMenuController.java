package com.example.view;

import com.example.model.config.LangManager;
import com.example.viewmodel.GameViewModel;
import com.example.viewmodel.viewstates.GameUIState;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.fxml.FXMLLoader;

public class TradingMenuController {
    @FXML
    private StackPane contentPane;

    @FXML
    private Label tradingMenuLabel;
    @FXML
    private Button playerTradeButton;
    @FXML
    private Button portTradeButton;
    @FXML
    private Button bankTradeButton;
    @FXML
    private Button endTradingButton;

    private Node currentContent;
    private Node tradePlayerContent;
    private Node tradeBankContent;
    private Node tradePortContent;

    private GameViewModel viewModel;
    private TradeBankMenuController tradeBankMenuController;
    private TradePlayerMenuController tradePlayerMenuController;
    private TradePortMenuController tradePortMenuController;

    public void initialize() {
        // Load the different content panes for each trading option
        FXMLLoader loader = loadContent("/fxml/tradePlayerMenu.fxml");
        try {
            tradePlayerContent = loader.load();
            tradePlayerMenuController = loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        loader = loadContent("/fxml/tradeBankMenu.fxml");
        try {
            tradeBankContent = loader.load();
            tradeBankMenuController = loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
        }

        loader = loadContent("/fxml/tradePortMenu.fxml");
        try {
            tradePortContent = loader.load();
            tradePortMenuController = loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set the initial content to the player trading menu
        setContent(tradePlayerContent);

        tradingMenuLabel.setText(LangManager.get("tradingMenuLabel"));
        playerTradeButton.setText(LangManager.get("playerTradeButton"));
        portTradeButton.setText(LangManager.get("portTradeButton"));
        bankTradeButton.setText(LangManager.get("bankTradeButton"));
        endTradingButton.setText(LangManager.get("endTradingButton"));
    }

    public void bind(GameViewModel viewModel) {
        this.viewModel = viewModel;
        tradeBankMenuController.bind(viewModel);
        tradePortMenuController.bind(viewModel);
        tradePlayerMenuController.bind(viewModel);
    }

    private FXMLLoader loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            return loader;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setContent(Node content) {
        if (currentContent != null) {
            contentPane.getChildren().remove(currentContent);
        }
        currentContent = content;
        contentPane.getChildren().add(currentContent);
    }

    @FXML
    private void handlePlayersButton() {
        setContent(tradePlayerContent);
    }

    @FXML
    private void handleBankButton() {
        setContent(tradeBankContent);
    }

    @FXML
    private void handlePortsButton() {
        setContent(tradePortContent);
    }

    @FXML
    private void handleEndTradingButton() {
        GameUIState.popupVisible.set(false);
        GameUIState.tradingMenuVisible.set(false);

    }
}