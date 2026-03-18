package com.example.pocketmenu.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.Ingredient;
import com.example.pocketmenu.data.model.Recipe;

import java.util.List;

public class EditRecipeDialog extends BaseRecipeDialog {

    private Recipe recipe;
    private String recipeId;

    public static EditRecipeDialog newInstance(String recipeId, Recipe recipe) {
        EditRecipeDialog dialog = new EditRecipeDialog();
        dialog.recipeId = recipeId;
        dialog.recipe = recipe;
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_recipe, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Editar receta");
        return dialog;
    }

    @Override
    protected void setupDeleteButton(Button deleteBtn, View view) {
        deleteBtn.setVisibility(View.VISIBLE);
        deleteBtn.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar receta")
                        .setMessage("¿Eliminar \"" + recipe.getName()
                                + "\"? Esta acción no se puede deshacer.")
                        .setPositiveButton("Eliminar", (d, i) -> {
                            viewModel.deleteRecipe(recipeId);
                            dismiss();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show()
        );
    }

    @Override
    protected void setupInitialData(EditText nameEt, EditText descEt,
                                    EditText portionsEt,
                                    LinearLayout ingredientsContainer) {
        if (recipe != null) {
            nameEt.setText(recipe.getName());
            descEt.setText(recipe.getDescription());
            portionsEt.setText(String.valueOf(recipe.getPortion()));

            ingredientsContainer.removeAllViews();
            if (recipe.getIngredients() != null) {
                for (Ingredient ing : recipe.getIngredients()) {
                    addIngredientRow(ingredientsContainer, ing);
                }
            }
        }
    }

    @Override
    protected void onSave(String name, String description,
                          int portions, List<Ingredient> ingredients) {
        recipe.setName(name);
        recipe.setDescription(description);
        recipe.setPortion(portions);
        recipe.setIngredients(ingredients);
        viewModel.updateRecipe(recipeId, recipe);
    }
}