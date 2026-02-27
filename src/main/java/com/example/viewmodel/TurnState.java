package com.example.viewmodel;

public enum TurnState {

    DICE_ROLL("hint.turn.roll_dice"),
    TRADE("hint.turn.trade"),
    BUILD("hint.turn.build"),
    BUILD_SETTLEMENT("hint.turn.build_settlement"),
    BUILD_ROAD("hint.turn.build_road"),
    BUILD_CITY("hint.turn.build_city"),
    MOVE_ROBBER_STATE("hint.turn.move_robber"),
    PLAY_DEV_CARD("hint.turn.play_dev_card"),
    ECO_CONFERENCE("hint.turn.eco_conference"),
    HIGHWAY_MADNESS("hint.turn.highway_madness"),
    TRADE_FRENZY("hint.turn.trade_frenzy"),
    MONOPOLY("hint.turn.monopoly"),
    STEAL_RESOURCE("hint.turn.steal_resource"),
    REPAIR_TILE("hint.turn.repair_tile");

    private final String hintKey;

    TurnState(String hintKey) {
        this.hintKey = hintKey;
    }

    public String getHintKey() {
        return hintKey;
    }
}
