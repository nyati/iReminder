package com.app.iReminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RemSvcAutoStarter extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent i) {
		// TODO Auto-generated method stub
		Log.e("RemSvcAutoStarter", "Received intent" + i.toString());
		Intent serviceIntent = new Intent();
		serviceIntent.setAction("com.app.iReminder.ReminderService");
		Log.e("RemSvcAutoStarter", "service" + serviceIntent.toString());
		ctx.startService(serviceIntent);

		
	}

}
