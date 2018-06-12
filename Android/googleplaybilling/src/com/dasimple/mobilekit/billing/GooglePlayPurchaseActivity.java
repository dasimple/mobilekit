package com.dasimple.mobilekit.billing;

import com.dasimple.mobilekit.bridge.Bridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public final class GooglePlayPurchaseActivity extends Activity
{
	private boolean resultTriggered = false;
	
	/**
	 * Android activity was succesfully created.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Bridge.log("Activity for purchasing is created.");
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		onNewIntent(intent);
	}
    
    @Override
    protected void onStart()
    {
    	Bridge.log("Activity for purchasing started.");
        super.onStart();
    }
	
    @Override
    protected void onResume()
    {
    	Bridge.log("Activity for purchasing resumed.");
        super.onResume();
    }

    @Override
	protected void onNewIntent(Intent intent)
	{
    	Bridge.log("Activity for purchasing has new intent.");
    	super.onNewIntent(intent);
    	setIntent(intent);
    	GooglePlay.createBuyIntent(this);
	}
	
	/**
	 * Android activity event.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		resultTriggered = true;
		Bridge.log("Activity result triggered. Request code - " + requestCode);
		if(requestCode == GooglePlay.REQUEST_CODE)
		{
			GooglePlay.handleActivityResult(resultCode, data);
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
		finish();
	}
	
	@Override
    protected void onPause()
	{
		Bridge.log("Activity for purchasing paused.");
        super.onPause();
    }

    @Override
    protected void onStop()
    {
    	Bridge.log("Activity for purchasing stoped.");
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
    	if(!resultTriggered) //means that activity was destroyed by pausing the game
    	{
    		GooglePlay.handleActivityResult(Activity.RESULT_CANCELED, null);
    	}
    	Bridge.log("Activity for purchasing destroying.");
        super.onDestroy();
    }
}