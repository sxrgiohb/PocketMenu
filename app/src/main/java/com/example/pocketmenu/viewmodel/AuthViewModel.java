package com.example.pocketmenu.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
//import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseUser;
import com.example.pocketmenu.data.repository.AuthRepository;

public class AuthViewModel extends ViewModel {

    private final AuthRepository repository;
    private final LiveData<FirebaseUser> userLiveData;
    private final LiveData<Boolean> loggedOutLiveData;

    public AuthViewModel() {

        this.repository = new AuthRepository();
        this.userLiveData = repository.getUserLiveData();
        this. loggedOutLiveData = repository.getLoggedOutLiveData();
    }

    // New user Auth and Firestore
    public void registerNewUser(String email, String password, String name) {
        repository.registerNewUser(email, password, name);
    }

    // Login method
    public void loginSession(String email, String password) {
        repository.loginSession(email, password);
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
}
