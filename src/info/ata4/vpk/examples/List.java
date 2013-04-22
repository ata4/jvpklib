/*
 ** 2013 April 20
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.vpk.examples;

import info.ata4.vpk.VPKArchive;
import info.ata4.vpk.VPKEntry;
import java.io.File;

/**
 * Example: list all entries in a VPK archive.
 * 
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class List {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        for (String arg : args) {
            File file = new File(arg);
            VPKArchive vpk = new VPKArchive();
            
            System.out.println(file);
            
            try {
                vpk.load(file);
            } catch (Exception ex) {
                System.err.println("Can't open archive: " + ex.getMessage());
                return;
            }
            
            for (VPKEntry entry : vpk.getEntries()) {
                if (vpk.isMultiChunk()) {
                    System.out.printf("%s:%s\n", entry.getFile().getName(), entry.getPath());
                } else {
                    System.out.println(entry.getPath());
                }
            }
        }
    }
}
