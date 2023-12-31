package de.uniks.beastopia.teaml.controller.menu;

import de.uniks.beastopia.teaml.App;
import de.uniks.beastopia.teaml.controller.AppPreparer;
import de.uniks.beastopia.teaml.utils.Prefs;
import de.uniks.beastopia.teaml.utils.SoundController;
import de.uniks.beastopia.teaml.utils.ThemeSettings;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationTest;

import javax.inject.Provider;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class SettingsControllerTest extends ApplicationTest {
    @Mock
    Prefs prefs;
    @Mock
    Provider<ResourceBundle> resourcesProvider;
    @Mock
    Provider<SoundController> soundControllerProvider;
    @Mock
    SoundController soundController;
    @Mock
    Provider<KeybindElementController> keybindElementControllerProvider;
    @Spy
    ThemeSettings themeSettings;
    @Spy
    App app;
    @Spy
    final ResourceBundle resources = ResourceBundle.getBundle("de/uniks/beastopia/teaml/assets/lang", Locale.forLanguageTag("en"));

    @InjectMocks
    SettingsController settingsController;

    @Override
    public void start(Stage stage) {
        AppPreparer.prepare(app, prefs);
        when(prefs.getTheme()).thenReturn("dark");
        KeybindElementController mockedKeyBindController = mock(KeybindElementController.class);
        doNothing().when(mockedKeyBindController).setKeyAndAction(anyString(), anyString());
        when(keybindElementControllerProvider.get()).thenReturn(mockedKeyBindController);
        when(mockedKeyBindController.render()).thenAnswer(e -> new Pane());
        when(soundControllerProvider.get()).thenReturn(soundController);

        app.setHistory(List.of());
        app.start(stage);
        app.show(settingsController);
        stage.requestFocus();
    }

    @Test
    public void setDarkTheme() {
        doNothing().when(prefs).setTheme(any());
        Consumer<String> mocked = mock();
        doNothing().when(mocked).accept(any());
        themeSettings.updateSceneTheme = mocked;

        clickOn("#lightRadioButton");
        clickOn("#darkRadioButton");

        verify(prefs, times(1)).setTheme("dark");
        verify(mocked, times(1)).accept("dark");
    }

    @Test
    public void setLightTheme() {
        doNothing().when(prefs).setTheme(any());
        Consumer<String> mocked = mock();
        doNothing().when(mocked).accept(any());
        themeSettings.updateSceneTheme = mocked;

        clickOn("#lightRadioButton");

        verify(prefs, times(1)).setTheme("light");
        verify(mocked, times(1)).accept("light");
    }

    @Test
    public void backMenu() {
        MenuController mocked = mock();
        when(mocked.render()).thenReturn(new Label());
        doNothing().when(mocked).init();

        app.setHistory(List.of(mocked));

        clickOn("#backButton");

        verify(mocked).render();
    }

    @Test
    public void backPause() {
        PauseController mocked = mock();
        when(mocked.render()).thenReturn(new Label());
        doNothing().when(mocked).init();

        app.setHistory(List.of(mocked));

        clickOn("#backButton");

        verify(mocked).render();
    }

    @Test
    void title() {
        assertEquals(resources.getString("titleSettings"), app.getStage().getTitle());
    }

    @Test
    public void setDe() {
        when(resourcesProvider.get()).thenReturn(resources);
        doNothing().when(prefs).setLocale(Locale.GERMAN.toLanguageTag());

        clickOn("#selectGermanLanguage");
        verify(prefs, times(1)).setLocale(Locale.GERMAN.toLanguageTag());
    }

    @Test
    public void setEn() {
        when(resourcesProvider.get()).thenReturn(resources);
        doNothing().when(prefs).setLocale(Locale.GERMAN.toLanguageTag());
        doNothing().when(prefs).setLocale(Locale.ENGLISH.toLanguageTag());
        when(prefs.getLocale()).thenReturn(Locale.GERMAN.toLanguageTag());

        clickOn("#selectGermanLanguage");
        clickOn("#selectEnglishLanguage");

        verify(prefs, times(1)).setLocale(Locale.ENGLISH.toLanguageTag());
    }
}