package com.example.pocketmenu.ui.main;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pocketmenu.data.repository.LeftoverRepository;
import com.example.pocketmenu.data.repository.MenuRepository;
import com.google.firebase.auth.FirebaseAuth;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.pocketmenu.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// Hosts bottom navigation: menu, shopping list, recipes
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Data cleanup when a user session exists
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            new MenuRepository().deleteMenusOlderThan(15, null);
            new LeftoverRepository().deleteExpiredPerishableLeftovers(null);
        }

        setContentView(R.layout.activity_main);

        // Adapt content
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.top_app_bar), (v, windowInsets) -> {
            // System status bar (clock, battery...)
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
        MaterialToolbar topAppBar = findViewById(R.id.top_app_bar);

        if (savedInstanceState == null) {
            loadFragment(new MenuFragment());
        }

        // Bottom nevigation
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_menu) {
                selectedFragment = new MenuFragment();
            } else if (itemId == R.id.navigation_shopping_list) {
                selectedFragment = new ShoppingListFragment();
            } else if (itemId == R.id.navigation_recipe) {
                selectedFragment = new RecipeFragment();
            }
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

        // Top navigation
        topAppBar.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.navigation_settings) {
                SettingsFragment settingsFragment = new SettingsFragment();
                settingsFragment.show(getSupportFragmentManager(), "settings");
                return true;
            }
            return false;
        });
    }

    // Method to load a fragment
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.nav_host_fragment, fragment);
        // Adds the fragment to the back stack
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}