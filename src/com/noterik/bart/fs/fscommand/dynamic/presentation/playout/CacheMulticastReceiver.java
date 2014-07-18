/* 
* CacheMulticastReceiver.java
* 
* Copyright (c) 2012 Noterik B.V.
* 
* This file is part of smithers, related to the Noterik Springfield project.
*
* Smithers is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Smithers is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Smithers.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.noterik.bart.fs.fscommand.dynamic.presentation.playout;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import com.ibm.icu.util.StringTokenizer;
import com.noterik.bart.fs.LazyHomer;
import com.noterik.bart.fs.Marge;

public class CacheMulticastReceiver extends Thread {
	private volatile boolean running = true; 
	String group = "224.0.0.0";
	int errorcounter = 0;
	int errorcounter2 = 0;
	
	public CacheMulticastReceiver(String name) {
		super(name);
	}
	
	public void run() {
		try {
			MulticastSocket s = new MulticastSocket(LazyHomer.getPort());
			s.joinGroup(InetAddress.getByName(group));
			byte[] buffer = new byte[1024];
			DatagramPacket dp = new DatagramPacket(buffer, 1024);
			while (running) {
				try {
					dp.setLength(1024);
					s.receive(dp);
					byte[] message = new byte[dp.getLength()];
					System.arraycopy(dp.getData(), 0, message, 0, dp.getLength());
					//System.out.println("Smithers : RECEIVED=> " + dp.getLength() + " bytes from " + dp.getAddress()+" MSG="+new String(message));
					String line = new String(message);
					String[] result = new String(message).split(" ");	
					//System.out.println("SMITHERS MULTICAST RECEIVER "+line);
					String from = result[0];
					if (result.length>3) {
						String portcheck = result[result.length-1];
						if ((""+LazyHomer.getPort()).equals(portcheck)) {
							//System.out.println("MM PORT WANTED="+portcheck+" OK");
							if (from.indexOf("/")!=-1) {
								from = from.substring(from.indexOf("/")+1);
							}
							// doesn't send 'complex' link messsages correctly yet
							if (!result[1].equals("INFO")) {
								cache.signalRemote(from,result[1],result[2]);
							} else {
								Marge.signalRemote(from,result[1],result[2]);
							}
						} else {
							System.out.println("Smithers :  PORT WANTED="+portcheck+" DIFFERENT "+LazyHomer.getPort());
						}
					} else {
					
						if (from.indexOf("/")!=-1) {
							from = from.substring(from.indexOf("/")+1);
						}
						if (!result[1].equals("INFO")) {
							cache.signalRemote(from,result[1],result[2]);
						} else {
							Marge.signalRemote(from,result[1],result[2]);
						}
					}
				} catch(Exception e2) {
					if (errorcounter<10) {
						errorcounter++;
						System.out.println("ERROR Multicast innerloop");
						e2.printStackTrace();
					}
				}
			}
		} catch(Exception e) {
			if (errorcounter2<10) {
				errorcounter2++;
				System.out.println("ERROR Multicast outerloop");
				e.printStackTrace();
			}
		}
		System.out.println("Smithers: shutting down CacheMulticastReceiver");
	}
	
	 public void destroy() {
	    running = false;
	    this.interrupt();
	 }
}
