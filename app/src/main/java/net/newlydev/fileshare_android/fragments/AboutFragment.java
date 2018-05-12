package net.newlydev.fileshare_android.fragments;

import android.content.*;
import android.net.*;
import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import net.newlydev.fileshare_android.*;
import android.sax.*;
import android.view.View.*;

public class AboutFragment extends Fragment
{
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View rootview=inflater.inflate(R.layout.fragment_about,container,false);
		rootview.findViewById(R.id.fragment_about_sourcecode_btn).setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					Uri uri = Uri.parse("https://github.com/uebian/fileshare");
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent);
					// TODO: Implement this method
				}
			});
		
		return rootview;
		//return super.onCreateView(inflater,container,savedInstanceState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);

	}
}
