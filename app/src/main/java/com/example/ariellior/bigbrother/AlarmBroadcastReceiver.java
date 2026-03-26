package com.example.ariellior.bigbrother;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

public class AlarmBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null &&
                intent.getAction().equalsIgnoreCase(Constants.SIGNALR.SENDMARKER_ALARM_ACTION)) {
            Log.d("GPSBroadcastReceiver", "SEND_MARKER_ACTION");
            scheduleAlarm(context);
        }
    }

    public void scheduleAlarm(Context context) {
        AlarmManager mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent serviceIntent = new Intent(context, GPSLocationSrv.class);
        serviceIntent.setAction(Constants.ACTION.SENDMARKER_ACTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        Intent intentAlarmBroadcastReceiver = new Intent(context, AlarmBroadcastReceiver.class);
        intentAlarmBroadcastReceiver.setAction(Constants.SIGNALR.SENDMARKER_ALARM_ACTION);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, Constants.SIGNALR.ALARM_REQUEST_CODE, intentAlarmBroadcastReceiver, pendingFlags);
        mAlarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + Constants.GPS.SLOW_INTERVAL, alarmIntent);
    }

    public void cancelAlarm(Context context) {
        AlarmManager mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent intentGPSBroadcastReceiverOnBoot = new Intent(context, AlarmBroadcastReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, Constants.SIGNALR.ALARM_REQUEST_CODE, intentGPSBroadcastReceiverOnBoot, pendingFlags);
        mAlarmMgr.cancel(alarmIntent);
    }
}
