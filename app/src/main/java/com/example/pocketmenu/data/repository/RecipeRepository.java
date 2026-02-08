package com.example.pocketmenu.data.repository;

import com.example.pocketmenu.data.model.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class RecipeRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public RecipeRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private String getUserId() {
        return auth.getCurrentUser().getUid();
    }

    public Query getRecipesQuery(String searchText) {
        Query query = db.collection("RECIPES").whereEqualTo("userId", getUserId());
        if (searchText != null && !searchText.isEmpty()) {
            query = query.orderBy("name").startAt(searchText).endAt(searchText + '\uf8ff');
        } else {
            query = query.orderBy("name", Query.Direction.ASCENDING);
        }
        return query;
    }

    public void addRecipe(Recipe recipe) {
        db.collection("RECIPES").add(recipe);
    }

    public void updateFavorite(String recipeId, boolean newValue) {
        db.collection("RECIPES").document(recipeId).update("favorite", newValue);
    }

    public void updateRecipe(String recipeId, Recipe recipe) {
        db.collection("RECIPES").document(recipeId).set(recipe);
    }

    public void deleteRecipe(String recipeId) {
        db.collection("RECIPES").document(recipeId).delete();
    }
}