package de.uniks.beastopia.teaml.controller.ingame.encounter;


import de.uniks.beastopia.teaml.controller.Controller;
import de.uniks.beastopia.teaml.rest.Encounter;
import de.uniks.beastopia.teaml.rest.Monster;
import de.uniks.beastopia.teaml.rest.Opponent;
import de.uniks.beastopia.teaml.service.DataCache;
import de.uniks.beastopia.teaml.service.EncounterOpponentsService;
import de.uniks.beastopia.teaml.service.PresetsService;
import de.uniks.beastopia.teaml.service.TrainerService;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import javax.inject.Inject;
import java.util.List;

import static de.uniks.beastopia.teaml.controller.ingame.TrainerController.PREVIEW_VIEWPORT;
import static de.uniks.beastopia.teaml.service.PresetsService.PREVIEW_SCALING;

public class StartFightNPCController extends Controller {
    @FXML
    public Label headline;
    @FXML
    public ImageView image;
    private Encounter encounter;
    @Inject
    TrainerService trainerService;
    @Inject
    DataCache cache;
    @Inject
    EncounterOpponentsService encounterOpponentsService;
    @Inject
    EncounterController encounterController;
    @Inject
    PresetsService presetsService;
    @SuppressWarnings("unused")
    private int myTrainerOpponentIndex = -5;
    private int myAllyTrainerOpponentIndex = -5;
    private int enemyTrainerOpponentIndex = -5;
    private int enemyAllyTrainerOpponentIndex = -5;

    @Inject
    public StartFightNPCController() {

    }

    public void setControllerInfo(Encounter encounter) {
        this.encounter = encounter;
    }

    @Override
    public String getTitle() {
        return resources.getString("titleEncounter");
    }

    @Override
    public Parent render() {
        Parent parent = super.render();

        disposables.add(encounterOpponentsService.getEncounterOpponents(cache.getJoinedRegion()._id(), encounter._id())
                .observeOn(FX_SCHEDULER)
                .subscribe(ops -> {
                    for (Opponent o : ops) {
                        if (!(o.trainer().equals(cache.getTrainer()._id()))) {
                            disposables.add(trainerService.getTrainer(cache.getJoinedRegion()._id(), o.trainer())
                                    .observeOn(FX_SCHEDULER)
                                    .subscribe(t -> {
                                        headline.setText(t.name() + " " + resources.getString("npcStart"));
                                        disposables.add(cache.getOrLoadTrainerImage(t.image(), true)
                                                .observeOn(FX_SCHEDULER)
                                                .subscribe(i -> {
                                                    image.setImage(i);
                                                    image.setViewport(PREVIEW_VIEWPORT);
                                                    image.setFitWidth(32 * PREVIEW_SCALING);
                                                    image.setFitHeight(64 * PREVIEW_SCALING);
                                                }));
                                    }));
                            return;
                        }
                    }
                }));

        return parent;
    }

    @FXML
    public void startFight() {
        disposables.add(encounterOpponentsService.getEncounterOpponents(cache.getJoinedRegion()._id(),
                        cache.getCurrentEncounter()._id())
                .map(o -> {
                    cache.setCurrentOpponents(o);
                    if (o.size() == 2) {
                        for (int i = 0; i < o.size(); i++) {
                            if (o.get(i).trainer().equals(cache.getTrainer()._id())) {
                                myTrainerOpponentIndex = i;
                            } else {
                                enemyTrainerOpponentIndex = i;
                            }
                        }
                    } else if (o.size() == 3) {
                        for (int i = 0; i < o.size(); i++) {
                            if (o.get(i).trainer().equals(cache.getTrainer()._id())) {
                                if (myTrainerOpponentIndex == -5) {
                                    myTrainerOpponentIndex = i;
                                } else {
                                    myAllyTrainerOpponentIndex = i;
                                }
                            } else {
                                if (!o.get(i).isAttacker()) {
                                    myAllyTrainerOpponentIndex = i;
                                } else {
                                    if (enemyTrainerOpponentIndex == -5) {
                                        enemyTrainerOpponentIndex = i;
                                    } else {
                                        enemyAllyTrainerOpponentIndex = i;
                                    }
                                }
                            }
                        }
                    } else if (o.size() == 4) {
                        for (int i = 0; i < o.size(); i++) {
                            if (o.get(i).trainer().equals(cache.getTrainer()._id())) {
                                if (myTrainerOpponentIndex != -5) {
                                    myAllyTrainerOpponentIndex = i;
                                } else {
                                    myTrainerOpponentIndex = i;
                                }
                            } else {
                                if (!o.get(i).isAttacker() || o.get(i).trainer().equals(cache.getTrainer()._id())) {
                                    myAllyTrainerOpponentIndex = i;
                                } else {
                                    if (enemyTrainerOpponentIndex == -5) {
                                        enemyTrainerOpponentIndex = i;
                                    } else {
                                        enemyAllyTrainerOpponentIndex = i;
                                    }
                                }
                            }
                        }
                    }

                    List<Monster> myMonsters = trainerService.getTrainerMonsters(cache.getJoinedRegion()._id(), cache.getTrainer()._id()).blockingFirst();
                    List<Monster> enemyMonsters = trainerService.getTrainerMonsters(cache.getJoinedRegion()._id(), o.get(enemyTrainerOpponentIndex).trainer()).blockingFirst();
                    encounterController.setMyTrainer(cache.getTrainer());
                    encounterController.setEnemyTrainer(trainerService.getTrainer(cache.getJoinedRegion()._id(), o.get(enemyTrainerOpponentIndex).trainer()).blockingFirst());
                    // When reconnecting: If no monster is set, then find the first monster with 0 HP to indicate swapping out
                    if (o.get(myTrainerOpponentIndex).monster() == null) {
                        encounterController.setOwnMonster(myMonsters.stream().filter(m -> m.currentAttributes().health() <= 0).findFirst().orElseThrow());
                    } else {
                        encounterController.setOwnMonster(myMonsters.stream().filter(m -> m._id().equals(o.get(myTrainerOpponentIndex).monster())).findFirst().orElseThrow());
                    }
                    encounterController.setEnemyMonster(enemyMonsters.stream().filter(m -> m._id().equals(o.get(enemyTrainerOpponentIndex).monster())).findFirst().orElseThrow());

                    if (o.size() == 3) {
                        if (myAllyTrainerOpponentIndex != -5) {
                            setAllyMonsters(o);
                        }
                        if (enemyAllyTrainerOpponentIndex != -5) {
                            setEnemyAllyMonsters(o);
                        }
                    } else if (o.size() == 4) {
                        setEnemyAllyMonsters(o);
                        setAllyMonsters(o);
                    }
                    return o;
                })
                .observeOn(FX_SCHEDULER)
                .subscribe(o -> {
                    encounterController.init();
                    app.show(encounterController);
                }));
    }

    private void setAllyMonsters(List<Opponent> o) {
        List<Monster> myAllyMonsters = trainerService.getTrainerMonsters(cache.getJoinedRegion()._id(), o.get(myAllyTrainerOpponentIndex).trainer()).blockingFirst();
        encounterController.setAllyTrainer(trainerService.getTrainer(cache.getJoinedRegion()._id(), o.get(myAllyTrainerOpponentIndex).trainer()).blockingFirst());
        if (o.get(myAllyTrainerOpponentIndex).monster() == null) {
            encounterController.setAllyMonster(myAllyMonsters.stream().filter(m -> m.currentAttributes().health() <= 0).findFirst().orElseThrow());
        } else {
            encounterController.setAllyMonster(myAllyMonsters.stream().filter(m -> m._id().equals(o.get(myAllyTrainerOpponentIndex).monster())).findFirst().orElseThrow());
        }
    }

    private void setEnemyAllyMonsters(List<Opponent> o) {
        List<Monster> enemyAllyMonsters = trainerService.getTrainerMonsters(cache.getJoinedRegion()._id(), o.get(enemyAllyTrainerOpponentIndex).trainer()).blockingFirst();
        encounterController.setEnemyAllyTrainer(trainerService.getTrainer(cache.getJoinedRegion()._id(), o.get(enemyAllyTrainerOpponentIndex).trainer()).blockingFirst());
        encounterController.setEnemyAllyMonster(enemyAllyMonsters.stream().filter(m -> m._id().equals(o.get(enemyAllyTrainerOpponentIndex).monster())).findFirst().orElseThrow());
    }
}
