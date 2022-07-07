package org.ml.options;



/**
 * <code>XMLParsingException</code> is thrown if an XML file provided to define
 * option sets and options contains errors
 */
public class XMLParsingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new
     * <code>XMLParsingException</code> exception with
     * <code>null</code> as its detail message. The cause is not initialized,
     * and may subsequently be initialized by a call to {@link #initCause}.
     */
    public XMLParsingException() {
        super();
    }

    /**
     * Constructs a new
     * <code>XMLParsingException</code> exception with the specified detail
     * message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later
     * retrieval by the {@link #getMessage()} method.
     */
    public XMLParsingException(String message) {
        super(message);
    }

    /**
     * Constructs a new
     * <code>XMLParsingException</code> exception with the specified detail
     * message and cause. <p>Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in this
     * <code>XMLParsingException</code> exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by
     * the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the
     * {@link #getCause()} method). (A <pre>null</pre> value is permitted, and
     * indicates that the cause is nonexistent or unknown.)
     */
    public XMLParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new
     * <code>XMLParsingException</code> exception with the specified cause and a
     * detail message of <pre>(cause==null ? null : cause.toString())</pre>
     * (which typically contains the class and detail message of
     * <pre>cause</pre>). This constructor is useful for
     * <code>XMLParsingException</code> exceptions that are little more than
     * wrappers for other throwables.
     *
     * @param cause the cause (which is saved for later retrieval by the
     * {@link #getCause()} method). (A <pre>null</pre> value is permitted, and
     * indicates that the cause is nonexistent or unknown.)
     */
    public XMLParsingException(Throwable cause) {
        super(cause);
    }
}
