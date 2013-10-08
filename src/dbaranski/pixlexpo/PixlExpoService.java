package dbaranski.pixlexpo;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;

public class PixlExpoService extends Service {

	private Bitmap bmap;

	private LinkedList<Wallpaper> wallpapers;

	static final String TAG = "PixlExpoService";

	static final String CHANGE_BACKGROUND = "dbaranski.pixlexpo.CHANGE_BACKGROUND";
	static final String PIN_BACKGROUND = "dbaranski.pixlexpo.PIN_BACKGROUND";

	static final String DB_NAME = "pixlExpo.db";

	private class DownloadImageTask extends AsyncTask<Integer, Void, Void> {
		protected Void doInBackground(Integer... params) {

			Log.v(TAG, params[0].toString());
			Log.v(TAG, params[1].toString());

			int tid = android.os.Process.getThreadPriority(Process.myTid());
			String tag = "TID : " + tid;

			Log.d(tag, "Running in doInBackground now..");

			try {

				String excludes = "";
				for (Iterator<Wallpaper> i = wallpapers.iterator(); i.hasNext();) {
					Wallpaper w = i.next();
					excludes += "exclude[]=" + w.getId() + "&";
				}

				Log.d(tag, "http://pixlexpo.com/api/img/rand?wp_width=" + params[0].toString() + "&wp_height="
						+ params[1].toString() + "&" + excludes);
				
				URL url = new URL("http://pixlexpo.com/api/img/rand?wp_width=" + params[0].toString() + "&wp_height="
						+ params[1].toString() + "&" + excludes);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

				InputStream is = new BufferedInputStream(urlConnection.getInputStream());

				bmap = BitmapFactory.decodeStream(is);
				String wallpaperId = urlConnection.getHeaderField("Pixlexpo-Id");
				String responseStatus = urlConnection.getHeaderField("Pixlexpo-Status");

				if(Integer.parseInt(responseStatus) == 200) {
					if (bmap == null) {
						throw new SocketException("Bitmap image was null, so bad connection");
					}
	
					Wallpaper wp = new Wallpaper(Integer.parseInt(wallpaperId), bmap);
					urlConnection.disconnect();
	
					boolean isDuplicate = false;
					Log.v(tag, "checking for duplicate");
	
					for (Iterator<Wallpaper> i = wallpapers.iterator(); i.hasNext() && !isDuplicate;) {
						Wallpaper w = i.next();
	
						if (w.getId() == wp.getId()) {
							isDuplicate = true;
							break;
						}
					}
	
					if (!isDuplicate) {
						Log.v(tag, "adding to list!");
						wallpapers.addLast(wp);
						writeWallpapersToDB();
					} else {
						Log.v(tag, "is a duplicate");
					}
				} else {
					Log.d(tag, "Server returned status '" + responseStatus + "', expected 200");
				}

			} catch (SocketException se) {
				Log.e(tag, "SocketException" + se.getMessage());

			} catch (Exception e) {
				Log.e(tag, "AsyncTask message : " + e.getMessage());
				e.printStackTrace();
			}

			return null;
		}
	}

	// This is the object that receives interactions from clients.
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		Log.d("debug", "onBind");
		return mBinder;
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		PixlExpoService getService() {
			Log.d("debug", "getService");
			return PixlExpoService.this;
		}
	}

	BroadcastReceiver changebackground = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("PixlExpoService", "Refresh wallpaper");

			boolean switch_wp = true;
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

			boolean isPinned = settings.getBoolean(PixlexpoActivity.SETTING_WALLPAPER_PINNED, false);

			// has user pinned the bg? (don't rotate it!)
			if (!isPinned) {

				// if this is a screen_on intent make sure the WP has been up
				// for given time first!
				if (intent.getAction() == Intent.ACTION_SCREEN_ON) {

					long wp_dateshown = settings.getLong("current_wp_datshown", 0);

					Log.v(TAG, System.currentTimeMillis() + " + "
							+ (settings.getInt(PixlexpoActivity.SETTING_WALLPAPER_ROTATE, 0) + " * 60 * 1000"));

					if (wp_dateshown > 0) {

						long wp_shownext = wp_dateshown + (settings.getInt(PixlexpoActivity.SETTING_WALLPAPER_ROTATE, 0) * 60 * 1000);

						Log.d("date test - now", new Date(System.currentTimeMillis()).toString());
						Log.d("date test - dateshown", new Date(wp_dateshown).toString());
						Log.d("date test - shownext", new Date(wp_shownext).toString());

						if (wp_shownext - System.currentTimeMillis() > 0) {
							Log.d(TAG, "no change yet.. still need " + ((wp_shownext - System.currentTimeMillis()) / 1000) + " seconds");
							switch_wp = false;
						} else {
							Log.d(TAG, "good to switch");
						}
					}
				}

				Log.d(TAG, "switch? : " + switch_wp + " && " + wallpapers.size());
				if (switch_wp && wallpapers.size() > 0) {

					readWallpapersFromDB();
					Log.v("size?", "" + wallpapers.size());

					Wallpaper wp = wallpapers.removeFirst();
					wp.dataDump();

					WallpaperManager myWallpaperManager = WallpaperManager.getInstance(getApplicationContext());

					try {

						Log.v(TAG, myWallpaperManager.getDesiredMinimumWidth() + "w x " + myWallpaperManager.getDesiredMinimumHeight()
								+ "h");

						myWallpaperManager.setStream(wp.getImageStream());

						SharedPreferences.Editor editor = settings.edit();
						editor.putLong("current_wp_datshown", System.currentTimeMillis());
						editor.commit();

						// add back in at the bottom if everything went
						// smoothly
						wallpapers.addLast(wp);

					} catch (NullPointerException npe) {

						// bad wallpaper here i think..
						Log.d("wallpaper check", "Found a bad wallpaper..");
						// Log.d("wallpaper check", "status : " +
						// wallpapers.remove(wp));

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					writeWallpapersToDB();
				}

			} else {
				Log.d(TAG, "bg is pinned, unpin first!");
			}
		}
	};

	BroadcastReceiver screenoff = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("PixlExpoService", "Screen off, get content in bg");

			try {

				Log.v("we attempting (network there?)?", "" + isNetworkAvailable());
				if (isNetworkAvailable()) {

					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

					// Log.d("PixlExpoService - screenoff", notShown+ " < "+
					// settings.getInt(PixlexpoActivity.SETTING_WALLPAPER_MAX,
					// PixlexpoActivity.SETTING_DEFAULT_WALLPAPER_MAX));

					// if (notShown <
					// settings.getInt(PixlexpoActivity.SETTING_WALLPAPER_MAX,
					// PixlexpoActivity.SETTING_DEFAULT_WALLPAPER_MAX)) {

					Log.v("attempting to get wp", "going for it");

					WallpaperManager myWallpaperManager = WallpaperManager.getInstance(getApplicationContext());
					Integer[] wp_size = new Integer[2];
					wp_size[0] = myWallpaperManager.getDesiredMinimumWidth();
					wp_size[1] = myWallpaperManager.getDesiredMinimumHeight();

					new DownloadImageTask().execute(wp_size);

					Log.v("PixlExpoService", "list contains " + wallpapers.size() + " items");

					// }

				}
			} catch (Exception e) {
				Log.e("PixlExpoService", "Exception " + e.getClass() + ": " + e.getMessage(), e);
			}
		}
	};

	BroadcastReceiver pinbackground = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.w(TAG, "pin that shit!");

			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			boolean isPinned = settings.getBoolean(PixlexpoActivity.SETTING_WALLPAPER_PINNED, false);
			SharedPreferences.Editor editor = settings.edit();

			if (isPinned) {
				Log.w(TAG, "unpin");
				editor.putBoolean(PixlexpoActivity.SETTING_WALLPAPER_PINNED, false);

			} else {
				Log.w(TAG, "pin");
				editor.putBoolean(PixlexpoActivity.SETTING_WALLPAPER_PINNED, true);
			}

			editor.commit();
		}
	};

	@Override
	public void onCreate() {
		Log.d("PixlExpoService", "Service Received onCreate()");

		if (wallpapers == null) {
			wallpapers = new LinkedList<Wallpaper>();
		}

		registerReceiver(changebackground, new IntentFilter(Intent.ACTION_SCREEN_ON));
		registerReceiver(changebackground, new IntentFilter(CHANGE_BACKGROUND));

		registerReceiver(pinbackground, new IntentFilter(PIN_BACKGROUND));

		registerReceiver(screenoff, new IntentFilter(Intent.ACTION_SCREEN_OFF));

		readWallpapersFromDB();
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "in onDestroy");

		unregisterReceiver(changebackground);
		unregisterReceiver(pinbackground);
		unregisterReceiver(screenoff);

		writeWallpapersToDB();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "in onStartCommand(" + intent + ", " + flags + ", " + startId + ")");

		readWallpapersFromDB();

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@SuppressWarnings("unchecked")
	private void readWallpapersFromDB() {

		ObjectInputStream ois;

		try {
			Log.v("Read DB", "Reading from database");

			ois = new ObjectInputStream(openFileInput(DB_NAME));
			wallpapers = (LinkedList<Wallpaper>) ois.readObject();
			ois.close();

			boolean isChanged = false;
			// Validate the presense of wallpapers
			for (Iterator<Wallpaper> i = wallpapers.iterator(); i.hasNext();) {
				Wallpaper w = i.next();

				if (w.getImage() == null) {
					Log.d(TAG, "Removing non-existent wp " + w.getId());
					i.remove();
					isChanged = true;
				}

			}

			if (isChanged) {
				writeWallpapersToDB();
			}

		} catch (OptionalDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean writeWallpapersToDB() {

		ObjectOutputStream oos;

		try {
			Log.v("Write DB", "Writing out database");

			oos = new ObjectOutputStream(openFileOutput(DB_NAME, Context.MODE_PRIVATE));
			oos.writeObject(wallpapers);
			oos.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}
}