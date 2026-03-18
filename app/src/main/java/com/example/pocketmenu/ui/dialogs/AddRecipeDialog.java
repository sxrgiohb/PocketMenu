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
import androidx.annotation.Nullable;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.Ingredient;
import com.example.pocketmenu.data.model.Recipe;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class AddRecipeDialog extends BaseRecipeDialog {

    public static AddRecipeDialog newInstance() {
        return new AddRecipeDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
    protected void setupDeleteButton(Button deleteBtn, View view) {
        deleteBtn.setVisibility(View.GONE);
    }

    @Override
    protected void setupInitialData(EditText nameEt, EditText descEt,
                                    EditText portionsEt,
                                    LinearLayout ingredientsContainer) {
        addIngredientRow(ingredientsContainer);
    }

    @Override
    protected void onSave(String name, String description,
                          int portions, List<Ingredient> ingredients) {
        Recipe recipe = new Recipe(
                FirebaseAuth.getInstance().getUid(),
                name, description, portions, ingredients);
        viewModel.addRecipe(recipe);
    }
}