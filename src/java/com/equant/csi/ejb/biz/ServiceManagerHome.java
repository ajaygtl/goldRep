/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/ejb/biz/ServiceManagerHome.java,v 1.9 2002/12/05 15:57:38 victoria.ovdenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.ejb.biz;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import java.rmi.RemoteException;

/**
 * The home interface of the ServiceManager EJB.
 *
 * @author kpathuri
 * @author kodamah
 * @author Vadim Gritsenko
 * @version $Revision: 1.9 $
 */
public interface ServiceManagerHome extends EJBHome {

    /**
     * Returns the Remote Interface for the ServiceManager EJB.
     *
     * @return ServiceManager Remote Interface
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call.
     * @throws CreateException thrown for any error during creation of the EJB
     */
    public ServiceManager create()
            throws RemoteException, CreateException;
}
