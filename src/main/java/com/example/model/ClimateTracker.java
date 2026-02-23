package com.example.model;

public class ClimateTracker {

    private int climateLevel;
    private int nextDisasterCard;
    private boolean giveDisasterCard;

    public ClimateTracker() {
        this.climateLevel = 0;
        this.nextDisasterCard = 5;
        this.giveDisasterCard = false;
    }

    public int getClimateLevel() { return climateLevel; }

    public void increaseClimate() {
        climateLevel++;
        if (climateLevel >= nextDisasterCard) {
            giveDisasterCard = true;
            nextDisasterCard += 5;
        }
    }

    public boolean shouldGiveDisasterCard() {
        if (giveDisasterCard) {
            giveDisasterCard = false;
            return true;
        }
        return false;
    }

    public int disasterCardNum() {
        int totalCards = 0;
        if (nextDisasterCard > 5) {
            totalCards++;
        }
        if (nextDisasterCard > 10) {
            totalCards++;
        }
        if (nextDisasterCard > 15) {
            totalCards++;
        }
        return totalCards;
    }

    public void resetClimateLevels() {
        climateLevel = 0;
        nextDisasterCard = 5;
        giveDisasterCard = false;
    }
    
}
