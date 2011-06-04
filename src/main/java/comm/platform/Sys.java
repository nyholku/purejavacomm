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
 * Utilities for accessing various system attributes and configuration.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public final class Sys {
	//<editor-fold defaultstate="collapsed" desc="Getters">
	public static OS getOS() {
		return OS.getSystemOS();
	}

	public static OSFamily getOSFamily() {
		return OS.getSystemOSFamily();
	}

	public static Arch getArch() {
		return Arch.getSystemArch();
	}

	public static int getPID() {
		return Process.getPID();
	}

	public static int getLastError() {
		return Process.getLastError();
	}

	public static String getPath() {
		return Env.getPath();
	}

	public static String getEnvironmentVariable(final String name) {
		return Env.getEnvironmentVariable(name);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static boolean isOS(final OS OS) {
		return (OS.getSystemOS() == OS);
	}

	public static boolean isOSFamily(final OSFamily OSFamily) {
		return (OS.getSystemOSFamily() == OSFamily);
	}

	public static boolean isArch(final Arch Arch) {
		return (Arch.getSystemArch() == Arch);
	}

	public static boolean isResourceAvailable(final String ResourceName) {
		final String res = (ResourceName.startsWith("/") && ResourceName.length() > 1 ? ResourceName.substring(1) : ResourceName).trim();
		if (StringUtil.isNullOrEmpty(res))
			return false;
		return (Thread.currentThread().getContextClassLoader().getResource(res) != null);
	}

	public static String createPlatformName() {
		return createPlatformName(getOSFamily());
	}

	public static String createPlatformName(OS OS) {
		return OS.getPlatformPartName() + "." + getArch().getPlatformPartName();
	}

	public static String createPlatformName(OSFamily OSFamily) {
		return OSFamily.getPlatformPartName() + "." + getArch().getPlatformPartName();
	}

	public static String createPackageResourcePrefix(final String PackagePrefix, final String PackageSuffix) {
		final String prefix = PackagePrefix.trim();
		final String suffix = PackageSuffix.trim();
		return
			'/' +
			(!StringUtil.isNullOrEmpty(prefix) ? prefix.replace('.', '/') + '/' : StringUtil.empty) +
			(!StringUtil.isNullOrEmpty(suffix) ? suffix.replace('.', '/') + '/' : StringUtil.empty)
		;
	}

	public static String createPackageResourcePrefix(final String PackagePrefix) {
		return createPackageResourcePrefix(PackagePrefix, StringUtil.empty);
	}

	public static String createPlatformPackageName(final String PackagePrefix, final String PlatformName, final String PackageSuffix) {
		final String prefix = PackagePrefix.trim();
		final String suffix = PackageSuffix.trim();

		String ret = PlatformName;
		
		if (!StringUtil.isNullOrEmpty(prefix)) {
			if (prefix.endsWith("."))
				ret = prefix + ret;
			else
				ret = prefix + "." + ret;
		}

		if (!StringUtil.isNullOrEmpty(suffix)) {
			if (suffix.startsWith("."))
				ret = ret + suffix;
			else
				ret = ret + "." + suffix;
		}

		return ret;
	}

	public static String createPlatformPackageName(final String PackagePrefix, final String PackageSuffix) {
		return createPlatformPackageName(PackagePrefix, createPlatformName(), PackageSuffix);
	}

	public static String createPlatformPackageName(final String PackagePrefix) {
		return createPlatformPackageName(PackagePrefix, createPlatformName(), StringUtil.empty);
	}

	public static String createPlatformPackageName(final String PackagePrefix, final OS OS, final String PackageSuffix) {
		return createPlatformPackageName(PackagePrefix, createPlatformName(OS), PackageSuffix);
	}

	public static String createPlatformPackageName(final String PackagePrefix, final OS OS) {
		return createPlatformPackageName(PackagePrefix, createPlatformName(OS), StringUtil.empty);
	}

	public static String createPlatformPackageName(final String PackagePrefix, final OSFamily OSFamily, final String PackageSuffix) {
		return createPlatformPackageName(PackagePrefix, createPlatformName(OSFamily), PackageSuffix);
	}

	public static String createPlatformPackageName(final String PackagePrefix, final OSFamily OSFamily) {
		return createPlatformPackageName(PackagePrefix, createPlatformName(OSFamily), StringUtil.empty);
	}

	public static String createPlatformPackageResourcePrefix(final String PackagePrefix, final String PackageSuffix) {
		return createPackageResourcePrefix(createPlatformPackageName(PackagePrefix, PackageSuffix));
	}

	public static String createPlatformPackageResourcePrefix(final String PackagePrefix) {
		return createPackageResourcePrefix(createPlatformPackageName(PackagePrefix));
	}

	public static String createPlatformPackageResourcePrefix(final String PackagePrefix, final OS OS, final String PackageSuffix) {
		return createPackageResourcePrefix(createPlatformPackageName(PackagePrefix, OS, PackageSuffix));
	}

	public static String createPlatformPackageResourcePrefix(final String PackagePrefix, final OS OS) {
		return createPackageResourcePrefix(createPlatformPackageName(PackagePrefix, OS));
	}

	public static String createPlatformPackageResourcePrefix(final String PackagePrefix, final OSFamily OSFamily, final String PackageSuffix) {
		return createPackageResourcePrefix(createPlatformPackageName(PackagePrefix, OSFamily, PackageSuffix));
	}

	public static String createPlatformPackageResourcePrefix(final String PackagePrefix, final OSFamily OSFamily) {
		return createPackageResourcePrefix(createPlatformPackageName(PackagePrefix, OSFamily));
	}

	public static String createPlatformPackageResourceName(final String PackagePrefix, final String PackageSuffix, final String ResourceName) {
		return createPlatformPackageResourcePrefix(PackagePrefix, PackageSuffix) + ResourceName;
	}

	public static String createPlatformPackageResourceName(final String PackagePrefix, final String ResourceName) {
		return createPlatformPackageResourcePrefix(PackagePrefix) + ResourceName;
	}

	public static String createPlatformPackageResourceName(final String PackagePrefix, final OS OS, final String PackageSuffix, final String ResourceName) {
		return createPlatformPackageResourcePrefix(PackagePrefix, OS, PackageSuffix) + ResourceName;
	}

	public static String createPlatformPackageResourceName(final String PackagePrefix, final OS OS, final String ResourceName) {
		return createPlatformPackageResourcePrefix(PackagePrefix, OS) + ResourceName;
	}

	public static String createPlatformPackageResourceName(final String PackagePrefix, final OSFamily OSFamily, final String PackageSuffix, final String ResourceName) {
		return createPlatformPackageResourcePrefix(PackagePrefix, OSFamily, PackageSuffix) + ResourceName;
	}

	public static String createPlatformPackageResourceName(final String PackagePrefix, final OSFamily OSFamily, final String ResourceName) {
		return createPlatformPackageResourcePrefix(PackagePrefix, OSFamily) + ResourceName;
	}
	//</editor-fold>
}
