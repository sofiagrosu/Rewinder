package com.example.myapplication1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication1.home.HomeFragment;
import com.example.myapplication1.quiz.QuizService;
import com.example.myapplication1.quiz.delivery.QuizNotifications;
import com.example.myapplication1.stats.StatsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;


/**
 * The frame of the app: a container (R.id.fragmentContainer) + the bottom navigation bar.
 * It contains NO screen content. Each tab is a Fragment in its own package
 * (home/, subjects/, stats/, settings/).
 *
 * TEAMMATES: when your fragment is ready, replace the matching PlaceholderFragment line
 * in fragmentFor() below with your fragment, e.g. "return new SubjectsFragment();".
 *
 * It also starts the quiz (notification permission + QuizService, which runs the trigger
 * in the background), because that belongs to the whole app, not to one tab.
 * Quiz code: see the "QUIZ" sections below.
 */
public class MainActivity extends AppCompatActivity {

    // ---- QUIZ: asks the user for the notification permission and tells us the answer ----
    // Must be a field (registered before the activity starts), not created inside onCreate's code.
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                String message = granted ? "Notifications allowed" : "Notifications denied";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Keep the content below the status bar (top) and clear of any side bars (landscape).
        // The BOTTOM inset is deliberately NOT applied here: BottomNavigationView adds it by
        // itself (its background extends behind the system navigation bar, the items stay
        // above it). Applying it here too pushed the bar up and left an empty white strip
        // under it, very visible on phones with a 3-button navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // ---- Navigation between tabs ----
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

        // ---- QUIZ: notifications setup ----
        QuizNotifications.createChannel(this);
        askForNotificationPermission();

        // ---- QUIZ: start the background service that sends the questions ----
        // It keeps running when the app is closed; stop it from its notification ("Stop").
        QuizService.start(this);
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

    /**
     * QUIZ: shows the system dialog "Allow Rewinder to send you notifications?" if not allowed yet.
     * The permission only exists since Android 13 (API 33); on older phones notifications
     * are allowed by default, so there is nothing to ask.
     */
    private void askForNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;   // Android 12 or older

        boolean alreadyGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        if (!alreadyGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
