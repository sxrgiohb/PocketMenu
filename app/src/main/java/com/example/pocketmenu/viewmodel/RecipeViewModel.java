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

    // Constructor
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

    // Method to load recipes
    public void loadRecipes(String searchText) {
        repository.getRecipes(searchText);
    }

    // Method to add a new recipe
    public void addRecipe(Recipe recipe) {
        repository.addRecipe(recipe);
    }

    // Method to change the favorite status of a recipe
    public void toggleFavorite(String recipeId, boolean isFavorite) {
        repository.updateFavorite(recipeId, !isFavorite);
    }

    // Method to change the data of a recipe
    public void updateRecipe(String recipeId, Recipe recipe) {
        repository.updateRecipe(recipeId, recipe);
    }
    // Method to delete a recipe
    public void deleteRecipe(String recipeId) {
        repository.deleteRecipe(recipeId);
    }

    // Method to search ingredient suggestions
    public void searchIngredientSuggestions(String prefix, RecipeRepository.OnIngredientsLoaded callback) {
        if (prefix == null || prefix.trim().isEmpty()) {
            // Returns an empty list if the prefix is empty to avoid unnecessary queries
            callback.onLoaded(new ArrayList<>());
            return;
        }
        repository.getIngredientSuggestions(prefix.trim(), callback);
    }
}