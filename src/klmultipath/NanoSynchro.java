package klmultipath;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Since this package is making time measurements between different
 * machines, and the measurements require precision finer than ms, we
 * need some way to track the difference in the systems' ns clocks.
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
 *   The protocol may work like this:
 *   - RX sends 8-byte frame containing its curtime
 *   - TX responds by sending a 16-byte packet  containing the RX data and the TX's own curtime
 *   - upon receiving the response, the RX has a sample of the transit time in each direction
 *   
 *   Notes:
 *   - the TX time in each direction includes he time to transit the stack up to the java program
 *   - any individual sample will have noise.  Better to do some aggregating of the time correction
 * 
 * @author brenton
 *
 */
public class NanoSynchro extends Thread {
	
	public static final int QPORT = 7656;
    public static final int RPORT = 7657;
	public static final int NUM_SAMPLES = 256;
	public static final int SEND_INTERVAL = 100;   // in ms

	InetAddress server_addr = null;
    DatagramSocket send_socket = null;
    DatagramSocket recv_socket = null;
    
    // in the case where I debug and run both sender and receiver on the same machine,
    // I can't have them both listening on he same port.  This will get assigned
    // QPORT on the side that sends the initial sync packets, and to RPORT on
    // the side that sends the sync responses.
    int destPort = 0;
	
	long[] roundTripTime = new long[NUM_SAMPLES];
    long[] clockDiff = new long[NUM_SAMPLES];
    int tofIndex = 0;
    boolean samplesFull = false;
	
	private long current_correction = 0;
	
	private boolean is_running = true;
	
	/**
	 * Constructor
	 * 
	 * @param is_server
	 * @param server_ip
	 */
	public NanoSynchro(String server_ip) {
	    //System.err.println("NanoSynchro "+server_ip);
		if (server_ip != null) {
		    destPort = QPORT;
			try {
				server_addr = 	InetAddress.getByName(server_ip);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
		} else {
	          destPort = RPORT;
		}
		
		// both RX and TX sides of the experiment have to listen for incoming datagrams and send
		// but the TX side only has to respond to incoming packets.
		try {
            send_socket = new DatagramSocket(null);
        } catch (SocketException e) {
            e.printStackTrace();
        }
		
		try {
            recv_socket = new DatagramSocket(null);
        } catch (SocketException e) {
            e.printStackTrace();
        }
		
        try {
            if (server_ip != null) {
                this.recv_socket.bind(new InetSocketAddress(RPORT));
            } else {
                this.recv_socket.bind(new InetSocketAddress(QPORT));
            }
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
	}
	
	
	@Override
	public void run() {

	    // the RX side has to spawn a separate thread for sending the time sync packets
	    Thread syncSender = null;
	    if (server_addr != null) {
	        InetAddress server_addr = this.server_addr;
	        syncSender = new Thread() {
	            public void run() {
	                byte[] syncData = new byte[8];

	                while (true) {
	                    if (Thread.currentThread().isInterrupted()) {
	                        break;
	                    }
	                    try {
	                        Thread.sleep(SEND_INTERVAL);
	                    } catch (InterruptedException e) {
	                        e.printStackTrace();
	                    }

	                    // create the sync packet data
	                    BytePacker.putLong(syncData, 0, System.nanoTime());
	                    
	                    //System.out.println("NanoSynchro: SEND");

	                    try {
	                        send_socket.send(new DatagramPacket(syncData,
	                                syncData.length,
	                                server_addr,
	                                NanoSynchro.QPORT));
	                    } catch (IOException e) {
	                        e.printStackTrace();
	                    }
	                }
	            }
	        };
	        syncSender.start();
	    }
	    
	    // the main listener loop.
        // If it receives an 8-byte packet it will respond by appending it's current time
        // If it receives a 16-byte packet it will record the transit times and recompute
        // the current time diff
        byte[] syncResponse = new byte[16];
        byte[] rxData = new byte[2048];
        DatagramPacket rxPacket = new DatagramPacket(rxData, rxData.length);
        while (is_running) {
            try {
                recv_socket.receive(rxPacket);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            //System.out.println("NanoSynchro: RECV");

            // 8-byte packet - respond to it
            if (rxPacket.getLength() == 8) {
                //System.out.println("NanoSynchro: RECV 8-byte");
                
                BytePacker.putBytes(syncResponse, 0, rxPacket.getData(), 0, 8);
                BytePacker.putLong(syncResponse, 8, System.nanoTime());
                
                // send the response back to the sender
                try {
                    send_socket.send(new DatagramPacket(syncResponse,
                            syncResponse.length,
                            rxPacket.getAddress(),
                            NanoSynchro.RPORT));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            // 16-byte packet - record the transit times
            if (rxPacket.getLength() == 16) {
                //System.out.println("NanoSynchro: RECV 16-byte");

                long rxTime = System.nanoTime();
                long txTime = BytePacker.getLong(rxPacket.getData(), 0);
                long respTime = BytePacker.getLong(rxPacket.getData(), 8);

                // TODO: should check that the times in the two directions don't differ by too much
                //       but how to do that until we've computed the clock difference....?

                // this assumes that the transit times in both directions are equal
                roundTripTime[tofIndex] = rxTime - txTime;
                clockDiff[tofIndex] = (txTime + rxTime)/2 - respTime;
                tofIndex = (tofIndex + 1) % NUM_SAMPLES;
                if (tofIndex == 0) samplesFull = true;
                
                // if we have enough data, update the current clock offset
                if (samplesFull) {
                    updateClockCorrection();
                }
                System.out.println(""+tofIndex+"\t"+rxTime+"\t"+txTime+"\t"+respTime+"\t"+((txTime + rxTime)/2 - respTime)+"\t"+current_correction);
            }
        }
        if (syncSender != null) syncSender.interrupt();
	}
	
	
	/**
	 * Use the current clock diff samples to estimate the current clock difference
	 * This assumes a static clock offset with noise in the measurements.
	 * Would be better to account for drift... how complicated should we make this?
	 */
	public void updateClockCorrection() {
	    long[] diffs = this.clockDiff.clone();
	    Arrays.sort(diffs);
	    
	    // we want to use the middle half of the samples
	    // could do the math with longs, but I'm worried it might overflow....
	    double sum = 0.0;
	    for (int i=NUM_SAMPLES/4; i<3*NUM_SAMPLES/4; i++) {
	        sum += (double) diffs[i];
	    }
	    this.current_correction = (long) (2.0 * sum / NUM_SAMPLES);
	}
	
	
	/**
	 * return the current ns clock correction between TX and RX
	 * 
	 * @return
	 */
	public long getClockCorrection() {
	    return current_correction;
	}
	
	
	/**
	 * This will set a flag telling the NanoSynchro to stop its listener thread.
	 */
	public void requestStop() {
	    //System.err.println("requestStop()");
	    is_running = false;
	}
	
	
}
