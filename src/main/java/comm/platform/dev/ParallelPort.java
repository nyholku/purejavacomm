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

import comm.IParallelPort;
import comm.PortType;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class ParallelPort extends CommPort implements IParallelPort {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Init">
	protected ParallelPort(String name, String title, String description, PortType portType) {
		super(name, title, description, portType);
		init();
	}
	
	protected ParallelPort() {
		super();
		init();
	}
	
	private void init() {
		//Use the system default values when setting these options.
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Helper Methods">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	@Override
	public final boolean updateConfiguration() {
		synchronized(commLock) {
			return configureSystemParallelPort(/*Use local variables*/);
		}
	}
	
	@Override
	public final boolean configure(IConfiguration config) {
		if (config == null)
			return false;
		return configure();
	}
	
	@Override
	public final boolean configure() {
		synchronized(commLock) {
			if (!opened) {
				/*Just update local variables and return.*/
				return true;
			}
			
			if (configureSystemParallelPort()) {
				/*Update local variables.*/
				return true;
			}
		}
		return false;
	}
	//</editor-fold>

	protected abstract boolean configureSystemParallelPort();
}
