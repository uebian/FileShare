package net.newlydev.fileshare_android;
import android.app.*;
import com.google.android.gms.ads.*;
import android.support.multidex.*;

public class mApplication extends MultiDexApplication
{

	@Override
	public void onCreate()
	{
		// TODO: Implement this method
		super.onCreate();
		MobileAds.initialize(this, "ca-app-pub-4267459436057308~9756116155");
	}
	
}
