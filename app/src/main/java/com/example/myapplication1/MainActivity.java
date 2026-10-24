package com.example.myapplication1;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication1.home.HomeFragment;
import com.example.myapplication1.stats.StatsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * The frame of the app: a container (R.id.fragmentContainer) + the bottom navigation bar.
 * It contains NO screen content. Each tab is a Fragment in its own package
 * (home/, subjects/, stats/, settings/).
 *
 * TEAMMATES: when your fragment is ready, replace the matching PlaceholderFragment line
 * in fragmentFor() below with your fragment, e.g. "return new SubjectsFragment();".
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = fragmentFor(item.getItemId());
            if (fragment == null) return false;
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
            return true;   // true = the tapped tab becomes the selected one
        });

        // First launch: show Home. After a rotation Android restores the fragment by itself.
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /** Which fragment belongs to which tab. (if/else, because R.id values are not constants.) */
    private Fragment fragmentFor(int menuItemId) {
        if (menuItemId == R.id.nav_home) {
            return new HomeFragment();
        } else if (menuItemId == R.id.nav_subjects) {
            return PlaceholderFragment.newInstance(getString(R.string.nav_subjects));   // TODO SubjectsFragment
        } else if (menuItemId == R.id.nav_stats) {
            return new StatsFragment();
        } else if (menuItemId == R.id.nav_settings) {
            return PlaceholderFragment.newInstance(getString(R.string.nav_settings));   // TODO SettingsFragment
        }
        return null;
    }
}
