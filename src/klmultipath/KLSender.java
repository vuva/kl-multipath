package klmultipath;

import java.net.DatagramSocket;

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
 * java -Xmx5g -cp "lib/commons-math3-3.6.1.jar:bin" lkmultipath.LKSender  

 * 
 * @author brenton
 *
 */
public class KLSender {

	public int k, l;
	IntertimeProcess tx_process = null;
	PathSpec[] paths = null;
	DatagramSocket[] sockets = null;
	

	public KLSender(int k, int l, IntertimeProcess tx_process, String[] path_strings) {
		this.k = k;
		this.l = l;
		this.tx_process = tx_process;
		
		// initialize the connections
		paths = new PathSpec[path_strings.length];
		sockets = new DatagramSocket[path_strings.length];
		for (int i=0; i<path_strings.length; i++) {
			paths[i] = new PathSpec(path_strings[i]);
		}
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];

	}
	
	public void startSending(int num_messages) {
		
		// send until we have sent enough
		
	}
	
	
	public static void main(String[] args) {

		Options cli_options = new Options();
		cli_options.addOption("h", "help", false, "print help message");
		cli_options.addOption("k", "k", true, "number of packets to send per transmission");
		cli_options.addOption("l", "l", true, "number of packets that must be recieved");
		cli_options.addOption("n", "numsamples", true, "number of transmission to make");
		cli_options.addOption("i", "samplinginterval", true, "samplig interval");
		cli_options.addOption("p", "path", true, "sender/reciever IP address pair in the form X.X.X.X:Y.Y.Y.Y");
		cli_options.addOption(OptionBuilder.withLongOpt("queuetype").hasArgs().isRequired().withDescription("queue type and arguments").create("q"));
		cli_options.addOption(OptionBuilder.withLongOpt("outfile").hasArg().isRequired().withDescription("the base name of the output files").create("o"));
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

		int num_messages = Integer.parseInt(options.getOptionValue("n"));
		String paths[] = options.getOptionValues("p");
		int k = Integer.parseInt(options.getOptionValue("k"));
		int l = Integer.parseInt(options.getOptionValue("l"));
		if (l > k) {
			System.err.println("ERROR: l > k");
			System.exit(0);
		}		
		
		KLSender sender = new KLSender(k, l, null, paths);
		
		sender.startSending(num_messages);
		
	}

}




