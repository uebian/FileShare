package net.newlydev.fileshare_android.fragments;

import android.os.*;
import android.support.v7.app.*;
import android.support.v7.preference.*;
import java.io.*;
import net.newlydev.fileshare_android.*;

import net.newlydev.fileshare_android.R;
import android.graphics.drawable.*;
import android.view.View.*;
import android.content.*;

public class SettingFragment extends PreferenceFragmentCompat
{
	
	@Override
	public void onCreatePreferences(Bundle p1, String p2)
	{
		addPreferencesFromResource(R.xml.setting_preference);
		ListPreference filesystem=(ListPreference) findPreference("filesystem");
		filesystem.setEntries(new String[]{"Document API","Shell(实验性)","Root权限的Shell(实验性)"});
		filesystem.setEntryValues(new String[]{"api","shell","root_shell"});
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
					Session.sessions.clear();
					// TODO: Implement this method
					return true;
				}
			});
		ListPreference lp=(ListPreference) findPreference("authtype");
		lp.setEntries(new String[]{"无认证（不安全）","密码验证","询问我经过我许可"});
		lp.setEntryValues(new String[]{"none","passwd","askme"});
		final EditTextPreference pwd=(EditTextPreference) findPreference("password");
		final Preference getpath=findPreference("getpath");
		getpath.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
				@Override
				public boolean onPreferenceClick(Preference p1)
				{
					Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					getActivity().startActivityForResult(i,1);
					// TODO: Implement this method
					return false;
				}
			});
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
		if(!filesystem.getValue().equals("api"))
		{
			rootpath.setVisible(true);
			getpath.setVisible(false);
		}else{
			rootpath.setVisible(false);
			getpath.setVisible(true);
		}
		filesystem.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

				@Override
				public boolean onPreferenceChange(Preference p1, Object p2)
				{
					if(!p2.equals("api"))
					{
						rootpath.setVisible(true);
						getpath.setVisible(false);
					}else{
						rootpath.setVisible(false);
						getpath.setVisible(true);
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
