package com.example.pocketmenu.data.repository;

import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.pocketmenu.data.model.User;

public class AuthRepository {

    //Firebase instances
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firebaseFirestore;

    //LiveData instances
    private final MutableLiveData<FirebaseUser> userLiveData;
    private final MutableLiveData<Boolean> loggedOutLiveData;

    //Constants for Firestore collection
    private static final String USERS_COLLECTION = "USERS";

    //Constructor
    public AuthRepository() {
        //Initialize instances
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firebaseFirestore = FirebaseFirestore.getInstance();
        this.userLiveData = new MutableLiveData<>();
        this.loggedOutLiveData = new MutableLiveData<>();

        //Check open session
        if (firebaseAuth.getCurrentUser() != null) {
            userLiveData.postValue(firebaseAuth.getCurrentUser());
            loggedOutLiveData.postValue(false);
        }

    }

    //LiveData getters
    public MutableLiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public MutableLiveData<Boolean> getLoggedOutLiveData() {
        return loggedOutLiveData;
    }

    //New user Auth and Firestore
    public void registerNewUser(String email, String password, String name) {
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                if (firebaseUser != null) {
                    //Create new user
                    User newUser = new User(firebaseUser.getUid(), name, email);

                    //Save user in Firestore
                    firebaseFirestore.collection(USERS_COLLECTION).document(firebaseUser.getUid()).set(newUser).addOnSuccessListener(Void -> {
                        userLiveData.postValue(firebaseUser);
                    }).addOnFailureListener(e -> {
                        userLiveData.postValue(null);
                    });
                }
            } else {
                userLiveData.postValue(null);
            }
        });
    }

    //Login method
    public void logInSession(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                userLiveData.postValue(firebaseAuth.getCurrentUser());
            } else {
                userLiveData.postValue(null);
            }
        });
    }

    //Log out method
    public void logOutSession(){
        firebaseAuth.signOut();
        loggedOutLiveData.postValue(true);
    }
}
