package com.example.myapplication1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import com.example.myapplication1.quiz.QuizCoordinator;
import com.example.myapplication1.quiz.delivery.QuizNotifications;
import com.example.myapplication1.quiz.trigger.MomentCondition;
import com.example.myapplication1.quiz.trigger.MomentDetector;
import com.example.myapplication1.quiz.trigger.TriggerManager;
import com.example.myapplication1.quiz.trigger.detectors.IntervalDetector;
import com.example.myapplication1.stats.StatsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * The frame of the app: a container (R.id.fragmentContainer) + the bottom navigation bar.
 * It contains NO screen content. Each tab is a Fragment in its own package
 * (home/, subjects/, stats/, settings/).
 *
 * TEAMMATES: when your fragment is ready, replace the matching PlaceholderFragment line
 * in fragmentFor() below with your fragment, e.g. "return new SubjectsFragment();".
 *
 * It also starts the quiz (notification permission + trigger), because that belongs
 * to the whole app, not to one tab. Quiz code: see the "QUIZ" sections below.
 */
public class MainActivity extends AppCompatActivity {

    // ---- QUIZ: decides WHEN a question should be sent ----
    private TriggerManager triggerManager;

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
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

        // ---- QUIZ: trigger (TEST: a "free moment" every 20 seconds while the app is open) ----
        List<MomentDetector> detectors = new ArrayList<>();
        detectors.add(new IntervalDetector(20_000));
        List<MomentCondition> conditions = new ArrayList<>();   // none yet
        QuizCoordinator quiz = QuizCoordinator.getInstance(this);
        triggerManager = new TriggerManager(detectors, conditions, reason -> {
            Log.d("TRIGGER", "Moment detected: " + reason);
            quiz.onMomentDetected(reason);   // picks the next question and sends the notification
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        triggerManager.start(this);   // QUIZ: app visible, start looking for moments
    }

    @Override
    protected void onStop() {
        super.onStop();
        triggerManager.stop();        // QUIZ: app hidden, stop (the test timer can't run in background)
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
