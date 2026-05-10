package com.example.pocketmenu.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketmenu.data.model.Recipe;
import com.example.pocketmenu.data.repository.RecipeRepository;

import java.util.ArrayList;
import java.util.List;

public class RecipeViewModel extends ViewModel {

    private final RecipeRepository repository;
    private final LiveData<List<Recipe>> recipes;
    private final LiveData<Boolean> operationSuccess;
    private final LiveData<String> errorMessage;

    // Constructor initializes streams and loads initial recipe set
    public RecipeViewModel() {
        repository = new RecipeRepository();
        recipes = repository.getRecipesLiveData();
        operationSuccess = repository.getOperationSuccessLiveData();
        errorMessage = repository.getErrorMessageLiveData();
        repository.getRecipes(null);
    }

    // Getters
    public LiveData<List<Recipe>> getRecipes() { return recipes; }
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    // Reloads recipes, optionally filtered by a search string
    public void loadRecipes(String searchText) {
        repository.getRecipes(searchText);
    }

    // Creates a new recipe document
    public void addRecipe(Recipe recipe) {
        repository.addRecipe(recipe);
    }

    // Flips favorite state from current value
    public void toggleFavorite(String recipeId, boolean isFavorite) {
        repository.updateFavorite(recipeId, !isFavorite);
    }

    // Updates recipe data while keeping the same id
    public void updateRecipe(String recipeId, Recipe recipe) {
        repository.updateRecipe(recipeId, recipe);
    }

    // Deletes a recipe by document id
    public void deleteRecipe(String recipeId) {
        repository.deleteRecipe(recipeId);
    }

    // Provides autocomplete ingredient names for recipe editors
    public void searchIngredientSuggestions(String prefix, RecipeRepository.OnIngredientsLoaded callback) {
        if (prefix == null || prefix.trim().isEmpty()) {
            // Returns an empty list if the prefix is empty to avoid unnecessary queries
            callback.onLoaded(new ArrayList<>());
            return;
        }
        repository.getIngredientSuggestions(prefix.trim(), callback);
    }
}