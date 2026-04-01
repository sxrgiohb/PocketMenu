package com.example.pocketmenu.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.ShoppingListItem;
import com.example.pocketmenu.data.model.auxiliar.WeeklyShoppingList;
import com.example.pocketmenu.ui.adapters.ShoppingListAdapter;
import com.example.pocketmenu.ui.dialogs.AddProductDialog;
import com.example.pocketmenu.viewmodel.ShoppingListViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ShoppingListFragment extends Fragment {

    private ShoppingListViewModel viewModel;
    private ShoppingListAdapter adapter;

    private RecyclerView recyclerShoppingList;
    private FloatingActionButton fabAddExtraProduct;
    private Chip chipFilterStore;
    private Chip chipFilterCategory;
    private MaterialButtonToggleGroup toggleViewMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shopping_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ShoppingListViewModel.class);

        recyclerShoppingList = view.findViewById(R.id.recycler_shopping_list);
        fabAddExtraProduct = view.findViewById(R.id.fab_add_extra_product);
        chipFilterStore = view.findViewById(R.id.chip_filter_store);
        chipFilterCategory = view.findViewById(R.id.chip_filter_category);
        toggleViewMode = view.findViewById(R.id.toggle_view_mode);

        view.findViewById(R.id.button_clear_filters)
                .setOnClickListener(v -> clearFilters());

        setupRecyclerView();
        setupObservers();
        setupListeners();

        viewModel.loadCurrentMonth();
    }

    private void setupRecyclerView() {
        adapter = new ShoppingListAdapter(new ShoppingListAdapter.OnShoppingListActionListener() {
            @Override
            public void onItemChecked(ShoppingListItem item) {
                viewModel.toggleItemChecked(item);
            }

            @Override
            public void onExtraItemDeleted(ShoppingListItem item) {
                showDeleteConfirmation(item);
            }
        });
        recyclerShoppingList.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerShoppingList.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getMonthlyShoppingLists().observe(getViewLifecycleOwner(), weeks -> {
            adapter.setWeeks(weeks);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty())
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupListeners() {
        fabAddExtraProduct.setOnClickListener(v -> showAddExtraProductDialog());
        chipFilterStore.setOnClickListener(v -> showStoreFilterDialog());
        chipFilterCategory.setOnClickListener(v -> showCategoryFilterDialog());

        toggleViewMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            viewModel.setWeekViewMode(checkedId == R.id.button_week_view);
        });
    }

    // ===========================
    // FILTROS — usa unfilteredLists para ver todos los valores posibles
    // ===========================

    private void showStoreFilterDialog() {
        List<WeeklyShoppingList> weeks = viewModel.getUnfilteredLists();
        if (weeks == null || weeks.isEmpty()) return;

        // LOG TEMPORAL
        for (WeeklyShoppingList week : weeks) {
            android.util.Log.d("SHOPPING_DEBUG", "Semana: " + week.getWeekId()
                    + " items: " + week.getItems().size());
            for (ShoppingListItem item : week.getItems()) {
                android.util.Log.d("SHOPPING_DEBUG",
                        "  item: " + item.getName()
                                + " | isExtra: " + item.isExtra()
                                + " | store: " + item.getStore()
                                + " | category: " + item.getCategory());
            }
        }
        // FIN LOG

        Set<String> stores = new LinkedHashSet<>();
        for (WeeklyShoppingList week : weeks) {
            for (ShoppingListItem item : week.getItems()) {
                if (item.getStore() != null && !item.getStore().isEmpty())
                    stores.add(item.getStore());
            }
        }

        if (stores.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No hay tiendas en la lista", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = stores.toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("Filtrar por tienda")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    chipFilterStore.setText(selected);
                    chipFilterStore.setChecked(true);
                    viewModel.setStoreFilter(selected);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showCategoryFilterDialog() {
        List<WeeklyShoppingList> weeks = viewModel.getUnfilteredLists();
        if (weeks == null || weeks.isEmpty()) return;

        Set<String> categories = new LinkedHashSet<>();
        for (WeeklyShoppingList week : weeks) {
            for (ShoppingListItem item : week.getItems()) {
                if (item.getCategory() != null && !item.getCategory().isEmpty())
                    categories.add(item.getCategory());
            }
        }

        if (categories.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No hay categorías en la lista", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = categories.toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("Filtrar por categoría")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    chipFilterCategory.setText(selected);
                    chipFilterCategory.setChecked(true);
                    viewModel.setCategoryFilter(selected);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void clearFilters() {
        chipFilterStore.setText("Tienda");
        chipFilterStore.setChecked(false);
        chipFilterCategory.setText("Categoría");
        chipFilterCategory.setChecked(false);
        viewModel.clearFilters();
    }

    // ===========================
    // DIÁLOGOS
    // ===========================

    private void showAddExtraProductDialog() {
        List<WeeklyShoppingList> weeks = viewModel.getUnfilteredLists();
        if (weeks == null || weeks.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No hay semanas disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        if (weeks.size() == 1) {
            AddProductDialog.newInstance(weeks.get(0).getWeekId())
                    .show(getChildFragmentManager(), "AddProductDialog");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String[] weekLabels = new String[weeks.size()];
        for (int i = 0; i < weeks.size(); i++) {
            weekLabels[i] = "Semana del " + sdf.format(weeks.get(i).getMonday());
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("¿A qué semana añadir?")
                .setItems(weekLabels, (dialog, which) ->
                        AddProductDialog.newInstance(weeks.get(which).getWeekId())
                                .show(getChildFragmentManager(), "AddProductDialog"))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showDeleteConfirmation(ShoppingListItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar producto")
                .setMessage("¿Eliminar \"" + item.getName() + "\" de la lista?")
                .setPositiveButton("Eliminar", (dialog, which) ->
                        viewModel.deleteExtraItem(item.getId()))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}