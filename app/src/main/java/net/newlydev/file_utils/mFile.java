package net.newlydev.file_utils;

import java.util.*;
import java.io.*;
import org.apache.commons.io.input.*;
import android.util.*;

public class mFile
{
	boolean isdir=false;
	String filename;
	String path;
	public mFile(File file)
	{
		path = file.getPath();
		filename = file.getName();
	}
	public String getFileName()
	{
		return filename;
	}
	public boolean isDir()
	{
		return isdir;
	}
	protected void setDir(boolean isdir)
	{
		this.isdir=isdir;
	}
	public long getSize(String sh, String core_bin) throws FileNotFoundException, UnsupportedEncodingException, IOException, InterruptedException
	{
		Process p = Runtime.getRuntime().exec(sh);

		BufferedReader is=new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
		BufferedWriter os=new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), "UTF-8"));
		os.write(core_bin + "\n");
		os.flush();
		os.write("2\n");
		os.write(base64Encode(path.getBytes("UTF-8" )));
		os.flush();
		Thread.sleep(100);
		String res0=is.readLine();
		if (res0.startsWith("error0"))
		{
			os.write("exit");
			os.close();
			p.destroy();
			throw new FileNotFoundException();
		}
		return Long.parseLong(res0);
	}
	public InputStream getInputStream(String sh, String core_bin, String fifo_dir) throws IOException, InterruptedException
	{

		Process p = Runtime.getRuntime().exec(sh);

		BufferedReader is=new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
		BufferedWriter os=new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), "UTF-8"));
		os.write(core_bin + "\n");
		os.flush();
		os.write("0\n");
		os.write(base64Encode(path.getBytes("UTF-8")));
		String fifopath=fifo_dir + getRandomString(32);
		Runtime.getRuntime().exec("mkfifo " + fifopath).waitFor();
		os.write(fifopath + "\n");
		os.flush();
		Thread.sleep(100);
		String res0=is.readLine();
		if (res0.startsWith("error0"))
		{
			os.write("exit");
			os.close();
			p.destroy();
			Runtime.getRuntime().exec("rm -f " + fifopath);
			throw new FileNotFoundException();
		}
		return new FifoInputStream(fifopath, os, p);
	}
	public OutputStream getOutputStream(String sh, String core_bin, String fifo_dir) throws IOException, InterruptedException
	{
		Process p = Runtime.getRuntime().exec(sh);

		BufferedReader is=new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
		BufferedWriter os=new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), "UTF-8"));
		os.write(core_bin + "\n");
		os.flush();
		os.write("1\n");
		os.write(base64Encode(path.getBytes("UTF-8")));
		String fifopath=fifo_dir + getRandomString(32);
		Runtime.getRuntime().exec("mkfifo " + fifopath).waitFor();
		os.write(fifopath + "\n");
		os.flush();
		Thread.sleep(100);
		String res0=is.readLine();
		return new mFileOutputStream(fifopath, os, p);
	}
	public static ArrayList<mFile> listFiles(String sh,String path,String core_bin,String fifo_dir) throws IOException, InterruptedException
	{
		ArrayList<mFile> files=new ArrayList<mFile>();
		Process p= Runtime.getRuntime().exec(sh);
		BufferedReader is=new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
		BufferedWriter os=new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), "UTF-8"));
		os.write(core_bin + "\n");
		os.flush();
		os.write("3\n");
		os.write(base64Encode(path.getBytes("UTF-8")));
		String fifopath=fifo_dir + getRandomString(32);
		Runtime.getRuntime().exec("mkfifo " + fifopath).waitFor();
		
		//Runtime.getRuntime().exec("mkfifo " + fifopath).waitFor();
		os.write(fifopath + "\n");
		os.flush();
		BufferedReader dis=new BufferedReader(new FileReader(fifopath));
		boolean isdirf=false;
		String line;
		int c=1;
		while ((line = dis.readLine())!=null){
			if(c==-1)
			{
				mFile file=new mFile(new File(path,new String(base64Decode(line),"UTF-8")));
				file.setDir(isdirf);
				files.add(file);
			}else{
				int tmp=Integer.parseInt(line);
				if(tmp==1)
				{
					isdirf=true;
				}else
				{
					isdirf=false;
				}
			}
			c=c*(-1);
		}
		Runtime.getRuntime().exec("rm -f " + fifopath);
		Collections.sort(files, new Comparator<mFile>(){

				@Override
				public int compare(mFile p1, mFile p2)
				{
					return p1.getFileName().compareTo(p2.getFileName());
					// TODO: Implement this method
				}
			});
		return files;
	}
	public static String getRandomString(int length)
	{
		//产生随机数
		Random random=new Random();
		StringBuffer sb=new StringBuffer();
		//循环length次
		for (int i=0; i < length; i++)
		{
			//产生0-2个随机数，既与a-z，A-Z，0-9三种可能
			int number=random.nextInt(3);
			long result=0;
			switch (number)
			{
					//如果number产生的是数字0；
				case 0:
					//产生A-Z的ASCII码
					result = Math.round(Math.random() * 25 + 65);
					//将ASCII码转换成字符
					sb.append(String.valueOf((char)result));
					break;
				case 1:
					//产生a-z的ASCII码
					result = Math.round(Math.random() * 25 + 97);
					sb.append(String.valueOf((char)result));
					break;
				case 2:
					//产生0-9的数字
					sb.append(String.valueOf
							  (new Random().nextInt(10)));
					break; 
			}
		}
		return sb.toString();
	}
	public static String base64Encode(byte[] data)
	{
		return Base64.encodeToString(data,Base64.DEFAULT);
	}
	public static byte[] base64Decode(String base64str)
	{
		return Base64.decode(base64str,Base64.DEFAULT);
	}
}

