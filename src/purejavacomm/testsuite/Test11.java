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

import purejavacomm.PureJavaSerialPort;
import purejavacomm.SerialPortEvent;
import purejavacomm.SerialPortEventListener;

public class Test11 extends TestBase {
	static volatile boolean m_ThreadRunning;

	static volatile boolean m_ExitViaException;

	static void run() throws Exception {

		try {
			begin("Test11 - exit from blocking read/write ");
			// interrupting blocking read  test -------------------------
			openPort();
			m_Out = m_Port.getOutputStream();
			m_In = m_Port.getInputStream();

			m_Port.disableReceiveTimeout();
			m_Port.disableReceiveThreshold();

			Thread thread = new Thread(new Runnable() {
				public void run() {
					m_ThreadRunning = true;
					byte[] rxbuffer = new byte[1];
					try {
						int rxn = m_In.read(rxbuffer, 0, rxbuffer.length);
					} catch (Exception e) {
						m_ExitViaException = true;
					}
					m_ThreadRunning = false;

				}
			});

			m_ThreadRunning = false;
			m_ExitViaException = false;
			thread.start();
			while (!m_ThreadRunning)
				Thread.sleep(10);
			m_Port.close();// do not closePort() because flushing may block
			m_Port = null;
			Thread.sleep(1000);
			if (!m_ExitViaException)
				fail("closing failed to interrupt a blocking read()");

			// interrupting blocking write test -------------------------
			openPort();

			m_Out = m_Port.getOutputStream();
			m_In = m_Port.getInputStream();

			m_Port.disableReceiveTimeout();
			m_Port.disableReceiveThreshold();
			thread = new Thread(new Runnable() {
				public void run() {
					final byte[] txbuffer = new byte[4 * 1024];
					m_ThreadRunning = true;
					try {
						while (true)
							m_Out.write(txbuffer, 0, txbuffer.length);
					} catch (Exception e) {
						m_ExitViaException = true;
					}
					m_ThreadRunning = false;
				}
			});

			m_ThreadRunning = false;
			m_ExitViaException = false;
			thread.start();
			while (!m_ThreadRunning)
				Thread.sleep(10);
			m_Port.close(); // do not closePort() because flushing may block
			m_Port = null;
			Thread.sleep(10);
			if (!m_ExitViaException)
				fail("closing failed to interrupt a blocking write()");

			Thread.sleep(4000); // the port maybe busy if previously written data has not been transmitted so we need to wait before continuing testing
			// interrupting the internal thread -------------------------
			openPort();
			m_Port.addEventListener(new SerialPortEventListener() {
				public void serialEvent(SerialPortEvent event) {
				}
			});
			m_Port.notifyOnDataAvailable(true);

			m_Port.disableReceiveTimeout();
			m_Port.disableReceiveThreshold();
			m_Port.close();
			if (((PureJavaSerialPort) m_Port).isInternalThreadRunning())
				fail("internal thread failed to stop");
			m_Port = null;
			finishedOK();
		} finally {
			closePort();
		}

	}
}
