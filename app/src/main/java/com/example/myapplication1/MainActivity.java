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
import com.example.myapplication1.quiz.settings.QuizSettings;
import com.example.myapplication1.quiz.settings.QuizSettingsFragment;
import com.example.myapplication1.quiz.delivery.QuizNotifications;
import com.example.myapplication1.quiz.ui.QuestionBanner;
import com.example.myapplication1.stats.StatsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.ArrayList;


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

    // ---- QUIZ: asks the user for the permissions the quiz needs (notifications, step sensor) ----
    // Must be a field (registered before the activity starts), not created inside onCreate's code.
    private final ActivityResultLauncher<String[]> quizPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), results -> {
                if (Boolean.FALSE.equals(results.get(Manifest.permission.ACTIVITY_RECOGNITION))) {
                    Toast.makeText(this, "Without physical activity access, Rewinder can't tell when you stop walking",
                            Toast.LENGTH_LONG).show();
                }
                if (Boolean.FALSE.equals(results.get(Manifest.permission.POST_NOTIFICATIONS))) {
                    Toast.makeText(this, "Notifications denied: questions can't be shown", Toast.LENGTH_LONG).show();
                }
                QuizService.restartDetectors(this);   // detectors that were missing a permission start now
            });

    // ---- QUIZ: the question waiting for an answer, as a banner above the tabs ----
    private QuestionBanner questionBanner;

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

        // ---- QUIZ: in-app banner of the question waiting for an answer ----
        // Shown whenever a question is pending, e.g. when the app is opened from the notification.
        questionBanner = new QuestionBanner(findViewById(R.id.questionBanner));

        // ---- QUIZ: notifications setup ----
        QuizNotifications.createChannel(this);

        // ---- QUIZ: start the background service that sends the questions ----
        // It keeps running when the app is closed; stop it from its notification ("Stop").
        QuizService.start(this);
        askForQuizPermissions();   // if something is granted now, the service restarts its detectors
    }

    // ---- QUIZ: the banner follows the quiz only while the app is on screen ----
    @Override
    protected void onStart() {
        super.onStart();
        questionBanner.start();
    }

    @Override
    protected void onStop() {
        questionBanner.stop();
        super.onStop();
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
            // TEMPORARY quiz settings (quiet hours, calendar), until the team's SettingsFragment exists
            return new QuizSettingsFragment();
        }
        return null;
    }

    /**
     * QUIZ: asks, in one dialog sequence, for the permissions that are still missing:
     *   - POST_NOTIFICATIONS (Android 13+ only; before that, notifications are allowed by default)
     *   - ACTIVITY_RECOGNITION (Android 10+), needed by the step sensor (BreakDetector)
     *   - READ_CALENDAR, only if the calendar is turned on in the quiz settings
     */
    private void askForQuizPermissions() {
        List<String> missing = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && !isGranted(Manifest.permission.ACTIVITY_RECOGNITION)) {
            missing.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        // only if the user turned the calendar on (Settings); off by default
        if (new QuizSettings(this).isCalendarEnabled()
                && !isGranted(Manifest.permission.READ_CALENDAR)) {
            missing.add(Manifest.permission.READ_CALENDAR);
        }
        if (!missing.isEmpty()) {
            quizPermissionsLauncher.launch(missing.toArray(new String[0]));
        }
    }

    private boolean isGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
