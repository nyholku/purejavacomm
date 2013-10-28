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

package jtermios.windows;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;
import static jtermios.JTermios.JTermiosLogging.*;

/**
 * This WinAPI class implements a simple wrapper API to access the Windows COM
 * ports from Java.
 * 
 * The purpose is to follow reasonably closely the WIN32 API so that COM port
 * related C-code can be ported to Java almost as-is with little changes when
 * this class is statically imported.
 * <p>
 * This is a pure lightweight wrapper around WIN32 API calls with no added
 * syntactic sugar, functionality or niceties.
 * <p>
 * Here is a rude example:
 * 
 * <pre>
 * <code>
 * import static jtermios.windows.WinAPI.*;
 * ...
 *    byte[] buffer = "Hello World".getBytes();
 *    HANDLE hcomm = CreateFileA( "COM5:", GENERIC_READ |GENERIC_WRITE, 0, null, 0, 0, null );
 *    int[] wrtn = {0};
 *    WriteFile(hcomm, buffer, buffer.length, wrtn);
 *    CloseHandle(hcomm);
 * </code>
 * </pre>
 * 
 * Can't get much closer to C-code, what!
 * <p>
 * In addition to the basic open/close/read/write and setup operations this
 * class also makes available enough of the WIN32 Event API to make it possible
 * to use overlapped (asynchronous) I/O on COM ports.
 * 
 * <p>
 * Note that overlapped IO API is full of fine print. Especially worth
 * mentioning is that the OVERLAPPED structure cannot use autosync as it is
 * modified (by Windows) outside the function calls that use it. OVERLAPPED
 * takes care of not autosyncing but it is best to us the writeField() methods
 * to set fields of OVERLAPPED.
 * 
 * <pre>
 * <code>
 *    OVERLAPPED ovl = new OVERLAPPED();
 *    ovl.writeField("hEvent",CreateEvent(null, true, false, null));
 *   ...
 *    WriteFile(hComm, txm, txb.length, txn, ovl);
 *   ...
 *    GetOverlappedResult(hComm, ovl, txn, true);
 * </code>
 * </pre>
 * 
 * @author Kustaa Nyholm
 * 
 */

public class WinAPI {
	static Windows_kernel32_lib m_K32lib = (Windows_kernel32_lib) Native.loadLibrary("kernel32", Windows_kernel32_lib.class);
	private static boolean TRACE = true;

	public static class HANDLE extends PointerType {
		private boolean immutable;

		public HANDLE() {
		}

		public HANDLE(Pointer p) {
			setPointer(p);
			immutable = true;
		}

		public Object fromNative(Object nativeValue, FromNativeContext context) {
			Object o = super.fromNative(nativeValue, context);
			if (NULL.equals(o))
				return NULL;
			if (INVALID_HANDLE_VALUE.equals(o))
				return INVALID_HANDLE_VALUE;
			return o;
		}

		public void setPointer(Pointer p) {
			if (immutable) {
				throw new UnsupportedOperationException("immutable");
			}

			super.setPointer(p);
		}
	}

	public static HANDLE INVALID_HANDLE_VALUE = new HANDLE(Pointer.createConstant(Pointer.SIZE == 8 ? -1 : 0xFFFFFFFFL));
	public static HANDLE NULL = new HANDLE(Pointer.createConstant(0));

	public interface Windows_kernel32_lib extends Library {
		HANDLE CreateFileW(WString name, int access, int mode, SECURITY_ATTRIBUTES security, int create, int atteribs, Pointer template);

		HANDLE CreateFileA(String name, int access, int mode, SECURITY_ATTRIBUTES security, int create, int atteribs, Pointer template);

		boolean WriteFile(HANDLE hFile, byte[] buf, int wrn, int[] nwrtn, Pointer lpOverlapped);

		boolean WriteFile(HANDLE hFile, Pointer buf, int wrn, int[] nwrtn, OVERLAPPED lpOverlapped);

		boolean ReadFile(HANDLE hFile, byte[] buf, int rdn, int[] nrd, Pointer lpOverlapped);

		boolean ReadFile(HANDLE hFile, Pointer lpBuffer, int rdn, int[] nrd, OVERLAPPED lpOverlapped);

		boolean FlushFileBuffers(HANDLE hFile);

		boolean PurgeComm(HANDLE hFile, int qmask);

		boolean CancelIo(HANDLE hFile);

		boolean CloseHandle(HANDLE hFile);

		boolean ClearCommError(HANDLE hFile, int[] n, COMSTAT s);

		boolean SetCommMask(HANDLE hFile, int dwEvtMask);

		boolean GetCommMask(HANDLE hFile, int[] dwEvtMask);

		boolean GetCommState(HANDLE hFile, DCB dcb);

		boolean SetCommState(HANDLE hFile, DCB dcb);

		boolean SetCommTimeouts(HANDLE hFile, COMMTIMEOUTS tout);

		boolean SetupComm(HANDLE hFile, int dwInQueue, int dwOutQueue);

		boolean SetCommBreak(HANDLE hFile);

		boolean ClearCommBreak(HANDLE hFile);

		boolean GetCommModemStatus(HANDLE hFile, int[] stat);

		boolean EscapeCommFunction(HANDLE hFile, int func);

		HANDLE CreateEventA(SECURITY_ATTRIBUTES lpEventAttributes, boolean bManualReset, boolean bInitialState, String lpName);

		HANDLE CreateEventW(SECURITY_ATTRIBUTES lpEventAttributes, boolean bManualReset, boolean bInitialState, WString lpName);

		boolean ResetEvent(HANDLE hEvent);

		boolean SetEvent(HANDLE hEvent);

		// Note lpEvtMask must be IntByRerence when WaitCommEvent is overlapped
		boolean WaitCommEvent(HANDLE hFile, IntByReference lpEvtMask, OVERLAPPED lpOverlapped);

		boolean WaitCommEvent(HANDLE hFile, int[] lpEvtMask, OVERLAPPED lpOverlapped);

		int WaitForSingleObject(HANDLE hHandle, int dwMilliseconds);

		int WaitForMultipleObjects(int nCount, HANDLE[] lpHandles, boolean bWaitAll, int dwMilliseconds);

		boolean GetOverlappedResult(HANDLE hFile, OVERLAPPED lpOverlapped, int[] lpNumberOfBytesTransferred, boolean bWait);

		int GetLastError();

		int FormatMessageW(int flags, Pointer src, int msgId, int langId, Pointer dst, int sze, Pointer va_list);

		int QueryDosDeviceA(String name, byte[] buffer, int bsize);

		int QueryDosDeviceW(WString name, char[] buffer, int bsize);

	}

	// There seems to be very little rhyme or reason from which header file
	// these come from in C, so I did not bother to keep track of the
	// origin of these constants
	public static final int ERROR_INSUFFICIENT_BUFFER = 122;
	public static final int MAXDWORD = 0xFFFFFFFF;
	public static final int STATUS_WAIT_0 = 0x00000000;
	public static final int STATUS_ABANDONED_WAIT_0 = 0x00000080;
	public static final int WAIT_ABANDONED = (STATUS_ABANDONED_WAIT_0) + 0;
	public static final int WAIT_ABANDONED_0 = (STATUS_ABANDONED_WAIT_0) + 0;
	public static final int WAIT_OBJECT_0 = ((STATUS_WAIT_0) + 0);
	public static final int WAIT_FAILED = 0xFFFFFFFF;
	public static final int INFINITE = 0xFFFFFFFF;
	public static final int WAIT_TIMEOUT = 258; //
	public static final int GENERIC_READ = 0x80000000;
	public static final int GENERIC_WRITE = 0x40000000;
	public static final int GENERIC_EXECUTE = 0x20000000;
	public static final int GENERIC_ALL = 0x10000000;
	public static final int CREATE_NEW = 1;
	public static final int CREATE_ALWAYS = 2;
	public static final int OPEN_EXISTING = 3;
	public static final int OPEN_ALWAYS = 4;
	public static final int TRUNCATE_EXISTING = 5;
	public static final int PURGE_TXABORT = 0x0001;
	public static final int PURGE_RXABORT = 0x0002;
	public static final int PURGE_TXCLEAR = 0x0004;
	public static final int PURGE_RXCLEAR = 0x0008;
	public static final int MS_CTS_ON = 0x0010;
	public static final int MS_DSR_ON = 0x0020;
	public static final int MS_RING_ON = 0x0040;
	public static final int MS_RLSD_ON = 0x0080;
	public static final int SETXOFF = 1;
	public static final int SETXON = 2;
	public static final int SETRTS = 3;
	public static final int CLRRTS = 4;
	public static final int SETDTR = 5;
	public static final int CLRDTR = 6;
	public static final int RESETDEV = 7;
	public static final int SETBREAK = 8;
	public static final int CLRBREAK = 9;

	public static final int FILE_FLAG_WRITE_THROUGH = 0x80000000;
	public static final int FILE_FLAG_OVERLAPPED = 0x40000000;
	public static final int FILE_FLAG_NO_BUFFERING = 0x20000000;
	public static final int FILE_FLAG_RANDOM_ACCESS = 0x10000000;
	public static final int FILE_FLAG_SEQUENTIAL_SCAN = 0x08000000;
	public static final int FILE_FLAG_DELETE_ON_CLOSE = 0x04000000;
	public static final int FILE_FLAG_BACKUP_SEMANTICS = 0x02000000;
	public static final int FILE_FLAG_POSIX_SEMANTICS = 0x01000000;
	public static final int FILE_FLAG_OPEN_REPARSE_POINT = 0x00200000;
	public static final int FILE_FLAG_OPEN_NO_RECALL = 0x00100000;
	public static final int FILE_FLAG_FIRST_PIPE_INSTANCE = 0x00080000;
	public static final int ERROR_OPERATION_ABORTED = 995;
	public static final int ERROR_IO_INCOMPLETE = 996;
	public static final int ERROR_IO_PENDING = 997;
	public static final int ERROR_BROKEN_PIPE = 109;
	public static final int ERROR_MORE_DATA = 234;
	public static final int ERROR_FILE_NOT_FOUND = 2;
	public static final byte NOPARITY = 0;
	public static final byte ODDPARITY = 1;
	public static final byte EVENPARITY = 2;
	public static final byte MARKPARITY = 3;
	public static final byte SPACEPARITY = 4;
	public static final byte ONESTOPBIT = 0;
	public static final byte ONE5STOPBITS = 1;
	public static final byte TWOSTOPBITS = 2;
	public static final int CBR_110 = 110;
	public static final int CBR_300 = 300;
	public static final int CBR_600 = 600;
	public static final int CBR_1200 = 1200;
	public static final int CBR_2400 = 2400;
	public static final int CBR_4800 = 4800;
	public static final int CBR_9600 = 9600;
	public static final int CBR_14400 = 14400;
	public static final int CBR_19200 = 19200;
	public static final int CBR_38400 = 38400;
	public static final int CBR_56000 = 56000;
	public static final int CBR_57600 = 57600;
	public static final int CBR_115200 = 115200;
	public static final int CBR_128000 = 128000;
	public static final int CBR_256000 = 256000;
	public static final int CE_RXOVER = 0x0001;
	public static final int CE_OVERRUN = 0x0002;
	public static final int CE_RXPARITY = 0x0004;
	public static final int CE_FRAME = 0x0008;
	public static final int CE_BREAK = 0x0010;
	public static final int CE_TXFULL = 0x0100;
	public static final int CE_PTO = 0x0200;
	public static final int CE_IOE = 0x0400;
	public static final int CE_DNS = 0x0800;
	public static final int CE_OOP = 0x1000;
	public static final int CE_MODE = 0x8000;
	public static final int IE_BADID = -1;
	public static final int IE_OPEN = -2;
	public static final int IE_NOPEN = -3;
	public static final int IE_MEMORY = -4;
	public static final int IE_DEFAULT = -5;
	public static final int IE_HARDWARE = -10;
	public static final int IE_BYTESIZE = -11;
	public static final int IE_BAUDRATE = -12;
	public static final int EV_RXCHAR = 0x0001;
	public static final int EV_RXFLAG = 0x0002;
	public static final int EV_TXEMPTY = 0x0004;
	public static final int EV_CTS = 0x0008;
	public static final int EV_DSR = 0x0010;
	public static final int EV_RLSD = 0x0020;
	public static final int EV_BREAK = 0x0040;
	public static final int EV_ERR = 0x0080;
	public static final int EV_RING = 0x0100;
	public static final int EV_PERR = 0x0200;
	public static final int EV_RX80FULL = 0x0400;
	public static final int EV_EVENT1 = 0x0800;
	public static final int EV_EVENT2 = 0x1000;

	public static final int FORMAT_MESSAGE_ALLOCATE_BUFFER = 0x00000100;
	public static final int FORMAT_MESSAGE_IGNORE_INSERTS = 0x00000200;
	public static final int FORMAT_MESSAGE_FROM_STRING = 0x00000400;
	public static final int FORMAT_MESSAGE_FROM_HMODULE = 0x00000800;
	public static final int FORMAT_MESSAGE_FROM_SYSTEM = 0x00001000;
	public static final int FORMAT_MESSAGE_ARGUMENT_ARRAY = 0x00002000;
	public static final int FORMAT_MESSAGE_MAX_WIDTH_MASK = 0x000000FF;

	public static final int LANG_NEUTRAL = 0x00;
	public static final int SUBLANG_DEFAULT = 0x01;

	public static int MAKELANGID(int p, int s) {
		return (s << 10) | p;
	}

	public static class ULONG_PTR extends IntegerType {
		public ULONG_PTR() {
			this(0);
		}

		public ULONG_PTR(long value) {
			super(Pointer.SIZE, value);
		}
	}

	/**
	 * Represent the Windows API struct OVERLAPPED. The constructor of this
	 * class does 'this.setAutoSynch(false)' because instances of this class
	 * should not be auto synchronized nor written as a whole, because Windows
	 * stores pointers to the actual memory representing this this struct and
	 * modifies it outside the function calls and copying (writing) the Java
	 * class fields to the actual memory will destroy those structures.
	 * 
	 * <p>
	 * To set the fields it recommend to use the 'writeField(String,Object)'. It
	 * is ok to read those fields of OVERLAPPED using Java dot-notatio. that
	 * have been written by Java code, but those field that Windows modifies
	 * should be accessed using 'readField(String)' or by invoking 'read()' on
	 * the object before accessing the fields with the java dot-notation.
	 * <p>
	 * For example this is acceptable usage for doing overlapped I/O (except
	 * this code does no error checking!):
	 * 
	 * <pre>
	 * <code>
	 *  OVERLAPPED ovl = new OVERLAPPED();
	 *  ovl.writeField("hEvent", CreateEvent(null, true, false, null));
	 *  ResetEvent(osReader.hEvent);
	 *  ReadFile(hComm, buffer, reqN, recN, ovl);
	 * </code>
	 * </pre>
	 * 
	 * @author nyholku
	 * 
	 */
	public static class OVERLAPPED extends Structure {
		private static boolean TRACE;
		public ULONG_PTR Internal;
		public ULONG_PTR InternalHigh;
		public int Offset;
		public int OffsetHigh;
		public HANDLE hEvent;

		@Override
		protected List getFieldOrder() {
			return Arrays.asList("Internal",//
					"InternalHigh",//
					"Offset",//
					"OffsetHigh",//
					"hEvent"//
			);
		}

		public OVERLAPPED() {
			setAutoSynch(false);
		}

		public String toString() {
			return String.format(//
					"[Offset %d OffsetHigh %d hEvent %s]",//
					Offset, OffsetHigh, hEvent.toString());
		}
	}

	public static class SECURITY_ATTRIBUTES extends Structure {
		public int nLength;
		public Pointer lpSecurityDescriptor;
		public boolean bInheritHandle;

		@Override
		protected List getFieldOrder() {
			return Arrays.asList("nLength",//
					"lpSecurityDescriptor",//
					"bInheritHandle"//
			);
		}
	}

	public static class DCB extends Structure {
		public int DCBlength;
		public int BaudRate;
		public int fFlags; // No bit field mapping in JNA so define a flags field and masks for fFlags
		public static final int fBinary = 0x00000001;
		public static final int fParity = 0x00000002;
		public static final int fOutxCtsFlow = 0x00000004;
		public static final int fOutxDsrFlow = 0x00000008;
		public static final int fDtrControl = 0x00000030;
		public static final int fDsrSensitivity = 0x00000040;
		public static final int fTXContinueOnXoff = 0x00000080;
		public static final int fOutX = 0x00000100;
		public static final int fInX = 0x00000200;
		public static final int fErrorChar = 0x00000400;
		public static final int fNull = 0x00000800;
		public static final int fRtsControl = 0x00003000;
		public static final int fAbortOnError = 0x00004000;
		public static final int fDummy2 = 0xFFFF8000;
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

		@Override
		protected List getFieldOrder() {
			return Arrays.asList("DCBlength",//
					"BaudRate",//
					"fFlags",//
					"wReserved",//
					"XonLim",//
					"XoffLim",//
					"ByteSize",//
					"Parity",//
					"StopBits",//
					"XonChar",//
					"XoffChar",//
					"ErrorChar",//
					"EofChar",//
					"EvtChar",//
					"wReserved1"//
			);
		}

		public String toString() {
			return String.format(//
					"[BaudRate %d fFlags %04X wReserved %d XonLim %d XoffLim %d ByteSize %d Parity %d StopBits %d XonChar %02X XoffChar %02X ErrorChar %02X EofChar %02X EvtChar %02X wReserved1 %d]", //
					BaudRate, fFlags, wReserved, XonLim, XoffLim, ByteSize, Parity, StopBits, XonChar, XoffChar, ErrorChar, EofChar, EvtChar, wReserved1);
		}

	};

	public static class COMMTIMEOUTS extends Structure {
		public int ReadIntervalTimeout;
		public int ReadTotalTimeoutMultiplier;
		public int ReadTotalTimeoutConstant;
		public int WriteTotalTimeoutMultiplier;
		public int WriteTotalTimeoutConstant;

		@Override
		protected List getFieldOrder() {
			return Arrays.asList("ReadIntervalTimeout",//
					"ReadTotalTimeoutMultiplier",//
					"ReadTotalTimeoutConstant",//
					"WriteTotalTimeoutMultiplier",//
					"WriteTotalTimeoutConstant"//
			);
		}

		public String toString() {
			return String.format(//
					"[ReadIntervalTimeout %d ReadTotalTimeoutMultiplier %d ReadTotalTimeoutConstant %d WriteTotalTimeoutMultiplier %d WriteTotalTimeoutConstant %d]", //
					ReadIntervalTimeout, ReadTotalTimeoutMultiplier, ReadTotalTimeoutConstant, WriteTotalTimeoutMultiplier, WriteTotalTimeoutConstant);
		}

	};

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

		@Override
		protected List getFieldOrder() {
			return Arrays.asList("fFlags",//
					"cbInQue",//
					"cbOutQue"//
			);
		}

		public String toString() {
			return String.format("[fFlags %04X cbInQue %d cbInQue %d]", fFlags, cbInQue, cbOutQue);
		}
	};

	static public HANDLE CreateFileA(String name, int access, int sharing, SECURITY_ATTRIBUTES security, int create, int attribs, Pointer template) {
		log = log && log(5, "> CreateFileA(%s, 0x%08X, 0x%08X, %s, 0x%08X, 0x%08X,%s)\n", name, access, sharing, security, create, attribs, template);
		HANDLE h = m_K32lib.CreateFileA(name, access, sharing, security, create, attribs, template);
		log = log && log(4, "< CreateFileA(%s, 0x%08X, 0x%08X, %s, 0x%08X, 0x%08X,%s) => %s\n", name, access, sharing, security, create, attribs, template, h);
		return h;
	}

	static public HANDLE CreateFileW(WString name, int access, int sharing, SECURITY_ATTRIBUTES security, int create, int attribs, Pointer template) {
		log = log && log(5, "> CreateFileW(%s, 0x%08X, 0x%08X, %s, 0x%08X, 0x%08X,%s)\n", name, access, sharing, security, create, attribs, template);
		HANDLE h = m_K32lib.CreateFileW(name, access, sharing, security, create, attribs, template);
		log = log && log(4, "< CreateFileW(%s, 0x%08X, 0x%08X, %s, 0x%08X, 0x%08X,%s) => %s\n", name, access, sharing, security, create, attribs, template, h);
		return h;
	}

	// This is for synchronous writes only
	static public boolean WriteFile(HANDLE hFile, byte[] buf, int wrn, int[] nwrtn) {
		log = log && log(5, "> WriteFile(%s, %s, %d, [%d])\n", hFile, log(buf, wrn), wrn, nwrtn[0]);
		boolean res = m_K32lib.WriteFile(hFile, buf, wrn, nwrtn, null);
		log = log && log(4, "< WriteFile(%s, %s, %d, [%d]) => %s\n", hFile, log(buf, wrn), wrn, nwrtn[0], res);
		return res;
	}

	// This can be used with synchronous as well as overlapped writes
	static public boolean WriteFile(HANDLE hFile, Pointer buf, int wrn, int[] nwrtn, OVERLAPPED ovrlp) {
		log = log && log(5, "> WriteFile(%s, %s, %d, [%d], %s)\n", hFile, log(buf.getByteArray(0, wrn), 5), wrn, nwrtn[0], ref(ovrlp));
		boolean res = m_K32lib.WriteFile(hFile, buf, wrn, nwrtn, ovrlp);
		log = log && log(4, "< WriteFile(%s, %s, %d, [%d], %s) => %s\n", hFile, log(buf.getByteArray(0, wrn), 5), wrn, nwrtn[0], ref(ovrlp), res);
		return res;
	}

	// This is for synchronous reads only
	static public boolean ReadFile(HANDLE hFile, byte[] buf, int rdn, int[] nrd) {
		log = log && log(5, "> ReadFile(%s, %s, %d, [%d])\n", hFile, log(buf, rdn), rdn, nrd[0]);
		boolean res = m_K32lib.ReadFile(hFile, buf, rdn, nrd, null);
		log = log && log(4, "< ReadFile(%s, %s, %d, [%d]) => %s\n", hFile, log(buf, rdn), rdn, nrd[0], res);
		return res;
	}

	// This can be used with synchronous as well as overlapped reads
	static public boolean ReadFile(HANDLE hFile, Pointer buf, int rdn, int[] nrd, OVERLAPPED ovrlp) {
		log = log && log(5, "> ReadFile(%s, %s, %d, [%d], %s)\n", hFile, log(buf.getByteArray(0, rdn), 5), rdn, nrd[0], ref(ovrlp));
		boolean res = m_K32lib.ReadFile(hFile, buf, rdn, nrd, ovrlp);
		log = log && log(4, "< ReadFile(%s, %s, %d, [%d], %s) => %s\n", hFile, log(buf.getByteArray(0, rdn), 5), rdn, nrd[0], ref(ovrlp), res);
		return res;
	}

	static public boolean FlushFileBuffers(HANDLE hFile) {
		log = log && log(5, "> FlushFileBuffers(%s)\n", hFile);
		boolean res = m_K32lib.FlushFileBuffers(hFile);
		log = log && log(4, "< FlushFileBuffers(%s) => %s\n", hFile, res);
		return res;
	}

	static public boolean PurgeComm(HANDLE hFile, int qmask) {
		log = log && log(5, "> PurgeComm(%s,0x%08X)\n", hFile, qmask);
		boolean res = m_K32lib.PurgeComm(hFile, qmask);
		log = log && log(4, "< PurgeComm(%s,0x%08X) => %s\n", hFile, qmask, res);
		return res;
	}

	static public boolean CancelIo(HANDLE hFile) {
		log = log && log(5, "> CancelIo(%s)\n", hFile);
		boolean res = m_K32lib.CancelIo(hFile);
		log = log && log(4, "< CancelIo(%s) => %s\n", hFile, res);
		return res;
	}

	static public boolean CloseHandle(HANDLE hFile) {
		log = log && log(5, "> CloseHandle(%s)\n", hFile);
		boolean res = m_K32lib.CloseHandle(hFile);
		log = log && log(4, "< CloseHandle(%s) => %s\n", hFile, res);
		return res;
	}

	static public boolean ClearCommError(HANDLE hFile, int[] n, COMSTAT s) {
		log = log && log(5, "> ClearCommError(%s, [%d], %s)\n", hFile, n[0], s);
		boolean res = m_K32lib.ClearCommError(hFile, n, s);
		log = log && log(4, "< ClearCommError(%s, [%d], %s) => %s\n", hFile, n[0], s, res);
		return res;
	}

	static public boolean SetCommMask(HANDLE hFile, int mask) {
		log = log && log(5, "> SetCommMask(%s, 0x%08X)\n", hFile, mask);
		boolean res = m_K32lib.SetCommMask(hFile, mask);
		log = log && log(4, "< SetCommMask(%s, 0x%08X) => %s\n", hFile, mask, res);
		return res;
	}

	static public boolean GetCommMask(HANDLE hFile, int[] mask) {
		log = log && log(5, "> GetCommMask(%s, [0x%08X])\n", hFile, mask[0]);
		boolean res = m_K32lib.GetCommMask(hFile, mask);
		log = log && log(4, "< GetCommMask(%s, [0x%08X]) => %s\n", hFile, mask[0], res);
		return res;
	}

	static public boolean GetCommState(HANDLE hFile, DCB dcb) {
		log = log && log(5, "> GetCommState(%s, %s)\n", hFile, dcb);
		boolean res = m_K32lib.GetCommState(hFile, dcb);
		log = log && log(4, "< GetCommState(%s, %s) => %s\n", hFile, dcb, res);
		return res;
	}

	static public boolean SetCommState(HANDLE hFile, DCB dcb) {
		log = log && log(5, "> SetCommState(%s, %s)\n", hFile, dcb);
		boolean res = m_K32lib.SetCommState(hFile, dcb);
		log = log && log(4, "< SetCommState(%s, %s) => %s\n", hFile, dcb, res);
		return res;
	}

	static public boolean SetCommTimeouts(HANDLE hFile, COMMTIMEOUTS touts) {
		log = log && log(5, "> SetCommTimeouts(%s, %s)\n", hFile, touts);
		boolean res = m_K32lib.SetCommTimeouts(hFile, touts);
		log = log && log(4, "< SetCommTimeouts(%s, %s) => %s\n", hFile, touts, res);
		return res;
	}

	static public boolean SetupComm(HANDLE hFile, int inQueueSz, int outQueueSz) {
		log = log && log(5, "> SetCommTimeouts(%s, %d, %d)\n", hFile, inQueueSz, outQueueSz);
		boolean res = m_K32lib.SetupComm(hFile, inQueueSz, outQueueSz);
		log = log && log(4, "< SetCommTimeouts(%s, %d, %d) => %s\n", hFile, inQueueSz, outQueueSz, res);
		return res;
	}

	static public boolean SetCommBreak(HANDLE hFile) {
		log = log && log(5, "> SetCommBreak(%s)\n", hFile);
		boolean res = m_K32lib.SetCommBreak(hFile);
		log = log && log(4, "< SetCommBreak(%s) => %s\n", hFile, res);
		return res;
	}

	static public boolean ClearCommBreak(HANDLE hFile) {
		log = log && log(5, "> ClearCommBreak(%s)\n", hFile);
		boolean res = m_K32lib.ClearCommBreak(hFile);
		log = log && log(4, "< ClearCommBreak(%s) => %s\n", hFile, res);
		return res;
	}

	static public boolean GetCommModemStatus(HANDLE hFile, int[] stat) {
		log = log && log(5, "> GetCommModemStatus(%s,0x%08X)\n", hFile, stat[0]);
		boolean res = m_K32lib.GetCommModemStatus(hFile, stat);
		log = log && log(4, "< GetCommModemStatus(%s,0x%08X) => %s\n", hFile, stat[0], res);
		return res;
	}

	static public boolean EscapeCommFunction(HANDLE hFile, int func) {
		log = log && log(5, "> EscapeCommFunction(%s,0x%08X)\n", hFile, func);
		boolean res = m_K32lib.EscapeCommFunction(hFile, func);
		log = log && log(4, "< EscapeCommFunction(%s,0x%08X) => %s\n", hFile, func, res);
		return res;
	}

	static public HANDLE CreateEventW(SECURITY_ATTRIBUTES security, boolean manual, boolean initial, WString name) {
		log = log && log(5, "> CreateEventW(%s, %s, %s, %s)\n", ref(security), manual, initial, name);
		HANDLE h = m_K32lib.CreateEventW(security, manual, initial, name);
		log = log && log(4, "< CreateEventW(%s, %s, %s, %s) => %s\n", ref(security), manual, initial, name, h);
		return h;
	}

	static public HANDLE CreateEventA(SECURITY_ATTRIBUTES security, boolean manual, boolean initial, String name) {
		log = log && log(5, "> CreateEventA(%s, %s, %s, %s)\n", ref(security), manual, initial, name);
		HANDLE h = m_K32lib.CreateEventA(security, manual, initial, name);
		log = log && log(4, "< CreateEventA(%s, %s, %s, %s) => %s\n", ref(security), manual, initial, name, h);
		return h;
	}

	static public boolean SetEvent(HANDLE hEvent) {
		log = log && log(5, "> SetEvent(%s)\n", hEvent);
		boolean res = m_K32lib.SetEvent(hEvent);
		log = log && log(4, "< SetEvent(%s) => %s\n", hEvent, res);
		return res;
	}

	static public boolean ResetEvent(HANDLE hEvent) {
		log = log && log(5, "> ResetEvent(%s)\n", hEvent);
		boolean res = m_K32lib.ResetEvent(hEvent);
		log = log && log(4, "< ResetEvent(%s) => %s\n", hEvent, res);
		return res;
	}

	static public boolean WaitCommEvent(HANDLE hFile, IntByReference lpEvtMask, OVERLAPPED ovl) {
		log = log && log(5, "> WaitCommEvent(%s, [%d], %s)\n", hFile, lpEvtMask.getValue(), ref(ovl));
		boolean res = m_K32lib.WaitCommEvent(hFile, lpEvtMask, ovl);
		log = log && log(4, "< WaitCommEvent(%s, [%d], %s) => %s\n", hFile, lpEvtMask.getValue(), ref(ovl), res);
		return res;
	}

	static public boolean WaitCommEvent(HANDLE hFile, int[] lpEvtMask) {
		log = log && log(5, "> WaitCommEvent(%s, [%d], %s) => %s\n", hFile, lpEvtMask[0], null);
		boolean res = m_K32lib.WaitCommEvent(hFile, lpEvtMask, null);
		log = log && log(4, "< WaitCommEvent(%s, [%d], %s) => %s\n", hFile, lpEvtMask[0], null, res);
		return res;
	}

	static public int WaitForSingleObject(HANDLE hHandle, int dwMilliseconds) {
		log = log && log(5, "> WaitForSingleObject(%s, %d)\n", hHandle, dwMilliseconds);
		int res = m_K32lib.WaitForSingleObject(hHandle, dwMilliseconds);
		log = log && log(4, "< WaitForSingleObject(%s, %d) => %s\n", hHandle, dwMilliseconds, res);
		return res;
	}

	static public int WaitForMultipleObjects(int nCount, HANDLE[] lpHandles, boolean bWaitAll, int dwMilliseconds) {
		log = log && log(5, "> WaitForMultipleObjects(%d, %s, %s, %d)\n", nCount, log(lpHandles, 3), bWaitAll, dwMilliseconds);
		int res = m_K32lib.WaitForMultipleObjects(nCount, lpHandles, bWaitAll, dwMilliseconds);
		log = log && log(4, "< WaitForMultipleObjects(%d, %s, %s, %d) => %s\n", nCount, log(lpHandles, 3), bWaitAll, dwMilliseconds, res);
		return res;
	}

	static public boolean GetOverlappedResult(HANDLE hFile, OVERLAPPED ovl, int[] ntfrd, boolean wait) {
		log = log && log(5, "> GetOverlappedResult(%s, %s, [%d], %s)\n", hFile, ref(ovl), ntfrd[0], wait);
		boolean res = m_K32lib.GetOverlappedResult(hFile, ovl, ntfrd, wait);
		log = log && log(4, "< GetOverlappedResult(%s, %s, [%d], %s) => %s\n", hFile, ref(ovl), ntfrd[0], wait, res);
		return res;
	}

	static public int GetLastError() {
		log = log && log(5, "> GetLastError()\n");
		int res = m_K32lib.GetLastError();
		log = log && log(4, "< GetLastError() => %d\n", res);
		return res;
	}

	static public int FormatMessageW(int flags, Pointer src, int msgId, int langId, Pointer dst, int sze, Pointer va_list) {
		log = log && log(5, "> FormatMessageW(%08x, %08x, %d, %d, %s, %d, %s)\n", flags, src, msgId, langId, dst, sze, va_list);
		int res = m_K32lib.FormatMessageW(flags, src, msgId, langId, dst, sze, va_list);
		log = log && log(4, "< FormatMessageW(%08x, %08x, %d, %d, %s, %d, %s) => %d\n", flags, src, msgId, langId, dst, sze, va_list, res);
		return res;
	}

	static public int QueryDosDeviceA(String name, byte[] buffer, int bsize) {
		log = log && log(5, "> QueryDosDeviceA(%s, %s, %d)\n", name, buffer, bsize);
		int res = m_K32lib.QueryDosDeviceA(name, buffer, bsize);
		log = log && log(4, "< QueryDosDeviceA(%s, %s, %d) => %d\n", name, buffer, bsize, res);
		return res;
	}

	// FIXME investigate why defining this as QueryDosDeviceW(WString name, Memory buffer, int bsize)
	// later crashes JVM
	static public int QueryDosDeviceW(WString name, char[] buffer, int bsize) {
		log = log && log(5, "> QueryDosDeviceW(%s,%s,%d)\n", name, buffer, bsize);
		int res = m_K32lib.QueryDosDeviceW(name, buffer, bsize);
		log = log && log(4, "< QueryDosDeviceW(%s,%s,%d) => %d\n", name, buffer, bsize, res);
		return res;
	}

}
