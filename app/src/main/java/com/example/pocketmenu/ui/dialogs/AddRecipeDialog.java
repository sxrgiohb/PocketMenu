package com.example.pocketmenu.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
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
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.Ingredient;
import com.example.pocketmenu.data.model.Recipe;
import com.example.pocketmenu.viewmodel.RecipeViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class AddRecipeDialog extends DialogFragment {

    private RecipeViewModel viewModel;

    public static AddRecipeDialog newInstance() {
        return new AddRecipeDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_recipe, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Añadir receta");
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment())
                .get(RecipeViewModel.class);

        EditText nameEt = view.findViewById(R.id.edit_text_recipe_name);
        EditText descEt = view.findViewById(R.id.edit_text_recipe_description);
        EditText portionsEt = view.findViewById(R.id.edit_text_recipe_portions);
        LinearLayout ingredientsContainer = view.findViewById(R.id.container_ingredients);
        Button addIngredientBtn = view.findViewById(R.id.button_add_ingredient);
        Button saveBtn = view.findViewById(R.id.button_save_recipe);
        Button cancelBtn = view.findViewById(R.id.button_cancel_recipe);

        addIngredientBtn.setOnClickListener(v -> addIngredientRow(ingredientsContainer));
        addIngredientRow(ingredientsContainer);

        saveBtn.setOnClickListener(v -> {
            if (!validateRecipeFields(nameEt, portionsEt)) return;
            List<Ingredient> ingredients = getIngredientsFromContainer(ingredientsContainer);
            if (ingredients == null) return;

            Recipe recipe = new Recipe(
                    FirebaseAuth.getInstance().getUid(),
                    nameEt.getText().toString().trim(),
                    descEt.getText().toString().trim(),
                    getPortions(portionsEt),
                    ingredients
            );

            viewModel.addRecipe(recipe);
            dismiss();
        });

        cancelBtn.setOnClickListener(v -> dismiss());
    }

    private void addIngredientRow(LinearLayout container) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_ingredient, container, false);
        container.addView(row);
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

    private List<Ingredient> getIngredientsFromContainer(LinearLayout container) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);
            Ingredient ingredient = validateAndBuildIngredient(row);
            if (ingredient == null) return null;
            ingredients.add(ingredient);
        }
        return ingredients;
    }

    private Ingredient validateAndBuildIngredient(View row) {
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

        double quantity = 1;
        String qtyText = qty.getText().toString().trim();
        if (!qtyText.isEmpty()) {
            try {
                quantity = Double.parseDouble(qtyText);
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
}