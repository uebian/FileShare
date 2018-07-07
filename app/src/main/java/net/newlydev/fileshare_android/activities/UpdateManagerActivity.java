package net.newlydev.fileshare_android.activities;
import android.os.*;
import android.support.v7.widget.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import java.io.*;
import net.newlydev.fileshare_android.*;

import android.support.v7.widget.Toolbar;
import android.content.*;

public class UpdateManagerActivity extends mActivity
{
	Button btn_cancel;
	LinearLayout mainview;
	Button btn_next;
	int step=0;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_update);
		Toolbar toolbar=(Toolbar) findViewById(R.id.toolbar_normal);
		setSupportActionBar(toolbar);
		btn_cancel = (Button) findViewById(R.id.activity_update_cancel_btn);
		btn_next = (Button) findViewById(R.id.activity_update_next_btn);
		btn_cancel.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					finish();
					// TODO: Implement this method
				}
			});
		btn_next.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					mainview.removeAllViews();
					switch (step)
					{
						case 0:

							btn_cancel.setEnabled(false);
							btn_next.setEnabled(false);
							TextView tv_progress=new TextView(UpdateManagerActivity.this);
							tv_progress.setText("正在处理...");
							mainview.addView(tv_progress);
							ProgressBar pb=new ProgressBar(UpdateManagerActivity.this);
							mainview.addView(pb);
							new Thread(){
								@Override
								public void run()
								{
									try
									{
										InputStream is=getClassLoader().getResourceAsStream("assets/bin/fileshare_core");
										File f = new File(getDataDir() + "/bin/fileshare_core");
										if (f.exists())
										{
											f.delete();
										}
										f.createNewFile();
										FileOutputStream fos=new FileOutputStream(f);
										byte[] buffer=new byte[1024];
										int ch = is.read(buffer);                
										while (ch != -1)
										{
											fos.write(buffer, 0, ch);  
											ch = is.read(buffer, 0, 1024);  
										}
										f.setExecutable(true);
										is.close();
										fos.close();
										f=new File(getFilesDir(), "var");
										f.delete();
										f.createNewFile();
										DataOutputStream dos=new DataOutputStream(new FileOutputStream(f));
										dos.writeInt(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
										runOnUiThread(new Runnable(){

												@Override
												public void run()
												{
													mainview.removeAllViews();
													TextView tv_ok=new TextView(UpdateManagerActivity.this);
													tv_ok.setText("您已完成了对新版本的配置");
													mainview.addView(tv_ok);
													btn_next.setEnabled(true);
													step=1;
													// TODO: Implement this method
												}
											});
									}
									catch (final Exception e)
									{
										runOnUiThread(new Runnable(){

												@Override
												public void run()
												{
													mainview.removeAllViews();
													TextView tv_error=new TextView(UpdateManagerActivity.this);
													tv_error.setText("错误:"+e.toString());
													mainview.addView(tv_error);
													btn_cancel.setEnabled(true);
													// TODO: Implement this method
												}
											});
									}
								}
							}.start();
							break;
						case 1:
							startActivity(new Intent(UpdateManagerActivity.this,MainActivity.class));
							finish();
							break;
					}

					// TODO: Implement this method
				}
			});
		mainview = (LinearLayout) findViewById(R.id.activity_update_mainview_ll);
	}

	@Override
	public void onBackPressed()
	{
		// TODO: Implement this method
		//super.onBackPressed();
	}

}
