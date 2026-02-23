package com.example.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.example.model.config.DevCardConfig;
import com.example.model.config.PlayerInfrastructureConfig;
import com.example.model.config.PortConfig;
import com.example.model.config.ResourceConfig;
import com.example.model.config.registry.ResourceRegistry;
import com.example.model.config.service.ConfigService;
import com.example.model.trading.TradeBank;
import com.example.model.trading.TradePlayer;
import com.example.model.trading.TradePort;

public class GameModel {
    private final ArrayList<Player> players;
    private final Tiles tiles;
    private final Ports ports;
    private final Roads roads;
    private final Settlements settlements;
    private final Dice dice;
    private final BankCards bankCards;
    private final ClimateTracker climateTracker;

    private boolean passBuildRule; // disables checking if roads or settlements are connected to others, to setup
                                   // the board

    public GameModel() {
        this.players = new ArrayList<>();
        this.tiles = new Tiles();
        this.ports = new Ports();
        this.roads = new Roads();
        this.settlements = new Settlements();
        this.dice = new Dice();
        this.bankCards = new BankCards();
        this.climateTracker = new ClimateTracker();

        this.passBuildRule = false;
    }

    // // code used to test how settlement placements are be valued
    // private void _logTestValuation(int playerID, double k, int[] vertices) {
    // System.out.print("-- Player ");
    // System.out.print(playerID);System.out.print("-- K: ");
    // System.out.print(k);System.out.print(", Vertices: ");
    // for (int v : vertices) {
    // System.out.print(v + ", ");
    // }

    // System.out.println();
    // }

    // Creates the tile bias for each tile type for the player
    private HashMap<String, Double> getTileBias(int playerID) {

        // 0 1 2 3 4 5 6 7 8 9 10 11 12
        int[] probs = { 0, 0, 1, 2, 3, 4, 5, 0, 5, 4, 3, 2, 1 };

        // create hashmap to count the number of cards the player can expect of the
        // tileID for the average die roll
        HashMap<String, Integer> ownedProbabilityPerTile = new HashMap<>();
        ownedProbabilityPerTile.put("tile.forest", 0);
        ownedProbabilityPerTile.put("tile.hills", 0);
        ownedProbabilityPerTile.put("tile.mountains", 0);
        ownedProbabilityPerTile.put("tile.fields", 0);
        ownedProbabilityPerTile.put("tile.pasture", 0);
        ownedProbabilityPerTile.put("tile.desert", 0);

        // goes through all owned settlements for the player, and adds to the
        // probability all surrounding tiles
        for (Settlement s : settlements.getAllOwnedSettlements()) {
            if (s.getPlayerID() == playerID) {
                int vertex = s.getVertex();
                for (Tile t : tiles.getTiles()) {
                    for (int v : t.getAdjVertices()) {
                        if (v == vertex) {
                            ownedProbabilityPerTile.merge(t.getTileID(), probs[t.getNumber()], Integer::sum);
                        }
                    }
                }
            }
        }

        HashMap<String, Double> tileBias = new HashMap<>();

        double k = 0.18d; // amount each owned probability point for a tile type decreases bias to get the
                          // tile
                          // improves spread of resources
                          // k = 0 ignores spread of resources, k > 0.3 is harsh enough to break valuation
                          // (bias is close 0 for already owned resources)

        // Bias is found by the equation: t * e**(-ka), where t is starting bias, k is a
        // constant (defined above), and a is the probability count

        tileBias.put("tile.forest", (Double) (1.01d * Math.exp(-k * ownedProbabilityPerTile.get("tile.forest"))));
        tileBias.put("tile.hills", (Double) (1.01d * Math.exp(-k * ownedProbabilityPerTile.get("tile.hills"))));
        tileBias.put("tile.mountains", (Double) (1.d * Math.exp(-k * ownedProbabilityPerTile.get("tile.mountains"))));
        tileBias.put("tile.fields", (Double) (1.d * Math.exp(-k * ownedProbabilityPerTile.get("tile.fields"))));
        tileBias.put("tile.pasture", (Double) (0.99d * Math.exp(-k * ownedProbabilityPerTile.get("tile.pasture"))));
        tileBias.put("tile.desert", (Double) 0.d);

        return tileBias;
    }

    // higher the rating, the better vertex
    private double rateVertex(int vertex, int playerID) {
        double rating = 0.f;
        // 0 1 2 3 4 5 6 7 8 9 10 11 12
        int[] probs = { 0, 0, 1, 2, 3, 4, 5, 0, 5, 4, 3, 2, 1 };

        HashMap<String, Double> tileBias = getTileBias(playerID);

        for (Tile t : this.tiles.getTiles()) {
            for (int n : t.getAdjVertices()) {
                if (n == vertex) {
                    rating += probs[t.getNumber()] * Math.max(tileBias.get(t.getTileID()), 0.d);
                }
            }
        }

        return rating;
    }

    private int[] verticesSortedByRating(int playerID) {

        // rate each vertex based on its probablility of being rolled
        int NUM_OF_VERTICES = 54;
        double[] vertexRatings = new double[NUM_OF_VERTICES];

        for (int i = 0; i < NUM_OF_VERTICES; i++) {
            vertexRatings[i] = rateVertex(i, playerID);
        }

        // enable building without connecting to existing settlements
        passBuildRule = true;

        // sort the vertices based on their ratings
        double[] vr = vertexRatings.clone();
        int[] vertices = new int[NUM_OF_VERTICES]; // the vertices sorted in order of rating
                                                   // Note: default constructs to `0` for each value

        // O(n^2) sorting algorithm :/ sorts vertices (j) in the order that the appear
        // in vr
        for (int i = 0; i < NUM_OF_VERTICES; i++) {
            double value = -1.d;
            int v = -1;

            for (int j = 0; j < NUM_OF_VERTICES; j++) {
                if (vr[j] > value) {
                    value = vr[j];
                    v = j;
                }
            }
            if (v == -1)
                break; // break early if no rating
            vertices[i] = v;
            vr[v] = -1.f;
        }

        return vertices;
    }

    // counts the number of empty vertices in a given direction
    // recursive function; distance is how far to check for emptiness, prevVertex is
    // last vertex (skips checking it again)
    // , vertex is the current vertex being checked, and checkedVertices are the
    // vertices check by this search already
    private int countEmptyVertices(int distance, int prevVertex, int vertex, HashSet<Integer> checkedVertices) {
        if (checkedVertices.contains(vertex)) {
            return 0; // if already checked, return 0
        }

        if (distance <= 0) {
            return 0; // if no distance, do not check
        }

        boolean unoccupied = this.settlements.GetSettlementFromVertex(vertex) == null;

        if (!unoccupied) {
            return 0; // no empty space this direction if blocked by settlement
        }

        checkedVertices.add(vertex);

        int count = 0;
        for (int neighbouringVertex : AdjacencyMaps.getAdjacentVertices(vertex)) {
            if (neighbouringVertex == prevVertex)
                continue; // skip previously checked vertex
            count += countEmptyVertices(distance - 1, vertex, neighbouringVertex, checkedVertices);
        }
        return 1 + count; // add one to the number of empty vertices in this direction
    }

    private ArrayList<Road> rateAndSortRoads(ArrayList<Road> roads) {
        ArrayList<Integer> roadRatings = new ArrayList<>(); // ratings of roads (to sort by)

        int DISTANCE = 4; // distance to check vertices

        for (Road r : roads) {
            int[] verts = r.getVertices();
            // rates both vertices of the road based on emptiness in each direction
            // NOTE: can double count vertices if distance is greater than 2, no better
            // solution possible
            // shouldn't happen in the current use-case i.e. one side of the road has a
            // settlement
            roadRatings.add(countEmptyVertices(DISTANCE, verts[1], verts[0], new HashSet<>())
                    + countEmptyVertices(DISTANCE, verts[0], verts[1], new HashSet<>()));
        }

        // Create list of indices
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < roads.size(); i++) {
            indices.add(i);
        }

        // Sort indices by associated rating (descending here)
        indices.sort((i, j) -> Integer.compare(
                roadRatings.get(j),
                roadRatings.get(i)));

        // Build sorted roads list
        ArrayList<Road> sortedRoads = new ArrayList<>();
        for (int i : indices) {
            sortedRoads.add(roads.get(i));
        }

        return sortedRoads;
    }

    // Adds two settlements and two roads per player
    public boolean initializeBoard() {

        // make a random assortment of playerIDs for selecting settlements twice
        // e.g. 2,1,3,4,4,3,1,2
        ArrayList<Integer> playerIds = new ArrayList<>();
        for (int i = 0; i < this.players.size(); i++) {
            playerIds.add((Integer) this.players.get(i).getId());
        }
        Collections.shuffle(playerIds);
        for (int i = this.players.size() - 1; i > -1; i--) {
            playerIds.add(playerIds.get(i));
        }

        ArrayList<Integer> builtVertices = new ArrayList<>(); // used for road building phase

        // | Build Settlement Phase |
        // for each player in the build order
        // trys to build a settlement on the next best square and loops till it finds
        // one that works

        for (int id : playerIds) {
            // _logTestValuation(id, 0.18, verticesSortedByRating(id));

            int[] vertices = verticesSortedByRating(id); // get the vertices, based on their value to the player
            boolean settlementBuilt = false;
            giveSettlementResources(id); // give settlement resources (to pass the check)

            int vIndex = 0; // index of next vertex to try build upon
            while (!settlementBuilt) {
                if (vIndex == 54) {
                    passBuildRule = false;
                    return false; // exit without finishing
                }

                int v = vertices[vIndex++]; // get the vertex (sorted with best first)
                if (!settlementValid(v, id))
                    continue; // if invalid skip to next

                settlementBuilt = buildSettlement(v, id); // build the settlement
                if (settlementBuilt) {
                    builtVertices.add(v); // store the vertex, for the road building phase
                }
            }

        }

        // | Build Road Phase |
        // for each player ID and built vertex, build a road in the optimal direction

        for (int i = 0; i < playerIds.size(); i++) {
            int playerID = playerIds.get(i);
            int vertex = builtVertices.get(i);

            giveRoadResources(playerID); // give resources to pass build check

            boolean roadBuilt = false;
            ArrayList<Road> potentialRoads = new ArrayList<>();

            for (Road r : this.roads.getAllRoads()) {
                if (r.getPlayerID() != Roads.UNOWNED_ROAD_ID)
                    continue; // skip if owned (shouldn't happen at this stage)
                for (int v : r.getVertices()) {
                    if (v == vertex) {
                        potentialRoads.add(r);
                    }
                }
            }

            // sort roads by optimal placement
            potentialRoads = rateAndSortRoads(potentialRoads);
            // Collections.shuffle(potentialRoads);

            // attempt to build road (should work on first road build attempt)
            for (Road r : potentialRoads) {
                int[] roadVerts = r.getVertices();
                roadBuilt = buildRoad(Roads.getRoadIndex(roadVerts[0], roadVerts[1]), playerID);
                if (roadBuilt) break;
            }
            if (!roadBuilt) {
                passBuildRule = false;
                return false; // exit without finishing
            }
        }

        // disable building without connecting to existing settlements
        passBuildRule = false;

        // Reset Climate Tracker
        climateTracker.resetClimateLevels();

        // Repare any destroyed tiles
        tiles.repareTiles();


        return true; // successful
    }

    public void initializePlayers(ArrayList<String> playerNames) {
        for (String name : playerNames) {
            players.add(new Player(name));
        }
    }

    public int getNumberOfTiles() {
        return tiles.getTiles().length;
    }

    public int getNumberOfVertices() {
        int[][] vertexPerTile = AdjacencyMaps.TileVertices;
        int vertixCount = 0;
        for (int[] vertices : vertexPerTile) {
            vertixCount += vertices.length;
        }
        return vertixCount;
    }

    public Tile[] getTiles() {
        return tiles.getTiles();
    }

    public Settlement[] getSettlements() {
        return settlements.getAllSettlements();
    }

    public String getSettlmentType(int index) {
        return settlements.getAllSettlements()[index].getSettlementType();
    }

    public int getSettlmentOwner(int index) {
        return settlements.getAllSettlements()[index].getPlayerID();
    }

    public boolean settlementValid(int vertex, int playerID) {
        boolean settlementDistanceValid = !settlements.nearbySettlement(vertex); // Note: settlement distance rule is
                                                                                 // valid when *NOT* a nearby settlement
        boolean linkedByRoad = roads.isVertexConnectedByPlayer(vertex, playerID) || passBuildRule;
        boolean unowned = getSettlmentOwner(vertex) == Settlements.UNOWNED_SETTLEMENT_ID;
        return settlementDistanceValid && linkedByRoad && unowned;
    }

    public boolean cityValid(int vertex, int playerID) {
        boolean isOwner = getSettlmentOwner(vertex) == playerID;
        boolean notAlreadyCity = !settlements.getAllSettlements()[vertex].isCity();
        return isOwner && notAlreadyCity;
    }

    public boolean roadValid(int edgeIndex, int playerID) {
        // We do not need to check if connected to settlement, road is enough (as every settlement is also conected to a road)
        boolean connectedToRoad = roads.isRoadConnectedByPlayer(edgeIndex, playerID) || passBuildRule;
        boolean unowned = !roads.isRoadOwned(edgeIndex);

        return connectedToRoad && unowned;
    }

    /**
     * checks if a steal is valid
     * @param vertex vertex of settlement being stole from
     * @param playerID player stealing
     * @return steal valid
     */
    public boolean stealValid(int vertex, int playerID) {
        int blockedTile = tiles.getBlockedTileIndex(); 
        if (blockedTile == -1) return false; // no blocked tile? stealing is invalid

        int[] adjacentVertices = tiles.getTiles()[blockedTile].getAdjVertices();
        for (int v : adjacentVertices) {
            if(v == vertex) {
                int ownerId = settlements.ownedByPlayer(v);
                if (ownerId != Settlements.UNOWNED_SETTLEMENT_ID && ownerId != playerID) {
                    return true; // valid target to steal from
                }
            }
        }
        return false; // This Vertex is not adjacent to the blocked tile or has no valid target to
                      // steal from
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public Player getPlayer(int playerID) {
        for (Player player : players) {
            if (player.getId() == playerID) {
                return player;
            }
        }
        return null;
    }

    public int nextPlayer(int currentPlayerId) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId() == currentPlayerId) {
                int nextIndex = (i + 1) % players.size();
                return players.get(nextIndex).getId();
            }
        }
        return -1; // invalid ID as input
    }

    public boolean checkIfGameOver() {
        for (Player p : this.players) {
            System.err.println(p.getTotalVictoryPoints());
            if (p.getTotalVictoryPoints() >= 10) {
                return true;
            }
        }
        return false;
    }

    public boolean buildSettlement(int vertex, int playerID) {
        Player player = getPlayer(playerID);
        String structureID = "player_infrastructure.settlement";

        boolean success_build = settlements.buildSettlement(vertex, playerID);
        boolean success_resources = getPlayer(playerID).deductStructureResources(structureID);
        boolean success_pieces = player.changeStructuresRemainingByType(structureID, -1);
        
        if (success_resources && success_build) {
            increaseClimateAndDistributeDisasterCards();
        }
        player.changeVictoryPoints(+1);// add victory point
        return success_resources && success_pieces && success_build;
    }

    public boolean playerHasSettlementResources(int playerID) {
        Player player = getPlayer(playerID);
        String structureID = "player_infrastructure.settlement";
        boolean validBuildSpace = false;
        for (int i = 0; i < settlements.getAllSettlements().length; i++) {
            if (settlementValid(i, playerID)) {
                validBuildSpace = true;
                break;
            }
        }

        return player.hasEnoughResourcesForStructure(structureID) && player.getStructuresRemaining(structureID) > 0 && validBuildSpace;
    }

    public boolean buildCity(int vertex, int playerID) {
        Player player = getPlayer(playerID);
        String structureID = "player_infrastructure.city";

        boolean success_upgrade = settlements.upgradeSettlement(vertex, playerID);
        boolean success_resources = player.deductStructureResources(structureID);
        // building a city removes a city and adds a settlement from pieces
        boolean success_pieces = player.changeStructuresRemainingByType("player_infrastructure.city", -1)
                && player.changeStructuresRemainingByType("player_infrastructure.settlement", +1);

        // add victory point
        player.changeVictoryPoints(+1);
        return success_resources && success_pieces && success_upgrade;
    }

    public boolean playerHasCityResources(int playerID) {
        Player player = getPlayer(playerID);
        String structureID = "player_infrastructure.city";
        boolean validBuildSpace = false;
        for (int i = 0; i < settlements.getAllSettlements().length; i++) {
            if (cityValid(i, playerID)) {
                validBuildSpace = true;
                break;
            }
        }
        
        return player.hasEnoughResourcesForStructure(structureID) && player.getStructuresRemaining(structureID) > 0 && validBuildSpace;
    }

    public boolean buildRoad(int edgeIndex, int playerID) {
        Player player = getPlayer(playerID);
        String structureID = roads.getAllRoads()[edgeIndex].getRoadType();
        
        boolean success_build = roads.buildRoad(edgeIndex, playerID);
        boolean success_resources = player.deductStructureResources(structureID);
        boolean success_pieces = player.changeStructuresRemainingByType(structureID, -1);
        return success_resources && success_pieces && success_build;
    }

    public boolean playerHasRoadResources(int playerID) {
        Player player = getPlayer(playerID);
        String structureID = "player_infrastructure.road";
        boolean validBuildSpace = false;
        for (int i = 0; i < roads.getAllRoads().length; i++) {
            if (roadValid(i, playerID)) {
                validBuildSpace = true;
                break;
            }
        }

        return player.hasEnoughResourcesForStructure(structureID) && player.getStructuresRemaining(structureID) > 0 && validBuildSpace;
    }


    public boolean stealResource(int vertexIndex, int playerID) {
        if (!stealValid(vertexIndex, playerID)) {
            return false;
        }
        int blockedTile = tiles.getBlockedTileIndex();
        int[] adjacentTiles = tiles.getTiles()[blockedTile].getAdjVertices();
        for (int vertex : adjacentTiles) {
            if (vertex == vertexIndex) {
                int ownerId = settlements.getAllSettlements()[vertex].getPlayerID();
                Player victim = getPlayer(ownerId);
                ResourceConfig stolenResource = victim.stealRandomResource();
                if (stolenResource != null) {
                    getPlayer(playerID).changeResourceCount(stolenResource, 1);
                    return true; // successfully stole a resource
                }
            }
        }
        return false; // failed to steal a resource
    }

    public boolean validTrade(TradePlayer trade) {
        // trade valid if both players have required resources
        if (trade.playerAId() == trade.playerBId()) {
            return false;
        }
        Player playerA = getPlayer(trade.playerAId());
        Player playerB = getPlayer(trade.playerBId());

        HashMap<ResourceConfig, Integer> resourcesAGive = trade.resourcesAGive();
        HashMap<ResourceConfig, Integer> resourcesBGive = trade.resourcesBGive();

        for (Map.Entry<ResourceConfig, Integer> entry : resourcesAGive.entrySet()) {
            ResourceConfig resource = entry.getKey();
            int amount = entry.getValue();
            if (playerA.getResourceCount(resource) < amount) {
                return false;
            }
        }

        for (Map.Entry<ResourceConfig, Integer> entry : resourcesBGive.entrySet()) {
            ResourceConfig resource = entry.getKey();
            int amount = entry.getValue();
            if (playerB.getResourceCount(resource) < amount) {
                return false;
            }
        }

        return true;
    } 

    public boolean validTrade(TradeBank trade) {
        // trade valid if player and bank have required resources
        int bankResourceCount = bankCards.getResourceCount(trade.recieveResource());
        if (bankResourceCount <= 0) {
            return false;
        }

        Player player = getPlayer(trade.playerId());

        int playerResourceCount = player.getResourceCount(trade.giveResource());
        if (playerResourceCount < TradeBank.TRADE_RATE) {
            return false;
        }

        return true;
    }

    public boolean validTrade(TradePort trade) {
        // trade valid if player and bank have required resources (ports are just a front for the bank)
        Player player = getPlayer(trade.playerId());

        int playerResourceCount = player.getResourceCount(ResourceRegistry.getInstance().get(trade.port().resourceID)); // gets
                                                                                                                        // the
                                                                                                                        // resource
                                                                                                                        // being
                                                                                                                        // given
                                                                                                                        // to
                                                                                                                        // the
                                                                                                                        // port;
        if (playerResourceCount < trade.port().giveQuantity) { // the number of resources the player has of that type
            return false;
        }

        int bankResourceCount = bankCards.getResourceCount(trade.resource());
        if (bankResourceCount < trade.port().receiveQuantity) {
            return false;
        }

        return true;
    }

    public boolean executeTrade(TradePlayer trade) {
        if (!validTrade(trade)) {
            return false;
        }

        System.out.println(getPlayer(trade.playerAId()));
        System.out.println(getPlayer(trade.playerBId()));

        Player playerA = getPlayer(trade.playerAId());
        Player playerB = getPlayer(trade.playerBId());
        HashMap<ResourceConfig, Integer> playerAResources = trade.resourcesAGive();
        HashMap<ResourceConfig, Integer> playerBResources = trade.resourcesBGive();

        for (Map.Entry<ResourceConfig, Integer> entry : playerAResources.entrySet()) {
            ResourceConfig resource = entry.getKey();   
            int amount = entry.getValue();
            playerA.changeResourceCount(resource, -amount);
            playerB.changeResourceCount(resource, +amount);
        }

        for (Map.Entry<ResourceConfig, Integer> entry : playerBResources.entrySet()) {
            ResourceConfig resource = entry.getKey();
            int amount = entry.getValue();
            playerB.changeResourceCount(resource, -amount);
            playerA.changeResourceCount(resource, +amount);
        }
        
        System.out.println("Trade executed successfully");
        System.out.println(getPlayer(trade.playerAId()));
        System.out.println(getPlayer(trade.playerBId()));


        return true;
    }

    public boolean executeTrade(TradeBank trade) {
        if (!validTrade(trade)) {
            return false;
        }
        Player player = getPlayer(trade.playerId());

        bankCards.giveResourceCard(trade.recieveResource(), 1);
        player.changeResourceCount(trade.giveResource(), -TradeBank.TRADE_RATE);

        bankCards.returnResourceCard(trade.giveResource(), TradeBank.TRADE_RATE);
        player.changeResourceCount(trade.recieveResource(), +1);

        return true;
    }

    public boolean executeTrade(TradePort trade) {
        if (!validTrade(trade)) {
            return false;
        }
        Player player = getPlayer(trade.playerId());

        bankCards.giveResourceCard(trade.resource(), trade.port().receiveQuantity);
        player.changeResourceCount(ResourceRegistry.getInstance().get(trade.port().resourceID),
                -trade.port().giveQuantity);

        bankCards.returnResourceCard(ResourceRegistry.getInstance().get(trade.port().resourceID),
                trade.port().giveQuantity);
        player.changeResourceCount(trade.resource(), +trade.port().receiveQuantity);

        return true;
    }

    // method to give players resources based on the dice roll
    public void giveResourcesToPlayers(int diceroll) {
        for (Tile tile : tiles.GetTilesFromDiceroll(diceroll)) {
            // get resource of rolled tile
            ResourceConfig resource = tile.getResourceFromTileID();

            // get all vertices on that tile
            for (int vertex : tile.getAdjVertices()) {

                // check if there's a settlement on that vertex
                Settlement currentSettlement = settlements.GetSettlementFromVertex(vertex);
                if (currentSettlement == null) {
                    continue; // no settlement found, skip the rest of this method
                }

                // find the player who owns the settlement
                Player player = getPlayer(currentSettlement.getPlayerID());
                if (player == null) {
                    throw new IllegalStateException("Player not found for settlement");
                }

                int production = 1;
                if (currentSettlement.isCity()) {
                    production++;
                }

                // for loop accounts for cities giving two resources, whilst still ensuring
                // that when only one resource is left, a city will still produce one
                for (int i = 0; i < production; i++) {
                    // check if there is a free resource left in the bank
                    if (bankCards.giveResourceCard(resource, 1)) {
                        player.changeResourceCount(resource, 1);
                        // Only settlements (not cities) cause climate to increase / disaster cards to
                        // be considered.
                        if (!currentSettlement.isCity()) {
                            increaseClimateAndDistributeDisasterCards();
                        }
                    }
                    // bank empty, stop giving out resources
                    else {
                        break;
                    }
                }

            }
        }
    }

    // method to trigger the robber
    public void moveRobber(int tileIndex) {
        tiles.changeBlockedTile(tileIndex);

        // checkPlayerResources is triggered from the robber button click
        // knight cards trigger the moveRobber method and NOT checkPlayerRobbers
    }

    // check if any players have more than 7 resources and discard excess cards randomly
    public void checkPlayerResources(){
        for (Player player : players){
            int cardCount = 0;// get total resource count
            ArrayList<ResourceConfig> playerResources = new ArrayList<>();
            
            for (ResourceConfig resource : ConfigService.getAllResources()){
                int count = player.getResourceCount(resource);
                cardCount += count;
                for (int i = 0; i < count; i++){
                    playerResources.add(resource);
                }
            }

            if (cardCount < 8){
                continue; // none to be discarded, go to next
            }

            // card count to be discarded
            int cardsToDiscard = cardCount / 2; // integer div, floors automatically

            // randomly discard the amount of cards
            Random random = new Random();
            for (int i = 0; i < cardsToDiscard; i++){
                int randomNum = random.nextInt(playerResources.size());
                player.changeResourceCount(playerResources.get(randomNum), -1);
                playerResources.remove(randomNum);
            }
        }
    }

    public Road[] getRoads() {
        return roads.getAllRoads();
    }

    public boolean playerHasLongestRoad(int playerId) {
        int[] playerIds = new int[players.size()]; // create playerId array
        int i=0;
        for (Player p: players) {
            playerIds[i++] = p.getId();
        }
        return roads.longestRoadExists(playerIds) && roads.longestRoadOwner(playerIds) == playerId;
    }


    // need to do front end stuff to chose tile to destroy
    // asks for same resource as tile atm - need to fix
    public boolean tileRestore(int tileIndex, int playerId) {
        Tile[] allTiles = tiles.getTiles();
        if (tileIndex < 0 || tileIndex >= allTiles.length) {
            return false;
        }
        Tile tile = allTiles[tileIndex];
        ResourceConfig resource = tile.getResourceFromTileID();
        if (resource == null) {
            return false; // desert or no-resource tile cannot be restored
        }
        if (!tile.getIsDestroyed()) {
            return false; // nothing to restore
        }

        Player player = getPlayer(playerId);
        if (player == null) {
            return false;
        }

        // derive the infrastructure id for this tile
        String structureId = tile.getTileID().replace("tile.", "player_infrastructure.") + "_tile";
        PlayerInfrastructureConfig cfg = ConfigService.getInfrastructure(structureId);
        if (cfg == null || cfg.constructionCosts.isEmpty()) {
            return false; // no configured cost for this tile
        }

        // check player has the required resources
        if (!player.hasEnoughResourcesForStructure(structureId)) {
            return false;
        }

        // deduct configured resources
        boolean deducted = player.deductStructureResources(structureId);
        if (!deducted) {
            return false;
        }

        // attempt restore; if it fails, refund the deducted resources
        boolean restored = tiles.restoreTile(tileIndex);
        if (!restored) {
            // refund: add back each configured resource and also return them to the bank
            for (String resourceID : cfg.constructionCosts.keySet()) {
                ResourceConfig r = ConfigService.getResource(resourceID);
                int amount = cfg.constructionCosts.get(resourceID);
                player.changeResourceCount(r, amount);
                bankCards.returnResourceCard(r, amount);
            }
            return false;
        }

        // on success, return the deducted resources to the bank
        for (String resourceID : cfg.constructionCosts.keySet()) {
            ResourceConfig r = ConfigService.getResource(resourceID);
            int amount = cfg.constructionCosts.get(resourceID);
            bankCards.returnResourceCard(r, amount);
        }

        player.increaseTilesRestored();

        return true;
    }

    public boolean playerHasCleanestEnvironment(int playerId) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return false;
        }

        int bestPlayerId = -1;
        int bestTiles = -1;
        for (Player p : players) {
            int tid = p.getTilesRestored();
            if (tid > bestTiles) {
                bestTiles = tid;
                bestPlayerId = p.getId();
            }
        }

        return bestPlayerId == playerId && bestTiles >= 3;
    }

    /*
     * also need to call this function:
     * when settlements get resources
     * when certain devcards are played
     * (trading frenzy, highway madness, monopoly)
     * -need to check which devcard before calling
     */
    // also where should tile restoration be implemented
    // for restore tile i need unique id for each tile to know which one to restore
    public void increaseClimateAndDistributeDisasterCards() {
        climateTracker.increaseClimate();

        if (climateTracker.shouldGiveDisasterCard()) {
            int numCards = climateTracker.disasterCardNum();
            for (int i = 0; i < numCards; i++) {
                String disasterCard = bankCards.giveDisasterCard();
                if (!disasterCard.isEmpty()) {
                    // give disaster card
                    // destroy tile
                    tiles.destroyTile(disasterCard);
                }
                // do nothing if no cards are left??
            }
        }
    }

    public boolean playerHasDevCardResources(int playerID) {
        Player player = getPlayer(playerID);
        String structureID = "player_infrastructure.dev_card";

        return player.hasEnoughResourcesForStructure(structureID) && player.getStructuresRemaining(structureID) > 0 && bankCards.hasDevelopmentCards();
    }

    public boolean buyDevelopmentCard(int playerId) {
        Player player = getPlayer(playerId);
        if (player == null)
            return false;

        // check & deduct cost
        if (!player.hasEnoughResourcesForStructure("player_infrastructure.dev_card")) {
            return false;
        }
        boolean deducted = player.deductStructureResources("player_infrastructure.dev_card");
        if (!deducted)
            return false;

        // attempt to draw from bank
        String devCardId = bankCards.giveDevelopmentCard();
        if (devCardId == null || devCardId.isEmpty()) {
            // refund resources
            PlayerInfrastructureConfig cfg = ConfigService.getInfrastructure("player_infrastructure.dev_card");
            for (String resourceID : cfg.constructionCosts.keySet()) {
                ResourceConfig r = ConfigService.getResource(resourceID);
                int amount = cfg.constructionCosts.get(resourceID);
                player.changeResourceCount(r, amount);
            }
            return false;
        }

        // deliver the card (handles victory-point semantics)
        handleReceivedDevelopmentCard(playerId, devCardId);
        return true;
    }

    private void handleReceivedDevelopmentCard(int playerId, String devCardId) {
        Player player = getPlayer(playerId);
        if (player == null)
            return;

        DevCardConfig cfg = ConfigService.getDevCard(devCardId);
        if (cfg == null) {
            // unknown card: treat as no-op
            return;
        }

        String action = cfg.actionType == null ? "" : cfg.actionType;
        if ("VICTORY_POINT".equals(action)) {
            // award invisible VP immediately and do NOT add the card to hand
            player.changeHiddenVictoryPoints(+1);
            return;
        }

        // other dev-cards are added to the player's hand and may be played
        player.addCard(devCardId);
    }

    public boolean playDevCard(int playerId, DevCardConfig devCardConfig) {
        Player player = getPlayer(playerId);
        if (player == null)
            return false;

        if (!player.hasCard(devCardConfig.id))
            return false;

        if (devCardConfig == null)
            return false;

        String action = devCardConfig.actionType == null ? "" : devCardConfig.actionType;
        if ("VICTORY_POINT".equals(action)) {
            // cannot be played
            return false;
        }

        // remove the card from the player's hand (played)
        boolean removed = player.removeCard(devCardConfig.id);
        if (!removed)
            return false;

        // dispatch to the appropriate effect handler
        boolean success = false;
        switch (action) {
            case "ECO_CONFERENCE" -> success = true;
            case "HIGHWAY_MADNESS" -> success = true;
            case "TRADING_FRENZY" -> success = true;
            case "MONOPOLY" -> success = true;
            default -> {
                break;
            }
        }

        return success;
    }

    public int getPlayerVictoryPoints(int playerId) {
        Player p = getPlayer(playerId);
        if (p == null) return 0;

        int points = p.getVictoryPoints(playerId);
        points += p.getHiddenVictoryPoints();
        return points;
    }

    // build two free roads
    public boolean applyHighwayMadness(int playerId, int edgeIndexA, int edgeIndexB) {
        boolean successA = roads.buildRoad(edgeIndexA, playerId);
        boolean successB = roads.buildRoad(edgeIndexB, playerId);
        increaseClimateAndDistributeDisasterCards();
        return successA && successB;
    }

    // take any three resource cards from the bank
    public boolean applyTradingFrenzy(int playerId, List<ResourceConfig> resources) {
        Player player = getPlayer(playerId);
        if (player == null || resources == null) return false;

        for (ResourceConfig r : resources) {
            if (r == null) continue;
            bankCards.giveResourceCard(r, 1);
            player.changeResourceCount(r, +1);
        }

        increaseClimateAndDistributeDisasterCards();

        return true;
    }

    // choose a resource and every player gives you all their cards of that resource
    public boolean applyMonopoly(int playerId, ResourceConfig resource) {
        if (resource == null) return false;

        int totalCollected = 0;
        for (Player other : players) {
            if (other.getId() == playerId) continue;
            int amt = other.getResourceCount(resource);
            if (amt <= 0) continue;
            boolean removed = other.changeResourceCount(resource, -amt);
            if (removed) {
                totalCollected += amt;
            }
        }

        if (totalCollected > 0) {
            Player player = getPlayer(playerId);
            player.changeResourceCount(resource, totalCollected);
        }

        increaseClimateAndDistributeDisasterCards();

        return totalCollected > 0;
    }

    public void rollDice() {
        int diceRoll = dice.roll();
        giveResourcesToPlayers(diceRoll);
    }

    public int getDice1() {
        return dice.getDie1();
    }

    public int getDice2() {
        return dice.getDie2();
    }

    public Map<ResourceConfig, Integer> getBankResources() {
        Collection<ResourceConfig> allResources = ConfigService.getAllResources();
        Map<ResourceConfig, Integer> bankResources = new HashMap<>();
        for (ResourceConfig resource : allResources) {
            bankResources.put(resource, bankCards.getResourceCount(resource));
        }
        return bankResources;
    }

    public ArrayList<PortConfig> getPlayerPorts(int playerId) {
        Player player = getPlayer(playerId);
        if (player == null)
            return new ArrayList<>();
        ArrayList<Integer> portNumbers = settlements.getPortsOwnedByPlayer(playerId);
        return ports.getPortConfigsByPortNumbers(portNumbers);
    }

    public ArrayList<DevCardConfig> getPlayerDevCards(int playerId) {
            Player player = getPlayer(playerId);
        if (player == null)
            return new ArrayList<>();
        return player.getDevCards();
    }

    public ClimateTracker getClimateTracker() {
        return climateTracker;
    }

    public boolean playerCanRepairAnyTile(int playerId) {
        boolean canRepairSomeTile = false;
        for (int i = 0; i < tiles.getTiles().length; i++) {
            if (playerCanRepairTile(playerId, i)) {
                canRepairSomeTile = true;
                break;
            }
        }
        return canRepairSomeTile;
    }

    public boolean playerCanRepairTile(int playerId, int tileIndex) {
        Player player = getPlayer(playerId);
        Tile tile = tiles.getTiles()[tileIndex];
        if (player == null || tile == null || !tile.getIsDestroyed()) {
            return false;
        }

        String structureId = tile.getTileID().replace("tile.", "player_infrastructure.") + "_tile";
        PlayerInfrastructureConfig cfg = ConfigService.getInfrastructure(structureId);
        if (cfg == null || cfg.constructionCosts.isEmpty()) {
            return false; // no configured cost for this tile
        }

        return player.hasEnoughResourcesForStructure(structureId);
    }

    // TESTING METHODS
    public void giveSettlementResources(int playerID) {
        Player player = getPlayer(playerID);
        PlayerInfrastructureConfig config = ConfigService.getInfrastructure("player_infrastructure.settlement");
        for (String resourceID : config.constructionCosts.keySet()) {
            ResourceConfig resourceConfig = ConfigService.getResource(resourceID);
            int amount = config.constructionCosts.get(resourceID);
            player.changeResourceCount(resourceConfig, amount);
        }
    }

    // TESTING METHOD
    public void giveCityResources(int playerID) {
        Player player = getPlayer(playerID);
        PlayerInfrastructureConfig config = ConfigService.getInfrastructure("player_infrastructure.city");
        for (String resourceID : config.constructionCosts.keySet()) {
            ResourceConfig resourceConfig = ConfigService.getResource(resourceID);
            int amount = config.constructionCosts.get(resourceID);
            player.changeResourceCount(resourceConfig, amount);
        }
    }

    // TESTING METHOD
    public void giveRoadResources(int playerID) {
        Player player = getPlayer(playerID);
        PlayerInfrastructureConfig config = ConfigService.getInfrastructure("player_infrastructure.road");
        for (String resourceID : config.constructionCosts.keySet()) {
            ResourceConfig resourceConfig = ConfigService.getResource(resourceID);
            int amount = config.constructionCosts.get(resourceID);
            player.changeResourceCount(resourceConfig, amount);
        }
    }

}
