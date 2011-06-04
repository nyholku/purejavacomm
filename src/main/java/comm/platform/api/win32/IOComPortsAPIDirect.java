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

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import comm.platform.api.Library;
import comm.platform.api.win32.API.HANDLE;

/**
 * Calls representing portions of the Win32 I/O completion ports API.
 * This class is setup as a direct-mapped library for enhanced performance.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class IOComPortsAPIDirect extends Library implements IOComPortsAPI {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public static IOComPortsAPI loadLibrary() {
		IOComPortsAPI inst = directMapping(IOComPortsAPI.LIBRARY_NAME, Win32Library.DEFAULT_OPTIONS, IOComPortsAPIDirect.class);
		//IOComPortsAPI inst = interfaceMapping(IOComPortsAPI.LIBRARY_NAME, Win32Library.DEFAULT_OPTIONS, IOComPortsAPI.class);
		if (inst == null)
			throw new UnsatisfiedLinkError("Could not load library " + LIBRARY_NAME);
		return inst;
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="API">
	@Override
	public native HANDLE  /*HANDLE*/ CreateIoCompletionPort(HANDLE /*HANDLE*/ fileHandle, HANDLE /*HANDLE*/ existingCompletionPort, Pointer /*ULONG_PTR*/ completionKey, int /*DWORD*/ numberOfConcurrentThreads);
	@Override
	public native boolean /*BOOL*/   CloseHandle(HANDLE hObject);
	
	@Override
	public native HANDLE  /*HANDLE*/ CreateEvent(Pointer /*SECURITY_ATTRIBUTES*/ security, boolean manual, boolean initial, String name);
	@Override
	public native int     /*DWORD*/  WaitForSingleObject(HANDLE /*HANDLE*/ hHandle, int /*DWORD*/ dwMilliseconds);
	@Override
	public native boolean /*BOOL*/   SetEvent(HANDLE /*HANDLE*/ hEvent);
	
	@Override
	public native boolean /*BOOL*/   GetQueuedCompletionStatus(HANDLE /*HANDLE*/ completionPort, IntByReference /*LPDWORD*/ lpNumberOfBytes, PointerByReference /*PULONG_PTR*/ lpCompletionKey, PointerByReference /*LPOVERLAPPED*/ lpOverlapped, int /*DWORD*/ dwMilliseconds);
	@Override
	public native boolean /*BOOL*/   PostQueuedCompletionStatus(HANDLE /*HANDLE*/ completionPort, int /*DWORD*/ dwNumberOfBytesTransferred, Pointer /*ULONG_PTR*/ dwCompletionKey, OVERLAPPEDEX /*LPOVERLAPPED*/ lpOverlapped);
	@Override
	public native boolean /*BOOL*/   ReadFile(HANDLE hFile, Pointer lpBuffer, int /*DWORD*/ nNumberOfBytesToRead, IntByReference /*LPDWORD*/ lpNumberOfBytesRead, Pointer /*LPOVERLAPPED*/ lpOverlapped);
	@Override
	public native boolean /*BOOL*/   ReadFile(HANDLE hFile, byte[] lpBuffer, int /*DWORD*/ nNumberOfBytesToRead, IntByReference /*LPDWORD*/ lpNumberOfBytesRead, OVERLAPPEDEX /*LPOVERLAPPED*/ lpOverlapped);
	@Override
	public native boolean /*BOOL*/   WriteFile(HANDLE hFile, Pointer buf, int wrn, int[] nwrtn, Pointer /*OVERLAPPED*/ lpOverlapped);
	@Override
	public native boolean /*BOOL*/   FlushFileBuffers(HANDLE hFile);
	@Override
	public native boolean /*BOOL*/   GetOverlappedResult(HANDLE hFile, Pointer /*OVERLAPPED*/ lpOverlapped, IntByReference lpNumberOfBytesTransferred, boolean bWait);
	
	@Override
	public native boolean /*BOOL*/   SetCommMask(HANDLE hFile, int dwEvtMask);
	@Override
	public native boolean /*BOOL*/   WaitCommEvent(HANDLE hFile, IntByReference lpEvtMask, OVERLAPPEDEX lpOverlapped);
	//</editor-fold>
}
