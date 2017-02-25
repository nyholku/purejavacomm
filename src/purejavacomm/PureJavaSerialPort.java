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

// FIXME move javadoc comments for input stream to SerialPort.java
import java.io.*;
import java.util.*;

import com.sun.jna.Native;

import jtermios.*;
import static jtermios.JTermios.JTermiosLogging.*;
import static jtermios.JTermios.*;

public class PureJavaSerialPort extends SerialPort {
	final boolean USE_POLL;
	final boolean RAW_READ_MODE;
	private Thread m_Thread;
	private volatile SerialPortEventListener m_EventListener;
	private volatile OutputStream m_OutputStream;
	private volatile InputStream m_InputStream;
	private volatile int m_FD = -1;
	private volatile boolean m_HaveNudgePipe = false;
	private volatile int m_PipeWrFD = 0;
	private volatile int m_PipeRdFD = 0;
	private byte[] m_NudgeData = { 0 };
	private volatile int m_BaudRate;
	private volatile int m_DataBits;
	private volatile int m_FlowControlMode;
	private volatile int m_Parity;
	private volatile int m_StopBits;
	private volatile Object m_ThresholdTimeoutLock = new Object();
	private volatile boolean m_TimeoutThresholdChanged = true;
	private volatile boolean m_ReceiveTimeoutEnabled;
	private volatile int m_ReceiveTimeoutValue;
	private volatile int m_ReceiveTimeoutVTIME;
	private volatile boolean m_ReceiveThresholdEnabled;
	private volatile int m_ReceiveThresholdValue;
	private volatile boolean m_PollingReadMode;
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
	private int m_MinVTIME;

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

	private synchronized void sendNonDataEvents() {
		if (ioctl(m_FD, TIOCMGET, m_ioctl) < 0)
			return; // FIXME decide what to with errors in the background thread
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
		nudgePipe();
	}

	@Override
	synchronized public void notifyOnCarrierDetect(boolean x) {
		checkState();
		if (x)
			updateControlLineState(TIOCM_CD);
		m_NotifyOnCD = x;
		nudgePipe();
	}

	@Override
	synchronized public void notifyOnDSR(boolean x) {
		checkState();
		if (x)
			updateControlLineState(TIOCM_DSR);
		m_NotifyOnDSR = x;
		nudgePipe();
	}

	@Override
	synchronized public void notifyOnDataAvailable(boolean x) {
		checkState();
		m_NotifyOnDataAvailable = x;
		nudgePipe();
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
		nudgePipe();
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
		nudgePipe();
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
		synchronized (m_ThresholdTimeoutLock) {
			m_ReceiveThresholdEnabled = false;
			thresholdOrTimeoutChanged();
		}
	}

	@Override
	synchronized public void disableReceiveTimeout() {
		checkState();
		synchronized (m_ThresholdTimeoutLock) {
			m_ReceiveTimeoutEnabled = false;
			thresholdOrTimeoutChanged();
		}
	}

	@Override
	synchronized public void enableReceiveThreshold(int value) throws UnsupportedCommOperationException {
		checkState();
		if (value < 0)
			throw new IllegalArgumentException("threshold" + value + " < 0 ");
		if (RAW_READ_MODE && value > 255)
			throw new IllegalArgumentException("threshold" + value + " > 255 in raw read mode");
		synchronized (m_ThresholdTimeoutLock) {
			m_ReceiveThresholdEnabled = true;
			m_ReceiveThresholdValue = value;
			thresholdOrTimeoutChanged();
		}
	}

	@Override
	synchronized public void enableReceiveTimeout(int value) throws UnsupportedCommOperationException {
		if (value < 0)
			throw new IllegalArgumentException("threshold" + value + " < 0 ");
		if (value > 25500)
			throw new UnsupportedCommOperationException("threshold" + value + " > 25500 ");

		checkState();
		synchronized (m_ThresholdTimeoutLock) {
			m_ReceiveTimeoutEnabled = true;
			m_ReceiveTimeoutValue = value;
			thresholdOrTimeoutChanged();
		}
	}

	@Override
	synchronized public void enableReceiveFraming(int arg0) throws UnsupportedCommOperationException {
		checkState();
		throw new UnsupportedCommOperationException("receive framing not supported/implemented");
	}

	private void thresholdOrTimeoutChanged() { // only call if you hold the lock
		m_PollingReadMode = (m_ReceiveTimeoutEnabled && m_ReceiveTimeoutValue == 0) || (m_ReceiveThresholdEnabled && m_ReceiveThresholdValue == 0);
		m_ReceiveTimeoutVTIME = (m_ReceiveTimeoutValue + 99) / 100; // precalculate this so we don't need the division in read
		m_TimeoutThresholdChanged = true;
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
	synchronized public void setFlowControlMode(int mode) throws UnsupportedCommOperationException {
		checkState();
		synchronized (m_Termios) {
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
	}

	@Override
	synchronized public void setSerialPortParams(int baudRate, int dataBits, int stopBits, int parity) throws UnsupportedCommOperationException {
		checkState();
		synchronized (m_Termios) {
			Termios prev = new Termios();// (termios);

			// save a copy in case we need to restore it
			prev.set(m_Termios);

			try {
				checkReturnCode(setspeed(m_FD, m_Termios, baudRate));

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
						sb = 1;
						break;
					case SerialPort.STOPBITS_1_5:
						// This setting must have been copied from the Win32 API and
						// hasn't been properly thought through. 1.5 stop bits are
						// only valid with 5 data bits and replace the 2 stop bits
						// in this mode. This is a feature of the 16550 and even
						// documented on MSDN
						// As nobody is aware of course, we silently use 1.5 and 2
						// stop bits interchangeably (just as the hardware does)
						// Many linux drivers follow this convention and termios
						// can't even differ between 1.5 and 2 stop bits 
						sb = 2;
						break;
					case SerialPort.STOPBITS_2:
						sb = 2;
						break;
					default:
						throw new UnsupportedCommOperationException("stopBits = " + stopBits);
				}

				int fi = m_Termios.c_iflag & ~(INPCK | ISTRIP);
				int fc = m_Termios.c_cflag & ~(PARENB | CMSPAR | PARODD);
				switch (parity) {
					case SerialPort.PARITY_NONE:
						break;
					case SerialPort.PARITY_EVEN:
						fc |= PARENB;
						break;
					case SerialPort.PARITY_ODD:
						fc |= PARENB;
						fc |= PARODD;
						break;
					case SerialPort.PARITY_MARK:
						fc |= PARENB;
						fc |= CMSPAR;
						fc |= PARODD;
						break;
					case SerialPort.PARITY_SPACE:
						fc |= PARENB;
						fc |= CMSPAR;
						break;
					default:
						throw new UnsupportedCommOperationException("parity = " + parity);
				}

				// update the hardware

				fc &= ~CSIZE; /* Mask the character size bits */
				fc |= db; /* Set data bits */

				if (sb == 2)
					fc |= CSTOPB;
				else
					fc &= ~CSTOPB;

				m_Termios.c_cflag = fc;
				m_Termios.c_iflag = fi;

				if (tcsetattr(m_FD, TCSANOW, m_Termios) != 0)
					throw new UnsupportedCommOperationException("tcsetattr failed");

				// Even if termios(3) tells us that tcsetattr succeeds if any change
				// has been made, not necessary all of them  we cannot check them by reading back
				// and checking the result as not every driver/OS playes by the rules

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
				if (e instanceof PureJavaIllegalStateException) {
					throw e;
				} else {
					throw new PureJavaIllegalStateException(e);
				}
			}
		}
	}

	/**
	 * Gets the native file descriptor used by this port.
	 * <p>
	 * The file descriptor can be used in calls to JTermios functions. This
	 * maybe useful in extreme cases where performance is more important than
	 * convenience, for example using <code>JTermios.read(...)</code> instead of
	 * <code>SerialPort.getInputStream().read(...)</code>.
	 * <p>
	 * Note that mixing direct JTermios read/write calls with SerialPort stream
	 * read/write calls is at best fragile and likely to fail, which also
	 * implies that when using JTermios directly then configuring the port,
	 * especially termios.cc[VMIN] and termios.cc[VTIME] is the users
	 * responsibility.
	 * <p>
	 * Below is a sketch of minimum necessary to perform a read using raw
	 * JTermios functionality.
	 * 
	 * <pre>
	 * 		// import the JTermios functionality like this
	 * 		import jtermios.*;
	 * 		import static jtermios.JTermios.*;
	 * 
	 * 		SerialPort port = ...;
	 * 
	 * 		// cast the port to PureJavaSerialPort to get access to getNativeFileDescriptor
	 * 		int FD = ((PureJavaSerialPort) port).getNativeFileDescriptor();
	 * 
	 * 		// timeout and threshold values
	 * 		int messageLength = 25; // bytes
	 * 		int timeout = 200; // msec
	 * 
	 * 		// to initialize timeout and threshold first read current termios
	 * 		Termios termios = new Termios();
	 * 
	 * 		if (0 != tcgetattr(FD, termios))
	 * 			errorHandling();
	 * 
	 * 		// then set VTIME and VMIN, note VTIME in 1/10th of sec and both max 255
	 * 		termios.c_cc[VTIME] = (byte) ((timeout+99) / 100);
	 * 		termios.c_cc[VMIN] = (byte) messageLength;
	 * 
	 * 		// update termios
	 * 		if (0 != tcsetattr(FD, TCSANOW, termios))
	 * 			errorHandling();
	 * 
	 * 		...
	 * 		// allocate read buffer
	 * 		byte[] readBuffer = new byte[messageLength];
	 * 	...
	 * 
	 * 		// then perform raw read, not this may block indefinitely
	 * 		int n = read(FD, readBuffer, messageLength);
	 * 		if (n &lt; 0)
	 * 			errorHandling();
	 * </pre>
	 * 
	 * @return the native OS file descriptor as int
	 */
	public int getNativeFileDescriptor() {
		return m_FD;
	}

	@Override
	synchronized public OutputStream getOutputStream() throws IOException {
		checkState();
		if (m_OutputStream == null) {
			m_OutputStream = new OutputStream() {
				// im_ for inner class member
				private byte[] im_Buffer = new byte[2048];

				@Override
				final public void write(int b) throws IOException {
					checkState();
					byte[] buf = { (byte) b };
					write(buf, 0, 1);
				}

				@Override
				final public void write(byte[] buffer, int offset, int length) throws IOException {
					if (buffer == null)
						throw new IllegalArgumentException();
					if (offset < 0 || length < 0 || offset + length > buffer.length)
						throw new IndexOutOfBoundsException("buffer.lengt " + buffer.length + " offset " + offset + " length " + length);
					checkState();
					while (length > 0) {
						int n = buffer.length - offset;
						if (n > im_Buffer.length)
							n = im_Buffer.length;
						if (n > length)
							n = length;
						if (offset > 0) {
							System.arraycopy(buffer, offset, im_Buffer, 0, n);
							n = jtermios.JTermios.write(m_FD, im_Buffer, n);
						} else
							n = jtermios.JTermios.write(m_FD, buffer, n);

						if (n < 0) {
							PureJavaSerialPort.this.close();
							throw new IOException();
						}

						length -= n;
						offset += n;
					}
					m_OutputEmptyNotified = false;
				}

				@Override
				final public void write(byte[] b) throws IOException {
					write(b, 0, b.length);
				}

				@Override
				public void close() throws IOException {
					super.close();
				}

				@Override
				final public void flush() throws IOException {
					checkState();
					if (tcdrain(m_FD) < 0) {
						close();
						throw new IOException();
					}
				}
			};
		}
		return m_OutputStream;
	}

	synchronized public InputStream getInputStream() throws IOException {
		checkState();
		if (m_InputStream == null) {
			// NOTE: Windows and unixes are so different that it actually might
			// make sense to have the backend (ie JTermiosImpl) to provide
			// an InputStream that is optimal for the platform, instead of
			// trying to share of the InputStream logic here and force
			// Windows backend to conform to the the POSIX select()/
			// read()/vtim/vtime model. See the amount of code here
			// and in windows.JTermiosImpl for  select() and read().
			//
			m_InputStream = new InputStream() {
				// im_ for inner class members
				private int[] im_Available = { 0 };
				private byte[] im_Buffer = new byte[2048];
				// this stuff is just cached/precomputed stuff to make read() faster
				private int im_VTIME = -1;
				private int im_VMIN = -1;
				private final jtermios.Pollfd[] im_ReadPollFD = new Pollfd[] { new Pollfd(), new Pollfd() };
				private byte[] im_Nudge;
				private FDSet im_ReadFDSet;
				private TimeVal im_ReadTimeVal;
				private int im_PollFDn;
				private boolean im_ReceiveTimeoutEnabled;
				private int im_ReceiveTimeoutValue;
				private boolean im_ReceiveThresholdEnabled;
				private int im_ReceiveThresholdValue;
				private boolean im_PollingReadMode;
				private int im_ReceiveTimeoutVTIME;

				{ // initialized block instead of construct in anonymous class
					im_ReadFDSet = newFDSet();
					im_ReadTimeVal = new TimeVal();
					im_ReadPollFD[0].fd = m_FD;
					im_ReadPollFD[0].events = POLLIN;
					im_ReadPollFD[1].fd = m_PipeRdFD;
					im_ReadPollFD[1].events = POLLIN;
					im_PollFDn = m_HaveNudgePipe ? 2 : 1;
					im_Nudge = new byte[1];
				}

				@Override
				final public int available() throws IOException {
					if (m_FD < 0)
						return 0;
					checkState();
					if (ioctl(m_FD, FIONREAD, im_Available) < 0) {
						PureJavaSerialPort.this.close();
						System.out.println(Native.getLastError());
						throw new IOException();
					}
					return im_Available[0];
				}

				@Override
				final public int read() throws IOException {
					byte[] buf = { 0 };
					int n = read(buf, 0, 1);

					return n > 0 ? buf[0] & 0xFF : -1;
				}

				@Override
				public void close() throws IOException {
					super.close();
				}

				private void throwStreamClosedException() throws IOException {
					throw new IOException("Stream Closed");
				}

				@Override
				final public int read(byte[] buffer, int offset, int length) throws IOException {

					if (buffer == null)
						throw new IllegalArgumentException("buffer null");
					if (length == 0)
						return 0;
					if (offset < 0 || length < 0 || offset + length > buffer.length)
						throw new IndexOutOfBoundsException("buffer.length " + buffer.length + " offset " + offset + " length " + length);
					if (m_FD < 0)
						throwStreamClosedException();

					if (RAW_READ_MODE) {
						if (m_TimeoutThresholdChanged) { // does not need the lock if we just check the value
							synchronized (m_ThresholdTimeoutLock) {
								int vtime = m_ReceiveTimeoutEnabled ? m_ReceiveTimeoutVTIME : 0;
								int vmin = m_ReceiveThresholdEnabled ? m_ReceiveThresholdValue : 1;
								synchronized (m_Termios) {
									m_Termios.c_cc[VTIME] = (byte) vtime;
									m_Termios.c_cc[VMIN] = (byte) vmin;
									checkReturnCode(tcsetattr(m_FD, TCSANOW, m_Termios));
								}
								m_TimeoutThresholdChanged = false;
							}
						}
						int bytesRead;
						if (offset > 0) {
							if (length < im_Buffer.length)
								bytesRead = jtermios.JTermios.read(m_FD, im_Buffer, length);
							else
								bytesRead = jtermios.JTermios.read(m_FD, im_Buffer, im_Buffer.length);
							if (bytesRead > 0)
								System.arraycopy(im_Buffer, 0, buffer, offset, bytesRead);
						} else
							bytesRead = jtermios.JTermios.read(m_FD, buffer, length);
						m_DataAvailableNotified = false;
						return bytesRead;

					} // End of raw read mode code

					if (m_TimeoutThresholdChanged) { // does not need the lock if we just check the alue
						synchronized (m_ThresholdTimeoutLock) {
							// capture these here under guard so that we get a coherent picture of the settings
							im_ReceiveTimeoutEnabled = m_ReceiveTimeoutEnabled;
							im_ReceiveTimeoutValue = m_ReceiveTimeoutValue;
							im_ReceiveThresholdEnabled = m_ReceiveThresholdEnabled;
							im_ReceiveThresholdValue = m_ReceiveThresholdValue;
							im_PollingReadMode = m_PollingReadMode;
							im_ReceiveTimeoutVTIME = m_ReceiveTimeoutVTIME;
							m_TimeoutThresholdChanged = false;
						}
					}

					int bytesLeft = length;
					int bytesReceived = 0;
					int minBytesRequired;

					// Note for optimal performance: message length == receive threshold == read length <= 255
					// the best case execution path is marked with BEST below

					while (true) {
						// loops++;
						int vmin;
						int vtime;
						if (im_PollingReadMode) {
							minBytesRequired = 0;
							vmin = 0;
							vtime = 0;
						} else {
							if (im_ReceiveThresholdEnabled)
								minBytesRequired = im_ReceiveThresholdValue; // BEST
							else
								minBytesRequired = 1;
							if (minBytesRequired > bytesLeft) // in BEST case 'if' not taken
								minBytesRequired = bytesLeft;
							if (minBytesRequired <= 255)
								vmin = minBytesRequired; // BEST case
							else
								vmin = 255;

							// FIXME someone might change m_ReceiveTimeoutEnabled
							if (im_ReceiveTimeoutEnabled)
								vtime = im_ReceiveTimeoutVTIME; // BEST case
							else
								vtime = 0;
						}
						if (vmin != im_VMIN || vtime != im_VTIME) { // in BEST case 'if' not taken more than once for given InputStream instance
							// ioctls++;
							im_VMIN = vmin;
							im_VTIME = vtime;
							// This needs to be guarded with m_Termios so that these thing don't change on us
							synchronized (m_Termios) {
								m_Termios.c_cc[VTIME] = (byte) im_VTIME;
								m_Termios.c_cc[VMIN] = (byte) im_VMIN;
								checkReturnCode(tcsetattr(m_FD, TCSANOW, m_Termios));
							}
						}

						// Now wait for data to be available, except in raw read mode
						// and polling read modes. Following looks a bit longish
						// but  there is actually not that much code to be executed
						boolean dataAvailable = false;
						boolean timedout = false;
						if (!im_PollingReadMode) {
							int n;
							// long T0 = System.nanoTime();
							// do a select()/poll(), just in case this read was
							// called when no data is available
							// so that we will not hang for ever in a read
							int timeoutValue = im_ReceiveTimeoutEnabled ? im_ReceiveTimeoutValue : Integer.MAX_VALUE;
							if (USE_POLL) { // BEST case in Linux but not on
											// Windows or Mac OS X
								n = poll(im_ReadPollFD, im_PollFDn, timeoutValue);
								if (n < 0 || m_FD < 0) // the port closed while we were blocking in poll
									throwStreamClosedException();

								if ((im_ReadPollFD[1].revents & POLLIN) != 0)
									jtermios.JTermios.read(m_PipeRdFD, im_Nudge, 1);
								int re = im_ReadPollFD[0].revents;
								if ((re & POLLNVAL) != 0)
									throwStreamClosedException();
								dataAvailable = (re & POLLIN) != 0;

							} else { // this is a bit slower but then again it is unlikely
								// this gets executed in a low horsepower system
								FD_ZERO(im_ReadFDSet);
								FD_SET(m_FD, im_ReadFDSet);
								int maxFD = m_FD;
								if (m_HaveNudgePipe) {
									FD_SET(m_PipeRdFD, im_ReadFDSet);
									if (m_PipeRdFD > maxFD)
										maxFD = m_PipeRdFD;
								}
								if (timeoutValue >= 1000) {
									int t = timeoutValue / 1000;
									im_ReadTimeVal.tv_sec = t;
									im_ReadTimeVal.tv_usec = (timeoutValue - t * 1000) * 1000;
								} else {
									im_ReadTimeVal.tv_sec = 0;
									im_ReadTimeVal.tv_usec = timeoutValue * 1000;
								}
								n = select(maxFD + 1, im_ReadFDSet, null, null, im_ReadTimeVal);
								if (m_FD < 0) // the port closed while we were
									// blocking in select
									throwStreamClosedException();
								if (n < 0)
									throw new IOException(String.format("select() < 0 , errno()=%d",errno()));
								dataAvailable = FD_ISSET(m_FD, im_ReadFDSet);
							}
							if (n == 0 && m_ReceiveTimeoutEnabled)
								timedout = true;
						}

						if (timedout)
							break;

						// At this point data is either available or we take our
						// chances in raw mode or this polling read which can't block
						int bytesRead = 0;
						if (dataAvailable || im_PollingReadMode) {
							if (offset > 0) {
								if (bytesLeft < im_Buffer.length)
									bytesRead = jtermios.JTermios.read(m_FD, im_Buffer, bytesLeft);
								else
									bytesRead = jtermios.JTermios.read(m_FD, im_Buffer, im_Buffer.length);
								if (bytesRead > 0)
									System.arraycopy(im_Buffer, 0, buffer, offset, bytesRead);
							} else
								// this the BEST case execution path
								bytesRead = jtermios.JTermios.read(m_FD, buffer, bytesLeft);
							// readtime += System.nanoTime() - T0;
							if (bytesRead == 0)
								timedout = true;
						}

						// Now we have read data and try to return as quickly as
						// possibly or we have timed out.

						if (bytesRead < 0) // an error occured
							throw new IOException(String.format("read() < 0 , errno()=%d",errno()));

						bytesReceived += bytesRead;

						if (bytesReceived >= minBytesRequired) // BEST case this if is taken and we  exit
							break; // we have read the minimum required and will return that

						if (timedout)
							break;

						// Ok, looks like we are in for an other loop, so update
						// the offset
						// and loop for some more
						offset += bytesRead;
						bytesLeft -= bytesRead;
					}

					m_DataAvailableNotified = false;
					return bytesReceived;
				}

			};
		}
		return m_InputStream;
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
		return m_ReceiveThresholdValue;
	}

	@Override
	synchronized public int getReceiveTimeout() {
		checkState();
		return m_ReceiveTimeoutValue;
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
		return m_ReceiveThresholdEnabled;
	}

	@Override
	synchronized public boolean isReceiveTimeoutEnabled() {
		checkState();
		return m_ReceiveTimeoutEnabled;
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

	private void nudgePipe() {
		if (m_HaveNudgePipe)
			write(m_PipeWrFD, m_NudgeData, 1);
	}

	@Override
	synchronized public void close() {
		int fd = m_FD;
		if (fd != -1) {
			m_FD = -1;
			try {
				if (m_InputStream != null)
					m_InputStream.close();
			} catch (IOException e) {
				log = log && log(1, "m_InputStream.close threw an IOException %s\n", e.getMessage());
			} finally {
				m_InputStream = null;
			}
			try {
				if (m_OutputStream != null)
					m_OutputStream.close();
			} catch (IOException e) {
				log = log && log(1, "m_OutputStream.close threw an IOException %s\n", e.getMessage());
			} finally {
				m_OutputStream = null;
			}
			nudgePipe();
			int flags = fcntl(fd, F_GETFL, 0);
			flags |= O_NONBLOCK;
			int fcres = fcntl(fd, F_SETFL, flags);
			if (fcres != 0) // not much we can do if this fails, so just log it
				log = log && log(1, "fcntl(%d,%d,%d) returned %d\n", fd, F_SETFL, flags, fcres);

			if (m_Thread != null)
				m_Thread.interrupt();
			int err = jtermios.JTermios.close(fd);
			if (err < 0)
				log = log && log(1, "JTermios.close returned %d, errno %d\n", err, errno());

			if (m_HaveNudgePipe) {
				err = jtermios.JTermios.close(m_PipeRdFD);
				if (err < 0)
					log = log && log(1, "JTermios.close returned %d, errno %d\n", err, errno());
				err = jtermios.JTermios.close(m_PipeWrFD);
				if (err < 0)
					log = log && log(1, "JTermios.close returned %d, errno %d\n", err, errno());
			}
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

	/* package */PureJavaSerialPort(String name, int timeout) throws PortInUseException {
		super();

		boolean usepoll = false;
		if (JTermios.canPoll()) {
			String key1 = "purejavacomm.use_poll";
			String key2 = "purejavacomm.usepoll";
			if (System.getProperty(key1) != null) {
				usepoll = Boolean.getBoolean(key1);
				log = log && log(1, "use of '%s' is deprecated, use '%s' instead\n", key1, key2);
			} else if (System.getProperty(key2) != null)
				usepoll = Boolean.getBoolean(key2);
			else
				usepoll = true;
		}
		USE_POLL = usepoll;

		RAW_READ_MODE = Boolean.getBoolean("purejavacomm.rawreadmode");

		this.name = name;

		int tries = (timeout + 5) / 10;
		while ((m_FD = open(name, O_RDWR | O_NOCTTY | O_NONBLOCK)) < 0) {
			int errno = errno();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
			if (tries-- < 0)
				throw new PortInUseException("Unknown Application", errno);
		}

		m_MinVTIME = Integer.getInteger("purejavacomm.minvtime", 100);
		int flags = fcntl(m_FD, F_GETFL, 0);
		if (flags < 0)
			checkReturnCode(flags);
		flags &= ~O_NONBLOCK;
		checkReturnCode(fcntl(m_FD, F_SETFL, flags));

		m_BaudRate = 9600;
		m_DataBits = SerialPort.DATABITS_8;
		m_FlowControlMode = SerialPort.FLOWCONTROL_NONE;
		m_Parity = SerialPort.PARITY_NONE;
		m_StopBits = SerialPort.STOPBITS_1;

		checkReturnCode(tcgetattr(m_FD, m_Termios));

		cfmakeraw(m_FD, m_Termios);

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

		try {
			setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		} catch (UnsupportedCommOperationException e) {
			// This really should not happen
			e.printStackTrace();
		}

		int res = ioctl(m_FD, TIOCMGET, m_ioctl);
		if (res == 0)
			m_ControlLineStates = m_ioctl[0];
		else
			log = log && log(1, "ioctl(TIOCMGET) returned %d, errno %d\n", res, errno());

		String nudgekey = "purejavacomm.usenudgepipe";
		if (System.getProperty(nudgekey) == null || Boolean.getBoolean(nudgekey)) {
			int[] pipes = new int[2];
			if (pipe(pipes) == 0) {
				m_HaveNudgePipe = true;
				m_PipeRdFD = pipes[0];
				m_PipeWrFD = pipes[1];
				checkReturnCode(fcntl(m_PipeRdFD, F_SETFL, fcntl(m_PipeRdFD, F_GETFL, 0) | O_NONBLOCK));
			}
		}

		Runnable runnable = new Runnable() {
			public void run() {
				try {
					m_ThreadRunning = true;
					// see: http://daniel.haxx.se/docs/poll-vs-select.html
					final int TIMEOUT = Integer.getInteger("purejavacomm.pollperiod", 10);

					TimeVal timeout = null;
					FDSet rset = null;
					FDSet wset = null;
					jtermios.Pollfd[] pollfd = null;
					byte[] nudge = null;

					if (USE_POLL) {
						pollfd = new Pollfd[] { new Pollfd(), new Pollfd() };
						nudge = new byte[1];
						pollfd[0].fd = m_FD;
						pollfd[1].fd = m_PipeRdFD;
					} else {
						rset = newFDSet();
						wset = newFDSet();
						timeout = new TimeVal();
						int t = TIMEOUT * 1000;
						timeout.tv_sec = t / 1000000;
						timeout.tv_usec = t - timeout.tv_sec * 1000000;
					}

					while (m_FD >= 0) {
						boolean read = (m_NotifyOnDataAvailable && !m_DataAvailableNotified);
						boolean write = (m_NotifyOnOutputEmpty && !m_OutputEmptyNotified);
						int n = 0;

						boolean pollCtrlLines = m_NotifyOnCTS || m_NotifyOnDSR || m_NotifyOnRI || m_NotifyOnCD;

						if (read || write || (!pollCtrlLines && m_HaveNudgePipe)) {
							if (USE_POLL) {
								short e = 0;
								if (read)
									e |= POLLIN;
								if (write)
									e |= POLLOUT;
								pollfd[0].events = e;
								pollfd[1].events = POLLIN;
								if (m_HaveNudgePipe)
									n = poll(pollfd, 2, TIMEOUT);
								else
									n = poll(pollfd, 1, TIMEOUT);

								int re = pollfd[1].revents;

								if ((re & POLLNVAL) != 0) {
									log = log && log(1, "poll() returned POLLNVAL, errno %d\n", errno());
									break;
								}

								if ((re & POLLIN) != 0)
									read(m_PipeRdFD, nudge, 1);

								re = pollfd[0].revents;
								if ((re & POLLNVAL) != 0) {
									log = log && log(1, "poll() returned POLLNVAL, errno %d\n", errno());
									break;
								}
								read = read && (re & POLLIN) != 0;
								write = write && (re & POLLOUT) != 0;
							} else {
								FD_ZERO(rset);
								FD_ZERO(wset);
								if (read)
									FD_SET(m_FD, rset);
								if (write)
									FD_SET(m_FD, wset);
								if (m_HaveNudgePipe)
									FD_SET(m_PipeRdFD, rset);
								n = select(m_FD + 1, rset, wset, null, m_HaveNudgePipe ? null : timeout);
								read = read && FD_ISSET(m_FD, rset);
								write = write && FD_ISSET(m_FD, wset);
							}

							if (m_FD < 0)
								break;
							if (n < 0) {
								log = log && log(1, "select() or poll() returned %d, errno %d\n", n, errno());
								close();
								break;
							}
						} else {
							Thread.sleep(TIMEOUT);
						}

						if (m_EventListener != null) {
							if (read || write)
								sendDataEvents(read, write);
							if (pollCtrlLines)
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
			throw new PureJavaIllegalStateException("ioctl(m_FD, TIOCMGET, m_ioctl) == -1");

		m_ControlLineStates = (m_ioctl[0] & line) + (m_ControlLineStates & ~line);
	}

	synchronized private boolean getControlLineState(int line) {
		checkState();
		if (ioctl(m_FD, TIOCMGET, m_ioctl) == -1)
			throw new PureJavaIllegalStateException("ioctl(m_FD, TIOCMGET, m_ioctl) == -1");
		return (m_ioctl[0] & line) != 0;
	}

	synchronized private void setControlLineState(int line, boolean state) {
		checkState();
		if (ioctl(m_FD, TIOCMGET, m_ioctl) == -1)
			throw new PureJavaIllegalStateException("ioctl(m_FD, TIOCMGET, m_ioctl) == -1");

		if (state)
			m_ioctl[0] |= line;
		else
			m_ioctl[0] &= ~line;
		if (ioctl(m_FD, TIOCMSET, m_ioctl) == -1)
			throw new PureJavaIllegalStateException("ioctl(m_FD, TIOCMSET, m_ioctl) == -1");
	}

	private void failWithIllegalStateException() {
		throw new PureJavaIllegalStateException("File descriptor is " + m_FD + " < 0, maybe closed by previous error condition");
	}

	private void checkState() {
		if (m_FD < 0)
			failWithIllegalStateException();
	}

	private void checkReturnCode(int code) {
		if (code != 0) {
			String msg = String.format("JTermios call returned %d at %s", code, lineno(1));
			log = log && log(1, "%s\n", msg);
			try {
				close();
			} catch (Exception e) {
				StackTraceElement st = e.getStackTrace()[0];
				String msg2 = String.format("close threw %s at class %s line% d", e.getClass().getName(), st.getClassName(), st.getLineNumber());
				log = log && log(1, "%s\n", msg2);
			}
			throw new PureJavaIllegalStateException(msg);
		}
	}

	/**
	 * This is not part of the PureJavaComm API, this is purely for testing, do
	 * not depend on this
	 */
	public boolean isInternalThreadRunning() {
		return m_ThreadRunning;
	}

}
