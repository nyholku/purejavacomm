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

import com.sun.jna.Native;
import comm.DataBits;
import comm.FlowControl;
import comm.Parity;
import comm.PortType;
import comm.StopBits;
import comm.platform.api.win32.CommAPI;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import static comm.platform.api.win32.API.*;
import static comm.platform.api.win32.CommAPI.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
class SerialPort extends comm.platform.dev.SerialPort {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private HANDLE handle;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Init">
	public SerialPort(String name, String title, String description, PortType portType) {
		super(name, title, description, portType);
		init();
	}
	
	private void init() {
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Getters">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Update">
	public void update(String name, String title, String description, PortType portType) {
		this.name = name;
		this.title = title;
		this.description = description;
		this.portType = portType;
	}
	//</editor-fold>

	@Override
	public boolean open(int inputBufferSize, int outputBufferSize) {
		if (inputBufferSize <= 0)
			throw new IllegalArgumentException("inputBufferSize must be > 0");
		if (outputBufferSize <= 0)
			throw new IllegalArgumentException("outputBufferSize must be > 0");
		
		synchronized(commLock) {
			if (opened)
				return true;
			
			try {
				CommAPI API = CommAPI.INSTANCE;
				
				//<editor-fold defaultstate="collapsed" desc="Create file">
				//From http://www.flounder.com/serial.htm:
				//    The names "COM1".."COM9" work because there is a special hack in CreateFile that recognizes "C" "O" "M" followed by a single 
				//    digit as being a special case. If you want to open COM10, however, you have to specify it as \\.\COM10, which in a quoted 
				//    string requires doubling the "\" character.
				HANDLE h = API.CreateFile((!name.startsWith("\\\\") ? "\\\\.\\" + name : name) /*COM1*/, GENERIC_READ | GENERIC_WRITE, 0, null, OPEN_EXISTING, FILE_FLAG_OVERLAPPED, null);
				//</editor-fold>
				
				//<editor-fold defaultstate="collapsed" desc="Validate call">
				int err = Native.getLastError();
				if (INVALID_HANDLE_VALUE == h) {
					if (err == ERROR_FILE_NOT_FOUND)
						return false; //ENOENT
					else
						return false; //EBUSY
				}
				//</editor-fold>
				
				if (!API.SetupComm(h, inputBufferSize, outputBufferSize)) {
					API.CloseHandle(h);
					return false;
				}
				
				if (!configureSystemSerialPort(h, baudRate, dataBits, stopBits, parity)) {
					API.CloseHandle(h);
					return false;
				}
				
				//Associate this port's handle with the IO completion port.
				if (!IOComPort.associateCommPort(h)) {
					IOComPort.unassociateCommPort(h);
					API.CloseHandle(h);
					return false;
				}
				
				this.handle = h;
				
				return (opened = true);
			} catch(ExceptionInInitializerError e) {
				throw e;
			} catch(Throwable t) {
				//Close
				return false;
			}
		}
	}
	
	@Override
	protected boolean configureSystemSerialPort(int baudRate, DataBits dataBits, StopBits stopBits, Parity parity) {
		return configureSystemSerialPort(handle, baudRate, dataBits, stopBits, parity);
	}
	
	protected boolean configureSystemSerialPort(HANDLE handle, int baudRate, DataBits dataBits, StopBits stopBits, Parity parity) {
		if (handle == null || handle == INVALID_HANDLE_VALUE)
			return false;
		
		CommAPI	API = CommAPI.INSTANCE;
		DCB dcb = new DCB();
		dcb.DCBlength = dcb.size();
		if (!API.GetCommState(handle, dcb))
			return false;
		
		byte sb = ONESTOPBIT;
		byte par = NOPARITY;
		byte db = 8;
		int flags = 0;
		int flowControlFlag = getFlowControlFlag();
		
		//Build DCB flags
		flags |= DCB.BIT_FIELD_FLAG_BINARY;
		flags |= (parity != Parity.NONE ? DCB.BIT_FIELD_FLAG_PARITY : DCB.BIT_FIELD_FLAG_NOP);
		flags |= (FlowControl.isFlagged(flowControlFlag, FlowControl.RTSCTS_IN) || FlowControl.isFlagged(flowControlFlag, FlowControl.RTSCTS_OUT) ? DCB.BIT_FIELD_FLAG_OUTXCTSFLOW : DCB.BIT_FIELD_FLAG_NOP);
		flags |= (FlowControl.isFlagged(flowControlFlag, FlowControl.XONXOFF_IN) ? DCB.BIT_FIELD_FLAG_INX : DCB.BIT_FIELD_FLAG_NOP);
		flags |= (FlowControl.isFlagged(flowControlFlag, FlowControl.XONXOFF_OUT) ? DCB.BIT_FIELD_FLAG_OUTX : DCB.BIT_FIELD_FLAG_NOP);
		flags |= (FlowControl.isFlagged(flowControlFlag, FlowControl.XONXOFF_IN) || FlowControl.isFlagged(flowControlFlag, FlowControl.XONXOFF_OUT) ? DCB.BIT_FIELD_FLAG_TXCONTINUEONXOFF : DCB.BIT_FIELD_FLAG_NOP);
		flags |= DCB.BIT_FIELD_FLAG_ABORTONERROR;
		
		switch(dataBits) {
			case DATABITS_5:
				db = 5;
				break;
			case DATABITS_6:
				db = 6;
				break;
			case DATABITS_7:
				db = 7;
				break;
			case DATABITS_8:
			default:
				db = 8;
				break;
		}
		
		switch(parity) {
			case EVEN:
				par = EVENPARITY;
				break;
			case MARK:
				par = MARKPARITY;
				break;
			case ODD:
				par = ODDPARITY;
				break;
			case SPACE:
				par = SPACEPARITY;
				break;
			case NONE:
			default:
				par = NOPARITY;
				break;
		}
		
		switch(stopBits) {
			case STOPBITS_1:
				sb = ONESTOPBIT;
				break;
			case STOPBITS_1_5:
				sb = ONE5STOPBITS;
				break;
			case STOPBITS_2:
				sb = TWOSTOPBITS;
				break;
		}
		
		dcb.BaudRate = BaudRates.mapToSystemConstant(baudRate);
		dcb.fFlags = flags;
		//dcb.XonLim = 128;
		//dcb.XoffLim = 128;
		dcb.ByteSize = db;
		dcb.Parity = par;
		dcb.StopBits = sb;
		//dcb.XonChar = DC1;
		//dcb.XoffChar = DC3;
		//dcb.EvtChar = '\n';
		//dcb.EofChar = 0;
		
		COMMTIMEOUTS tm = new COMMTIMEOUTS();
		tm.ReadIntervalTimeout = MAXDWORD;
		tm.ReadTotalTimeoutMultiplier = 0;
		tm.ReadTotalTimeoutConstant = 0;
		tm.WriteTotalTimeoutMultiplier = 0;
		tm.WriteTotalTimeoutConstant = 0;
		
		return (API.SetCommState(handle, dcb) && API.SetCommTimeouts(handle, tm));
	}
	
	@Override
	protected boolean changeSystemFlowControl(FlowControl... flowControl) {
		return true;
	}
	
	@Override
	public boolean close() {
		if (!opened)
			return true;
		
		synchronized(commLock) {
			try {
				CommAPI API = CommAPI.INSTANCE;
				
				//Unassociate this port from the IO completion port.
				//This will cause the IOCP worker threads to exit if this is 
				//the last running port. It's possible that this could take 
				//a while if there are lots of pending I/O events.
				if (!IOComPort.unassociateCommPort(handle))
					return false;
				
				//Instruct the OS that we're done with this handle.
				if (!API.CloseHandle(handle))
					return false;

				this.handle = null;
				
				return !(opened = false);
			} catch(Throwable t) {
				//Still open
				return false;
			}
		}
	}
}
