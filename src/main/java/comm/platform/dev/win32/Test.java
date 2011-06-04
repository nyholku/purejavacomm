/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package comm.platform.dev.win32;

import comm.ISerialPort;

/**
 *
 * @author hoyt6
 */
class Test {
	public static void main(String[] args) throws InterruptedException {
		ISerialPort serialPort = comm.SerialPort.find("COM2");
		if (serialPort == null)
			serialPort = comm.SerialPort.getAvailableSerialPorts()[0];
		
		System.out.println("Opening " + serialPort.getName() + " [" + serialPort.getTitle() + "]");
		
		for(int i = 0; i < 1; ++i) {
			System.out.println("Opening and closing (attempt " + (i + 1) + ")...");
			serialPort.open();
			//Thread.sleep(100 * 1);
			Thread.sleep(10000 * 3);
			serialPort.close();
		}
	}
}
