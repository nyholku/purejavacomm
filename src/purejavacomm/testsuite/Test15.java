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

public class Test15 extends TestBase {
	static void run() throws Exception {

		try {
			int timeout = 100;
			begin("Test15 - treshold disabled, timeout == " + timeout);
			openPort();
			m_Out = m_Port.getOutputStream();
			m_In = m_Port.getInputStream();

			final byte[] txbuffer = new byte[1000];
			final byte[] rxbuffer = new byte[txbuffer.length];

			m_Port.enableReceiveTimeout(100);
			m_Port.disableReceiveThreshold();

			{
				long T0 = System.currentTimeMillis();
				int n = m_In.read(rxbuffer, 0, 10);
				long T1 = System.currentTimeMillis();
				if (n != 0)
					fail("was expecting 0 bytes, but got " + n + " bytes");
				int timeLo = timeout;
				int timeHi = timeout * 110 / 100;
				int time = (int) (T1 - T0);
				if (time < timeLo)
					fail("timed out early, was expecting  " + timeLo + " but got " + time + " msec");
				if (time > timeHi)
					fail("timed out late, was expecting  " + timeHi + " but got " + time + " msec");
			}

			{
				m_Out.write(txbuffer, 0, 1000); // at 9600 this should take about 1 sec 
				sleep(50); // give time to about 50 chars to loop back
				long T0 = System.currentTimeMillis();
				int n = m_In.read(rxbuffer, 0, 1000);
				long T1 = System.currentTimeMillis();
				int time = (int) (T1 - T0);
				int etime= n * 150/100; // at 9600
				if (time > etime)
					fail("expected read to return in " + etime+" but it took " + time + " msec and returned "+n +" bytes");
				if (n < 10)
					fail("was expecting at least 900 bytes, but got " + n + " bytes");
			}

			
			finishedOK();
		} finally {
			closePort();
		}

	}
}
