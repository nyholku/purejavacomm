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

package jtermios.linux;

import java.io.File;

import java.nio.Buffer;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import jtermios.FDSet;

import jtermios.Pollfd;
import jtermios.Termios;
import jtermios.TimeVal;
import jtermios.linux.JTermiosImpl.Linux_C_lib.pollfd;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;

import static jtermios.JTermios.*;
import static jtermios.JTermios.JTermiosLogging.log;

public class JTermiosImpl implements jtermios.JTermios.JTermiosInterface {
	private static String DEVICE_DIR_PATH = "/dev/";
	static Linux_C_lib m_Clib = (Linux_C_lib) Native.loadLibrary("c", Linux_C_lib.class);

	public interface Linux_C_lib extends com.sun.jna.Library {
		public IntByReference __error();
			
		public int tcdrain(int fd);

		public void cfmakeraw(Termios termios);

		public int fcntl(int fd, int cmd, int[] arg);

		public int fcntl(int fd, int cmd, int arg);

		public int ioctl(int fd, int cmd, int[] arg);

		public int open(String path, int flags);

		public int close(int fd);

		public int tcgetattr(int fd, Termios termios);

		public int tcsetattr(int fd, int cmd, Termios termios);

		public int cfsetispeed(Termios termios, NativeLong i);

		public int cfsetospeed(Termios termios, NativeLong i);

		public NativeLong cfgetispeed(Termios termios);

		public NativeLong cfgetospeed(Termios termios);

		public NativeLong write(int fd, ByteBuffer buffer, NativeLong count);

		public NativeLong read(int fd, ByteBuffer buffer, NativeLong count);

		public int select(int n, int[] read, int[] write, int[] error, TimeVal timeout);

		public int poll(pollfd[] fds, int nfds, int timeout);

		public int tcflush(int fd, int qs);

		public void perror(String msg);

		static public class TimeVal extends Structure {
			public NativeLong tv_sec;
			public NativeLong tv_usec;

			public TimeVal(jtermios.TimeVal timeout) {
				tv_sec = new NativeLong(timeout.tv_sec);
				tv_usec = new NativeLong(timeout.tv_usec);
			}
		}

		static public class pollfd extends Structure {
			public int fd;
			public short events;
			public short revents;

			public pollfd(Pollfd pfd) {
				fd = pfd.fd;
				events = pfd.events;
				revents = pfd.revents;
			}
		}

		static public class Termios extends Structure {
			public int c_iflag;
			public int c_oflag;
			public int c_cflag;
			public int c_lflag;
			public byte[] c_cc = new byte[32];
			public int c_ispeed;
			public int c_ospeed;

			public Termios() {
			}

			public Termios(jtermios.Termios t) {
				c_iflag=t.c_iflag;
				c_oflag=t.c_oflag;
				c_cflag=t.c_cflag;
				c_lflag=t.c_lflag;
				System.arraycopy(t.c_cc, 0, c_cc, 0, t.c_cc.length);
				c_ispeed=t.c_ispeed;
				c_ospeed=t.c_ospeed;
			}

			public void update(jtermios.Termios t) {
				t.c_iflag = c_iflag;
				t.c_oflag = c_oflag;
				t.c_cflag = c_cflag;
				t.c_lflag = c_lflag;
				System.arraycopy(c_cc, 0, t.c_cc, 0, t.c_cc.length);
				t.c_ispeed = c_ispeed;
				t.c_ospeed = c_ospeed;
			}		
		}
	}

	static private class FDSetImpl extends FDSet {
		static final int FD_SET_SIZE = 1024;
		static final int NFBBITS = 32;
		int[] bits = new int[(FD_SET_SIZE + NFBBITS - 1) / NFBBITS];

		public String toString() {
			return String.format("%08X%08X", bits[0], bits[1]);
		}
	}

	public JTermiosImpl() {
		log = log && log(1, "instantiating %s\n", getClass().getCanonicalName());

		//linux/serial.h stuff
		FIONREAD  =      0x541B; // Looked up manually
		//fcntl.h stuff
		O_RDWR = 0x00000002;
		O_NONBLOCK= 0x00000800;
		O_NOCTTY = 0x00000100;
		O_NDELAY = 0x00000800;
		F_GETFL = 0x00000003;
		F_SETFL = 0x00000004;
		//errno.h stuff
		EAGAIN = 35;
		EACCES= 22;
		EEXIST= 17;
		EINTR= 4;
		EINVAL= 22;
		EIO= 5;
		EISDIR= 21;
		ELOOP= 40;
		EMFILE= 24;
		ENAMETOOLONG= 36;
		ENFILE= 23;
		ENOENT= 2;
		ENOSR= 63;
		ENOSPC= 28;
		ENOTDIR= 20;
		ENXIO= 6;
		EOVERFLOW= 75;
		EROFS= 30;
		ENOTSUP= 95;
		//termios.h stuff
		TIOCM_RNG = 0x00000080;
		TIOCM_CAR = 0x00000040;
		IGNBRK = 0x00000001;
		BRKINT = 0x00000002;
		PARMRK = 0x00000008;
		INLCR = 0x00000040;
		IGNCR = 0x00000080;
		ICRNL = 0x00000100;
		ECHONL = 0x00000040;
		IEXTEN = 0x00008000;
		CLOCAL = 0x00000800;
		OPOST = 0x00000001;
		VSTART = 0x00000008;
		TCSANOW = 0x00000000;
		VSTOP = 0x00000009;
		VMIN = 0x00000006;
		VTIME = 0x00000005;
		VEOF = 0x00000004;
		TIOCMGET = 0x00005415;
		TIOCM_CTS = 0x00000020;
		TIOCM_DSR = 0x00000100;
		TIOCM_RI = 0x00000080;
		TIOCM_CD = 0x00000040;
		TIOCM_DTR = 0x00000002;
		TIOCM_RTS = 0x00000004;
		ICANON = 0x00000002;
		ECHO = 0x00000008;
		ECHOE = 0x00000010;
		ISIG = 0x00000001;
		TIOCMSET= 0x00005418;
		IXON = 0x00000400;
		IXOFF = 0x00001000;
		IXANY = 0x00000800;
		CRTSCTS = 0x80000000;
		TCSADRAIN = 0x00000001;
		INPCK = 0x00000010;
		ISTRIP = 0x00000020;
		CSIZE = 0x00000030;
		TCIFLUSH = 0x00000000;
		TCOFLUSH = 0x00000001;
		TCIOFLUSH = 0x00000002;
		CS5 = 0x00000000;
		CS6 = 0x00000010;
		CS7 = 0x00000020;
		CS8 = 0x00000030;
		CSTOPB = 0x00000040;
		CREAD = 0x00000080;
		PARENB = 0x00000100;
		PARODD = 0x00000200;
		B0 = 0;
		B50 = 1;
		B75 = 2;
		B110 = 3;
		B134 = 4;
		B150 = 5;
		B200 = 6;
		B300 = 7;
		B600 = 8;
		B1200 = 8;
		B1800 = 10;
		B2400 = 11;
		B4800 = 12;
		B9600 = 13;
		B19200 = 14;
		B38400 = 15;
		B57600 = 4097;
		B115200 = 4098;
		B230400 = 4099;
		//poll.h stuff
		POLLIN = 0x0001;
		POLLPRI = 0x0002;
		POLLOUT = 0x0004;
		POLLERR = 0x0008;
		POLLNVAL = 0x0020;
		//select.h stuff

		}
	
	public int errno() {
		return Native.getLastError();
		}

	public void cfmakeraw(Termios termios) {
		Linux_C_lib.Termios t = new Linux_C_lib.Termios(termios);
		m_Clib.cfmakeraw(t);
	}

	public int fcntl(int fd, int cmd, int[] arg) {
		return m_Clib.fcntl(fd, cmd, arg);
	}

	public int fcntl(int fd, int cmd, int arg) {
		return m_Clib.fcntl(fd, cmd, arg);
	}

	public int tcdrain(int fd) {
		return m_Clib.tcdrain(fd);
	}

	public int cfgetispeed(Termios termios) {
		return m_Clib.cfgetispeed(new Linux_C_lib.Termios(termios)).intValue();
	}

	public int cfgetospeed(Termios termios) {
		return m_Clib.cfgetospeed(new Linux_C_lib.Termios(termios)).intValue();
	}

	public int cfsetispeed(Termios termios, int speed) {
		Linux_C_lib.Termios t = new Linux_C_lib.Termios(termios);
		int ret = m_Clib.cfsetispeed(t, new NativeLong(speed));
		t.update(termios);
		return ret;
	}

	public int cfsetospeed(Termios termios, int speed) {
		Linux_C_lib.Termios t = new Linux_C_lib.Termios(termios);
		int ret = m_Clib.cfsetospeed(t, new NativeLong(speed));
		t.update(termios);
		return ret;
	}

	public int open(String s, int t) {
		if (s != null && !s.startsWith("/"))
			s = DEVICE_DIR_PATH + s;
		return m_Clib.open(s, t);
	}

	public int read(int fd, byte[] buffer, int len) {
		return m_Clib.read(fd, ByteBuffer.wrap(buffer), new NativeLong(len)).intValue();
	}

	public int write(int fd, byte[] buffer, int len) {
		return m_Clib.write(fd, ByteBuffer.wrap(buffer), new NativeLong(len)).intValue();
	}

	public int close(int fd) {
		return m_Clib.close(fd);
	}

	public int tcflush(int fd, int b) {
		return m_Clib.tcflush(fd, b);
	}

	public int tcgetattr(int fd, Termios termios) {
		Linux_C_lib.Termios t = new Linux_C_lib.Termios();
		int ret = m_Clib.tcgetattr(fd, t);
		t.update(termios);
		return ret;
	}

	public void perror(String msg) {
		m_Clib.perror(msg);
	}

	public int tcsendbreak(int fd, int duration) {
		throw new IllegalArgumentException("Unimplemented function");
	}

	public int tcsetattr(int fd, int cmd, Termios termios) {
		return m_Clib.tcsetattr(fd, cmd, new Linux_C_lib.Termios(termios));
	}

	public void FD_CLR(int fd, FDSet set) {
		if (set == null)
			return;
		FDSetImpl p = (FDSetImpl) set;
		p.bits[fd / FDSetImpl.NFBBITS] &= ~(1 << (fd % FDSetImpl.NFBBITS));
	}

	public boolean FD_ISSET(int fd, FDSet set) {
		if (set == null)
			return false;
		FDSetImpl p = (FDSetImpl) set;
		return (p.bits[fd / FDSetImpl.NFBBITS] & (1 << (fd % FDSetImpl.NFBBITS))) != 0;
	}

	public void FD_SET(int fd, FDSet set) {
		if (set == null)
			return;
		FDSetImpl p = (FDSetImpl) set;
		p.bits[fd / FDSetImpl.NFBBITS] |= 1 << (fd % FDSetImpl.NFBBITS);
	}

	public void FD_ZERO(FDSet set) {
		if (set == null)
			return;
		FDSetImpl p = (FDSetImpl) set;
		java.util.Arrays.fill(p.bits, 0);
	}

	public int select(int nfds, FDSet rfds, FDSet wfds, FDSet efds, TimeVal timeout) {
		Linux_C_lib.TimeVal tout = null;
		if (timeout != null)
			tout = new Linux_C_lib.TimeVal(timeout);

		int[] r = rfds != null ? ((FDSetImpl) rfds).bits : null;
		int[] w = wfds != null ? ((FDSetImpl) wfds).bits : null;
		int[] e = efds != null ? ((FDSetImpl) efds).bits : null;
		return m_Clib.select(nfds, r, w, e, tout);
	}

	public int poll(Pollfd fds[], int nfds, int timeout) {
		pollfd[] pfds = new pollfd[fds.length];
		for (int i = 0; i < nfds; i++)
			pfds[i] = new pollfd(fds[i]);
		return m_Clib.poll(pfds, nfds, timeout);
	}

	public FDSet newFDSet() {
		return new FDSetImpl();
	}

	public int ioctl(int fd, int cmd, int[] data) {
		return m_Clib.ioctl(fd, cmd, data);
	}

	public List<String> getPortList() {
		File dir = new File(DEVICE_DIR_PATH);
		if (!dir.isDirectory()) {
			log = log && log(1, "device directory %s does not exist\n", DEVICE_DIR_PATH);
			return null;
		}
		String[] devs = dir.list();
		LinkedList<String> list = new LinkedList<String>();
		if (devs != null) {
			for (int i = 0; i < devs.length; i++) {
				String s = devs[i];
				if (s.startsWith("tty"))
					list.add(DEVICE_DIR_PATH + s);
			}

		}

		return list;
	}

	public void shutDown() {

	}
}

