package com.example.viewmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.example.model.AdjacencyMaps;
import com.example.model.GameModel;
import com.example.model.Player;
import com.example.model.Road;
import com.example.model.Settlement;
import com.example.model.Tile;
import com.example.model.config.DevCardConfig;
import com.example.model.config.LangManager;
import com.example.model.config.PortConfig;
import com.example.model.config.ResourceConfig;
import com.example.model.trading.TradeBank;
import com.example.model.trading.TradePlayer;
import com.example.model.trading.TradePort;
import com.example.service.NavigationService;
import com.example.viewmodel.viewstates.BankViewState;
import com.example.viewmodel.viewstates.DiceViewState;
import com.example.viewmodel.viewstates.PlayerViewState;
import com.example.viewmodel.viewstates.PortViewState;
import com.example.viewmodel.viewstates.ResourceViewState;
import com.example.viewmodel.viewstates.RoadViewState;
import com.example.viewmodel.viewstates.TileViewState;
import com.example.viewmodel.viewstates.VertexViewState;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

/**
 * ViewModel for the main game screen.
 * Owns game flow and bridges model -> view.
 */
public final class GameViewModel {

    private GameModel gameModel;
    private NavigationService navigationService;

    private final ObservableList<TileViewState> tiles = FXCollections.observableArrayList();
    private ObjectProperty<TurnState> turnState = new SimpleObjectProperty<>(TurnState.DICE_ROLL);
    private final ObservableList<RoadViewState> roads = FXCollections.observableArrayList();
    private final ObservableList<VertexViewState> vertices = FXCollections.observableArrayList();
    private final ObservableList<PlayerViewState> players = FXCollections.observableArrayList(); // All players except
                                                                                                 // current
    private final ObjectProperty<PlayerViewState> currentPlayer = new SimpleObjectProperty<>(); // Current player
    private final StringProperty turnHintText = new SimpleStringProperty();
    private final ObservableList<PortViewState> ports = FXCollections.observableArrayList();

    private final ArrayList<Integer> highwaySelectedRoads = new ArrayList<>();
    private final ArrayList<ResourceConfig> frenzySelectedResources = new ArrayList<>();
    private ResourceConfig monopolySelectedResource = null;

    private final ObjectProperty<DiceViewState> diceRoll = new SimpleObjectProperty<>(new DiceViewState());
    private final ObjectProperty<BankViewState> bankState = new SimpleObjectProperty<>(new BankViewState());
    private final IntegerProperty climateTracker = new SimpleIntegerProperty();

    private TurnState previousState = TurnState.DICE_ROLL;

    public GameViewModel(GameModel gameModel, NavigationService navigationService) {
        this.gameModel = gameModel;
        this.navigationService = navigationService;

        // Initialize TileViewStates
        for (Tile tile : gameModel.getTiles()) {
            TileViewState tileState = new TileViewState();
            tileState.number.set(tile.getNumber());
            tileState.resource.set(tile.getTileID());
            tileState.blocked.set(tile.getIsBlocked());
            tileState.destroyed.set(tile.getIsDestroyed());
            tiles.add(tileState);
        }

        Settlement[] settlements = gameModel.getSettlements();
        for (int i = 0; i < settlements.length; i++) {
            VertexViewState vertexState = new VertexViewState();
            vertexState.owner.set(settlements[i].getPlayerID());
            vertexState.type.set(settlements[i].getSettlementType());
            vertexState.visible.set(isVertexOwned(i));
            vertices.add(vertexState);
        }

        // Initialize PlayerViewStates
        ArrayList<Player> modelPlayers = gameModel.getPlayers();

        Player firstPlayer = modelPlayers.get(0);
        PlayerViewState firstPlayerState = setUpPlayerViewState(firstPlayer);
        currentPlayer.set(firstPlayerState);

        for (int i = 1; i < modelPlayers.size(); i++) {
            PlayerViewState playerState = setUpPlayerViewState(modelPlayers.get(i));
            players.add(playerState);
        }

        // Initialize RoadViewStates
        Road[] modelRoads = gameModel.getRoads();
        int[][] roadConnections = AdjacencyMaps.RoadConnections;
        for (int i = 0; i < roadConnections.length; i++) {
            RoadViewState roadState = new RoadViewState();
            roadState.owner.set(modelRoads[i].getPlayerID());
            roadState.visible.set(isRoadOwned(i)); // owner defaults to -1
            roads.add(roadState);
        }

        turnState.addListener((obs, oldState, newState) -> updateTurnHintText(newState));
        updateTurnHintText(turnState.get());
        bankState.set(setUpBankViewState());
        updateBankViewState(bankState.get());

        updateDiceRoll();
        updatePlayerViewStates();

        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
    }

    private void updateTurnHintText(TurnState state) {
        turnHintText.set(LangManager.get(state.getHintKey()));
    }

    private void updateDiceRoll() {
        diceRoll.get().dice1Property().set(gameModel.getDice1());
        diceRoll.get().dice2Property().set(gameModel.getDice2());
    }

    private PlayerViewState setUpPlayerViewState(Player player) {
        PlayerViewState playerState = new PlayerViewState();
        playerState.idProperty().set(player.getId());
        playerState.nameProperty().set(player.getName());
        playerState.canBuildSettlementProperty().set(gameModel.playerHasSettlementResources(player.getId()));
        playerState.canBuildCityProperty().set(gameModel.playerHasCityResources(player.getId()));
        playerState.canBuildRoadProperty().set(gameModel.playerHasRoadResources(player.getId()));
        playerState.canBuildDevCardProperty().set(gameModel.playerHasDevCardResources(player.getId()));
        playerState.canRepairTileProperty().set(gameModel.playerCanRepairAnyTile(player.getId()));

        playerState.knownScoreProperty().set(player.getKnownVictoryPoints());
        playerState.realScoreProperty().set(player.getTotalVictoryPoints());
        playerState.longestRoadProperty().set(false);
        playerState.cleanestEnvironmentProperty().set(false);
        playerState.colorProperty().set(getPlayerColor(player.getId()));

        initPlayerResources(playerState);
        initPlayerPorts(playerState);
        initPlayerDevCards(playerState);
        return playerState;
    }

    private PlayerViewState updatePlayerViewState(PlayerViewState playerState) {
        Player player = gameModel.getPlayer(playerState.idProperty().get());
        playerState.canBuildSettlementProperty().set(gameModel.playerHasSettlementResources(player.getId()));
        playerState.canBuildCityProperty().set(gameModel.playerHasCityResources(player.getId()));
        playerState.canBuildRoadProperty().set(gameModel.playerHasRoadResources(player.getId()));
        playerState.canBuildDevCardProperty().set(gameModel.playerHasDevCardResources(player.getId()));
        playerState.canRepairTileProperty().set(gameModel.playerCanRepairAnyTile(player.getId()));
        playerState.knownScoreProperty().set(player.getKnownVictoryPoints());
        playerState.realScoreProperty().set(player.getTotalVictoryPoints());
        playerState.longestRoadProperty().set(gameModel.playerHasLongestRoad(player.getId()));
        playerState.cleanestEnvironmentProperty().set(gameModel.playerHasCleanestEnvironment(player.getId()));
        
        updateResourceCounts(playerState);
        updatePlayerPorts(playerState);
        updatePlayerDevCards(playerState);
        return playerState;
    }

    private BankViewState setUpBankViewState() {
        BankViewState bankState = new BankViewState();
        for (ResourceConfig type : gameModel.getBankResources().keySet()) {
            ResourceViewState rvs = new ResourceViewState();
            rvs.configProperty().set(type);
            rvs.countProperty().set(19); // Bank starts with 19 of each resource
            bankState.getResources().add(rvs);
        }
        return bankState;
    }

    private BankViewState updateBankViewState(BankViewState bankState) {
        Map<ResourceConfig, Integer> resources = gameModel.getBankResources();
        for (ResourceViewState rvs : bankState.getResources()) {
            Integer newValue = resources.get(rvs.configProperty().get());
            if (newValue != null) {
                rvs.countProperty().set(newValue);
            }
        }
        return bankState;
    }

    private void initPlayerResources(PlayerViewState playerState) {
        Player player = gameModel.getPlayer(playerState.idProperty().get());

        for (ResourceConfig type : player.getResourcesMap().keySet()) {
            ResourceViewState rvs = new ResourceViewState();
            rvs.configProperty().set(type);
            rvs.countProperty().set(0);
            playerState.getResources().add(rvs);
        }
    }

    private void updateResourceCounts(PlayerViewState playerState) {
        Player player = gameModel.getPlayer(playerState.idProperty().get());
        Map<ResourceConfig, Integer> resources = player.getResourcesMap();

        for (ResourceViewState rvs : playerState.getResources()) {
            Integer newValue = resources.get(rvs.configProperty().get());
            if (newValue != null) {
                rvs.countProperty().set(newValue);
            }
        }
    }

    private void initPlayerPorts(PlayerViewState playerState) {
        int playerID = playerState.idProperty().get();
        ArrayList<PortConfig> portConfigs = gameModel.getPlayerPorts(playerID);
        playerState.getPorts().addAll(portConfigs);
    }

    private void updatePlayerPorts(PlayerViewState playerState) {
        int playerID = playerState.idProperty().get();
        ArrayList<PortConfig> portConfigs = gameModel.getPlayerPorts(playerID);
        playerState.getPorts().setAll(portConfigs);
    }

    private void initPlayerDevCards(PlayerViewState playerState) {
        int playerID = playerState.idProperty().get();
        ArrayList<DevCardConfig> devCards = gameModel.getPlayerDevCards(playerID);
        playerState.getDevCards().addAll(devCards);
    }

    private void updatePlayerDevCards(PlayerViewState playerState) {
        int playerID = playerState.idProperty().get();
        ArrayList<DevCardConfig> devCards = gameModel.getPlayerDevCards(playerID);
        playerState.getDevCards().setAll(devCards);
    }

    private void updateTileViewStates() {
        for (int i = 0; i < tiles.size(); i++) {
            Tile tile = gameModel.getTiles()[i];
            tiles.get(i).number.set(tile.getNumber());
            tiles.get(i).resource.set(tile.getTileID());
            tiles.get(i).blocked.set(tile.getIsBlocked());
            tiles.get(i).destroyed.set(tile.getIsDestroyed());
        }
    }

    public ObservableList<TileViewState> tilesProperty() {
        return tiles;
    }

    public ObservableList<VertexViewState> verticesProperty() {
        return vertices;
    }

    public ObservableList<PlayerViewState> playersProperty() {
        return players;
    }

    public PlayerViewState getCurrentPlayer() {
        return currentPlayer.get();
    }

    public ObjectProperty<TurnState> turnStateProperty() {
        return turnState;
    }

    public StringProperty turnHintTextProperty() {
        return turnHintText;
    }

    public ObservableList<RoadViewState> roadsProperty() {
        return roads;
    }

    public ObservableList<PortViewState> portsProperty() {
        return ports;
    }

    public ObjectProperty<DiceViewState> diceRollProperty() {
        return diceRoll;
    }

    public ObjectProperty<BankViewState> bankStateProperty() {
        return bankState;
    }

    public IntegerProperty climateTrackerProperty() {
        return climateTracker;
    }

    public int[][] getTileVertices() {
        return AdjacencyMaps.TileVertices;
    }

    private void buildSettlement(int vertexIndex) {
        if (turnState.get() != TurnState.BUILD_SETTLEMENT) {
            return;
        }

        boolean success = gameModel.buildSettlement(vertexIndex, getCurrentPlayer().idProperty().get());
        if (success) {
            int playerID = getCurrentPlayer().idProperty().get();
            vertices.get(vertexIndex).owner.set(playerID);
            vertices.get(vertexIndex).type.set(gameModel.getSettlmentType(vertexIndex));
        }
    }

    private void buildCity(int vertexIndex) {
        if (turnState.get() != TurnState.BUILD_CITY) {
            return;
        }

        boolean success = gameModel.buildCity(vertexIndex, getCurrentPlayer().idProperty().get());
        if (success) {
            int playerID = getCurrentPlayer().idProperty().get();
            vertices.get(vertexIndex).type.set(gameModel.getSettlmentType(vertexIndex));
            vertices.get(vertexIndex).owner.set(playerID);
        }
    }

    private void buildRoad(int roadIndex) {
        if (turnState.get() != TurnState.BUILD_ROAD) {
            return;
        }
        boolean success = gameModel.buildRoad(roadIndex, currentPlayer.get().idProperty().get());
        if (success) {
            int playerID = currentPlayer.get().idProperty().get();
            roads.get(roadIndex).owner.set(playerID);

            gameModel.updateLongestRoad();
            this.updatePlayerViewStates(); // update the viewstates, in case the longest road has changed
        }
    }

    public void buildDevCard() {
        boolean success = gameModel.buyDevelopmentCard(currentPlayer.get().idProperty().get());
        if (success) {
            updatePlayerViewState(currentPlayer.get());
            updateBankViewState(bankState.get());
        }
    }

    private void stealResource(int vertexIndex) {
        if (turnState.get() != TurnState.STEAL_RESOURCE) {
            return;
        }
        boolean success = gameModel.stealResource(vertexIndex, currentPlayer.get().idProperty().get());
        if (success) {
            updatePlayerViewState(currentPlayer.get());
        }
    }

    private boolean repairTile(int tileIndex) {
        if (turnState.get() != TurnState.REPAIR_TILE) {
            return false;
        }
        boolean success = gameModel.tileRestore(tileIndex, currentPlayer.get().idProperty().get());
        if (success) {
            gameModel.updateCleanestEnvironment();
            this.updatePlayerViewStates(); // update the viewstates, in case the cleanest environment has changed
            updateTileViewStates();
            return true;
        }
        return false;
    }

    public void onVertexClicked(int vertexIndex) {
        switch (turnState.get()) {
            case BUILD_SETTLEMENT -> {
                buildSettlement(vertexIndex);
                switchToBuildState();
            }
            case BUILD_CITY -> {
                buildCity(vertexIndex);
                switchToBuildState();
            }
            case STEAL_RESOURCE -> {
                stealResource(vertexIndex);
                switchToTradeState();
            }
            default -> {
                // No action
            }
        }
    }

    public void onTileClicked(int vertexIndex) {
        System.out.println("Tile " + vertexIndex + " clicked in state " + turnState.get());
        switch (turnState.get()) {
            case MOVE_ROBBER_STATE -> {
                moveRobber(vertexIndex);
            }
            case REPAIR_TILE -> {
                boolean success = repairTile(vertexIndex);
                if (success) {
                    switchToBuildState();
                }
            }
            case ECO_CONFERENCE -> {
                moveRobber(vertexIndex);
            }
            default -> {
                // No action
            }
        }
    }

    public void onRoadClicked(int roadIndex) {
        switch (turnState.get()) {
            case BUILD_ROAD -> {
                buildRoad(roadIndex);
                switchToBuildState();
            }
            case HIGHWAY_MADNESS -> {
                // collect two road selections, then apply
                if (!highwaySelectedRoads.contains(roadIndex)) {
                    highwaySelectedRoads.add(roadIndex);
                }
                if (highwaySelectedRoads.size() == 2) {
                    int pId = currentPlayer.get().idProperty().get();
                    boolean success = gameModel.applyHighwayMadness(pId, highwaySelectedRoads.get(0),
                            highwaySelectedRoads.get(1));
                    if (success) {
                        int playerID = pId;
                        for (int idx : highwaySelectedRoads) {
                            roads.get(idx).owner.set(playerID);
                            roads.get(idx).visible.set(true);
                        }
                        gameModel.updateLongestRoad();
                        this.updatePlayerViewStates();
                    }
                    highwaySelectedRoads.clear();
                    switchToPreviousState();
                }
            }
            default -> {
                // No action
            }
        }
    }

    public void onResourceTypeSelected(ResourceConfig resource) {
        switch (turnState.get()) {
            case TRADE_FRENZY -> {
                frenzySelectedResources.add(resource);
                if (frenzySelectedResources.size() == 3) {
                    int pId = currentPlayer.get().idProperty().get();
                    boolean success = gameModel.applyTradingFrenzy(pId, new ArrayList<>(frenzySelectedResources));
                    if (success) {
                        updatePlayerViewState(getCurrentPlayer());
                        for (int i = 0; i < players.size(); i++) {
                            updatePlayerViewState(players.get(i));
                        }
                    }
                    frenzySelectedResources.clear();
                    switchToPreviousState();
                }
            }
            case MONOPOLY -> {
                monopolySelectedResource = resource;
                int pId = currentPlayer.get().idProperty().get();
                boolean success = gameModel.applyMonopoly(pId, monopolySelectedResource);
                if (success) {
                    updatePlayerViewState(getCurrentPlayer());
                    for (int i = 0; i < players.size(); i++) {
                        updatePlayerViewState(players.get(i));
                    }
                }
                monopolySelectedResource = null;
                switchToPreviousState();
            }
            default -> {
                // ignore
            }
        }
    }

    public void playDevCard(DevCardConfig devCardConfig) {
        int playerId = getCurrentPlayer().idProperty().get();
        boolean success = gameModel.playDevCard(playerId, devCardConfig);
        if (!success) {
            return; // card play failed (card not found, not in hand, etc.)
        }

        // Get the action type and switch to the appropriate state
        if (devCardConfig == null)
            return;

        String action = devCardConfig.actionType == null ? "" : devCardConfig.actionType;
        switch (action) {
            case "ECO_CONFERENCE" -> switchToEcoConferenceState();
            case "HIGHWAY_MADNESS" -> switchToHighwayMadnessState();
            case "TRADING_FRENZY" -> switchToTradeFrenzyState();
            case "MONOPOLY" -> switchToMonopolyState();
            default -> {
                // unknown card type or victory point
            }
        }
    }

    private boolean canCurrentPlayerBuildSettlement(int i) {

        return gameModel.settlementValid(i, getCurrentPlayer().idProperty().get());
    }

    private boolean canCurrentPlayerBuildRoad(int i) {
        return gameModel.roadValid(i, getCurrentPlayer().idProperty().get());
    }

    private boolean canCurrentPlayerBuildCity(int i) {
        return gameModel.cityValid(i, getCurrentPlayer().idProperty().get());
    }

    private boolean canCurrentPlayerSteal(int i) {
        return gameModel.stealValid(i, getCurrentPlayer().idProperty().get());
    }

    private boolean isVertexOwned(int i) {
        return gameModel.getSettlements()[i].getPlayerID() != -1;
    }

    private boolean isRoadOwned(int i) {
        return gameModel.getRoads()[i].getPlayerID() != -1;
    }

    private boolean isGameOver() {
        return gameModel.checkIfGameOver();
    }

    private int getIndexOfPlayerWithID(int playerID) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).idProperty().get() == playerID) {
                return i;
            }
        }
        return -1; // Not found
    }

    private void setDefaultVisibility() {
        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).visible.set(isVertexOwned(i));
        }
        for (int i = 0; i < roads.size(); i++) {
            roads.get(i).visible.set(isRoadOwned(i));
        }
    }

    private void updatePlayerViewStates() {
        updatePlayerViewState(currentPlayer.get());
        for (int i = 0; i < players.size(); i++) {
            updatePlayerViewState(players.get(i));
        }
    }

    public void nextPlayer() {
        int nextPlayerID = gameModel.nextPlayer(getCurrentPlayer().idProperty().get());
        players.add(getCurrentPlayer());
        int nextPlayerIndex = getIndexOfPlayerWithID(nextPlayerID);
        currentPlayer.set(players.remove(nextPlayerIndex));

        // example rule
        switchToRollDiceState();
    }

    public void switchToRollDiceState() {
        turnState.set(TurnState.DICE_ROLL);
        setDefaultVisibility();
        updatePlayerViewStates();
        updateBankViewState(bankState.get());
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();

    }

    public void rollDice() {
        if (turnState.get() != TurnState.DICE_ROLL) {
            return;
        }
        gameModel.rollDice();
        updateDiceRoll();

        if (diceRoll.get().dice1Property().get() + diceRoll.get().dice2Property().get() == 7) {
            switchToMoveRobberState();
            return;
        }
        switchToTradeState();
        
    }

    public void switchToTradeState() {
        turnState.set(TurnState.TRADE);
        setDefaultVisibility();
        updatePlayerViewStates();
        updateBankViewState(bankState.get());
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();
    }

    public void switchToBuildState() {
        turnState.set(TurnState.BUILD);
        setDefaultVisibility();
        updatePlayerViewStates();
        updateBankViewState(bankState.get());
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();
    }

    public void switchToBuildSettlementState() {
        if (!gameModel.playerHasSettlementResources(getCurrentPlayer().idProperty().get())) {
            return;
        }
        turnState.set(TurnState.BUILD_SETTLEMENT);
        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).visible.set(canCurrentPlayerBuildSettlement(i));
        }
        for (int i = 0; i < roads.size(); i++) {
            roads.get(i).visible.set(isRoadOwned(i));
        }
        updatePlayerViewStates();
        updateBankViewState(bankState.get());
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();

    }

    public void switchToBuildRoadState() {
        turnState.set(TurnState.BUILD_ROAD);
        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).visible.set(isVertexOwned(i));
        }
        for (int i = 0; i < roads.size(); i++) {
            roads.get(i).visible.set(canCurrentPlayerBuildRoad(i));
        }
        updatePlayerViewStates();
        updateBankViewState(bankState.get());
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();
    }

    public void switchToBuildCityState() {
        if (!gameModel.playerHasCityResources(getCurrentPlayer().idProperty().get())) {
            return;
        }
        turnState.set(TurnState.BUILD_CITY);
        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).visible.set(canCurrentPlayerBuildCity(i));
        }
        for (int i = 0; i < roads.size(); i++) {
            roads.get(i).visible.set(isRoadOwned(i));
        }
        updatePlayerViewStates();
        updateBankViewState(bankState.get());
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();
    }

    public void switchToMoveRobberState() {
        turnState.set(TurnState.MOVE_ROBBER_STATE);
        setDefaultVisibility();
        updatePlayerViewStates();
        updateBankViewState(bankState.get());
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();

    }

    public void switchToStealResourceState() {
        turnState.set(TurnState.STEAL_RESOURCE);
        boolean canSteal = false;
        for (int i = 0; i < vertices.size(); i++) {
            boolean currentCanSteal = canCurrentPlayerSteal(i);
            vertices.get(i).visible.set(currentCanSteal);
            canSteal |= currentCanSteal;
        }
        for (int i = 0; i < roads.size(); i++) {
            roads.get(i).visible.set(false);
        }
        updatePlayerViewStates();
        updateBankViewState(bankState.get());

        if (!canSteal) {
            switchToTradeState();
        }
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();
    }

    // do these need more happening in them?
    public void switchToEcoConferenceState() {
        previousState = turnState.get();
        turnState.set(TurnState.ECO_CONFERENCE);
        updatePlayerViewStates();
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();

    }

    public void switchToHighwayMadnessState() {
        previousState = turnState.get();
        turnState.set(TurnState.HIGHWAY_MADNESS);
        for (int i = 0; i < roads.size(); i++) {
            roads.get(i).visible.set(canCurrentPlayerBuildRoad(i));
        }
        updatePlayerViewStates();
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();
    }

    public void switchToTradeFrenzyState() {
        previousState = turnState.get();
        turnState.set(TurnState.TRADE_FRENZY);
        updatePlayerViewStates();
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();
    }

    public void switchToMonopolyState() {
        previousState = turnState.get();
        turnState.set(TurnState.MONOPOLY);
        updatePlayerViewStates();
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();
    }

    public void switchToPlayDevCardState() {
        turnState.set(TurnState.PLAY_DEV_CARD);
        updateTileViewStates();

    }

    public void switchToRepairTileState() {
        turnState.set(TurnState.REPAIR_TILE);
        updateTileViewStates();
        updatePlayerViewStates();
        updateBankViewState(bankState.get());
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        System.out.println("Switched to repair tile state");
    }

    public void switchToPreviousState() {
        turnState.set(previousState);
        climateTracker.set(gameModel.getClimateTracker().getClimateLevel());
        updateTileViewStates();

    }

    public void endTurn() { 
        if (isGameOver()) {
            // switch to stats screen
            StatsViewModel statsViewModel = new StatsViewModel(gameModel, navigationService);
            navigationService.navigateTo("stats", statsViewModel);
            return;
        }

        nextPlayer();
        switchToRollDiceState();
    }

    public int[][] getRoads() {
        return AdjacencyMaps.RoadConnections;
    }

    public int[][] getPorts() {
        return AdjacencyMaps.PortVertices;
    }

    public ObjectProperty<PlayerViewState> currentPlayerProperty() {
        return currentPlayer;
    }

    public void moveRobber(int index) {
        if (turnState.get() == TurnState.MOVE_ROBBER_STATE) {
            gameModel.checkPlayerResources();
            gameModel.moveRobber(index);
            switchToStealResourceState();
        } else if (turnState.get() == TurnState.ECO_CONFERENCE) {
            gameModel.moveRobber(index);
            switchToStealResourceState();
        }
        updateTileViewStates();

    }

    public void setBankTrade(ResourceConfig giveResource, ResourceConfig receiveResource) {
        TradeBank tradeBank = new TradeBank(currentPlayer.get().idProperty().get(), giveResource, receiveResource);
        if (gameModel.validTrade(tradeBank)) {
            gameModel.executeTrade(tradeBank);
            updatePlayerViewState(currentPlayer.get());
            updateBankViewState(bankState.get());
        }
    }

    public void setPortTrade(PortConfig portConfig, ResourceConfig receiveResource) {
        TradePort tradePort = new TradePort(portConfig, currentPlayer.get().idProperty().get(), receiveResource);
        if (gameModel.validTrade(tradePort)) {
            gameModel.executeTrade(tradePort);
            updatePlayerViewState(currentPlayer.get());
            updateBankViewState(bankState.get());
        }
    }

    public void setPlayerTrade(int playerID, HashMap<ResourceConfig, Integer> giveResource,
            HashMap<ResourceConfig, Integer> receiveResource) {
        TradePlayer tradePlayer = new TradePlayer(currentPlayer.get().idProperty().get(), playerID, giveResource,
                receiveResource);
        if (gameModel.validTrade(tradePlayer)) {
            gameModel.executeTrade(tradePlayer);
            System.out.println("Trade executed successfully");
            updatePlayerViewStates();
        }
    }

    private static final Color[] PLAYER_COLOURS = {
            Color.web("#e43b29"), // player 1 red
            Color.web("#4fa6eb"), // player 2 blue
            Color.web("#f0ad00"), // player 3 yellow
            Color.web("#517d19") // player 4 green
    };

    private static final Color UNOWNED_COLOR = Color.GRAY;

    public Color getPlayerColor(int owner) {
        owner = owner - 1; // player IDs are 1-indexed, but our array is 0-indexed
        return (owner >= 0 && owner < PLAYER_COLOURS.length)
                ? PLAYER_COLOURS[owner]
                : UNOWNED_COLOR;
    }

    // TESTING METHODS
    public void giveCityResources() {
        gameModel.giveCityResources(getCurrentPlayer().idProperty().get());
    }

    public void giveSettlementResources() {
        gameModel.giveSettlementResources(getCurrentPlayer().idProperty().get());
    }

    public void giveRoadResources() {
        gameModel.giveRoadResources(getCurrentPlayer().idProperty().get());
    }
}
