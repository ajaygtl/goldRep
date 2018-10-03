/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/database/DeleteManager.java,v 1.14 2004/12/09 14:36:30 andrey.krot Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.database;

import com.equant.csi.common.CSIConstants;
import com.equant.csi.exceptions.CSIException;
import com.equant.csi.exceptions.CSIRollbackException;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.utilities.LoggerFactory;
import org.apache.log4j.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;

/**
 * Handles status changes of JABX objects when orders of
 * DISCONNECT type is received.
 *
 * @author Vadim Gritsenko
 * @author Vasyl Rublyov
 * @version $Revision: 612 $
 *
 * @see Manager
 */
public class DeleteManager extends Manager {
    /** Initializing the logger. */
    private static final Category m_logger = LoggerFactory.getInstance(DeleteManager.class.getName());

    /**
     * Constructor to be used in EJB.
     *
     * @param connection the Connection using for database connection
     */
    public DeleteManager(Connection connection) {
        super(connection);
    }

    /**
     * Deletes all service versions of EXISTING type, identified by the
     * Version, from the database. This is "hard" delete, information
     * could not be restored after completion of this operation.
     *
     * @param xVersion the JAXB service version object to delete
     */
    public void deleteVersion(VersionType xVersion) throws CSIException {
        debug("Deleting VersionType - call deleteVersion(\""+xVersion.getOrderid()+"\")");
        deleteVersion(xVersion.getOrderid());
    }

    /**
     * Deletes all the service version objects.
     *
     * @param xVersion the JAXB service version object to delete
     */
    public void deleteAllVersions(VersionType xVersion) throws CSIException {
        debug("Deleting All VersionType - call deleteAllVersions(\""+xVersion.getOrderid()+"\"");
        deleteAllVersions(xVersion.getOrderid());
    }

    /**
     * Deletes all attributes, charges, servieelement, versionservieelement, versions
     *
     * @param usid
     */
    public String deleteServiceElement(String usid) throws CSIException {
        debug("public void deleteServiceElement(" + usid + ")");
        return deleteServiceElementType(usid);
    }

    /**
     * Deletes all the service version objects.
     *
     * @param customerHandle the customer handle for orders to be deleted
     * @param siteHandle the site handle for orders to be deleted
     * @param serviceHandle the service handle/id for orders to be deleted
     */
    public void deleteAllVersions(String customerHandle, String siteHandle, String serviceHandle) throws CSIException {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        Vector versions = new Vector();

        debug("Deleting All VersionType - call " + "deleteAllVersions(\"" + customerHandle + "\", \"" + siteHandle + "\", \"" + serviceHandle + "\")");

        if( siteHandle==null || siteHandle.length()==0 ||
            serviceHandle==null || serviceHandle.length()==0)
        {
            info("Deleting All VersionType - got an empty attribute, exiting...");
            return;
        }

        // Get the persitent CVersion objects by order ID
        try {
            debug(  "Searching for all VersionIDs for " +
                    "customerHandle = \"" + customerHandle + "\", " +
                    "siteHandle = \"" + siteHandle + "\", " +
                    "serviceHandle = \"" + serviceHandle + "\""
            );

            stmt = m_connection.prepareStatement(
                      "SELECT VersionID "
                    + "FROM   CVersion "
                    + "WHERE  CustHandle = ? AND SiteHandle = ? AND ServiceHandle = ? "
            );
            stmt.setString(1, customerHandle);
            stmt.setString(2, siteHandle);
            stmt.setString(3, serviceHandle);
            rs = stmt.executeQuery();
            while(rs.next()) {
                if(rs.getLong(1)!=0) versions.add(new Long(rs.getLong(1)));
            }
            carefullyClose(rs, stmt); rs=null; stmt=null;

            for(Iterator iter=versions.iterator(); iter.hasNext(); ) {
                long l = ((Long)iter.next()).longValue();

                debug("deleteVersion(DBConn, " + l + ")");
                deleteVersionType(l);
            }
        } catch (SQLException sqlex) {
            throw new CSIRollbackException("Can't perform SQL query.\n" + sqlex);
        } finally {
            versions.clear();
            carefullyClose(rs, stmt); rs=null; stmt=null;
        }
    }

    /**
     * Deletes all service versions of EXISTING type, identified by
     * the order ID, from the database. This is "hard" delete, information
     * could not be restored after completion of this operation.
     * <p>This is the only entry point into this manager.
     *
     * @param orderID the order ID to delete
     */
    public void deleteVersion(String orderID) throws CSIException {
        ResultSet rs = null;
        PreparedStatement stmt = null;

        Iterator iter = null;

        debug("Deleting existing versions of order " + orderID);
        Vector vVersion = new Vector();

        // Get the persitent CVersion objects by order ID
        try {
            debug("Searching for all VersionIDs for OrdHandle = " + orderID + " and OrderType != " + CSIConstants.ORDER_TYPE_EXISTING);
            stmt = m_connection.prepareStatement(
                      "SELECT VersionID "
                    + "FROM   CVersion "
                    + "WHERE  OrdHandle = ? and ordertype != ? "
            );
            stmt.setString(1, orderID);
            stmt.setString(2, CSIConstants.ORDER_TYPE_EXISTING);
            rs = stmt.executeQuery();
            while(rs.next()) {
                if(rs.getLong(1)!=0) vVersion.add(new Long(rs.getLong(1)));
            }
            carefullyClose(rs, stmt); rs=null; stmt=null;

            iter=vVersion.iterator();
            while(iter.hasNext()) {
                Long l = (Long)iter.next();
                debug("deleteAllAttributes(DBConn, " + l.longValue() + ")");
                deleteAllAttributes(l.longValue());
            }

            debug("Deleting all Versions for OrdHandle = " + orderID + " and OrderType = " + CSIConstants.ORDER_TYPE_EXISTING);
            stmt = m_connection.prepareStatement(
                      "DELETE FROM CVersion "
                    + "WHERE  OrdHandle = ? and ordertype != ? "
            );
            stmt.setString(1, orderID);
            stmt.setString(2, CSIConstants.ORDER_TYPE_EXISTING);
            stmt.executeUpdate();
        } catch (SQLException sqlex) {
            info("Can't perform SQL query.", sqlex);
            throw new CSIRollbackException("Can't perform SQL query.", sqlex);
        } finally {
            vVersion.clear();
            carefullyClose(rs, stmt); rs=null; stmt=null;
        }
    }

    /**
     * Deletes all the service version objects.
     *
     * @param orderID the order ID to delete
     */
    public void deleteAllVersions(String orderID) throws CSIException {
        ResultSet rs = null;
        PreparedStatement stmt = null;

        Iterator iter = null;
        Vector vVersion = new Vector();

        debug("Deleting all versions of order " + orderID);

        // Get the persitent CVersion objects by order ID
        try {

            debug("Searching for all VersionIDs for OrdHandle = " + orderID);
            stmt = m_connection.prepareStatement(
                      "SELECT VersionID "
                    + "FROM   CVersion "
                    + "WHERE  OrdHandle = ? "
            );
            stmt.setString(1, orderID);
            rs = stmt.executeQuery();
            while(rs.next()) {
                if(rs.getLong(1)!=0) vVersion.add(new Long(rs.getLong(1)));
            }
            carefullyClose(rs, stmt); rs=null; stmt=null;

            iter=vVersion.iterator();
            while(iter.hasNext()) {
                Long l = (Long)iter.next();
                debug("deleteAllAttributes(DBConn, " + l.longValue() + ")");
                deleteAllAttributes(l.longValue());
            }

            debug("Deleting all Versions for OrdHandle = " + orderID);
            stmt = m_connection.prepareStatement(
                      "DELETE FROM CVersion "
                    + "WHERE  OrdHandle = ? "
            );
            stmt.setString(1, orderID);
            stmt.executeUpdate();
        } catch (SQLException sqlex) {
            throw new CSIRollbackException("Can't perform SQL query.\n"+sqlex);
        } finally {
            vVersion.clear();
            carefullyClose(rs, stmt); rs=null; stmt=null;
        }
    }

    /**
     * Delete specific version by VersionID
     *
     * @param versionID the version id
     */
    public void deleteVersionType(long versionID) throws CSIException {
        ResultSet rs = null;
        PreparedStatement stmt = null;

        debug("Deleting version of order versionID = " + versionID);

        // Get the persitent CVersion objects by order ID
        try {
            deleteAllAttributes(versionID);

            stmt = m_connection.prepareStatement(
                      "DELETE FROM CVersion "
                    + "WHERE  VERSIONID = ? "
            );
            stmt.setLong(1, versionID);
            stmt.executeUpdate();
        } catch(SQLException e) {
            info("deleetVersionType(" + versionID +") got an exception.", e);
            throw new CSIRollbackException("deleteVersionType(" + versionID +") got an exception.", e);
        } finally {
            carefullyClose(rs, stmt); rs=null; stmt=null;
        }
    }

    /**
     * Delete all properties of version (HARD DELETE!!!)
     *
     * @param id the versionid
     */
    private void deleteAllAttributes(long id) throws CSIException {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        Vector listVSE = new Vector();

        try {
            debug("Searching for all CChargeChangeItem for VersionID = " + id);
            stmt = m_connection.prepareStatement(
                      "SELECT VersionServiceElementID       "
                    + "FROM CVersionServiceElement          "
                    + "WHERE VersionID = ?                  "
            );

            stmt.setLong(1, id);
            rs = stmt.executeQuery();
            while(rs.next()) {
                if(rs.getLong(1)!=0) {
                    listVSE.add(new Long(rs.getLong(1)));
                }
            }
            carefullyClose(rs, stmt); rs=null; stmt=null;

			List<Long> list=new ArrayList<Long>();
            
            for(int i=1;i<=listVSE.size();i++){
            	debug("listVSE.size()=="+listVSE.size());				
            	list.add((Long)listVSE.get(i-1));
            	if(i==listVSE.size() || (i%999)==0){
				debug("Entering in if i="+i);
            	debug("Sending for delete = " + list);
            	deleteVersionServiceElementType(list);
            	list=new ArrayList<Long>();
				debug("creating new array list ="+list);
            	}
            	
            }

           // deleteVersionServiceElementType(listVSE);
        } catch(SQLException e) {
            info("deleteAllAttributes(" + id +") got an exception.", e);
            throw new CSIRollbackException("deleteAllAttributes(" + id +") got an exception.", e);
        } finally {
            listVSE.clear();
            carefullyClose(rs, stmt); rs=null; stmt=null;
        }
    }

    /**
     * Delete all properties of VersionServiceElement (HARD DELETE!!!)
     *
     * @param versionServiceElementId the collection of VersionServcieElementID
     */
    private void deleteVersionServiceElementType(Collection versionServiceElementId) throws CSIException {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        Statement stmt1 = null;

        Iterator iter = null;
        Vector vUsageCharge = new Vector();
        Vector vRecurringCharge = new Vector();
        Vector vOnceOffCharge = new Vector();
        Vector vServiceElement = new Vector();
        Vector vServiceAttributeID = new Vector();

        /* TODO: VR: It is real hack, isn't it? */
        StringBuffer versionServiceElementInList = null;
        for(iter=versionServiceElementId.iterator();iter.hasNext(); ) {
            String l = ((Long)iter.next()).toString();
            if(versionServiceElementInList == null) {
                versionServiceElementInList = new StringBuffer(l);
            } else {
                versionServiceElementInList.append(",").append(l);
            }
        }
        debug("Deleting all CVersionServiceElement for (" + versionServiceElementInList + ") collection of ids");

        try {
            // step 1.  delete from CChargeChangeItem

            debug("Searching for all CChargeChangeItem for VersionServiceElementID in (" + versionServiceElementInList + ") collection of ids");
            stmt1 = m_connection.createStatement();
            rs = stmt1.executeQuery(
                      "SELECT UsageChargeID "
                    + "     , RecurringChargeID "
                    + "     , OnceOffChargeID "
                    + "FROM   CChargeChangeItem "
                    + "WHERE  VersionServiceElementID in ( " + versionServiceElementInList + ")"
            );
            while(rs.next()) {
                if(rs.getLong(1)!=0) vUsageCharge.add(new Long(rs.getLong(1)));
                if(rs.getLong(2)!=0) vRecurringCharge.add(new Long(rs.getLong(2)));
                if(rs.getLong(3)!=0) vOnceOffCharge.add(new Long(rs.getLong(3)));
            }
            carefullyClose(rs, null); rs=null;


            debug("Searching for all CServiceChangeItem for VersionServiceElementID in (" + versionServiceElementInList + ")");
            rs = stmt1.executeQuery(
                      "SELECT "
                    + "    CServiceChangeItem.ServiceAttributeID "
                    + "FROM "
                    + "    CServiceChangeItem "
                    + "  , CVersionServiceElement "
                    + "WHERE "
                    + "    CServiceChangeItem.VersionServiceElementID = CVersionServiceElement.VersionServiceElementID "
                    + "  AND "
                    + "    CServiceChangeItem.ServiceAttributeID IS NOT NULL "
                    + "  AND "
                    + "    CVersionServiceElement.VersionServiceElementID in (" + versionServiceElementInList + ")"
            );
            while(rs.next()) {
                Long l = new Long(rs.getLong(1));
                if(l.longValue()!=0 && !vServiceAttributeID.contains(l)) {
                    debug("(CServiceChangeItem) Adding to delete ServiceAttributeID = " + l);
                    vServiceAttributeID.add(l);
                }
            }
            carefullyClose(rs, null); rs=null;

            debug("Searching for all CServiceAttrutes by CChargeChangeItem for VersionServiceElementID in " + versionServiceElementInList + ")");
            rs = stmt1.executeQuery(
                      "SELECT DISTINCT "
                    + "     CChargeChangeItem.ServiceAttributeID "
                    + "FROM "
                    + "     CChargeChangeItem "
                    + "   , CVersionServiceElement "
                    + "WHERE "
                    + "     CChargeChangeItem.VersionServiceElementID = CVersionServiceElement.VersionServiceElementID "
                    + "   AND "
                    + "     CChargeChangeItem.ServiceAttributeID IS NOT NULL "
                    + "   AND "
                    + "     CVersionServiceElement.VersionServiceElementID in (" + versionServiceElementInList + ")"
            );
            while(rs.next()) {
                Long l = new Long(rs.getLong(1));
                if(l.longValue()!=0 && !vServiceAttributeID.contains(l)) {
                    debug("(CChargeChangeItem) Adding to delete ServiceAttributeID = " + l);
                    vServiceAttributeID.add(l);
                }
            }
            carefullyClose(rs, null); rs=null;

            debug("Deleting all CChargeChangeItem for VersionServiceElementID in (" + versionServiceElementInList + ")");
            stmt1.executeUpdate(
                      "DELETE FROM CChargeChangeItem "
                    + "WHERE  VersionServiceElementID in (" + versionServiceElementInList + ")"
            );

            // step 2.1 delete from CUsageCharge
            iter = vUsageCharge.iterator();
            stmt = m_connection.prepareStatement(
                      "DELETE FROM CUsageCharge "
                    + "WHERE UsageChargeID = ?"
            );
            while(iter.hasNext()) {
		        Long l = (Long)iter.next();
                debug("Deleting all CUsageCharge for UsageChargeID = " + l);
                stmt.setLong(1, l.longValue());
                stmt.executeUpdate();
            }
            carefullyClose(null, stmt); stmt=null;

            // step 2.2 delete from CRecurringCharge
            iter = vRecurringCharge.iterator();
            stmt = m_connection.prepareStatement(
                      "DELETE FROM CRecurringCharge "
                    + "WHERE RecurringChargeID = ?"
            );
            while(iter.hasNext()) {
    		    Long l = (Long)iter.next();
                debug("Deleting all CRecurringCharge for RecurringChargeID = " + l);
                stmt.setLong(1, l.longValue());
                stmt.executeUpdate();
            }
            carefullyClose(null, stmt); stmt=null;

            // step 2.3 delete from COnceOffCharge
            iter = vOnceOffCharge.iterator();
            stmt = m_connection.prepareStatement(
                      "DELETE FROM COnceOffCharge "
                    + "WHERE OnceOffChargeID = ?"
            );
            while(iter.hasNext()) {
		        Long l = (Long)iter.next();
                debug("Deleting all COnceOffCharge for OnceOffChargeID = " + l);
                stmt.setLong(1, l.longValue());
                stmt.executeUpdate();
            }
            carefullyClose(null, stmt); stmt=null;

            // step 3. delete from CServiceChangeItem
            debug("Deleting all CServiceChangeItem for VersionServiceElementID in (" + versionServiceElementInList + ")");
            stmt1.executeUpdate(
                    "DELETE "
                  + "FROM   CServiceChangeItem "
                  + "WHERE  VersionServiceElementID in (" + versionServiceElementInList + ")"
            );

            // step 4 delete from CServiceAttribute
            iter = vServiceAttributeID.iterator();
            stmt = m_connection.prepareStatement(
                    "DELETE "
                  + "FROM   CServiceAttribute "
                  + "WHERE  ServiceAttributeID = ? "
            );
            while(iter.hasNext()) {
                //boolean toDelete = true;
                Long l = (Long)iter.next();
                debug("Deleting all CServiceAttribute for ServiceAttributeID = " + l);
                stmt.setLong(1, l.longValue());
                stmt.executeUpdate();
            }
            carefullyClose(null, stmt); stmt=null;

            // step 5 delete from CVersionServiceElementSts
            debug("Deleting all VersionServiceElementID in (" + versionServiceElementInList + ")");
            stmt1.executeUpdate(
                    "DELETE "
                  + "FROM   CVersionServiceElementSts "
                  + "WHERE  VersionServiceElementID in (" + versionServiceElementInList + ")"
            );

            // step 6 delete from CVersionServiceElement
            debug("Deleting all CVersionServiceElement for VersionServiceElementID in (" + versionServiceElementInList + ")");
            rs = stmt1.executeQuery(
                      "SELECT ServiceElementID "
                    + "FROM   CVersionServiceElement "
                    + "WHERE  VersionServiceElementID in (" + versionServiceElementInList + ")"
            );
            while(rs.next()) {
                if(rs.getLong(1)!=0) vServiceElement.add(new Long(rs.getLong(1)));
            }
            carefullyClose(rs, null); rs=null;

            debug("Deleting all CVersionServiceElement for VersionServiceElementID in (" + versionServiceElementInList + ")");
            stmt1.executeUpdate(
                    "DELETE "
                  + "FROM   CVersionServiceElement "
                  + "WHERE  VersionServiceElementID in (" + versionServiceElementInList + ")"
            );

            // step 7 delete from CServiceElement
            iter = vServiceElement.iterator();
            stmt = m_connection.prepareStatement(
                    "DELETE "
                  + "FROM   CServiceElement "
                  + "WHERE  ServiceElementID = ? "
                  + " AND   ServiceElementID NOT IN "
                  + "        (SELECT ServiceElementID FROM CVersionServiceElement) "
            );
            while(iter.hasNext()) {
                Long l = (Long)iter.next();
                debug("Deleting all CServiceElement for ServiceElementID = " + l);
                stmt.setLong(1, l.longValue());
                stmt.executeUpdate();
            }
        } catch(SQLException e) {
            info("deleteVersionServiceElementType(" + versionServiceElementInList +") got an exception.", e);
            throw new CSIRollbackException("deleteVersionServiceElementType(" + versionServiceElementInList +") got an exception.", e);
        } finally {
            vUsageCharge.clear();
            vRecurringCharge.clear();
            vOnceOffCharge.clear();
            vServiceElement.clear();
            vServiceAttributeID.clear();
            carefullyClose(rs, stmt); rs=null; stmt=null;
            carefullyClose(null, stmt1); stmt1=null;
        }
    }

    
    /**
     * Deletes all attributes, charges, servieelement, versionservieelement, versions
     *
     * @param usid
     * @return String - source system of deleted ServiceElement
     */
    private String deleteServiceElementType(String usid) throws CSIException {
        String sourceSystem = null;
        HashSet hVersion = new HashSet(), hVersionServiceElement = new HashSet();
        long serviceElementId = 0;

        ResultSet rs = null;
        PreparedStatement stmt = null;
        Statement stmt1 = null;

        if(usid==null || usid.length()==0) {
            throw new CSIException("USID is missing");
        }

        debug("deleteServiceElement for USID = " + usid);

        try {
            // get SE, VSE, V objects
            stmt = m_connection.prepareStatement(
                    "SELECT v.VERSIONID                                                 " +
                    "     , vse.VERSIONSERVICEELEMENTID                                 " +
                    "     , se.SERVICEELEMENTID                                         " +
                    "     , se.SOURCESYSTEM                                             " +
                    "FROM   CSERVICEELEMENT se                                          " +
                    "     , CVERSIONSERVICEELEMENT vse                                  " +
                    "     , CVERSION v                                                  " +
                    "WHERE  vse.VERSIONID = v.VERSIONID                                 " +
                    "   AND se.SERVICEELEMENTID = vse.SERVICEELEMENTID                  " +
                    "   AND se.USID = ?                                                 "
            );
            stmt.setString(1, usid);

            rs = stmt.executeQuery();
            while(rs.next()) {
                if(rs.getLong(1)!=0) hVersion.add(new Long(rs.getLong(1)));
                if(rs.getLong(2)!=0) hVersionServiceElement.add(new Long(rs.getLong(2)));
                if(serviceElementId==0 && rs.getLong(3)!=0) serviceElementId = rs.getLong(3);
                sourceSystem = rs.getString(4);
            }

            if(serviceElementId !=0 && hVersion.size() > 0) {
                // there are values, exlude now all versions which have other SEs
                StringBuffer versionListString = null;
                for(Iterator iter=hVersion.iterator();iter.hasNext();) {
                    if(versionListString==null) {
                        versionListString = new StringBuffer(iter.next().toString());
                    } else {
                        versionListString.append(", ").append(iter.next());
                    }
                }

                debug("deleteServiceElement in (" + versionListString + ")");
                // we have all INT values, so can directly set to the string
                stmt1 = m_connection.createStatement();
                rs = stmt1.executeQuery(
                        "SELECT DISTINCT v.VERSIONID                                        " +
                        "FROM CVERSION v                                                    " +
                        "   , CVERSIONSERVICEELEMENT vse                                    " +
                        "   , CSERVICEELEMENT se                                            " +
                        "WHERE  vse.VERSIONID = v.VERSIONID                                 " +
                        "   AND se.SERVICEELEMENTID = vse.SERVICEELEMENTID                  " +
                        "   AND v.VERSIONID in (" + versionListString.toString() + ")       " +
                        "   AND se.SERVICEELEMENTID NOT IN (" + serviceElementId + ")       "
                );
                while(rs.next()) {
                    Long l = new Long(rs.getLong(1));
                    if(l.longValue()!=0) {
                        hVersion.remove(l);
                    }
                }
                // Ok, now we have (hope) clean arrays for the deletion... let us start from CVersion so it easy
                for(Iterator iter=hVersion.iterator();iter.hasNext();) {
                    deleteVersionType(((Long)iter.next()).longValue());
                }

                // step 2: delete all depended CVersionServiceElements->CServiceElements->....
                deleteVersionServiceElementType(hVersionServiceElement);
            }
        } catch(SQLException e) {
            throw new CSIRollbackException("Can't perform SQL query.\n" + e);
        } finally {
            hVersion.clear();
            hVersionServiceElement.clear();

            carefullyClose(rs, stmt); rs=null; stmt=null;
            carefullyClose(null, stmt1); stmt1=null;
        }
        return sourceSystem;
    }

    protected Category getLogger() {
        return DeleteManager.m_logger;
    }
}

