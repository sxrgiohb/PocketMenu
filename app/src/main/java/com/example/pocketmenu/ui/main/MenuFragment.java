package com.example.pocketmenu.ui.main;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.Ingredient;
import com.example.pocketmenu.data.model.Leftover;
import com.example.pocketmenu.data.model.Recipe;
import com.example.pocketmenu.data.model.auxiliar.DayMenuWrapper;
import com.example.pocketmenu.data.model.auxiliar.MenuAssignment;
import com.example.pocketmenu.ui.adapters.LeftoverSelectAdapter;
import com.example.pocketmenu.ui.adapters.MenuAdapter;
import com.example.pocketmenu.ui.adapters.RecipeSelectAdapter;
import com.example.pocketmenu.ui.dialogs.FavoriteTemplatesDialog;
import com.example.pocketmenu.viewmodel.MenuViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MenuFragment extends Fragment {

    private MenuViewModel viewModel;
    private MenuAdapter menuAdapter;

    private RecyclerView recyclerWeek;
    private FloatingActionButton fabEdit;
    private android.widget.Button buttonDateSelector;
    private MaterialButton buttonFavoriteWeek;
    private LinearLayout layoutEditActions;
    private android.widget.Button buttonExitEdit;
    private android.widget.Button buttonUseFavorite;

    private AlertDialog recipeSelectDialog;
    private AlertDialog leftoverSelectDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MenuViewModel.class);
        initViews(view);
        setupRecyclerView();
        setupObservers();
        setupListeners();
    }

    private void initViews(View view) {
        recyclerWeek = view.findViewById(R.id.recycler_week);
        fabEdit = view.findViewById(R.id.fab_add_or_edit_menu);
        buttonDateSelector = view.findViewById(R.id.button_date_selector);
        buttonFavoriteWeek = view.findViewById(R.id.button_favorite_week);
        layoutEditActions = view.findViewById(R.id.layout_edit_actions);
        buttonExitEdit = view.findViewById(R.id.exit_edit);
        buttonUseFavorite = view.findViewById(R.id.button_use_favorite);
    }

    private void setupRecyclerView() {
        menuAdapter = new MenuAdapter(new MenuAdapter.OnDayActionListener() {
            @Override
            public void onAddRecipeClicked(DayMenuWrapper day) {
                showRecipeSearchDialog(day);
            }
            @Override
            public void onAddLeftoverClicked(DayMenuWrapper day) {
                showLeftoverSelectionDialog(day);
            }
            @Override
            public void onDeleteRecipeClicked(DayMenuWrapper day, MenuAssignment assignment) {
                showDeleteConfirmation(assignment);
            }
            @Override
            public void onInfoRecipeClicked(MenuAssignment assignment) {
                showRecipeInfoDialog(assignment);
            }
        });
        recyclerWeek.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerWeek.setAdapter(menuAdapter);
    }

    private void setupObservers() {
        viewModel.getWeekDays().observe(getViewLifecycleOwner(), days ->
                menuAdapter.setDays(days));

        viewModel.getSelectedWeekStart().observe(getViewLifecycleOwner(), monday -> {
            if (monday != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                buttonDateSelector.setText("Semana del " + sdf.format(monday));
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty())
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupListeners() {
        buttonDateSelector.setOnClickListener(v -> {
            if (menuAdapter.isEditMode()) return;
            Calendar cal = Calendar.getInstance();
            Date current = viewModel.getSelectedWeekStart().getValue();
            if (current != null) cal.setTime(current);
            new DatePickerDialog(requireContext(),
                    (dp, year, month, day) -> {
                        cal.set(year, month, day);
                        viewModel.selectWeek(cal.getTime());
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        buttonFavoriteWeek.setOnClickListener(v -> {
            if (!menuAdapter.isEditMode()) showSaveAsFavoriteDialog();
        });

        fabEdit.setOnClickListener(v -> enterEditMode());
        buttonExitEdit.setOnClickListener(v -> exitEditMode());
        buttonUseFavorite.setOnClickListener(v -> showFavoriteTemplatesDialog());
    }

    private void enterEditMode() {
        menuAdapter.setEditMode(true);
        fabEdit.setVisibility(View.GONE);
        layoutEditActions.setVisibility(View.VISIBLE);
        buttonDateSelector.setEnabled(false);
        buttonFavoriteWeek.setEnabled(false);
    }

    private void exitEditMode() {
        menuAdapter.setEditMode(false);
        fabEdit.setVisibility(View.VISIBLE);
        layoutEditActions.setVisibility(View.GONE);
        buttonDateSelector.setEnabled(true);
        buttonFavoriteWeek.setEnabled(true);
    }

    private void showRecipeInfoDialog(MenuAssignment assignment) {
        Recipe recipe = assignment.getRecipe();

        android.util.Log.d("RECIPE_INFO", "nombre=" + recipe.getName()
                + " porciones=" + recipe.getPortion()
                + " descripcion=" + recipe.getDescription()
                + " ingredientes=" + (recipe.getIngredients() != null
                ? recipe.getIngredients().size() : "null"));

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_recipe_info, null);

        TextView textPortions = dialogView.findViewById(R.id.text_portions);
        TextView labelDescription = dialogView.findViewById(R.id.label_description);
        TextView textDescription = dialogView.findViewById(R.id.text_description);
        View separatorDescription = dialogView.findViewById(R.id.separator_description);
        TextView labelIngredients = dialogView.findViewById(R.id.label_ingredients);
        LinearLayout headerIngredients = dialogView.findViewById(R.id.header_ingredients);
        LinearLayout containerIngredients = dialogView.findViewById(R.id.container_ingredients);

        textPortions.setText("Raciones: " + recipe.getPortion());

        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            labelDescription.setVisibility(View.VISIBLE);
            textDescription.setVisibility(View.VISIBLE);
            separatorDescription.setVisibility(View.VISIBLE);
            textDescription.setText(recipe.getDescription());
        }

        if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
            labelIngredients.setVisibility(View.VISIBLE);
            headerIngredients.setVisibility(View.VISIBLE);
            containerIngredients.setVisibility(View.VISIBLE);

            for (Ingredient ingredient : recipe.getIngredients()) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 6, 0, 6);

                TextView tvName = new TextView(requireContext());
                tvName.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
                tvName.setText(ingredient.getName());
                tvName.setTextSize(14f);

                TextView tvQuantity = new TextView(requireContext());
                tvQuantity.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tvQuantity.setGravity(android.view.Gravity.END);
                tvQuantity.setTextSize(14f);
                tvQuantity.setText(ingredient.getQuantity() > 0
                        ? String.valueOf(ingredient.getQuantity()) : "—");

                TextView tvUnit = new TextView(requireContext());
                tvUnit.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tvUnit.setGravity(android.view.Gravity.END);
                tvUnit.setTextSize(14f);
                tvUnit.setText(ingredient.getUnit() != null && !ingredient.getUnit().isEmpty()
                        ? ingredient.getUnit() : "—");

                row.addView(tvName);
                row.addView(tvQuantity);
                row.addView(tvUnit);
                containerIngredients.addView(row);
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(recipe.getName())
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void showFavoriteTemplatesDialog() {
        FavoriteTemplatesDialog dialog = FavoriteTemplatesDialog.newInstance();
        dialog.setOnTemplateAppliedListener(unassignedPortions -> {
            String msg = "Plantilla aplicada. Revisa y pulsa Guardar.";
            if (unassignedPortions > 0) {
                msg += "\n⚠️ " + unassignedPortions
                        + " ración(es) sin asignar quedarán disponibles la semana siguiente.";
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        });
        dialog.show(getChildFragmentManager(), "FavoriteTemplatesFragment");
    }

    private void showRecipeSearchDialog(DayMenuWrapper day) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_select_recipe, null);

        EditText editSearch = dialogView.findViewById(R.id.edit_search_recipe);
        MaterialButton buttonFilterFavorite = dialogView.findViewById(R.id.button_filter_favorite);
        RecyclerView recyclerRecipes = dialogView.findViewById(R.id.recycler_select_recipe);

        RecipeSelectAdapter adapter = new RecipeSelectAdapter(recipe -> {
            if (recipeSelectDialog != null) recipeSelectDialog.dismiss();
            if (recipe.getPortion() <= 1) {
                viewModel.assignRecipeToDay(recipe, day.getDate(), false, 0);
            } else {
                showPerishableDialog(recipe, day.getDate());
            }
        });

        buttonFilterFavorite.setIconResource(R.drawable.ic_favorite_false);

        recyclerRecipes.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerRecipes.setAdapter(adapter);

        viewModel.getAllRecipes().observe(getViewLifecycleOwner(), adapter::setRecipes);
        viewModel.loadAllRecipes();

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setSearchText(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        buttonFilterFavorite.setOnClickListener(v -> {
            adapter.toggleFavoriteFilter();
            buttonFilterFavorite.setIconResource(
                    adapter.isShowingOnlyFavorites()
                            ? R.drawable.ic_favorite_true
                            : R.drawable.ic_favorite_false);
        });

        recipeSelectDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Añadir receta")
                .setView(dialogView)
                .setNegativeButton("Cancelar", null)
                .create();
        recipeSelectDialog.show();
    }

    private void showLeftoverSelectionDialog(DayMenuWrapper day) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_select_leftover, null);

        RecyclerView recyclerLeftovers = dialogView.findViewById(R.id.recycler_select_leftover);
        TextView textEmpty = dialogView.findViewById(R.id.text_no_leftovers);

        LeftoverSelectAdapter adapter = new LeftoverSelectAdapter(item -> {
            if (leftoverSelectDialog != null) leftoverSelectDialog.dismiss();
            viewModel.assignLeftoverToDay(item.getLeftover(), item.getRecipe(), day.getDate());
        });

        recyclerLeftovers.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerLeftovers.setAdapter(adapter);

        viewModel.getValidLeftovers().observe(getViewLifecycleOwner(), items -> {
            adapter.setItems(items);
            boolean isEmpty = items == null || items.isEmpty();
            textEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerLeftovers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        viewModel.loadValidLeftovers(day.getDate());

        leftoverSelectDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Usar sobras")
                .setView(dialogView)
                .setNegativeButton("Cancelar", null)
                .create();
        leftoverSelectDialog.show();
    }

    private void showPerishableDialog(Recipe recipe, Date dayDate) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_perishable, null);

        Switch switchPerishable = dialogView.findViewById(R.id.switch_perishable);
        EditText editValidDays = dialogView.findViewById(R.id.edit_valid_days);

        editValidDays.setVisibility(View.GONE);
        switchPerishable.setOnCheckedChangeListener((btn, isChecked) ->
                editValidDays.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        new AlertDialog.Builder(requireContext())
                .setTitle("Sobras de " + recipe.getName())
                .setView(dialogView)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    boolean isPerishable = switchPerishable.isChecked();
                    int validDays = 0;
                    if (isPerishable) {
                        String daysStr = editValidDays.getText().toString().trim();
                        validDays = daysStr.isEmpty() ? 0 : Integer.parseInt(daysStr);
                    }
                    viewModel.assignRecipeToDay(recipe, dayDate, isPerishable, validDays);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showDeleteConfirmation(MenuAssignment assignment) {
        String recipeName = assignment.getRecipe().getName();
        String message;

        if (assignment.getMenu().isFromLeftover()) {
            message = "¿Eliminar esta asignación de \"" + recipeName
                    + "\"? La ración volverá a estar disponible.";
        } else {
            Leftover leftover = assignment.getLeftover();
            if (leftover != null && leftover.getRemainingPortions() > 0) {
                message = "¿Eliminar \"" + recipeName + "\"? También se eliminarán "
                        + leftover.getRemainingPortions() + " ración(es) de sobras.";
            } else {
                message = "¿Eliminar \"" + recipeName + "\"? Esta acción no se puede deshacer.";
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar receta")
                .setMessage(message)
                .setPositiveButton("Eliminar", (dialog, which) ->
                        viewModel.removeAssignmentFromDay(assignment))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showSaveAsFavoriteDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Nombre del menú favorito");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Guardar semana como favorita")
                .setView(input)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    input.setError("Introduce un nombre");
                } else {
                    viewModel.saveCurrentWeekAsFavorite(name);
                    Toast.makeText(requireContext(), "Guardado como favorito",
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    public void onRecipeSelected(Recipe recipe, Date dayDate) {
        showPerishableDialog(recipe, dayDate);
    }
}