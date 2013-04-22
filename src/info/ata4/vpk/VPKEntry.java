/*
 ** 2013 April 20
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.vpk;

import info.ata4.util.io.NIOFileUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class VPKEntry {
    
    private final File vpkFile;
    private ByteBuffer bb;
    private String type;
    private String name;
    private String dir;
    private long crc32;
    private int offset;
    private int size;
    private byte[] preload = new byte[0];
    private boolean readOnly;
    
    VPKEntry(File vpkFile, boolean readOnly) {
        this.vpkFile = vpkFile;
        this.readOnly = readOnly;
    }
    
    /**
     * Returns the VPK archive file for this entry.
     * 
     * @return VPK archive file
     */
    public File getFile() {
        return vpkFile;
    }
    
    /**
     * Creates and returs a byte buffer for this entry.
     * 
     * @return byte buffer containing the data of this entry
     * @throws IOException If the buffer creation caused an error
     */
    public ByteBuffer getData() throws IOException {
        // if there's nothing defined, simply return an empty buffer
        if (size == 0 && preload.length == 0) {
            return ByteBuffer.allocate(0);
        }
        
        // don't create another buffer if a previous one was created
        if (bb != null && bb.capacity() == size) {
            return bb.duplicate();
        }
        
        // return the preloaded data directly if there's no actual size for the
        // file
        if (size == 0 && preload.length > 0) {
            return ByteBuffer.wrap(preload);
        }
        
        if (size == 0 && preload.length > 0) {
            // data is fully preloaded, simply wrap the array
            bb = ByteBuffer.wrap(preload);
        } else if (size > 0 && preload.length > 0) {
            // concat preloaded and external data
            bb = ByteBuffer.allocateDirect(getDataSize());
            bb.put(preload);
            NIOFileUtils.load(vpkFile, offset, size, bb);
        } else if (readOnly) {
            if (vpkFile.exists()) {
                // map the file directly
                bb = NIOFileUtils.openReadOnly(vpkFile, offset, size);
            } else {
                // can't create files in read-only mode
                throw new FileNotFoundException();
            }
        } else {
            bb = NIOFileUtils.openReadWrite(vpkFile, offset, size);
        }
        
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.rewind();
        
        return bb.duplicate();
    }
    
    /**
     * Checks the data integrity by comparing the saved CRC32 checksum with the
     * actual checksum. If no exception is thrown, the data is OK.
     * 
     * @throws IOException on I/O errors or if the checksum mismatches
     */
    public void checkData() throws IOException {
        long targetCrc = getCRC32();
        long actualCrc = calcCRC32();

        if (actualCrc != targetCrc) {
            throw new IOException(String.format("CRC32 checksum mismatch: got 0x%06x, expected 0x%06x", actualCrc, targetCrc));
        }
    }
    
    /**
     * Calculates the actual CRC32 checksum for the data.
     * 
     * @return actual calculated CRC32 checksum
     * @throws IOException If the checksum calculation caused an I/O error
     */
    public long calcCRC32() throws IOException {
        CRC32 crc = new CRC32();
        byte[] buf = new byte[4096];
        
        ByteBuffer bbCheck = getData();
        
        while (bbCheck.hasRemaining()) {
            int bsize = Math.min(buf.length, bbCheck.remaining());
            bbCheck.get(buf, 0, bsize);
            crc.update(buf, 0, bsize);
        }

        return crc.getValue();
    }
    
    /**
     * Updates the current target checksum from the current data.
     * 
     * @throws IOException If the checksum calculation caused an I/O error
     */
    void updateCRC32() throws IOException {
        setCRC32(calcCRC32());
    }

    /**
     * Returns the file extension for this entry.
     * 
     * @return file extension
     */
    public String getType() {
        return type;
    }

    /**
     * Sets a new file extension for this entry.
     * 
     * @param type new file extension
     */
    void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the name of this entry without its extension.
     * 
     * @return entry name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets a new name for this entry.
     * 
     * @param name new entry name
     */
    void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the directory path for this entry.
     * 
     * @return directory path
     */
    public String getDir() {
        return dir;
    }

    /**
     * Sets a new directory path for this entry.
     * 
     * @param dir directory path
     */
    void setDir(String dir) {
        this.dir = dir;
    }

    /**
     * Returns the target CRC32 checksum for this entry.
     * 
     * @return CRC32 checksum
     */
    public long getCRC32() {
        return crc32;
    }

    /**
     * Sets a new target CRC32 checksum for this entry.
     * 
     * @param crc32 CRC32 checksum
     */
    void setCRC32(long crc32) {
        this.crc32 = crc32;
    }

    /**
     * Returns the preload data for this entry.
     * 
     * @return preload data array
     */
    byte[] getPreloadData() {
        return preload;
    }

    /**
     * Sets the preload data for this entry. It should not be included with the
     * actual data buffer and no greater than 20k bytes.
     * 
     * @param preload preload data array
     */
    void setPreloadData(byte[] preload) {
        if (preload == null) {
            throw new NullPointerException();
        }
        this.preload = preload;
    }
    
    /**
     * Returns the array size for the preload data.
     * 
     * @return preload size in bytes
     */
    public int getPreloadSize() {
        return preload.length;
    }
    
    /**
     * Allocates the preload data array to the specified size.
     * 
     * @param preloadSize preload data size in bytes
     */
    void setPreloadSize(int preloadSize) {
        preload = new byte[preloadSize];
    }

    /**
     * Returns the data offset in the associated file for this entry.
     * 
     * @return file offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Sets the data offset in the associated file for this entry.
     * 
     * @param offset new file offset
     */
    void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Returns the external archive data size for this entry.
     * 
     * @return data size
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the new external archive data size for this entry.
     * 
     * @param size new data size
     */
    void setSize(int size) {
        this.size = size;
    }
    
    /**
     * Returns the full data size for this entry. It includes the size of the
     * preloaded data and the archive data.
     * 
     * @return full data size
     */
    public int getDataSize() {
        return size + preload.length;
    }
    
    /**
     * Returns the full path of this entry.
     * 
     * @return entry path
     */
    public String getPath() {
        return getDir() + getName() + "." + getType();
    }
    
    /**
     * Sets the new full path for this entry.
     * 
     * @param path new entry path
     */
    void setPath(String path) {
        setType(FilenameUtils.getExtension(path));
        setName(FilenameUtils.getBaseName(path));
        setDir(FilenameUtils.getPath(path));
    }
}
