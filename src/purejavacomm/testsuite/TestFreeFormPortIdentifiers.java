package purejavacomm.testsuite;

import java.io.File;

import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;

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
