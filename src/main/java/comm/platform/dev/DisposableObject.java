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
package comm.platform.dev;

import comm.IDisposable;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Creates an object that can be manually or automatically cleaned up.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class DisposableObject implements IDisposable {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private volatile boolean disposed = false;
	private static final Object disposeObjectLock = new Object();
	private static final LinkedList<IDisposable> disposeObjects = new LinkedList<IDisposable>();
	private static final Thread disposeShutdownHook;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Init">
	static {
		Runtime.getRuntime().addShutdownHook(disposeShutdownHook = new Thread(new Runnable() {
			@Override
			public void run() {
				IDisposable d;
				synchronized(disposeObjectLock) {
					try {
						while((d = disposeObjects.pop()) != null)
							if (!d.isDisposed())
								d.dispose();
					} catch(NoSuchElementException nsee) {
					}
				}
			}
		}));
	}
	
	@SuppressWarnings("LeakingThisInConstructor")
	public DisposableObject() {
		synchronized(disposeObjectLock) {
			disposeObjects.push(this);
		}
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	public final boolean isDisposed() {
		synchronized(this) {
			return disposed;
		}
	}
	
	@Override
	public final void dispose() {
		synchronized(this) {
			if (disposed)
				return;
			
			synchronized(disposeObjectLock) {
				disposed = true;
				disposeObjects.remove(this);
				disposeObject();
			}
		}
	}
	
	protected void disposeObject() {
	}
	
	@Override
	protected final void finalize() throws Throwable {
		super.finalize();
		dispose();
	}
	//</editor-fold>
}
