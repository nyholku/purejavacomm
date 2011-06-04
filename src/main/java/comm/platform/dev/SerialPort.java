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

import comm.DataBits;
import comm.FlowControl;
import comm.ISerialPort;
import comm.Parity;
import comm.PortType;
import comm.StopBits;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class SerialPort extends CommPort implements ISerialPort {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected int baudRate;
	protected DataBits dataBits;
	protected StopBits stopBits;
	protected Parity parity;
	protected FlowControl[] flowControl;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Init">
	protected SerialPort(String name, String title, String description, PortType portType) {
		super(name, title, description, portType);
		init();
	}
	
	protected SerialPort() {
		super();
		init();
	}
	
	private void init() {
		//Use the system default values when setting these options.
		this.parity = comm.SerialPort.getSystemDefaultParity();
		this.baudRate = comm.SerialPort.getSystemDefaultBaudRate();
		this.dataBits = comm.SerialPort.getSystemDefaultDataBits();
		this.stopBits = comm.SerialPort.getSystemDefaultStopBits();
		this.flowControl = comm.SerialPort.getSystemDefaultFlowControl();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	@Override
	public final int getBaudRate() {
		return baudRate;
	}
	
	@Override
	public final DataBits getDataBits() {
		return dataBits;
	}
	
	@Override
	public final StopBits getStopBits() {
		return stopBits;
	}
	
	@Override
	public final Parity getParity() {
		return parity;
	}
	
	@Override
	public final FlowControl[] getFlowControl() {
		return flowControl;
	}
	
	@Override
	public final int getFlowControlFlag() {
		return FlowControl.asFlag(flowControl);
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Helper Methods">
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	@Override
	public final boolean changeFlowControl(int flag) {
		return changeFlowControl(FlowControl.fromFlag(flag));
	}
	
	@Override
	public final boolean changeFlowControl(FlowControl...flowControl) {
		synchronized(commLock) {
			if (!opened) {
				this.flowControl = flowControl;
				return true;
			}
			
			if (changeSystemFlowControl(flowControl)) {
				this.flowControl = flowControl;
				return true;
			}
		}
		return false;
	}
	
	@Override
	public final boolean updateConfiguration() {
		synchronized(commLock) {
			return configureSystemSerialPort(baudRate, dataBits, stopBits, parity);
		}
	}
	
	@Override
	public final boolean configure(IConfiguration config) {
		if (config == null)
			return false;
		return configure(config.getBaudRate(), config.getDataBits(), config.getStopBits(), config.getParity());
	}

	@Override
	public final boolean configure(int baudRate, int dataBits, int stopBits, int parity) {
		return configure(baudRate, DataBits.fromValue(dataBits), StopBits.fromValue(stopBits), Parity.fromValue(parity));
	}
	
	@Override
	public final boolean configure(int baudRate, DataBits dataBits, StopBits stopBits, Parity parity) {
		synchronized(commLock) {
			if (!opened) {
				this.baudRate = baudRate;
				this.dataBits = dataBits;
				this.stopBits = stopBits;
				this.parity = parity;
				return true;
			}
			
			if (configureSystemSerialPort(baudRate, dataBits, stopBits, parity)) {
				this.baudRate = baudRate;
				this.dataBits = dataBits;
				this.stopBits = stopBits;
				this.parity = parity;
				return true;
			}
		}
		return false;
	}
	//</editor-fold>

	protected abstract boolean configureSystemSerialPort(int baudRate, DataBits dataBits, StopBits stopBits, Parity parity);
	protected abstract boolean changeSystemFlowControl(FlowControl...flowControl);
}
