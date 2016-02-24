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

package jtermios.windows.testsuite;

import static jtermios.windows.WinAPI.CBR_1200;
import static jtermios.windows.WinAPI.CloseHandle;
import static jtermios.windows.WinAPI.CreateEvent;
import static jtermios.windows.WinAPI.CreateFile;
import static jtermios.windows.WinAPI.ERROR_IO_INCOMPLETE;
import static jtermios.windows.WinAPI.ERROR_IO_PENDING;
import static jtermios.windows.WinAPI.FILE_FLAG_OVERLAPPED;
import static jtermios.windows.WinAPI.GENERIC_READ;
import static jtermios.windows.WinAPI.GENERIC_WRITE;
import static jtermios.windows.WinAPI.GetLastError;
import static jtermios.windows.WinAPI.GetOverlappedResult;
import static jtermios.windows.WinAPI.INVALID_HANDLE_VALUE;
import static jtermios.windows.WinAPI.NOPARITY;
import static jtermios.windows.WinAPI.ONESTOPBIT;
import static jtermios.windows.WinAPI.OPEN_EXISTING;
import static jtermios.windows.WinAPI.ReadFile;
import static jtermios.windows.WinAPI.ResetEvent;
import static jtermios.windows.WinAPI.SetCommState;
import static jtermios.windows.WinAPI.SetCommTimeouts;
import static jtermios.windows.WinAPI.SetupComm;
import static jtermios.windows.WinAPI.WAIT_OBJECT_0;
import static jtermios.windows.WinAPI.WAIT_TIMEOUT;
import static jtermios.windows.WinAPI.WaitForSingleObject;
import static jtermios.windows.WinAPI.WriteFile;
import jtermios.windows.WinAPI.COMMTIMEOUTS;
import jtermios.windows.WinAPI.DCB;
import jtermios.windows.WinAPI.HANDLE;
import jtermios.windows.WinAPI.OVERLAPPED;

import com.sun.jna.Memory;

public class TestSuite {

	private void check(boolean ok, String what) {
		if (!ok) {
			System.err.println(what + " failed, error " + GetLastError());
			System.exit(0);
		}
	}

	public void test1() {
		System.out.println("A contorted loopback test with overlapped IO for WinAPI");

		String COM = "COM5:";
		HANDLE hComm = CreateFile(COM, GENERIC_READ | GENERIC_WRITE, 0, null, OPEN_EXISTING, FILE_FLAG_OVERLAPPED, null);

		check(SetupComm(hComm, 2048, 2048), "SetupComm ");

		DCB dcb = new DCB();
		dcb.DCBlength = dcb.size();
		dcb.BaudRate = CBR_1200;
		dcb.ByteSize = 8;
		dcb.fFlags = 0;
		dcb.Parity = NOPARITY;
		dcb.XonChar = 0x11;
		dcb.StopBits = ONESTOPBIT;
		dcb.XonChar = 0x13;

		check(SetCommState(hComm, dcb), "SetCommState ");

		COMMTIMEOUTS touts = new COMMTIMEOUTS();
		check(SetCommTimeouts(hComm, touts), "SetCommTimeouts ");

		check(!INVALID_HANDLE_VALUE.equals(hComm), "CreateFile " + COM);
		String send = "Hello World";
		int tlen = send.getBytes().length;

		int[] txn = { 0 };
		Memory txm = new Memory(tlen + 1);
		txm.clear();
		txm.write(0, send.getBytes(), 0, tlen);

		int[] rxn = { 0 };
		Memory rxm = new Memory(tlen);

		OVERLAPPED osReader = new OVERLAPPED();
		osReader.writeField("hEvent", CreateEvent(null, true, false, null));
		check(osReader.hEvent != null, "CreateEvent/osReader");

		OVERLAPPED osWriter = new OVERLAPPED();
		osWriter.writeField("hEvent", CreateEvent(null, true, false, null));
		check(osWriter.hEvent != null, "CreateEvent/osWriter");

		boolean first = true;

		// First time through here send some stuff
		first = false;
		check(ResetEvent(osWriter.hEvent), "ResetEvent/osWriter.hEvent");
		boolean write = WriteFile(hComm, txm, tlen, txn, osWriter);
		if (!write) {
			check(GetLastError() == ERROR_IO_PENDING, "WriteFile");
			System.out.println("Write pending");
		}
		while (!write) {
			System.out.println("WaitForSingleObject/write");
			int dwRes = WaitForSingleObject(osWriter.hEvent, 1000);
			switch (dwRes) {
				case WAIT_OBJECT_0:
					if (!GetOverlappedResult(hComm, osWriter, txn, true))
						check(GetLastError() == ERROR_IO_INCOMPLETE, "GetOverlappedResult/osWriter");
					else
						write = true;
					break;
				case WAIT_TIMEOUT:
					System.out.println("write TIMEOT");
					break;
				default:
					check(false, "WaitForSingleObject/write");
					break;
			}
		}
		System.out.println("Transmit: '" + txm.getString(0) + "' , len=" + txn[0]);

		// First set up the read so that we actually get some overlap
		check(ResetEvent(osReader.hEvent), "ResetEvent/osReader.hEvent ");
		boolean read = ReadFile(hComm, rxm, tlen, rxn, osReader);
		if (!read) {
			check(GetLastError() == ERROR_IO_PENDING, "ReadFile");
			System.out.println("Read pending");
		}

		while (!read) {
			if (first) {
			}

			System.out.println("WaitForSingleObject/read");
			check(ResetEvent(osReader.hEvent), "ResetEvent/osReader.hEvent");
			int dwRes = WaitForSingleObject(osReader.hEvent, 1000);
			switch (dwRes) {
				case WAIT_OBJECT_0:
					if (!GetOverlappedResult(hComm, osReader, rxn, false))
						check(GetLastError() == ERROR_IO_INCOMPLETE, "GetOverlappedResult/osReader");
					else
						read = true;
					break;

				case WAIT_TIMEOUT:
					System.out.println("WAIT_TIMEOUT");
					break;

				default:
					check(false, "WaitForSingleObject/osReader.hEvent");
					break;
			}
		}

		System.out.println("Received: '" + rxm.getString(0) + "' , len=" + rxn[0]);
		check(CloseHandle(osWriter.hEvent), "CloseHandle/osWriter.hEvent");
		check(CloseHandle(osReader.hEvent), "CloseHandle/osReader.hEvent");
		check(CloseHandle(hComm), "CloseHandle/hComm");
		System.out.println("All done");
	}

	private void run() {
		test1();
	}

	public static void main(String[] args) {
		TestSuite ts = new TestSuite();
		ts.run();
	}

}
