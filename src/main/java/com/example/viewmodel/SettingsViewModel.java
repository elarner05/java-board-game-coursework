package com.example.viewmodel;

import com.example.service.NavigationService;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import com.example.model.GameModel;
import com.example.model.config.LangManager;

public class SettingsViewModel {
    
    private NavigationService navigationService;
    private GameModel gameModel;
    public ObservableMap<String, String> availableLanguages = FXCollections.observableHashMap();
    public StringProperty selectedLanguage = new SimpleStringProperty();

    public SettingsViewModel(GameModel gameModel, NavigationService navigationService) {
        this.gameModel = gameModel;
        this.navigationService = navigationService;
        availableLanguages.putAll(LangManager.getAvailableLanguages());
        selectedLanguage.set(LangManager.getCurrentLanguage());
        selectedLanguage.addListener((obs, oldLang, newLang) -> {
        if (newLang != null && !newLang.equals(oldLang)) {
            LangManager.setLanguage(newLang);
        }
    });
    }

    public void playGame() {
        SetupViewModel setupVM = new SetupViewModel(gameModel, navigationService);
        navigationService.navigateTo("setupScreen", setupVM);
    }
}
