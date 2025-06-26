package com.example.honeybee;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

public class GameAudioManager {
    private static GameAudioManager instance;
    private MediaPlayer backgroundMusic;
    private MediaPlayer gameMusic;
    private SoundPool soundPool;
    private int winSoundId, loseSoundId, interactSoundId;
    private boolean soundsLoaded = false;
    private Context context;
    private boolean isBackgroundMusicEnabled = true;
    private boolean isGameMusicEnabled = true;
    private float backgroundVolume = 0.5f;
    private float gameVolume = 0.7f;
    private boolean isSfxEnabled = true;

    private Handler fadeHandler = new Handler(Looper.getMainLooper());
    private Runnable fadeRunnable;

    private GameAudioManager(Context context) {
        this.context = context.getApplicationContext();
        setupSoundPool();
    }

    private void setupSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5) // Allow up to 5 overlapping interaction sounds
                .setAudioAttributes(audioAttributes)
                .build();

        winSoundId = soundPool.load(context, R.raw.winaudio, 1);
        loseSoundId = soundPool.load(context, R.raw.loseaudio, 1);
        interactSoundId = soundPool.load(context, R.raw.interactaudio, 1);

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) {
                // Check if all sounds are loaded
                if (winSoundId != 0 && loseSoundId != 0 && interactSoundId != 0) {
                    soundsLoaded = true;
                }
            } else {
                Log.e("GameAudioManager", "Error loading sound, status: " + status);
            }
        });
    }

    public static GameAudioManager getInstance(Context context) {
        if (instance == null) {
            instance = new GameAudioManager(context);
        }
        return instance;
    }

    public void startBackgroundMusic() {
        if (!isBackgroundMusicEnabled) return;
        
        try {
            if (backgroundMusic == null) {
                backgroundMusic = MediaPlayer.create(context, R.raw.bgaudio);
                backgroundMusic.setLooping(true);
                backgroundMusic.setVolume(backgroundVolume, backgroundVolume);
            }
            
            if (!backgroundMusic.isPlaying()) {
                backgroundMusic.start();
            }
        } catch (Exception e) {
            Log.e("GameAudioManager", "Error starting background music: " + e.getMessage());
        }
    }

    public void stopBackgroundMusic() {
        if (fadeRunnable != null) {
            fadeHandler.removeCallbacks(fadeRunnable);
            fadeRunnable = null;
        }
        try {
            if (backgroundMusic != null && backgroundMusic.isPlaying()) {
                backgroundMusic.pause();
            }
        } catch (Exception e) {
            Log.e("GameAudioManager", "Error stopping background music: " + e.getMessage());
        }
    }

    public void startGameMusic() {
        if (!isGameMusicEnabled) return;
        
        try {
            // Stop background music when game music starts
            stopBackgroundMusic();
            
            if (gameMusic == null) {
                gameMusic = MediaPlayer.create(context, R.raw.gameaudio);
                gameMusic.setLooping(true);
                gameMusic.setVolume(gameVolume, gameVolume);
            }
            
            if (!gameMusic.isPlaying()) {
                gameMusic.start();
            }
        } catch (Exception e) {
            Log.e("GameAudioManager", "Error starting game music: " + e.getMessage());
        }
    }

    public void restartGameMusic() {
        if (!isGameMusicEnabled) return;
        
        try {
            // Stop background music when game music starts
            stopBackgroundMusic();
            
            // Release existing game music to restart from beginning
            if (gameMusic != null) {
                gameMusic.release();
                gameMusic = null;
            }
            
            // Create new game music instance
            gameMusic = MediaPlayer.create(context, R.raw.gameaudio);
            gameMusic.setLooping(true);
            gameMusic.setVolume(gameVolume, gameVolume);
            gameMusic.start();
        } catch (Exception e) {
            Log.e("GameAudioManager", "Error restarting game music: " + e.getMessage());
        }
    }

    public void stopGameMusic() {
        try {
            if (gameMusic != null && gameMusic.isPlaying()) {
                gameMusic.pause();
            }
        } catch (Exception e) {
            Log.e("GameAudioManager", "Error stopping game music: " + e.getMessage());
        }
    }

    public void resumeBackgroundMusic() {
        if ((gameMusic != null && gameMusic.isPlaying()) || !isBackgroundMusicEnabled) {
            return;
        }
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            return;
        }

        if (fadeRunnable != null) {
            fadeHandler.removeCallbacks(fadeRunnable);
        }

        if (backgroundMusic == null) {
            backgroundMusic = MediaPlayer.create(context, R.raw.bgaudio);
            if (backgroundMusic == null) {
                return;
            }
            backgroundMusic.setLooping(true);
        }

        backgroundMusic.setVolume(0f, 0f);
        try {
            backgroundMusic.start();
        } catch (IllegalStateException e) {
            backgroundMusic.release();
            backgroundMusic = MediaPlayer.create(context, R.raw.bgaudio);
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(0f, 0f);
            backgroundMusic.start();
        }

        final float FADE_DURATION = 2000;
        final int FADE_INTERVAL = 100;
        final int FADE_STEPS = (int) (FADE_DURATION / FADE_INTERVAL);
        final float targetVolume = this.backgroundVolume;
        final float deltaVolume = targetVolume / FADE_STEPS;
        final float[] currentVolume = {0f};

        fadeRunnable = new Runnable() {
            @Override
            public void run() {
                if (backgroundMusic == null) return;
                currentVolume[0] += deltaVolume;
                if (currentVolume[0] > targetVolume) {
                    currentVolume[0] = targetVolume;
                }
                
                try {
                    backgroundMusic.setVolume(currentVolume[0], currentVolume[0]);
                } catch (Exception e) {
                    fadeHandler.removeCallbacks(this);
                    return;
                }

                if (currentVolume[0] < targetVolume) {
                    fadeHandler.postDelayed(this, FADE_INTERVAL);
                } else {
                    fadeRunnable = null;
                }
            }
        };
        fadeHandler.post(fadeRunnable);
    }

    public void setBackgroundMusicEnabled(boolean enabled) {
        this.isBackgroundMusicEnabled = enabled;
        if (!enabled) {
            stopBackgroundMusic();
        } else {
            startBackgroundMusic();
        }
    }

    public void setGameMusicEnabled(boolean enabled) {
        this.isGameMusicEnabled = enabled;
        if (!enabled) {
            stopGameMusic();
        }
    }

    public void setBackgroundVolume(float volume) {
        this.backgroundVolume = volume;
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(volume, volume);
        }
    }

    public void setGameVolume(float volume) {
        this.gameVolume = volume;
        if (gameMusic != null) {
            gameMusic.setVolume(volume, volume);
        }
    }

    public boolean isBackgroundMusicEnabled() {
        return isBackgroundMusicEnabled;
    }

    public boolean isGameMusicEnabled() {
        return isGameMusicEnabled;
    }

    public float getBackgroundVolume() {
        return backgroundVolume;
    }

    public float getGameVolume() {
        return gameVolume;
    }

    public void playWinSound() {
        if (soundsLoaded && isSfxEnabled) {
            soundPool.play(winSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    public void playLoseSound() {
        if (soundsLoaded && isSfxEnabled) {
            soundPool.play(loseSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    public void playInteractSound() {
        if (soundsLoaded && isSfxEnabled) {
            soundPool.play(interactSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    public void release() {
        try {
            if (backgroundMusic != null) {
                backgroundMusic.release();
                backgroundMusic = null;
            }
            if (gameMusic != null) {
                gameMusic.release();
                gameMusic = null;
            }
            if (soundPool != null) {
                soundPool.release();
                soundPool = null;
            }
            if (fadeRunnable != null) {
                fadeHandler.removeCallbacks(fadeRunnable);
            }
        } catch (Exception e) {
            Log.e("GameAudioManager", "Error releasing audio resources: " + e.getMessage());
        }
    }
} 