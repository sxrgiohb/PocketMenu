package com.example.pocketmenu.ui.main;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private final long DEBOUNCE_DELAY = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recipe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_recipes);
        fabAddRecipe = view.findViewById(R.id.fab_add_recipe);
        searchView = view.findViewById(R.id.search_view_recipes);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        viewModel = new ViewModelProvider(this).get(RecipeViewModel.class);

        viewModel.getRecipesOptions().observe(getViewLifecycleOwner(), options -> {
            if (adapter != null) adapter.stopListening();
            adapter = new RecipeAdapter(options);
            adapter.setOnRecipeInteractionListener(this);
            recyclerView.setAdapter(adapter);
            adapter.startListening();
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        viewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(getContext(), "Operación realizada", Toast.LENGTH_SHORT).show();
                viewModel.clearSuccess();
            }
        });

        setupSearchView();

        fabAddRecipe.setOnClickListener(v -> {
            AddRecipeDialog dialog = AddRecipeDialog.newInstance();
            dialog.show(getChildFragmentManager(), "AddRecipeDialog");
        });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.loadRecipes(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> viewModel.loadRecipes(newText);
                searchHandler.postDelayed(searchRunnable, DEBOUNCE_DELAY);
                return true;
            }
        });
    }

    @Override
    public void onFavoriteClick(String recipeId, boolean isCurrentlyFavorite) {
        viewModel.toggleFavorite(recipeId, isCurrentlyFavorite);
    }

    @Override
    public void onEditClick(String recipeId, Recipe recipe) {
        EditRecipeDialog dialog = EditRecipeDialog.newInstance(recipeId, recipe);
        dialog.show(getChildFragmentManager(), "EditRecipeDialog");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }
}