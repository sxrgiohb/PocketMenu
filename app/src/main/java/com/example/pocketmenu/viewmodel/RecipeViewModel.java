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

    public RecipeViewModel() {
        repository = new RecipeRepository();
        loadRecipes(null);
    }

    public LiveData<FirestoreRecyclerOptions<Recipe>> getRecipesOptions() {
        return recipesOptions;
    }

    public void loadRecipes(String searchText) {
        Query query = repository.getRecipesQuery(searchText);
        FirestoreRecyclerOptions<Recipe> options = new FirestoreRecyclerOptions.Builder<Recipe>().setQuery(query, Recipe.class).build();
        recipesOptions.setValue(options);
    }

    public void addRecipe(Recipe recipe) {
        repository.addRecipe(recipe);
    }

    public void toggleFavorite(String recipeId, boolean isFavorite) {
        repository.updateFavorite(recipeId, !isFavorite);
    }

    public void updateRecipe(String recipeId, Recipe recipe) {
        repository.updateRecipe(recipeId, recipe);
    }

    public void deleteRecipe(String recipeId) {
        repository.deleteRecipe(recipeId);
    }
}