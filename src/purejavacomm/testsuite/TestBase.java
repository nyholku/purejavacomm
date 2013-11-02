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
import java.util.Enumeration;
import java.util.Random;

import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;
import purejavacomm.SerialPort;

public class TestBase {
	static class TestFailedException extends Exception {

	}
	
	public static final String APPLICATION_NAME = "PureJavaCommTestSuite";

	protected static volatile String m_TestPortName;
	protected static volatile SerialPort m_Port;
	private  static volatile long m_T0;
	protected static volatile OutputStream m_Out;
	protected static volatile InputStream m_In;
	protected static volatile int[] m_SyncSema4 = { 0 };
	protected static int m_Tab;
	protected static int m_Progress;

	protected static void sync(int N) throws InterruptedException {
		synchronized (m_SyncSema4) {
			m_SyncSema4[0]++;
			if (m_SyncSema4[0] < N) {
				m_SyncSema4.wait();
			} else {
				m_SyncSema4[0] = 0;
				m_SyncSema4.notifyAll();
			}
		}
	}

	static protected void openPort() throws Exception {
		try {
			CommPortIdentifier portid = CommPortIdentifier.getPortIdentifier(m_TestPortName);
			m_Port = (SerialPort) portid.open(APPLICATION_NAME, 1000);
			m_Out = m_Port.getOutputStream();
			m_In = m_Port.getInputStream();
			drain(m_In);
		} catch (NoSuchPortException e) {
			fail("could no open port '%s'\n", m_TestPortName);
		}
	}

	static protected void closePort() {
		if (m_Port != null) {
			try {
				m_Out.flush();
				m_Port.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally {
				m_Port = null;
			}
		}
	}

	static protected void drain(InputStream ins) throws Exception {
		sleep(100);
		int n;
		while ((n = ins.available()) > 0) {
			for (int i = 0; i < n; ++i)
				ins.read();
			sleep(100);
		}
	}

	static void begin(String name) {
		System.out.printf("%-46s", name);
		m_Tab = 46;
		m_T0 = System.currentTimeMillis();
		m_Progress = 0;
	}

	/**
	 * Sleep a short amount of time to allow hardware feedback, which isn't
	 * instant
	 */
	static protected void sleep() throws InterruptedException {
		sleep(40);
	}
	
	static protected void sleep(int t) throws InterruptedException {
		int m = 1000;
		while (t > 0) {
			Thread.sleep(t > m ? m : t);
			t -= m;
			while ((System.currentTimeMillis() - m_T0) / m > m_Progress) {
				System.out.print(".");
				m_Tab--;
				m_Progress++;
			}
		}
	}

	static void fail(String format, Object... args) throws TestFailedException {
		System.out.println(" FAILED");
		System.out.println("------------------------------------------------------------");
		System.out.printf(format, args);
		System.out.println();
		System.out.println("------------------------------------------------------------");
		throw new TestFailedException();

	}

	static void finishedOK() {
		finishedOK("");
	}

	static void finishedOK(String format, Object... args) {
		for (int i = 0; i < m_Tab; i++)
			System.out.print(".");
		System.out.printf(" OK " + format, args);
		System.out.println();
	}

	static public void init(String[] args) {
		m_TestPortName = "cu.usbserial-FTOXM3NX";
		if (args.length > 0)
			m_TestPortName = args[0];
	}

	static public String getPortName() {
		return m_TestPortName;

	}
}
