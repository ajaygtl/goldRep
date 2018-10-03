/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/database/RollbackManager.java,v 1.10 2002/12/09 16:35:47 vadim.gritsenko Exp $
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
import com.equant.csi.exceptions.CSINoRollbackVersionException;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.utilities.LoggerFactory;
import org.apache.log4j.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Vector;

/**
 * Handles status changes of existing CVersion objects
 * when ROLLBACK order of any type (NEW, CHANGE, DISCONNECT) is received.
 *
 * @author Vadim Gritsenko
 * @author Vasyl Rublyov
 * @version $Revision: 612 $
 *
 * @see Manager
 * @see CSIConstants
 */
public class RollbackManager extends Manager implements CSIConstants {
    /** Initializing the logger. */
    private static final Category m_logger = LoggerFactory.getInstance(RollbackManager.class.getName());

    /**
     * Constructor to be used in EJB.
     *
     * @param connection the Connection using for database connection
     */
    public RollbackManager(Connection connection) {
        super(connection);
    }

    /**
     * Processes the Rollback order.
     *
     * @param xVersion the JAXB Version object
     *
     * @throws CSIException if can not process the rollback order
     */
    public void rollbackVersion(VersionType xVersion) throws CSIException {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        Vector vVersion = new Vector();
        DeleteManager deleteManager = new DeleteManager(m_connection);

        //
        // Check input parameters
        //
        if (!STATUS_ORDER_ROLLBACK.equals(xVersion.getOrderstatus())) {
            throw new CSIIllegalStateException("RollbackManager handles only orders with status " + STATUS_ORDER_ROLLBACK);
        }

        //
        // Rollback order: Find existing version and update it.
        //
        try {
            stmt = m_connection.prepareStatement(
                      "SELECT VersionID "
                    + "     , LupdDate "
                    + "     , OrderStatus "
                    + "FROM   CVersion "
                    + "WHERE  OrdHandle = ? and OrderType != ? "
            );
            stmt.setString(1, xVersion.getOrderid());
            stmt.setString(2, CSIConstants.ORDER_TYPE_EXISTING);
            rs = stmt.executeQuery();
            while(rs.next()) {
                if(rs.getLong(1)!=0 && rs.getTimestamp(2)!=null) {
                    vVersion.add(new ShortVersion(rs.getLong(1), rs.getTimestamp(2), rs.getString(3)));
                }
            }
            carefullyClose(rs, stmt); rs=null; stmt=null;

            ShortVersion currentVersion = null;
            if (vVersion.size() > 1) {
                // Sort versions by date and remove all old ones
                info("Multiple Version Found for the order ID " + xVersion.getOrderid());
                currentVersion = (ShortVersion)vVersion.get(0);
                for (int i = 1; i < vVersion.size(); i++) {
                    ShortVersion sv = (ShortVersion)vVersion.get(i);
                    if (sv.getLupdDate().after(currentVersion.getLupdDate())) {
                        // Remove stale version
                        info("Removing duplicate version " + currentVersion.getVersionId() + " for the order ID " + xVersion.getOrderid());
                        deleteManager.deleteVersionType(currentVersion.getVersionId());
                        currentVersion = sv;
                    }
                }
            } else if (vVersion.size() == 1) {
                currentVersion = (ShortVersion)vVersion.get(0);
            } else if (vVersion.size() == 0) {
                // Can't process order - no version found.
                throw new CSINoRollbackVersionException("No version found for the order ID " + xVersion.getOrderid() + ". Can not process rollback order");
            }

            // Now we have one version left.
            if (STATUS_ORDER_RELEASE.equals(currentVersion.getOrderStatus())) {
                // Rollback of the Released order.
                deleteManager.deleteVersion(xVersion);
            } else if (STATUS_ORDER_MANAGE.equals(currentVersion.getOrderStatus())) {
                // Rollback of the order in status "Manage"
                rollbackVersion(xVersion, currentVersion, STATUS_ORDER_RELEASE);
            } else if (STATUS_CUSTOMER_ACCEPTS_SERVICE.equals(currentVersion.getOrderStatus())) {
                // Rollback of the order in status "Customer Accepts Service"
                rollbackVersion(xVersion, currentVersion, STATUS_ORDER_MANAGE);
            }
        } catch (SQLException sqlex) {
            throw new CSIRollbackException("Can't perform SQL query.", sqlex);
        } finally {
            vVersion.clear();
            carefullyClose(rs, stmt); rs=null; stmt=null;            
        }
    }

    /**
     * Processes the Rollback order. Change the CURRENT status to INPROGRESS.
     *
     * @param xVersion the JAXB Version object
     * @param cVersion the ShortVersion (internal) Version object
     * @param status
     */
    private void rollbackVersion(VersionType xVersion, ShortVersion cVersion, String status) throws SQLException {
        //
        // Get user who requested operation and date of operation
        //
        ResultSet rs = null;
        PreparedStatement stmt = null;

        try {
            stmt = m_connection.prepareStatement(
                      "UPDATE              "
                    + "   CVersion         "
                    + "SET                 "
                    + "   OrderStatus = ?  "
                    + " , LupdDate = ?     "
                    + " , LupdBy = ?       "
                    + "WHERE               "
                    + "   VersionID = ?    "
            );
            stmt.setString(1, status);
            stmt.setTimestamp(2, TransDate.getTimestamp(xVersion.getOrderSentDate().getTimestamp()));
            stmt.setString(3, xVersion.getSystem().getUser().getName());
            stmt.setLong(4, cVersion.getVersionId());
            stmt.executeUpdate();

            carefullyClose(null, stmt); stmt=null;

            //
            // Go through all CVersionServiceElement of the CVersion
            //
            stmt = m_connection.prepareStatement(
                        "UPDATE                             "
                      + "    CVersionServiceElementSts      "
                      + "SET                                "
                      + "    StatusTypeCode = ?             "
                      + "  , StatusDate = ?                 "
                      + "  , LupdDate = ?                   "
                      + "  , LupdBy = ?                     "
                      + "WHERE                              "
                      + "    VersionServiceElementID in     "
                      + "	    (SELECT                     "
                      + "		    VersionServiceElementID "
                      + "		 FROM                       "
                      + "		 	CVersionServiceElement  "
                      + "		 WHERE                      "
                      + "		    VersionID = ?           "
                      + "		)                           "
                      + "  AND                              "
                      + "   StatusTypeCode in (?,?,?)       "
            );
            stmt.setString(1, STATUS_ORDER_MANAGE.equals(status)? STATUS_CURRENT : STATUS_INPROGRESS);
            stmt.setTimestamp(2, xVersion.getOrderSentDate().getTimestamp());
            stmt.setTimestamp(3, xVersion.getOrderSentDate().getTimestamp());
            stmt.setString(4, xVersion.getSystem().getUser().getName());
            stmt.setLong(5, cVersion.getVersionId());
            stmt.setString(6, STATUS_DISCONNECT);
            stmt.setString(7, STATUS_CURRENT);
            stmt.setString(8, STATUS_ORDER_MANAGE);

            stmt.executeUpdate();
        } finally {
            carefullyClose(rs, stmt); rs=null; stmt=null;
        }
    }

    protected class ShortVersion {
        public long versionId;
        public Timestamp lupdDate;
        public String orderStatus;

        public ShortVersion(long versionId, Timestamp lupdDate, String orderStatus) {
            this.versionId = versionId;
            this.lupdDate = lupdDate;
            this.orderStatus = orderStatus;
        }

        public long getVersionId() {
            return versionId;
        }

        public Timestamp getLupdDate() {
            return lupdDate;
        }

        public String getOrderStatus() {
            return orderStatus;
        }
    }

    protected Category getLogger() {
        return RollbackManager.m_logger;
    }
}
