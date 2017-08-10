package klmultipath;

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
	Integer srcPort;
	InetAddress dest;
	Integer dstPort;
	
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
			if (ips[0].contains(",")) {
				String[] sep = ips[0].split(",", 0);
				src = InetAddress.getByName(sep[0]);
				srcPort = Integer.parseInt(sep[1]) ;
			}else{
				src = InetAddress.getByName(ips[0]);
			}			

			if (ips[1].contains(",")) {
				String[] dep = ips[1].split(",", 0);
				dest = InetAddress.getByName(dep[0]);
				dstPort = Integer.parseInt(dep[1]) ;
			}else{
				dest = InetAddress.getByName(ips[1]);
			}	
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	/**
	 * toString()
	 */
	public String toString() {
		return src.toString()+":"+dest.toString();
	}
	
}


