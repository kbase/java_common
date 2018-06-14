package us.kbase.common.mongo.exceptions;

/**
 * Thrown when the host provided for a mongo database is not valid.
 * @author gaprice@lbl.gov
 *
 */
public class InvalidHostException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidHostException() { super(); }
    public InvalidHostException(String message) { super(message); }
    public InvalidHostException(String message, Throwable cause) { super(message, cause); }
    public InvalidHostException(Throwable cause) { super(cause); }
}
