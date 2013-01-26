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

import java.io.IOException;

import purejavacomm.SerialPortEvent;
import purejavacomm.SerialPortEventListener;

public class Test11 extends TestBase {
	static volatile boolean m_ReadThreadRunning;

	static void run() throws Exception {

		try {
			begin("Test11 - exit from blocking read ");
			openPort();

			m_Out = m_Port.getOutputStream();
			m_In = m_Port.getInputStream();

			m_Port.disableReceiveTimeout();
			m_Port.disableReceiveThreshold();

			final byte[] rxbuffer = new byte[1];

			Thread rxthread = new Thread(new Runnable() {
				public void run() {
					m_ReadThreadRunning = true;
					try {
						int rxn = m_In.read(rxbuffer, 0, rxbuffer.length);
					} catch (IOException e) {
					}
					m_ReadThreadRunning = false;

				}
			});

			m_ReadThreadRunning = false;
			rxthread.start();
			while (!m_ReadThreadRunning)
				Thread.sleep(10);
			closePort();
			Thread.sleep(1000);
			if (m_ReadThreadRunning)
				fail("closing failed to interrupt a blocking read()");
			finishedOK();
		} finally {
			closePort();
		}

	}
}
