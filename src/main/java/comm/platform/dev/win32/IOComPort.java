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

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import comm.ICommPort;
import comm.platform.api.win32.CommAPI;
import comm.platform.api.win32.CommAPI.OVERLAPPED;
import comm.platform.api.win32.IOComPortsAPI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jtermios.windows.WinAPI;
import static comm.platform.api.win32.API.*;
import static comm.platform.api.win32.IOComPortsAPI.*;

/**
 * Manages our global IO completion port.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class IOComPort {
	private static final Object portLock = new Object();
	private static final AtomicInteger portCount = new AtomicInteger(0);
	private static final Map<HANDLE, PortInfo> ports = new ConcurrentHashMap<HANDLE, PortInfo>(8);
	private static HANDLE ioCompletionPort = INVALID_HANDLE_VALUE;
	private static List<ThreadInfo> ioCompletionPortServiceThreads = null;
	
	private static class PortInfo {
		HANDLE port;
		Memory readBuffer;
		int readBufferSize;
		IntByReference pBytesRead;
		IntByReference pEventMask;
		OVERLAPPEDEX waitCommOverlapped;
		Pointer pWaitCommOverlapped;
		OVERLAPPEDEX waitCommImmediateOverlapped;
		Pointer pWaitCommImmediateOverlapped;
		OVERLAPPEDEX readOverlapped;
		Pointer pReadOverlapped;
		
		public PortInfo(HANDLE port, int readBufferSize) {
			this.port = port;
			this.readBufferSize = (readBufferSize > 0 ? readBufferSize : 2048);
			this.pEventMask = new IntByReference();
			this.pBytesRead = new IntByReference();
			this.readBuffer = new Memory(this.readBufferSize);
			
			//<editor-fold defaultstate="collapsed" desc="Read">
			this.readOverlapped = new OVERLAPPEDEX();
			this.readOverlapped.op = OVERLAPPEDEX.OP_READ;
			this.readOverlapped.write();
			this.pReadOverlapped = this.readOverlapped.getPointer();
			//</editor-fold>
			
			//<editor-fold defaultstate="collapsed" desc="WaitComm">
			this.waitCommOverlapped = new OVERLAPPEDEX();
			this.waitCommOverlapped.op = OVERLAPPEDEX.OP_WAITCOMMEVENT;
			this.waitCommOverlapped.write();
			this.pWaitCommOverlapped = this.waitCommOverlapped.getPointer();
			//</editor-fold>
			
			//<editor-fold defaultstate="collapsed" desc="WaitCommImmediate">
			this.waitCommImmediateOverlapped = new OVERLAPPEDEX();
			this.waitCommImmediateOverlapped.op = OVERLAPPEDEX.OP_WAITCOMMEVENT_IMMEDIATE;
			this.waitCommImmediateOverlapped.ev = IOComPortsAPI.INSTANCE.CreateEvent(null, false, false, null);
			this.waitCommImmediateOverlapped.write();
			this.pWaitCommImmediateOverlapped = this.waitCommImmediateOverlapped.getPointer();
			//</editor-fold>
		}
		
		public Object getLock() {
			return this;
		}
		
		public boolean dispose() {
			//Clean up native resources.
			if (this.waitCommImmediateOverlapped.ev != null && this.waitCommImmediateOverlapped.ev != INVALID_HANDLE_VALUE) {
				IOComPortsAPI.INSTANCE.CloseHandle(this.waitCommImmediateOverlapped.ev);
				this.waitCommImmediateOverlapped.ev = null;
			}
			return true;
		}
	}
	
	private static class ThreadInfo {
		public Thread thread;
		public HANDLE completionPort;
		public boolean pleaseExit = false;
		public CountDownLatch threadExited = new CountDownLatch(1);
		
		public ThreadInfo(Thread thread, HANDLE completionPort) {
			this.thread = thread;
			this.completionPort = completionPort;
		}
	}
	
	public static boolean associateCommPort(HANDLE port) {
		synchronized(portLock) {
			//Verify that we don't already hold this port.
			if (!ports.containsKey(port)) {
				//If this is the first port we're adding, we'll need to create an unassociated IO completion port.
				if (portCount.incrementAndGet() == 1) {
					//Determine the number of concurrent threads that IOCP will use. Typically it's best to actually 
					//create twice as many as the value passed to CreateIoCompletionPort().
					Integer concurrentThreadCount = SystemHint.hint(SystemHint.IOCompletionPortNumberOfConcurrentThreads);
					int threadCount = (concurrentThreadCount == null || concurrentThreadCount < 0 ? ICommPort.DEFAULT_THREAD_POOL_SIZE : concurrentThreadCount.intValue());
					ThreadFactory threadFactory = SystemHint.hint(SystemHint.IOCompletionPortThreadFactory);
					if (threadFactory == null)
						threadFactory = Executors.defaultThreadFactory();
					
					//Create the completion port.
					HANDLE completionPort = ioCompletionPort = IOComPortsAPI.Util.CreateUnassociatedIoCompletionPort();
					 
					//Spin up each service thread.
					//The call will block until all the threads have started.
					ioCompletionPortServiceThreads = launchServiceThreads(threadCount * 2, threadFactory, completionPort);
				}
				
				//Now associate our open file handle with the IO completion port.
				if (!IOComPortsAPI.Util.AssociateHandleWithIoCompletionPort(ioCompletionPort, port, port.getPointer())) {
					portCount.decrementAndGet();
					return false;
				}
				
				//Add our port to the list.
				IOComPortsAPI API = IOComPortsAPI.INSTANCE;
				PortInfo pi = new PortInfo(port, 64);
				ports.put(port, pi);
				
				//Specify which events we're interested in knowing about.
				API.SetCommMask(port, CommAPI.EV_RXCHAR | CommAPI.EV_TXEMPTY);
				
				waitCommEvent(API, port, pi, false);
					
				//If not, then there's something else amiss
				//System.out.println("LAST WAITCOMMEVENT ERR: " + WinAPI.GetLastError());
//				//overlapped.hEvent = IOComPortsAPI.INSTANCE.CreateEvent(null, true, false, null);
//				if (!IOComPortsAPI.INSTANCE.ReadFile(port, buff, 1024, pBytesRead, overlapped)) {
//					System.out.println("LAST READFILE ERR: " + Native.getLastError());
//					return false;
//				}
//				System.out.println("LAST READFILE ERR: " + Native.getLastError());
//				System.out.println("BYTES READ: " + pBytesRead.getValue());
				
				//Add the port to our list
				
				return true;
			}
		}
		return false;
	}
	
	private static List<ThreadInfo> launchServiceThreads(final int threadCount, final ThreadFactory threadFactory, final HANDLE completionPort) {
		//Create a pool of threads and keep hold of them.
		final CountDownLatch counter = new CountDownLatch(threadCount);
		final List<ThreadInfo> serviceThreads = new CopyOnWriteArrayList<ThreadInfo>();
		
		try {
			for(int i = 0; i < threadCount; ++i) {
				//Create a new thread and leave it up and running.
				Thread t = threadFactory.newThread(new Runnable() {
					@Override
					public void run() {
						ThreadInfo ti = null;
						try {
							if (false)
								throw new InterruptedException();
							ti = new ThreadInfo(Thread.currentThread(), completionPort);
							serviceThreads.add(ti);
							counter.countDown();
							serviceThread(ti);
						} catch(Throwable t) {
						} finally {
							if (ti != null) {
								serviceThreads.remove(ti);
								ti.threadExited.countDown();
							}
						}
					}
				});
				t.start();
			}
			counter.await();
			return serviceThreads;
		} catch(Throwable t) {
			//Destroy running threads
			shutdownServiceThreads(serviceThreads);
			return null;
		}
	}
	
	private static void shutdownServiceThreads(List<ThreadInfo> serviceThreads) {
		//<editor-fold defaultstate="collapsed" desc="Setup params">
		if (serviceThreads == null || serviceThreads.isEmpty())
			return;
		IOComPortsAPI API = IOComPortsAPI.INSTANCE;
		OVERLAPPEDEX post = new OVERLAPPEDEX();
		post.op = OVERLAPPEDEX.OP_EXITTHREAD;
		//</editor-fold>

		//TODO: Find a more intelligent way to do this that does not require pounding on the worker 
		//      threads until they see that they should exit.
		for(ThreadInfo ti : serviceThreads) {
			if (ti == null || !ti.thread.isAlive())
				continue;
			
			//Ask the thread to exit nicely.
			ti.pleaseExit = true;
			
			//Post message to thread.
			API.PostQueuedCompletionStatus(ioCompletionPort, 0, null, post);
		}
		
		boolean allDone;
		do {
			allDone = true;
			//This list will get gradually smaller and smaller since as threads exit 
			//they remove themselves from the list.
			for(ThreadInfo ti : serviceThreads) {
				//If the thread hasn't died yet, then forcefully remove it.
				try {
					//Ensure that the thread has exited.
					if (!ti.thread.isAlive() && !ti.threadExited.await(1L, TimeUnit.MILLISECONDS)) {
						//Post message again.
						API.PostQueuedCompletionStatus(ioCompletionPort, 0, null, post);
						allDone = false;
					}
				} catch(Throwable w) {
				}
			}
		} while(!allDone);
		
		serviceThreads.clear();
	}
	
	private static void serviceThread(ThreadInfo ti) throws Throwable {
		HANDLE completionPort = ti.completionPort;
		HANDLE port = new HANDLE();
		IntByReference pBytesTransferred = new IntByReference();
		OVERLAPPEDEX overlapped = new OVERLAPPEDEX();
		IOComPortsAPI API = IOComPortsAPI.INSTANCE;
		int bytesTransferred;
		PointerByReference ppOverlapped = new PointerByReference();//overlapped.getPointer());
		PointerByReference pCompletionKey = new PointerByReference();
		
		IntByReference pBytesRead = new IntByReference();
		Pointer pOverlapped;
		PortInfo pi;
		
		//DOH -- need memory that's associated WITH the file handle...
		Memory mem = new Memory(1024);
		
		while(!ti.pleaseExit) {
			//Retrieve the queued event and then examine it.
			if (!API.GetQueuedCompletionStatus(completionPort, pBytesTransferred, pCompletionKey, ppOverlapped, INFINITE)) 
				return;
			
			//If no OVERLAPPED/OVERLAPPEDEX instance is specified, then there's 
			//something wrong and we need to exit.
			if (ppOverlapped == null || (pOverlapped = ppOverlapped.getValue()) == null || pOverlapped == Pointer.NULL)
				return;
			
			//Retrieve data from the event.
			overlapped.reuse(pOverlapped);
			port.reuse(pCompletionKey.getValue());
			bytesTransferred = pBytesTransferred.getValue();
			
			//If, for some unknown reason, we are processing an event for a port we 
			//haven't seen before, then go ahead and ignore it.
			if ((pi = ports.get(port)) == null)
				continue;
			
			//If we've received a message asking to break out of the thread, then 
			//loop back and around and check that our flag has been set. If so, 
			//then it's time to go!
			if (overlapped.op == OVERLAPPEDEX.OP_EXITTHREAD)
				continue;
			
			if (bytesTransferred <= 0) {
				//close file
				//notify
				//System.out.println("NO BYTES RECVD!! :(");
			} else {
				//System.out.println("GOT SOME BYTES: " + bytesTransferred);
			}
			
			switch(overlapped.op) {
				case OVERLAPPEDEX.OP_WAITCOMMEVENT:
					if (!API.GetOverlappedResult(port, pOverlapped, pBytesTransferred, false))
						continue;
					evaluateCommEvent(API, port, pi, pi.pEventMask.getValue());
					break;
				case OVERLAPPEDEX.OP_WAITCOMMEVENT_IMMEDIATE:
					//The events found (event mask) were placed in .ex so it needs to be 
					//evaluated and then the calling thread that's currently blocking waiting 
					//for this to complete can continue.
					evaluateCommEvent(API, port, pi, overlapped.ex);
					API.SetEvent(overlapped.ev);
					break;
				case OVERLAPPEDEX.OP_READ:
					if (!API.GetOverlappedResult(port, pOverlapped, pBytesTransferred, false))
						continue;
					
					if (bytesTransferred > 0) {
						//Notify application!
						//System.out.println("" + Charset.forName("ASCII").newDecoder().decode(pi.readBuffer.getByteBuffer(0, bytesTransferred)).toString());
						System.out.println("" + new String(pi.readBuffer.getByteArray(0, bytesTransferred)));
					}
					//Read again
					if (bytesTransferred <= 0 || !read(API, port, pi))
						waitCommEvent(API, port, pi, true);
					break;
			}
		}
	}
	
	private static void waitCommEvent(IOComPortsAPI API, HANDLE port, PortInfo pi, boolean iocpThread) {
		//This will typically return false and GetLastError() should return ERROR_IO_PENDING.
		//If it's successful, then there are events to be evaluated right away. Go ahead and 
		//post them to the IOCP but then block until they've been processed. That way, we 
		//can ensure that all our event firing, reading, and writing are always done from the 
		//worker threads. It's just nicer for consistency's sake.
		while (API.WaitCommEvent(port, pi.pEventMask, pi.waitCommOverlapped) && !iocpThread) {
			//Set the event mask so we can pick it up inside the IOCP worker thread.
			pi.waitCommImmediateOverlapped.ex = pi.pEventMask.getValue();
			//Post the event.
			API.PostQueuedCompletionStatus(ioCompletionPort, Integer.SIZE / Byte.SIZE /* 4 bytes */, port.getPointer(), pi.waitCommImmediateOverlapped);
			//Wait for the IOCP worker thread to signal that it's done processing it.
			API.WaitForSingleObject(pi.waitCommImmediateOverlapped.ev, INFINITE);
			//evaluateCommEvent(pi.eventMask.getValue());
		}
	}
	
	private static void evaluateCommEvent(IOComPortsAPI API, HANDLE port, PortInfo pi, int eventMask) {
		if ((eventMask & CommAPI.EV_RXCHAR) == CommAPI.EV_RXCHAR)
			read(API, port, pi);
		else if ((eventMask & CommAPI.EV_TXEMPTY) == CommAPI.EV_TXEMPTY)
			writeComplete(API, port, pi);
	}
	
	private static boolean read(IOComPortsAPI API, HANDLE port, PortInfo pi) {
		return API.ReadFile(port, pi.readBuffer, pi.readBufferSize, pi.pBytesRead, pi.pReadOverlapped);
	}
	
	private static boolean writeComplete(IOComPortsAPI API, HANDLE port, PortInfo pi) {
		return true;
	}
	
	public static boolean unassociateCommPort(HANDLE port) {
		PortInfo pi;
		synchronized(portLock) {
			//Verify that this port is actually managed by this IOCP and if so, then 
			//clean up any native resources before we unassociate it.
			if (ports.containsKey(port) && (pi = ports.remove(port)) != null && pi.dispose()) {
				//Unassociate this port from the IO completion port.
				
				if (portCount.decrementAndGet() == 0) {
					//Stop the threads in the pool.
					shutdownServiceThreads(ioCompletionPortServiceThreads);
					
					if (ioCompletionPort != INVALID_HANDLE_VALUE) {
						IOComPortsAPI.INSTANCE.CloseHandle(ioCompletionPort);
						ioCompletionPort = INVALID_HANDLE_VALUE;
					}
				}
				
				return true;
			}
		}
		return false;
	}
}
