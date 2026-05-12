package com.example.pocketmenu.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import static java.util.Collections.emptyList;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.WeeklyMenuTemplate;
import com.example.pocketmenu.ui.adapters.FavoriteTemplatesAdapter;
import com.example.pocketmenu.viewmodel.MenuViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class FavoriteTemplatesDialog extends DialogFragment {

    public interface OnTemplateAppliedListener {
        void onTemplateApplied(int unassignedPortions);
    }

    private OnTemplateAppliedListener appliedListener;
    private MenuViewModel viewModel;
    private FavoriteTemplatesAdapter adapter;

    private RecyclerView recyclerTemplates;
    private TextView textNoTemplates;

    public static FavoriteTemplatesDialog newInstance() {
        return new FavoriteTemplatesDialog();
    }

    // Allows the parent fragment to set a listener
    public void setOnTemplateAppliedListener(OnTemplateAppliedListener listener) {
        this.appliedListener = listener;
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_favorite_templates, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(MenuViewModel.class);

        recyclerTemplates = view.findViewById(R.id.recycler_templates);
        textNoTemplates = view.findViewById(R.id.text_no_templates);
        TextInputEditText editSearch = view.findViewById(R.id.edit_search_template);

        adapter = new FavoriteTemplatesAdapter(
                new FavoriteTemplatesAdapter.OnTemplateActionListener() {
                    @Override
                    public void onApplyClicked(WeeklyMenuTemplate template) {
                        showApplyConfirmation(template);
                    }

                    @Override
                    public void onDeleteClicked(WeeklyMenuTemplate template) {
                        showDeleteConfirmation(template);
                    }
                });

        recyclerTemplates.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTemplates.setAdapter(adapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setSearchText(s.toString());
                updateEmptyState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        viewModel.getFavoriteTemplatesLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading)) {
                textNoTemplates.setText("Cargando...");
                textNoTemplates.setVisibility(View.VISIBLE);
                recyclerTemplates.setVisibility(View.GONE);
            }
        });

        // updates the adapter with the list of templates
        viewModel.getFavoriteTemplates().observe(getViewLifecycleOwner(), templates -> {
            List<WeeklyMenuTemplate> safeTemplates = templates != null ? templates : emptyList();
            adapter.setTemplates(safeTemplates, viewModel.getFavoriteTemplateRecipeNames().getValue());
            updateEmptyState();
        });

        viewModel.getFavoriteTemplateRecipeNames().observe(getViewLifecycleOwner(), names -> {
            List<WeeklyMenuTemplate> templates = viewModel.getFavoriteTemplates().getValue();
            adapter.setTemplates(templates, names);
            updateEmptyState();
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                textNoTemplates.setText(error);
                textNoTemplates.setVisibility(View.VISIBLE);
                recyclerTemplates.setVisibility(View.GONE);
            }
        });

        viewModel.loadFavoriteTemplatesForDialog();
    }

    @Override
    public Dialog onCreateDialog( Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Menús favoritos");
        return dialog;
    }

    // Sets the height and width of the dialog
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // After search filter, adapter may show no rows
    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        textNoTemplates.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerTemplates.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (isEmpty) textNoTemplates.setText("No hay menús favoritos guardados");
    }

    private void showApplyConfirmation(WeeklyMenuTemplate template) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Aplicar menú favorito")
                .setMessage("Se sobreescribirá el menú de la semana actual con \"" + template.getName() + "\". Los datos existentes se eliminarán. ¿Continuar?")
                .setPositiveButton("Aplicar", (dialog, which) -> {
                    viewModel.applyTemplate(template, unassignedPortions -> {
                        if (appliedListener != null)
                            appliedListener.onTemplateApplied(unassignedPortions);
                        dismiss();
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showDeleteConfirmation(WeeklyMenuTemplate template) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar plantilla")
                .setMessage("¿Eliminar \"" + template.getName() + "\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) ->
                        viewModel.deleteFavoriteTemplate(template.getId()))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}