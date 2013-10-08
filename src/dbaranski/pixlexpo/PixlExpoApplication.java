package dbaranski.pixlexpo;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "dHA0QldMbkd1NXFuSVdTVDBObXhvRlE6MQ")
public class PixlExpoApplication extends Application {
	@Override
	public void onCreate() {
		// The following line triggers the initialisation of ACRA
		ACRA.init(this);
		super.onCreate();
	}

}
