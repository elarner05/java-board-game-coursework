package com.example.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.model.config.LangManager;
import com.example.view.components.Hex;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;
import javafx.scene.layout.HBox;
import javafx.scene.input.MouseEvent;

import com.example.viewmodel.TitleViewModel;

public class TitleScreenController implements ViewModelAware<TitleViewModel> {

    private TitleViewModel viewModel;
    @FXML
    private Polygon playHex, settingsHex, exitHex;
    @FXML
    private Label playText, settingsText, exitText;
    @FXML
    private HBox playHBox, settingsHBox, exitHBox;
    @FXML
    private Canvas hexCanvas;
    private double r = 75; // hex radius
    private final List<Hex> hexes = new ArrayList<>();
    private WritableImage staticBackground;

    @Override
    public void setViewModel(TitleViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        // Draw the shifting hex background
        createHexGrid();
        drawStaticBackground();
        startHexSpiralAnimation();

        // Add Gradual Scaling to all Menu Options
            // Play
        applyHoverScaling(playHex);
        applyHoverScaling(playText);
            // Settings
        applyHoverScaling(settingsHex);
        applyHoverScaling(settingsText);
            // Exit
        applyHoverScaling(exitHex);
        applyHoverScaling(exitText);

        // Tie Hovering to All Elements in HBoxes
        setupHoverAnimation(playHBox, playHex);
        setupHoverAnimation(settingsHBox, settingsHex);
        setupHoverAnimation(exitHBox, exitHex);

        playText.setText(LangManager.get("playText"));
        settingsText.setText(LangManager.get("settingsText"));
        exitText.setText(LangManager.get("exitText"));                     
    }

    // Added to the Exit 'Button'
    @FXML
    private void switchToPrimary(MouseEvent event) throws IOException {
        viewModel.exitGame();
    }

    // Added to Settings 'Button'
    @FXML
    private void switchToSettings(MouseEvent event) throws IOException {
        viewModel.openSettings();
    }

    // Added to the Play 'Button'
    @FXML
    private void switchToSetup(MouseEvent event) throws IOException {
        viewModel.startNewGame();
    }

    // Tie Hovering to All Elements in HBoxes
    private void setupHoverAnimation(HBox menuContainer, Polygon hex) {
        Timeline hoverIn = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(hex.opacityProperty(), hex.getOpacity()),
                new KeyValue(hex.scaleXProperty(), hex.getScaleX()),
                new KeyValue(hex.scaleYProperty(), hex.getScaleY())
            ),
            new KeyFrame(Duration.millis(200),
                new KeyValue(hex.opacityProperty(), 1),
                new KeyValue(hex.scaleXProperty(), 1.15),
                new KeyValue(hex.scaleYProperty(), 1.15)
            )
        );

        Timeline hoverOut = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(hex.opacityProperty(), hex.getOpacity()),
                new KeyValue(hex.scaleXProperty(), hex.getScaleX()),
                new KeyValue(hex.scaleYProperty(), hex.getScaleY())
            ),
            new KeyFrame(Duration.millis(300),
                new KeyValue(hex.opacityProperty(), 0),
                new KeyValue(hex.scaleXProperty(), 0),
                new KeyValue(hex.scaleYProperty(), 0)
            )
        );

        menuContainer.setOnMouseEntered(e -> {
            hoverOut.stop();
            hoverIn.playFromStart();
        });

        menuContainer.setOnMouseExited(e -> {
            hoverIn.stop();
            hoverOut.playFromStart();
        });
    }
    
    // Add Gradual Scaling to all Menu Options
    private void applyHoverScaling(javafx.scene.Node node) {
        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), node);
            st.setToX(1.1);
            st.setToY(1.1);
            st.play();
        });

        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        node.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), node);
            st.setToX(0.95);
            st.setToY(0.95);
            st.play();
        });

        node.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), node);
            st.setToX(node.isHover() ? 1.1 : 1.0);
            st.setToY(node.isHover() ? 1.1 : 1.0);
            st.play();
        });
    }

    // Layout of hex grid for the background
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
                if (col % 2 == 1) y += vertSpacing / 2;

                hexes.add(new Hex(x, y, r, centerX, centerY));
            }
        }
    }

    // Draw the laid out hexes
    private void drawStaticBackground() {
        double width = hexCanvas.getWidth();
        double height = hexCanvas.getHeight();

        // Draw once to a WritableImage
        staticBackground = new WritableImage((int) width, (int) height);
        GraphicsContext tempGC = new Canvas(width, height).getGraphicsContext2D();

        // Fill background black
        tempGC.setFill(Color.BLACK);
        tempGC.fillRect(0, 0, width, height);

        // Draw hex outlines (static)
        tempGC.setStroke(Color.web("#444"));
        tempGC.setLineWidth(3);
        for (Hex hex : hexes) {
            tempGC.strokePolygon(hex.xPoints, hex.yPoints, 6);
        }

        tempGC.getCanvas().snapshot(null, staticBackground);
    }

    // Gradual colour changes of all hexes
    private void startHexSpiralAnimation() {
        GraphicsContext gc = hexCanvas.getGraphicsContext2D();

        final long startNanoTime = System.nanoTime();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double t = (now - startNanoTime) / 3e9; // seconds

                // Draw static background
                gc.drawImage(staticBackground, 0, 0);

                // Animate hex fill
                for (Hex hex : hexes) {
                    double opacity = 0.6 + 0.4 * Math.sin(hex.distanceToCenter / 100 - t * 1.5);
                    opacity = Math.max(0, Math.min(1, opacity));
                    gc.setFill(Color.rgb(85, 85, 85, opacity));
                    gc.fillPolygon(hex.xPoints, hex.yPoints, 6);
                }
            }
        }.start();
    }
}
