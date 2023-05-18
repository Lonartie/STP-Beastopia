package de.uniks.beastopia.teaml.controller.menu.social;

import de.uniks.beastopia.teaml.controller.Controller;
import de.uniks.beastopia.teaml.rest.Group;
import de.uniks.beastopia.teaml.service.GroupListService;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatListController extends Controller {
    private final List<Controller> subControllers = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();

    @Inject
    GroupListService groupListService;

    @FXML
    private VBox chatList;
    @Inject
    Provider<DirectMessageController> directMessageControllerProvider;

    @Inject
    Provider<ChatUserController> chatUserControllerProvider;
    @Inject
    Provider<ChatGroupController> chatGroupControllerProvider;

    private Consumer<Group> onGroupClicked;

    @Inject
    public ChatListController() {

    }

    @Override
    public Parent render() {
        Parent parent = super.render();

        disposables.add(groupListService.getGroups().observeOn(FX_SCHEDULER).subscribe(groups -> {
            this.groups.clear();
            this.groups.addAll(groups);
            updateGroupList();
        }));

        return parent;
    }

    public void setOnGroupClicked(Consumer<Group> onGroupClicked) {
        this.onGroupClicked = onGroupClicked;
    }

    private void addUser() {

    }

    private void createGroup() {

    }

    @Override
    public void destroy() {
        clearSubControllers();
        super.destroy();
    }

    @FXML
    public void showChats() {
        app.show(directMessageControllerProvider.get());
    }

    public void updateGroupList() {
        if (groups.size() == 0) {
            return;
        }

        clearSubControllers();

        // <ida>_<idb> is the name of a group with two members
        for (Group group : groups) {
            //noinspection StatementWithEmptyBody
            if (group.members().size() == 2 && (
                    group.name().equals(group.members().get(0) + "_" + group.members().get(1)))
                    || group.name().equals(group.members().get(0) + "_" + group.members().get(0))) {
              /*  ChatUserController chatUserController = chatUserControllerProvider.get();
                subControllers.add(chatUserController);
                chatList.getChildren().add(chatUserController.render());*/
            } else {
                ChatGroupController chatGroupController = chatGroupControllerProvider.get();
                subControllers.add(chatGroupController);
                chatGroupController.setOnGroupClicked(onGroupClicked);
                chatGroupController.setGroup(group, false);
                chatList.getChildren().add(chatGroupController.render());
            }
        }
    }

    private void clearSubControllers() {
        for (Controller controller : subControllers) {
            controller.destroy();
        }
        subControllers.clear();
        //userList.getChildren().clear();
        //groupList.getChildren().clear();
    }

}
