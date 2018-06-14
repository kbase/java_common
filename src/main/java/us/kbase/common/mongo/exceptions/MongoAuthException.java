package us.kbase.common.mongo.exceptions;

/**
 * Thrown when authorization to a mongo database is denied.
 * @author gaprice@lbl.gov
 *
 */
public class MongoAuthException extends Exception {

    private static final long serialVersionUID = 1L;

    public MongoAuthException() { super(); }
    public MongoAuthException(String message) { super(message); }
    public MongoAuthException(String message, Throwable cause) { super(message, cause); }
    public MongoAuthException(Throwable cause) { super(cause); }
}
