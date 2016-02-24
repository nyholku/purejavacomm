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

package jtermios.testsuite;

import static jtermios.JTermios.*;
import static jtermios.testsuite.TestBase.S;
import static jtermios.testsuite.TestBase.fail;

import jtermios.Termios;
import jtermios.TimeVal;

public class JTermiosDemo {

	public static void run() throws TestFailedException {
		String port = TestBase.getPortName();

		int fd;
		S(fd = open(port, O_RDWR | O_NOCTTY | O_NONBLOCK), "failed to open port");

		Termios opts = new Termios();

		S(tcgetattr(fd, opts));

		opts.c_iflag = IGNBRK | IGNPAR;
		opts.c_oflag = 0;
		opts.c_cflag = CLOCAL | CREAD | CS8;
		opts.c_lflag = 0;
		
		opts.c_cc[VMIN] = 0;
		opts.c_cc[VTIME] = 10;

		cfsetispeed(opts, B9600);
		cfsetospeed(opts, B9600);

		S(tcsetattr(fd, TCSANOW, opts));

		S(tcflush(fd, TCIOFLUSH));

		final String TEST_STRING = "Not so very long text string";
		
		byte[] tx = TEST_STRING.getBytes();
		byte[] rx = new byte[tx.length];
		int l = tx.length;
		S(write(fd, tx, l), "write() failed ");

		FDSet rdset = newFDSet();
		FD_ZERO(rdset);
		FD_SET(fd, rdset);

		TimeVal tout = new TimeVal();
		tout.tv_sec = 10;

		byte buffer[] = new byte[1024];

		while (l > 0) {
			int s;
			S(s = select(fd + 1, rdset, null, null, tout), "select() failed ");
			if (s == 0) {
				fail("Timeout (no dongle connected?)");
			} else {
				int m;
				S(m = read(fd, buffer, l), "read() failed ");
				System.arraycopy(buffer, 0, rx, rx.length - l, m);
				l -= m;
			}
		}

		if (!new String(rx).equals(TEST_STRING)) {
			fail("Didn't receive what we expected (is \"%s\", should be \"%s\")", new String(rx), TEST_STRING);
		}
		S(close(fd));
	}
}
