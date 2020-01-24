package net.newlydev.fileshare_android;
import android.app.*;
import android.content.*;
import android.os.*;
import androidx.core.app.*;
import androidx.preference.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.newlydev.fileshare_android.activities.*;
import net.newlydev.fileshare_android.http.*;

public class MainService extends Service
{
	private static ExecutorService mainThreadPool= Executors.newCachedThreadPool();
	public Handler handler=new Handler();
	private NotificationCompat.Builder builder;
	private ServerSocket ss;
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
				int port=Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("serverPort","-1"));
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
					mainThreadPool.submit(new HttpThread(client,MainService.this));
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

	@Override
	public void onCreate()
	{
		super.onCreate();
		builder= new NotificationCompat.Builder(this,"0");
		builder.setContentTitle("文件共享服务运行中");
		builder.setContentText("点击管理");
		builder.setSmallIcon(R.mipmap.ic_launcher);
		builder.setOngoing(true);
		builder.setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),0));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		startForeground(1,builder.build());
		running=true;
		mainThread.start();
		return super.onStartCommand(intent, flags, startId);
	}

	
	@Override
	public IBinder onBind(Intent p1)
	{
		return new mBinder();
	}

	@Override
	public void onDestroy()
	{
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
		super.onDestroy();
		Session.sessions.clear();
	}
	
	public class mBinder extends Binder
	{
		public MainService service=MainService.this;
	}
}
