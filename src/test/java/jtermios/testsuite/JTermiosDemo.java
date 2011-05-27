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
import static jtermios.windows.WinAPI.*;

import java.util.List;

import jtermios.FDSet;
import jtermios.Termios;
import jtermios.TimeVal;

public class JTermiosDemo {
	private static void fail(String msg) {
		System.out.println("Fail: " + msg);
		System.exit(0);
	}

	public static void main(String[] args) {
		System.out.println("JTermio simple loopback demo");
		List<String> portlist = getPortList();
		String port = "COM5:";
		for (String pname : portlist) {
			System.out.println("Found port " + pname);
			port = pname;
		}

		//port = "/dev/tty.usbserial-FTOXM3NX";
		int fd = open(port, O_RDWR | O_NOCTTY | O_NONBLOCK);
		if (fd == -1)
			fail("Could not open " + port);

		fcntl(fd, F_SETFL, 0);

		Termios opts = new Termios();

		tcgetattr(fd, opts);

		opts.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);

		opts.c_cflag |= (CLOCAL | CREAD);
		opts.c_cflag &= ~PARENB;
		opts.c_cflag |= CSTOPB;
		opts.c_cflag &= ~CSIZE;
		opts.c_cflag |= CS8;

		opts.c_oflag &= ~OPOST;

		opts.c_iflag &= ~INPCK;
		opts.c_iflag &= ~(IXON | IXOFF | IXANY);
		opts.c_cc[VMIN] = 0;
		opts.c_cc[VTIME] = 10;

		cfsetispeed(opts, B9600);
		cfsetospeed(opts, B9600);

		tcsetattr(fd, TCSANOW, opts);

		tcflush(fd, TCIOFLUSH);

		byte[] tx = "Not so very long text string".getBytes();
		byte[] rx = new byte[tx.length];
		int l = tx.length;
		int n = write(fd, tx, l);
		if (n < 0) {
			System.out.println("write() failed ");
			System.exit(0);
		}
		System.out.println("Transmitted '" + new String(tx) + "' len=" + n);

		FDSet rdset = newFDSet();
		FD_ZERO(rdset);
		FD_SET(fd, rdset);

		TimeVal tout = new TimeVal();
		tout.tv_sec = 10;

		byte buffer[] = new byte[1024];

		while (l > 0) {
			int s = select(fd + 1, rdset, null, null, tout);
			if (s < 0) {
				System.out.println("select() failed ");
				System.exit(0);
			}
			int m = read(fd, buffer, l);
			if (m < 0) {
				System.out.println("read() failed ");
				System.exit(0);
			}
			System.arraycopy(buffer, 0, rx, rx.length - l, m);
			l -= m;
		}

		System.out.println("Received    '" + new String(rx) + "'");
		int ec = close(fd);
	}
}
