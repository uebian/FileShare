package net.newlydev.fileshare_android;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.preference.PreferenceManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import net.newlydev.fileshare_android.threads.HttpThread;
import android.os.*;

public class MainService extends Service
{
	public Handler handler=new Handler();
	Notification.Builder builder;
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
		builder= new Notification.Builder(this);
		builder.setContentTitle("文件共享服务运行中");
		builder.setContentText("点击管理");
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setOngoing(true);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(0,builder.build());
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
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
		running=false;
		new Thread(){
			@Override
			public void run(){
				try
				{
					ss.close();

				}
				catch (IOException e)
				{}}
		}.start();
		// TODO: Implement this method
		super.onDestroy();
	}
	
	public class mBinder extends Binder
	{
		public MainService service=MainService.this;
	}
}
