package com.example.honeybee;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;

public class Level2Activity extends AppCompatActivity {
    private Level2GameView gameView;
    private boolean isDialogShowing = false;
    private CountDownTimer timer;
    private static final long GAME_TIME_MS = 30000; // 30 seconds
    private boolean gameStarted = false;
    private boolean gameEnded = false;
    private TextView timerText;
    private long timeLeftMs = GAME_TIME_MS;
    private GameAudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level2);

        // Initialize audio manager
        audioManager = GameAudioManager.getInstance(this);

        // Enable back button in ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        gameView = findViewById(R.id.level2GameView);
        timerText = findViewById(R.id.timerText);
        gameView.setEnabled(false);
        gameView.setActivity(this);
        showInstructionsDialog();
    }

    @SuppressLint("SetTextI18n")
    private void showInstructionsDialog() {
        if (isDialogShowing) return;
        isDialogShowing = true;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.level2_custom_dialog);
        dialog.setCancelable(false);

        TextView instructionsText = dialog.findViewById(R.id.instructionsText);
        instructionsText.setText("Welcome to Lesson 2: Inside a Bee's Stomach!\n\n" +
                "Time to process the nectar!\n\n" +
                "• Drag enzymes (blue or red circles) to the yellow nectar drops\n" +
                "• Find out which enzyme works correctly\n" +
                "• Process all nectar within 30 seconds\n\n" +
                "Fun Fact: Bees have different enzymes for different jobs!");

        Button closeButton = dialog.findViewById(R.id.closeButton);
        closeButton.setText("Let's Begin!");
        closeButton.setOnClickListener(v -> {
            dialog.dismiss();
            isDialogShowing = false;
            startGame();
        });

        dialog.setOnDismissListener(d -> isDialogShowing = false);
        dialog.show();
    }

    private void startGame() {
        gameStarted = true;
        gameEnded = false;
        timeLeftMs = GAME_TIME_MS;
        updateTimerText();
        gameView.setEnabled(true);
        gameView.resetGame();
        startTimer();
        gameView.postDelayed(this::checkGameEnd, 100);
        // Restart game music when the game begins
        audioManager.restartGameMusic();
    }

    private void startTimer() {
        timer = new CountDownTimer(GAME_TIME_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMs = millisUntilFinished;
                updateTimerText();
            }
            @Override
            public void onFinish() {
                timeLeftMs = 0;
                updateTimerText();
                if (!gameEnded) {
                    gameEnded = true;
                    showResultDialog(false, 0);
                }
            }
        };
        timer.start();
    }

    private void updateTimerText() {
        int seconds = (int) (timeLeftMs / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);
        if (timerText != null) timerText.setText(timeStr);
    }

    private void checkGameEnd() {
        if (gameEnded || !gameStarted) return;
        if (gameView.isGameComplete()) {
            gameEnded = true;
            if (timer != null) timer.cancel();
            // Stop game music before showing dialog
            audioManager.stopGameMusic();
            showResultDialog(true, gameView.getScore());
        } else {
            gameView.postDelayed(this::checkGameEnd, 100);
        }
    }

    @SuppressLint("SetTextI18n")
    private void showResultDialog(boolean won, int score) {
        if (isDialogShowing) return;
        isDialogShowing = true;

        // Play win or lose sound
        if (won) {
            audioManager.playWinSound();
        } else {
            audioManager.playLoseSound();
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.level2_result_dialog);
        dialog.setCancelable(false);

        TextView resultText = dialog.findViewById(R.id.resultText);
        Button okButton = dialog.findViewById(R.id.okButton);

        if (won) {
            resultText.setText("Fantastic work, young scientist! 🐝\n\n" +
                             "You've helped break down all the nectar into simple sugars, just like a real bee's stomach!\n\n" +
                             "Now we're ready to learn how these sugars become honey in the next lesson!");
            okButton.setText("Next Lesson");
            okButton.setOnClickListener(v -> {
                dialog.dismiss();
                isDialogShowing = false;
                Intent intent = new Intent(Level2Activity.this, Level3Activity.class);
                startActivity(intent);
                finish();
            });
        } else {
            resultText.setText("Time's up!\n\n" +
                             "Bees need to work quickly to process nectar before it spoils.\n" +
                             "Let's try again to help our bee break down the nectar faster!");
            okButton.setText("Try Again");
            okButton.setOnClickListener(v -> {
                dialog.dismiss();
                isDialogShowing = false;
                startGame();
            });
        }

        dialog.setOnDismissListener(d -> isDialogShowing = false);
        dialog.show();
    }

    public void onWrongEnzyme() {
        if (timer != null) {
            timer.cancel();
        }
        // Stop game music and play lose sound
        audioManager.stopGameMusic();
        audioManager.playLoseSound();
        showWrongEnzymeDialog();
    }

    public void onGameWon(int score) {
        if (timer != null) {
            timer.cancel();
        }
        // Stop game music and play win sound
        audioManager.stopGameMusic();
        audioManager.playWinSound();
        runOnUiThread(() -> {
            gameView.setEnabled(false);
            showResultDialog(true, score);
        });
    }

    private void showWrongEnzymeDialog() {
        if (isDialogShowing) return;
        isDialogShowing = true;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.level2_result_dialog);
        dialog.setCancelable(false);

        TextView resultText = dialog.findViewById(R.id.resultText);
        Button okButton = dialog.findViewById(R.id.okButton);

        resultText.setText("Oops! That's not the right enzyme for nectar!\n\n" +
                         "Try the other enzyme and see what happens!");

        okButton.setText("Try Again");
        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            isDialogShowing = false;
            startGame();
        });

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
        if (gameStarted && !gameEnded) {
            audioManager.restartGameMusic();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 