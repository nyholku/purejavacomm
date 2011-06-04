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

/**
 * Provides access to system serial ports.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface ISerialPort extends ICommPort {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	/**
	 * Describes a state where no serial ports are available on the system.
	 */
	public static final ISerialPort[] EMPTY_SERIAL_PORTS = { };
	/**
	 * 5 data bit format.
	 */
	public static final int DATABITS_5 = 5;
	/**
	 * 6 data bit format.
	 */
	public static final int DATABITS_6 = 6;
	/**
	 * 7 data bit format.
	 */
	public static final int DATABITS_7 = 7;
	/**
	 * 8 data bit format.
	 */
	public static final int DATABITS_8 = 8;
	/**
	 * No parity bit.
	 */
	public static final int PARITY_NONE = 0;
	/**
	 * Odd parity scheme. The parity bit is added so there are an odd number of
	 * TRUE bits.
	 */
	public static final int PARITY_ODD = 1;
	/**
	 * Even parity scheme. The parity bit is added so there are an even number
	 * of TRUE bits.
	 */
	public static final int PARITY_EVEN = 2;
	/**
	 * Mark parity scheme.
	 */
	public static final int PARITY_MARK = 3;
	/**
	 * Space parity scheme.
	 */
	public static final int PARITY_SPACE = 4;
	/**
	 * One stop bit.
	 */
	public static final int STOPBITS_1 = 1;
	/**
	 * Two stop bits.
	 */
	public static final int STOPBITS_2 = 2;
	/**
	 * One and 1/2 stop bits. Some UARTs permit 1-1/2 stop bits only with 5 data
	 * bit format, but permit 1 or 2 stop bits with any format.
	 */
	public static final int STOPBITS_1_5 = 3;
	/**
	 * Flow control off.
	 */
	public static final int FLOWCONTROL_NONE = 0;
	/**
	 * RTS/CTS flow control on input.
	 */
	public static final int FLOWCONTROL_RTSCTS_IN = 1;
	/**
	 * RTS/CTS flow control on output.
	 */
	public static final int FLOWCONTROL_RTSCTS_OUT = 2;
	/**
	 * XON/XOFF flow control on input.
	 */
	public static final int FLOWCONTROL_XONXOFF_IN = 4;
	/**
	 * XON/XOFF flow control on output.
	 */
	public static final int FLOWCONTROL_XONXOFF_OUT = 8;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Interfaces">
	public static interface IConfiguration {
		int getBaudRate();
		DataBits getDataBits();
		StopBits getStopBits();
		Parity getParity();
	}
	//</editor-fold>
	
	int getBaudRate();
	DataBits getDataBits();
	StopBits getStopBits();
	Parity getParity();
	FlowControl[] getFlowControl();
	int getFlowControlFlag();
	
	boolean configure(IConfiguration configuration);
	boolean configure(int baudRate, int dataBits, int stopBits, int parity);
	boolean configure(int baudRate, DataBits dataBits, StopBits stopBits, Parity parity);
	
	boolean changeFlowControl(int flag);
	boolean changeFlowControl(FlowControl...flowControl);
}
