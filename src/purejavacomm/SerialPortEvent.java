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

import java.util.EventObject;

public class SerialPortEvent extends EventObject {
	/**
	 * Data available at the serial port.
	 */
	public static final int DATA_AVAILABLE = 1;
	/**
	 * Output buffer is empty.
	 */
	public static final int OUTPUT_BUFFER_EMPTY = 2;
	/**
	 * Clear to send.
	 */
	public static final int CTS = 3;
	/**
	 * Data set ready.
	 */
	public static final int DSR = 4;
	/**
	 * Ring indicator.
	 */
	public static final int RI = 5;
	/**
	 * Carrier detect.
	 */
	public static final int CD = 6;
	/**
	 * Overrun error.
	 */
	public static final int OE = 7;
	/**
	 * Parity error.
	 */
	public static final int PE = 8;
	/**
	 * Framing error.
	 */
	public static final int FE = 9;
	/**
	 * Break interrupt.
	 */
	public static final int BI = 10;

	private final int eventType;
	private final boolean newValue;
	private final boolean oldValue;

	/**
	 * Constructs a <CODE>SerialPortEvent</CODE> with the specified serial port,
	 * event type, old and new values. Application programs should not directly
	 * create <CODE>SerialPortEvent</CODE> objects.
	 * 
	 * @param source
	 * @param eventType
	 * @param oldValue
	 * @param newValue
	 */
	public SerialPortEvent(SerialPort source, int eventType, boolean oldValue, boolean newValue) {
		super(source);
		this.eventType = eventType;
		this.newValue = newValue;
		this.oldValue = oldValue;
	}

	/**
	 * Returns the type of this event.
	 * 
	 * @return The type of this event.
	 */
	public int getEventType() {
		return this.eventType;
	}

	/**
	 * Returns the new value of the state change that caused the
	 * <CODE>SerialPortEvent</CODE> to be propagated.
	 * 
	 * @return The new value of the state change.
	 */
	public boolean getNewValue() {
		return this.newValue;
	}

	/**
	 * Returns the old value of the state change that caused the
	 * <CODE>SerialPortEvent</CODE> to be propagated.
	 * 
	 * @return The old value of the state change.
	 */
	public boolean getOldValue() {
		return this.oldValue;
	}
}
