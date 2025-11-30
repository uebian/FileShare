package net.newlydev.fileshare_android;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.lifecycle.LifecycleService;
import androidx.preference.PreferenceManager;

import net.newlydev.fileshare_android.activities.MainActivity;
import net.newlydev.fileshare_android.http.HttpThread;
import net.newlydev.fileshare_android.PermissionRequestManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainService extends LifecycleService {
	private static final String CHANNEL_ID = "fileshare_service";
	private static final int NOTIFICATION_ID = 1;
	private final ExecutorService connectionExecutor = Executors.newCachedThreadPool();
	private final ExecutorService serverExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "FileShareServer");
		thread.setPriority(Thread.NORM_PRIORITY);
		return thread;
	});
	private final AtomicBoolean running = new AtomicBoolean(false);
	private NotificationCompat.Builder builder;
	private Future<?> serverFuture;
	private ServerSocket serverSocket;

	@Override
	public void onCreate() {
		super.onCreate();
		PermissionRequestManager.init(getApplicationContext());
		createNotificationChannel();
		builder = new NotificationCompat.Builder(this, CHANNEL_ID)
			.setContentTitle("文件共享服务运行中")
			.setContentText("点击管理")
			.setSmallIcon(R.mipmap.ic_launcher)
			.setOngoing(true)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
			.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		startForeground(NOTIFICATION_ID, builder.build());
		startServerLoop();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		running.set(false);
		if (serverFuture != null) {
			serverFuture.cancel(true);
		}
		closeServerSocket();
		serverExecutor.shutdownNow();
		connectionExecutor.shutdownNow();
		ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
		ServiceStatusTracker.updateStatus(ServiceStatusTracker.Status.STOPPED);
		Session.sessions.clear();
		super.onDestroy();
	}

	private void startServerLoop() {
		if (!running.compareAndSet(false, true)) {
			return;
		}
		ServiceStatusTracker.updateStatus(ServiceStatusTracker.Status.STARTING);
		serverFuture = serverExecutor.submit(() -> {
			try {
				int port = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("serverPort", "-1"));
				serverSocket = new ServerSocket(port);
				ServiceStatusTracker.updateStatus(ServiceStatusTracker.Status.RUNNING);
				listenForClients();
			} catch (Exception e) {
				ServiceStatusTracker.reportError(e.getMessage());
				stopSelf();
			} finally {
				running.set(false);
				ServiceStatusTracker.updateStatus(ServiceStatusTracker.Status.STOPPED);
				closeServerSocket();
				serverFuture = null;
			}
		});
	}

	private void listenForClients() {
		while (running.get() && !Thread.currentThread().isInterrupted()) {
			try {
				Socket client = serverSocket.accept();
				try {
					connectionExecutor.execute(new HttpThread(client, MainService.this));
				} catch (RuntimeException executorException) {
					ServiceStatusTracker.reportError(executorException.getMessage());
					try {
						client.close();
					} catch (IOException ignored) {
					}
				}
			} catch (IOException acceptException) {
				if (running.get()) {
					ServiceStatusTracker.reportError(acceptException.getMessage());
				}
				break;
			}
		}
	}
	private void closeServerSocket() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException ignored) {
			} finally {
				serverSocket = null;
			}
		}
	}

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
			NotificationManager manager = getSystemService(NotificationManager.class);
			if (manager != null) {
				manager.createNotificationChannel(channel);
			}
		}
	}

}
