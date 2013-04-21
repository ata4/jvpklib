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
import java.io.IOException;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class Validate {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        for (String arg : args) {
            File file = new File(arg);
            VPKArchive vpk = new VPKArchive();
            
            try {
                vpk.load(file);
            } catch (Exception ex) {
                System.err.println("Can't open archive: " + ex.getMessage());
                return;
            }

            int failed = 0;

            for (VPKEntry entry : vpk.getEntries()) {
                try {
                    entry.checkData();
                } catch (IOException ex) {
                    System.err.println(entry.getPath() + " failed! " + ex.getMessage());
                    failed++;
                }
            }

            if (failed == 0) {
                System.out.println("All files validated successfully");
            } else {
                System.out.println(failed + " files failed validation");
            }
        }
    }
}
