package com.example.pocketmenu.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.auxiliar.MenuAssignment;

public class MenuRecipeAdapter extends ListAdapter<MenuAssignment, MenuRecipeAdapter.ViewHolder> {

    public interface OnRecipeActionListener {
        void onDeleteClicked(MenuAssignment assignment);
        void onInfoClicked(MenuAssignment assignment);
    }

    private final OnRecipeActionListener listener;
    private boolean isEditMode = false;

    public MenuRecipeAdapter(OnRecipeActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<MenuAssignment> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MenuAssignment>() {
                @Override
                public boolean areItemsTheSame(@NonNull MenuAssignment a, @NonNull MenuAssignment b) {
                    return a.getMenu().getId() != null
                            && a.getMenu().getId().equals(b.getMenu().getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull MenuAssignment a, @NonNull MenuAssignment b) {
                    boolean samePortions = a.getMenu().getUsedPortions() == b.getMenu().getUsedPortions();
                    boolean sameLeftover = (a.getLeftover() == null) == (b.getLeftover() == null);
                    if (a.getLeftover() != null && b.getLeftover() != null) {
                        sameLeftover = a.getLeftover().getRemainingPortions()
                                == b.getLeftover().getRemainingPortions();
                    }
                    return samePortions && sameLeftover;
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_in_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textRecipeName;
        private final TextView textRecipeInfo;
        private final ImageButton buttonInfo;
        private final ImageButton buttonDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textRecipeName = itemView.findViewById(R.id.text_recipe_name);
            textRecipeInfo = itemView.findViewById(R.id.text_recipe_info);
            buttonInfo = itemView.findViewById(R.id.button_info_recipe);
            buttonDelete = itemView.findViewById(R.id.button_delete_recipe);
        }

        void bind(MenuAssignment assignment) {
            textRecipeName.setText(assignment.getRecipe().getName());
            buttonDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
            buttonDelete.setOnClickListener(v -> listener.onDeleteClicked(assignment));
            buttonInfo.setOnClickListener(v -> listener.onInfoClicked(assignment));
        }
    }
}