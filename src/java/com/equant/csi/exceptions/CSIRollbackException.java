/*
 * $IonIdea: $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2003 Equant corporation.
 */
package com.equant.csi.exceptions;

/**
 * The CSIRollbackException class represents the exception that thrown in
 * order to tell EJB to rollback current transaction.
 *
 * @author Kostyantyn Yevenko
 * @version $Revision: 1.1 $
 *
 * @see CSIException
 */
public class CSIRollbackException extends CSIException {
    /**
     * Creates new <code>CSIRollbackException</code> without detail message.
     */
    public CSIRollbackException() {
    }

    /**
     * Constructs an <code>CSIRollbackException</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public CSIRollbackException(String msg) {
        super(msg);
    }

    /**
     * Constructs and <code>CSIRollbackException</code> with specified
     * detail message and exception.
     *
     * @param message the detail message
     * @param exception nested
     *
     */
    public CSIRollbackException(String message, Exception exception) {
        super(message, exception);
    }

    /**
     * Constructs and <code>CSIRollbackException</code> with specified
     * exception.
     *
     * @param exception nested
     *
     */
    public CSIRollbackException(Exception exception) {
        super(exception);
    }
}
