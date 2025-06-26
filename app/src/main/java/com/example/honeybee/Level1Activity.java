package com.example.honeybee;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;

public class Level1Activity extends AppCompatActivity {
    private GameView gameView;
    private Button startButton;
    private boolean isDialogShowing = false;
    private GameAudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level1);

        // Initialize audio manager
        audioManager = GameAudioManager.getInstance(this);

        // Enable back button in ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        gameView = findViewById(R.id.gameView);
        startButton = findViewById(R.id.startButton);

        // Set up start button
        startButton.setOnClickListener(v -> {
            startButton.setVisibility(Button.INVISIBLE);
            gameView.startGame();
            // Restart game music when the game begins
            audioManager.restartGameMusic();
        });

        // Show instructions when first opening the level
        showInstructionsDialog();
    }

    @SuppressLint("SetTextI18n")
    private void showInstructionsDialog() {
        if (isDialogShowing) return;
        
        isDialogShowing = true;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.level1_custom_dialog);
        dialog.setCancelable(false);

        TextView instructionsText = dialog.findViewById(R.id.instructionsText);
        instructionsText.setText("Welcome to Lesson 1: Help the Bee Find a Flower!\n\n" +
                               "Let's learn how bees collect nectar from flowers!\n\n" +
                               "• Tap the screen to help your bee flap its wings and fly up\n" +
                               "• Watch out for obstacles like branches and clouds\n" +
                               "• Guide your bee safely through 10 spaces to reach the flower\n" +
                               "• Keep your bee flying - just like real bees work hard to find flowers!");

        Button closeButton = dialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> {
            dialog.dismiss();
            isDialogShowing = false;
        });

        dialog.setOnDismissListener(d -> isDialogShowing = false);
        dialog.show();
    }

    public void playInteractSound() {
        audioManager.playInteractSound();
    }

    public void onGameOver(int finalScore) {
        runOnUiThread(() -> {
            startButton.setVisibility(Button.VISIBLE);
            // Stop game music and play lose sound
            audioManager.stopGameMusic();
            audioManager.playLoseSound();
            showResultDialog(false, finalScore);
        });
    }

    public void onGameWon(int finalScore) {
        runOnUiThread(() -> {
            startButton.setVisibility(Button.VISIBLE);
            // Stop game music and play win sound
            audioManager.stopGameMusic();
            audioManager.playWinSound();
            showResultDialog(true, finalScore);
        });
    }

    @SuppressLint("SetTextI18n")
    private void showResultDialog(boolean won, int finalScore) {
        if (isDialogShowing) return;
        
        isDialogShowing = true;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.level1_result_dialog);
        dialog.setCancelable(false);

        TextView resultText = dialog.findViewById(R.id.resultText);
        Button okButton = dialog.findViewById(R.id.okButton);

        if (won) {
            resultText.setText("Amazing job, young beekeeper! 🌟\n\n" +
                             "You've helped our bee find a flower! Now it's time to learn what happens to the nectar inside the bee's stomach.\n\n" +
                             "Progress: " + finalScore + "/10");
            okButton.setText("Next Lesson");
            okButton.setOnClickListener(v -> {
                dialog.dismiss();
                isDialogShowing = false;
                // Go to next level
                Intent intent = new Intent(Level1Activity.this, Level2Activity.class);
                startActivity(intent);
                finish();
            });
        } else {
            resultText.setText("Oh no! Our bee hit an obstacle!\n\n" +
                             "Just like real bees need to carefully navigate around obstacles, let's try again to find a safe path to the flower.\n\n" +
                             "Progress: " + finalScore + "/10");
            okButton.setText("Try Again");
            okButton.setOnClickListener(v -> {
                dialog.dismiss();
                isDialogShowing = false;
            });
        }

        dialog.setOnDismissListener(d -> isDialogShowing = false);
        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause game music when activity is paused
        audioManager.stopGameMusic();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart game music if game is active
        if (gameView != null && gameView.isGameActive()) {
            audioManager.restartGameMusic();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 