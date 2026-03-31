package com.example.pocketmenu.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocketmenu.R;
import com.example.pocketmenu.data.model.Product;
import com.example.pocketmenu.data.model.ShoppingListItem;
import com.example.pocketmenu.viewmodel.ShoppingListViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AddProductDialog extends DialogFragment {

    private ShoppingListViewModel viewModel;
    private String weekId;
    private AutoCompleteTextView autocompleteName;
    private TextInputEditText editQuantity;
    private TextInputEditText editUnit;
    private TextInputEditText editCategory;
    private TextInputEditText editStore;
    private Product matchedProduct = null;
    private boolean isFillingFromSuggestion = false;

    public static AddProductDialog newInstance(String weekId) {
        AddProductDialog dialog = new AddProductDialog();
        Bundle args = new Bundle();
        args.putString("weekId", weekId);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_product, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Añadir producto");
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) weekId = getArguments().getString("weekId");

        viewModel = new ViewModelProvider(requireParentFragment())
                .get(ShoppingListViewModel.class);

        autocompleteName = view.findViewById(R.id.autocomplete_product_name);
        autocompleteName.setDropDownAnchor(R.id.autocomplete_product_name);
        editQuantity = view.findViewById(R.id.edit_product_quantity);
        editUnit = view.findViewById(R.id.edit_product_unit);
        editCategory = view.findViewById(R.id.edit_product_category);
        editStore = view.findViewById(R.id.edit_product_store);
        Button buttonSave = view.findViewById(R.id.button_save_product);
        Button buttonCancel = view.findViewById(R.id.button_cancel_product);

        setupAutocomplete();

        viewModel.getProductSuggestions().observe(getViewLifecycleOwner(), products -> {
            if (products == null) return;
            List<String> names = new ArrayList<>();
            for (Product p : products) names.add(p.getName());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, names);
            autocompleteName.setAdapter(adapter);
        });

        buttonSave.setOnClickListener(v -> saveProduct());
        buttonCancel.setOnClickListener(v -> dismiss());
    }

    private void setupAutocomplete() {
        autocompleteName.setThreshold(1);

        autocompleteName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isFillingFromSuggestion) return;
                matchedProduct = null;
                clearFieldsExceptName();
                String text = s.toString().trim();
                if (!text.isEmpty()) {
                    String normalized = text.substring(0, 1).toUpperCase()
                            + text.substring(1);
                    viewModel.searchProductSuggestions(normalized);
                } else {
                    viewModel.searchProductSuggestions("");
                }
            }
        });

        viewModel.getProductSuggestions().observe(getViewLifecycleOwner(), products -> {
            if (products == null || products.isEmpty()) {
                autocompleteName.dismissDropDown();
                return;
            }
            if (matchedProduct != null) return;
            List<String> names = new ArrayList<>();
            for (Product p : products) names.add(p.getName());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    names);
            autocompleteName.setAdapter(adapter);
            autocompleteName.post(() -> {
                if (autocompleteName.getText().length() > 0) {
                    autocompleteName.showDropDown();
                }
            });
        });

        autocompleteName.setOnItemClickListener((parent, v, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            List<Product> suggestions = viewModel.getProductSuggestions().getValue();
            if (suggestions != null) {
                for (Product p : suggestions) {
                    if (p.getName().equalsIgnoreCase(selectedName)) {
                        isFillingFromSuggestion = true;
                        matchedProduct = p;
                        fillFieldsFromProduct(p);
                        isFillingFromSuggestion = false;
                        break;
                    }
                }
            }
            autocompleteName.dismissDropDown();
            autocompleteName.setAdapter(null);
            editQuantity.requestFocus();
        });
    }

    private void fillFieldsFromProduct(Product product) {
        editUnit.setText(product.getUnit() != null ? product.getUnit() : "");
        editCategory.setText(product.getCategory() != null ? product.getCategory() : "");
        editStore.setText(product.getStore() != null ? product.getStore() : "");
        editQuantity.requestFocus();
    }

    private void clearFieldsExceptName() {
        editUnit.setText("");
        editCategory.setText("");
        editStore.setText("");
    }

    private void saveProduct() {
        String raw = autocompleteName.getText() != null
                ? autocompleteName.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            autocompleteName.setError("Nombre obligatorio");
            return;
        }
        String name = raw.substring(0, 1).toUpperCase() + raw.substring(1);

        String qtyStr = editQuantity.getText() != null
                ? editQuantity.getText().toString().trim() : "";
        double quantity = 0;
        if (!qtyStr.isEmpty()) {
            try {
                quantity = Double.parseDouble(qtyStr);
                if (quantity <= 0) {
                    editQuantity.setError("La cantidad debe ser mayor que 0");
                    return;
                }
            } catch (NumberFormatException e) {
                editQuantity.setError("Cantidad inválida");
                return;
            }
        }

        String unit = editUnit.getText() != null
                ? editUnit.getText().toString().trim() : "";
        String category = editCategory.getText() != null
                ? editCategory.getText().toString().trim() : "";
        String store = editStore.getText() != null
                ? editStore.getText().toString().trim() : "";

        ShoppingListItem item = new ShoppingListItem(
                null, weekId, name, quantity, unit, category, store, true);

        boolean isNewProduct = matchedProduct == null;
        Product product = isNewProduct
                ? new Product(null, name, unit, category, store)
                : null;

        viewModel.addExtraItem(item, isNewProduct, product);
        dismiss();
    }
}