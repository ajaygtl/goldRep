/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/ejb/api/CsiApi.java,v 1.8 2002/12/05 12:49:07 victoria.ovdenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.ejb.api;

import javax.ejb.EJBObject;

/**
 * The remote interface of the CsiApi EJB.
 *
 * @author kpathuri
 * @author Vadim Gritsenko
 * @version $Revision: 1.8 $
 *
 * @see CsiApiBusiness
 */
public interface CsiApi extends EJBObject, CsiApiBusiness {

}
