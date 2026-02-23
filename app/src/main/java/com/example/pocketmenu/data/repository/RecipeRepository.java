package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class RecipeRepository {

    public static final String COLLECTION_PATH = "RECIPES";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public RecipeRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String getUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        } else {
            return null;
        }
    }

    public Query getRecipesQuery(String searchText) {
        String uid = getUserId();
        if (uid == null) {
            return db.collection(COLLECTION_PATH).limit(0);
        }

        Query query = db.collection(COLLECTION_PATH).whereEqualTo("userId", uid);

        if (searchText != null && !searchText.isEmpty()) {
            query = query.orderBy("name").startAt(searchText).endAt(searchText + '\uf8ff');
        } else {
            query = query.orderBy("name", Query.Direction.ASCENDING);
        }

        return query;
    }

    // Callback interface
    public interface RecipeCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Add recipe
    public void addRecipe(Recipe recipe, RecipeCallback callback) {
        db.collection(COLLECTION_PATH)
                .add(recipe)
                .addOnSuccessListener(docRef -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // Update favorite
    public void updateFavorite(String recipeId, boolean newValue, RecipeCallback callback) {
        db.collection(COLLECTION_PATH)
                .document(recipeId)
                .update("favorite", newValue)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // Update recipe
    public void updateRecipe(String recipeId, Recipe recipe, RecipeCallback callback) {
        db.collection(COLLECTION_PATH)
                .document(recipeId)
                .set(recipe)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // Delete recipe
    public void deleteRecipe(String recipeId, RecipeCallback callback) {
        db.collection(COLLECTION_PATH)
                .document(recipeId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // Callback para obtener una receta por ID
    public interface OnRecipeFound {
        void onFound(Recipe recipe);
        void onNotFound();
        void onFailure(Exception e);
    }

    // Get recipe by ID
    public void getRecipeById(String recipeId, OnRecipeFound callback) {
        db.collection(COLLECTION_PATH)
                .document(recipeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onFound(doc.toObject(Recipe.class));
                    } else {
                        callback.onNotFound();
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
}