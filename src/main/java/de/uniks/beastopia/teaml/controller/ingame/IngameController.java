package de.uniks.beastopia.teaml.controller.ingame;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.uniks.beastopia.teaml.App;
import de.uniks.beastopia.teaml.controller.Controller;
import de.uniks.beastopia.teaml.controller.ingame.encounter.FightWildBeastController;
import de.uniks.beastopia.teaml.controller.menu.PauseController;
import de.uniks.beastopia.teaml.rest.Map;
import de.uniks.beastopia.teaml.rest.*;
import de.uniks.beastopia.teaml.service.*;
import de.uniks.beastopia.teaml.sockets.EventListener;
import de.uniks.beastopia.teaml.sockets.UDPEventListener;
import de.uniks.beastopia.teaml.utils.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

public class IngameController extends Controller {
    static final double TILE_SIZE = 20;
    static final int MENU_NONE = 0;
    static final int MENU_SCOREBOARD = 1;
    static final int MENU_BEASTLIST = 2;

    @FXML
    public Pane tilePane;
    @FXML
    private HBox scoreBoardLayout;
    @Inject
    App app;
    @Inject
    AreaService areaService;
    @Inject
    PresetsService presetsService;
    @Inject
    TrainerService trainerService;
    @Inject
    PauseController pauseController;
    @Inject
    Provider<PauseController> pauseControllerProvider;
    @Inject
    BeastListController beastListController;
    @Inject
    Provider<FightWildBeastController> fightWildBeastControllerProvider;
    @Inject
    Provider<BeastDetailController> beastDetailControllerProvider;
    @Inject
    Prefs prefs;
    @Inject
    DataCache cache;
    @Inject
    TokenStorage tokenStorage;
    @Inject
    Provider<EntityController> entityControllerProvider;
    @Inject
    UDPEventListener udpEventListener;
    @Inject
    EventListener eventListener;
    @Inject
    Provider<MapController> mapControllerProvider;
    @Inject
    ScoreboardController scoreBoardController;
    @Inject
    Provider<IngameController> ingameControllerProvider;
    @Inject
    Provider<SoundController> soundControllerProvider;
    @Inject
    RegionEncountersService regionEncountersService;
    @Inject
    EncounterOpponentsService encounterOpponentsService;
    private Region region;
    private Map map;
    private final List<Pair<TileSetDescription, Pair<TileSet, Image>>> tileSets = new ArrayList<>();
    private int posx = 0;
    private int posy = 0;
    private int lastposx = 0;
    private int lastposy = 0;
    private boolean spawned = false;
    private int width;
    private int height;
    private LoadingPage loadingPage;
    private final List<Controller> subControllers = new ArrayList<>();
    private Monster lastMonster;
    private int currentMenu = MENU_NONE;

    Direction direction;
    final ObjectProperty<PlayerState> state = new SimpleObjectProperty<>();
    Parent player;
    Parent beastListParent;
    Parent beastDetailParent;
    EntityController playerController;
    SoundController soundController;
    Parent scoreBoardParent;
    Parent pauseMenuParent;
    final java.util.Map<EntityController, Parent> otherPlayers = new HashMap<>();
    private final List<KeyCode> pressedKeys = new ArrayList<>();
    private final String[] locationStrings = {"Moncenter", "House", "Store"};
    private long lastValueChangeTime = 0;

    @Inject
    public IngameController() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                moveLoop();
            }
        }, 0, 100);
    }

    /**
     * Initializes the controller by Creating the main player and assigning its callback functions
     */
    @Override
    public void init() {
        super.init();

        scoreBoardController.setOnCloseRequested(() -> {
            scoreBoardLayout.getChildren().remove(scoreBoardParent);
            currentMenu = MENU_NONE;
        });
        scoreBoardController.init();

        beastListController.setOnCloseRequest(() -> {
            scoreBoardLayout.getChildren().remove(beastListParent);
            scoreBoardLayout.getChildren().remove(beastDetailParent);
            lastMonster = null;
            currentMenu = MENU_NONE;
        });
        beastListController.setOnBeastClicked(this::toggleBeastDetails);
        beastListController.init();

        state.setValue(PlayerState.IDLE);
        playerController = entityControllerProvider.get();
        playerController.playerState().bind(state);
        playerController.setOnTrainerUpdate(trainer -> {
            if (!trainer.area().equals(prefs.getArea()._id())) {
                if (Arrays.stream(locationStrings).anyMatch(cache.getArea(trainer.area()).name()::contains)) {
                    soundController.play("sfx:opendoor");
                }

                IngameController controller = ingameControllerProvider.get();
                controller.setRegion(region);
                app.show(controller);
                return;
            }
            posx = trainer.x();
            posy = trainer.y();
            updateOrigin();

            spawned = true;
        });

        soundController = soundControllerProvider.get();
    }

    /**
     * Sets the region this controller should load into
     *
     * @param region The region of the current trainer
     */
    public void setRegion(Region region) {
        prefs.setCurrentRegion(region);
        this.region = region;
    }

    @Override
    public Parent render() {
        loadingPage = LoadingPage.makeLoadingPage(super.render());

        disposables.add(trainerService.getAllTrainer(this.region._id())
                .subscribe(this::loadTrainers));

        return loadingPage.parent();
    }

    private void loadTrainers(List<Trainer> trainers) {
        Trainer myTrainer = loadMyTrainer(trainers);

        if (cache.getAreas().isEmpty()) {
            disposables.add(areaService.getAreas(this.region._id()).observeOn(FX_SCHEDULER).subscribe(areas -> {
                cache.setAreas(areas);
                loadMap(cache.getAreas(), myTrainer, trainers);
            }));
        } else {
            loadMap(cache.getAreas(), myTrainer, trainers);
        }
        disposables.add(eventListener.listen("encounters.*.trainers." + cache.getTrainer()._id()
                        + ".opponents.*.created", Opponent.class)
                .observeOn(FX_SCHEDULER)
                .concatMap(opponentEvent -> {
                    Opponent opponent = opponentEvent.data();
                    return regionEncountersService.getRegionEncounter(cache.getJoinedRegion()._id(), opponent.encounter())
                            .observeOn(FX_SCHEDULER);
                })
                .subscribe(
                        encounter -> {
                            if (encounter.isWild()) {
                                openFightBeastScreen(encounter);
                            } else {
                                // TODO start NPC screen
                            }
                        },
                        error -> System.err.println("Fehler: " + error.getMessage())
                )
        );
    }

    private void openFightBeastScreen(Encounter encounter) {
        FightWildBeastController controller = fightWildBeastControllerProvider.get();
        disposables.add(encounterOpponentsService
                .getEncounterOpponents(cache.getJoinedRegion()._id(), encounter._id())
                .observeOn(FX_SCHEDULER)
                .subscribe(o -> {
                    for (Opponent op : o) {
                        if (op.trainer().equals("000000000000000000000000")) {
                            controller.setControllerInfo(op.monster(), op.trainer());
                            app.show(controller);
                        }
                    }
                }));
    }

    private void listenToTrainerEvents() {
        disposables.add(eventListener.listen(
                        "regions." + this.region._id() + ".trainers.*.created",
                        Trainer.class)
                .observeOn(FX_SCHEDULER)
                .subscribe(event -> this.createRemotePlayer(event.data()),
                        error -> Dialog.error(error, resources.getString("getAllTrainerError"))));
        disposables.add(eventListener.listen(
                        "regions." + this.region._id() + ".trainers.*.deleted",
                        Trainer.class)
                .observeOn(FX_SCHEDULER)
                .subscribe(event -> this.removeRemotePlayer(event.data()),
                        error -> Dialog.error(error, resources.getString("getAllTrainerError"))));
    }

    private void loadRemoteTrainer(List<Trainer> trainers) {
        for (Trainer trainer : trainers) {
            if (trainer._id().equals(cache.getTrainer()._id())) {
                continue;
            }
            createRemotePlayer(trainer);
        }
    }

    private void loadMap(List<Area> areas, Trainer myTrainer, List<Trainer> trainers) {
        Area area = areas.stream().filter(a -> a._id().equals(myTrainer.area())).findFirst().orElseThrow();
        prefs.setArea(area);

        disposables.add(areaService.getArea(this.region._id(), area._id())
                .observeOn(FX_SCHEDULER)
                .subscribe(a -> {
                            this.map = a.map();
                            for (TileSetDescription tileSetDesc : map.tilesets()) {
                                TileSet tileSet = presetsService.getTileset(tileSetDesc).blockingFirst();
                                Image image = presetsService.getImage(tileSet).blockingFirst();
                                tileSets.add(new Pair<>(tileSetDesc, new Pair<>(tileSet, image)));
                            }
                            drawMap();

                            if (a.name().contains("Route")) {
                                soundController.play("bgm:route");
                            } else if (a.name().contains("House")) {
                                soundController.play("bgm:house");
                            } else {
                                soundController.play("bgm:city");
                            }

                            beastListParent = beastListController.render();
                            scoreBoardParent = scoreBoardController.render();
                            pauseMenuParent = pauseController.render();
                            loadRemoteTrainer(trainers);
                            listenToTrainerEvents();
                            loadingPage.setDone();
                        }
                ));
    }

    /**
     * Looks for the current trainer in the list and initializes the player controller
     *
     * @param trainers The list of all trainers of the current region
     * @return Our current trainer
     */
    private Trainer loadMyTrainer(List<Trainer> trainers) {
        Trainer myTrainer = trainers.stream().filter(t -> t.user().equals(tokenStorage.getCurrentUser()._id())).findFirst().orElseThrow();

        playerController.setTrainer(myTrainer);
        playerController.init();

        cache.setTrainer(myTrainer);
        posx = myTrainer.x();
        posy = myTrainer.y();

        return myTrainer;
    }

    private void createRemotePlayer(Trainer trainer) {
        EntityController controller = entityControllerProvider.get();
        ObjectProperty<PlayerState> ps = new SimpleObjectProperty<>();
        controller.playerState().bind(ps);
        ps.setValue(PlayerState.IDLE);
        controller.setTrainer(trainer);
        controller.setOnTrainerUpdate(moveDto -> {
            if (!isBeingRendered(moveDto._id()) && moveDto.area().equals(prefs.getArea()._id())) {
                revealRemotePlayer(cache.getTrainer(moveDto._id()));
            } else if (isBeingRendered(moveDto._id()) && !moveDto.area().equals(prefs.getArea()._id())) {
                hideRemotePlayer(cache.getTrainer(moveDto._id()));
            } else {
                moveRemotePlayer(controller, moveDto.x(), moveDto.y());
            }
        });
        controller.init();
        Parent parent = drawRemotePlayer(controller, trainer.x(), trainer.y());
        otherPlayers.put(controller, parent);
        if (prefs.getArea() != null && !prefs.getArea()._id().equals(trainer.area())) {
            hideRemotePlayer(trainer);
        }
    }

    private EntityController getEntityController(Trainer trainer) {
        EntityController trainerController = null;
        for (EntityController controller : otherPlayers.keySet()) {
            if (controller.getTrainer()._id().equals(trainer._id())) {
                trainerController = controller;
                break;
            }
        }
        return trainerController;
    }

    private void removeRemotePlayer(Trainer trainer) {
        EntityController trainerController = getEntityController(trainer);
        if (trainerController == null) {
            return;
        }
        tilePane.getChildren().remove(otherPlayers.get(trainerController));
        trainerController.destroy();
        otherPlayers.remove(trainerController);
    }

    private void hideRemotePlayer(Trainer trainer) {
        if (trainer == null) {
            return;
        }
        EntityController trainerController = getEntityController(trainer);
        if (trainerController == null) {
            return;
        }
        tilePane.getChildren().remove(otherPlayers.get(trainerController));
    }

    private void revealRemotePlayer(Trainer trainer) {
        if (trainer == null) {
            return;
        }
        EntityController trainerController = getEntityController(trainer);
        if (trainerController == null) {
            return;
        }
        tilePane.getChildren().add(otherPlayers.get(trainerController));
    }

    private boolean isBeingRendered(String trainer) {
        for (EntityController controller : otherPlayers.keySet()) {
            if (controller.getTrainer()._id().equals(trainer)) {
                return tilePane.getChildren().contains(otherPlayers.get(controller));
            }
        }
        return false;
    }

    private void drawMap() {
        drawPlayer(posx, posy);
        for (Layer layer : map.layers()) {
            if (layer.chunks() != null) {
                for (Chunk chunk : layer.chunks()) {
                    int chunkX = chunk.x();
                    int chunkY = chunk.y();
                    int index = 0;
                    for (int id : chunk.data()) {
                        int x = index % chunk.width() + chunkX;
                        int y = index / chunk.width() + chunkY;
                        index++;
                        Pair<Pair<TileSet, Image>, Integer> tileSet = findTileSet(id);
                        if (tileSet == null) {
                            continue;
                        }

                        drawTile(x, y, tileSet.getKey().getValue(), presetsService.getTileViewPort(tileSet.getValue(), tileSet.getKey().getKey()));
                    }
                }
            } else if (layer.data() != null) {
                int chunkX = layer.x();
                int chunkY = layer.y();
                int index = 0;
                for (int id : layer.data()) {
                    int x = index % layer.width() + chunkX;
                    int y = index / layer.width() + chunkY;
                    index++;
                    Pair<Pair<TileSet, Image>, Integer> tileSet = findTileSet(id);
                    if (tileSet == null) {
                        continue;
                    }

                    drawTile(x, y, tileSet.getKey().getValue(), presetsService.getTileViewPort(tileSet.getValue(), tileSet.getKey().getKey()));
                }
            }
        }

        updateOrigin();
    }

    private Pair<Pair<TileSet, Image>, Integer> findTileSet(int id) {
        id++;
        for (int i = tileSets.size() - 1; i >= 0; i--) {
            Pair<TileSetDescription, Pair<TileSet, Image>> tileSet = tileSets.get(i);
            if (tileSet.getKey().firstgid() <= id) {
                return new Pair<>(tileSet.getValue(), id - tileSet.getKey().firstgid());
            }
        }
        return null;
    }

    private void drawTile(int x, int y, Image image, Rectangle2D viewPort) {
        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setImage(image);
        view.setFitWidth(TILE_SIZE);
        view.setFitHeight(TILE_SIZE);
        view.setViewport(viewPort);
        view.setTranslateX(x * TILE_SIZE);
        view.setTranslateY(y * TILE_SIZE);
        tilePane.getChildren().add(view);
    }

    public void setOrigin(int tilex, int tiley) {
        double parentWidth = width;
        double parentHeight = height;
        double originX = parentWidth / 2 - TILE_SIZE / 2;
        double originY = parentHeight / 2 - TILE_SIZE / 2;
        double tilePaneTranslationX = originX - tilex * TILE_SIZE;
        double tilePaneTranslationY = originY - tiley * TILE_SIZE;
        tilePane.setTranslateX(tilePaneTranslationX);
        tilePane.setTranslateY(tilePaneTranslationY);
        movePlayer(tilex, tiley);
        prefs.setPosition(new Point2D(tilex, tiley));

        if (spawned) {
            long currentTime = System.currentTimeMillis();
            // Delay in milliseconds
            double debounceDelay = 250;
            if (currentTime - lastValueChangeTime > debounceDelay) {
                if (lastposx == posx && lastposy == posy) {
                    soundController.play("sfx:bump");
                    lastValueChangeTime = currentTime;
                }
            }
        }
    }

    public void updateOrigin() {
        setOrigin(posx, posy);
    }

    private void drawPlayer(int posx, int posy) {
        tilePane.getChildren().remove(player);
        player = playerController.render();
        player.setTranslateX(posx * TILE_SIZE);
        player.setTranslateY((posy - 1) * TILE_SIZE);
        tilePane.getChildren().add(player);
        player.toFront();
    }

    private Parent drawRemotePlayer(EntityController controller, int posx, int posy) {
        Parent parent = controller.render();
        parent.setTranslateX(posx * TILE_SIZE);
        parent.setTranslateY((posy - 1) * TILE_SIZE);
        tilePane.getChildren().add(parent);
        parent.toFront();
        return parent;
    }

    private void movePlayer(int x, int y) {
        playerController.updateViewPort();
        player.toFront();
        player.setTranslateX(x * TILE_SIZE);
        player.setTranslateY((y - 1) * TILE_SIZE);
    }

    private void moveRemotePlayer(EntityController controller, int x, int y) {
        Parent remotePlayer = otherPlayers.get(controller);
        controller.updateViewPort();
        remotePlayer.toFront();
        remotePlayer.setTranslateX(x * TILE_SIZE);
        remotePlayer.setTranslateY((y - 1) * TILE_SIZE);
    }

    @Override
    public void onResize(int width, int height) {
        this.width = width;
        this.height = height;
        if (loadingPage.isDone()) {
            updateOrigin();
        }
    }

    private void toggleBeastDetails(Monster monster) {
        if (Objects.equals(lastMonster, monster)) {
            scoreBoardLayout.getChildren().remove(beastDetailParent);
            lastMonster = null;
            return;
        }

        lastMonster = monster;

        BeastDetailController controller = beastDetailControllerProvider.get();
        subControllers.add(controller);
        controller.setBeast(monster);
        controller.init();

        scoreBoardLayout.getChildren().remove(beastDetailParent);
        beastDetailParent = controller.render();
        scoreBoardLayout.getChildren().add(0, beastDetailParent);
    }

    private void updateTrainerPos(Direction direction) {
        Trainer trainer = cache.getTrainer();
        JsonObject data = new JsonObject();
        data.add("_id", new JsonPrimitive(trainer._id()));
        data.add("area", new JsonPrimitive(trainer.area()));
        data.add("x", new JsonPrimitive(posx));
        data.add("y", new JsonPrimitive(posy));
        data.add("direction", new JsonPrimitive(direction.ordinal()));

        JsonObject message = new JsonObject();
        message.add("event", new JsonPrimitive("areas." + trainer.area() + ".trainers." + trainer._id() + ".moved"));
        message.add("data", data);

        udpEventListener.send(message.toString());
    }

    @FXML
    public void keyDown(KeyEvent keyEvent) {
        handlePlayerMovement(keyEvent);
        handleMap(keyEvent);
        handlePauseMenu(keyEvent);
        handleScoreboard(keyEvent);
        handleBeastList(keyEvent);
    }

    public void handleBeastList(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.B) && (currentMenu == MENU_NONE || currentMenu == MENU_BEASTLIST)) {
            if (scoreBoardLayout.getChildren().contains(beastListParent)) {
                scoreBoardLayout.getChildren().remove(beastListParent);
                scoreBoardLayout.getChildren().remove(beastDetailParent);
                lastMonster = null;
                currentMenu = MENU_NONE;
            } else {
                scoreBoardLayout.getChildren().add(beastListParent);
                currentMenu = MENU_BEASTLIST;
            }
        }
    }

    private void handleScoreboard(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.N) && (currentMenu == MENU_NONE || currentMenu == MENU_SCOREBOARD)) {
            if (scoreBoardLayout.getChildren().contains(scoreBoardParent)) {
                scoreBoardLayout.getChildren().remove(scoreBoardParent);
                currentMenu = MENU_NONE;
            } else {
                scoreBoardLayout.getChildren().add(scoreBoardParent);
                currentMenu = MENU_SCOREBOARD;
            }
        }
    }

    private void handlePauseMenu(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
            PauseController controller = pauseControllerProvider.get();
            controller.setRegion(region);
            app.show(controller);
        }
    }

    private void handleMap(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.M)) {
            MapController map = mapControllerProvider.get();
            app.show(map);
        }
    }

    private void handlePlayerMovement(KeyEvent keyEvent) {
        if (!pressedKeys.contains(keyEvent.getCode())) {
            if (keyEvent.getCode().equals(KeyCode.UP) || keyEvent.getCode().equals(KeyCode.W)) {
                pressedKeys.add(keyEvent.getCode());
            } else if (keyEvent.getCode().equals(KeyCode.DOWN) || keyEvent.getCode().equals(KeyCode.S)) {
                pressedKeys.add(keyEvent.getCode());
            } else if (keyEvent.getCode().equals(KeyCode.LEFT) || keyEvent.getCode().equals(KeyCode.A)) {
                pressedKeys.add(keyEvent.getCode());
            } else if (keyEvent.getCode().equals(KeyCode.RIGHT) || keyEvent.getCode().equals(KeyCode.D)) {
                pressedKeys.add(keyEvent.getCode());
            }
        }
    }

    public void keyUp(KeyEvent keyEvent) {
        pressedKeys.removeIf(keyCode -> keyCode.equals(keyEvent.getCode()));
        setIdleState();
    }

    public void moveLoop() {
        if (currentMenu == MENU_NONE) {
            boolean moved = false;

            lastposx = posx;
            lastposy = posy;

            if (pressedKeys.contains(KeyCode.UP) || pressedKeys.contains(KeyCode.W)) {
                posy--;
                direction = Direction.UP;
                moved = true;
            } else if (pressedKeys.contains(KeyCode.DOWN) || pressedKeys.contains(KeyCode.S)) {
                posy++;
                direction = Direction.DOWN;
                moved = true;
            } else if (pressedKeys.contains(KeyCode.LEFT) || pressedKeys.contains(KeyCode.A)) {
                posx--;
                direction = Direction.LEFT;
                moved = true;
            } else if (pressedKeys.contains(KeyCode.RIGHT) || pressedKeys.contains(KeyCode.D)) {
                posx++;
                direction = Direction.RIGHT;
                moved = true;
            }

            if (moved) {
                onUI(() -> {
                    state.setValue(PlayerState.WALKING);
                    updateTrainerPos(direction);
                    updateOrigin();
                });
            }
        }
    }

    @FXML
    public void setIdleState() {
        state.setValue(PlayerState.IDLE);
        drawPlayer(posx, posy);
    }


    @Override
    public String getTitle() {
        return resources.getString("titleIngame");
    }

    @Override
    public void destroy() {
        super.destroy();
        playerController.destroy();
        scoreBoardController.destroy();
        beastListController.destroy();
        for (Controller controller : subControllers) {
            controller.destroy();
        }
        for (EntityController controller : otherPlayers.keySet()) {
            controller.destroy();
        }
    }
}
