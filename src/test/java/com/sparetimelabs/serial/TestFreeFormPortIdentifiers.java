/*
 * Copyright (c) 2011, Kustaa Nyholm / SpareTimeLabs
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
 * Neither the name of the Kustaa Nyholm or SpareTimeLabs nor the names of its 
 * contributors may be used to endorse or promote products derived from this software 
 * without specific prior written permission.
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
package com.sparetimelabs.serial;

import java.io.File;

/**
 * This test case tests how CommPortIdentifier handles free form port names.
 * Serial ports are notorious for not being able to enumerate reliably, so an
 * application that allows a user to select a serial port will always have to
 * provide a free-form entry to provide the serial device name.
 *
 * The API that takes this device name and returns an identifier is
 * {@link CommPortIdentifier#getPortIdentifier(String)}.
 */
public class TestFreeFormPortIdentifiers extends TestBase {

    public static void testMissingPortInCommPortIdentifier() throws Exception {
        begin("TestMissingPort"); //  - getPortIdentifier on missing port

        // Must throw NoSuchPortException
        try {
            CommPortIdentifier.getPortIdentifier("blablub");

            fail("Got an identifier for non-exisiting path");
        } catch (NoSuchPortException nspe) {
            // All good
        }

        finishedOK();
    }

    public static void testDevicePathInCommPortIdentifier() throws Exception {
        begin("TestDevicePath "); // - getPortIdentifier on device path");

        // Must return an identifier
        try {
            CommPortIdentifier.getPortIdentifier(getPortName());
        } catch (NoSuchPortException nspe) {
            fail("Couldn't obtain identifier for device path");
        }

        finishedOK();
    }

    public static void testDevicePathToInvalidTTYInCommPortIdentifier() throws Exception {
        begin("TestDevicePathToInvalidTTY");// - getPortIdentifier on invalid device");

        // Must throw NoSuchPortException
        File tempFile = File.createTempFile("pjc", null);
        tempFile.deleteOnExit();

        try {
            CommPortIdentifier.getPortIdentifier(tempFile.getAbsolutePath());

            fail("Got an identifier for an invalid device");
        } catch (NoSuchPortException nspe) {
            // All good
        }

        finishedOK();
    }
}
