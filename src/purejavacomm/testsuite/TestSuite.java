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

import java.io.InputStream;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Random;

import com.sun.jna.Native;

import purejavacomm.CommPortIdentifier;
import purejavacomm.SerialPort;
import purejavacomm.SerialPortEvent;
import purejavacomm.SerialPortEventListener;

public class TestSuite {
	private volatile String m_TestPortName = "cu.usbserial-FTOXM3NX";
	private volatile SerialPort m_Port;
	private volatile Random rnd = new Random();
	private volatile byte[] m_ReceiveBuffer = new byte[10000];
	private volatile int m_BytesReceived = 0;
	private volatile int m_TotalReceived;
	private volatile long m_T0;
	private volatile long m_T1;
	private volatile int m_TxCount = 0;
	private volatile int m_RxCount = 0;
	private volatile int m_ErrorCount = 0;
	private volatile boolean m_Stop;

	private void openPort() throws Exception {
		m_TxCount = 0;
		m_BytesReceived = 0;
		m_RxCount = 0;
		m_ErrorCount = 0;
		CommPortIdentifier portid = null;
		Enumeration e = CommPortIdentifier.getPortIdentifiers();
		while (e.hasMoreElements()) {
			portid = (CommPortIdentifier) e.nextElement();
			if (portid.getName().equals(m_TestPortName))
				break;
		}
		if (portid != null) {
			System.out.printf("-openin port '%s'\n", portid.getName());
			m_Port = (SerialPort) portid.open("PureJavaCommTestSuite", 1000);
		} else
			System.out.printf("-could no open port '%s'\n", m_TestPortName);
	}

	private void closePort() {
		if (m_Port != null)
			m_Port.close();
	}

	private byte[] generateRandomMessage() {
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

	private void processBuffer(byte[] buffer, int n) {
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

	/**
	 * Test: <b>test_loopbackWithEventListener</b>
	 * <p>
	 * Tests:
	 * <p>
	 * <li>event listening mechanism</li>
	 * <li>reading inside event listener when data available</li>
	 * <li>writing inside event listener when transmit queue is empty</li>
	 * <li>clean exit when close is called outside event listener</li>
	 * <p>
	 * Test requires:
	 * <li>loopback cable</li>
	 * <p>
	 * Works by:
	 * <p>
	 * Sending LF terminated random ASCII (32..95) messages with check sum and
	 * receiving them via a loopback cable.
	 */
	void test_loopbackWithEventListener() {
		try {
			//jtermios.JTermios.JTermiosLogging.setLogLevel(4);

			openPort();
			System.out.print("test_loopbackWithEventListener starting\n");
			m_Port.notifyOnDataAvailable(true);
			m_Port.notifyOnOutputEmpty(true);
			m_Port.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN + SerialPort.FLOWCONTROL_XONXOFF_OUT);
			final OutputStream outs = m_Port.getOutputStream();
			final InputStream ins = m_Port.getInputStream();
			drain(ins);
			final boolean[] stop = { false };
			m_T0 = System.currentTimeMillis();
			m_Port.addEventListener(new SerialPortEventListener() {
				public void serialEvent(SerialPortEvent event) {
					try {
						if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
							byte[] buffer = new byte[ins.available()];
							int n = ins.read(buffer);
							m_TotalReceived += n;
							processBuffer(buffer, n);
							if (m_RxCount++ >= 1000) {
								m_Stop = true;
							}
						}
						if (event.getEventType() == SerialPortEvent.OUTPUT_BUFFER_EMPTY) {
							byte[] buffer = generateRandomMessage();
							outs.write(buffer, 0, buffer.length);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			//outs.write("huuhaa".getBytes(),0,6);
			while (!m_Stop) {
				try {
					Thread.sleep(1000);
					System.out.printf("-test progress so far: %d messages %d failed\n", m_RxCount, m_ErrorCount);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			m_T1 = System.currentTimeMillis();
			System.out.printf("test_loopbackWithEventListener completed: %s %s\n", (m_ErrorCount == 0) ? "OK" : "FAILED", m_ErrorCount > 0 ? "" + m_ErrorCount + " of " + m_RxCount + " messages failed" : "");

			int cs = m_Port.getDataBits() + 2;
			System.out.printf("avarage speed %1.0f b/sec at baud rate %d\n", m_TotalReceived * cs * 1000.0 / (m_T1 - m_T0), m_Port.getBaudRate());
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closePort();
		}

	}

	private void drain(InputStream ins) throws Exception {
		Thread.sleep(100);
		int n;
		while ((n = ins.available()) > 0) {
			System.out.printf("-draining %d bytes\n", n);
			for (int i = 0; i < n; ++i)
				ins.read();
			Thread.sleep(100);
		}
	}

	private void test_all_ascii() {
		try {
			System.out.print("test_all_ascii starting\n");
			openPort();
			//m_Port.notifyOnDataAvailable(true);
			//m_Port.notifyOnOutputEmpty(true);
			//m_Port.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN + SerialPort.FLOWCONTROL_XONXOFF_OUT);
			byte[] sent = new byte[256];
			for (int i = 0; i < 256; i++)
				sent[i] = (byte) i;
			//byte[] sent = "ABCDEFG".getBytes();
			byte[] rcvd = new byte[sent.length];
			m_Port.enableReceiveTimeout(1000);
			OutputStream outs = m_Port.getOutputStream();
			InputStream ins = m_Port.getInputStream();

			drain(ins);

			outs.write(sent);

			System.out.printf("- sent:");
			for (int i = 0; i < sent.length; i++)
				System.out.printf(" %02X", 0xFF & sent[i]);
			System.out.println();

			Thread.sleep(100);

			int n = ins.read(rcvd);

			System.out.printf("- rcvd:");
			for (int i = 0; i < n; i++)
				System.out.printf(" %02X", 0xFF & rcvd[i]);
			System.out.println();

			System.out.printf("test_all_ascii completed %s\n", Arrays.equals(sent, rcvd) ? "OK" : "FAIL");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closePort();
		}

	}

	private void run() {
		test_all_ascii();
		test_loopbackWithEventListener();
	}

	public static void main(String[] args) {
		Native.setProtected(true);
		TestSuite ts = new TestSuite();
		ts.run();

	}
}
