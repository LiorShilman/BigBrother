package com.example.ariellior.bigbrother;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class GPSBroadcastReceiverOnBootComplete extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) return;

        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences sharedPref = PreferenceManager
                    .getDefaultSharedPreferences(context);

            sharedPref.edit().putBoolean("service_preference", true).apply();

            Intent serviceIntent = new Intent(context, GPSLocationSrv.class);
            serviceIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
            startForegroundServiceCompat(context, serviceIntent);

        } else if (intent.getAction().equalsIgnoreCase(Constants.ACTION.START_ACTION)) {
            SharedPreferences sharedPref = PreferenceManager
                    .getDefaultSharedPreferences(context);

            sharedPref.edit().putBoolean("service_preference", true).apply();

            Intent serviceIntent = new Intent(context, GPSLocationSrv.class);
            serviceIntent.setAction(Constants.ACTION.RESTARTFOREGROUND_ACTION);
            startForegroundServiceCompat(context, serviceIntent);

        } else if (intent.getAction().equalsIgnoreCase(Constants.ACTION.PAUSE_ACTION)) {
            SharedPreferences sharedPref = PreferenceManager
                    .getDefaultSharedPreferences(context);

            sharedPref.edit().putBoolean("service_preference", false).apply();

            Intent serviceIntent = new Intent(context, GPSLocationSrv.class);
            serviceIntent.setAction(Constants.ACTION.PAUSEFOREGROUND_ACTION);
            startForegroundServiceCompat(context, serviceIntent);
        }
    }

    private void startForegroundServiceCompat(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
