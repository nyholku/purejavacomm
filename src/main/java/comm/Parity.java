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
 * The method for detecting errors in transmission.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum Parity {
	  /**
	   * No parity bit.
	   */
	  NONE(ISerialPort.PARITY_NONE)
	  /**
	   * Odd parity scheme. The parity bit is added so there are an odd number of
	   * TRUE bits.
	   */
	, ODD (ISerialPort.PARITY_ODD)
	  /**
	   * Even parity scheme. The parity bit is added so there are an even number
	   * of TRUE bits.
	   */
	, EVEN(ISerialPort.PARITY_EVEN)
	  /**
	   * Mark parity scheme.
	   */
	, MARK(ISerialPort.PARITY_MARK)
	  /**
	   * Space parity scheme.
	   */
	, SPACE(ISerialPort.PARITY_SPACE)
	;
	
	//<editor-fold defaultstate="collapsed" desc="Init">
	private int value;
	
	private Parity(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static Parity getDefault() {
		return Parity.NONE;
	}
	
	public static int toValue(Parity value) {
		return value.value;
	}
	
	public static Parity fromValue(int value) {
		for(Parity e : values())
			if (e.value == value)
				return e;
		return getDefault();
	}
	//</editor-fold>
}
