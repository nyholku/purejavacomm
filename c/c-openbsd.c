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
	printf("O_CREAT = 0x%08X;\n",O_CREAT);
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
//	printf("ENOSR= %d;\n",ENOSR);
	printf("ENOSPC= %d;\n",ENOSPC);
	printf("ENOTDIR= %d;\n",ENOTDIR);
	printf("ENXIO= %d;\n",ENXIO);
	printf("EOVERFLOW= %d;\n",EOVERFLOW);
	printf("EROFS= %d;\n",EROFS);
	printf("ENOTSUP= %d;\n",ENOTSUP);
	printf("EBUSY= %d;\n",EBUSY);
	
	printf("// termios.h stuff\n");
	printf("TIOCM_RNG = 0x%08X;\n",TIOCM_RNG);
	printf("TIOCM_CAR = 0x%08X;\n",TIOCM_CAR);
	
	printf("IGNBRK = 0x%08X;\n",IGNBRK);
	printf("BRKINT = 0x%08X;\n",BRKINT);
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
//	printf("CNEW_RTSCTS = 0x%08X;\n",CNEW_RTSCTS); // Not availabel on Mac OX X 10.6.6 atleast
	printf("CRTSCTS = 0x%08X;\n",CRTSCTS);
	printf("TCSADRAIN = 0x%08X;\n",TCSADRAIN);
	printf("INPCK = 0x%08X;\n",INPCK);
	printf("ISTRIP = 0x%08X;\n",ISTRIP);
	printf("CSIZE = 0x%08X;\n",CSIZE);
	printf("TCIFLUSH = 0x%08X;\n",TCIFLUSH);
	printf("TCOFLUSH = 0x%08X;\n",TCOFLUSH);
	printf("TCIOFLUSH = 0x%08X;\n",TCIOFLUSH);	
//	printf("TIOCGSERIAL = 0x%08X;\n",TIOCGSERIAL);
//	printf("TIOCSSERIAL = 0x%08X;\n",TIOCSSERIAL);

//	printf("ASYNC_SPD_MASK = 0x%08X;\n",ASYNC_SPD_MASK);
//	printf("ASYNC_SPD_CUST = 0x%08X;\n",ASYNC_SPD_CUST);

	
	
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
	printf("B7200 = %d;\n",B7200);
	printf("B14400 = %d;\n",B14400);
	printf("B28800 = %d;\n",B28800);
	printf("B57600 = %d;\n",B57600);
	printf("B76800 = %d;\n",B76800);
	printf("B115200 = %d;\n",B115200);
	printf("B230400 = %d;\n",B230400);
//	printf("B307200 = %d;\n",B307200);
//	printf("B460800 = %d;\n",B460800);
//	printf("B921600 = %d;\n",B921600);
	
	printf("// poll.h stuff\n");
	printf("POLLIN = 0x%04X;\n",POLLIN);
	printf("POLLRDNORM = 0x%04X;\n",POLLRDNORM);
	printf("POLLRDBAND = 0x%04X;\n",POLLRDBAND);
	printf("POLLPRI = 0x%04X;\n",POLLPRI);
	printf("POLLOUT = 0x%04X;\n",POLLOUT);
	printf("POLLWRNORM = 0x%04X;\n",POLLWRNORM);
	printf("POLLWRBAND = 0x%04X;\n",POLLWRBAND);
	printf("POLLERR = 0x%04X;\n",POLLERR);
	printf("POLLNVAL = 0x%04X;\n",POLLNVAL);

	
	printf("// select.h stuff\n");
	printf("FD_SETSIZE = 0x%08X;\n",FD_SETSIZE);
	printf("__NFDBITS = 0x%08X;\n",__NFDBITS);

    printf("// _misc\n");

	struct termios t;
	printf("termios %d\n",sizeof(t));
	printf(".c_iflag %d\n",((char*)&t.c_iflag)-((char*)&t));	
	printf(".c_oflag %d\n",((char*)&t.c_oflag)-((char*)&t));
	printf(".c_cflag %d\n",((char*)&t.c_cflag)-((char*)&t));
	printf(".c_lflag %d\n",((char*)&t.c_lflag)-((char*)&t));
//	printf(".c_line %d\n",((char*)&(t.c_line))-((char*)&t));
	printf(".c_cc[0] %d\n",((char*)&(t.c_cc[0]))-((char*)&t));
	printf(".c_cc[1] %d\n",((char*)&(t.c_cc[1]))-((char*)&t));
	printf(".c_cc[30] %d\n",((char*)&(t.c_cc[30]))-((char*)&t));
	printf(".c_cc[31] %d\n",((char*)&(t.c_cc[31]))-((char*)&t));
	printf(".c_ispeed %d\n",((char*)&t.c_ispeed)-((char*)&t));
	printf(".c_ospeed %d\n",((char*)&t.c_ospeed)-((char*)&t));

//	struct serial_struct ss;
//	printf("serial_struct %d\n",sizeof(ss));
//	printf(".type %d\n",((char*)&ss.type)-((char*)&ss));
//	printf(".line %d\n",((char*)&ss.line)-((char*)&ss));
//	printf(".port %d\n",((char*)&ss.port)-((char*)&ss));
//	printf(".irq %d\n",((char*)&ss.irq)-((char*)&ss));
//	printf(".flags %d\n",((char*)&ss.flags)-((char*)&ss));
//	printf(".xmit_fifo_size %d\n",((char*)&ss.xmit_fifo_size)-((char*)&ss));
//	printf(".custom_divisor %d\n",((char*)&ss.custom_divisor)-((char*)&ss));
//	printf(".baud_base %d\n",((char*)&ss.baud_base)-((char*)&ss));
//	printf(".close_delay %d\n",((char*)&ss.close_delay)-((char*)&ss));
//	printf(".io_type %d\n",((char*)&ss.io_type)-((char*)&ss));
//	printf(".reserved_char %d\n",((char*)&ss.reserved_char)-((char*)&ss));
//	printf(".hub6 %d\n",((char*)&ss.hub6)-((char*)&ss));
//	printf(".closing_wait %d\n",((char*)&ss.closing_wait)-((char*)&ss));
//	printf(".closing_wait2 %d\n",((char*)&ss.closing_wait2)-((char*)&ss));
//	printf(".iomem_base %d\n",((char*)&ss.iomem_base)-((char*)&ss));
//	printf(".iomem_reg_shift %d\n",((char*)&ss.iomem_reg_shift)-((char*)&ss));
//	printf(".port_high %d\n",((char*)&ss.port_high)-((char*)&ss));
//	printf(".iomap_base %d\n",((char*)&ss.iomap_base)-((char*)&ss));

	printf("timeval %d\n",sizeof(struct timeval));

	fd_set fs;
	FD_ZERO(&fs);
	FD_SET(41,&fs);
	printf("sizeof(fd_set)=%d\n",sizeof(fs));
	int i;
	for (i=0; i<8; i++)
		printf("%08X ",fs.fds_bits[i]);

	printf("\n");

	int com;
	printf("open    %d\n",com =  open("/dev/tty.usbserial-FTOXM3NX", O_RDWR | O_NOCTTY | O_NDELAY));

	struct termios opts;

	printf("get     %d\n",tcgetattr(com, &opts));
	printf("c_cflag %08X\n",opts.c_cflag);

	opts.c_cflag |= CRTSCTS;
	printf("c_cflag %08X\n",opts.c_cflag);
	printf("set     %d\n",tcsetattr(com,TCSANOW, &opts));

	opts.c_cflag = 0;
	printf("get     %d\n",tcgetattr(com, &opts));
	printf("c_cflag %08X\n",opts.c_cflag);

	printf(" sizeof(speed_t) %d\n", sizeof(speed_t));










  }
