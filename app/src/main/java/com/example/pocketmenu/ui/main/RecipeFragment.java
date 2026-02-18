package com.example.pocketmenu.ui.main;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.Ingredient;
import com.example.pocketmenu.data.model.Recipe;
import com.example.pocketmenu.ui.adapters.RecipeAdapter;
import com.example.pocketmenu.viewmodel.RecipeViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_recipes);
        fabAddRecipe = view.findViewById(R.id.fab_add_recipe);
        searchView = view.findViewById(R.id.search_view_recipes);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        viewModel = new ViewModelProvider(this).get(RecipeViewModel.class);

        // Observe LiveData from FirestoreRecyclerOptions
        viewModel.getRecipesOptions().observe(getViewLifecycleOwner(), options -> {
            // Create new adapter for new data
            if (adapter != null) {
                adapter.stopListening();
            }
            adapter = new RecipeAdapter(options);
            adapter.setOnRecipeInteractionListener(this);
            recyclerView.setAdapter(adapter);
            adapter.startListening();
        });

        setupSearchView();

        fabAddRecipe.setOnClickListener(v -> showAddRecipeDialog());
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
        showEditRecipeDialog(recipeId, recipe);
    }

    //Dialogs

    private void showAddRecipeDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_add_recipe, null);

        EditText nameEt = dialogView.findViewById(R.id.edit_text_recipe_name);
        EditText descEt = dialogView.findViewById(R.id.edit_text_recipe_description);
        EditText portionsEt = dialogView.findViewById(R.id.edit_text_recipe_portions);
        LinearLayout ingredientsContainer = dialogView.findViewById(R.id.container_ingredients);
        Button addIngredientBtn = dialogView.findViewById(R.id.button_add_ingredient);

        addIngredientBtn.setOnClickListener(v -> addIngredientRow(ingredientsContainer));
        addIngredientBtn.performClick();

        AlertDialog dialog = builder
                .setView(dialogView)
                .setTitle("Añadir receta")
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {

            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            saveButton.setOnClickListener(v -> {

                if (!validateRecipeFields(nameEt, portionsEt)) return;

                List<Ingredient> ingredients =
                        getIngredientsFromContainer(ingredientsContainer);

                if (ingredients == null) return;

                Recipe recipe = new Recipe(
                        FirebaseAuth.getInstance().getUid(),
                        nameEt.getText().toString().trim(),
                        descEt.getText().toString(),
                        getPortions(portionsEt),
                        ingredients
                );

                viewModel.addRecipe(recipe);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showEditRecipeDialog(String recipeId, Recipe recipe) {

        if (recipe == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_recipe, null);

        EditText nameEt = dialogView.findViewById(R.id.edit_text_recipe_name);
        EditText descEt = dialogView.findViewById(R.id.edit_text_recipe_description);
        EditText portionsEt = dialogView.findViewById(R.id.edit_text_recipe_portions);
        LinearLayout ingredientsContainer = dialogView.findViewById(R.id.container_ingredients);
        Button addIngredientBtn = dialogView.findViewById(R.id.button_add_ingredient);

        nameEt.setText(recipe.getName());
        descEt.setText(recipe.getDescription());
        portionsEt.setText(String.valueOf(recipe.getPortion()));

        ingredientsContainer.removeAllViews();
        for (Ingredient ing : recipe.getIngredients()) {
            addIngredientRow(ingredientsContainer, ing);
        }

        addIngredientBtn.setOnClickListener(v ->
                addIngredientRow(ingredientsContainer));

        AlertDialog dialog = builder
                .setView(dialogView)
                .setTitle("Editar receta")
                .setPositiveButton("Guardar", null)
                .setNeutralButton("Eliminar",
                        (d, i) -> viewModel.deleteRecipe(recipeId))
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button saveButton =
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            saveButton.setOnClickListener(v -> {
                 if (!validateRecipeFields(nameEt, portionsEt)) return;
                 List<Ingredient> ingredients =
                         getIngredientsFromContainer(ingredientsContainer);

                 if (ingredients == null) return;

                 recipe.setName(nameEt.getText().toString().trim());
                 recipe.setDescription(descEt.getText().toString());
                 recipe.setPortion(getPortions(portionsEt));
                 recipe.setIngredients(ingredients);

                 viewModel.updateRecipe(recipeId, recipe);
                 dialog.dismiss();
            });
        });
        dialog.show();
    }


    private boolean validateRecipeFields(EditText nameEt, EditText portionsEt) {

        String name = nameEt.getText().toString().trim();
        if (name.isEmpty()) {
            nameEt.setError("Nombre obligatorio");
            return false;
        }

        String portionsText = portionsEt.getText().toString().trim();
        if (!portionsText.isEmpty()) {
            try {
                Integer.parseInt(portionsText);
            } catch (NumberFormatException e) {
                portionsEt.setError("Número inválido");
                return false;
            }
        }

        return true;
    }

    private int getPortions(EditText portionsEt) {
        String text = portionsEt.getText().toString().trim();
        if (text.isEmpty()) return 1;
        return Integer.parseInt(text);
    }

    private Ingredient validateAndBuildIngredient(View row) {

        AutoCompleteTextView name =
                row.findViewById(R.id.autocomplete_ingredient_name);
        EditText qty =
                row.findViewById(R.id.edit_text_ingredient_quantity);

        String nameText = name.getText().toString().trim();
        String qtyText = qty.getText().toString().trim();

        if (nameText.isEmpty()) {
            name.setError("Nombre obligatorio");
            return null;
        }

        double quantity = 1;

        if (!qtyText.isEmpty()) {
            try {
                quantity = Double.parseDouble(qtyText);
            } catch (NumberFormatException e) {
                qty.setError("Cantidad inválida");
                return null;
            }
        }

        return new Ingredient(nameText, quantity, "", "", "");
    }

    private List<Ingredient> getIngredientsFromContainer(LinearLayout container) {

        List<Ingredient> ingredients = new ArrayList<>();

        for (int i = 0; i < container.getChildCount(); i++) {

            View row = container.getChildAt(i);
            Ingredient ingredient = validateAndBuildIngredient(row);

            if (ingredient == null) {
                return null;
            }

            ingredients.add(ingredient);
        }

        return ingredients;
    }

    private void addIngredientRow(LinearLayout container) {
        View row = getLayoutInflater()
                .inflate(R.layout.item_ingredient, container, false);
        container.addView(row);
    }

    private void addIngredientRow(LinearLayout container, Ingredient ing) {
        View row = getLayoutInflater()
                .inflate(R.layout.item_ingredient, container, false);

        AutoCompleteTextView name =
                row.findViewById(R.id.autocomplete_ingredient_name);
        EditText qty =
                row.findViewById(R.id.edit_text_ingredient_quantity);

        name.setText(ing.getName());
        qty.setText(String.valueOf(ing.getQuantity()));

        container.addView(row);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }
}