package com.noterik.bart.fs.fscommand.dynamic.presentation.playout;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import com.noterik.bart.fs.LazyHomer;

public class CacheMulticastSender {
	
	static String group = "224.0.0.0";
	static int ttl = 1;
	
	public static synchronized void send(String adr,String method, String imsg) {
		try {
			MulticastSocket s = new MulticastSocket();
			String msg = adr+" "+method+" "+imsg+" "+LazyHomer.getPort();
			//System.out.println("SENDING=> "+msg);
			byte[] buf = msg.getBytes();
			DatagramPacket pack = new DatagramPacket(buf, buf.length,InetAddress.getByName(group), LazyHomer.getPort());
			s.send(pack,(byte)ttl);
			s.close();
		} catch(Exception e) {
			System.out.println("Smithers : multicast sender error");
		}
	}

}
