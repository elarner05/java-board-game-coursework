package com.example.viewmodel;

import java.util.ArrayList;

import com.example.model.GameModel;
import com.example.service.NavigationService;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SetupViewModel {

    private GameModel gameModel;
    private NavigationService navigationService;

    private final StringProperty player1Name = new SimpleStringProperty();
    private final StringProperty player2Name = new SimpleStringProperty();
    private final StringProperty player3Name = new SimpleStringProperty();
    private final StringProperty player4Name = new SimpleStringProperty();

    private final IntegerProperty numPlayers = new SimpleIntegerProperty(4);

    public StringProperty getPlayer1Name() {
        return this.player1Name;
    }

    public void setPlayer1Name(String player1Name) {
        this.player1Name.set(player1Name);
    }

    public StringProperty getPlayer2Name() {
        return this.player2Name;
    }

    public void setPlayer2Name(String player2Name) {
        this.player2Name.set(player2Name);
    }

    public StringProperty getPlayer3Name() {
        return this.player3Name;
    }

    public void setPlayer3Name(String player3Name) {
        this.player3Name.set(player3Name);
    }

    public StringProperty getPlayer4Name() {
        return this.player4Name;
    }

    public void setPlayer4Name(String player4Name) {
        this.player4Name.set(player4Name);
    }

    public IntegerProperty getNumPlayers() {
        return this.numPlayers;
    }

    public void setNumPlayers(int numPlayers) {
        this.numPlayers.set(numPlayers);
    }

    public SetupViewModel(GameModel gameModel, NavigationService navigationService) {
        this.gameModel = gameModel;
        this.navigationService = navigationService;
    }

    public void startGame() {
        ArrayList<String> playerNames = new ArrayList<>();
        playerNames.add(player1Name.get());
        playerNames.add(player2Name.get());
        playerNames.add(player3Name.get());
        if (numPlayers.get() == 4) {
            playerNames.add(player4Name.get());
        }
        gameModel.initializePlayers(playerNames);
        
        boolean settlementSetupSuccess = gameModel.initializeBoard();
        if (!settlementSetupSuccess) {System.err.println("Starting settlements/roads did not setup correctly!");} // if this line ever runs, screenshot the board and send to Ethan
        
        GameViewModel gameVM = new GameViewModel(gameModel, navigationService);
        navigationService.navigateTo("gameScreen", gameVM);
    }
}
