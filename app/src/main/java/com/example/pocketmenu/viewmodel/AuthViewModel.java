package com.example.pocketmenu.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import com.google.firebase.auth.FirebaseUser;
import com.example.pocketmenu.data.repository.AuthRepository;

public class AuthViewModel extends ViewModel {

    private final AuthRepository repository;
    private final LiveData<FirebaseUser> userLiveData;
    private final LiveData<String> errorMessageLiveData;
    private final LiveData<Boolean> registrationSuccessLiveData;

    // Constructor
    public AuthViewModel() {

        this.repository = new AuthRepository();
        this.userLiveData = repository.getUserLiveData();
        this.errorMessageLiveData = repository.getErrorMessageLiveData();
        this.registrationSuccessLiveData = repository.getRegistrationSuccessLiveData();
    }

    // New user method
    public void registerNewUser(String email, String password, String name) {
        repository.registerNewUser(email, password, name);
    }

    // Login method
    public void logInSession(String email, String password) {
        repository.logInSession(email, password);
    }

    //Getters
    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<Boolean> getRegistrationSuccessLiveData() {
        return registrationSuccessLiveData;
    }
}
