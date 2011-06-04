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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import static comm.platform.api.win32.API.*;

/**
 * Calls accessing the Win32 registry. 
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface RegistryAPI extends com.sun.jna.Library {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String
		  LIBRARY_NAME = "Advapi32"
	;
	
	public static final RegistryAPI
		INSTANCE = RegistryAPIDirect.loadLibrary()
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Util">
	public static class Util {
		public static String RegQueryKeyStringValue(HKEY hKey, String lpValueName) {
			return RegQueryKeyStringValue(null, hKey, lpValueName);
		}
		
		public static String RegQueryKeyStringValue(RegistryAPI API, HKEY hKey, String lpValueName) {
			if (API == null)
				API = RegistryAPI.INSTANCE;
			
			try {
				int rc;
				IntByReference lpcbData = new IntByReference();
				IntByReference lpType = new IntByReference();
				
				rc = API.RegQueryValueEx(hKey, lpValueName, 0, lpType, (char[]) null, lpcbData);
				if (rc != ERROR_SUCCESS && rc != ERROR_INSUFFICIENT_BUFFER)
					return null;
				if (lpType.getValue() != REG_SZ)
					throw new RuntimeException("Unexpected registry type " + lpType.getValue() + ", expected REG_SZ");
				
				char[] data = new char[lpcbData.getValue()];
				rc = API.RegQueryValueEx(hKey, lpValueName, 0, lpType, data, lpcbData);
				if (rc != ERROR_SUCCESS && rc != ERROR_INSUFFICIENT_BUFFER)
					return null;

				return Native.toString(data);
			} catch(Exception e) {
				return null;
			}
		}
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="API">
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final int 
		  KEY_QUERY_VALUE                   = 0x0001
		, KEY_SET_VALUE                     = 0x0002
		, KEY_CREATE_SUB_KEY                = 0x0004
		, KEY_ENUMERATE_SUB_KEYS            = 0x0008
		, KEY_NOTIFY                        = 0x0010
		, KEY_CREATE_LINK                   = 0x0020
		, KEY_WOW64_32KEY                   = 0x0200
		, KEY_WOW64_64KEY                   = 0x0100
		, KEY_WOW64_RES                     = 0x0300
	;
	
	public static final int 
		  KEY_READ                          = STANDARD_RIGHTS_READ | KEY_QUERY_VALUE | KEY_ENUMERATE_SUB_KEYS | KEY_NOTIFY & (~SYNCHRONIZE)
		, KEY_WRITE                         = STANDARD_RIGHTS_WRITE | KEY_SET_VALUE | KEY_CREATE_SUB_KEY & (~SYNCHRONIZE)
		, KEY_EXECUTE                       = KEY_READ & (~SYNCHRONIZE)
		, KEY_ALL_ACCESS                    = STANDARD_RIGHTS_ALL | KEY_QUERY_VALUE | KEY_SET_VALUE | KEY_CREATE_SUB_KEY | KEY_ENUMERATE_SUB_KEYS | KEY_NOTIFY | KEY_CREATE_LINK & (~SYNCHRONIZE)
	;
	
	public static final int 
		  REG_NONE                          = 0
		, REG_SZ                            = 1
		, REG_EXPAND_SZ                     = 2
		, REG_BINARY                        = 3
		, REG_DWORD                         = 4
		, REG_DWORD_LITTLE_ENDIAN           = 4
		, REG_DWORD_BIG_ENDIAN              = 5
		, REG_LINK                          = 6
		, REG_MULTI_SZ                      = 7
		, REG_RESOURCE_LIST                 = 8
		, REG_FULL_RESOURCE_DESCRIPTOR      = 9
		, REG_RESOURCE_REQUIREMENTS_LIST    = 10
		, REG_QWORD                         = 11
		, REG_QWORD_LITTLE_ENDIAN           = 11
	;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Types">
	public static class HKEY extends HANDLE {
		public HKEY() {
		}
		
		public HKEY(Pointer p) {
			super(p);
		}
		
		public HKEY(int value) {
			super(new Pointer(value));
		}
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Structs">
	//</editor-fold>
	
	/**
	 * Retrieves the type and data for the specified value name associated with an open registry key.
	 * 
	 * @param hKey A handle to an open registry key. The key must have been opened with the KEY_QUERY_VALUE access right.
	 * @param lpValueName The name of the registry value.
	 * @param lpReserved This parameter is reserved and must be NULL.
	 * @param lpType 
	 *        A pointer to a variable that receives a code indicating the type of data stored in the specified value. The lpType parameter can be NULL if the 
	 *        type code is not required.
	 * @param lpData A pointer to a buffer that receives the value's data. This parameter can be NULL if the data is not required.
	 * @param lpcbData 
	 *        A pointer to a variable that specifies the size of the buffer pointed to by the lpData parameter, in bytes. 
	 * 
	 *        When the function returns, this variable contains the size of the data copied to lpData. The lpcbData parameter can be NULL only if lpData is NULL.
	 *        If the data has the REG_SZ, REG_MULTI_SZ or REG_EXPAND_SZ type, this size includes any terminating null character or characters unless the data was 
	 *        stored without them. 
	 * 
	 *        If the buffer specified by lpData parameter is not large enough to hold the data, the function returns ERROR_MORE_DATA and stores the required buffer 
	 *        size in the variable pointed to by lpcbData. In this case, the contents of the lpData buffer are undefined.
	 * 
	 *        If lpData is NULL, and lpcbData is non-NULL, the function returns ERROR_SUCCESS and stores the size of the data, in bytes, in the variable pointed to 
	 *        by lpcbData. This enables an application to determine the best way to allocate a buffer for the value's data.
	 * 
	 *        If hKey specifies HKEY_PERFORMANCE_DATA and the lpData buffer is not large enough to contain all of the returned data, RegQueryValueEx returns 
	 *        ERROR_MORE_DATA and the value returned through the lpcbData parameter is undefined. This is because the size of the performance data can change from one 
	 *        call to the next. In this case, you must increase the buffer size and call RegQueryValueEx again passing the updated buffer size in the lpcbData parameter. 
	 *        Repeat this until the function succeeds. You need to maintain a separate variable to keep track of the buffer size, because the value returned by lpcbData 
	 *        is unpredictable.
	 * 
	 *        If the lpValueName registry value does not exist, RegQueryValueEx returns ERROR_FILE_NOT_FOUND and the value returned through the lpcbData parameter is 
	 *        undefined.
	 * @return If the function succeeds, the return value is ERROR_SUCCESS.
	 */
	public int RegQueryValueEx(HKEY hKey, String lpValueName, int lpReserved, IntByReference lpType, char[] lpData, IntByReference lpcbData);
	public int RegQueryValueEx(HKEY hKey, String lpValueName, int lpReserved, IntByReference lpType, byte[] lpData, IntByReference lpcbData);
	public int RegQueryValueEx(HKEY hKey, String lpValueName, int lpReserved, IntByReference lpType, IntByReference lpData, IntByReference lpcbData);
	public int RegQueryValueEx(HKEY hKey, String lpValueName, int lpReserved, IntByReference lpType, Pointer lpData, IntByReference lpcbData);
	
	/**
	 * The RegCloseKey function releases a handle to the specified registry key.
	 * 
	 * @param hKey Handle to the open key to be closed. The handle must have been opened by the RegCreateKeyEx, RegOpenKeyEx, or RegConnectRegistry function.
	 * @return If the function succeeds, the return value is ERROR_SUCCESS. If the function fails, the return value is a nonzero error code defined in Winerror.h.
	 */
	public int RegCloseKey(HKEY hKey);
	//</editor-fold>
}
