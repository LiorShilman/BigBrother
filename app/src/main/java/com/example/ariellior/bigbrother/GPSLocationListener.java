package com.example.ariellior.bigbrother;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.LocationListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.NOTIFICATION_SERVICE;

public class GPSLocationListener implements LocationListener {

    private NotificationManager mNM;
    private NotificationCompat.Builder mBuilder;
    private Location mLocation;
    private double mLongitude;
    private double mLatitude;
    private Intent mResultIntent;
    private PendingIntent mResultPendingIntent;
    private Marker mMarker;
    private Marker mLastSendMarker = null;
    private String mName = "";
    private String mTelephone = "";
    private final Context mContext;
    private RestService mRestService;
    private boolean mRunService = false;

    public GPSLocationListener(Context context, NotificationCompat.Builder builder) {
        this.mContext = context;
        this.mRestService = new RestService();

        try {
            mNM = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
            mBuilder = builder;

            mResultIntent = new Intent(mContext, MainActivity.class);

            int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
            }

            mResultPendingIntent = PendingIntent.getActivity(
                    this.mContext,
                    0,
                    mResultIntent,
                    pendingFlags
            );

            builder.setContentIntent(mResultPendingIntent);

        } catch (Exception ex) {
            Log.e("GPSLocationListener", "Error in constructor", ex);
        }
    }

    public void SendLocation() {
        if (mLastSendMarker != null) {
            Log.d("GpsLocationService", "SendLocation from Handler ..............: ");
            SendLocation(mLastSendMarker.Lat, mLastSendMarker.Long, mLastSendMarker.Alt, mLastSendMarker.Accuracy);
        }
    }

    private void SendLocation(Double latitude, Double longitude, Double alt, float accuracy) {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(mContext);

        mName = sharedPref.getString("name_preference", "");
        mTelephone = sharedPref.getString("tel_preference", "");
        mRunService = sharedPref.getBoolean("service_preference", false);

        mMarker = new Marker();
        mMarker.Name = mName;
        mMarker.Telephone = mTelephone;
        mMarker.IsBatteryPlugged = PowerUtil.isBatteryPlugged(mContext);
        mMarker.Battery = PowerUtil.getBatteryLevel(mContext);
        mMarker.Lat = latitude;
        mMarker.Long = longitude;
        mMarker.Alt = alt;
        mMarker.Accuracy = accuracy;

        // Reverse geocoding - get street address
        mMarker.Street = getStreetAddress(latitude, longitude);

        // Street View Static API URLs
        String svBase = "https://maps.googleapis.com/maps/api/streetview?size=300x200"
                + "&location=" + latitude + "," + longitude
                + "&key=" + Constants.GOOGLE.API_KEY;
        mMarker.StreetViewImage0 = svBase + "&heading=0";
        mMarker.StreetViewImage90 = svBase + "&heading=90";
        mMarker.StreetViewImage180 = svBase + "&heading=180";
        mMarker.StreetViewImage_Minus90 = svBase + "&heading=270";

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        mMarker.Time = sdf.format(new Date());

        if (mRunService) {
            try {
                mLastSendMarker = mMarker;

                mRestService.getService().addMarker(mMarker).enqueue(new Callback<Marker>() {
                    @Override
                    public void onResponse(Call<Marker> call, Response<Marker> response) {
                        String streetInfo = mMarker.Street.isEmpty() ?
                                "(" + String.format("%.4f", mMarker.Lat) + ", " + String.format("%.4f", mMarker.Long) + ")" :
                                mMarker.Street;
                        showNotification(R.drawable.ic_launcher_status,
                                streetInfo + " - " + mMarker.Time,
                                "Big Brother (Running) - " + String.format("%.0f", mMarker.Battery) + "%", 101);
                        broadcastLocation(mMarker);
                    }

                    @Override
                    public void onFailure(Call<Marker> call, Throwable t) {
                        if (t != null && t.getMessage() != null)
                            Toast.makeText(mContext, t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception ex) {
                Log.e("GPSLocationListener", "Error sending location", ex);
            }
        } else {
            showNotification(R.drawable.ic_launcher_status,
                    "( " + mMarker.Lat.toString() + "," + mMarker.Long.toString() + " ) - " + mMarker.Time,
                    "Big Brother (Stopped)", 101);
            stop();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            Log.d("GpsLocationService", "SendLocation from onLocationChanged ..............: ");
            SendLocation(location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getAccuracy());
        } catch (Exception ex) {
            showNotification(R.drawable.ic_launcher_status, ex.getMessage() + " --> " + mMarker.Time, "Big Brother", 101);
        }
    }

    private void broadcastLocation(Marker marker) {
        Intent intent = new Intent(Constants.BROADCAST.LOCATION_UPDATE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(Constants.BROADCAST.EXTRA_LAT, marker.Lat);
        intent.putExtra(Constants.BROADCAST.EXTRA_LNG, marker.Long);
        intent.putExtra(Constants.BROADCAST.EXTRA_TIME, marker.Time);
        intent.putExtra(Constants.BROADCAST.EXTRA_BATTERY, marker.Battery);
        intent.putExtra(Constants.BROADCAST.EXTRA_STREET, marker.Street);
        intent.putExtra(Constants.BROADCAST.EXTRA_ACCURACY, marker.Accuracy);
        mContext.sendBroadcast(intent);
    }

    private String getStreetAddress(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                if (address.getThoroughfare() != null) {
                    sb.append(address.getThoroughfare());
                    if (address.getSubThoroughfare() != null) {
                        sb.append(" ").append(address.getSubThoroughfare());
                    }
                }
                if (address.getLocality() != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(address.getLocality());
                }
                return sb.toString();
            }
        } catch (Exception ex) {
            Log.e("GPSLocationListener", "Geocoder error", ex);
        }
        return "";
    }

    public void showNotification(int icon, String message, String label, int id) {
        Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher_gps);
        mBuilder.setLargeIcon(Bitmap.createScaledBitmap(bmp, 128, 128, false));
        mBuilder.setSmallIcon(icon);
        mBuilder.setContentText(message);
        mBuilder.setContentTitle(label);

        mNM.notify(id, mBuilder.build());
    }

    public void cancelNotification(int id) {
        mNM.cancel(id);
    }

    public void restart() {
        showNotification(R.drawable.ic_launcher_status,
                "( " + mMarker.Lat.toString() + "," + mMarker.Long.toString() + " ) - " + mMarker.Time,
                "Big Brother (Running)", 101);
    }

    public void pause() {
        showNotification(R.drawable.ic_launcher_status,
                "( " + mMarker.Lat.toString() + "," + mMarker.Long.toString() + " ) - " + mMarker.Time,
                "Big Brother (Paused)", 101);
    }

    public void stop() {
        mNM.cancelAll();
    }
}
