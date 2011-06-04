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
 * Describes the types of communication port objects available.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum PortType {
	  /**
	   * The type of port is unrecognizable.
	   */
	  UNKNOWN   (0)
	  /**
	   * It could be serial, parallel, or anything except unknown.
	   */
	, ANY       (1)
	
	  /**
	   * The communication port is a serial port.
	   */
	, SERIAL    (2)
	  /**
	   * The communication port is a parallel port.
	   */
	, PARALLEL  (3)
	;
	
	//<editor-fold defaultstate="collapsed" desc="Init">
	private int value;
	
	private PortType(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public boolean isUnknown() {
		return (this == UNKNOWN);
	}
	
	public static boolean isInFilter(PortType value, PortType filter) {
		switch(filter) {
			case ANY:
				return !value.isUnknown();
			case UNKNOWN:
				return value.isUnknown();
			default:
				return (value == filter);
		}
	}
	//</editor-fold>
}
