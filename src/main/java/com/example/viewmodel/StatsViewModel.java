package com.example.viewmodel;

import com.example.model.GameModel;
import com.example.model.Player;
import com.example.service.NavigationService;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class StatsViewModel {

    private GameModel gameModel;
    private NavigationService navigationService;

    private final StringProperty player1Name = new SimpleStringProperty("Player 1");
    private final StringProperty player2Name = new SimpleStringProperty("Player 2");
    private final StringProperty player3Name = new SimpleStringProperty("Player 3");
    private final StringProperty player4Name = new SimpleStringProperty("Player 4");

    private final IntegerProperty player1VictoryPoints = new SimpleIntegerProperty(0);
    private final IntegerProperty player2VictoryPoints = new SimpleIntegerProperty(0);
    private final IntegerProperty player3VictoryPoints = new SimpleIntegerProperty(0);
    private final IntegerProperty player4VictoryPoints = new SimpleIntegerProperty(0);

    private final IntegerProperty player1TotalResources = new SimpleIntegerProperty(0);
    private final IntegerProperty player2TotalResources = new SimpleIntegerProperty(0);
    private final IntegerProperty player3TotalResources = new SimpleIntegerProperty(0);
    private final IntegerProperty player4TotalResources = new SimpleIntegerProperty(0);

    private final IntegerProperty player1DevCards = new SimpleIntegerProperty(0);
    private final IntegerProperty player2DevCards = new SimpleIntegerProperty(0);
    private final IntegerProperty player3DevCards = new SimpleIntegerProperty(0);
    private final IntegerProperty player4DevCards = new SimpleIntegerProperty(0);

    private final BooleanProperty player1LongestRoad = new SimpleBooleanProperty(false);
    private final BooleanProperty player2LongestRoad = new SimpleBooleanProperty(false);
    private final BooleanProperty player3LongestRoad = new SimpleBooleanProperty(false);
    private final BooleanProperty player4LongestRoad = new SimpleBooleanProperty(false);

    private final BooleanProperty player1LargestArmy = new SimpleBooleanProperty(false);
    private final BooleanProperty player2LargestArmy = new SimpleBooleanProperty(false);
    private final BooleanProperty player3LargestArmy = new SimpleBooleanProperty(false);
    private final BooleanProperty player4LargestArmy = new SimpleBooleanProperty(false);

    private final BooleanProperty player1Winner = new SimpleBooleanProperty(false);
    private final BooleanProperty player2Winner = new SimpleBooleanProperty(false);
    private final BooleanProperty player3Winner = new SimpleBooleanProperty(false);
    private final BooleanProperty player4Winner = new SimpleBooleanProperty(false);

    private final BooleanProperty player1Present = new SimpleBooleanProperty(true);
    private final BooleanProperty player2Present = new SimpleBooleanProperty(true);
    private final BooleanProperty player3Present = new SimpleBooleanProperty(true);
    private final BooleanProperty player4Present = new SimpleBooleanProperty(true);

// Check this 

    public StatsViewModel(GameModel gameModel, NavigationService navigationService) {
        this.gameModel = gameModel;
        this.navigationService = navigationService;
    }

    // TODO hook game stats here
    public void loadFromGame() {

    // Show placeholder rows 
    if (gameModel == null || gameModel.getPlayers() == null || gameModel.getPlayers().isEmpty()) {
        setPresentPlayers(4);
        updateWinnerFlags();
        return;
    }

    int count = gameModel.getPlayers().size();
    setPresentPlayers(count);

    if (count >= 1) fillFromPlayer(1, gameModel.getPlayers().get(0));
    if (count >= 2) fillFromPlayer(2, gameModel.getPlayers().get(1));
    if (count >= 3) fillFromPlayer(3, gameModel.getPlayers().get(2));
    if (count >= 4) fillFromPlayer(4, gameModel.getPlayers().get(3));

    // TODO hook longest road and largest army

    player1LongestRoad.set(false);
    player2LongestRoad.set(false);
    player3LongestRoad.set(false);
    player4LongestRoad.set(false);

    player1LargestArmy.set(false);
    player2LargestArmy.set(false);
    player3LargestArmy.set(false);
    player4LargestArmy.set(false);

    updateWinnerFlags();
}


    private void fillFromPlayer(int idx, Player p) {
        if (p == null) {
            return;
        }

        if (idx == 1) {
            player1Name.set(p.getName());
            player1TotalResources.set(p.getTotalResources());
            player1DevCards.set(p.numberOfCards());
            player1VictoryPoints.set(calcVictoryPoints(p)); // TODO check this matches your rules
        } else if (idx == 2) {
            player2Name.set(p.getName());
            player2TotalResources.set(p.getTotalResources());
            player2DevCards.set(p.numberOfCards());
            player2VictoryPoints.set(calcVictoryPoints(p));
        } else if (idx == 3) {
            player3Name.set(p.getName());
            player3TotalResources.set(p.getTotalResources());
            player3DevCards.set(p.numberOfCards());
            player3VictoryPoints.set(calcVictoryPoints(p));
        } else if (idx == 4) {
            player4Name.set(p.getName());
            player4TotalResources.set(p.getTotalResources());
            player4DevCards.set(p.numberOfCards());
            player4VictoryPoints.set(calcVictoryPoints(p));
        }
    }

    private int calcVictoryPoints(Player p) {
        // sorry euan, i am not fixing that code, just use the getter
        return p.getTotalVictoryPoints();
    }

    private void setPresentPlayers(int numPlayers) {
        player1Present.set(numPlayers >= 1);
        player2Present.set(numPlayers >= 2);
        player3Present.set(numPlayers >= 3);
        player4Present.set(numPlayers >= 4); // Hide row if 3 players
    }

    private void updateWinnerFlags() {
        player1Winner.set(false);
        player2Winner.set(false);
        player3Winner.set(false);
        player4Winner.set(false);

        int best = Integer.MIN_VALUE;
        int bestIdx = -1;

        if (player1Present.get() && player1VictoryPoints.get() > best) { best = player1VictoryPoints.get(); bestIdx = 1; }
        if (player2Present.get() && player2VictoryPoints.get() > best) { best = player2VictoryPoints.get(); bestIdx = 2; }
        if (player3Present.get() && player3VictoryPoints.get() > best) { best = player3VictoryPoints.get(); bestIdx = 3; }
        if (player4Present.get() && player4VictoryPoints.get() > best) { best = player4VictoryPoints.get(); bestIdx = 4; }

        if (bestIdx == 1) player1Winner.set(true);
        if (bestIdx == 2) player2Winner.set(true);
        if (bestIdx == 3) player3Winner.set(true);
        if (bestIdx == 4) player4Winner.set(true);
    }

    public StringProperty getPlayer1Name() { return player1Name; }
    public StringProperty getPlayer2Name() { return player2Name; }
    public StringProperty getPlayer3Name() { return player3Name; }
    public StringProperty getPlayer4Name() { return player4Name; }

    public IntegerProperty getPlayer1VictoryPoints() { return player1VictoryPoints; }
    public IntegerProperty getPlayer2VictoryPoints() { return player2VictoryPoints; }
    public IntegerProperty getPlayer3VictoryPoints() { return player3VictoryPoints; }
    public IntegerProperty getPlayer4VictoryPoints() { return player4VictoryPoints; }

    public IntegerProperty getPlayer1TotalResources() { return player1TotalResources; }
    public IntegerProperty getPlayer2TotalResources() { return player2TotalResources; }
    public IntegerProperty getPlayer3TotalResources() { return player3TotalResources; }
    public IntegerProperty getPlayer4TotalResources() { return player4TotalResources; }

    public IntegerProperty getPlayer1DevCards() { return player1DevCards; }
    public IntegerProperty getPlayer2DevCards() { return player2DevCards; }
    public IntegerProperty getPlayer3DevCards() { return player3DevCards; }
    public IntegerProperty getPlayer4DevCards() { return player4DevCards; }

    public BooleanProperty getPlayer1LongestRoad() { return player1LongestRoad; }
    public BooleanProperty getPlayer2LongestRoad() { return player2LongestRoad; }
    public BooleanProperty getPlayer3LongestRoad() { return player3LongestRoad; }
    public BooleanProperty getPlayer4LongestRoad() { return player4LongestRoad; }

    public BooleanProperty getPlayer1LargestArmy() { return player1LargestArmy; }
    public BooleanProperty getPlayer2LargestArmy() { return player2LargestArmy; }
    public BooleanProperty getPlayer3LargestArmy() { return player3LargestArmy; }
    public BooleanProperty getPlayer4LargestArmy() { return player4LargestArmy; }

    public BooleanProperty getPlayer1Winner() { return player1Winner; }
    public BooleanProperty getPlayer2Winner() { return player2Winner; }
    public BooleanProperty getPlayer3Winner() { return player3Winner; }
    public BooleanProperty getPlayer4Winner() { return player4Winner; }

    public BooleanProperty getPlayer1Present() { return player1Present; }
    public BooleanProperty getPlayer2Present() { return player2Present; }
    public BooleanProperty getPlayer3Present() { return player3Present; }
    public BooleanProperty getPlayer4Present() { return player4Present; }
}

