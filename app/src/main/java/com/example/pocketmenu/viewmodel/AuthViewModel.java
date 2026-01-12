package com.example.pocketmenu.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
//import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseUser;
import com.example.pocketmenu.data.repository.AuthRepository;

public class AuthViewModel extends ViewModel {

    private final AuthRepository repository;
    private final LiveData<FirebaseUser> userLiveData;
    private final LiveData<Boolean> loggedOutLiveData;
    private final LiveData<String> errorMessageLiveData;
    private final LiveData<Boolean> registrationSuccessLiveData;


    public AuthViewModel() {

        this.repository = new AuthRepository();
        this.userLiveData = repository.getUserLiveData();
        this.loggedOutLiveData = repository.getLoggedOutLiveData();
        this.errorMessageLiveData = repository.getErrorMessageLiveData();
        this.registrationSuccessLiveData = repository.getRegistrationSuccessLiveData();
    }

    // New user Auth and Firestore method
    public void registerNewUser(String email, String password, String name) {
        repository.registerNewUser(email, password, name);
    }

    // Login method
    public void logInSession(String email, String password) {
        repository.logInSession(email, password);
    }

    // Log out method
    public void logOutSession() {
        repository.logOutSession();
    }

    //Getters
    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }
    public LiveData<Boolean> getLoggedOutLiveData() {
        return loggedOutLiveData;
    }
    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<Boolean> getRegistrationSuccessLiveData() {
        return registrationSuccessLiveData;
    }

    //Setters



}
