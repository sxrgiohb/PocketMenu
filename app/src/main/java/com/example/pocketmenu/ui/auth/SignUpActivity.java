package com.example.pocketmenu.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocketmenu.R;
import com.example.pocketmenu.ui.main.MainActivity;
import com.example.pocketmenu.viewmodel.AuthViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity {

    //private AuthViewModel viewModel;

    private AuthViewModel viewModel;
    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private TextView requirementLength, requirementNumber, requirementCapital;
    private Button registerButton;
    private ProgressBar progressBar;

    private TextView gologInText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initializeViews();
        setupViewModel();
        setupListeners();
        setupObservers();

    }

    // Links objects to the layout
    private void initializeViews() {
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);
        requirementLength = findViewById(R.id.requirementLengthTextView);
        requirementNumber = findViewById(R.id.requirementNumberTextView);
        requirementCapital = findViewById(R.id.requirementCapitalTextView);
    }

    // Links the view model to the activity
    private void setupViewModel () {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    // Observes the changes in the view model
    private void setupObservers () {
        viewModel.getUserLiveData().observe(this, firebaseUser -> {
            progressBar.setVisibility(View.GONE);
            if (firebaseUser != null) {
                Toast.makeText(this, "Registro completado", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });

        viewModel.getErrorMessageLiveData().observe(this, errorMessage -> {
            progressBar.setVisibility(View.GONE);
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Listens to the buttons and text fields
    private void setupListeners () {
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword(s.toString());
            }
        });

        registerButton.setOnClickListener(v -> {
            handleRegistration();

        });
    }

    // Method to register a new user
    private void handleRegistration () {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Check if fields are empty
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validatePassword(password)) {
            Toast.makeText(this, "La contraseña no cumple los requisitos", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        viewModel.registerNewUser(email, password, name);

    }

    // Method to validate the password
    private boolean validatePassword (String password){
        int notValidColor = ContextCompat.getColor(this, android.R.color.darker_gray);
        int isValidColor = ContextCompat.getColor(this, R.color.green);

        requirementLength.setTextColor(password.length() >= 8 ? isValidColor : notValidColor);
        requirementNumber.setTextColor(password.matches(".*\\d.*") ? isValidColor : notValidColor);
        requirementCapital.setTextColor(password.matches(".*[A-Z].*") ? isValidColor : notValidColor);
        return password.length() >= 8 &&
                password.matches(".*\\d.*") &&
                password.matches(".*[A-Z].*");
    }


}
