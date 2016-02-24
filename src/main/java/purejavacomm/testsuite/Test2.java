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

import java.util.Arrays;
import java.util.Random;

import purejavacomm.SerialPort;
import purejavacomm.SerialPortEvent;
import purejavacomm.SerialPortEventListener;

public class Test2 extends TestBase {
	private static boolean m_Done;
	private static volatile Random rnd = new Random();
	private static volatile byte[] m_ReceiveBuffer = new byte[10000];
	private static volatile int m_BytesReceived = 0;
	private static volatile int m_TotalReceived;
	private static volatile long m_T0;
	private static volatile long m_T1;
	private static volatile int m_TxCount = 0;
	private static volatile int m_RxCount = 0;
	private static volatile int m_ErrorCount = 0;
	private static int N = 1000;

	static void run(int speed) throws Exception {
		try {
			m_Done = false;
			rnd = new Random();
			m_BytesReceived = 0;
			m_TotalReceived = 0;
			m_TxCount = 0;
			m_RxCount = 0;
			m_ErrorCount = 0;

			begin("Test2 - tx/rx with event listener");
			openPort();
			m_Port.notifyOnDataAvailable(true);
			m_Port.notifyOnOutputEmpty(true);
			m_Port.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN + SerialPort.FLOWCONTROL_XONXOFF_OUT);
			m_Port.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			final boolean[] stop = { false };
			m_T0 = System.currentTimeMillis();
			m_Port.addEventListener(new SerialPortEventListener() {
				public void serialEvent(SerialPortEvent event) {
					try {
						if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
							byte[] buffer = new byte[m_In.available()];
							int n = m_In.read(buffer);
							if (!m_Done) {
								m_TotalReceived += n;
								processBuffer(buffer, n);
								if (m_RxCount >= N) {
									m_Done = true;
								}
							}
						}
						if (event.getEventType() == SerialPortEvent.OUTPUT_BUFFER_EMPTY) {
							if (m_TxCount < N) {
								byte[] buffer = generateRandomMessage();
								m_Out.write(buffer, 0, buffer.length);
								m_TxCount++;
							}
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			while (!m_Done) {
				try {
					sleep(100);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			m_T1 = System.currentTimeMillis();
			if (m_ErrorCount > 0)
				fail("checksum sum failure in %d out %d messages", m_ErrorCount, N);

			int cs = m_Port.getDataBits() + 2;
			double actual = m_TotalReceived * cs * 1000.0 / (m_T1 - m_T0);
			int requested = m_Port.getBaudRate();
			finishedOK("average speed %1.0f b/sec at baud rate %d", actual, requested);
		} finally {
			closePort();
		}

	}

	static private byte[] generateRandomMessage() {
		int n = 4 + (rnd.nextInt() & 63);
		byte[] buffer = new byte[n + 2];
		//System.out.print("Sending: " + new String(buffer));
		int s = 0;
		int i;
		for (i = 0; i < n; i++) {
			byte b = (byte) (32 + (rnd.nextInt() & 63));
			buffer[i] = b;
			s += b;
		}
		buffer[i++] = (byte) (32 + (s & 63));
		buffer[i++] = '\n';
		return buffer;
	}

	static private void processBuffer(byte[] buffer, int n) {
		for (int i = 0; i < n; ++i) {
			byte b = buffer[i];
			if (n > buffer.length) {
				m_ErrorCount++;
				return;
			}

			m_ReceiveBuffer[m_BytesReceived++] = b;
			if (b == '\n') {
				//System.out.print("Received: " + new String(linebuf, 0, inp));
				int s = 0;
				int j;
				for (j = 0; j < m_BytesReceived - 2; j++)
					s += m_ReceiveBuffer[j];
				byte cb = (byte) (32 + (s & 63));
				if (cb != m_ReceiveBuffer[j] && m_RxCount > 0) {
					System.out.println("check sum failure");
					m_ErrorCount++;
				}
				m_RxCount++;
				m_BytesReceived = 0;
			}
		}
	}
}
