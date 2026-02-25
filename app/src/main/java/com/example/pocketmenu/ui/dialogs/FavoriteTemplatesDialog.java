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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.WeeklyMenuTemplate;
import com.example.pocketmenu.data.model.WeeklyMenuItem;
import com.example.pocketmenu.data.repository.RecipeRepository;
import com.example.pocketmenu.data.repository.WeeklyMenuTemplateRepository;
import com.example.pocketmenu.ui.adapters.FavoriteTemplatesAdapter;
import com.example.pocketmenu.viewmodel.MenuViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class FavoriteTemplatesDialog extends DialogFragment {

    public interface OnTemplateAppliedListener {
        void onTemplateApplied(int unassignedPortions);
    }

    private OnTemplateAppliedListener appliedListener;
    private MenuViewModel viewModel;
    private FavoriteTemplatesAdapter adapter;
    private WeeklyMenuTemplateRepository templateRepository;
    private RecipeRepository recipeRepository;

    private RecyclerView recyclerTemplates;
    private TextView textNoTemplates;

    public static FavoriteTemplatesDialog newInstance() {
        return new FavoriteTemplatesDialog();
    }

    public void setOnTemplateAppliedListener(OnTemplateAppliedListener listener) {
        this.appliedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_favorite_templates, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment())
                .get(MenuViewModel.class);
        templateRepository = new WeeklyMenuTemplateRepository();
        recipeRepository = new RecipeRepository();

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

        loadTemplates();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Menús favoritos");
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void loadTemplates() {
        textNoTemplates.setText("Cargando...");
        textNoTemplates.setVisibility(View.VISIBLE);
        recyclerTemplates.setVisibility(View.GONE);

        templateRepository.getAllTemplates(
                new WeeklyMenuTemplateRepository.OnTemplatesLoaded() {
                    @Override
                    public void onLoaded(List<WeeklyMenuTemplate> templates) {
                        if (templates.isEmpty()) {
                            textNoTemplates.setText("No hay menús favoritos guardados");
                            textNoTemplates.setVisibility(View.VISIBLE);
                            recyclerTemplates.setVisibility(View.GONE);
                            return;
                        }
                        resolveRecipeNames(templates);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        textNoTemplates.setText("Error cargando plantillas");
                        textNoTemplates.setVisibility(View.VISIBLE);
                        recyclerTemplates.setVisibility(View.GONE);
                    }
                });
    }

    private void resolveRecipeNames(List<WeeklyMenuTemplate> templates) {
        Set<String> recipeIds = new HashSet<>();
        for (WeeklyMenuTemplate template : templates) {
            if (template.getItems() == null) continue;
            for (WeeklyMenuItem item : template.getItems()) {
                if (item.getRecipeId() != null)
                    recipeIds.add(item.getRecipeId());
                if (item.isLeftover() && item.getSourceRecipeId() != null)
                    recipeIds.add(item.getSourceRecipeId());
            }
        }

        if (recipeIds.isEmpty()) {
            adapter.setTemplates(templates, new HashMap<>());
            updateEmptyState();
            return;
        }

        Map<String, String> recipeNames = new HashMap<>();
        List<String> recipeIdList = new ArrayList<>(recipeIds);
        AtomicInteger pending = new AtomicInteger(recipeIdList.size());

        for (String recipeId : recipeIdList) {
            recipeRepository.getRecipeById(recipeId,
                    new RecipeRepository.OnRecipeFound() {
                        @Override
                        public void onFound(com.example.pocketmenu.data.model.Recipe recipe) {
                            synchronized (recipeNames) {
                                recipeNames.put(recipeId, recipe.getName());
                            }
                            if (pending.decrementAndGet() == 0) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        adapter.setTemplates(templates, recipeNames);
                                        updateEmptyState();
                                    });
                                }
                            }
                        }

                        @Override
                        public void onNotFound() {
                            if (pending.decrementAndGet() == 0) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        adapter.setTemplates(templates, recipeNames);
                                        updateEmptyState();
                                    });
                                }
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (pending.decrementAndGet() == 0) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        adapter.setTemplates(templates, recipeNames);
                                        updateEmptyState();
                                    });
                                }
                            }
                        }
                    });
        }
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        textNoTemplates.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerTemplates.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (isEmpty) textNoTemplates.setText("No hay menús favoritos guardados");
    }

    private void showApplyConfirmation(WeeklyMenuTemplate template) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Aplicar menú favorito")
                .setMessage("Se sobreescribirá el menú de la semana actual con \""
                        + template.getName()
                        + "\". Los datos existentes se eliminarán. ¿Continuar?")
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
                .setMessage("¿Eliminar \"" + template.getName()
                        + "\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    templateRepository.deleteTemplate(template.getId(),
                            new WeeklyMenuTemplateRepository.WeeklyMenuCallback() {
                                @Override
                                public void onSuccess() {
                                    loadTemplates();
                                }
                                @Override
                                public void onFailure(Exception e) {
                                    textNoTemplates.setText("Error eliminando plantilla");
                                    textNoTemplates.setVisibility(View.VISIBLE);
                                }
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}