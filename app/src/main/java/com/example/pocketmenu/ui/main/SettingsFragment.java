package com.example.pocketmenu.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    private void initializeViews(View view) {
        logoutButton = view.findViewById(R.id.button_logout);
        deleteAccountButton = view.findViewById(R.id.button_delete_account);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupViewModel() {
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    }

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

    private void setupListeners() {
        logoutButton.setOnClickListener(v -> settingsViewModel.logOutSession());
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar cuenta")
                .setMessage("¿Estás seguro de que quieres eliminar tu cuenta? Esta acción eliminará todos tus datos y no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    settingsViewModel.deleteAccount();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(getActivity(), LogInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}