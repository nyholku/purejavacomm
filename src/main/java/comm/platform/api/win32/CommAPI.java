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
package comm.platform.api.win32;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import static comm.platform.api.win32.API.*;

/**
 * Calls representing portions of the Win32 I/O completion ports API.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface CommAPI extends com.sun.jna.Library {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String
		  LIBRARY_NAME = "kernel32"
	;
	
	public static final CommAPI
		INSTANCE = CommAPIDirect.loadLibrary()
	;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Util">
	public static class Util {
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="API">
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final int 
		  MAXDWORD = 0xFFFFFFFF
	;
	
	public static final int 
		  FILE_FLAG_WRITE_THROUGH       = 0x80000000
		, FILE_FLAG_OVERLAPPED          = 0x40000000
		, FILE_FLAG_NO_BUFFERING        = 0x20000000
		, FILE_FLAG_RANDOM_ACCESS       = 0x10000000
		, FILE_FLAG_SEQUENTIAL_SCAN     = 0x08000000
		, FILE_FLAG_DELETE_ON_CLOSE     = 0x04000000
		, FILE_FLAG_BACKUP_SEMANTICS    = 0x02000000
		, FILE_FLAG_POSIX_SEMANTICS     = 0x01000000
		, FILE_FLAG_OPEN_REPARSE_POINT  = 0x00200000
		, FILE_FLAG_OPEN_NO_RECALL      = 0x00100000
		, FILE_FLAG_FIRST_PIPE_INSTANCE = 0x00080000
	;
	
	public static final byte 
		  NOPARITY                      = 0
		, ODDPARITY                     = 1
		, EVENPARITY                    = 2
		, MARKPARITY                    = 3
		, SPACEPARITY                   = 4
	;
	
	public static final byte 
		  ONESTOPBIT                    = 0
		, ONE5STOPBITS                  = 1
		, TWOSTOPBITS                   = 2
	;
	
	public static final int 
		  EV_RXCHAR                     = 0x0001
		, EV_RXFLAG                     = 0x0002
		, EV_TXEMPTY                    = 0x0004
		, EV_CTS                        = 0x0008
		, EV_DSR                        = 0x0010
		, EV_RLSD                       = 0x0020
		, EV_BREAK                      = 0x0040
		, EV_ERR                        = 0x0080
		, EV_RING                       = 0x0100
		, EV_PERR                       = 0x0200
		, EV_RX80FULL                   = 0x0400
		, EV_EVENT1                     = 0x0800
		, EV_EVENT2                     = 0x1000
	;
	
	public static byte 
		  DC1 = 0x11 //Ctrl-Q
		, DC3 = 0x13 //Ctrl-S
	;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Types">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Structs">
	public static class OVERLAPPED extends Structure {
		public int Internal;
		public int InternalHigh;
		public int Offset;
		public int OffsetHigh;
		public int hEvent;

		public OVERLAPPED() {
			super();
		}
		
		public void reuse(Pointer p) {
			useMemory(p);
			read();
		}
	}

	/**
	 * http://msdn.microsoft.com/en-us/library/aa363214%28v=vs.85%29.aspx
	 */
	public static class DCB extends Structure {
		public static final int 
			  BIT_FIELD_FLAG_NOP                = 0x00000000
			, BIT_FIELD_FLAG_BINARY             = 0x00000001
			, BIT_FIELD_FLAG_PARITY             = 0x00000002
			, BIT_FIELD_FLAG_OUTXCTSFLOW        = 0x00000004
			, BIT_FIELD_FLAG_OUTXDSRFLOW        = 0x00000008
			, BIT_FIELD_FLAG_DTRCONTROL         = 0x00000030
			, BIT_FIELD_FLAG_DSRSENSITIVITY     = 0x00000040
			, BIT_FIELD_FLAG_TXCONTINUEONXOFF   = 0x00000080
			, BIT_FIELD_FLAG_OUTX               = 0x00000100
			, BIT_FIELD_FLAG_INX                = 0x00000200
			, BIT_FIELD_FLAG_ERRORCHAR          = 0x00000400
			, BIT_FIELD_FLAG_NULL               = 0x00000800
			, BIT_FIELD_FLAG_RTSCONTROL         = 0x00003000
			, BIT_FIELD_FLAG_ABORTONERROR       = 0x00004000
			, BIT_FIELD_FLAG_DUMMY2             = 0xFFFF8000
		;
		
		public int DCBlength;
		public int BaudRate;
		public int fFlags; //No bit field mapping in JNA so define a flags field and masks for fFlags -- fBinary through fDummy2
		public short wReserved;
		public short XonLim;
		public short XoffLim;
		public byte ByteSize;
		public byte Parity;
		public byte StopBits;
		public byte XonChar;
		public byte XoffChar;
		public byte ErrorChar;
		public byte EofChar;
		public byte EvtChar;
		public short wReserved1;
	}

	public static class COMMTIMEOUTS extends Structure {
		public int ReadIntervalTimeout;
		public int ReadTotalTimeoutMultiplier;
		public int ReadTotalTimeoutConstant;
		public int WriteTotalTimeoutMultiplier;
		public int WriteTotalTimeoutConstant;
	}

	public static class COMSTAT extends Structure {
		public int fFlags;
		public static final int fCtsHold = 0x00000001;
		public static final int fDsrHold = 0x00000002;
		public static final int fRlsdHold = 0x00000004;
		public static final int fXoffHold = 0x00000008;
		public static final int fXoffSent = 0x00000010;
		public static final int fEof = 0x00000020;
		public static final int fTxim = 0x00000040;
		public static final int fReserved = 0xFFFFFF80;
		public int cbInQue;
		public int cbOutQue;
	}
	//</editor-fold>
	
	HANDLE CreateFile(String name, int access, int sharing, SECURITY_ATTRIBUTES security, int create, int attribs, Pointer template);
	boolean ReadFile(HANDLE hFile, byte[] buf, int rdn, int[] nrd, Pointer lpOverlapped);
	boolean ReadFile(HANDLE hFile, Pointer lpBuffer, int rdn, int[] nrd, OVERLAPPED lpOverlapped);
	boolean WriteFile(HANDLE hFile, byte[] buf, int wrn, int[] nwrtn, Pointer lpOverlapped);
	boolean WriteFile(HANDLE hFile, Pointer buf, int wrn, int[] nwrtn, OVERLAPPED lpOverlapped);
	boolean FlushFileBuffers(HANDLE hFile);
	boolean GetOverlappedResult(HANDLE hFile, OVERLAPPED lpOverlapped, int[] lpNumberOfBytesTransferred, boolean bWait);
	boolean CloseHandle(HANDLE hFile);
	
	boolean ClearCommError(HANDLE hFile, int[] n, COMSTAT s);
	boolean SetCommMask(HANDLE hFile, int dwEvtMask);
	boolean GetCommMask(HANDLE hFile, int[] dwEvtMask);
	boolean GetCommState(HANDLE hFile, DCB dcb);
	boolean SetCommState(HANDLE hFile, DCB dcb);
	boolean GetCommTimeouts(HANDLE hFile, COMMTIMEOUTS tout);
	boolean SetCommTimeouts(HANDLE hFile, COMMTIMEOUTS tout);
	boolean SetupComm(HANDLE hFile, int dwInQueue, int dwOutQueue);
	boolean SetCommBreak(HANDLE hFile);
	boolean ClearCommBreak(HANDLE hFile);
	boolean GetCommModemStatus(HANDLE hFile, int[] stat);
	boolean EscapeCommFunction(HANDLE hFile, int func);
	boolean WaitCommEvent(HANDLE hFile, IntByReference lpEvtMask, OVERLAPPED lpOverlapped);
	boolean WaitCommEvent(HANDLE hFile, int[] lpEvtMask, OVERLAPPED lpOverlapped);
	boolean PurgeComm(HANDLE hFile, int qmask);
	//</editor-fold>
}
