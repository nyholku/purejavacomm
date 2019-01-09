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
import java.util.regex.Pattern;

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.IntegerType;
import com.sun.jna.Native;

import java.util.*;

import jtermios.windows.WinAPI.OVERLAPPED;

/**
 * JTermios provides a limited cross platform unix termios type interface to
 * serial ports.
 * 
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
	// is best and because then the implementation of that buffer is in one place, 
	// not in each of the JTermiosImpl classes. In this way Mac OS X (and presumably
	// Linux/Unix) does need not a buffer at all in JTermiosImpl. Windows needs a
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
	public static int O_CREAT = 0x00000200;
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
	public static int EBUSY = 16;
	public static int ENOTTY = 25;
	// termios.h stuff
	public static int TIOCM_RNG = 0x00000080;
	public static int TIOCM_CAR = 0x00000040;
	public static int IGNBRK = 0x00000001;
	public static int BRKINT = 0x00000002;
	public static int IGNPAR = 0x00000004;
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
	public static int B1200 = 1200;
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
	public static short POLLERR_OUT = 0x0008;
	public static short POLLNVAL = 0x0020;

	// misc stuff
	public static int DC1 = 0x11; // Ctrl-Q;
	public static int DC3 = 0x13; // Ctrl-S;

	// reference to single arc/os specific implementation
	private static JTermiosInterface m_Termios;

	public interface FDSet {
		public void FD_SET(int fd);

		public void FD_CLR(int fd);

		public boolean FD_ISSET(int fd);

		public void FD_ZERO();
	}

	public interface JTermiosInterface {
		public static class NativeSize extends IntegerType {

			/**
                     *
                     */
			private static final long serialVersionUID = 2398288011955445078L;
			/**
			 * Size of a size_t integer, in bytes.
			 */
			public static int SIZE = Native.SIZE_T_SIZE;//Platform.is64Bit() ? 8 : 4;

			/**
			 * Create a zero-valued Size.
			 */
			public NativeSize() {
				this(0);
			}

			/**
			 * Create a Size with the given value.
			 */
			public NativeSize(long value) {
				super(SIZE, value);
			}
		}

		public FDSet newFDSet();

		int pipe(int[] fds);

		void shutDown();

		int errno();

		int fcntl(int fd, int cmd, int arg);

		int setspeed(int fd, Termios termios, int speed);

		int cfgetispeed(Termios termios);

		int cfgetospeed(Termios termios);

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

		int ioctl(int fd, int cmd, int... data);

		int select(int n, FDSet read, FDSet write, FDSet error, TimeVal timeout);

		int poll(Pollfd[] fds, int nfds, int timeout);

		/**
		 * poll() on Windows has not been implemented and while implemented on
		 * Mac OS X, does not work for devices.
		 */
		boolean canPoll();

		void perror(String msg);

		List<String> getPortList();

		public String getPortNamePattern();

	}

	public void shutdown() {
		if (m_Termios != null)
			m_Termios.shutDown();
	}

	static { // INSTANTIATION 
		if (Platform.isMac()) {
			m_Termios = new jtermios.macosx.JTermiosImpl();
		} else if (Platform.isWindows()) {
			m_Termios = new jtermios.windows.JTermiosImpl();
		} else if (Platform.isLinux()) {
			m_Termios = new jtermios.linux.JTermiosImpl();
		} else if (Platform.isSolaris()) {
			m_Termios = new jtermios.solaris.JTermiosImpl();
		} else if (Platform.isFreeBSD()) {
			m_Termios = new jtermios.freebsd.JTermiosImpl();
		} else {
			log(0, "JTermios has no support for OS %s\n", System.getProperty("os.name"));
		}
	}

	static public int errno() {
		log = log && log(5, "> errno()\n");
		int ret = m_Termios.errno();
		log = log && log(3, "< errno() => %d\n", ret);
		return ret;
	}

	static public int fcntl(int fd, int cmd, int arg) {
		log = log && log(5, "> fcntl(%d, %d, %d)\n", fd, cmd, arg);
		int ret = m_Termios.fcntl(fd, cmd, arg);
		log = log && log(3, "< fcntl(%d, %d, %d) => %d\n", fd, cmd, arg, ret);
		return ret;
	}

	static public int cfgetispeed(Termios termios) {
		log = log && log(5, "> cfgetispeed(%s)\n", termios);
		int ret = m_Termios.cfgetispeed(termios);
		log = log && log(3, "< cfgetispeed(%s) => %d\n", termios, ret);
		return ret;
	}

	static public int cfgetospeed(Termios termios) {
		log = log && log(5, "> cfgetospeed(%s)\n", termios);
		int ret = m_Termios.cfgetospeed(termios);
		log = log && log(3, "< cfgetospeed(%s) => %d\n", termios, ret);
		return ret;
	}

	static public int cfsetispeed(Termios termios, int speed) {
		log = log && log(5, "> cfgetospeed(%s,%d)\n", termios, speed);
		int ret = m_Termios.cfsetispeed(termios, speed);
		log = log && log(3, "< cfgetospeed(%s,%d) => %d\n", termios, speed, ret);
		return ret;
	}

	static public int cfsetospeed(Termios termios, int speed) {
		log = log && log(5, "> cfgetospeed(%s,%d)\n", termios, speed);
		int ret = m_Termios.cfsetospeed(termios, speed);
		log = log && log(3, "< cfgetospeed(%s,%d) => %d\n", termios, speed, ret);
		return ret;
	}

	static public int setspeed(int fd, Termios termios, int speed) {
		log = log && log(5, "> setspeed(%d,%s,%d)\n", fd, termios, speed);
		int ret = m_Termios.setspeed(fd, termios, speed);
		log = log && log(3, "< setspeed(%d,%s,%d) => %d\n", fd, termios, speed, ret);
		return ret;
	}

	static public int tcflush(int a, int b) {
		log = log && log(5, "> tcflush(%d,%d)\n", a, b);
		int ret = m_Termios.tcflush(a, b);
		log = log && log(3, "< tcflush(%d,%d) => %d\n", a, b, ret);
		return ret;
	}

	static public int tcdrain(int fd) {
		log = log && log(5, "> tcdrain(%d)\n", fd);
		int ret = m_Termios.tcdrain(fd);
		log = log && log(3, "< tcdrain(%d) => %d\n", fd, ret);
		return ret;
	}

	static public void cfmakeraw(int fd, Termios termios) {
		log = log && log(5, "> cfmakeraw(%d,%s)\n", fd, termios);
		m_Termios.cfmakeraw(termios);
		log = log && log(3, "< cfmakeraw(%d,%s)\n", fd, termios);
	}

	static public int tcgetattr(int fd, Termios termios) {
		log = log && log(5, "> tcgetattr(%d,%s)\n", fd, termios);
		int ret = m_Termios.tcgetattr(fd, termios);
		log = log && log(3, "< tcgetattr(%d,%s) => %d\n", fd, termios, ret);
		return ret;
	}

	static public int tcsetattr(int fd, int cmd, Termios termios) {
		log = log && log(5, "> tcsetattr(%d,%d,%s)\n", fd, cmd, termios);
		int ret = m_Termios.tcsetattr(fd, cmd, termios);
		log = log && log(3, "< tcsetattr(%d,%d,%s) => %d\n", fd, cmd, termios, ret);
		return ret;
	}

	static public int tcsendbreak(int fd, int duration) {
		log = log && log(5, "> tcsendbreak(%d,%d,%s)\n", fd, duration);
		int ret = m_Termios.tcsendbreak(fd, duration);
		log = log && log(3, "< tcsendbreak(%d,%d,%s) => %d\n", fd, duration, ret);
		return ret;
	}

	static public int open(String s, int t) {
		log = log && log(5, "> open('%s',%08X)\n", s, t);
		int ret = m_Termios.open(s, t);
		log = log && log(3, "< open('%s',%08X) => %d\n", s, t, ret);
		return ret;
	}

	static public int close(int fd) {
		log = log && log(5, "> close(%d)\n", fd);
		int ret = m_Termios.close(fd);
		log = log && log(3, "< close(%d) => %d\n", fd, ret);
		return ret;
	}

	static public int write(int fd, byte[] buffer, int len) {
		log = log && log(5, "> write(%d,%s,%d)\n", fd, log(buffer, 8), len);
		int ret = m_Termios.write(fd, buffer, len);
		log = log && log(3, "< write(%d,%s,%d) => %d\n", fd, log(buffer, 8), len, ret);
		return ret;
	}

	static public int read(int fd, byte[] buffer, int len) {
		log = log && log(5, "> read(%d,%s,%d)\n", fd, log(buffer, 8), len);
		int ret = m_Termios.read(fd, buffer, len);
		log = log && log(3, "< read(%d,%s,%d) => %d\n", fd, log(buffer, 8), len, ret);
		return ret;
	}

	static public int ioctl(int fd, int cmd, int... data) {
		log = log && log(5, "> ioctl(%d,%d,[%s])\n", fd, cmd, Arrays.toString(data));
		int ret = m_Termios.ioctl(fd, cmd, data);
		log = log && log(3, "< ioctl(%d,%d,[%s]) => %d\n", fd, cmd, Arrays.toString(data), ret);
		return ret;
	}

	private static String toString(int n, FDSet fdset) {
		StringBuffer s = new StringBuffer("[");
		for (int fd = 0; fd < n; fd++) {
			if (fd > 0)
				s.append(",");
			if (FD_ISSET(fd, fdset))
				s.append(Integer.toString(fd));
		}
		s.append("]");
		return s.toString();
	}

	/**
	 * Unlike Linux select this does not modify 'timeout' so it can be re-used.
	 * 
	 */
	static public int select(int n, FDSet read, FDSet write, FDSet error, TimeVal timeout) {
		log = log && log(5, "> select(%d,%s,%s,%s,%s)\n", n, toString(n, read), toString(n, write), toString(n, error), timeout);
		int ret = m_Termios.select(n, read, write, error, timeout);
		log = log && log(3, "< select(%d,%s,%s,%s,%s) => %d\n", n, toString(n, read), toString(n, write), toString(n, error), timeout, ret);
		return ret;
	}

	static public int poll(Pollfd[] fds, int nfds, int timeout) {
		log = log && log(5, "> poll(%s,%d,%d)\n", log(fds, 8), nfds, timeout);
		int ret = m_Termios.poll(fds, nfds, timeout);
		log = log && log(3, "< poll(%s,%d,%d) => %d\n", log(fds, 8), nfds, timeout, ret);
		return ret;
	}

	static public boolean canPoll() {
		return m_Termios.canPoll();
	}

	static public int pipe(int[] fds) {
		log = log && log(5, "> pipe([%d,%d,%d])\n", fds.length, fds[0], fds[1]);
		int ret = m_Termios.pipe(fds);
		log = log && log(3, "< pipe([%d,%d,%d]) => %d\n", fds.length, fds[0], fds[1], ret);
		return ret;
	}

	static public void perror(String msg) {
		m_Termios.perror(msg);
	}

	static public FDSet newFDSet() {
		return m_Termios.newFDSet();
	}

	static public void FD_SET(int fd, FDSet set) {
		if (set != null)
			set.FD_SET(fd);
	}

	static public void FD_CLR(int fd, FDSet set) {
		if (set != null)
			set.FD_CLR(fd);
	}

	static public boolean FD_ISSET(int fd, FDSet set) {
		if (set == null)
			return false;
		return set.FD_ISSET(fd);
	}

	static public void FD_ZERO(FDSet set) {
		if (set != null)
			set.FD_ZERO();
	}

	static public List<String> getPortList() {
		return m_Termios.getPortList();

	}

	static public Pattern getPortNamePattern(jtermios.JTermios.JTermiosInterface jtermios) {
		String ps = System.getProperty("purejavacomm.portnamepattern." + jtermios.getClass().getName());
		if (ps == null)
			ps = System.getProperty("purejavacomm.portnamepattern");
		if (ps == null)
			ps = jtermios.getPortNamePattern();
		return Pattern.compile(ps);
	}

	public static class JTermiosLogging {
		private static int LOG_MASK = 1;
		public static boolean log = false;

		static { // initialization 
			String loglevel = System.getProperty("purejavacomm.loglevel");
			if (loglevel != null)
				setLogLevel(Integer.parseInt(loglevel));
		}

		public static String lineno() {
			return lineno(0);
		}

		public static String lineno(int n) {
			StackTraceElement e = Thread.currentThread().getStackTrace()[2 + n];
			return String.format("class %s line% d", e.getClassName(), e.getLineNumber());
		}

		public static String ref(Pointer pointer) {
			if (pointer == null)
				return "null";
			else
				return pointer.toString();
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

		public static String log(int[] ints, int n) {
			StringBuffer b = new StringBuffer();
			if (n < 0 || n > ints.length)
				n = ints.length;
			b.append(String.format("[%d", ints.length));
			for (int i = 0; i < n; i++)
				b.append(String.format(",0x%08X", ints[i]));
			if (n < ints.length)
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

		static private StringBuffer buffer = new StringBuffer();

		static public boolean log(int l, String format, Object... args) {
			if (l == 0 || LOG_MASK != 0) {
				synchronized (buffer) {
					buffer.setLength(0);
					if ((LOG_MASK & (1 << (5))) != 0)
						buffer.append(String.format("%06d,", System.currentTimeMillis() % 1000000));
					if ((LOG_MASK & (1 << (6))) != 0) {
						buffer.append(lineno(2));
						buffer.append(", ");
					}
					if ((LOG_MASK & (1 << (7))) != 0) {
						buffer.append("thread id ");
						buffer.append(Thread.currentThread().getId());
						buffer.append(", ");
						buffer.append(Thread.currentThread().getName());
						buffer.append(", ");
					}
					if (l == 0 || (LOG_MASK & (1 << (l - 1))) != 0)
						buffer.append(String.format(format, args));
					if (buffer.length() > 0) {
						System.err.printf("log: " + buffer.toString());
					}
				}
			}
			return true;
		}

		public static void setLogLevel(int l) {
			LOG_MASK = 0;
			for (int i = 0; i < l; i++) {
				LOG_MASK = (LOG_MASK << 1) + 1;
			}
			log = LOG_MASK != 0;
		}

		public static void setLogMask(int mask) {
			LOG_MASK = mask;
			log = LOG_MASK != 0;
		}
	}

}
