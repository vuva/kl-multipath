package klmultipath;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Since this package is making time measurements between different
 * machines, and the measurements require precision finer than ms, we
 * need some way to track the difference in the systems' clocks.
 * 
 * This is my completely hokey attempt at doing that.
 * 
 * The measurements only need to be made on the RX end, since that's where
 * the data is finally recorded.
 * 
 * This class will run a separate thread and exchange messages with the TX end.
 * - It will send over the control network.
 * - It will send a 16 byte UDP packet containing its current nanoTime
 * - The TX will reply with the same packet with its nanoTime added
 * - The RX can then receive this, measure its nanoTime again, and have a sample
 *   of the round trip between pcs, as well as their clocks.
 * 
 * @author brenton
 *
 */
public class NanoSynchro extends Thread {
	
	public static final int PORT = 7656;
	public static final int NUM_SAMPLES = 1024;

	public boolean is_server = false;
	InetAddress server_addr = null;
	
	long[] samples = new long[NUM_SAMPLES];
	
	DatagramSocket socket = null;

	/**
	 * Constructor
	 * 
	 * @param is_server
	 * @param server_ip
	 */
	public NanoSynchro(boolean is_server, String server_ip) {
		this.is_server = is_server;
		if (server_ip != null) {
			try {
				server_addr = 	InetAddress.getByName(server_ip);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		if (is_server) {
			
		}
	}
	
	
	@Override
	public void run() {
		
	}
	
	
}
