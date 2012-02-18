// This c.c is NOT part of PureJavaComm, however it can be useful 
// in porting JTermiosImpl to new platforms


#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/ioctl.h>    
#include <termios.h>    
#include <sys/select.h>    
#include <poll.h>    
#include <sys/filio.h>    

main(){
	printf("// sys/filio.h stuff\n");
	printf("FIONREAD = 0x%08X;\n",FIONREAD);

	printf("// fcntl.h stuff\n");
	printf("O_RDWR = 0x%08X;\n",O_RDWR);
	printf("O_NONBLOCK= 0x%08X;\n",O_NONBLOCK);
	printf("O_NOCTTY = 0x%08X;\n",O_NOCTTY);
	printf("O_NDELAY = 0x%08X;\n",O_NDELAY);
	printf("F_GETFL = 0x%08X;\n",F_GETFL);
	printf("F_SETFL = 0x%08X;\n",F_SETFL);

	printf("// errno.h stuff\n");
	printf("EAGAIN = %d;\n",EAGAIN);
	printf("EBADF = %d;\n",EBADF);
	printf("EACCES= %d;\n",EINVAL);
	printf("EEXIST= %d;\n",EEXIST);
	printf("EINTR= %d;\n",EINTR);
	printf("EINVAL= %d;\n",EINVAL);
	printf("EIO= %d;\n",EIO);
	printf("EISDIR= %d;\n",EISDIR);
	printf("ELOOP= %d;\n",ELOOP);
	printf("EMFILE= %d;\n",EMFILE);
	printf("ENAMETOOLONG= %d;\n",ENAMETOOLONG);
	printf("ENFILE= %d;\n",ENFILE);
	printf("ENOENT= %d;\n",ENOENT);
	printf("ENOSR= %d;\n",ENOSR);
	printf("ENOSPC= %d;\n",ENOSPC);
	printf("ENOTDIR= %d;\n",ENOTDIR);
	printf("ENXIO= %d;\n",ENXIO);
	printf("EOVERFLOW= %d;\n",EOVERFLOW);
	printf("EROFS= %d;\n",EROFS);
	printf("ENOTSUP= %d;\n",ENOTSUP);	
	
	printf("// termios.h stuff\n");
	printf("TIOCM_RNG = 0x%08X;\n",TIOCM_RNG);
	printf("TIOCM_CAR = 0x%08X;\n",TIOCM_CAR);
	
	printf("IGNBRK = 0x%08X;\n",IGNBRK);
	printf("BRKINT = 0x%08X;\n",BRKINT);
	printf("IGNPAR = 0x%08X;\n",PARMRK);
	printf("PARMRK = 0x%08X;\n",PARMRK);
	printf("INLCR = 0x%08X;\n",INLCR);
	printf("IGNCR = 0x%08X;\n",IGNCR);
	printf("ICRNL = 0x%08X;\n",ICRNL); 
	printf("ECHONL = 0x%08X;\n",ECHONL); 
	printf("IEXTEN = 0x%08X;\n",IEXTEN);
	
	printf("CLOCAL = 0x%08X;\n",CLOCAL);
	printf("OPOST = 0x%08X;\n",OPOST);
	printf("VSTART = 0x%08X;\n",VSTART);
	printf("TCSANOW = 0x%08X;\n",TCSANOW);
	printf("VSTOP = 0x%08X;\n",VSTOP);
	printf("VMIN = 0x%08X;\n",VMIN);
	printf("VTIME = 0x%08X;\n",VTIME);
	printf("VEOF = 0x%08X;\n",VEOF);
	printf("TIOCMGET = 0x%08X;\n",TIOCMGET);
	printf("TIOCM_CTS = 0x%08X;\n",TIOCM_CTS);
	printf("TIOCM_DSR = 0x%08X;\n",TIOCM_DSR);
	printf("TIOCM_RI = 0x%08X;\n",TIOCM_RI);
	printf("TIOCM_CD = 0x%08X;\n",TIOCM_CD);
	printf("TIOCM_DTR = 0x%08X;\n",TIOCM_DTR);
	printf("TIOCM_RTS = 0x%08X;\n",TIOCM_RTS);
	printf("ICANON = 0x%08X;\n",ICANON);
	printf("ECHO = 0x%08X;\n", ECHO);
	printf("ECHOE = 0x%08X;\n", ECHOE);
	printf("ISIG = 0x%08X;\n",ISIG);
	printf("TIOCMSET= 0x%08X;\n",TIOCMSET);
	printf("IXON = 0x%08X;\n",IXON);
	printf("IXOFF = 0x%08X;\n",IXOFF);
	printf("IXANY = 0x%08X;\n",IXANY);
	printf("CRTSCTS = 0x%08X;\n",CRTSCTS);
	printf("TCSADRAIN = 0x%08X;\n",TCSADRAIN);
	printf("INPCK = 0x%08X;\n",INPCK);
	printf("ISTRIP = 0x%08X;\n",ISTRIP);
	printf("CSIZE = 0x%08X;\n",CSIZE);
	printf("TCIFLUSH = 0x%08X;\n",TCIFLUSH);
	printf("TCOFLUSH = 0x%08X;\n",TCOFLUSH);
	printf("TCIOFLUSH = 0x%08X;\n",TCIOFLUSH);

	printf("CS5 = 0x%08X;\n",CS5);
	printf("CS6 = 0x%08X;\n",CS6);
	printf("CS7 = 0x%08X;\n",CS7);
	printf("CS8 = 0x%08X;\n",CS8);

	printf("CSTOPB = 0x%08X;\n",CSTOPB);
	printf("CREAD = 0x%08X;\n",CREAD);
	printf("PARENB = 0x%08X;\n",PARENB);
	printf("PARODD = 0x%08X;\n",PARODD);

	printf("CCTS_OFLOW = 0x%08X;\n",CCTS_OFLOW);
	printf("CRTS_IFLOW = 0x%08X;\n",CRTS_IFLOW);
//	printf("CDTR_IFLOW = 0x%08X;\n",CDTR_IFLOW);
//	printf("CDSR_OFLOW = 0x%08X;\n",CDSR_OFLOW);
//	printf("CCAR_OFLOW = 0x%08X;\n",CCAR_OFLOW);

	printf("B0 = %d;\n",B0);
	printf("B50 = %d;\n",B50);
	printf("B75 = %d;\n",B75);
	printf("B110 = %d;\n",B110);
	printf("B134 = %d;\n",B134);
	printf("B150 = %d;\n",B150);
	printf("B200 = %d;\n",B200);
	printf("B300 = %d;\n",B300);
	printf("B600 = %d;\n",B600);
	printf("B1200 = %d;\n",B600);
	printf("B1800 = %d;\n",B1800);
	printf("B2400 = %d;\n",B2400);
	printf("B4800 = %d;\n",B4800);
	printf("B9600 = %d;\n",B9600);
	printf("B19200 = %d;\n",B19200);
	printf("B38400 = %d;\n",B38400);
//	printf("B7200 = %d;\n",B7200);
//	printf("B14400 = %d;\n",B14400);
//	printf("B28800 = %d;\n",B28800);
	printf("B57600 = %d;\n",B57600);
	printf("B76800 = %d;\n",B76800);
	printf("B115200 = %d;\n",B115200);
	printf("B230400 = %d;\n",B230400);
	printf("B307200 = %d;\n",B307200);
	printf("B460800 = %d;\n",B460800);
	printf("B921600 = %d;\n",B921600);
	
	
	printf("// poll.h stuff\n");
	printf("POLLIN = 0x%04X;\n",POLLIN);
	//printf("POLLRDNORM = 0x%04X;\n",POLLRDNORM);
	//printf("POLLRDBAND = 0x%04X;\n",POLLRDBAND);
	printf("POLLPRI = 0x%04X;\n",POLLPRI);
	printf("POLLOUT = 0x%04X;\n",POLLOUT);
	//printf("POLLWRNORM = 0x%04X;\n",POLLWRNORM);
	//printf("POLLWRBAND = 0x%04X;\n",POLLWRBAND);
	printf("POLLERR = 0x%04X;\n",POLLERR);
	printf("POLLNVAL = 0x%04X;\n",POLLNVAL);

	
	printf("// select.h stuff\n");
	printf("FD_SETSIZE = 0x%08X;\n",FD_SETSIZE);
	
	struct termios t;
	printf("%d\n",sizeof(t));
	printf("%d\n",sizeof(t.c_iflag));
	printf("%d\n",sizeof(t.c_cflag));
	printf("%d\n",sizeof(t.c_lflag));
	printf("%d\n",sizeof(t.c_cc));
//	printf("%d\n",sizeof(t.c_ispeed));
//	printf("%d\n",sizeof(t.c_ospeed));

  }
