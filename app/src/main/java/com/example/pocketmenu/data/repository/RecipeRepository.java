package com.example.pocketmenu.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pocketmenu.data.model.Ingredient;
import com.example.pocketmenu.data.model.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecipeRepository {

    public static final String COLLECTION_PATH = "RECIPES";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private final MutableLiveData<List<Recipe>> recipesLiveData;
    private final MutableLiveData<Boolean> operationSuccessLiveData;
    private final MutableLiveData<String> errorMessageLiveData;

    public RecipeRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        recipesLiveData = new MutableLiveData<>();
        operationSuccessLiveData = new MutableLiveData<>();
        errorMessageLiveData = new MutableLiveData<>();
    }

    public LiveData<List<Recipe>> getRecipesLiveData() { return recipesLiveData; }
    public LiveData<Boolean> getOperationSuccessLiveData() { return operationSuccessLiveData; }
    public LiveData<String> getErrorMessageLiveData() { return errorMessageLiveData; }

    public void getRecipes(String searchText) {
        String uid = getUserId();
        if (uid == null) {
            errorMessageLiveData.postValue("Usuario no autenticado");
            return;
        }

        Query query = db.collection(COLLECTION_PATH).whereEqualTo("userId", uid);

        if (searchText != null && !searchText.isEmpty()) {
            query = query.orderBy("name")
                    .startAt(searchText)
                    .endAt(searchText + '\uf8ff');
        } else {
            query = query.orderBy("name");
        }

        query.get().addOnSuccessListener(snap -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        recipe.setId(doc.getId());
                        recipes.add(recipe);
                    }
                    recipesLiveData.postValue(recipes);
                })
                .addOnFailureListener(e -> errorMessageLiveData.postValue(e.getMessage()));
    }

    public void addRecipe(Recipe recipe) {
        db.collection(COLLECTION_PATH)
                .add(recipe)
                .addOnSuccessListener(docRef -> operationSuccessLiveData.postValue(true))
                .addOnFailureListener(e -> errorMessageLiveData.postValue(e.getMessage()));
    }

    public void updateFavorite(String recipeId, boolean newValue) {
        db.collection(COLLECTION_PATH)
                .document(recipeId)
                .update("favorite", newValue)
                .addOnSuccessListener(aVoid -> operationSuccessLiveData.postValue(true))
                .addOnFailureListener(e -> errorMessageLiveData.postValue(e.getMessage()));
    }

    public void updateRecipe(String recipeId, Recipe recipe) {
        db.collection(COLLECTION_PATH)
                .document(recipeId)
                .set(recipe)
                .addOnSuccessListener(aVoid -> operationSuccessLiveData.postValue(true))
                .addOnFailureListener(e -> errorMessageLiveData.postValue(e.getMessage()));
    }

    public void deleteRecipe(String recipeId) {
        db.collection(COLLECTION_PATH)
                .document(recipeId)
                .delete()
                .addOnSuccessListener(aVoid -> operationSuccessLiveData.postValue(true))
                .addOnFailureListener(e -> errorMessageLiveData.postValue(e.getMessage()));
    }

    public interface OnRecipeFound {
        void onFound(Recipe recipe);
        void onNotFound();
        void onFailure(Exception e);
    }

    public interface OnIngredientsLoaded {
        void onLoaded(List<Ingredient> ingredients);
        void onFailure(Exception e);
    }

    // Kept as callback because it's used internally between modules
    public void getRecipeById(String recipeId, OnRecipeFound callback) {
        db.collection(COLLECTION_PATH)
                .document(recipeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe != null) recipe.setId(doc.getId());
                        callback.onFound(recipe);
                    } else {
                        callback.onNotFound();
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getIngredientSuggestions(String prefix, OnIngredientsLoaded callback) {
        String uid = getUserId();
        if (uid == null) return;
        db.collection(COLLECTION_PATH)
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    Map<String, Ingredient> seen = new LinkedHashMap<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe.getIngredients() != null) {
                            for (Ingredient ing : recipe.getIngredients()) {
                                if (ing.getName() != null && !ing.getName().isEmpty()
                                        && ing.getName().toLowerCase()
                                        .startsWith(prefix.toLowerCase())) {
                                    seen.putIfAbsent(ing.getName().toLowerCase(), ing);
                                }
                            }
                        }
                    }
                    callback.onLoaded(new ArrayList<>(seen.values()));
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    private String getUserId() {
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return null;
    }
}