/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/database/Manager.java,v 1.20 2002/12/05 16:51:41 victoria.ovdenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.database;

import com.equant.csi.common.CSIConstants;
import com.equant.csi.jaxb.ObjectFactory;
import com.equant.csi.jaxb.ServiceElementStatusType;
import com.equant.csi.jaxb.ServiceElementType;
import org.apache.log4j.Category;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;

/**
 * The main manager class that handles status changes of JAXB objects when
 * orders received.
 *
 * @author sherry
 * @author Vadim Gritsenko
 * @author Vasyl Rublyov
 * @version $Revision: 1.20 $
 */
public abstract class Manager {
    /** The DataSource instance. */
    protected Connection m_connection = null;

    /** JAXB Object Factory */
    protected ObjectFactory m_objectFactory = null;

    /**
     * Constructor to create the new Manager instance.
     *
     * @param connection the Connection passed to connect to database
     */
    public Manager(Connection connection) {
        m_connection = connection;
        m_objectFactory = new ObjectFactory();
    }

    /**
     * Checks whether the given element is in specified status.
     *
     * @param element    the given Version Service Element object to check
     * @param statusCode the specified status to check
     * @return true if the given element is in specified status
     */
    protected boolean isInStatus(ServiceElementType element, String statusCode) {
        if (element != null && statusCode != null) {
            Collection statuses = element.getServiceElementStatus();
            if (statuses != null) {
                for (Iterator i = statuses.iterator(); i.hasNext();) {
                    ServiceElementStatusType status = (ServiceElementStatusType) i.next();
                    if (statusCode.equals(status.getStatus())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Retrieves the next value from sequence.
     * @param sequenceName the sequence name to get value from
     * @return valid <code>long</code> value for given sequence
     * @throws SQLException if there was an error or if query didn't returned a value.
     */
    protected long getNextValue(String sequenceName) throws SQLException {
        long result = -1;

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = m_connection.createStatement();
            rs = stmt.executeQuery("select ".concat(sequenceName).concat(".nextval from dual"));
            if (rs.next()) {
                result = rs.getLong(1);
            } else {
                throw new SQLException("Select query for next value from sequence ("+sequenceName+") didn't returned a value.");
            }
        } finally {
            try {if (rs != null) rs.close();} catch (Exception e) {info("ResultSet was not closed succesfully", e);}
            try {if (stmt != null) stmt.close();} catch (Exception e) {info("PreparedStatement was not closed succesfully", e);}
            rs = null;
            stmt = null;
        }

        return result;
    }

    public boolean carefulCompare(String v1, String v2) {
        if(v1==null || v2==null || v1.length()==0 || v2.length()==0) {
            return false;
        }
        return v1.equals(v2);
    }

    protected String getSourceSystem(ServiceElementType se) {
        String defSourceSystem = CSIConstants.SYSTEM_TYPE_GOLD;
        if(se.getSourceSystem()!=null) {
            String tmp = se.getSourceSystem().trim();
            if(tmp.length()>0) defSourceSystem = tmp;
        }
        return defSourceSystem;
    }

    protected void carefullyClose(ResultSet rs, Statement ps) {
        try {
            if(rs!=null) rs.close();
        } catch(Exception e) {
            info("ResultSet was not closed succesfully", e);
        }

        try {
            if (ps != null) ps.close();
        } catch (Exception e) {
            info("Statement was not closed succesfully", e);
        }
    }

    public void debug(String message) {
        if(getLogger().isDebugEnabled()) {
            getLogger().debug(message);
        }
    }

    public void debug(String message, Throwable t) {
        if(getLogger().isDebugEnabled()) {
            getLogger().debug(message, t);
        }
    }

    public void info(String message) {
        if(getLogger().isInfoEnabled()) {
            getLogger().info(message);
        }
    }

    public void info(String message, Throwable t) {
        if(getLogger().isInfoEnabled()) {
            getLogger().info(message, t);
        }
    }

    protected abstract Category getLogger();
}
