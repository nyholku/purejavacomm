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

import com.sun.jna.Library;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides some common functionality for Win32 libraries. The options handle 
 * mapping function names to platform-specific unicode and ascii equivalents.
 * 
 * Courtesy JNA project:
 *     https://jna.dev.java.net/source/browse/jna/trunk/jnalib/src/com/sun/jna/examples/win32/W32API.java?rev=963&view=markup
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
@SuppressWarnings("unchecked")
public interface Win32Library extends StdCallLibrary, Win32Errors {
	//<editor-fold defaultstate="collapsed" desc="Options">
	/** Standard options to use the unicode version of a w32 API. */
	public static final Map UNICODE_OPTIONS = new HashMap() {{
			put(Library.OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
			put(Library.OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
	}};

	/** Standard options to use the ASCII/MBCS version of a w32 API. */
	public static final Map ASCII_OPTIONS = new HashMap() {{
			put(Library.OPTION_TYPE_MAPPER, W32APITypeMapper.ASCII);
			put(Library.OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.ASCII);
	}};
	
	public static final Map DEFAULT_OPTIONS = Boolean.getBoolean("w32.ascii") ? ASCII_OPTIONS : UNICODE_OPTIONS;
	//</editor-fold>
}
