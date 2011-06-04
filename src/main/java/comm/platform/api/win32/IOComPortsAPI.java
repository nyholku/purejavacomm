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

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.IntByReference;
import static comm.platform.api.win32.API.*;

/**
 * Calls representing portions of the Win32 I/O completion ports API.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IOComPortsAPI extends com.sun.jna.Library {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String
		  LIBRARY_NAME = "kernel32"
	;
	
	public static final IOComPortsAPI
		INSTANCE = IOComPortsAPIDirect.loadLibrary()
	;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Util">
	public static class Util {
		public static HANDLE CreateUnassociatedIoCompletionPort() {
			//Zero indicates to the OS to use the # of processors on the system.
			return CreateUnassociatedIoCompletionPort(0);
		}
		
		public static HANDLE CreateUnassociatedIoCompletionPort(int numberOfConcurrentThreads) {
			return CreateUnassociatedIoCompletionPort(null, numberOfConcurrentThreads);
		}
		
		public static HANDLE CreateUnassociatedIoCompletionPort(IOComPortsAPI API, int numberOfConcurrentThreads) {
			if (API == null)
				API = IOComPortsAPI.INSTANCE;
			return API.CreateIoCompletionPort(comm.platform.api.win32.API.Util.newINVALID_HANDLE_VALUE(), null, null, numberOfConcurrentThreads);
		}
		
		public static boolean AssociateHandleWithIoCompletionPort(HANDLE IOComPortHandle, HANDLE Associate, Pointer CompletionKey) {
			return AssociateHandleWithIoCompletionPort(null, IOComPortHandle, Associate, CompletionKey);
		}
		
		public static boolean AssociateHandleWithIoCompletionPort(IOComPortsAPI API, HANDLE IOComPortHandle, HANDLE Associate, Pointer CompletionKey) {
			if (API == null)
				API = IOComPortsAPI.INSTANCE;
			return IOComPortHandle.equals(API.CreateIoCompletionPort(Associate, IOComPortHandle, CompletionKey, 0));
		}
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="API">
	//<editor-fold defaultstate="collapsed" desc="Constants">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Types">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Structs">
	public static class OVERLAPPEDEX extends Structure {
		public static final int 
			  OP_OPEN                       = 0
			, OP_WAITCOMMEVENT              = 1
			, OP_WAITCOMMEVENT_IMMEDIATE    = 2
			, OP_READ                       = 3
			, OP_WRITE                      = 4
			, OP_CLOSE                      = 5
			, OP_EXITTHREAD                 = 6
		;
		
		public CommAPI.OVERLAPPED ovl;
		public int op;
		public int ex;
		public HANDLE ev;
		
		public OVERLAPPEDEX() {
			super();
		}
		
		public OVERLAPPEDEX(Pointer memory) {
			super(memory);
		}
		
		public void reuse(Pointer memory) {
			useMemory(memory);
			read();
		}
	}
	//</editor-fold>
	
	HANDLE  /*HANDLE*/ CreateIoCompletionPort(HANDLE /*HANDLE*/ fileHandle, HANDLE /*HANDLE*/ existingCompletionPort, Pointer /*ULONG_PTR*/ completionKey, int /*DWORD*/ numberOfConcurrentThreads);
	boolean /*BOOL*/   CloseHandle(HANDLE hObject);
	
	HANDLE  /*HANDLE*/ CreateEvent(Pointer /*SECURITY_ATTRIBUTES*/ security, boolean manual, boolean initial, String name);
	int     /*DWORD*/  WaitForSingleObject(HANDLE /*HANDLE*/ hHandle, int /*DWORD*/ dwMilliseconds);
	boolean /*BOOL*/   SetEvent(HANDLE /*HANDLE*/ hEvent);
	
	boolean /*BOOL*/   GetQueuedCompletionStatus(HANDLE /*HANDLE*/ completionPort, IntByReference /*LPDWORD*/ lpNumberOfBytes, PointerByReference /*PULONG_PTR*/ lpCompletionKey, PointerByReference /*LPOVERLAPPED*/ lpOverlapped, int /*DWORD*/ dwMilliseconds);
	boolean /*BOOL*/   PostQueuedCompletionStatus(HANDLE /*HANDLE*/ completionPort, int /*DWORD*/ dwNumberOfBytesTransferred, Pointer /*ULONG_PTR*/ dwCompletionKey, OVERLAPPEDEX /*LPOVERLAPPED*/ lpOverlapped);
	boolean /*BOOL*/   ReadFile(HANDLE hFile, Pointer lpBuffer, int /*DWORD*/ nNumberOfBytesToRead, IntByReference /*LPDWORD*/ lpNumberOfBytesRead, Pointer /*LPOVERLAPPED*/ lpOverlapped);
	boolean /*BOOL*/   ReadFile(HANDLE hFile, byte[] lpBuffer, int /*DWORD*/ nNumberOfBytesToRead, IntByReference /*LPDWORD*/ lpNumberOfBytesRead, OVERLAPPEDEX /*LPOVERLAPPED*/ lpOverlapped);
	boolean /*BOOL*/   WriteFile(HANDLE hFile, Pointer buf, int wrn, int[] nwrtn, Pointer /*OVERLAPPED*/ lpOverlapped);
	boolean /*BOOL*/   FlushFileBuffers(HANDLE hFile);
	boolean /*BOOL*/   GetOverlappedResult(HANDLE hFile, Pointer /*OVERLAPPED*/ lpOverlapped, IntByReference lpNumberOfBytesTransferred, boolean bWait);
	
	boolean /*BOOL*/   SetCommMask(HANDLE hFile, int dwEvtMask);
	boolean /*BOOL*/   WaitCommEvent(HANDLE hFile, IntByReference lpEvtMask, OVERLAPPEDEX lpOverlapped);
	//</editor-fold>
}
