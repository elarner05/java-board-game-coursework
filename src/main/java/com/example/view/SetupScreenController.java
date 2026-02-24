package com.example.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale.LanguageRange;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;
import javafx.scene.control.TextField;

import com.example.model.config.LangManager;
import com.example.view.components.Hex;
import com.example.viewmodel.SetupViewModel;

public class SetupScreenController implements ViewModelAware<SetupViewModel> {

    private SetupViewModel viewModel;
    @FXML
    private Canvas hexCanvas;
    @FXML
    private Label threeOption, fourOption, playText;
    @FXML
    private VBox threeBox, fourBox;
    @FXML
    private Polygon threeHex, fourHex, playHex;
    @FXML
    private StackPane threePane, fourPane;
    @FXML
    private HBox pFourBox, playHBox;
    @FXML
    private VBox selectedBox, playVBox; // Only one of threeBox or fourBox can be active
    private double r = 75; // hex radius
    private final List<Hex> hexes = new ArrayList<>();
    private WritableImage staticBackground;

    @FXML
    private TextField player1Field, player2Field, player3Field, player4Field;
    @FXML
    private Label player1Label, player2Label, player3Label, player4Label, setupTitleLabel, playerNumberLabel;

    @Override
    public void setViewModel(SetupViewModel viewModel) {
        this.viewModel = viewModel;
        bindViewModel();
    }

    private void bindViewModel() {
        viewModel.getPlayer1Name().bindBidirectional(player1Field.textProperty());
        viewModel.getPlayer2Name().bindBidirectional(player2Field.textProperty());
        viewModel.getPlayer3Name().bindBidirectional(player3Field.textProperty());
        viewModel.getPlayer4Name().bindBidirectional(player4Field.textProperty());

        viewModel.getNumPlayers().addListener((obs, oldVal, newVal) -> {
            updateSelectionVisuals(newVal.intValue());
        });

        // Set up click handlers for each option
        threeBox.setOnMouseClicked(event -> viewModel.setNumPlayers(3));
        fourBox.setOnMouseClicked(event -> viewModel.setNumPlayers(4));

        updateSelectionVisuals(viewModel.getNumPlayers().get());

        setupHoverAnimation(threeBox, threeHex, threeOption);
        setupHoverAnimation(fourBox, fourHex, fourOption);

        threeOption.setText(LangManager.get("threeOption"));
        fourOption.setText(LangManager.get("fourOption"));
        playText.setText(LangManager.get("playText"));
        setupTitleLabel.setText(LangManager.get("setupTitleLabel"));
        playerNumberLabel.setText(LangManager.get("playerNumberLabel"));
        player1Label.setText(LangManager.get("player1Label"));
        player2Label.setText(LangManager.get("player2Label"));
        player3Label.setText(LangManager.get("player3Label"));
        player4Label.setText(LangManager.get("player4Label"));
    }

    @FXML
    public void initialize() {
        createHexGrid();
        drawStaticBackground();
        startHexSpiralAnimation();

        playHBox.setPickOnBounds(false);

        // Add Gradual Scaling to all Menu Options
        applyHoverScaling(threeOption);
        applyHoverScaling(fourOption);
        // Play
        applyHoverScaling(playHex);
        applyHoverScaling(playText);

        // Tie Hovering to All Elements in HBoxes
    
        setupPlayHover(playHex, playText);

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

    private void setupPlayHover(Polygon hex, Label lbl) {
        Timeline hoverIn = new Timeline(
                new KeyFrame(Duration.millis(250),
                        new KeyValue(hex.opacityProperty(), 1.0),
                        new KeyValue(hex.scaleXProperty(), 1.15),
                        new KeyValue(hex.scaleYProperty(), 1.15),
                        new KeyValue(lbl.rotateProperty(), 5),
                        new KeyValue(lbl.scaleXProperty(), 1.15),
                        new KeyValue(lbl.scaleYProperty(), 1.15)));

        Timeline hoverOut = new Timeline(
                new KeyFrame(Duration.millis(200),
                        new KeyValue(hex.opacityProperty(), 0.0),
                        new KeyValue(hex.scaleXProperty(), 1.0),
                        new KeyValue(hex.scaleYProperty(), 1.0),
                        new KeyValue(lbl.rotateProperty(), 0),
                        new KeyValue(lbl.scaleXProperty(), 1.0),
                        new KeyValue(lbl.scaleYProperty(), 1.0)));

        // Attach to both Polygon and Label
        javafx.event.EventHandler<javafx.scene.input.MouseEvent> enterHandler = e -> {
            hoverOut.stop();
            hoverIn.playFromStart();
        };

        javafx.event.EventHandler<javafx.scene.input.MouseEvent> exitHandler = e -> {
            hoverIn.stop();
            hoverOut.playFromStart();
        };

        hex.setOnMouseEntered(enterHandler);
        lbl.setOnMouseEntered(enterHandler);

        hex.setOnMouseExited(exitHandler);
        lbl.setOnMouseExited(exitHandler);
    }

    private boolean isSelected(VBox vbox) {
        return (vbox == threeBox && viewModel.getNumPlayers().get() == 3)
                || (vbox == fourBox && viewModel.getNumPlayers().get() == 4);
    }

    private void updateSelectionVisuals(int number) {

        boolean isThree = number == 3;

        // Update selection styles
        threeBox.getStyleClass().remove("selected");
        fourBox.getStyleClass().remove("selected");

        if (isThree) {
            threeBox.getStyleClass().add("selected");
        } else {
            fourBox.getStyleClass().add("selected");
        }

        // Show/hide player 4 input
        pFourBox.setVisible(!isThree);
        pFourBox.setManaged(!isThree);
    }

    // Keeps selected hex highlighted and disables hoverOut for it.
    private void setupHoverAnimation(VBox vbox, Polygon hex, Label lbl) {

        // Start invisible unless it’s currently selected
        hex.setOpacity(
                (vbox == threeBox && viewModel.getNumPlayers().get() == 3) ||
                        (vbox == fourBox && viewModel.getNumPlayers().get() == 4)
                                ? 1.0
                                : 0.0);

        Timeline hoverIn = new Timeline(
                new KeyFrame(Duration.millis(250),
                        new KeyValue(hex.opacityProperty(), 1.0),
                        new KeyValue(hex.scaleXProperty(), 1.15),
                        new KeyValue(hex.scaleYProperty(), 1.15),
                        new KeyValue(lbl.rotateProperty(), 5),
                        new KeyValue(lbl.scaleXProperty(), 1.15),
                        new KeyValue(lbl.scaleYProperty(), 1.15)));

        Timeline hoverOut = new Timeline(
                new KeyFrame(Duration.millis(200),
                        new KeyValue(hex.opacityProperty(), 0.0),
                        new KeyValue(hex.scaleXProperty(), 1.0),
                        new KeyValue(hex.scaleYProperty(), 1.0),
                        new KeyValue(lbl.rotateProperty(), 0),
                        new KeyValue(lbl.scaleXProperty(), 1.0),
                        new KeyValue(lbl.scaleYProperty(), 1.0)));

        // --- HOVER IN ---
        vbox.setOnMouseEntered(e -> {
            if (isSelected(vbox)) return;
            hoverOut.stop();
            hoverIn.playFromStart();
        });

        // --- HOVER OUT ---
        vbox.setOnMouseExited(e -> {
            if (isSelected(vbox)) return;
            hoverIn.stop();
            hoverOut.playFromStart();
        });

        vbox.setUserData(new Timeline[] { hoverIn, hoverOut });
    }

    @FXML
    private void switchToGameScreen() {
        viewModel.startGame();
    }

}
