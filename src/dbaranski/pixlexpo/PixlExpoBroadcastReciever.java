package dbaranski.pixlexpo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PixlExpoBroadcastReciever extends BroadcastReceiver {

	static final String TAG = "PixlExpoBroadcastReciever";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, "starting service on first boot!");

		Intent startServiceIntent = new Intent(context, PixlExpoService.class);
		context.startService(startServiceIntent);
	}

}
