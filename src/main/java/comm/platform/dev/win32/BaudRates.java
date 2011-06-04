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

import comm.platform.dev.StandardBaudRate;

/**
 * Provides details on valid Windows baud rates.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class BaudRates {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final int 
		  B7200             = 7200 //Win-only
		, B14400            = 14400//Win-only
		, B28800            = 28800//Win-only
		, B76800            = 76800 //Win-only
	;
	
	public static final int 
		DEFAULT_BAUD_RATE   = StandardBaudRate.B9600
	;
	//</editor-fold>
	
	public static final int[] ValidBaudRates = {
		  StandardBaudRate.B0
		, StandardBaudRate.B50
		, StandardBaudRate.B75
		, StandardBaudRate.B110
		, StandardBaudRate.B134
		, StandardBaudRate.B150
		, StandardBaudRate.B200
		, StandardBaudRate.B300
		, StandardBaudRate.B600
		, StandardBaudRate.B1200
		, StandardBaudRate.B1800
		, StandardBaudRate.B2400
		, StandardBaudRate.B4800
		, B7200
		, StandardBaudRate.B9600
		, B14400
		, StandardBaudRate.B19200
		, B28800
		, StandardBaudRate.B38400
		, StandardBaudRate.B57600
		, B76800
		, StandardBaudRate.B115200
		, StandardBaudRate.B230400
	};
	
	public static int mapToSystemConstant(int baudRate) {
		return baudRate;
	}
	
	public static int getDefaultBaudRate() {
		return DEFAULT_BAUD_RATE;
	}
	
	public static boolean isValidBaudRate(int baudRate) {
		for(int i = 0; i < ValidBaudRates.length; ++i)
			if (ValidBaudRates[i] == baudRate)
				return true;
		return false;
	}
}
