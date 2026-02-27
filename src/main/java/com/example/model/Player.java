package com.example.model;

import java.util.ArrayList;
import java.util.HashMap;

import com.example.model.config.DevCardConfig;
import com.example.model.config.PlayerInfrastructureConfig;
import com.example.model.config.ResourceConfig;
import com.example.model.config.registry.ResourceRegistry;
import com.example.model.config.service.ConfigService;

/**
 * Player Class; stores per player info
 * 
 * @author 40452739
 */
public class Player {
    private static int nextId = 1;

    private int id;
    private String name;

    // This data structures should be changed as necessary
    private HashMap<ResourceConfig, Integer> resources;
    private ArrayList<String> devCards;
    private HashMap<String, Integer> structuresRemaining;

    private int tilesRestored;

    private int victoryPoints;
    private int hiddenVictoryPoints;

    /**
     * Player Class Constructor
     * 
     * @param name name of the player
     */
    public Player(String name) { // Maybe pass in the starting structuresRemaining?

        this.id = Player.nextId++;

        this.name = (name != null ? name : "");

        this.resources = new HashMap<>();
        for (ResourceConfig t : ResourceRegistry.getInstance().all()) {
            this.resources.put(t, 0);
        }

        this.devCards = new ArrayList<>();

        // Replace with global version
        ArrayList<String> structureTypes = ConfigService.getAllInfrastructureIDs();

        this.structuresRemaining = new HashMap<>();
        for (int i = 0, n = structureTypes.size(); i < n; i++) {
            int startingCount = ConfigService.getInfrastructure(structureTypes.get(i)).maxQuantity;
            this.structuresRemaining.put(structureTypes.get(i), startingCount);
        }

        tilesRestored = 0;
        victoryPoints = 0;
        hiddenVictoryPoints = 0;
    }

    /**
     * Getter for player object's id
     * 
     * @return Player.id
     */
    public int getId() {
        return this.id;
    }

    /**
     * Getter for name
     * 
     * @return Player.name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Setter for name
     * 
     * @param newName the new name of the player
     * @return success of the operation
     */
    public boolean setName(String newName) {
        if (newName == null) {
            return false; // must be set to a value
        }
        this.name = newName;
        return true;
    }

    /**
     * Getter for resource count
     * 
     * @param type the type being set
     * @return resource count
     */
    public int getResourceCount(ResourceConfig type) {
        if (this.resources.containsKey(type)) {
            return this.resources.get(type);
        } else {
            return 0;
        }
    }

    /**
     * Getter for total number of resources
     * 
     * @return the total number of resources
     */
    public int getTotalResources() {
        int total = 0;

        for (ResourceConfig type : this.resources.keySet()) {
            total += this.resources.get(type);
        }

        return total;
    }

    /**
     * Set for a type of resource
     * 
     * @param type  type of resource to be set
     * @param count number of this resource
     * @return success of the operation
     */
    public boolean setResourceCount(ResourceConfig type, int count) {
        if (this.resources.containsKey(type)) {
            this.resources.put(type, count);
            return true;
        }

        return false;
    }

    /**
     * Change a given resource by an amount
     * 
     * @param type   type of resource to change
     * @param change the amount of change; can be negative or positive
     * @return success of the operation
     */
    public boolean changeResourceCount(ResourceConfig type, int change) {
        if (!this.resources.containsKey(type)) {
            return false;
        }

        int newCount = this.getResourceCount(type) + change;
        if (newCount < 0) {
            return false;
        }

        this.resources.put(type, newCount);

        return true;
    }

    /**
     * Get a card at a given index (indices do not change)
     * 
     * @param index index of the card
     * @return the card
     */
    public String getCard(int index) {
        if (index >= 0 && index < this.devCards.size()) {
            return this.devCards.get(index);
        }

        return "";
    }

    /**
     * Adds a card to the players hand
     * 
     * @param card card to be added
     * @return success of the operation
     */
    public boolean addCard(String card) {
        if (card == null) {
            return false;
        }

        this.devCards.add(card);

        return true;
    }

    /**
     * Remove the first occurrence of a development card from the player's hand.
     * 
     * @return true if a card was removed
     */
    public boolean removeCard(String card) {
        return this.devCards.remove(card);
    }

    public int getVictoryPoints(int playerId) {
        return this.victoryPoints;
    }

    /**
     * Checks whether a given card type is stored
     * 
     * @param type type of the card
     * @return whether the card is owned at least once
     */
    public boolean hasCard(String type) {
        for (String s : this.devCards) {
            if (s.equals(type)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the amount of devCards of a given type is owned by the player
     * 
     * @param type type of the card being searched
     * @return the number of devCards of the type
     */
    public int countCards(String type) {

        int count = 0;

        for (String s : this.devCards) {
            if (s.equals(type)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Getter for the total number of devCards
     * 
     * @return the total number of devCards
     */
    public int numberOfCards() {
        return this.devCards.size();
    }

    /**
     * Check whether this player has depleted their structuresRemaining list
     * 
     * @param type the type of structure that is being checked
     * @return whether this player has depleted this structure type
     */
    public boolean depletedStructures(String type) {
        if (this.structuresRemaining.containsKey(type)) {
            return this.structuresRemaining.get(type) < 1;
        }
        return true;
    }

    /**
     * Gets how many structures of a given type are left
     * 
     * @param type type of structure
     * @return number left
     */
    public int getStructuresRemaining(String type) {
        if (this.structuresRemaining.containsKey(type)) {
            return this.structuresRemaining.get(type);
        }
        return 0;
    }

    /**
     * Change the number of stuctures of a type remaining
     * 
     * @param type   type of structure
     * @param change the amount its changing; positive or negative
     * @return success of the operation
     */
    public boolean changeStructuresRemainingByType(String type, int change) {
        if (!this.structuresRemaining.containsKey(type)) {
            return false;
        }

        int newCount = this.getStructuresRemaining(type) + change;
        if (newCount < 0) {
            return false;
        }
        this.structuresRemaining.put(type, newCount);
        return true;
    }

    /**
     * Set a number of remaining structures for a given type
     * 
     * @param type  type of the structure
     * @param count the amount of structures left for the player
     * @return
     */
    public boolean setStructuresRemainingByType(String type, int count) {
        if (this.structuresRemaining.containsKey(type)) {
            this.structuresRemaining.put(type, count);
            return true;
        }

        return false;
    }

    /**
     * Setter for the structuresRemaining HashMap; probably not used
     * 
     * @param newStructures the new structuresRemaining hash map
     * @return success of the operation
     */
    public boolean replaceStructuresRemaining(HashMap<String, Integer> newStructures) {
        if (newStructures != null) {
            this.structuresRemaining = newStructures;
            return true;
        }

        return false;
    }

    /**
     * Fully empties the structuresRemaining; sets them all to 0
     */
    public void emptyStructuresRemaining() {
        for (String key : this.structuresRemaining.keySet()) {
            this.structuresRemaining.put(key, 0);
        }
    }

    public int getTilesRestored() {
        return this.tilesRestored;
    }

    public void increaseTilesRestored(){
        this.tilesRestored++;
    }

    /**
     * Creates a printable string version of the Player
     * 
     * @return the string
     */
    @Override
    public String toString() {
        return "Player { id=" + this.id + ", name=" + this.name + ", resources=" + this.resources + ", devCards="
                + this.devCards + ", hiddenVP=" + this.hiddenVictoryPoints + ", structuresRemaining="
                + this.structuresRemaining + " }";
    }

    public boolean hasEnoughResourcesForStructure(String structureType) {
        PlayerInfrastructureConfig structureConfig = ConfigService.getInfrastructure(structureType);
        for (String resource : structureConfig.constructionCosts.keySet()) {
            ResourceConfig resourceConfig = ConfigService.getResource(resource);
            int cost = structureConfig.constructionCosts.get(resource);
            if (this.getResourceCount(resourceConfig) < cost) {
                return false;
            }
        }
        return true;
    }

    public boolean deductStructureResources(String structureType) {
        boolean success = true;
        PlayerInfrastructureConfig structureConfig = ConfigService.getInfrastructure(structureType);
        // System.out.println("Deducting resources for structure: " + structureType);
        for (String resource : structureConfig.constructionCosts.keySet()) {
            ResourceConfig resourceConfig = ConfigService.getResource(resource);
            int cost = structureConfig.constructionCosts.get(resource);
            // System.out.println(" - " + cost + " of " + resource);
            // System.out.println(" Current amount: " +
            // this.getResourceCount(resourceConfig));
            success = success && this.changeResourceCount(resourceConfig, -cost);
        }
        return success;
    }

    public ArrayList<Integer> getResourceCountsList() {
        ArrayList<Integer> counts = new ArrayList<>();
        for (ResourceConfig resource : ResourceRegistry.getInstance().all()) {
            System.out.println("Getting resource count for: " + resource.id);
            counts.add(this.getResourceCount(resource));
        }
        return counts;
    }

    public HashMap<ResourceConfig, Integer> getResourcesMap() {
        return this.resources;
    }

    public int getTotalVictoryPoints() {
        return victoryPoints + hiddenVictoryPoints;
    }

    public int getKnownVictoryPoints() {
        return victoryPoints;
    }

    public int getHiddenVictoryPoints() {
        return hiddenVictoryPoints;
    }

    public void changeVictoryPoints(int amount) {
        victoryPoints += amount;
    }

    public void changeHiddenVictoryPoints(int amount) {
        hiddenVictoryPoints += amount;
    }

    public ResourceConfig stealRandomResource() {
        ArrayList<ResourceConfig> ownedResources = new ArrayList<>();
        for (ResourceConfig resource : this.resources.keySet()) {
            if (this.getResourceCount(resource) > 0) {
                ownedResources.add(resource);
            }
        }

        if (ownedResources.isEmpty()) {
            return null; // No resources to steal
        }

        int randomIndex = (int) (Math.random() * ownedResources.size());
        ResourceConfig stolenResource = ownedResources.get(randomIndex);
        this.changeResourceCount(stolenResource, -1); // Remove one of the stolen resource
        return stolenResource;
    }

    public ArrayList<DevCardConfig> getDevCards() {
        ArrayList<DevCardConfig> devCardConfigs = new ArrayList<>();
        for (String cardId : this.devCards) {
            DevCardConfig cardConfig = ConfigService.getDevCard(cardId);
            if (cardConfig != null) {
                devCardConfigs.add(cardConfig);
            }
        }
        return devCardConfigs;

    }
}
