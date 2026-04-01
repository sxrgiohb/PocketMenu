package com.example.pocketmenu.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.Recipe;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class RecipeSelectAdapter extends RecyclerView.Adapter<RecipeSelectAdapter.ViewHolder> {

    public interface OnRecipeSelectedListener {
        void onRecipeSelected(Recipe recipe);
    }

    private List<Recipe> allRecipes = new ArrayList<>();
    private List<Recipe> filteredRecipes = new ArrayList<>();
    private boolean showOnlyFavorites = false;
    private String currentSearch = "";
    private final OnRecipeSelectedListener listener;

    public RecipeSelectAdapter(OnRecipeSelectedListener listener) {
        this.listener = listener;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.allRecipes = recipes != null ? recipes : new ArrayList<>();
        applyFilters();
    }

    public void setSearchText(String text) {
        this.currentSearch = text != null ? text.toLowerCase().trim() : "";
        applyFilters();
    }

    public void toggleFavoriteFilter() {
        this.showOnlyFavorites = !this.showOnlyFavorites;
        applyFilters();
    }

    public boolean isShowingOnlyFavorites() {
        return showOnlyFavorites;
    }

    private void applyFilters() {
        filteredRecipes = new ArrayList<>();
        for (Recipe r : allRecipes) {
            boolean matchesSearch = currentSearch.isEmpty()
                    || r.getName().toLowerCase().contains(currentSearch);
            boolean matchesFavorite = !showOnlyFavorites || r.isFavorite();
            if (matchesSearch && matchesFavorite) {
                filteredRecipes.add(r);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = filteredRecipes.get(position);
        holder.textName.setText(recipe.getName());
        holder.textPortions.setText(recipe.getPortion() + " raciones");

        if (recipe.isFavorite()) {
            holder.iconFavorite.setVisibility(View.VISIBLE);
        } else {
            holder.iconFavorite.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onRecipeSelected(recipe));
    }

    @Override
    public int getItemCount() {
        return filteredRecipes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textPortions;
        MaterialButton iconFavorite;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_recipe_select_name);
            textPortions = itemView.findViewById(R.id.text_recipe_select_portions);
            iconFavorite = itemView.findViewById(R.id.icon_recipe_select_favorite);
        }
    }
}