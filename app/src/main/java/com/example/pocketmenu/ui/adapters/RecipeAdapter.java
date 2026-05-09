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

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    // Callbacks: notifies the actions to RecipeFragment
    public interface OnRecipeInteractionListener {
        void onFavoriteClick(String recipeId, boolean isCurrentlyFavorite);
        void onEditClick(String recipeId, Recipe recipe);
    }

    private List<Recipe> recipes = new ArrayList<>();
    private OnRecipeInteractionListener listener;

    // Setter that assign the object implemented by the interface
    public void setOnRecipeInteractionListener(OnRecipeInteractionListener l) {
        listener = l;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes != null ? recipes : new ArrayList<>();
        // Notifies the RecyclerView that the data has changed
        notifyDataSetChanged();
    }

    // Overrides RecyclerView.Adapter. Counts the number of items.
    @Override
    public int getItemCount() {
        return recipes.size();
    }

    // The RecylcerView calls this method to create a new ViewHolder
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Obtains the LayoutInflater from the parent context (the RecyclerView) and inflates the view.
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(v);
    }

    // The RecyclerView calls this method each time an item is displayed for the first time or when the ViewHolder is recycled.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Gets the recipe at the given position
        Recipe recipe = recipes.get(position);
        holder.name.setText(recipe.getName());
        //Changes the favorite icon: true -> favorite, false -> not favorite
        holder.favorite.setIconResource(
                recipe.isFavorite()
                        ? R.drawable.ic_favorite_true
                        : R.drawable.ic_favorite_false
        );

        // Listeners for the buttons
        holder.favorite.setOnClickListener(v -> {
            if (listener != null)
                listener.onFavoriteClick(recipe.getId(), recipe.isFavorite());
        });

        holder.edit.setOnClickListener(v -> {
            if (listener != null)
                listener.onEditClick(recipe.getId(), recipe);
        });
    }

    // Generic ViewHolder class
     public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        MaterialButton favorite, edit;
        // Constructor
        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.text_view_recipe_name);
            favorite = v.findViewById(R.id.button_favorite);
            edit = v.findViewById(R.id.button_edit);
        }
    }
}