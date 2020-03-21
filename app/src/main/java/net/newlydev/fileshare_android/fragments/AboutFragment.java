package net.newlydev.fileshare_android.fragments;

import android.content.*;
import android.content.pm.PackageManager;
import android.net.*;
import android.os.*;
import android.view.*;
import net.newlydev.fileshare_android.*;

import android.view.View.*;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class AboutFragment extends Fragment
{
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView=inflater.inflate(R.layout.fragment_about,container,false);
		rootView.findViewById(R.id.fragment_about_sourcecode_github_btn).setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					Uri uri = Uri.parse("https://github.com/uebian/fileshare");
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent);
				}
			});
		try {
			((TextView)rootView.findViewById(R.id.fragment_about_version_name_tv)).setText(getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(),0).versionName);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return rootView;
		//return super.onCreateView(inflater,container,savedInstanceState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}
}
