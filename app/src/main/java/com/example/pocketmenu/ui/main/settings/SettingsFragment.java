package com.example.pocketmenu.ui.main.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.pocketmenu.R;
// Importa la actividad de Login que tengas (ajusta el nombre si es diferente)
// import com.example.pocketmenu.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private Button logoutButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 1. Encontrar el botón en el layout
        logoutButton = view.findViewById(R.id.button_logout);

        // 2. Añadir el listener para el clic
        logoutButton.setOnClickListener(v -> {
            // 3. Cerrar la sesión de Firebase
            FirebaseAuth.getInstance().signOut();

            // 4. Redirigir al usuario a la pantalla de Login
            // Asegúrate de que tienes una LoginActivity o un nombre similar
            /*
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            // Flags para limpiar la pila de actividades:
            // - FLAG_ACTIVITY_NEW_TASK: Inicia la actividad en una nueva tarea.
            // - FLAG_ACTIVITY_CLEAR_TASK: Borra cualquier tarea existente asociada con la actividad.
            // El resultado es que el usuario no puede volver a la MainActivity pulsando "atrás".
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            */

            // Si aún no tienes LoginActivity, muestra un Toast para confirmar
            //Toast.makeText(getContext(), "Has cerrado la sesión.", Toast.LENGTH_LONG).show();

            // Opcional: Cierra la actividad actual si es necesario
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }
}
