package com.example.ariellior.bigbrother;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

public class PowerUtil 
{
	public static boolean isBatteryPlugged(Context context) 
	{
		boolean isPlugged	= false;
        Intent intent 		= context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int plugged 		= intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
       	isPlugged 		= (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB);
    	if (VERSION.SDK_INT > VERSION_CODES.JELLY_BEAN) 
		{
			isPlugged = isPlugged || (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS);
		}
		return isPlugged;
	}
	
	public static double getBatteryLevel(Context context) 
	{
        Intent intent 		= context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level 			= intent.getIntExtra(BatteryManager.EXTRA_LEVEL , -1);
        int scale 			= intent.getIntExtra(BatteryManager.EXTRA_SCALE , -1);
        
        return (double)level * (double)100 / (double)scale;
	}
}