package com.example.view;

import com.example.viewmodel.GameViewModel;
import com.example.viewmodel.viewstates.PlayerViewState;
import com.example.model.config.DevCardConfig;
import com.example.model.config.LangManager;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.collections.ListChangeListener;
import javafx.beans.binding.Bindings;

public class DevCardBoxController {

    private GameViewModel viewModel;
    private IntegerProperty currentDevCardIndex = new SimpleIntegerProperty(0);
    private ListChangeListener<DevCardConfig> devCardListener;
    @FXML
    private StackPane devCardStackPane;
    @FXML
    private Button backButton, nextButton;

    @FXML
    private Label cardNameLabel;
    @FXML
    private Text cardDescriptionLabel;

    public void bindDevCards(GameViewModel viewModel) {
        this.viewModel = viewModel;

        // Listen to changes in the current player
        viewModel.currentPlayerProperty().addListener((obs, oldPlayer, newPlayer) -> {
            // Remove listener from old player
            if (oldPlayer != null && devCardListener != null) {
                oldPlayer.getDevCards().removeListener(devCardListener);
            }

            if (newPlayer != null) {
                // Listen for changes in the dev card list
                devCardListener = change -> {
                    // Ensure index stays in bounds
                    if (!newPlayer.getDevCards().isEmpty()) {
                        if (currentDevCardIndex.get() >= newPlayer.getDevCards().size()) {
                            currentDevCardIndex.set(newPlayer.getDevCards().size() - 1);
                        }
                    } else {
                        currentDevCardIndex.set(0);
                    }
                    updateDevCardDisplay();
                    backButton.disableProperty().bind(currentDevCardIndex.lessThanOrEqualTo(0));
                    nextButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                        PlayerViewState player = viewModel.currentPlayerProperty().get();
                        return player == null || currentDevCardIndex.get() >= player.getDevCards().size() - 1;
                    }, currentDevCardIndex, viewModel.currentPlayerProperty()));
                };
                newPlayer.getDevCards().addListener(devCardListener);
            }

            // Reset index and update UI immediately
            currentDevCardIndex.set(0);
            updateDevCardDisplay();
            backButton.disableProperty().bind(currentDevCardIndex.lessThanOrEqualTo(0));
            nextButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                PlayerViewState player = viewModel.currentPlayerProperty().get();
                return player == null || currentDevCardIndex.get() >= player.getDevCards().size() - 1;
            }, currentDevCardIndex, viewModel.currentPlayerProperty()));
        });

        // Handle case where current player already exists when binding
        PlayerViewState currentPlayer = viewModel.currentPlayerProperty().get();
        if (currentPlayer != null) {
            devCardListener = change -> {
                if (!currentPlayer.getDevCards().isEmpty()) {
                    if (currentDevCardIndex.get() >= currentPlayer.getDevCards().size()) {
                        currentDevCardIndex.set(currentPlayer.getDevCards().size() - 1);
                    }
                } else {
                    currentDevCardIndex.set(0);
                }
                updateDevCardDisplay();
                backButton.disableProperty().bind(currentDevCardIndex.lessThanOrEqualTo(0));
                nextButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                    PlayerViewState player = viewModel.currentPlayerProperty().get();
                    return player == null || currentDevCardIndex.get() >= player.getDevCards().size() - 1;
                }, currentDevCardIndex, viewModel.currentPlayerProperty()));
            };
            currentPlayer.getDevCards().addListener(devCardListener);
            currentDevCardIndex.set(0);
            updateDevCardDisplay();
            backButton.disableProperty().bind(currentDevCardIndex.lessThanOrEqualTo(0));
            nextButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                PlayerViewState player = viewModel.currentPlayerProperty().get();
                return player == null || currentDevCardIndex.get() >= player.getDevCards().size() - 1;
            }, currentDevCardIndex, viewModel.currentPlayerProperty()));
        }

        // Button enable/disable logic
        // Bind button disable state to currentDevCardIndex and list size
        backButton.disableProperty().bind(currentDevCardIndex.lessThanOrEqualTo(0));
        nextButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            PlayerViewState player = viewModel.currentPlayerProperty().get();
            return player == null || currentDevCardIndex.get() >= player.getDevCards().size() - 1;
        }, currentDevCardIndex, viewModel.currentPlayerProperty()));

    }

    // Helper method to enable/disable buttons
    private void updateButtonStates() {
        PlayerViewState player = viewModel.currentPlayerProperty().get();
        if (player == null || player.getDevCards().isEmpty()) {
            backButton.setDisable(true);
            nextButton.setDisable(true);
        } else {
            backButton.setDisable(currentDevCardIndex.get() <= 0);
            nextButton.setDisable(currentDevCardIndex.get() >= player.getDevCards().size() - 1);
        }
    }

    private void updateDevCardDisplay() {
        PlayerViewState currentPlayer = viewModel.currentPlayerProperty().get();
        System.out.println(currentPlayer.getDevCards().size());
        if (currentPlayer.getDevCards().isEmpty()) {
            devCardStackPane.setVisible(false);
            return;
        }
        devCardStackPane.setVisible(true);
        DevCardConfig currentCard = currentPlayer.getDevCards().get(currentDevCardIndex.get());
        cardNameLabel.setText(LangManager.get(currentCard.id + ".name"));
        cardDescriptionLabel.setText(LangManager.get(currentCard.id + ".description"));
    }

    @FXML
    private void showPreviousDevCard() {
        if (currentDevCardIndex.get() > 0) {
            currentDevCardIndex.set(currentDevCardIndex.get() - 1);
            updateDevCardDisplay();
        }
    }

    @FXML
    private void showNextDevCard() {
        PlayerViewState currentPlayer = viewModel.currentPlayerProperty().get();
        if (currentDevCardIndex.get() < currentPlayer.getDevCards().size() - 1) {
            currentDevCardIndex.set(currentDevCardIndex.get() + 1);
            updateDevCardDisplay();
        }
    }

    @FXML
    private void playDevCard() {
        PlayerViewState currentPlayer = viewModel.currentPlayerProperty().get();
        if (currentPlayer == null || currentPlayer.getDevCards().isEmpty()) {
            return;
        }
        DevCardConfig currentCard = currentPlayer.getDevCards().get(currentDevCardIndex.get());
        viewModel.playDevCard(currentCard);
    }
}
