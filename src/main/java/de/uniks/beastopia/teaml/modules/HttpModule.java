package de.uniks.beastopia.teaml.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;
import de.uniks.beastopia.teaml.Main;
import de.uniks.beastopia.teaml.rest.*;
import de.uniks.beastopia.teaml.rest.trainer.TrainerApiService;
import de.uniks.beastopia.teaml.service.TokenStorage;
import javafx.util.Pair;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;


@Module
public class HttpModule {
    private static final int MAX_REQUESTS = 10;
    private static final int MAX_REQUESTS_TIME_FRAME_SECONDS = 11;
    private static final List<Pair<Date, String>> LAST_REQUESTS = new ArrayList<>();
    private static final Semaphore SEMAPHORE = new Semaphore(1);

    private static int getRequestsLastTimeFrame() {
        long currentTime = new Date().getTime();
        long timeBeginFrame = currentTime - MAX_REQUESTS_TIME_FRAME_SECONDS * 1000;
        LAST_REQUESTS.removeIf(pair -> pair.getKey().getTime() < timeBeginFrame);
        return LAST_REQUESTS.size();
    }

    private static long getRequiredDelay() {
        long currentTime = new Date().getTime();
        long timeBeginFrame = currentTime - MAX_REQUESTS_TIME_FRAME_SECONDS * 1000;
        long timeOfFirstRequest = LAST_REQUESTS.get(0).getKey().getTime();
        return timeOfFirstRequest - timeBeginFrame;
    }

    @Provides
    @Singleton
    static OkHttpClient client(TokenStorage tokenStorage) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    try {
                        SEMAPHORE.acquire();
                        if (getRequestsLastTimeFrame() >= MAX_REQUESTS) {
                            long timeToSleep = getRequiredDelay();
                            System.out.println("Sleeping for " + timeToSleep + "ms");
                            for (Pair<Date, String> pair : LAST_REQUESTS) {
                                System.out.println("\t" + pair.getKey() + " " + pair.getValue());
                            }
                            Thread.sleep(timeToSleep);
                        }
                        LAST_REQUESTS.add(new Pair<>(new Date(), chain.request().url().toString()));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        SEMAPHORE.release();
                    }

                    final String token = tokenStorage.getAccessToken();
                    if (token == null) {
                        return chain.proceed(chain.request());
                    }
                    final Request newRequest = chain
                            .request()
                            .newBuilder()
                            .addHeader("Authorization", "Bearer " + token)
                            .build();

                    return chain.proceed(newRequest);
                }).build();
    }

    @Provides
    @Singleton
    Retrofit retrofit(OkHttpClient client, ObjectMapper objectMapper) {
        return new Retrofit.Builder()
                .baseUrl(Main.API_URL + "/")
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    UserApiService user(Retrofit retrofit) {
        return retrofit.create(UserApiService.class);
    }

    @Provides
    @Singleton
    AuthApiService auth(Retrofit retrofit) {
        return retrofit.create(AuthApiService.class);
    }

    @Provides
    @Singleton
    GroupApiService group(Retrofit retrofit) {
        return retrofit.create(GroupApiService.class);
    }

    @Provides
    @Singleton
    MessageApiService message(Retrofit retrofit) {
        return retrofit.create(MessageApiService.class);
    }

    @Provides
    @Singleton
    RegionApiService region(Retrofit retrofit) {
        return retrofit.create(RegionApiService.class);
    }

    @Provides
    @Singleton
    TrainerApiService trainer(Retrofit retrofit) {
        return retrofit.create(TrainerApiService.class);
    }

    @Provides
    @Singleton
    AreaApiService area(Retrofit retrofit) {
        return retrofit.create(AreaApiService.class);
    }

    @Provides
    @Singleton
    PresetsApiService presets(Retrofit retrofit) {
        return retrofit.create(PresetsApiService.class);
    }
}
