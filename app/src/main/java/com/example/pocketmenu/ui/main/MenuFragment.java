package com.example.pocketmenu.ui.main;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.example.pocketmenu.data.model.Recipe;
import com.example.pocketmenu.data.model.auxiliar.DayMenuWrapper;
import com.example.pocketmenu.data.model.auxiliar.LeftoverWithRecipe;
import com.example.pocketmenu.data.model.auxiliar.MenuAssignment;
import com.example.pocketmenu.ui.adapters.LeftoverSelectAdapter;
import com.example.pocketmenu.ui.adapters.MenuAdapter;
import com.example.pocketmenu.ui.adapters.RecipeSelectAdapter;
import com.example.pocketmenu.viewmodel.MenuViewModel;
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
    private android.widget.ImageButton buttonFavoriteWeek;
    private LinearLayout layoutEditActions;
    private android.widget.Button buttonSaveEdit;
    private android.widget.Button buttonCancelEdit;

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
        buttonSaveEdit = view.findViewById(R.id.button_save_edit);
        buttonCancelEdit = view.findViewById(R.id.button_cancel_edit);
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
        buttonSaveEdit.setOnClickListener(v -> exitEditMode(true));
        buttonCancelEdit.setOnClickListener(v -> exitEditMode(false));
    }

    // ===========================
    // MODO EDICIÓN
    // ===========================
    private void enterEditMode() {
        menuAdapter.setEditMode(true);
        fabEdit.setVisibility(View.GONE);
        layoutEditActions.setVisibility(View.VISIBLE);
        buttonDateSelector.setEnabled(false);
        buttonFavoriteWeek.setEnabled(false);
    }

    private void exitEditMode(boolean save) {
        menuAdapter.setEditMode(false);
        fabEdit.setVisibility(View.VISIBLE);
        layoutEditActions.setVisibility(View.GONE);
        buttonDateSelector.setEnabled(true);
        buttonFavoriteWeek.setEnabled(true);

        if (save) {
            Toast.makeText(requireContext(), "Menú guardado", Toast.LENGTH_SHORT).show();
        } else {
            viewModel.reloadCurrentWeek();
        }
    }

    // ===========================
    // DIÁLOGOS
    // ===========================
    private void showRecipeSearchDialog(DayMenuWrapper day) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_select_recipe, null);

        EditText editSearch = dialogView.findViewById(R.id.edit_search_recipe);
        ImageButton buttonFilterFavorite = dialogView.findViewById(R.id.button_filter_favorite);
        RecyclerView recyclerRecipes = dialogView.findViewById(R.id.recycler_select_recipe);

        RecipeSelectAdapter adapter = new RecipeSelectAdapter(recipe -> {
            if (recipeSelectDialog != null) recipeSelectDialog.dismiss();
            showPerishableDialog(recipe, day.getDate());
        });

        recyclerRecipes.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerRecipes.setAdapter(adapter);

        // Observar recetas
        viewModel.getAllRecipes().observe(getViewLifecycleOwner(), adapter::setRecipes);
        viewModel.loadAllRecipes();

        // Buscador por texto
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setSearchText(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filtro favoritos
        buttonFilterFavorite.setOnClickListener(v -> {
            adapter.toggleFavoriteFilter();
            buttonFilterFavorite.setImageResource(
                    adapter.isShowingOnlyFavorites()
                            ? R.drawable.ic_favorite_filled
                            : R.drawable.ic_favorite_border);
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
        viewModel.loadValidLeftovers();

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
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar receta")
                .setMessage("¿Eliminar \"" + assignment.getRecipe().getName()
                        + "\" del menú? También se eliminarán sus sobras generadas.")
                .setPositiveButton("Eliminar", (dialog, which) ->
                        viewModel.removeAssignmentFromDay(assignment))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showSaveAsFavoriteDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Nombre del menú favorito");
        new AlertDialog.Builder(requireContext())
                .setTitle("Guardar semana como favorita")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        viewModel.saveCurrentWeekAsFavorite(name);
                        Toast.makeText(requireContext(), "Guardado como favorito",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Introduce un nombre",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public void onRecipeSelected(Recipe recipe, Date dayDate) {
        showPerishableDialog(recipe, dayDate);
    }
}