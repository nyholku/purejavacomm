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
package comm;

import comm.platform.Sys;
import comm.util.StringUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides access to system serial ports.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class SerialPort {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private static Implementation impl;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		switch(Sys.getOSFamily()) {
			case Windows:
				impl = new comm.platform.dev.win32.APISerialPortImplementation();
				break;
			default:
				impl = null;
		}
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Interfaces">
	/**
	 * Used as a guide for platform-specific implementations.
	 */
	public static interface Implementation {
		int getDefaultBaudRate();
		
		int[] getSystemBaudRateOptions();
		DataBits[] getSystemDataBitsOptions();
		StopBits[] getSystemStopBitsOptions();
		FlowControl[] getSystemFlowControlOptions();
		Parity[] getSystemParityOptions();
		
		void visitAvailableSerialPorts(IVisitor visitor);
		void addSystemHint(final String name, final Object value);
		<T> T findSystemHint(final String name);
	}
	
	/**
	 * Used as a callback into the discovered list of ports.
	 */
	public static interface IVisitor {
		/**
		 * Called for every port found.
		 * 
		 * @param SerialPort The associated found port.
		 * @return True if you want the callbacks to continue. False to short circuit the callback.
		 */
		boolean visit(ISerialPort SerialPort);
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	/**
	 * Examine each found port via the {@link IVisitor#visit(comm.ISerialPort) visit()} callback.
	 * @param visitor The object that will handle the callback.
	 */
	public static void visitAvailableSerialPorts(IVisitor visitor) {
		if (visitor == null || impl == null)
			return;
		impl.visitAvailableSerialPorts(visitor);
	}
	
	/**
	 * Retrieves the list of available ports by visiting each one and accumulating references.
	 * 
	 * @return An array of valid, available ports.
	 */
	public static ISerialPort[] getAvailableSerialPorts() {
		if (impl == null)
			return null;
		
		final List<ISerialPort> lst = new ArrayList<ISerialPort>(4);
		visitAvailableSerialPorts(new IVisitor() {
			@Override
			public boolean visit(ISerialPort SerialPort) {
				lst.add(SerialPort);
				return true;
			}
		});
		
		if (!lst.isEmpty())
			return lst.toArray(new ISerialPort[lst.size()]);
		else
			return ISerialPort.EMPTY_SERIAL_PORTS;
	}
	
	/**
	 * Searches through the ports looking for one with the given name.
	 * 
	 * @param name The name of the port such as "COM1" or "LPT1".
	 * @return The instance of the port if found, null otherwise.
	 */
	public static ISerialPort find(final String name) {
		if (StringUtil.isNullOrEmpty(name))
			return null;
		final ISerialPort[] finder = new ISerialPort[1];
		visitAvailableSerialPorts(new IVisitor() {
			@Override
			public boolean visit(ISerialPort SerialPort) {
				if (name.equalsIgnoreCase(SerialPort.getName())) {
					finder[0] = SerialPort;
					//We can exit since we found what we were looking for.
					return false;
				}
				return true;
			}
		});
		//Will return null if it couldn't find the serial port.
		return finder[0];
	}
	
	/**
	 * Provides a way to augment/modify system-specific behavior such as the 
	 * number of IO completion port threads on Windows.
	 * 
	 * @param name The name of the hint.
	 * @param value The value to assign the hint.
	 */
	public static void addSystemHint(final String name, final Object value) {
		if (impl == null)
			return;
		impl.addSystemHint(name, value);
	}
	
	/**
	 * Gets the system default baud rate.
	 * 
	 * @return An integer representing the system default baud rate.
	 */
	public static int getSystemDefaultBaudRate() {
		if (impl == null)
			return 0;
		return impl.getDefaultBaudRate();
	}
	
	/**
	 * Gets the system standard length of data bits per byte.
	 * 
	 * @return The default value for the system.
	 */
	public static DataBits getSystemDefaultDataBits() {
		return DataBits.getDefault();
	}
	
	/**
	 * Gets the system standard flow control methods.
	 * 
	 * @return The default value for the system.
	 */
	public static FlowControl[] getSystemDefaultFlowControl() {
		return FlowControl.getDefault();
	}
	
	/**
	 * Gets the system standard stop bits.
	 * 
	 * @return The default value for the system.
	 */
	public static StopBits getSystemDefaultStopBits() {
		return StopBits.getDefault();
	}
	
	/**
	 * Gets the system standard parity.
	 * 
	 * @return The default value for the system.
	 */
	public static Parity getSystemDefaultParity() {
		return Parity.getDefault();
	}
	
	/**
	 * Requests a list of valid baud rates that the system allows.
	 * 
	 * @return An integer array containing valid system baud rates.
	 */
	public static int[] getSystemBaudRateOptions() {
		if (impl == null)
			return new int[0];
		return impl.getSystemBaudRateOptions();
	}
	
	/**
	 * Requests a list of valid data bit values that the system allows.
	 * 
	 * @return The default value for the system.
	 */
	public static DataBits[] getSystemDataBitsOptions() {
		if (impl == null)
			return DataBits.values();
		return impl.getSystemDataBitsOptions();
	}
	
	/**
	 * Requests a list of valid stop bit values that the system allows.
	 * 
	 * @return The default value for the system.
	 */
	public static StopBits[] getSystemStopBitsOptions() {
		if (impl == null)
			return StopBits.values();
		return impl.getSystemStopBitsOptions();
	}
	
	/**
	 * Requests a list of valid flow control values that the system allows.
	 * 
	 * @return The default value for the system.
	 */
	public static FlowControl[] getSystemFlowControlOptions() {
		if (impl == null)
			return FlowControl.values();
		return impl.getSystemFlowControlOptions();
	}
	
	/**
	 * Requests a list of valid parity values that the system allows.
	 * 
	 * @return The default value for the system.
	 */
	public static Parity[] getSystemParityOptions() {
		if (impl == null)
			return Parity.values();
		return impl.getSystemParityOptions();
	}
	//</editor-fold>
}
