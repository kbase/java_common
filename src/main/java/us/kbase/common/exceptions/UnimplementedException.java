package us.kbase.common.exceptions;

/**
 * Thrown when an operation is intentionally unimplemented or has not been
 * implemented yet.
 * @author gaprice@lbl.gov
 *
 */
public class UnimplementedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnimplementedException() { super(); }
    public UnimplementedException(String message) { super(message); }
    public UnimplementedException(String message, Throwable cause) { super(message, cause); }
    public UnimplementedException(Throwable cause) { super(cause); }
}
