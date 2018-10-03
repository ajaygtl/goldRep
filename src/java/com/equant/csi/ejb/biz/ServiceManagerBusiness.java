/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/ejb/biz/ServiceManagerBusiness.java,v 1.13 2002/12/05 16:51:41 victoria.ovdenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.ejb.biz;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Vector;

import com.equant.csi.exceptions.CSIException;
import com.equant.csi.jaxb.Message;

/**
 * The business interface of the ServiceManagerEJB.
 *
 * @author kpathuri
 * @author harvey
 * @author Vadim Gritsenko
 * @version $Revision: 1.13 $
 *
 * @see ServiceManagerEJB
 */
public interface ServiceManagerBusiness {
    /**
     * Processes the Message object containing in the message from CIS Interface
     * Remote/Synchronized call
     *
     * @param message the message to process
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call.
     * @throws CSIException thrown if an error occurred during message processing
     */
    public void processMessageFromCis(String message) throws RemoteException;

    /**
     * Processes the Message object containing in the message from CIS Interface
     * The call should be called within the container only
     *
     * @param message the message to process
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call.
     * @throws CSIException thrown if an error occurred during message processing
     */
    public void processMessageFromCis(Message message) throws RemoteException;

    /**
     * Processes all the Version objects containing in the message.
     *
     * @param message the message to process
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call.
     * @throws CSIException    thrown if an error occurred during message processing
     */
    public void processMessage(Message message) throws RemoteException;


    /**
     * Returns a vector of service elements.
     *
     * @param customerId customer id of the component used to build the query
     * @param siteId     site id of the component used to build the query
     * @return a vector of ServiceNames
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call.
     */
    public Vector getServices(String customerId, String siteId, String coreSiteId, String addressId) throws RemoteException;


    /**
     * Returns the <code>Message</code> containing the service details.
     *
     * @param customerId  customer id of the component used to build the query
     * @param siteId      site id of the component used to build the query
     * @param serviceName service name of the component used to build the query
     * @return the message containing the service details for the provided customerId,
     *         siteId, serviceName
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call
     */
    public Message getServiceDetails(String customerId, String siteId, String coreSiteId, String addressId, String serviceName) throws RemoteException;

    /**
     * Returns the <code>Message</code> containing the CSP service details.
     *
     * @param customerId  customer id of the component used to build the query
     * @param serviceName CSP service name of the component used to build the query
     * @return the message containing the service details for the provided customerId,
     *         siteId, serviceName
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call
     */
    public Message getCSPServiceDetails(String customerId, String serviceName) throws RemoteException;

    /**
     * Returns a XML String with Service Element details for given USID.
     * This function will only be called by the GOLD System.
     *
     * @param usid service element usid
     * @return Message the service element details for the provided usid
     *         returns null if no service element exists
     *
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call.
     */
    public Message serviceLookup(String usid) throws RemoteException;

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
     * executes and submit exception report to GOLD
     *
     * @param isMontlyRun
     * @param storage
     * @throws RemoteException
     */
    public void runExceptionReport(boolean isMontlyRun, String storage) throws RemoteException;

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
     *
     * @param runType
     * @param productList
     */
    public void runCISExtract(int runType, String productList) throws RemoteException;

    /**
     * Publish message to the 'CSI to GOLD Queue' queue
     * 
     * @param msg - message to be published
     * @return sent message as array of bytes
     * @throws RemoteException
     * @throws CSIException there is wrapped JAXBException or IOException in case if was some problem
     */
    public byte[] publishMessage( Message msg) throws RemoteException, CSIException;
}