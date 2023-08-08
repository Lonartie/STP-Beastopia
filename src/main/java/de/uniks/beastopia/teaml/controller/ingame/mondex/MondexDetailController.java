package de.uniks.beastopia.teaml.controller.ingame.mondex;

import de.uniks.beastopia.teaml.controller.Controller;
import de.uniks.beastopia.teaml.rest.MonsterTypeDto;
import de.uniks.beastopia.teaml.service.DataCache;
import de.uniks.beastopia.teaml.service.ImageService;
import de.uniks.beastopia.teaml.service.MondexService;
import de.uniks.beastopia.teaml.service.PresetsService;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import javax.inject.Inject;

public class MondexDetailController extends Controller {

    @FXML
    public VBox VBoxMondexDetail;
    @FXML
    public Label label_name;
    @FXML
    public Label label_type;
    @FXML
    public ImageView imageView_Avatar;
    @FXML
    public TextArea textArea_description;
    @FXML
    public ImageView imageView_TypeIcon1;
    @FXML
    public ImageView imageView_TypeIcon2;
    @Inject
    MondexService mondexService;
    @Inject
    PresetsService presetsService;
    @Inject
    ImageService imageService;
    @Inject
    DataCache dataCache;
    private MonsterTypeDto monster;
    private boolean known;


    @Inject
    public MondexDetailController() {

    }

    public MondexDetailController setMonster(MonsterTypeDto monster) {
        this.monster = monster;
        return this;
    }

    @Override
    public void init() {
        super.init();
        mondexService.init();
        known = mondexService.checkKnown(monster.id());
    }

    @Override
    public Parent render() {
        Parent parent = super.render();
        if (known) {
            label_name.setText(monster.name());
            label_type.setText("Type: " + monster.type().get(0));
            Image typeIcon1 = new Image("file:src/main/resources/de/uniks/beastopia/teaml/assets/monsterTypeIcons/" + monster.type().get(0) + ".png");
            imageView_TypeIcon1.setImage(typeIcon1);
            if (monster.type().size() == 2) {
                label_type.setText(label_type.getText() + ", " + monster.type().get(1));
                Image typeIcon2 = new Image("file:src/main/resources/de/uniks/beastopia/teaml/assets/monsterTypeIcons/" + monster.type().get(1) + ".png");
                imageView_TypeIcon2.setImage(typeIcon2);
            }
            textArea_description.setText(monster.description());
            imageView_Avatar.setImage(dataCache.getMonsterImage(monster.id()));
        } else {
            label_name.setText("???");
            label_type.setText("Type: ???");
            textArea_description.setText("Unknown monster");
            imageView_Avatar.setImage(imageService.makeImageBlack(dataCache.getMonsterImage(monster.id())));
        }


        return parent;
    }


}