/*
 * Copyright 2011 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package comm;

import comm.ISerialPort;
import comm.SerialPort;
import comm.ICommPort;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author David Hoyt
 */
public class SerialPortTest {
	//<editor-fold defaultstate="collapsed" desc="Init">
	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}
	//</editor-fold>

	@Test
	public void checkCommPorts() throws InterruptedException {
		ICommPort cp = null;
		ISerialPort[] serialPorts = SerialPort.getAvailableSerialPorts();
		assertNotNull(serialPorts);
		
		assertTrue(serialPorts.length > 0);
		
		ISerialPort serialPort = SerialPort.find("COM2"); //serialPorts[0];
		assertNotNull(serialPort);
		assertFalse("".equalsIgnoreCase(serialPort.getName()));
		
		assertTrue(serialPort.open());
		Thread.sleep(60000 * 3);
		assertTrue(serialPort.close());
	}
}
