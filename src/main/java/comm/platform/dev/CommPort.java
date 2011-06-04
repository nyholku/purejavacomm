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
package comm.platform.dev;

import comm.ICommPort;
import comm.PortType;
import comm.util.StringUtil;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class CommPort extends DisposableObject implements ICommPort {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected final Object commLock = new Object();
	protected String name, title, description, owner;
	protected boolean opened, owned, available;
	protected PortType portType;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Init">
	protected CommPort(String name, String title, String description, PortType portType) {
		init(name, title, description, portType);
	}
	
	protected CommPort() {
		init(StringUtil.empty, StringUtil.empty, StringUtil.empty, PortType.UNKNOWN);
	}
	
	private void init(String name, String title, String description, PortType portType) {
		this.name = name;
		this.title = title;
		this.portType = portType;
		this.description = description;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	@Override
	public final Object getLock() {
		return commLock;
	}
	
	@Override
	public final String getName() {
		return name;
	}
	
	@Override
	public final String getTitle() {
		return title;
	}
	
	@Override
	public final String getDescription() {
		return description;
	}
	
	@Override
	public final boolean isOpen() {
		return opened;
	}
	
	@Override
	public final boolean isOwned() {
		return owned;
	}
	
	@Override
	public final String getOwner() {
		return owner;
	}
	
	@Override
	public final boolean isAvailable() {
		return available;
	}
	
	@Override
	public final PortType getPortType() {
		return portType;
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Helper Methods">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	@Override
	public final boolean open() {
		return open(DEFAULT_INPUT_BUFFER_SIZE, DEFAULT_OUTPUT_BUFFER_SIZE);
	}
	//</editor-fold>
}
