When using PureJavaComm on Linux there is a breaking change in the native API for the GNU C LIbrary (glibc) version 2.42 (and higher)

To quote from the release notes here [The GNU C Library version 2.42 is now available](https://lists.gnu.org/archive/html/info-gnu/2025-07/msg00011.html)

>On Linux, the <termios.h> interface now supports arbitrary baud rates;
>speed_t is redefined to simply be the baud rate specified as an
>unsigned int, which matches the kernel interface.

Prior to this change, the actual speed was set by an int code that was mapped to the required speed.

These int codes were defined in m_BaudRates in [JTermiosImpl.java](src/jtermios/linux/JTermiosImpl.java) and the `setSpeed` method in there
performed a lookup of the requested speed (e.g. 9600 baud) to the flag (e.g. 0000015).  This flag value was then passed to the native code
structure.

After this change, as detailed in the quote, the speed_t value should just be the required baud rate, e.g. 9600 baud.

Compatibility with glibc 2.42 and higher can be achieved by running your program with `-Djtermios.modern.speed=true` as an argument to the JVM.
