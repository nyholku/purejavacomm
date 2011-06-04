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
 * Specifies the number of stop bits to use.
 * 
 * From Wikipedia (http://en.wikipedia.org/wiki/Serial_port):
 * 
 * Stop bits sent at the end of every character allow the receiving signal hardware to detect 
 * the end of a character and to resynchronise with the character stream. Electronic devices 
 * usually use one stop bit. If slow electromechanical teleprinters are used, one-and-one half 
 * or two stop bits are required.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum StopBits {
	  /**
	   * One stop bit.
	   */
	  STOPBITS_1(ISerialPort.STOPBITS_1)
	
	  /**
	   * Two stop bits.
	   */
	, STOPBITS_2(ISerialPort.STOPBITS_2)
	  /**
	   * One and 1/2 stop bits. Some UARTs permit 1-1/2 stop bits only with 5 data
	   * bit format, but permit 1 or 2 stop bits with any format.
	   */
	, STOPBITS_1_5(ISerialPort.STOPBITS_1_5)
	;
	
	//<editor-fold defaultstate="collapsed" desc="Init">
	private int value;
	
	private StopBits(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static StopBits getDefault() {
		return StopBits.STOPBITS_1;
	}
	
	public static int toValue(StopBits value) {
		return value.value;
	}
	
	public static StopBits fromValue(int value) {
		for(StopBits e : values())
			if (e.value == value)
				return e;
		return getDefault();
	}
	//</editor-fold>
}
