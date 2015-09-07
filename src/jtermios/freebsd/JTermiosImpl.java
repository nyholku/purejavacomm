/*
 * Copyright (c) 2012 Kustaa Nyholm / SpareTimeLabs
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
 * 
 * This FreeBSD backend contributed by Denver Hull 
 * 
 * Many thanks for his persistence and efforts to make it happen!
 * 
 */

package jtermios.freebsd;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;
import java.io.File;

import java.util.*;

import jtermios.Pollfd;
import jtermios.Termios;
import jtermios.TimeVal;

import static jtermios.JTermios.*;
import static jtermios.JTermios.JTermiosLogging.log;

public class JTermiosImpl implements jtermios.JTermios.JTermiosInterface {

    private static String DEVICE_DIR_PATH = "/dev/";
    static C_lib_DirectMapping m_ClibDM;
    static C_lib m_Clib;
    static NonDirectCLib m_ClibND;

    static {
        m_ClibND = (NonDirectCLib) Native.loadLibrary(Platform.C_LIBRARY_NAME, NonDirectCLib.class);
        Native.register(C_lib_DirectMapping.class, NativeLibrary.getInstance(Platform.C_LIBRARY_NAME));
        m_ClibDM = new C_lib_DirectMapping();
        m_Clib = m_ClibDM;
    }

    public static class C_lib_DirectMapping implements C_lib {

        native public int pipe(int[] fds);

        native public int tcdrain(int fd);

        native public void cfmakeraw(termios termios);

        native public int fcntl(int fd, int cmd, int arg);

        native public int ioctl(int fd, int cmd, int[] arg);

        native public int open(String path, int flags);

        native public int close(int fd);

        native public int tcgetattr(int fd, termios termios);

        native public int tcsetattr(int fd, int cmd, termios termios);

        native public int cfsetispeed(termios termios, NativeLong i);

        native public int cfsetospeed(termios termios, NativeLong i);

        native public NativeLong cfgetispeed(termios termios);

        native public NativeLong cfgetospeed(termios termios);

        native public NativeSize write(int fd, byte[] buffer, NativeSize count);

        native public NativeSize read(int fd, byte[] buffer, NativeSize count);

        native public int tcflush(int fd, int qs);

        native public void perror(String msg);

        native public int tcsendbreak(int fd, int duration);

        native public int select(int n, fd_set read, fd_set write, fd_set error, timeval timeout);

    }

    public interface C_lib extends com.sun.jna.Library {

        public int pipe(int[] fds);

        public int tcdrain(int fd);

        public void cfmakeraw(termios termios);

        public int fcntl(int fd, int cmd, int arg);

        public int ioctl(int fd, int cmd, int[] arg);

        public int open(String path, int flags);

        public int close(int fd);

        public int tcgetattr(int fd, termios termios);

        public int tcsetattr(int fd, int cmd, termios termios);

        public int cfsetispeed(termios termios, NativeLong i);

        public int cfsetospeed(termios termios, NativeLong i);

        public NativeLong cfgetispeed(termios termios);

        public NativeLong cfgetospeed(termios termios);

        public NativeSize write(int fd, byte[] buffer, NativeSize count);

        public NativeSize read(int fd, byte[] buffer, NativeSize count);

        public int tcflush(int fd, int qs);

        public void perror(String msg);

        public int tcsendbreak(int fd, int duration);

        public int select(int n, fd_set read, fd_set write, fd_set error, timeval timeout);

    }

    public interface NonDirectCLib extends com.sun.jna.Library {

        public int poll(pollfd.ByReference pfds, int nfds, int timeout);
    }

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

        public static class ByReference extends pollfd implements Structure.ByReference {
        }
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

        public pollfd() {
        }

        public pollfd(Pollfd pfd) {
            fd = pfd.fd;
            events = pfd.events;
            revents = pfd.revents;
        }
    }

    static public class fd_set extends Structure implements FDSet {

		private final static int NFBBITS = 32;
		private final static int fd_count = 1024;
		public int[] fd_array = new int[(fd_count + NFBBITS - 1) / NFBBITS];

		@Override
		protected List getFieldOrder() {
			return Arrays.asList(//
					"fd_array"//
			);
		}

		public void FD_SET(int fd) {
			fd_array[fd / NFBBITS] |= (1 << (fd % NFBBITS));
		}

		public boolean FD_ISSET(int fd) {
			return (fd_array[fd / NFBBITS] & (1 << (fd % NFBBITS))) != 0;
		}

		public void FD_ZERO() {
			Arrays.fill(fd_array, 0);
		}

		public void FD_CLR(int fd) {
			fd_array[fd / NFBBITS] &= ~(1 << (fd % NFBBITS));
		}

    }

    static public class termios extends Structure {

        public int c_iflag;
        public int c_oflag;
        public int c_cflag;
        public int c_lflag;
        public byte[] c_cc = new byte[20];
        public int c_ispeed;
        public int c_ospeed;

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
            c_iflag = t.c_iflag;
            c_oflag = t.c_oflag;
            c_cflag = t.c_cflag;
            System.arraycopy(t.c_cc, 0, c_cc, 0, Math.min(t.c_cc.length, c_cc.length));
            c_ispeed = t.c_ispeed;
            c_ospeed = t.c_ospeed;
        }

        public void update(jtermios.Termios t) {
            t.c_iflag = c_iflag;
            t.c_oflag = c_oflag;
            t.c_cflag = c_cflag;
            System.arraycopy(c_cc, 0, t.c_cc, 0, Math.min(t.c_cc.length, c_cc.length));
            t.c_ispeed = c_ispeed;
            t.c_ospeed = c_ospeed;
        }
    }

    public JTermiosImpl() {
        log = log && log(1, "instantiating %s\n", getClass().getCanonicalName());

        // sys/filio.h stuff
        FIONREAD = 0x4004667F;
        // fcntl.h stuff
        O_RDWR = 0x00000002;
        O_NONBLOCK = 0x00000004;
        O_NOCTTY = 0x00008000;
        O_NDELAY = 0x00000004;
        F_GETFL = 0x00000003;
        F_SETFL = 0x00000004;
        // errno.h stuff
        EAGAIN = 35;
        EBADF = 9;
        EACCES = 22;
        EEXIST = 17;
        EINTR = 4;
        EINVAL = 22;
        EIO = 5;
        EISDIR = 21;
        ELOOP = 62;
        EMFILE = 24;
        ENAMETOOLONG = 63;
        ENFILE = 23;
        ENOENT = 2;
        ENOSPC = 28;
        ENOTDIR = 20;
        ENXIO = 6;
        EOVERFLOW = 84;
        EROFS = 30;
        ENOTSUP = 45;
        // termios.h stuff
        TIOCM_RNG = 0x00000080;
        TIOCM_CAR = 0x00000040;
        IGNBRK = 0x00000001;
        BRKINT = 0x00000002;
        PARMRK = 0x00000008;
        INLCR = 0x00000040;
        IGNCR = 0x00000080;
        ICRNL = 0x00000100;
        ECHONL = 0x00000010;
        IEXTEN = 0x00000400;
        CLOCAL = 0x00008000;
        OPOST = 0x00000001;
        VSTART = 0x0000000C;
        TCSANOW = 0x00000000;
        VSTOP = 0x0000000D;
        VMIN = 0x00000010;
        VTIME = 0x00000011;
        VEOF = 0x00000000;
        TIOCMGET = 0x4004746A;
        TIOCM_CTS = 0x00000020;
        TIOCM_DSR = 0x00000100;
        TIOCM_RI = 0x00000080;
        TIOCM_CD = 0x00000040;
        TIOCM_DTR = 0x00000002;
        TIOCM_RTS = 0x00000004;
        ICANON = 0x00000100;
        ECHO = 0x00000008;
        ECHOE = 0x00000002;
        ISIG = 0x00000080;
        TIOCMSET = 0x8004746D;
        IXON = 0x00000200;
        IXOFF = 0x00000400;
        IXANY = 0x00000800;
        CRTSCTS = 0x00030000;
        TCSADRAIN = 0x00000001;
        INPCK = 0x00000010;
        ISTRIP = 0x00000020;
        CSIZE = 0x00000300;
        TCIFLUSH = 0x00000001;
        TCOFLUSH = 0x00000002;
        TCIOFLUSH = 0x00000003;
        CS5 = 0x00000000;
        CS6 = 0x00000100;
        CS7 = 0x00000200;
        CS8 = 0x00000300;
        CSTOPB = 0x00000400;
        CREAD = 0x00000800;
        PARENB = 0x00001000;
        PARODD = 0x00002000;
        B0 = 0;
        B50 = 50;
        B75 = 75;
        B110 = 110;
        B134 = 134;
        B150 = 150;
        B200 = 200;
        B300 = 300;
        B600 = 600;
        B1200 = 600;
        B1800 = 1800;
        B2400 = 2400;
        B4800 = 4800;
        B9600 = 9600;
        B19200 = 19200;
        B38400 = 38400;
        B7200 = 7200;
        B14400 = 14400;
        B28800 = 28800;
        B57600 = 57600;
        B76800 = 76800;
        B115200 = 115200;
        B230400 = 230400;
        // poll.h stuff
        POLLIN = 0x0001;
        POLLPRI = 0x0002;
        POLLOUT = 0x0004;
        POLLERR = 0x0008;
        POLLNVAL = 0x0020;
        // select.h stuff
    }

    public int errno() {
        return Native.getLastError();
    }

    public void cfmakeraw(Termios termios) {
        termios t = new termios(termios);
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
        return m_Clib.cfgetispeed(new termios(termios)).intValue();
    }

    public int cfgetospeed(Termios termios) {
        return m_Clib.cfgetospeed(new termios(termios)).intValue();
    }

    public int cfsetispeed(Termios termios, int speed) {
        termios t = new termios(termios);
        int ret = m_Clib.cfsetispeed(t, new NativeLong(speed));
        t.update(termios);
        return ret;
    }

    public int cfsetospeed(Termios termios, int speed) {
        termios t = new termios(termios);
        int ret = m_Clib.cfsetospeed(t, new NativeLong(speed));
        t.update(termios);
        return ret;
    }

    public int open(String s, int t) {
        if (s != null && !s.startsWith("/")) {
            s = DEVICE_DIR_PATH + s;
        }
        return m_Clib.open(s, t);
    }

    public int read(int fd, byte[] buffer, int len) {
        return m_Clib.read(fd, buffer, new NativeSize(len)).intValue();
    }

    public int write(int fd, byte[] buffer, int len) {
        return m_Clib.write(fd, buffer, new NativeSize(len)).intValue();
    }

    public int close(int fd) {
        return m_Clib.close(fd);
    }

    public int tcflush(int fd, int b) {
        return m_Clib.tcflush(fd, b);
    }

    public int tcgetattr(int fd, Termios termios) {
        termios t = new termios();
        int ret = m_Clib.tcgetattr(fd, t);
        t.update(termios);
        return ret;
    }

    public void perror(String msg) {
        m_Clib.perror(msg);
    }

    public int tcsendbreak(int fd, int duration) {
        // If duration is not zero, it sends zero-valued bits for duration*N seconds,
        // where N is at least 0.25, and not more than 0.5.
        return m_Clib.tcsendbreak(fd, duration / 250);
    }

    public int tcsetattr(int fd, int cmd, Termios termios) {
        return m_Clib.tcsetattr(fd, cmd, new termios(termios));
    }

    public int select(int nfds, FDSet rfds, FDSet wfds, FDSet efds, TimeVal timeout) {
        timeval tout = null;
        if (timeout != null) {
            tout = new timeval(timeout);
        }

        return m_Clib.select(nfds, (fd_set) rfds, (fd_set) wfds, (fd_set) efds, tout);
    }

    public int poll(Pollfd fds[], int nfds, int timeout) {
        if (nfds <= 0 || nfds > fds.length) {
            throw new java.lang.IllegalArgumentException("nfds " + nfds + " must be <= fds.length " + fds.length);
        }
        pollfd.ByReference parampfds = new pollfd.ByReference();
        pollfd[] pfds = (pollfd[]) parampfds.toArray(nfds);
        for (int i = 0; i < nfds; i++) {
            pfds[i].fd = fds[i].fd;
            pfds[i].events = fds[i].events;
        }
        int ret = m_ClibND.poll(parampfds, nfds, timeout);
        for (int i = 0; i < nfds; i++) {
            fds[i].revents = pfds[i].revents;
        }
        return ret;
    }

    public boolean canPoll() {
        return true;
    }

    public FDSet newFDSet() {
        return new fd_set();
    }

    public int ioctl(int fd, int cmd, int... data) {
                // At this time, all ioctl commands we have defined are either no parameter or 4 byte parameter.
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
				if (s.startsWith("cua") || s.startsWith("tty"))
					list.add(s);
			}

		}

		return list;
	}

	public void shutDown() {

	}

	public String getPortNamePattern() {
		return "^(tty\\.|cu\\.).*";
	}

	public int setspeed(int fd, Termios termios, int speed) {
		int r;
		r = cfsetispeed(termios, speed);
		if (r == 0)
			r = cfsetospeed(termios, speed);
		if (r == 0)
			r = tcsetattr(fd, TCSANOW, termios);
		return r;
	}

	public int pipe(int[] fds) {
		return m_Clib.pipe(fds);
	}

}
