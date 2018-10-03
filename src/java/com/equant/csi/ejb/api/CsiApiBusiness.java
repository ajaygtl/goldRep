/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/ejb/api/CsiApiBusiness.java,v 1.5 2002/12/05 15:57:38 victoria.ovdenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.ejb.api;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Vector;

import com.equant.csi.exceptions.CSIException;

/**
 * The business interface of the CsiApi EJB. This EJB is the provider of the CSI functionality
 * to external systems. External systems will only be given access to this ejb.
 *
 * @author kpathuri
 * @author Vadim Gritsenko
 * @author Vasyl Rublyov
 * @version 3.0
 */
public interface CsiApiBusiness {

    /**
     * This function returns a vector of service elements.
     * The query can be done using sites, sub-billing-accounts,
     * contract or vpn.
     *
     * @param custId customer id of the component used to build the query
     * @param siteId site id of the component used to build the query
     * @return a vector of ServiceNames
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call.
     */
    public Vector getServices(String custId, String siteId, String coreSiteId, String addressId) throws RemoteException;

    /**
     * Handles the order send by Gold.
     *
     * @param xmlMessage the data in XML format
     *
     * @throws RemoteException a remote exception is thrown for any errors
     *                         during the save/modify/disconnect order
     */
    public void handleOrder(String xmlMessage) throws RemoteException;


    /**
     * Returns a XML String with all service details . This function will only be
     * called by the GOLD System.
     *
     * @param customerId  customer id of the component used to build the query
     * @param siteId      site id of the component used to build the query
     * @param serviceName service name of the component used to build the query
     * @return the service details for the provided customerId, siteId, serviceName
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call.
     */
    public String getServiceDetails(String customerId, String siteId, String coreSiteId, String addressId,  String serviceName) throws RemoteException;

    /**
     * Returns a XML String with CSP service details . This function will only be
     * called by the GOLD System.
     *
     * @param customerId  customer id of the component used to build the query
     * @param serviceName CSP service name of the component used to build the query
     * @return the service details for the provided customerId, siteId, serviceName
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call.
     */
    public String getCSPServiceDetails(String customerId, String serviceName) throws RemoteException;

    /**
     * Returns a XML String with Service Element details for given USID.
     * This function will only be called by the GOLD System.
     *
     * @param usid service element usid
     * @return the service element details for the provided usid
     *         returns null if no service element exists
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call.
     */
    public String serviceLookup(String usid) throws RemoteException;

    /**
     * Merges 2 Service Elements (passing pricing information from on to another one)
     * The first USID must belong to GOLD source system (source)
     * The second USID must belong to CIS source system (target)
     * This function will only be called by the GOLD System.
     *
     * @param goldUsid GOLD Service Element USID
     * @param cisUsid CIS Service Element USID
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call or
     *                         failed service merge
     */
    public void serviceMerge(String goldUsid, String cisUsid) throws RemoteException;

    /**
     * retrieve the service list for all site for the customer
     *
     * @param customerId - String Customer/OrganizationID
     * @return List of SiteHandles (keys) and List of Active Services at the site (value)

     * @throws RemoteException
     * @throws CSIException
     */
    public Map getBulkServiceList(String customerId) throws RemoteException;
    
    /**
     * 
     * @param usid
     * @return
     * @throws RemoteException
     */
    public String getLatestServiceElementStatus(String usid) throws RemoteException;

    /**
     * Retrieves CustomerId, SiteHandle and ServiceHandle based on ServiceElement USID
     * 
     * @param usid
     * @return String[3] corresponding CustomerId, SiteHandle and ServiceHandle or null if USID does not exst
     * @throws RemoteException if any problem
     */
    public String[] getCustomerInfoByServiceElementUSID(String usid) throws RemoteException;

   /**
    * method for GOLD JMX to kick the run
    *
    * @param runType     
    * @param productList
    */
   public void callRunCISExtract(int runType, String productList) throws RemoteException;
   
   /**
    * method for GOLD JMX to kick the run
    *
    * @param isMontlyRun     
    * @param storage
    */
   public void runExceptionReport(boolean isMontlyRun, String storage) throws RemoteException;
   
   /**
    * method for running "Dragon" migration script from external applications.
    * 
    * @param cvsFileName - The Migration Report File (csv)
    *       -   Old Router Usid (for Level 3)
    *       -   New Router Usid (for level 3)
    *       -   Old Access Usid (for "pure" level 2)
    *       -   New Access Usid (for "pure" level 2)
    * @throws RemoteException
    */
   //public void callRunDragon(String cvsFileName) throws RemoteException;
}
