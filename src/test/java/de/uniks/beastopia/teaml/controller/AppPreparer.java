package de.uniks.beastopia.teaml.controller;

import de.uniks.beastopia.teaml.App;
import de.uniks.beastopia.teaml.DaggerMainComponent;
import de.uniks.beastopia.teaml.MainComponent;
import de.uniks.beastopia.teaml.controller.auth.LoginController;
import de.uniks.beastopia.teaml.sockets.EventListener;
import de.uniks.beastopia.teaml.utils.Prefs;
import io.reactivex.rxjava3.core.Observable;
import javafx.scene.control.Label;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppPreparer {
    public static void prepare(App app) {
        MainComponent mockedMainComponent = mock();
        MainComponent realMainComponent = DaggerMainComponent.builder().mainApp(app).build();
        LoginController mockedLoginController = mock();
        when(mockedLoginController.render()).thenReturn(new Label());
        Prefs mockedPrefs = mock(Prefs.class);
        when(mockedPrefs.isRememberMe()).thenReturn(false);
        when(mockedPrefs.getTheme()).thenReturn("dark");
        when(mockedMainComponent.loginController()).thenReturn(mockedLoginController);
        when(mockedMainComponent.prefs()).thenReturn(mockedPrefs);
        when(mockedMainComponent.themeSettings()).thenAnswer(i -> realMainComponent.themeSettings());
        app.setMainComponent(mockedMainComponent);
    }

    public static void prepare(App app, Prefs prefs) {
        when(prefs.getLocale()).thenReturn("en");
        MainComponent mockedMainComponent = mock();
        MainComponent realMainComponent = DaggerMainComponent.builder().mainApp(app).build();
        LoginController mockedLoginController = mock();
        when(mockedLoginController.render()).thenReturn(new Label());
        Prefs mockedPrefs = mock(Prefs.class);
        when(mockedPrefs.isRememberMe()).thenReturn(false);
        when(mockedPrefs.getTheme()).thenReturn("dark");
        when(mockedMainComponent.loginController()).thenReturn(mockedLoginController);
        when(mockedMainComponent.prefs()).thenReturn(mockedPrefs);
        when(mockedMainComponent.themeSettings()).thenAnswer(i -> realMainComponent.themeSettings());
        app.setMainComponent(mockedMainComponent);
    }

    public static void prepare(App app, EventListener eventListener) {
        when(eventListener.listen(any(), any())).thenReturn(Observable.empty());
        MainComponent mockedMainComponent = mock();
        MainComponent realMainComponent = DaggerMainComponent.builder().mainApp(app).build();
        LoginController mockedLoginController = mock();
        when(mockedLoginController.render()).thenReturn(new Label());
        Prefs mockedPrefs = mock(Prefs.class);
        when(mockedPrefs.isRememberMe()).thenReturn(false);
        when(mockedPrefs.getTheme()).thenReturn("dark");
        when(mockedMainComponent.loginController()).thenReturn(mockedLoginController);
        when(mockedMainComponent.prefs()).thenReturn(mockedPrefs);
        when(mockedMainComponent.themeSettings()).thenAnswer(i -> realMainComponent.themeSettings());
        app.setMainComponent(mockedMainComponent);
    }
}
