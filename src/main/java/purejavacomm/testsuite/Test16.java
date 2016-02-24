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
package purejavacomm.testsuite;

import java.util.Enumeration;

import purejavacomm.CommPortIdentifier;

public class Test16 extends TestBase {

	@SuppressWarnings("unchecked")
	static void run() throws Exception {

		Enumeration<CommPortIdentifier> cpiEnum;
		
		String origOwnerName = null;
		String checkOwnerName = null;
		
		try {

			begin("Test16 - port ownership");
			
			openPort();

			// Check ownership of the id of our test port first
			{
				CommPortIdentifier id = CommPortIdentifier
						.getPortIdentifier(m_Port);
				if (id == null) {
					fail("No id for this serial port");
				}

				if (id.getCurrentOwner() == null
						|| !id.getCurrentOwner().equals(APPLICATION_NAME)) {
					fail("Wrong or missing owner for this serial port (got \"%s\", expected \"%s\")",
							id.getCurrentOwner(), APPLICATION_NAME);
				}
			}
			
			//first call to enumerate port identifiers
			cpiEnum = CommPortIdentifier.getPortIdentifiers();
			
			//get original owner name
			while (cpiEnum.hasMoreElements()) {
				CommPortIdentifier cpi = cpiEnum.nextElement();
				if (cpi.getName().equals(getPortName())) {
					origOwnerName = cpi.getCurrentOwner();
					break;
				}
			}

			//second call to enumerate port identifiers
			cpiEnum = CommPortIdentifier.getPortIdentifiers();

			//get owner name again
			while (cpiEnum.hasMoreElements()) {
				CommPortIdentifier cpi = cpiEnum.nextElement();
				if (cpi.getName().equals(getPortName())) {
					checkOwnerName = cpi.getCurrentOwner();
					break;
				}
			}
			
			//these should be exactly the same
			if (checkOwnerName != origOwnerName && !checkOwnerName.equals(origOwnerName)) {
				fail("Owner name incorrectly changed simply by reenumerating ports." + origOwnerName + " vs. " + checkOwnerName);
			}
			else {
				finishedOK();
			}
			
		} finally {
			closePort();
		}

	}
}