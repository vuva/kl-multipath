package klmultipath;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.PosixParser;

import klmultipath.randomprocesses.IntertimeProcess;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


/**
 * 
 * java -cp "bin:lib/commons-cli-1.2.jar:lib/commons-math3-3.6.1.jar" klmultipath.KLSender -S x 0.01 -p 127.0.0.1:127.0.0.1 -p 127.0.0.1:127.0.0.2 -p 127.0.0.1:127.0.0.3  -p 127.0.0.1:127.0.0.4 -k 4 -l 4
 * 
 * 
 * 
 * @author brenton
 *
 */
public class KLSender {
	
	public static final int PACKETSIZE = 1024;
	
	public int k, l;
	IntertimeProcess tx_process = null;
	PathSpec[] paths = null;
	DatagramSocket[] sockets = null;
	byte[][] tx_bufs = null;

	public KLSender(int k, int l, IntertimeProcess tx_process, String[] path_strings) {
		this.k = k;
		this.l = l;
		this.tx_process = tx_process;
		// initialize the connections
		paths = new PathSpec[path_strings.length];
		sockets = new DatagramSocket[path_strings.length];
		tx_bufs = new byte[k][];
		for (int i=0; i<k; i++) {
			tx_bufs[i] = new byte[PACKETSIZE];
			tx_bufs[i][0] = (byte) ((k & 0xFF000000) >> 24);
			tx_bufs[i][1] = (byte) ((k & 0x00FF0000) >> 16);
			tx_bufs[i][2] = (byte) ((k & 0x0000FF00) >> 8);
			tx_bufs[i][3] = (byte) ((k & 0x000000FF) >> 0);
			tx_bufs[i][4] = (byte) ((i & 0xFF000000) >> 24);
			tx_bufs[i][5] = (byte) ((i & 0x00FF0000) >> 16);
			tx_bufs[i][6] = (byte) ((i & 0x0000FF00) >> 8);
			tx_bufs[i][7] = (byte) ((i & 0x000000FF) >> 0);
		}
		
		for (int i=0; i<path_strings.length; i++) {
			paths[i] = new PathSpec(path_strings[i]);
			System.out.println(paths[i]);
			try {
				sockets[i] = new DatagramSocket();
				//byte[] buf = "sending something\n".getBytes();
				//sockets[i].send(new DatagramPacket(buf, buf.length, paths[i].dest, KLReceiver.PORT));
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void startSending(int num_messages) {

		int path_index = 0;
		long sendstart = System.nanoTime();
		long endwait = sendstart;
		long time_elapsed = 0;
		
		for (int i=0; i<num_messages; i++) {
			long txtime = System.nanoTime();
			for (int j=0; j<k; j++) {
				// write seq number and the arrival time and txtime into the packets
				tx_bufs[j][8] =  (byte) ((i & 0xFF000000) >> 24);
				tx_bufs[j][9] =  (byte) ((i & 0x00FF0000) >> 16);
				tx_bufs[j][10] = (byte) ((i & 0x0000FF00) >> 8);
				tx_bufs[j][11] = (byte) ((i & 0x000000FF));
				tx_bufs[j][12] = (byte) ((time_elapsed & 0xFF00000000000000L) >> 56);
				tx_bufs[j][13] = (byte) ((time_elapsed & 0x00FF000000000000L) >> 48);
				tx_bufs[j][14] = (byte) ((time_elapsed & 0x0000FF0000000000L) >> 40);
				tx_bufs[j][15] = (byte) ((time_elapsed & 0x000000FF00000000L) >> 32);
				tx_bufs[j][16] = (byte) ((time_elapsed & 0x00000000FF000000L) >> 24);
				tx_bufs[j][17] = (byte) ((time_elapsed & 0x0000000000FF0000L) >> 16);
				tx_bufs[j][18] = (byte) ((time_elapsed & 0x000000000000FF00L) >> 8);
				tx_bufs[j][19] = (byte) ((time_elapsed & 0x00000000000000FFL));
				tx_bufs[j][20] = (byte) ((txtime & 0xFF00000000000000L) >> 56);
				tx_bufs[j][21] = (byte) ((txtime & 0x00FF000000000000L) >> 48);
				tx_bufs[j][22] = (byte) ((txtime & 0x0000FF0000000000L) >> 40);
				tx_bufs[j][23] = (byte) ((txtime & 0x000000FF00000000L) >> 32);
				tx_bufs[j][24] = (byte) ((txtime & 0x00000000FF000000L) >> 24);
				tx_bufs[j][25] = (byte) ((txtime & 0x0000000000FF0000L) >> 16);
				tx_bufs[j][26] = (byte) ((txtime & 0x000000000000FF00L) >> 8);
				tx_bufs[j][27] = (byte) ((txtime & 0x00000000000000FFL));
				try {
					sockets[path_index].send(new DatagramPacket(tx_bufs[j],
							tx_bufs[j].length,
							paths[path_index].dest,
							KLReceiver.PORT));
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				path_index = (path_index + 1) % paths.length;
			}
				
			// sleep for some length of time given by the arrival process
			long dt = (long) (1000.0 * this.tx_process.nextInterval());
			endwait += dt;
			time_elapsed += dt;
			long curtime = System.nanoTime();
			if (endwait < curtime) {
				System.err.println("WARNING: system is backlogged: "+endwait+" < "+curtime+"    ("+(curtime-endwait)+")");
			} else {
				System.err.println("empty system");
			}
			do{
				curtime = System.nanoTime();
			} while(curtime < endwait);
		}
		
	}
	
	
	public static void main(String[] args) {

		Options cli_options = new Options();
		cli_options.addOption("h", "help", false, "print help message");
		cli_options.addOption("k", "k", true, "number of packets to send per transmission");
		cli_options.addOption("l", "l", true, "number of packets that must be recieved");
		cli_options.addOption("n", "numsamples", true, "number of transmission to make");
		cli_options.addOption("i", "samplinginterval", true, "samplig interval");
		cli_options.addOption("p", "path", true, "sender/reciever IP address pair in the form X.X.X.X:Y.Y.Y.Y");
		//cli_options.addOption(OptionBuilder.withLongOpt("queuetype").hasArgs().isRequired().withDescription("queue type and arguments").create("q"));
		//cli_options.addOption(OptionBuilder.withLongOpt("outfile").hasArg().isRequired().withDescription("the base name of the output files").create("o"));
		cli_options.addOption(OptionBuilder.withLongOpt("txprocess").hasArgs().isRequired().withDescription("sending process").create("S"));

		CommandLineParser parser = new GnuParser();
		CommandLine options = null;
		try {
			options = parser.parse(cli_options, args);
		} catch (ParseException e) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("KLSender", cli_options);
			e.printStackTrace();
			System.exit(0);
		}

		int num_messages = options.hasOption("n") ? Integer.parseInt(options.getOptionValue("n")) : 1000;
		String paths[] = options.hasOption("p") ? options.getOptionValues("p") : null;
		int k = options.hasOption("k") ? Integer.parseInt(options.getOptionValue("k")) : 1;
		int l = options.hasOption("l") ? Integer.parseInt(options.getOptionValue("l")) : 1;
		if (l > k) {
			System.err.println("ERROR: l > k");
			System.exit(0);
		}
		IntertimeProcess tx_process = IntertimeProcess.parseProcessSpec(options.getOptionValues("S"));
		
		KLSender sender = new KLSender(k, l, tx_process, paths);
		
		sender.startSending(num_messages);
		
	}

}




