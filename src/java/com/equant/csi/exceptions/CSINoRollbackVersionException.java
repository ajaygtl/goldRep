/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/exceptions/CSIIllegalStateException.java,v 1.5 2002/12/05 12:49:07 victoria.ovdenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.exceptions;

/**
 * The CSI Exception class that used for handling the illegal
 * combination of order type / order status.
 *
 * @author Vasyl Rublyov
 * @version $Revision: 1.5 $
 *
 * @see CSIException
 */
public class CSINoRollbackVersionException extends CSIException {

    /**
     * Creates new <code>CSIIllegalStateException</code> without detail message.
     *
     */
    public CSINoRollbackVersionException() {
    }

    /**
     * Constructs an <code>CSIIllegalStateException</code> with the specified
     * detail message.
     *
     * @param message the detail message.
     */
    public CSINoRollbackVersionException(String message) {
        super(message);
    }

    /**
     * Constructs and <code>CSIIllegalStateException</code> with specified
     * detail message and exception.
     *
     * @param message the detail message
     * @param exception nested
     *
     */
    public CSINoRollbackVersionException(String message, Exception exception) {
        super(message, exception);
    }

    /**
     * Constructs and <code>CSIIllegalStateException</code> with specified
     * exception.
     *
     * @param exception nested
     *
     */
    public CSINoRollbackVersionException(Exception exception) {
        super(exception);
    }
}
