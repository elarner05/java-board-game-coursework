package com.example.viewmodel.viewstates;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public final class TileViewState {
    public final IntegerProperty number = new SimpleIntegerProperty();
    public final ObjectProperty<String> resource = new SimpleObjectProperty<>();
    public final BooleanProperty blocked = new SimpleBooleanProperty();
    public final BooleanProperty destroyed = new SimpleBooleanProperty();
}
