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

public class Test4 extends TestBase {
	private static Exception m_Exception = null;
	private static Thread receiver;
	private static Thread transmitter;

	static void run() throws Exception {
		try {
			begin("Test4 - blocking behaviour");
			openPort();
			// receiving thread
			receiver = new Thread(new Runnable() {
				public void run() {
					try {
						{ // step 1
							sync(2);
							// step 1: read a char, which should block for more than 10 seconds
							long T0 = System.currentTimeMillis();
							byte[] b = { 0 };
							int n = m_In.read(b);
							long dT = System.currentTimeMillis() - T0;
							if (n != 1)
								fail("read did not block, read returned %d", n);
							if (b[0] != 73)
								fail("read did not get looped back '73' got '%d'", b[0]);
							if (dT < 10000)
								fail("read did not block for 10000 msec, received loopback in %d msec", dT);
						}

						{ // step 2
							sync(2);
							m_Port.enableReceiveThreshold(0);
							m_Port.enableReceiveTimeout(1000);
							long T0 = System.currentTimeMillis();
							byte[] b = { 0 };
							int n = m_In.read(b);
							long dT = System.currentTimeMillis() - T0;
							if (n != 0)
								fail("read did not time out as expected, read returned %d", n);
							if (dT < 1000)
								fail("-timed out early, expected 1000 msec, got %d msec", dT);
							if (dT > 1010)
								fail("read timed out with suspicious delay, expected 1000 msec, got %d msec", dT);

						}

						{// step 3
							sync(2);
							m_Port.enableReceiveThreshold(4);
							m_Port.enableReceiveTimeout(1000);
							long T0 = System.currentTimeMillis();
							byte[] b = new byte[8];
							int n = m_In.read(b);
							long dT = System.currentTimeMillis() - T0;
							if (n != 4)
								fail("read did not get 4 bytes as expected, got %d ", n);
							if (dT >= 1000)
								fail("read timed out though we got 4 bytes");

						}
						{ // step 4
							sync(2);
							m_Port.enableReceiveThreshold(7);
							m_Port.disableReceiveTimeout();
							long T0 = System.currentTimeMillis();
							byte[] b = new byte[8];
							int n = m_In.read(b);
							long dT = System.currentTimeMillis() - T0;
							if (n != 7)
								fail("read did not get 7 bytes as expected, got %d", n);
							if (dT < 10000)
								fail("-timed out though we got 4 bytes");

						}

					} catch (InterruptedException e) {
					} catch (Exception e) {
						if (m_Exception == null)
							m_Exception = e;
						receiver.interrupt();
						transmitter.interrupt();
					}
				};
			});

			// sending thread
			transmitter = new Thread(new Runnable() {
				public void run() {
					try {
						{// step 1
							sync(2);
							sleep(10000);
							m_Out.write(73);
						}

						{// step 2
							sync(2);
						}

						{// step 3
							sync(2);
							m_Out.write(new byte[4]);
						}
						{// step 4
							sync(2);
							// step 4:
							sleep(10000);
							m_Out.write(new byte[7]);
						}

					} catch (InterruptedException e) {
					} catch (Exception e) {
						e.printStackTrace();
						if (m_Exception == null)
							m_Exception = e;
						receiver.interrupt();
						transmitter.interrupt();
					}
				};
			});

			receiver.start();
			transmitter.start();

			while (receiver.isAlive() || transmitter.isAlive()) {
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
