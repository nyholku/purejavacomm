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

import purejavacomm.*;

public class Test8 extends TestBase {
	
	// To allow us to run this test with limited hardware, we use two sets of
	// tests. The limited set includes only 8 bits, 1 stop bit and none/even/odd
	// parity.
	private static final int LIMITED_PARITY[] = { SerialPort.PARITY_NONE, SerialPort.PARITY_ODD, SerialPort.PARITY_EVEN };
	private static final int LIMITED_STOPBITS[] = { SerialPort.STOPBITS_1 };
	private static final int LIMITED_DATABITS[] = { SerialPort.DATABITS_8 };
	private static final int LIMITED_DATAMASK[] = { 0xFF };

	private static final int FULL_PARITY[] = { SerialPort.PARITY_NONE, SerialPort.PARITY_ODD, SerialPort.PARITY_EVEN, SerialPort.PARITY_MARK, SerialPort.PARITY_SPACE };
	private static final int FULL_STOPBITS[] = { SerialPort.STOPBITS_1, SerialPort.STOPBITS_1_5, SerialPort.STOPBITS_2 };
	private static final int FULL_DATABITS[] = { SerialPort.DATABITS_8, SerialPort.DATABITS_7, SerialPort.DATABITS_6, SerialPort.DATABITS_5 };
	private static final int FULL_DATAMASK[] = { 0xFF, 0x7F, 0x3F, 0x1F };

	
	static void run() throws Exception {
		run(true);
	}
	
	static void run(boolean allModes) throws Exception {
		try {
			begin("Test8 - parity etc");
			openPort();
			final int[] parity;
			final int[] stopbits;
			final int[] databits;
			final int[] datamask;
			if (allModes) {
				parity = FULL_PARITY;
				stopbits = FULL_STOPBITS;
				databits = FULL_DATABITS;
				datamask = FULL_DATAMASK;
			} else {
				parity = LIMITED_PARITY;
				stopbits = LIMITED_STOPBITS;
				databits = LIMITED_DATABITS;
				datamask = LIMITED_DATAMASK;				
			}
			System.out.println();
			int tn = 0;
			for (int ppi = 0; ppi < parity.length; ppi++) {
				for (int sbi = 0; sbi < stopbits.length; sbi++) {
					for (int dbi = 0; dbi < databits.length; dbi++) {

						m_Port.enableReceiveTimeout(10000);
						m_Port.enableReceiveThreshold(256);
						try {
							String db = "?";
							switch (databits[dbi]) {
								case SerialPort.DATABITS_5:
									db = "5";
									break;
								case SerialPort.DATABITS_6:
									db = "6";
									break;
								case SerialPort.DATABITS_7:
									db = "7";
									break;
								case SerialPort.DATABITS_8:
									db = "8";
									break;
							}

							String sb = "?";
							switch (stopbits[sbi]) {
								case SerialPort.STOPBITS_1:
									sb = "1";
									break;
								case SerialPort.STOPBITS_1_5:
									sb = "1.5";
									break;
								case SerialPort.STOPBITS_2:
									sb = "2";
									break;
							}

							String pb = "?";
							switch (parity[ppi]) {
								case SerialPort.PARITY_EVEN:
									pb = "E";
									break;
								case SerialPort.PARITY_ODD:
									pb = "O";
									break;
								case SerialPort.PARITY_MARK:
									pb = "M";
									break;
								case SerialPort.PARITY_SPACE:
									pb = "S";
									break;
								case SerialPort.PARITY_NONE:
									pb = "N";
									break;
							}
							tn++;
							begin("Test8." + tn + " databits=" + db + " stopbits=" + sb + " parity=" + pb);
							m_Port.setSerialPortParams(19200, databits[dbi], stopbits[sbi], parity[ppi]);
							sleep(100);
							byte[] sent = new byte[256];
							byte[] rcvd = new byte[256];
							// Send all 256 possible values, regardless of the bit count
							for (int i = 0; i < 256; i++)
								sent[i] = (byte) i;
							m_Out = m_Port.getOutputStream();
							m_In = m_Port.getInputStream();
							long t0 = System.currentTimeMillis();
							m_Out.write(sent);

							int n = 0;
							while ((n += m_In.read(rcvd, n, 256 - n)) < 256)
								;

							if (n != sent.length)
								fail("was expecting %d characters got %d", sent.length, n);
							for (int i = 0; i < 256; ++i) {
								if (i <= datamask[dbi]) {
									// These bytes must be transmitted unmodified
									if (rcvd[i] != sent[i])
										fail("failed: transmit '0x%02X' != receive'0x%02X'", sent[i], rcvd[i]);
								} else {
									// If we send more bits than can be
									// transmitted, we expect the excessive bits
									// to be discarded
									if (databits[dbi]>=7) { // no OS seems to really support 5/6 bits so we cannot test them
										int tx = sent[i] & datamask[dbi];
										if (rcvd[i] != tx) {
											fail("failed: transmit (excessive) '0x%02X' != receive'0x%02X'%n", tx, rcvd[i]);
										}
									}
								}
							}
							if (n < 256)
								fail("did not receive all 256 chars, got %d", n);
							finishedOK();
						} catch (UnsupportedCommOperationException e) {
							finishedOK(" NOT SUPPORTED " + e.getMessage());
						}
						// sleep(1);
					}
				}
			}
		} finally {
			closePort();
		}

	}
}
