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

// FIXME no mechanism to warn about duplicate port names
import static jtermios.JTermios.*;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CommPortIdentifier {
	public static final int PORT_SERIAL = 1;
	public static final int PORT_PARALLEL = 2;
	private static volatile Object m_Mutex = new Object();
	private volatile String m_PortName;
	private volatile int m_PortType;
	private volatile CommPort m_CommPort;
	private volatile CommDriver m_Driver;
	private volatile static Hashtable<String, CommPortIdentifier> m_PortIdentifiers = new Hashtable<String, CommPortIdentifier>();
	private volatile static Hashtable<CommPort, CommPortIdentifier> m_OpenPorts = new Hashtable<CommPort, CommPortIdentifier>();
	private volatile static Hashtable<CommPortIdentifier, String> m_Owners = new Hashtable<CommPortIdentifier, String>();
	private volatile Hashtable<CommPortIdentifier, List<CommPortOwnershipListener>> m_OwnerShipListeners = new Hashtable<CommPortIdentifier, List<CommPortOwnershipListener>>();

	/**
	 * This has not been tested at all
	 */
	public static void addPortName(String portName, int portType, CommDriver driver) {
		synchronized (m_Mutex) {
			m_PortIdentifiers.put(portName, new CommPortIdentifier(portName, portType, driver));
		}
	}

	private CommPortIdentifier(String name, int portType, CommDriver driver) {
		m_PortName = name;
		m_PortType = portType;
		m_Driver = driver;
	}

	public static CommPortIdentifier getPortIdentifier(String portName) throws NoSuchPortException {
		synchronized (m_Mutex) {
			boolean ENUMERATE = false;
			for (CommPortIdentifier portid : m_OpenPorts.values())
				if (portid.getName().equals(portName))
					return portid;
			if (ENUMERATE) { // enumerating ports takes time, lets see if we can avoid it
				Enumeration e = getPortIdentifiers();
				while (e.hasMoreElements()) {
					CommPortIdentifier portid = (CommPortIdentifier) e.nextElement();
					if (portid.getName().equals(portName))
						return portid;
				}
			} else {
				CommPortIdentifier portid = m_PortIdentifiers.get(portName);
				if (portid != null)
					return portid;

				// check if we can open a port with the given name
				int fd = jtermios.JTermios.open(portName, O_RDWR | O_NOCTTY | O_NONBLOCK);
				if (fd != -1) { // yep, it exists, so close it and create a port id object for it
					// FIXME this does not detect if the port name is invalid device name
					// but is valid and existing filename
					jtermios.JTermios.close(fd);
					return new CommPortIdentifier(portName, PORT_SERIAL, null);
				}
				if (errno() == EBUSY) // exists but busy, cannot throw anything here, so return a port if for it
					return new CommPortIdentifier(portName, PORT_SERIAL, null);
			}
			throw new NoSuchPortException();
		}
	}

	public static CommPortIdentifier getPortIdentifier(CommPort port) throws NoSuchPortException {
		synchronized (m_Mutex) {
			CommPortIdentifier portid = m_OpenPorts.get(port);
			if (portid == null)
				throw new NoSuchPortException();
			return portid;
		}
	}

	public CommPort open(String appname, int timeout) throws PortInUseException {
		synchronized (m_Mutex) {
			long t0 = System.currentTimeMillis();

			String owner = m_Owners.get(m_PortName);
			if (owner != null) {
				fireOwnershipEvent(CommPortOwnershipListener.PORT_OWNERSHIP_REQUESTED);
				try {
					while (System.currentTimeMillis() - t0 < timeout) {
						m_Mutex.wait(5);
						if (!isCurrentlyOwned())
							break;
					}
				} catch (InterruptedException ie) {
					// can't throw it, can't swallow it, try to propagate it
					Thread.currentThread().interrupt();
				}
			}
			if (isCurrentlyOwned())
				throw new PortInUseException();

			CommPortIdentifier info = m_PortIdentifiers.get(m_PortName);
			CommPort port;
			if (info != null)
				port = info.m_Driver.getCommPort(m_PortName, info.m_PortType);
			else
				port = new PureJavaSerialPort(m_PortName, timeout); // FIXME timeout here is not used
			m_OpenPorts.put(port, this);
			m_Owners.put(this, appname);
			fireOwnershipEvent(CommPortOwnershipListener.PORT_OWNED);
			return port;
		}
	}

	/* package */
	static void close(CommPort port) {
		synchronized (m_Mutex) {
			m_Owners.remove(port.name);
			CommPortIdentifier portid = m_OpenPorts.remove(port);
			if (portid != null)
				portid.fireOwnershipEvent(CommPortOwnershipListener.PORT_UNOWNED);
		}
	}

	public CommPort open(java.io.FileDescriptor fd) throws UnsupportedCommOperationException {
		throw new UnsupportedCommOperationException();
	}

	public String getName() {
		return m_PortName;
	}

	public int getPortType() {
		return m_PortType;
	}

	public static Enumeration getPortIdentifiers() {
		synchronized (m_Mutex) {

			return new Enumeration() {
				List<CommPortIdentifier> m_PortIDs;
				{ // insert the  'addPortName' ports to the dynamic port list
					m_PortIDs = new LinkedList<CommPortIdentifier>();
					for (CommPortIdentifier portid : m_PortIdentifiers.values())
						m_PortIDs.add(portid);
					// and now add the PureSerialPorts
					List<String> pureports = getPortList();
					if (pureports != null)
						for (String name : pureports)
							m_PortIDs.add(new CommPortIdentifier(name, PORT_SERIAL, null));
				}
				Iterator<CommPortIdentifier> m_Iterator = m_PortIDs.iterator();

				public boolean hasMoreElements() {
					return m_Iterator != null ? m_Iterator.hasNext() : false;
				}

				public Object nextElement() {
					return m_Iterator.next();
				};
			};
		}
	}

	public String getCurrentOwner() {
		synchronized (m_Mutex) {
			return m_Owners.get(this);
		}
	}

	public boolean isCurrentlyOwned() {
		return getCurrentOwner() != null;
	}

	public void addPortOwnershipListener(CommPortOwnershipListener listener) {
		synchronized (m_Mutex) {
			List<CommPortOwnershipListener> list = m_OwnerShipListeners.get(this);
			if (list == null) {
				list = new LinkedList<CommPortOwnershipListener>();
				m_OwnerShipListeners.put(this, list);
			}
			list.add(listener);
		}
	}

	public void removePortOwnershipListener(CommPortOwnershipListener listener) {
		synchronized (m_Mutex) {
			List<CommPortOwnershipListener> list = m_OwnerShipListeners.get(this);
			if (list == null)
				return;
			list.remove(listener);
			if (list.isEmpty())
				m_OwnerShipListeners.remove(list);

		}
	}

	private void fireOwnershipEvent(int type) {
		synchronized (m_Mutex) {
			List<CommPortOwnershipListener> list = m_OwnerShipListeners.get(this);
			if (list == null)
				return;
			for (CommPortOwnershipListener listener : list)
				listener.ownershipChange(type);
		}
	}
}
