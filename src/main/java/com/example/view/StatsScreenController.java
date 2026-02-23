package com.example.view;

import java.util.ArrayList;
import java.util.List;

import com.example.view.components.Hex;
import com.example.viewmodel.StatsViewModel;

import javafx.fxml.FXML;

import javafx.animation.AnimationTimer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class StatsScreenController implements ViewModelAware<StatsViewModel> {

    private StatsViewModel viewModel;

    @FXML
    private Canvas hexCanvas;

    @FXML
    private HBox p1Row, p2Row, p3Row, p4Row;

    @FXML
    private Label p1NameLabel, p2NameLabel, p3NameLabel, p4NameLabel;
    @FXML
    private Label p1VpLabel, p2VpLabel, p3VpLabel, p4VpLabel;
    @FXML
    private Label p1ResLabel, p2ResLabel, p3ResLabel, p4ResLabel;
    @FXML
    private Label p1DevLabel, p2DevLabel, p3DevLabel, p4DevLabel;

    @FXML
    private Label p1LongestRoadLabel, p2LongestRoadLabel, p3LongestRoadLabel, p4LongestRoadLabel;
    @FXML
    private Label p1LargestArmyLabel, p2LargestArmyLabel, p3LargestArmyLabel, p4LargestArmyLabel;
    @FXML
    private Label p1WinnerLabel, p2WinnerLabel, p3WinnerLabel, p4WinnerLabel;

    private double r = 75; // hex radius
    private final List<Hex> hexes = new ArrayList<>();
    private WritableImage staticBackground;

    @Override
    public void setViewModel(StatsViewModel viewModel) {
        this.viewModel = viewModel;
        bindViewModel();
    }

    // 
    private void bindViewModel() {
        // TODO bindings fix after 
        viewModel.loadFromGame();


        p1NameLabel.textProperty().bind(viewModel.getPlayer1Name());
        p2NameLabel.textProperty().bind(viewModel.getPlayer2Name());
        p3NameLabel.textProperty().bind(viewModel.getPlayer3Name());
        p4NameLabel.textProperty().bind(viewModel.getPlayer4Name());


        p1VpLabel.textProperty().bind(viewModel.getPlayer1VictoryPoints().asString());
        p2VpLabel.textProperty().bind(viewModel.getPlayer2VictoryPoints().asString());
        p3VpLabel.textProperty().bind(viewModel.getPlayer3VictoryPoints().asString());
        p4VpLabel.textProperty().bind(viewModel.getPlayer4VictoryPoints().asString());

        p1ResLabel.textProperty().bind(viewModel.getPlayer1TotalResources().asString());
        p2ResLabel.textProperty().bind(viewModel.getPlayer2TotalResources().asString());
        p3ResLabel.textProperty().bind(viewModel.getPlayer3TotalResources().asString());
        p4ResLabel.textProperty().bind(viewModel.getPlayer4TotalResources().asString());



        p1DevLabel.textProperty().bind(viewModel.getPlayer1DevCards().asString());
        p2DevLabel.textProperty().bind(viewModel.getPlayer2DevCards().asString());
        p3DevLabel.textProperty().bind(viewModel.getPlayer3DevCards().asString());
        p4DevLabel.textProperty().bind(viewModel.getPlayer4DevCards().asString());


        // Badges - basically the trophies for longest road, winner, or army ( or cleanest enviorment)

        bindBadge(p1LongestRoadLabel, viewModel.getPlayer1LongestRoad());
        bindBadge(p2LongestRoadLabel, viewModel.getPlayer2LongestRoad());
        bindBadge(p3LongestRoadLabel, viewModel.getPlayer3LongestRoad());
        bindBadge(p4LongestRoadLabel, viewModel.getPlayer4LongestRoad());

        bindBadge(p1LargestArmyLabel, viewModel.getPlayer1LargestArmy());
        bindBadge(p2LargestArmyLabel, viewModel.getPlayer2LargestArmy());
        bindBadge(p3LargestArmyLabel, viewModel.getPlayer3LargestArmy());
        bindBadge(p4LargestArmyLabel, viewModel.getPlayer4LargestArmy());

        bindBadge(p1WinnerLabel, viewModel.getPlayer1Winner());
        bindBadge(p2WinnerLabel, viewModel.getPlayer2Winner());
        bindBadge(p3WinnerLabel, viewModel.getPlayer3Winner());
        bindBadge(p4WinnerLabel, viewModel.getPlayer4Winner());

        // Hide row if 3 players
        p1Row.visibleProperty().bind(viewModel.getPlayer1Present());
        p1Row.managedProperty().bind(p1Row.visibleProperty());

        p2Row.visibleProperty().bind(viewModel.getPlayer2Present());
        p2Row.managedProperty().bind(p2Row.visibleProperty());

        p3Row.visibleProperty().bind(viewModel.getPlayer3Present());
        p3Row.managedProperty().bind(p3Row.visibleProperty());

        p4Row.visibleProperty().bind(viewModel.getPlayer4Present());
        p4Row.managedProperty().bind(p4Row.visibleProperty());
    }

    private void bindBadge(Label label, javafx.beans.property.BooleanProperty show) {
        label.visibleProperty().bind(show);
        label.managedProperty().bind(label.visibleProperty());
    }

    @FXML
    public void initialize() {
        createHexGrid();
        drawStaticBackground();
        startHexSpiralAnimation();
    }

    // Setip hex grid
    private void createHexGrid() {
        double width = hexCanvas.getWidth();
        double height = hexCanvas.getHeight();

        double hexWidth = Math.sqrt(3) * r;
        double hexHeight = 2 * r;
        double horizSpacing = hexWidth;
        double vertSpacing = hexHeight;

        int cols = (int) Math.ceil(width / horizSpacing) + 2;
        int rows = (int) Math.ceil(height / vertSpacing) + 2;

        double centerX = width / 2;
        double centerY = height / 2;

        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                double x = col * horizSpacing;
                double y = row * vertSpacing;
                if (col % 2 == 1) {
                    y += vertSpacing / 2;
                }

                hexes.add(new Hex(x, y, r, centerX, centerY));
            }
        }
    }

    
    private void drawStaticBackground() {
        double width = hexCanvas.getWidth();
        double height = hexCanvas.getHeight();

        staticBackground = new WritableImage((int) width, (int) height);
        GraphicsContext tempGC = new Canvas(width, height).getGraphicsContext2D();

        tempGC.setFill(Color.BLACK);
        tempGC.fillRect(0, 0, width, height);

        tempGC.setStroke(Color.web("#444"));
        tempGC.setLineWidth(3);
        for (Hex hex : hexes) {
            tempGC.strokePolygon(hex.xPoints, hex.yPoints, 6);
        }

        tempGC.getCanvas().snapshot(null, staticBackground);
    }

    private void startHexSpiralAnimation() {
        GraphicsContext gc = hexCanvas.getGraphicsContext2D();
        final long startNanoTime = System.nanoTime();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double t = (now - startNanoTime) / 3e9;

                gc.drawImage(staticBackground, 0, 0);

                for (Hex hex : hexes) {
                    double opacity = 0.6 + 0.4 * Math.sin(hex.distanceToCenter / 80 - t * 0.5);
                    opacity = Math.max(0, Math.min(1, opacity));
                    gc.setFill(Color.rgb(85, 85, 85, opacity));
                    gc.fillPolygon(hex.xPoints, hex.yPoints, 6);
                }
            }
        }.start();
    }
}
