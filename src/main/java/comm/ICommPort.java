/*
 * Copyright (c) 2011 David Hoyt
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
 * The names of any contributors may not be used to endorse or promote products derived 
 * from this software without specific prior written permission.
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
package comm;

/**
 * Provides generic access to system communication ports.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface ICommPort extends IDisposable {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	/**
	 * Describes a state where no communication ports are available on the system.
	 */
	public static final ICommPort[] EMPTY = { };
	
	public static final int 
		  DEFAULT_INPUT_BUFFER_SIZE     = 2048
		, DEFAULT_OUTPUT_BUFFER_SIZE    = 2048
	;
	
	public static final int 
		  DEFAULT_THREAD_POOL_SIZE = Math.max(1, Runtime.getRuntime().availableProcessors())
	;
	//</editor-fold>
	
	String getName();
	String getTitle();
	String getDescription();
	boolean isOpen();
	boolean isOwned();
	String getOwner();
	boolean isAvailable();
	PortType getPortType();
	Object getLock();
	
	boolean open(int inputBufferSize, int outputBufferSize);
	boolean open();
	boolean updateConfiguration();
	boolean close();
}
