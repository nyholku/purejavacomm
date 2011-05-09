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
	private volatile OutputStream m_Out;
	private volatile InputStream m_In;
	private volatile boolean m_Done;
	private volatile boolean m_OK;
	private volatile String m_Message;

	private void openPort() throws Exception {
		m_TxCount = 0;
		m_BytesReceived = 0;
		m_RxCount = 0;
		m_ErrorCount = 0;
		m_Done = false;
		m_OK = false;
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
			m_Out = m_Port.getOutputStream();
			m_In = m_Port.getInputStream();
			drain(m_In);
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
	void test_loopbackWithEventListener() throws Exception {
		try {
			//jtermios.JTermios.JTermiosLogging.setLogLevel(4);

			openPort();
			System.out.print("test_loopbackWithEventListener starting\n");
			m_Port.notifyOnDataAvailable(true);
			m_Port.notifyOnOutputEmpty(true);
			m_Port.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN + SerialPort.FLOWCONTROL_XONXOFF_OUT);
			drain(m_In);
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
								if (m_RxCount++ >= 1000) {
									m_Done = true;
								}
							}
						}
						if (event.getEventType() == SerialPortEvent.OUTPUT_BUFFER_EMPTY) {
							if (!m_Done) {
								byte[] buffer = generateRandomMessage();
								m_Out.write(buffer, 0, buffer.length);
							}
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			//outs.write("huuhaa".getBytes(),0,6);
			while (!m_Done) {
				try {
					Thread.sleep(1000);
					System.out.printf("-test progress so far: %d messages %d failed\n", m_RxCount, m_ErrorCount);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			m_T1 = System.currentTimeMillis();
			m_OK = m_ErrorCount == 0;
			System.out.printf("test_loopbackWithEventListener completed: %s %s\n", m_OK ? "OK" : "FAILED", m_ErrorCount > 0 ? "" + m_ErrorCount + " of " + m_RxCount + " messages failed" : "");

			int cs = m_Port.getDataBits() + 2;
			System.out.printf("avarage speed %1.0f b/sec at baud rate %d\n", m_TotalReceived * cs * 1000.0 / (m_T1 - m_T0), m_Port.getBaudRate());
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			drain(m_In);
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

	private void test_all_ascii() throws Exception {
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
			m_Out = m_Port.getOutputStream();
			m_In = m_Port.getInputStream();

			drain(m_In);

			m_Out.write(sent);

			System.out.printf("- sent:");
			for (int i = 0; i < sent.length; i++)
				System.out.printf(" %02X", 0xFF & sent[i]);
			System.out.println();

			Thread.sleep(100);

			int n = m_In.read(rcvd);

			System.out.printf("- rcvd:");
			for (int i = 0; i < n; i++)
				System.out.printf(" %02X", 0xFF & rcvd[i]);
			System.out.println();

			m_OK = Arrays.equals(sent, rcvd);
			System.out.printf("test_all_ascii completed %s\n", m_OK ? "OK" : "FAIL");
		} finally {
			closePort();
		}

	}

	private void sleep(int t) throws InterruptedException {
		Thread.sleep(t);
	}

	private void waitStep(int[] txstep, int[] rxstep) throws InterruptedException {
		while (txstep[0] == rxstep[0]) {
			sleep(100);
		}
		txstep[0] = rxstep[0];
	}

	private volatile boolean m_SyncSema4 = false;

	private synchronized void sync() throws InterruptedException {
		if (m_SyncSema4) {
			m_SyncSema4 = false;
			notify();
		} else {
			m_SyncSema4 = true;
			wait();
		}
	}

	private void test_blocking_heaviour() throws Exception {
		try {
			System.out.print("test_blocking_heaviour starting\n");
			openPort();

			final int[] rxstep = { 1 };
			final int[] txstep = { 1 };

			// receiving thread
			Thread receiver = new Thread(new Runnable() {
				public void run() {
					try {
						{
							sync();
							// step 1: read a char, which should block for more than 10 seconds
							long T0 = System.currentTimeMillis();
							System.out.println("-rcve: waiting for a byte");
							byte[] b = { 0 };
							int n = m_In.read(b);
							long dT = System.currentTimeMillis() - T0;
							if (n != 1) {
								System.out.println("-did not block, read returned " + n);
								return;
							}
							if (b[0] != 73) {
								System.out.println("-did not get looped back '73' got '" + b[0] + "'");
								return;
							}
							if (dT < 10000) {
								System.out.println("-did not block for 10000 msec, received loopback in " + dT + " msec");
								return;
							}
							System.out.println("-check 1 ok, blocks by default ");
						}

						{
							sync();
							// step 2: check that the timeout works
							m_Port.enableReceiveThreshold(0);
							m_Port.enableReceiveTimeout(1000);
							long T0 = System.currentTimeMillis();
							byte[] b = { 0 };
							System.out.println("-rcve: waiting for timeout");
							int n = m_In.read(b);
							long dT = System.currentTimeMillis() - T0;
							if (n != 0) {
								System.out.println("-did not time out as expected, read returned " + n);
								return;
							}
							if (dT < 1000) {
								System.out.println("-timed out early, expected 1000 msec, got  " + dT + " msec");
								return;
							}
							if (dT > 1010) {
								System.out.println("-timed out with suspicious delay, expected 1000 msec, got  " + dT + " msec");
							}
							System.out.println("-check 2 ok, times out as expected ");

						}

						{
							sync();
							// step 3: check that the timeout + threshold works
							m_Port.enableReceiveThreshold(4);
							m_Port.enableReceiveTimeout(1000);
							long T0 = System.currentTimeMillis();
							byte[] b = new byte[8];
							System.out.println("-rcve: waiting for 4 bytes");
							int n = m_In.read(b);
							long dT = System.currentTimeMillis() - T0;
							if (n != 4) {
								System.out.println("-did not get 4 bytes as expected, read returned " + n);
								return;
							}
							if (dT >= 1000) {
								System.out.println("-timed out though we got 4 bytes");
							}
							System.out.println("-check 3 ok, got 4 bytes within timeout");

						}
						{
							sync();
							// step 4: check that the threshold works
							m_Port.enableReceiveThreshold(7);
							m_Port.disableReceiveTimeout();
							long T0 = System.currentTimeMillis();
							byte[] b = new byte[8];
							System.out.println("-rcve: waiting for 7 bytes");
							int n = m_In.read(b);
							long dT = System.currentTimeMillis() - T0;
							if (n != 7) {
								System.out.println("-did not get 7 bytes as expected, read returned " + n);
								return;
							}
							if (dT < 10000) {
								System.out.println("-timed out though we got 4 bytes");
							}
							System.out.println("-check 3 ok, got 4 bytes within timeout");

						}
						m_OK = true;

					} catch (InterruptedException e) {

					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						m_Done = true;
					}
				};
			});

			// sending thread
			Thread transmitter = new Thread(new Runnable() {
				public void run() {
					try {
						{
							sync();
							// step 1: wait 10 seconds and the send one char
							System.out.println("-send: wait 10000 msec");
							sleep(10000);
							System.out.println("-send: send '73'");
							m_Out.write(73);
						}

						{// step 2: 
							sync();
							System.out.println("-send: not sending anything");
						}

						{// step 3:
							sync();
							System.out.println("-send: send 4 bytes");
							m_Out.write(new byte[4]);
						}
						{
							sync();
							// step 4:
							System.out.println("-send: wait 10000 msec");
							sleep(10000);
							System.out.println("-send: send 7 bytes");
							m_Out.write(new byte[7]);
						}

					} catch (InterruptedException e) {
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						m_Done = true;
					}
				};
			});

			receiver.start();
			transmitter.start();

			while (!m_Done) {
				sleep(100);
			}
			receiver.interrupt();
			transmitter.interrupt();

			System.out.printf("test_blocking_heaviour completed %s\n", m_OK ? "OK" : "FAIL");
		} finally {
			closePort();
		}

	}

	private void test_control_lines() throws Exception {

		try {
			openPort();
			System.out.print("test_control_lines starting\n");
			m_Port.setRTS(false);
			m_Port.setDTR(false);

			m_Port.notifyOnCTS(true);
			m_Port.notifyOnRingIndicator(true);
			m_Port.notifyOnCarrierDetect(true);
			m_Port.notifyOnDSR(true);
			final int[] counts = new int[11];
			m_Port.addEventListener(new SerialPortEventListener() {
				public void serialEvent(SerialPortEvent event) {
					try {
						if (event.getEventType() == SerialPortEvent.CTS)
							counts[SerialPortEvent.CTS]++;
						if (event.getEventType() == SerialPortEvent.RI)
							counts[SerialPortEvent.RI]++;
						if (event.getEventType() == SerialPortEvent.CD)
							counts[SerialPortEvent.CD]++;
						if (event.getEventType() == SerialPortEvent.DSR)
							counts[SerialPortEvent.DSR]++;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			try {
				for (int i = 0; i < 128; i++) {
					m_Port.setRTS((i & 1) != 0);
					m_Port.setDTR((i & 2) != 0);
					Thread.sleep(10);
				}
				m_OK = true;
				if (m_OK &= (counts[SerialPortEvent.CTS] != 127))
					System.out.printf("- CTS loopback fail, expected 127 got " + counts[SerialPortEvent.CTS]);
				if (m_OK &= (counts[SerialPortEvent.CTS] != 63))
					System.out.printf("- DSR loopback fail, expected 63 got " + counts[SerialPortEvent.DSR]);
				if (m_OK &= (counts[SerialPortEvent.CTS] != 127))
					System.out.printf("- RI loopback fail, expected 127 got " + counts[SerialPortEvent.RI]);
				if (m_OK &= (counts[SerialPortEvent.CD] != 63))
					System.out.printf("- CTS loopback fail, expected 63 got " + counts[SerialPortEvent.CD]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			m_T1 = System.currentTimeMillis();
			m_OK = m_ErrorCount == 0;
			System.out.printf("test_control_lines completed: %s\n", m_OK ? "OK" : "FAILED");

		} finally {
			closePort();
		}

	}

	private void run() {
		try {
			test_control_lines();
			//test_blocking_heaviour();
			//test_all_ascii();
			//test_loopbackWithEventListener();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Native.setProtected(true);
		TestSuite ts = new TestSuite();
		ts.run();

	}
}
