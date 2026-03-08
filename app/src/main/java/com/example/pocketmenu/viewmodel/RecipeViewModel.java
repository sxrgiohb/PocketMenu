package com.example.pocketmenu.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketmenu.data.model.Recipe;
import com.example.pocketmenu.data.repository.RecipeRepository;

import java.util.List;

public class RecipeViewModel extends ViewModel {

    private final RecipeRepository repository;

    private final MutableLiveData<List<Recipe>> recipes = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();

    public RecipeViewModel() {
        repository = new RecipeRepository();
        loadRecipes(null);
    }

    public LiveData<List<Recipe>> getRecipes() { return recipes; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }

    public void loadRecipes(String searchText) {
        repository.getRecipes(searchText, new RecipeRepository.OnRecipesLoaded() {
            @Override
            public void onLoaded(List<Recipe> loaded) {
                recipes.postValue(loaded);
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
            }
        });
    }

    public void addRecipe(Recipe recipe) {
        repository.addRecipe(recipe, new RecipeRepository.RecipeCallback() {
            @Override
            public void onSuccess() {
                loadRecipes(null);
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
            }
        });
    }

    public void toggleFavorite(String recipeId, boolean isFavorite) {
        repository.updateFavorite(recipeId, !isFavorite, new RecipeRepository.RecipeCallback() {
            @Override
            public void onSuccess() {
                loadRecipes(null);
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
            }
        });
    }

    public void updateRecipe(String recipeId, Recipe recipe) {
        repository.updateRecipe(recipeId, recipe, new RecipeRepository.RecipeCallback() {
            @Override
            public void onSuccess() {
                loadRecipes(null);
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
            }
        });
    }

    public void deleteRecipe(String recipeId) {
        repository.deleteRecipe(recipeId, new RecipeRepository.RecipeCallback() {
            @Override
            public void onSuccess() {
                loadRecipes(null);
            }
            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
            }
        });
    }

    public void clearError() { errorMessage.setValue(null); }
}