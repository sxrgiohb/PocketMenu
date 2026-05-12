package com.example.pocketmenu.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipes list in the recipe screen.
 * Exposes favorite/edit actions to the host fragment through callbacks.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    // Notifies row actions (favorite/edit) back to RecipeFragment.
    public interface OnRecipeInteractionListener {
        void onFavoriteClick(String recipeId, boolean isCurrentlyFavorite);
        void onEditClick(String recipeId, Recipe recipe);
    }

    private List<Recipe> recipes = new ArrayList<>();
    private OnRecipeInteractionListener listener;

    // Registers the fragment callback handler.
    public void setOnRecipeInteractionListener(OnRecipeInteractionListener l) {
        listener = l;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes != null ? recipes : new ArrayList<>();
        // Full refresh is enough for this small list.
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.name.setText(recipe.getName());
        // Mirrors favorite state in the icon.
        holder.favorite.setIconResource(
                recipe.isFavorite()
                        ? R.drawable.ic_favorite_true
                        : R.drawable.ic_favorite_false
        );

        holder.favorite.setOnClickListener(v -> {
            if (listener != null)
                listener.onFavoriteClick(recipe.getId(), recipe.isFavorite());
        });

        holder.edit.setOnClickListener(v -> {
            if (listener != null)
                listener.onEditClick(recipe.getId(), recipe);
        });
    }

    // Simple holder for the recipe row controls.
     public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        MaterialButton favorite, edit;
        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.text_view_recipe_name);
            favorite = v.findViewById(R.id.button_favorite);
            edit = v.findViewById(R.id.button_edit);
        }
    }
}