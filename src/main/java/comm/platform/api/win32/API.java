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

import com.sun.jna.FromNativeContext;
import com.sun.jna.IntegerType;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import comm.util.StringUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class API {
	//<editor-fold defaultstate="collapsed" desc="Util">
	public static class Util {
		//<editor-fold defaultstate="collapsed" desc="Regular Expressions">
		public static final String
			  REGEX_SERIAL_PORT_NAME_MATCH = "^COM\\d+$"
			, REGEX_PARALLEL_PORT_NAME_MATCH = "^LPT\\d+$"
		;

		public static final Pattern
			  REGEX_SERIAL_PORT_NAME_MATCH_PATTERN = Pattern.compile(REGEX_SERIAL_PORT_NAME_MATCH, Pattern.CASE_INSENSITIVE)
			, REGEX_PARALLEL_PORT_NAME_MATCH_PATTERN = Pattern.compile(REGEX_PARALLEL_PORT_NAME_MATCH)
		;
		//</editor-fold>
		
		/**
		 * Determines if a potential name matches a Win32 serial port naming scheme.
		 * @param value The value to check.
		 * @return True if the name seems to be a Win32 serial port name.
		 */
		public static boolean isSerialPortNameMatch(String value) {
			if (StringUtil.isNullOrEmpty(value))
				return false;
			Matcher m = REGEX_SERIAL_PORT_NAME_MATCH_PATTERN.matcher(value);
			if (m == null)
				return false;
			return m.find();
		}
		
		/**
		 * Determines if a potential name matches a Win32 parallel port naming scheme.
		 * @param value The value to check.
		 * @return True if the name seems to be a Win32 parallel port name.
		 */
		public static boolean isParallelPortNameMatch(String value) {
			if (StringUtil.isNullOrEmpty(value))
				return false;
			Matcher m = REGEX_PARALLEL_PORT_NAME_MATCH_PATTERN.matcher(value);
			if (m == null)
				return false;
			return m.find();
		}
		
		/**
		 * Determines if a potential name matches any Win32 port naming scheme.
		 * @param value The value to check.
		 * @return True if the name seems to be a Win32 port name.
		 */
		public static boolean isAnyPortNameMatch(String value) {
			return (
				   isSerialPortNameMatch(value) 
				|| isParallelPortNameMatch(value)
			);
		}
		
		public static HANDLE newINVALID_HANDLE_VALUE() {
			HANDLE h = new HANDLE();
			h.setPointer(Pointer.createConstant(-1));
			return h;
		}
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="API">
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final int 
		  ERROR_SUCCESS             = 0
		, ERROR_NOT_FOUND           = 0x490
		, ERROR_INSUFFICIENT_BUFFER = 0x7A
			
		, ERROR_IO_INCOMPLETE       = 996
		, ERROR_IO_PENDING          = 997
		, ERROR_BROKEN_PIPE         = 109
		, ERROR_MORE_DATA           = 234
		, ERROR_FILE_NOT_FOUND      = 2
	;
	
	public static final int 
		  DELETE                    = 0x00010000
		, READ_CONTROL              = 0x00020000
		, WRITE_DAC                 = 0x00040000
		, WRITE_OWNER               = 0x00080000
		, SYNCHRONIZE               = 0x00100000
	;
	
	public static final int 
		  GENERIC_READ              = 0x80000000
		, GENERIC_WRITE             = 0x40000000
		, GENERIC_EXECUTE           = 0x20000000
		, GENERIC_ALL               = 0x10000000
	;
	
	public static final int 
		  CREATE_NEW                = 1
		, CREATE_ALWAYS             = 2
		, OPEN_EXISTING             = 3
		, OPEN_ALWAYS               = 4
		, TRUNCATE_EXISTING         = 5
	;
	
	public static final int 
		  STANDARD_RIGHTS_REQUIRED  = 0x000F0000
		, STANDARD_RIGHTS_READ      = READ_CONTROL
		, STANDARD_RIGHTS_WRITE     = READ_CONTROL
		, STANDARD_RIGHTS_EXECUTE   = READ_CONTROL
		, STANDARD_RIGHTS_ALL       = 0x001F0000
		, SPECIFIC_RIGHTS_ALL       = 0x0000FFFF
	;
	
	public static final int 
		  STATUS_WAIT_0             = 0x00000000
		, STATUS_ABANDONED_WAIT_0   = 0x00000080
	;
	
	public static final int 
		  WAIT_ABANDONED_0          = (STATUS_ABANDONED_WAIT_0) + 0
		, WAIT_OBJECT_0             = ((STATUS_WAIT_0) + 0)
		, WAIT_FAILED               = 0xFFFFFFFF
		, WAIT_TIMEOUT              = 258
	;
	
	public static final int 
		  INFINITE                  = 0xFFFFFFFF
	;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Types">
	public static final long 
		  INVALID_HANDLE_VALUE_AS_LONG = -1L
	;
	
	public static final HANDLE INVALID_HANDLE_VALUE = new HANDLE() {
		{ super.setPointer(Pointer.createConstant(INVALID_HANDLE_VALUE_AS_LONG)); }
		
		@Override
		public void setPointer(Pointer p) {
			throw new UnsupportedOperationException("Immutable reference");
		}
	};
	
	public static class HANDLE extends PointerType {
		private boolean immutable;
		
		public HANDLE() {
		}
		
		public HANDLE(Pointer p) {
			setPointer(p);
			immutable = true;
		}
		
		public void reuse(Pointer p) {
			setPointer(p);
		}
		
		@Override
		public Object fromNative(Object nativeValue, FromNativeContext context) {
			Object o = super.fromNative(nativeValue, context);
			if (INVALID_HANDLE_VALUE.equals(o))
				return INVALID_HANDLE_VALUE;
			return o;
		}
		
		@Override
		public void setPointer(Pointer p) {
			if (immutable)
				throw new UnsupportedOperationException("immutable reference");
			super.setPointer(p);
		}
	}
	
	public static class HWND extends HANDLE {
	}
	
	public static class ULONG_PTR extends IntegerType {
		public ULONG_PTR() {
			this(0);
		}

		public ULONG_PTR(long value) {
			super(Pointer.SIZE, value);
		}
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Structs">
	public static class GUID extends Structure {
		public int Data1;
		public short Data2;
		public short Data3;
		public byte Data4[] = new byte[8];
		
		public GUID() {
		}
		
		public GUID(Pointer memory) {
			useMemory(memory);
			read();
		}
		
		public static class ByValue extends GUID implements Structure.ByValue { }
		public static class ByReference extends GUID implements Structure.ByReference { public ByReference() { } public ByReference(Pointer memory) { super(memory); } }
	}
	
	public static class SECURITY_ATTRIBUTES extends Structure {
		public int nLength;
		public Pointer lpSecurityDescriptor;
		public boolean bInheritHandle;
	}
	//</editor-fold>
	//</editor-fold>
}
