package com.example.pocketmenu.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.Recipe;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class RecipeAdapter
        extends FirestoreRecyclerAdapter<Recipe, RecipeAdapter.ViewHolder> {

    public interface OnRecipeInteractionListener {
        void onFavoriteClick(String recipeId, boolean isCurrentlyFavorite);
        void onEditClick(String recipeId);
    }

    private OnRecipeInteractionListener listener;

    public RecipeAdapter(@NonNull FirestoreRecyclerOptions<Recipe> options) {
        super(options);
    }

    public void setOnRecipeInteractionListener(OnRecipeInteractionListener l) {
        listener = l;
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder,
                                    int position,
                                    @NonNull Recipe model) {

        holder.name.setText(model.getName());
        /*
        Glide.with(holder.itemView.getContext())
                .load(model.getUrlImg())
                .placeholder(R.drawable.ic_placeholder_image)
                .into(holder.image);
        */
        holder.favorite.setImageResource(
                model.isFavorite()
                        ? R.drawable.ic_favorite_filled
                        : R.drawable.ic_favorite_border
        );

        String docId = getSnapshots()
                .getSnapshot(position)
                .getId();

        holder.favorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteClick(docId, model.isFavorite());
            }
        });

        holder.edit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(docId);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(v);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name;
        ImageButton favorite, edit;

        ViewHolder(View v) {
            super(v);
            //image = v.findViewById(R.id.image_view_recipe);
            name = v.findViewById(R.id.text_view_recipe_name);
            favorite = v.findViewById(R.id.button_favorite);
            edit = v.findViewById(R.id.button_edit);
        }
    }
}
