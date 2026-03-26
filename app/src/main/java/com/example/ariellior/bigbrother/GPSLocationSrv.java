package com.example.ariellior.bigbrother;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import static com.example.ariellior.bigbrother.Constants.ACTION.PAUSE_ACTION;
import static com.example.ariellior.bigbrother.Constants.ACTION.START_ACTION;

public class GPSLocationSrv extends Service {

    public static final String CHANNEL_ID = "big_brother_foreground";
    public static final String CHANNEL_NOTIFICATION_ID = "big_brother_notifications";

    private static final String TAG = "GPSLocationSrv";

    private MainActivity myMainActivity;

    private LocationManager mLocationManager;
    private LocationRequest mLocationRequest;
    private GPSLocationListener mLocationListener;
    public NotificationCompat.Builder mNotification;
    private PendingIntent mPendingIntentStart;
    private PendingIntent mPendingIntentPause;

    private LocationCallback mLocationCallback;

    // SignalR (ASP.NET Core)
    private HubConnection mHubConnection;
    private String mName = "";

    private Intent mStartReceive;
    private Intent mPauseReceive;
    private Bitmap mIcon;

    public GPSLocationSrv() {
    }

    public void setUICallback(MainActivity activity) {
        myMainActivity = activity;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        NotificationChannel foregroundChannel = new NotificationChannel(
                CHANNEL_ID,
                "Big Brother Location Service",
                NotificationManager.IMPORTANCE_LOW
        );
        foregroundChannel.setDescription("Shows when Big Brother is tracking your location");

        NotificationChannel alertChannel = new NotificationChannel(
                CHANNEL_NOTIFICATION_ID,
                "Big Brother Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        alertChannel.setDescription("Alert notifications from Big Brother");
        alertChannel.enableVibration(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(foregroundChannel);
        manager.createNotificationChannel(alertChannel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_REDELIVER_INTENT;
        }

        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(TAG, "Received start id " + startId + ": " + intent);
            try {
                if (!isGooglePlayServicesAvailable()) {
                    Log.w(TAG, "Google Play Services unavailable");
                }

                int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

                mStartReceive = new Intent(this, GPSBroadcastReceiverOnBootComplete.class);
                mStartReceive.setAction(START_ACTION);

                mPauseReceive = new Intent(this, GPSBroadcastReceiverOnBootComplete.class);
                mPauseReceive.setAction(PAUSE_ACTION);

                mPendingIntentPause = PendingIntent.getBroadcast(this, 12345, mPauseReceive, pendingFlags);
                mPendingIntentStart = PendingIntent.getBroadcast(this, 12346, mStartReceive, pendingFlags);
                mIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_gps);

                mNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Big Brother")
                        .setContentText("Search...")
                        .setSmallIcon(R.drawable.ic_launcher_gps)
                        .setLargeIcon(Bitmap.createScaledBitmap(mIcon, 128, 128, false))
                        .setOngoing(true)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setWhen(0)
                        .addAction(R.drawable.ic_launcher_pause, "Pause", mPendingIntentPause)
                        .addAction(R.drawable.ic_launcher_start, "Start", mPendingIntentStart);

                mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

                createLocationRequest();

                mLocationListener = new GPSLocationListener(this, mNotification);

                mLocationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        mLocationListener.onLocationChanged(locationResult.getLastLocation());
                    }
                };

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                            mNotification.build(),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
                } else {
                    startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, mNotification.build());
                }

                startLocationUpdates();

                SharedPreferences sharedPref = PreferenceManager
                        .getDefaultSharedPreferences(this);

                mName = sharedPref.getString("name_preference", "");

                startSignalR();

            } catch (Exception e) {
                Log.e(TAG, "Error starting service.", e);
            }
        } else if (intent.getAction().equals(Constants.ACTION.RESTARTFOREGROUND_ACTION)) {
            try {
                if (isGooglePlayServicesAvailable()) {
                    mLocationListener.restart();
                    startLocationUpdates();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                                mNotification.build(),
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
                    } else {
                        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, mNotification.build());
                    }
                    startSignalR();
                } else {
                    Log.w(TAG, "Google Play Services unavailable");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error restarting service.", e);
            }
        } else if (intent.getAction().equals(Constants.ACTION.PAUSEFOREGROUND_ACTION)) {
            try {
                Log.i(TAG, "Received Pause Foreground Intent");
                mLocationListener.pause();
                stopLocationUpdate();
                stopSignalR();
            } catch (Exception ex) {
                Log.e(TAG, "PAUSEFOREGROUND_ACTION.", ex);
            }
        } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            try {
                Log.i(TAG, "Received Stop Foreground Intent");
                stopLocationUpdate();
                mLocationListener.stop();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                stopSignalR();
            } catch (Exception ex) {
                Log.e(TAG, "STOP_ACTION.", ex);
            }
        } else if (intent.getAction().equals(Constants.ACTION.SENDMARKER_ACTION)) {
            try {
                mLocationListener.SendLocation();
            } catch (Exception ex) {
                Log.e(TAG, "SENDMARKER_ACTION.", ex);
            }
        } else if (intent.getAction().equals(Constants.ACTION.SOS_ACTION)) {
            try {
                sendSOS();
            } catch (Exception ex) {
                Log.e(TAG, "SOS_ACTION.", ex);
            }
        }

        return START_REDELIVER_INTENT;
    }

    private void startSignalR() {
        try {
            mHubConnection = HubConnectionBuilder.create(
                    Constants.SIGNALR.SIGNALR_HUB_URL
            ).build();

            // Handle HeadsUpNotification from server
            mHubConnection.on("HeadsUpNotification", (title, message) -> {
                Log.d(TAG, "HeadsUpNotification: " + title + " - " + message);

                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(GPSLocationSrv.this, CHANNEL_NOTIFICATION_ID)
                                .setContentTitle(title)
                                .setContentText(message)
                                .setDefaults(Notification.DEFAULT_ALL)
                                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                .setSmallIcon(R.drawable.ic_launcher_gps)
                                .setPriority(NotificationCompat.PRIORITY_MAX);

                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(0, builder.build());

                // Vibrate
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                    Vibrator vibrator = vibratorManager.getDefaultVibrator();
                    vibrator.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            }, String.class, String.class);

            // Connection events
            mHubConnection.onClosed(error -> {
                Log.d(TAG, "SignalR DISCONNECTED");
                broadcastConnectionStatus("Disconnected");
                if (error != null) {
                    Log.e(TAG, "SignalR closed with error: " + error.getMessage());
                }
            });

            // Start connection on background thread
            broadcastConnectionStatus("Connecting...");
            new Thread(() -> {
                try {
                    mHubConnection.start().blockingAwait();
                    Log.d(TAG, "SignalR CONNECTED");
                    broadcastConnectionStatus("Connected");

                    // Subscribe to Android group
                    mHubConnection.invoke("Subscribe", mName, "Android");

                    // Setup alarm for periodic marker sending
                    setupAlarm();

                } catch (Exception e) {
                    Log.e(TAG, "SignalR connection failed: " + e.toString());
                    broadcastConnectionStatus("Error: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up SignalR", e);
        }
    }

    private void setupAlarm() {
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        Intent intentAlarmBroadcastReceiver = new Intent(this, AlarmBroadcastReceiver.class);
        intentAlarmBroadcastReceiver.setAction(Constants.SIGNALR.SENDMARKER_ALARM_ACTION);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, Constants.SIGNALR.ALARM_REQUEST_CODE, intentAlarmBroadcastReceiver, pendingFlags);
        alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + Constants.GPS.SLOW_INTERVAL, alarmIntent);
    }

    private void sendSOS() {
        if (mHubConnection != null && mHubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            mHubConnection.invoke("SendSOS", mName);
            Log.d(TAG, "SOS sent for " + mName);
        }
        // Also force send current location
        if (mLocationListener != null) {
            mLocationListener.SendLocation();
        }
    }

    private void stopSignalR() {
        try {
            if (mHubConnection != null && mHubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
                mHubConnection.invoke("Unsubscribe", mName, "Android");
                mHubConnection.stop().blockingAwait();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping SignalR", e);
        }

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent intentAlarmBroadcastReceiver = new Intent(this, AlarmBroadcastReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, Constants.SIGNALR.ALARM_REQUEST_CODE, intentAlarmBroadcastReceiver, pendingFlags);
        alarmMgr.cancel(alarmIntent);
    }

    private void broadcastConnectionStatus(String status) {
        Intent intent = new Intent(Constants.BROADCAST.CONNECTION_STATUS);
        intent.setPackage(getPackageName());
        intent.putExtra(Constants.BROADCAST.EXTRA_STATUS, status);
        sendBroadcast(intent);
    }

    private boolean isGooglePlayServicesAvailable() {
        try {
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            int status = apiAvailability.isGooglePlayServicesAvailable(this);
            return (ConnectionResult.SUCCESS == status);
        } catch (Exception ex) {
            Log.e(TAG, "isGooglePlayServicesAvailable.", ex);
        }
        return false;
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, Constants.GPS.INTERVAL)
                .setMinUpdateIntervalMillis(Constants.GPS.FASTEST_INTERVAL)
                .setMinUpdateDistanceMeters(Constants.GPS.SMALL_DISPLACEMENT)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            LocationServices.getFusedLocationProviderClient(this)
                    .requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    protected void stopLocationUpdate() {
        try {
            LocationServices.getFusedLocationProviderClient(this)
                    .removeLocationUpdates(mLocationCallback);
        } catch (Exception ex) {
            Log.e(TAG, "Error stopping location updates", ex);
        }
    }
}
