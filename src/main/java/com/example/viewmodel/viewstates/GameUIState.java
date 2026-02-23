package com.example.viewmodel.viewstates;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class GameUIState {
    public static final BooleanProperty popupVisible =
            new SimpleBooleanProperty(false);
    public static final BooleanProperty tradingMenuVisible =
            new SimpleBooleanProperty(false);
    public static final BooleanProperty selectResourceMenuVisible =
            new SimpleBooleanProperty(false);
}
