package com.example.pocketmenu.ui.main;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

// Importaciones necesarias para el cierre de sesión:
import androidx.lifecycle.ViewModelProvider;
import android.content.Intent;
import android.widget.Button;

import com.example.pocketmenu.R;
import com.example.pocketmenu.viewmodel.AuthViewModel;
import com.example.pocketmenu.ui.auth.LogInActivity;

public class MainActivity extends AppCompatActivity {

    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Vincularemos esta Activity con el layout activity_main.xml
        setContentView(R.layout.activity_main);

        // 1. Inicializar el ViewModel
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 2. Inicializar la Vista (el botón)
        Button logoutButton = findViewById(R.id.logoutButton);

        // 3. Observador para el Cierre de Sesión
        viewModel.getLoggedOutLiveData().observe(this, loggedOut -> {
            if (loggedOut) {
                // Si loggedOut es true (el repositorio confirmó el cierre)
                // Navega a la pantalla de Login y finaliza la actividad actual
                Intent intent = new Intent(MainActivity.this, LogInActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 4. Listener del Botón
        logoutButton.setOnClickListener(v -> {
            // Llama al metodo del ViewModel que delega la tarea al AuthRepository
            viewModel.logOutSession();
        });
    }
}