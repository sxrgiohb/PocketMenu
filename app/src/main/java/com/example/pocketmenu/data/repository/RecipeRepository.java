package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RecipeRepository {

    public static final String COLLECTION_PATH = "RECIPES";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public RecipeRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String getUserId() {
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return null;
    }

    public interface RecipeCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnRecipeFound {
        void onFound(Recipe recipe);
        void onNotFound();
        void onFailure(Exception e);
    }

    public interface OnRecipesLoaded {
        void onLoaded(List<Recipe> recipes);
        void onFailure(Exception e);
    }

    public void getRecipes(String searchText, OnRecipesLoaded callback) {
        String uid = getUserId();
        if (uid == null) {
            callback.onFailure(new Exception("Usuario no autenticado"));
            return;
        }

        com.google.firebase.firestore.Query query =
                db.collection(COLLECTION_PATH).whereEqualTo("userId", uid);

        if (searchText != null && !searchText.isEmpty()) {
            query = query.orderBy("name")
                    .startAt(searchText)
                    .endAt(searchText + '\uf8ff');
        } else {
            query = query.orderBy("name");
        }

        query.get()
                .addOnSuccessListener(snap -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        recipe.setId(doc.getId());
                        recipes.add(recipe);
                    }
                    callback.onLoaded(recipes);
                })
                .addOnFailureListener(callback::onFailure);
    }

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

    public void getRecipeById(String recipeId, OnRecipeFound callback) {
        db.collection(COLLECTION_PATH)
                .document(recipeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) callback.onFound(doc.toObject(Recipe.class));
                    else callback.onNotFound();
                })
                .addOnFailureListener(callback::onFailure);
    }
}