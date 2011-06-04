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
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import comm.platform.api.Library;
import static comm.platform.api.win32.SetupAPI.*;
import static comm.platform.api.win32.API.*;

/**
 * Calls representing the Win32 SetupAPI library. 
 * This class is setup as a direct-mapped library for enhanced performance.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class SetupAPIDirect extends Library /*implements SetupAPI*/ {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public static SetupAPI loadLibrary() {
		//SetupAPI inst = directMapping(SetupAPI.LIBRARY_NAME, Win32Library.DEFAULT_OPTIONS, SetupAPIDirect.class);
		SetupAPI inst = interfaceMapping(SetupAPI.LIBRARY_NAME, Win32Library.DEFAULT_OPTIONS, SetupAPI.class);
		if (inst == null)
			throw new UnsatisfiedLinkError("Could not load library " + LIBRARY_NAME);
		return inst;
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="API">
	//@Override
	public native boolean SetupDiClassGuidsFromName(WString className, Pointer /*GUID[]*/ classGuidList, int classGuidListSize, IntByReference requiredSize);
	//@Override
	public native boolean SetupDiClassGuidsFromName(WString className, GUID[] classGuidList, int classGuidListSize, IntByReference requiredSize);
	//@Override
	public native HDEVINFO SetupDiGetClassDevs(GUID guid, String enumerator, HWND parent, int flags);
	//@Override
	public native boolean SetupDiDestroyDeviceInfoList(HDEVINFO DeviceInfoSet);
	//@Override
	public native boolean SetupDiEnumDeviceInfo(HDEVINFO deviceInfoSet, int memberIndex, SP_DEVINFO_DATA pDeviceInfoData);
	//</editor-fold>
}
