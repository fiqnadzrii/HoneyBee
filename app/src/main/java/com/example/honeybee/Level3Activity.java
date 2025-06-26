package com.example.honeybee;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;
import android.widget.ImageView;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.graphics.PointF;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.Scalar;
import android.media.AudioManager;

public class Level3Activity extends AppCompatActivity {
    private Button fanBtn;
    private ProgressBar progressBar;
    private TextView timerText;
    private CountDownTimer timer;
    private boolean isGameStarted = false;
    private boolean isGameEnded = false;
    private boolean isDialogShowing = false;
    private int progress = 0;
    private static final int MAX_PROGRESS = 100;
    private static final int TIMER_DURATION = 15000; // 15 seconds in milliseconds
    private static final int TIMER_INTERVAL = 1000; // 1 second interval
    private FrameLayout beeContainer;
    private ImageView beehiveImage;
    private Bitmap originalBeehive;
    private Mat beehiveMat;
    private List<ImageView> beeImages = new ArrayList<>();
    private List<Boolean> beeUpStates = new ArrayList<>();
    private static final int BEE_COUNT = 10;
    private Random random = new Random();
    private Handler beeMoveHandler = new Handler(Looper.getMainLooper());
    private Runnable beeMoveRunnable;
    private boolean isBeeMoving = false;
    private static final int BEE_MOVE_INTERVAL = 16; // ms, ~60fps
    private boolean isFast = false;
    private static final float SLOW_SPEED = 1.5f;
    private static final float FAST_SPEED = 50.0f;
    private List<Bee> bees = new ArrayList<>();
    private Handler speedHandler = new Handler(Looper.getMainLooper());
    private Runnable speedResetRunnable;
    private static final int SPEED_BOOST_DURATION = 150; // ms
    private TextView tapInstructions;
    private GameAudioManager audioManager;

    private static class Bee {
        ImageView view;
        PointF position;
        PointF direction;
        float speed;
        Bee(ImageView v, PointF pos, PointF dir, float spd) {
            view = v;
            position = pos;
            direction = dir;
            speed = spd;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level3);

        // Initialize audio manager
        audioManager = GameAudioManager.getInstance(this);

        // Initialize OpenCV
        OpenCVLoader.initDebug();

        // Enable back button in ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        progressBar = findViewById(R.id.progressBar);
        timerText = findViewById(R.id.timerText);
        beeContainer = findViewById(R.id.beeContainer);
        beehiveImage = findViewById(R.id.beehiveImage);
        tapInstructions = findViewById(R.id.tapInstructions);

        // Save original beehive image
        if (beehiveImage.getDrawable() != null) {
            originalBeehive = ((BitmapDrawable) beehiveImage.getDrawable()).getBitmap();
            beehiveMat = new Mat();
            Utils.bitmapToMat(originalBeehive, beehiveMat);
        }

        // Set up progress bar
        progressBar.setMax(MAX_PROGRESS);
        progressBar.setProgress(0);

        // Set up touch listener
        beeContainer.setOnClickListener(v -> {
            if (!isGameStarted && !isGameEnded) {
                startGame();
            } else if (isGameStarted && !isGameEnded) {
                increaseProgress();
            }
        });

        // Show the instructions dialog
        showInstructionsDialog();

        addBeesToContainer();
    }

    private void startGame() {
        isGameStarted = true;
        isGameEnded = false;
        tapInstructions.setText("Tap the screen to flap!");
        progress = 0;
        progressBar.setProgress(0);
        
        // Restart game music when the game begins
        audioManager.restartGameMusic();
        
        // Start the timer
        if (timer != null) {
            timer.cancel();
        }
        
        timer = new CountDownTimer(TIMER_DURATION, TIMER_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                timerText.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
            }

            @Override
            public void onFinish() {
                if (!isGameEnded) {
                    isGameEnded = true;
                    // Stop game music and play lose sound
                    audioManager.stopGameMusic();
                    audioManager.playLoseSound();
                    showResultDialog(false);
                }
            }
        }.start();

        // Start bee movement
        startBeeMovement();
    }

    private void increaseProgress() {
        if (isGameEnded) return;
        // Speed boost for a short duration
        isFast = true;
        if (speedResetRunnable != null) speedHandler.removeCallbacks(speedResetRunnable);
        speedResetRunnable = () -> isFast = false;
        speedHandler.postDelayed(speedResetRunnable, SPEED_BOOST_DURATION);
        progress += 2; // Increase progress by 2 each time
        if (progress > MAX_PROGRESS) {
            progress = MAX_PROGRESS;
        }
        progressBar.setProgress(progress);
        
        // Update beehive color at checkpoints
        if (progress == 25 || progress == 50 || progress == 75 || progress >= MAX_PROGRESS) {
            updateBeehiveColor(progress);
        }

        if (progress >= MAX_PROGRESS && !isGameEnded) {
            isGameEnded = true;
            if (timer != null) {
                timer.cancel();
            }
            // Stop game music and play win sound
            audioManager.stopGameMusic();
            audioManager.playWinSound();
            showResultDialog(true);
        }
    }

    private void updateBeehiveColor(int progress) {
        if (originalBeehive == null || beehiveMat == null) return;

        // Create a working copy of the original image
        Mat workingMat = beehiveMat.clone();
        
        // Split the channels (BGRA for PNG with alpha)
        List<Mat> channels = new ArrayList<>();
        Core.split(workingMat, channels);
        
        // Get alpha channel (3rd index in BGRA)
        Mat alpha = channels.get(3);
        
        // Create mask from alpha channel
        Mat mask = new Mat();
        Core.compare(alpha, new Scalar(0), mask, Core.CMP_GT);

        // Set golden color and intensity based on progress
        double alpha_contrast;
        double beta_brightness;
        double goldIntensity;
        
        if (progress >= MAX_PROGRESS) {
            // Full golden effect at 100% (previous 50% effect)
            alpha_contrast = 1.05;
            beta_brightness = 7;
            goldIntensity = 0.075;
        } else if (progress >= 75) {
            // 75% checkpoint
            alpha_contrast = 1.037;
            beta_brightness = 6;
            goldIntensity = 0.06;
        } else if (progress >= 50) {
            // Halfway point
            alpha_contrast = 1.025;
            beta_brightness = 4;
            goldIntensity = 0.04;
        } else if (progress >= 25) {
            // Quarter way
            alpha_contrast = 1.012;
            beta_brightness = 2;
            goldIntensity = 0.02;
        } else {
            // No effect below 25%
            workingMat.release();
            mask.release();
            alpha.release();
            for (Mat channel : channels) {
                channel.release();
            }
            return;
        }

        // Apply contrast and brightness
        Mat adjustedMat = new Mat();
        Core.convertScaleAbs(workingMat, adjustedMat, alpha_contrast, beta_brightness);
        
        // Copy only the adjusted pixels where mask is true
        adjustedMat.copyTo(workingMat, mask);

        // Create golden overlay and temporary mat for blending
        Mat goldOverlay = new Mat(workingMat.size(), workingMat.type(), new Scalar(0, 215, 255, 255));
        Mat tempMat = new Mat();
        
        // Apply golden tint
        Core.addWeighted(workingMat, 1.0, goldOverlay, goldIntensity, 0, tempMat);
        
        // Copy only the golden-tinted pixels where mask is true
        tempMat.copyTo(workingMat, mask);

        // Ensure alpha channel is preserved
        List<Mat> resultChannels = new ArrayList<>();
        Core.split(workingMat, resultChannels);
        resultChannels.set(3, alpha); // Restore original alpha channel
        Core.merge(resultChannels, workingMat);

        // Convert back to Bitmap
        Bitmap resultBitmap = Bitmap.createBitmap(originalBeehive.getWidth(), 
                                                originalBeehive.getHeight(), 
                                                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(workingMat, resultBitmap);

        // Update ImageView on UI thread
        runOnUiThread(() -> beehiveImage.setImageBitmap(resultBitmap));

        // Clean up
        workingMat.release();
        adjustedMat.release();
        goldOverlay.release();
        tempMat.release();
        mask.release();
        alpha.release();
        for (Mat channel : channels) {
            channel.release();
        }
        for (Mat channel : resultChannels) {
            channel.release();
        }
    }

    private void showInstructionsDialog() {
        if (isDialogShowing) return;
        isDialogShowing = true;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.level3_custom_dialog);
        dialog.setCancelable(false);

        TextView instructionsText = dialog.findViewById(R.id.instructionsText);
        instructionsText.setText("Welcome to Lesson 3: Making Honey!\n\n" +
                               "• Tap the screen repeatedly to fan your wings\n" +
                               "• Keep tapping to remove water from nectar\n" +
                               "• Watch the beehive change as honey forms\n" +
                               "• Complete the process in 15 seconds");

        Button closeButton = dialog.findViewById(R.id.closeButton);
        closeButton.setText("Let's Begin!");
        closeButton.setOnClickListener(v -> {
            dialog.dismiss();
            isDialogShowing = false;
        });

        dialog.setOnDismissListener(d -> isDialogShowing = false);
        dialog.show();
    }

    private void showResultDialog(boolean isWin) {
        if (isDialogShowing) return;
        isDialogShowing = true;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.level3_result_dialog);
        dialog.setCancelable(false);

        // NOTE: Sounds are played *before* this dialog is shown.

        TextView resultText = dialog.findViewById(R.id.resultText);
        Button okButton = dialog.findViewById(R.id.okButton);

        if (isWin) {
            resultText.setText("Amazing job! 🍯\n\n" +
                             "You've helped turn nectar into honey!");
            okButton.setText("Continue");
            okButton.setOnClickListener(v -> {
                dialog.dismiss();
                isDialogShowing = false;
                showCompletionDialog();
            });
        } else {
            resultText.setText("Time's up!\n\n" +
                             "The nectar needs more fanning to become honey.\n" +
                             "Let's try again!");
            okButton.setText("Try Again");
            okButton.setOnClickListener(v -> {
                dialog.dismiss();
                isDialogShowing = false;
                resetGame();
            });
        }

        dialog.setOnDismissListener(d -> isDialogShowing = false);
        dialog.show();
    }

    private void showCompletionDialog() {
        if (isDialogShowing) return;
        isDialogShowing = true;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.level3_result_dialog);
        dialog.setCancelable(false);

        TextView resultText = dialog.findViewById(R.id.resultText);
        Button okButton = dialog.findViewById(R.id.okButton);

        resultText.setText("Congratulations! 🎉🐝\n\n" +
                         "You've mastered all three steps of honey making:\n" +
                         "1. Finding flowers and collecting nectar\n" +
                         "2. Breaking down nectar with enzymes\n" +
                         "3. Turning nectar into honey\n\n" +
                         "Want to see video explanation of the process?\n" +
                         "Watch the video in the main menu!");

        okButton.setText("Back to Menu");
        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            isDialogShowing = false;
            finish();
        });

        dialog.setOnDismissListener(d -> isDialogShowing = false);
        dialog.show();
    }

    private void resetGame() {
        isGameStarted = false;
        isGameEnded = false;
        tapInstructions.setText("Tap the screen when you are ready!");
        progress = 0;
        progressBar.setProgress(0);
        
        // Reset beehive image to original state
        if (beehiveImage != null && originalBeehive != null) {
            beehiveImage.setImageBitmap(originalBeehive.copy(originalBeehive.getConfig(), true));
        }

        // Reset timer text
        timerText.setText("00:15");

        // Cancel any existing timer
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        // Remove all bees and add again
        beeContainer.removeViews(1, beeContainer.getChildCount() - 1); // keep beehive at index 0
        addBeesToContainer();
        stopBeeMovement();
    }

    private void addBeesToContainer() {
        beeImages.clear();
        beeUpStates.clear();
        bees.clear();
        beeContainer.post(() -> {
            int width = beeContainer.getWidth();
            int height = beeContainer.getHeight();
            int beehiveWidth = beehiveImage.getWidth();
            int beehiveHeight = beehiveImage.getHeight();
            int beeSize = 80; // px, same as layout
            for (int i = 0; i < BEE_COUNT; i++) {
                ImageView bee = new ImageView(this);
                bee.setImageResource(R.drawable.bee);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(beeSize, beeSize);
                // Randomize position, avoid beehive center
                int minX = 0;
                int maxX = width - beeSize;
                int minY = 0;
                int maxY = height - beeSize;
                int beehiveCenterX = width / 2;
                int beehiveCenterY = height / 2;
                int safeRadius = Math.max(beehiveWidth, beehiveHeight);
                int x, y;
                int attempts = 0;
                do {
                    x = random.nextInt(Math.max(1, maxX - minX + 1)) + minX;
                    y = random.nextInt(Math.max(1, maxY - minY + 1)) + minY;
                    attempts++;
                } while (Math.hypot(x + beeSize/2 - beehiveCenterX, y + beeSize/2 - beehiveCenterY) < safeRadius && attempts < 20);
                params.leftMargin = x;
                params.topMargin = y;
                bee.setLayoutParams(params);
                beeContainer.addView(bee);
                beeImages.add(bee);
                beeUpStates.add(false);
                // Set up Bee object
                float angle = (float) (random.nextFloat() * 2 * Math.PI);
                PointF dir = new PointF((float)Math.cos(angle), (float)Math.sin(angle));
                PointF pos = new PointF(x, y);
                bees.add(new Bee(bee, pos, dir, SLOW_SPEED));
            }
        });
    }

    private void startBeeMovement() {
        stopBeeMovement();
        isBeeMoving = true;
        beeMoveRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isBeeMoving) return;
                moveBeesSmoothly();
                beeMoveHandler.postDelayed(this, BEE_MOVE_INTERVAL);
            }
        };
        beeMoveHandler.post(beeMoveRunnable);
    }

    private void stopBeeMovement() {
        isBeeMoving = false;
        beeMoveHandler.removeCallbacksAndMessages(null);
    }

    private void moveBeesSmoothly() {
        int width = beeContainer.getWidth();
        int height = beeContainer.getHeight();
        int beeSize = 80;
        for (Bee bee : bees) {
            // Set speed
            bee.speed = isFast ? FAST_SPEED : SLOW_SPEED;
            // Move
            bee.position.x += bee.direction.x * bee.speed;
            bee.position.y += bee.direction.y * bee.speed;
            // Bounce off walls
            if (bee.position.x < 0) {
                bee.position.x = 0;
                bee.direction.x *= -1;
            } else if (bee.position.x > width - beeSize) {
                bee.position.x = width - beeSize;
                bee.direction.x *= -1;
            }
            if (bee.position.y < 0) {
                bee.position.y = 0;
                bee.direction.y *= -1;
            } else if (bee.position.y > height - beeSize) {
                bee.position.y = height - beeSize;
                bee.direction.y *= -1;
            }
            // Update view position
            bee.view.setTranslationX(bee.position.x - ((FrameLayout.LayoutParams)bee.view.getLayoutParams()).leftMargin);
            bee.view.setTranslationY(bee.position.y - ((FrameLayout.LayoutParams)bee.view.getLayoutParams()).topMargin);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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
        if (isGameStarted && !isGameEnded) {
            audioManager.restartGameMusic();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (beehiveMat != null) {
            beehiveMat.release();
        }
        stopBeeMovement();
    }
} 