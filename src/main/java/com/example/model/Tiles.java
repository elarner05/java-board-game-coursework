package com.example.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.example.model.config.service.ConfigService;

public class Tiles {

    private Tile[] tiles;

    // Board size, change if a bigger/smaller board is desired
    // 19 is a normal 3 hex-per-side board
    public static final int NUMBER_OF_HEXES = 19;

    public Tiles() {
        this.tiles = setUpTiles();
    }

    // getter
    public Tile[] getTiles() {
        return this.tiles;
    }

    // setter
    public void setTiles(Tile[] _tiles) {
        this.tiles = _tiles;
    }

    // set up all the tiles in the gameboard
    private Tile[] setUpTiles() {
        Tile[] tiles = new Tile[NUMBER_OF_HEXES];

        // Instantiate tiles
        for (int i = 0; i < NUMBER_OF_HEXES; i++) {
            tiles[i] = new Tile();
        }

        // Build the bag of tile IDs with correct quantities
        ArrayList<String> tileBag = new ArrayList<>();
        for (String tileID : ConfigService.getAllTileIDs()) {
            int maxQty = ConfigService.getTile(tileID).maxQuantity;
            for (int j = 0; j < maxQty; j++) {
                tileBag.add(tileID);
            }
        }

        // Shuffle the bag
        java.util.Collections.shuffle(tileBag);

        int desertIndex = 0;

        // Assign shuffled tiles to the tile array
        for (int i = 0; i < NUMBER_OF_HEXES; i++) {
    
            tiles[i].setTileID(tileBag.get(i));
            
            String resourceID = ConfigService.getTile(tiles[i].getTileID()).resourceID;
            if (resourceID.isEmpty()) {
                desertIndex = i;
            }
        }

        // Assign numbers and block deserts
        int[] tokens = getTokens();
        int[] numberSequence = generateValidLayout(tokens, desertIndex); // should have 19 numbers, 0 for deserts
        int numberIndex = 0;

        for (int i = 0; i < NUMBER_OF_HEXES; i++) {
            Tile tile = tiles[i];
            String resourceID = ConfigService.getTile(tile.getTileID()).resourceID;

            if (resourceID.isEmpty()) { // desert
                tile.setIsBlocked(true);
                tile.setNumber(0); // desert has no number
            } else {
                tile.setIsBlocked(false);
                tile.setIsDestroyed(false);
                tile.setNumber(numberSequence[numberIndex]);
                numberIndex++;
            }
        }

        // Set adjacency info
        return setAdjVerticesForEachTile(tiles);
    }

    private int[] getTokens() {
        Map<Integer, Integer> numberTokens = ConfigService.getNumberTokens();
        int NUMBER_OF_TOKENS = numberTokens.values().stream().mapToInt(Integer::intValue).sum();
        int[] sequence = new int[NUMBER_OF_TOKENS];

        int sizeOfSequence = 0;
        for (int key : numberTokens.keySet()) {
            int occurences = numberTokens.get(key);
            for (int i = 0; i < occurences; i++) {
                sequence[sizeOfSequence] = key;
                sizeOfSequence++;
            }
        }

        return sequence;
    }

    private static final int MAX_ATTEMPTS = 30000000;

    private static int[] generateValidLayout(int[] tokens, int desertIndex) {
        Random rand = new Random();

        // Convert int[] → mutable List<Integer>
        List<Integer> layout = new ArrayList<>();
        for (int token : tokens) {
            layout.add(token);
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Collections.shuffle(layout, rand);
            layout.remove(Integer.valueOf(-1)); // remove desert placeholder
            layout.add(desertIndex, -1); // add desert back in the correct spot

            if (isValidLayout(layout)) {
                layout.remove(Integer.valueOf(-1)); // remove desert placeholder
                return layout.stream().mapToInt(Integer::intValue).toArray();
            }
        }

        throw new RuntimeException(
                "Couldn't find a valid layout after " + MAX_ATTEMPTS + " attempts");
    }

    private static boolean isValidLayout(List<Integer> layout) {
        for (int i = 0; i < layout.size(); i++) {
            if (layout.get(i) == 6 || layout.get(i) == 8) {
                for (int adj : AdjacencyMaps.TileAdjacency[i]) {
                    if (layout.get(adj) == 6 || layout.get(adj) == 8) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private Tile[] setAdjVerticesForEachTile(Tile[] tiles) {
        for (int i = 0; i < AdjacencyMaps.TileVertices.length; i++) {
            tiles[i].setAdjVertices(AdjacencyMaps.TileVertices[i]);
        }

        return tiles;
    }

    //need to make sure its not destroying the same tile every time
    public boolean destroyTile(String tileID){
        ArrayList<Tile> canDestroy = new ArrayList<>();
        for (Tile tile : tiles) {
            if (tile.getTileID().equals(tileID) && !tile.getIsDestroyed()) {
                canDestroy.add(tile);
            }
        }
        if (!canDestroy.isEmpty()) {
            Random rand = new Random();
            int index = rand.nextInt(canDestroy.size());
            canDestroy.get(index).setIsDestroyed(true);
            return true;
        }
        return false;
    }

    //fix to be tile index instead of id
    public boolean restoreTile(int tileIndex){
        if (tileIndex >= 0 && tileIndex < tiles.length && tiles[tileIndex].getIsDestroyed()) {
            tiles[tileIndex].setIsDestroyed(false);
            return true;
        }
        return false;
    }



    //Find tiles based off of a diceroll
    public ArrayList<Tile> GetTilesFromDiceroll(int diceroll){
        ArrayList<Tile> tilesWithDiceroll = new ArrayList<Tile>();
        Tile[] allTiles = getTiles();

        //check if each tile has diceroll value (And isn't blocked)
        for (int i = 0; i < allTiles.length; i++){
            if (allTiles[i].getNumber() == diceroll){
                //tile has the desired value
                if (!allTiles[i].getIsBlocked() && !allTiles[i].getIsDestroyed()){
                    tilesWithDiceroll.add(allTiles[i]);
                }
            }
        }

        return tilesWithDiceroll;
    }

    
    //Find robber tile and change it to the new robber tile
    public void changeBlockedTile(int destinationTileIndex){
        for (Tile tile : tiles){
            if (tile.getIsBlocked()){
                tile.setIsBlocked(false);
                break;
            }
        }

        tiles[destinationTileIndex].setIsBlocked(true);
        System.out.println("Robber moved to tile " + destinationTileIndex);
    }

    public int getBlockedTileIndex() {
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i].getIsBlocked()) {
                return i;
            }
        }
        return -1; // No blocked tile found
    }

    public void repareTiles() {
        for (Tile tile : tiles) {
            tile.setIsDestroyed(false);
        }
    }

    
}