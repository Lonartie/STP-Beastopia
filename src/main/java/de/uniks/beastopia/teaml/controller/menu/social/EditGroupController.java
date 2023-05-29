package de.uniks.beastopia.teaml.controller.menu.social;

import de.uniks.beastopia.teaml.controller.Controller;
import de.uniks.beastopia.teaml.rest.User;
import de.uniks.beastopia.teaml.service.DataCache;
import de.uniks.beastopia.teaml.service.FriendListService;
import de.uniks.beastopia.teaml.service.TokenStorage;
import de.uniks.beastopia.teaml.utils.Prefs;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class EditGroupController extends Controller {

    private final List<Controller> subControllers = new ArrayList<>();
    @FXML
    public TextField username;
    @FXML
    public VBox users;
    @FXML
    public TextField newGroupName;
    @Inject
    Provider<DirectMessageController> directMessageControllerProvider;
    @Inject
    Provider<UserController> userControllerProvider;
    @Inject
    FriendListService friendListService;
    @Inject
    TokenStorage tokenStorage;
    @Inject
    Prefs prefs;
    @Inject
    DataCache cache;

    @Inject
    public EditGroupController() {

    }

    @Override
    public String getTitle() {
        return resources.getString("titleEditGroup");
    }

    @FXML
    public void updateUserList() {
        clearSubControllers();
        if (username.getText().isEmpty()) {
            return;
        }
        List<Parent> filteredParents = getFilteredParents();
        users.getChildren().addAll(filteredParents);
    }

    @FXML
    public void back() {
        app.show(directMessageControllerProvider.get());
    }

    public void editGroup() {

    }

    @Override
    public void destroy() {
        super.destroy();
        clearSubControllers();
    }

    private void clearSubControllers() {
        subControllers.forEach(Controller::destroy);
        subControllers.clear();
        users.getChildren().clear();
    }

    private List<Parent> getFilteredParents() {

        List<User> filteredUsers = new ArrayList<>();
        List<Parent> filteredParents = new ArrayList<>();

        for (User user : cache.getAllUsers()) {
            if (user.name().toLowerCase().startsWith(username.getText().toLowerCase())
                    && !user._id().equals(tokenStorage.getCurrentUser()._id())) {
                filteredUsers.add(user);
            }
        }

        filteredUsers = filteredUsers.stream().sorted((firstUser, secondUser) -> {
            boolean notPinned = prefs.isPinned(firstUser);
            if (notPinned) {
                return -1;
            } else {
                return firstUser.name().compareTo(secondUser.name());
            }
        }).toList();


        for (User user : filteredUsers) {
            UserController userController = userControllerProvider.get();
            userController.setUser(user);
            userController.init();
            filteredParents.add(userController.render());
        }

        return filteredParents;
    }
}