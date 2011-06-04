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
package comm.platform.dev.win32;

import comm.PortType;
import comm.platform.api.win32.DosAPI;
import comm.platform.api.win32.SetupAPI;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class APIParallelPortImplementation implements comm.ParallelPort.Implementation {
	//<editor-fold defaultstate="collapsed" desc="Init">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	private static final Object cacheLock = new Object();
	private static final Map<String, ParallelPort> cache = new TreeMap<String, ParallelPort>();
	
	@Override
	public void visitAvailableParallelPorts(comm.ParallelPort.IVisitor visitor) {
		//This is expected to be called, actually, from within a STATIC method call.
		//It actually comes from the SerialPort.Implementation interface.
		//
		//We try and maintain existing references as much as possible. That is, if the ports haven't really changed in between 
		//calls, then be sure and return the same objects as before
		
		if (visitor == null)
			return;
		
		//Get a list of ports such as "COM1, COM2, COM3, LPT1" etc.
		Map<String, DosAPI.Util.CommInfo> ports = DosAPI.Util.discoverCommNames(PortType.PARALLEL);
		if (ports == null || ports.size() <= 0)
			return;
		
		//Get "friendly names" such as "Communications Port (COM1)" etc.
		Map<String, SetupAPI.Util.CommDetails> details = SetupAPI.Util.discoverCommDetails(PortType.PARALLEL);
		
		//Merge the lists
		ParallelPort inst = null;
		DosAPI.Util.CommInfo instInfo = null;
		SetupAPI.Util.CommDetails instDetails = null;
		
		synchronized(cacheLock) {
			for(String portName : ports.keySet()) {
				//Locate its info.
				instInfo = ports.get(portName);
				if (instInfo == null)
					continue;
				
				//Locate details if there are any.
				instDetails = details.get(portName);
				
				//Does this guy exist in the cache?
				if ((inst = cache.get(portName)) == null) {
					cache.put(
						portName, 
						(inst = 
							(instDetails != null ? 
								  new ParallelPort(
									instInfo.getName(), 
									instDetails.getFriendlyName(), 
									instDetails.getDescription(), 
									instInfo.getPortType()
								  )
								: new ParallelPort(
									instInfo.getName(), 
									instInfo.getName(), 
									instInfo.getName(), 
									instInfo.getPortType()
								  )
							)
						)
					);
				} else {
					if (instDetails != null) {
						inst.update(
							instInfo.getName(), 
							instDetails.getFriendlyName(), 
							instDetails.getDescription(), 
							instInfo.getPortType()
						);
					} else {
						inst.update(
							instInfo.getName(), 
							instInfo.getName(), 
							instInfo.getName(), 
							instInfo.getPortType()
						);
					}
				}
				
				if (inst == null)
					continue;
				
				if (!visitor.visit(inst))
					break;
			}
		}
	}
	
	@Override
	public void addSystemHint(String name, Object value) {
		SystemHint.add(name, value);
	}
	
	@Override
	public <T> T findSystemHint(String name) {
		return SystemHint.hint(name);
	}
	
	@Override
	public int[] getBaudRates() {
		return BaudRates.ValidBaudRates;
	}
	
	@Override
	public int getDefaultBaudRate() {
		return BaudRates.DEFAULT_BAUD_RATE;
	}
	//</editor-fold>
}
