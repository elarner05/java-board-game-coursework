package com.example.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import com.example.model.config.LangManager;
import com.example.view.components.Hex;
import com.example.viewmodel.SettingsViewModel;

public class SettingsController implements ViewModelAware<SettingsViewModel> {

    private SettingsViewModel viewModel;
    @FXML
    private Canvas hexCanvas;
    private double r = 75; // hex radius
    private final List<Hex> hexes = new ArrayList<>();
    private WritableImage staticBackground;
    @FXML
    private ComboBox<String> languageCombo;
    @FXML
    private Label settingsLabel;
    @FXML
    private Label languageLabel;
    @FXML
    private Label playText;

    @Override
    public void setViewModel(SettingsViewModel viewModel) {
        this.viewModel = viewModel;
        bind();
    }

    public void bind() {
        ObservableMap<String, String> map = viewModel.availableLanguages;
        ObservableList<String> keys = FXCollections.observableArrayList();
        keys.setAll(map.keySet());
        System.out.println("Available languages: " + map);
        // Keep in sync if map changes
        map.addListener((MapChangeListener<String, String>) change -> {
            keys.setAll(map.keySet());
        });

        languageCombo.setItems(keys);

        languageCombo.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String key) {
                return key == null ? "" : map.get(key);
            }

            @Override
            public String fromString(String value) {
                return null; // not needed unless editable
            }
        });
        languageCombo.valueProperty().bindBidirectional(viewModel.selectedLanguage);
        viewModel.selectedLanguage.addListener((obs, oldLang, newLang) -> {
            settingsLabel.setText(LangManager.get("settingsLabel"));
            languageLabel.setText(LangManager.get("languageLabel"));
            playText.setText(LangManager.get("playText"));
        });

    }

    @FXML
    public void initialize() {
        createHexGrid();
        drawStaticBackground();
        startHexSpiralAnimation();
        settingsLabel.setText(LangManager.get("settingsLabel"));
        languageLabel.setText(LangManager.get("languageLabel"));
        playText.setText(LangManager.get("playText"));
    }

    // Added to the Play 'Button'
    @FXML
    private void switchToSetup(MouseEvent event) throws IOException {
        viewModel.playGame();
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
                if (col % 2 == 1)
                    y += vertSpacing / 2;

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
                    double opacity = 0.6 + 0.4 * Math.sin(hex.distanceToCenter / 80 - t * 0.5);
                    opacity = Math.max(0, Math.min(1, opacity));
                    gc.setFill(Color.rgb(85, 85, 85, opacity));
                    gc.fillPolygon(hex.xPoints, hex.yPoints, 6);
                }
            }
        }.start();
    }

}