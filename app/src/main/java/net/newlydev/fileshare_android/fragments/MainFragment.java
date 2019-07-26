package net.newlydev.fileshare_android.fragments;
import android.content.*;
import android.graphics.*;
import android.os.*;

import androidx.appcompat.app.*;
import androidx.preference.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import com.google.zxing.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.*;
import java.util.*;
import net.newlydev.fileshare_android.*;
import net.newlydev.fileshare_android.activities.*;

import net.newlydev.fileshare_android.R;

import androidx.fragment.app.Fragment;

public class MainFragment extends Fragment
{
	Switch switch_ss;
	private MyConnection conn;
	private TextView ip_tv;
	private ImageView QRCode;
	private void update_status()
	{
		if (Utils.isMainServiceRunning(getActivity()))
		{
			switch_ss.setChecked(true);
			getActivity().bindService(new Intent(getActivity(), MainService.class), conn, Context.BIND_AUTO_CREATE);
		}
		else
		{
			switch_ss.setChecked(false);
		}
	}
	private void refreshQR()
	{
		new Handler().postDelayed(new Runnable(){

				@Override
				public void run()
				{
					if (Utils.isMainServiceRunning(getActivity()))
					{
						String ip=Utils.getLocalIpAddress();
						if (ip.equals("0"))
						{
							ip_tv.setText("您的设备似乎没有有效的ipv4连接，请连接WiFi或打开WiFi热点后点本段文字重试\n我们暂不支持ipv6连接");
							QRCode.setVisibility(View.GONE);
						}
						else
						{
							String url="http://" + ip + ":" + PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getString("serverPort", "-1");
							ip_tv.setText("与您处于同一局域网(热点)的用户无需流量无需安装客户端即可通过访问" + url + "或扫描下方二维码访问您共享的文件(网络状态更改时请点击本文字来刷新)");
							Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
							hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
							//图像数据转换，使用了矩阵转换
							try
							{
								BitMatrix bitMatrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, QRCode.getWidth(), QRCode.getHeight(), hints);
								int[] pixels = new int[QRCode.getWidth() * QRCode.getHeight()];
								//下面这里按照二维码的算法，逐个生成二维码的图片，
								//两个for循环是图片横列扫描的结果
								for (int y = 0; y < QRCode.getHeight(); y++)
								{
									for (int x = 0; x < QRCode.getWidth(); x++)
									{
										if (bitMatrix.get(x, y))
										{
											pixels[y * QRCode.getWidth() + x] = 0xff000000;
										}
										else
										{
											pixels[y * QRCode.getWidth() + x] = 0xffffffff;
										}
									}
								}
								//生成二维码图片的格式，使用ARGB_8888
								Bitmap bitmap = Bitmap.createBitmap(QRCode.getWidth(), QRCode.getHeight(), Bitmap.Config.ARGB_8888);
								bitmap.setPixels(pixels, 0, QRCode.getWidth(), 0, 0, QRCode.getWidth(), QRCode.getHeight());
								QRCode.setImageBitmap(bitmap);
								QRCode.setVisibility(View.VISIBLE);
							}
							catch (Exception e)
							{
								e.printStackTrace();
								ip_tv.setText("无法生成二维码，点我重试。也可以通过访问" + url + "访问您共享的文件");
							}
						}
					}
					else
					{
						ip_tv.setText("服务未开启");
						QRCode.setVisibility(View.GONE);
					}
				}
			}, 100);

	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootview=inflater.inflate(R.layout.fragment_main, container, false);
		ip_tv = (TextView) rootview.findViewById(R.id.fragment_main_ip_textview);
		QRCode = (ImageView) rootview.findViewById(R.id.fragment_main_qrcode_imageview);
		ip_tv.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					refreshQR();
				}
			});
		switch_ss = (Switch) rootview.findViewById(R.id.fragment_status_switch);
		update_status();
		switch_ss.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

				@Override
				public void onCheckedChanged(CompoundButton p1, boolean p2)
				{
					if (!p1.isPressed())
					{
						return;
					}
					Intent intent = new Intent(getActivity(), MainService.class);
					if (p2)
					{
						getActivity().startService(intent);
						((MainActivity)getActivity()).waiting = true;
						((MainActivity)getActivity()).lv.setEnabled(false);
						((MainActivity)getActivity()).pb.setVisibility(View.VISIBLE);
						switch_ss.setEnabled(false);
					}
					else
					{
						getActivity().unbindService(conn);
						getActivity().stopService(intent);
					}
					update_status();
					refreshQR();
				}
			});


		return rootview;
		//return super.onCreateView(inflater,container,savedInstanceState);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		update_status();
		refreshQR();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		conn = new MyConnection();
	}
	private class MyConnection implements ServiceConnection
	{

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder)
		{
			final MainService.mBinder service = (MainService.mBinder) binder;
			if (!service.service.started)
			{
				new Handler().postDelayed(new Runnable(){

						@Override
						public void run()
						{
							String err=service.service.getError();
							if (!err.equals(""))
							{
								if (Utils.isMainServiceRunning(getActivity()))
								{
									getActivity().unbindService(conn);
									Intent intent = new Intent(getActivity(), MainService.class);
									getActivity().stopService(intent);
									new AlertDialog.Builder(getActivity()).setTitle("服务器启动失败").setMessage(err).setPositiveButton("确定", null).setCancelable(false).show();
								}
								//Toast.makeText(getActivity(),err,Toast.LENGTH_SHORT).show();
							}
							((MainActivity)getActivity()).waiting = false;
							((MainActivity)getActivity()).pb.setVisibility(View.GONE);
							((MainActivity)getActivity()).lv.setEnabled(true);
							switch_ss.setEnabled(true);
							update_status();
						}
					}, 500);
				service.service.started = true;
			}

		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{

		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (Utils.isMainServiceRunning(getActivity()))
		{
			getActivity().unbindService(conn);
		}
	}

}
