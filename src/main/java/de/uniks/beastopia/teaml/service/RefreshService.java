package de.uniks.beastopia.teaml.service;

import de.uniks.beastopia.teaml.rest.AuthApiService;
import de.uniks.beastopia.teaml.rest.LoginResult;
import de.uniks.beastopia.teaml.rest.RefreshDto;
import io.reactivex.rxjava3.core.Observable;

import javax.inject.Inject;

public class RefreshService {
    @Inject
    AuthApiService authApiService;
    @Inject
    public RefreshService() {
    }

    public Observable<LoginResult> refresh(String refreshToken){
        return authApiService.refresh(new RefreshDto(refreshToken));
    }
}
