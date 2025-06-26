package com.example.honeybee;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;

public class LevelsActivity extends AppCompatActivity {
    private ImageButton level1Button;
    private ImageButton level2Button;
    private ImageButton level3Button;
    private GameAudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_levels);

        audioManager = GameAudioManager.getInstance(this);

        // Enable back button in ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        level1Button = findViewById(R.id.level1Button);
        level2Button = findViewById(R.id.level2Button);
        level3Button = findViewById(R.id.level3Button);

        level1Button.setOnClickListener(v -> {
            Intent intent = new Intent(LevelsActivity.this, Level1Activity.class);
            startActivity(intent);
        });

        level2Button.setOnClickListener(v -> {
            Intent intent = new Intent(LevelsActivity.this, Level2Activity.class);
            startActivity(intent);
        });

        level3Button.setOnClickListener(v -> {
            Intent intent = new Intent(LevelsActivity.this, Level3Activity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        audioManager.resumeBackgroundMusic();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 