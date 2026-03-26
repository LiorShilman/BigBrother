package com.example.ariellior.bigbrother;

/**
 * Created by ArielLior on 21/11/2016.
 */

public class Constants {
    public interface ACTION {
        public static final String STARTFOREGROUND_ACTION               = "com.example.ariellior.bigbrother.START_FOREGROUND";
        public static final String PAUSEFOREGROUND_ACTION               = "com.example.ariellior.bigbrother.PAUSE_FOREGROUND";
        public static final String RESTARTFOREGROUND_ACTION             = "com.example.ariellior.bigbrother.RESTART_FOREGROUND";
        public static final String STOPFOREGROUND_ACTION                = "com.example.ariellior.bigbrother.STOP_FOREGROUND";
        public static final String SENDMARKER_ACTION                    = "com.example.ariellior.bigbrother.SEND_MARKER";
        public static final String SOS_ACTION                           = "com.example.ariellior.bigbrother.SOS";

        public static final String START_ACTION                         = "com.example.ariellior.bigbrother.START";
        public static final String PAUSE_ACTION                         = "com.example.ariellior.bigbrother.PAUSE";
    }

    public interface NOTIFICATION_ID {
        public static final int     FOREGROUND_SERVICE                  = 101;
    }

    public interface SIGNALR {
        // Server base URL (change to your server IP/hostname)
        public static final String  SIGNALR_HUB_SERVICE                 = "http://shilmanlior2608.ddns.net:26500/";
        // Full SignalR hub URL
        public static final String  SIGNALR_HUB_URL                     = SIGNALR_HUB_SERVICE + "bigbrotherhub";
        // REST API base URL
        public static final String  REST_API_BASE                       = SIGNALR_HUB_SERVICE + "api/";
        public static final String  SENDMARKER_ALARM_ACTION             = "com.example.ariellior.bigbrother.SEND_MARKER_ALARM";
        public static final int     ALARM_REQUEST_CODE                  = 201;
    }

    public interface BROADCAST {
        public static final String  LOCATION_UPDATE                     = "com.example.ariellior.bigbrother.LOCATION_UPDATE";
        public static final String  CONNECTION_STATUS                   = "com.example.ariellior.bigbrother.CONNECTION_STATUS";
        public static final String  EXTRA_LAT                           = "lat";
        public static final String  EXTRA_LNG                           = "lng";
        public static final String  EXTRA_TIME                          = "time";
        public static final String  EXTRA_BATTERY                       = "battery";
        public static final String  EXTRA_STATUS                        = "status";
        public static final String  EXTRA_STREET                        = "street";
        public static final String  EXTRA_ACCURACY                      = "accuracy";
    }

    public interface GOOGLE {
        public static final String  API_KEY                             = "AIzaSyCnBoA9j9HgG5aG4gi-vTZwZTnXhr1VsGk";
    }

    public interface PERMISSION {
        public static  final int    REQUEST_ID_MULTIPLE_PERMISSIONS     = 101;
    }

    public interface GPS {
        public static final long    SLOW_INTERVAL                       = 1000 * 60;
        public static final long    INTERVAL                            = 1000 * 30;
        public static final long    FASTEST_INTERVAL                    = 1000 * 15;
        public static final float   SMALL_DISPLACEMENT                  = 5f;
    }
}
