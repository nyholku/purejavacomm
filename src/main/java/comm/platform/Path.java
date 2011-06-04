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
package comm.platform;

import comm.platform.api.win32.Win32Library;
import comm.util.ResourceUtil;
import com.sun.jna.Library;
import com.sun.jna.Native;
import java.io.File;
import comm.util.StringUtil;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Path {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String
		  directorySeparator = System.getProperty("file.separator")
		, pathSeparator = File.pathSeparator
	;

	public static final String
		  tempDirectory = clean(System.getProperty("java.io.tmpdir"))
		, homeDirectory = clean(System.getProperty("user.home"))
		, workingDirectory = clean(new File(".").getAbsolutePath())
	;
	
	public static final String 
		  nativeResourcesDirectoryName = "native_java_resources"
	;

	public static final String
		  nativeResourcesDirectory
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="JNA Library Declarations">
	interface PathLibraryWindows extends Win32Library {
		//<editor-fold defaultstate="collapsed" desc="Constants">
		public static final String
			LIB_NAME = ""
		;

		public static final PathLibraryWindows
			INSTANCE = null;//(PathLibraryWindows)Native.loadLibrary(LIB_NAME, PathLibraryWindows.class, DEFAULT_OPTIONS)
		;
		//</editor-fold>
	}

	interface PathLibraryUnix extends Library {
		//<editor-fold defaultstate="collapsed" desc="Constants">
		public static final String
			LIB_NAME = "c"
		;

		public static final PathLibraryUnix
			INSTANCE = (PathLibraryUnix)Native.loadLibrary(LIB_NAME, PathLibraryUnix.class);
		;
		//</editor-fold>

		public int symlink(final String to, final String from);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	private static Library nativelib;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		nativeResourcesDirectory = Path.combine(tempDirectory, nativeResourcesDirectoryName).getAbsolutePath();

		switch(OS.getSystemOSFamily()) {
			case Unix:
			case Mac:
				nativelib = PathLibraryUnix.INSTANCE;
				break;
			case Windows:
				nativelib = PathLibraryWindows.INSTANCE;
				break;
			default:
				break;
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static String nativeDirectorySeparator(final String path) {
		if (path == null)
			return StringUtil.empty;
		return path.replace("/", directorySeparator).replace("\\", directorySeparator);
	}

	public static String clean(final String path) {
		if (StringUtil.isNullOrEmpty(path))
			return StringUtil.empty;

		if (path.endsWith(directorySeparator))
			return path;

		if (path.endsWith("/") || path.endsWith("\\"))
			return path.substring(0, path.length() - 1) + directorySeparator;

		return path + directorySeparator;
	}

	public static File combine(final File parent, final String child) {
		return new File(parent, child);
	}

	public static File combine(final String parent, final String child) {
		return new File(parent, child);
	}

	public static boolean exists(final String path) {
		return exists(new File(path));
	}

	public static boolean exists(final File path) {
		if (path == null)
			return false;
		return path.exists();
	}

	public static boolean delete(final String path) {
		return delete(new File(path));
	}

	public static boolean delete(final File path) {
		if (path == null)
			return false;
		
		try {
			//True b/c the intent of this function is satisfied -- the directory/file no longer exists!
			if (!path.exists())
				return true;

			if (path.isFile())
				return path.delete();
			else
				return ResourceUtil.deleteDirectory(path);
		} catch(SecurityException se) {
			return false;
		}
	}

	public static boolean createSymbolicLink(final File to, final File from) {
		if (from == null || to == null)
			return false;
		return createSymbolicLink(to.getAbsolutePath(), from.getAbsolutePath());
	}
	
	public static boolean createSymbolicLink(final String to, final String from) {
		if (StringUtil.isNullOrEmpty(from) || StringUtil.isNullOrEmpty(to))
			throw new IllegalArgumentException("from and to cannot be empty");

		if (nativelib == null)
			throw new UnsupportedOperationException("Creating symbolic links is unsupported on this platform");

		if (nativelib instanceof PathLibraryUnix)
			return (PathLibraryUnix.INSTANCE.symlink(to, from) == 0);
		else if (nativelib instanceof PathLibraryWindows)
			return false;
		else
			throw new UnsatisfiedLinkError("Platform specific library for path manipulation is unavailable");
	}
	//</editor-fold>
}
