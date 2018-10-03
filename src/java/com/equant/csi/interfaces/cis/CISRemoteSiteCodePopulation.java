package com.equant.csi.interfaces.cis;

import com.equant.csi.database.QueryManager;
import com.equant.csi.database.UpdateManager;
import com.equant.csi.exceptions.CSIException;
import com.equant.csi.utilities.LoggerFactory;
import com.equant.csi.common.ServiceIdHolder;

import java.sql.SQLException;
import java.sql.Connection;
import java.util.ArrayList;

import org.apache.log4j.Category;

/**
 * Created by IntelliJ IDEA.
 * Date: Sep 17, 2004
 * Time: 12:28:57 PM
 */
public class CISRemoteSiteCodePopulation {
    /** Initializing the logger. */
    private static final Category m_logger = LoggerFactory.getInstance(QueryManager.class.getName());

    public void run(Connection connection) {
        boolean autoCommit = false;

        try {
            QueryManager qm = new QueryManager(connection);
            m_logger.info("CISRemoteSiteCodePopulation: Getting Remote Site Code - Remote Access Connection USID pairs...");
            ArrayList remoteSites = qm.getRemoteSiteUSIDs();
            m_logger.info("CISRemoteSiteCodePopulation: Done retrieving Remote Site Code - Remote Access Connection USID pairs.");

            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            for (int i = 0; remoteSites.size() > i; i++) {
                RemoteSiteCodeInfo rec = (RemoteSiteCodeInfo) remoteSites.get(i);
                m_logger.debug("CISRemoteSiteCodePopulation: Processing record: attributeId=" + rec.attributeId +
                               " USID=" + rec.USID +
                               " VersionServiceElementId=" + rec.versionServiceElementId);
                ServiceIdHolder holder = qm.getCustomerSiteByCISUSID(rec.USID);
                // update Remote Site Code value if it exists or create new record
                if (holder != null) {
                    UpdateManager um = new UpdateManager(connection);
                    m_logger.debug("CISRemoteSiteCodePopulation: Updateing Remote Site Code information");
                    um.updateRemoteSiteCode(rec.versionServiceElementId, rec.attributeId, holder.siteId);
                }

                if (i % 100 == 0 && i != 0) {
                    // commit a transaction every 100 records
                    connection.commit();
                }
                rec = null;
            }

            connection.commit();

        } catch (SQLException e) {
            m_logger.error("SQL Error", e);
        } catch(CSIException e) {
            m_logger.error("CSI error", e);
        } finally {
            if (autoCommit == true) {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException e) {
                    m_logger.error("SQL Error", e);
                }
            }
        }
    }

}
