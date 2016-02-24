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

public class Test10 extends TestBase {
	static volatile boolean m_ReadThreadRunning;
	static volatile boolean m_ThreadStarted;
	static volatile int m_ReadBytes = 0;
	static volatile long m_T0;
	static volatile long m_T1;

	static void run() throws Exception {

		try {
			final int threshold = 128;
			final int chunks = 16; // send the data in 16 chunks
			begin("Test10 - treshold 100, timeout disabled ");
			openPort();
			m_Out = m_Port.getOutputStream();
			m_In = m_Port.getInputStream();

			final byte[] txbuffer = new byte[1000];
			final byte[] rxbuffer = new byte[txbuffer.length];

			m_Port.disableReceiveTimeout();
			m_Port.enableReceiveThreshold(threshold);
			{ // Test a single big read with threshold < read length
				Thread rxthread = new Thread(new Runnable() {
					public void run() {
						m_ReadThreadRunning = true;
						m_ThreadStarted = true;
						try {
							m_ReadBytes = m_In.read(rxbuffer, 0, rxbuffer.length);
							m_T1 = System.currentTimeMillis();
						} catch (IOException e) {
							e.printStackTrace();
						}
						m_ReadThreadRunning = false;
					}
				});

				m_ReadThreadRunning = false;
				m_ThreadStarted = false;
				m_ReadBytes = -666;
				rxthread.start();
				while (!m_ThreadStarted)
					Thread.sleep(10);

				for (int i = 0; i < chunks; i++) {
					int n = threshold / chunks;
					sleep(n * 10);
					if (!m_ReadThreadRunning)
						fail("read did not block but returned with " + m_ReadBytes + " bytes");
					m_Out.write(txbuffer, 0, n);
				}
				m_T0 = System.currentTimeMillis();
				Thread.sleep(100);
				int i = 2000;
				while (--i > 0 && m_ReadThreadRunning)
					Thread.sleep(5);
				if (i <= 0)
					fail("read did not return in time");

				if (m_ReadBytes != threshold)
					fail("was expecting read to return " + threshold + " but got " + m_ReadBytes);
				int time = (int) (m_T1 - m_T0);
				int timeMax = 30;
				if (time > timeMax)
					fail("was expecting read to happen in " + timeMax + " but it took " + time + " msec");
			}

		{ // Test a single big read with  read length < threshold 
				Thread rxthread = new Thread(new Runnable() {
					public void run() {
						m_ReadThreadRunning = true;
						m_ThreadStarted = true;
						try {
							m_ReadBytes = m_In.read(rxbuffer, 0, threshold / 2);
							m_T1 = System.currentTimeMillis();
					} catch (IOException e) {
							e.printStackTrace();
						}
						m_ReadThreadRunning = false;
					}
				});

				m_ReadThreadRunning = false;
				m_ThreadStarted = false;
				m_ReadBytes = -666;
			rxthread.start();
				while (!m_ThreadStarted)
					Thread.sleep(10);

				for (int i = 0; i < chunks / 2; i++) {
					int n = threshold / chunks;
					sleep(n * 10);
					if (!m_ReadThreadRunning)
						fail("read did not block but returned with " + m_ReadBytes + " bytes");
					m_Out.write(txbuffer, 0, n);
				}
				m_T0 = System.currentTimeMillis();
				Thread.sleep(100);
				int i = 200;
				while (--i > 0 && m_ReadThreadRunning)
					Thread.sleep(5);
				if (i <= 0)
					fail("read did not return in time");

				if (m_ReadBytes != threshold / 2)
					fail("was expecting read to return " + threshold + " but got " + m_ReadBytes);
				int time = (int) (m_T1 - m_T0);
				int timeMax = 30;
				if (time > timeMax)
					fail("was expecting read to happen in " + timeMax + " but it took " + time + " msec");
			}

			finishedOK();
		} finally {
			closePort();
		}

	}
}
