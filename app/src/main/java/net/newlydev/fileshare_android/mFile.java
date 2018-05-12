package net.newlydev.fileshare_android;

import java.util.*;

public class mFile
{
	String cmdline;
	String filename;
	String path;
	public mFile(String cmdline,String path)
	{
		this.cmdline=cmdline;
		this.path=path;
		/*String tmps[]=cmdline.split(" ");
		ArrayList<String> cs=new ArrayList<String>();
		for(String tmp:tmps)
		{
			if(!tmp.trim().equals(""))
			{
				cs.add(tmp.trim());
			}
		}
		filename=cs.get(cs.size()-1);*/
		boolean islast=false;
		int count=0;
		int js=0;
		for(char c:cmdline.toCharArray())
		{
			if(c==' ' && islast==false)
			{
				count++;
				islast=true;
			}
			if(c!=' ')
			{
				islast=false;
			}
			if(c!=' ' && count==7){
				break;
			}
			js++;
		}
		filename=cmdline.substring(js);
	}
	public String getFileName()
	{
		return filename;
	}
	public boolean isDir()
	{
		return cmdline.startsWith("d");
	}
}
