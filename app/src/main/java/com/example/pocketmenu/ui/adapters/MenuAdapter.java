package com.example.pocketmenu.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.auxiliar.DayMenuWrapper;
import com.example.pocketmenu.data.model.auxiliar.MenuAssignment;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.DayViewHolder> {

    public interface OnDayActionListener {
        void onAddRecipeClicked(DayMenuWrapper day);
        void onAddLeftoverClicked(DayMenuWrapper day);
        void onDeleteRecipeClicked(DayMenuWrapper day, MenuAssignment assignment);
        void onInfoRecipeClicked(MenuAssignment assignment);
    }

    private List<DayMenuWrapper> days = new ArrayList<>();
    private final OnDayActionListener listener;
    private boolean isEditMode = false;

    private final String[] dayNames = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

    // Constructor
    public MenuAdapter(OnDayActionListener listener) {
        this.listener = listener;
    }

    // Replaces week data from ViewModel
    public void setDays(List<DayMenuWrapper> days) {
        this.days = days != null ? days : new ArrayList<>();
        notifyDataSetChanged();
    }

    // Shows add/delete actions when user taps FAB edit flow
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return isEditMode;
    }


    @Override
    public DayViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day, parent, false);
        return new DayViewHolder(view, listener, dayNames, sdf);
    }

    @Override
    public void onBindViewHolder(DayViewHolder holder, int position) {
        holder.bind(days.get(position), isEditMode);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDayName;
        private final TextView textEmptyDay;
        private final MaterialButton buttonAddRecipe;
        private final MaterialButton buttonAddLeftover;
        private final RecyclerView recyclerRecipes;
        private final MenuRecipeAdapter recipeAdapter;
        private final OnDayActionListener listener;
        private final String[] dayNames;
        private final SimpleDateFormat sdf;

        DayViewHolder(View itemView, OnDayActionListener listener, String[] dayNames, SimpleDateFormat sdf) {
            super(itemView);
            this.listener = listener;
            this.dayNames = dayNames;
            this.sdf = sdf;
            textDayName = itemView.findViewById(R.id.text_day_name);
            textEmptyDay = itemView.findViewById(R.id.text_empty_day);
            buttonAddRecipe = itemView.findViewById(R.id.button_add_recipe);
            buttonAddLeftover = itemView.findViewById(R.id.button_add_leftover);

            recyclerRecipes = new RecyclerView(itemView.getContext());
            recyclerRecipes.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            // Nested list scrolls with parent RecyclerView, not independently
            recyclerRecipes.setNestedScrollingEnabled(false);

            LinearLayout container = itemView.findViewById(R.id.container_recipes);
            container.addView(recyclerRecipes);

            recipeAdapter = new MenuRecipeAdapter(new MenuRecipeAdapter.OnRecipeActionListener() {
                @Override
                public void onDeleteClicked(DayMenuWrapper day, MenuAssignment assignment) {
                    listener.onDeleteRecipeClicked(day, assignment);
                }

                @Override
                public void onInfoClicked(MenuAssignment assignment) {
                    listener.onInfoRecipeClicked(assignment);
                }
            });
            recyclerRecipes.setAdapter(recipeAdapter);
        }

        void bind(DayMenuWrapper day, boolean isEditMode) {
            String dayLabel = dayNames[day.getDayOfWeek() - 1];
            String dateStr = day.getDate() != null ? " · " + sdf.format(day.getDate()) : "";
            textDayName.setText(dayLabel + dateStr);

            int buttonVisibility = isEditMode ? View.VISIBLE : View.GONE;
            buttonAddRecipe.setVisibility(buttonVisibility);
            buttonAddLeftover.setVisibility(buttonVisibility);

            recipeAdapter.setEditMode(isEditMode);

            boolean isEmpty = day.getAssignments().isEmpty();
            textEmptyDay.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerRecipes.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            recipeAdapter.setDayContext(day);
            recipeAdapter.submitList(new ArrayList<>(day.getAssignments()));

            buttonAddRecipe.setOnClickListener(v -> listener.onAddRecipeClicked(day));
            buttonAddLeftover.setOnClickListener(v -> listener.onAddLeftoverClicked(day));
        }
    }
}