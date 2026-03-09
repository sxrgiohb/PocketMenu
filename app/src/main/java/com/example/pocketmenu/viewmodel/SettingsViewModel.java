package com.example.pocketmenu.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketmenu.data.repository.SettingsRepository;

public class SettingsViewModel extends ViewModel {

    private final SettingsRepository repository;
    private final LiveData<Boolean> loggedOutLiveData;
    private final LiveData<Boolean> accountDeletedLiveData;
    private final LiveData<String> errorMessageLiveData;

    public SettingsViewModel() {
        this.repository = new SettingsRepository();
        this.loggedOutLiveData = repository.getLoggedOutLiveData();
        this.accountDeletedLiveData = repository.getAccountDeletedLiveData();
        this.errorMessageLiveData = repository.getErrorMessageLiveData();
    }

    public void logOutSession() {
        repository.logOutSession();
    }

    public void deleteAccount() {
        repository.deleteAccount();
    }

    public LiveData<Boolean> getLoggedOutLiveData() {
        return loggedOutLiveData;
    }

    public LiveData<Boolean> getAccountDeletedLiveData() {
        return accountDeletedLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }
}