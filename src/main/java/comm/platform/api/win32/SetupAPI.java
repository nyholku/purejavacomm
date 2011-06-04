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

import java.util.Map;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import comm.PortType;
import comm.util.StringUtil;
import java.util.TreeMap;
import static comm.platform.api.win32.API.*;
import static comm.platform.api.win32.RegistryAPI.*;

/**
 * Calls representing the Win32 SetupAPI library. 
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface SetupAPI extends com.sun.jna.Library{
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String
		  LIBRARY_NAME = "setupapi"
	;
	
	public static final SetupAPI
		INSTANCE = SetupAPIDirect.loadLibrary()
	;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Util">
	public static class Util {
		/**
		 * Wraps calling SetupDiGetDeviceRegistryProperty() by making a couple of calls (it first finds the size and then makes the query using that size).
		 */
		public static String SetupDiGetDeviceRegistryPropertyString(SetupAPI API, HDEVINFO /*HDEVINFO*/ deviceInfoSet, SP_DEVINFO_DATA /*PSP_DEVINFO_DATA*/ pDeviceInfoData, int /*DWORD*/ property) {
			if (API == null)
				API = SetupAPI.INSTANCE;
			
			int devicePropSize = 0;
			IntByReference pDevicePropSize = new IntByReference();
			
			//Get size of array
			API.SetupDiGetDeviceRegistryProperty(deviceInfoSet, pDeviceInfoData, property, null, null, 0, pDevicePropSize);
			devicePropSize = pDevicePropSize.getValue();
			char[] devicePropString = new char[devicePropSize];//new Memory(devicePropSize);

			if (API.SetupDiGetDeviceRegistryProperty(deviceInfoSet, pDeviceInfoData, property, null, devicePropString, devicePropSize, null))
				return Native.toString(devicePropString);
			else
				return null;
		}
		
		/**
		 * 
		 * @return A hashmap containing the name and its details.
		 */
		public static Map<String, CommDetails> discoverCommDetails() {
			return discoverCommDetails(null, null, PortType.ANY);
		}
		
		public static Map<String, CommDetails> discoverCommDetails(PortType filter) {
			return discoverCommDetails(null, null, filter);
		}
		
		public static Map<String, CommDetails> discoverCommDetails(SetupAPI API, RegistryAPI RegAPI, PortType filter) {
			if (API == null)
				API = SetupAPI.INSTANCE;
			if (RegAPI == null)
				RegAPI = RegistryAPI.INSTANCE;
			
			int reqdSize = 0;
			IntByReference reqdSizeRef = new IntByReference();

			//Find the size of the array needed to hold the GUIDs
			API.SetupDiClassGuidsFromName(COM_PORT_CLASS_NAME, Pointer.NULL, 0, reqdSizeRef);
			reqdSize = reqdSizeRef.getValue();

			//Request the data
			if (reqdSize <= 0) 
				return null;

			GUID[] guids = new GUID[reqdSize];
			if (!API.SetupDiClassGuidsFromName(COM_PORT_CLASS_NAME, guids, guids.length, reqdSizeRef))
				return null;

			SP_DEVINFO_DATA deviceInfoData = new SP_DEVINFO_DATA();
			deviceInfoData.cbSize = deviceInfoData.size();
			int deviceIndex = 0;
			int devicePropSize = 0;
			HKEY deviceHKey = null;
			String devicePortName = null;
			IntByReference pDevicePropSize = new IntByReference();
			
			Map<String, CommDetails> map = new TreeMap<String, CommDetails>();
			
			for(GUID guid : guids) {
				HDEVINFO devinfo = null;

				try {
					devinfo = API.SetupDiGetClassDevs(guid, null, null, DIGCF_PRESENT);
					if (devinfo == INVALID_HANDLE_VALUE)
						continue;

					deviceIndex = 0;
					while(API.SetupDiEnumDeviceInfo(devinfo, deviceIndex, deviceInfoData)) {
						++deviceIndex;
						if (Native.getLastError() == ERROR_NOT_FOUND)
							break;
						
						try {
							if ((deviceHKey = API.SetupDiOpenDevRegKey(devinfo, deviceInfoData, DICS_FLAG_GLOBAL, 0, DIREG_DEV, KEY_QUERY_VALUE)) != null && deviceHKey != INVALID_HANDLE_VALUE) {
								if (!StringUtil.isNullOrEmpty(devicePortName = RegistryAPI.Util.RegQueryKeyStringValue(RegAPI, deviceHKey, REG_COMM_PORT_VALUE_NAME))) {
									if (comm.platform.api.win32.API.Util.isAnyPortNameMatch(devicePortName)) {
										//At this point we've located a port name that will (hopefully) have a friendly name associated with it.
										//Some may not (e.g. some virtual serial port devices).
										map.put(devicePortName, new CommDetails(
											devicePortName, 
											SetupAPI.Util.SetupDiGetDeviceRegistryPropertyString(API, devinfo, deviceInfoData, SPDRP_FRIENDLYNAME), 
											SetupAPI.Util.SetupDiGetDeviceRegistryPropertyString(API, devinfo, deviceInfoData, SPDRP_DEVICEDESC)
										));
									}
								}
							}

						} finally {
							//Clean up our native resources.
							if (deviceHKey != null)
								RegAPI.RegCloseKey(deviceHKey);
						}
					}
				} finally {
					if (devinfo != null)
						API.SetupDiDestroyDeviceInfoList(devinfo);
				}
			}
			
			return map;
		}
		
		public static class CommDetails {
			private String name;
			private String friendlyName;
			private String description;
			
			public CommDetails(String name, String friendlyName, String description) {
				this.name = name;
				this.friendlyName = friendlyName;
				this.description = description;
			}

			@Override
			public String toString() {
				return String.format("{Name: %s, Friendly Name: %s, Description: %s}", name, friendlyName, description);
			}
			
			public String getName() {
				return name;
			}
			
			public String getFriendlyName() {
				return friendlyName;
			}
			
			public String getDescription() {
				return description;
			}
		}
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="API">
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final WString 
		  COM_PORT_CLASS_NAME               = new WString("Ports")
	;
	
	public static final String 
		  REG_COMM_PORT_VALUE_NAME          = "PortName"
	;
	
	public static final int 
		  MAX_CLASS_NAME_LEN                = 128
		, MAX_PORT_NAME_LEN                 = 256
	;
	
	public static final int 
		  DIGCF_PRESENT                     = 0x00000002
		, DIGCF_INTERFACEDEVICE             = 0x00000010
	;

	public static final int 
		  DICS_FLAG_GLOBAL                  = 1
		, DICS_FLAG_CONFIGSPECIFIC          = 2
	;
	
	public static final int 
		  DIREG_DEV                         = 1
		, DIREG_DRV                         = 2
	;
	
	public static final int 
		  SPDRP_DEVICEDESC                  = 0
		, SPDRP_HARDWAREID                  = 1
		, SPDRP_COMPATIBLEIDS               = 2
		, SPDRP_SERVICE                     = 4
		, SPDRP_CLASS                       = 7
		, SPDRP_CLASSGUID                   = 8
		, SPDRP_DRIVER                      = 9
		, SPDRP_CONFIGFLAGS                 = 10
		, SPDRP_MFG                         = 11
		, SPDRP_FRIENDLYNAME                = 12
		, SPDRP_LOCATION_INFORMATION        = 13
		, SPDRP_PHYSICAL_DEVICE_OBJECT_NAME = 14
		, SPDRP_CAPABILITIES                = 15
		, SPDRP_UI_NUMBER                   = 16
		, SPDRP_UPPERFILTERS                = 17
		, SPDRP_LOWERFILTERS                = 18
		, SPDRP_BUSTYPEGUID                 = 19
		, SPDRP_LEGACYBUSTYPE               = 20
		, SPDRP_BUSNUMBER                   = 21
		, SPDRP_ENUMERATOR_NAME             = 22
		, SPDRP_SECURITY                    = 23
		, SPDRP_SECURITY_SDS                = 24
		, SPDRP_DEVTYPE                     = 25
		, SPDRP_EXCLUSIVE                   = 26
		, SPDRP_CHARACTERISTICS             = 27
		, SPDRP_ADDRESS                     = 28
		, SPDRP_UI_NUMBER_DESC_FORMAT       = 30
	;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Types">
	public static class HDEVINFO extends HANDLE {
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Structs">
	public static class SP_DEVINFO_DATA extends Structure {
		public int cbSize;
		public GUID ClassGuid;
		public int DevInst;
		public ULONG_PTR Reserved;
		
		public SP_DEVINFO_DATA() {
		}
		
		public SP_DEVINFO_DATA(Pointer memory) {
			useMemory(memory);
			read();
		}
		
		public static class ByValue extends SP_DEVINFO_DATA implements Structure.ByValue { }
		public static class ByReference extends SP_DEVINFO_DATA implements Structure.ByReference { public ByReference() { } public ByReference(Pointer memory) { super(memory); } }
	}
	//</editor-fold>
	
	/**
	 * Retrieves the GUID(s) associated with the specified class name. This list is built based on the classes currently installed on the system.
	 * 
	 * @param className The name of the class for which to retrieve the class GUID.
	 * @param classGuidList A pointer to an array to receive the list of GUIDs associated with the specified class name.
	 * @param classGuidListSize The number of GUIDs in the ClassGuidList array.
	 * @param requiredSize Supplies a pointer to a variable that receives the number of GUIDs associated with the class name. If this number is greater than the size of the ClassGuidList buffer, the number indicates how large the array must be in order to store all the GUIDs.
	 * @return The function returns TRUE if it is successful. Otherwise, it returns FALSE and the logged error can be retrieved by making a call to GetLastError.
	 */
	boolean SetupDiClassGuidsFromName(WString className, Pointer /*GUID[]*/ classGuidList, int classGuidListSize, IntByReference requiredSize);

	/**
	 * Retrieves the GUID(s) associated with the specified class name. This list is built based on the classes currently installed on the system.
	 * 
	 * @param className The name of the class for which to retrieve the class GUID.
	 * @param classGuidList A pointer to an array to receive the list of GUIDs associated with the specified class name.
	 * @param classGuidListSize The number of GUIDs in the ClassGuidList array.
	 * @param requiredSize Supplies a pointer to a variable that receives the number of GUIDs associated with the class name. If this number is greater than the size of the ClassGuidList buffer, the number indicates how large the array must be in order to store all the GUIDs.
	 * @return The function returns TRUE if it is successful. Otherwise, it returns FALSE and the logged error can be retrieved by making a call to GetLastError.
	 */
	boolean SetupDiClassGuidsFromName(WString className, GUID[] classGuidList, int classGuidListSize, IntByReference requiredSize);
	
	/**
	 * Returns a handle to a device information set that contains requested device information elements for a local computer. The caller of SetupDiGetClassDevs must delete the returned device information set when it is no longer needed by calling SetupDiDestroyDeviceInfoList.
	 * 
	 * @param guid A pointer to the GUID for a device setup class or a device interface class. This pointer is optional and can be NULL.
	 * @param enumerator A pointer to a NULL-terminated string that specifies an identifier (ID) of a Plug and Play (PnP) enumerator or a PnP device instance ID. This pointer is optional and can be NULL. If an enumeration value is not used to select devices, set Enumerator to NULL.
	 * @param parent A handle to the top-level window to be used for a user interface that is associated with installing a device instance in the device information set. This handle is optional and can be NULL.
	 * @param flags A variable of type DWORD that specifies control options that filter the device information elements that are added to the device information set.
	 * @return If the operation succeeds, SetupDiGetClassDevs returns a handle to a device information set that contains all installed devices that matched the supplied parameters. If the operation fails, the function returns INVALID_HANDLE_VALUE.
	 */
	HDEVINFO SetupDiGetClassDevs(GUID guid, String enumerator, HWND parent, int flags);
	
	/**
	 * Deletes a device information set and frees all associated memory.
	 * 
	 * @param DeviceInfoSet A handle to the device information set to delete.
	 * @return True if successful.
	 */
	boolean SetupDiDestroyDeviceInfoList(HDEVINFO DeviceInfoSet);
	
	/**
	 * Returns a SP_DEVINFO_DATA structure that specifies a device information element in a device information set.
	 * 
	 * @param deviceInfoSet A handle to the device information set for which to return an SP_DEVINFO_DATA structure that represents a device information element.
	 * @param memberIndex A zero-based index of the device information element to retrieve.
	 * @param pDeviceInfoData A pointer to an SP_DEVINFO_DATA structure to receive information about an enumerated device information element. The caller must set DeviceInfoData.cbSize to sizeof(SP_DEVINFO_DATA).
	 * @return The function returns TRUE if it is successful. Otherwise, it returns FALSE.
	 */
	boolean SetupDiEnumDeviceInfo(HDEVINFO deviceInfoSet, int memberIndex, SP_DEVINFO_DATA pDeviceInfoData);
	
	/**
	 * Opens a registry key for device-specific configuration information.
	 * 
	 * @param deviceInfoSet A handle to the device information set that contains a device information element that represents the device for which to open a registry key.
	 * @param pDeviceInfoData A pointer to an SP_DEVINFO_DATA structure that specifies the device information element in DeviceInfoSet.
	 * @param scope The scope of the registry key to open. The scope determines where the information is stored. The scope can be global or specific to a hardware profile.
	 * @param hwProfile A hardware profile value.
	 * @param keyType The type of registry storage key to open
	 * @param samDesired The registry security access that is required for the requested key.
	 * @return If the function is successful, it returns a handle to an opened registry key where private configuration data about this device instance can be stored/retrieved.
	 */
	HKEY SetupDiOpenDevRegKey(HDEVINFO /*HDEVINFO*/ deviceInfoSet, SP_DEVINFO_DATA /*PSP_DEVINFO_DATA*/ pDeviceInfoData, int /*DWORD*/ scope, int /*DWORD*/ hwProfile, int /*DWORD*/ keyType, int /*REGSAM*/ samDesired);

	/**
	 * Retrieves a specified Plug and Play device property.
	 * 
	 * @param deviceInfoSet  A handle to a device information set that contains a device information element that represents the device for which to retrieve a Plug and Play property.
	 * @param pDeviceInfoData A pointer to an SP_DEVINFO_DATA structure that specifies the device information element in DeviceInfoSet.
	 * @param property The property to be retrieved (SPDRP_ADDRESS, SPDRP_BUSNUMBER, etc.).
	 * @param pPropertyRegDataType A pointer to a variable that receives the data type of the property that is being retrieved. This is one of the standard registry data types. This parameter is optional and can be NULL.
	 * @param pPropertyBuffer A pointer to a buffer that receives the property that is being retrieved. If this parameter is set to NULL, and PropertyBufferSize is also set to zero, the function returns the required size for the buffer in RequiredSize.
	 * @param propertyBufferSize The size, in bytes, of the PropertyBuffer buffer.
	 * @param pRequiredSize A pointer to a variable of type DWORD that receives the required size, in bytes, of the PropertyBuffer buffer that is required to hold the data for the requested property. This parameter is optional and can be NULL.
	 * @return Returns TRUE if the call was successful.
	 */
	boolean SetupDiGetDeviceRegistryProperty(HDEVINFO /*HDEVINFO*/ deviceInfoSet, SP_DEVINFO_DATA /*PSP_DEVINFO_DATA*/ pDeviceInfoData, int /*DWORD*/ property, IntByReference /*PDWORD*/ pPropertyRegDataType, char[] /*PBYTE*/ pPropertyBuffer, int /*DWORD*/ propertyBufferSize, IntByReference /*PDWORD*/ pRequiredSize);
	//</editor-fold>
}
