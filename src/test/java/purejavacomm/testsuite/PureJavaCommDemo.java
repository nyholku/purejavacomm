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
import java.util.Enumeration;
import java.util.Random;

import purejavacomm.*;

public class PureJavaCommDemo {

	public static void main(String[] args) {
		try {
			System.out.println("PureJavaCommDemo");
			CommPortIdentifier portid = null;
			Enumeration e = CommPortIdentifier.getPortIdentifiers();
			while (e.hasMoreElements()) {
				portid = (CommPortIdentifier) e.nextElement();
				System.out.println("found " + portid.getName());
			}
			if (portid != null) {
				System.out.println("use " + portid.getName());
				SerialPort port = (SerialPort) portid.open("PureJavaCommDemo", 1000);
				port.notifyOnDataAvailable(true);
				port.notifyOnOutputEmpty(true);
				port.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN + SerialPort.FLOWCONTROL_XONXOFF_OUT);
				final OutputStream outs = port.getOutputStream();
				final InputStream ins = port.getInputStream();
				final boolean[] stop = { false };
				port.addEventListener(new SerialPortEventListener() {
					byte[] linebuf = new byte[10000];
					int inp = 0;
					int okcnt = 0;
					int errcnt = 0;
					Random rnd = new Random();

					public void serialEvent(SerialPortEvent event) {
						try {
							if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
								int n = ins.available();
								byte[] buffer = new byte[n];
								n = ins.read(buffer, 0, n);
								for (int i = 0; i < n; ++i) {
									byte b = buffer[i];
									linebuf[inp++] = b;
									if (b == '\n') {
										//System.err.print("Received: " + new String(linebuf, 0, inp));
										int s = 0;
										int j;
										for (j = 0; j < inp - 2; j++)
											s += linebuf[j];
										byte cb = (byte) (32 + (s & 63));
										okcnt++;
										if (cb != linebuf[j]) {
											System.out.println("check sum failure");
											errcnt++;
										}
										System.out.println("msg "+inp+" ok " + okcnt + " err " + errcnt);
										inp = 0;
									}
								}
							}
							if (event.getEventType() == SerialPortEvent.OUTPUT_BUFFER_EMPTY) {
								int n = 4+ (rnd.nextInt() & 63);
								byte[] buffer = new byte[n + 2];
								//System.err.print("Sending: " + new String(buffer));
								int s = 0;
								int i;
								for (i = 0; i < n; i++) {
									byte b = (byte) (32 + (rnd.nextInt() & 63));
									buffer[i] = b;
									s += b;
								}
								buffer[i++] = (byte) (32 + (s & 63));
								buffer[i++] = '\n';
								outs.write(buffer, 0, buffer.length);
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				while (!stop[0]) {
					try {
						Thread.sleep(100);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
