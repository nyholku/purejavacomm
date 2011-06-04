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

import java.util.ArrayList;
import java.util.List;

/**
 * Serial port methods for controlling the pausing and transmission of data.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum FlowControl {
	  /**
	   * Flow control off.
	   */
	  NONE(ISerialPort.FLOWCONTROL_NONE)
	  /**
	   * RTS/CTS flow control on input.
	   */
	, RTSCTS_IN(ISerialPort.FLOWCONTROL_RTSCTS_IN)
	
	  /**
	   * RTS/CTS flow control on output.
	   */
	, RTSCTS_OUT(ISerialPort.FLOWCONTROL_RTSCTS_OUT)
	  /**
	   * XON/XOFF flow control on input.
	   */
	, XONXOFF_IN(ISerialPort.FLOWCONTROL_XONXOFF_IN)
	  /**
	   * XON/XOFF flow control on output.
	   */
	, XONXOFF_OUT(ISerialPort.FLOWCONTROL_XONXOFF_OUT)
	;
	
	//<editor-fold defaultstate="collapsed" desc="Init">
	private int value;
	private static final FlowControl[] def = { NONE };
	
	private FlowControl(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public boolean isInFlag(int value) {
		return isFlagged(value, this);
	}
	
	public static FlowControl[] getDefault() {
		return def;
	}
	
	public static int asFlag(FlowControl...values) {
		int value = 0;
		for(int i = 0; i < values.length; ++i)
			value |= values[i].value;
		return value;
	}
	
	public static FlowControl[] fromFlag(int value) {
		List<FlowControl> flags = new ArrayList<FlowControl>(5);
		for(FlowControl e : values())
			if ((value & e.value) == e.value)
				flags.add(e);
		return flags.toArray(new FlowControl[flags.size()]);
	}
	
	public static boolean isFlagged(int value, FlowControl f) {
		return ((value & f.value) == f.value);
	}
	//</editor-fold>
}
