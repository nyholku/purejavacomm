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

public class Test6 extends TestBase {
	private static Exception m_Exception = null;
	private static Thread m_Receiver;
	private static Thread m_Transmitter;

	static void run() throws Exception {
		try {
			begin("Test6 - threshold + timeout");
			openPort();
			m_Port.setSerialPortParams(230400, 8, 1, 0);
			//m_In = purejavacomm.RawStream.getInputStream(m_Port);
			// receiving thread
			m_Receiver = new Thread(new Runnable() {
				public void run() {
					try {
						sync(2);
						m_Port.enableReceiveThreshold(4);
						m_Port.enableReceiveTimeout(10000);
						//purejavacomm.RawStream.configureThresholdTimeout(m_Port, 4, 2000);
						byte[] b = new byte[4];
						for (int i = 0; i < 1000; i++) {
							long T0 = System.currentTimeMillis();
							int n = m_In.read(b);
							long dT = System.currentTimeMillis() - T0;
							if (n != 4)
								fail("read did not get 4 bytes as expected, got %d ", n);
							if (dT >= 1000)
								fail("read timed out though we got 4 bytes "+dT);
						}

					} catch (InterruptedException e) {
					} catch (Exception e) {
						if (m_Exception == null)
							m_Exception = e;
						m_Receiver.interrupt();
						m_Transmitter.interrupt();
					}
				};
			});

			// sending thread
			m_Transmitter = new Thread(new Runnable() {
				public void run() {
					try {
						sync(2);
						for (int i = 0; i < 1000; i++)
							m_Out.write(new byte[4]);
					} catch (InterruptedException e) {
					} catch (Exception e) {
						e.printStackTrace();
						if (m_Exception == null)
							m_Exception = e;
						m_Receiver.interrupt();
						m_Transmitter.interrupt();
					}
				};
			});

			m_Transmitter.start();
			sleep(100);
			m_Receiver.start();

			while (m_Receiver.isAlive() || m_Transmitter.isAlive()) {
				sleep(100);
			}

			if (m_Exception != null)
				throw m_Exception;
			finishedOK();
		} finally {
			closePort();
		}

	}
}
