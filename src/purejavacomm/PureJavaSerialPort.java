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

	private volatile boolean mReceiveTimeOutEnabled;
	private volatile int mReceiveTimeOutValue;

	private volatile boolean m_NotifyOnDataAvailable;
	private volatile boolean m_DataAvailableNotified;

	private volatile boolean m_NotifyOnOutputEmpty;
	private volatile boolean m_OutputEmptyNotified;

	private volatile boolean m_NotifyOnRI;
	private volatile boolean m_NotifiedStateOfRI;

	private volatile boolean m_NotifyOnCTS;
	private volatile boolean m_NotifiedStateOfCTS;

	private volatile boolean m_NotifyOnDSR;
	private volatile boolean m_NotifiedStateOfDSR;

	private volatile boolean m_NotifyOnCD;
	private volatile boolean m_NotifiedStateOfCD;

	private volatile boolean m_NotifyOnOverrunError;
	private volatile boolean m_NotifyOnParityError;
	private volatile boolean m_NotifyOnFramingError;
	private volatile boolean m_NotifyOnBreakInterrupt;

	synchronized private void sendDataEvents(boolean read, boolean write) {
		if (read && m_NotifyOnDataAvailable && !m_DataAvailableNotified) {
			m_DataAvailableNotified = true;
			m_EventListener.serialEvent(new SerialPortEvent(this, SerialPortEvent.DATA_AVAILABLE, false, true));
		}
		if (write && m_NotifyOnOutputEmpty && !m_OutputEmptyNotified) {
			m_OutputEmptyNotified = true;
			m_EventListener.serialEvent(new SerialPortEvent(this, SerialPortEvent.OUTPUT_BUFFER_EMPTY, false, true));
		}
	}

	synchronized private void sendNonDataEvents() {
		int[] iostatus = new int[1];
		if (ioctl(m_FD, TIOCMGET, iostatus) != -1)
			return; //FIXME decide what to with errors in the background thread
		int status = iostatus[0];

		boolean newstate;

		newstate = (status & TIOCM_CTS) != 0;
		if (m_NotifyOnCTS && m_NotifiedStateOfCTS != newstate) {
			m_EventListener.serialEvent(new SerialPortEvent(this, SerialPortEvent.CTS, m_NotifiedStateOfCTS, newstate));
		}
		m_NotifiedStateOfCTS = newstate;

		newstate = (status & TIOCM_DSR) != 0;
		if (m_NotifyOnDSR && m_NotifiedStateOfDSR != newstate) {
			m_EventListener.serialEvent(new SerialPortEvent(this, SerialPortEvent.DSR, m_NotifiedStateOfDSR, newstate));
		}
		m_NotifiedStateOfDSR = newstate;

		newstate = (status & TIOCM_RI) != 0;
		if (m_NotifyOnRI && m_NotifiedStateOfRI != newstate) {
			m_EventListener.serialEvent(new SerialPortEvent(this, SerialPortEvent.RI, m_NotifiedStateOfRI, newstate));
		}
		m_NotifiedStateOfRI = newstate;

		newstate = (status & TIOCM_CD) != 0;
		if (m_NotifyOnCD && m_NotifiedStateOfCD != newstate) {
			m_EventListener.serialEvent(new SerialPortEvent(this, SerialPortEvent.CD, m_NotifiedStateOfCD, newstate));
		}
		m_NotifiedStateOfCD = newstate;
	}

	@Override
	synchronized public void addEventListener(SerialPortEventListener eventListener) throws TooManyListenersException {
		checkState();
		if (eventListener == null)
			throw new IllegalArgumentException("eventListener cannot be null");
		if (m_EventListener != null)
			throw new TooManyListenersException();
		m_EventListener = eventListener;
		m_Thread.start();

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
		m_NotifyOnCTS = x;
	}

	@Override
	synchronized public void notifyOnCarrierDetect(boolean x) {
		checkState();
		m_NotifyOnCD = x;
	}

	@Override
	synchronized public void notifyOnDSR(boolean x) {
		checkState();
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
		// Not supported
	}

	@Override
	synchronized public void disableReceiveTimeout() {
		checkState();
		Termios termios = new Termios();
		mReceiveTimeOutEnabled = false;
		checkReturnCode(tcgetattr(m_FD, termios));

		// what about setting blocking mode
		termios.c_cc[VMIN] = 1;
		termios.c_cc[VTIME] = 0;

		checkReturnCode(tcsetattr(m_FD, TCSANOW, termios));
	}

	@Override
	synchronized public void enableReceiveFraming(int arg0) throws UnsupportedCommOperationException {
		checkState();
		throw new UnsupportedCommOperationException();
	}

	@Override
	synchronized public void enableReceiveThreshold(int arg0) throws UnsupportedCommOperationException {
		checkState();
		throw new UnsupportedCommOperationException();
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
		if (value / 100 > 255)
			throw new UnsupportedCommOperationException();

		Termios termios = new Termios();

		checkReturnCode(tcgetattr(m_FD, termios));

		termios.c_cc[VMIN] = 0;
		termios.c_cc[VTIME] = (byte) (value / 100);

		checkReturnCode(tcsetattr(m_FD, TCSANOW, termios));

		mReceiveTimeOutEnabled = true;
		mReceiveTimeOutValue = value;

	}

	@Override
	synchronized public void setFlowControlMode(int mode) throws UnsupportedCommOperationException {
		checkState();

		boolean hwin = (mode & FLOWCONTROL_RTSCTS_IN) != 0;
		boolean hwout = (mode & FLOWCONTROL_RTSCTS_OUT) != 0;
		boolean hw = hwin || hwout;

		boolean swin = (mode & FLOWCONTROL_XONXOFF_IN) != 0;
		boolean swout = (mode & FLOWCONTROL_XONXOFF_IN) != 0;
		boolean sw = swin || swout;
		Termios options = new Termios();
		checkReturnCode(tcgetattr(m_FD, options));

		int c_iflag = options.c_iflag;

		c_iflag &= ~IXANY;

		if ((mode & (FLOWCONTROL_RTSCTS_IN | FLOWCONTROL_RTSCTS_OUT)) != 0)
			c_iflag |= CRTSCTS;
		else
			c_iflag &= ~CRTSCTS;

		if ((mode & FLOWCONTROL_XONXOFF_IN) != 0)
			c_iflag |= IXOFF;
		else
			c_iflag &= ~IXOFF;

		if ((mode & FLOWCONTROL_XONXOFF_OUT) != 0)
			c_iflag |= IXON;
		else
			c_iflag &= ~IXON;
		options.c_iflag = c_iflag;

		checkReturnCode(tcsetattr(m_FD, TCSANOW, options));

		m_FlowControlMode = mode;

	}

	@Override
	synchronized public void setSerialPortParams(int baudRate, int dataBits, int stopBits, int parity) throws UnsupportedCommOperationException {
		checkState();
		// get current attributes
		Termios termios = new Termios();
		Termios prev = new Termios();// (termios);

		checkReturnCode(tcgetattr(m_FD, termios));

		// save a copy in case we need to restore it
		prev.set(termios);

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
			checkReturnCode(cfsetispeed(termios, br));
			checkReturnCode(cfsetospeed(termios, br));
			checkReturnCode(tcsetattr(m_FD, TCSANOW, termios));

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

			int fi = termios.c_iflag;
			int fc = termios.c_cflag;
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

			termios.c_cflag = fc;
			termios.c_iflag = fi;

			termios.c_cflag &= ~CSIZE; /* Mask the character size bits */
			termios.c_cflag |= db; /* Select 8 data bits */

			checkReturnCode(tcsetattr(m_FD, TCSANOW, termios));
			checkReturnCode(tcflush(m_FD, TCIOFLUSH));

			// finally everything went ok, so we can update our settings
			m_BaudRate = baudRate;
			m_Parity = parity;
			m_DataBits = dataBits;
			m_StopBits = stopBits;
		} catch (UnsupportedCommOperationException e) {
			checkReturnCode(tcsetattr(m_FD, TCSANOW, prev));
			throw e;
		} catch (IllegalStateException e) {
			checkReturnCode(tcsetattr(m_FD, TCSANOW, prev));
			throw e;
		}
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
				m_OutputEmptyNotified = false;
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				checkState();
				while (len > 0) {
					int n = len;
					if (n > m_Buffer.length)
						n = m_Buffer.length;
					if (n > b.length)
						n = b.length;
					System.arraycopy(b, off, m_Buffer, 0, n);

					n = jtermios.JTermios.write(m_FD, b, len);
					if (n < 0)
						PureJavaSerialPort.this.close();
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
				m_DataAvailableNotified = false;

				return n > 0 ? buf[0] & 0xFF : -1;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				checkState();
				int n = jtermios.JTermios.read(m_FD, b, len);
				m_DataAvailableNotified = false;
				return n;
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
		return mReceiveTimeOutValue;
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
		return mReceiveTimeOutEnabled;
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
		m_Thread.interrupt();
		// At least on Windows we get crash if an Overlapped IO is in progress
		// so we need to wait for the thread to die to ensure that nothing is
		// in progress
		while (m_Thread.isAlive()) {
			try {
				log = log & log(1, "close() waiting for the thread to die\n");
				Thread.sleep(5);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		int err = jtermios.JTermios.close(m_FD);
		// No point in making much noise if close fails, as this is the last thing we do.
		// So just log it at level 1 in case someone is logging this
		if (err < 0)
			log = log && log(1, "JTermios.close returned %d\n", err);
		super.close();
		m_FD = -1;
	}

	public PureJavaSerialPort(String name, int timeout) throws PortInUseException {
		super(name);
		m_FD = open(name, O_RDWR | O_NOCTTY | O_NONBLOCK);
		if (m_FD < 0)
			throw new PortInUseException();

		m_BaudRate = 9600;
		m_DataBits = SerialPort.DATABITS_8;
		m_FlowControlMode = SerialPort.FLOWCONTROL_NONE;
		m_Parity = SerialPort.PARITY_NONE;
		m_StopBits = SerialPort.STOPBITS_1;

		// FIXME what to do if port is in use or open fails

		Termios termios = new Termios();
		checkReturnCode(tcgetattr(m_FD, termios));
		termios.c_cflag |= CLOCAL | CREAD;
		termios.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
		termios.c_oflag &= ~OPOST;

		termios.c_cc[VSTART] = (byte) DC1;
		termios.c_cc[VSTOP] = (byte) DC3;
		termios.c_cc[VMIN] = 1;
		termios.c_cc[VTIME] = 0;
		checkReturnCode(tcsetattr(m_FD, TCSANOW, termios));

		try {
			setSerialPortParams(m_BaudRate, m_DataBits, m_StopBits, m_Parity);
		} catch (UnsupportedCommOperationException e) {
			// This really should not happen
			e.printStackTrace();
		}

		Runnable runnable = new Runnable() {
			public void run() {
				// see: http://daniel.haxx.se/docs/poll-vs-select.html
				final boolean USE_SELECT = true;
				final int TIMEOUT = 10; // msec
				TimeVal timeout;
				FDSet rset;
				FDSet wset;
				Pollfd[] pollfd;

				if (USE_SELECT) {
					rset = USE_SELECT ? newFDSet() : null;
					wset = USE_SELECT ? newFDSet() : null;
					timeout = new TimeVal();
					timeout.tv_sec = 0;
					timeout.tv_usec = TIMEOUT * 1000; // 10 msec polling period
				} else
					pollfd = USE_SELECT ? null : new Pollfd[] { new Pollfd() };

				while (m_FD >= 0) { // lets die if the file descript dies on us ie the port closes
					boolean read = (m_NotifyOnDataAvailable && !m_DataAvailableNotified);
					boolean write = (m_NotifyOnOutputEmpty && !m_OutputEmptyNotified);
					int n;
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
						System.out.printf("%d %d %04X %04X\n", n, pollfd[0].fd, pollfd[0].events, pollfd[0].revents);
						if ((re & POLLNVAL) != 0) {
							log = log && log(1, "poll() returned POLLNVAL\n");
							if (log)
								perror("perror(): ");
							//close();
							//break;
						}
						read = read && (re & POLLIN) != 0;
						write = write && (re & POLLOUT) != 0;
					}
					if (Thread.currentThread().isInterrupted())
						break;
					if (n < 0) {
						log = log && log(1, "select() or poll() returned %d\n", n);
						if (log)
							perror("perror(): ");
						close();
						break;
					}

					if (m_EventListener != null) {
						if (read || write)
							sendDataEvents(read, write);
						if (m_NotifyOnCTS || m_NotifyOnDSR || m_NotifyOnRI || m_NotifyOnCD)
							sendNonDataEvents();
					}
				}
			}
		};
		m_Thread = new Thread(runnable, "PureJavaSerialPort(" + getName() + ")");
		m_Thread.setDaemon(true);
	}

	private boolean getControlLineState(int line) {
		checkState();
		int[] status = new int[1];
		if (ioctl(m_FD, TIOCMGET, status) == -1)
			throw new IllegalStateException();
		return (status[0] & line) != 0;
	}

	private void setControlLineState(int line, boolean state) {
		checkState();
		int[] status = new int[1];
		if (ioctl(m_FD, TIOCMGET, status) == -1)
			throw new IllegalStateException();

		if (state)
			status[0] |= line;
		else
			status[0] &= line;
		if (ioctl(m_FD, TIOCMSET, status) == -1)
			throw new IllegalStateException();
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
