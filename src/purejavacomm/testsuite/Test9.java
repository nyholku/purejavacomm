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

public class Test9 extends TestBase {
	static volatile boolean m_ReadThreadRunning;
	static volatile int m_ReadBytes = 0;
	static volatile long m_T0;
	static volatile long m_T1;
	static byte[] m_TxBuffer = new byte[1000];
	static byte[] m_RxBuffer = new byte[m_TxBuffer.length];

	
	static void startReadThread()  throws Exception {
		Thread rxthread = new Thread(new Runnable() {
			public void run() {
				m_ReadThreadRunning = true;
				try {
					m_T0 = System.currentTimeMillis();
					m_ReadBytes = m_In.read(m_RxBuffer, 0, m_RxBuffer.length);
					m_T1 = System.currentTimeMillis();
				} catch (IOException e) {
					e.printStackTrace();
				}
				m_ReadThreadRunning = false;
			}
		});

		m_ReadThreadRunning = false;
		rxthread.setPriority(Thread.MAX_PRIORITY);
		rxthread.start();
		while (!m_ReadThreadRunning)
			Thread.sleep(10);

	}
	static void run() throws Exception {

		try {
			final int timeout = 100;
			final int threshold = 100;
			final int chunks = 16; // send the data in 16 chunks
			begin("Test9 - treshold 100, timeout 100 ");
			openPort();
			m_Out = m_Port.getOutputStream();
			m_In = m_Port.getInputStream();


			m_Port.enableReceiveTimeout(timeout);
			m_Port.enableReceiveThreshold(threshold);
			{ // Test a single big read without letting the timeout kick in
				
				startReadThread();
				
				sleep(500);
				if (m_ReadThreadRunning)
					fail("read did not timeout");
			
				startReadThread();

				int txn=10;
				int txt=50;
				for (int i = 0; i < 1000; i++) {
					m_Out.write(m_TxBuffer, 0, txn);
					sleep(50);
					if (!m_ReadThreadRunning)
						break;
				}
				if (m_ReadThreadRunning)
					fail("read did not complete in resonable time");
				if (m_ReadBytes<threshold)
					fail("expected at minimum "+threshold+" bytes but got "+m_ReadBytes);
				if (m_ReadThreadRunning)
					fail("read did not complete in time");

				int time = (int) (m_T1 - m_T0);
				int timeMax = (threshold/txn+2)*txt;
				
				if (time>timeMax)
					fail("was expecting read to happen in " + timeMax + " but it took " + time + " msec");
			}


			finishedOK();
		} finally {
			closePort();
		}

	}
}
