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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class CommPort {
	protected String name;

	/**
	 * Closes the communications port.
	 */
	public void close() {
		CommPortIdentifier.close(this);
	}

	/**
	 * Disables receive framing.
	 */
	public abstract void disableReceiveFraming();

	/**
	 * Disables receive threshold.
	 */
	public abstract void disableReceiveThreshold();

	/**
	 * Disables receive timeout.
	 */
	public abstract void disableReceiveTimeout();

	/**
	 * Enables receive framing.
	 * 
	 * @param framingByte
	 * @throws UnsupportedCommOperationException
	 */
	public abstract void enableReceiveFraming(int framingByte) throws UnsupportedCommOperationException;

	/**
	 * Enables receive threshold.
	 * 
	 * @param threshold
	 * @throws UnsupportedCommOperationException
	 */
	public abstract void enableReceiveThreshold(int threshold) throws UnsupportedCommOperationException;

	/**
	 * Enables receive timeout.
	 * 
	 * @param rcvTimeout
	 *            Timeout value in milliseconds
	 * @throws UnsupportedCommOperationException
	 */
	public abstract void enableReceiveTimeout(int rcvTimeout) throws UnsupportedCommOperationException;

	/**
	 * Returns the input buffer size in bytes.
	 * 
	 * @return The input buffer size in bytes.
	 */
	public abstract int getInputBufferSize();

	/**
	 * Returns an input stream.
	 * 
	 * @return An input stream, or <code>null</code> if the port is
	 *         unidirectional and doesn't support receiving data.
	 * @throws IOException
	 */
	public abstract InputStream getInputStream() throws IOException;

	/**
	 * Returns the port name.
	 * 
	 * @return The port name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the output buffer size in bytes.
	 * 
	 * @return The output buffer size in bytes.
	 */
	public abstract int getOutputBufferSize();

	/**
	 * Returns an output stream. 
	 * 
	 * @return An output stream, or <code>null</code> if the port is
	 *         unidirectional and doesn't support sending data.
	 * @throws IOException
	 */
	public abstract OutputStream getOutputStream() throws IOException;

	/**
	 * Returns the current byte used for receive framing.
	 * 
	 * @return The current byte used for receive framing.
	 */
	public abstract int getReceiveFramingByte();

	/**
	 * Returns the integer value of the receive threshold.
	 * 
	 * @return The integer value of the receive threshold.
	 */
	public abstract int getReceiveThreshold();

	/**
	 * Returns the integer value of the receive timeout. If the receive timeout
	 * is disabled, then the value returned is meaningless.
	 * 
	 * @return The integer value of the receive timeout.
	 */
	public abstract int getReceiveTimeout();

	/**
	 * Returns <code>true</code> if receive framing is enabled.
	 * 
	 * @return <code>true</code> if receive framing is enabled.
	 */
	public abstract boolean isReceiveFramingEnabled();

	/**
	 * Returns <code>true</code> if receive threshold is enabled.
	 * 
	 * @return <code>true</code> if receive threshold is enabled.
	 */
	public abstract boolean isReceiveThresholdEnabled();

	/**
	 * Returns <code>true</code> if receive timeout is enabled.
	 * 
	 * @return <code>true</code> if receive timeout is enabled.
	 */
	public abstract boolean isReceiveTimeoutEnabled();

	/**
	 * Reads data into the <code>buffer</code> byte array if any bytes are
	 * available and returns the number of bytes read.
	 * 
	 * @param buffer
	 * @param offset
	 * @param length
	 * @return The number of bytes read.
	 * @throws IOException
	 */
	/**
	 * Sets the input buffer size.
	 * 
	 * @param size
	 */
	public abstract void setInputBufferSize(int size);

	/**
	 * Sets the output buffer size.
	 * 
	 * @param size
	 */
	public abstract void setOutputBufferSize(int size);
}