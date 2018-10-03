/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/database/UpdateManager.java,v 1.40 2005/03/02 13:13:12 andrey.krot Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Hashtable;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import com.equant.csi.common.CSIConstants;
import com.equant.csi.common.TransDate;
import com.equant.csi.exceptions.CSIException;
import com.equant.csi.exceptions.CSIRollbackException;
import com.equant.csi.exceptions.CSIWrongUSID;
import com.equant.csi.exceptions.NoServiceElementFound;
import com.equant.csi.interfaces.cis.CISConstants;
import com.equant.csi.jaxb.CustomerHandleChangeType;
import com.equant.csi.jaxb.ObjectFactory;
import com.equant.csi.jaxb.OnceOffChargeType;
import com.equant.csi.jaxb.RecurringChargeType;
import com.equant.csi.jaxb.ServiceElement;
import com.equant.csi.jaxb.ServiceElementAttribute;
import com.equant.csi.jaxb.ServiceElementAttributeType;
import com.equant.csi.jaxb.ServiceElementType;
import com.equant.csi.jaxb.SiteContactType;
import com.equant.csi.jaxb.SiteHandleChangeType;
import com.equant.csi.jaxb.UsageChargeType;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.jaxb.SiteInformationType;
import com.equant.csi.jaxb.SiteContactType;
import com.equant.csi.jaxb.impl.ServiceElementTypeImpl;
import com.equant.csi.utilities.LoggerFactory;

/**
 * Handles status changes of JAXB objects when orders of
 * UPDATE type is received.
 *
 * @author  Vadim Gritsenko
 * @author  Vasyl Rublyov
 * @author  Kostyantyn Yevenko
 * @version $Revision: 632 $
 *
 * @see Manager
 * @see CSIConstants
 */
public class UpdateManager extends Manager implements CSIConstants {
    /**
     * Initializing the logger.
     */
    private static final Category m_logger = LoggerFactory.getInstance(UpdateManager.class.getName());

    protected static final Hashtable CIS_AC_IGNORE_VALUE;
    protected static final Hashtable CIS_BACKUP_IGNORE_VALUE;
    protected static final Hashtable CIS_TRANSPORT_IGNORE_VALUE;

    static {
        CIS_AC_IGNORE_VALUE = new Hashtable();
        CIS_BACKUP_IGNORE_VALUE = new Hashtable();
        CIS_TRANSPORT_IGNORE_VALUE = new Hashtable();

        CIS_AC_IGNORE_VALUE.put("Access Connection Name", "Access Connection");
        CIS_BACKUP_IGNORE_VALUE.put("Access Connection Name", "Access Connection");
        CIS_TRANSPORT_IGNORE_VALUE.put("Remote Site Code", "UNKNOWN");
        CIS_TRANSPORT_IGNORE_VALUE.put("Remote Site", "UNKNOWN");
        CIS_TRANSPORT_IGNORE_VALUE.put("Transport Type", "UNKNOWN");
    }
    /**
     * The user name.
     */
    private String m_user;

    /**
     * The current date.
     */
    private Date m_currentDate;

    /**
     * The reference to the CreateManager class.
     */
    private CreateManager m_createManager;

    /**
     * The reference to the CreateManager class.
     */
    private QueryManager m_queryManager;
    /**
     * Constructor to be used in EJB.
     *
     * @param connection the Connection using for database connection
     */
    /**
     * The reference to the CreateManager class.
     */
    private DeleteManager m_deleteManager;
    /**
     * Constructor to be used in EJB.
     *
     * @param connection the Connection using for database connection
     */
    public UpdateManager(Connection connection) {
        super(connection);
    }

    /**
     * Updates the version object.
     *
     * @param xVersion the JAXB version object
     * @throws CSIException if any error occurred during version updating
     */
    public void updateVersion(VersionType xVersion, String status, boolean updateAll) throws CSIException {
        // User and currentDate used to fill in createdBy and created date fields.
        m_user = xVersion.getSystem().getUser().getName();
        m_currentDate = xVersion.getOrderSentDate();

        //Create CreateManager
        m_createManager = new CreateManager(
                m_connection,
                m_currentDate,
                m_user,
                xVersion.getOrdertype(),
                xVersion.getOrderstatus()
        );

        m_queryManager = new QueryManager(m_connection);
        m_deleteManager = new DeleteManager(m_connection);
        
        try {
            if (SYSTEM_TYPE_CIS.equals(xVersion.getSystem().getId()) ) {
                updateVersionTypeFromCIS(xVersion, status, updateAll);
            } else {
                updateVersionType(xVersion, status, updateAll);
            }
        } catch (SQLException e) {
            debug("Can't perform SQL query.", e);
            throw new CSIRollbackException("Can't perform SQL query.", e);
        } catch (Exception e) {//don't care what kind of exception it will be, just rollback and notify
            debug("CreateManager unable to create Version through exception.", e);
            throw new CSIRollbackException("CreateManager unable to create Version through exception.", e);
        }
    }

    /**
     * Updates the versions service id, used for migration of the service.
     *
     * @param xVersion the JAXB version object
     */
    public void updateLegacyVersion(VersionType xVersion) throws CSIException {
        PreparedStatement ps=null;

        if(xVersion==null ||
                xVersion.getServiceId()==null || xVersion.getServiceId().length()==0 ||
                xVersion.getCustomerId()==null || xVersion.getCustomerId().length()==0 ||
                xVersion.getSiteId()==null || xVersion.getSiteId().length()==0 ||
                xVersion.getLegacyServiceId()==null || xVersion.getLegacyServiceId().length()==0
        ) {
            info("updateLegacyVersion requires ServiceId, CustomerId, SiteId and LegacyServiceId are set");
        } else {
            try {
                ps = m_connection.prepareStatement(
                        "UPDATE CVersion " +
                        "   SET ServiceHandle = ? " +
                        "WHERE " +
                        "       CustHandle =     ? " +
                        "   AND SiteHandle = ? " +
                        "   AND ServiceHandle =             ? "
                );
                ps.setString(1, xVersion.getServiceId());
                ps.setString(2, xVersion.getCustomerId());
                        ps.setString(3, xVersion.getSiteId());
                ps.setString(4, xVersion.getLegacyServiceId());
                ps.executeUpdate();

                String legacyUsid = xVersion.getSiteId() + xVersion.getLegacyServiceId();
                String currentUsid = xVersion.getSiteId() + xVersion.getServiceId();
                if(!legacyUsid.equals(currentUsid)) {
                    // update all CServiceElements where SiteId and LegacyServiceId used in USID field
                    ps = m_connection.prepareStatement(
                            "UPDATE CServiceElement " +
                            "   SET Usid = ? " +
                            "WHERE " +
                            "       Usid = ? "
                    );
    
                    
                    
                    debug(
                            "update all CServiceElements where SiteId and LegacyServiceId used in USID field, " +
                            "LegacyUSID = \"" + legacyUsid + "\", CurrentUSID = \"" +  currentUsid +"\""
                    );
    
                    ps.setString(1, currentUsid);
                    ps.setString(2, legacyUsid);
                    ps.executeUpdate();
                }
            } catch(SQLException e) {
                info(
                        "updateLegacyVersion("
                      + "ServiceId=\"" + xVersion.getServiceId()
                      + "\", CustomerId=\"" + xVersion.getCustomerId()
                      + "\", SiteId=\"" + xVersion.getSiteId()
                      + "\", LegacyServiceId=\"" + xVersion.getLegacyServiceId()
                      + "\") got an exception."
                      , e
                );
                throw new CSIRollbackException(
                        "updateLegacyVersion("
                      + "ServiceId=\"" + xVersion.getServiceId()
                      + "\", CustomerId=\"" + xVersion.getCustomerId()
                      + "\", SiteId=\"" + xVersion.getSiteId()
                      + "\", LegacyServiceId=\"" + xVersion.getLegacyServiceId()
                      + "\") got an exception."
                      , e
                );
            } finally {
                carefullyClose(null, ps); ps=null;
            }
        }
    }

    /**
     * Iterates through given (newly created) CVersionServiceElement objects
     * and find out all the old (existing on the DB) ones. Updates existing
     * with new information. If no existing found, then new ones have to
     * be created.
     *
     * @param xVersion the JAXB object of new Version
     * @throws SQLException if any error occurred during processing the query
     * @throws CSIException if any crtical error occurred during processing
     */
    private void updateVersionType(VersionType xVersion, String status, boolean updateAll) throws SQLException, CSIException, JAXBException {
        String debugVersionName = "[ " + xVersion.getCustomerId() + " / " + xVersion.getSiteId() + " / " + xVersion.getServiceId() + " ]";
        
        debug("updateVersionType(" +  debugVersionName + " \"" + status + "\", " + updateAll + ");");

        // Holds the newly created Version ID. Helps to identify if Version was
        // already created for one of ServiceElement set.
        long newVersionId = -1;

        // Iterate through given ServiceElements and find out all
        // old (existing on the DB) ones. Update existing with new information.
        // If no existing found, then new ones have to be created.
        for (Iterator i = xVersion.getServiceElement().iterator(); i.hasNext();) {

            ServiceElementType xServiceElement = (ServiceElementType)i.next();
            //(KY): First, try to find this ServiceElement in DB and update if it is changed.
            debug("about to call m_createManager.findAndUpdateServiceElement(" + debugVersionName + ", USID=\"" + xServiceElement.getId() + "\", updateAll=" + updateAll);
            ServiceElementType dbServiceElement = m_createManager.findServiceElement(xServiceElement);
            m_createManager.updateServiceElement(dbServiceElement, xServiceElement, updateAll);
            debug("m_createManager.findAndUpdateServiceElement(" + debugVersionName + ", USID=\"" + xServiceElement.getId() + "\", updateAll=" + updateAll);
            long oldVersionServiceElementId = -1;
            List oldVersionServiceElementLst = null;
            
            if (dbServiceElement!=null) {
                // Update existing CServiceElement
                debug(status + " Version of cServiceElement found. ID: " + dbServiceElement.getId());

                // Get the latest current VersionServiceElementId which is in the DB
                if(updateAll) {
                    oldVersionServiceElementLst = findAllVersionServiceElementId(dbServiceElement, status);
                } else {
                    oldVersionServiceElementId = findLatestVersionServiceElementId(dbServiceElement, status);
                }
            }

            if(updateAll && oldVersionServiceElementLst!=null && !oldVersionServiceElementLst.isEmpty()) {
                for(Iterator iter=oldVersionServiceElementLst.iterator();iter.hasNext();) {
                    Long l = (Long)(iter.next());
                    newVersionId = updateVersionServiceElement(
                                            xVersion, 
                                            xServiceElement, 
                                            status, 
                                            dbServiceElement, 
                                            l.longValue(), 
                                            newVersionId, 
                                            false
                                    );
                }
                oldVersionServiceElementLst.clear();
            } else {
                newVersionId = updateVersionServiceElement(
                                    xVersion, 
                                    xServiceElement, 
                                    status, 
                                    dbServiceElement, 
                                    oldVersionServiceElementId, 
                                    newVersionId, 
                                    false
                );
            }
        }
    }

    private long updateVersionServiceElement(
            VersionType xVersion, 
            ServiceElementType xServiceElement, 
            String status, 
            ServiceElementType dbServiceElement, 
            long oldVersionServiceElementId,
            long newVersionId,
            boolean updateAll
    ) throws CSIException, SQLException, JAXBException {
        if (dbServiceElement!=null && oldVersionServiceElementId!=-1) {
            debug("Latest cVersionServiceElement ID: " + oldVersionServiceElementId);

            // Check wether newly created VersionServiceElement has status delete.
            if (isInStatus(xServiceElement, STATUS_DELETE)) {
                // It has status delete, it means that UPDATE EXISTING order came to delete this ServiceElement.
                debug("Deleting cVersionServiceElement USID: " + xServiceElement.getId());

                updateStatus(oldVersionServiceElementId);
            } //Added by ajayg to resolve delete inconsistency issue
            else if (STATUS_DELETE.equals(xServiceElement.getType().getCategory()) && ORDER_TYPE_EXISTING.equals(xVersion.getOrdertype())) {
            	debug("Deleting cVersionServiceElement USID: " + xServiceElement.getId());
            	updateStatus(oldVersionServiceElementId);
            	
            }
            
            else {
                // This update is not about DELETE, but about update... Get current attributes.

                Map oldAttributes = getAttributesByStatus(dbServiceElement, STATUS_CURRENT);

                debug("Attributes size " + oldAttributes.size());

                if (oldAttributes.size() > 0) {
                    // New element is not about delete. Thus, have to update existing attributes.

                    // Handle attribute value and attribute charges for existing
                    updateAttributes(oldAttributes, xServiceElement, oldVersionServiceElementId);

                    // Handle service element charges for existing
                    updateServiceCharges(xServiceElement, oldVersionServiceElementId);
                    
                    oldAttributes.clear();
                }
            }
        } else {
            // This CServiceElement is not found in the DB.
            debug("No " + status + " versions of CServiceElement found. USID: " + xServiceElement.getId());

            //First, check if CVersion record was already created
            if (newVersionId == -1) {
                // Not yet. Create one
                newVersionId = m_createManager.createVersionType(xVersion);
            }

            //Will have to create new CServiceElement.
            m_createManager.createVersionServiceElement(newVersionId, xServiceElement, updateAll);
        }
        
        return newVersionId;
    }
    
    public void disconnectLatestServiceElement(
            String customerId, 
            String siteId, 
            String serviceId, 
            String usid, 
            Date date, 
            String user
    ) throws SQLException, CSIException {
        long currentVersionServiceElementId = -1;
        
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // Grab latest CURRENT VersionServiceElements from the DB.
            ps = m_connection.prepareStatement(
                            "SELECT   vse.VERSIONSERVICEELEMENTID "
                          + "  FROM   CVERSION v "
                          + "       , CSERVICEELEMENT se "
                          + "       , CVERSIONSERVICEELEMENT vse "
                          + "       , CVERSIONSERVICEELEMENTSTS vses "
                          + " WHERE  v.VERSIONID = vse.VERSIONID "
                          + "   AND  se.SERVICEELEMENTID = vse.SERVICEELEMENTID "
                          + "   AND  vse.VERSIONSERVICEELEMENTID = vses.VERSIONSERVICEELEMENTID "
                          + "   AND  vses.statustypecode IN ('InProgress', 'Current', 'Disconnect', 'Delete') "
                          + "   AND  se.USID = ? "
                          + "   AND  v.CUSTHANDLE = ? "
                          + "   AND  v.SITEHANDLE = ? "
                          + "   AND  v.SERVICEHANDLE = ? "
//                          + "   AND  vses.STATUSTYPECODE = ? "
                          + " ORDER BY vses.LUPDDATE DESC "
                    );

            ps.setString(1, usid);
            ps.setString(2, customerId);
            ps.setString(3, siteId);
            ps.setString(4, serviceId);
//            ps.setString(5, STATUS_CURRENT);

            rs = ps.executeQuery();
            // We need only first (most recent VSE)
            if (rs.next()) {
                currentVersionServiceElementId = rs.getLong(1);
            }
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }
        
        if(currentVersionServiceElementId!=-1) {
            updateStatus(currentVersionServiceElementId, STATUS_DISCONNECT, date, user);
        }
    }
    
    /**
     * Search for latest current VersionServiceElementId for given ServiceElement.
     * @param serviceElementId the ServiceElement ID.
     * @return either the ID of found current VersionServiceElement or -1 if not found.
     * @throws SQLException
     */
    private List findAllVersionServiceElementId(ServiceElementType dbServiceElement, String status) throws SQLException {
        List ret = new ArrayList();

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            debug("findAllVersionServiceElementId(" + dbServiceElement.getId() + ", \"" + status + "\");");
            
            // Grab latest CURRENT VersionServiceElements from the DB.
            ps = m_connection.prepareStatement(
                            "SELECT DISTINCT vse.versionServiceElementId                        " +
                            "  FROM cVersionServiceElement vse,cVersionServiceElementSts vses,  cServiceElement se " +
                            " WHERE se.USID = ?                                    " + //1
                            "   AND vses.statusTypeCode = ?                                     " + //2
                            "   AND vse.versionServiceElementId = vses.versionServiceElementId  " +
                            "   AND se.ServiceElementId = vse.ServiceElementId  "
                    );

            ps.setString(1, dbServiceElement.getId());
            ps.setString(2, status);

            rs = ps.executeQuery();
            // Iterate through results and put in Vector only IDs.
            while(rs.next()) {
                ret.add(new Long(rs.getLong(1)));
            }
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return ret;
    }

    /**
     * Search for latest current VersionServiceElementId for given ServiceElement.
     * @param serviceElementId the ServiceElement ID.
     * @return either the ID of found current VersionServiceElement or -1 if not found.
     * @throws SQLException
     */
    private long findLatestVersionServiceElementId(ServiceElementType dbServiceElement, String status) throws SQLException {
        long currentVersionServiceElementId = -1;

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // Grab latest CURRENT VersionServiceElements from the DB.
            ps = m_connection.prepareStatement(
                            "SELECT vse.versionServiceElementId                                 " +
                            "  FROM cVersionServiceElement vse,cVersionServiceElementSts vses,  cServiceElement se " +
                            " WHERE se.USID = ?                                    " + //1
                            "   AND vses.statusTypeCode = ?                                     " + //2
                            "   AND vse.versionServiceElementId = vses.versionServiceElementId  " +
                            "   AND se.ServiceElementId = vse.ServiceElementId  "                 +
                            " ORDER BY vse.lupdDate DESC                                        "
                    );

            ps.setString(1, dbServiceElement.getId());
            ps.setString(2, status);

            rs = ps.executeQuery();
            // Iterate through results and put in Vector only IDs.
            if (rs.next()) {
                currentVersionServiceElementId = rs.getLong(1);
            }
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return currentVersionServiceElementId;
    }

    private void updateStatus(long versionServiceElementId) throws SQLException {
        updateStatus(versionServiceElementId, STATUS_DELETE, m_currentDate, m_user);
    }
    
    /**
     * Updates the status of the version service element object.
     *
     * @param versionServiceElementId the version service element ID to update
     * @throws SQLException if any error occurred during processing the query
     */
    private void updateStatus(long versionServiceElementId, String status, Date currentDate, String user) throws SQLException {

        // Delete all old statuses
        PreparedStatement ps = null;
        try {
            ps = m_connection.prepareStatement(
                            "DELETE FROM cVersionServiceElementSts      " +
                            "      WHERE versionServiceElementId = ?    " +
                            "        AND statusTypeCode IN (?,?,?,?)    "
                    );

            ps.setLong(1, versionServiceElementId);
            ps.setString(2, STATUS_INPROGRESS);
            ps.setString(3, STATUS_CURRENT);
            ps.setString(4, STATUS_DISCONNECT);
            ps.setString(5, STATUS_DELETE);

            ps.executeUpdate();
        } finally {
            carefullyClose(null, ps); ps=null;
        }

        try {
            ps = m_connection.prepareStatement(
                    "INSERT INTO CVersionServiceElementSts (versionServiceElementStsId  " + //1
                    "                                       ,statusTypeCode             " + //2
                    "                                       ,statusDate                 " + //3
                    "                                       ,createDate                 " + //4
                    "                                       ,createdBy                  " + //5
                    "                                       ,lupdDate                   " + //6
                    "                                       ,lupdBy                     " + //7
                    "                                       ,versionServiceElementId    " + //8
                    "   ) VALUES (?,?,?,?,?,?,?,?)                                      "
//                                1 2 3 4 5 6 7 8
            );

            ps.setLong(1, getNextValue(CSIConstants.SEQ_VERSION_SERVICE_ELEMENT_STS)); //versionServiceElementStsId
            ps.setString(2, status); //statusTypeCode
            ps.setTimestamp(3, TransDate.getTimestamp(currentDate));//statusDate
            ps.setTimestamp(4, TransDate.getTimestamp(currentDate));//createDate
            ps.setString(5, user); //createdBy
            ps.setTimestamp(6, TransDate.getTimestamp(currentDate));//lupdDate
            ps.setString(7, user); //lupdBy
            ps.setLong(8, versionServiceElementId); //versionServiceElementId

            ps.executeUpdate();
        } finally {
            carefullyClose(null, ps); ps=null;
        }
    }


    /**
     * Retrieves the map of all the service attribute objects from database
     * that has the given ServiceElement ID and status.
     *
     * @param serviceElementId the ServiceElement ID.
     * @return the map of ServiceAttributeID (Long objects)
     * @throws SQLException if any SQL related exception happen
     */
    private Map getAttributesByStatus(ServiceElementType dbServiceElement, String status) throws SQLException {
        Map attributes = new HashMap();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            //NOTE (KY): The query should bring us all ServiceAttributes for
            //           all CURRENT VersionServiceElements sorted by UpdatedDate
            //           (of CVersionServiceElement table) in descending
            //           order. (The latest - first)
            ps = m_connection.prepareStatement(
                        "SELECT sa.ServiceAttributeId                                      " + //1
                        "     , sa.ServiceAttributeName                                    " + //2
                        "  FROM CVersionServiceElement vse                                 " +
                        "     , CVersionServiceElementSts vses                             " +
                        "     , CServiceAttribute sa                                       " +
                        "     , CServiceElement se                                       " +
                        "     , CServiceChangeItem sci                                     " +
                        " WHERE se.USID = ?                                   " +
                        "   AND vses.StatusTypeCode = ?                                    " +
                        "   AND vse.VersionServiceElementId = vses.VersionServiceElementId " +
                        "   AND sci.VersionServiceElementId = vse.VersionServiceElementId  " +
                        "   AND se.ServiceElementId = vse.ServiceElementId  " +
                        "   AND sa.ServiceAttributeId = sci.ServiceAttributeId             " +
                        " ORDER BY vse.LupdDate DESC                                       "
                    );

            ps.setString(1, dbServiceElement.getId());
            ps.setString(2, status);

            rs = ps.executeQuery();
            while (rs.next()) {
                // Do not override latest attributes with older ones.
                long serviceAttributeId = rs.getLong(1);
                String serviceAttributeName = rs.getString(2);
                if (!attributes.containsKey(serviceAttributeName)) {
                    attributes.put(serviceAttributeName, new Long(serviceAttributeId));

                    if (rs.isFirst()) {
                        debug("Obtained value of the " + serviceAttributeName + " attribute from previous version.");
                    }
                }
            }
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return attributes;
    }

    /**
     * Updates the attribute objects.
     *
     * @param oldAttributes              the map of old CServiceAttribute IDs
     * @param newElement                 the new version service element object
     * @param oldVersionServiceElementId the old <code>CVersionServiceElement</code> ID
     * @throws SQLException
     */
    private void updateAttributes(Map oldAttributes, ServiceElementType newElement, long oldVersionServiceElementId) throws SQLException {
        //Iterate through old Attributes.
        for (Iterator i = newElement.getServiceElementAttribute().iterator(); i.hasNext();) {
            ServiceElementAttributeType newAttribute = (ServiceElementAttributeType)i.next();
            if (newAttribute == null) {
                info("New attribute is null!!!");
                continue;
            }

            // Look through the sorted map and find the latest/first attribute
            Long oldAttributeId = (Long)oldAttributes.get(newAttribute.getName());
            if (oldAttributeId != null) {
                if (!isIgnoreAttribute(newElement.getServiceElementClass(), newAttribute.getName(), newAttribute.getValue())) {
                PreparedStatement ps = null;
                String updateStr =  "UPDATE CServiceAttribute       " +
                                            "   SET Value = ?               " + //1
                                            "     , lupdDate = ?            " + //2
                                            "     , lupdBy = ?              " + //3
                //Customer label additions
                                            "     , customerLabel = ?       " + //4
                                            "     , localCurrency = ?       " + //5
                                            " WHERE ServiceAttributeId = ?  " + //6
                                            "   AND Value <> ? " ; //7
                try {
                    ps = m_connection.prepareStatement(updateStr);
                    ps.setString(1, newAttribute.getValue());
                    ps.setTimestamp(2, TransDate.getTimestamp(m_currentDate));
                    ps.setString(3, m_user);
                    //added for customer label applications to service element attribute
                    ps.setString(4, newAttribute.getCustomerLabel());
                    ps.setBoolean(5, newAttribute.isLocalCurrency());
                    ps.setLong(6, oldAttributeId.longValue());
                    ps.setString(7, newAttribute.getValue());

                    ps. executeUpdate();
                } finally {
                    carefullyClose(null, ps); ps=null;
                }

                // update the attribute charges. GOLD will send the
                // entire attribute with the attribute charge when there is
                // a change on value or a change on charges.
                updateAttributeCharges(newAttribute, oldVersionServiceElementId, oldAttributeId.longValue());
            } else {
                    m_logger.info("ignore update attribute:" + newElement.getId() + ":" + newAttribute.getName() + ":" + newAttribute.getValue());
                }
            } else {
                // Add the attribute to the latest current order
                m_createManager.createServiceAttribute(newAttribute, oldVersionServiceElementId);
            }
        }
    }
    protected boolean isIgnoreAttribute(String serviceEelementType, String attrName, String attrValue) {
        boolean isIgnoreValue = false;
        if (CSIConstants.SECLASS_ACCESSCONNECTION.equals(serviceEelementType)) {
            if (CIS_AC_IGNORE_VALUE.get(attrName) != null && CIS_AC_IGNORE_VALUE.get(attrName).equals(attrValue)) {
                isIgnoreValue = true;
            }
        } else if (CSIConstants.SECLASS_BACKUP.equals(serviceEelementType)) {
            if (CIS_BACKUP_IGNORE_VALUE.get(attrName) != null && CIS_BACKUP_IGNORE_VALUE.get(attrName).equals(attrValue)) {
                isIgnoreValue = true;
            }
        } else if (CSIConstants.SECLASS_TRANSPORT.equals(serviceEelementType)) {
            if (CIS_TRANSPORT_IGNORE_VALUE.get(attrName) != null && CIS_TRANSPORT_IGNORE_VALUE.get(attrName).equals(attrValue)) {
                isIgnoreValue = true;
            }
        }
        return isIgnoreValue;
    }

    /**
     * Updates the Attribute Charge objects.
     *
     * @param newAttribute the JAXB object of new service attribute
     * @param oldVersionServiceElementId the old <code>CVersionServiceElement</code> ID
     * @param oldServiceAttributeId      the old <code>CServiceAttribute</code> ID
     * @throws SQLException
     */
    private void updateAttributeCharges(ServiceElementAttributeType newAttribute,
                                        long oldVersionServiceElementId,
                                        long oldServiceAttributeId) throws SQLException {
        updateCharges(
                newAttribute.getOnceOffCharge(),
                newAttribute.getRecurringCharge(),
                newAttribute.getUsageCharge(),
                oldVersionServiceElementId,
                oldServiceAttributeId
        );
    }

    /**
     * Updates the Service Charge objects.
     *
     * @param xServiceElement            the JAXB object of new service element
     * @param oldVersionServiceElementId the old <code>CVersionServiceElement</code> ID
s     * @throws SQLException
     */
    private void updateServiceCharges(ServiceElementType xServiceElement, long oldVersionServiceElementId) throws SQLException {
        updateCharges(
                xServiceElement.getOnceOffCharge(),
                xServiceElement.getRecurringCharge(),
                xServiceElement.getUsageCharge(),
                oldVersionServiceElementId,
                -1
        );
    }

    /**
     * Updates the service charge objects.
     *
     * @param newOnceOffCharge           the JAXB OnceOffCharge Change Item object
     * @param newRecurringCharge         the JAXB RecurringCharge Change Item object
     * @param newUsageCharge             the JAXB UsageCharge Change Item object
     * @param oldVersionServiceElementId the old <code>CVersionServiceElement</code> ID
     * @param oldServiceAttributeId      the old <code>CServiceAttribute</code> ID
     * @throws SQLException
     */
    private void updateCharges(OnceOffChargeType newOnceOffCharge,
                               RecurringChargeType newRecurringCharge,
                               UsageChargeType newUsageCharge,
                               long oldVersionServiceElementId,
                               long oldServiceAttributeId) throws SQLException {

        // Process not empty charges only
        if (newOnceOffCharge != null) {
            processOnceOffCharge(newOnceOffCharge, oldVersionServiceElementId, oldServiceAttributeId);
        }
        if (newRecurringCharge != null) {
            processRecurringCharge(newRecurringCharge, oldVersionServiceElementId, oldServiceAttributeId);
        }
        if (newUsageCharge != null) {
            processUsageCharge(newUsageCharge, oldVersionServiceElementId, oldServiceAttributeId);
        }
    }

    /**
     * Processes the Usage Charge object.
     *
     * @param xUsageCharge               the JAXB UsageCharge Change Item object
     * @param oldVersionServiceElementId the old <code>CVersionServiceElement</code> ID
     * @param oldServiceAttributeId      the old <code>CServiceAttribute</code> ID
     * @throws SQLException
     */
    private void processUsageCharge(UsageChargeType xUsageCharge,
                                    long oldVersionServiceElementId,
                                    long oldServiceAttributeId) throws SQLException {
        PreparedStatement ps = null;
        try {
            //First try to update the record
            if (oldServiceAttributeId == -1) {
                //Update the UsageCharge at the VersionServiceElement level
                ps = m_connection.prepareStatement(
                        "UPDATE CUsageCharge                                " +
                        "   SET ChgCatId = ?                                " + //1
                        "     , LupdDate = ?                                " + //2
                        "     , LupdBy = ?                                  " + //3
                        " WHERE UsageChargeId IN (                          " +
                        "       SELECT uc.UsageChargeId                     " +
                        "         FROM CUsageCharge uc                      " +
                        "            , CChargeChangeItem cci                " +
                        "        WHERE cci.VersionServiceElementId = ?      " + //4
                        "          AND uc.UsageChargeId = cci.UsageChargeId " +
                        "       )                                           "
                );
                ps.setLong(4, oldVersionServiceElementId);
            } else {
                //Update the UsageCharge at the ServiceAttribute level
                ps = m_connection.prepareStatement(
                        "UPDATE CUsageCharge                                " +
                        "   SET ChgCatId = ?                                " + //1
                        "     , LupdDate = ?                                " + //2
                        "     , LupdBy = ?                                  " + //3
                        " WHERE UsageChargeId IN (                          " +
                        "       SELECT uc.UsageChargeId                     " +
                        "         FROM CUsageCharge uc                      " +
                        "            , CChargeChangeItem cci                " +
                        "        WHERE cci.ServiceAttributeId = ?           " + //4
                        "          AND uc.UsageChargeId = cci.UsageChargeId " +
                        "       )                                           "
                );
                ps.setLong(4, oldServiceAttributeId);
            }

            //Set common fields
            ps.setString(1, xUsageCharge.getChargeCategoryId());
            ps.setTimestamp(2, TransDate.getTimestamp(m_currentDate));
            ps.setString(3, m_user);

            if (ps.executeUpdate()==0) {
                //No UsageCharge exist, so create new
                m_createManager.createUsageChargeChangeItem(
                        oldVersionServiceElementId,
                        oldServiceAttributeId,
                        xUsageCharge
                );
            }
        } finally {
            carefullyClose(null, ps); ps=null;
        }
    }

    /**
     * Processes the Recurring Charge object.
     *
     * @param xRecurringCharge           the JAXB RecurringCharge Change Item object
     * @param oldVersionServiceElementId the old <code>CVersionServiceElement</code> ID
     * @param oldServiceAttributeId      the old <code>CServiceAttribute</code> ID
     * @throws SQLException
     */
    private void processRecurringCharge(RecurringChargeType xRecurringCharge,
                                        long oldVersionServiceElementId,
                                        long oldServiceAttributeId) throws SQLException {
        PreparedStatement ps = null;
        try {
            //First try to update the record
            if (oldServiceAttributeId==-1) {
                //Update the RecurringCharge at the VersionServiceElement level
                ps = m_connection.prepareStatement(
                        "UPDATE CRecurringCharge                                    " +
                        "   SET Amount = ?                                          " + //1
                        "     , DiscCode = ?                                        " + //2
                        "     , LupdDate = ?                                        " + //3
                        "     , LupdBy = ?                                          " + //4
                        " WHERE RecurringChargeId IN (                              " +
                        "       SELECT rc.RecurringChargeId                         " +
                        "         FROM CRecurringCharge rc                          " +
                        "            , CChargeChangeItem cci                        " +
                        "        WHERE cci.VersionServiceElementId = ?              " + //5
                        "          AND rc.RecurringChargeId = cci.RecurringChargeId " +
                        "       )                                                   "
                );
                ps.setLong(5, oldVersionServiceElementId);
            } else {
                //Update the RecurringCharge at the ServiceAttribute level
                ps = m_connection.prepareStatement(
                        "UPDATE CRecurringCharge                                    " +
                        "   SET Amount = ?                                          " + //1
                        "     , DiscCode = ?                                        " + //2
                        "     , LupdDate = ?                                        " + //3
                        "     , LupdBy = ?                                          " + //4
                        " WHERE RecurringChargeId IN (                              " +
                        "       SELECT rc.RecurringChargeId                         " +
                        "         FROM CRecurringCharge rc                          " +
                        "            , CChargeChangeItem cci                        " +
                        "        WHERE cci.ServiceAttributeId = ?                   " + //5
                        "          AND rc.RecurringChargeId = cci.RecurringChargeId " +
                        "       )"
                );
                ps.setLong(5, oldServiceAttributeId);
            }

            //Set common fields
            ps.setDouble(1, xRecurringCharge.getAmount());
            ps.setString(2, xRecurringCharge.getDiscountCode());
            ps.setTimestamp(3, TransDate.getTimestamp(m_currentDate));
            ps.setString(4, m_user);

            if (ps.executeUpdate()==0) {
                //No RecurringCharge exist, so create new
                m_createManager.createRecurringChargeChangeItem(
                        oldVersionServiceElementId,
                        oldServiceAttributeId,
                        xRecurringCharge
                );
            }
        } finally {
            carefullyClose(null, ps); ps=null;
        }
    }

    /**
     * Processes the Once Off Charge object.
     *
     * @param xOnceOffCharge             the JAXB OnceOffCharge Change Item object
     * @param oldVersionServiceElementId the old <code>CVersionServiceElement</code> ID
     * @param oldServiceAttributeId      the old <code>CServiceAttribute</code> ID
     * @throws SQLException
     */
    private void processOnceOffCharge(OnceOffChargeType xOnceOffCharge,
                                      long oldVersionServiceElementId,
                                      long oldServiceAttributeId) throws SQLException {
        PreparedStatement ps = null;
        try {
            //First try to update the record
            if (oldServiceAttributeId==-1) {
                //Update the OnceOffCharge at the VersionServiceElement level
                ps = m_connection.prepareStatement(
                        "UPDATE COnceOffCharge                                      " +
                        "   SET Amount = ?                                          " + //1
                        "     , DiscCode = ?                                        " + //2
                        "     , LupdDate = ?                                        " + //3
                        "     , LupdBy = ?                                          " + //4
                        " WHERE OnceOffChargeId IN (                                " +
                        "       SELECT ooc.OnceOffChargeId                          " +
                        "         FROM COnceOffCharge ooc                           " +
                        "            , CChargeChangeItem cci                        " +
                        "        WHERE cci.VersionServiceElementId = ?              " + //5
                        "          AND ooc.OnceOffChargeId = cci.OnceOffChargeId    " +
                        "       )                                                   "
                );
                ps.setLong(5, oldVersionServiceElementId);
            } else {
                //Update the OnceOffCharge at the ServiceAttribute level
                ps = m_connection.prepareStatement(
                        "UPDATE COnceOffCharge                                      " +
                        "   SET Amount = ?                                          " + //1
                        "     , DiscCode = ?                                        " + //2
                        "     , LupdDate = ?                                        " + //3
                        "     , LupdBy = ?                                          " + //4
                        " WHERE OnceOffChargeId IN (                                " +
                        "       SELECT ooc.OnceOffChargeId                          " +
                        "         FROM COnceOffCharge ooc                           " +
                        "            , CChargeChangeItem cci                        " +
                        "        WHERE cci.ServiceAttributeId = ?                   " + //5
                        "          AND ooc.OnceOffChargeId = cci.OnceOffChargeId    " +
                        "       )                                                   "
                );
                ps.setLong(5, oldServiceAttributeId);
            }

            //Set common fields
            ps.setDouble(1, xOnceOffCharge.getAmount());
            ps.setString(2, xOnceOffCharge.getDiscountCode());
            ps.setTimestamp(3, TransDate.getTimestamp(m_currentDate));
            ps.setString(4, m_user);

            if (ps.executeUpdate()==0) {
                //No OnceOffCharge exist, so create new
                m_createManager.createOnceOffChargeChangeItem(
                        oldVersionServiceElementId,
                        oldServiceAttributeId,
                        xOnceOffCharge
                );
            }
        } finally {
            carefullyClose(null, ps); ps=null;
        }
    }

    /**
     * Updated Customer Handle
     *
     * @param xCustomerHandleChange the changes from/to customer handle
     * @throws CSIException throws if any problem.
     */
    public void updateCustomerHandle(CustomerHandleChangeType xCustomerHandleChange) throws CSIException {
        // updates all customer handles in CV ERSION.SITEHANDLE & CSERVICEELEMENT.USID

        /*
             1. update CVERSION set CUSTHANDLE = ? where CUSTHANDLE = ?
            2.
                a) select USID from CSERVICEELEMENT where USID like 'CSP_' || OldSiteID || ': %';
                b) extract product ids
                c) update CSERVICEELEMENT set USID = 'CSP_' || NewSiteID || ': ' || ProductID where USID like 'CSP_' || OldSiteID || ': ' || ProductID;
        */
        PreparedStatement ps=null, psupdate=null;
        ResultSet rs=null;

        if(     xCustomerHandleChange==null ||
                xCustomerHandleChange.getOldId()==null ||
                xCustomerHandleChange.getUpdatedID()==null ||
                xCustomerHandleChange.getOldId().length()==0 ||
                xCustomerHandleChange.getUpdatedID().length()==0 ||
                xCustomerHandleChange.getOldId().equals(xCustomerHandleChange.getUpdatedID())) {
            info("updateCustomerHandle skipped because nothing to update.");
            return;
        }
        try {
            ps = m_connection.prepareStatement(
                    "select " +
                    "   SERVICEELEMENTID, SUBSTR(USID, ?) " +
                    "from " +
                    "   CSERVICEELEMENT " +
                    "where " +
                    "   USID like ? "
            );
            ps.setInt(1, xCustomerHandleChange.getOldId().length()+7);
            ps.setString(2, "CSP_" + xCustomerHandleChange.getOldId() +  ": %");
            debug(
                "select SERVICEELEMENTID, SUBSTR(USID, '" +
                (xCustomerHandleChange.getOldId().length()+7) +
                "') from CSERVICEELEMENT where USID like '" +
                ("CSP_" + xCustomerHandleChange.getOldId() +  ": %") +
                "'"
            );
            rs = ps.executeQuery();
            if(rs!=null) {
                while(rs.next()) {
                    long id = rs.getLong(1);
                    String service = rs.getString(2);
                    if(id!=0 && service!=null && service.length()>0) {
                        // update this record
                        if(psupdate==null) {
                            // create prepared statement
                            psupdate = m_connection.prepareStatement(
                                    "update " +
                                    "   CSERVICEELEMENT " +
                                    "set " +
                                    "   USID = ? " +
                                    "where " +
                                    "   SERVICEELEMENTID = ? "
                            );
                            ps.clearBatch();
                        }
                        String usid = "CSP_" + xCustomerHandleChange.getUpdatedID() + ": " + service;
                        psupdate.setString(1, usid);
                        psupdate.setLong(2, id);
                        psupdate.addBatch();
                        debug("update CSERVICEELEMENT set USID = '" + usid + "' where SERVICEELEMENTID = " + id);
                    }
                }
                if(psupdate!=null) {
                    psupdate.executeBatch();
                    carefullyClose(null, psupdate); psupdate=null;
                }
                carefullyClose(rs, ps); psupdate=null;
            }
            ps = m_connection.prepareStatement(
                    "update " +
                    "   CVERSION " +
                    "set " +
                    "   CUSTHANDLE = ? " +
                    "where " +
                    "   CUSTHANDLE = ? "
            );
            ps.setString(1, xCustomerHandleChange.getUpdatedID());
            ps.setString(2, xCustomerHandleChange.getOldId());
            debug("update CVERSION set CUSTHANDLE  = '" + xCustomerHandleChange.getUpdatedID() + "' where CUSTHANDLE = '" + xCustomerHandleChange.getOldId() + "'");
            ps.executeUpdate();
            carefullyClose(null, ps); ps=null;
        } catch (SQLException e) {
            throw new CSIRollbackException("Can't perform SQL query.", e);
        } catch (Exception e) {//don't care what kind of exception it will be, just rollback and notify
            throw new CSIRollbackException("CreateManager unable to Version or ServiceElement through exception.", e);
        } finally {
            carefullyClose(rs, ps);
            carefullyClose(null, psupdate);
        }
    }

    /**
     * Updates Site Handle
     *
     * @param xSiteHandleChange
     * @throws CSIException throws for SQL related and data problems.
     */
    public void updateSiteHandle(SiteHandleChangeType xSiteHandleChange) throws CSIException {
        PreparedStatement ps = null, psupdate = null;
        ResultSet rs = null;

        if(     xSiteHandleChange==null ||
                xSiteHandleChange.getOldId()==null ||
                xSiteHandleChange.getUpdatedID()==null ||
                xSiteHandleChange.getOldId().length()==0 ||
                 xSiteHandleChange.getUpdatedID().length()==0 
                 //||xSiteHandleChange.getOldId().equals(xSiteHandleChange.getUpdatedID())//updating coresiteid and addressid
                 ) {
            info("updateSiteHandle skipped because nothing to update.");
            return;
        }

        try {
            ps = m_connection.prepareStatement(
                    "select " +
                    "   SERVICEELEMENTID, SUBSTR(USID, ?) " +
                    "from " +
                    "   CSERVICEELEMENT " +
                    "where " +
                    "   SERVICEELEMENTCLASS in ('ServiceOptions') " +
                    " and " +
                    "   USID like ? "
            );
            ps.setInt(1, xSiteHandleChange.getOldId().length()+1);
            ps.setString(2, xSiteHandleChange.getOldId() + "%");
            debug(
                "select SERVICEELEMENTID, SUBSTR(USID, '" +
                (xSiteHandleChange.getOldId().length()+1) +
                "') from CSERVICEELEMENT where USID like '" +
                (xSiteHandleChange.getOldId() + "%") +
                "'");
            rs = ps.executeQuery();
            if(rs!=null) {
                debug("select returned USID rows, parsing them");
                while(rs.next()) {
                    long id = rs.getLong(1);
                    String service = rs.getString(2);
                    if(id!=0 && service!=null && service.length()>0) {
                        // update this record
                        if(psupdate==null) {
                            // create prepared statement
                            psupdate = m_connection.prepareStatement(
                                    "update " +
                                    "   CSERVICEELEMENT " +
                                    "set " +
                                    "   USID = ? " +
                                    "where " +
                                    "   SERVICEELEMENTID = ? "
                            );
                            ps.clearBatch();
                        }
                        String usid = xSiteHandleChange.getUpdatedID() + service;
                        psupdate.setString(1, usid);
                        psupdate.setLong(2, id);
                        psupdate.addBatch();
                        debug("update CSERVICEELEMENT set USID = '" + usid + "' where SERVICEELEMENTID = " + id);
                    }
                }
                if(psupdate!=null) {
                    psupdate.executeBatch();
                    carefullyClose(null, psupdate); psupdate=null;
                }
                carefullyClose(rs, ps); rs=null; ps=null;
            }
            ps = m_connection.prepareStatement(
                    "update " +
                    "   CVERSION " +
                    "set " +
                    "   SITEHANDLE = ?, " +
                    "   CoreSiteID = ?, " +
                    "   AddressID = ? " +
                    "where " +
                    "   SITEHANDLE = ? "
            );
            ps.setString(1, xSiteHandleChange.getUpdatedID());
            ps.setString(2, xSiteHandleChange.getUpdatedCoreID());
            ps.setString(3, xSiteHandleChange.getUpdatedAddressID());
            ps.setString(4, xSiteHandleChange.getOldId());
            debug(
                "update CVERSION set SITEHANDLE  = '" +
                xSiteHandleChange.getUpdatedID() +
                "', CoreSiteID = '" + xSiteHandleChange.getUpdatedCoreID() +"', AddressID = '" + xSiteHandleChange.getUpdatedAddressID() + "' where SITEHANDLE = '" +
                xSiteHandleChange.getOldId() +
                "'"
            );
            ps.executeUpdate();
            carefullyClose(null, ps); ps=null;
        } catch (SQLException e) {
            throw new CSIRollbackException("Can't perform SQL query.", e);
        } catch (Exception e) {//don't care what kind of exception it will be, just rollback and notify
            throw new CSIRollbackException("CreateManager unable to Version or ServiceElement through exception.", e);
        } finally {
            carefullyClose(rs, ps);
            carefullyClose(null, psupdate);
        }
    }

    /**
     * Creates new version for existent site (CSP global impl)
     * CR#6673
     *
     * @param xVersion Version - site level version
     * @param se ServiceElementTypeImpl - global CSP options
     * @throws CSIException throws for SQL related and data problems.
     */
    public void findAndUpdateOrCreateCspOptions(VersionType xVersion, ServiceElementTypeImpl se, boolean updateAll) throws CSIException, SQLException, JAXBException {
        List se_cur = null, sea_cur = null, sea_gl = null;
        se_cur = xVersion.getServiceElement();

        boolean hasSO = false;
        for (Iterator iter = se_cur.iterator(); iter.hasNext();) {
            ServiceElement se_t = (ServiceElement) iter.next();
            ObjectFactory objectFactory = new ObjectFactory();
            if (CSIConstants.SERVICE_ELEMENT_SO.equals(se_t.getServiceElementClass())) {
                // Have Service Options element. Check for Attributes.
                if (!hasSO) { hasSO = true; }
                sea_cur = se_t.getServiceElementAttribute();
                sea_gl = se.getServiceElementAttribute();
                ServiceElementAttribute rs = null;
                try {
                    rs = objectFactory.createServiceElementAttribute();
                } catch (JAXBException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

                // Iterate Global attributes
                for (Iterator gsea_iter = sea_gl.iterator(); gsea_iter.hasNext();) {
                    ServiceElementAttributeType gsea_t = (ServiceElementAttributeType) gsea_iter.next();

                    if (CSIConstants.GLOBAL_CSP_FM.equals(gsea_t.getName())) {
                        // Global CSP Fault Management

                        rs = modifySEA(sea_cur, gsea_t, CSIConstants.SITE_CSP_FM);
                        if (rs != null) {
                            se_t.getServiceElementAttribute().add(rs);
                        }
                        se_t.setCreationDate(new TransDate(System.currentTimeMillis()));

                    } else if (CSIConstants.GLOBAL_CSP_SM.equals(gsea_t.getName())) {
                        // Global CSP Service Management

                        rs = modifySEA(sea_cur, gsea_t, CSIConstants.SITE_CSP_SM);
                        if (rs != null) {
                            se_t.getServiceElementAttribute().add(rs);
                        }
                        se_t.setCreationDate(new TransDate(System.currentTimeMillis()));

                    } else if (CSIConstants.GLOBAL_CSP_PM.equals(gsea_t.getName())) {
                        // Global CSP Project Management

                        rs = modifySEA(sea_cur, gsea_t, CSIConstants.SITE_CSP_PM);
                        if (rs != null) {
                            se_t.getServiceElementAttribute().add(rs);
                        }
                        se_t.setCreationDate(new TransDate(System.currentTimeMillis()));

                    }
                }
            }
        }

        if (!hasSO) {
            // No Service Options in existent version. Have to add.
            ObjectFactory objectFactory = new ObjectFactory();
            ServiceElement se_new = null;
            ServiceElementAttribute sea_new = null;
            try {
                se_new = objectFactory.createServiceElement();
                sea_new = objectFactory.createServiceElementAttribute();
            } catch (JAXBException e) {
                e.printStackTrace();
            }
            se_new.setName("Service Level");
            se_new.setDescription("Service Level");
            se_new.setId("SO::" + xVersion.getCustomerId() + "::" + xVersion.getSiteId() + "::" + xVersion.getServiceId());
            se_new.setCreationDate(new TransDate(System.currentTimeMillis()));
            se_new.setServiceElementClass(CSIConstants.SERVICE_ELEMENT_SO);

            // Add attributes
            for (Iterator iter = se.getServiceElementAttribute().iterator(); iter.hasNext();) {
                ServiceElementAttributeType gsea_t = (ServiceElementAttributeType) iter.next();
                if (CSIConstants.GLOBAL_CSP_FM.equals(gsea_t.getName())) {
                    // Global CSP Fault Management
                    sea_new.setName(CSIConstants.SITE_CSP_FM);
                    sea_new.setValue(gsea_t.getValue());
                    sea_new.setStatus(gsea_t.getStatus());
                    se_new.getServiceElementAttribute().add(sea_new);
                } else if (CSIConstants.GLOBAL_CSP_SM.equals(gsea_t.getName())) {
                    // Global CSP Service Management
                    sea_new.setName(CSIConstants.SITE_CSP_SM);
                    sea_new.setValue(gsea_t.getValue());
                    sea_new.setStatus(gsea_t.getStatus());
                    se_new.getServiceElementAttribute().add(sea_new);
                } else if (CSIConstants.GLOBAL_CSP_PM.equals(gsea_t.getName())) {
                    // Global CSP Project Management
                    sea_new.setName(CSIConstants.SITE_CSP_PM);
                    sea_new.setValue(gsea_t.getValue());
                    sea_new.setStatus(gsea_t.getStatus());
                    se_new.getServiceElementAttribute().add(sea_new);
                }
            }
            
            xVersion.getServiceElement().add(se_new);
        }

        String orderId  = null;
        int split = xVersion.getOrderid().indexOf('$');
        if (split == -1) {
            orderId = xVersion.getOrderid();
        } else {
            orderId = xVersion.getOrderid().substring(0, split - 1);
        }
        xVersion.setOrderid(orderId + "$" + getNextValue(CSIConstants.SEQ_VERSION));
        xVersion.setOrdertype("Change");
        m_createManager.createVersion(xVersion, updateAll);
    }

    private ServiceElementAttribute modifySEA(List sea, ServiceElementAttributeType gsea, String name) throws JAXBException {
        boolean isFound = false;
        ServiceElementAttribute ret = null;
        ret = m_objectFactory.createServiceElementAttribute();

        for (Iterator iter = sea.iterator(); iter.hasNext();) {
            ServiceElementAttribute sea_t = (ServiceElementAttribute) iter.next();
            if (name.equals(sea_t.getName())) {
                isFound = true;
                sea_t.setValue(gsea.getValue());
                return null;
            }
        }

        // No such attribute in site level order. Have to add.
        if (!isFound) {
            ret.setName(name);
            ret.setValue(gsea.getValue());
            ret.setStatus(gsea.getStatus());
            //csutomer label for csp needs check
            ret.setCustomerLabel(gsea.getCustomerLabel());
            ret.setLocalCurrency(gsea.isLocalCurrency());
        }

        return ret;
    }

    /**
     * Update versions with new product's color (Blue->Purple; CSP global impl)
     * CR#6673
     *
     * @param cust String - Customer
     * @param prod String - Product ("ATM", "Frame Relay" etc)
     * @throws CSIException throws for SQL related and data problems.
     */
    public void findAndUpdateColor( String cust, String prod) throws CSIException {
        PreparedStatement ps = null;
        String oldprod = null, newprod = null;

        // Set up update clause
        if (CSIConstants.CSP_PURPLE_ATM.equals(prod)) {
            oldprod = CSIConstants.CSP_COLOR_BLUE_ATM;
            newprod = CSIConstants.CSP_COLOR_PURPLE_ATM;
        } else if (CSIConstants.CSP_PURPLE_FR.equals(prod)) {
            oldprod = CSIConstants.CSP_COLOR_BLUE_FRAME_RELAY;
            newprod = CSIConstants.CSP_COLOR_PURPLE_FRAME_RELAY;
        } else if (CSIConstants.CSP_PURPLE_LAN_ACCESS.equals(prod)) {
            oldprod = CSIConstants.CSP_COLOR_BLUE_LAN_ACCESS;
            newprod = CSIConstants.CSP_COLOR_PURPLE_LAN_ACCESS;
        }

        if (oldprod != null && newprod != null) {
            try {
                ps = m_connection.prepareStatement(
                        "update " +
                        "   CVERSION " +
                        "set " +
                        "   SERVICEHANDLE = ? " +
                        "where " +
                        "   SERVICEHANDLE = ? " +
                        "   AND CUSTHANDLE = ? "
                );
                ps.setString(1, newprod);
                ps.setString(2, oldprod);
                ps.setString(3, cust);
                debug(
                        "update CVERSION set SERVICEHANDLE  = '" +
                        newprod +
                        "' where SERVICEHANDLE = '" +
                        oldprod +
                        "' AND CUSTHANDLE = '" +
                        cust +
                        "'"
                );
                ps.executeUpdate();
            } catch (SQLException e) {
                info("Got SQLException", e);
                throw new CSIException("Got SQLException", e);
            }
        } else {
            info("Migration of color: Nothing to mirgate - product is not ATM/FR/LANAS");
        }
   }

    public boolean copyPrintInformation(ServiceElementType gldSE, ServiceElementType cisSE) {
        if(gldSE==null 
                || cisSE==null 
                || gldSE.getServiceElementAttribute()==null 
                || cisSE.getServiceElementAttribute()==null
                || gldSE.getServiceElementAttribute().size() == 0
                || cisSE.getServiceElementAttribute().size() == 0
        ) return false;
    
        boolean toUpdate = false;
        for(Iterator iter = gldSE.getServiceElementAttribute().iterator(); iter.hasNext(); ) {
            ServiceElementAttributeType sea = (ServiceElementAttributeType)iter.next();
            ServiceElementAttributeType sea2 = null;
            if(sea!=null && sea.getName()!=null) {
                for(Iterator iter2 = cisSE.getServiceElementAttribute().iterator();iter2.hasNext(); ) {
                    ServiceElementAttributeType tmp = (ServiceElementAttributeType)iter2.next();
                    if(tmp!=null && tmp.getName()!=null && tmp.getName().equals(sea.getName())) {
                        sea2 = tmp;
                        break;
                    }
                }
            }
            if(sea2!=null) {
                if(sea.getOnceOffCharge()!=null) {
                    sea.setOnceOffCharge(sea2.getOnceOffCharge());
                    toUpdate = true;
                }
                if(sea.getRecurringCharge()!=null) {
                    sea.setRecurringCharge(sea2.getRecurringCharge());
                    toUpdate = true;
                }
                if(sea.getUsageCharge()!=null) {
                    sea.setUsageCharge(sea2.getUsageCharge());
                    toUpdate = true;
                }
            }
        }
        return toUpdate;
    }

    /**
     * This method releases main logic of Dragon migration
     * @param oldUsid
     * @param newUsid
     * @param serviceOptions TODO
     * @param oldSiteList TODO
     * @param dragonLoger
     * @param goldCOnnection
     * @return true if migration was finished successfully, false - if any errors
     * @throws CSIException
     */
    public boolean migrateServiceElementsForDragon (String oldUsid, String newUsid, 
            HashMap serviceOptions, HashMap oldSiteSet, Logger dragonLoger, Connection gold_connection) throws CSIException {
        QueryManager queryMananger = new QueryManager(m_connection);

        if(oldUsid==null || newUsid==null || newUsid.equals( oldUsid)) {
            dragonLoger.error("One of USIDs is null OR both are equal: oldUsid=" + oldUsid + " newUsid=" + newUsid);
            return false;
        }
        
        //get OLD component and check its existence
        VersionType oldVersion = queryMananger.getVersionServiceElementByUSID(oldUsid);
        if (oldVersion == null) {
            dragonLoger.error("Old component (USID=" + oldUsid + ") not in CSI.");
            return false;
        }
        
        //get NEW component and check its existence
        VersionType newVersion = queryMananger.getVersionServiceElementByUSID( newUsid);
        if (newVersion == null) {
            dragonLoger.error("New component (USID=" + newUsid + ") not in CSI.");
            return false;
        }

        String oldSEClass = getServiceElementClass( oldVersion);
        String newSEClass = getServiceElementClass( newVersion);

        if( !StringUtils.equals( oldSEClass, newSEClass)) {
            dragonLoger.error( "ComponentClass discrepency between old and new components: OLD: "
                + oldSEClass + " NEW: " + newSEClass);
            return false;
        }
        
        m_user = newVersion.getSystem().getUser().getName();
        
        //Check Customer code
        if (!StringUtils.equals( oldVersion.getCustomerId(), newVersion.getCustomerId())) {
            dragonLoger.error("CustCode Discrepancy between old and new components:" 
                    + " OLD: " + oldVersion.getCustomerId()
                    + " NEW: " + newVersion.getCustomerId());
            return false;
        }

        //check existence of associated orders and their WF statuses
        if (!areAllAssociatedOrdersExistedAndClosed(
                oldVersion, newVersion, oldUsid, newUsid, dragonLoger, gold_connection, queryMananger)) {
            return false;
        }
        
        //ALL CHECKS ARE PASSED AND WE SHOULD DO MIGRATION:
        //disconnect ALL CURRENT and INPROGRESS cvse related to OLD component
        //and copy pricing info


        //do copying of pricing info
        boolean pricingInfoWasMigrated =
            copyPricingInfoForDragon(
                    (ServiceElementType)oldVersion.getServiceElement().get(0), 
                    (ServiceElementType)newVersion.getServiceElement().get(0), 
                    dragonLoger);

        if (pricingInfoWasMigrated) {
            doDragonTransaction(oldUsid, newUsid, dragonLoger, queryMananger, newVersion);
        }
///"SO::OrganinizationID::SiteCode::ProductID
        String oldSOId="SO::"+oldVersion.getCustomerId()+"::"+
                              oldVersion.getSiteId()+"::"+
                              oldVersion.getServiceId();
        
        String newSOId="SO::"+newVersion.getCustomerId()+"::"+
                              newVersion.getSiteId()+"::"+
                              newVersion.getServiceId();
        
        serviceOptions.put(oldSOId,newSOId);
        oldSiteSet.put( oldVersion.getSiteId(),
            new String[]{ oldVersion.getCustomerId(), oldVersion.getServiceId()});
        
        //Check result
        dragonLoger.info("Old Component (USID=" + oldUsid 
                + ") was " + (pricingInfoWasMigrated ? "" : "NOT") 
                + " migrated to new component (USID=" + newUsid + ")");

        return pricingInfoWasMigrated;
    }

    protected String getServiceElementClass( VersionType version) {
        List sElements = version.getServiceElement();
        
        if( sElements==null || sElements.isEmpty()) {
            return null;
        }

        ServiceElementType set = (ServiceElementType)sElements.get( 0);

        return set.getServiceElementClass();
    }
    
    /**
     * Does check existence of associated orders and their WF statuses 
     * Note 1: check only orders that are accociated to Old Service Element.
     * Note 2: this logic is used for Dragon Migration 
     *         and it was extracted toseparate method just for increase readability 
     *         of migrateServiceElementsForDragon method
     */
    private boolean areAllAssociatedOrdersExistedAndClosed(VersionType oldVersion, VersionType newVersion, 
            String oldUsid, String newUsid, 
            Logger dragonLoger, 
            Connection gold_connection, QueryManager queryMananger) throws CSIException {
        
        //Check of status and check of Closed orders in case of InProgress status
            
        //get Order(s) associated to InProgress versions of ServiceElement
        List orderList=queryMananger.getOrderHandlesByStatus( oldUsid, STATUS_INPROGRESS);

        if( orderList.isEmpty()) {
            //there are no orders associated to InProgress versions of OLD service element
            if (dragonLoger.isDebugEnabled()) dragonLoger.debug( "There are no versions in the InProgress stage of the old component (USID=" + oldUsid + ").");
        } else {
            if( orderList.contains("null") || orderList.contains("NULL")
                    || orderList.contains( null)) {
                //we skip migration in case of "no orderhandle" 
                dragonLoger.error( "There is InProgress versions (old component USID=" + oldUsid + ") without associated orders.");
                return false;
            }

            //check existence of Enclosed related orders
            GOLDManager gm = new GOLDManager (m_connection, gold_connection); 
            
            List existedOrders = gm.getExistedOrdersIds( orderList);
            if( existedOrders.size() != orderList.size()) {
                List diff = new ArrayList( orderList);
                diff.removeAll( existedOrders);

                dragonLoger.error( "There is InProgress versions (old component USID=" +
                    oldUsid + ") with wrong order IDs: " + diff);
                return false;
            }

            Map notClosedOrders = gm.getNotClosedOrders( orderList);
            if ((notClosedOrders!=null) && (!notClosedOrders.isEmpty())) {
                //skip migration - There is Not Closed order 
                StringBuffer log = new StringBuffer( "Old component (USID=" + oldUsid 
                    + ") has versions in InProgress status and there are associated and not Closed order(s): ");

                log.append( notClosedOrders);
                
                log.append( ".");
                dragonLoger.error( log);
                return false;
            } else {
                //there are no NotClosed orders associated to InProgress versions of OLD service element
                if (dragonLoger.isDebugEnabled()) dragonLoger.debug( "There are no NotClosed orders associated to InProgress versions of the old component (USID=" + oldUsid + ").");
            }
        }
        
        return true;
    }
    
    /**
     * Do DB Transaction for Dragon migration 
     */
    private void doDragonTransaction(String oldUsid, String newUsid, Logger dragonLoger, QueryManager queryMananger, VersionType newVersion) throws CSIException {

        if (m_createManager == null) {
            m_createManager = new CreateManager( m_connection);
        }

        m_createManager.setUser( newVersion.getSystem().getUser().getName());
        
        //start transaction
        boolean oldAutoCommitState = true; 
        try {
            oldAutoCommitState = m_connection.getAutoCommit();
            m_connection.setAutoCommit( false);
        } catch (SQLException e) {
            throw new CSIException( "Migration was skipped (OldUSID=" + oldUsid 
                    + ") because can not open transaction ", e);
        }

        boolean isTransactionOpened = true; 
        try {   
            String newComponentOldStatus = queryMananger.getLatestServiceElementStatus( newUsid);
            dragonLoger.info("The status of new component (USID=" + newUsid + ") is \"" + newComponentOldStatus + "\"");

            //Find all related to USID versions: Note: should be retrived before Update of new.  
            List allVSEId = queryMananger.getAllActiveVersionServiceElementIDsByUSID ( oldUsid);

            //save updated new version
            // devendra
			//updateVersionType (newVersion, newComponentOldStatus, newComponentOldStatus, true);

            //Disconect all. Note: should be disconnected after Update of new.
            Date dateOfTransaction = new Date();
            for(int i=0; i<allVSEId.size(); i++) {
                String versionServiceElementId = (String)allVSEId.get( i);
                
                updateStatus( Long.parseLong( versionServiceElementId ), 
                            CSIConstants.STATUS_DISCONNECT, 
                            dateOfTransaction, 
                            CISConstants.CIS_CSI_PROGUSER );
            }

            //commit transaction if all Ok
            m_connection.commit();
            isTransactionOpened = false;
          
        } catch (Exception e) {
            throw new CSIException( "Exception duiring transaction ", e);
        } finally {
            try {
                if (isTransactionOpened) {
                    m_connection.rollback();
                }
            } catch (SQLException e) {
                throw new CSIException( "SQL Error duiring ROLBACK", e);
            } finally {
                try {
                    m_connection.setAutoCommit( oldAutoCommitState);
                } catch (SQLException e) {
                    dragonLoger.error("SQL Error duiring restoring AutoCommit", e);
                }
            }
        }
    }
    
    /**
     * Copies pricing information from old specified component to new one including pricing of attributes
     * Note: method supposes that both component are existed
     * @param newSE
     * @param oldSE
     * @param dragonLoger
     * @return
     */
    protected boolean copyPricingInfoForDragon( ServiceElementType oldSE, ServiceElementType newSE, Logger dragonLoger){
        if (newSE == null || oldSE == null) {
            dragonLoger.error("incorrect usage of copyPricingInfoForDragon method! one of ServiceElement is null");
            return false;
        }
        if (dragonLoger.isDebugEnabled()) dragonLoger.debug("start copying of pricing information");

        //Store all attributes from the old component 
        HashMap newAttributes=new HashMap();
        for(Iterator i=newSE.getServiceElementAttribute().iterator(); i.hasNext();){
            ServiceElementAttributeType sea=(ServiceElementAttributeType)i.next();
            newAttributes.put(sea.getName(),sea);
        }

        HashMap absentOldAttributes=new HashMap();
        for(Iterator i=oldSE.getServiceElementAttribute().iterator();i.hasNext();){
            //copy pricing info for each attribute
            ServiceElementAttributeType oldSEAttribut=(ServiceElementAttributeType)i.next();

            OnceOffChargeType oldOnceOffCharge = oldSEAttribut.getOnceOffCharge();
            RecurringChargeType oldRecurringCharge = oldSEAttribut.getRecurringCharge();
            UsageChargeType oldUsageCharge = oldSEAttribut.getUsageCharge();

            //we are intrested only on attributes that were priced before
            if ((oldOnceOffCharge != null && oldOnceOffCharge.getAmount()!=0) 
                    || (oldRecurringCharge != null && oldRecurringCharge.getAmount()!=0)  
                    || (oldUsageCharge != null)) {
    
                ServiceElementAttributeType newAttr=(ServiceElementAttributeType)newAttributes.get( oldSEAttribut.getName());
                if(newAttr != null){
                    newAttr.setOnceOffCharge( oldSEAttribut.getOnceOffCharge());
                    newAttr.setRecurringCharge( oldSEAttribut.getRecurringCharge());
                    newAttr.setUsageCharge( oldSEAttribut.getUsageCharge());
					newAttr.setCustomerLabel(oldSEAttribut.getCustomerLabel());
    
                } else {
                    if (dragonLoger.isDebugEnabled()) dragonLoger.debug("New Component doesn't contain \"" + oldSEAttribut.getName() + "\" attribute");
                    absentOldAttributes.put( oldSEAttribut.getName(), oldSEAttribut.getValue());
                }
            }

        }

        if (absentOldAttributes.isEmpty()) {
            newSE.setOnceOffCharge( oldSE.getOnceOffCharge());
            newSE.setRecurringCharge( oldSE.getRecurringCharge());
            newSE.setUsageCharge( oldSE.getUsageCharge());

            if (dragonLoger.isDebugEnabled()) dragonLoger.debug( "Pricing information was copied completely.");
        } else {
            //By requirements we skip migration of element with log of all
            StringBuffer log = new StringBuffer( "Pricing information (OldUSID=");
            log.append( oldSE.getId()); 
            log.append( ", NewUSID=");
            log.append( newSE.getId()); 
            log.append(  ") was NOT copied because \"Priced attribute don't present in new component\": ");

            log.append( absentOldAttributes);

            log.append( ".");

            dragonLoger.error( log);

            return false;
        }

        return true;
    };
    
    /**
     * Merges the ServiceElements - from GOLD to CIS
     * CIS Service Element becomes a mester, but pricing information is copied
     * from GOLD SE to CIS SE and GOLD SE removed from database
     *
     * Criteria:
     * 1. The customerid should be the same for both SE
     * 2. The siteid should be the same
     * 3. The serviceid should be the same
     * 4. The SE Class should be the same
     *
     * @param goldUsid - GOLD Version (SE)
     * @param cisUsid - CIS Version (SE)
     * @throws CSIException - if any dismatch
     */
    public void mergeServiceElements(String goldUsid, String cisUsid) throws CSIException {
        /* do checks */
        if(goldUsid==null && cisUsid==null) {
            throw new CSIWrongUSID("GOLD and CIS USID are null");
        }

        QueryManager queryMananger = new QueryManager(m_connection);
        DeleteManager deleteManager = new DeleteManager(m_connection);

        // check ServiceElement integrity
        VersionType gldVersion = queryMananger.getVersionServiceElementByUSID(goldUsid);
        VersionType cisVersion = queryMananger.getVersionServiceElementByUSID(cisUsid);

        if(gldVersion==null && cisVersion==null) {
            throw new NoServiceElementFound("The Service Element does not exist");
        }


        if(!carefulCompare(gldVersion.getCustomerId(), cisVersion.getCustomerId())) {
            throw new CSIWrongUSID("The ServiceElements belongs to different Customers");
        }

        if(!carefulCompare(gldVersion.getSiteId(), cisVersion.getSiteId())) {
            throw new CSIWrongUSID("The ServiceElements belongs to different Sites");
        }

        if(!carefulCompare(gldVersion.getServiceId(), cisVersion.getServiceId())) {
            throw new CSIWrongUSID("The ServiceElements belongs to different Services");
        }

        // 1. set source (remove) SE & target (update) SE
        if( gldVersion!=null 
         && cisVersion!=null 
         && gldVersion.getServiceElement()!=null 
         && cisVersion.getServiceElement()!=null
         && gldVersion.getServiceElement().size()>0 
         && cisVersion.getServiceElement().size()>0 ) {
            ServiceElementTypeImpl gldSE = (ServiceElementTypeImpl)gldVersion.getServiceElement().get(0);
            ServiceElementTypeImpl cisSE = (ServiceElementTypeImpl)cisVersion.getServiceElement().get(0);
            
            if(!carefulCompare(gldSE.getServiceElementClass(), cisSE.getServiceElementClass())) {
                throw new CSIWrongUSID("The ServiceElements belongs to different Service element categories");
            }

            // drop GOLD version
            /* Discard Source System */
            deleteManager.deleteServiceElement(goldUsid);
            
            if(copyPrintInformation(gldSE, cisSE)) {
                cisVersion.setOrdertype(ORDER_TYPE_EXISTING);
                updateVersion(cisVersion, STATUS_CURRENT, false);
            }
        }
    }

    public void moveServiceElements(Set serviceElementUSIDs, VersionType targetVersion) throws CSIException {
        if(serviceElementUSIDs==null || serviceElementUSIDs.isEmpty() || targetVersion==null) return;

        QueryManager queryMananger = new QueryManager(m_connection);
        CreateManager createMananger = new CreateManager(
                m_connection,
                new Date(targetVersion.getOrderSentDate().getTime()),
                targetVersion.getSystem().getUser().getName(),
                targetVersion.getOrdertype(),
                targetVersion.getOrderstatus()
        );
        
        String orderType = targetVersion.getOrdertype();
        String orderStatus = targetVersion.getOrderstatus();
        String statusType;
        
        if(ORDER_TYPE_EXISTING.equals(orderType) || STATUS_ORDER_MANAGE.equals(orderStatus)) {
            statusType = STATUS_CURRENT;
        } else if(STATUS_ORDER_CANCEL.equals(orderStatus)) {
            statusType = STATUS_DELETE;
        } else {
            statusType = STATUS_INPROGRESS;
        }
            
        long moveToVersionId = queryMananger.getLatestVersion(targetVersion, statusType);
        if(moveToVersionId==-1) { // not found
            try {
                moveToVersionId = createMananger.createVersionType(targetVersion);
            } catch(SQLException e) {
                throw new CSIRollbackException("Can't perform SQL query.", e);
            }
        }
        
        PreparedStatement ps = null;
        
        // update VersionID for all serviceElementUSIDs and statusType
        try {
            ps = m_connection.prepareStatement(
                   "UPDATE                                                                            " 
                 + "         CVERSIONSERVICEELEMENT vse                                               "
                 + "SET                                                                               "
                 + "         vse.VERSIONID = ?                                                        "
                 + "WHERE                                                                             "
                 + "         vse.SERVICEELEMENTID = (                                                 "
                 + "             SELECT SERVICEELEMENTID FROM CSERVICEELEMENT WHERE USID = ?          "
                 + "         )                                                                        "
            );
            for(Iterator iter=serviceElementUSIDs.iterator();iter.hasNext();) {
                ps.setLong(1, moveToVersionId);
                ps.setString(2, (String)iter.next());
                ps.executeUpdate();
            }
        } catch(SQLException e) {
            m_logger.error("Can't perform SQL query.", e);
            throw new CSIRollbackException("Can't perform SQL query.", e);
        }finally {
            carefullyClose(null, ps);  ps=null;            
        }
    }
    public void updateUsid(String oldUsid, String newUsid) throws CSIException {
        PreparedStatement ps = null;

        if(oldUsid==null || newUsid==null) {
            throw new CSIRollbackException("updateUsid - both USIDs should not be null");
        }

        if(oldUsid.equals(newUsid)) {
            m_logger.debug("Old and new USIDs are same. USID = "+newUsid);
            return;            
        }
        
        try {
            ps = m_connection.prepareStatement(
                    "update " +
                    "   CSERVICEELEMENT " +
                    "set " +
                    "   USID = ? " +
                    "where " +
                    "   USID = ? "
            );
            ps.setString(1, newUsid);
            ps.setString(2, oldUsid);
            ps.executeUpdate();
            carefullyClose(null, ps); ps=null;
        } catch (SQLException e) {
            throw new CSIRollbackException("Can't perform SQL query.", e);
        } catch (Exception e) {//don't care what kind of exception it will be, just rollback and notify
            throw new CSIRollbackException("updateUsid got an exception.", e);
        } finally {
            carefullyClose(null, ps);
        }
   }

    public void updateRemoteSiteCode(long versionServiceElementId, long attrId, String siteId) throws SQLException, CSIException {
        if (siteId == null || "".equals(siteId)) {
            info("Site Code cannot be empty.");
            throw new CSIException("updateRemoteSiteCode: Site Code cannot be empty");
        }

        if (attrId != 0) {
            // existing Remote Site Code record
            PreparedStatement ps = null;
            try {
                ps = m_connection.prepareStatement(
                    "UPDATE CServiceAttribute       " +
                    "   SET Value = ?               " + //1
                    "     , lupdDate = ?            " + //2
                    "     , lupdBy = ?              " + //3
                    " WHERE ServiceAttributeId = ?  "   //4
                );

                ps.setString(1, siteId);
                ps.setTimestamp(2, TransDate.getTimestamp(m_currentDate));
                ps.setString(3, CISConstants.CIS_CSI_PROGUSER);
                ps.setLong(4, attrId);

                ps. executeUpdate();
            } finally {
                carefullyClose(null, ps); ps = null;
            }
        } else {
            // create new Remote Site Code record
            debug("Creating new attribute record");
            CreateManager cm = new CreateManager(m_connection);
            cm.createRemoteSiteCode(versionServiceElementId, "Remote Site Code", siteId);
        }
    }

    protected Category getLogger() {
        return UpdateManager.m_logger;
    }

    protected ArrayList getStatusOfElement(ServiceElementType dbServiceElement) throws SQLException {
        PreparedStatement ps = null;
        ArrayList result = new ArrayList();
        if(dbServiceElement!=null) {
            try {
                ps = m_connection.prepareStatement("SELECT vses.statusTypeCode" +
                        " FROM cServiceElement se, cVersionServiceElement vse, cVersionServiceElementSts vses" +
                        " WHERE se.USID = ?" + //1
                        " AND vse.versionServiceElementId = vses.versionServiceElementId" +
                        " AND se.serviceElementId = vse.serviceElementId" +
                        " ORDER BY vses.versionServiceElementStsId DESC ");
    
                ps.setString(1, dbServiceElement.getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
            } finally {
                carefullyClose(null, ps);
                ps = null;
            }
        }
        return result;
    }

    
    protected boolean checkAndPopulatePrice(ServiceElementType xServiceElement) throws CSIException {
        debug("USID of element is "+xServiceElement.getId());
        VersionType   xVersion=m_queryManager.getLatestVersionServiceElementByUSID(xServiceElement.getId());
       
        if(xVersion==null) {
            //Version exists, but not returned
            return true;
        }
        
        ServiceElementType xCSIElem=(ServiceElementType)xVersion.getServiceElement().get(0);
        boolean result=false;
        int count=0;
        HashMap cisAttributes=new HashMap();
        for(Iterator i=xServiceElement.getServiceElementAttribute().iterator();i.hasNext();){
            ServiceElementAttributeType sea=(ServiceElementAttributeType)i.next();
            cisAttributes.put(sea.getName(),sea);
        }
        for(Iterator i=xCSIElem.getServiceElementAttribute().iterator();i.hasNext();){
            ServiceElementAttributeType sea=(ServiceElementAttributeType)i.next();
            ServiceElementAttributeType cisAttr=(ServiceElementAttributeType)cisAttributes.get(sea.getName());
            if(cisAttr!=null){
                //Set usage charge and recurring charge
                cisAttr.setOnceOffCharge(sea.getOnceOffCharge());
                cisAttr.setRecurringCharge(sea.getRecurringCharge());
                cisAttr.setUsageCharge(sea.getUsageCharge());
                result|= !cisAttr.getValue().equals(sea.getValue());
                debug("Result is "+result+". Comparation is "+!cisAttr.getValue().equals(sea.getValue()));
                debug("CIS value is "+cisAttr.getValue()+". CSI Value is " + sea.getValue());
                count++;
            }
        } 
        result|= count!=cisAttributes.size();
        cisAttributes.clear();
        
        if(result) {
            xServiceElement.setOnceOffCharge(xCSIElem.getOnceOffCharge());
            xServiceElement.setRecurringCharge(xCSIElem.getRecurringCharge());
            xServiceElement.setOnceOffCharge(xCSIElem.getOnceOffCharge());
        }
        
        return result;
    }
    /**
     * Methods implements only for CIS message
     * Iterates through given (newly created) CVersionServiceElement objects
     * and find out all the old (existing on the DB) ones. Updates existing
     * by rules from UC6700 10.1.
     *
     * @param xVersion the JAXB object of new Version
     * @throws SQLException if any error occurred during processing the query
     * @throws CSIException if any crtical error occurred during processing
     */
    private void updateVersionTypeFromCIS(VersionType xVersion, String cisStatus, boolean updateAll) throws SQLException, CSIException, JAXBException {
        String debugVersionName = "[ " + xVersion.getCustomerId() + " / " + xVersion.getSiteId() + " / " + xVersion.getServiceId() + " ]";

        debug("updateVersionTypeFromCIS(" + debugVersionName + " \"" + cisStatus + "\", " + updateAll + ");");

        // Holds the newly created Version ID. Helps to identify if Version was
        // already created for one of ServiceElement set.
        long newVersionId = -1;
        int  countNewElem=0;
        // Iterate through given ServiceElements and find out all
        // old (existing on the DB) ones. Update existing with new information.
        // If no existing found, then new ones have to be created.
        for (Iterator i = xVersion.getServiceElement().iterator(); i.hasNext();) {

            ServiceElementType xServiceElement = (ServiceElementType) i.next();
            //(KY): First, try to find this ServiceElement in DB and update if it is changed.
            ServiceElementType dbServiceElement = m_createManager.findServiceElement(xServiceElement);
            long oldVersionServiceElementId = -1;
            List oldVersionServiceElementLst = null;
            ArrayList status_history = getStatusOfElement(dbServiceElement);
            String csiStatusLatest = (String)(status_history.size()>0?status_history.get(0):"");

            if (STATUS_INPROGRESS.equals(cisStatus)) {
                if(STATUS_INPROGRESS.equals(csiStatusLatest)){
                    //CR9260
                    m_createManager.updateServiceElement(dbServiceElement, xServiceElement, updateAll);
                    //UC6700 10.1 rule 7
                    oldVersionServiceElementLst =
                            findAllVersionServiceElementId(dbServiceElement, cisStatus);
                }//else UC6700 10.1 rule 7
            } else if (STATUS_DISCONNECT.equals(cisStatus)) {
                if (STATUS_CURRENT.equals(csiStatusLatest) || "".equals(csiStatusLatest) ) {
                    //CR9260
                    m_createManager.updateServiceElement(dbServiceElement, xServiceElement, updateAll);
                    //UC6700 10.1 rule 8,9,10
                    if (newVersionId == -1) {
                        // Not yet. Create one
                        newVersionId = m_createManager.createVersionType(xVersion);
                    }
                    //Will have to create new CServiceElement.
                    m_createManager.createVersionServiceElement(newVersionId, xServiceElement,
                            updateAll);
                }else if(STATUS_DISCONNECT.equals(csiStatusLatest) ) {
                    oldVersionServiceElementLst = findAllVersionServiceElementId(dbServiceElement, cisStatus);
                }
            } else if (STATUS_CURRENT.equals(cisStatus)){
                if(STATUS_DISCONNECT.equals(csiStatusLatest) ) {
                   //CR9260
                    m_createManager.updateServiceElement(dbServiceElement, xServiceElement, updateAll);
                    //part of rule 11
                    changeStatusOfServiceElement(xServiceElement.getId(),STATUS_DISCONNECT,STATUS_DELETE);                    
                }
                //new interpretation of rules 1,2,11,3
                if(STATUS_CURRENT.equals(csiStatusLatest)){
                    m_createManager.updateServiceElement(dbServiceElement, xServiceElement, updateAll);
                    oldVersionServiceElementLst = findAllVersionServiceElementId(dbServiceElement, cisStatus);
                }else  if(STATUS_DISCONNECT.equals(csiStatusLatest) || STATUS_DELETE.equals(csiStatusLatest)
                          || "".equals(csiStatusLatest) ) {
  
//                if((STATUS_CURRENT.equals(csiStatusLatest)&& checkAndPopulatePrice(xServiceElement)) 
//                        || STATUS_DISCONNECT.equals(csiStatusLatest) || STATUS_DELETE.equals(csiStatusLatest)
//                        || "".equals(csiStatusLatest) ) {
                    //UC6700 10.1 rule 1,2,11
                    if (newVersionId == -1) {
                        // Not yet. Create one
                        newVersionId = m_createManager.createVersionType(xVersion);
                    }                        
                    //Will have to create new CServiceElement.
                    m_createManager.createVersionServiceElement(newVersionId,
                            xServiceElement, updateAll);
                }
            }

            status_history.clear();            
            //update all attributes which need
            if (oldVersionServiceElementLst != null && !oldVersionServiceElementLst.isEmpty()) {
                for (Iterator iter = oldVersionServiceElementLst.iterator(); iter.hasNext();) {
                    oldVersionServiceElementId = ((Long)(iter.next())).longValue();
                    Map oldAttributes = getAttributesByStatus(dbServiceElement, cisStatus);

                    if (oldAttributes.size() > 0) {
                        updateAttributes(oldAttributes, xServiceElement, oldVersionServiceElementId);
                        oldAttributes.clear();
                    }

                }
                oldVersionServiceElementLst.clear();
            }
        }
    }
//    
    protected void changeStatusOfServiceElement(String usid, String oldStatus, String newStatus) throws CSIRollbackException {
        PreparedStatement ps = null;
        int result = 0;
        try {
            ps = m_connection.prepareStatement("UPDATE CVersionServiceElementSts set STATUSTYPECODE=?" + //1 
                        " WHERE STATUSTYPECODE=? AND versionserviceelementid IN" + //2
                        " (select versionserviceelementid from CVersionServiceElement vse, CServiceElement se " +
                        " where vse.serviceelementid=se.serviceelementid and se.usid=? )"); //3
            ps.setString(1, newStatus);
            ps.setString(2, oldStatus);
            ps.setString(3, usid);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new CSIRollbackException("Can't perform SQL query.", e);
        } finally {
            carefullyClose(null, ps);
            ps = null;
        }       
    }

    /**
     * helper class for inserting data into site temp table
     *
     * @param siteInfo
     */
    public void insertSiteTempTable(SiteInformationType siteInfo) {
        if (siteInfo == null) return;
        PreparedStatement ps = null;
        try {
            ps = m_connection.prepareStatement("INSERT INTO CIS_TEMP_SITE ( " +
                    "    CUSTCODE " + //1
                    "  , SITECODE " + //2
                    "  , ADDRESS1 " + //3
                    "  , ADDRESS2 " + //4
                    "  , ADDRESS3 " + //5
                    "  , CITYCODE " + //6
                    "  , COUNTRYCODE " + //7
                    "  , POSTALCODE " + //8
                    "  , STATECODE " + //9
                    "  , CITYNAME " + //10
                    "  , CONTACTNAME " + //11
                    "  , TELEPHONENUMBER " + //12
                    "  , FAXNUMBER " + //13
                    "  , EMAILADDRESS ) " + //14
                    "  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, siteInfo.getCustCode());
            ps.setString(2, siteInfo.getCISSiteId());
            ps.setString(3, siteInfo.getAddress1());
            ps.setString(4, siteInfo.getAddress2());
            ps.setString(5, siteInfo.getAddress3());
            ps.setString(6, siteInfo.getCityCode());
            ps.setString(7, siteInfo.getCountryCode());
            ps.setString(8, siteInfo.getPostalCode());
            ps.setString(9, siteInfo.getStateCode());
            ps.setString(10, siteInfo.getCityName());

            if (siteInfo.getSiteContact() != null && siteInfo.getSiteContact().size() != 0) {
                SiteContactType siteContact = (SiteContactType) siteInfo.getSiteContact().iterator().next();
                ps.setString(11, siteContact.getContactName());
                ps.setString(12, siteContact.getTelephoneNumber());
                ps.setString(13, siteContact.getFaxNumber());
                ps.setString(14, siteContact.getEmailAddress());
            }
            ps.executeQuery();
        } catch (SQLException sqle) {
            m_logger.error("Error while inserting to site temp table: " + siteInfo.getCISSiteId());
            m_logger.error(sqle);
        } finally {
            carefullyClose(null, ps);
            ps = null;
        }
    }

}


