/* 
* Marge.java
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
