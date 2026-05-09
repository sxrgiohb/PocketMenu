package com.example.pocketmenu.ui.dialogs;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.Ingredient;
import com.example.pocketmenu.data.repository.RecipeRepository;
import com.example.pocketmenu.utils.RecipeValidator;
import com.example.pocketmenu.viewmodel.RecipeViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRecipeDialog extends DialogFragment {
    // Reference to the ViewModel
    protected RecipeViewModel viewModel;

    // Lifecycle method used before inflating the layout
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    // Max width possible
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    // Min height required
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // Lifecycle method used after inflating the layout
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // The dialog obtains the same ViewModel as the parent fragment
        viewModel = new ViewModelProvider(requireParentFragment()).get(RecipeViewModel.class);

        // Links the views to the variables
        EditText nameEt = view.findViewById(R.id.edit_text_recipe_name);
        EditText descEt = view.findViewById(R.id.edit_text_recipe_description);
        EditText portionsEt = view.findViewById(R.id.edit_text_recipe_portions);
        LinearLayout ingredientsContainer = view.findViewById(R.id.container_ingredients);
        Button addIngredientBtn = view.findViewById(R.id.button_add_ingredient);
        Button saveBtn = view.findViewById(R.id.button_save_recipe);
        Button cancelBtn = view.findViewById(R.id.button_cancel_recipe);
        Button deleteBtn = view.findViewById(R.id.button_delete_recipe_form);

        // Methods to be implemented by the child classes
        setupDeleteButton(deleteBtn, view);

        setupInitialData(nameEt, descEt, portionsEt, ingredientsContainer);

        // Listeners
        addIngredientBtn.setOnClickListener(v -> addIngredientRow(ingredientsContainer));

        saveBtn.setOnClickListener(v -> {
            if (!validateRecipeFields(nameEt, portionsEt)) return;
            List<Ingredient> ingredients = getIngredientsFromContainer(ingredientsContainer);
            if (ingredients == null) return;
            onSave(nameEt.getText().toString().trim(),
                    descEt.getText().toString().trim(),
                    getPortions(portionsEt),
                    ingredients);
            dismiss();
        });
        cancelBtn.setOnClickListener(v -> dismiss());
    }

    // Abstract methods
    protected abstract void setupDeleteButton(Button deleteBtn, View view);
    protected abstract void setupInitialData(EditText nameEt, EditText descEt,
                                             EditText portionsEt,
                                             LinearLayout ingredientsContainer);
    protected abstract void onSave(String name, String description,
                                   int portions, List<Ingredient> ingredients);

    // Adds a new row to the ingredients container
    protected void addIngredientRow(LinearLayout container) {
        // Inflate the layout for the new row
        View row = getLayoutInflater().inflate(R.layout.item_ingredient, container, false);

        // Link the views to the variables
        AutoCompleteTextView nameView = row.findViewById(R.id.autocomplete_ingredient_name);
        EditText qty = row.findViewById(R.id.edit_text_ingredient_quantity);
        EditText unit = row.findViewById(R.id.edit_text_ingredient_unit);
        EditText category = row.findViewById(R.id.edit_text_ingredient_category);
        EditText store = row.findViewById(R.id.edit_text_ingredient_store);

        // Final flags used due to Java restrictions. Avoids infinite loops
        final boolean[] isFillingFromSuggestion = {false};
        final boolean[] hasSelected = {false};

        // Show suggestions with 1 character
        nameView.setThreshold(1);
        // Text watcher to show suggestions
        nameView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancels if the text is coming from a suggestion
                if (isFillingFromSuggestion[0]) return;
                hasSelected[0] = false;
                viewModel.searchIngredientSuggestions(s.toString(),
                        new RecipeRepository.OnIngredientsLoaded() {
                            @Override
                            public void onLoaded(List<Ingredient> ingredients) {
                                if (hasSelected[0]) return;
                                // Ingredients names to show in the dropdown
                                List<String> names = new ArrayList<>();
                                for (Ingredient ing : ingredients) names.add(ing.getName());
                                // Transforms the array to a dropdown
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                        requireContext(),
                                        android.R.layout.simple_dropdown_item_1line,
                                        names);
                                // Connects the adapter to the autocomplete
                                nameView.setAdapter(adapter);
                                // Saves the ingredients in the tag
                                nameView.setTag(ingredients);
                                if (!names.isEmpty()) nameView.showDropDown();
                            }
                            @Override public void onFailure(Exception e) {}
                        });
            }
        });

        // User selects an option from the dropdown
        nameView.setOnItemClickListener((parent, v, position, id) -> {
            hasSelected[0] = true;
            isFillingFromSuggestion[0] = true;
            // Retrieves the ingredients from the tag
            List<Ingredient> suggestions = (List<Ingredient>) nameView.getTag();
            if (suggestions != null && position < suggestions.size()) {
                Ingredient selected = suggestions.get(position);
                // Retrieves the info from the selected ingredient
                unit.setText(selected.getUnit() != null ? selected.getUnit() : "");
                category.setText(selected.getCategory() != null ? selected.getCategory() : "");
                store.setText(selected.getStore() != null ? selected.getStore() : "");
            }
            // Hides the dropdown
            nameView.dismissDropDown();
            // Disables the adapter
            nameView.setAdapter(null);
            // The following changes will be ignored by text watcher
            isFillingFromSuggestion[0] = false;
            qty.requestFocus();
        });

        MaterialButton removeButton = row.findViewById(R.id.button_remove_ingredient);
        removeButton.setOnClickListener(v -> {
            if (container.getChildCount() > 1) {
                container.removeView(row);
            } else {
                Toast.makeText(requireContext(),
                        "La receta debe tener al menos un ingrediente",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Shows a warning if the user starts typing
        unit.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showUnitWarningIfFirstTime();
        });
        container.addView(row);
    }

    // Loads the ingredient data into the row
    protected void addIngredientRow(LinearLayout container, Ingredient ing) {
        // Inflate the layout for the new row
        View row = getLayoutInflater().inflate(R.layout.item_ingredient, container, false);

        // Links the view, add the listener and checks if there is an ingredient (mandatory)
        MaterialButton removeButton = row.findViewById(R.id.button_remove_ingredient);
        removeButton.setOnClickListener(v -> {
            if (container.getChildCount() > 1) {
                container.removeView(row);
            } else {
                Toast.makeText(requireContext(),
                        "La receta debe tener al menos un ingrediente",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Links the views to the variables
        AutoCompleteTextView name = row.findViewById(R.id.autocomplete_ingredient_name);
        EditText qty = row.findViewById(R.id.edit_text_ingredient_quantity);
        EditText unit = row.findViewById(R.id.edit_text_ingredient_unit);
        EditText category = row.findViewById(R.id.edit_text_ingredient_category);
        EditText store = row.findViewById(R.id.edit_text_ingredient_store);

        // Loads the data
        name.setText(ing.getName());
        qty.setText(ing.getQuantity() > 0 ? String.valueOf(ing.getQuantity()) : "");
        unit.setText(ing.getUnit() != null ? ing.getUnit() : "");
        category.setText(ing.getCategory() != null ? ing.getCategory() : "");
        store.setText(ing.getStore() != null ? ing.getStore() : "");

        // Adds the row to the container
        container.addView(row);
    }

    // AUX METHODS
    protected boolean validateRecipeFields(EditText nameEt, EditText portionsEt) {
        String name = nameEt.getText().toString().trim();
        if (!RecipeValidator.isNameValid(name)) {
            nameEt.setError("Nombre obligatorio");
            return false;
        }
        String portionsText = portionsEt.getText().toString().trim();
        if (!RecipeValidator.isPortionsValid(portionsText)) {
            portionsEt.setError("Número inválido");
            return false;
        }
        return true;
    }

    protected int getPortions(EditText portionsEt) {
        return RecipeValidator.parsePortions(portionsEt.getText().toString().trim());
    }

    protected List<Ingredient> getIngredientsFromContainer(LinearLayout container) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);
            Ingredient ingredient = validateAndBuildIngredient(row);
            if (ingredient == null) return null;
            ingredients.add(ingredient);
        }
        return ingredients;
    }

    protected Ingredient validateAndBuildIngredient(View row) {
        AutoCompleteTextView name = row.findViewById(R.id.autocomplete_ingredient_name);
        EditText qty = row.findViewById(R.id.edit_text_ingredient_quantity);
        EditText unit = row.findViewById(R.id.edit_text_ingredient_unit);
        EditText category = row.findViewById(R.id.edit_text_ingredient_category);
        EditText store = row.findViewById(R.id.edit_text_ingredient_store);

        String nameText = name.getText().toString().trim();
        if (nameText.isEmpty()) {
            name.setError("Nombre obligatorio");
            return null;
        }

        // Sets the default value if the field is empty
        double quantity = 1;
        String qtyText = qty.getText().toString().trim();
        if (!qtyText.isEmpty()) {
            try {
                quantity = Double.parseDouble(qtyText);
                if (quantity <= 0) {
                    qty.setError("La cantidad debe ser mayor que 0");
                    return null;
                }
            } catch (NumberFormatException e) {
                qty.setError("Cantidad inválida");
                return null;
            }
        }

        return new Ingredient(
                nameText, quantity,
                unit.getText().toString().trim(),
                category.getText().toString().trim(),
                store.getText().toString().trim()
        );
    }

    // Suggest the user to use the same unit for each ingredient
    private void showUnitWarningIfFirstTime() {
        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences("pocketmenu_prefs", android.content.Context.MODE_PRIVATE);
        boolean shown = prefs.getBoolean("unit_warning_shown", false);
        if (shown) return;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Consejo sobre unidades")
                .setMessage("Se recomienda usar la misma unidad de medida para cada ingrediente. " +
                        "Si introduces 1 kg de carne en una receta y 500 g en otra, aparecerán como ingredientes separados en la lista de la compra.")
                .setPositiveButton("Entendido", (dialog, which) ->
                        // Changes the value of the flag to true
                        prefs.edit().putBoolean("unit_warning_shown", true).apply())
                .setCancelable(false)
                .show();
    }
}