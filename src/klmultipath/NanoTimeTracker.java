package klmultipath;

/**
 * In order to measure packet delays we need to measure on a very small
 * ns level.  But System.nanoTime() and other ns-scale time measurement
 * resources seem to have issues.  Issues with not being synchronized
 * between CPUs, or between cores of the same CPU.  This class is just
 * a quick and easy experiment to see what happens on this machine.
 * 
 * @author brenton
 *
 */

public class NanoTimeTracker extends Thread {

	public static final int SAMPLE_SIZE = (0x00000001 << 10) + 1; // 1024
	long[] tsamples = new long[SAMPLE_SIZE];
	int sample_index = 0;
	boolean samples_full = false;
	
	public NanoTimeTracker() {
		
	}

	@Override
	public void run() {
		System.out.println("running...");
		long t2 = System.nanoTime();
		long t = System.nanoTime();
		long mean_step = 0;
		while (true) {
			t2 = t;
			t = System.nanoTime();
			long dt = t - t2;

			// really rough detection for anomalies
			// this code doesn't start having any effect until we have made a full sample
			if (dt > mean_step*2 || dt < mean_step/2) {
				if (samples_full) {
					System.out.println(sample_index+"\t"+t+"\t"+(t-t2)+"\t"+mean_step);
					samples_full = false;
					sample_index = 0;
				}
			}
			
			// sample the time difference
			tsamples[sample_index] = dt;
			if (sample_index == SAMPLE_SIZE) samples_full = true;
			
			// compute the mean of the current samples
			long sum = 0;
			for (int i=0; i<SAMPLE_SIZE; i++) {
				//System.out.println(i);
				sum += tsamples[i];
			}
			mean_step = sum >> 10;
			
			sample_index = (sample_index + 1) % SAMPLE_SIZE;
			//System.out.println(sample_index+"\t"+t+"\t"+(t-t2)+"\t"+mean_step+"\t"+sum);
			
		}
		
	}
	
	
	public static void main(String[] args) {
		NanoTimeTracker ntt = new NanoTimeTracker();
		ntt.start();
	}
	
}



