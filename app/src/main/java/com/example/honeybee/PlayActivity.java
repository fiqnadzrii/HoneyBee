package com.example.honeybee;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class PlayActivity extends AppCompatActivity {
    private WebView webView;
    private GameAudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        // Initialize audio manager
        audioManager = GameAudioManager.getInstance(this);

        webView = findViewById(R.id.webView);

        // Enable JavaScript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Stay in WebView when navigating
        webView.setWebViewClient(new WebViewClient());

        // Load the YouTube video (embedded format)
        String videoEmbed = "<iframe width=\"100%\" height=\"100%\" " +
                "src=\"https://www.youtube.com/embed/KPKg43uUUtA\" " +
                "frameborder=\"0\" allowfullscreen></iframe>";

        webView.loadData(videoEmbed, "text/html", "utf-8");

        // Stop background music immediately when video activity starts
        audioManager.stopBackgroundMusic();

        // Enable ActionBar back button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Ensure background music is stopped when video is playing
        audioManager.stopBackgroundMusic();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Don't resume background music here since video is still playing
        // Background music will be resumed when returning to main activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // The previous activity's onResume will handle audio resumption.
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        // Allow WebView to go back in history
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // The previous activity's onResume will handle audio resumption.
            super.onBackPressed();
        }
    }
}
