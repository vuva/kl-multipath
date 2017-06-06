package klmultipath;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


/**
 * sudo ifconfig lo:0 127.0.0.2 netmask 255.0.0.0 up
 * sudo ifconfig lo:1 127.0.0.3 netmask 255.0.0.0 up
 * sudo ifconfig lo:2 127.0.0.4 netmask 255.0.0.0 up
 * 
 * java -cp "bin:lib/commons-cli-1.2.jar" klmultipath.KLReceiver -p 127.0.0.1:127.0.0.1 -p 127.0.0.1:127.0.0.2 -p 127.0.0.1:127.0.0.3  -p 127.0.0.1:127.0.0.4  -o testrun
 * 
 * @author brenton
 *
 */
public class KLReceiver {
	
	public static final int PORT = 7654;
	
	PathSpec[] paths = null;
	String outfile_base = "";
	DatagramSocket[] sockets = null;
	
	/**
	 * Constructor
	 * 
	 * @param path_strings
	 * @param outfile_base
	 */
	public KLReceiver(String[] path_strings, String outfile_base) {
		paths = new PathSpec[path_strings.length];
		sockets = new DatagramSocket[path_strings.length];
		this.outfile_base = outfile_base;
		
		for (int i=0; i<path_strings.length; i++) {
			paths[i] = new PathSpec(path_strings[i]);
			System.out.println(paths[i]);
			try {
				sockets[i] = new DatagramSocket(null);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Start listening for connections and recording received packets
	 */
	public void listen() {

		for (int i=0; i<paths.length; i++) {
			InetSocketAddress address = new InetSocketAddress(paths[i].dest, PORT);
			try {
				sockets[i].bind(address);
			} catch (SocketException e) {
				e.printStackTrace();
				System.exit(1);
			}
			DatagramSocket sock = sockets[i];
			int threadnum = i;
			String outfile = this.outfile_base+"_"+threadnum+".dat";
			
			Thread listener = new Thread() {
				public void run() {
					byte[] rxData = new byte[1024];
					FileWriter fstream = null;
					try {
						fstream = new FileWriter(outfile);
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}
					BufferedWriter out = new BufferedWriter(fstream);
					while (true) {
						DatagramPacket rxPacket = new DatagramPacket(rxData, rxData.length);
						try {
							sock.receive(rxPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
						// extract the info from the packet
						//long curtime = System.nanoTime();
						long curtime = System.currentTimeMillis();
						try {
							out.write(curtime+"\treceived in thread "+threadnum+"\n");
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
						System.out.println("received in thread "+threadnum);
					}
				}
			};
			listener.start();
		}
	}

	public static void main(String[] args) {

		Options cli_options = new Options();
		cli_options.addOption("h", "help", false, "print help message");
		cli_options.addOption("p", "path", true, "sender/reciever IP address pair in the form X.X.X.X:Y.Y.Y.Y");
		cli_options.addOption(OptionBuilder.withLongOpt("outfile").hasArg().isRequired().withDescription("the base name of the output files").create("o"));
		
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
		
		KLReceiver receiver = new KLReceiver(paths, options.getOptionValue("o"));
		
		receiver.listen();
		
	}
	
}
