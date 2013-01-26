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

package jtermios.macosx;

import java.io.File;

import java.nio.Buffer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import jtermios.FDSet;

import jtermios.JTermios;
import jtermios.Pollfd;
import jtermios.Termios;
import jtermios.TimeVal;
import jtermios.macosx.JTermiosImpl.MacOSX_C_lib.pollfd;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;

import static jtermios.JTermios.*;
import static jtermios.JTermios.JTermiosLogging.log;

public class JTermiosImpl implements jtermios.JTermios.JTermiosInterface {
	private static int IOSSIOSPEED = 0x80045402;
	private static String DEVICE_DIR_PATH = "/dev/";
	static MacOSX_C_lib m_Clib = (MacOSX_C_lib) Native.loadLibrary("c", MacOSX_C_lib.class);

	public interface MacOSX_C_lib extends com.sun.jna.Library {
		public int pipe(int[] fds);

		public int tcdrain(int fd);

		public void cfmakeraw(termios termios);

		public int fcntl(int fd, int cmd, int arg);

		public int ioctl(int fd, int cmd, int[] arg);

		public int ioctl(int fd, int cmd, NativeLong[] arg);

		public int open(String path, int flags);

		public int close(int fd);

		public int tcgetattr(int fd, termios termios);

		public int tcsetattr(int fd, int cmd, termios termios);

		public int cfsetispeed(termios termios, NativeLong i);

		public int cfsetospeed(termios termios, NativeLong i);

		public NativeLong cfgetispeed(termios termios);

		public NativeLong cfgetospeed(termios termios);

		public NativeLong write(int fd, ByteBuffer buffer, NativeLong count);

		public NativeLong read(int fd, ByteBuffer buffer, NativeLong count);

		public int select(int n, int[] read, int[] write, int[] error, timeval timeout);

		public int poll(pollfd[] fds, int nfds, int timeout);
		
		public int poll(int[] fds, int nfds, int timeout);

		public int tcflush(int fd, int qs);

		public void perror(String msg);

		static public class timeval extends Structure {
			public NativeLong tv_sec;
			public NativeLong tv_usec;

			@Override
			protected List getFieldOrder() {
				return Arrays.asList(//
						"tv_sec",//
						"tv_usec"//
				);
			}

			public timeval(jtermios.TimeVal timeout) {
				tv_sec = new NativeLong(timeout.tv_sec);
				tv_usec = new NativeLong(timeout.tv_usec);
			}
		}

		static public class pollfd extends Structure {
			public int fd;
			public short events;
			public short revents;

			@Override
			protected List getFieldOrder() {
				return Arrays.asList(//
						"fd",//
						"events",//
						"revents"//
				);
			}

			public pollfd(Pollfd pfd) {
				fd = pfd.fd;
				events = pfd.events;
				revents = pfd.revents;
			}
		}

		static public class termios extends Structure {
			public NativeLong c_iflag = new NativeLong();
			public NativeLong c_oflag = new NativeLong();
			public NativeLong c_cflag = new NativeLong();
			public NativeLong c_lflag = new NativeLong();
			public byte[] c_cc = new byte[20];
			public NativeLong c_ispeed = new NativeLong();
			public NativeLong c_ospeed = new NativeLong();

			@Override
			protected List getFieldOrder() {
				return Arrays.asList(//
						"c_iflag",//
						"c_oflag",//
						"c_cflag",//
						"c_lflag",//
						"c_cc",//
						"c_ispeed",//
						"c_ospeed"//
				);
			}

			public termios() {
			}

			public termios(jtermios.Termios t) {
				c_iflag.setValue(t.c_iflag);
				c_oflag.setValue(t.c_oflag);
				c_cflag.setValue(t.c_cflag);
				c_lflag.setValue(t.c_lflag);
				System.arraycopy(t.c_cc, 0, c_cc, 0, t.c_cc.length);
				c_ispeed.setValue(t.c_ispeed);
				c_ospeed.setValue(t.c_ospeed);
			}

			public void update(jtermios.Termios t) {
				t.c_iflag = c_iflag.intValue();
				t.c_oflag = c_oflag.intValue();
				t.c_cflag = c_cflag.intValue();
				t.c_lflag = c_lflag.intValue();
				System.arraycopy(c_cc, 0, t.c_cc, 0, t.c_cc.length);
				t.c_ispeed = c_ispeed.intValue();
				t.c_ospeed = c_ospeed.intValue();
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
	}

	public int errno() {
		return Native.getLastError();
	}

	public void cfmakeraw(Termios termios) {
		MacOSX_C_lib.termios t = new MacOSX_C_lib.termios(termios);
		m_Clib.cfmakeraw(t);
		t.update(termios);
	}

	public int fcntl(int fd, int cmd, int arg) {
		return m_Clib.fcntl(fd, cmd, arg);
	}

	public int tcdrain(int fd) {
		return m_Clib.tcdrain(fd);
	}

	public int cfgetispeed(Termios termios) {
		return m_Clib.cfgetispeed(new MacOSX_C_lib.termios(termios)).intValue();
	}

	public int cfgetospeed(Termios termios) {
		return m_Clib.cfgetospeed(new MacOSX_C_lib.termios(termios)).intValue();
	}

	public int cfsetispeed(Termios termios, int speed) {
		MacOSX_C_lib.termios t = new MacOSX_C_lib.termios(termios);
		int ret = m_Clib.cfsetispeed(t, new NativeLong(speed));
		t.update(termios);
		return ret;
	}

	public int cfsetospeed(Termios termios, int speed) {
		MacOSX_C_lib.termios t = new MacOSX_C_lib.termios(termios);
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
		MacOSX_C_lib.termios t = new MacOSX_C_lib.termios();
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
		return m_Clib.tcsetattr(fd, cmd, new MacOSX_C_lib.termios(termios));
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
		MacOSX_C_lib.timeval tout = null;
		if (timeout != null)
			tout = new MacOSX_C_lib.timeval(timeout);

		int[] r = rfds != null ? ((FDSetImpl) rfds).bits : null;
		int[] w = wfds != null ? ((FDSetImpl) wfds).bits : null;
		int[] e = efds != null ? ((FDSetImpl) efds).bits : null;
		return m_Clib.select(nfds, r, w, e, tout);
	}

	public int poll(Pollfd fds[], int nfds, int timeout) {
		pollfd[] pfds = new pollfd[fds.length];
		for (int i = 0; i < nfds; i++)
			pfds[i] = new pollfd(fds[i]);
        int ret = m_Clib.poll(pfds, nfds, timeout);
        for(int i = 0; i < nfds; i++)
            fds[i].revents = pfds[i].revents;
		return ret;
	}
	
	public int poll(int fds[], int nfds, int timeout) {
        return m_Clib.poll(fds, nfds, timeout);
	}


	public FDSet newFDSet() {
		return new FDSetImpl();
	}

	public int ioctl(int fd, int cmd, int[] data) {
		return m_Clib.ioctl(fd, cmd, data);
	}

	public int ioctl(int fd, int cmd, NativeLong[] data) {
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

		Pattern p = JTermios.getPortNamePattern(this);
		if (devs != null) {
			for (int i = 0; i < devs.length; i++) {
				String s = devs[i];
				if (p.matcher(s).matches())
					list.add(s);
			}
		}
		return list;
	}

	public String getPortNamePattern() {
		return "^(tty\\.|cu\\.).*";
	}

	public void shutDown() {

	}

	public int setspeed(int fd, Termios termios, int speed) {
		int r;
		r = cfsetispeed(termios, speed);
		if (r == 0)
			r = cfsetospeed(termios, speed);
		if (r == 0)
			r = tcsetattr(fd, TCSANOW, termios);
		if (r != 0) {
			// Darell Tan had patched RXTX with this sequence, so lets try this
			if (cfsetispeed(termios, B9600) == 0 && cfsetospeed(termios, B9600) == 0 && tcsetattr(fd, TCSANOW, termios) == 0) {
				NativeLong[] data = new NativeLong[] { new NativeLong(speed) };
				r = ioctl(fd, IOSSIOSPEED, data);
			}
		}
		return r;
	}

	public int pipe(int[] fds) {
		return m_Clib.pipe(fds);
	}
}
