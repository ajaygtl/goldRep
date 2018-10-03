/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/ejb/api/CsiApiHome.java,v 1.9 2002/12/05 15:57:38 victoria.ovdenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.ejb.api;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import java.rmi.RemoteException;

/**
 * The home interface of the CsiApi EJB.
 *
 * @author kpathuri
 * @author Vadim Gritsenko
 * @version $Revision: 1.9 $
 */
public interface CsiApiHome extends EJBHome {

    /**
     * Returns the Remote Interface of the CsiApi EJB.
     *
     * @return CsiApi Remote Interface
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call.
     * @throws CreateException thrown for any error during creation of the EJB
     */
    public CsiApi create() throws RemoteException, CreateException;
}
