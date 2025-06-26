package com.example.honeybee;

import android.os.Bundle;
import android.widget.Switch;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;

public class SettingsActivity extends AppCompatActivity {
    private GameAudioManager audioManager;
    private Switch backgroundMusicSwitch;
    private Switch gameMusicSwitch;
    private SeekBar backgroundVolumeSeekBar;
    private SeekBar gameVolumeSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize audio manager
        audioManager = GameAudioManager.getInstance(this);

        // Enable back button in ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        backgroundMusicSwitch = findViewById(R.id.backgroundMusicSwitch);
        gameMusicSwitch = findViewById(R.id.gameMusicSwitch);
        backgroundVolumeSeekBar = findViewById(R.id.backgroundVolumeSeekBar);
        gameVolumeSeekBar = findViewById(R.id.gameVolumeSeekBar);

        // Set initial values
        backgroundMusicSwitch.setChecked(audioManager.isBackgroundMusicEnabled());
        gameMusicSwitch.setChecked(audioManager.isGameMusicEnabled());
        backgroundVolumeSeekBar.setProgress((int) (audioManager.getBackgroundVolume() * 100));
        gameVolumeSeekBar.setProgress((int) (audioManager.getGameVolume() * 100));

        // Set up listeners
        backgroundMusicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            audioManager.setBackgroundMusicEnabled(isChecked);
        });

        gameMusicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            audioManager.setGameMusicEnabled(isChecked);
        });

        backgroundVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float volume = progress / 100f;
                    audioManager.setBackgroundVolume(volume);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        gameVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float volume = progress / 100f;
                    audioManager.setGameVolume(volume);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 