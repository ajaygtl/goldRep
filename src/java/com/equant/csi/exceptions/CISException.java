package com.equant.csi.exceptions;

/**
 * Created by IntelliJ IDEA.
 * User: szhu
 * Date: Jul 9, 2004
 * Time: 9:30:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class CISException extends CSIException{

    /**
     * Creates a new <code>CISException</code> instance.
     *
     * @param s a <code>String</code> value
     */
    public CISException (String s) {
        super(s);
    }

    /**
     *
     * @param e
     */
    public CISException (Exception e) {
        super(e);
    }

    /**
     * 
     * @param msg
     * @param e
     */
    public CISException (String msg, Exception e) {
        super(msg, e);
    }

}
