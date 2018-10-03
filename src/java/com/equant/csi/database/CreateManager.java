/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/database/CreateManager.java,v 1.27 2002/12/17 22:15:28 vasyl.rublyov Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.database;

import com.equant.csi.common.CSIConstants;
import com.equant.csi.common.TransDate;
import com.equant.csi.exceptions.CSIException;
import com.equant.csi.exceptions.CSIIllegalStateException;
import com.equant.csi.exceptions.CSIRollbackException;
import com.equant.csi.exceptions.CSIWrongUSID;
import com.equant.csi.jaxb.ObjectFactory;
import com.equant.csi.jaxb.OnceOffChargeType;
import com.equant.csi.jaxb.RecurringChargeType;
import com.equant.csi.jaxb.ServiceElementAttributeType;
import com.equant.csi.jaxb.ServiceElementStatusType;
import com.equant.csi.jaxb.ServiceElementType;
import com.equant.csi.jaxb.SystemType;
import com.equant.csi.jaxb.UsageChargeType;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.utilities.LoggerFactory;
import com.equant.csi.interfaces.cis.CISConstants;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;

import javax.xml.bind.JAXBException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Handles status changes of JABX objects when orders of
 * NEW type is received.
 *
 * @author Vadim Gritsenko
 * @author Vasyl Rublyov
 * @author Kostyantyn Yevenko
 * @version $Revision: 612 $
 *
 * @see Manager
 * @see CSIConstants
 */
public class CreateManager extends Manager implements CSIConstants {
    /** Initializing the logger. */
    private static final Category m_logger = LoggerFactory.getInstance(CreateManager.class.getName());

    /** The user name */
    private String m_user;

    /** The current date */
    private Date m_currentDate;

    /** The order type */
    private String m_orderType;

    /** The order status */
    private String m_orderStatus;

    /**
     * Constructor to be used in EJB.
     *
     * @param connection the Connection using for database connection
     */
    public CreateManager(Connection connection) {
        super(connection);
    }

    /**
     * Constructor to be used from com.equant.csi.database and com.equant.csi.test packages.
     */
    public CreateManager(Connection connection,
                         Date currentDate,
                         String currentUser,
                         String orderType,
                         String orderStatus) {
        super(connection);
        // User and currentDate used to fill in createdBy and createdDate fields.
        m_user = currentUser;
        m_currentDate = currentDate;
        m_orderType = orderType;
        m_orderStatus = orderStatus;
    }

    /**
     * Creates new version of the service in the database.
     *
     * @param xVersion the JAXB version object to create in the database
     *
     * @throws CSIException if any error occurred during version creation
     */
    public void createVersion(VersionType xVersion, boolean updateAll) throws CSIException {
        //
        // Check input parameters
        //STATUS_MODIFY_WITH_PRICE_IMPACT.equals(orderStatus)
        if (    !STATUS_ORDER_RELEASE.equals(xVersion.getOrderstatus()) &&
                !STATUS_MODIFY_WITHOUT_PRICE_IMPACT.equals(xVersion.getOrderstatus()) &&
                !STATUS_ORDER_MANAGE.equals(xVersion.getOrderstatus()) &&
                !STATUS_ORDER_UNCANCEL.equals(xVersion.getOrderstatus()) &&
                !STATUS_CUSTOMER_ACCEPTS_SERVICE.equals(xVersion.getOrderstatus())
        ) {
            throw new CSIIllegalStateException(
                    "CreateManager handles only orders with status " +
                        STATUS_ORDER_RELEASE + ", " +
                        STATUS_ORDER_MANAGE + ", " +
                        STATUS_MODIFY_WITHOUT_PRICE_IMPACT + ", " +
                        STATUS_ORDER_UNCANCEL + ", " +
                        STATUS_CUSTOMER_ACCEPTS_SERVICE
            );
        }

        // User and currentDate used to fill in createdBy and created date fields.
        m_user = xVersion.getSystem().getUser().getName();
        m_currentDate = new Date(xVersion.getOrderSentDate().getTime());

        // These two are used to determine new status of the service version
        m_orderType = xVersion.getOrdertype();
        m_orderStatus = xVersion.getOrderstatus();

        try {
            //Create cVersion here, and continue with populating ServiceElements.
            long versionId = createVersionType(xVersion);

            //Insert cServiceElement records
            for (Iterator i = xVersion.getServiceElement().iterator(); i.hasNext();) {
                final ServiceElementType element = (ServiceElementType)i.next();
                // Create CVersionServiceElement for every ServiceElement.
                createVersionServiceElement(versionId, element, updateAll);
            }
        } catch (SQLException e) {
            throw new CSIRollbackException("Can't perform SQL query. Changes are rolled back.\n"+e);
        } catch (Exception e) {//don't care what kind of exception it will be, just rollback and notify
            throw new CSIRollbackException("CreateManager unable to create Version through exception. Changes are rolled back.\n"+e);
        }
    }

    /**
     * Creates CVersion record in database without creation of
     * CVersionServiceElement records.
     * Suitable for using from com.equant.csi.database package.
     * @param xVersion the JAXB object of new Version.
     * @return ID of newly created CVersion record.
     * @throws SQLException
     */
    long createVersionType(VersionType xVersion) throws SQLException {
        long systemUserId = findOrCreateSystemUser(xVersion.getSystem());

        info("Creating CVersion (type " + xVersion.getOrdertype() + ", status " + xVersion.getOrderstatus() + ") due to order #" + xVersion.getOrderid());

        long versionId = getNextValue(CSIConstants.SEQ_VERSION);
        if(m_currentDate==null) {
            // setting m_currentDate from xVersion (it comes from CIS)
            m_currentDate = new Date(xVersion.getOrderSentDate().getTime());
        }
        
        if(m_user==null) {
            m_user = xVersion.getSystem().getUser().getName();
        }

        PreparedStatement ps = null;
        try {
            ps = m_connection.prepareStatement(
                            "INSERT INTO CVersion (VersionId                    " + //1
                            "                    , SbaHandle                    " + //2
                            "                    , OrdHandle                    " + //3
                            "                    , CtrHandle                    " + //4
                            "                    , SiteHandle                   " + //5
                            "                    , OrderType                    " + //6
                            "                    , CustHandle                   " + //7
                            "                    , EndUserHandle                " + //8
                            "                    , ServiceHandle                " + //9
                            "                    , RatingCurrency               " + //10
                            "                    , ExchangeRate                 " + //11
                            "                    , CreateDate                   " + //12
                            "                    , CreatedBy                    " + //13
                            "                    , LupdDate                     " + //14
                            "                    , LupdBy                       " + //15
                            "                    , OrderStatus                  " + //16
                            "                    , SystemUserId                 " + //17 (fkey)
                            "                    , LocalCurrency                " + //18
                            "                    , LocalCurrencyExchangeRate    " + //19  
							"                    , CoreSiteID                   " + //20	
							"                    , AddressID                    " + //21							
                            "   ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)    "
//                                        1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9
                    );

            ps.setLong(1, versionId);                                       //versionId
            ps.setLong(2, xVersion.getSubbillingAccountId());               //sbaHandle
            ps.setString(3, xVersion.getOrderid());                         //ordHandle
            ps.setString(4, xVersion.getContractId());                      //ctrHandle
            ps.setString(5, xVersion.getSiteId());                          //siteHandle
            ps.setString(6, xVersion.getOrdertype());                       //orderType
            ps.setString(7, xVersion.getCustomerId());                      //custHandle
            ps.setString(8, xVersion.getEndUserId());                       //endUserHandle
            ps.setString(9, xVersion.getServiceId());                       //serviceHandle
            ps.setString(10, xVersion.getCurrency());                       //ratingCurrency
            ps.setDouble(11, xVersion.getExchangeRate());                   //exchangeRate
            ps.setTimestamp(12, TransDate.getTimestamp(m_currentDate));     //createDate
            ps.setString(13, m_user);                                       //createdBy
            ps.setTimestamp(14, TransDate.getTimestamp(m_currentDate));     //lupdDate
            ps.setString(15, m_user);                                       //lupdBy
            ps.setString(16, xVersion.getOrderstatus());                    //orderStatus
            ps.setLong(17, systemUserId);                                   //systemUserId
            ps.setString(18, xVersion.getLocalCurrency());                  //LocalCurrency
            ps.setDouble(19, xVersion.getLocalCurrencyExchangeRate());        //CurrencyExchangeRate
			ps.setString(20, xVersion.getCoreSiteID());                          //CoreSiteID
			ps.setString(21, xVersion.getAddressID());                          //AddressID

            //Insert CVersion here
            ps.executeUpdate();
        } finally {
            carefullyClose(null, ps); ps=null;
        }

        return versionId;
    }

    /**
     * Retrieves the system user and create one if necessary.
     *
     * @param xSystem the JAXB <code>System</code> object
     * @return the <code>CSystemUser</code> record Id
     */
    public long findOrCreateSystemUser(SystemType xSystem) throws SQLException {
        long systemUserId = -1;

        debug("Searching for SystemUser (systemId=" + xSystem.getId() + ", userName=" + xSystem.getUser().getName() + ")");

        PreparedStatement ps = null;
        try {
            ps = m_connection.prepareStatement(
                            "SELECT SystemUserId        " +
                            "  FROM CSystemUser         " +
                            " WHERE SystemUserName = ?  " +
                            "   AND SystemCode = ?      "
                    );

            ps.setString(1, xSystem.getUser().getName());   //systemUserName
            ps.setString(2, xSystem.getId());               //systemCode

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                systemUserId = rs.getLong(1);
            }
        } finally {
            carefullyClose(null, ps); ps=null;
        }

        if (systemUserId == -1) {
            debug("SystemUser not found. Creating new (systemId=" + xSystem.getId() + ", userName=" + xSystem.getUser().getName() + ")");

            try {
                ps = m_connection.prepareStatement(
                        "INSERT INTO CSystemUser (SystemUserId      " + //1
                        "                       , Systemusername    " + //2
                        "                       , Systemcode        " + //3
                        "                       , CreateDate        " + //4
                        "                       , CreatedBy         " + //5
                        "                       , LupdDate          " + //6
                        "                       , LupdBy            " + //7
                        "   ) VALUES (?,?,?,?,?,?,?)                "
                );

                systemUserId = getNextValue(CSIConstants.SEQ_SYSTEM_USER);
                ps.setLong(1, systemUserId);                                    //systemUserId
                ps.setString(2, xSystem.getUser().getName());                   //systemUserName
                ps.setString(3, xSystem.getId());                               //systemCode
                ps.setTimestamp(4, TransDate.getTimestamp(m_currentDate));      //createDate
                ps.setString(5, m_user);                                        //createdBy
                ps.setTimestamp(6, TransDate.getTimestamp(m_currentDate));      //lupdDate
                ps.setString(7, m_user);                                        //lupdBy

                ps.executeUpdate();
            } finally {
                carefullyClose(null, ps); ps=null;
            }
        }

        return systemUserId;
    }

    /**
     * Creates a version of the service element for the current
     * RELEASE (or UNCANCEL) order.
     *
     * @param versionId the JDO Version object
     * @param xElement the JAXB service element
     * @throws CSIException if any error occurred during version creation
     */
    void createVersionServiceElement(long versionId, ServiceElementType xElement, boolean updateAll) throws CSIException, SQLException, JAXBException {

        try { /* Used for debuging */
            // Check if USID is NULL or empty, throw exception.
            if(xElement.getId()==null || xElement.getId().length()==0) {
               throw new CSIWrongUSID("VersionServiceElement.USID is NULL or empty");
            }
    
            QueryManager qm = new QueryManager(m_connection);
            
            // Find existing CServiceElement and update it or create new CServiceElement.
            ServiceElementType dbServiceElement = findOrCreateServiceElement(xElement, updateAll);
    
            if(dbServiceElement==null) {
                debug("Creating VersionServiceElement for serviceElementId=NULL and versionId=" + versionId);
            } else {
                debug("Creating VersionServiceElement for serviceElementId=" + dbServiceElement.getId() + " and versionId=" + versionId);
            }
            long serviceElementId = qm.getServiceElementId(dbServiceElement.getId());
    
            long versionServiceElementId = getNextValue(CSIConstants.SEQ_VERSION_SERVICE_ELEMENT);
    
            PreparedStatement ps = null;
            try {
                ps = m_connection.prepareStatement(
                                "INSERT INTO CVersionServiceElement (VersionServiceElementId    " + //1
                                "                                  , CreateDate                 " + //2
                                "                                  , CreatedBy                  " + //3
                                "                                  , LupdDate                   " + //4
                                "                                  , LupdBy                     " + //5
                                "                                  , VersionId                  " + //6 (fkey)
                                "                                  , ServiceElementId           " + //7 (fkey)
                                "   ) VALUES (?,?,?,?,?,?,?)"
    //                                        1 2 3 4 5 6 7
                        );
    
                ps.setLong(1, versionServiceElementId);                         //versionServiceElementId
                ps.setTimestamp(2, TransDate.getTimestamp(m_currentDate));      //createDate
                ps.setString(3, m_user);                                        //createdBy
                ps.setTimestamp(4, TransDate.getTimestamp(m_currentDate));      //lupdDate
                ps.setString(5, m_user);                                        //lupdBy
                ps.setLong(6, versionId);                                       //versionId
                ps.setLong(7, serviceElementId);                                //serviceElementId
    
                ps.executeUpdate();
            } finally {
                carefullyClose(null, ps); ps=null;
            }
    
            // Save ServiceElementStatus'es in DB.
            createVersionServiceElementStatuses(xElement, versionServiceElementId);
    
            // Save the chargeChangeItem for service element.
            createChargeChangeItems(xElement, null, -1, versionServiceElementId);
    
            // Save ServiceChangeItems without parent Child Relationship.
            createServiceChangeItems(xElement, versionServiceElementId);
        } catch (SQLException e) {
            debug(
                    "Got an SQLException in CreateManager.createVersionServiceElement("
                  + "versionId = " + versionId + ", "
                  + "xElement = ["
                    +  "Id=\"" + xElement.getId() + "\", "
                    +  "UpdatedID=\"" + xElement.getUpdatedID() + "\", "
                    +  "ServiceElementClass=\"" + xElement.getServiceElementClass() + "\", "
                    +  "Name=\"" + xElement.getName() + "\", "
                    +  "Description=\"" + xElement.getDescription() + "\", "
                    +  "CreationDate=\"" + xElement.getCreationDate() + "\", "
                    +  "GrandfatherDate=\"" + xElement.getGrandfatherDate() + "\", "
                    +  "ProductId=\"" + xElement.getProductId() + "\", "
                    +  "SourceSystem=\"" + xElement.getSourceSystem() + "\"]"
                  + "updateAll=" + updateAll + ");"
            );
            throw e;
        }
    }

    /**
     * Finds service element by USID or UpdatedID from the xElement,
     * updates service element if needed, and returns it. If service element
     * could not be found, then new one is created.
     * <p>Thus, service element is created only once for the order. On subsequent
     * MANAGE orders, existing service element will be updated.
     *
     * @param xElement the JAXB service element
     * @return the version service element Id
     */
    private ServiceElementType findOrCreateServiceElement(ServiceElementType xElement, boolean updateAll) throws SQLException, JAXBException {
        // Find (and update) existing ServiceElement by USID or Updated USID
        ServiceElementType dbServiceElement = findServiceElement(xElement);
        updateServiceElement(dbServiceElement, xElement, updateAll);

        if (dbServiceElement == null) {//not found.
            debug("Can't find element " + xElement.getId() + " (" + xElement.getName() + "), creating new.");

            long serviceElementId = getNextValue(CSIConstants.SEQ_SERVICE_ELEMENT);

            PreparedStatement ps = null;
            try {
                ps = m_connection.prepareStatement(
                                    "INSERT INTO CServiceElement (ServiceElementId      " + //1
                                    "                           , Usid                  " + //2
                                    "                           , ServiceElementClass   " + //3
                                    "                           , ServiceElementName    " + //4
                                    "                           , Description           " + //5
                                    "                           , CreationDate          " + //6
                                    "                           , GrandFatherDate       " + //7
                                    "                           , ProdId                " + //8
                                    "                           , CreateDate            " + //9
                                    "                           , CreatedBy             " + //10
                                    "                           , LupdDate              " + //11
                                    "                           , LupdBy                " + //12
                                    "                           , SourceSystem          " + //13
                                    "   ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"
//                                                1 2 3 4 5 6 7 8 9 0 1 2,3
                            );

                ps.setLong(1, serviceElementId);                            //serviceElementId
                ps.setString(2, ((xElement.getUpdatedID()==null) || ("".equals(xElement.getUpdatedID()))?             //usid
                                 xElement.getId() : xElement.getUpdatedID()));
                ps.setString(3, xElement.getServiceElementClass());         //serviceElementClass
                ps.setString(4, xElement.getName());                        //serviceElementName
                ps.setString(5, xElement.getDescription());                 //description
                if (xElement.getCreationDate()==null) {                     //creationDate
                    ps.setNull(6, Types.TIMESTAMP);
                } else {
                    ps.setTimestamp(6, TransDate.getTimestamp(xElement.getCreationDate()));
                }
                if (xElement.getGrandfatherDate()==null) {                  //grandFatherDate
                    ps.setNull(7, Types.TIMESTAMP);
                } else {
                    ps.setTimestamp(7, TransDate.getTimestamp(xElement.getGrandfatherDate()));
                }
                ps.setString(8, xElement.getProductId());                   //prodId
                ps.setTimestamp(9, TransDate.getTimestamp(m_currentDate));//createDate
                ps.setString(10, m_user);                                //createdBy
                ps.setTimestamp(11, TransDate.getTimestamp(m_currentDate));//lupdDate
                ps.setString(12, m_user);                                //lupdBy
                ps.setString(13, getSourceSystem(xElement));               //sourceSystem

                ps.executeUpdate();
                dbServiceElement = findServiceElement(xElement);
            } finally {
                carefullyClose(null, ps); ps=null;
            }
        }

        return dbServiceElement;
    }

    /**
     * Gets all the statuses from the service element and creates
     * corresponding <code>CVersionServiceElementSts</code> records.
     *
     * @param xElement the JAXB service element
     * @param versionServiceElementId version service element Id
     * @throws CSIException if any error occurred during version creation
     */
    private void createVersionServiceElementStatuses(ServiceElementType xElement, long versionServiceElementId) throws SQLException, CSIException {

        debug("Creating VersionServiceElementSts for VersionServiceElementId=" + versionServiceElementId);

        // Get all statuses from the ServiceElement and create corresponding CVersionServiceElementSts objects
        // DO NOT ALLOW DUPLICATES!!!
        final HashMap xStatuses = new HashMap();
        for (Iterator i = xElement.getServiceElementStatus().iterator(); i.hasNext();) {
            final ServiceElementStatusType xStatus = (ServiceElementStatusType) i.next();
            xStatuses.put(xStatus.getStatus(), xStatus);
        }

        // Remove CSI statuses, leave GOLD (or any other) statuses
        xStatuses.remove(CSIConstants.STATUS_INPROGRESS);
        xStatuses.remove(CSIConstants.STATUS_CURRENT);
        xStatuses.remove(CSIConstants.STATUS_DISCONNECT);
        xStatuses.remove(CSIConstants.STATUS_DELETE);

        // Add current CSI status for this create operation
        ServiceElementStatusType xStatusCurrent = null;
        try {
            xStatusCurrent = m_objectFactory.createServiceElementStatusType();
        } catch (JAXBException e) {
            throw new CSIRollbackException("Unable to create JAXB object (ServiceElementStatusType) through exception: "+e);
        }

        if (ORDER_TYPE_NEW.equals(m_orderType) || ORDER_TYPE_CHANGE.equals(m_orderType)) {
            if (STATUS_ORDER_RELEASE.equals(m_orderStatus) || STATUS_ORDER_UNCANCEL.equals(m_orderStatus) || STATUS_MODIFY_WITHOUT_PRICE_IMPACT.equals(m_orderStatus)) {
            	if (STATUS_DISCONNECT.equals(xElement.getType().getCategory())) {
	                xStatusCurrent.setStatus(STATUS_DISCONNECT);
	                xStatusCurrent.setDate(new TransDate(m_currentDate.getTime()));
	                xStatuses.put(STATUS_DISCONNECT, xStatusCurrent);
            	} else {
	                xStatusCurrent.setStatus(STATUS_INPROGRESS);
	                xStatusCurrent.setDate(new TransDate(m_currentDate.getTime()));
	                xStatuses.put(STATUS_INPROGRESS, xStatusCurrent);
            	}
            } else if (STATUS_ORDER_MANAGE.equals(m_orderStatus) || STATUS_CUSTOMER_ACCEPTS_SERVICE.equals(m_orderStatus) || STATUS_CUSTOMER_ACCEPTS_SERVICE.equals(m_orderStatus)) {
            	if (STATUS_DISCONNECT.equals(xElement.getType().getCategory())) {
	                xStatusCurrent.setStatus(STATUS_DISCONNECT);
	                xStatusCurrent.setDate(new TransDate(m_currentDate.getTime()));
	                xStatuses.put(STATUS_DISCONNECT, xStatusCurrent);
            	} else {
	                xStatusCurrent.setStatus(STATUS_CURRENT);
	                xStatusCurrent.setDate(new TransDate(m_currentDate.getTime()));
	                xStatuses.put(STATUS_CURRENT, xStatusCurrent);
            	}
            } else {
                // CreateManager is not intended to be used in this situation
                throw new CSIIllegalStateException("CreateManager can not process order of type " + m_orderType + " in status " + m_orderStatus);
            }
        } else if (ORDER_TYPE_DISCONNECT.equals(m_orderType)) {
            xStatusCurrent.setStatus(STATUS_DISCONNECT);
            xStatusCurrent.setDate(new TransDate(m_currentDate.getTime()));
            xStatuses.put(STATUS_DISCONNECT, xStatusCurrent);
        } else if (ORDER_TYPE_EXISTING.equals(m_orderType)) {
            if (STATUS_DELETE.equals(xElement.getType().getCategory())) {
                xStatusCurrent.setStatus(STATUS_DELETE);
                xStatusCurrent.setDate(new TransDate(m_currentDate.getTime()));
                xStatuses.put(STATUS_DELETE, xStatusCurrent);
            } else if (STATUS_DISCONNECT.equals(xElement.getType().getCategory())) {
                xStatusCurrent.setStatus(STATUS_DISCONNECT);
                xStatusCurrent.setDate(new TransDate(m_currentDate.getTime()));
                xStatuses.put(STATUS_DISCONNECT, xStatusCurrent);
        	} else {
                xStatusCurrent.setStatus(STATUS_CURRENT);
                xStatusCurrent.setDate(new TransDate(m_currentDate.getTime()));     // minus one second in order to make it happened before the changed side
                xStatuses.put(STATUS_CURRENT, xStatusCurrent);
            }
        } else {
            // CreateManager is not intended to be used in this situation
            throw new CSIIllegalStateException("CreateManager can not process order of type " + m_orderType + " in status " + m_orderStatus);
        }

        debug("Set status to " + xStatusCurrent.getStatus().toUpperCase() + " for element " + xElement.getId() + " (type: " + m_orderType + ", status " + m_orderStatus + ")");

        PreparedStatement ps = null;
        try {
            ps = m_connection.prepareStatement(
                    "INSERT INTO CVersionServiceElementSts (VersionServiceElementStsId  " + //1
                    "                                     , StatusTypeCode              " + //2
                    "                                     , StatusDate                  " + //3
                    "                                     , CreateDate                  " + //4
                    "                                     , CreatedBy                   " + //5
                    "                                     , LupdDate                    " + //6
                    "                                     , LupdBy                      " + //7
                    "                                     , VersionServiceElementId     " + //8 (fkey)
                    "   ) VALUES (?,?,?,?,?,?,?,?)                                      "
//                                1 2 3 4 5 6 7 8
                    );

            // Now go through collected statuses and insert them into DB.
            for (Iterator i = xStatuses.values().iterator(); i.hasNext();) {
                ServiceElementStatusType xSts = (ServiceElementStatusType)i.next();

                ps.clearParameters();

                ps.setLong(1, getNextValue(CSIConstants.SEQ_VERSION_SERVICE_ELEMENT_STS)); //versionServiceElementStsId
                ps.setString(2, xSts.getStatus());                                              //statusTypeCode
                if (xSts.getDate()==null) {                                                     //statusDate
                    ps.setNull(3, Types.TIMESTAMP);
                } else {
                    ps.setTimestamp(3, TransDate.getTimestamp(xSts.getDate()));
                }
                ps.setTimestamp(4, TransDate.getTimestamp(m_currentDate));                   //createDate
                ps.setString(5, m_user);                                                     //createdBy
                ps.setTimestamp(6, TransDate.getTimestamp(m_currentDate));                   //lupdDate
                ps.setString(7, m_user);                                                     //lupdBy
                ps.setLong(8, versionServiceElementId);                                         //versionServiceElementId

                ps.executeUpdate();
            }
        } finally {
            xStatuses.clear();
            carefullyClose(null, ps); ps=null;
        }
    }

    /**
     * Gets all the service change items from the service element and
     * creates corresponding change item for every attribute of the
     * service in incoming order.
     *
     * @param xElement the JAXB service element object
     * @param versionServiceElementId the version service element Id
     */
    private void createServiceChangeItems(ServiceElementType xElement, long versionServiceElementId) throws SQLException {

        // For every attribute of the service in incoming order, create corresponding change item
        for (Iterator i = xElement.getServiceElementAttribute().iterator(); i.hasNext();) {
            final ServiceElementAttributeType xAttribute = (ServiceElementAttributeType) i.next();

            createServiceAttribute(xAttribute, versionServiceElementId);
        }
    }

    /**
     * Creates CServiceAttribute record and corresponding CServiceChangeItem.
     * @param xAttribute the JAXB ServiceElementAttributeType object
     * @param versionServiceElementId the version service element ID
     * @throws SQLException
     */
    void createServiceAttribute(final ServiceElementAttributeType xAttribute, long versionServiceElementId) throws SQLException {

        long serviceAttributeId = getNextValue(CSIConstants.SEQ_SERVICE_ATTRIBUTE);

        PreparedStatement ps = null;
        try {
            ps = m_connection.prepareStatement(
                    "INSERT INTO CServiceAttribute (ServiceAttributeId      " + //1
                    "                             , ServiceAttributeName    " + //2
                    "                             , Value                   " + //3
                    "                             , CreateDate              " + //4
                    "                             , CreatedBy               " + //5
                    "                             , LupdDate                " + //6
                    "                             , LupdBy                  " + //7
                    "                             , CustomerLabel           " + //8
                    "                             , LocalCurrency           " + //9
                    "   ) VALUES (?,?,?,?,?,?,?,?,?)                            "
//                                1 2 3 4 5 6 7 8 9
                    );

            ps.setLong(1, serviceAttributeId);              //serviceAttributeId
            ps.setString(2, xAttribute.getName());          //serviceAttributeName
            if (xAttribute.getValue()==null || xAttribute.getValue().length()==0) {//value
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, xAttribute.getValue());
            }
            ps.setTimestamp(4, TransDate.getTimestamp(m_currentDate));//createDate
            ps.setString(5, m_user);                     //createdBy
            ps.setTimestamp(6, TransDate.getTimestamp(m_currentDate));//lupdDate
            ps.setString(7, m_user);                     //lupdBy
            ps.setString(8, xAttribute.getCustomerLabel());//customerLabel
            ps.setBoolean(9, xAttribute.isLocalCurrency());//localCurrency            
            ps.executeUpdate();
        } finally {
            carefullyClose(null, ps); ps=null;
        }

        // Create charge items for this attribute
        createChargeChangeItems(null, xAttribute, serviceAttributeId, versionServiceElementId);

        long serviceChangeItemId = getNextValue(CSIConstants.SEQ_SERVICE_CHANGE_ITEM);
        // Create CServiceChangeItem record
        try {
            ps = m_connection.prepareStatement(
                    "INSERT INTO CServiceChangeItem (ServiceChangeItemId        " + //1
                    "                              , ChangeTypeCode             " + //2
                    "                              , CreateDate                 " + //3
                    "                              , CreatedBy                  " + //4
                    "                              , LupdDate                   " + //5
                    "                              , LupdBy                     " + //6
                    "                              , ServiceAttributeId         " + //7
                    "                              , VersionServiceElementId    " + //8
                    "   ) VALUES (?,?,?,?,?,?,?,?)                              "
//                                1 2 3 4 5 6 7 8
            );

            ps.setLong(1, serviceChangeItemId);             //serviceChangeItemId
            ps.setString(2, xAttribute.getChangeTypeCode());          //changeTypeCode
            ps.setTimestamp(3, TransDate.getTimestamp(m_currentDate));//createDate
            ps.setString(4, m_user);                     //createdBy
            ps.setTimestamp(5, TransDate.getTimestamp(m_currentDate));//lupdDate
            ps.setString(6, m_user);                     //lupdBy
            ps.setLong(7, serviceAttributeId);              //serviceAttributeId
            ps.setLong(8, versionServiceElementId);         //versionServiceElementId

            ps.executeUpdate();
        } finally {
            carefullyClose(null, ps); ps=null;
        }
    }

    /**
     * Gets all the charge change items from the service element and creates
     * corresponding CChargeChangeItem objects.
     *
     * @param xElement   the JAXB service element object
     * @param xAttribute the JAXB service element attribute object
     * @param serviceAttributeId the version service attribute Id
     * @param versionServiceElementId   the version service element Id
     */
    private void createChargeChangeItems(ServiceElementType xElement,
                                         ServiceElementAttributeType xAttribute,
                                         long serviceAttributeId,
                                         long versionServiceElementId)
            throws SQLException {

        // It never be more then 3 items
        final UsageChargeType xUsageCharge = (xAttribute == null) ? xElement.getUsageCharge() : xAttribute.getUsageCharge();
        if (xUsageCharge != null) {
            createUsageChargeChangeItem(versionServiceElementId, serviceAttributeId, xUsageCharge);
        }

        final OnceOffChargeType xOnceOffCharge = (xAttribute == null) ? xElement.getOnceOffCharge() : xAttribute.getOnceOffCharge();
        if (xOnceOffCharge != null) {
            createOnceOffChargeChangeItem(versionServiceElementId, serviceAttributeId, xOnceOffCharge);
        }

        final RecurringChargeType xRecurringCharge = (xAttribute == null) ? xElement.getRecurringCharge() : xAttribute.getRecurringCharge();
        if (xRecurringCharge != null) {
            createRecurringChargeChangeItem(versionServiceElementId, serviceAttributeId, xRecurringCharge);
        }
    }

    /**
     * Creates the usage charge change item record from the
     * <code>CVersionServiceElement</code> and <code>CServiceAttribute</code> IDs.
     *
     * @param versionServiceElementId  the version service element Id
     * @param serviceAttributeId the service attribute Id
     * @param xCharge    the JAXB <code>UsageCharge</code> object
     */
    void createUsageChargeChangeItem(long versionServiceElementId, long serviceAttributeId, UsageChargeType xCharge)
            throws SQLException {

        long usageChargeId = getNextValue(CSIConstants.SEQ_USAGE_CHARGE);

        // Create object holding actual charge amount
        PreparedStatement ps = null;
        try {
            ps = m_connection.prepareStatement(
                            "INSERT INTO CUsageCharge (UsageChargeId    " + //1
                            "                        , ChgCatId         " + //2
                            "                        , CreateDate       " + //3
                            "                        , CreatedBy        " + //4
                            "                        , LupdDate         " + //5
                            "                        , LupdBy           " + //6
                            "   ) VALUES (?,?,?,?,?,?)");
//                                        1 2 3 4 5 6

            ps.setLong(1, usageChargeId);                   //usageChargeId
            ps.setString(2, xCharge.getChargeCategoryId()); //chgCatId
            ps.setTimestamp(3, TransDate.getTimestamp(m_currentDate));//createDate
            ps.setString(4, m_user);                     //createdBy
            ps.setTimestamp(5, TransDate.getTimestamp(m_currentDate));//lupdDate
            ps.setString(6, m_user);                     //lupdBy

            ps.executeUpdate();
        } finally {
            carefullyClose(null, ps); ps=null;
        }

        long chargeChangeItemId = getNextValue(CSIConstants.SEQ_CHARGE_CHANGE_ITEM);
        // Now create the ChargeChangeItem record
        try {
            ps = m_connection.prepareStatement(
                    "INSERT INTO CChargeChangeItem (ChargeChangeItemId      " + //1
                    "                             , ChangeTypeCode          " + //2
                    "                             , CreateDate              " + //3
                    "                             , CreatedBy               " + //4
                    "                             , LupdDate                " + //5
                    "                             , LupdBy                  " + //6
                    "                             , ServiceAttributeId      " + //7 (fkey)
                    "                             , UsageChargeId           " + //8 (fkey)
                    "                             , VersionServiceElementId " + //9 (fkey)
                    "   ) VALUES (?,?,?,?,?,?,?,?,?)"
//                                1 2 3 4 5 6 7 8 9
            );

            ps.setLong(1, chargeChangeItemId);              //chargeChangeItemId
            ps.setString(2, xCharge.getChangeTypeCode());   //changeTypeCode
            ps.setTimestamp(3, TransDate.getTimestamp(m_currentDate));//createDate
            ps.setString(4, m_user);                     //createdBy
            ps.setTimestamp(5, TransDate.getTimestamp(m_currentDate));//lupdDate
            ps.setString(6, m_user);                     //lupdBy
            if (serviceAttributeId!=-1) {                   //serviceAttributeId
                ps.setLong(7, serviceAttributeId);
            } else {
                ps.setNull(7, Types.NUMERIC);
            }
            ps.setLong(8, usageChargeId);                  //usageChargeId
            ps.setLong(9, versionServiceElementId);        //versionServiceElementId

            ps.executeUpdate();
        } finally {
            carefullyClose(null, ps); ps=null;
        }
    }

    /**
     * Creates the recurring charge change item record from the
     * CVersionServiceElement and CServiceAttribute IDs.
     *
     * @param versionServiceElementId  the version service element Id
     * @param serviceAttributeId the service attribute Id
     * @param xCharge    the JAXB RecurringCharge object
     * @throws SQLException
     */
    void createRecurringChargeChangeItem(long versionServiceElementId,
                                         long serviceAttributeId,
                                         RecurringChargeType xCharge)
            throws SQLException {

        long recurringChargeId = getNextValue(CSIConstants.SEQ_RECURRING_CHARGE);

        // Create object holding actual charge amount
        PreparedStatement ps = null;
        try {
            ps = m_connection.prepareStatement(
                            "INSERT INTO CRecurringCharge (RecurringChargeId    " + //1
                            "                            , Amount               " + //2
                            "                            , DiscCode             " + //3
                            "                            , ChgCatId             " + //4
                            "                            , CreateDate           " + //5
                            "                            , CreatedBy            " + //6
                            "                            , LupdDate             " + //7
                            "                            , LupdBy               " + //8
                            "   ) VALUES (?,?,?,?,?,?,?,?)");
//                                        1 2 3 4 5 6 7 8

            ps.setLong(1, recurringChargeId);               //recurringChargeId
            ps.setDouble(2, xCharge.getAmount());           //amount
            ps.setString(3, xCharge.getDiscountCode());     //discCode
            ps.setString(4, xCharge.getChargeCategoryId()); //chgCatId
            ps.setTimestamp(5, TransDate.getTimestamp(m_currentDate));//createDate
            ps.setString(6, m_user);                     //createdBy
            ps.setTimestamp(7, TransDate.getTimestamp(m_currentDate));//lupdDate
            ps.setString(8, m_user);                     //lupdBy

            ps.executeUpdate();
        } finally {
            carefullyClose(null, ps); ps=null;
        }

        long chargeChangeItemId = getNextValue(CSIConstants.SEQ_CHARGE_CHANGE_ITEM);

        // Now create the ChargeChangeItem record
        try {
            ps = m_connection.prepareStatement(
                    "INSERT INTO cChargeChangeItem (chargeChangeItemId      " + //1
                    "                             , changeTypeCode          " + //2
                    "                             , createDate              " + //3
                    "                             , createdBy               " + //4
                    "                             , lupdDate                " + //5
                    "                             , lupdBy                  " + //6
                    "                             , recurringChargeId       " + //7 (fkey)
                    "                             , serviceAttributeId      " + //8 (fkey)
                    "                             , versionServiceElementId " + //9 (fkey)
                    ") VALUES (?,?,?,?,?,?,?,?,?)");
//                             1 2 3 4 5 6 7 8 9

            ps.setLong(1, chargeChangeItemId);              //chargeChangeItemId
            ps.setString(2, xCharge.getChangeTypeCode());   //changeTypeCode
            ps.setTimestamp(3, TransDate.getTimestamp(m_currentDate));//createDate
            ps.setString(4, m_user);                     //createdBy
            ps.setTimestamp(5, TransDate.getTimestamp(m_currentDate));//lupdDate
            ps.setString(6, m_user);                     //lupdBy
            ps.setLong(7, recurringChargeId);               //recurringChargeId
            if (serviceAttributeId!=-1) {                   //serviceAttributeId
                ps.setLong(8, serviceAttributeId);
            } else {
                ps.setNull(8, Types.NUMERIC);
            }
            ps.setLong(9, versionServiceElementId);         //versionServiceElementId

            ps.executeUpdate();
        } finally {
            carefullyClose(null, ps); ps=null;
        }
    }

    /**
     * Creates the Once Off charge change item record.
     *
     * @param versionServiceElementId the version service element Id
     * @param serviceAttributeId the service attribute Id
     * @param xCharge    the JAXB OnceOffCharge object
     */
    void createOnceOffChargeChangeItem(long versionServiceElementId,
                                       long serviceAttributeId,
                                       OnceOffChargeType xCharge)
            throws SQLException {

        long onceOffChargeId = getNextValue(CSIConstants.SEQ_ONCE_OFF_CHARGE);

        // Create object holding actual charge amount
        PreparedStatement ps = null;
        try {
            ps = m_connection.prepareStatement(
                            "INSERT INTO COnceOffCharge (OnceOffChargeId    " + //1
                            "                          , Amount             " + //2
                            "                          , DiscCode           " + //3
                            "                          , ChgCatId           " + //4
                            "                          , CreateDate         " + //5
                            "                          , CreatedBy          " + //6
                            "                          , LupdDate           " + //7
                            "                          , LupdBy             " + //8
                            "   ) VALUES (?,?,?,?,?,?,?,?)                  "
//                                        1 2 3 4 5 6 7 8
                    );

            ps.setLong(1, onceOffChargeId);                 //onceOffChargeId
            ps.setDouble(2, xCharge.getAmount());           //amount
            ps.setString(3, xCharge.getDiscountCode());     //discCode
            ps.setString(4, xCharge.getChargeCategoryId()); //chgCatId
            ps.setTimestamp(5, TransDate.getTimestamp(m_currentDate));//createDate
            ps.setString(6, m_user);                     //createdBy
            ps.setTimestamp(7, TransDate.getTimestamp(m_currentDate));//lupdDate
            ps.setString(8, m_user);                     //lupdBy

            ps.executeUpdate();
        } finally {
            carefullyClose(null, ps); ps=null;
        }

        long chargeChangeItemId = getNextValue(CSIConstants.SEQ_CHARGE_CHANGE_ITEM);

        // Now create the ChargeChangeItem record
        try {
            ps = m_connection.prepareStatement(
                    "INSERT INTO CChargeChangeItem (ChargeChangeItemId      " + //1
                    "                             , ChangeTypeCode          " + //2
                    "                             , CreateDate              " + //3
                    "                             , CreatedBy               " + //4
                    "                             , LupdDate                " + //5
                    "                             , LupdBy                  " + //6
                    "                             , OnceOffChargeId         " + //7 (fkey)
                    "                             , ServiceAttributeId      " + //8 (fkey)
                    "                             , VersionServiceElementId " + //9 (fkey)
                    "   ) VALUES (?,?,?,?,?,?,?,?,?)                        "
//                                1 2 3 4 5 6 7 8 9
            );

            ps.setLong(1, chargeChangeItemId);              //chargeChangeItemId
            ps.setString(2, xCharge.getChangeTypeCode());   //changeTypeCode
            ps.setTimestamp(3, TransDate.getTimestamp(m_currentDate));//createDate
            ps.setString(4, m_user);                     //createdBy
            ps.setTimestamp(5, TransDate.getTimestamp(m_currentDate));//lupdDate
            ps.setString(6, m_user);                     //lupdBy
            ps.setLong(7, onceOffChargeId);                 //onceOffChargeId
            if (serviceAttributeId!=-1) {                   //serviceAttributeId
                ps.setLong(8, serviceAttributeId);
            } else {
                ps.setNull(8, Types.NUMERIC);
            }
            ps.setLong(9, versionServiceElementId);        //versionServiceElementId

            ps.executeUpdate();
        } finally {
            carefullyClose(null, ps); ps=null;
        }
    }

    /**
     * Finds the service element by USID or Updated USID, and updates
     * service element if it differs from the passed xServiceElement.
     *
     * @param xServiceElement the specified <code>ServiceElement</code>
     * @return retrieved ServiceElementType value or value of null if not found.
     *
     * @throws SQLException if any error occurred during processing the query
     */
    public ServiceElementType findServiceElement(ServiceElementType xServiceElement) throws SQLException, JAXBException {

        long serviceElementId = -1;
        final String updatedId = xServiceElement.getUpdatedID();
        final ObjectFactory factory = new ObjectFactory();
        ServiceElementType set = null;
        
        debug("Searching for ServiceElement (USID=" + xServiceElement.getId() + ", updatedUSID=" + updatedId + ")");

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = m_connection.prepareStatement(
                            "SELECT Usid                " + //1
                            "     , ServiceElementName  " + //2
                            "     , Description         " + //3
                            "     , CreationDate        " + //4
                            "     , SourceSystem        " + //5
                            "  FROM CServiceElement     " +
                            " WHERE Usid = ?            " +
                            "    OR Usid = ?            "
                    );

            ps.setString(1, xServiceElement.getId());
            ps.setString(2, updatedId);

            rs = ps.executeQuery();
            if (rs.next()) {
                set = factory.createServiceElementType();
                set.setId(rs.getString(1));
                set.setName(rs.getString(2));
                set.setDescription(rs.getString(3));
                Timestamp tmp = rs.getTimestamp(4);
                if(tmp!=null) {
                    set.setCreationDate(new TransDate(tmp.getTime()));
                } else {
                    set.setCreationDate(null);
                }
                set.setSourceSystem(rs.getString(5));
                debug("Found ServiceElement (ServiceElementID=" + serviceElementId + ")");
            }
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return set;
    }

    protected void createRemoteSiteCode(long versionServiceElementId, String attrName, String siteCode) throws SQLException {
        long serviceAttributeId = 0;
        serviceAttributeId = getNextValue(CSIConstants.SEQ_SERVICE_ATTRIBUTE);

        PreparedStatement ps = null;
        try {
            debug("Creating new attribute record....");
            ps = m_connection.prepareStatement(
                    "INSERT INTO CServiceAttribute (ServiceAttributeId      " + //1
                    "                             , ServiceAttributeName    " + //2
                    "                             , Value                   " + //3
                    "                             , CreateDate              " + //4
                    "                             , CreatedBy               " + //5
                    "                             , LupdDate                " + //6
                    "                             , LupdBy                  " + //7
                    "   ) VALUES (?,?,?,?,?,?,?)                            "
//                                1 2 3 4 5 6 7
                    );

            ps.setLong(1, serviceAttributeId);                         //serviceAttributeId
            ps.setString(2, attrName);                                 //serviceAttributeName
            ps.setString(3, siteCode);
            ps.setTimestamp(4, TransDate.getTimestamp(m_currentDate)); //createDate
            ps.setString(5, CISConstants.CIS_CSI_PROGUSER);            //createdBy
            ps.setTimestamp(6, TransDate.getTimestamp(m_currentDate)); //lupdDate
            ps.setString(7, CISConstants.CIS_CSI_PROGUSER);            //lupdBy

            ps.executeUpdate();
            debug("New attribute record created. ID=" + serviceAttributeId);
        } finally {
            carefullyClose(null, ps); ps=null;
        }

        long serviceChangeItemId = getNextValue(CSIConstants.SEQ_SERVICE_CHANGE_ITEM);
        // Create CServiceChangeItem record
        try {
            debug("Creating new service change record....");
            ps = m_connection.prepareStatement(
                    "INSERT INTO CServiceChangeItem (ServiceChangeItemId        " + //1
                    "                              , CreateDate                 " + //2
                    "                              , CreatedBy                  " + //3
                    "                              , LupdDate                   " + //4
                    "                              , LupdBy                     " + //5
                    "                              , ServiceAttributeId         " + //6
                    "                              , VersionServiceElementId    " + //7
                    "   ) VALUES (?,?,?,?,?,?,?)                                "
//                                1 2 3 4 5 6 7
            );

            ps.setLong(1, serviceChangeItemId);                        //serviceChangeItemId
            ps.setTimestamp(2, TransDate.getTimestamp(m_currentDate)); //createDate
            ps.setString(3, CISConstants.CIS_CSI_PROGUSER);            //createdBy
            ps.setTimestamp(4, TransDate.getTimestamp(m_currentDate)); //lupdDate
            ps.setString(5, CISConstants.CIS_CSI_PROGUSER);            //lupdBy
            ps.setLong(6, serviceAttributeId);                         //serviceAttributeId
            ps.setLong(7, versionServiceElementId);                    //versionServiceElementId

            ps.executeUpdate();
            debug("New service change item created. ID=" + serviceChangeItemId);
        } finally {
            carefullyClose(null, ps); ps=null;
        }
    }

    /**
     * updates service element if it differs from the passed xServiceElement.
     *
     * @param dbServiceElement the specified <code>ServiceElement</code>
     * @return retrieved cServiceElementId value or value of -1 if not found.
     *
     * @throws SQLException if any error occurred during processing the query
     */
    public void updateServiceElement(ServiceElementType dbServiceElement, ServiceElementType xServiceElement, boolean updateAll) throws SQLException{

        if(dbServiceElement==null || xServiceElement==null || StringUtils.isEmpty(xServiceElement.getId())) {
            return;
        }
        
        final String updatedId = xServiceElement.getUpdatedID();

        PreparedStatement ps = null;
        //ResultSet rs = null;

        try {
            //Check if new Service Element was changed
            boolean hasChanged = false;
            boolean hasNewUsid = false;
            String usidNew = dbServiceElement.getId(); //get current value
            if (updatedId!=null && updatedId.trim().length()>0 && !updatedId.equals(dbServiceElement.getId())) {
                hasChanged = true;
                usidNew = updatedId;
                hasNewUsid=true;
            }

            final String classType = xServiceElement.getServiceElementClass();
            String servicElementClassTypeNew = dbServiceElement.getServiceElementClass();
            if (classType !=null && !classType.equals(servicElementClassTypeNew)) {
                hasChanged = true;
                servicElementClassTypeNew = classType;
            }

            final String name = xServiceElement.getName();
            String serviceElementNameNew = dbServiceElement.getName(); //get current value
            if (name!=null && !name.equals(serviceElementNameNew)) {
                hasChanged = true;
                serviceElementNameNew = name;
            }

            final String description = xServiceElement.getDescription();
            String descriptionNew = dbServiceElement.getDescription(); //get current value
            if (description!=null && !description.equals(descriptionNew)) {
                descriptionNew = description;
                hasChanged = true;
            }

            final TransDate creationDate = xServiceElement.getCreationDate();
            Date creationDateNew = dbServiceElement.getCreationDate(); //get current value
            if (creationDate!=null && !creationDate.equals(creationDateNew)) {
                creationDateNew = creationDate;
                hasChanged = true;
            }

            String origSourceSystem = dbServiceElement.getSourceSystem(); // Source System
            if(origSourceSystem!=null && !origSourceSystem.equals(xServiceElement.getSourceSystem()) && !origSourceSystem.equals(SYSTEM_TYPE_CIS)) {
                hasChanged = true;
                origSourceSystem = getSourceSystem(xServiceElement);
            }

            try {if (ps != null) ps.close();} catch (Exception e) {info("PreparedStatement was not closed succesfully", e);}
            ps = null;

            if (hasChanged) {
                debug("Updating ServiceElement (USID=" + xServiceElement.getId() + ", updatedUSID=" + updatedId + ", SourceSystem=\"" + origSourceSystem + "\")");

                if(hasNewUsid) {
                ps = m_connection.prepareStatement(
                        "UPDATE CServiceElement         " +
                        "   SET Usid = ?                " + //1
                        "     , ServiceElementName = ?  " + //2
                        "     , Description = ?         " + //3
                        "     , CreationDate = ?        " + //4
                        "     , LupdDate = ?            " + //5
                        "     , LupdBy = ?              " + //6
                        "     , SourceSystem = ?        " + //7
                        "     , SERVICEELEMENTCLASS = ? " + //8
                        " WHERE USID = ?    "   //9
                );

                ps.setString(1, usidNew);               //usid
                ps.setString(2, serviceElementNameNew); //serviceElementName
                ps.setString(3, descriptionNew);        //description
                ps.setTimestamp(4, TransDate.getTimestamp(creationDateNew));//creationDate
                ps.setTimestamp(5, TransDate.getTimestamp(m_currentDate));//lupdDate
                ps.setString(6, m_user);                  //lupdBy
                ps.setString(7, origSourceSystem);
                ps.setString(8, servicElementClassTypeNew); //serviceelementclass
                ps.setString(9, dbServiceElement.getId());        //serviceElementId
                }else {
                    ps = m_connection.prepareStatement(
                            "UPDATE CServiceElement         " +
                            "   SET                         " + //
                            "       ServiceElementName = ?  " + //1
                            "     , Description = ?         " + //2
                            "     , CreationDate = ?        " + //3
                            "     , LupdDate = ?            " + //4
                            "     , LupdBy = ?              " + //5
                            "     , SourceSystem = ?        " + //6
                            "     , SERVICEELEMENTCLASS = ? " + //7
                            " WHERE USID = ?    "   //8
                    );

                    ps.setString(1, serviceElementNameNew); //serviceElementName
                    ps.setString(2, descriptionNew);        //description
                    ps.setTimestamp(3, TransDate.getTimestamp(creationDateNew));//creationDate
                    ps.setTimestamp(4, TransDate.getTimestamp(m_currentDate));//lupdDate
                    ps.setString(5, m_user);                  //lupdBy
                    ps.setString(6, origSourceSystem);
                    ps.setString(7, servicElementClassTypeNew); //serviceelementclass
                    ps.setString(8, dbServiceElement.getId());        //serviceElementId
                }

                ps.executeUpdate();
            }
        } finally {
            carefullyClose(null, ps); ps=null;
        }
    }
    
    protected Category getLogger() {
        return CreateManager.m_logger;
    }

    public void setUser( String user) {
        m_user = user;
    }
}
