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
 * Describes the standard length of data bits per byte.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum DataBits {
	  /**
	   * 5 data bit format.
	   */
	  DATABITS_5(ISerialPort.DATABITS_5)
	  /**
	   * 6 data bit format.
	   */
	, DATABITS_6(ISerialPort.DATABITS_6)
	
	  /**
	   * 7 data bit format.
	   */
	, DATABITS_7(ISerialPort.DATABITS_7)
	  /**
	   * 8 data bit format.
	   */
	, DATABITS_8(ISerialPort.DATABITS_8)
	;
	
	//<editor-fold defaultstate="collapsed" desc="Init">
	private int value;
	
	private DataBits(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static DataBits getDefault() {
		return DataBits.DATABITS_8;
	}
	
	public static int toValue(DataBits value) {
		return value.value;
	}
	
	public static DataBits fromValue(int value) {
		for(DataBits e : values())
			if (e.value == value)
				return e;
		return getDefault();
	}
	//</editor-fold>
}
