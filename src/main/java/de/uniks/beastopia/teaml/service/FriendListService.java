package de.uniks.beastopia.teaml.service;

import de.uniks.beastopia.teaml.rest.UpdateUserDto;
import de.uniks.beastopia.teaml.rest.User;
import de.uniks.beastopia.teaml.rest.UserApiService;
import io.reactivex.rxjava3.core.Observable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class FriendListService {

    public enum Status {
        Online,
        Offline
    }

    @Inject
    TokenStorage tokenStorage;

    @Inject
    UserApiService userApiService;

    @Inject
    public FriendListService() {
    }

    public Observable<List<User>> getUsers() {
        return userApiService.getUsers(null, null);
    }

    public List<String> getFriendIDs() {
        return tokenStorage.getCurrentUser().friends();
    }

    public Observable<User> getUser(String id) {
        return userApiService.getUser(id);
    }

    public Observable<List<User>> getFriends() {
        if (tokenStorage.getCurrentUser().friends().isEmpty())
            return Observable.just(List.of());
        return userApiService
                .getUsers(tokenStorage.getCurrentUser().friends(), null);
    }

    public Observable<List<User>> getFriends(Status status) {
        if (tokenStorage.getCurrentUser().friends().isEmpty())
            return Observable.just(List.of());
        return userApiService
                .getUsers(tokenStorage.getCurrentUser().friends(), switch (status) {
                    case Online -> "online";
                    case Offline -> "offline";
                });
    }

    public boolean isFriend(User user) {
        return tokenStorage.getCurrentUser().friends().contains(user._id());
    }

    public Observable<User> addFriend(User friend) {
        return Observable.create(source -> {
            List<String> friends = new ArrayList<>(tokenStorage.getCurrentUser().friends());
            friends.add(friend._id());
            String userID = tokenStorage.getCurrentUser()._id();
            tokenStorage.setCurrentUser(userApiService.updateUser(userID, new UpdateUserDto(null, null, null, friends, null)).blockingFirst());
            source.onNext(tokenStorage.getCurrentUser());
            source.onComplete();
        });
    }

    public Observable<User> removeFriend(User friend) {
        return Observable.create(source -> {
            List<String> friends = new ArrayList<>(tokenStorage.getCurrentUser().friends());
            friends.remove(friend._id());
            String userID = tokenStorage.getCurrentUser()._id();
            tokenStorage.setCurrentUser(userApiService.updateUser(userID, new UpdateUserDto(null, null, null, friends, null)).blockingFirst());
            source.onNext(tokenStorage.getCurrentUser());
            source.onComplete();
        });
    }
}
