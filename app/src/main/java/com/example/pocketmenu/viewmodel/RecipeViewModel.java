package com.example.pocketmenu.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketmenu.data.model.Recipe;
import com.example.pocketmenu.data.repository.RecipeRepository;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

public class RecipeViewModel extends ViewModel {

    private final RecipeRepository repository;

    private final MutableLiveData<FirestoreRecyclerOptions<Recipe>> recipesOptions = new MutableLiveData<>();
    private final MutableLiveData<Recipe> selectedRecipe = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();

    public RecipeViewModel() {
        repository = new RecipeRepository();
        loadRecipes(null);
    }

    // FirestoreRecyclerOptions LiveData
    public LiveData<FirestoreRecyclerOptions<Recipe>> getRecipesOptions() {
        return recipesOptions;
    }

    // Selected recipe LiveData
    public LiveData<Recipe> getSelectedRecipe() {
        return selectedRecipe;
    }

    // Error LiveData
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Success LiveData
    public LiveData<Boolean> getOperationSuccess() {
        return operationSuccess;
    }

    // Load recipes with optional search
    public void loadRecipes(String searchText) {
        Query query = repository.getRecipesQuery(searchText);
        FirestoreRecyclerOptions<Recipe> options = new FirestoreRecyclerOptions.Builder<Recipe>()
                .setQuery(query, Recipe.class)
                .build();
        recipesOptions.setValue(options);
    }

    // Add recipe
    public void addRecipe(Recipe recipe) {
        repository.addRecipe(recipe, new RecipeRepository.RecipeCallback() {
            @Override
            public void onSuccess() {
                operationSuccess.postValue(true);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                operationSuccess.postValue(false);
            }
        });
    }

    // Toggle favorite
    public void toggleFavorite(String recipeId, boolean isFavorite) {
        repository.updateFavorite(recipeId, !isFavorite, new RecipeRepository.RecipeCallback() {
            @Override
            public void onSuccess() {
                operationSuccess.postValue(true);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                operationSuccess.postValue(false);
            }
        });
    }

    // Update recipe
    public void updateRecipe(String recipeId, Recipe recipe) {
        repository.updateRecipe(recipeId, recipe, new RecipeRepository.RecipeCallback() {
            @Override
            public void onSuccess() {
                operationSuccess.postValue(true);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                operationSuccess.postValue(false);
            }
        });
    }

    // Delete recipe
    public void deleteRecipe(String recipeId) {
        repository.deleteRecipe(recipeId, new RecipeRepository.RecipeCallback() {
            @Override
            public void onSuccess() {
                operationSuccess.postValue(true);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.postValue(e.getMessage());
                operationSuccess.postValue(false);
            }
        });
    }

    // Clear selected recipe
    public void clearSelectedRecipe() {
        selectedRecipe.setValue(null);
    }

    // Clear error
    public void clearError() {
        errorMessage.setValue(null);
    }

    // Clear success flag
    public void clearSuccess() {
        operationSuccess.setValue(null);
    }
}
