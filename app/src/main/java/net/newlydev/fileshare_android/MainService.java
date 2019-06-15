package net.newlydev.fileshare_android;
import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v7.preference.*;
import java.io.*;
import java.net.*;
import net.newlydev.fileshare_android.activities.*;
import net.newlydev.fileshare_android.threads.*;

public class MainService extends Service
{
	public Handler handler=new Handler();
	NotificationCompat.Builder builder;
	ServerSocket ss;
	private boolean running=false;
	private String errorstr="";
	public boolean started=false;
	Thread mainThread=new Thread(){
		@Override
		public void run()
		{
			try
			{
				started=false;
				int port=Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("serverport","-1"));
				ss=new ServerSocket(port);
			}
			catch (Exception e)
			{
				errorstr=e.toString();
				return;
			}
			while (running)
			{
				try
				{
					Socket client=ss.accept();
					new HttpThread(client,MainService.this).start();
				}
				catch (IOException e)
				{
				}
			}
		}
	};
	
	public String getError()
	{
		String err=errorstr;
		errorstr="";
		return err;
	}
	
	public boolean isrunning()
	{
		return running;
	}
	
	@Override
	public void onCreate()
	{
		// TODO: Implement this method
		super.onCreate();
		builder= new NotificationCompat.Builder(this);
		builder.setContentTitle("文件共享服务运行中");
		builder.setContentText("点击管理");
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setOngoing(true);
		builder.setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),0));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			builder.setChannelId("0");
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		startForeground(0,builder.build());
		running=true;
		mainThread.start();
		// TODO: Implement this method
		return super.onStartCommand(intent, flags, startId);
	}

	
	@Override
	public IBinder onBind(Intent p1)
	{
		// TODO: Implement this method
		return new mBinder();
	}

	@Override
	public void onDestroy()
	{
		//((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
		running=false;
		new Thread(){
			@Override
			public void run(){
				try
				{
					ss.close();
				}
				catch (Exception e)
				{}
			}
		}.start();
		// TODO: Implement this method
		super.onDestroy();
		Session.sessions.clear();
	}
	
	public class mBinder extends Binder
	{
		public MainService service=MainService.this;
	}
}
