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

import com.sun.jna.Platform;

import jtermios.*;

import static jtermios.JTermios.JTermiosLogging.*;

import static jtermios.JTermios.*;
import com.sun.jna.Platform;

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
		m_ReceiveThresholdEnabled = false;
	}

	@Override
	synchronized public void disableReceiveTimeout() {
		checkState();
		m_ReceiveTimeOutEnabled = false;
	}

	@Override
	synchronized public void enableReceiveFraming(int arg0) throws UnsupportedCommOperationException {
		checkState();
		throw new UnsupportedCommOperationException();
	}

	@Override
	synchronized public void enableReceiveThreshold(int value) throws UnsupportedCommOperationException {
		checkState();
		if (m_ReceiveThresholdValue < 0)
			throw new IllegalArgumentException("threshold" + value + " < 0 ");
		m_ReceiveThresholdEnabled = true;
		m_ReceiveThresholdValue = value;
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
		if (m_ReceiveThresholdValue < 0)
			throw new IllegalArgumentException("threshold" + value + " < 0 ");
		checkState();
		m_ReceiveTimeOutEnabled = true;
		m_ReceiveTimeOutValue = value;
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
					case SerialPort.STOPBITS_2:
						sb = 2;
						break;
					default:
						throw new UnsupportedCommOperationException("stopBits = " + stopBits);
				}

				int fi = m_Termios.c_iflag;
				int fc = m_Termios.c_cflag;
				switch (parity) {
					case SerialPort.PARITY_NONE:
						fc &= ~PARENB;
						fi &= ~(INPCK | ISTRIP);
						break;
					case SerialPort.PARITY_EVEN:
						fc |= PARENB;
						fc &= ~PARODD;
						fi &= ~(INPCK | ISTRIP);
						break;
					case SerialPort.PARITY_ODD:
						fc |= PARENB;
						fc |= PARODD;
						fi &= ~(INPCK | ISTRIP);
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

				checkReturnCode(tcsetattr(m_FD, TCSANOW, m_Termios));

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
	}

	private static int min(int a, int b) {
		return a < b ? a : b;
	}

	private static int max(int a, int b) {
		return a > b ? a : b;
	}

	@Override
	synchronized public OutputStream getOutputStream() throws IOException {
		checkState();
		if (m_OutputStream == null) {
			m_OutputStream = new OutputStream() {
				private byte[] m_Buffer = new byte[2048];

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
						int n = min(length, min(m_Buffer.length, buffer.length - offset));
						if (offset > 0) {
							System.arraycopy(buffer, offset, m_Buffer, 0, n);
							n = jtermios.JTermios.write(m_FD, m_Buffer, n);
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
			m_InputStream = new InputStream() {
				private int[] m_Available = { 0 };
				private byte[] m_Buffer = new byte[2048];
				private int m_VTIME = 0;
				private int m_VMIN = 0;
				private int[] m_ReadPollFD;
				private byte[] m_Nudge;
				private FDSet m_ReadFDSet;
				private TimeVal m_ReadTimeVal;

				{ // initialized block instead of construct in anonymous class
					m_ReadFDSet = newFDSet();
					m_ReadTimeVal = new TimeVal();
					m_ReadPollFD = new int[4];
					m_Nudge = new byte[1];
				}

				@Override
				final public int available() throws IOException {
					checkState();
					if (ioctl(m_FD, FIONREAD, m_Available) < 0) {
						PureJavaSerialPort.this.close();
						throw new IOException();
					}
					return m_Available[0];
				}

				@Override
				final public int read() throws IOException {
					checkState();
					byte[] buf = { 0 };
					int n = read(buf, 0, 1);

					return n > 0 ? buf[0] & 0xFF : -1;
				}

				//THINGS TO TEST:
				//-breakout from blocking read even without timeout

				@Override
				final public int read(byte[] buffer, int offset, int length) throws IOException {
					synchronized (m_Termios) {
						if (buffer == null)
							throw new IllegalArgumentException("buffer null");
						if (offset < 0 || length < 0 || offset + length > buffer.length)
							throw new IndexOutOfBoundsException("buffer.lengt " + buffer.length + " offset " + offset + " length " + length);
						if (length == 0)
							return 0;

						checkState();

						// Now configure VTIME and VMIN

						int bytesReceived = 0;
						long T0 = m_ReceiveTimeOutEnabled ? System.currentTimeMillis() : 0;
						long T1 = T0;
						while (true) {
							int bytesLeft = length - bytesReceived;
							int timeLeft = 0;

							int vtime;
							int vmin;
							boolean pollingRead = (m_ReceiveTimeOutEnabled && m_ReceiveTimeOutValue == 0) || (m_ReceiveThresholdEnabled && m_ReceiveThresholdValue == 0);
							if (pollingRead) {
								// This is Kusti's interpretation of the JavaComm javadoc for getInputStream()
								vtime = 0;
								vmin = 0;
							} else {
								if (RAW_READ_MODE) {
									vtime = min(255, ((m_ReceiveTimeOutEnabled ? m_ReceiveTimeOutValue : 0) + 99) / 100);
									vmin = min(255, m_ReceiveThresholdEnabled ? m_ReceiveThresholdValue : 1);
								} else {
									// calculate VTIME value
									// MAX_VALUE ???? minVTIME
									timeLeft = m_ReceiveTimeOutEnabled ? max(0, m_ReceiveTimeOutValue - ((int) (T1 - T0))) : Integer.MAX_VALUE;
									vtime = m_ReceiveTimeOutEnabled ? timeLeft : m_MinVTIME;
									// roundup to 1/10th of sec
									vtime = (vtime + 99) / 100;
									// if overflow divide timeout to equal slices
									if (vtime > 255)
										vtime = vtime / (vtime / 256 + 1);

									// calculate VMIN value
									vmin = min(255, min(bytesLeft, m_ReceiveThresholdEnabled ? m_ReceiveThresholdValue : 1));
								}
							}
							// to avoid unnecessary calls to OS we only call tcsetattr() if necessary
							if (vtime != m_VTIME || vmin != m_VMIN) {
								m_VTIME = vtime;
								m_VMIN = vmin;
								m_Termios.c_cc[VTIME] = (byte) m_VTIME;
								m_Termios.c_cc[VMIN] = (byte) m_VMIN;
								checkReturnCode(tcsetattr(m_FD, TCSANOW, m_Termios));
							}

							int bytesRead = 0;

							boolean dataAvailable = false;
							if (!RAW_READ_MODE && !pollingRead) {
								// do a select()/poll(), just in case this read was
								// called when no data is available
								// so that we will not hang for ever in a read
								if (USE_POLL) {
									m_ReadPollFD[0] = m_FD;
									m_ReadPollFD[1] = POLLIN_IN;
									m_ReadPollFD[2] = m_PipeRdFD;
									m_ReadPollFD[3] = POLLIN_IN;
									int n;
									if (m_HaveNudgePipe)
										n = poll(m_ReadPollFD, 2, timeLeft);
									else
										n = poll(m_ReadPollFD, 1, timeLeft);
									if (n < 0 || m_FD < 0) // the port closed while we were blocking in poll
										throw new IOException();
									// FIXME is there a raise condition with the internal thread doing poll
									if ((m_ReadPollFD[3] & POLLIN_OUT) != 0)
										jtermios.JTermios.read(m_PipeRdFD, m_Nudge, 1);

									int re = m_ReadPollFD[1];
									if ((re & POLLNVAL_OUT) != 0)
										throw new IOException();
									dataAvailable = (re & POLLIN_OUT) != 0;

								} else {

									FD_ZERO(m_ReadFDSet);
									FD_SET(m_FD, m_ReadFDSet);
									int maxFD = m_FD;
									if (m_HaveNudgePipe) {
										FD_SET(m_PipeRdFD, m_ReadFDSet);
										if (m_PipeRdFD > maxFD)
											maxFD = m_PipeRdFD;
									}
									if (timeLeft >= 1000) {
										int t = timeLeft / 1000;
										m_ReadTimeVal.tv_sec = t;
										m_ReadTimeVal.tv_usec = (timeLeft - t * 1000) * 1000;
									} else {
										m_ReadTimeVal.tv_sec = 0;
										m_ReadTimeVal.tv_usec = timeLeft * 1000;
									}
									//System.err.println(maxFD + " " + m_PipeRdFD + " " + m_FD);
									if (select(maxFD + 1, m_ReadFDSet, null, null, m_ReadTimeVal) < 0)
										throw new IOException();
									if (m_FD < 0) // the port closed while we were blocking in select
										throw new IOException();
									dataAvailable = FD_ISSET(m_FD, m_ReadFDSet);
								}
							}

							// at this point data is either available or we take our chances in raw mode
							if (RAW_READ_MODE || pollingRead || dataAvailable) {
								if (offset > 0) {
									bytesRead = jtermios.JTermios.read(m_FD, m_Buffer, bytesLeft);
									if (bytesRead > 0)
										System.arraycopy(m_Buffer, 0, buffer, offset, bytesRead);
								} else
									bytesRead = jtermios.JTermios.read(m_FD, buffer, bytesLeft);
							}

							if (bytesRead < 0)
								throw new IOException();

							bytesReceived += bytesRead;
							offset += bytesRead;
							if (pollingRead)
								break;
							if (!m_ReceiveThresholdEnabled && bytesReceived > 0)
								break;
							if (m_ReceiveThresholdEnabled && bytesReceived >= min(m_ReceiveThresholdValue, length))
								break;
							T1 = m_ReceiveTimeOutEnabled ? System.currentTimeMillis() : 0;
							if (m_ReceiveTimeOutEnabled && T1 - T0 >= m_ReceiveTimeOutValue)
								break;
						}

						m_DataAvailableNotified = false;
						return bytesReceived;
					}
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
		return m_ReceiveThresholdEnabled;
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

	private void nudgePipe() {
		if (m_HaveNudgePipe)
			write(m_PipeWrFD, m_NudgeData, 1);
	}

	@Override
	synchronized public void close() {
		int fd = m_FD;
		if (fd != -1) {
			m_FD = -1;
			nudgePipe();
			int flags = fcntl(fd, F_GETFL, 0);
			flags |= O_NONBLOCK;
			int fcres = fcntl(fd, F_SETFL, flags);
			if (fcres != 0) // not much we can do if this fails, so just log it
				log = log && log(1, "fcntl(%d,%d,%d) returned %d\n", m_FD, F_SETFL, flags, fcres);

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
		if (Platform.isLinux()) {
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

		// unbelievable, sometimes quickly closing and re-opening fails on Windows
		// so try a few times
		int tries = 100;
		long T0 = System.currentTimeMillis();
		while ((m_FD = open(name, O_RDWR | O_NOCTTY | O_NONBLOCK)) < 0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
			if (tries-- < 0 || System.currentTimeMillis() - T0 >= timeout)
				throw new PortInUseException();
		}

		m_MinVTIME = Integer.getInteger("purejavacomm.minvtime", 100);
		int flags = fcntl(m_FD, F_GETFL, 0);
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

		checkReturnCode(ioctl(m_FD, TIOCMGET, m_ioctl));
		m_ControlLineStates = m_ioctl[0];

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
					int[] pollfd = null;
					byte[] nudge = null;

					if (USE_POLL) {
						pollfd = new int[4];
						nudge = new byte[1];
						pollfd[0] = m_FD;
						pollfd[2] = m_PipeRdFD;
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
								int e = 0;
								if (read)
									e |= POLLIN_IN;
								if (write)
									e |= POLLOUT_IN;
								pollfd[1] = e;
								pollfd[3] = POLLIN_IN;
								if (m_HaveNudgePipe)
									n = poll(pollfd, 2, -1);
								else
									n = poll(pollfd, 1, TIMEOUT);

								int re = pollfd[3];

								if ((re & POLLNVAL_OUT) != 0) {
									log = log && log(1, "poll() returned POLLNVAL, errno %d\n", errno());
									break;
								}

								if ((re & POLLIN_OUT) != 0)
									read(m_PipeRdFD, nudge, 1);

								re = pollfd[1];
								if ((re & POLLNVAL_OUT) != 0) {
									log = log && log(1, "poll() returned POLLNVAL, errno %d\n", errno());
									break;
								}
								read = read && (re & POLLIN_OUT) != 0;
								write = write && (re & POLLOUT_OUT) != 0;
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

							if (m_FD < 0 || Thread.currentThread().isInterrupted())
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

	private void checkState() {
		if (m_FD < 0)
			throw new IllegalStateException("File descriptor is " + m_FD + " < 0, maybe closed by previous error condition");
	}

	private void checkReturnCode(int code) {
		if (code != 0) {
			String msg = String.format("JTermios call returned %d at %s", code, lineno(1)); // 1 qas implicit 0
			log = log && log(1, "%s\n", msg);
			try {
				close();
			} catch (Exception e) {
				StackTraceElement st = e.getStackTrace()[0];
				String msg2 = String.format("close threw %s at class %s line% d", e.getClass().getName(), st.getClassName(), st.getLineNumber());
				log = log && log(1, "%s\n", msg2);
			}
			throw new IllegalStateException(msg);
		}
	}

}
