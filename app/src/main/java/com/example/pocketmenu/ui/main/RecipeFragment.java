package com.example.pocketmenu.ui.main;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.Recipe;
import com.example.pocketmenu.ui.adapters.RecipeAdapter;
import com.example.pocketmenu.ui.dialogs.AddRecipeDialog;
import com.example.pocketmenu.ui.dialogs.EditRecipeDialog;
import com.example.pocketmenu.viewmodel.RecipeViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class RecipeFragment extends Fragment implements RecipeAdapter.OnRecipeInteractionListener {

    private RecipeViewModel viewModel;
    private RecipeAdapter adapter;

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddRecipe;
    private SearchView searchView;

    // Handler to avoid multiple queries when typing
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    // Searching task
    private Runnable searchRunnable;
    // Searching delay
    private final long DEBOUNCE_DELAY = 300;

    // Creates the fragment
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recipe, container, false);
    }

    // Sets up the fragment
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_recipes);
        fabAddRecipe = view.findViewById(R.id.fab_add_recipe);
        searchView = view.findViewById(R.id.search_view_recipes);

        // RecyclerView setup
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecipeAdapter();
        adapter.setOnRecipeInteractionListener(this);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(RecipeViewModel.class);
        setupObservers();
        setupSearchView();

        // Floating action button
        fabAddRecipe.setOnClickListener(v ->
                AddRecipeDialog.newInstance().show(getChildFragmentManager(), "AddRecipeDialog"));
    }

    private void setupObservers() {
        // Updates the adapter when the list changes
        viewModel.getRecipes().observe(getViewLifecycleOwner(), recipes ->
                adapter.setRecipes(recipes));

        // Loads the recipes when the operation is successful
        viewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                viewModel.loadRecipes(null);
            }
        });
        // Shows an error message
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Search method
    private void setupSearchView() {
        // Detects the input
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.loadRecipes(query);
                return false;
            }

            @Override
            // Avoids multiple queries while typing
            public boolean onQueryTextChange(String newText) {
                // Cancels the previous search task
                searchHandler.removeCallbacks(searchRunnable);
                // Starts a new search task
                searchRunnable = () -> viewModel.loadRecipes(newText);
                // Schedules the search task to run after the delay
                searchHandler.postDelayed(searchRunnable, DEBOUNCE_DELAY);
                return true;
            }
        });
    }

    // RecipeAdapter interfaces
    @Override
    public void onFavoriteClick(String recipeId, boolean isCurrentlyFavorite) {
        viewModel.toggleFavorite(recipeId, isCurrentlyFavorite);
    }
    @Override
    public void onEditClick(String recipeId, Recipe recipe) {
        EditRecipeDialog.newInstance(recipeId, recipe).show(getChildFragmentManager(), "EditRecipeDialog");
    }
}