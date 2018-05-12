package net.newlydev.fileshare_android.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import net.newlydev.fileshare_android.MainService;
import net.newlydev.fileshare_android.R;
import net.newlydev.fileshare_android.Utils;

public class AboutFragment extends Fragment
{
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View rootview=inflater.inflate(R.layout.fragment_about,container,false);
		
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
