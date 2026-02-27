package com.example.viewmodel.viewstates;

import com.example.model.config.DevCardConfig;
import com.example.model.config.PortConfig;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

public class PlayerViewState {
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty knownScore = new SimpleIntegerProperty();
    private final IntegerProperty realScore = new SimpleIntegerProperty();
    private final BooleanProperty longestRoad = new SimpleBooleanProperty();
    private final BooleanProperty cleanestEnvironment = new SimpleBooleanProperty();
    private final BooleanProperty canBuildSettlement = new SimpleBooleanProperty();
    private final BooleanProperty canBuildCity = new SimpleBooleanProperty();
    private final BooleanProperty canBuildRoad = new SimpleBooleanProperty();
    private final BooleanProperty canBuildDevCard = new SimpleBooleanProperty();
    private final BooleanProperty canRepairTile = new SimpleBooleanProperty();

    private final ObjectProperty<Color> color = new SimpleObjectProperty<>();

    private final ObservableList<ResourceViewState> resources = FXCollections.observableArrayList();
    private final ObservableList<PortConfig> ports = FXCollections.observableArrayList();
    private final ObservableList<DevCardConfig> devCards = FXCollections.observableArrayList();

    public StringProperty nameProperty() {
        return name;
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public IntegerProperty knownScoreProperty() {
        return knownScore;
    }

    public IntegerProperty realScoreProperty() {
        return realScore;
    }

    public BooleanProperty longestRoadProperty() {
        return longestRoad;
    }

    public BooleanProperty cleanestEnvironmentProperty() {
        return cleanestEnvironment;
    }

    public BooleanProperty canBuildSettlementProperty() {
        return canBuildSettlement;
    }

    public BooleanProperty canBuildCityProperty() {
        return canBuildCity;
    }

    public BooleanProperty canBuildRoadProperty() {
        return canBuildRoad;
    }

    public BooleanProperty canBuildDevCardProperty() {
        return canBuildDevCard;
    }

    public BooleanProperty canRepairTileProperty() {
        return canRepairTile;
    }

    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    public ObservableList<ResourceViewState> getResources() {
        return resources;
    }

    public ObservableList<PortConfig> getPorts() {
        return ports;
    }

    public ObservableList<DevCardConfig> getDevCards() {
        return devCards;
    }

    public IntegerBinding devCardCountBinding() {
        return Bindings.size(devCards);
    }
}