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

package jtermios.windows;

import java.util.*;
import java.util.regex.Pattern;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;

import static jtermios.JTermios.*;
import static jtermios.JTermios.JTermiosLogging.*;
import jtermios.*;
import jtermios.windows.WinAPI.*;
import static jtermios.windows.WinAPI.*;
import static jtermios.windows.WinAPI.DCB.*;

public class JTermiosImpl implements jtermios.JTermios.JTermiosInterface {
	private volatile int m_ErrNo = 0;

	private volatile boolean m_PortFDs[] = new boolean[FDSetImpl.FD_SET_SIZE];

	private volatile Hashtable<Integer, Port> m_OpenPorts = new Hashtable<Integer, Port>();

	private class Port {
		volatile int m_FD = -1;
		volatile boolean m_Locked;
		volatile HANDLE m_Comm;
		volatile int m_OpenFlags;
		volatile DCB m_DCB = new DCB();
		volatile COMMTIMEOUTS m_Timeouts = new COMMTIMEOUTS();
		volatile COMSTAT m_COMSTAT = new COMSTAT();
		volatile int[] m_ClearErr = { 0 };
		volatile Memory m_RdBuffer = new Memory(2048);
		volatile int[] m_RdErr = { 0 };
		volatile int m_RdN[] = { 0 };
		volatile OVERLAPPED m_RdOVL = new OVERLAPPED();
		volatile HANDLE m_ReadCancelObject;
		volatile HANDLE[] m_ReadWaitObjects = new HANDLE[2];
		volatile Memory m_WrBuffer = new Memory(2048);
		volatile COMSTAT m_WrStat = new COMSTAT();
		volatile int[] m_WrErr = { 0 };
		volatile int m_WrN[] = { 0 };
		volatile int m_WritePending;
		volatile OVERLAPPED m_WrOVL = new OVERLAPPED();
		volatile HANDLE m_WriteCancelObject;
		volatile HANDLE[] m_WriteWaitObjects = new HANDLE[2];
		volatile boolean m_WaitPending;
		volatile int m_SelN[] = { 0 };
		volatile HANDLE WaitCommEventCancelObject;
		volatile OVERLAPPED m_SelOVL = new OVERLAPPED();
		volatile IntByReference m_EventFlags = new IntByReference();
		volatile Termios m_Termios = new Termios();
		volatile int MSR; // initial value
		// these cached values are used to detect changes in termios structure and speed up things by avoiding unnecessary updates
		volatile int m_VTIME = -1;
		volatile int m_VMIN = -1;
		volatile int m_c_speed = -1;
		volatile int m_c_cflag = -1;
		volatile int m_c_iflag = -1;
		volatile int m_c_oflag = -1;

		synchronized public void fail() throws Fail {
			int err = GetLastError();
			Memory buffer = new Memory(2048);
			FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS, null, err, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), buffer, (int) buffer.size(), null);

			log = log && log(1, "fail() %s, Windows GetLastError()= %d, %s\n", lineno(1), err, buffer.getWideString(0));

			// FIXME here convert from Windows error code to 'posix' error code

			Fail f = new Fail();
			throw f;
		}

		synchronized public void lock() throws InterruptedException {
			while (m_Locked)
				wait();
			m_Locked = true;
		}

		synchronized public void unlock() {
			if (!m_Locked)
				throw new IllegalArgumentException("Port was not locked");
			m_Locked = false;
			notifyAll();
		}

		synchronized public void waitUnlock() {
			while (m_Locked) {
				try {
					wait();
				} catch (InterruptedException e) {
					// interruption cannot cancel wait
				}
			}
		}

		public void open(String filename, int flags) throws Fail {
			synchronized (JTermiosImpl.this) {
				m_FD = -1;
				for (int i = 0; i < m_PortFDs.length; ++i) {
					if (!m_PortFDs[i]) {
						m_FD = i;
						m_PortFDs[i] = true;
						m_OpenPorts.put(m_FD, this);
						m_OpenFlags = flags;

						if (!filename.startsWith("\\\\"))
							filename = "\\\\.\\" + filename;

						m_Comm = CreateFile(filename, GENERIC_READ | GENERIC_WRITE, 0, null, OPEN_EXISTING, FILE_FLAG_OVERLAPPED, null);

						if (INVALID_HANDLE_VALUE == m_Comm) {
							if (GetLastError() == ERROR_FILE_NOT_FOUND)
								m_ErrNo = ENOENT;
							else
								m_ErrNo = EBUSY;
							fail();
						}

						if (!SetupComm(m_Comm, (int) m_RdBuffer.size(), (int) m_WrBuffer.size()))
							fail(); // FIXME what would be appropriate error code here

						cfmakeraw(m_Termios);
						cfsetispeed(m_Termios, B9600);
						cfsetospeed(m_Termios, B9600);
						m_Termios.c_cc[VTIME] = 0;
						m_Termios.c_cc[VMIN] = 0;
						updateFromTermios(this);

						WaitCommEventCancelObject = CreateEvent(null, false, false, null);
						if (WaitCommEventCancelObject == INVALID_HANDLE_VALUE)
							fail();

						m_ReadCancelObject = CreateEvent(null, true, false, null);
						if (m_ReadCancelObject == INVALID_HANDLE_VALUE)
							fail();
						m_ReadWaitObjects[1] = m_ReadCancelObject;

						m_WriteCancelObject = CreateEvent(null, true, false, null);
						if (m_WriteCancelObject == INVALID_HANDLE_VALUE)
							fail();
						m_WriteWaitObjects[1] = m_WriteCancelObject;

						m_RdOVL.writeField("hEvent", CreateEvent(null, true, false, null));
						if (m_RdOVL.hEvent == INVALID_HANDLE_VALUE)
							fail();

						m_WrOVL.writeField("hEvent", CreateEvent(null, true, false, null));
						if (m_WrOVL.hEvent == INVALID_HANDLE_VALUE)
							fail();

						m_SelOVL.writeField("hEvent", CreateEvent(null, true, false, null));
						if (m_SelOVL.hEvent == INVALID_HANDLE_VALUE)
							fail();
						return;

					}
				}
				throw new RuntimeException("Too many ports open");
			}
		}

		public void close() {
			synchronized (JTermiosImpl.this) {
				if (m_FD >= 0) {
					m_OpenPorts.remove(m_FD);
					m_PortFDs[m_FD] = false;
					m_FD = -1;
				}

				HANDLE h; // / 'hEvent' might never have been 'read' so read it to this var first

				if (m_ReadCancelObject != null)
					SetEvent(m_ReadCancelObject);
				synchronized (m_RdBuffer) {
					h = (HANDLE) m_RdOVL.readField("hEvent");
					m_RdOVL = null;
					if (h != null && !h.equals(NULL) && !h.equals(INVALID_HANDLE_VALUE))
						CloseHandle(h);

					if (m_ReadCancelObject != null && m_ReadCancelObject != NULL && m_ReadCancelObject != INVALID_HANDLE_VALUE)
						CloseHandle(m_ReadCancelObject);
					m_ReadCancelObject = null;
				}

				if (m_WriteCancelObject != null)
					SetEvent(m_WriteCancelObject);
				synchronized (m_WrBuffer) {
					h = (HANDLE) m_WrOVL.readField("hEvent");
					m_WrOVL = null;

					if (h != null && !h.equals(NULL) && !h.equals(INVALID_HANDLE_VALUE))
						CloseHandle(h);

					if (m_WriteCancelObject != null && m_WriteCancelObject != NULL && m_WriteCancelObject != INVALID_HANDLE_VALUE)
						CloseHandle(m_WriteCancelObject);
					m_ReadCancelObject = null;
				}

				if (WaitCommEventCancelObject != null)
					SetEvent(WaitCommEventCancelObject);
				waitUnlock();
				if (WaitCommEventCancelObject != null && WaitCommEventCancelObject != NULL && WaitCommEventCancelObject != INVALID_HANDLE_VALUE)
					CloseHandle(WaitCommEventCancelObject);
				WaitCommEventCancelObject = null;

				h = (HANDLE) m_SelOVL.readField("hEvent");
				m_SelOVL = null;
				if (h != null && !h.equals(NULL) && !h.equals(INVALID_HANDLE_VALUE))
					CloseHandle(h);

				if (m_Comm != null && m_Comm != NULL && m_Comm != INVALID_HANDLE_VALUE)
					CloseHandle(m_Comm);
				m_Comm = null;
			}
		}

	};

	static class Fail extends Exception {

	}

	//static private class FDSetImpl extends FDSet {
	//	static final int FD_SET_SIZE = 256; // Windows supports max 255 serial ports so this is enough
	//	static final int NFBBITS = 32;
	//	int[] bits = new int[(FD_SET_SIZE + NFBBITS - 1) / NFBBITS];
	//}

	static private class FDSetImpl implements FDSet {
		static final int FD_SET_SIZE = 256; // Windows supports max 255 serial ports so this is enough
		static final int NFBBITS = 32;
		int[] bits = new int[(FD_SET_SIZE + NFBBITS - 1) / NFBBITS];

		public void FD_CLR(int fd) {
			bits[fd / NFBBITS] &= ~(1 << (fd % NFBBITS));
		}

		public boolean FD_ISSET(int fd) {
			return (bits[fd / NFBBITS] & (1 << (fd % NFBBITS))) != 0;
		}

		public void FD_SET(int fd) {
			bits[fd / NFBBITS] |= 1 << (fd % NFBBITS);
		}

		public void FD_ZERO() {
			java.util.Arrays.fill(bits, 0);
		}
	}

	public JTermiosImpl() {
		log = log && log(1, "instantiating %s\n", getClass().getCanonicalName());
	}

	public int errno() {
		return m_ErrNo;
	}

	public void cfmakeraw(Termios termios) {
		termios.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
		termios.c_oflag &= ~OPOST;
		termios.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
		termios.c_cflag &= ~(CSIZE | PARENB);
		termios.c_cflag |= CS8;
	}

	public int fcntl(int fd, int cmd, int arg) {

		Port port = getPort(fd);
		if (port == null)
			return -1;
		if (F_SETFL == cmd)
			port.m_OpenFlags = arg;
		else if (F_GETFL == cmd)
			return port.m_OpenFlags;
		else {
			m_ErrNo = ENOTSUP;
			return -1;
		}
		return 0;
	}

	public int tcdrain(int fd) {
		Port port = getPort(fd);
		if (port == null)
			return -1;
		try {
			synchronized (port.m_WrBuffer) {
				if (!FlushFileBuffers(port.m_Comm))
					port.fail();
				return 0;
			}
		} catch (Fail f) {
			return -1;
		}
	}

	public int cfgetispeed(Termios termios) {
		return termios.c_ispeed;
	}

	public int cfgetospeed(Termios termios) {
		return termios.c_ospeed;
	}

	public int cfsetispeed(Termios termios, int speed) {
		termios.c_ispeed = speed;
		return 0;
	}// Error code for Interrupted = EINTR

	public int cfsetospeed(Termios termios, int speed) {
		termios.c_ospeed = speed;
		return 0;
	}

	public int open(String filename, int flags) {
		Port port = new Port();
		try {
			port.open(filename, flags);
			return port.m_FD;
		} catch (Fail f) {
			port.close();
			return -1;
		}

	}

	private static void nanoSleep(long nsec) throws Fail {
		try {
			Thread.sleep((int) (nsec / 1000000), (int) (nsec % 1000000));
		} catch (InterruptedException ie) {
			throw new Fail();
		}
	}

	private int getCharBits(Termios tios) {
		int cs = 8; // default to 8
		if ((tios.c_cflag & CSIZE) == CS5)
			cs = 5;
		if ((tios.c_cflag & CSIZE) == CS6)
			cs = 6;
		if ((tios.c_cflag & CSIZE) == CS7)
			cs = 7;
		if ((tios.c_cflag & CSIZE) == CS8)
			cs = 8;
		if ((tios.c_cflag & CSTOPB) != 0)
			cs++; // extra stop bit
		if ((tios.c_cflag & PARENB) != 0)
			cs++; // parity adds an other bit
		cs += 1 + 1; // start bit + stop bit
		return cs;
	}

	private static int min(int a, int b) {
		return a < b ? a : b;
	}

	private static int max(int a, int b) {
		return a > b ? a : b;
	}

	public int read(int fd, byte[] buffer, int length) {

		Port port = getPort(fd);
		if (port == null)
			return -1;
		synchronized (port.m_RdBuffer) {
			try {
				// limit reads to internal buffer size
				if (length > port.m_RdBuffer.size())
					length = (int) port.m_RdBuffer.size();

				if (length == 0)
					return 0;

				int error;

				if ((port.m_OpenFlags & O_NONBLOCK) != 0) {
					clearCommErrors(port);
					int available = port.m_COMSTAT.cbInQue;
					if (available == 0) {
						m_ErrNo = EAGAIN;
						return -1;
					}
					length = min(length, available);
				} else {
					clearCommErrors(port);
					int available = port.m_COMSTAT.cbInQue;
					int vtime = 0xff & port.m_Termios.c_cc[VTIME];
					int vmin = 0xff & port.m_Termios.c_cc[VMIN];

					if (vmin == 0 && vtime == 0) {
						// VMIN = 0 and VTIME = 0 => totally non blocking,if data is
						// available, return it, ie this is poll operation
						// For reference below commented out is how timeouts are set for this vtime/vmin combo
						//touts.ReadIntervalTimeout = MAXDWORD;
						//touts.ReadTotalTimeoutConstant = 0;
						//touts.ReadTotalTimeoutMultiplier = 0;
						if (available == 0)
							return 0;
						length = min(length, available);
					}
					if (vmin == 0 && vtime > 0) {
						// VMIN = 0 and VTIME > 0 => timed read, return as soon as data is
						// available, VTIME = total time
						// For reference below commented out is how timeouts are set for this vtime/vmin combo
						//touts.ReadIntervalTimeout = 0;
						//touts.ReadTotalTimeoutConstant = vtime;
						//touts.ReadTotalTimeoutMultiplier = 0;

						// NOTE to behave like unix we should probably wait until there is something available
						// and then try to do a read as many bytes as are available bytes at that point in time. 
						// As this is coded now, this will attempt to read as many bytes as requested and this may end up
						// spending vtime in the read when a unix would return less bytes but as soon as they become
						// available. 
					}
					if (vmin > 0 && vtime > 0) {
						// VMIN > 0 and VTIME > 0 => blocks until VMIN chars has arrived or  between chars expired, 
						// note that this will block if nothing arrives
						// For reference below commented out is how timeouts are set for this vtime/vmin combo
						//touts.ReadIntervalTimeout = vtime;
						//touts.ReadTotalTimeoutConstant = 0;
						//touts.ReadTotalTimeoutMultiplier = 0;
						length = min(max(vmin, available), length);
					}
					if (vmin > 0 && vtime == 0) {
						// VMIN > 0 and VTIME = 0 => blocks until VMIN characters have been
						// received
						// For reference below commented out is how timeouts are set for this vtime/vmin combo
						//touts.ReadIntervalTimeout = 0;
						//touts.ReadTotalTimeoutConstant = 0;
						//touts.ReadTotalTimeoutMultiplier = 0;
						length = min(max(vmin, available), length);
					}

				}

				if (!ResetEvent(port.m_RdOVL.hEvent))
					port.fail();

				if (!ReadFile(port.m_Comm, port.m_RdBuffer, length, port.m_RdN, port.m_RdOVL)) {
					if (GetLastError() != ERROR_IO_PENDING)
						port.fail();
					port.m_ReadWaitObjects[0] = port.m_RdOVL.hEvent;
					if (WaitForMultipleObjects(2, port.m_ReadWaitObjects, false, INFINITE) != WAIT_OBJECT_0)
						port.fail();
					if (!GetOverlappedResult(port.m_Comm, port.m_RdOVL, port.m_RdN, true))
						port.fail();
				}

				port.m_RdBuffer.read(0, buffer, 0, port.m_RdN[0]);
				return port.m_RdN[0];
			} catch (Fail ie) {
				return -1;
			}
		}
	}

	public int write(int fd, byte[] buffer, int length) {
		Port port = getPort(fd);
		if (port == null)
			return -1;

		synchronized (port.m_WrBuffer) {
			try {
				if (port.m_WritePending > 0) {
					while (true) {
						port.m_WriteWaitObjects[0] = port.m_WrOVL.hEvent;

						int res = WaitForMultipleObjects(2, port.m_WriteWaitObjects, false, INFINITE);
						if (res == WAIT_TIMEOUT) { // Hmmm, can this ever happen, why we have that here
							clearCommErrors(port);
							log = log && log(1, "write pending, cbInQue %d cbOutQue %d\n", port.m_COMSTAT.cbInQue, port.m_COMSTAT.cbOutQue);
							continue;
						}
						if (!GetOverlappedResult(port.m_Comm, port.m_WrOVL, port.m_WrN, false))
							port.fail();
						if (port.m_WrN[0] != port.m_WritePending) // I exptect this is never going to happen, if it does
							new RuntimeException("Windows OVERLAPPED WriteFile failed to write all, tried to write " + port.m_WritePending + " but got " + port.m_WrN[0]);
						break;
					}
					port.m_WritePending = 0;
				}
				if ((port.m_OpenFlags & O_NONBLOCK) != 0) {
					if (!ClearCommError(port.m_Comm, port.m_WrErr, port.m_WrStat))
						port.fail();
					int room = (int) port.m_WrBuffer.size() - port.m_WrStat.cbOutQue;
					if (length > room)
						length = room;
				}

				int old_flag;

				if (!ResetEvent(port.m_WrOVL.hEvent))
					port.fail();

				if (length > port.m_WrBuffer.size())
					length = (int) port.m_WrBuffer.size();
				port.m_WrBuffer.write(0, buffer, 0, length); // copy from buffer to Memory
				boolean ok = WriteFile(port.m_Comm, port.m_WrBuffer, length, port.m_WrN, port.m_WrOVL);

				if (!ok) {
					if (GetLastError() != ERROR_IO_PENDING)
						port.fail();
					port.m_WritePending = length;
				}
				// 
				return length; // port.m_WrN[0];
			} catch (Fail f) {
				return -1;
			}
		}
	}

	public int close(int fd) {
		Port port = getPort(fd);
		if (port == null)
			return -1;
		port.close();
		return 0;
	}

	public int tcflush(int fd, int queue) {
		Port port = getPort(fd);
		if (port == null)
			return -1;
		try {
			if (queue == TCIFLUSH) {
				if (!PurgeComm(port.m_Comm, PURGE_RXABORT))
					port.fail();
			} else if (queue == TCOFLUSH) {
				if (!PurgeComm(port.m_Comm, PURGE_TXABORT))
					port.fail();
			} else if (queue == TCIOFLUSH) {
				if (!PurgeComm(port.m_Comm, PURGE_TXABORT))
					port.fail();
				if (!PurgeComm(port.m_Comm, PURGE_RXABORT))
					port.fail();
			} else {
				m_ErrNo = ENOTSUP;
				return -1;
			}

			return 0;
		} catch (Fail f) {
			return -1;
		}
	}

	/*
	 * (non-Javadoc) Basically this is wrong, as tcsetattr is supposed to set
	 * only those things it can support and tcgetattr is the used to see that
	 * what actually happened. In this instance tcsetattr never fails and
	 * tcgetattr always returns the last settings even though it possible (even
	 * likely) that tcsetattr was not able to carry out all settings, as there
	 * is no 1:1 mapping between Windows Comm API and posix/termios API.
	 * 
	 * @see jtermios.JTermios.JTermiosInterface#tcgetattr(int, jtermios.Termios)
	 */

	public int tcgetattr(int fd, Termios termios) {
		Port port = getPort(fd);
		if (port == null)
			return -1;
		termios.set(port.m_Termios);
		return 0;
	}

	public int tcsendbreak(int fd, int duration) {
		Port port = getPort(fd);
		if (port == null)
			return -1;
		try {
			if (!SetCommBreak(port.m_Comm))
				port.fail();
			nanoSleep(duration * 250000000L);
			if (!ClearCommBreak(port.m_Comm))
				port.fail();
			return 0;
		} catch (Fail f) {
			return -1;
		}
	}

	public int tcsetattr(int fd, int cmd, Termios termios) {
		if (cmd != TCSANOW)
			log(0, "tcsetattr only supports TCSANOW\n");

		Port port = getPort(fd);
		if (port == null)
			return -1;
		synchronized (port.m_Termios) {
			try {
				port.m_Termios.set(termios);
				updateFromTermios(port);
				return 0;
			} catch (Fail f) {
				return -1;
			}
		}
	}

	// FIXME this needs serious code review from people who know this stuff...
	public int updateFromTermios(Port port) throws Fail {
		Termios tios = port.m_Termios;
		int c_speed = tios.c_ospeed;
		int c_cflag = tios.c_cflag;
		int c_iflag = tios.c_iflag;
		int c_oflag = tios.c_oflag;

		if (c_speed != port.m_c_speed || c_cflag != port.m_c_cflag || c_iflag != port.m_c_iflag || c_oflag != port.m_c_oflag) {
			DCB dcb = port.m_DCB;
			if (!GetCommState(port.m_Comm, dcb))
				port.fail();

			dcb.DCBlength = dcb.size();
			dcb.BaudRate = c_speed;
			if (tios.c_ospeed != tios.c_ispeed)
				log(0, "c_ospeed (%d) != c_ispeed (%d)\n", tios.c_ospeed, tios.c_ispeed);
			int flags = 0;
			// rxtx does: if ( s_termios->c_iflag & ISTRIP ) dcb.fBinary = FALSE;
			// but Winapi doc says fBinary always true
			flags |= fBinary;
			if ((c_cflag & PARENB) != 0)
				flags |= fParity;

			if ((c_iflag & IXON) != 0)
				flags |= fOutX;
			if ((c_iflag & IXOFF) != 0)
				flags |= fInX;
			if ((c_iflag & IXANY) != 0)
				flags |= fTXContinueOnXoff;

			if ((c_iflag & CRTSCTS) != 0) {
				flags |= fRtsControl;
				flags |= fOutxCtsFlow;
			}

			// Following have no corresponding functionality in unix termios
			// fOutxDsrFlow = 0x00000008;
			// fDtrControl = 0x00000030;
			// fDsrSensitivity = 0x00000040;
			// fErrorChar = 0x00000400;
			// fNull = 0x00000800;
			// fAbortOnError = 0x00004000;
			// fDummy2 = 0xFFFF8000;
			dcb.fFlags = flags;
			// Don't touch these, windows seems to use: XonLim 2048 XoffLim 512 and who am I to argue with those
			//dcb.XonLim = 0;
			//dcb.XoffLim = 128;
			byte cs = 8;
			int csize = c_cflag & CSIZE;
			if (csize == CS5)
				cs = 5;
			if (csize == CS6)
				cs = 6;
			if (csize == CS7)
				cs = 7;
			if (csize == CS8)
				cs = 8;
			dcb.ByteSize = cs;

			if ((c_cflag & PARENB) != 0) {
				if ((c_cflag & PARODD) != 0 && (c_cflag & CMSPAR) != 0)
					dcb.Parity = MARKPARITY;
				else if ((c_cflag & PARODD) != 0)
					dcb.Parity = ODDPARITY;
				else if ((c_cflag & CMSPAR) != 0)
					dcb.Parity = SPACEPARITY;
				else
					dcb.Parity = EVENPARITY;
			} else
				dcb.Parity = NOPARITY;

			dcb.StopBits = (c_cflag & CSTOPB) != 0 ? ((csize == CS5) ? ONE5STOPBITS : TWOSTOPBITS) : ONESTOPBIT;
			dcb.XonChar = tios.c_cc[VSTART]; // In theory these could change but they only get updated if the baudrate/char size changes so this could be a time bomb 
			dcb.XoffChar = tios.c_cc[VSTOP]; // In practice in PJC these are never changed so updating on the first pass is enough
			dcb.ErrorChar = 0;

			// rxtx has some thing like
			// if ( EV_BREAK|EV_CTS|EV_DSR|EV_ERR|EV_RING | ( EV_RLSD & EV_RXFLAG )
			// )
			// dcb.EvtChar = '\n';
			// else
			// dcb.EvtChar = '\0';
			// But those are all defines so there is something fishy there?

			dcb.EvtChar = '\n';
			dcb.EofChar = tios.c_cc[VEOF];

			if (!SetCommState(port.m_Comm, dcb))
				port.fail();

			port.m_c_speed = c_speed;
			port.m_c_cflag = c_cflag;
			port.m_c_iflag = c_iflag;
			port.m_c_oflag = c_oflag;
		}

		int vmin = port.m_Termios.c_cc[VMIN] & 0xFF;
		int vtime = (port.m_Termios.c_cc[VTIME] & 0xFF) * 100;
		if (vmin != port.m_VMIN || vtime != port.m_VTIME) {
			COMMTIMEOUTS touts = port.m_Timeouts;
			// There are really no write timeouts in classic unix termios
			// FIXME test that we can still interrupt the tread
			touts.WriteTotalTimeoutConstant = 0;
			touts.WriteTotalTimeoutMultiplier = 0;
			if (vmin == 0 && vtime == 0) {
				// VMIN = 0 and VTIME = 0 => totally non blocking,if data is
				// available, return it, ie this is poll operation
				touts.ReadIntervalTimeout = MAXDWORD;
				touts.ReadTotalTimeoutConstant = 0;
				touts.ReadTotalTimeoutMultiplier = 0;
			}
			if (vmin == 0 && vtime > 0) {
				// VMIN = 0 and VTIME > 0 => timed read, return as soon as data is
				// available, VTIME = total time
				touts.ReadIntervalTimeout = 0;
				touts.ReadTotalTimeoutConstant = vtime;
				touts.ReadTotalTimeoutMultiplier = 0;
			}
			if (vmin > 0 && vtime > 0) {
				// VMIN > 0 and VTIME > 0 => blocks until VMIN chars has arrived or  between chars expired, 
				// note that this will block if nothing arrives
				touts.ReadIntervalTimeout = vtime;
				touts.ReadTotalTimeoutConstant = 0;
				touts.ReadTotalTimeoutMultiplier = 0;
			}
			if (vmin > 0 && vtime == 0) {
				// VMIN > 0 and VTIME = 0 => blocks until VMIN characters have been
				// received
				touts.ReadIntervalTimeout = 0;
				touts.ReadTotalTimeoutConstant = 0;
				touts.ReadTotalTimeoutMultiplier = 0;
			}
			if (!SetCommTimeouts(port.m_Comm, port.m_Timeouts))
				port.fail();
			port.m_VMIN = vmin;
			port.m_VTIME = vtime;
			log = log && log(2, "vmin %d vtime %d ReadIntervalTimeout %d ReadTotalTimeoutConstant %d ReadTotalTimeoutMultiplier %d\n", vmin, vtime, touts.ReadIntervalTimeout, touts.ReadTotalTimeoutConstant, touts.ReadTotalTimeoutMultiplier);
		}

		return 0;
	}

	private int maskToFDSets(Port port, FDSet readfds, FDSet writefds, FDSet exceptfds, int ready) throws Fail {
		clearCommErrors(port);
		int emask = port.m_EventFlags.getValue();
		int fd = port.m_FD;
		if ((emask & EV_RXCHAR) != 0 && port.m_COMSTAT.cbInQue > 0) {
			FD_SET(fd, readfds);
			ready++;
		}
		if ((emask & EV_TXEMPTY) != 0 && port.m_COMSTAT.cbOutQue == 0) {
			FD_SET(fd, writefds);
			ready++;
		}
		return ready;
	}

	private void clearCommErrors(Port port) throws Fail {
		synchronized (port.m_COMSTAT) {
			if (!ClearCommError(port.m_Comm, port.m_ClearErr, port.m_COMSTAT))
				port.fail();
		}
	}

	public int select(int n, FDSet readfds, FDSet writefds, FDSet exceptfds, TimeVal timeout) {
		// long T0 = System.currentTimeMillis();
		int ready = 0;
		LinkedList<Port> locked = new LinkedList<Port>();
		try {
			try {
				LinkedList<Port> waiting = new LinkedList<Port>();
				for (int fd = 0; fd < n; fd++) {
					boolean rd = FD_ISSET(fd, readfds);
					boolean wr = FD_ISSET(fd, writefds);
					FD_CLR(fd, readfds);
					FD_CLR(fd, writefds);
					if (rd || wr) {
						Port port = getPort(fd);
						if (port == null)
							return -1;
						try {
							port.lock();
							locked.add(port);
							clearCommErrors(port);

							// check if there is data to be read, as WaitCommEvent
							// does check for only *new* data that and thus
							// might wait indefinitely if select() is called twice
							// without first reading away all data

							if (rd && port.m_COMSTAT.cbInQue > 0) {
								FD_SET(fd, readfds);
								ready++;
							}

							if (wr && port.m_COMSTAT.cbOutQue == 0) {
								FD_SET(fd, writefds);
								ready++;
							}

							if (port.m_WaitPending) {
								if (!SetCommMask(port.m_Comm, 0))
									port.fail();
								if (!GetOverlappedResult(port.m_Comm, port.m_SelOVL, port.m_SelN, false)) {
									if (GetLastError() != ERROR_IO_INCOMPLETE)
										port.fail();
								}
								port.m_WaitPending = false;
							}

							if (!ResetEvent(port.m_SelOVL.hEvent))
								port.fail();

							int flags = 0;
							if (rd)
								flags |= EV_RXCHAR;
							if (wr)
								flags |= EV_TXEMPTY;

							if (!SetCommMask(port.m_Comm, flags))
								port.fail();

							if (WaitCommEvent(port.m_Comm, port.m_EventFlags, port.m_SelOVL)) {
								if (!GetOverlappedResult(port.m_Comm, port.m_SelOVL, port.m_SelN, false))
									port.fail();
								// actually it seems that overlapped
								// WaitCommEvent never returns true so we never get here
								ready = maskToFDSets(port, readfds, writefds, exceptfds, ready);
							} else {
								// FIXME if the port dies on us what happens
								// FIXME Under Windows sometimes the error code 87 (ERROR_INVALID_PARAMETER) is returned.
								// it has not been not investigated why but this appears to work
								if (GetLastError() != ERROR_IO_PENDING && GetLastError() != ERROR_INVALID_PARAMETER)
									port.fail();
								waiting.add(port);
								port.m_WaitPending = true;
							}
						} catch (InterruptedException ie) {
							m_ErrNo = EINTR;
							return -1;
						}

					}
				}
				if (ready == 0) {
					int waitn = waiting.size();
					if (waitn > 0) {
						HANDLE[] wobj = new HANDLE[waiting.size() * 2];
						int i = 0;
						for (Port port : waiting) {
							wobj[i++] = port.m_SelOVL.hEvent;
							wobj[i++] = port.WaitCommEventCancelObject;
						}
						int tout = timeout != null ? (int) (timeout.tv_sec * 1000 + timeout.tv_usec / 1000) : INFINITE;
						// int res = WaitForSingleObject(wobj[0], tout);
						int res = WaitForMultipleObjects(waitn * 2, wobj, false, tout);

						if (res == WAIT_TIMEOUT) {
							// work around the fact that sometimes we miss
							// events
							for (Port port : waiting) {
								clearCommErrors(port);
								int[] mask = { 0 };

								if (!GetCommMask(port.m_Comm, mask))
									port.fail();
								if (port.m_COMSTAT.cbInQue > 0 && ((mask[0] & EV_RXCHAR) != 0)) {
									FD_SET(port.m_FD, readfds);
									log = log && log(1, "missed EV_RXCHAR event\n");
									return 1;
								}
								if (port.m_COMSTAT.cbOutQue == 0 && ((mask[0] & EV_TXEMPTY) != 0)) {
									FD_SET(port.m_FD, writefds);
									log = log && log(1, "missed EV_TXEMPTY event\n");
									return 1;
								}
							}

						}
						if (res != WAIT_TIMEOUT) {
							i = (res - WAIT_OBJECT_0) / 2;
							if (i < 0 || i >= waitn)
								throw new Fail();
							if (((res - WAIT_OBJECT_0) & 1) == 1) {
								// it was the cancel sema4 so just return 
								return 0;
							}
							Port port = waiting.get(i);
							if (!GetOverlappedResult(port.m_Comm, port.m_SelOVL, port.m_SelN, false))
								port.fail();

							ready = maskToFDSets(port, readfds, writefds, exceptfds, ready);
							port.m_WaitPending = false;
						}
					} else {
						if (timeout != null)
							nanoSleep(timeout.tv_sec * 1000000000L + timeout.tv_usec * 1000);
						else {
							m_ErrNo = EINVAL;
							return -1;
						}
						return 0;
					}
				}
			} catch (Fail f) {
				f.printStackTrace();
				return -1;
			}
		} finally {
			for (Port port : locked)
				port.unlock();

		}
		// long T1 = System.currentTimeMillis();
		// System.err.println("select() " + (T1 - T0));

		return ready;
	}

	public int poll(Pollfd fds[], int nfds, int timeout) {
		m_ErrNo = EINVAL;
		return -1;
	}

	public int poll(int fds[], int nfds, int timeout) {
		m_ErrNo = EINVAL;
		return -1;
	}

	public boolean canPoll() {
		return false;
	}

	public void perror(String msg) {
		if (msg != null && msg.length() > 0)
			System.out.print(msg + ": ");
		System.out.printf("%d\n", m_ErrNo);
	}

	// This is a bit pointless function as Windows baudrate constants are
	// just the baudrates so basically this is a no-op, it returns what it gets
	// Note this assumes that the Bxxxx constants in JTermios have the default
	// values ie the values are the baudrates.
	private static int baudToDCB(int baud) {
		switch (baud) {
			case 110:
				return CBR_110;
			case 300:
				return CBR_300;
			case 600:
				return CBR_600;
			case 1200:
				return CBR_1200;
			case 2400:
				return CBR_2400;
			case 4800:
				return CBR_4800;
			case 9600:
				return CBR_9600;
			case 14400:
				return CBR_14400;
			case 19200:
				return CBR_19200;
			case 38400:
				return CBR_38400;
			case 57600:
				return CBR_57600;
			case 115200:
				return CBR_115200;
			case 128000:
				return CBR_128000;
			case 256000:
				return CBR_256000;

			default:
				return baud;
		}
	}

	public FDSet newFDSet() {
		return new FDSetImpl();
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

	public int ioctl(int fd, int cmd, int[] arg) {
		Port port = getPort(fd);
		if (port == null)
			return -1;
		try {
			if (cmd == FIONREAD) {
				clearCommErrors(port);
				arg[0] = port.m_COMSTAT.cbInQue;
				return 0;
			} else if (cmd == TIOCMSET) {
				int a = arg[0];
				if ((a & TIOCM_DTR) != 0)
					port.MSR |= TIOCM_DTR;
				else
					port.MSR &= ~TIOCM_DTR;

				if (!EscapeCommFunction(port.m_Comm, ((a & TIOCM_DTR) != 0) ? SETDTR : CLRDTR))
					port.fail();

				if ((a & TIOCM_RTS) != 0)
					port.MSR |= TIOCM_RTS;
				else
					port.MSR &= ~TIOCM_RTS;
				if (!EscapeCommFunction(port.m_Comm, ((a & TIOCM_RTS) != 0) ? SETRTS : CLRRTS))
					port.fail();
				return 0;
			} else if (cmd == TIOCMGET) {
				int[] stat = { 0 };
				if (!GetCommModemStatus(port.m_Comm, stat))
					port.fail();
				int s = stat[0];
				int a = arg[0];
				if ((s & MS_RLSD_ON) != 0)
					a |= TIOCM_CAR;
				else
					a &= ~TIOCM_CAR;
				if ((s & MS_RING_ON) != 0)
					a |= TIOCM_RNG;
				else
					a &= ~TIOCM_RNG;
				if ((s & MS_DSR_ON) != 0)
					a |= TIOCM_DSR;
				else
					a &= ~TIOCM_DSR;
				if ((s & MS_CTS_ON) != 0)
					a |= TIOCM_CTS;
				else
					a &= ~TIOCM_CTS;

				if ((port.MSR & TIOCM_DTR) != 0)
					a |= TIOCM_DTR;
				else
					a &= ~TIOCM_DTR;
				if ((port.MSR & TIOCM_RTS) != 0)
					a |= TIOCM_RTS;
				else
					a &= ~TIOCM_RTS;
				arg[0] = a;

				return 0;
			} else {
				m_ErrNo = ENOTSUP;
				return -1;
			}
		} catch (Fail f) {
			return -1;
		}
	}

	private void set_errno(int x) {
		m_ErrNo = x;
	}

	private void report(String msg) {
		System.err.print(msg);
	}

	private Port getPort(int fd) {
		synchronized (this) {
			Port port = m_OpenPorts.get(fd);
			if (port == null)
				m_ErrNo = EBADF;
			return port;
		}
	}

	private static String getString(byte[] buffer, int offset) {
		StringBuffer s = new StringBuffer();
		byte c;
		while ((c = buffer[offset++]) != 0)
			s.append((char) c);
		return s.toString();
	}

	public String getPortNamePattern() {
		return "^COM.*";
	}

	public List<String> getPortList() {
		Pattern p = JTermios.getPortNamePattern(this);
		byte[] buffer;
		int size = 0;
		for (size = 16 * 1024; size < 1024 * 1024; size *= 2) {
			buffer = new byte[size];
			int res = QueryDosDevice(null, buffer, buffer.length);
			if (res > 0) { //
				LinkedList<String> list = new LinkedList<String>();
				int offset = 0;
				String port;
				while ((port = getString(buffer, offset)).length() > 0) {
					if (p.matcher(port).matches())
						list.add(port);

					offset += port.length() + 1;
				}
				return list;
			} else {
				int err = GetLastError();
				if (err != ERROR_INSUFFICIENT_BUFFER) {
					log = log && log(1, "QueryDosDeviceW() failed with GetLastError() = %d\n", err);
					return null;
				}
			}
		}
		log = log && log(1, "Repeated QueryDosDeviceW() calls failed up to buffer size %d\n", size);
		return null;
	}

	public void shutDown() {
		for (Port port : m_OpenPorts.values()) {
			try {
				log = log && log(1, "shutDown() closing port %d\n", port.m_FD);
				port.close();
			} catch (Exception e) {
				// should never happen
				e.printStackTrace();
			}
		}
	}

	public int setspeed(int fd, Termios termios, int speed) {
		int br = speed;
		switch (speed) {
			case 50:
				br = B50;
				break;
			case 75:
				br = B75;
				break;
			case 110:
				br = B110;
				break;
			case 134:
				br = B134;
				break;
			case 150:
				br = B150;
				break;
			case 200:
				br = B200;
				break;
			case 300:
				br = B300;
				break;
			case 600:
				br = B600;
				break;
			case 1200:
				br = B1200;
				break;
			case 1800:
				br = B1800;
				break;
			case 2400:
				br = B2400;
				break;
			case 4800:
				br = B4800;
				break;
			case 9600:
				br = B9600;
				break;
			case 19200:
				br = B19200;
				break;
			case 38400:
				br = B38400;
				break;
			case 7200:
				br = B7200;
				break;
			case 14400:
				br = B14400;
				break;
			case 28800:
				br = B28800;
				break;
			case 57600:
				br = B57600;
				break;
			case 76800:
				br = B76800;
				break;
			case 115200:
				br = B115200;
				break;
			case 230400:
				br = B230400;
				break;
		}
		int r;
		if ((r = cfsetispeed(termios, br)) != 0)
			return r;
		if ((r = cfsetospeed(termios, br)) != 0)
			return r;
		if ((r = tcsetattr(fd, TCSANOW, termios)) != 0)
			return r;
		return 0;
	}

	public int pipe(int[] fds) {
		m_ErrNo = EMFILE; // pipe() not implemented on Windows backend
		return -1;
	}

}