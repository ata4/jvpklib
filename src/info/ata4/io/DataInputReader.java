/*
 ** 2013 Juni 22
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.io;

import java.io.DataInput;
import java.io.IOException;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class DataInputReader {
    
    private static final String DEFAULT_CHARSET = "ASCII";
    
    private final DataInput in;
    
    public DataInputReader(DataInput in) {
        this.in = in;
    }
    
    /*
     * Reads an unsigned integer value and returns the value as long.
     */
    public long readUnsignedInt() throws IOException {
        return in.readInt() & 0xffffffffL;
    }
    
    /**
     * Reads a null-terminated string.
     * 
     * @param limit maximum amount of bytes to read before truncation
     * @param charset character set to use when converting the bytes to string
     * @param padded if set to true, always read "limit" bytes and skip anything
     *               after the null char. Otherwise, stop reading after the null
     *               char.
     * @return string
     * @throws IOException 
     */
    public String readStringNull(int limit, String charset, boolean padded) throws IOException {
        if (limit <= 0) {
            throw new IllegalArgumentException("Invalid limit");
        }
        
        byte[] raw = new byte[limit];
        int length = 0;
        for (byte b; length < raw.length && (b = in.readByte()) != 0; length++) {
            raw[length] = b;
        }
        
        if (padded) {
            in.skipBytes(limit - length - 1);
        }
        
        return new String(raw, 0, length, charset);
    }
    
    /**
     * Reads a null-terminated string without byte padding.
     * 
     * @param limit maximum amount of bytes to read before truncation
     * @param charset character set to use when converting the bytes to string
     * @return string
     * @throws IOException 
     */
    public String readStringNull(int limit, String charset) throws IOException {
        return readStringNull(limit, charset, false);
    }
    
    /**
     * Reads a null-terminated string without byte padding, using the ASCII charset.
     * 
     * @param limit maximum amount of bytes to read before truncation
     * @param charset character set to use when converting the bytes to string
     * @return string
     * @throws IOException 
     */
    public String readStringNull(int limit) throws IOException {
        return readStringNull(limit, DEFAULT_CHARSET);
    }
    
    /**
     * Reads a null-terminated string without byte padding, using the ASCII
     * charset and with a limit of 256 bytes.
     * 
     * @param limit maximum amount of bytes to read before truncation
     * @param charset character set to use when converting the bytes to string
     * @return string
     * @throws IOException 
     */
    public String readStringNull() throws IOException {
        return readStringNull(256);
    }
}
