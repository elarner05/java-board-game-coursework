package com.example.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.viewmodel.GameViewModel;
import com.example.viewmodel.viewstates.GameUIState;
import com.example.viewmodel.viewstates.PlayerViewState;
import com.example.viewmodel.viewstates.RoadViewState;
import com.example.viewmodel.viewstates.TileViewState;
import com.example.viewmodel.viewstates.VertexViewState;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.beans.binding.Bindings;

public class GameScreenController implements ViewModelAware<GameViewModel> {
    private GameViewModel viewModel;

    @FXML
    private Font mainFont;
    @FXML
    private Font italicFont;
    @FXML
    private Pane vertexPane;
    @FXML
    private Pane roadsPane;
    @FXML
    private Pane portsPane;
    @FXML
    private Pane borderPane;
    @FXML
    private Polygon mainPentagon;
    @FXML
    private Pane rootPane, catanBoardPane;
    @FXML
    private Pane playerColumnPane;
    @FXML
    private Polygon smallTriangle;

    @FXML
    private Polygon numberArrow;

    @FXML
    private VBox playerList;

    @FXML
    private Pane bottomPane;

    @FXML
    private StackPane popupOverlay;
    @FXML
    private StackPane popupBox;

    // Static holder for names before screen loads
    private Shape[] vertexNodes = new Shape[54]; // can hold Circle or Rectangle

    // Static holder for names before screen loads
    private int[] vertexToPort = new int[54]; // -1 = not a port
    private Node[] portDecorations;
    private Shape[] roadNodes = new Shape[72];

    @Override
    public void setViewModel(GameViewModel viewModel) {
        this.viewModel = viewModel;

        // Initialize portDecorations here
        int numberOfPorts = viewModel.getPorts().length;
        portDecorations = new Node[numberOfPorts];

        bindViewModel();
    }

    private void bindViewModel() {
        // --- Tiles ---
        for (int i = 0; i < tileGroup.length; i++) {
            bindTile(i, viewModel.tilesProperty().get(i));
        }

        // --- Ports FIRST ---
        mapPortsToVertices(viewModel.getPorts());

        // --- Vertex positions FIRST ---
        setupAllVertices(viewModel.getTileVertices());

        // --- THEN bind vertices (calls setVertex with ports available) ---
        for (int i = 0; i < vertexNodes.length; i++) {
            bindVertex(i, viewModel.verticesProperty().get(i));
        }

        // --- Roads geometry ---
        setupAllRoads(viewModel.getRoads());

        // --- Roads ---
        for (int i = 0; i < roadNodes.length; i++) {
            bindRoad(i, viewModel.roadsProperty().get(i));
        }

        // --- Players ---
        ObservableList<PlayerViewState> players = viewModel.playersProperty();

        // Initial load
        for (int i = 0; i < players.size(); i++) {
            addPlayerRow(players.get(i), i);
        }

        setPlayerIndents();

        // Listen for changes
        players.addListener((ListChangeListener<PlayerViewState>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (PlayerViewState p : change.getAddedSubList()) {
                        int index = players.indexOf(p);
                        addPlayerRow(p, index);
                    }
                }
                if (change.wasRemoved()) {
                    for (PlayerViewState p : change.getRemoved()) {
                        removePlayerRow(p);
                    }
                }
            }
            setPlayerIndents();
        });

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/currentPlayer.fxml"));
            Node node = loader.load();

            CurrentPlayerController ctrl = loader.getController();
            ctrl.bindCurrentPlayer(viewModel);
            bottomPane.getChildren().add(node);
            

        } catch (IOException e) {
            e.printStackTrace();
        }

        bindTradingMenu();

    }

    private void bindTradingMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tradingMenu.fxml"));
            Node menu = loader.load();
            TradingMenuController ctrl = loader.getController();
            ctrl.bind(viewModel);

            //row.setUserData(player); // store reference for removal
            menu.maxHeight(Region.USE_PREF_SIZE);
            popupBox.getChildren().add(menu);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addPlayerRow(PlayerViewState player, int index) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/player.fxml"));
            Node row = loader.load();
            PlayerController ctrl = loader.getController();
            ctrl.bind(player);

            row.setUserData(player); // store reference for removal
            playerList.getChildren().add(row);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removePlayerRow(PlayerViewState player) {
        playerList.getChildren().removeIf(node -> node.getUserData() == player);
    }

    private void setPlayerIndents() {
        for(int i = 0; i < playerList.getChildren().size(); i++)
        {
            Node row = playerList.getChildren().get(i);

            row.setTranslateX(0);
            row.setTranslateX(60 * i);
        }
    }

    

    private void bindTile(int index, TileViewState state) {
        state.number.addListener((obs, old, val) -> {
            setTile(index, val.intValue(), resolveColor(state.resource.get()));
            attachTileClickHandler(index, tileGroup[index]);
        });

        state.blocked.addListener((obs, old, val) -> {
            setTileDisabled(index, val);
        });

        setTile(index, state.number.get(), resolveColor(state.resource.get()));
        attachTileClickHandler(index, tileGroup[index]);
        setTileDisabled(index, state.blocked.get());
    }

    private void bindVertex(int id, VertexViewState state) {
        state.type.addListener((obs, old, type) -> {
            setVertex(id, state.owner.get(), type);
            bindVertexVisibility(vertexNodes[id], state);
            attachVertexClickHandler(vertexNodes[id], id);
        });

        state.owner.addListener((obs, old, owner) -> {
            setVertex(id, owner.intValue(), state.type.get());
            bindVertexVisibility(vertexNodes[id], state);
            attachVertexClickHandler(vertexNodes[id], id);
        });

        setVertex(id, state.owner.get(), state.type.get());
        attachVertexClickHandler(vertexNodes[id], id);
        bindVertexVisibility(vertexNodes[id], state);
    }

    private void bindRoad(int id, RoadViewState roadState) {
        // Listen for changes to the road owner and update UI
        roadState.owner.addListener((obs, oldOwner, newOwner) -> {
            setRoad(id, newOwner.intValue());
            bindRoadVisibility(roadNodes[id], roadState);
            attachRoadClickHandler(roadNodes[id], id);
        });

        // Initialize road with current owner
        setRoad(id, roadState.owner.get());
        bindRoadVisibility(roadNodes[id], roadState);
        attachRoadClickHandler(roadNodes[id], id);
    }

    private Color resolveColor(String type) {
        return switch (type) {
            case "tile.forest" -> Color.FORESTGREEN;
            case "tile.hills" -> Color.ORANGERED;
            case "tile.desert" -> Color.rgb(198, 170, 71);
            case "tile.mountains" -> Color.GRAY;
            case "tile.fields" -> Color.GOLD;
            case "tile.pasture" -> Color.LIGHTGREEN;
            default -> Color.LIGHTGRAY;
        };
    }

    @FXML
    public void initialize() {

        // Load font from classpath
        try {
            //mainFont = Font.loadFont(getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf"), 40);
            mainFont = Font.loadFont(getClass().getResourceAsStream("/fonts/Oswald-Regular.ttf"), 50);
            italicFont = Font.loadFont(getClass().getResourceAsStream("/fonts/NotoSans-Italic.ttf"), 40);
        } catch (Exception e) {
            throw new IllegalStateException("JavaFX failed to load font:  Noto Sans");
        }

        mainPentagon.setTranslateX(-rootPane.getWidth() / 2);
        mainPentagon.setTranslateY(-rootPane.getHeight() / 2);

        System.out.println("GameScreenV initialized"); // Debug

        createCatanBoard(rootPane);
        playerColumnPane.toBack();
        mainPentagon.toBack();

        popupOverlay.visibleProperty()
            .bind(GameUIState.popupVisible);

    }

    private void mapPortsToVertices(int[][] ports) {
        Arrays.fill(vertexToPort, -1);

        for (int portId = 0; portId < ports.length; portId++) {
            int v1 = ports[portId][0];
            int v2 = ports[portId][1];

            vertexToPort[v1] = portId;
            vertexToPort[v2] = portId;
        }
    }

    private Group[] tileGroup = new Group[19];

    private Group createTile(double width, double height, int numberToken, Color resourceColor) {
        Group tile = new Group();

        // --- MAIN HEX ---
        Polygon hex = createHex(width, height);
        hex.setFill(resourceColor);
        hex.setStroke(Color.rgb(36, 31, 14));
        hex.setStrokeWidth(3);

        // --- NUMBER TOKEN ---
        String displayStr;
        if (numberToken == 0) {
            displayStr = "";
        } else if (numberToken == 1) {
            displayStr = "R";
        } else {
            displayStr = String.valueOf(numberToken);
        }

        Text outlineText = new Text(displayStr);
        outlineText.setFont(mainFont);
        outlineText.setFill(Color.BLACK);
        outlineText.setStroke(Color.BLACK);
        outlineText.setStrokeWidth(6);
        outlineText.setTextOrigin(VPos.CENTER);

        Text numberText = new Text(displayStr);
        numberText.setFont(outlineText.getFont());
        numberText.setFill(Color.WHITE);
        numberText.setTextOrigin(VPos.CENTER);

        StackPane numberPane = new StackPane(outlineText, numberText);
        numberPane.setPrefSize(width, height - 30);
        numberPane.setMouseTransparent(true);

        tile.getChildren().addAll(hex, numberPane);

        // --- BORDER HEX in borderPane ---
        double borderScale = 1.6;
        Polygon borderHex = createHex(width * borderScale, height * borderScale);
        borderHex.setFill(Color.rgb(236, 210, 114));

        // Center border hex behind the tile
        borderHex.layoutXProperty().bind(
                tile.layoutXProperty()
                        .subtract((borderHex.getBoundsInLocal().getWidth() - width) / 2));

        borderHex.layoutYProperty().bind(
                tile.layoutYProperty()
                        .subtract((borderHex.getBoundsInLocal().getHeight() - height) / 2));

        borderPane.getChildren().add(borderHex);

        return tile;
    }

    private void setupAllVertices(int[][] tileVertices) {
        vertexPane.toFront(); // Ensure vertex layer is above everything

        // Map vertexId -> list of hexes it belongs to
        Map<Integer, List<Group>> vertexHexMap = new HashMap<>();
        Map<Integer, List<double[]>> vertexLocalCorners = new HashMap<>();

        // Step 1: Collect hexes and local corner positions
        for (int tileIndex = 0; tileIndex < tileVertices.length; tileIndex++) {
            Group tile = tileGroup[tileIndex];
            if (tile == null)
                continue;

            Polygon hex = (Polygon) tile.getChildren().get(0);
            ObservableList<Double> pts = hex.getPoints();

            // Calculate hex center
            double centerX = 0, centerY = 0;
            for (int i = 0; i < 6; i++) {
                centerX += pts.get(i * 2);
                centerY += pts.get(i * 2 + 1);
            }
            centerX /= 6;
            centerY /= 6;

            for (int i = 0; i < 6; i++) {
                int vertexId = tileVertices[tileIndex][i];
                double cornerX = pts.get(i * 2);
                double cornerY = pts.get(i * 2 + 1);

                vertexHexMap.computeIfAbsent(vertexId, k -> new ArrayList<>()).add(tile);
                vertexLocalCorners.computeIfAbsent(vertexId, k -> new ArrayList<>())
                        .add(new double[] { cornerX, cornerY, centerX, centerY });
            }
        }

        // Step 2: Place each vertex
        for (int vertexId = 0; vertexId < vertexNodes.length; vertexId++) {
            if (!vertexLocalCorners.containsKey(vertexId))
                continue;

            List<double[]> corners = vertexLocalCorners.get(vertexId);
            double avgX = 0, avgY = 0;

            for (double[] data : corners) {
                double cornerX = data[0];
                double cornerY = data[1];
                double centerX = data[2];
                double centerY = data[3];

                // Vector from hex center to corner
                double dx = cornerX - centerX;
                double dy = cornerY - centerY;

                // Push outward a little
                double pushFactor = 0.25;
                double pushedX = cornerX + dx * pushFactor;
                double pushedY = cornerY + dy * pushFactor;

                // Convert to vertexPane coordinates
                Group tile = vertexHexMap.get(vertexId).get(corners.indexOf(data));
                double sceneX = tile.localToScene(pushedX, pushedY).getX() - vertexPane.getLayoutX();
                double sceneY = tile.localToScene(pushedX, pushedY).getY() - vertexPane.getLayoutY();

                avgX += sceneX;
                avgY += sceneY;
            }

            avgX /= corners.size();
            avgY /= corners.size();

            // Create vertex shape (default Circle for now)
            Shape vertex = createCircleVertex(Color.GREY, avgX, avgY);

            vertexNodes[vertexId] = vertex;
            vertexPane.getChildren().add(vertex);

            // Add vertex ID label above
            // Text label = new Text(String.valueOf(vertexId));
            // label.setFill(Color.INDIGO);
            // label.setFont(Font.font(12));
            // label.setLayoutX(avgX - 4);
            // label.setLayoutY(avgY - 12);
            // vertexPane.getChildren().add(label);
        }
    }

    private void setupAllRoads(int[][] roadVertices) {
        roadsPane.toFront(); // Ensure road layer is above everything

        if (roadVertices == null || roadVertices.length == 0 || vertexNodes == null)
            return;

        roadNodes = new Shape[roadVertices.length];

        double shorten = 16; // pixels to shorten at each end

        for (int roadId = 0; roadId < roadVertices.length; roadId++) {
            int v1 = roadVertices[roadId][0];
            int v2 = roadVertices[roadId][1];

            Shape vertex1 = vertexNodes[v1];
            Shape vertex2 = vertexNodes[v2];

            if (vertex1 == null || vertex2 == null)
                continue;

            double x1 = vertex1.getLayoutX();
            double y1 = vertex1.getLayoutY();
            double x2 = vertex2.getLayoutX();
            double y2 = vertex2.getLayoutY();

            // Compute full vector
            double dx = x2 - x1;
            double dy = y2 - y1;
            double fullLength = Math.hypot(dx, dy);

            // Normalize vector
            double ux = dx / fullLength;
            double uy = dy / fullLength;

            // Shorten both ends
            double startX = x1 + ux * shorten;
            double startY = y1 + uy * shorten;
            double endX = x2 - ux * shorten;
            double endY = y2 - uy * shorten;

            // Midpoint for rectangle placement
            double midX = (startX + endX) / 2;
            double midY = (startY + endY) / 2;

            // New length
            double roadLength = Math.hypot(endX - startX, endY - startY);

            // Rotation angle
            double angle = Math.toDegrees(Math.atan2(endY - startY, endX - startX));

            // Create rectangle
            double roadWidth = 9;
            Rectangle road = new Rectangle(roadLength, roadWidth);
            road.setFill(Color.GRAY);
            road.setStroke(Color.BLACK);
            road.setStrokeWidth(1);

            // Center rectangle on midpoint
            road.setTranslateX(midX - roadLength / 2);
            road.setTranslateY(midY - roadWidth / 2);
            road.setRotate(angle);

            roadNodes[roadId] = road;
            roadsPane.getChildren().add(road);
        }
    }

    private Group createPips(int numberToken) {
        Group pips = new Group();

        int dotCount = switch (numberToken) {
            case 2, 12 -> 1;
            case 3, 11 -> 2;
            case 4, 10 -> 3;
            case 5, 9 -> 4;
            case 6, 8 -> 5;
            default -> 0;
        };

        double spacing = 16;
        double startX = -(dotCount - 1) * spacing / 2;

        for (int i = 0; i < dotCount; i++) {
            Circle dot = new Circle(6);
            dot.setFill(numberToken == 6 || numberToken == 8 ? Color.RED : Color.WHITE);

            dot.setStroke(Color.BLACK);
            dot.setStrokeWidth(2);

            dot.setLayoutX((startX + i * spacing) - 2);
            pips.getChildren().add(dot);
        }

        return pips;
    }

    private void setTile(int index, int numberToken, Color resourceColor) {
        if (!isValidIndex(index)) {
            return;
        }

        Group tile = tileGroup[index];

        Polygon hex = (Polygon) tile.getChildren().get(0);
        StackPane numberPane = (StackPane) tile.getChildren().get(1);

        Text outlineText = (Text) numberPane.getChildren().get(0);
        Text numberText = (Text) numberPane.getChildren().get(1);

        // Update main hex color
        hex.setFill(resourceColor);

        // Update number token
        String tokenStr;
        if (numberToken == 0) {
            tokenStr = "";
        } else if (numberToken == 1) {
            tokenStr = "R";
        } else {
            tokenStr = String.valueOf(numberToken);
        }

        outlineText.setText(tokenStr);
        numberText.setText(tokenStr);
        numberText.setFill(numberToken == 6 || numberToken == 8 ? Color.RED : Color.WHITE);

        // Remove old pips (keep border hex and number pane)
        tile.getChildren().removeIf(n -> n instanceof Group && n != tile.getChildren().get(1) && n != numberPane);

        // Add new pips
        Group pips = createPips(numberToken);
        Bounds b = hex.getBoundsInLocal();
        pips.setLayoutX(b.getWidth() / 2);
        pips.setLayoutY(b.getHeight() / 2 + 18);
        tile.getChildren().add(pips);
    }

    private void attachTileClickHandler(int index, Group tile){
        tile.setOnMouseClicked(event ->{
            event.consume();
            viewModel.moveRobber(index);
        });
    }

    private void setVertex(int vertexId, int playerOwner, String type) 
    {

        if (vertexId < 0 || vertexId >= vertexNodes.length || vertexNodes[vertexId] == null) {
            return;
        }

        // Remove old node
        vertexPane.getChildren().remove(vertexNodes[vertexId]);

        Color fillColor = viewModel.getPlayerColor(playerOwner);

        double layoutX = vertexNodes[vertexId].getLayoutX();
        double layoutY = vertexNodes[vertexId].getLayoutY();

        Shape newVertex;

        if ("player_infrastructure.city".equals(type)) {
            newVertex = createSquareVertex(fillColor, layoutX, layoutY);
        } else if ("player_infrastructure.settlement".equals(type)) {
            newVertex = createCircleVertex(fillColor, layoutX, layoutY);
        } else {
            newVertex = createCircleVertex(Color.GREY, layoutX, layoutY);
        }

        vertexNodes[vertexId] = newVertex;
        vertexPane.getChildren().add(newVertex);

        // -------------------------
        // PORT RENDERING (Improved)
        // -------------------------

        int portId = vertexToPort[vertexId];

        if (portId == -1) return;

        // Render only once (first vertex in pair)
        int v1 = viewModel.getPorts()[portId][0];
        if (vertexId != v1) return;

        // Remove old port node if present
        if (portDecorations[portId] != null) {
            portsPane.getChildren().remove(portDecorations[portId]);
            portDecorations[portId] = null;
        }

        int v2 = viewModel.getPorts()[portId][1];

        Shape vertex1 = vertexNodes[v1];
        Shape vertex2 = vertexNodes[v2];

        double x1 = vertex1.getLayoutX();
        double y1 = vertex1.getLayoutY();
        double x2 = vertex2.getLayoutX();
        double y2 = vertex2.getLayoutY();

        // Midpoint
        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;

        Bounds centerTileBounds = tileGroup[9].getBoundsInParent();

        // Extract x and y into separate variables
        double boardCenterX = centerTileBounds.getMinX();
        double boardCenterY = centerTileBounds.getMinY();

        double dx = midX - boardCenterX;
        double dy = midY - boardCenterY;
        double length = Math.hypot(dx, dy);

        double pushDistance = 80;

        double finalX = midX + (dx / length) * pushDistance;
        double finalY = midY + (dy / length) * pushDistance;

        // ------------------------
        // SYMBOL-BASED PORT BADGE
        // ------------------------
        
        StackPane badge = createSymbolPortBadge();

        // Center badge
        badge.setLayoutX(finalX);
        badge.setLayoutY(finalY);

        // Rotate outward
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        badge.setRotate(angle + 90);

        badge.setMouseTransparent(true);

        portDecorations[portId] = badge;
        //portsPane.getChildren().add(badge);
    }

    private StackPane createSymbolPortBadge() 
    {
        StackPane root = new StackPane();

        Circle bg = new Circle(22);
        bg.setFill(Color.BEIGE);
        bg.setStroke(Color.SADDLEBROWN);
        bg.setStrokeWidth(3);

        Text questionMark = new Text("?");
        questionMark.setFont(mainFont);
        questionMark.setFill(Color.BLACK);

        root.getChildren().addAll(bg, questionMark);

        return root;
    }

    private Shape createCircleVertex(Color fillColor, double layoutX, double layoutY) 
    {
        double radius = 12;
        Circle circle = new Circle(radius);
        circle.setFill(fillColor);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(1);
        circle.setLayoutX(layoutX);
        circle.setLayoutY(layoutY);
        return circle;
    }

    private Shape createSquareVertex(Color fillColor, double layoutX, double layoutY) {
        double size = 25;
        Rectangle square = new Rectangle(size, size);
        square.setFill(fillColor);
        square.setStroke(Color.BLACK);
        square.setStrokeWidth(2);
        square.setLayoutX(layoutX - size / 2);
        square.setLayoutY(layoutY - size / 2);
        return square;
    }

    private void attachVertexClickHandler(Shape vertex, int vertexId) {
        vertex.setOnMouseClicked(event -> {
            event.consume(); // prevents clicks falling through

            // Example: tell ViewModel
            viewModel.onVertexClicked(vertexId);
        });

        // Optional hover feedback
        vertex.setOnMouseEntered(e -> vertex.setOpacity(0.8));
        vertex.setOnMouseExited(e -> vertex.setOpacity(1.0));
    }

    private void bindVertexVisibility(Shape vertex, VertexViewState state) {

        vertex.visibleProperty().bind(state.visible);
        vertex.mouseTransparentProperty().bind(state.visible.not());
    }

    private void setRoad(int roadId, int playerOwner) {
        if (roadId < 0 || roadId >= roadNodes.length || roadNodes[roadId] == null) {
            return;
        }

        Color fillColor = viewModel.getPlayerColor(playerOwner);

        Shape roadShape = roadNodes[roadId];
        if (roadShape instanceof Rectangle rect) {
            rect.setFill(fillColor);
        }
    }

    private void attachRoadClickHandler(Shape road, int roadId) {
        road.setOnMouseClicked(event -> {
            event.consume(); // prevents clicks falling through

            // Example: tell ViewModel
            viewModel.onRoadClicked(roadId);

            // Optional visual feedback
        });

        // Optional hover feedback
        road.setOnMouseEntered(e -> road.setOpacity(0.8));
        road.setOnMouseExited(e -> road.setOpacity(1.0));
    }

    private void bindRoadVisibility(Shape road, RoadViewState state) {

        road.visibleProperty().bind(state.visible);
        road.mouseTransparentProperty().bind(state.visible.not());
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < tileGroup.length && tileGroup[index] != null;
    }

    private void highlightTile(int index, boolean highlight) {
        if (!isValidIndex(index))
            return;

        Platform.runLater(() -> {
            Polygon hex = (Polygon) tileGroup[index].getChildren().get(0);
            hex.setStroke(highlight ? Color.GOLD : Color.WHITE);
            hex.setStrokeWidth(highlight ? 4 : 2);
        });
    }

    private void setTileDisabled(int index, boolean disabled) {
        if (!isValidIndex(index))
            return;

        Platform.runLater(() -> {
            tileGroup[index].setOpacity(disabled ? 0.2 : 1.0);
            tileGroup[index].setMouseTransparent(disabled);
        });
    }

    private void createCatanBoard(Pane boardPane) {
        double hexWidth = 115;
        double hexHeight = 115;
        double gap = 34;
        double rightShift = 100;
        double totalHeight = 864;

        int[] rowHexCounts = { 3, 4, 5, 4, 3 };

        int centerRowIndex = 2;
        double centerRowY = totalHeight / 2;
        double middleHexY = centerRowIndex * (0.75 * hexHeight + gap) + (hexHeight / 2);
        double verticalOffset = centerRowY - middleHexY;

        int id = 0;

        // --- Background ---
        Polygon background = createFlatTopHex(hexWidth * 8, totalHeight - 92);

        // I Will want to bring this back at some point, just having a bit of a layering
        // issue
        background.setFill(Color.rgb(57, 69, 147));
        background.setStroke(Color.rgb(7, 4, 60));
        background.setStrokeWidth(3);
        boardPane.getChildren().add(background);

        background.toBack();
        mainPentagon.toBack();

        // --- Create tiles ---
        for (int row = 0; row < rowHexCounts.length; row++) {

            int count = rowHexCounts[row];
            double y = verticalOffset + row * (0.75 * hexHeight + gap);

            for (int col = 0; col < count; col++) {

                double x = col * (hexWidth + gap);

                if (row % 2 == 1) {
                    x += (hexWidth + gap) / 2;
                }

                if (row == 0 || row == rowHexCounts.length - 1) {
                    x += hexWidth + gap;
                }

                x += rightShift;

                // ---- TILE CREATION ----
                Group tile = createTile(
                        hexWidth,
                        hexHeight,
                        0, // placeholder number token
                        Color.LIGHTGRAY // placeholder resource
                );

                tile.setLayoutX(x);
                tile.setLayoutY(y);

                tileGroup[id] = tile;
                boardPane.getChildren().add(tile);

                id++;
            }
        }

        // --- Center background on center tile (index 9) ---
        Bounds centerTileBounds = tileGroup[9].getBoundsInParent();
        Bounds bgBounds = background.getBoundsInLocal();

        double centerX = centerTileBounds.getMinX() + centerTileBounds.getWidth() / 2;
        double centerY = centerTileBounds.getMinY() + centerTileBounds.getHeight() / 2;

        background.setLayoutX(centerX - bgBounds.getWidth() / 2);
        background.setLayoutY(centerY - bgBounds.getHeight() / 2 + 12);

        background.setLayoutY(centerY - bgBounds.getHeight() / 2 + 21);
        System.out.println("Catan board created with 19 tile views.");
    }

    private Polygon createHex(double width, double height) {
        Polygon hex = new Polygon();
        double w = width;
        double h = height;
        hex.getPoints().addAll(
                w / 2, 0.0,
                w, h / 4,
                w, 3 * h / 4,
                w / 2, h,
                0.0, 3 * h / 4,
                0.0, h / 4);
        return hex;
    }

    private Polygon createFlatTopHex(double width, double height) {
        Polygon hex = new Polygon();
        hex.getPoints().addAll(
                width / 4, 0.0, // top-left
                3 * width / 4, 0.0, // top-right
                width, height / 2, // right
                3 * width / 4, height, // bottom-right
                width / 4, height, // bottom-left
                0.0, height / 2 // left
        );
        return hex;
    }

    public void nextPlayer() {
        viewModel.nextPlayer();
        System.out.println("Next player: " + viewModel.getCurrentPlayer().nameProperty().get());
    }

    public void setCurrentPlayer(int currentPlayerIndex) {
        // then update UI
        // assignPlayersToPanes(currentPlayerIndex);
    }

    public void giveSettlementResources() {
        viewModel.giveSettlementResources();
        System.out.println("Gave settlement resources to current player.");
    }

    public void giveCityResources() {
        viewModel.giveCityResources();
        System.out.println("Gave city resources to current player.");
    }

    public void giveRoadResources() {
        viewModel.giveRoadResources();
        System.out.println("Gave road resources to current player.");
    }
}
