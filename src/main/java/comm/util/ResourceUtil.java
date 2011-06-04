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
package comm.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import comm.util.StringUtil;

/**
 * Utility class.
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class ResourceUtil {
	public static ExecutorService createPrivilegedExecutorService() {
		return Executors.newSingleThreadExecutor(createPrivilegedThreadFactory());
	}

	public static ThreadFactory createPrivilegedThreadFactory() {
		return AccessController.doPrivileged(new PrivilegedAction<ThreadFactory>() {
			public ThreadFactory run() {
				return Executors.privilegedThreadFactory();
			}
		});
	}

	public static ExecutorService createUnprivilegedExecutorService() {
		return Executors.newSingleThreadExecutor(createUnprivilegedThreadFactory());
	}

	public static ThreadFactory createUnprivilegedThreadFactory() {
		return Executors.defaultThreadFactory();
	}

	//Thank you http://forums.sun.com/thread.jspa?threadID=341935
	@Deprecated
	public static Class[] getClasses(final String pckgname) throws ClassNotFoundException {
		ArrayList<Class> classes = new ArrayList<Class>(3);
		
		// Get a File object for the package
		File directory = null;
		try {
			ClassLoader cld = Thread.currentThread().getContextClassLoader();
			if (cld == null)
				throw new ClassNotFoundException("Can't get class loader.");

			String path = pckgname.replace('.', '/');
			URL resource = cld.getResource(path);
			if (resource == null)
				throw new ClassNotFoundException("No resource for " + path);

			directory = new File(resource.getFile());
		} catch (NullPointerException x) {
			throw new ClassNotFoundException(pckgname + " (" + directory + ") does not appear to be a valid package");
		}

		if (directory.exists()) {
			// Get the list of the files contained in the package
			String[] files = directory.list();
			for (int i = 0; i < files.length; i++) {
				// we are only interested in .class files
				if (files[i].endsWith(".class")) {
					// removes the .class extension
					classes.add(Class.forName(pckgname + '.' + files[i].substring(0, files[i].length() - 6)));
				}
			}
		} else {
			throw new ClassNotFoundException(pckgname + " does not appear to be a valid package");
		}

		Class[] classesA = new Class[classes.size()];
		classes.toArray(classesA);
		return classesA;
	}

	public static boolean cleanDirectory(final File path) {
		if (path == null)
			return false;
		if (!path.exists())
			return true;

		boolean ret = true;
		final File[] files = path.listFiles();
		for(int i = 0; i < files.length; ++i) {
			try {
				ret = ret && files[i].delete();
			} catch(Throwable t) {
				//Catches any security issues, but continues w/ cleaning anyway
				ret = false;
			}
		}
		return ret;
	}

	public static boolean deleteDirectory(final File path) {
		if (path == null)
			return false;
		if (path.exists()) {
			final File[] files = path.listFiles();
			for(int i = 0; i < files.length; ++i) {
				if (files[i].isDirectory())
					deleteDirectory(files[i]);
				else
					files[i].delete();
			}
		}
		return path.delete();
	}

	public static boolean attemptLibraryLoad(final String libraryPath) {
		System.load(libraryPath);
		return (com.sun.jna.NativeLibrary.getInstance(libraryPath) != null);
	}

	public static boolean attemptSystemLibraryLoad(final String libraryName) {
		try {
			return (com.sun.jna.NativeLibrary.getInstance(libraryName) != null);
		} catch(Throwable t) {
			return false;
		}
	}

	public static long sizeFromResource(final String fullResourceName) {
		if (StringUtil.isNullOrEmpty(fullResourceName))
			return 0L;
		final URL url = ResourceUtil.class.getResource(fullResourceName);
		if (url == null)
			return 0L;
		try {
			return url.openConnection().getContentLength();
		} catch(IOException ie) {
			return 0L;
		}
	}

	public static long lastModifiedFromResource(final String fullResourceName) {
		if (StringUtil.isNullOrEmpty(fullResourceName))
			return 0L;
		final URL url = ResourceUtil.class.getResource(fullResourceName);
		if (url == null)
			return 0L;
		try {
			return url.openConnection().getLastModified();
		} catch(IOException ie) {
			return 0L;
		}
	}

	public static boolean saveLastModified(final File destination, final long lastModified) {
		try {
			if (lastModified > 0L)
				destination.setLastModified(lastModified);
			return true;
		} catch(Throwable t) {
			return false;
		}
	}

	public static boolean extractResource(final String fullResourceName, final File destination, final boolean isTransient) {
		//If the destination exists and it's last modified date/time is older than now,
		//then we can safely skip it. Otherwise, it must be replaced or created.
		long lastModified = ResourceUtil.lastModifiedFromResource(fullResourceName);
		if (destination.exists() && isTransient) {
			if (lastModified <= 0L)
				return false;
			if (destination.lastModified() >= lastModified)
				return true;
		}

		//Make any necessary directories
		final File parentDir = destination.getParentFile();
		if (!parentDir.exists())
			parentDir.mkdirs();
		if (!parentDir.exists() || !parentDir.isDirectory())
			return false;

		final byte[] buffer;
		InputStream input = null;
		OutputStream output = null;
		int read = 0;

		try {
			
			input = ResourceUtil.class.getResourceAsStream(fullResourceName);
			if (input == null)
				return false;

			buffer = new byte[4096];
			output = new FileOutputStream(destination);

			while((read = input.read(buffer, 0, buffer.length)) >= 0) {
				output.write(buffer, 0, read);
			}

			output.flush();
			return true;
		} catch(IOException ie) {
			return false;
		} finally {
			try {
				if (input != null)
					input.close();
			} catch(IOException ie) {
			}

			try {
				if (output != null)
					output.close();
			} catch(IOException ie) {
			}

			saveLastModified(destination, lastModified);
		}
	}
}
