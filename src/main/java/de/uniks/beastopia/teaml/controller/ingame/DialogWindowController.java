package de.uniks.beastopia.teaml.controller.ingame;

import de.uniks.beastopia.teaml.controller.Controller;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DialogWindowController extends Controller {

    @FXML
    VBox dialogWindow;
    @FXML
    VBox textBox;
    @FXML
    Text dialogText;
    @FXML
    VBox imageBox;
    @FXML
    HBox choiceBox;
    @FXML
    ImageView trainerImageView;
    @Inject
    Provider<ResourceBundle> resourcesProvider;
    @Inject
    Provider<IngameController> ingameControllerProvider;

    private Consumer<Integer> onButtonClicked;
    private Runnable onCloseRequested;
    private List<String> choices = new ArrayList<>();
    private List<Image> buttonImages = new ArrayList<>();
    private Image trainerImage;
    private String text;

    @Inject
    public DialogWindowController() {
    }

    public void setOnCloseRequested(Runnable onCloseRequested) {
        if (onCloseRequested == null) {
            return;
        }
        this.onCloseRequested = onCloseRequested;
    }

    public void setOnButtonClicked(Consumer<Integer> onButtonClicked) {
        if (onButtonClicked == null) {
            return;
        }
        this.onButtonClicked = onButtonClicked;
    }

    public DialogWindowController setChoices(List<String> choices) {
        if (choices == null) {
            return this;
        }
        this.choices = choices;
        return this;
    }

    public DialogWindowController setTrainerImage(Image trainerImage) {
        if (trainerImage == null) {
            return this;
        }
        this.trainerImage = trainerImage;
        return this;
    }

    public DialogWindowController setButtonImages(List<Image> images) {
        if (images == null) {
            return this;
        }
        this.buttonImages = images;
        return this;
    }

    public DialogWindowController setText(String text) {
        this.text = text;
        return this;
    }

    @Override
    public Parent render() {
        Parent parent = super.render();

        for (int i = 0; i < choices.size(); i++) {
            AtomicInteger index = new AtomicInteger(i);
            if (buttonImages.isEmpty()) {
                Button choiceButton = new Button(choices.get(i));
                choiceButton.setOnAction(ev -> {
                    if (onButtonClicked != null) {
                        onButtonClicked.accept(index.get());
                    }
                });
                choiceBox.getChildren().add(choiceButton);
            } else {
                ImageView buttonImageView = new ImageView(buttonImages.get(i));
                buttonImageView.setFitWidth(64);
                buttonImageView.setFitHeight(64);
                Text choiceButton = new Text(choices.get(i));
                choiceButton.setTextAlignment(TextAlignment.CENTER);
                VBox buttonBox = new VBox();
                buttonBox.setOnMouseClicked(ev -> {
                    if (onButtonClicked != null) {
                        onButtonClicked.accept(index.get());
                    }
                });
                buttonBox.getChildren().add(choiceButton);
                buttonBox.getChildren().add(buttonImageView);
                choiceBox.getChildren().add(buttonBox);
            }
        }

        trainerImageView.setImage(trainerImage);
        dialogText.setText(text);

        return parent;
    }

    @FXML
    public void close() {
        if (onCloseRequested != null) {
            onCloseRequested.run();
        }
    }

    @FXML
    public void handleKeyEvent(KeyEvent event) {
        if (event.getCode().equals(KeyCode.T)) {
            onCloseRequested.run();
        }
    }
}