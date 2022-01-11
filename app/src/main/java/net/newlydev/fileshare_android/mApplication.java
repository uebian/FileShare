package net.newlydev.fileshare_android;
import android.app.*;
import android.os.*;
import com.google.android.gms.ads.*;

public class mApplication extends Application
{

	@Override
	public void onCreate()
	{
		super.onCreate();
		MobileAds.initialize(this);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel mChannel = new NotificationChannel("0", "状态", NotificationManager.IMPORTANCE_LOW);
			((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(mChannel);
		}
	}

}
