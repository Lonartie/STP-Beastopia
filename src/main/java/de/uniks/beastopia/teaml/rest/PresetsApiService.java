package de.uniks.beastopia.teaml.rest;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;

public interface PresetsApiService {

    @GET("presets/tilesets/{filename}")
    Observable<ResponseBody> getTileset(@Path("filename") String filename);

    @GET("presets/characters")
    Observable<List<String>> getCharacters();

    @GET("presets/characters/{filename}")
    Observable<ResponseBody> getCharacterSprites(@Path("filename") String fileName);

    @GET("presets/monsters")
    Observable<List<MonsterTypeDto>> getMonsters();

    @GET("presets/monsters/{type}")
    Observable<MonsterTypeDto> getMonsterType(@Path("type") int type);

    @GET("presets/monsters/{type}/image")
    Observable<ResponseBody> getMonsterImage(@Path("type") int type);

    @GET("presets/abilities/{id}")
    Observable<AbilityDto> getAbility(@Path("id") int type);

    @GET("presets/abilities")
    Observable<List<AbilityDto>> getAbilities();

    @GET("presets/items")
    Observable<List<ItemTypeDto>> getItems();

    @GET("presets/items/{itemId}")
    Observable<ItemTypeDto> getItem(@Path("itemId") int itemId);

    @GET("presets/items/{itemId}/image")
    Observable<ResponseBody> getItemImage(@Path("itemId") int itemId);
}