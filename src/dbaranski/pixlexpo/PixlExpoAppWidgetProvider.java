package dbaranski.pixlexpo;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class PixlExpoAppWidgetProvider extends AppWidgetProvider {

	static final String TAG = "PixlExpoAppWidgetProvider";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		Log.d(TAG, "onUpdate called..");

		ComponentName thisWidget = new ComponentName(context, PixlExpoAppWidgetProvider.class);

		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

		Log.d("PixlExpoAppWidgetProvider", "Widget onUpdate called");

		// Register buttons to fire "Intent"s
		remoteViews.setOnClickPendingIntent(R.id.btn_refresh_wallpaper,
				PendingIntent.getBroadcast(context, 0, new Intent(PixlExpoService.CHANGE_BACKGROUND), PendingIntent.FLAG_UPDATE_CURRENT));

		remoteViews.setOnClickPendingIntent(R.id.btn_pin_wallpaper,
				PendingIntent.getBroadcast(context, 0, new Intent(PixlExpoService.PIN_BACKGROUND), PendingIntent.FLAG_UPDATE_CURRENT));

		appWidgetManager.updateAppWidget(thisWidget, remoteViews);
	}
}
