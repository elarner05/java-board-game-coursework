package com.example.model;

import java.util.ArrayList;
import java.util.Collection;
import static java.util.Collections.shuffle;
import java.util.HashMap;
import java.util.Map;

import com.example.model.config.DevCardConfig;
import com.example.model.config.DisasterCardConfig;
import com.example.model.config.ResourceConfig;
import com.example.model.config.service.ConfigService;

public class BankCards {

    //store how many of each resource card are left and how many dev cards
    private Map<String, Integer> resourceCards;
    private ArrayList<String> developmentCards;
    private ArrayList<String> disasterCards;

    //constructor
    //each resource card starts with 19
    //dev cards start at 25
    public BankCards() {
        initilizeResourceCards();
        initilizeDevelopmentCards();
        initilizeDisasterCards();
        shuffle(developmentCards);
        shuffle(disasterCards);
    }

    private void initilizeResourceCards() {
        resourceCards = new HashMap<>();
        Collection<ResourceConfig> resources= ConfigService.getAllResources();
        for (ResourceConfig resource : resources) {
            resourceCards.put(resource.id, resource.maxQuantity);
        }
    }

    private void initilizeDevelopmentCards() {
        developmentCards = new ArrayList<String>();
        Collection<DevCardConfig> devCards = ConfigService.getAllDevCards();
        for (DevCardConfig devCard : devCards) {
            for (int i = 0; i < devCard.count; i++) {
                developmentCards.add(devCard.id);
            }
        }
    }

    private void initilizeDisasterCards() {
        disasterCards = new ArrayList<String>();
        Collection<DisasterCardConfig> disasterCardsConfig = ConfigService.getAllDisasterCards();
        for (DisasterCardConfig disasterCard : disasterCardsConfig) {
            for (int i = 0; i < disasterCard.count; i++) {
                disasterCards.add(disasterCard.tileAffected);
            }
        }
    }

    //get how many cards of a type are left
    public int getResourceCount(ResourceConfig resource) {
        return resourceCards.getOrDefault(resource.id, 0);
    }

    //gives cards to player from bank
    //returns true if there are enough cards and reduces card count
    //returns false if there are not enough cards in deck
    public boolean giveResourceCard(ResourceConfig resource, int amount) {
        int current = getResourceCount(resource);
        if (current >= amount) {
            resourceCards.put(resource.id, current - amount);
            return true;
        }
        return false;
    }

    //returns empty string if deck is empty
    public String giveDevelopmentCard() {
        if (!developmentCards.isEmpty()) {
            String devCard = developmentCards.get(developmentCards.size()-1);
            developmentCards.remove(developmentCards.size()-1);
            return devCard;
        }
        return "";
    }

    //returns empty string if deck is empty
    public String giveDisasterCard() {
        if (!disasterCards.isEmpty()) {
            String disCard = disasterCards.get(disasterCards.size()-1);
            disasterCards.remove(disasterCards.size()-1);
            return disCard;
        }
        return "";
    }

    //receive cards from player and adds them to the bank
    public void returnResourceCard(ResourceConfig resource, int amount) {
        int current = getResourceCount(resource);
        resourceCards.put(resource.id, current + amount);
    }

    //print bank status
    public void printStatus() {
        Collection<ResourceConfig> resources= ConfigService.getAllResources();
        for (ResourceConfig resource : resources) {
            System.out.println(resource.id + ": " + getResourceCount(resource));
        }
        System.out.println("Development cards: " + developmentCards);
    }

    public boolean hasDevelopmentCards() {
        return !developmentCards.isEmpty();
    }

}
