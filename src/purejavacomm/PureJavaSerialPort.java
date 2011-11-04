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

package purejavacomm;

import java.io.*;
import java.util.*;

import jtermios.*;

import static jtermios.JTermios.JTermiosLogging.*;

import static jtermios.JTermios.*;

public class PureJavaSerialPort extends SerialPort {

	private static Thread m_Thread;

	private volatile SerialPortEventListener m_EventListener;

	private volatile int m_FD = -1;

	private volatile int m_BaudRate;
	private volatile int m_DataBits;
	private volatile int m_FlowControlMode;
	private volatile int m_Parity;
	private volatile int m_StopBits;

	private volatile boolean m_ReceiveTimeOutEnabled;
	private volatile int m_ReceiveTimeOutValue;
	private volatile boolean m_ReceiveThresholdEnabled;
	private volatile int m_ReceiveThresholdValue;

	private volatile boolean m_NotifyOnDataAvailable;
	private volatile boolean m_DataAvailableNotified;

	private volatile boolean m_NotifyOnOutputEmpty;
	private volatile boolean m_OutputEmptyNotified;

	private volatile boolean m_NotifyOnRI;
	private volatile boolean m_NotifyOnCTS;
	private volatile boolean m_NotifyOnDSR;
	private volatile boolean m_NotifyOnCD;
	private volatile boolean m_NotifyOnOverrunError;
	private volatile boolean m_NotifyOnParityError;
	private volatile boolean m_NotifyOnFramingError;
	private volatile boolean m_NotifyOnBreakInterrupt;
	private volatile boolean m_ThreadRunning;
	private volatile boolean m_ThreadStarted;

	private int[] m_ioctl = { 0 };
	private int m_ControlLineStates;
	// we cache termios in m_Termios because we don't rely on reading it back with tcgetattr()
	// which for Mac OS X / CRTSCTS does not work, it is also more efficient 
	private Termios m_Termios = new Termios();

	private void sendDataEvents(boolean read, boolean write) {
		if (read && m_NotifyOnDataAvailable && !m_DataAvailableNotified) {
			m_DataAvailableNotified = true;
			m_EventListener.serialEvent(new SerialPortEvent(this, SerialPortEvent.DATA_AVAILABLE, false, true));
		}
		if (write && m_NotifyOnOutputEmpty && !m_OutputEmptyNotified) {
			m_OutputEmptyNotified = true;
			m_EventListener.serialEvent(new SerialPortEvent(this, SerialPortEvent.OUTPUT_BUFFER_EMPTY, false, true));
		}
	}

	private void sendNonDataEvents() {
		if (ioctl(m_FD, TIOCMGET, m_ioctl) < 0)
			return; //FIXME decide what to with errors in the background thread
		int oldstates = m_ControlLineStates;
		m_ControlLineStates = m_ioctl[0];
		int newstates = m_ControlLineStates;
		int changes = oldstates ^ newstates;
		if (changes == 0)
			return;

		int line;

		if (m_NotifyOnCTS && (((line = TIOCM_CTS) & changes) != 0))
			m_EventListener.serialEvent(new SerialPortEvent(this, SerialPortEvent.CTS, (oldstates & line) != 0, (newstates & line) != 0));

		if (m_NotifyOnDSR && (((line = TIOCM_DSR) & changes) != 0))
			m_EventListener.serialEvent(new SerialPortEvent(this, SerialPortEvent.DSR, (oldstates & line) != 0, (newstates & line) != 0));

		if (m_NotifyOnRI && (((line = TIOCM_RI) & changes) != 0))
			m_EventListener.serialEvent(new SerialPortEvent(this, SerialPortEvent.RI, (oldstates & line) != 0, (newstates & line) != 0));

		if (m_NotifyOnCD && (((line = TIOCM_CD) & changes) != 0))
			m_EventListener.serialEvent(new SerialPortEvent(this, SerialPortEvent.CD, (oldstates & line) != 0, (newstates & line) != 0));
	}

	@Override
	synchronized public void addEventListener(SerialPortEventListener eventListener) throws TooManyListenersException {
		checkState();
		if (eventListener == null)
			throw new IllegalArgumentException("eventListener cannot be null");
		if (m_EventListener != null)
			throw new TooManyListenersException();
		m_EventListener = eventListener;
		if (!m_ThreadStarted) {
			m_ThreadStarted = true;
			m_Thread.start();
		}
	}

	@Override
	synchronized public int getBaudRate() {
		checkState();
		return m_BaudRate;
	}

	@Override
	synchronized public int getDataBits() {
		checkState();
		return m_DataBits;
	}

	@Override
	synchronized public int getFlowControlMode() {
		checkState();
		return m_FlowControlMode;
	}

	@Override
	synchronized public int getParity() {
		checkState();
		return m_Parity;
	}

	@Override
	synchronized public int getStopBits() {
		checkState();
		return m_StopBits;
	}

	@Override
	synchronized public boolean isCD() {
		checkState();
		return getControlLineState(TIOCM_CD);
	}

	@Override
	synchronized public boolean isCTS() {
		checkState();
		return getControlLineState(TIOCM_CTS);
	}

	@Override
	synchronized public boolean isDSR() {
		checkState();
		return getControlLineState(TIOCM_DSR);
	}

	@Override
	synchronized public boolean isDTR() {
		checkState();
		return getControlLineState(TIOCM_DTR);
	}

	@Override
	synchronized public boolean isRI() {
		checkState();
		return getControlLineState(TIOCM_RI);
	}

	@Override
	synchronized public boolean isRTS() {
		checkState();
		return getControlLineState(TIOCM_RTS);
	}

	@Override
	synchronized public void notifyOnBreakInterrupt(boolean x) {
		checkState();
		m_NotifyOnBreakInterrupt = x;
	}

	@Override
	synchronized public void notifyOnCTS(boolean x) {
		checkState();
		if (x)
			updateControlLineState(TIOCM_CTS);
		m_NotifyOnCTS = x;
	}

	@Override
	synchronized public void notifyOnCarrierDetect(boolean x) {
		checkState();
		if (x)
			updateControlLineState(TIOCM_CD);
		m_NotifyOnCD = x;
	}

	@Override
	synchronized public void notifyOnDSR(boolean x) {
		checkState();
		if (x)
			updateControlLineState(TIOCM_DSR);
		m_NotifyOnDSR = x;
	}

	@Override
	synchronized public void notifyOnDataAvailable(boolean x) {
		checkState();
		m_NotifyOnDataAvailable = x;
	}

	@Override
	synchronized public void notifyOnFramingError(boolean x) {
		checkState();
		m_NotifyOnFramingError = x;
	}

	@Override
	synchronized public void notifyOnOutputEmpty(boolean x) {
		checkState();
		m_NotifyOnOutputEmpty = x;
	}

	@Override
	synchronized public void notifyOnOverrunError(boolean x) {
		checkState();
		m_NotifyOnOverrunError = x;
	}

	@Override
	synchronized public void notifyOnParityError(boolean x) {
		checkState();
		m_NotifyOnParityError = x;
	}

	@Override
	synchronized public void notifyOnRingIndicator(boolean x) {
		checkState();
		if (x)
			updateControlLineState(TIOCM_RI);
		m_NotifyOnRI = x;
	}

	@Override
	synchronized public void removeEventListener() {
		checkState();
		m_EventListener = null;
	}

	@Override
	synchronized public void sendBreak(int duration) {
		checkState();
		// FIXME POSIX does not specify how duration is interpreted
		// Opengroup POSIX says:
		// If the terminal is using asynchronous serial data transmission, tcsendbreak() 
		// shall cause transmission of a continuous stream of zero-valued bits for a specific duration. 
		// If duration is 0, it shall cause transmission of zero-valued bits for at least 0.25 seconds, 
		// and not more than 0.5 seconds. If duration is not 0, it shall send zero-valued bits for an implementation-defined period of time.
		// From the man page for Linux tcsendbreak:
		// The effect of a non-zero duration with tcsendbreak() varies. 
		// SunOS specifies a break of duration*N seconds, 
		// where N is at least 0.25, and not more than 0.5. Linux, AIX, DU, Tru64 send a break of duration milliseconds. 
		// FreeBSD and NetBSD and HP-UX and MacOS ignore the value of duration. 
		// Under Solaris and Unixware, tcsendbreak() with non-zero duration behaves like tcdrain().

		tcsendbreak(m_FD, duration);
	}

	@Override
	synchronized public void setDTR(boolean x) {
		checkState();
		setControlLineState(TIOCM_DTR, x);
	}

	@Override
	synchronized public void setRTS(boolean x) {
		checkState();
		setControlLineState(TIOCM_RTS, x);
	}

	@Override
	synchronized public void disableReceiveFraming() {
		checkState();
		// Not supported
	}

	@Override
	synchronized public void disableReceiveThreshold() {
		checkState();
		m_ReceiveThresholdEnabled = false;
		setReceiveTimeout();
	}

	@Override
	synchronized public void disableReceiveTimeout() {
		checkState();
		m_ReceiveTimeOutEnabled = false;
		setReceiveTimeout();
	}

	@Override
	synchronized public void enableReceiveFraming(int arg0) throws UnsupportedCommOperationException {
		checkState();
		throw new UnsupportedCommOperationException();
	}

	@Override
	synchronized public void enableReceiveThreshold(int value) throws UnsupportedCommOperationException {
		checkState();
		m_ReceiveThresholdEnabled = true;
		m_ReceiveThresholdValue = value;
		setReceiveTimeout();
	}

	@Override
	synchronized public int getInputBufferSize() {
		checkState();
		// Not supported
		return 0;
	}

	@Override
	synchronized public int getOutputBufferSize() {
		checkState();
		// Not supported
		return 0;
	}

	@Override
	synchronized public void enableReceiveTimeout(int value) throws UnsupportedCommOperationException {
		checkState();
		m_ReceiveTimeOutEnabled = true;
		m_ReceiveTimeOutValue = value;
		setReceiveTimeout();
	}

	@Override
	synchronized public void setFlowControlMode(int mode) throws UnsupportedCommOperationException {
		checkState();
		m_Termios.c_iflag &= ~IXANY;

		if ((mode & (FLOWCONTROL_RTSCTS_IN | FLOWCONTROL_RTSCTS_OUT)) != 0)
			m_Termios.c_cflag |= CRTSCTS;
		else
			m_Termios.c_cflag &= ~CRTSCTS;

		if ((mode & FLOWCONTROL_XONXOFF_IN) != 0)
			m_Termios.c_iflag |= IXOFF;
		else
			m_Termios.c_iflag &= ~IXOFF;

		if ((mode & FLOWCONTROL_XONXOFF_OUT) != 0)
			m_Termios.c_iflag |= IXON;
		else
			m_Termios.c_iflag &= ~IXON;

		checkReturnCode(tcsetattr(m_FD, TCSANOW, m_Termios));

		m_FlowControlMode = mode;
	}

	@Override
	synchronized public void setSerialPortParams(int baudRate, int dataBits, int stopBits, int parity) throws UnsupportedCommOperationException {
		checkState();
		Termios prev = new Termios();// (termios);

		// save a copy in case we need to restore it
		prev.set(m_Termios);

		try {
			int br = baudRate;
			switch (baudRate) {
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
			// try to set the baud rate before anything else
			// as it may fail at 'tcsetattr' stage and in that
			// case we do not want to change anything
			checkReturnCode(cfsetispeed(m_Termios, br));
			checkReturnCode(cfsetospeed(m_Termios, br));

			int db;
			switch (dataBits) {
				case SerialPort.DATABITS_5:
					db = CS5;
					break;
				case SerialPort.DATABITS_6:
					db = CS6;
					break;
				case SerialPort.DATABITS_7:
					db = CS7;
					break;
				case SerialPort.DATABITS_8:
					db = CS8;
					break;
				default:
					throw new UnsupportedCommOperationException("dataBits = " + dataBits);
			}

			int sb;
			switch (stopBits) {
				case SerialPort.STOPBITS_1:
					sb = CS5;
					break;
				case SerialPort.STOPBITS_1_5:
					sb = CS6;
					break;
				case SerialPort.STOPBITS_2:
					sb = CS7;
					break;
				default:
					throw new UnsupportedCommOperationException("stopBits = " + stopBits);
			}

			int fi = m_Termios.c_iflag;
			int fc = m_Termios.c_cflag;
			switch (parity) {
				case SerialPort.PARITY_NONE:
					fc &= ~PARENB;
					fc &= ~CSTOPB;
					fi &= ~(INPCK | ISTRIP);
					break;
				case SerialPort.PARITY_EVEN:
					fc |= PARENB;
					fc &= ~PARODD;
					fc &= ~CSTOPB;
					fi |= (INPCK | ISTRIP);

					break;
				case SerialPort.PARITY_ODD:
					fc |= PARENB;
					fc |= PARODD;
					fc &= ~CSTOPB;
					fi |= (INPCK | ISTRIP);
					break;
				default:
					throw new UnsupportedCommOperationException("parity = " + stopBits);
			}

			// update the hardware 

			m_Termios.c_cflag = fc;
			m_Termios.c_iflag = fi;

			m_Termios.c_cflag &= ~CSIZE; /* Mask the character size bits */
			m_Termios.c_cflag |= db; /* Select 8 data bits */

			checkReturnCode(tcsetattr(m_FD, TCSANOW, m_Termios));
			checkReturnCode(tcflush(m_FD, TCIOFLUSH));

			// finally everything went ok, so we can update our settings
			m_BaudRate = baudRate;
			m_Parity = parity;
			m_DataBits = dataBits;
			m_StopBits = stopBits;
		} catch (UnsupportedCommOperationException e) {
			m_Termios.set(prev);
			checkReturnCode(tcsetattr(m_FD, TCSANOW, m_Termios));
			throw e;
		} catch (IllegalStateException e) {
			m_Termios.set(prev);
			checkReturnCode(tcsetattr(m_FD, TCSANOW, m_Termios));
			throw e;
		}
	}

	private static int min(int a, int b) {
		return a < b ? a : b;
	}

	@Override
	synchronized public OutputStream getOutputStream() throws IOException {
		checkState();
		return new OutputStream() {
			private byte[] m_Buffer = new byte[2048];

			@Override
			public void write(int b) throws IOException {
				checkState();
				byte[] buf = { (byte) b };
				write(buf, 0, 1);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				checkState();
				while (len > 0) {
					int n = min(len, min(m_Buffer.length, b.length - off));
					if (off > 0) {
						System.arraycopy(b, off, m_Buffer, 0, n);
						n = jtermios.JTermios.write(m_FD, m_Buffer, n);
					} else
						n = jtermios.JTermios.write(m_FD, b, n);

					if (n < 0) {
						PureJavaSerialPort.this.close();
						throw new IOException();
					}

					len -= n;
					off += n;
				}
				m_OutputEmptyNotified = false;
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}

			@Override
			public void flush() throws IOException {
				checkState();
				if (tcdrain(m_FD) < 0) {
					close();
					throw new IOException();
				}
			}
		};
	}

	synchronized public InputStream getInputStream() throws IOException {
		checkState();
		return new InputStream() {
			private TimeVal m_TimeOut = new TimeVal();
			private int[] m_Available = { 0 };
			private byte[] m_Buffer = new byte[2048];

			@Override
			public int available() throws IOException {
				checkState();
				if (ioctl(m_FD, FIONREAD, m_Available) < 0) {
					PureJavaSerialPort.this.close();
					throw new IOException();
				}

				return m_Available[0];
			}

			@Override
			public int read() throws IOException {
				checkState();
				byte[] buf = { 0 };
				int n = read(buf, 0, 1);

				return n > 0 ? buf[0] & 0xFF : -1;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				checkState();
				long T0 = m_ReceiveTimeOutEnabled ? System.currentTimeMillis() : 0;
				int N = 0;
				while (true) {
					int n = len - N;
					if (off > 0) {
						n = min(n, min(m_Buffer.length, b.length - off));
						n = jtermios.JTermios.read(m_FD, m_Buffer, n);
						if (n > 0)
							System.arraycopy(m_Buffer, 0, b, off, n);
					} else
						n = jtermios.JTermios.read(m_FD, b, n);
					if (n < 0)
						throw new IOException();

					N += n;
					//System.out.printf("n=%d off=%d left=%d N=%d th=%d to=%d dt=%d\n",n, off,left,N,m_ReceiveThresholdValue,m_ReceiveTimeOutValue,System.currentTimeMillis() - T0);
					if (!m_ReceiveThresholdEnabled && N > 0)
						break;
					if (m_ReceiveThresholdEnabled && N >= m_ReceiveThresholdValue)
						break;
					if (m_ReceiveTimeOutEnabled && System.currentTimeMillis() - T0 >= m_ReceiveTimeOutValue)
						break;
					off += n;
				}
				m_DataAvailableNotified = false;
				return N;
			}
		};
	}

	@Override
	synchronized public int getReceiveFramingByte() {
		checkState();
		// Not supported
		return 0;
	}

	@Override
	synchronized public int getReceiveThreshold() {
		checkState();
		// Not supported
		return 0;
	}

	@Override
	synchronized public int getReceiveTimeout() {
		checkState();
		return m_ReceiveTimeOutValue;
	}

	@Override
	synchronized public boolean isReceiveFramingEnabled() {
		checkState();
		// Not supported
		return false;
	}

	@Override
	synchronized public boolean isReceiveThresholdEnabled() {
		checkState();
		// Not supported
		return false;
	}

	@Override
	synchronized public boolean isReceiveTimeoutEnabled() {
		checkState();
		return m_ReceiveTimeOutEnabled;
	}

	@Override
	synchronized public void setInputBufferSize(int arg0) {
		checkState();
		// Not supported
	}

	@Override
	synchronized public void setOutputBufferSize(int arg0) {
		checkState();
		// Not supported
	}

	@Override
	synchronized public void close() {
		int fd = m_FD;
		if (fd != -1) {
			m_FD = -1;
			int flags = fcntl(fd, F_GETFL, 0);
			flags |= O_NONBLOCK;
			int fcres = fcntl(fd, F_SETFL, flags);
			if (fcres != 0) // not much we can do if this fails, so just log it
				log = log && log(1, "fcntl(%d,%d,%d) returned %d\n", m_FD, F_SETFL, flags, fcres);

			m_Thread.interrupt();
			int err = jtermios.JTermios.close(fd);
			if (err < 0)
				log = log && log(1, "JTermios.close returned %d, errno %d\n", err, errno());
			long t0 = System.currentTimeMillis();
			while (m_ThreadRunning) {
				try {
					Thread.sleep(5);
					if (System.currentTimeMillis() - t0 > 2000)
						break;
				} catch (InterruptedException e) {
					break;
				}
			}
			super.close();
		}
	}

	public PureJavaSerialPort(String name, int timeout) throws PortInUseException {
		super(name);
		// unbelievable, sometimes quickly closing and re-opening fails on Windows
		// so try a few times
		int tries = 100;
		while ((m_FD = open(name, O_RDWR | O_NOCTTY | O_NONBLOCK)) < 0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
			if (tries-- < 0)
				throw new PortInUseException();
		}
		while (m_FD < 0)
			;

		int flags = fcntl(m_FD, F_GETFL, 0);
		flags &= ~O_NONBLOCK;
		checkReturnCode(fcntl(m_FD, F_SETFL, flags));

		m_BaudRate = 9600;
		m_DataBits = SerialPort.DATABITS_8;
		m_FlowControlMode = SerialPort.FLOWCONTROL_NONE;
		m_Parity = SerialPort.PARITY_NONE;
		m_StopBits = SerialPort.STOPBITS_1;

		checkReturnCode(tcgetattr(m_FD, m_Termios));
		m_Termios.c_cflag |= CLOCAL | CREAD;
		m_Termios.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
		m_Termios.c_oflag &= ~OPOST;

		m_Termios.c_cc[VSTART] = (byte) DC1;
		m_Termios.c_cc[VSTOP] = (byte) DC3;
		m_Termios.c_cc[VMIN] = 0;
		m_Termios.c_cc[VTIME] = 0;
		checkReturnCode(tcsetattr(m_FD, TCSANOW, m_Termios));

		try {
			setSerialPortParams(m_BaudRate, m_DataBits, m_StopBits, m_Parity);
		} catch (UnsupportedCommOperationException e) {
			// This really should not happen
			e.printStackTrace();
		}

		setReceiveTimeout();

		checkReturnCode(ioctl(m_FD, TIOCMGET, m_ioctl));
		m_ControlLineStates = m_ioctl[0];

		Runnable runnable = new Runnable() {
			public void run() {
				try {
					m_ThreadRunning = true;
					// see: http://daniel.haxx.se/docs/poll-vs-select.html
					final boolean USE_SELECT = true;
					final int TIMEOUT = 10; // msec
					TimeVal timeout;
					FDSet rset;
					FDSet wset;
					Pollfd[] pollfd;

					if (USE_SELECT) {
						rset = newFDSet();
						wset = newFDSet();
						timeout = new TimeVal();
						timeout.tv_sec = 0;
						timeout.tv_usec = TIMEOUT * 1000; // 10 msec polling period
					} else
						pollfd = new Pollfd[] { new Pollfd() };

					while (m_FD >= 0) { // lets die if the file descriptor dies on us ie the port closes
						boolean read = (m_NotifyOnDataAvailable && !m_DataAvailableNotified);
						boolean write = (m_NotifyOnOutputEmpty && !m_OutputEmptyNotified);
						int n = 0;
						if (!read && !write)
							Thread.sleep(TIMEOUT);
						else { // do all this only if we actually wait for read or write
							if (USE_SELECT) {
								FD_ZERO(rset);
								FD_ZERO(wset);
								if (read)
									FD_SET(m_FD, rset);
								if (write)
									FD_SET(m_FD, wset);
								n = select(m_FD + 1, rset, wset, null, timeout);
								read = read && FD_ISSET(m_FD, rset);
								write = write && FD_ISSET(m_FD, wset);
							} else { // use poll
								pollfd[0].fd = m_FD;
								short e = 0;
								if (read)
									e |= POLLIN;
								if (write)
									e |= POLLOUT;
								pollfd[0].events = e;
								pollfd[0].revents = 0;
								n = poll(pollfd, 1, TIMEOUT);
								int re = pollfd[0].revents;
								if ((re & POLLNVAL) != 0) {
									log = log && log(1, "poll() returned POLLNVAL, errno %\n", errno());
									break;
								}
								read = read && (re & POLLIN) != 0;
								write = write && (re & POLLOUT) != 0;
							}
							if (Thread.currentThread().isInterrupted())
								break;
							if (n < 0) {
								log = log && log(1, "select() or poll() returned %d, errno %d\n", n, errno());
								close();
								break;
							}
						}

						if (m_EventListener != null) {
							if (read || write)
								sendDataEvents(read, write);
							if (m_NotifyOnCTS || m_NotifyOnDSR || m_NotifyOnRI || m_NotifyOnCD)
								sendNonDataEvents();
						}
					}
				} catch (InterruptedException ie) {
				} finally {
					m_ThreadRunning = false;
				}
			}
		};
		m_Thread = new Thread(runnable, getName());
		m_Thread.setDaemon(true);
	}

	synchronized private void updateControlLineState(int line) {
		checkState();

		if (ioctl(m_FD, TIOCMGET, m_ioctl) == -1)
			throw new IllegalStateException();

		m_ControlLineStates = (m_ioctl[0] & line) + (m_ControlLineStates & ~line);
	}

	synchronized private boolean getControlLineState(int line) {
		checkState();
		if (ioctl(m_FD, TIOCMGET, m_ioctl) == -1)
			throw new IllegalStateException();
		return (m_ioctl[0] & line) != 0;
	}

	synchronized private void setControlLineState(int line, boolean state) {
		checkState();
		if (ioctl(m_FD, TIOCMGET, m_ioctl) == -1)
			throw new IllegalStateException();

		if (state)
			m_ioctl[0] |= line;
		else
			m_ioctl[0] &= ~line;
		if (ioctl(m_FD, TIOCMSET, m_ioctl) == -1)
			throw new IllegalStateException();
	}

	private void setReceiveTimeout() {
		// Javadoc for javacomm says:
		// Enabling the Timeout OR Threshold with a value a zero is a special case. 
		// This causes the underlying driver to poll for incoming data instead being 
		// event driven. Otherwise, the behaviour is identical to having both the 
		// Timeout and Threshold disabled.
		// but what does it mean?

		// Threshold	         Timeout                         Behaviour
		//
		// disabled	 -	         disabled	 -	     n bytes	 block until any data is available
		//                                                       VMIN = 1, VTIME = 0
		//
		// enabled	 m bytes	 disabled	 -	     n bytes	 block until min(m,n) bytes are available
		//                                                       VMIN = 1, VTIME = 0
		//                                                       need to loop, inside InputStream.read(), 
		//                                                       until min(m,n) bytes received 
		//
		// disabled	 -	         enabled	 x ms	 n bytes	 block for x ms or until any data is available
		//                                                       VMIN = 0
		//                                                       if x<25500 then 
		//                                                          VTIME = x / 100
		//                                                       else
		//                                                          k = (x / 25500 + 1);
		//                                                          VTIME = x / k / 100 + 1
		//                                                          and we need to loop k times 
		//                                                          inside InputStream.read()  
		//
		// enabled	 m bytes	 enabled	 x ms	 n bytes	 block for x ms or until min(m,n) bytes are available		
		//                                                       same as previous, except we need to loop
		//                                                       inside InputStream.read() until 
		//                                                       until min(m,n) bytes received 

		byte vmin = 1;
		byte vtime = 0;
		if (m_ReceiveTimeOutEnabled) {
			vmin = 0;
			int t = m_ReceiveTimeOutValue;
			if (t < 25500)
				vtime = (byte) (t / 100);
			else {
				int n = t / 25500 + 1;
				vtime = (byte) (t / n / 100 + 1);
			}
		}

		m_Termios.c_cc[VMIN] = vmin;
		m_Termios.c_cc[VTIME] = vtime;
		checkReturnCode(tcsetattr(m_FD, TCSANOW, m_Termios));
	}

	private void checkState() {
		if (m_FD < 0)
			throw new IllegalStateException("File descriptor is " + m_FD + " < 0, maybe closed by previous error condition");
	}

	private void checkReturnCode(int code) {
		if (code != 0) {
			close();
			StackTraceElement ste = Thread.currentThread().getStackTrace()[1];
			String msg = String.format("JTermios call returned %d at %s", code, lineno());
			log = log && log(1, "%s\n", msg);
			throw new IllegalStateException(msg);
		}
	}

}
