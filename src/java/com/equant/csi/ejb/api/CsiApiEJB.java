/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/ejb/api/CsiApiEJB.java,v 1.29 2003/01/09 22:51:44 constantine.evenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */

package com.equant.csi.ejb.api;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Category;

import com.equant.csi.common.CSIConstants;
import com.equant.csi.ejb.biz.ServiceManager;
import com.equant.csi.ejb.biz.ServiceManagerHome;
import com.equant.csi.exceptions.CSIException;
import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.utilities.JAXBUtils;
import com.equant.csi.utilities.LoggerFactory;

/**
 * EJB class which is the interface to all CSI functionality.
 * External systems will only be given access to this ejb.
 *
 * @author kpathuri
 * @author Vadim Gritsenko
 * @author Kostyantyn Yevenko
 * @version $Revision: 1.29 $
 *
 * @see CsiApiBusiness
 */
public class CsiApiEJB implements SessionBean, CsiApiBusiness {

    /** Initializing the logger. */
    private static final Category logger = LoggerFactory.getInstance(CsiApiEJB.class.getName());

     /** Initializing the CIS logger. */
    private final static Category cis_logger = LoggerFactory.getInstance("cis_extraction.ServiceManagerEJB");

    /** The instance of ServiceManager EJB. */
    private transient ServiceManager serviceManager;

    /**
     * Creates method specified in EJB 1.1 section 6.10.3.
     *
     */
    public void ejbCreate() {
    }

    /* Methods required by SessionBean Interface. EJB 1.1 section 6.5.1. */

    /**
     * Sets the associated session context.
     *
     * @param context the session context to set
     * @see javax.ejb.SessionBean#setSessionContext(javax.ejb.SessionContext)
     */
    public void setSessionContext(SessionContext context) {
    }

    /**
     * Activates the ejb.
     *
     * @see javax.ejb.SessionBean#ejbActivate()
     */
    public void ejbActivate() {
    }

    /**
     * Passivates the ejb.
     *
     * @see javax.ejb.SessionBean#ejbPassivate()
     */
    public void ejbPassivate() {
    }

    /**
     * Removes the ejb from the container.
     *
     * @see javax.ejb.SessionBean#ejbRemove()
     */
    public void ejbRemove() {
    }

    /**
     * Creates and returns an instance of ServiceManager EJB.
     *
     * @return the instance of ServiceManager EJB
     * @throws NamingException thrown for any error during retrieving
     *                         the object with JNDI
     * @throws CreateException the creation of the ServiceManager object failed
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call
     */
    private ServiceManager getServiceManager() throws NamingException, CreateException, RemoteException {

        if (serviceManager == null) {
            InitialContext ic = new InitialContext();
         //   ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup("java:/comp/env/ejb/ServiceManager"), ServiceManagerHome.class);
        	logger.debug("Entering getServiceManager()...");
        	logger.debug("ic...");
    		ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup("com.equant.csi.ejb.biz.ServiceManagerHome"), ServiceManagerHome.class);
    		logger.debug("ServiceManagerHome hom..."+home);
            serviceManager = home.create();
            logger.debug("serviceManager..."+home);
        }
        return serviceManager;
    }

    /**
     * Returns a vector of service elements.
     * The query can be done using sites, sub-billing-accounts,
     * contract or vpn.
     *
     * @param customerId customer id of the component used to build the query
     * @param siteId     site id of the component used to build the query
     * @return a vector of ServiceNames
     */
    public Vector getServices(String customerId, String siteId, String coreSiteId, String addressId) throws RemoteException, CSIException {
        try {
            return getServiceManager().getServices(customerId, siteId, coreSiteId, addressId);
        } catch (RemoteException e) {
            logger.error("Processing exception:", e);
            throw e;
        } catch (Exception e) {
            logger.error("Can't find home interface:", e);
            throw new CSIException("Can't find home interface:", e);
        }
    }


    /**
     * Returns the details of service elements.
     *
     * @param customerId  customer id of the component used to build the query
     * @param siteId      site id of the component used to build the query
     * @param serviceName service name of the component used to build the query
     * @return the string representation of the service element's details
     */
    public String getServiceDetails(String customerId, String siteId, String coreSiteId, String addressId, String serviceName) throws RemoteException, CSIException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Get service details. Calling ServiceManager...");
            }

            Message message = getServiceManager().getServiceDetails(customerId, siteId, coreSiteId, addressId, serviceName);

            if (logger.isDebugEnabled()) {
                logger.debug("Prepare XML output");
            }

            return JAXBUtils.marshalMessage(message);
        } catch (RemoteException e) {
            logger.error("Can't find home interface or business error occured:", e);
            throw e;
        } catch (NamingException e) {
            logger.error("Can't lookup the ServiceManager EJB:", e);
            throw new CSIException("Can't lookup the ServiceManager EJB:", e);
        } catch (CreateException e) {
            logger.error("Can't create ServiceManager EJB instance:", e);
            throw new CSIException("Can't create ServiceManager EJB instance:", e);
        } catch (JAXBException e) {
            logger.error("Can't process the message object:", e);
            throw new CSIException("Can't process the message object:", e);
        } catch (IOException e) {
            logger.error("Can't write the message object:", e);
            throw new CSIException("Can't write the message object:", e);
        }
    }

    /**
     * Returns CSP details of service elements.
     *
     * @param customerId  customer id of the component used to build the query
     * @param serviceName CSP service name of the component used to build the query
     * @return the string representation of the service element's details
     */
    public String getCSPServiceDetails(String customerId, String serviceName) throws RemoteException {
        try {
            if (logger.isDebugEnabled())
                logger.debug("Get service details. Calling ServiceManager...");

            Message message = getServiceManager().getCSPServiceDetails(customerId, serviceName);
            if(message==null) {
                return null;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Prepare XML output");
            }

            return JAXBUtils.marshalMessage(message);
        } catch (RemoteException e) {
            logger.error("Can't find home interface or business error occured:", e);
            throw e;
        } catch (NamingException e) {
            logger.error("Can't lookup the ServiceManager EJB:", e);
            throw new CSIException("Can't lookup the ServiceManager EJB:", e);
        } catch (CreateException e) {
            logger.error("Can't create ServiceManager EJB instance:", e);
            throw new CSIException("Can't create ServiceManager EJB instance:", e);
        } catch (JAXBException e) {
            logger.error("Can't process the message object:", e);
            throw new CSIException("Can't process the message object:", e);
        } catch (IOException e) {
            logger.error("Can't write the message object:", e);
            throw new CSIException("Can't write the message object:", e);
        }
    }

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
    public String serviceLookup(String usid) throws RemoteException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Get service lookup. Calling ServiceManager...");
            }

            Message message = getServiceManager().serviceLookup(usid);
            if(message==null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No Version/Message returned, exiting with null.");
                }
                return null;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Prepare XML output");
            }

            return JAXBUtils.marshalMessage(message);
        } catch (RemoteException e) {
            logger.error("Can't find home interface or business error occured:", e);
            throw e;
        } catch (NamingException e) {
            logger.error("Can't lookup the ServiceManager EJB:", e);
            throw new CSIException("Can't lookup the ServiceManager EJB:", e);
        } catch (CreateException e) {
            logger.error("Can't create ServiceManager EJB instance:", e);
            throw new CSIException("Can't create ServiceManager EJB instance:", e);
        } catch (JAXBException e) {
            logger.error("Can't process the message object:", e);
            throw new CSIException("Can't process the message object:" + e.getMessage());
        } catch (IOException e) {
            logger.error("Can't write the message object:", e);
            throw new CSIException("Can't write the message object:", e);
        }
    }

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
    public void serviceMerge(String goldUsid, String cisUsid) throws RemoteException {
        try {
            getServiceManager().serviceMerge(goldUsid, cisUsid);
        } catch(CSIException e) {
            logger.error("CsiApiEJB.serviceMerge got CSIException - " + e.getMessage(), e);
            throw e;
        } catch(Exception e) {
            logger.error("CsiApiEJB.serviceMerge got an exception \"" + e.getClass().getName() + "\" - " + e.getMessage(), e);
            throw new CSIException("CsiApiEJB.serviceMerge got an exception \"" + e.getClass().getName() + "\" - " + e.getMessage(), e);
        }
    }

    /**
     * retrieve the service list for all site for the customer
     *
     * @param customerId - String Customer/OrganizationID
     * @return List of SiteHandles (keys) and List of Active Services at the site (value)

     * @throws CSIException
     */
    public Map getBulkServiceList(String customerId) throws CSIException {
        try {
            return getServiceManager().getBulkServiceList(customerId);
        } catch(CSIException e) {
            logger.error("CsiApiEJB.serviceMerge got CSIException - " + e.getMessage(), e);
            throw e;
        } catch(Exception e) {
            logger.error("CsiApiEJB.serviceMerge got an exception \"" + e.getClass().getName() + "\" - " + e.getMessage(), e);
            throw new CSIException("CsiApiEJB.serviceMerge got an exception \"" + e.getClass().getName() + "\" - " + e.getMessage(), e);
        }
   }

    /**
     * Handles the order send by Gold.
     *
     * @param xmlMessage the data in XML format
     */
    public void handleOrder(String xmlMessage) throws CSIException {
        try {
        	
        	logger.debug("before JAXBUtils.unmarshalMessage handleOrder "+xmlMessage);
        	Message message = JAXBUtils.unmarshalMessage(xmlMessage);
        	logger.debug("before JAXBUtils.unmarshalMessage handleOrder "+xmlMessage);
            if(message==null) {
                logger.error("Null MessageType? XML is " + xmlMessage);
                return;
            }

             if (message.getVersion() != null && message.getVersion().size() != 0
                     && message.getVersion().get(0) != null
                     && CSIConstants.SYSTEM_TYPE_CIS.equals(((VersionType) message.getVersion().get(0)).getSystem().getId())
                     && cis_logger.isDebugEnabled()) {
                 cis_logger.debug("CIS message from incoming GOLD Queue, XML:\n" + xmlMessage);
             } else if (logger.isDebugEnabled()) {
                 logger.debug("Build Message object from incoming XML:\n" + xmlMessage);
             }

            getServiceManager().processMessage(message);
        } catch (CSIException e) {
            logger.error("Can't find home interface or business error occured:", e);
            throw e;
        } catch (NamingException e) {
            logger.error("Can't lookup the ServiceManager EJB:", e);
            throw new CSIException("Can't lookup the ServiceManager EJB:", e);
        } catch (CreateException e) {
            logger.error("Can't create ServiceManager EJB instance:", e);
            throw new CSIException("Can't create ServiceManager EJB instance:", e);
        } catch (JAXBException e) {
            logger.error("Can't process the message object:", e);
            throw new CSIException("Can't process the message object:", e);
        } catch (IOException e) {
            logger.error("Can't write the message object:", e);
            throw new CSIException("Can't write the message object:", e);
        }
    }
    
    /**
     * 
     */
    public String getLatestServiceElementStatus(String usid) throws CSIException {
        try {
            return getServiceManager().getLatestServiceElementStatus(usid);
        } catch(CSIException e) {
            logger.error("CsiApiEJB.getLatestServiceElementStatus got CSIException - " + e.getMessage(), e);
            throw e;
        } catch(Exception e) {
            logger.error("CsiApiEJB.getLatestServiceElementStatus got an exception \"" + e.getClass().getName() + "\" - " + e.getMessage(), e);
            throw new CSIException("CsiApiEJB.getLatestServiceElementStatus got an exception \"" + e.getClass().getName() + "\" - " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves CustomerId, SiteHandle and ServiceHandle based on ServiceElement USID
     * 
     * @param usid
     * @return String[3] corresponding CustomerId, SiteHandle and ServiceHandle or null if USID does not exst
     * @throws CSIException if any problem
     */
    public String[] getCustomerInfoByServiceElementUSID(String usid) throws CSIException {
        try {
            return getServiceManager().getCustomerInfoByServiceElementUSID(usid);
        } catch(CSIException e) {
            logger.error("CsiApiEJB.getCustomerInfoByServiceElementUSID got CSIException - " + e.getMessage(), e);
            throw e;
        } catch(Exception e) {
            logger.error("CsiApiEJB.getCustomerInfoByServiceElementUSID got an exception \"" + e.getClass().getName() + "\" - " + e.getMessage(), e);
            throw new CSIException("CsiApiEJB.getCustomerInfoByServiceElementUSID got an exception \"" + e.getClass().getName() + "\" - " + e.getMessage(), e);
        }
    }

     /**
      * method for GOLD JMX to kick the run
      *
      * @param runType
      * @param productList
      */
     public void callRunCISExtract(int runType, String productList) {
         try {
             getServiceManager().runCISExtract(runType, productList);
         } catch (CSIException e) {
             logger.error("CsiApiEJB.callRunCISExtract got CSIException - " + e.getMessage(), e);
         } catch (Exception e) {
             logger.error("CsiApiEJB.callRunCISExtract got an exception \"" + e.getClass().getName() + "\" - " + e.getMessage(), e);
         }
     }
     
     public void runExceptionReport(boolean isMontlyRun, String storage) throws CSIException {
         try {
             getServiceManager().runExceptionReport(isMontlyRun, storage);
         } catch (CSIException e) {
             logger.error("CsiApiEJB.runExceptionReport got CSIException - " + e.getMessage(), e);
         } catch (Exception e) {
             logger.error("CsiApiEJB.runExceptionReport got an exception \"" + e.getClass().getName() + "\" - " + e.getMessage(), e);
         }
     }
}