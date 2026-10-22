package com.example.myapplication1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import com.example.myapplication1.quiz.QuizCoordinator;
import com.example.myapplication1.quiz.delivery.QuizNotifications;
import com.example.myapplication1.quiz.trigger.MomentCondition;
import com.example.myapplication1.quiz.trigger.MomentDetector;
import com.example.myapplication1.quiz.trigger.TriggerManager;
import com.example.myapplication1.quiz.trigger.detectors.IntervalDetector;

import java.util.ArrayList;
import java.util.List;

/**
 * Home screen.
 * The XML (activity_main.xml) says HOW the screen looks;
 * this class says WHAT data appears in it and WHAT happens on taps.
 */
public class MainActivity extends AppCompatActivity {

    // Screen data. Hard-coded for now; later it will come from the database.
    private int questionsToday = 14;
    private int dailyGoal = 20;
    private int streakDays = 6;
    private int accuracyPercent = 82;
    private int subject1Percent = 62;
    private int subject2Percent = 35;

    // ---- Quiz trigger: decides WHEN a question should be sent ----
    private TriggerManager triggerManager;

    // ---- Quiz notifications: asks the user for permission and tells us the answer ----
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

        // ---- 1. Bind the XML views to Java objects ----
        TextView txtSolvedToday = findViewById(R.id.txtSolvedToday);
        TextView txtGoal = findViewById(R.id.txtGoal);
        LinearProgressIndicator progressGoal = findViewById(R.id.progressGoal);
        TextView txtStreak = findViewById(R.id.txtStreak);
        TextView txtAccuracy = findViewById(R.id.txtAccuracy);
        TextView txtSubject1Percent = findViewById(R.id.txtSubject1Percent);
        TextView txtSubject2Percent = findViewById(R.id.txtSubject2Percent);
        MaterialButton btnWhyNow = findViewById(R.id.btnWhyNow);
        MaterialButton btnUpload = findViewById(R.id.btnUpload);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // ---- 2. Put the data on screen ----
        // getString(id, a, b) takes the text from strings.xml and replaces %1$d, %2$d with a, b
        txtSolvedToday.setText(getString(R.string.solved_today, questionsToday));
        txtGoal.setText(getString(R.string.goal_format, questionsToday, dailyGoal));
        progressGoal.setMax(dailyGoal);
        progressGoal.setProgress(questionsToday);
        txtStreak.setText(getString(R.string.days_format, streakDays));
        txtAccuracy.setText(getString(R.string.percent_format, accuracyPercent));
        txtSubject1Percent.setText(getString(R.string.percent_format, subject1Percent));
        txtSubject2Percent.setText(getString(R.string.percent_format, subject2Percent));

        // ---- 3. What happens on taps (just a message for now) ----
        btnWhyNow.setOnClickListener(view ->
                Toast.makeText(this, "The \"Why now?\" screen will open here", Toast.LENGTH_SHORT).show());

        btnUpload.setOnClickListener(view ->
                Toast.makeText(this, "The \"Upload material\" screen will open here", Toast.LENGTH_SHORT).show());

        bottomNav.setOnItemSelectedListener(item -> {
            Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
            return true;   // true = the tapped tab becomes the selected one
        });

        // ---- 4. Quiz notifications setup ----
        QuizNotifications.createChannel(this);
        askForNotificationPermission();

        // ---- 5. Quiz trigger (TEST: a "free moment" every 20 seconds while the app is open) ----
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
        triggerManager.start(this);   // app visible: start looking for moments
    }

    @Override
    protected void onStop() {
        super.onStop();
        triggerManager.stop();        // app hidden: stop (the test timer can't run in background)
    }

    /** Shows the system dialog "Allow Rewinder to send you notifications?" if not allowed yet. */
    private void askForNotificationPermission() {
        boolean alreadyGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        if (!alreadyGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
