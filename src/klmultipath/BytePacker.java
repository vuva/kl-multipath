package klmultipath;


/**
 * Filling up packets we often have to copy data into a byte array.
 * There probably is a Java class for this, but I'm just going to write this anyway.
 * 
 * This class is big-endian.
 * 
 * @author brenton
 *
 */
public class BytePacker {

	public static void putInteger(byte[] b, int loc, int val) {
		if ((b.length - loc) < 4) {
			throw new IllegalArgumentException("buffer is too small to pack an integer at this location");
		}
		for (int i=(loc+3); i>=loc; i--) {
			b[i] = (byte) (val & 0xFF);
			val = val >> 8;
		}
	}
	
	public static void putLong(byte[] b, int loc, long val) {
		if ((b.length - loc) < 8) {
			throw new IllegalArgumentException("buffer is too small to pack a long at this location");
		}
		for (int i=(loc+7); i>=loc; i--) {
			b[i] = (byte) (val & 0xFF);
			val = val >> 8;
		}
	}
	
	public static int getInteger(byte[] b, int loc) {
		if ((b.length - loc) < 4) {
			throw new IllegalArgumentException("buffer is too extract an integer from this location");
		}
		int val = 0;
		for (int i=loc; i<(loc+4); i++) {
			val <<= 8;
			val |= (b[i] & 0xFF);
		}
		return val;
	}
	
	public static long getLong(byte[] b, int loc) {
		if ((b.length - loc) < 8) {
			throw new IllegalArgumentException("buffer is too extract a long from this location");
		}
		long val = 0;
		for (int i=loc; i<(loc+8); i++) {
			val <<= 8;
			val |= (b[i] & 0xFF);
		}
		return val;
	}
}
