package klmultipath.randomprocesses;

import java.util.Random;


/**
 * This interface represents a random process of things arriving or being
 * serviced.  I defines the basic API where the user can ask: "When will the
 * next thing happen?"
 * 
 * That thing could be arrivals or services, and the implementing class
 * could be constrained above or below or both.  This just defines the API.
 * 
 * Intertime processes should be allocated by calling the static method: parseProcessSpec()
 * 
 * @author brenton
 *
 */
public abstract class IntertimeProcess {
	
	protected static Random rand = new Random();
	
	/**
	 * This method should be called for getting inter-arrival times.
	 * If you omit the jobSize it will default to 1.
	 * 
	 * @param jobSize
	 * @return
	 */
	public double nextInterval(int jobSize) {
		throw new UnsupportedOperationException("ERROR: method not implemeted");
	}
	
	public double nextInterval() {
		return nextInterval(1);
	}
	
	/**
	 * This method should be called for getting service times.
	 * Because service processes can be idle, you need to pass in
	 * the current time so the process knows what time "now" is.
	 * 
	 * @param curentTime
	 * @return
	 */
	public double nextInterval(double curentTime) {
		throw new UnsupportedOperationException("ERROR: method not implemeted");
	}
	
	/**
	 * This returns a new instance of whatever type of IntertimeProcess you have,
	 * configured with the same parameters.
	 */
	public abstract IntertimeProcess clone();
	
	/**
	 * return a tab-separated string containing the processes parameters
	 * 
	 * @return
	 */
	public abstract String processParameters();
	
	
	/**
	 * Parse the arguments given for either the arrival or service process, and
	 * return an appropriately configured IntertimeProcess.
	 * 
	 * @param process_spec
	 * @return
	 */
	public static IntertimeProcess parseProcessSpec(String[] process_spec) {
		System.out.println("process spec: "+process_spec+"  length: "+process_spec.length);
		IntertimeProcess process = null;
		if (process_spec[0].equals("x")) {
			// exponential
			double rate = Double.parseDouble(process_spec[1]);
			process = new ExponentialIntertimeProcess(rate);
		} else if (process_spec[0].equals("e")) {
			// erlang k
			int k = Integer.parseInt(process_spec[1]);
			double rate = Double.parseDouble(process_spec[2]);
			process = new ErlangIntertimeProcess(rate, k);
		} else if (process_spec[0].equals("g") || process_spec[0].equals("n")) {
			// gaussian/normal
			double mean = Double.parseDouble(process_spec[1]);
			double var = Double.parseDouble(process_spec[2]);
			process = new FullNormalIntertimeProcess(mean, var);
		} else if (process_spec[0].equals("w")) {
			// weibull
			double shape = Double.parseDouble(process_spec[1]);
			if (process_spec.length == 2) {
				// normalized to have mean 1.0
				process = new WeibullIntertimeProcess(shape);
			} else if (process_spec.length == 3) {
				double scale = Double.parseDouble(process_spec[2]);
				process = new WeibullIntertimeProcess(shape, scale);
			}
		} else if (process_spec[0].equals("c")) {
			// constant inter-arrival times
			double rate = Double.parseDouble(process_spec[1]);
			process = new ConstantIntertimeProcess(rate);
		} else {
			System.err.println("ERROR: unable to parse process spec!");
			System.exit(1);
		}
		
		return process;
	}
}
