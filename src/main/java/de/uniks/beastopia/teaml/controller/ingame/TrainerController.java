package de.uniks.beastopia.teaml.controller.ingame;

import de.uniks.beastopia.teaml.controller.Controller;
import de.uniks.beastopia.teaml.rest.Region;
import de.uniks.beastopia.teaml.rest.Trainer;
import de.uniks.beastopia.teaml.service.TokenStorage;
import de.uniks.beastopia.teaml.service.TrainerService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import javax.inject.Inject;
import javax.inject.Provider;

public class TrainerController extends Controller {
    @FXML
    private TextField trainerNameInput;
    @FXML
    private Text regionNameDisplay;
    @FXML
    private ImageView trainerSprite;
    @FXML
    private Button backButton;
    @FXML
    private Button chooseLeftButton;
    @FXML
    private Button chooseRightButton;
    @FXML
    private Button deleteTrainerButton;
    @FXML
    private Button saveTrainerButton;

    @Inject
    TokenStorage tokenStorage;
    @Inject
    TrainerService trainerService;
    @Inject
    Provider<IngameController> ingameControllerProvider;

    private Region region;
    private Trainer trainer;

    private final SimpleStringProperty trainerName = new SimpleStringProperty();

    @Inject
    public TrainerController() {
    }

    @Override
    public void init() {
        // Check if current user has a trainer for the specified region
        disposables.add(trainerService.getAllTrainer(region._id())
                .observeOn(FX_SCHEDULER)
                .subscribe(trainers -> {
                    Trainer tr = trainers.stream().filter(t -> t.name().equals(tokenStorage.getCurrentUser().name())).findFirst().orElse(null);

                    if (tr != null) {
                        this.trainer = tr;
                        app.show(ingameControllerProvider.get());
                    }
                }));
    }

    public void saveTrainer() {
        //TODO: Save trainer for current region
    }

    public void deleteTrainer() {
        //TODO: Delete trainer for current region
    }

    public void back() {
        //TODO: Implement back functionality
    }

    @Override
    public Parent render() {
        Parent parent = super.render();

        trainerNameInput.textProperty().bindBidirectional(trainerName);
        regionNameDisplay.setText("RegionName");
        return parent;
    }

    @Override
    public String getTitle() {
        return resources.getString("titleTrainer");
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public void setTrainer(Trainer trainer) {
        this.trainer = trainer;
    }
}
