package dbaranski.pixlexpo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

public class Wallpaper implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -862883852480082641L;

	private int id;

	private int timesShown;
	private Date dateRetrieved;
	private Date dateFirstShown;

	public Wallpaper(int id, Bitmap image) throws FileNotFoundException {
		Log.v("Wallpaper", "Creating Wallpaper (" + id + ")");

		this.id = id;
		this.timesShown = 0;
		this.dateRetrieved = new Date();

		File appdir = new File(Environment.getExternalStorageDirectory().toString() + "/PixlExpo");
		appdir.mkdirs();

		FileOutputStream out = new FileOutputStream(new File(appdir, "" + id));
		image.compress(Bitmap.CompressFormat.PNG, 90, out);
		image.recycle();
	}

	/** Used for wpm.setResource() */
	public Bitmap getImage() {

		String fp = Environment.getExternalStorageDirectory().toString();

		if (dateFirstShown == null) {
			this.dateFirstShown = new Date();
		}
		this.timesShown++;

		return BitmapFactory.decodeFile(fp + "/PixlExpo/" + id);
	}

	/**
	 * Used this for wpm.setStream()
	 * 
	 * I think this is more efficient because I don't have to create a bitmap
	 * all over the place
	 * */
	public InputStream getImageStream() {
		InputStream is = null;

		try {
			String fp = Environment.getExternalStorageDirectory().toString();
			is = new FileInputStream(new File(fp + "/PixlExpo/" + id));

			if (dateFirstShown == null) {
				this.dateFirstShown = new Date();
			}
			this.timesShown++;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			is = null;
		}
		return is;
	}

	public Date getDateRetrieved() {
		return dateRetrieved;
	}

	public void setDateRetrieved(Date dateRetrieved) {
		this.dateRetrieved = dateRetrieved;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void dataDump() {

		Log.d("Wallpaper Debug", "id : " + id);
		Log.d("Wallpaper Debug", "timesShown : " + timesShown);
		Log.d("Wallpaper Debug", "dateRetrieved : " + dateRetrieved);
		Log.d("Wallpaper Debug", "dateFirstShown : " + dateFirstShown);

	}
}
