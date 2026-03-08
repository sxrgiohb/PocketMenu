package com.example.pocketmenu.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocketmenu.R;
import com.example.pocketmenu.ui.auth.LogInActivity;
import com.example.pocketmenu.viewmodel.AuthViewModel;

public class SettingsFragment extends Fragment {

    private Button logoutButton;
       private AuthViewModel authViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        logoutButton = view.findViewById(R.id.button_logout);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        authViewModel.getLoggedOutLiveData().observe(getViewLifecycleOwner(), loggedOut -> {
            if (loggedOut != null && loggedOut) {
                Intent intent = new Intent(getActivity(), LogInActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        logoutButton.setOnClickListener(v -> authViewModel.logOutSession());
        return view;
    }
}