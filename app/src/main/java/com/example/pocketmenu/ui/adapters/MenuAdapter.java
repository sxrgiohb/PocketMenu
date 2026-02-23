package com.example.pocketmenu.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.auxiliar.DayMenuWrapper;
import com.example.pocketmenu.data.model.auxiliar.MenuAssignment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.DayViewHolder> {

    public interface OnDayActionListener {
        void onAddRecipeClicked(DayMenuWrapper day);
        void onAddLeftoverClicked(DayMenuWrapper day);
        void onDeleteRecipeClicked(DayMenuWrapper day, MenuAssignment assignment);
    }

    private List<DayMenuWrapper> days = new ArrayList<>();
    private final OnDayActionListener listener;
    private boolean isEditMode = false;

    private final String[] dayNames = {"Lunes", "Martes", "Miércoles",
            "Jueves", "Viernes", "Sábado", "Domingo"};
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

    public MenuAdapter(OnDayActionListener listener) {
        this.listener = listener;
    }

    public void setDays(List<DayMenuWrapper> days) {
        this.days = days != null ? days : new ArrayList<>();
        notifyDataSetChanged();
    }

    // Activa o desactiva el modo edición y refresca todos los items
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        holder.bind(days.get(position));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    class DayViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDayName;
        private final TextView textEmptyDay;
        private final ImageButton buttonAddRecipe;
        private final ImageButton buttonAddLeftover;
        private final RecyclerView recyclerRecipes;
        private final MenuRecipeAdapter recipeAdapter;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            textDayName = itemView.findViewById(R.id.text_day_name);
            textEmptyDay = itemView.findViewById(R.id.text_empty_day);
            buttonAddRecipe = itemView.findViewById(R.id.button_add_recipe);
            buttonAddLeftover = itemView.findViewById(R.id.button_add_leftover);

            recyclerRecipes = new RecyclerView(itemView.getContext());
            recyclerRecipes.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            recyclerRecipes.setNestedScrollingEnabled(false);

            LinearLayout container = itemView.findViewById(R.id.container_recipes);
            container.addView(recyclerRecipes);

            recipeAdapter = new MenuRecipeAdapter(assignment -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_ID) {
                    listener.onDeleteRecipeClicked(days.get(pos), assignment);
                }
            });
            recyclerRecipes.setAdapter(recipeAdapter);
        }

        void bind(DayMenuWrapper day) {
            // Nombre + fecha
            String dayLabel = dayNames[day.getDayOfWeek() - 1];
            String dateStr = day.getDate() != null ? " · " + sdf.format(day.getDate()) : "";
            textDayName.setText(dayLabel + dateStr);

            // Botones: solo visibles en modo edición
            int buttonVisibility = isEditMode ? View.VISIBLE : View.GONE;
            buttonAddRecipe.setVisibility(buttonVisibility);
            buttonAddLeftover.setVisibility(buttonVisibility);

            // Botón eliminar en cada receta: solo en modo edición
            recipeAdapter.setEditMode(isEditMode);

            // Mensaje vacío
            boolean isEmpty = day.getAssignments().isEmpty();
            textEmptyDay.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerRecipes.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            recipeAdapter.submitList(new ArrayList<>(day.getAssignments()));

            buttonAddRecipe.setOnClickListener(v -> listener.onAddRecipeClicked(day));
            buttonAddLeftover.setOnClickListener(v -> listener.onAddLeftoverClicked(day));
        }
    }
}