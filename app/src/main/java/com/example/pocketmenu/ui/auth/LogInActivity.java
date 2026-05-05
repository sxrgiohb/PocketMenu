package com.example.pocketmenu.ui.auth;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.pocketmenu.R;
import com.example.pocketmenu.viewmodel.AuthViewModel;
import com.example.pocketmenu.ui.main.MainActivity;


public class LogInActivity extends AppCompatActivity {
    private AuthViewModel viewModel;
    private EditText emailEditText, passwordEditText;
    private Button goSignUpButton, logInButton;
    private ProgressBar progressBar;

    private final ActivityResultLauncher<Intent> registerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    navigateToMain();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Forced light theme
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupViewModel();
        setupObservers();
        setupListeners();
    }

    // Links objects to the layout
    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        progressBar = findViewById(R.id.progressBar);
        logInButton = findViewById(R.id.logInButton);
        goSignUpButton = findViewById(R.id.goSignUpButton);
    }

    // Links the view model to the activity
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    // Observes the changes in the view model
    private void setupObservers() {
        viewModel.getUserLiveData().observe(this, userLiveData -> {
            progressBar.setVisibility(View.GONE);
            if (userLiveData != null) {
                navigateToMain();
            }
        });

        viewModel.getErrorMessageLiveData().observe(this, errorMessage -> {
            progressBar.setVisibility(View.GONE);
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Inputs listeners
    private void setupListeners() {
        logInButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LogInActivity.this, "Introduce el correo", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(LogInActivity.this, "Introduce la contraseña", Toast.LENGTH_SHORT).show();
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            viewModel.logInSession(email, password);
        });

        goSignUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            registerLauncher.launch(intent);
        });
    }

    // Method to navigate to the main activity
    private void navigateToMain() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}