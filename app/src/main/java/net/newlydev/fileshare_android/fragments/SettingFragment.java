package net.newlydev.fileshare_android.fragments;

import android.os.*;
import android.support.v7.app.*;
import android.support.v7.preference.*;
import java.io.*;
import net.newlydev.fileshare_android.*;

import net.newlydev.fileshare_android.R;

public class SettingFragment extends PreferenceFragmentCompat
{

	@Override
	public void onCreatePreferences(Bundle p1, String p2)
	{
		/*PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
		EditTextPreference etp=new EditTextPreference(getActivity());
		etp.setKey("port");
		etp.setDefaultValue("8080");
		etp.setTitle("http协议端口");
		preferenceScreen.addPreference(etp);
		setPreferenceScreen(preferenceScreen);*/
		addPreferencesFromResource(R.xml.setting_preference);
		findPreference("serverport").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

				@Override
				public boolean onPreferenceChange(Preference p1, Object p2)
				{
					boolean ok=true;
					try{
						int i=Integer.parseInt(p2.toString());
						if(i<=1024 || i>65536)
						{
							ok=false;
						}
					}catch(NumberFormatException e)
					{
						ok=false;
						
					}
					if(!ok)
					{
						AlertDialog.Builder ab=new AlertDialog.Builder(getActivity());
						ab.setTitle("更改未保存");
						ab.setCancelable(false);
						ab.setPositiveButton("确定",null);
						ab.setMessage("您的输入有误");
						ab.show();
						return false;
					}
					// TODO: Implement this method
					return true;
				}
			});
		final EditTextPreference rootpath=(EditTextPreference) findPreference("rootpath");
		rootpath.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

				@Override
				public boolean onPreferenceChange(Preference p1, Object p2)
				{
					
					// TODO: Implement this method
					return true;
				}
			});
		ListPreference lp=(ListPreference) findPreference("authtype");
		lp.setEntries(new String[]{"无认证（不安全）","密码验证","询问我经过我许可"});
		lp.setEntryValues(new String[]{"none","passwd","askme"});
		final EditTextPreference pwd=(EditTextPreference) findPreference("password");
		if(pwd.getText()==null)
		{
			pwd.setText(Utils.getRandomString(8));
		}
		pwd.setEnabled(false);
		pwd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

				@Override
				public boolean onPreferenceChange(Preference p1, Object p2)
				{
					// TODO: Implement this method
					if(p2.toString().equals(""))
					{
						new AlertDialog.Builder(getActivity()).setTitle("更改未保存").setMessage("密码不得为空").setCancelable(false).setPositiveButton("确定",null).show();
						return false;
					}else{
						return true;
					}
				}
			});
		if(lp.getValue().equals("passwd"))
		{
			pwd.setEnabled(true);
		}
		lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

				@Override
				public boolean onPreferenceChange(Preference p1, Object p2)
				{
					if(p2.equals("passwd"))
					{
						pwd.setEnabled(true);
					}else{
						pwd.setEnabled(false);
					}
					// TODO: Implement this method
					return true;
				}
			});
		// TODO: Implement this method
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		
	}
}
