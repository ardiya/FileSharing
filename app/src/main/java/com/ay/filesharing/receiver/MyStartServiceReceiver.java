package com.ay.filesharing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ay.filesharing.FileSharingService;

public class MyStartServiceReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent service = new Intent(context, FileSharingService.class);
		context.startService(service);
		Log.e("MyStartService on Receive", "FileSharing Service start");
	}
}