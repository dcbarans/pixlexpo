package dbaranski.pixlexpo;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

public class PixlexpoActivity extends Activity {

	static final String SETTING_WALLPAPER_MAX = "wallpapers_max";
	static final int SETTING_DEFAULT_WALLPAPER_MAX = 6;

	static final String SETTING_WALLPAPER_ROTATE = "wallpapers_rotate";
	static final int SETTING_DEFAULT_WALLPAPER_ROTATE = 3;

	static final String SETTING_WALLPAPER_ROTATE_INCREMENT = "wallpapers_rotate_increment";
	static final int SETTING_DEFAULT_WALLPAPER_ROTATE_INCREMENT = 60;

	static final String SETTING_WALLPAPER_PINNED = "wallpapers_pinned";

	SharedPreferences settings;

	SeekBar sb_maxWallpapers;
	SeekBar sb_wallpaperTime;
	TextView lbl_maxWallpapers;
	TextView lbl_wallpaperTime;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Do some settings stuff here
		settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		lbl_maxWallpapers = (TextView) findViewById(R.id.lbl_maxWallpapers);
		lbl_wallpaperTime = (TextView) findViewById(R.id.lbl_wallpaperTime);

		// ----------------------

		sb_maxWallpapers = (SeekBar) findViewById(R.id.sb_maxWallpapers);
		sb_maxWallpapers.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				lbl_maxWallpapers.setText(progress + " " + getString(R.string.wallpapers_will_be_loaded));

				SharedPreferences.Editor editor = settings.edit();
				editor.putInt(SETTING_WALLPAPER_MAX, progress);
				editor.commit();

			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		sb_maxWallpapers.setProgress(settings.getInt(SETTING_WALLPAPER_MAX, SETTING_DEFAULT_WALLPAPER_MAX));

		// ----------------------

		sb_wallpaperTime = (SeekBar) findViewById(R.id.sb_wallpaperTime);
		sb_wallpaperTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				lbl_wallpaperTime.setText(getString(R.string.text_rotate_wallpaper) + " " + progress
						* settings.getInt(SETTING_WALLPAPER_ROTATE_INCREMENT, SETTING_DEFAULT_WALLPAPER_ROTATE_INCREMENT) + " minutes");

				Log.v("new setting : ",
						progress + " * " + settings.getInt(SETTING_WALLPAPER_ROTATE_INCREMENT, SETTING_DEFAULT_WALLPAPER_ROTATE_INCREMENT));
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt(SETTING_WALLPAPER_ROTATE,
						progress * settings.getInt(SETTING_WALLPAPER_ROTATE_INCREMENT, SETTING_DEFAULT_WALLPAPER_ROTATE_INCREMENT));
				editor.commit();
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		sb_wallpaperTime.setProgress(settings.getInt(SETTING_WALLPAPER_ROTATE, SETTING_DEFAULT_WALLPAPER_ROTATE));

		Log.d("debug", "attempting to bind to service in Activity");
		startService(new Intent(this, PixlExpoService.class));
	}
}