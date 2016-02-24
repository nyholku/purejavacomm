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

package jtermios;

import java.util.Arrays;

final public class Termios {
	public int c_iflag;
	public int c_oflag;
	public int c_cflag;
	public int c_lflag;
	public byte[] c_cc = new byte[20];
	public int c_ispeed;
	public int c_ospeed;
	public void set(Termios s) {
		c_iflag=s.c_iflag;
		c_oflag=s.c_oflag;
		c_cflag=s.c_cflag;
		c_lflag=s.c_lflag;
		System.arraycopy(s.c_cc,0,c_cc,0,c_cc.length);
		c_ispeed=s.c_ispeed;
		c_ospeed=s.c_ospeed;	
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(c_cc);
		result = prime * result + c_cflag;
		result = prime * result + c_iflag;
		result = prime * result + c_ispeed;
		result = prime * result + c_lflag;
		result = prime * result + c_oflag;
		result = prime * result + c_ospeed;
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Termios other = (Termios) obj;
		if (!Arrays.equals(c_cc, other.c_cc))
			return false;
		if (c_cflag != other.c_cflag)
			return false;
		if (c_iflag != other.c_iflag)
			return false;
		if (c_ispeed != other.c_ispeed)
			return false;
		if (c_lflag != other.c_lflag)
			return false;
		if (c_oflag != other.c_oflag)
			return false;
		if (c_ospeed != other.c_ospeed)
			return false;
		return true;
	}
}
