package com.example.pocketmenu.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocketmenu.R;
import com.example.pocketmenu.ui.auth.LogInActivity;
import com.example.pocketmenu.viewmodel.SettingsViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SettingsFragment extends BottomSheetDialogFragment {

    private Button logoutButton;
    private Button deleteAccountButton;
    private ProgressBar progressBar;
    private SettingsViewModel settingsViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        initializeViews(view);
        setupViewModel();
        setupObservers();
        setupListeners();

        return view;
    }

    // Links objects to the layout
    private void initializeViews(View view) {
        logoutButton = view.findViewById(R.id.button_logout);
        deleteAccountButton = view.findViewById(R.id.button_delete_account);
        progressBar = view.findViewById(R.id.progressBar);
    }

    // Creates a fragment-scoped SettingsViewModel instance
    private void setupViewModel() {
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    }

    // Observes logout/delete/error events
    private void setupObservers() {
        settingsViewModel.getLoggedOutLiveData().observe(getViewLifecycleOwner(), loggedOut -> {
            if (loggedOut != null && loggedOut) {
                navigateToLogin();
            }
        });

        settingsViewModel.getAccountDeletedLiveData().observe(getViewLifecycleOwner(), deleted -> {
            if (deleted != null && deleted) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
        });

        settingsViewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Hooks button taps to ViewModel actions
    private void setupListeners() {
        logoutButton.setOnClickListener(v -> settingsViewModel.logOutSession());
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
    }

    // Delete account dialog with password confirmation for re-authentication
    private void showDeleteAccountDialog() {
        EditText passwordInput = new EditText(requireContext());
        passwordInput.setHint("Introduce tu contraseña");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar cuenta")
                .setMessage("Esta acción eliminará todos tus datos y no se puede deshacer.")
                .setView(passwordInput)
                .setPositiveButton("Eliminar", null)
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = passwordInput.getText() != null
                    ? passwordInput.getText().toString().trim()
                    : "";

            if (password.isEmpty()) {
                passwordInput.setError("Introduce tu contraseña");
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            settingsViewModel.deleteAccount(password);
            dialog.dismiss();
        }));

        dialog.show();
    }

    // Clears task stack to prevent returning after logout/delete.
    private void navigateToLogin() {
        Intent intent = new Intent(getActivity(), LogInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
