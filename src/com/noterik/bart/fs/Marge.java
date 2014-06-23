package com.noterik.bart.fs;

import java.net.InetAddress;

import com.noterik.bart.fs.fscommand.dynamic.presentation.playout.CacheMulticastSender;

public class Marge {
	
	public static String myip = "unknown";
	
	public static void signalRemote(String ip,String method,String url) {
		// ugly daniel
		try{
			InetAddress mip=InetAddress.getLocalHost();
			myip = ""+mip.getHostAddress();
		}catch (Exception e){
			System.out.println("Exception ="+e.getMessage());
		}
		
		//System.out.println("ip="+ip+" method="+method+" S2="+url);
		if (url.equals("/domain/internal/service/getname")) {
			String sends = myip+":"+LazyHomer.getSmithersPort()+":"+LazyHomer.getPort();
			CacheMulticastSender.send(sends, "INFO", "ALIVE");
			//System.out.println("SIGNAL ALIVE");
		}
	}

}
