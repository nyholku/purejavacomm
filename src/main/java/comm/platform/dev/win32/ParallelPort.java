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
package comm.platform.dev.win32;

import comm.PortType;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
class ParallelPort extends comm.platform.dev.ParallelPort {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Init">
	public ParallelPort(String name, String title, String description, PortType portType) {
		super();
		this.name = name;
		this.title = title;
		this.portType = portType;
		this.description = description;
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Getters">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Update">
	public void update(String name, String title, String description, PortType portType) {
		this.name = name;
		this.title = title;
		this.description = description;
		this.portType = portType;
	}
	//</editor-fold>
	
	@Override
	public boolean open(int inputBufferSize, int outputBufferSize) {
		return false;
	}
	
	@Override
	public boolean configureSystemParallelPort() {
		return true;
	}
	
	@Override
	public boolean close() {
		return true;
	}
}
