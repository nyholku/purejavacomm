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
#include <sys/filio.h>    
#include <string.h>
#include <sys/time.h>
#include <pthread.h>

volatile int N;
volatile int com;
volatile int txcnt;
volatile int rxcnt;
volatile int secs;
volatile int packetsz;

volatile char txchr[1024];
volatile char rxchr[1024];

int packetSizes[] = { 1, 2, 3, 4, 5, 6, 7, 8, 16, 32, 64 };
speed_t brvalues[] = {1200,2400,4800,9600,19200,38400,76800,115200,230400};
speed_t bconsts[] = {B1200,B2400,B4800,B9600,B19200,B38400,B76800,B115200,B230400};

void *txthread(void *threadid) {
    while (txcnt<N) {
        // fill buffer with test data
        int i;
        for (i=0; i<packetsz; i++)
            txchr[i] = (char) txcnt;
        int len=packetsz;
        while (len>0) {
            int wn=write(com,&txchr[packetsz-len],len);
            //fprintf(stderr,"write %d %d %d\n",+packetsz-len,len,wn);
            if (wn<1) {
                fprintf(stderr,"write %d %s\n",wn,strerror(errno));
                exit(1);
            }
            len -= wn;
        }
        txcnt++;
    }
    pthread_exit(NULL);
}

void *rxthread(void *threadid) {
    while (rxcnt<N) {
        int len=packetsz;
        while (len>0) {
            int rn=read(com,&rxchr[packetsz-len],len);
            //fprintf(stderr,"read %d %d %d\n",+packetsz-len,len,rn);
            if (rn<1) {  
                fprintf(stderr,"read %d %s\n",rn,strerror(errno));
                exit(1);
            }
            len -= rn;
        }
        // verify buffer contents
        int i;
        for (i=0; i<packetsz; i++) {
            if (rxchr[i] != (char) rxcnt) {
                fprintf(stderr,"loopback fail at %d tx %d rx %d\n",i,(char) rxcnt,rxchr[i]);
                exit(1);
            }   
        }
        rxcnt++;        
    } 
    pthread_exit(NULL);
}

int main(int argn, char** argv){      
    char* port="/dev/tty.usbserial-FTOXM3NX";
    
    if (argn>1)
        port=argv[1];
    
    int k;
    for (k = 0; k<sizeof(packetSizes)/sizeof(packetSizes[0]);k++)  {
        packetsz=packetSizes[k];
        printf("%d\n",packetsz);
        
        
        secs=10;
        
        com =  open(port, O_RDWR | O_NOCTTY | O_NDELAY);
        if (com<0) {
            fprintf(stderr,"open %s: %s\n",port,strerror(errno));
            exit(1);
        }
        
        fcntl(com, F_SETFL, 0);
        
        fd_set wfd;
        FD_ZERO(&wfd);
        fd_set rfd;
        FD_ZERO(&rfd);
        
        int i;
        for (i=0; i<sizeof(brvalues)/sizeof(brvalues[0]); i++) {
            int baudrate = brvalues[i];
            
            N = secs*baudrate/10/packetsz;
            
            // configure baudreat and everything else
            struct termios opts;
            
            if (tcgetattr(com, &opts) != 0) {
                fprintf(stderr,"tcgetattr: %s\n",strerror(errno));
                exit(1);
            }
            
            opts.c_lflag  &=  ~(ICANON | ECHO | ECHOE | ISIG);
            
            opts.c_cflag |=  (CLOCAL | CREAD);
            opts.c_cflag &=  ~PARENB;
            opts.c_cflag &=  ~CSTOPB; 
            opts.c_cflag &=  ~CSIZE;
            opts.c_cflag |=  CS8;
            
            opts.c_oflag &=  ~OPOST;
            
            opts.c_iflag &=  ~INPCK;
            opts.c_iflag &=  ~(IXON | IXOFF | IXANY);
            opts.c_cc[ VMIN ] = 1;
            opts.c_cc[ VTIME ] = 0;
            
            opts.c_cflag |= CRTSCTS;
            
            //printf("%08X %08X %08X %08X\n",opts.c_iflag,opts.c_oflag,opts.c_cflag,opts.c_lflag);
            //exit(0);
            
            cfsetispeed(&opts,  bconsts[i]);   
            cfsetospeed(&opts,  bconsts[i]);   
            
            if (tcsetattr(com, TCSANOW, &opts) != 0) {
                fprintf(stderr,"tcsetattr: %s\n",strerror(errno));
                exit(1);
            }
            
            // read start tiem
            struct timeval t0;
            if (gettimeofday(&t0,NULL) != 0) {
                fprintf(stderr,"gettimeofday: %s\n",strerror(errno));
                exit(1);
            }
            
            // zero the thread control variables
            txcnt=0;
            rxcnt=0;
            
            // start threads
            pthread_t txthreadp;
            int trc=pthread_create(&txthreadp,NULL,txthread,NULL);
            if (trc != 0) {
                fprintf(stderr,"pthread_create %d\n",trc);
                exit(1);
            }
            
            pthread_t rxthreadp;
            int rrc=pthread_create(&rxthreadp,NULL,rxthread,NULL);
            if (rrc != 0) {
                fprintf(stderr,"pthread_create %d\n",rrc);
                exit(1);
            }
            
            // wait for threads to stop 
            while (txcnt<N || rxcnt<N)  {
                fprintf(stderr,"wait %d %d\n",txcnt,rxcnt);
                struct timespec st;
                st.tv_sec=0;
                st.tv_nsec=50000000; // 50 msec
                if (nanosleep(&st,NULL) != 0) {
                    fprintf(stderr,"nanosleep: s\n",strerror(errno));
                    exit(1);
                }
            }
            
            // read the stop time (this has an error ~10 msec, because of the sleep above)
            struct timeval t1;
            if (gettimeofday(&t1,NULL)!=0) {
                fprintf(stderr,"gettimeofday: %s\n",strerror(errno));
                exit(1);
            }
            int N=rxcnt*packetsz;
            double t = ((t1.tv_sec + t1.tv_usec/ 1000000.0) - (t0.tv_sec + t0.tv_usec/ 1000000.0)) ;
            fprintf(stderr,"%d , %f\n",baudrate,10*N/t); 
        }
    }
    return 0;
}
