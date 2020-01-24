package net.newlydev.fileshare_android;
import android.content.*;

import androidx.preference.PreferenceManager;

import java.util.*;

public class Session
{
	public static ArrayList<Session> sessions=new ArrayList<Session>();
	private String token;
	private String path;
	private String rootpath;
	private static HashMap<String,String> downloadLinks=new HashMap<String, String>();
	public Session(Context ctx)
	{
		token = Utils.getRandomString(32);
		if(PreferenceManager.getDefaultSharedPreferences(ctx).getString("fileSystem", "api").equals("api"))
		{
			rootpath="/";
		}else{
			rootpath = PreferenceManager.getDefaultSharedPreferences(ctx).getString("rootPath", "/");
		}
		path=rootpath;
		sessions.add(this);
	}

	public String getPath()
	{
		return path;
	}

	public String getToken()
	{
		return token;
	}
	public void enterDir(String name)
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
			String[] paths = path.split("/");
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

	public String createDownloadToken(String filePath)
	{
		String downloadToken=token+Utils.getRandomString(32);
		downloadLinks.put(downloadToken,filePath);
		return downloadToken;
	}

	public static String getRealPath(String downloadToken)
	{
		return downloadLinks.get(downloadToken);
	}
}
