package net.newlydev.fileshare_android;

import android.app.*;
import android.content.*;
import android.net.wifi.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import android.net.*;

public class Utils
{
	private static final String[][] MIME_StrTable = {
		{".xml", "text/plain"},
		{".html", "text/html"},
		{".htm", "text/html"},
		{".css", "text/css"},
		{".jpg", "image/jpeg"},
		{".jpeg", "image/jpeg"},
		{".bmp", "image/bmp"},
		{".gif", "image/gif"},
		{".png", "image/png"},
		{".js", "application/x-javascript"},
	};
	static HashMap<String,String> mimeMapKeyIsContentType = null;
	static HashMap<String,String> mimeMapKeyIsExpands = null;
	public static HashMap<String,String> CreateMIMEMapKeyIsContentType(){

		HashMap<String,String> mimeHashMap = new HashMap<String,String>();

		for(int i = 0; i < MIME_StrTable.length; i ++){
			if(MIME_StrTable[i][1].length() > 0 && (!mimeHashMap.containsKey(MIME_StrTable[i][1]))){
				mimeHashMap.put(MIME_StrTable[i][1],MIME_StrTable[i][0]);
			}
		}
		return mimeHashMap;
	}
	
	public static String getLocalIpAddress() {
		try {
			Enumeration<NetworkInterface> en = NetworkInterface
				.getNetworkInterfaces();
			while (en.hasMoreElements()) {
				NetworkInterface ni = en.nextElement();
				Enumeration<InetAddress> enIp= ni.getInetAddresses();
				while (enIp.hasMoreElements()) {
					InetAddress inet = enIp.nextElement();
					if (!inet.isLoopbackAddress()
						&& (inet instanceof Inet4Address)) {
						return inet.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}

		return "0";
	}
	public static byte[] byteMerger(byte[] bt1, byte[] bt2){  
        byte[] bt3 = new byte[bt1.length+bt2.length];  
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);  
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);  
        return bt3;  
    }
	
	public static String getExtensionByCutStr(String fileName){
		int lastIndexOfDot = fileName.lastIndexOf(".");
		if(lastIndexOfDot < 0){
			return "";//没有拓展名
		}
		String extension = fileName.substring(lastIndexOfDot+1);
		return extension;
	}
//创建以拓展名为key值的HashMap
	public static HashMap<String,String> CreateMIMEMapKeyIsExpands(){

		HashMap<String,String> mimeHashMap = new HashMap<String,String>();

		for(int i = 0; i < MIME_StrTable.length; i ++){
			if(MIME_StrTable[i][0].length() > 0 && (!mimeHashMap.containsKey(MIME_StrTable[i][0]))){
				mimeHashMap.put(MIME_StrTable[i][0],MIME_StrTable[i][1]);
			}
		}
		return mimeHashMap;
	}
//获取MIME列表的HashMap，设置Content-type为key值
	public static HashMap<String,String> getMIMEMapKeyIsContentType(){
		//为了防止重复创建消耗时间和消耗资源，将mimeMapKeyIsContentType设置全局变量并赋初值null
		if(mimeMapKeyIsContentType == null){
			mimeMapKeyIsContentType = CreateMIMEMapKeyIsContentType();
		}
		return mimeMapKeyIsContentType;
	}

//获取MIME列表的HashMap,设置拓展名为文件拓展名（含有"."）
	public static HashMap<String,String> getMIMEMapKeyIsExpands(){
		//为了防止重复创建消耗时间和消耗资源，将mimeMapKeyIsExpands设置全局变量并赋初值null
		if(mimeMapKeyIsExpands == null){
			mimeMapKeyIsExpands = CreateMIMEMapKeyIsExpands();
		}
		return mimeMapKeyIsExpands;
	}
	public static String getContentTypeByExpansion(String expansionName){
		String expansion = expansionName;
		if(!expansion.startsWith(".")){
			expansion = "." + expansion;
		}
		HashMap<String,String> expandMap = getMIMEMapKeyIsExpands();
		String contentType = expandMap.get(expansion);
		return contentType == null?"*/*":contentType; //当找不到的时候就会返回空
	}
	public static boolean isMainServiceRunning(Context context) {
	    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if ("net.newlydev.fileshare_android.MainService".equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	public static String EncoderByMd5(String buf) {
		try {
			MessageDigest digist = MessageDigest.getInstance("MD5");
			byte[] rs = digist.digest(buf.getBytes("UTF-8"));
			StringBuffer digestHexStr = new StringBuffer();
			for (int i = 0; i < 16; i++) {
				digestHexStr.append(byteHEX(rs[i]));
			}
			return digestHexStr.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String byteHEX(byte ib) {
		char[] Digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		char[] ob = new char[2];
		ob[0] = Digit[(ib >>> 4) & 0X0F];
		ob[1] = Digit[ib & 0X0F];
		String s = new String(ob);
		return s;
	}

	//产生随机字符串
	public static String getRandomString(int length){
		Random random=new Random();
		StringBuffer sb=new StringBuffer();
		for(int i=0; i<length; i++){
			int number=random.nextInt(3);
			long result=0;
			switch(number){
				case 0:
					result=Math.round(Math.random()*25+65);
					sb.append(String.valueOf((char)result));
					break;
				case 1:
					result=Math.round(Math.random()*25+97);
					sb.append(String.valueOf((char)result));
					break;
				case 2:
					sb.append(String.valueOf
							  (new Random().nextInt(10)));
					break; 
			}
		}
		return sb.toString();
	}
}
