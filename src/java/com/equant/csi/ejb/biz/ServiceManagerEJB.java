/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/ejb/biz/ServiceManagerEJB.java,v 1.45 2004/12/24 10:39:24 andrey.krot Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.ejb.biz;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;

import com.equant.csi.common.CSIConstants;
import com.equant.csi.common.ServiceIdHolder;
import com.equant.csi.common.TransDate;
import com.equant.csi.database.CreateManager;
import com.equant.csi.database.DeleteManager;
import com.equant.csi.database.GOLDManager;
import com.equant.csi.database.QueryManager;
import com.equant.csi.database.RollbackManager;
import com.equant.csi.database.UpdateManager;
import com.equant.csi.exceptions.CISException;
import com.equant.csi.exceptions.CSIException;
import com.equant.csi.exceptions.CSINoRollbackVersionException;
import com.equant.csi.exceptions.CSIRollbackException;
import com.equant.csi.interfaces.cis.CISManagerHelper;
import com.equant.csi.jaxb.ConflictNoteType;
import com.equant.csi.jaxb.CustomerHandleChangeType;
import com.equant.csi.jaxb.DataChangesType;
import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.ObjectFactory;
import com.equant.csi.jaxb.ServiceElementAttributeType;
import com.equant.csi.jaxb.ServiceElementType;
import com.equant.csi.jaxb.SiteHandleChangeType;
import com.equant.csi.jaxb.SiteInformationType;
import com.equant.csi.jaxb.SystemType;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.jaxb.impl.ServiceElementAttributeTypeImpl;
import com.equant.csi.jaxb.impl.ServiceElementTypeImpl;
import com.equant.csi.utilities.JAXBUtils;
import com.equant.csi.utilities.LoggerFactory;
import com.sun.xml.bind.marshaller.DataWriter;

/**
 * The interface to all ServiceManager functionality.
 * External systems will only be given access to this ejb.
 *
 * It also handle all JIT transactions where it needs {added in CSI v3.0 VR: Aug 2004}
 *
 * @author kpathuri
 * @author Vadim Gritsenko
 * @author Kostyantyn Yevenko
 * @author Vasyl Rublyov
 * @version $Revision: 630 $
 *
 * @see ServiceManagerBusiness
 * @see CSIConstants
 */
public class ServiceManagerEJB implements SessionBean, ServiceManagerBusiness, CSIConstants {

    /** Initializing the logger. */
    private final static Category m_logger = LoggerFactory.getInstance(ServiceManagerEJB.class.getName());

    /** Initializing the CIS logger. */
    private final static Category cis_logger = LoggerFactory.getInstance("cis_extraction.ServiceManagerEJB");

    /**
     * This array describes service element priority for siteId selection
     */
    protected final static String[] SERVICE_ELEMENT_PRIORITY = {SECLASS_ACCESSCONNECTION, SECLASS_CPE, SECLASS_BACKUP, SECLASS_NASBACKUP, SECLASS_SERVICEOPTIONS, SECLASS_TRANSPORT};

    /** Session context **/
    private SessionContext m_context = null;

    protected Boolean m_isQueueConnection = null; /* none */
    private javax.jms.ConnectionFactory m_connectionFactory = null;
    private javax.jms.Connection m_connectionJMS = null;
    private javax.jms.Session m_sessionJMS = null;

    /* topic */
    private Topic m_topic = null;
    private TopicPublisher m_publisher = null;
    /* queue */
    private QueueSender m_qsender = null;
    private Queue m_queue = null;

    public static final SimpleDateFormat m_exceptionReportDateFormat = new SimpleDateFormat("yyyy.MM.dd.hhmmss");

    /** The  <code>TxDataSource</code> object */
    private transient DataSource dataSource;
    private transient DataSource m_cisDataSource;
    private transient DataSource m_goldDataSource;

    private ServiceManager m_serviceManager;

    public static final List LIST_SEA_ACCESSCONNECTION;
    public static final List LIST_SEA_BACKUP;
    public static final List LIST_SEA_CPE;
    public static final List LIST_SEA_TRANSPORT;

    public static final List LIST_CIS_SEA_IGNORE;

    protected static String m_jndiCISDataSourceName = "CisSybaseDataSource";
    protected static String m_jndiGOLDDataSourceName = "GOLDDataSource";
    protected static String m_jndiCSIDataSourceName = "CsiDataSource";
    

    static {
        LIST_SEA_ACCESSCONNECTION = new ArrayList();
        LIST_SEA_ACCESSCONNECTION.add("Local DNA Address");
        LIST_SEA_ACCESSCONNECTION.add("Local User Repertory Name");

        LIST_SEA_BACKUP = new ArrayList();
        LIST_SEA_BACKUP.add("Backup Type");
        LIST_SEA_BACKUP.add("Speed");

        LIST_SEA_CPE = new ArrayList();
        LIST_SEA_CPE.add("Router Name");

        LIST_SEA_TRANSPORT = new ArrayList();
        LIST_SEA_TRANSPORT.add("Remote Router Name");
        LIST_SEA_TRANSPORT.add("Remote DNA Address");
        LIST_SEA_TRANSPORT.add("Remote DLCI");

        LIST_CIS_SEA_IGNORE = new ArrayList();
        LIST_CIS_SEA_IGNORE.add(CSIConstants.SECLASS_ACCESSCONNECTION + "|" + "Access Connection Name");
        LIST_CIS_SEA_IGNORE.add(CSIConstants.SECLASS_BACKUP + "|" + "Access Connection Name");
        LIST_CIS_SEA_IGNORE.add(CSIConstants.SECLASS_TRANSPORT + "|" + "Remote Site Code");
        LIST_CIS_SEA_IGNORE.add(CSIConstants.SECLASS_TRANSPORT + "|" + "Remote Site");

    }

    /**
     * Create method specified in EJB 1.1 section 6.10.3.
     *
     */
    public void ejbCreate() throws CreateException {
        /** Initialize queue or topic publisher, so we can send or publish messages back to GOLD */
        try {
            initializeJMS();
        } catch(CSIException e) {
            m_logger.error(e);
            throw new CreateException(e.getMessage());
        }
    }

    /**
     * Sets the associated session context.
     *
     * @param context the session context to set
     * @see javax.ejb.SessionBean#setSessionContext
     */
    public void setSessionContext(SessionContext context) {
        m_context = context;
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
        deinitializeJMS();
    }

    /**
     * Retrieves the <code>DataSource</code> object.
     *
     * @return the <code>DataSource</code> object
     *
     * @throws NamingException thrown for any error during retrieving
     *                         the object with JNDI
     */
    private DataSource getDataSource() throws NamingException {
        if (dataSource == null) {
            InitialContext ic = new InitialContext();
            dataSource = (DataSource) ic.lookup(m_jndiCSIDataSourceName);
        }
        return dataSource;
    }

    /**
     * @return
     * @throws NamingException
     */
    private DataSource getCISDataSource() throws NamingException {
        if (m_cisDataSource == null) {
            InitialContext ic = new InitialContext();
            m_cisDataSource = (DataSource) ic.lookup(m_jndiCISDataSourceName);
        }
        return m_cisDataSource;
    }

    /**
     *
     */
    private DataSource getGOLDDataSource() throws NamingException {
        if (m_goldDataSource == null) {
            InitialContext ic = new InitialContext();
            m_goldDataSource = (DataSource) ic.lookup(m_jndiGOLDDataSourceName);
        }
        return m_goldDataSource;
    }

    ServiceManager getServiceManager()
            throws NamingException, CreateException, RemoteException {

        if (m_serviceManager == null) {
            InitialContext ic = new InitialContext();
           // changed as part of gold infra change project 
          //  ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup(ServiceManagerHome.class.getName()), ServiceManagerHome.class);
            ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup("com.equant.csi.ejb.biz.ServiceManagerHome"), ServiceManagerHome.class);
            m_serviceManager = home.create();
        }
        return m_serviceManager;
    }

    /**
     *
     * @param runType
     * @param productList
     */
    public void runCISExtract(int runType, String productList) {
        Connection cisConnection = null;
        Connection localConnection = null;
        Connection goldConnection = null;
        HashSet productSet;
        try {
            productSet = CISManagerHelper.parseProductToRun(productList);

            cisConnection = getCISDataSource().getConnection();
            localConnection = getDataSource().getConnection();
            goldConnection = getGOLDDataSource().getConnection();

            //TODO: find a good way to initialize SM
            CISManagerHelper.cisSubRun(cisConnection, localConnection, runType, getServiceManager(), goldConnection, productSet, cis_logger);
        } catch (CISException e) {
            m_logger.error("error while cis call ", e);
        } catch (javax.naming.NamingException ne) {
            m_logger.error("error while create connection ", ne);
        } catch (java.sql.SQLException se) {
            m_logger.error("error while create connection ", se);
        } catch (javax.ejb.CreateException jece) {
            m_logger.error("error while create service manager ", jece);
        } catch (java.rmi.RemoteException jrre) {
            m_logger.error("error while create service manager ", jrre);
        }
        finally {
            m_logger.debug("close connection");
            if (cisConnection != null) {
                try {
                    cisConnection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
            if (localConnection != null) {
                try {
                    localConnection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
            if (goldConnection != null) {
                try {
                    goldConnection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
        }
    }

    /**
     * Processes the Message object containing in the message from CIS Interface
     *
     * @param message the message to process
     * @throws CSIException thrown if an error occurred during message processing
     */
    public void processMessageFromCis(String message) throws CSIException {
        try {
            processMessageFromCis(JAXBUtils.unmarshalMessage(message));
        } catch (JAXBException e) {
            m_logger.error("Can't unmarshal message - " + message, e);
            throw new CSIException("Can't unmarshal message - " + message, e);
        }
    }

    /**
     * Gets message from CIS Interface and process to
     * Csi2GoldTopic JMS topic
     *
     * @param message Message object constructed from CIS tables
     */
    public void processMessageFromCis(Message message) throws CSIException {
        Connection connection = null;
        Message msgWithSite =null;
        if(cis_logger.isDebugEnabled()) {
            cis_logger.debug("Processing the CIS Message to Csi2GoldTopic(GOLD)-------");
        }

        try {
            connection = getDataSource().getConnection();
            msgWithSite = (new ObjectFactory()).createMessage();
        } catch (SQLException e) {
            m_logger.error("Can't get dataSource", e);
            throw new CSIException("Can't get dataSource", e);
        } catch (NamingException e) {
            m_logger.error("Can't get dataSource", e);
            throw new CSIException("Can't get dataSource", e);
        } catch (JAXBException e) {
            m_logger.error("Can't create Message object", e);
            throw new CSIException("Can't create Message object", e);
        }

        try {
            //use the update siteinformation for next version
            SiteInformationType sitetemp = message.getSiteInformation();
            if (sitetemp != null) {

                //TODO: we like to handle the major verison first, how do we do that?
                //TODO: based on the message construction, the first one is alwasy the major one. No needed, but will double check.
                int verCount = 0;
                boolean allVersionsWasProcessed = true;
                sitetemp.setSourceSystem(SYSTEM_TYPE_CIS);
                for(Iterator i=message.getVersion().iterator();i.hasNext();){
                    VersionType v = (VersionType) i.next();
                    cis_logger.debug("Version " + verCount + ":" + v.getCustomerId() + ":" + v.getSiteId() + ":" + v.getServiceId() + ":" + v.getOrderstatus() );
                    processServiceComponent(sitetemp, v, verCount == 0);
                    if (StringUtils.isBlank(sitetemp.getCISSiteId())) {
                        //this mean that we cannot even generate SiteId - i.e. we hdon't have correct Contact Information
                        allVersionsWasProcessed = false;
                        break;
                    }
                    verCount ++;
                }
                if(allVersionsWasProcessed) {
                    QueryManager qm = new QueryManager(connection); 
                    sitetemp = qm.populateServiceListForSite(sitetemp);
                    msgWithSite.setSiteInformation(sitetemp);
                    publishMessage(msgWithSite);
                }
            } else {
                //site is null
                cis_logger.error("ServiceManagerEJB.processMessageFromCis: sitetemp is Null - message was skipped");
            }
        } finally {
            if(connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    m_logger.error("ProcessMessageFromCIS: error closing connection");
                }
            }
        }

        if(cis_logger.isDebugEnabled()) {
            cis_logger.debug("End process the CIS Message to Csi2GoldTopic(GOLD).");
        }
    }

    /**
     *
     */
      public void processMessage(Message message) throws CSIException {
        processMessage(message, true);
    }
    /**
     * Processes all the Version objects containing in the message.
     *
     * @param message the message to process
     *
     * @throws CSIException thrown for any error during order processing
     */
    public void processMessage(Message message, boolean publishSiteMsg) throws CSIException {
        long startTime = java.lang.System.currentTimeMillis();
        ObjectFactory factory = new ObjectFactory();
        Connection connection = null;
        UserTransaction transation = null;

        if (m_logger.isDebugEnabled()) {
            m_logger.debug("Processing the Message");
        }

        try {
            transation =  m_context.getUserTransaction();
        } catch (IllegalStateException e) {
            m_logger.error("Can't get UserTransaction", e);
            throw new CSIException("Can't get UserTransaction", e);
        }

        try {
            ArrayList listForServieUpdate = new ArrayList();

            Message msgServiceListOnly = null;
            SiteInformationType siteInformationServiceListOnly = null;
            try {
                msgServiceListOnly = factory.createMessage();
                siteInformationServiceListOnly = factory.createSiteInformationType();
                // by default Source System is GOLD
                siteInformationServiceListOnly.setSourceSystem(SYSTEM_TYPE_GOLD);
                msgServiceListOnly.setSiteInformation(siteInformationServiceListOnly);
            } catch(JAXBException e) {
                // report and process next
                m_logger.error("Can't create JAXB Objects", e);
                msgServiceListOnly = null;
                siteInformationServiceListOnly = null;
            }

            transation.begin();

            try {
                connection = getDataSource().getConnection();
            } catch(NamingException e) {
                m_logger.error("Can't get dataSource", e);
                throw new CSIException("Can't get dataSource", e);
            } catch(SQLException e) {
                m_logger.error("Can't get connection", e);
                throw new CSIException("Can't get connection", e);
            }

            try {
                // Get all Version objects from the message and process them
                if(message.getVersion()!=null && message.getVersion().size()>0) {
                    for (Iterator i = message.getVersion().iterator(); i.hasNext();) {
                        VersionType ver = (VersionType)i.next();

                        if(CSIConstants.CONFIG_CSP_PURPLE.equalsIgnoreCase(ver.getServiceId())) {
                            if(m_logger.isDebugEnabled()) {
                                m_logger.debug("Processing CONFIG_CSP_PURPLE, setting SiteID to 'GLOBAL'");
                            }
                            ver.setSiteId("GLOBAL");
                        }

                        if (m_logger.isDebugEnabled()) {
                            m_logger.debug("Processing the Version <"
                                         + ver.getServiceId() + "/" + ver.getSiteId() + ">");
                        }

                        if(processOrder(connection, ver)) {
                            // we have to trigger the list of services for this customer/site
                            String customerId = ver.getCustomerId();
                            String siteId = ver.getSiteId();
                            if(customerId!=null && siteId!=null) {
                                boolean found = false;
                                for (int j = 0; j < listForServieUpdate.size(); j++) {
                                    Message messageForUpdate = (Message)listForServieUpdate.get(j);
                                    String msgCustCode = messageForUpdate.getSiteInformation().getCustCode();
                                    String msgSiteId = messageForUpdate.getSiteInformation().getCISSiteId();
                                    if(msgCustCode!=null && msgSiteId!=null && msgCustCode.equals(ver.getCustomerId()) && msgSiteId.equals(ver.getSiteId())) {
                                        found = true;
                                        break;
                                    }
                                }
                                if(!found) {
                                    try {
                                        Message messageForUpdate = factory.createMessage();
                                        SiteInformationType siteInfoForUpdate = factory.createSiteInformationType();
                                        siteInfoForUpdate.setCustCode(ver.getCustomerId());
                                        siteInfoForUpdate.setCISSiteId(ver.getSiteId());
                                        messageForUpdate.setSiteInformation(siteInfoForUpdate);
                                        listForServieUpdate.add(messageForUpdate);
                                    } catch(JAXBException e) {
                                        m_logger.error("Can't create JAXB objects", e);
                                        // nothing else should be done
                                    }
                                }
                            }
                        }
                    }
                }
            } catch(CSINoRollbackVersionException e) {
                m_logger.error(e.getMessage());
            } catch(JAXBException e) {
                m_logger.error("JAXB error", e);
                throw new CSIException("JAXB error", e);
            } catch(SQLException e) {
                m_logger.error("SQL Error", e);
                throw new CSIException("SQL error", e);
            } catch(CSIException e) {
                m_logger.error("CSI error", e);
                throw e;
            }

            if(message.getDataChanges()!=null) {
                DataChangesType dc = message.getDataChanges();
                if(dc.getCustomerHandleChange()!=null && dc.getCustomerHandleChange().size()>0) {
                    // update Customer Handles
                    for(Iterator i = dc.getCustomerHandleChange().iterator();i.hasNext();) {
                        CustomerHandleChangeType t = (CustomerHandleChangeType)i.next();
                        if (m_logger.isDebugEnabled()) {
                            m_logger.debug("Processing CustomerHandle update <"
                                         + t.getOldId() + "/" + t.getUpdatedID() + ">");
                        }
                        processCustomerHandleUpdate(connection, t);
                    }
                }
                if(dc.getSiteHandleChange()!=null && dc.getSiteHandleChange().size()>0) {
                    // update Site Handles
                    for(Iterator i = dc.getSiteHandleChange().iterator();i.hasNext();) {
                        SiteHandleChangeType t = (SiteHandleChangeType)i.next();
                        if (m_logger.isDebugEnabled()) {
                            m_logger.debug("Processing SiteHandle update <"
                                         + t.getOldId() + "/" + t.getUpdatedID() + ">");
                        }
                        processSiteHandleUpdate(connection, t);
                    }
                }
            }

            // ok, we done, now if needs we have to trigger Service List to GOLD
            if (publishSiteMsg) {
                if(listForServieUpdate.size()>0 ) {
                    QueryManager qm = new QueryManager(connection);

                    for (int i = 0; i < listForServieUpdate.size(); i++) {
                        Message messageForUpdate = (Message) listForServieUpdate.get(i);

                        messageForUpdate.setSiteInformation( qm.populateServiceListForSite(
                                messageForUpdate.getSiteInformation())
                        );
                        publishMessage(messageForUpdate);
                    }
                }
            }
            // hope we can sommit this changes
            if(m_logger.isDebugEnabled()) {
                m_logger.debug("Committing all transactions.");
            }
            transation.commit();
        } catch(NotSupportedException e) {
            m_logger.error("NotSupportedException", e);
            throw new CSIException("NotSupportedException", e);
        } catch(SystemException e) {
            m_logger.error("SystemException", e);
            throw new CSIException("SystemException", e);
        } catch (RollbackException e) {
            m_logger.error("RollbackException", e);
            throw new CSIException("RollbackException", e);
        } catch (HeuristicMixedException e) {
            m_logger.error("HeuristicMixedException", e);
            throw new CSIException("HeuristicMixedException", e);
        } catch (HeuristicRollbackException e) {
            m_logger.error("HeuristicRollbackException", e);
            throw new CSIException("HeuristicRollbackException", e);
        } finally {
            if(transation!=null) {
                try {
                    if(transation.getStatus() == Status.STATUS_ACTIVE) {
                        transation.rollback();
                    }
                } catch (SystemException e) {
                    m_logger.error("Transation was not rolled back succesfully", e);
                }
            }

            if(connection!=null) {
                try {
                    connection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
                connection = null;
            }

            if (m_logger.isInfoEnabled()) {
                m_logger.info("ServiceManagerEJB.processMessage(...) call took " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
            }
        }
    }

    /**
     *
     * @param qm
     * @param um
     * @param xVersion
     * @return true if need create new version
     * @throws CSIException
     * @throws JAXBException
     */
    protected boolean synchronizeVersion(QueryManager qm, UpdateManager um, CreateManager cm, VersionType xVersion) throws CSIException, JAXBException {
        if(xVersion!=null && xVersion.getCustomerId()!=null && xVersion.getSiteId()!=null && xVersion.getServiceId()!=null
                && xVersion.getServiceElement()!=null && xVersion.getServiceElement().size()>0) {
            List listSE = xVersion.getServiceElement();
            Set lstMove = new HashSet();
            for (int i = 0; i < listSE.size(); i++) {
                ServiceElementType se = (ServiceElementType) listSE.get(i);
                if(se!=null) {
					synchronizeServiceElement(qm, um, xVersion.getCustomerId(), xVersion.getSiteId(), xVersion.getServiceId(), se, lstMove);
                }
            }

            if(lstMove!=null && !lstMove.isEmpty()) {
                try {
                    um.moveServiceElements(lstMove, xVersion);
                } catch(Exception e) {
                    m_logger.error("Move ServiceElement call has failed", e);
                }
            }
        }
        return true;
    }

    protected boolean synchronizeServiceElement(
            QueryManager qm, UpdateManager um,
            String customerId, String siteHandle, String serviceHandle, ServiceElementType se,
            Set lstMove
            ) throws CSIException, JAXBException {

        if(SECLASS_SERVICEOPTIONS.equals(se.getServiceElementClass()) && !CSIConstants.CONFIG_CSP_PURPLE.equals(serviceHandle)) {
            // GOLD: only one SE per Customer/Site/Service
            se.setId("SO::" + customerId + "::" + siteHandle + "::" + serviceHandle);
        }

        ServiceIdHolder idHolder = null;
        List lst = null;

        if (SYSTEM_TYPE_CIS.equals(se.getSourceSystem())) {
            lst = qm.getAllStatusCustomerSiteByUSID(se.getId());
        } else {
            lst = qm.getAllCustomerSiteByUSID(se.getId());
        }
        if(lst!=null) idHolder = (ServiceIdHolder)lst.get(0);
        if(idHolder!=null) {
            if(!customerId.equals(idHolder.customerId)) {
                if(SYSTEM_TYPE_CIS.equals(se.getSourceSystem())) {
                    // move the SE to that customer
                    lstMove.add(se.getId());
                } else {
                    throw new CSIRollbackException("USID \"" + se.getId() + "\" belongs to \"" + idHolder.customerId + "\", but given \"" + customerId + "\" customer");
                }
            }

            if(!siteHandle.equals(idHolder.siteId)) {
                if(SYSTEM_TYPE_CIS.equals(se.getSourceSystem())) {
//                  move the SE to that customer
                    lstMove.add(se.getId());
                } else {
                    throw new CSIRollbackException("USID \"" + se.getId() + "\" belongs to \"" + idHolder.siteId + "\", but given \"" + siteHandle + "\" site");
                }
            }

            if(!serviceHandle.equals(idHolder.servicerId)) {
                if(SYSTEM_TYPE_CIS.equals(se.getSourceSystem())) {
//                  move the SE to that customer
                    lstMove.add(se.getId());
                } else {
                    throw new CSIRollbackException("USID \"" + se.getId() + "\" belongs to \"" + idHolder.servicerId + "\", but given \"" + serviceHandle + "\" service");
                }
            }
        } else {
            m_logger.debug("SE usid not in csi, searching attribute:  Attr-USID=" + se.getId());
            /*
             * 10.4 Service Component Matching
             * The Service Component Matching shall be undertaken as a case-insensitive exact match on
             * the string values of the following fields:
             *      -   Access Connection: (SECLASS_ACCESSCONNECTION)
             *          ?   DNA;
             *          ?   User Repertory Name.
             *      -   Backup: (SECLASS_BACKUP)
             *          ?   Backup Type (ISDN / PSTN);
             *          ?   Speed.
             *      -   Router: (SECLASS_CPE)
             *          ?   Router Name.
             *      -   Meshing / PVC (SECLASS_TRANSPORT)
             *          ?   Remote Router Name (PE Router)  OR Remote DNA  or Remote DLCI .
             *
             * Extra, per GOLD def: SECLASS_SERVICEOPTIONS & SECLASS_NASBACKUP
             */
            List attributeNameList = null;
            if(SECLASS_ACCESSCONNECTION.equals(se.getServiceElementClass())) {
                attributeNameList = LIST_SEA_ACCESSCONNECTION;
            } else if(SECLASS_BACKUP.equals(se.getServiceElementClass())) {
                attributeNameList = LIST_SEA_BACKUP;
            } else if(SECLASS_CPE.equals(se.getServiceElementClass())) {
                attributeNameList = LIST_SEA_CPE;
            } else if(SECLASS_TRANSPORT.equals(se.getServiceElementClass())) {
                attributeNameList = LIST_SEA_TRANSPORT;
            } else if(SECLASS_NASBACKUP.equals(se.getServiceElementClass())) {
                // nothing as of now :(
            }

            if(se!=null && SYSTEM_TYPE_CIS.equals(se.getSourceSystem()) && attributeNameList!=null && se.getServiceElementAttribute()!=null) {
                for (int i = 0; i < se.getServiceElementAttribute().size(); i++) {
                    ServiceElementAttributeType sa = (ServiceElementAttributeType) se.getServiceElementAttribute().get(i);
                    if(sa!=null && sa.getName()!=null && sa.getValue()!=null) {
                        if(attributeNameList.contains(sa.getName())) {
                            if (!checkCisAttributeAndUpdate(qm, um, customerId, siteHandle, serviceHandle,
                                    se.getServiceElementClass(), sa.getName(), sa.getValue(), se.getId())){
                                return false;
                            };
                        }
                    }
                }

            }
        }
        return true;
    }

    protected boolean checkCisAttributeAndUpdate(
            QueryManager qm,
            UpdateManager um,
            String customerId,
            String siteHandle,
            String serviceHandle,
            String serviceElemnentClass,
            String attributeName,
            String attributeValue,
            String serviceElementId
            ) throws CSIException {
        Map pairUsid = qm.getUsidBySiteAttribute(customerId, siteHandle, serviceHandle, serviceElemnentClass, attributeName, attributeValue);
        if(pairUsid!=null && !pairUsid.isEmpty() && !pairUsid.containsKey(serviceElementId)) {
            // we don't have this USID, but have attribute in the database... overwrite USID if it is 'GOLD'
            Iterator usids = pairUsid.keySet().iterator();
            if(usids.hasNext()) {
                String usidDb = (String)usids.next();
                String sourceSystemDb = (String)pairUsid.get(usidDb);
                if(SYSTEM_TYPE_GOLD.equals(sourceSystemDb)) {
                    // okay - overwrite it
                    um.updateUsid(usidDb, serviceElementId);
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Processes the order.
     *
     * @param xVersion the <code>Version<code> object
     * @return boolean if the service list has been changed
     *
     * @throws CSIException thrown if an error occurred during order processing
     */
    private boolean processOrder(Connection connection, VersionType xVersion) throws CSIException, JAXBException, SQLException {
        boolean isServicesListChanged = false;
        String orderType = xVersion.getOrdertype();
        String orderStatus = xVersion.getOrderstatus();

        DeleteManager dm = new DeleteManager(connection);
        QueryManager qm = new QueryManager(connection);
        CreateManager cm = new CreateManager(connection);
        UpdateManager um = new UpdateManager(connection);
        RollbackManager rm = new RollbackManager(connection);

        if (xVersion.getLegacyServiceId()!=null && xVersion.getLegacyServiceId().length()>0) {
            if (m_logger.isDebugEnabled()) {
                m_logger.debug(
                        "Excecute UpdateManager(all version for the service) call for updating from legacyService \""+
                        xVersion.getLegacyServiceId() + "\" to \"" + xVersion.getServiceId() + "\"."
                );
            }
            um.updateLegacyVersion(xVersion);
        }


        boolean isVersionCIS=SYSTEM_TYPE_CIS.equals(xVersion.getSystem().getId());

        if(!synchronizeVersion(qm, um, cm,xVersion)){
            return true;
        };

        Hashtable sourceSystems = null;
        if(xVersion.getOrderid()!=null) {
            sourceSystems = qm.getSourceSystem(xVersion.getOrderid());
        }

        if (ORDER_TYPE_EXISTING.equals(orderType)&& !isVersionCIS) {
            if (m_logger.isDebugEnabled())
                m_logger.debug("Excecute Update(Existing) order");
            /**
             * Existing order.
             * For existing order, existing version(s) are found and updated directly without
             * creation of new version. Only latest versions of the attributes are updated.
             */
            xVersion.setOrderSentDate(new TransDate(xVersion.getOrderSentDate().getTime() - 1000));
            doCspSiteCheck(um, qm, xVersion);
            um.updateVersion(xVersion, STATUS_CURRENT, false);
        } else if (ORDER_TYPE_DISCONNECT.equals(orderType)&& isVersionCIS) {
            //10.1 rule 9 - create disconnect version and update all details
            um.updateVersion(xVersion, STATUS_DISCONNECT, true);
            isServicesListChanged = true;
        } else if (STATUS_ORDER_RELEASE.equals(orderStatus)) {
            if (m_logger.isDebugEnabled())
                m_logger.debug("Excecute Release order");

            doCspSiteCheck(um, qm, xVersion);

            // Save SourceSystem of previous SEs
            // Release order should always come once per OrderID. But if this order was cancelled and
            // CSI missed this message, the order still will be in the database. Thus, remove stale data.
            if(isVersionCIS) {
                // CIS Source System - update only
                um.updateVersion(xVersion, STATUS_INPROGRESS, true);
            } else {
                // GOLD Source System - delete & update (old logic)
                dm.deleteVersion(xVersion);
                populateSourceSystem(sourceSystems, xVersion);
                // Release order: Create a new version of the service
                cm.createVersion(xVersion, false);
            }
            isServicesListChanged = true;
        } else if ((STATUS_ORDER_MANAGE.equals(orderStatus) || STATUS_ORDER_UNCANCEL.equals(orderStatus)) || STATUS_CUSTOMER_ACCEPTS_SERVICE.equals(orderStatus)) {
            if (m_logger.isDebugEnabled())
                m_logger.debug("Excecute Manage/UnCancel/CustomerAcceptsService order");

            if(isVersionCIS) {
                // CIS Source System - update only
                // TODO??????? TODO??????? TODO??????? TODO??????
                um.updateVersion(xVersion, STATUS_CURRENT, true);
            } else {
                // GOLD Source System - delete & update (old logic)
                dm.deleteVersion(xVersion);
                populateSourceSystem(sourceSystems, xVersion);
                cm.createVersion(xVersion, false);
            }
            // the service list may be changed in this case
            isServicesListChanged = true;
        } else if(STATUS_MODIFY_WITHOUT_PRICE_IMPACT.equals(orderStatus) ){
          //  xVersion.
            String lastOrdStatus=qm.getOrderStatusForOrderId(xVersion.getOrderid());
            if(STATUS_ORDER_MANAGE.equals(lastOrdStatus) || STATUS_CUSTOMER_ACCEPTS_SERVICE.equals(lastOrdStatus)) {
                um.updateVersion(xVersion, STATUS_CURRENT, false);
            } else {
                dm.deleteVersion(xVersion);
                populateSourceSystem(sourceSystems, xVersion);
                cm.createVersion(xVersion, false);
            }
        } if (((STATUS_ORDER_CANCEL.equals(orderStatus)&& !isVersionCIS )||
                STATUS_MODIFY_WITH_PRICE_IMPACT.equals(orderStatus))) {
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("Excecute Cancel order");
            }

            // Cancel order: Delete version of the service TODO: Need to restore all site level in case of Global CSP with Global Update flag
            dm.deleteVersion(xVersion);
            // the service list may be changed in this case
            isServicesListChanged = true;
        } else if (STATUS_ORDER_ROLLBACK.equals(orderStatus)) {
            if (m_logger.isDebugEnabled())
                m_logger.debug("Excecute Rollback order");

            // Rollback order: Change status to INPROGRESS or delete version of the service TODO: Need to restore all site level in case of Global CSP with Global Update flag
            rm.rollbackVersion(xVersion);
            // the service list may be changed in this case
            isServicesListChanged = true;
        }
        return isServicesListChanged;
    }

    /**
     * Don't overwrite some add on attribute from CIS
     * @param gldSE
     * @param cisSE
     */
    protected void ignoreCISAttribute(ServiceElementType gldSE, ServiceElementType cisSE) {
        if (gldSE == null
                || cisSE == null
                || gldSE.getServiceElementAttribute() == null
                || cisSE.getServiceElementAttribute() == null
                || gldSE.getServiceElementAttribute().size() == 0
                || cisSE.getServiceElementAttribute().size() == 0
        )
            return;

        for (Iterator iter = gldSE.getServiceElementAttribute().iterator(); iter.hasNext();) {
            ServiceElementAttributeType sea = (ServiceElementAttributeType) iter.next();
            if (sea != null && sea.getName() != null && LIST_CIS_SEA_IGNORE.contains(gldSE.getServiceElementClass() + "|" + sea.getName())
                    && sea.getValue() != null && sea.getValue().length() > 0) {
                for (Iterator iter2 = cisSE.getServiceElementAttribute().iterator(); iter2.hasNext();) {
                    ServiceElementAttributeType tmp = (ServiceElementAttributeType) iter2.next();
                    if (tmp != null && tmp.getName() != null && tmp.getName().equals(sea.getName())) {
                        cisSE.getServiceElementAttribute().remove(tmp);
                        break;
                    }
                }
            }
        }
    }

    protected boolean validServiceElementToApply(QueryManager qm, VersionType xVersion, ServiceElementType se) throws CSIException {
        if(se!=null && SYSTEM_TYPE_CIS.equals(se.getSourceSystem())) {
            if(STATUS_ORDER_RELEASE.equals(xVersion.getOrderstatus()) && !ORDER_TYPE_EXISTING.equals(xVersion.getOrdertype())) {
                // data came from CIS and is in progress state... check if we have data in CSI
                String status = qm.getLatestServiceElementStatus(se.getId());
                if(status==null) {
                    // we do not have this SE in database... skip it
                    return false;
                } else if(!(STATUS_INPROGRESS.equals(status) || STATUS_INPRONOCURR.equals(status))) {
                    // we have current or disconnected version in database... in progress is going to arrive to us
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if the order is CSP Global and requested global update for all site level CSP options
     * @param xVersion
     */
    protected void doCspSiteCheck(UpdateManager updateManager, QueryManager queryManager, VersionType xVersion) throws CSIException, JAXBException, SQLException {
        if(CSIConstants.CONFIG_CSP_PURPLE.equals(xVersion.getServiceId())) {
            // check if we need to update all site level service options for CSP
            Collection col = null;
            QueryManager qm = null;
            ServiceElementAttributeType flag = new ObjectFactory().createServiceElementAttributeType();
            flag = null;

            List lst = xVersion.getServiceElement();
            for(Iterator iter = lst.iterator(); iter.hasNext(); ) {
                ServiceElementTypeImpl se = (ServiceElementTypeImpl)iter.next();
                if(CSIConstants.CONFIG_CSP_SERVICE_CLASS.equals(se.getServiceElementClass())) {
                    List lst2 = se.getServiceElementAttribute();
                    String servId = null;
                    if(lst2!=null && lst2.size()>0) {
                        for(Iterator attrs = lst2.iterator();attrs.hasNext();) {
                            ServiceElementAttributeTypeImpl seat = (ServiceElementAttributeTypeImpl)attrs.next();
                            if (CSIConstants.CONFIG_CSP_SERVICE.equals(seat.getName())) {
                                // get service
                                servId = seat.getValue();
                            }

                            if(CSIConstants.CONFIG_CSP_GLOBAL_UPDATE_FLAG.equals(seat.getName())) {
                                flag = seat;
                                String val = seat.getValue();
                                if("Yes".equalsIgnoreCase(val) || "True".equals(val)) {
                                    if(m_logger.isDebugEnabled()) {
                                        m_logger.debug("we have found the CSP site level flag... now need to update " +
                                                "and/or insert all versions with new CSP site level options");
                                    }
                                    // we have found the CSP site level flag... now need to
                                    // update and/or insert all versions with new CSP site level options

                                    // Migration of "Color"
                                    updateManager.findAndUpdateColor(xVersion.getCustomerId(), servId);
                                    // get list of sites for current customer/service
                                    col = queryManager.getSites(xVersion.getCustomerId(), getServiceHandle(servId));

                                    // Continue with migration..
                                    if (col.size() != 0) {
                                        for (Iterator siter = col.iterator(); siter.hasNext();) {
                                            String site = (String) siter.next();
                                            VersionType xV = qm.getServiceDetails(xVersion.getCustomerId(), site, null, null, getServiceHandle(servId));

                                            updateManager.findAndUpdateOrCreateCspOptions(xV, se, false);
                                        }
                                    } else {
                                        if(m_logger.isDebugEnabled()) {
                                            m_logger.debug("No one site affected by this CSP new order");
                                        }
                                    }
                                }
                            }
                        }

                        if (flag != null) {
                            se.getServiceElementAttribute().remove(flag);
                        }
                    }
                }
            }
        }
    }

    /**
     * Processes the update for SiteHandle.
     *
     * @param xCustomerHandleChange the <code>CustomerHandleChange<code> object
     *
     * @throws CSIException thrown if an error occurred during order processing
     */
    private void processCustomerHandleUpdate(Connection connection, CustomerHandleChangeType xCustomerHandleChange) throws CSIException {
        if (m_logger.isDebugEnabled())
            m_logger.debug("Excecute Update CustomerHandle");
        new UpdateManager(connection).updateCustomerHandle(xCustomerHandleChange);
    }

    /**
     * Processes the update for SiteHandle.
     *
     * @param xSiteHandleChange the <code>SiteHandleChange<code> object
     *
     * @throws CSIException thrown if an error occurred during order processing
     */
    private void processSiteHandleUpdate(Connection connection, SiteHandleChangeType xSiteHandleChange) throws CSIException {
        if (m_logger.isDebugEnabled())
            m_logger.debug("Excecute Update SiteHandle");
        new UpdateManager(connection).updateSiteHandle(xSiteHandleChange);
    }

    /**
     * Returns a vector of service element objects.
     *
     * @param customerId customer id of the component used to build the query
     * @param siteId     site id of the component used to build the query
     * @return a vector of ServiceNames
     */
    public Vector getServices(String customerId, String siteId, String coreSiteId, String addressId) throws CSIException {
        long startTime = java.lang.System.currentTimeMillis();
        Connection connection = null;
        try {
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("Get services for <"+ customerId +"/"+ siteId +"/"+ coreSiteId +"/"+ addressId +">");
            }
            connection = getDataSource().getConnection();

            return new QueryManager(connection).getServices(customerId, siteId, coreSiteId, addressId);
        } catch (NamingException e) {
            m_logger.error("Can't get dataSource", e);
            throw new CSIException("Can't get dataSource", e);
        } catch(SQLException e) {
            m_logger.error("Can't get connection", e);
            throw new CSIException("Can't get connection", e);
        } finally {
            if (m_logger.isInfoEnabled()) {
                m_logger.info("ServiceManagerEJB.getServices(\"" + customerId + "\", \"" + siteId + "\") call took " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
            }
            if(connection!=null) {
                try {
                    connection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
        }
    }


    /**
     * Returns the <code>Message</code> containing the service details.
     *
     * @param customerId  customer id of the component used to build the query
     * @param siteId      site id of the component used to build the query
     * @param serviceName service name of the component used to build the query
     * @return the service details for the provided customerId, siteId, serviceName
     *
     * @throws CSIException if an error was encountered while creating the object
     */
    public Message getServiceDetails(String customerId, String siteId, String coreSiteId, String addressId, String serviceName) throws CSIException {
        long startTime = java.lang.System.currentTimeMillis();
        Message message = null;
        Connection connection = null;
        try {
            if (m_logger.isDebugEnabled())
                m_logger.debug("Get service details for <" + customerId + "/" + siteId + "/" + coreSiteId + "/"  + addressId + "/" + serviceName+">");

            connection = getDataSource().getConnection();

            VersionType version = new QueryManager(connection).getServiceDetails(customerId, siteId, coreSiteId, addressId, serviceName);
            if (version != null) {
                message = new ObjectFactory().createMessage();
                message.getVersion().add(version);

            }
        } catch (NamingException e) {
            m_logger.error("Can't find DataSource:", e);
            throw new CSIException("Can't find DataSource:\n", e);
        } catch(SQLException e) {
            m_logger.error("Can't get connection", e);
            throw new CSIException("Can't get connection", e);
        } catch(JAXBException e) {
            m_logger.error("Can't get Message object", e);
            throw new CSIException("Can't get Message object", e);
        } finally {
            if (m_logger.isInfoEnabled()) {
                m_logger.info("ServiceManagerEJB.getServiceDetails(\"" + customerId + "\", \"" + siteId + "\", \"" + serviceName +"\") call took " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
            }
            if(connection!=null) {
                try {
                    connection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
        }

        return message;
    }

    /**
     * Returns the <code>Message</code> containing the CSP service details.
     */
    public Message getCSPServiceDetails(String customerId, String serviceName) throws CSIException {
        long startTime = java.lang.System.currentTimeMillis();
        Message message = null;
        Connection connection = null;
        try {
            if (m_logger.isDebugEnabled())
                m_logger.debug("Get CSP service details for <" + customerId + "/"+serviceName+">");

            connection = getDataSource().getConnection();
            VersionType version = new QueryManager(connection).getCSPServiceDetails(customerId, serviceName);
            if(version!=null) {
                message = new ObjectFactory().createMessage();
                message.getVersion().add(version);
            }
        } catch (NamingException e) {
            m_logger.error("Can't find DataSource:", e);
            throw new CSIException("Can't find DataSource:\n", e);
        } catch(SQLException e) {
            m_logger.error("Can't get connection", e);
            throw new CSIException("Can't get connection", e);
        } catch(JAXBException e) {
            m_logger.error("Can't get Message object", e);
            throw new CSIException("Can't get Message object", e);
        } finally {
            if (m_logger.isInfoEnabled()) {
                m_logger.info("ServiceManagerEJB.getCSPServiceDetails(\"" + customerId + "\", \"" + serviceName + "\") call took " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
            }
            if(connection!=null) {
                try {
                    connection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
        }

        return message;
    }

    private static String getServiceHandle(String serv) {
        String ret = null;

        if (CSIConstants.CSP_PURPLE_ATM.equals(serv)) {
            ret = CSIConstants.SITE_HANDLE_ATM;
        } else if (CSIConstants.CSP_PURPLE_CONTACT_CENTER.equals(serv)) {
            ret = CSIConstants.SITE_HANDLE_CONTACT_CENTER;
        } else if (CSIConstants.CSP_PURPLE_FR.equals(serv)) {
            ret = CSIConstants.SITE_HANDLE_FR;
        } else if (CSIConstants.CSP_PURPLE_IP_VPN.equals(serv)) {
            ret = CSIConstants.SITE_HANDLE_IP_VPN;
        } else if (CSIConstants.CSP_PURPLE_IP_VIDEO.equals(serv)) {
            ret = CSIConstants.SITE_HANDLE_IP_VIDEO;
        } else if (CSIConstants.CSP_PURPLE_LAN_ACCESS.equals(serv)) {
            ret = CSIConstants.SITE_HANDLE_LAN_ACCESS;
        } else if (CSIConstants.CSP_PURPLE_VOICE_VPN.equals(serv)) {
            ret = CSIConstants.SITE_HANDLE_VOICE_VPN;
        }

        return ret;
    }

    /**
     * Returns a XML String with Service Element details for given USID.
     * This function will only be called by the GOLD System.
     *
     * @param usid service element usid
     * @return the service element details for the provided usid
     *         returns null if no service element exists
     *
     * @throws CSIException   if an error was encountered while creating the object
     */
    public Message serviceLookup(String usid) throws CSIException {
        long startTime = java.lang.System.currentTimeMillis();
        Message message = null;
        Connection connection = null;
        try {
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("Get \"service lookup\" details for <" + usid +">");
            }

            connection = getDataSource().getConnection();
            VersionType version = new QueryManager(connection).getVersionServiceElementByUSID(usid);

            if(version!=null && version.getServiceElement()!=null && version.getServiceElement().size()>0) {
                message = new ObjectFactory().createMessage();
                message.getVersion().add(version);
            }
        } catch (NamingException e) {
            m_logger.error("Can't find DataSource:", e);
            throw new CSIException("Can't find DataSource:\n", e);
        } catch(SQLException e) {
            m_logger.error("Can't get connection", e);
            throw new CSIException("Can't get connection", e);
        } catch(JAXBException e) {
            m_logger.error("Can't get Message object", e);
            throw new CSIException("Can't get Message object", e);
        } finally {
            if (m_logger.isInfoEnabled()) {
                m_logger.info("ServiceManagerEJB.serviceLookup(\"" + usid + "\") call took " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
            }
            if(connection!=null) {
                try {
                    connection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
        }

        return message;
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
     * @throws CSIException   if an error was encountered while creating the object
     */
    public void serviceMerge(String goldUsid, String cisUsid) throws CSIException {
        long startTime = java.lang.System.currentTimeMillis();
        Connection connection = null;

        if(m_logger.isDebugEnabled()) {
            m_logger.error("ServiceManagerEJB.serviceMerge(\"" + goldUsid + "\", \"" + cisUsid + "\")");
        }

        try {
            connection = getDataSource().getConnection();
            new UpdateManager(connection).mergeServiceElements(goldUsid, cisUsid);
        } catch (NamingException e) {
            m_logger.error("Can't find DataSource:", e);
            throw new CSIException("Can't find DataSource:\n", e);
        } catch (CSIException e) {
            m_logger.error("Unable to process XML:", e);
            throw e;
        } catch(SQLException e) {
            m_logger.error("Can't get connection", e);
            throw new CSIException("Can't get connection", e);
        } finally {
            if (m_logger.isInfoEnabled()) {
                m_logger.info("ServiceManagerEJB.serviceMerge(\"" + goldUsid + "\", \"" + cisUsid + "\") call took " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
            }
            if(connection!=null) {
                try {
                    connection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
        }
    }

    /**
     *
     * @param siteInfo
     * @param cisVer
     * @param needSearchSite
     * @throws CSIException
     */
    public void processServiceComponent(SiteInformationType siteInfo, VersionType cisVer, boolean needSearchSite)throws CSIException {
        ObjectFactory objectFactory = new ObjectFactory();
        Message message = null;
        Connection conn = null;
        Connection goldConn = null;
        HashMap mapDisconnect = new HashMap();

        try {
            try {
                conn = getDataSource().getConnection();
                goldConn = getGOLDDataSource().getConnection();
                message = objectFactory.createMessage();
            } catch (NamingException e) {
                m_logger.error("Can't find DataSource\n:",e);
                cis_logger.error("Can't find DataSource\n:",e);
                throw new CSIRollbackException("Can't find DataSource\n:",e);
            } catch (SQLException e) {
                m_logger.error("Can't find Connection\n:",e);
                cis_logger.error("Can't find Connection\n:",e);
                throw new CSIRollbackException("Can't find Connection\n:",e);
            } catch(JAXBException e) {
                m_logger.error("Can't create \"Message\" object", e);
                cis_logger.error("Can't create \"Message\" object", e);
                throw new CSIRollbackException("Can't create \"Message\" object", e);
            }

            message.setSiteInformation(siteInfo);
            message.getVersion().add(cisVer);

            QueryManager qm = new QueryManager(conn);
            GOLDManager gm = new GOLDManager(conn, goldConn);

            String customerId = cisVer.getCustomerId();
            if(customerId!=null && customerId.length()==0) {
                customerId = null;
            }

            String siteId = siteInfo.getCISSiteId();
            if(siteId!=null && siteId.length()==0) {
                siteId = null;
            }

            //for the second version, we don't need to handle the site information one more time.
            //we don't need to put into the queue.
            if (!needSearchSite && StringUtils.isBlank( siteId)) {
                siteInfo.setCISSiteId( null);
                cis_logger.error("ServiceManagerEJB.processServiceComponent: siteId is NULL for the \"not first\" version");
                return;
            }


            List lst = cisVer.getServiceElement();
            String serviceId = cisVer.getServiceId();
            int servCount = 0;
            //here is map for all SiteIds
            HashMap elementToSiteIdMap = new HashMap();
            boolean needSearchSiteInsideOfElements = needSearchSite && (siteId == null);
            if(lst!=null && lst.size()>0 && serviceId!=null) {
                // we have service elements - process them
                // TODO: based on the incoming message contruction, we know the major component alwasy handle first.  Need to verify
                for(Iterator iter = lst.iterator(); iter.hasNext();) {
                    ServiceElementType se = (ServiceElementType)iter.next();
                    cis_logger.debug("Service Element Type:" + se.getServiceElementClass() + ":Position:" + servCount);

                    if(se!=null && se.getId()!=null && se.getId().length()>0) {
                        if(needSearchSiteInsideOfElements) {
                            //trying to search inside the CSI db based on usid
                            ServiceIdHolder id = qm.getCustomerSiteByUSID(se.getId(), customerId, serviceId);
                            if(id!=null && StringUtils.isNotBlank( id.siteId)) {
                                cis_logger.debug("SiteId found for:" + se.getServiceElementClass() + ":USID:" + se.getId());
                                elementToSiteIdMap.put(se.getServiceElementClass(), id.siteId);
                                siteId = id.siteId;
                            }
                        }

                        // let us check if we have some SEs to discconnect (Versions) CR: 8234
                        List allLst = qm.getAllStatusCustomerSiteByUSID(se.getId());
                        if(allLst!=null) {
                            for(Iterator allIter=allLst.iterator();allIter.hasNext(); ) {
                                ServiceIdHolder id2 = (ServiceIdHolder)allIter.next();
                                if(id2!=null && !(customerId.equalsIgnoreCase(id2.customerId) && serviceId.equals(id2.servicerId))) {
                                    String latestStatus = qm.getLatestServiceElementStatus(id2.customerId, id2.siteId, id2.servicerId, se.getId());
                                    cis_logger.debug("Different custcode/servid found for:" + se.getServiceElementClass() + ":USID:" + se.getId());
                                    if (needSearchSiteInsideOfElements) siteId = ""; // 8370-fix: allow GOLD to fill this field
                                    // we do not need to disconnect already disconnected service elements, so exlude them.
                                    if(! (STATUS_DISCONNECT.equals(latestStatus) || STATUS_DELETE.equals(latestStatus)) ) {
                                        // we need to disconnect all versions for found customer/site/product & usid
                                        if(mapDisconnect.containsKey(id2)) {
                                            ((Set)mapDisconnect.get(id2)).add(se.getId());
                                        } else {
                                            Set n = new HashSet();
                                            n.add(se.getId());
                                            mapDisconnect.put(id2, n);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    servCount ++;
                }
                if(mapDisconnect.size()>0) {
                    UpdateManager um = new UpdateManager(conn);
                    Date currentDate = new Date();
                    String currentUser = SYSTEM_TYPE_CIS;
                    SystemType sys = cisVer.getSystem();
                    if(sys!=null && sys.getUser()!=null && sys.getUser().getName()!=null) {
                        currentUser = sys.getUser().getName();
                    }
                    // we have some elements to disconnect and all of those elements are grouped by Customer/Site/Service
                    for(Iterator iter=mapDisconnect.keySet().iterator();iter.hasNext(); ) {
                        ServiceIdHolder id = (ServiceIdHolder)iter.next();
                        Set usids = (Set)mapDisconnect.get(id);
                        for(Iterator iter2=usids.iterator();iter2.hasNext(); ) {
                            String usid = (String)iter2.next();
                            if(m_logger.isDebugEnabled()) {
                                m_logger.debug("Will disconnect " + id + " USID: \"" + usid + "\"");
                                cis_logger.debug("Will disconnect " + id + " USID: \"" + usid + "\"");
                            }
                            try {
                                um.disconnectLatestServiceElement(id.customerId, id.siteId, id.servicerId, usid, currentDate, currentUser);
                            } catch(Exception e) {
                                // should try to disconnect all required VSEs
                                m_logger.error("Got an exception when tried to update status to disconnect for customerId=\"" + id.customerId
                                    + "\", siteId=\"" + id.siteId + "\", servicerId=\"" + id.servicerId
                                    + "\", USID = \"" + usid + ", currentDate=\"" + currentDate + "\", currentUser=\"" + currentUser + "\""
                                    + "... will process next. "
                                  , e
                                );

                                 cis_logger.error("Got an exception when tried to update status to disconnect for customerId=\"" + id.customerId
                                    + "\", siteId=\"" + id.siteId + "\", servicerId=\"" + id.servicerId
                                    + "\", USID = \"" + usid + ", currentDate=\"" + currentDate + "\", currentUser=\"" + currentUser + "\""
                                    + "... will process next. "
                                  , e
                                );
                            }
                        }
                    }
                }
            } else {
                // TODO: Log if there are no SEs
            }

            if (needSearchSiteInsideOfElements) {
                siteId = chooseSiteBasedOnElementPriority( elementToSiteIdMap);
            }
            if (StringUtils.isBlank( siteId)) {
                //siteId was not find in serviceElements
                siteId = searchSiteId( siteInfo, qm, gm);
            }

            //save any value of siteInfo to siteInfo object
            siteInfo.setCISSiteId(siteId);

            //check value
            if(siteId == null) {
                //log the error and skip the cis message
                //we don't find siteId anywhere and we can not generate it
                cis_logger.error("SiteId was not found anywhere.");
                return;

            }

            // ok, now we have almost completed version object

            //store siteId to temp table
            UpdateManager um = new UpdateManager(conn);
            um.insertSiteTempTable(siteInfo);

            //populate siteid into version
            cisVer.setSiteId( siteId);

            // populate service list.  call from process message
//                if(customerId!=null) {
//                    message = qm.populateServiceListForSite(message, customerId, siteId);
//                }

            // publish it
            // problem: change here not put int he queue, but call processMessage()
            // TODO: reserch proccessMessage to publish siteinformation to gold
            processMessage(message, false);

            //problem: only send the message with siteinfo to the queue
//                message.getVersion().clear();
//                publishMessage(message);
        } finally {
            if(conn!=null) {
                try {
                    conn.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }

            if(goldConn!=null) {
                try {
                    goldConn.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
            mapDisconnect.clear();
        }
    }


    /**
     * Method should return the siteId which should be chosen from provided table
     * with the following priority: AC, than CPE (?????), than BackUp, than anything
     * @param elementToSiteIdMap 
     * @return
     */
    protected String chooseSiteBasedOnElementPriority( HashMap elementToSiteIdMap) {
        if (elementToSiteIdMap.isEmpty()) return null;
        //first trying find SiteId based on SERVICE_ELEMENT_PRIORITY
        for (int i=0; i<SERVICE_ELEMENT_PRIORITY.length; i++ ){
            if (elementToSiteIdMap.containsKey(SERVICE_ELEMENT_PRIORITY[i])){
                return (String)elementToSiteIdMap.get(SERVICE_ELEMENT_PRIORITY[i]);
            }
        }
        // if not found return first found element
        Iterator i = elementToSiteIdMap.keySet().iterator();
        if (i.hasNext()) {
            return (String)elementToSiteIdMap.get( i.next());
        }
        return null;
    }


    /**
     * Method releases the following logic
     * problem:  need to add search here for temp table that message in the queue if we couldn't find the site in csi
     * problem:  need to add the search here for gold db if we couldn't find site in csi and in temp table. goldcode: siteExists(SiteInformation siteInfo, String customerGID, ISCManager mgr)
     * problem:  need to check if we have enough contact information or not if we couldn't find the site at all.
     * problem:  need to generate the siteid from gold if we couldn't find the site at all. goldcode: EQIDNumberGenerator.getNextSiteCode(m_locator.getPersisterService());
     */
    protected String searchSiteId ( SiteInformationType siteInfo, 
                QueryManager qm, GOLDManager gm) throws CSIException {


        //try to find in the Temporary Table
        String siteId = qm.getCISSiteIDFromTempTable(siteInfo);
        if (StringUtils.isNotBlank( siteId)) {
            if(cis_logger.isDebugEnabled()) cis_logger.debug( "siteId was found in Temp table");
            return siteId;
        }
        
        //check in gold
        siteId = gm.findSiteID( siteInfo);
        if (StringUtils.isNotBlank( siteId)) {
            if(cis_logger.isDebugEnabled()) cis_logger.debug( "siteId was found in GOLD");
            return siteId;
        }

        //check contact information
        if (gm.isContactValid( siteInfo)) {
            if(cis_logger.isDebugEnabled()) cis_logger.debug( "contact information is valid");

            //contact info is Ok - generate siteid
            siteId = gm.generateSiteID(siteInfo);

            if(cis_logger.isDebugEnabled()) cis_logger.debug( "siteId was generated as: " + siteId);
          
        } else {
            if(cis_logger.isDebugEnabled()) cis_logger.debug( "contact information is NOT valid");
        }

        return StringUtils.isBlank( siteId) ? null : siteId;
    }

    /**
     * retrieve all versions at Current stage and Source system as GOLD
     * @param isMontlyRun
     * @throws CSIException
     */
    public void runExceptionReport(boolean isMontlyRun, String storage) throws CSIException {
        long startTime = java.lang.System.currentTimeMillis();
        // TODO: what is the difference between MontlyRun and DailyRun ???
        ObjectFactory objectFactory = new ObjectFactory();
        Connection connection = null;
        try {
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("runExceptionReport(" + isMontlyRun + ")");
            }

            connection = getDataSource().getConnection();

            ConflictNoteType cn = new QueryManager(connection).getConflictNote(
                    isMontlyRun?CSI_EXCEPTION_REPORT_EXCEPTIONREPORT:CSI_EXCEPTION_REPORT_REGULARREPORT,
                    SYSTEM_TYPE_GOLD
            );
            if(cn!=null) {
                Message message = objectFactory.createMessage();
                message.setConflictNote(cn);
                // okey... now to publish it
                byte byteMessage[] = publishMessage(message);
                String filename = null;
                FileOutputStream fos = null;
                try {
                    if(storage!=null && byteMessage!=null && byteMessage.length>0) {
                        filename = storage + File.separator + m_exceptionReportDateFormat.format(new Date()) + ".bin";
                        fos = new FileOutputStream(filename);
                        fos.write(byteMessage);
                    }
                } catch(FileNotFoundException fnfe) {
                    m_logger.error("Can't create XML to file: " + filename + "\n" , fnfe);
                } catch(IOException ioe) {
                    m_logger.error("Can't write XML to file:\n", ioe);
                } finally {
                    try {
                        fos.flush();
                    } catch(Exception e) {}
                    try {
                        fos.close();
                    } catch(Exception e) {}
                    fos=null;
                }
            }
        } catch(NamingException e) {
            m_logger.error("Can't find DataSource:\n", e);
        } catch(JAXBException e) {
            m_logger.error("Can't create \"Message\" object", e);
        } catch(SQLException e) {
            m_logger.error("Can't get Connection", e);
        } finally {
            if(connection!=null) {
                try {
                    connection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
            if (m_logger.isInfoEnabled()) {
                m_logger.info("ServiceManagerEJB.runExceptionReport(" + isMontlyRun + ", \"" + storage + "\") call took " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
            }
        }
    }

    protected void initializeJMS() throws CSIException {
        InitialContext ic;
        //jonas/jms migration
        Hashtable env = new Hashtable();
       /* env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://10.238.22.42/cn=TestGold,o=CCEAIDev");
        env.put("java.naming.security.principal", "cn=user1,ou=People,o=CCEAIDev");
        env.put("java.naming.security.credentials", "toto");*/
        
       // env.put(Context.INITIAL_CONTEXT_FACTORY,"org.ow2.carol.jndi.spi.IRMIContextWrapperFactory"); 
     //   env.put(Context.URL_PKG_PREFIXES,"org.ow2.jonas.naming" );
     //   env.put(Context.PROVIDER_URL,"rmi://localhost:1099" );

        m_isQueueConnection=null;
        
        try {
            ic = new InitialContext(env);
        } catch (NamingException e) {
            throw new CSIException("Unable crate initial context \"CsiConnectionFactory\"", e);
        }

        // 1. We will try to establish Topic first
        /*try {
            initializeTopicPublisher(ic);
        } catch (ClassCastException e) {
            //m_logger.debug("ups... we have wrong connection factory :-)", e);
            m_logger.debug("ups... we have wrong connection factory :-)");
        } catch (NamingException e) {
            //m_logger.debug("topic does not exist?", e);
            m_logger.debug("topic does not exist?");
        } catch (JMSException e) {
            //m_logger.debug("Can't create topic connection - fix config.xml if it should be Queue", e);
            m_logger.debug("Can't create topic connection - fix config.xml if it should be Queue");
            throw new CSIException("Can't create topic connection - fix config.xml if it should be Queue", e);
        }  finally {
            if(m_isQueueConnection==null) {
                deinitializeJMS();
            }
            
        }*/

        if(m_isQueueConnection==null) {
            // 2. Seems we need Queue?
            try {
                initializeQueueSender(ic);
            } catch (Exception e) {
                deinitializeJMS();
                m_logger.debug("Can't create topic or queue connection", e);
                throw new CSIException("Can't create topic or queue connection", e);
            }
        }
    }
    
    protected void initializeTopicPublisher(InitialContext ic) throws NamingException, JMSException {
        m_connectionFactory = (TopicConnectionFactory) PortableRemoteObject.narrow(ic.lookup("CsiConnectionFactory"), TopicConnectionFactory.class);
        
        m_connectionJMS = ((TopicConnectionFactory)m_connectionFactory).createTopicConnection();
        m_connectionJMS.start();
        
        m_sessionJMS = ((TopicConnection)m_connectionJMS).createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        m_topic = (Topic) PortableRemoteObject.narrow(ic.lookup("Csi2GoldTopic"), Topic.class);
        
        m_publisher = ((TopicSession)m_sessionJMS).createPublisher(m_topic);

        m_isQueueConnection = Boolean.FALSE;
        m_logger.debug("Okay... now we are sending to TOPIC");
    }
    
    protected void initializeQueueSender(InitialContext ic) throws NamingException, JMSException {
        //jonas/jms migration
    	m_connectionFactory = (QueueConnectionFactory) ic.lookup("CsiConnectionFactory");
    	m_connectionJMS = ((QueueConnectionFactory)m_connectionFactory).createQueueConnection();
    	m_connectionJMS.start();
    	m_sessionJMS = ((QueueConnection)m_connectionJMS).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
    	m_queue = (Queue)ic.lookup("Csi2GoldQueue");
    	
    	/*
		m_connectionFactory = (QueueConnectionFactory) PortableRemoteObject.narrow(ic.lookup("CsiConnectionFactory"), QueueConnectionFactory.class);

        m_connectionJMS = ((QueueConnectionFactory)m_connectionFactory).createQueueConnection();
        m_connectionJMS.start();
        
        m_sessionJMS = ((QueueConnection)m_connectionJMS).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        m_queue = (Queue) PortableRemoteObject.narrow(ic.lookup("Csi2GoldQueue"), Queue.class);
		*/
        
        m_qsender = ((QueueSession)m_sessionJMS).createSender(m_queue);

        m_isQueueConnection = Boolean.TRUE;
        m_logger.debug("Okay... now we are sending to QUEUE");
    }
    
    protected void deinitializeJMS() {
        if(m_sessionJMS!=null) {
            try {
                m_sessionJMS.close();
            } catch(Throwable t) { /* do not worry here... too late */ }
        }
        if(m_connectionJMS!=null) {
            try {
                m_connectionJMS.stop();
            } catch(Throwable t) { /* do not worry here... too late */ }
            try {
                m_connectionJMS.close();
            } catch(Throwable t) { /* do not worry here... too late */ }
        }

        /* topic */
        m_topic = null;
        m_publisher = null;
        /* queue */
        m_qsender = null;
        m_queue = null;    
        
        m_connectionJMS = null;
        m_sessionJMS = null;
        
        m_isQueueConnection = null;
        m_connectionFactory = null;
    }

    public byte[] publishMessage(Message msg) throws CSIException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte ret[] = null;
        if(m_isQueueConnection==null) {
            initializeJMS();
        }

        DataWriter writer = null;
        try {
            JAXBContext ctx = JAXBUtils.getContext();
            Marshaller marshaller = ctx.createMarshaller();

            //DataWriter makes output XML well formatted.
            writer = new DataWriter(new OutputStreamWriter(byteStream),
                    (String)marshaller.getProperty(Marshaller.JAXB_ENCODING));
                    
            writer.setIndentStep(4);

            if (ctx.createValidator().validate(msg)) {
                marshaller.marshal(msg, writer);
            }

        } catch(JAXBException e) {
            m_logger.error("Unable to serialize Message object to XML. ", e);
            throw new CSIException("Unable to serialize Message object to XML. ", e);
        } finally {
            try {
                if (writer != null) {
                    writer.flush();
                }
            } catch (IOException e) {
                m_logger.error("publishMessage: Cannot flush writer. " + e);
                throw new CSIException(
                        "IOException in ServiceManagerEJB.publishMessage: Cannot flush writer. ", e);
            }
        }

        try {
            ret = byteStream.toByteArray();
            BytesMessage byteMessage = m_sessionJMS.createBytesMessage();
            byteMessage.writeBytes(ret);

            if(m_logger.isDebugEnabled()) {
                if(
                        msg.getSiteInformation()!=null &&
                        msg.getSiteInformation().getSourceSystem()!=null &&
                        CSIConstants.SYSTEM_TYPE_CIS.equals(msg.getSiteInformation().getSourceSystem())
                ) {
                    cis_logger.debug("Publishing Message object to JMS...");
                    cis_logger.debug("XML: ");
                    cis_logger.debug(byteStream.toString());
                } else {
                    m_logger.debug("Publishing Message object to JMS...");
                    m_logger.debug("XML: ");
                    m_logger.debug(byteStream.toString());
                }
            }
            if(m_isQueueConnection!=null && m_isQueueConnection.booleanValue()) {
                m_qsender.send(byteMessage);
            } else if(m_isQueueConnection!=null) {
                m_publisher.publish(byteMessage);
            }

/*
NOTE: For Getters!!!
ByteArrayOutputStream byteInputStream = new ByteArrayOutputStream();
byte buf[] = new byte[255];
int len;
while((len=byteMessage.readBytes(buf, 255))!=-1) byteInputStream.write(buf, 0, len);
byteInputStream.flush();
String str = byteInputStream.toString();
*/
        } catch(JMSException e) {
            m_logger.error("Unable to publish message", e);
            throw new CSIException("Unable to publish message", e);
        } finally {
            if(byteStream!=null) {
                try {
                    byteStream.close();
                } catch(Exception e) {}
                byteStream = null;
            }
        }
        return ret;
    }

    protected void populateSourceSystem(Hashtable sourceSystems, VersionType version) {
        if(sourceSystems!=null && sourceSystems.size()>0) {
            List se = version.getServiceElement();
            for (int i = 0; i < se.size(); i++) {
                ServiceElementType element = (ServiceElementType) se.get(i);
                String id = element.getId();
                if(id!=null && sourceSystems.containsKey(id) && !SYSTEM_TYPE_CIS.equals(element.getSourceSystem())) {
                    element.setSourceSystem((String)sourceSystems.get(id));
                }
            }
        }

    }

    public Map getBulkServiceList(String customerId) throws CSIException {
        long startTime = java.lang.System.currentTimeMillis();
        Connection connection = null;
        try {
            connection = getDataSource().getConnection();
            return new QueryManager(connection).getBulkServiceList(customerId);
        } catch (NamingException e) {
            m_logger.error("Can't find DataSource:", e);
            throw new CSIException("Can't find DataSource:\n", e);
        } catch(SQLException e) {
            m_logger.error("Can't get connection", e);
            throw new CSIException("Can't get connection", e);
        } finally {
            if (m_logger.isInfoEnabled()) {
                m_logger.info("ServiceManagerEJB.getBulkServiceList(\"" + customerId + "\") call took " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
            }
            if(connection!=null) {
                try {
                    connection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
        }
    }
    
    public String getLatestServiceElementStatus(String usid) throws CSIException {
        long startTime = java.lang.System.currentTimeMillis();
        Connection connection = null;
        try {
            connection = getDataSource().getConnection();
            return new QueryManager(connection).getLatestServiceElementStatus(usid);
        } catch (NamingException e) {
            m_logger.error("Can't find DataSource:", e);
            throw new CSIException("Can't find DataSource:\n", e);
        } catch(SQLException e) {
            m_logger.error("Can't get connection", e);
            throw new CSIException("Can't get connection", e);
        } finally {
            if (m_logger.isInfoEnabled()) {
                m_logger.info("ServiceManagerEJB.getLatestServiceElementStatus(\"" + usid + "\") call took " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
            }
            if(connection!=null) {
                try {
                    connection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
        }
    }

    /**
     * Retrieves CustomerId, SiteHandle and ServiceHandle based on ServiceElement USID
     * 
     * @param usid
     * @return String[3] corresponding CustomerId, SiteHandle and ServiceHandle or null if USID does not exst
     */
    public String[] getCustomerInfoByServiceElementUSID(String usid) throws CSIException {
        long startTime = java.lang.System.currentTimeMillis();
        String [] ret = null;
        Connection connection = null;
        try {
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("Get \"getCustomerInfoByServiceElementUSID(\"" + usid +"\");");
            }

            connection = getDataSource().getConnection();
            ret = new QueryManager(connection).getCustomerInfoByServiceElementUSID(usid);
        } catch (NamingException e) {
            m_logger.error("Can't find DataSource:", e);
            throw new CSIException("Can't find DataSource:\n", e);
        } catch(SQLException e) {
            m_logger.error("Can't get connection", e);
            throw new CSIException("Can't get connection", e);
        } finally {
            if (m_logger.isInfoEnabled()) {
                m_logger.info("ServiceManagerEJB.getCustomerInfoByServiceElementUSID(\"" + usid + "\") call took " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
            }
            if(connection!=null) {
                try {
                    connection.close();
                } catch(Exception e) {
                    m_logger.error("Connection was not closed succesfully", e);
                }
            }
        }

        return ret;
   }
}