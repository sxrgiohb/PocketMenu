package com.example.pocketmenu.data.repository;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.pocketmenu.data.model.User;

public class AuthRepository {

    //Firebase instances
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    //LiveData instances
    private final MutableLiveData<FirebaseUser> userLiveData;
    private final MutableLiveData<Boolean> loggedOutLiveData;
    private final MutableLiveData<String> errorMessageLiveData;
    private final MutableLiveData<Boolean> registrationSuccessLiveData;


    //Constants for Firestore collection
    private static final String USERS_COLLECTION = "USERS";

    //Constructor
    public AuthRepository() {
        //Initialize instances
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.userLiveData = new MutableLiveData<>();
        this.loggedOutLiveData = new MutableLiveData<>();
        this.errorMessageLiveData = new MutableLiveData<>();
        this.registrationSuccessLiveData = new MutableLiveData<>();


        //Check open session
        if (auth.getCurrentUser() != null) {
            userLiveData.postValue(auth.getCurrentUser());
        }

    }

    //LiveData getters
    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Boolean> getLoggedOutLiveData() {
        return loggedOutLiveData;
    }

    public LiveData<Boolean> getRegistrationSuccessLiveData() {
        return registrationSuccessLiveData;
    }

    //New user Auth and Firestore
    public void registerNewUser(String email, String password, String name) {

        registrationSuccessLiveData.postValue(null);

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = auth.getCurrentUser();
                if (firebaseUser != null) {
                    //Create new user
                    User newUser = new User(firebaseUser.getUid(), name, email);
                    //Save user in Firestore
                    db.collection(USERS_COLLECTION).document(firebaseUser.getUid()).set(newUser).addOnSuccessListener(Void -> {
                        userLiveData.postValue(firebaseUser);
                        registrationSuccessLiveData.postValue(true);
                    }).addOnFailureListener(e -> {
                        String errorMessage = e.getMessage() != null
                                ? e.getMessage()
                                : "Error desconocido";

                        errorMessageLiveData.postValue("Error de registro: " + errorMessage);
                        registrationSuccessLiveData.postValue(false);
                    });
                }
            } else {
                if (task.getException() != null) {
                    errorMessageLiveData.postValue(task.getException().getMessage());
                } else {
                    errorMessageLiveData.postValue("Se ha producido un error desconocido");
                }
                registrationSuccessLiveData.postValue(false);
            }
        });
    }

    //Login method
    public void logInSession(String email, String password) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                userLiveData.postValue(auth.getCurrentUser());
            } else {
                String errorMessage = "Error al iniciar sesión";
                if (task.getException() != null) {
                    errorMessage = task.getException().getMessage();
                }
                errorMessageLiveData.postValue(errorMessage);
            }
        });
    }

    //Log out method
    public void logOutSession(){
        auth.signOut();
        loggedOutLiveData.postValue(true);
    }
}
