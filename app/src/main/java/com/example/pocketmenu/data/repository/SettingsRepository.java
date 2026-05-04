package com.example.pocketmenu.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.concurrent.atomic.AtomicInteger;

public class SettingsRepository {
    // Firebase instances
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    //LiveData instances
    private final MutableLiveData<Boolean> loggedOutLiveData;
    private final MutableLiveData<Boolean> accountDeletedLiveData;
    private final MutableLiveData<String> errorMessageLiveData;

    // Constants for Firestore collection
    private static final String COLLECTION_USERS = "USERS";
    private static final String COLLECTION_RECIPES = "RECIPES";
    private static final String COLLECTION_MENUS = "MENUS";
    private static final String COLLECTION_LEFTOVERS = "LEFTOVERS";
    private static final String COLLECTION_SHOPPING_LIST_ITEMS = "SHOPPING_LIST_ITEMS";
    private static final String COLLECTION_WEEKLY_MENU_TEMPLATES = "WEEKLY_MENU_TEMPLATES";
    private static final String COLLECTION_PRODUCTS = "PRODUCTS";
    private static final String FIELD_USER_ID = "userId";

    public SettingsRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.loggedOutLiveData = new MutableLiveData<>();
        this.accountDeletedLiveData = new MutableLiveData<>();
        this.errorMessageLiveData = new MutableLiveData<>();
    }

    // LiveData getters
    public LiveData<Boolean> getLoggedOutLiveData() {
        return loggedOutLiveData;
    }
    public LiveData<Boolean> getAccountDeletedLiveData() {
        return accountDeletedLiveData;
    }
    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public void logOutSession() {
        auth.signOut();
        loggedOutLiveData.postValue(true);
    }

    public void deleteAccount() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            errorMessageLiveData.postValue("No hay ningún usuario activo");
            return;
        }
        String uid = currentUser.getUid();
        // All collections except COLLECTION_USER
        String[] userCollections = {
                COLLECTION_RECIPES,
                COLLECTION_MENUS,
                COLLECTION_LEFTOVERS,
                COLLECTION_SHOPPING_LIST_ITEMS,
                COLLECTION_WEEKLY_MENU_TEMPLATES,
                COLLECTION_PRODUCTS
        };

        AtomicInteger pendingCollections = new AtomicInteger(userCollections.length);
        AtomicInteger failedCollections = new AtomicInteger(0);

        // Deletes collections
        for (String collection : userCollections) {
            deleteUserDocumentsFromCollection(collection, uid, pendingCollections, failedCollections, currentUser);
        }
    }

    // Aux methods
    private void deleteUserDocumentsFromCollection(String collection, String uid,
                                                   AtomicInteger pendingCollections,
                                                   AtomicInteger failedCollections,
                                                   FirebaseUser currentUser) {
        // Checks all documents where userID equals to the current user
        db.collection(collection)
                .whereEqualTo(FIELD_USER_ID, uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        checkAllCollectionsDeleted(pendingCollections, failedCollections, currentUser);
                        return;
                    }
                    // Counts documents in the collection
                    AtomicInteger pendingDocs = new AtomicInteger(querySnapshot.size());

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    if (pendingDocs.decrementAndGet() == 0) {
                                        checkAllCollectionsDeleted(pendingCollections, failedCollections, currentUser);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    errorMessageLiveData.postValue(
                                            "Error al eliminar datos de " + collection + ": " + e.getMessage());
                                    failedCollections.incrementAndGet();
                                    checkAllCollectionsDeleted(pendingCollections, failedCollections, currentUser);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessageLiveData.postValue(
                            "Error al acceder a " + collection + ": " + e.getMessage());
                    failedCollections.incrementAndGet();
                    checkAllCollectionsDeleted(pendingCollections, failedCollections, currentUser);
                });
    }
    // Ensures all documents are deleted before deleting the account
    private void checkAllCollectionsDeleted(AtomicInteger pendingCollections,
                                            AtomicInteger failedCollections,
                                            FirebaseUser currentUser) {
        if (pendingCollections.decrementAndGet() == 0) {
            if (failedCollections.get() > 0) {
                errorMessageLiveData.postValue(
                        "No se pudo eliminar la cuenta por completo. Inténtalo de nuevo.");
                return;
            }
            // Deletes COLLECTION_USERS
            db.collection(COLLECTION_USERS)
                    .document(currentUser.getUid())
                    .delete()
                    .addOnSuccessListener(aVoid ->
                            // Deletes user from Firebase
                            currentUser.delete()
                                    .addOnSuccessListener(authVoid ->
                                            accountDeletedLiveData.postValue(true))
                                    .addOnFailureListener(e -> errorMessageLiveData.postValue(
                                            "Error al eliminar la cuenta: " + e.getMessage())))
                    .addOnFailureListener(e -> errorMessageLiveData.postValue(
                            "Error al eliminar el perfil: " + e.getMessage()));
        }
    }
}