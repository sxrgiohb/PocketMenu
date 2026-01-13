package com.example.pocketmenu.ui.main;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.pocketmenu.R;
import com.example.pocketmenu.ui.main.menu.MenuFragment;
import com.example.pocketmenu.ui.main.recipe.RecipeFragment;
import com.example.pocketmenu.ui.main.settings.SettingsFragment;
import com.example.pocketmenu.ui.main.shoppinglist.ShoppingListFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // References to the views
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
        MaterialToolbar topAppBar = findViewById(R.id.top_app_bar);

        // Load the initial fragment
        if (savedInstanceState == null) {
            loadFragment(new MenuFragment());
        }

        // Bottom navigation listener
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
            // Load fragment
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

        // Top navigation listener
        topAppBar.setOnMenuItemClickListener(menuItem -> {
            // Checks if the item is the settings icon
            if (menuItem.getItemId() == R.id.navigation_settings) {
                loadFragment(new SettingsFragment());
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