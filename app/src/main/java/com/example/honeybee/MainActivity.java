package com.example.honeybee;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private Button playButton;
    private Button levelsButton;
    private Button settingsButton;
    private Button quitButton;
    private GameAudioManager audioManager;
    private ImageView flyingBee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Initialize audio manager and start background music
        audioManager = GameAudioManager.getInstance(this);
        audioManager.startBackgroundMusic();
        
        // Initialize views
        playButton = findViewById(R.id.playButton);
        levelsButton = findViewById(R.id.levelsButton);
        settingsButton = findViewById(R.id.settingsButton);
        quitButton = findViewById(R.id.quitButton);
        flyingBee = findViewById(R.id.flyingBee);

        // Start flying bee animation
        startFlyingBeeAnimation();

        // Set click listeners
        playButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PlayActivity.class);
            startActivity(intent);
        });

        levelsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LevelsActivity.class);
            startActivity(intent);
        });

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        quitButton.setOnClickListener(v -> {
            finish();
        });

        // Initialize OpenCV
        if (OpenCVLoader.initLocal()) {
            Toast.makeText(this, "OpenCV loaded successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void startFlyingBeeAnimation() {
        // Create a smooth up and down animation
        TranslateAnimation flyAnimation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, -0.1f, // Move up
            Animation.RELATIVE_TO_SELF, 0.1f  // Move down
        );
        
        flyAnimation.setDuration(1500); // 1.5 seconds for one cycle
        flyAnimation.setRepeatCount(Animation.INFINITE);
        flyAnimation.setRepeatMode(Animation.REVERSE); // Reverse to go back up
        
        flyingBee.startAnimation(flyAnimation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume background music when returning to main activity
        audioManager.resumeBackgroundMusic();
        // Restart flying bee animation
        startFlyingBeeAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release audio resources when app is closed
        audioManager.release();
    }
}