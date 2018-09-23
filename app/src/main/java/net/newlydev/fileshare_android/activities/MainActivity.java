package net.newlydev.fileshare_android.activities;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v4.widget.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;
import com.google.android.gms.ads.*;
import com.zinc.libpermission.utils.*;
import java.io.*;
import net.newlydev.fileshare_android.*;
import net.newlydev.fileshare_android.activities.*;
import net.newlydev.fileshare_android.fragments.*;

import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import net.newlydev.fileshare_android.R;

public class MainActivity extends mActivity
{
	public boolean waiting;
	public ProgressBar pb;
	public ListView lv;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		JPermissionUtil.requestAllPermission(this);
		
		AdView adview=(AdView) findViewById(R.id.adView);
		//AdRequest adRequest = new AdRequest.Builder().addTestDevice("27E31343F422BD0D601A6F9D3D438A95").build();
		AdRequest adRequest=new AdRequest.Builder().addTestDevice("27E31343F422BD0D601A6F9D3D438A95").build();
        adview.loadAd(adRequest);
		final Fragment statusfragment=new MainFragment();
		final Fragment aboutfragment=new AboutFragment();
		final Fragment settingfragment=new SettingFragment();
		final FragmentManager fragmentManager = getSupportFragmentManager();

		waiting = false;
		Toolbar toolbar=(Toolbar) findViewById(R.id.toolbar_normal);
		lv = (ListView) findViewById(R.id.activity_main_list);
		pb = (ProgressBar) findViewById(R.id.activity_main_waitingprogressbar);
		pb.setVisibility(View.GONE);
		ArrayAdapter<String> aa=new ArrayAdapter<String>(this, R.layout.list_text);
		aa.add("首页");
		aa.add("设置");
		aa.add("关于");
		lv.setAdapter(aa);

		setSupportActionBar(toolbar);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		PackageManager manager = this.getPackageManager();
		PackageInfo info =null;
		try
		{
			info = manager.getPackageInfo(this.getPackageName(), 0);
		}
		catch (PackageManager.NameNotFoundException e)
		{}
		File varfile=new File(getFilesDir(), "var");
		if (!varfile.exists())
		{
			fragmentManager.beginTransaction().replace(R.id.activity_main_content, settingfragment).commit();
			try
			{
				varfile.createNewFile();

				DataOutputStream varos=new DataOutputStream(new FileOutputStream(varfile));
				varos.writeInt(info.versionCode);
				varos.close();
			}
			catch (Exception e)
			{}
			Toast.makeText(this, "首次使用，请先设置", Toast.LENGTH_SHORT).show();
			//String CPU_ABI = android.os.Build.CPU_ABI;
			try
			{
				File f=new File(getDataDir() + "/bin/");
				f.mkdirs();
				f = new File(getDataDir() + "/fifo/");
				f.mkdirs();
				f = new File(getDataDir() + "/bin/fileshare_core");
				f.createNewFile();
				InputStream is=getClassLoader().getResourceAsStream("assets/bin/fileshare_core");
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
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			lv.setItemChecked(1, true);
		}
		else
		{
			try
			{
				DataInputStream varis=new DataInputStream(new FileInputStream(varfile));
				if (varis.readInt() < info.versionCode)
				{
					startActivity(new Intent(this,UpdateManagerActivity.class));
					finish();
					/*DataOutputStream varos=new DataOutputStream(new FileOutputStream(varfile));
					varos.writeInt(info.versionCode);
					varos.close();*/
				}
				varis.close();
			}
			catch (Exception e)
			{}
			fragmentManager.beginTransaction().replace(R.id.activity_main_content, statusfragment).commit();
			lv.setItemChecked(0, true);
		}
		final DrawerLayout mDrawerLayout=(DrawerLayout) findViewById(R.id.activity_main_dl);
		ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open, R.string.close) {
			@Override
			public void onDrawerOpened(View drawerView)
			{
				super.onDrawerOpened(drawerView);
			}
			@Override
			public void onDrawerClosed(View drawerView)
			{
				super.onDrawerClosed(drawerView);
			}
		};
		mDrawerToggle.syncState();
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		lv.setOnItemClickListener(new OnItemClickListener(){

				@Override
				public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
				{
					switch (p3)
					{
						case 0:
							fragmentManager.beginTransaction().replace(R.id.activity_main_content, statusfragment).commit();
							break;
						case 1:
							fragmentManager.beginTransaction().replace(R.id.activity_main_content, settingfragment).commit();
							break;
						case 2:
							fragmentManager.beginTransaction().replace(R.id.activity_main_content, aboutfragment).commit();
							break;
					}
					mDrawerLayout.closeDrawers();
					// TODO: Implement this method
				}
			});
	}

	@Override
	public void onBackPressed()
	{
		// TODO: Implement this method
		if (!waiting)
		{
			super.onBackPressed();
		}
	}

}
