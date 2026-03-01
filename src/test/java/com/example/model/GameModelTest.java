package com.example.model;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.model.config.ConfigManager;
import com.example.model.config.PortConfig;
import com.example.model.config.ResourceConfig;
import com.example.model.config.service.ConfigService;
import com.example.model.trading.TradeBank;
import com.example.model.trading.TradePlayer;
import com.example.model.trading.TradePort;

/**
 * Unit tests for GameModel class
 */
public class GameModelTest {
    private GameModel gameModel;
    private ArrayList<String> playerNames;

    @BeforeAll
    public static void setup() throws Exception {
        try {
            ConfigManager.loadAll();
        } catch (Exception e) {
        } // needed to load ResourceRegistry if not loaded, throws if already loaded
    }

    @BeforeEach
    public void setUp() {
        gameModel = new GameModel();
        playerNames = new ArrayList<>();
        playerNames.add("Alice");
        playerNames.add("Bob");
        playerNames.add("Charlie");
    }

    @Test
    public void testConstructor() {
        // test its not null and a field seems instantiated
        assertNotNull(gameModel);
        assertNotNull(gameModel.getPlayers());
        assertEquals(0, gameModel.getPlayers().size());
    }

    @Test
    public void testInitializePlayers() {
        gameModel.initializePlayers(playerNames);
        assertEquals(3, gameModel.getPlayers().size());
        assertEquals("Alice", gameModel.getPlayers().get(0).getName());
    }

    @Test
    public void testInitializePlayersEmpty() {
        ArrayList<String> emptyNames = new ArrayList<>();
        gameModel.initializePlayers(emptyNames);
        assertEquals(0, gameModel.getPlayers().size());
    }

    @Test
    public void testGetNumberOfTiles() {
        int numTiles = gameModel.getNumberOfTiles();
        assertTrue(numTiles > 0);
    }

    @Test
    public void testGetNumberOfVertices() {
        int numVertices = gameModel.getNumberOfVertices();
        assertTrue(numVertices > 0);
    }

    @Test
    public void testGetTiles() {
        assertNotNull(gameModel.getTiles());
        assertEquals(gameModel.getNumberOfTiles(), gameModel.getTiles().length);
    }

    @Test
    public void testGetSettlements() {
        assertNotNull(gameModel.getSettlements());
    }

    @Test
    public void testGetPlayer() {
        gameModel.initializePlayers(playerNames);
        Player player = gameModel.getPlayer(gameModel.getPlayers().get(0).getId());
        assertNotNull(player);
        assertEquals("Alice", player.getName());
    }

    @Test
    public void testGetPlayerNotFound() {
        gameModel.initializePlayers(playerNames);
        assertNull(gameModel.getPlayer(999));
    }

    @Test
    public void testNextPlayer() {
        gameModel.initializePlayers(playerNames);
        int player1Id = gameModel.getPlayers().get(0).getId();
        int player2Id = gameModel.getPlayers().get(1).getId();
        int player3Id = gameModel.getPlayers().get(2).getId();

        assertEquals(player2Id, gameModel.nextPlayer(player1Id));
        assertEquals(player3Id, gameModel.nextPlayer(player2Id));
        assertEquals(player1Id, gameModel.nextPlayer(player3Id));
    }

    @Test
    public void testNextPlayerSinglePlayer() {
        ArrayList<String> singlePlayer = new ArrayList<>();
        singlePlayer.add("Alice");
        gameModel.initializePlayers(singlePlayer);
        int playerId = gameModel.getPlayers().get(0).getId();
        assertEquals(playerId, gameModel.nextPlayer(playerId));
    }

    @Test
    public void testCityNotValid() {
        gameModel.initializePlayers(playerNames);
        int playerId = gameModel.getPlayers().get(0).getId();
        assertFalse(gameModel.cityValid(0, playerId)); // No settlement exists
    }

    @Test
    public void testBuildSettlement() {
        gameModel.initializePlayers(playerNames);
        int playerId = gameModel.getPlayers().get(0).getId();
        gameModel.giveSettlementResources(playerId);

        boolean result = gameModel.buildSettlement(0, playerId);
        // Result depends on Settlements logic
        assertTrue(result);
    }

    @Test
    public void testBuildCity() {
        gameModel.initializePlayers(playerNames);
        int playerId = gameModel.getPlayers().get(0).getId();
        gameModel.giveSettlementResources(playerId);
        gameModel.giveCityResources(playerId);

        gameModel.buildSettlement(0, playerId); // First build settlement
        boolean result = gameModel.buildCity(0, playerId);
        // Result depends on Settlements logic
        assertTrue(result);
    }

    @Test
    public void testBuildRoad() {
        gameModel.initializePlayers(playerNames);
        int playerId = gameModel.getPlayers().get(0).getId();
        gameModel.giveRoadResources(playerId);

        boolean result = gameModel.buildRoad(0, playerId);
        // Result depends on Roads logic
        assertTrue(result);
    }

    @Test
    public void testGetRoads() {
        assertNotNull(gameModel.getRoads());
        assertTrue(gameModel.getRoads().length > 0);
    }

    @Test
    public void testGetSettlementType() {
        assertNotNull(gameModel.getSettlmentType(0));
    }

    @Test
    public void testGetSettlementOwner() {
        int owner = gameModel.getSettlmentOwner(0);
        assertNotNull(owner);
    }

    @Test
    public void testValidTradePlayerSamePlayer() {
        gameModel.initializePlayers(playerNames);
        ResourceConfig wood = ConfigService.getResource("resource.wood");
        ResourceConfig brick = ConfigService.getResource("resource.brick");

        int playerId = gameModel.getPlayers().get(0).getId();
        gameModel.getPlayer(playerId).setResourceCount(wood, 10);
        gameModel.getPlayer(playerId).setResourceCount(brick, 10);
        HashMap<ResourceConfig, Integer> offer = new HashMap<>();
        offer.put(wood, 1);
        HashMap<ResourceConfig, Integer> request = new HashMap<>();
        request.put(brick, 1);
        TradePlayer trade = new TradePlayer(playerId, playerId, offer, request);
        assertFalse(gameModel.validTrade(trade));
    }

    @Test
    public void testValidTradePlayerInsufficientResources() {
        gameModel.initializePlayers(playerNames);
        ResourceConfig wood = ConfigService.getResource("resource.wood");
        ResourceConfig brick = ConfigService.getResource("resource.brick");
        int player1Id = gameModel.getPlayers().get(0).getId();
        int player2Id = gameModel.getPlayers().get(1).getId();

        // no resources given to players, invalid trade
        HashMap<ResourceConfig, Integer> offer = new HashMap<>();
        offer.put(wood, 10);
        HashMap<ResourceConfig, Integer> request = new HashMap<>();
        request.put(brick, 1);
        TradePlayer trade = new TradePlayer(player1Id, player2Id, offer, request);
        assertFalse(gameModel.validTrade(trade));
    }

    @Test
    public void testValidTradePlayerValidTrade() {
        gameModel.initializePlayers(playerNames);
        int player1Id = gameModel.getPlayers().get(0).getId();
        int player2Id = gameModel.getPlayers().get(1).getId();
        ResourceConfig wood = ConfigService.getResource("resource.wood");
        ResourceConfig brick = ConfigService.getResource("resource.brick");

        gameModel.getPlayer(player1Id).setResourceCount(wood, 10);
        gameModel.getPlayer(player2Id).setResourceCount(brick, 10);

        HashMap<ResourceConfig, Integer> offer = new HashMap<>();
        offer.put(wood, 10);
        HashMap<ResourceConfig, Integer> request = new HashMap<>();
        request.put(brick, 1);

        TradePlayer trade = new TradePlayer(player1Id, player2Id, offer, request);
        assertTrue(gameModel.validTrade(trade));
    }

    @Test
    public void testValidTradeBankInsufficientPlayerResources() {
        gameModel.initializePlayers(playerNames);
        ResourceConfig wood = ConfigService.getResource("resource.wood");
        ResourceConfig brick = ConfigService.getResource("resource.brick");
        int playerId = gameModel.getPlayers().get(0).getId();
        TradeBank trade = new TradeBank(playerId, wood, brick);
        assertFalse(gameModel.validTrade(trade));
    }

    @Test
    public void testValidTradeBankValidTrade() {
        gameModel.initializePlayers(playerNames);
        ResourceConfig wood = ConfigService.getResource("resource.wood");
        ResourceConfig brick = ConfigService.getResource("resource.brick");
        int playerId = gameModel.getPlayers().get(0).getId();
        gameModel.getPlayer(playerId).setResourceCount(brick, 10);
        TradeBank trade = new TradeBank(playerId, brick, wood);
        assertTrue(gameModel.validTrade(trade));
    }

    @Test
    public void testValidTradePortInsufficientResources() {
        gameModel.initializePlayers(playerNames);
        ResourceConfig brick = ConfigService.getResource("resource.brick");
        PortConfig port = ConfigService.getPort("port.brick");
        int playerId = gameModel.getPlayers().get(0).getId();
        TradePort trade = new TradePort(port, playerId, brick);
        assertFalse(gameModel.validTrade(trade)); // player has no brick to trade
    }

    @Test
    public void testValidTradePortValidTrade() {
        gameModel.initializePlayers(playerNames);
        ResourceConfig brick = ConfigService.getResource("resource.brick");
        PortConfig port = ConfigService.getPort("port.brick");
        int playerId = gameModel.getPlayers().get(0).getId();
        gameModel.getPlayer(playerId).setResourceCount(brick, 10);
        TradePort trade = new TradePort(port, playerId, brick);
        assertTrue(gameModel.validTrade(trade)); // player has brick to trade
    }

    @Test
    public void testExecuteTradePlayerValid() {
        gameModel.initializePlayers(playerNames);
        int player1Id = gameModel.getPlayers().get(0).getId();
        int player2Id = gameModel.getPlayers().get(1).getId();
        ResourceConfig wood = ConfigService.getResource("resource.wood");
        ResourceConfig brick = ConfigService.getResource("resource.brick");
        gameModel.getPlayer(player1Id).setResourceCount(wood, 10);
        gameModel.getPlayer(player2Id).setResourceCount(brick, 10);

        HashMap<ResourceConfig, Integer> offer = new HashMap<>();
        offer.put(wood, 10);
        HashMap<ResourceConfig, Integer> request = new HashMap<>();
        request.put(brick, 1);
        TradePlayer trade = new TradePlayer(player1Id, player2Id, offer, request);

        boolean result = gameModel.executeTrade(trade);
        assertTrue(result);
    }

    @Test
    public void testExecuteTradeBankValid() {
        gameModel.initializePlayers(playerNames);
        ResourceConfig wood = ConfigService.getResource("resource.wood");
        ResourceConfig brick = ConfigService.getResource("resource.brick");
        int playerId = gameModel.getPlayers().get(0).getId();
        gameModel.getPlayer(playerId).setResourceCount(brick, 10);
        TradeBank trade = new TradeBank(playerId, brick, wood);
        boolean result = gameModel.executeTrade(trade);
        assertTrue(result);
    }

    @Test
    public void testExecuteTradePortValid() {
        gameModel.initializePlayers(playerNames);
        ResourceConfig brick = ConfigService.getResource("resource.brick");
        PortConfig port = ConfigService.getPort("port.brick");
        int playerId = gameModel.getPlayers().get(0).getId();
        gameModel.getPlayer(playerId).setResourceCount(brick, 10);
        TradePort trade = new TradePort(port, playerId, brick);
        boolean result = gameModel.executeTrade(trade); // player has brick to trade
        assertTrue(result);
    }
}