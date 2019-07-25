package net.newlydev.fileshare_android;
import android.content.*;
import android.preference.*;
import java.util.*;
//import org.apache.http.impl.io.*;

public class Session
{
	public static ArrayList<Session> sessions=new ArrayList<Session>();
	private String token;
	private String path;
	private String rootpath;
	public Session(Context ctx)
	{
		token = Utils.getRandomString(32);
		if(PreferenceManager.getDefaultSharedPreferences(ctx).getString("filesystem", "api").equals("api"))
		{
			rootpath="/";
		}else{
			rootpath = PreferenceManager.getDefaultSharedPreferences(ctx).getString("rootpath", "/");
		}
		path=rootpath;
		sessions.add(this);
	}

	public String getpath()
	{
		return path;
	}

	public String getToken()
	{
		return token;
	}
	public void enterdir(String name)
	{
		if(name.indexOf("/")!=-1||name.equals("."))
		{
			return;
		}
		if (name.equals(".."))
		{
			if(path.equals(rootpath)||path.equals(rootpath+"/")||path.equals(rootpath+"//"))
			{
				return;
			}
			//----/sdcard/abc/
			String paths[]=path.split("/");
			//alert(paths)
			path = "/";
			for (int i=1;i < paths.length - 1;i++)
			{
				path = path + paths[i] + "/";
			}
		}
		else
		{
			path = path + name + "/";
		}

	}
	public static Session getSession(String token)
	{
		for (Session s:sessions)
		{
			if (s.token.equals(token))
			{
				return s;
			}
		}
		return null;
	}
}
