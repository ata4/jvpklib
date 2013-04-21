/*
 ** 2013 April 21
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.vpk;

import java.io.IOException;

/**
 * Exception class for VPK data errors.
 * 
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class VPKException extends IOException {

    /**
     * Creates a new instance of
     * <code>VPKException</code> without detail message.
     */
    public VPKException() {
    }

    /**
     * Constructs an instance of
     * <code>VPKException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public VPKException(String msg) {
        super(msg);
    }
    
    public VPKException(Throwable cause) {
        super(cause);
    }

    public VPKException(String message, Throwable cause) {
        super(message, cause);
    }
}
