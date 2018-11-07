/*
 * Copyright (c) 2011, Kustaa Nyholm / SpareTimeLabs
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
 * Neither the name of the Kustaa Nyholm or SpareTimeLabs nor the names of its 
 * contributors may be used to endorse or promote products derived from this software 
 * without specific prior written permission.
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

package purejavacomm;

import java.io.IOException;
import java.io.InputStream;
import java.util.TooManyListenersException;

abstract public class SerialPort extends CommPort {
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

	public SerialPort() {
	}

	/**
	 * Registers a <CODE>SerialPortEventListener</CODE> object to listen for
	 * <CODE>SerialEvent</CODE>s.
	 * 
	 * @param listener
	 * @throws TooManyListenersException
	 */
	public abstract void addEventListener(SerialPortEventListener listener) throws TooManyListenersException;

	/**
	 * Returns the currently configured baud rate.
	 * 
	 * @return The currently configured baud rate.
	 */
	public abstract int getBaudRate();

	/**
	 * Returns the currently configured number of data bits.
	 * 
	 * @return The currently configured number of data bits.
	 */
	public abstract int getDataBits();

	/**
	 * Returns the currently configured flow control mode.
	 * 
	 * @return The currently configured flow control mode.
	 */
	public abstract int getFlowControlMode();

	/**
	 * Returns the currently configured parity setting.
	 * 
	 * @return The currently configured parity setting.
	 */
	public abstract int getParity();

	/**
	 * Returns the currently defined stop bits.
	 * 
	 * @return The currently defined stop bits.
	 */
	public abstract int getStopBits();

	/**
	 * Returns the state of the CD (Carrier Detect) bit in the UART, if
	 * supported by the underlying implementation.
	 * 
	 * @return The state of the CD (Carrier Detect) bit.
	 */
	public abstract boolean isCD();

	/**
	 * Returns the state of the CTS (Clear To Send) bit in the UART, if
	 * supported by the underlying implementation.
	 * 
	 * @return The state of the CTS (Clear To Send) bit.
	 */
	public abstract boolean isCTS();

	/**
	 * Returns the state of the DSR (Data Set Ready) bit in the UART, if
	 * supported by the underlying implementation.
	 * 
	 * @return The state of the DSR (Data Set Ready) bit.
	 */
	public abstract boolean isDSR();

	/**
	 * Returns the state of the DTR (Data Terminal Ready) bit in the UART, if
	 * supported by the underlying implementation.
	 * 
	 * @return The state of the DTR (Data Terminal Ready) bit.
	 */
	public abstract boolean isDTR();

	/**
	 * Returns the state of the RI (Ring Indicator) bit in the UART, if
	 * supported by the underlying implementation.
	 * 
	 * @return The state of the RI (Ring Indicator) bit.
	 */
	public abstract boolean isRI();

	/**
	 * Returns the state of the RTS (Request To Send) bit in the UART, if
	 * supported by the underlying implementation.
	 * 
	 * @return The state of the RTS (Request To Send) bit.
	 */
	public abstract boolean isRTS();

	/**
	 * Expresses interest in receiving notification when there is a break
	 * interrupt on the line.
	 * 
	 * @param enable
	 */
	public abstract void notifyOnBreakInterrupt(boolean enable);

	/**
	 * Expresses interest in receiving notification when the CD (Carrier Detect)
	 * bit changes.
	 * 
	 * @param enable
	 */
	public abstract void notifyOnCarrierDetect(boolean enable);

	/**
	 * Expresses interest in receiving notification when the CTS (Clear To Send)
	 * bit changes.
	 * 
	 * @param enable
	 */
	public abstract void notifyOnCTS(boolean enable);

	/**
	 * Expresses interest in receiving notification when input data is
	 * available.
	 * 
	 * @param enable
	 */
	public abstract void notifyOnDataAvailable(boolean enable);

	/**
	 * Expresses interest in receiving notification when the DSR (Data Set
	 * Ready) bit changes.
	 * <P>
	 * This notification is hardware dependent and may not be supported by all
	 * implementations.
	 * 
	 * @param enable
	 */
	public abstract void notifyOnDSR(boolean enable);

	/**
	 * Expresses interest in receiving notification when there is a framing
	 * error.
	 * 
	 * @param enable
	 */
	public abstract void notifyOnFramingError(boolean enable);

	/**
	 * Expresses interest in receiving notification when the output buffer is
	 * empty.
	 * 
	 * @param enable
	 */
	public abstract void notifyOnOutputEmpty(boolean enable);

	/**
	 * Expresses interest in receiving notification when there is an overrun
	 * error.
	 * 
	 * @param enable
	 */
	public abstract void notifyOnOverrunError(boolean enable);

	/**
	 * Expresses interest in receiving notification when there is a parity
	 * error.
	 * 
	 * @param enable
	 */
	public abstract void notifyOnParityError(boolean enable);

	/**
	 * Expresses interest in receiving notification when the RI (Ring Indicator)
	 * bit changes.
	 * 
	 * @param enable
	 */
	public abstract void notifyOnRingIndicator(boolean enable);

	/**
	 * Deregisters event listener registered using <CODE>addEventListener</CODE>
	 * .
	 * <P>
	 * This is done automatically when the port is closed.
	 */
	public abstract void removeEventListener();

	/**
	 * Sends a break of <CODE>duration</CODE> milliseconds duration.
	 * 
	 * @param duration
	 *            The break duration in milliseconds.
	 */
	public abstract void sendBreak(int duration);

	/**
	 * Sets or clears the DTR (Data Terminal Ready) signal, if supported by the
	 * underlying implementation.
	 * 
	 * @param state
	 */
	public abstract void setDTR(boolean state);

	/**
	 * Sets the flow control mode.
	 * 
	 * @param flowcontrol
	 * @throws UnsupportedCommOperationException
	 */
	public abstract void setFlowControlMode(int flowcontrol) throws UnsupportedCommOperationException;

	/**
	 * Sets or clears the RTS (Request To Send) bit in the UART, if supported by
	 * the underlying implementation.
	 * 
	 * @param state
	 */
	public abstract void setRTS(boolean state);

	/**
	 * Sets the serial port parameters.
	 * <P>
	 * Default: 9600 baud, 8 data bits, 1 stop bit, no parity.
	 * 
	 * @param baudRate
	 * @param dataBits
	 * @param stopBits
	 * @param parity
	 * @throws UnsupportedCommOperationException
	 */
	public abstract void setSerialPortParams(int baudRate, int dataBits, int stopBits, int parity) throws UnsupportedCommOperationException;

	
	/**
	 * Returns an input stream. This is the only way to receive data from the
	 * communications port. If the port is unidirectional and doesn't support
	 * receiving data, then getInputStream returns null.
	 * 
	 * The read behavior of the input stream returned by getInputStream depends
	 * on combination of the threshold and timeout values. The behaviors are
	 * described in the table below.
	 * 
	 * <p>
	 * 
	 * <table summary="" border="3" cellpadding="4">
	 * <tr>
	 * <th colspan="2" align="center">threshold</th>
	 * <th colspan="2"align="center" >timeout</th>
	 * <th rowspan="2" align="center">read buffer size</th>
	 * <th rowspan="2" align="center">read behaviour</th>
	 * </tr>
	 * <tr>
	 * <th align="center">state</th>
	 * <th align="center">value</th>
	 * <th align="center">state</th>
	 * <th align="center">value</th>
	 * </tr>
	 * <tr>
	 * <td align="center">disabled</td>
	 * <td align="center">-</td>
	 * <td align="center">disabled</td>
	 * <td align="center">-</td>
	 * <td align="center">n bytes</td>
	 * <td align="left">block until minimum one byte of data is available</td>
	 * </tr>
	 * <tr>
	 * <td align="center">enabled</td>
	 * <td align="center">m bytes</td>
	 * <td align="center">disabled</td>
	 * <td align="center">-</td>
	 * <td align="center">n bytes</td>
	 * <td align="left">block until min(m,n) bytes are available</td>
	 * </tr>
	 * <tr>
	 * <td align="center">disabled</td>
	 * <td align="center">-</td>
	 * <td align="center">enabled</td>
	 * <td align="center">x msec</td>
	 * <td align="center">n bytes</td>
	 * <td align="left">block for x msec or until any data is available</td>
	 * </tr>
	 * <tr>
	 * <td align="center">enabled</td>
	 * <td align="center">m bytes</td>
	 * <td align="center">enabled</td>
	 * <td align="center">x msec</td>
	 * <td align="center">n bytes</td>
	 * <td align="center">block for x msec or until min(m,n) bytes are available
	 * </td>
	 * </tr>
	 * </table>
	 * <p>
	 * Framing errors may cause the Timeout and Threshold trigger early and to
	 * complete the read prematurely without raising an exception.
	 * <p>
	 * Enabling the Timeout OR Threshold with a value a zero is a special case.
	 * This causes the underlying driver to poll for incoming data instead being
	 * event driven. Otherwise, the behaviour is identical to having both the
	 * Timeout and Threshold disabled. Returns: InputStream object that can be
	 * used to read from the port Throws: java.io.IOException - if an I/O error
	 * occurred.
	 * <p>
	 * Timeout is interpreted as inter character timeout, in other words
	 * the timeout will not occur as long as the pause before the first
	 * character or between characters is shorter that the timeout value.
	 * <p>
	 * 
	 */
	
	public abstract InputStream getInputStream() throws IOException;


}
