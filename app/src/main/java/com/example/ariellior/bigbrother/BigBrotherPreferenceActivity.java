package com.example.ariellior.bigbrother;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;

public class BigBrotherPreferenceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_prefs);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_activity_big_brother_preference);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        try {
            PreferenceManager.setDefaultValues(getApplicationContext(),
                    R.xml.big_brother_init_preference, true);
        } catch (Exception e) {
            // handle exception
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.prefs_container, new BigBrotherPreferenceFragment())
                    .commit();
        }
    }
}
