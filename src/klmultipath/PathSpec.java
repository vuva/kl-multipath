package klmultipath;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class contains the information about a src/dest pair.
 * That is, the IP addresses of the two endpoints.
 * 
 * 
 * @author brenton
 *
 */
public class PathSpec {
	
	InetAddress src;
	InetAddress dest;
	
	/**
	 * Constructor.
	 * This does the work of parsing the IP addresses out of a string
	 * of the form X.X.X.X:Y.Y.Y.Y.
	 * 
	 * @param s
	 */
	public PathSpec(String s) {
		String[] ips = s.split(":", 0);
		if (ips.length != 2) {
			throw new IllegalArgumentException("ERROR: malformated path spec: "+s);
		}
		try {
			src = InetAddress.getByName(ips[0]);
			dest = InetAddress.getByName(ips[1]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

}


