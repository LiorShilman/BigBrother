package com.example.ariellior.bigbrother;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int CONFIGURATION_CODE_PREFERENCES = 1;
    private static final String PREF_DARK_MODE = "dark_mode_preference";

    private TextView txtStatus;
    private TextView txtLatitude;
    private TextView txtLongitude;
    private TextView txtLastUpdate;
    private TextView txtStreet;
    private TextView txtAccuracy;
    private View statusDot;
    private MaterialButton btnStart;
    private MaterialButton btnStop;
    private MaterialSwitch switchDarkMode;
    private MaterialCardView heroCard;
    private MaterialCardView statusCard;

    private boolean isServiceRunning = false;

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra(Constants.BROADCAST.EXTRA_LAT, 0);
            double lng = intent.getDoubleExtra(Constants.BROADCAST.EXTRA_LNG, 0);
            String time = intent.getStringExtra(Constants.BROADCAST.EXTRA_TIME);
            double battery = intent.getDoubleExtra(Constants.BROADCAST.EXTRA_BATTERY, 0);
            String street = intent.getStringExtra(Constants.BROADCAST.EXTRA_STREET);
            float accuracy = intent.getFloatExtra(Constants.BROADCAST.EXTRA_ACCURACY, 0);
            updateLocationUI(lat, lng, time, battery, street, accuracy);
        }
    };

    private final BroadcastReceiver connectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra(Constants.BROADCAST.EXTRA_STATUS);
            updateConnectionStatus(status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Splash screen
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // Apply saved dark mode preference
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = prefs.getBoolean(PREF_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        setContentView(R.layout.activity_main);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        // Bind views
        txtStatus = findViewById(R.id.txt_status);
        txtLatitude = findViewById(R.id.txt_latitude);
        txtLongitude = findViewById(R.id.txt_longitude);
        txtLastUpdate = findViewById(R.id.txt_last_update);
        txtStreet = findViewById(R.id.txt_street);
        txtAccuracy = findViewById(R.id.txt_accuracy);
        statusDot = findViewById(R.id.status_dot);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        heroCard = findViewById(R.id.hero_card);
        statusCard = findViewById(R.id.status_card);

        // Dark mode switch
        switchDarkMode.setChecked(isDarkMode);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PREF_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Start button
        btnStart.setOnClickListener(v -> {
            startTrackingService();
            animateStatusChange(true);
        });

        // Stop button
        btnStop.setOnClickListener(v -> {
            stopTrackingService();
            animateStatusChange(false);
        });

        // Settings click
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            Intent launchPreferencesIntent = new Intent().setClass(this,
                    BigBrotherPreferenceActivity.class);
            startActivityForResult(launchPreferencesIntent, CONFIGURATION_CODE_PREFERENCES);
        });

        requestRequiredPermissions();

        // Entrance animations
        playEntranceAnimations();
    }

    private void startTrackingService() {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        sharedPref.edit().putBoolean("service_preference", true).commit();

        Intent serviceIntent = new Intent(getApplicationContext(), GPSLocationSrv.class);
        serviceIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        isServiceRunning = true;
    }

    private void stopTrackingService() {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        sharedPref.edit().putBoolean("service_preference", false).commit();

        Intent serviceIntent = new Intent(getApplicationContext(), GPSLocationSrv.class);
        serviceIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        startService(serviceIntent);

        isServiceRunning = false;

        txtStatus.setText(R.string.status_stopped);
        txtLatitude.setText("--");
        txtLongitude.setText("--");
        txtStreet.setText("--");
        txtAccuracy.setText("--");
        txtLastUpdate.setText(R.string.label_last_update);
    }

    private void animateStatusChange(boolean running) {
        int dotColor = running ?
                ContextCompat.getColor(this, R.color.status_active) :
                ContextCompat.getColor(this, R.color.status_stopped);

        GradientDrawable dotDrawable = (GradientDrawable) statusDot.getBackground();
        dotDrawable.setColor(dotColor);

        txtStatus.setText(running ? R.string.status_running : R.string.status_stopped);

        // Pulse animation on the status dot
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(statusDot, "scaleX", 1f, 1.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(statusDot, "scaleY", 1f, 1.5f, 1f);
        AnimatorSet pulse = new AnimatorSet();
        pulse.playTogether(scaleX, scaleY);
        pulse.setDuration(400);
        pulse.setInterpolator(new OvershootInterpolator());
        pulse.start();

        // Button state
        btnStart.setEnabled(!running);
        btnStop.setEnabled(running);
        btnStart.setAlpha(running ? 0.5f : 1f);
        btnStop.setAlpha(running ? 1f : 0.5f);
    }

    private void playEntranceAnimations() {
        // Hero card slides in from top
        heroCard.setTranslationY(-100f);
        heroCard.setAlpha(0f);
        heroCard.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .start();

        // Status card fades in with delay
        statusCard.setAlpha(0f);
        statusCard.setTranslationY(50f);
        statusCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(200)
                .setDuration(500)
                .start();

        // Buttons slide in from bottom
        btnStart.setTranslationY(80f);
        btnStart.setAlpha(0f);
        btnStart.animate()
                .translationY(0f)
                .alpha(1f)
                .setStartDelay(350)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(0.5f))
                .start();

        btnStop.setTranslationY(80f);
        btnStop.setAlpha(0f);
        btnStop.animate()
                .translationY(0f)
                .alpha(1f)
                .setStartDelay(400)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(0.5f))
                .start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter locationFilter = new IntentFilter(Constants.BROADCAST.LOCATION_UPDATE);
        IntentFilter connectionFilter = new IntentFilter(Constants.BROADCAST.CONNECTION_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationReceiver, locationFilter, Context.RECEIVER_NOT_EXPORTED);
            registerReceiver(connectionReceiver, connectionFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(locationReceiver, locationFilter);
            registerReceiver(connectionReceiver, connectionFilter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(locationReceiver);
            unregisterReceiver(connectionReceiver);
        } catch (Exception ignored) {
        }
    }

    public void updateLocationUI(double lat, double lng, String time, double battery, String street, float accuracy) {
        runOnUiThread(() -> {
            txtLatitude.setText(String.format("%.6f", lat));
            txtLongitude.setText(String.format("%.6f", lng));
            txtLastUpdate.setText("Last update: " + time);
            txtStreet.setText(street != null && !street.isEmpty() ? street : "--");
            txtAccuracy.setText(String.format("%.0f m", accuracy));

            com.google.android.material.chip.Chip chipBattery = findViewById(R.id.chip_battery);
            chipBattery.setText(String.format("%.0f%%", battery));
        });
    }

    private void updateConnectionStatus(String status) {
        runOnUiThread(() -> {
            txtStatus.setText(status);

            int dotColor;
            if ("Connected".equals(status)) {
                dotColor = ContextCompat.getColor(this, R.color.status_active);
            } else if (status != null && status.startsWith("Error")) {
                dotColor = ContextCompat.getColor(this, R.color.status_stopped);
            } else if ("Connecting...".equals(status)) {
                dotColor = ContextCompat.getColor(this, R.color.status_connecting);
            } else {
                dotColor = ContextCompat.getColor(this, R.color.status_stopped);
            }

            GradientDrawable dotDrawable = (GradientDrawable) statusDot.getBackground();
            dotDrawable.setColor(dotColor);

            // Pulse animation
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(statusDot, "scaleX", 1f, 1.3f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(statusDot, "scaleY", 1f, 1.3f, 1f);
            AnimatorSet pulse = new AnimatorSet();
            pulse.playTogether(scaleX, scaleY);
            pulse.setDuration(300);
            pulse.start();
        });
    }

    private void requestRequiredPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    Constants.PERMISSION.REQUEST_ID_MULTIPLE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.PERMISSION.REQUEST_ID_MULTIPLE_PERMISSIONS) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", permissions[i] + " granted");
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            Constants.PERMISSION.REQUEST_ID_MULTIPLE_PERMISSIONS + 1);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.big_brother_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.big_brother_settings) {
            Intent launchPreferencesIntent = new Intent().setClass(this,
                    BigBrotherPreferenceActivity.class);
            startActivityForResult(launchPreferencesIntent, CONFIGURATION_CODE_PREFERENCES);
        } else if (item.getItemId() == R.id.big_brother_start) {
            startTrackingService();
            animateStatusChange(true);
        } else {
            finish();
        }
        return false;
    }
}
