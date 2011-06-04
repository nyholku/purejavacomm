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
package comm.platform.api;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.win32.StdCallFunctionMapper;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class Library implements com.sun.jna.Library {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String[] DEFAULT_LIBRARY_NAME_FORMATS = {
		  "%s"
		, "lib%s"
		, "lib%s-0"
	};
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	private static AtomicBoolean initialized = new AtomicBoolean(false);
	//</editor-fold>

	static void init() {
		if (initialized.compareAndSet(true, true))
			return;
	}

	public static boolean isInitialized() {
		return initialized.get();
	}
	
	protected static <T> T directMapping(String libraryName, Class cls) {
		return directMapping(libraryName, null, Library.class);
	}
	
	protected static <T> T directMapping(String libraryName, Map options, Class cls) {
		for (int i = 0; i < DEFAULT_LIBRARY_NAME_FORMATS.length; ++i) {
			try {
				NativeLibrary lib;
				
				if (options != null)
					lib = NativeLibrary.getInstance(String.format(DEFAULT_LIBRARY_NAME_FORMATS[i], libraryName), options);
				else
					lib = NativeLibrary.getInstance(String.format(DEFAULT_LIBRARY_NAME_FORMATS[i], libraryName));
				
				if (lib == null)
					continue;
				
				Native.register(cls, lib);
				
			} catch (UnsatisfiedLinkError ex) {
				continue;
			}
			
			try {
				return (T)cls.newInstance();
			} catch(Throwable t) {
				return null;
			}
		}
		return null;
	}
	
	protected static <T> T interfaceMapping(String libraryName, Map options, Class cls) {
		for (int i = 0; i < DEFAULT_LIBRARY_NAME_FORMATS.length; ++i) {
			try {
				return (T)Native.loadLibrary(libraryName, cls, options);
			} catch (UnsatisfiedLinkError ex) {
				continue;
			}
		}
		return null;
	}
}
