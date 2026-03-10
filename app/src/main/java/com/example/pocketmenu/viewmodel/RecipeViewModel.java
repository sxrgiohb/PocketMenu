package com.example.pocketmenu.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketmenu.data.model.Recipe;
import com.example.pocketmenu.data.repository.RecipeRepository;

import java.util.List;

public class RecipeViewModel extends ViewModel {

    private final RecipeRepository repository;
    private final LiveData<List<Recipe>> recipes;
    private final LiveData<Boolean> operationSuccess;
    private final LiveData<String> errorMessage;

    public RecipeViewModel() {
        repository = new RecipeRepository();
        recipes = repository.getRecipesLiveData();
        operationSuccess = repository.getOperationSuccessLiveData();
        errorMessage = repository.getErrorMessageLiveData();
        repository.getRecipes(null);
    }

    // Getters
    public LiveData<List<Recipe>> getRecipes() {
        return recipes;
    }
    public LiveData<Boolean> getOperationSuccess() {
        return operationSuccess;
    }
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Methods
    public void loadRecipes(String searchText) {
        repository.getRecipes(searchText);
    }

    public void addRecipe(Recipe recipe) {
        repository.addRecipe(recipe);
    }

    public void toggleFavorite(String recipeId, boolean isFavorite) {
        repository.updateFavorite(recipeId, !isFavorite); // Inverter
    }

    public void updateRecipe(String recipeId, Recipe recipe) {
        repository.updateRecipe(recipeId, recipe);
    }

    public void deleteRecipe(String recipeId) {
        repository.deleteRecipe(recipeId);
    }
}