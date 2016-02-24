// This c.c is NOT part of PureJavaComm, however it can be useful 
// in porting JTermiosImpl to new platforms


#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>    
#include <termios.h>    
#include <sys/select.h>    
//#include <sys/filio.h>    
#include <string.h>
#include <sys/time.h>


volatile char txchr[32*1024];
volatile char rxchr[32*1024];

int packetSizes[] = { 1, 2, 3, 4, 5, 6, 7, 8, 16, 32, 64 };
int baudRates[] = { 1200, 2400, 4800, 9600, 19200, 38400, 115200, 230400 };
int baudConst[] = { B1200, B2400, B4800, B9600, B19200, B38400, B115200, B230400 };




int main(int argn, char** argv){      
    int secs = 10;
    int packetsz;
    char* port="/dev/ttyS0";
    
    int com =  open(port, O_RDWR | O_NOCTTY | O_NDELAY | O_NONBLOCK);
    if (com<0) {
        printf("open %s: %s\n",port,strerror(errno));
        exit(1);
    }
    
    fd_set wfd;
    FD_ZERO(&wfd);
    fd_set rfd;
    FD_ZERO(&rfd);
    int k;
    for (k = 0; k<sizeof(packetSizes)/sizeof(packetSizes[0]);k++)  {
        packetsz=packetSizes[k];
        printf("%d\n",packetsz);
        
        int i;
        
        for (i = 0; i<sizeof(baudRates)/sizeof(baudRates[0]);i++)  {
            
            int baudrate=baudRates[i];
            struct termios opts;
            
            if (tcgetattr(com, &opts) != 0) {
                printf("tcgetattr: %s\n",strerror(errno));
                exit(1);
            }
            
            opts.c_lflag  &=  ~(ICANON | ECHO | ECHOE | ISIG);
            
            opts.c_cflag |=  (CLOCAL | CREAD);
            opts.c_cflag &=  ~PARENB;
            opts.c_cflag &=  ~CSTOPB; 
            opts.c_cflag &=  ~CSIZE;
            opts.c_cflag |=  CS8;
            opts.c_cflag |= CRTSCTS;
            
            opts.c_oflag &=  ~OPOST;
            
            opts.c_iflag &=  ~INPCK;
            opts.c_iflag &=  ~(IXON | IXOFF | IXANY);
            opts.c_cc[ VMIN ] = 1;
            opts.c_cc[ VTIME ] = 0;
            
            cfsetispeed(&opts, baudConst[i]);   
            cfsetospeed(&opts, baudConst[i]);   
            
            if (tcsetattr(com, TCSANOW, &opts) != 0) {
                printf("tcsetattr: %s\n",strerror(errno));
                exit(1);
            }
            
            
            struct timeval t0;
            if (gettimeofday(&t0,NULL)!=0) {
                printf("gettimeofday: %s\n",strerror(errno));
                exit(1);
            }
            
            
            int txcnt = 0;
            int rxcnt = 0;
            int txlen =0;
            int rxlen =0;
            int i;
            for (i=0; i<packetsz; i++)
                txchr[i]=txcnt;
            int N = secs * baudrate / 10/packetsz;
            while (txcnt<N || rxcnt<N) {
                FD_CLR(com,&wfd);
                FD_CLR(com,&rfd);
                if (txcnt<N)
                    FD_SET(com,&wfd);
                if (rxcnt<N)
                    FD_SET(com,&rfd);
                int n=select(com+1,&rfd,&wfd,NULL,NULL);
                if (n<0){
                    printf("select %s\n",strerror(errno));
                    exit(1);
                }
                if (FD_ISSET(com,&wfd)) {
                    int wn=write(com,&txchr[txlen],packetsz-txlen);
                    //printf("w %d\n",wn);
                    if (wn<0 && errno!=EAGAIN) {
                        printf("write %d %s\n",wn,strerror(errno));
                        exit(1);
                    }
                    if (wn>0) {
                        txlen+=wn;
                        if (txlen>=packetsz) {
                            txlen=0;
                            txcnt++;
                            for (i=0; i<packetsz; i++)
                                txchr[i]=txcnt;
                        }
                    }
                }
                if (FD_ISSET(com,&rfd)) {
                    int rn=read(com,&rxchr[rxlen],packetsz-rxlen);
                    //printf("r %d\n",rn);
                    if (rn<1 && errno!=EAGAIN) {
                        printf("read %d %s\n",rn,strerror(errno));
                        exit(1);
                    }
                    if (rn>0) {
                        rxlen+=rn;
                        if (rxlen>=packetsz) {
                            //printf("%d %d %d\n",N,txcnt,rxcnt);
                            for (i=0; i<packetsz; i++)
                                if ( rxchr[i]!=(char)rxcnt) {
                                    int j;
                                    for (j=0; j<packetsz; j++)
                                        printf("%02X ",rxchr[j]&0xFF);
                                    printf("loopback fail %d %d %d %d %02X\n",rxlen,rn,rxchr[i],i,(char)rxcnt);
                                    exit(1);
                                }
                            rxlen=0;
                            rxcnt++;
                        }
                        
                    }
                }
            }   
            
            struct timeval t1;
            if (gettimeofday(&t1,NULL)!=0) {
                printf("gettimeofday: %s\n",strerror(errno));
                exit(1);
            }
            
            double t = ((t1.tv_sec + t1.tv_usec/ 1000000.0) - (t0.tv_sec + t0.tv_usec/ 1000000.0)) ;
            printf("%d , %f\n",baudrate,packetsz*10*N/t); 
        }
    }
    return 0;
}
