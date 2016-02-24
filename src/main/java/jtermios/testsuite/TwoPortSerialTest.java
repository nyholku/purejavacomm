package jtermios.testsuite;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import purejavacomm.CommPortIdentifier;
import purejavacomm.SerialPort;

/**
 * Executes tests to check the correct implementation of frame format settings.
 * You'll need two serial ports connected by a null-modem cable to execute them.
 * 
 * <p>
 * Start this application for the receiving port first and then the sender.
 * 
 * <p>
 * <strong>Beware that you'll need capable hardware and drivers to check all
 * settings, especially mark/space parity and &lt; 7 data bits. The bleeding
 * edge Linux or Win32 drivers for PL2303 are good candidates.</strong>
 * 
 * <p>
 * To ensure that your driver/device isn't lying about its support of a specific
 * setting, you should run your tests with different models of serial ports for
 * sender and receiver.
 * 
 * @author cleitner
 */
public class TwoPortSerialTest {

	public static void main(String[] args) throws Exception {
		if (args.length != 6
				|| (!args[0].equalsIgnoreCase("S") && !args[0]
						.equalsIgnoreCase("R"))) {
			System.err
					.printf("usage: %s <S|R> <port> <baudrate> <databits> <stopbits> <parity>%n",
							TwoPortSerialTest.class.getName());
			System.exit(1);
			return;
		}

		if (args[0].equalsIgnoreCase("s")) {
			runSender(Arrays.copyOfRange(args, 1, 6));
		} else {
			runReceiver(Arrays.copyOfRange(args, 1, 6));
		}
	}

	public static void runSender(String[] args) throws Exception {
		int baudRate = Integer.valueOf(args[1]);
		int dataBits = Integer.valueOf(args[2]);
		int stopBits = SerialPort.STOPBITS_1;
		{
			if (args[3].equals("1")) {
				stopBits = SerialPort.STOPBITS_1;
			} else if (args[3].equals("1.5") || args[3].equals("2")) {
				stopBits = SerialPort.STOPBITS_2;
			} else {
				System.err.println("Invalid stop bits");
				System.exit(1);
				return;
			}
		}
		int parity = SerialPort.PARITY_NONE;
		{
			if (args[4].equalsIgnoreCase("e")) {
				parity = SerialPort.PARITY_EVEN;
			} else if (args[4].equalsIgnoreCase("o")) {
				parity = SerialPort.PARITY_ODD;
			} else if (args[4].equalsIgnoreCase("n")) {
				parity = SerialPort.PARITY_NONE;
			} else if (args[4].equalsIgnoreCase("m")) {
				parity = SerialPort.PARITY_MARK;
			} else if (args[4].equalsIgnoreCase("s")) {
				parity = SerialPort.PARITY_SPACE;
			} else {
				System.err.println("Invalid parity");
				System.exit(1);
				return;
			}
		}

		int requiredExtraBits = 0;
		if (stopBits != SerialPort.STOPBITS_1) {
			requiredExtraBits += 1;
		}
		if (parity != SerialPort.PARITY_NONE) {
			requiredExtraBits += 1;
		}

		if (dataBits + requiredExtraBits > 8) {
			System.err.printf(
					"This test can only run with up to %d data bits%n",
					8 - requiredExtraBits);
			System.exit(1);
			return;
		}

		SerialPort p = (SerialPort) CommPortIdentifier.getPortIdentifier(
				args[0]).open(TwoPortSerialTest.class.getName(), 0);

		p.setSerialPortParams(baudRate, dataBits, stopBits, parity);

		OutputStream out = p.getOutputStream();

		final int N = (1 << dataBits);

		for (int n = 0; n < N; n++) {
			out.write(n);
			// We need a synthetic stop bit
			Thread.sleep(1);
		}
	}

	public static void runReceiver(String[] args) throws Exception {
		int baudRate = Integer.valueOf(args[1]);
		int dataBits = Integer.valueOf(args[2]);
		int stopBits = SerialPort.STOPBITS_1;
		{
			if (args[3].equals("1")) {
				stopBits = SerialPort.STOPBITS_1;
			} else if (args[3].equals("1.5") || args[3].equals("2")) {
				stopBits = SerialPort.STOPBITS_2;
			} else {
				System.err.println("Invalid stop bits");
				System.exit(1);
				return;
			}
		}
		int parity = SerialPort.PARITY_NONE;
		{
			if (args[4].equalsIgnoreCase("e")) {
				parity = SerialPort.PARITY_EVEN;
			} else if (args[4].equalsIgnoreCase("o")) {
				parity = SerialPort.PARITY_ODD;
			} else if (args[4].equalsIgnoreCase("n")) {
				parity = SerialPort.PARITY_NONE;
			} else if (args[4].equalsIgnoreCase("m")) {
				parity = SerialPort.PARITY_MARK;
			} else if (args[4].equalsIgnoreCase("s")) {
				parity = SerialPort.PARITY_SPACE;
			} else {
				System.err.println("Invalid parity");
				System.exit(1);
				return;
			}
		}

		int requiredExtraBits = 1;
		if (stopBits != SerialPort.STOPBITS_1) {
			requiredExtraBits += 1;
		}
		if (parity != SerialPort.PARITY_NONE) {
			requiredExtraBits += 1;
		}

		if (dataBits + requiredExtraBits > 8) {
			System.err.printf(
					"This test can only run with up to %d data bits%n",
					8 - requiredExtraBits);
			System.exit(1);
			return;
		}

		SerialPort p = (SerialPort) CommPortIdentifier.getPortIdentifier(
				args[0]).open(TwoPortSerialTest.class.getName(), 0);

		p.setSerialPortParams(baudRate, dataBits + requiredExtraBits,
				SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		p.enableReceiveTimeout(10000);

		InputStream in = p.getInputStream();
		while (in.available() != 0) {
			in.read();
		}

		System.out.println("Waiting for A...");

		final int N = (1 << dataBits);

		boolean failed = false;

		for (int n = 0; n < N; n++) {
			int v = in.read();
			if (v == -1) {
				System.out.printf("Timeout on value %d%n", n);
				failed = true;
				break;
			}

			if ((v & (N - 1)) != n) {
				System.out
						.printf("Invalid data byte %02X for value %d%n", v, n);
				failed = true;
			}

			int par = (v >> dataBits) & 1;
			switch (parity) {
			case SerialPort.PARITY_NONE:
				// Ignore
				break;
			case SerialPort.PARITY_EVEN:
				if (par != (Integer.bitCount(v & (N - 1)) % 2)) {
					System.out.printf(
							"Invalid even parity in byte %02X for value %d%n",
							v, n);
					failed = true;
				}
				break;
			case SerialPort.PARITY_ODD:
				if (par == (Integer.bitCount(v & (N - 1)) % 2)) {
					System.out.printf(
							"Invalid odd parity in byte %02X for value %d%n",
							v, n);
					failed = true;
				}
				break;
			case SerialPort.PARITY_MARK:
				if (par != 1) {
					System.out.printf(
							"Invalid mark parity in byte %02X for value %d%n",
							v, n);
					failed = true;
				}
				break;
			case SerialPort.PARITY_SPACE:
				if (par != 0) {
					System.out.printf(
							"Invalid space parity in byte %02X for value %d%n",
							v, n);
					failed = true;
				}
				break;
			default:
				throw new AssertionError();
			}

			int stop = v >> (dataBits + ((parity != SerialPort.PARITY_NONE) ? 1
					: 0));
			if (stopBits != SerialPort.STOPBITS_1) {
				stop &= 0x3;
				if (stop != 0x3) {
					System.out.printf("Invalid stop bits %X for value %d%n",
							stop, n);
					failed = true;
				}
			} else {
				stop &= 0x1;
				if (stop != 0x1) {
					System.out.printf("Invalid stop bit %d for value %d%n",
							stop, n);
					failed = true;
				}
			}
		}

		System.out.println(failed ? "FAIL" : "SUCCESS");
		System.exit(failed ? 2 : 1);
	}
}
