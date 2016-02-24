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

public class Test12 extends TestBase {
	static void run() throws Exception {

		try {
			begin("Test12 - enableReceiveTimeout(0)");
			openPort();

			m_Out = m_Port.getOutputStream();
			m_In = m_Port.getInputStream();

			final byte[] txbuffer = new byte[10];
			final byte[] rxbuffer = new byte[txbuffer.length];

			m_Port.enableReceiveTimeout(0);
			m_Port.enableReceiveThreshold(100);
			int totalN = 10;
			int bytesN = 8;
			{
				long totalT = 0;
				for (int i = 0; i < totalN; i++) {
					m_Out.write(txbuffer, 0, bytesN);
					sleep(100); // give the data some time to loop back
					{ // ask for 10 but expect to get back immediately with the 8 bytes that are available
						long T0 = System.currentTimeMillis();
						int n = m_In.read(rxbuffer, 0, 10);
						long T1 = System.currentTimeMillis();
						totalT += T1 - T0;
						if (n != 8)
							fail("did not get all data back, got only " + n + " bytes");
					}
				}
				if (totalT / totalN > 1)
					fail("read did not return immediately, it took " + totalT / totalN + " msec on average to read " + bytesN + " bytes");

			}
			{
				long totalT = 0;
				for (int i = 0; i < totalN; i++) {
					{ // ask for 10 but expect to get back immediately with the 0 bytes that are available
						long T0 = System.currentTimeMillis();
						int n = m_In.read(rxbuffer, 0, 10);
						long T1 = System.currentTimeMillis();
						totalT += T1 - T0;
						if (n != 0)
							fail("was expecting 0 bytes, but got " + n + " bytes");
					}
				}
				if (totalT / totalN > 1)
					fail("read did not return immediately, it took " + totalT / totalN + " msec");
			}

			finishedOK();
		} finally {
			closePort();
		}

	}
}
