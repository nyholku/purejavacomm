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
package purejavacomm.testsuite;

import purejavacomm.SerialPortEvent;
import purejavacomm.SerialPortEventListener;

public class Test1 extends TestBase {
	static void run() throws Exception {

		try {
			begin("Test1 - control lines ");
			openPort();
			m_Port.setRTS(false);
			m_Port.setDTR(false);
			sleep();
			
			m_Port.notifyOnCTS(true);
			m_Port.notifyOnRingIndicator(true);
			m_Port.notifyOnCarrierDetect(true);
			m_Port.notifyOnDSR(true);
			final int[] counts = new int[11];
			m_Port.addEventListener(new SerialPortEventListener() {
				public void serialEvent(SerialPortEvent event) {
					try {
						if (event.getEventType() == SerialPortEvent.CTS)
							counts[SerialPortEvent.CTS]++;
						if (event.getEventType() == SerialPortEvent.RI)
							counts[SerialPortEvent.RI]++;
						if (event.getEventType() == SerialPortEvent.CD)
							counts[SerialPortEvent.CD]++;
						if (event.getEventType() == SerialPortEvent.DSR)
							counts[SerialPortEvent.DSR]++;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			int N = 128;
			for (int i = 0; i < N; i++) {
				m_Port.setRTS((i & 1) != 0);
				m_Port.setDTR((i & 2) != 0);
				sleep();
			}
			if (counts[SerialPortEvent.CTS] != N - 1)
				fail("CTS loopback failed, expected %d toggles, got %d", N - 1, counts[SerialPortEvent.CTS]);
			if (counts[SerialPortEvent.DSR] != N / 2 - 1)
				fail("DSR loopback failed, expected %d toggles, got %d", N / 2 - 1, counts[SerialPortEvent.DSR]);
			if (counts[SerialPortEvent.RI] != N - 1)
				fail("RI loopback failed, expected %d toggles, got %d", N-1, counts[SerialPortEvent.RI]);
			if (counts[SerialPortEvent.CD] != N / 2 - 1)
				fail("CTS loopback failed, expected %d toggles, got %d", N / 2 - 1, counts[SerialPortEvent.CD]);
			finishedOK();
		} finally {
			closePort();
		}

	}
}
