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
import jtermios.FDSet;
import jtermios.Termios;
import jtermios.TimeVal;

public class TermiosTest {
	public static void main(String[] args) {
		System.out.println("Termios test");

		String filename = "/dev/tty.usbserial-FTOXM3NX";
		int fd = open(filename, O_RDWR | O_NOCTTY | O_NONBLOCK);
		System.out.println("fd=" + fd);

		fcntl(fd, F_SETFL, 0);

		Termios opts = new Termios();

		tcgetattr(fd, opts);

		opts.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);

		opts.c_cflag |= (CLOCAL | CREAD);
		opts.c_cflag &= ~PARENB;
		opts.c_cflag |= CSTOPB; // two stop bits
		opts.c_cflag &= ~CSIZE;
		opts.c_cflag |= CS8;

		opts.c_oflag &= ~OPOST;

		opts.c_iflag &= ~INPCK;
		opts.c_iflag &= ~(IXON | IXOFF | IXANY);
		opts.c_cc[VMIN] = 0;
		opts.c_cc[VTIME] = 10;//0.1 sec

		cfsetispeed(opts, B9600);
		cfsetospeed(opts, B9600);

		tcsetattr(fd, TCSANOW, opts);

		tcflush(fd, TCIOFLUSH);

		byte[] b = "Not so very long text string".getBytes();
		byte[] b2 = new byte[b.length];
		int l = b.length;
		int n = write(fd, b, l);
		System.out.println("sent " + n);

		FDSet rdset = newFDSet();
		FD_ZERO(rdset);
		FD_SET(fd, rdset);

		TimeVal tout = new TimeVal();
		tout.tv_sec = 10;

		int s = select(fd + 1, rdset, null, null, tout);
		System.out.println("select " + s);
		for (int i = 0; i < 1024; i++)
			if (FD_ISSET(i, rdset))
				System.out.println("rd FD_ISSET(" + i + ")");

		int m = read(fd, b2, l);
		System.out.println("received " + m);
		for (int i = 0; i < m; i++)
			System.out.printf("%c", b2[i]);
		System.out.println();
		int ec = close(fd);
	}
}
