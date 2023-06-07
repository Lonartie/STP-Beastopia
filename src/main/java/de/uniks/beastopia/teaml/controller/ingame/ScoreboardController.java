package de.uniks.beastopia.teaml.controller.ingame;

import de.uniks.beastopia.teaml.controller.Controller;
import de.uniks.beastopia.teaml.rest.Trainer;
import de.uniks.beastopia.teaml.rest.User;
import de.uniks.beastopia.teaml.service.AchievementsService;
import de.uniks.beastopia.teaml.service.DataCache;
import de.uniks.beastopia.teaml.service.TrainerService;
import de.uniks.beastopia.teaml.utils.Prefs;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class ScoreboardController extends Controller {

    @FXML
    private VBox achievements;
    @FXML
    private VBox scoreBoard;
    @Inject
    DataCache cache;
    @Inject
    AchievementsService achievementsService;
    @Inject
    TrainerService trainerService;
    @Inject
    Prefs prefs;
    @Inject
    Provider<ScoreboardUserItemController> scoreBoardUserItemControllerProvider;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<Controller> subControllers = new ArrayList<>();
    private Runnable onCloseRequested;

    @Inject
    public ScoreboardController() {
    }

    @Override
    public Parent render() {
        Parent parent = super.render();
        achievements.setVisible(false);
        loadAchievements();
        return parent;
    }

    private void loadAchievements() {
        disposables.add(achievementsService.getAchievements().subscribe(allAchievements ->
                disposables.add(delay().map(t -> disposables.add(trainerService.getAllTrainer(prefs.getRegionID()).subscribe(trainers -> {
                    for (Trainer trainer : trainers) {
                        User user = cache.getAllUsers().stream()
                                .filter(u -> u._id().equals(trainer.user()))
                                .findFirst()
                                .orElse(null);

                        if (user == null) {
                            continue;
                        }

                        Thread.sleep(1000);

                        disposables.add(delay().map(t2 -> {
                            disposables.add(achievementsService.getUserAchievements(user._id())
                                    .observeOn(FX_SCHEDULER)
                                    .subscribe(achievementList -> {
                                        ScoreboardUserItemController controller = scoreBoardUserItemControllerProvider.get()
                                                .setName(user.name())
                                                .setAchievements(achievementList.size())
                                                .setTotalAchievements(allAchievements.size());
                                        Parent parent = controller.render();
                                        scoreBoard.getChildren().add(parent);
                                        HBox.setHgrow(parent, javafx.scene.layout.Priority.ALWAYS);
                                        subControllers.add(controller);
                                    }));
                            return t;
                        }).subscribe());
                    }
                }))).subscribe())));
    }

    public void handleKeyEvent(KeyEvent event) {
        if (event.getCode().equals(KeyCode.N)) {
            onCloseRequested.run();
        }
    }

    @Override
    public void destroy() {
        subControllers.forEach(Controller::destroy);
        super.destroy();
    }

    public void setOnCloseRequested(Runnable onCloseRequested) {
        this.onCloseRequested = onCloseRequested;
    }
}
