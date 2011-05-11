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

package jtermios;

import static jtermios.JTermios.JTermiosLogging.*;

import java.util.List;

import com.sun.jna.Platform;
import com.sun.jna.Structure;

/**
 * JTermios provides a limited cross platform unix termios type interface to serial ports.
 * @author nyholku
 *
 */
public class JTermios {

	// Note About the read/write methods and the buffers
	//
	// This library provides read/write(byte[] buffer,int length) without an offset
	// to the buffer. This is because it appears that there is a bug in JNA's use of
	// ByteBuffer.wrap(byte[] buffer,int offset,int length) in that the offset gets
	// ignored. So this needs to be handled in the JTermiosImpl classes
	// or somewhere else. Handling the offset requires a buffer to hold 
	// temporarily the bytes. I deemed that it is better to pass the buck ie burden 
	// to the clients of JTermios as they know better what size of buffer (if any) 
	// is best and because then the implementation that buffer is in one place, 
	// not in each of the JTermiosImpl classes. In this way Mac OS X (and presumably
	// Linux/Unix) does not a buffer at all in JTermiosImpl. Windows needs a
	// JNA Memory buffer anyway because of the limitations inherent in using 
	// Overlapped I/O with JNA.

	// The 'constants' here, which are equivalent to the corresponding #defines in C
	// come from Mac OS X 10.6.6 / x86_64 architecture
	// Every implementing class for each architecture needs to initialize them in 
	// their JTermiosImpl constructor. For Windows the termios functionality is
	// totally emulated so jtermios.windows.JTermiosImpl can just use these default values as
	// can obviously jtermios.macosx.JTermiosImpl (at least for x86_64 architecture).
	// Much as we liked these cannot be defined 'final' but should be treated immutable all the same.

	// sys/filio.h stuff
	public static int FIONREAD = 0x4004667F;
	// fcntl.h stuff
	public static int O_RDWR = 0x00000002;
	public static int O_NONBLOCK = 0x00000004;
	public static int O_NOCTTY = 0x00020000;
	public static int O_NDELAY = 0x00000004;
	public static int F_GETFL = 0x00000003;
	public static int F_SETFL = 0x00000004;
	// errno.h stuff
	public static int EAGAIN = 35;
	public static int EBADF = 9;
	public static int EACCES = 22;
	public static int EEXIST = 17;
	public static int EINTR = 4;
	public static int EINVAL = 22;
	public static int EIO = 5;
	public static int EISDIR = 21;
	public static int ELOOP = 62;
	public static int EMFILE = 24;
	public static int ENAMETOOLONG = 63;
	public static int ENFILE = 23;
	public static int ENOENT = 2;
	public static int ENOSR = 98;
	public static int ENOSPC = 28;
	public static int ENOTDIR = 20;
	public static int ENXIO = 6;
	public static int EOVERFLOW = 84;
	public static int EROFS = 30;
	public static int ENOTSUP = 45;
	// termios.h stuff
	public static int TIOCM_RNG = 0x00000080;
	public static int TIOCM_CAR = 0x00000040;
	public static int IGNBRK = 0x00000001;
	public static int BRKINT = 0x00000002;
	public static int PARMRK = 0x00000008;
	public static int INLCR = 0x00000040;
	public static int IGNCR = 0x00000080;
	public static int ICRNL = 0x00000100;
	public static int ECHONL = 0x00000010;
	public static int IEXTEN = 0x00000400;
	public static int CLOCAL = 0x00008000;
	public static int OPOST = 0x00000001;
	public static int VSTART = 0x0000000C;
	public static int TCSANOW = 0x00000000;
	public static int VSTOP = 0x0000000D;
	public static int VMIN = 0x00000010;
	public static int VTIME = 0x00000011;
	public static int VEOF = 0x00000000;
	public static int TIOCMGET = 0x4004746A;
	public static int TIOCM_CTS = 0x00000020;
	public static int TIOCM_DSR = 0x00000100;
	public static int TIOCM_RI = 0x00000080;
	public static int TIOCM_CD = 0x00000040;
	public static int TIOCM_DTR = 0x00000002;
	public static int TIOCM_RTS = 0x00000004;
	public static int ICANON = 0x00000100;
	public static int ECHO = 0x00000008;
	public static int ECHOE = 0x00000002;
	public static int ISIG = 0x00000080;
	public static int TIOCMSET = 0x8004746D;
	public static int IXON = 0x00000200;
	public static int IXOFF = 0x00000400;
	public static int IXANY = 0x00000800;
	public static int CRTSCTS = 0x00030000;
	public static int TCSADRAIN = 0x00000001;
	public static int INPCK = 0x00000010;
	public static int ISTRIP = 0x00000020;
	public static int CSIZE = 0x00000300;
	public static int TCIFLUSH = 0x00000001;
	public static int TCOFLUSH = 0x00000002;
	public static int TCIOFLUSH = 0x00000003;
	public static int CS5 = 0x00000000;
	public static int CS6 = 0x00000100;
	public static int CS7 = 0x00000200;
	public static int CS8 = 0x00000300;
	public static int CSTOPB = 0x00000400;
	public static int CREAD = 0x00000800;
	public static int PARENB = 0x00001000;
	public static int PARODD = 0x00002000;
	public static int CMSPAR = 010000000000; // Is this standard ? Not available on Mac OS X
	//public static int CCTS_OFLOW = 0x00010000; // Not linux
	//public static int CRTS_IFLOW = 0x00020000; // Not linux
	//public static int CDTR_IFLOW = 0x00040000; // Not linux
	//public static int CDSR_OFLOW = 0x00080000; // Not linux
	//public static int CCAR_OFLOW = 0x00100000; // Not linux
	public static int B0 = 0;
	public static int B50 = 50;
	public static int B75 = 75;
	public static int B110 = 110;
	public static int B134 = 134;
	public static int B150 = 150;
	public static int B200 = 200;
	public static int B300 = 300;
	public static int B600 = 600;
	public static int B1200 = 600;
	public static int B1800 = 1800;
	public static int B2400 = 2400;
	public static int B4800 = 4800;
	public static int B9600 = 9600;
	public static int B19200 = 19200;
	public static int B38400 = 38400;
	public static int B7200 = 7200; // Not Linux
	public static int B14400 = 14400;// Not Linux
	public static int B28800 = 28800;// Not Linux
	public static int B57600 = 57600;
	public static int B76800 = 76800; // Not Linux
	public static int B115200 = 115200;
	public static int B230400 = 230400;
	// poll.h stuff
	public static short POLLIN = 0x0001;
	//public static short POLLRDNORM = 0x0040; // Not Linux
	//public static short POLLRDBAND = 0x0080; // Not Linux
	public static short POLLPRI = 0x0002;
	public static short POLLOUT = 0x0004;
	//public static short POLLWRNORM = 0x0004; // Not Linux
	//public static short POLLWRBAND = 0x0100; // Not Linux
	public static short POLLERR = 0x0008;
	public static short POLLNVAL = 0x0020;

	// misc stuff
	public static int DC1 = 0x11; // Ctrl-Q;
	public static int DC3 = 0x13; // Ctrl-S;

	// reference to single arc/os specific implementation
	private static JTermiosInterface m_Termios;

	public interface JTermiosInterface {
		void shutDown();

		int fcntl(int fd, int cmd, int arg);

		long cfgetispeed(Termios termios);

		long cfgetospeed(Termios termios);

		int cfsetispeed(Termios termios, int speed);

		int cfsetospeed(Termios termios, int speed);

		int tcflush(int fd, int b);

		int tcdrain(int fd);

		void cfmakeraw(Termios termios);

		int tcgetattr(int fd, Termios termios);

		int tcsetattr(int fd, int cmd, Termios termios);

		int tcsendbreak(int fd, int duration);

		int open(String s, int t);

		int close(int fd);

		int write(int fd, byte[] buffer, int len);

		int read(int fd, byte[] buffer, int len);

		int ioctl(int fd, int cmd, int[] data);

		int select(int n, FDSet read, FDSet write, FDSet error, TimeVal timeout);

		/**
		 * poll() on Windows has not been implemented and while implemented on
		 * Mac OS X, does not work for devices.
		 */
		int poll(Pollfd[] fds, int nfds, int timeout);

		void perror(String msg);

		FDSet newFDSet();

		void FD_SET(int fd, FDSet set);

		void FD_CLR(int fd, FDSet set);

		boolean FD_ISSET(int fd, FDSet set);

		void FD_ZERO(FDSet set);

		List<String> getPortList();
	}

	static { // INSTANTIATION 
		JTermiosLogging.setLogLevel(0);
		int path_max;
		if (Platform.isMac()) {
			m_Termios = new jtermios.macosx.JTermiosImpl();
		} else if (Platform.isWindows()) {
			m_Termios = new jtermios.windows.JTermiosImpl();
		} else if (Platform.isLinux()){
			m_Termios = new jtermios.linux.JTermiosImpl();
		} else {
			log(0, "JTermios has no support for OS %s\n", System.getProperty("os.name"));
		}
		if (m_Termios != null) {
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

				public void run() {
					m_Termios.shutDown();
				}
			}));
		}
	}

	static public int fcntl(int fd, int cmd, int arg) {
		return m_Termios.fcntl(fd, cmd, arg);
	}

	static public long cfgetispeed(Termios termios) {
		return m_Termios.cfgetispeed(termios);
	}

	static public long cfgetospeed(Termios termios) {
		return m_Termios.cfgetospeed(termios);
	}

	static public int cfsetispeed(Termios termios, int speed) {
		return m_Termios.cfsetispeed(termios, speed);
	}

	static public int cfsetospeed(Termios termios, int speed) {
		return m_Termios.cfsetospeed(termios, speed);
	}

	static public int tcflush(int a, int b) {
		return m_Termios.tcflush(a, b);
	}

	static public int tcdrain(int fd) {
		return m_Termios.tcdrain(fd);
	}

	static public int tcgetattr(int fd, Termios termios) {
		return m_Termios.tcgetattr(fd, termios);
	}

	static public int tcsetattr(int fd, int cmd, Termios termios) {
		return m_Termios.tcsetattr(fd, cmd, termios);
	}

	static public int tcsendbreak(int fd, int duration) {
		return m_Termios.tcsendbreak(fd, duration);
	}

	static public int open(String s, int t) {
		return m_Termios.open(s, t);
	}

	static public int close(int fd) {
		return m_Termios.close(fd);
	}

	static public int write(int fd, byte[] buffer, int len) {
		return m_Termios.write(fd, buffer, len);
	}

	static public int read(int fd, byte[] buffer, int len) {
		return m_Termios.read(fd, buffer, len);
	}

	static public int ioctl(int fd, int cmd, int[] data) {
		return m_Termios.ioctl(fd, cmd, data);
	}

	/**
	 * Unlike Linux select this does not modify 'timeout' so it can be re-used.
	 * 
	 */
	static public int select(int n, FDSet read, FDSet write, FDSet error, TimeVal timeout) {
		return m_Termios.select(n, read, write, error, timeout);
	}

	static public int poll(Pollfd[] fds, int nfds, int timeout) {

		return m_Termios.poll(fds, nfds, timeout);
	}

	static public void perror(String msg) {
		m_Termios.perror(msg);
	}

	static public FDSet newFDSet() {
		return m_Termios.newFDSet();
	}

	static public void FD_SET(int fd, FDSet set) {
		m_Termios.FD_SET(fd, set);
	}

	static public void FD_CLR(int fd, FDSet set) {
		m_Termios.FD_CLR(fd, set);
	}

	static public boolean FD_ISSET(int fd, FDSet set) {
		return m_Termios.FD_ISSET(fd, set);
	}

	static public void FD_ZERO(FDSet set) {
		m_Termios.FD_ZERO(set);
	}

	static public List<String> getPortList() {
		return m_Termios.getPortList();

	}

	public static class JTermiosLogging {
		public static int LOG_LEVEL;
		public static boolean log;

		public static String lineno() {
			return lineno(0);
		}

		public static String lineno(int n) {
			StackTraceElement e = Thread.currentThread().getStackTrace()[2 + n];
			return String.format("class '%s', line% d", e.getClassName(), e.getLineNumber());
		}

		public static String ref(Structure struct) {
			if (struct == null)
				return "null";
			else
				return struct.getPointer().toString();
		}

		public static String log(byte[] bts, int n) {
			StringBuffer b = new StringBuffer();
			if (n < 0 || n > bts.length)
				n = bts.length;
			b.append(String.format("[%d", bts.length));
			for (int i = 0; i < n; i++)
				b.append(String.format(",0x%02X", bts[i]));
			if (n < bts.length)
				b.append("...");
			b.append("]");
			return b.toString();
		}

		public static String log(char[] bts, int n) {
			StringBuffer b = new StringBuffer();
			if (n < 0 || n > bts.length)
				n = bts.length;
			b.append(String.format("[%d", bts.length));
			for (int i = 0; i < n; i++)
				b.append(String.format(",%c", bts[i]));
			if (n < bts.length)
				b.append("...");
			b.append("]");
			return b.toString();
		}

		public static String log(Object[] bts, int n) {
			StringBuffer b = new StringBuffer();
			if (n < 0 || n > bts.length)
				n = bts.length;
			b.append(String.format("[%d", bts.length));
			for (int i = 0; i < n; i++) {
				b.append(",");
				b.append(bts[i] != null ? bts[i].toString() : "null");
			}
			if (n < bts.length)
				b.append("...");
			b.append("]");
			return b.toString();
		}

		static public boolean log(int l, String format, Object... args) {
			if (LOG_LEVEL >= l) {
				System.out.printf("log: " + format, args);
			}
			return true;
		}

		public static void setLogLevel(int l) {
			LOG_LEVEL = l;
			log = l != 0;
		}
	}

}
