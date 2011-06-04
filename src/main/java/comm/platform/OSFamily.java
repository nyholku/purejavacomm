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

import comm.util.StringUtil;

/**
 * Gathers information about operating systems and the one we're hosted on.
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum OSFamily {
	  Unknown (StringUtil.empty)

	, Windows ("windows")
	, Mac     ("osx")
	, Unix    ("unix")
	, Solaris ("solaris")
	, VMS     ("vms")
	
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	private String platformPartName;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	OSFamily(final String PlatformPartName) {
		this.platformPartName = PlatformPartName;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public static OSFamily getSystemOSFamily() {
		return OS.getSystemOSFamily();
	}

	public String getPlatformPartName() {
		return platformPartName;
	}

	public boolean isPOSIX() {
		return isPOSIX(this);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static boolean isPOSIX(final OS OS) {
		return isPOSIX(OS.getFamily());
	}

	public static boolean isPOSIX(final OSFamily OSFamily) {
		switch(OSFamily) {
			case Unix:
			case Mac:
			case Solaris:
				return true;
			default:
				return false;
		}
	}

	public static OSFamily fromName(final String Name) {
		if (StringUtil.isNullOrEmpty(Name))
			return OSFamily.Unknown;

		for(OSFamily family : OSFamily.values()) {
			if (family.platformPartName.equalsIgnoreCase(Name))
				return family;
		}

		final String lower = Name.toLowerCase();
		if (lower.contains("win"))
			return OSFamily.Windows;
		else if (lower.contains("mac"))
			return OSFamily.Mac;
		else if (lower.contains("nix") || lower.contains("nux"))
			return OSFamily.Unix;
		else if (lower.contains("vms"))
			return OSFamily.VMS;
		else if (lower.contains("solaris"))
			return OSFamily.Solaris;
		else
			return OSFamily.Unknown;
	}
	//</editor-fold>
}
