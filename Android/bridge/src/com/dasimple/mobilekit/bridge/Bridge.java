package com.dasimple.mobilekit.bridge;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings.Secure;
import android.text.TextUtils;

import com.unity3d.player.UnityPlayer;

public class Bridge
{
	public static void log(String message)
	{
		//Below row can be commented.
		//android.util.Log.d("EXPlugin", message);
	}

	public static Activity getCurrentActivity()
	{
		return UnityPlayer.currentActivity;
	}

	public static Context getApplicationContext()
	{
		return getCurrentActivity().getApplicationContext();
	}
	
	public static String getPackageName()
	{
		return getCurrentActivity().getPackageName();
	}
	
	public static String getDeviceId(Context context)
	{
		String deviceId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		if(TextUtils.isEmpty(deviceId))
		{
			deviceId = "RANDOM_DEVICE_ID";
		}
		return deviceId;
	}
	
	public static void sendEvent(String listener, String type, String message)
	{
		UnityPlayer.UnitySendMessage(listener + "Events", "Event" + type, message);
	}
}
