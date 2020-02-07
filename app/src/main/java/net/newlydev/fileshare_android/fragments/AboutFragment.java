package net.newlydev.fileshare_android.fragments;

import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import net.newlydev.fileshare_android.*;

import android.view.View.*;

import androidx.fragment.app.Fragment;

public class AboutFragment extends Fragment
{
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootview=inflater.inflate(R.layout.fragment_about,container,false);
		rootview.findViewById(R.id.fragment_about_sourcecode_github_btn).setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					Uri uri = Uri.parse("https://github.com/uebian/fileshare");
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent);
				}
			});
		return rootview;
		//return super.onCreateView(inflater,container,savedInstanceState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}
}
