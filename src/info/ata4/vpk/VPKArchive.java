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

import info.ata4.io.DataInputReader;
import info.ata4.io.util.ByteBufferUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;

/**
 * VPK archive class.
 * 
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class VPKArchive {
    
    public static final int SIGNATURE = 0x55aa1234;
    public static final int VERS_MIN = 1;
    public static final int VERS_MAX = 2;
    
    private List<VPKEntry> entries = new ArrayList<>();
    private Map<String, List<VPKEntry>> typeEntries = new HashMap<>();
    private Map<String, List<VPKEntry>> dirEntries = new HashMap<>();
    private Map<String, VPKEntry> pathEntries = new HashMap<>();
    private int version = 1;
    private boolean multiChunk;
    
    /**
     * Loads all entries from a VPK archive file.
     * 
     * @param file VPK archive file. For multichunk archives, this must be the
     *             "_dir" index file.
     * @throws IOException when the archive can't be read correctly
     * @throws VPKException when a VPK file format error occured
     */
    public void load(File file) throws VPKException, IOException {
        File baseDir = file.getParentFile();
        String vpkName = FilenameUtils.getBaseName(file.getName());
        
        // it must be a multichunk VPK if it ends with _dir
        multiChunk = vpkName.endsWith("_dir");
        
        // strip "_dir"
        if (multiChunk) {
            vpkName = vpkName.substring(0, vpkName.length() - 4);
        }
        
        DataInputReader in = DataInputReader.newReader(ByteBufferUtils.openReadOnly(file.toPath()));
        in.setSwap(true);

        int sig = in.readInt();
        
        if (sig != SIGNATURE) {
            throw new VPKException(String.format("Unknown signature: 0x%06x (expected: 0x%06x)", sig, SIGNATURE));
        }
        
        version = in.readInt();
        int headerSize;

        switch (version) {
            case 1:
                headerSize = 12;
                break;
            case 2:
                headerSize = 28;
                // TODO: unknown fields
                int v1 = in.readInt(); // footer offset
                int v2 = in.readInt(); // always 0?
                int v3 = in.readInt(); // footer size?
                int v4 = in.readInt(); // always 48?
//                System.out.printf("%d %d %d %d\n", v1, v2, v3, v4);
                break;
            default:
                throw new VPKException("Unsupported version: " + version);
        }
        
        // dictionary size in v1 (something else in v2?)
        int dictSize = in.readInt();

        for (String type; !(type = in.readStringNull(1024)).isEmpty();) {
            if (!typeEntries.containsKey(type)) {
                typeEntries.put(type, new ArrayList<VPKEntry>());
            }
            
            for (String dir; !(dir = in.readStringNull(1024)).isEmpty();) {
                // separator should always be "/"
                dir = dir.replace('\\', '/');
                
                // fix root dir
                if (dir.equals(" ")) {
                    dir = "";
                }
                
                // add missing slash unless it's the root dir
                if (!dir.isEmpty() && !dir.endsWith("/")) {
                    dir += "/";
                }
                
                if (!dirEntries.containsKey(dir)) {
                    dirEntries.put(dir, new ArrayList<VPKEntry>());
                }
                
                for (String name; !(name = in.readStringNull(1024)).isEmpty();) {
                    long crc32 = in.readUnsignedInt();
                    int preloadSize = in.readUnsignedShort();
                    byte[] preload = new byte[preloadSize];
                    int chunkIndex = in.readUnsignedShort();
                    int offset = in.readInt();
                    int size = in.readInt();

                    int term = in.readUnsignedShort();

                    if (term != 0xffff) {
                        throw new VPKException("Unexpected terminator: " + term);
                    }

                    if (preload.length > 0) {
                        in.readFully(preload);
                    }
                    
                    File entryFile;
                    
                    if (multiChunk) {
                        String entryName = String.format("%s_%03d.vpk", vpkName, chunkIndex);
                        entryFile = new File(baseDir, entryName);
                    } else {
                        entryFile = file;
                        if (version == 1) {
                            // offset is relative to the header/dictionary, fix it
                            offset += headerSize + dictSize;
                        }
                        // TODO: how to fix offsets for v2?
                    }
                    
                    VPKEntry entry = new VPKEntry(entryFile, true);
                    entry.setType(type);
                    entry.setName(name);
                    entry.setDir(dir);
                    entry.setCRC32(crc32);
                    entry.setOffset(offset);
                    entry.setSize(size);
                    entry.setPreloadData(preload);
                    
                    entries.add(entry);
                    typeEntries.get(type).add(entry);
                    dirEntries.get(dir).add(entry);
                    pathEntries.put(entry.getPath(), entry);
                }
            }
        }
        
        // check the current position
        if (version == 1) {
            long dictSizeActual = in.position() - headerSize;
            if (dictSize != 0 && dictSizeActual != dictSize) {
                throw new VPKException(String.format("Incorrect dictionary size %d (expected %d)", dictSizeActual, dictSize));
            }
        }
    }
    
    /**
     * Returns a list of all VPK entries.
     * 
     * @return VPK entry list
     */
    public List<VPKEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
    
    /**
     * Returns a list of all VPK entries for the given directory. If the directory
     * isn't used, {@code null} will be returned.
     * 
     * @param dir directory path
     * @return VPK entry list inside the given directory
     */
    public List<VPKEntry> getEntriesForDir(String dir) {
        List<VPKEntry> result = dirEntries.get(dir);
        return result == null ? result : Collections.unmodifiableList(result);
    }
    
    /**
     * Returns a list of all VPK entries for the given file type/extension. If
     * the type isn't used, {@code null} will be returned.
     * 
     * @param type file type
     * @return VPK entry list of the given type
     */
    public List<VPKEntry> getEntriesForType(String type) {
        List<VPKEntry> result = typeEntries.get(type);
        return result == null ? result : Collections.unmodifiableList(result);
    }
    
    /**
     * Returns the VPK entry for the given path. If no entry with the path exists,
     * {@code null} will be returned.
     * 
     * @param path full file path
     * @return VPK entry for this path
     */
    public VPKEntry getEntry(String path) {
        return pathEntries.get(path);
    }
    
    /**
     * Returns the version number of this VPK archive.
     * 
     * @return VPK version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Sets a new version number for this VPK archive.
     * 
     * @param version new version number
     * @throws IllegalArgumentException if the version number is outside the
     *                                  allowed range
     */
    public void setVersion(int version) {
        if (version > VERS_MAX || version < VERS_MIN) {
            throw new IllegalArgumentException("Unsupported version: " + version);
        }
        
        this.version = version;
    }
    
    /**
     * Returns true if this archive is split up into multiple chunk files.
     * 
     * @return true if this is a multi-chunk archive
     */
    public boolean isMultiChunk() {
        return multiChunk;
    }
    
    /**
     * Sets if this archive should split up into multiple chunk files.
     * 
     * @param multiChunk multi-chunk flag
     */
    public void setMultiChunk(boolean multiChunk) {
        this.multiChunk = multiChunk;
    }
    
    /**
     * Clears all loaded entries from this archive instance. This won't effect any data.
     */
    public void clear() {
        entries.clear();
        dirEntries.clear();
        typeEntries.clear();
        pathEntries.clear();
    }
}
