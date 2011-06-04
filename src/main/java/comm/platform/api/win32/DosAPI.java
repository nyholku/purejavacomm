/*
 * Copyright (c) 2011 David Hoyt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list 
 * of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this 
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 *  
 * The names of any contributors may not be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
package comm.platform.api.win32;

import com.sun.jna.Native;
import comm.PortType;
import comm.util.StringUtil;
import java.util.Map;
import java.util.TreeMap;
import static comm.platform.api.win32.API.*;

/**
 * Calls representing the Win32 SetupAPI library. 
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface DosAPI extends com.sun.jna.Library {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String
		  LIBRARY_NAME = "kernel32"
	;
	
	public static final DosAPI
		INSTANCE = DosAPIDirect.loadLibrary()
	;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Util">
	public static class Util {
		public static Map<String, CommInfo> discoverCommNames() {
			return discoverCommNames(PortType.ANY);
		}
		
		public static Map<String, CommInfo> discoverCommNames(PortType filter) {
			return discoverCommNames(null, filter);
		}
		
		public static Map<String, CommInfo> discoverCommNames(DosAPI API, PortType filter) {
			if (API == null)
				API = DosAPI.INSTANCE;
			
			final int MAX_BUFFER_SIZE = 256 * 1024;
			final int BUFFER_EXPAND_BY = 1024;
			char[] buffer = new char[16384];
			int charsUsed = 0;
			boolean success = false;

			//Keep growing the dos device buffer up till a max size.
			do {
				charsUsed = API.QueryDosDevice(null, buffer, buffer.length);

				if (charsUsed > 0 || Native.getLastError() != ERROR_INSUFFICIENT_BUFFER) {
					success = true;
					break;
				}

				if (buffer.length >= MAX_BUFFER_SIZE) {
					success = false;
					break;
				}

				buffer = new char[buffer.length + BUFFER_EXPAND_BY];
			} while (true);

			if (!success)
				return null;

			int index = 0;
			StringBuilder sb = new StringBuilder(256);
			String deviceName = StringUtil.empty;
			PortType devicePortType = PortType.UNKNOWN;
			Map<String, CommInfo> deviceList = new TreeMap<String, CommInfo>();

			//Search through the list which is a bunch of entries delimited by null '\0' characters with a null '\0' at the end (so the last entry has 2 nulls).
			while(index < charsUsed && buffer[index] != '\0') {
				//Only look at serial/parallel ports
				if (buffer[index] == 'C' || buffer[index] == 'c' || buffer[index] == 'L' || buffer[index] == 'l') {
					while(index < charsUsed && buffer[index] != '\0') {
						sb.append(buffer[index]) ;
						++index;
					}
					deviceName = sb.toString();
					//Reset the string builder.
					sb.setLength(0);

					if (comm.platform.api.win32.API.Util.isSerialPortNameMatch(deviceName))
						devicePortType = PortType.SERIAL;
					else if (comm.platform.api.win32.API.Util.isParallelPortNameMatch(deviceName))
						devicePortType = PortType.PARALLEL;
					else
						devicePortType = PortType.UNKNOWN;

					if (PortType.isInFilter(devicePortType, filter))
						deviceList.put(deviceName, new CommInfo(deviceName, devicePortType));
				} else {
					while(index < charsUsed && buffer[index] != '\0')
						++index;
				}
				++index;
			}
			sb.setLength(0);
			return deviceList;
		}
		
		public static class CommInfo {
			private String name;
			private PortType portType;
			
			public CommInfo(String name, PortType portType) {
				this.name = name;
				this.portType = portType;
			}
			
			public String getName() {
				return name;
			}
			
			public PortType getPortType() {
				return portType;
			}
		}
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="API">
	//<editor-fold defaultstate="collapsed" desc="Constants">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Types">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Structs">
	//</editor-fold>
	
	int QueryDosDevice(String name, char[] buffer, int bsize);
	//</editor-fold>
}
