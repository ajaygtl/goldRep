/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/database/QueryManager.java,v 1.77 2005/02/14 16:11:39 artem.shevchenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Category;

import com.equant.csi.common.CSIConstants;
import com.equant.csi.common.ServiceIdHolder;
import com.equant.csi.common.TransDate;
import com.equant.csi.exceptions.CSIException;
import com.equant.csi.exceptions.CSIRollbackException;
import com.equant.csi.jaxb.ConflictNoteType;
import com.equant.csi.jaxb.OnceOffChargeType;
import com.equant.csi.jaxb.RecurringChargeType;
import com.equant.csi.jaxb.ServiceElementAttributeType;
import com.equant.csi.jaxb.ServiceElementStatusType;
import com.equant.csi.jaxb.ServiceElementSummaryType;
import com.equant.csi.jaxb.ServiceElementType;
import com.equant.csi.jaxb.ServiceElementUSIDType;
import com.equant.csi.jaxb.ServiceSummaryType;
import com.equant.csi.jaxb.ServicesType;
import com.equant.csi.jaxb.SystemType;
import com.equant.csi.jaxb.TypeType;
import com.equant.csi.jaxb.UsageCharge;
import com.equant.csi.jaxb.UsageChargeType;
import com.equant.csi.jaxb.UserType;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.jaxb.SiteInformationType;
import com.equant.csi.utilities.LoggerFactory;
import com.equant.csi.interfaces.cis.RemoteSiteCodeInfo;

/**
 * Manages queries to the data store. Returns a list of services by
 * site and customer or service details by site, customer, and service id.
 *
 * @author  kiran
 * @author  Vadim Gritsenko
 * @version $Revision: 623 $
 *
 * @see Manager
 * @see CSIConstants
 */
public class QueryManager extends Manager implements CSIConstants {
    /** Initializing the logger. */
    private static final Category m_logger = LoggerFactory.getInstance(QueryManager.class.getName());

    /**
     * Initialises a persistence manager.
     *
     * @param connection the <code>Connection</code> object
     */
    public QueryManager(Connection connection) {
        super(connection);
    }

    /**
     * Returns the version object by given customer ID, site ID,
     * and service name
     *
     * @param customerId  the customer ID
     * @param siteId      the site ID
     * @param serviceName the service name
     * @return the version instance
     */
    public VersionType getServiceDetails(String customerId, String siteId, String coreSiteId, String addressId, String serviceName) throws CSIException {
        VersionType version = null;
        PreparedStatement ps=null;
        ResultSet rs = null;
        if(coreSiteId != null && !"".equals(coreSiteId) && addressId != null && !"".equals(addressId)){
        	try {
        		debug("Get Service Details for " + customerId + "/" + siteId + "/" + serviceName);

        		ps = m_connection.prepareStatement(
        				// Hand crafted query.
        				"SELECT v_custhandle                                                                   " // 1
        				+ "     , v_sitehandle                                                                   " // 2
        				+ "     , v_servicehandle                                                                " // 3
        				+ "     , v_ordhandle                                                                    " // 4
        				+ "     , v_orderstatus                                                                  " // 5
        				+ "     , v_ordertype                                                                    " // 6
        				+ "     , v_ctrhandle                                                                    " // 7
        				+ "     , v_sbahandle                                                                    " // 8
        				+ "     , v_enduserhandle                                                                " // 9
        				+ "     , v_exchangerate                                                                 " // 10
        				+ "     , v_createdate                                                                   " // 11
        				+ "     , vse_versionserviceelementid                                                    " // 12
        				+ "     , vse_lupddate                                                                   " // 13
        				+ "     , vses_statustypecode                                                            " // 14
        				+ "     , se_serviceelementid                                                            " // 15
        				+ "     , se_usid                                                                        " // 16
        				+ "     , se_serviceelementclass                                                         " // 17
        				+ "     , se_serviceelementname                                                          " // 18
        				+ "     , se_description                                                                 " // 19
        				+ "     , se_creationdate                                                                " // 20
        				+ "     , se_grandfatherdate                                                             " // 21
        				+ "     , se_startbillingdate                                                            " // 22
        				+ "     , se_endbillingdate                                                              " // 23
        				+ "     , se_prodid                                                                      " // 24
        				+ "     , u_systemusername                                                               " // 25
        				+ "     , u_systemcode                                                                   " // 26
        				+ "     , i_lupddate                                                                     " // 27
        				+ "     , i_changetypecode                                                               " // 28
        				+ "     , t_serviceattributeid                                                           " // 29
        				+ "     , t_serviceattributename                                                         " // 30
        				+ "     , t_value                                                                        " // 31
        				+ "     , vse_createdate                                                                 " // 32
        				+ "     , vse_sourcesystem                                                               " // 33
        				//customer label
        				+ "     , t_customerlabel			                                                       " // 34
        				+ "     , t_localcurrency			                                                       " // 35
        				+ "     , v_localcurrency                                                                " // 36
        				+ "     , v_localcurrencyexchangerate                                                    " // 37
        				+ "  FROM viw_version_element_attribute v                                                "
        				+ " WHERE v_custhandle = ?                                                               "
        				+ "   AND v_coresiteid = ?                                                               "
        				+ "   AND v_addressid = ?                                                                "
        				+ "   AND v_servicehandle = ?                                                            "
        				+ "   AND se_serviceelementid NOT IN (SELECT DISTINCT se_serviceelementid                "
        				+ "                                  FROM viw_version_element_status vv                  "
        				+ "                                 WHERE v_custhandle = ?                               "
        				+ "                                   AND v_coresiteid = ?                               "
        				+ "                                   AND v_addressid = ?                                "
        				+ "                                   AND v_servicehandle = ?                            "
        				+ "                                   AND vses_statustypecode IN ('Delete', 'Disconnect') "
        				+ "                                   AND vv.se_serviceelementid =  v.se_serviceelementid"
        				+ "                                   AND vv.vse_lupddate >= v.vse_lupddate)             "
        				//          + "   AND vses_statustypecode NOT IN ('InProgress')                                      "
        				+ " ORDER BY se_serviceelementid, vse_createdate DESC, i_lupddate DESC                   "
        		);
        		ps.setString(1, customerId);
        		ps.setString(2, coreSiteId);
        		ps.setString(3, addressId);
        		ps.setString(4, serviceName);
        		ps.setString(5, customerId);
        		ps.setString(6, coreSiteId);
        		ps.setString(7, addressId);
        		ps.setString(8, serviceName);

        		rs = ps.executeQuery();

        		version = constructVersionFromResultSet(version, rs, false);


        	} catch (SQLException e) {
        		info("Can't perform SQL query.", e);
        		throw new CSIRollbackException("Can't perform SQL query.", e);
        	} catch (JAXBException e) {
        		info("Can't instantiate 'ServiceElement' class.", e);
        		throw new CSIRollbackException("Version received from database but not initialized through JAXB exception.", e);
        	} finally {
        		carefullyClose(rs, ps); rs = null; ps = null;
        	}
        }
        if(version == null){
        	try {
        		debug("Get Service Details for " + customerId + "/" + siteId + "/" + serviceName);

        		ps = m_connection.prepareStatement(
        				// Hand crafted query.
        				"SELECT v_custhandle                                                                   " // 1
        				+ "     , v_sitehandle                                                                   " // 2
        				+ "     , v_servicehandle                                                                " // 3
        				+ "     , v_ordhandle                                                                    " // 4
        				+ "     , v_orderstatus                                                                  " // 5
        				+ "     , v_ordertype                                                                    " // 6
        				+ "     , v_ctrhandle                                                                    " // 7
        				+ "     , v_sbahandle                                                                    " // 8
        				+ "     , v_enduserhandle                                                                " // 9
        				+ "     , v_exchangerate                                                                 " // 10
        				+ "     , v_createdate                                                                   " // 11
        				+ "     , vse_versionserviceelementid                                                    " // 12
        				+ "     , vse_lupddate                                                                   " // 13
        				+ "     , vses_statustypecode                                                            " // 14
        				+ "     , se_serviceelementid                                                            " // 15
        				+ "     , se_usid                                                                        " // 16
        				+ "     , se_serviceelementclass                                                         " // 17
        				+ "     , se_serviceelementname                                                          " // 18
        				+ "     , se_description                                                                 " // 19
        				+ "     , se_creationdate                                                                " // 20
        				+ "     , se_grandfatherdate                                                             " // 21
        				+ "     , se_startbillingdate                                                            " // 22
        				+ "     , se_endbillingdate                                                              " // 23
        				+ "     , se_prodid                                                                      " // 24
        				+ "     , u_systemusername                                                               " // 25
        				+ "     , u_systemcode                                                                   " // 26
        				+ "     , i_lupddate                                                                     " // 27
        				+ "     , i_changetypecode                                                               " // 28
        				+ "     , t_serviceattributeid                                                           " // 29
        				+ "     , t_serviceattributename                                                         " // 30
        				+ "     , t_value                                                                        " // 31
        				+ "     , vse_createdate                                                                 " // 32
        				+ "     , vse_sourcesystem                                                               " // 33
        				//customer label
        				+ "     , t_customerlabel			                                                       " // 34
        				+ "     , t_localcurrency			                                                       " // 35
        				+ "     , v_localcurrency                                                                " // 36
        				+ "     , v_localcurrencyexchangerate                                                    " // 37
        				+ "  FROM viw_version_element_attribute v                                                "
        				+ " WHERE v_custhandle = ?                                                               "
        				+ "   AND v_sitehandle = ?                                                               "
        				+ "   AND v_servicehandle = ?                                                            "
        				+ "   AND se_serviceelementid NOT IN (SELECT DISTINCT se_serviceelementid                "
        				+ "                                  FROM viw_version_element_status vv                  "
        				+ "                                 WHERE v_custhandle = ?                               "
        				+ "                                   AND v_sitehandle = ?                               "
        				+ "                                   AND v_servicehandle = ?                            "
        				+ "                                   AND vses_statustypecode IN ('Delete', 'Disconnect') "
        				+ "                                   AND vv.se_serviceelementid =  v.se_serviceelementid"
        				+ "                                   AND vv.vse_lupddate >= v.vse_lupddate)             "
        				//          + "   AND vses_statustypecode NOT IN ('InProgress')                                      "
        				+ " ORDER BY se_serviceelementid, vse_createdate DESC, i_lupddate DESC                   "
        		);
        		ps.setString(1, customerId);
        		ps.setString(2, siteId);
        		ps.setString(3, serviceName);
        		ps.setString(4, customerId);
        		ps.setString(5, siteId);
        		ps.setString(6, serviceName);

        		rs = ps.executeQuery();

        		version = constructVersionFromResultSet(version, rs, false);

        	} catch (SQLException e) {
        		info("Can't perform SQL query.", e);
        		throw new CSIRollbackException("Can't perform SQL query.", e);
        	} catch (JAXBException e) {
        		info("Can't instantiate 'ServiceElement' class.", e);
        		throw new CSIRollbackException("Version received from database but not initialized through JAXB exception.", e);
        	} finally {
        		carefullyClose(rs, ps); rs = null; ps = null;
        	}
        }
        
        return version;
    }

    /**
     * Retrieves the <code>OnceOffCharge</code> object by given element ID.
     *
     * @param elementID the element ID
     * @return the <code>OnceOffCharge</code> object
     */
    public OnceOffChargeType getSEOnceOffCharge(long elementID) throws CSIException {
        debug("Get OnceOffCharge for ServiceElement <" + elementID + ">");

        OnceOffChargeType res = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = m_connection.prepareStatement(
                    "SELECT ooc_amount                         "
                  + "     , ooc_chgcatid                       "
                  + "     , ooc_disccode                       "
                  + "     , cci_change_code                    "
                  + "FROM viw_charge                           "
                  + "WHERE NVL(sa_id, 0) = 0                   "
                  + "  AND rc_amount IS NULL                   "
                  + "  AND NVL(uc_chgcatid, 'empty') = 'empty' "
                  + "  AND vse_id = ?                          "
            );
            ps.setLong(1, elementID);

            rs = ps.executeQuery();
            if (rs.next()) {
                res = m_objectFactory.createOnceOffCharge();
                res.setAmount(rs.getDouble(1));
                res.setChargeCategoryId(rs.getString(2));
                res.setDiscountCode(rs.getString(3));
                res.setChangeTypeCode(rs.getString(4));

                debug("Found OnceOffCharge (CategoryId=" + res.getChargeCategoryId() + ", TypeCode=" + res.getChangeTypeCode() + ")");
            }
        } catch (SQLException e) {
            info("Can't retrieve 'OnceOffCharge' for the ServiceElement " + elementID, e);
            throw new CSIRollbackException("Can't retrieve 'OnceOffCharge' for the ServiceElement " + elementID, e);
        } catch (JAXBException e) {
            info("Can't create 'OnceOffCharge' object for the ServiceElement " + elementID, e);
            throw new CSIRollbackException("Can't create 'OnceOffCharge' object for the ServiceElement " + elementID, e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }

        return res;
    }

    /**
     * Retrieves the <code>RecurringCharge</code> object by given element ID.
     *
     * @param elementID the element ID
     * @return the <code>RecurringCharge</code> object
     */
    public RecurringChargeType getSERecurringCharge(long elementID) throws CSIException {
        debug("Get RecurringCharge for ServiceElement <" + elementID + ">");

        RecurringChargeType res = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = m_connection.prepareStatement(
                    "SELECT rc_amount                          "
                  + "     , rc_chgcatid                        "
                  + "     , rc_disccode                        "
                  + "     , cci_change_code                    "
                  + "FROM viw_charge                           "
                  + "WHERE NVL(sa_id, 0) = 0                   "
                  + "  AND ooc_amount IS NULL                  "
                  + "  AND NVL(uc_chgcatid, 'empty') = 'empty' "
                  + "  AND vse_id = ?                          "
            );
            ps.setLong(1, elementID);

            rs = ps.executeQuery();
            if (rs.next()) {
                res = m_objectFactory.createRecurringCharge();
                res.setAmount(rs.getDouble(1));
                res.setChargeCategoryId(rs.getString(2));
                res.setDiscountCode(rs.getString(3));
                res.setChangeTypeCode(rs.getString(4));

                debug("Found RecurringCharge (CategoryId=" + res.getChargeCategoryId() + ", TypeCode=" + res.getChangeTypeCode() + ")");
            }
        } catch (SQLException e) {
            info("Can't retrieve 'RecurringCharge' for the ServiceElement " + elementID, e);
            throw new CSIRollbackException("Can't retrieve 'RecurringCharge' for the ServiceElement " + elementID, e);
        } catch (JAXBException e) {
            info("Can't create 'RecurringCharge' object for the ServiceElement " + elementID, e);
            throw new CSIRollbackException("Can't create 'RecurringCharge' object for the ServiceElement " + elementID, e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }

        return res;
    }

    /**
     * Retrieves the <code>UsageCharge</code> object by given element ID.
     *
     * @param elementID the element ID
     * @return the <code>UsageCharge</code> object
     */
    public UsageChargeType getSEUsageCharge(long elementID) throws CSIException {
        debug("Get UsageCharge for ServiceElement <" + elementID + ">");

        UsageCharge res = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = m_connection.prepareStatement(
                    "SELECT uc_chgcatid           "
                  + "     , cci_change_code       "
                  + "FROM viw_charge              "
                  + "WHERE NVL(sa_id, 0) = 0      "
                  + "  AND ooc_amount IS NULL     "
                  + "  AND rc_amount IS NULL      "
                  + "  AND vse_id = ?             "
            );
            ps.setLong(1, elementID);

            rs = ps.executeQuery();
            if (rs.next()) {
                res = m_objectFactory.createUsageCharge();
                res.setChargeCategoryId(rs.getString(1));
                res.setChangeTypeCode(rs.getString(2));

                debug("Found UsageCharge (CategoryId=" + res.getChargeCategoryId() + ", TypeCode=" + res.getChangeTypeCode() + ")");
            }
        } catch (SQLException e) {
            info("Can't retrieve 'UsageCharge' for the ServiceElement " + elementID, e);
            throw new CSIRollbackException("Can't retrieve 'UsageCharge' for the ServiceElement " + elementID, e);
        } catch (JAXBException e) {
            info("Can't create 'UsageCharge' object for the ServiceElement " + elementID, e);
            throw new CSIRollbackException("Can't create 'UsageCharge' object for the ServiceElement " + elementID, e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }

        return res;
    }


    /**
     * Retrieves the <code>OnceOffCharge</code> object by given attribute ID.
     *
     * @param attributeID the attribute ID
     * @return the <code>OnceOffCharge</code> object
     */
    public OnceOffChargeType getSEAOnceOffCharge(long attributeID) throws CSIException {
        debug("Get OnceOffCharge for ServiceElementAttribute <" + attributeID + ">");

        OnceOffChargeType res = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = m_connection.prepareStatement(
                    "SELECT ooc_amount                         "
                  + "     , ooc_chgcatid                       "
                  + "     , ooc_disccode                       "
                  + "     , cci_change_code                    "
                  + "FROM viw_charge                           "
                  + "WHERE rc_amount IS NULL                   "
                  + "  AND NVL(uc_chgcatid, 'empty') = 'empty' "
                  + "  AND sa_id = ?                           "
            );
            ps.setLong(1, attributeID);

            rs = ps.executeQuery();
            if (rs.next()) {
                res = m_objectFactory.createOnceOffCharge();
                res.setAmount(rs.getDouble(1));
                res.setChargeCategoryId(rs.getString(2));
                res.setDiscountCode(rs.getString(3));
                res.setChangeTypeCode(rs.getString(4));

                debug("Found OnceOffCharge (CategoryId=" + res.getChargeCategoryId() + ", TypeCode=" + res.getChangeTypeCode() + ")");
            }
        } catch (SQLException e) {
            info("Can't retrieve 'OnceOffCharge' for the Attribute " + attributeID, e);
            throw new CSIRollbackException("Can't retrieve 'OnceOffCharge' for the Attribute " + attributeID, e);
        } catch (JAXBException e) {
            info("Can't create 'OnceOffCharge' object for the Attribute " + attributeID, e);
            throw new CSIRollbackException("Can't create 'OnceOffCharge' object for the Attribute " + attributeID, e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }

        return res;
    }

    /**
     * Retrieves the <code>RecurringCharge</code> object by given attribute ID.
     *
     * @param attributeID the attribute ID
     * @return the <code>RecurringCharge</code> object
     */
    public RecurringChargeType getSEARecurringCharge(long attributeID) throws CSIException {
        debug("Get RecurringCharge for ServiceElementAttribute <" + attributeID + ">");

        RecurringChargeType res = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = m_connection.prepareStatement(
                    "SELECT rc_amount                          "
                  + "     , rc_chgcatid                        "
                  + "     , rc_disccode                        "
                  + "     , cci_change_code                    "
                  + "FROM viw_charge                           "
                  + "WHERE ooc_amount IS NULL                  "
                  + "  AND NVL(uc_chgcatid, 'empty') = 'empty' "
                  + "  AND sa_id = ?                           "
            );
            ps.setLong(1, attributeID);

            rs = ps.executeQuery();
            if (rs.next()) {
                res = m_objectFactory.createRecurringCharge();
                res.setAmount(rs.getDouble(1));
                res.setChargeCategoryId(rs.getString(2));
                res.setDiscountCode(rs.getString(3));
                res.setChangeTypeCode(rs.getString(4));

                debug("Found RecurringCharge (CategoryId=" + res.getChargeCategoryId() + ", TypeCode=" + res.getChangeTypeCode() + ")");
            }
        } catch (SQLException e) {
            info("Can't retrieve 'OnceOffCharge' for the Attribute " + attributeID, e);
            throw new CSIRollbackException("Can't retrieve 'OnceOffCharge' for the Attribute " + attributeID, e);
        } catch (JAXBException e) {
            info("Can't create 'OnceOffCharge' object for the Attribute " + attributeID, e);
            throw new CSIRollbackException("Can't create 'OnceOffCharge' object for the Attribute " + attributeID, e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }

        return res;
    }

    /**
     * Retrieves the UsageCharge object by given attribute ID.
     *
     * @param attributeID the attribute ID
     * @return the <code>UsageCharge</code> object
     */
    public UsageCharge getSEAUsageCharge(long attributeID) throws CSIException {
        debug("Get UsageCharge for ServiceElementAttribute <" + attributeID + ">");

        UsageCharge res = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = m_connection.prepareStatement(
                    "SELECT uc_chgcatid"
                  + "     , cci_change_code "
                  + "FROM viw_charge "
                  + "WHERE ooc_amount IS NULL "
                  + "  AND rc_amount IS NULL "
                  + "  AND sa_id = ? "
            );
            ps.setLong(1, attributeID);

            rs = ps.executeQuery();
            if (rs.next()) {
                res = m_objectFactory.createUsageCharge();
                res.setChargeCategoryId(rs.getString(1));
                res.setChangeTypeCode(rs.getString(2));
                debug("Found UsageCharge (CategoryId=" + res.getChargeCategoryId() + ", TypeCode=" + res.getChangeTypeCode() + ")");
            }
        } catch (SQLException e) {
            info("Can't retrieve 'UsageCharge' for the Attribute " + attributeID, e);
            throw new CSIRollbackException("Can't retrieve 'UsageCharge' for the Attribute " + attributeID, e);
        } catch (JAXBException e) {
            info("Can't create 'UsageCharge' object for the Attribute " + attributeID, e);
            throw new CSIRollbackException("Can't create 'UsageCharge' object for the Attribute " + attributeID, e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }

        return res;
    }


    /**
     * Returns all the service elements, for a specified parameter
     * type and value (e.g., "site" and 1562, where 1562 is a site id).
     *
     * @param customerId the customer ID
     * @param siteId     the site ID
     * @return vector of service names for this customer, site combination.
     */
    public Vector getServices(String customerId, String siteId, String coreSiteId, String addressId) throws CSIRollbackException {
        Vector services = new Vector();

        debug("Get Services for " + customerId + "/" + siteId + "/" + coreSiteId + "/" + addressId);

        PreparedStatement ps = null;
        ResultSet rs = null;
        
        if(coreSiteId != null && !"".equals(coreSiteId) && addressId != null && !"".equals(addressId)){
        	try {
        		ps = m_connection.prepareStatement(
        				"SELECT DISTINCT v_servicehandle                                                                         "
        				+ "FROM viw_version_element_attribute v                                                                  "
        				+ "WHERE v_custhandle = ?                                                                                "
        				+ "  AND v_coresiteid = ?                                                                                "
        				+ "  AND v_addressid = ?                                                                                 "
        				+ "  AND se_serviceelementid NOT IN (SELECT DISTINCT se_serviceelementid                                 "
        				+ "                                  FROM viw_version_element_status vv                   "
        				+ "                                  WHERE v_custhandle = ?                               "
        				+ "                                   AND v_coresiteid = ?                                "
        				+ "                                   AND v_addressid = ?                                 "
        				+ "                                   AND vses_statustypecode IN ('Delete', 'Disconnect') "
        				+ "                                   AND vv.se_serviceelementid =  v.se_serviceelementid "
        				+ "                                   AND vv.vse_lupddate >= v.vse_lupddate)              "
        		);
        		ps.setString(1, customerId);
        		ps.setString(2, coreSiteId);
        		ps.setString(3, addressId);
        		ps.setString(4, customerId);
        		ps.setString(5, coreSiteId);
        		ps.setString(6, addressId);

        		rs = ps.executeQuery();
        		while (rs.next()) {
        			String serviceHandler = rs.getString(1);
        			debug("Found Service <" + serviceHandler + ">");
        			services.add(serviceHandler);
        		}
        	} catch (SQLException e) {
        		info("Can't retrieve 'servicehandle' field.", e);
        		throw new CSIRollbackException("Can't retrieve 'servicehandle' field.", e);
        	} finally {
        		carefullyClose(rs, ps); rs = null; ps = null;
        	}
        }
        
        if(services.size() == 0){
        	try {
        		ps = m_connection.prepareStatement(
        				"SELECT DISTINCT v_servicehandle                                                                       "
        				+ "FROM viw_version_element_attribute v                                                                                       "
        				+ "WHERE v_custhandle = ?                                                                                "
        				+ "  AND v_sitehandle = ?                                                                                "
        				+ "  AND se_serviceelementid NOT IN (SELECT DISTINCT se_serviceelementid                                 "
        				+ "                                  FROM viw_version_element_status vv                  "
        				+ "                                  WHERE v_custhandle = ?                               "
        				+ "                                   AND v_sitehandle = ?                               "
        				+ "                                   AND vses_statustypecode IN ('Delete', 'Disconnect') "
        				+ "                                   AND vv.se_serviceelementid =  v.se_serviceelementid"
        				+ "                                   AND vv.vse_lupddate >= v.vse_lupddate)             "
        		);
        		ps.setString(1, customerId);
        		ps.setString(2, siteId);
        		ps.setString(3, customerId);
        		ps.setString(4, siteId);

        		rs = ps.executeQuery();
        		while (rs.next()) {
        			String serviceHandler = rs.getString(1);
        			debug("Found Service <" + serviceHandler + ">");
        			services.add(serviceHandler);
        		}
        	} catch (SQLException e) {
        		info("Can't retrieve 'servicehandle' field.", e);
        		throw new CSIRollbackException("Can't retrieve 'servicehandle' field.", e);
        	} finally {
        		carefullyClose(rs, ps); rs = null; ps = null;
        	}
        }
        return services;
    }

    /**
     * Returns the list of sites by given customer ID, and service name
     *
     * @param customerId  the customer ID
     * @param serviceName the service name
     * @return collection of Local Sites
     */
    public Collection getSites(String customerId, String serviceName) throws CSIException {
        Collection col = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            debug("Site searching for customer: " + customerId + ", and service:" + serviceName);

            // retrive list of sites
            ps = m_connection.prepareStatement(
                    "SELECT DISTINCT cversion.sitehandle                            "
                  + "FROM cversion, cversionserviceelement                          "
                  + "WHERE cversionserviceelement.versionid = cversion.versionid    "
                  + "  AND cversion.custhandle = ?                                  "
                  + "  AND cversion.servicehandle = ?                               "
            );
            ps.setString(1, customerId);
            ps.setString(2, serviceName);

            rs = ps.executeQuery();

            while (rs.next()) {
                col.add(rs.getString(1));
            }
        } catch (SQLException e) {
            info("Can't retrieve sites.", e);
            throw new CSIRollbackException("Can't retrieve sites.", e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }

        return col;
    }

    /**
     * Returns the version object by given customer ID, and service name (CSP)
     *
     * @param customerId  the customer ID
     * @param serviceName the service name
     * @return the version instance
     */
    public VersionType getCSPServiceDetails(String customerId, String serviceName) throws CSIException {
        VersionType version = null;

        PreparedStatement ps = null;
        ResultSet rs = null;

        String cspServiceElement = "CSP_" + customerId + ": " + serviceName;
        try {
            debug("Get CSP Service Details for " + customerId + "/" + cspServiceElement);

            // checking if the customer has CSP service
            ps = m_connection.prepareStatement(
                      "SELECT DISTINCT v_servicehandle                                                                       "
                    + "FROM viw_version_element_attribute v                                                                                       "
                    + " WHERE v_custhandle = ?                                                                                "
                    + " AND v_servicehandle = ?                                                                                "
                    + " AND se_serviceelementid NOT IN (SELECT DISTINCT se_serviceelementid                                 "
                    + "                                  FROM viw_version_element_status vv                  "
                    + "                                  WHERE v_custhandle = ?                               "
                    + "                                   AND v_servicehandle = ?                               "
                    + "                                   AND vses_statustypecode IN ('Delete', 'Disconnect') "
                    + "                                   AND vv.se_serviceelementid =  v.se_serviceelementid"
                    + "                                   AND vv.vse_lupddate >= v.vse_lupddate)                                "
            );
            ps.setString(1, customerId);
            ps.setString(2, CSIConstants.CONFIG_CSP_PURPLE);
            ps.setString(3, customerId);
            ps.setString(4, CSIConstants.CONFIG_CSP_PURPLE);

            rs = ps.executeQuery();
            boolean hasCSP = rs.next();

            carefullyClose(rs, ps); rs = null; ps = null;

            if(!hasCSP) return null;

            // CSP Service Handle = CSIConstants.CONFIG_CSP_PURPLE
            // CSP Sub-Service Handle (aka Service Element USID) = "CSP_" + customerId + ": " + serviceName

            ps = m_connection.prepareStatement(
                  // Hand crafted query.
                    "SELECT v_custhandle                                                                   " // 1
                  + "     , v_sitehandle                                                                   " // 2
                  + "     , v_servicehandle                                                                " // 3
                  + "     , v_ordhandle                                                                    " // 4
                  + "     , v_orderstatus                                                                  " // 5
                  + "     , v_ordertype                                                                    " // 6
                  + "     , v_ctrhandle                                                                    " // 7
                  + "     , v_sbahandle                                                                    " // 8
                  + "     , v_enduserhandle                                                                " // 9
                  + "     , v_exchangerate                                                                 " // 10
                  + "     , v_createdate                                                                   " // 11
                  + "     , vse_versionserviceelementid                                                    " // 12
                  + "     , vse_lupddate                                                                   " // 13
                  + "     , vses_statustypecode                                                            " // 14
                  + "     , se_serviceelementid                                                            " // 15
                  + "     , se_usid                                                                        " // 16
                  + "     , se_serviceelementclass                                                         " // 17
                  + "     , se_serviceelementname                                                          " // 18
                  + "     , se_description                                                                 " // 19
                  + "     , se_creationdate                                                                " // 20
                  + "     , se_grandfatherdate                                                             " // 21
                  + "     , se_startbillingdate                                                            " // 22
                  + "     , se_endbillingdate                                                              " // 23
                  + "     , se_prodid                                                                      " // 24
                  + "     , u_systemusername                                                               " // 25
                  + "     , u_systemcode                                                                   " // 26
                  + "     , i_lupddate                                                                     " // 27
                  + "     , i_changetypecode                                                               " // 28
                  + "     , t_serviceattributeid                                                           " // 29
                  + "     , t_serviceattributename                                                         " // 30
                  + "     , t_value                                                                        " // 31
                  + "     , vse_createdate                                                                 " // 32
                  + "     , vse_sourcesystem                                                               " // 33
                  //customer label
                  + "     , t_customerlabel                                                                " // 34
                  + "     , t_localcurrency                                                                " // 35
                  + "     , v_localcurrency                                                                " // 36
                  + "     , v_localcurrencyexchangerate                                                    " // 37
                  
                  + "  FROM viw_version_element_attribute v                                                "
                  + " WHERE v_custhandle = ?                                                               "
                  + "   AND v_servicehandle = ?                                                            "
                  + "   AND se_usid = ?                                                                    "
                  + "   AND se_serviceelementid NOT IN (SELECT DISTINCT se_serviceelementid                                 "
                  + "                                  FROM viw_version_element_status vv                  "
                  + "                                  WHERE v_custhandle = ?                               "
                  + "                                   AND v_sitehandle = ?                               "
                  + "                                   AND se_usid = ?                               "
                  + "                                   AND vses_statustypecode IN ('Delete', 'Disconnect') "
                  + "                                   AND vv.se_serviceelementid =  v.se_serviceelementid"
                  + "                                   AND vv.vse_lupddate >= v.vse_lupddate)              "
//                  + "   AND vses_statustypecode NOT IN ('InProgress')                                      "
                  + " ORDER BY se_serviceelementid, vse_createdate DESC, i_lupddate DESC                   "
            );
            ps.setString(1, customerId);
            ps.setString(2, CSIConstants.CONFIG_CSP_PURPLE);
            ps.setString(3, cspServiceElement);
            ps.setString(4, customerId);
            ps.setString(5, CSIConstants.CONFIG_CSP_PURPLE);
            ps.setString(6, cspServiceElement);

            rs = ps.executeQuery();
            version = constructVersionFromResultSet(version, rs, false);
            if(version!=null) {
                version.setCustomerId(customerId);
                version.setSiteId("GLOBAL");
                version.setServiceId(CSIConstants.CONFIG_CSP_PURPLE);
            }
        } catch (SQLException e) {
            info("Can't perform query in getCSPServiceDetails(\"" + customerId + "\", \"" + serviceName + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getCSPServiceDetails(\"" + customerId + "\", \"" + serviceName + "\", ...)", e);
        } catch (JAXBException e) {
            info("Can't instantiate 'ServiceElement' class in getCSPServiceDetails(\"" + customerId + "\", \"" + serviceName + "\", ...)", e);
            throw new CSIRollbackException("Can't instantiate 'ServiceElement' class in getCSPServiceDetails(\"" + customerId + "\", \"" + serviceName + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }
        return version;
    }
    
    public long getLatestVersionServiceElementIDByUSID(String usid) throws CSIException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        long result=-1;
        
        try {
            debug("Get latest VersionServiceElementID for " + usid);
            ps = m_connection.prepareStatement(
                  // Hand crafted query.
                  " Select MAX(vse_versionserviceelementid) " +
                        "FROM viw_version_element_attribute  " +
                        "WHERE se_usid = ?                   "
            );

            ps.setString(1, usid);
            rs = ps.executeQuery();
            if(rs.next()) {
                result=rs.getInt(1);
            }
        } catch (SQLException e) {
            info("Can't perform query in getLatestVersionServiceElementIDByUSID(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getLatestVersionServiceElementIDByUSID(\"" + usid + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }
        return result;        
    }

    public VersionType getLatestVersionServiceElementByUSID(String usid) throws CSIException {
        VersionType version = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        long vseId=getLatestVersionServiceElementIDByUSID(usid);    
        
        if(vseId==-1) {
            return null;            
        }
        try {
            debug("Get latest Version/ServiceElement Details for " + usid);
            ps = m_connection.prepareStatement(
                  // Hand crafted query.
                    "SELECT v_custhandle                                                                   " // 1
                  + "     , v_sitehandle                                                                   " // 2
                  + "     , v_servicehandle                                                                " // 3
                  + "     , v_ordhandle                                                                    " // 4
                  + "     , v_orderstatus                                                                  " // 5
                  + "     , v_ordertype                                                                    " // 6
                  + "     , v_ctrhandle                                                                    " // 7
                  + "     , v_sbahandle                                                                    " // 8
                  + "     , v_enduserhandle                                                                " // 9
                  + "     , v_exchangerate                                                                 " // 10
                  + "     , v_createdate                                                                   " // 11
                  + "     , vse_versionserviceelementid                                                    " // 12
                  + "     , vse_lupddate                                                                   " // 13
                  + "     , vses_statustypecode                                                            " // 14
                  + "     , se_serviceelementid                                                            " // 15
                  + "     , se_usid                                                                        " // 16
                  + "     , se_serviceelementclass                                                         " // 17
                  + "     , se_serviceelementname                                                          " // 18
                  + "     , se_description                                                                 " // 19
                  + "     , se_creationdate                                                                " // 20
                  + "     , se_grandfatherdate                                                             " // 21
                  + "     , se_startbillingdate                                                            " // 22
                  + "     , se_endbillingdate                                                              " // 23
                  + "     , se_prodid                                                                      " // 24
                  + "     , u_systemusername                                                               " // 25
                  + "     , u_systemcode                                                                   " // 26
                  + "     , i_lupddate                                                                     " // 27
                  + "     , i_changetypecode                                                               " // 28
                  + "     , t_serviceattributeid                                                           " // 29
                  + "     , t_serviceattributename                                                         " // 30
                  + "     , t_value                                                                        " // 31
                  + "     , vse_createdate                                                                 " // 32
                  + "     , vse_sourcesystem                                                               " // 33
                  //customer label
                  + "     , t_customerlabel                                                                " // 34
                  + "     , t_localcurrency                                                                " // 35
                  + "     , v_localcurrency                                                                " // 36
                  + "     , v_localcurrencyexchangerate                                                    " // 37
                  + "  FROM viw_version_element_attribute v                                                "
                  + " WHERE se_usid = ?                                                                    "
                  + " AND  vse_versionserviceelementid = ?                                                 "
            );

            ps.setString(1, usid);
            ps.setLong(2, vseId);
            rs = ps.executeQuery();
            version = constructVersionFromResultSet(version, rs, false);
        } catch (SQLException e) {
            info("Can't perform query in getLatestVersionServiceElementByUSID(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getLatestVersionServiceElementByUSID(\"" + usid + "\", ...)", e);
        } catch (JAXBException e) {
            info("Can't instantiate 'ServiceElement' class in getLatestVersionServiceElementByUSID(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't instantiate 'ServiceElement' class in getLatestVersionServiceElementByUSID(\"" + usid + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }
        return version;
        
    }

    /**
     * Returns the version object by given USID (contains only single ServiceElement Object)
     *
     * @param usid  the Service Element USID
     * @return the version instance
     */
    public List getAllActiveVersionServiceElementIDsByUSID( String usid) throws CSIException {
        List result = new ArrayList ();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = m_connection.prepareStatement(
                    "SELECT DISTINCT v.vse_versionserviceelementid"
                  + "  FROM viw_version_element_attribute v"
                  + "  WHERE v.se_usid = ?"
                  + "    AND v.vses_statustypecode IN ('InProgress', 'Current')"
                  + "  ORDER BY v.vse_versionserviceelementid "
            );

            ps.setString(1, usid);
            rs = ps.executeQuery();

            while(rs.next()) {
                result.add( rs.getString(1));
            }

        } catch (SQLException e) {
            info("Can't perform query in getAllVersionServiceElementIDsByUSID(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getAllVersionServiceElementIDsByUSID(\"" + usid + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }
        return result;
        
    }

    /**
     * Retrieves list of active service elements for the same Customer, Site 
     * and service as has specified service options.
     * 
     * @param serviceOptinId Service option USID
     * @return list of USIDs for active service components 
     * @throws CSIException wrapper for SQLException
     */
    public List getActiveServiceElements( String serviceOptinId) throws CSIException {

        PreparedStatement ps = null;
        ResultSet rs = null;

        String custId = null;
        String siteId = null;
        String serviceId = null;
        try {
            ps = m_connection.prepareStatement(
                "SELECT DISTINCT v_custhandle, v_sitehandle, v_servicehandle " +
                "FROM viw_version_element_attribute " +
                "WHERE se_usid=?"
            );

            ps.setString( 1, serviceOptinId);
            rs = ps.executeQuery();

            if(rs.next()) {
                custId = rs.getString( 1);
                siteId = rs.getString( 2);
                serviceId = rs.getString( 3);
            } else {
                return Collections.EMPTY_LIST;
            }

            if(rs.next()) {
                throw new CSIRollbackException( "Exists more than one set " +
                    "CustomerId, SiteId and product ID for service option:" + serviceOptinId);
            }
        } catch (SQLException e) {
            info("Can't perform query in getActiveServiceElements(\"" + serviceOptinId + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getAllVersionServiceElementIDsByUSID(\"" + serviceOptinId + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }

        return getActiveServiceElements( custId, siteId, serviceId);
    }

    /**
     * Retrieves list of active service elements for the specified Customer, Site
     * and service.
     * 
     * @param customerId Customer ID
     * @param siteId Site ID
     * @param serviceId Service ID
     * @return list of USIDs for active service components 
     * @throws CSIException wrapper for SQLException
     */
    public List getActiveServiceElements( String customerId, String siteId,
            String serviceId) throws CSIException {

        List result = new ArrayList ();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = m_connection.prepareStatement(
                "select max(i_lupddate), se_usid " +
                "from viw_version_element_attribute " +
                "WHERE v_custhandle=? AND v_sitehandle=? AND v_servicehandle=? " +
                    "AND vses_statustypecode IN('Current' ,'InProgress') AND se_usid NOT LIKE 'SO::%' " +
                    "GROUP BY vses_statustypecode, se_usid"
            );

            ps.setString( 1, customerId);
            ps.setString( 2, siteId);
            ps.setString( 3, serviceId);
            rs = ps.executeQuery();

            while(rs.next()) {
                result.add( rs.getString(2));
            }

        } catch (SQLException e) {
            info("Can't perform query in getActiveServiceElements(\"" + customerId +
                    ", " + siteId + ", " + serviceId + "\", ...)", e);
            throw new CSIRollbackException( "Can't perform query in getAllVersionServiceElementIDsByUSID(\""
                + customerId + ", " + siteId + ", " + serviceId +  "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }

        return result;
    }
    
    /**
     * Returns the version object by given USID (contains only single ServiceElement Object)
     *
     * @param usid  the Service Element USID
     * @return the version instance
     */
    public VersionType getVersionServiceElementByUSID(String usid) throws CSIException {
        VersionType version = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            debug("Get Version/ServiceElement Details for " + usid);
            ps = m_connection.prepareStatement(
                  // Hand crafted query.
                    "SELECT v_custhandle                                                                   " // 1
                  + "     , v_sitehandle                                                                   " // 2
                  + "     , v_servicehandle                                                                " // 3
                  + "     , v_ordhandle                                                                    " // 4
                  + "     , v_orderstatus                                                                  " // 5
                  + "     , v_ordertype                                                                    " // 6
                  + "     , v_ctrhandle                                                                    " // 7
                  + "     , v_sbahandle                                                                    " // 8
                  + "     , v_enduserhandle                                                                " // 9
                  + "     , v_exchangerate                                                                 " // 10
                  + "     , v_createdate                                                                   " // 11
                  + "     , vse_versionserviceelementid                                                    " // 12
                  + "     , vse_lupddate                                                                   " // 13
                  + "     , vses_statustypecode                                                            " // 14
                  + "     , se_serviceelementid                                                            " // 15
                  + "     , se_usid                                                                        " // 16
                  + "     , se_serviceelementclass                                                         " // 17
                  + "     , se_serviceelementname                                                          " // 18
                  + "     , se_description                                                                 " // 19
                  + "     , se_creationdate                                                                " // 20
                  + "     , se_grandfatherdate                                                             " // 21
                  + "     , se_startbillingdate                                                            " // 22
                  + "     , se_endbillingdate                                                              " // 23
                  + "     , se_prodid                                                                      " // 24
                  + "     , u_systemusername                                                               " // 25
                  + "     , u_systemcode                                                                   " // 26
                  + "     , i_lupddate                                                                     " // 27
                  + "     , i_changetypecode                                                               " // 28
                  + "     , t_serviceattributeid                                                           " // 29
                  + "     , t_serviceattributename                                                         " // 30
                  + "     , t_value                                                                        " // 31
                  + "     , vse_createdate                                                                 " // 32
                  + "     , vse_sourcesystem                                                               " // 33
                  //Customer Label addition
                  + "     , t_customerlabel					                                               " // 34
                  + "     , t_localcurrency					                                               " // 35
                  + "     , v_localcurrency                                                                " // 36
                  + "     , v_localcurrencyexchangerate                                                    " // 37
                  + "  FROM viw_version_element_attribute v                                                "
                  + " WHERE se_usid = ?                                                                    "
                  + " ORDER BY se_serviceelementid, vse_createdate DESC, i_lupddate DESC                   "
            );

            ps.setString(1, usid);
            rs = ps.executeQuery();
            version = constructVersionFromResultSet(version, rs, false);
        } catch (SQLException e) {
            info("Can't perform query in getVersionServiceElementByUSID(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getVersionServiceElementByUSID(\"" + usid + "\", ...)", e);
        } catch (JAXBException e) {
            info("Can't instantiate 'ServiceElement' class in getVersionServiceElementByUSID(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't instantiate 'ServiceElement' class in getVersionServiceElementByUSID(\"" + usid + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }
        return version;
    }

    private VersionType constructVersionFromResultSet(VersionType v, ResultSet rs, boolean returnEmpty)
            throws SQLException, JAXBException, CSIException {
        VersionType version = v;
        // Process Results
        String prevousID = null;
        Map attributes = new HashMap();
        ServiceElementType element = null;
        long versionDate = 0;
        while (rs.next()) {
            // First, create and populate Version object
            if (version == null) {
                version = m_objectFactory.createVersion();
                // Version generation
                version.setCustomerId(rs.getString(1));     // customerId="2346"
                version.setSiteId(rs.getString(2));         // SiteId="3M Dubai"
                version.setServiceId(rs.getString(3));      // ServiceId="FRAME_RELAY_PURPLE"
                version.setOrderid(rs.getString(4));        // Orderid="006227"
                version.setOrderstatus(rs.getString(5));    // Orderstatus="CancelOrder"
                version.setOrdertype(rs.getString(6));      // Ordertype="New"
                version.setContractId(rs.getString(7));     // ContractId="Standard Contract 2346"
                version.setSubbillingAccountId(rs.getLong(8));
                version.setEndUserId(rs.getString(9));      // EndUserId="2346"
                version.setExchangeRate(rs.getDouble(10));  // ExchangeRate="1.0"
                Timestamp tmp = rs.getTimestamp(11);        // OrderSentDate="11/7/2002"
                if (tmp != null) {
                    version.setOrderSentDate(new TransDate(tmp.getTime()));
                }
                debug("Created Version object: OrderID=" + version.getOrderid());

                // System and User elements
                UserType user = m_objectFactory.createUser();
                user.setName(rs.getString(25));
                SystemType system = m_objectFactory.createSystem();
                system.setId(rs.getString(26));
                system.setUser(user);
                version.setSystem(system);

                tmp = rs.getTimestamp(32);
                if (tmp != null) {
                    versionDate = tmp.getTime();
                }
                version.setLocalCurrency(rs.getString(36)); //LocalCurrency
                version.setLocalCurrencyExchangeRate(rs.getDouble(37)); //LocalCurrencyExchangeRate
            } else {
                Timestamp tmp = rs.getTimestamp(32);
                if (tmp != null && (tmp.getTime() > versionDate)) {
                    // HACK: Update Version with latest values
                    version.setOrderid(rs.getString(4));                // Orderid="006227"
                    version.setOrderstatus(rs.getString(5));            // Orderstatus="CancelOrder"
                    version.setOrdertype(rs.getString(6));              // Ordertype="New"
                    version.setContractId(rs.getString(7));             // ContractId="Standard Contract 2346"
                    version.setSubbillingAccountId(rs.getLong(8));
                    version.setEndUserId(rs.getString(9));              // EndUserId="2346"
                    version.setExchangeRate(rs.getDouble(10));          // ExchangeRate="1.0"
                    version.setOrderSentDate(new TransDate(tmp.getTime()));

                    // System and User elements
                    UserType user = m_objectFactory.createUser();
                    user.setName(rs.getString(25));
                    SystemType system = m_objectFactory.createSystem();
                    system.setId(rs.getString(26));
                    system.setUser(user);
                    version.setSystem(system);

                    versionDate = tmp.getTime();
                    
                    version.setLocalCurrency(rs.getString(36)); //LocalCurrency
                    version.setLocalCurrencyExchangeRate(rs.getDouble(37)); //LocalCurrencyExchangeRate

                }
            }

            // Second, create and populate VersionElement
            final String elementID = rs.getString(15);
            if (!elementID.equals(prevousID)) {
                // Finish up preceding ServiceElement
                if (element != null && attributes != null) {
                    // Add collected attributes to the element
                    element.getServiceElementAttribute().addAll(attributes.values());
                    attributes.clear();
                }

                // Create and populate next ServiceElement
                element = m_objectFactory.createServiceElement();
                element.setId(rs.getString(16));
                element.setServiceElementClass(rs.getString(17));
                element.setName(rs.getString(18));
                element.setDescription(rs.getString(19));

                debug("Created ServiceElement <" + element.getId() + ">");

                Timestamp tmp = rs.getTimestamp(20);
                if (tmp != null) {
                    element.setCreationDate(new TransDate(tmp.getTime()));
                } else {
                    element.setCreationDate(null);
                }
                tmp = rs.getTimestamp(21);
                if (tmp != null) {
                    element.setGrandfatherDate(new TransDate(tmp.getTime()));
                } else {
                    element.setGrandfatherDate(null);
                }
                element.setProductId(rs.getString(24));

                // Type is always ELEMENT_TYPE_MODIFICATION
                TypeType type = m_objectFactory.createType();
                type.setCategory(CSIConstants.ELEMENT_TYPE_MODIFICATION);
                element.setType(type);
                element.setSourceSystem(rs.getString(33));

                version.getServiceElement().add(element);

                // Statuses
                final long versionServiceElementID = rs.getLong(12);

                PreparedStatement psSts = m_connection.prepareStatement(
                        "SELECT status_code, status_date FROM viw_status WHERE vse_id = ?"
                );
                psSts.setLong(1, versionServiceElementID);
                ResultSet rsSts = psSts.executeQuery();
                while (rsSts.next()) {
                    try {
                        String status = rsSts.getString(1);
                        debug("Created ServiceElementStatus <" + status + ">");
                        ServiceElementStatusType ses = m_objectFactory.createServiceElementStatus();
                        ses.setStatus(status);
                        tmp = rsSts.getTimestamp(2);
                        if (tmp != null) {
                            ses.setDate(new TransDate(tmp.getTime()));
                        } else {
                            ses.setDate(null);
                        }

                        // Add statuse here
                        element.getServiceElementStatus().add(ses);
                    } catch (JAXBException e) {
                        info("Can't instantiate 'ServiceElementStatus' class.");
                        info("ServiceElementStatus received from database {status="
                           + rsSts.getString(1)
                           + "; date=" + rsSts.getString(2)
                           + "} but not populated into ServiceElement through exception.",
                           e
                        );
                    }
                }

                carefullyClose(rsSts, psSts); rsSts=null; psSts=null;

                // Charges
                element.setOnceOffCharge(getSEOnceOffCharge(versionServiceElementID));
                element.setRecurringCharge(getSERecurringCharge(versionServiceElementID));
                element.setUsageCharge(getSEUsageCharge(versionServiceElementID));

                // Prepare place for attributes
                attributes.clear();
            }

            // Attribute area
            try {
                String attributeName = rs.getString(30);
                if (attributeName != null) {
                    if (!attributes.containsKey(attributeName)) {
                        ServiceElementAttributeType sea = m_objectFactory.createServiceElementAttribute();
                        sea.setChangeTypeCode(rs.getString(28));
                        sea.setName(attributeName);
                        sea.setValue(rs.getString(31));
                        //customer label value set
                        sea.setCustomerLabel(rs.getString(34));
                        sea.setLocalCurrency(rs.getBoolean(35));

                        // If status is InProgress, then put InProgressNoCurrent
                        String seaStatus = rs.getString(14);
                        if (CSIConstants.STATUS_INPROGRESS.equals(seaStatus)) {
                            seaStatus = CSIConstants.STATUS_INPRONOCURR;
                        }
                        sea.setStatus(seaStatus);

                        long attributeID = rs.getLong(29);
                        debug("Created ServiceAttribute <" + sea.getName() +
                              "> (ID: " + attributeID + "), Status " + sea.getStatus() + 
                              ", Customer Label " + sea.getCustomerLabel()+", Local Currency "+ sea.isLocalCurrency());

                        // Attribute charges
                        sea.setOnceOffCharge(getSEAOnceOffCharge(attributeID));
                        sea.setRecurringCharge(getSEARecurringCharge(attributeID));
                        sea.setUsageCharge(getSEAUsageCharge(attributeID));

                        // Adding the attribute
                        attributes.put(sea.getName(), sea);
                    } else {
                        // Check status of the attribute from the database. If it is CURRENT,
                        // and status of the attribute in the map is InProgressNoCurrent,
                        // then change it to InProgress.

                        String seaStatusPrev = rs.getString(14);
                        if (CSIConstants.STATUS_CURRENT.equals(seaStatusPrev)) {
                            //take the LAST status from Map
                            ServiceElementAttributeType sea = (ServiceElementAttributeType)attributes.get(attributeName);

                            if (CSIConstants.STATUS_INPRONOCURR.equals(sea.getStatus())) {
                                sea.setStatus(CSIConstants.STATUS_INPROGRESS);
                            }
                        }

                        debug("Previous ServiceAttribute <" + attributeName +
                              "> (ID: " + rs.getLong(29) + "), Status " + seaStatusPrev);
                    }
               }
            } catch (JAXBException e) {
                info("Can't instantiate 'ServiceElementAttribute' class.");
                info("ServiceElementAttribute received from database {ChangeTypeCode="
                   + rs.getString(14)
                   + "; Name=" + rs.getString(30)
                   + "; Value=" + rs.getString(31)
                   + "; Status=" + rs.getString(14)
                   + "; attributeID=" + rs.getString(29)
                   + "} but not populated into ServiceElement through exception.",
                   e
                );
            }
            // Moving to next
            prevousID = elementID;
        }

        // Finish up last ServiceElement
        if (element != null && attributes != null  && !attributes.isEmpty()) {
            // Add collected attributes to the element
            element.getServiceElementAttribute().addAll(attributes.values());
        }

        if(!returnEmpty && (version==null || version.getServiceElement()==null || version.getServiceElement().size()==0)) {
            return null;
        }
        return version;
    }

    public ConflictNoteType getConflictNote(String type, String sourceSystem) throws CSIException {
        ConflictNoteType ret = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            debug("Get getConflictNote(\"" + type + "\", \"" + sourceSystem + "\");");

            // get all customer, site, service which have service elements with the source system as ${sourceSystem}
            ps = m_connection.prepareStatement(
            		"SELECT DISTINCT														  "
            	  + "		v.CUSTHANDLE													  "
		          + "     , v.SITEHANDLE													  "
		          + "     , v.SERVICEHANDLE                                           		  "
		          + "     , se.USID                                                   		  "
		          + "     , se.SOURCESYSTEM                                           		  "
            	  +	"FROM   CVERSION v														  "
            	  + "     , CVERSIONSERVICEELEMENT vse										  "
		          + "     , CSERVICEELEMENT se												  "
            	  + "WHERE  v.VERSIONID = vse.VERSIONID										  "
            	  +	"  AND  vse.SERVICEELEMENTID = se.SERVICEELEMENTID						  "
            	  +	"  AND  v.VERSIONID IN (SELECT vse.VERSIONID							  "
		          + "                       FROM   CVERSIONSERVICEELEMENT vse				  "
		          + "                            , CSERVICEELEMENT se						  "
		          + "                       WHERE  vse.SERVICEELEMENTID = se.SERVICEELEMENTID "
		          + "                         AND  se.SOURCESYSTEM = ? )			          "
            	  +	"ORDER BY v.CUSTHANDLE													  "
            	  +	"       , v.SITEHANDLE													  "
		          + "       , v.SERVICEHANDLE												  "
            );
            ps.setString(1, sourceSystem);
            rs = ps.executeQuery();
            ServiceSummaryType sst = null;
            ServiceElementSummaryType sest = null;
            String lastServiceId = null;
            for(int count=0;rs.next();count++) {
                if(count>CSI_EXCEPTION_REPORT_LIMIT) {
                    info("CSI Exception Report reached the limit " + CSI_EXCEPTION_REPORT_LIMIT + " number of version objects. Ending");
                    break;
                }
                if(ret==null) {
                    // create ConflictNote object
                    ret = m_objectFactory.createConflictNoteType();
                    ret.setType(type);
                }
                String customerId = rs.getString(1);
                String siteId = rs.getString(2);
                String serviceId = rs.getString(3);
                String usid = rs.getString(4);
                String usidSourceSystem = rs.getString(5);

                if(customerId==null || siteId==null || serviceId==null) continue;

                if(sst==null || !customerId.equals(sst.getCustomerId()) || !siteId.equals(sst.getSiteId())) {
                    sst = m_objectFactory.createServiceSummaryType();
                    sst.setCustomerId(customerId);
                    sst.setSiteId(siteId);
                    ret.getServiceSummary().add(sst);
                    lastServiceId = null;
                }

                if(sest==null || !serviceId.equals(lastServiceId)) {
                    lastServiceId = serviceId;
                    sest = m_objectFactory.createServiceElementSummaryType();
                    sest.setServiceId(serviceId);
                    sst.getServiceElementSummary().add(sest);
                }

                ServiceElementUSIDType seut = m_objectFactory.createServiceElementUSIDType();
                seut.setUSID(usid);
                seut.setSourceSystem(usidSourceSystem);
                sest.getServiceElementUSID().add(seut);
            }

            rs.close(); rs = null;
            ps.close(); ps = null;
        } catch (SQLException e) {
            info("Can't perform query in getConflictNote(\"" + type + "\", \"" + sourceSystem + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getConflictNote(\"" + type + "\", \"" + sourceSystem + "\", ...)", e);
        } catch (JAXBException e) {
            info("Can't instantiate JAXB class(es) in getConflictNote(\"" + type + "\", \"" + sourceSystem + "\", ...)", e);
            throw new CSIRollbackException("Can't instantiate JAXB class(es) in getConflictNote(\"" + type + "\", \"" + sourceSystem + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }
        return ret;
    }
    /*
     *
     *
     * @param sourceSystem
     * @return collection of Version objects
     * @throws CSIException
     */
/*
    public Collection getServiceDetailsBySourceSystem(String sourceSystem) throws CSIException {
        Vector ret = new Vector();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();

            if (logger.isDebugEnabled()) {
                logger.debug("Get getServiceDetailsByStateAndSourceSystem(\"" + sourceSystem + "\");");
            }

            // get all customer, site, service which have service elements with the source system as ${sourceSystem}
            ps = conn.prepareStatement(
                    "SELECT DISTINCT v.CUSTHANDLE                                                           " // 1
                  + "              , v.SITEHANDLE                                                           " // 2
                  + "              , v.SERVICEHANDLE                                                        " // 3
                  + "FROM   CVERSION v                                                                      "
                  + "     , CVERSIONSERVICEELEMENT vse                                                      "
                  + "     , CSERVICEELEMENT se                                                              "
                  + "WHERE  v.VERSIONID = vse.VERSIONID                                                     "
                  + "  AND  vse.SERVICEELEMENTID = se.SERVICEELEMENTID                                      "
                  + "  AND  se.SOURCESYSTEM = ?                                                             "
            );
            ps.setString(1, sourceSystem);
            rs = ps.executeQuery();
            List masterList = new ArrayList();
            while (rs.next()) {
                masterList.add(new ServiceIdHolder(rs.getString(1), rs.getString(2), rs.getString(3)));
            }

            rs.close();
            ps.close();

            ps = conn.prepareStatement(
                                  // Hand crafted query.
                                    "SELECT v_custhandle                                                                   " // 1
                                  + "     , v_sitehandle                                                                   " // 2
                                  + "     , v_servicehandle                                                                " // 3
                                  + "     , v_ordhandle                                                                    " // 4
                                  + "     , v_orderstatus                                                                  " // 5
                                  + "     , v_ordertype                                                                    " // 6
                                  + "     , v_ctrhandle                                                                    " // 7
                                  + "     , v_sbahandle                                                                    " // 8
                                  + "     , v_enduserhandle                                                                " // 9
                                  + "     , v_exchangerate                                                                 " // 10
                                  + "     , v_createdate                                                                   " // 11
                                  + "     , vse_versionserviceelementid                                                    " // 12
                                  + "     , vse_lupddate                                                                   " // 13
                                  + "     , vses_statustypecode                                                            " // 14
                                  + "     , se_serviceelementid                                                            " // 15
                                  + "     , se_usid                                                                        " // 16
                                  + "     , se_serviceelementclass                                                         " // 17
                                  + "     , se_serviceelementname                                                          " // 18
                                  + "     , se_description                                                                 " // 19
                                  + "     , se_creationdate                                                                " // 20
                                  + "     , se_grandfatherdate                                                             " // 21
                                  + "     , se_startbillingdate                                                            " // 22
                                  + "     , se_endbillingdate                                                              " // 23
                                  + "     , se_prodid                                                                      " // 24
                                  + "     , u_systemusername                                                               " // 25
                                  + "     , u_systemcode                                                                   " // 26
                                  + "     , i_lupddate                                                                     " // 27
                                  + "     , i_changetypecode                                                               " // 28
                                  + "     , t_serviceattributeid                                                           " // 29
                                  + "     , t_serviceattributename                                                         " // 30
                                  + "     , t_value                                                                        " // 31
                                  + "     , vse_createdate                                                                 " // 32
                                  + "     , vse_sourcesystem                                                               " // 33
                                  + "  FROM viw_version_element_attribute v                                                "
                                  + " WHERE v_custhandle = ?                                                               "
                                  + "   AND v_sitehandle = ?                                                               "
                                  + "   AND v_servicehandle = ?                                                            "
                                  + "   AND vse_sourcesystem = ?                                                            "
                                  + "   AND se_serviceelementid NOT IN (SELECT DISTINCT se_serviceelementid                "
                                  + "                                  FROM viw_version_element_status vv                  "
                                  + "                                 WHERE v_custhandle = ?                               "
                                  + "                                   AND v_sitehandle = ?                               "
                                  + "                                   AND v_servicehandle = ?                            "
                                  + "                                   AND vses_statustypecode IN ('Delete', 'Disconnect', 'InProgress') "
                                  + "                                   AND vv.se_serviceelementid =  v.se_serviceelementid"
                                  + "                                   AND vv.vse_lupddate >= v.vse_lupddate)             "
                                  + " ORDER BY se_serviceelementid, vse_createdate DESC, i_lupddate DESC                   "
            );

            for (int i = 0; i < masterList.size(); i++) {
                ServiceIdHolder service = (ServiceIdHolder) masterList.get(i);

                ps.setString(1, service.customerId);
                ps.setString(2, service.siteId);
                ps.setString(3, service.servicerId);
                ps.setString(4, sourceSystem);
                ps.setString(5, service.customerId);
                ps.setString(6, service.siteId);
                ps.setString(7, service.servicerId);

                rs = ps.executeQuery();
                VersionType v = null;
                v = constructVersionFromResultSet(conn, v, rs, null, false);
                if(v!=null) {
                    ret.add(v);
                }
                try {
                    if (rs != null) {
                        rs.close();
                    }
                } catch (Exception e) {
                    logger.info("ResultSet was not closed succesfully", e);
                }
                rs = null;

                if(ret.size() > CSI_EXCEPTION_REPORT_LIMIT) {
                    logger.info("CSI Exception Report reached the limit " + CSI_EXCEPTION_REPORT_LIMIT + " number of version objects. Ending");
                    break;
                }
            }
        } catch (SQLException e) {
            csiLog.sendError("Can't perform SQL query.\n", e);
        } catch (JAXBException e) {
            csiLog.sendError("Can't instantiate 'ServiceElement' class.\n", e);
        } finally {
            try {if (rs != null) rs.close();} catch (Exception e) {logger.info("ResultSet was not closed succesfully", e);}
            try {if (ps != null) ps.close();} catch (Exception e) {logger.info("PreparedStatement was not closed succesfully", e);}
            rs = null;
            ps = null;
            try {if (conn != null) conn.close();} catch (Exception e) {logger.info("Connection was not closed succesfully", e);}
            conn = null;
        }
        return ret;
    }
*/

    public ServiceIdHolder getCustomerSiteByUSID(String usid, String customerId, String serviceId) throws CSIException {
        ServiceIdHolder ret = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            debug("Get getCustomerSiteByUSID(\"" + usid + "\");");

            // get customer and site
            ps = m_connection.prepareStatement(
                    "SELECT v.CUSTHANDLE                                                        "
                  + "     , v.SITEHANDLE                                                        "
                  + "     , v.SERVICEHANDLE                                                     "
                  + "FROM   CSERVICEELEMENT se                                                  "
                  + "     , CVERSIONSERVICEELEMENT vse                                          "
                  + "     , CVERSION v                                                          "
                  + "WHERE  se.SERVICEELEMENTID = vse.SERVICEELEMENTID                          "
                  + "   AND vse.VERSIONID = v.VERSIONID                                         "
                  + "   AND se.USID = ?                                                         "
                  + "   AND v.CUSTHANDLE = ?                                                    "
                  + "   AND v.SERVICEHANDLE = ?                                                    "
            );
            ps.setString(1, usid);
            ps.setString(2, customerId);
            ps.setString(3, serviceId);
            rs = ps.executeQuery();
            if(rs.next()) {
                ret = new ServiceIdHolder(rs.getString(1), rs.getString(2), rs.getString(3));
            }
        } catch (SQLException e) {
            info("Can't perform query in getCustomerSiteByUSID(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getCustomerSiteByUSID(\"" + usid + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return ret;
    }

    public List getAllCustomerSiteByUSID(String usid) throws CSIException {
        List ret = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            debug("Get getAllCustomerSiteByUSID(\"" + usid + "\");");

            // get customer and site
            ps = m_connection.prepareStatement(
                    "SELECT DISTINCT v.CUSTHANDLE   "
                  + " , v.SITEHANDLE "
                  + " , v.SERVICEHANDLE "
                  + "  FROM   CSERVICEELEMENT se "
                  + " , CVERSIONSERVICEELEMENT vse "
                  + " , CVERSION v "
                  + " , CVERSIONSERVICEELEMENTSTS vses "
                  + " WHERE  se.SERVICEELEMENTID = vse.SERVICEELEMENTID  "
                  + "   AND vse.VERSIONID = v.VERSIONID "
                  + "   AND vse.VERSIONSERVICEELEMENTID = vses.VERSIONSERVICEELEMENTID "
                  + "   AND vses.statustypecode NOT IN ('Disconnect', 'Delete') "
                  + "   AND se.USID = ? "
            );
            ps.setString(1, usid);
            rs = ps.executeQuery();
            while(rs.next()) {
                if(ret==null) ret = Collections.synchronizedList(new ArrayList());
                ret.add(new ServiceIdHolder(rs.getString(1), rs.getString(2), rs.getString(3)));
            }
        } catch (SQLException e) {
            info("Can't perform query in getCustomerSiteByUSID(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getCustomerSiteByUSID(\"" + usid + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return ret;
    }

    public List getAllStatusCustomerSiteByUSID(String usid) throws CSIException {
        List ret = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            debug("Get getAllStatusCustomerSiteByUSID(\"" + usid + "\");");

            // get customer and site
            ps = m_connection.prepareStatement(
                    "SELECT DISTINCT v.CUSTHANDLE   "
                  + " , v.SITEHANDLE "
                  + " , v.SERVICEHANDLE "
                  + "  FROM   CSERVICEELEMENT se "
                  + " , CVERSIONSERVICEELEMENT vse "
                  + " , CVERSION v "
//                  + " , CVERSIONSERVICEELEMENTSTS vses "
                  + " WHERE  se.SERVICEELEMENTID = vse.SERVICEELEMENTID  "
                  + "   AND vse.VERSIONID = v.VERSIONID "
//                  + "   AND vse.VERSIONSERVICEELEMENTID = vses.VERSIONSERVICEELEMENTID "
//                  + "   AND vses.statustypecode NOT IN ('Disconnect', 'Delete') "
                  + "   AND se.USID = ? "
            );
            ps.setString(1, usid);
            rs = ps.executeQuery();
            while(rs.next()) {
                if(ret==null) ret = Collections.synchronizedList(new ArrayList());
                ret.add(new ServiceIdHolder(rs.getString(1), rs.getString(2), rs.getString(3)));
            }
        } catch (SQLException e) {
            info("Can't perform query in getCustomerSiteByUSID(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getCustomerSiteByUSID(\"" + usid + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return ret;
    }

    public HashMap getBulkServiceList(String customerId) throws CSIException {
            HashMap result = new HashMap();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            debug("Get geBulkServiceList(..., \"" + customerId + "\"");
            // get customer and site
            ps = m_connection.prepareStatement(
                     "SELECT DISTINCT v.sitehandle, v.servicehandle                                                          "
                   + "FROM cversion v                                                                                        "
                   + "   , cversionserviceelement vse                                                                        "
                   + "   , cserviceelement se                                                                                "
                   + "WHERE v.custhandle = ?                                                                                 "
                   + "  AND v.versionid = vse.versionid                                                                      "
                   + "  AND vse.serviceelementid = se.serviceelementid                                                       "
                   + "  AND se.serviceelementid NOT IN (SELECT DISTINCT se_serviceelementid                                 "
                  + "                                  FROM viw_version_element_status vv                  "
                  + "                                  WHERE v_custhandle = ?                               "
                  + "                                   AND vses_statustypecode IN ('Delete', 'Disconnect') "
                  + "                                   AND vv.se_serviceelementid =  se.serviceelementid"
                  + "                                   AND vv.vse_lupddate >= vse.lupddate)                                 "
                   + "ORDER BY v.sitehandle"
            );
            ps.setString(1, customerId);
            ps.setString(2, customerId);
            rs = ps.executeQuery();
            String prevSite = null;
            ArrayList services = null;
            while(rs.next()) {
                String siteid = rs.getString(1);
                String serviceid = rs.getString(2);
                if(siteid!=null && serviceid!=null) {
                    if(!siteid.equals(prevSite)) {
                        services = new ArrayList();
                        result.put(siteid, services);
                    }
                    services.add(serviceid);
                }
            }
        } catch (SQLException e) {
            info("Can't perform query in geBulkServiceList(\"" + customerId + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in geBulkServiceList(\"" + customerId + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return result;
    }
    
    public SiteInformationType populateServiceListForSite(SiteInformationType siteInfo) throws CSIException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        if(siteInfo==null) {
            info("Got empty SiteInformation - nothing to fill... skipping");
            return siteInfo;
        }

        try {
            debug("Get populateServiceListForSite(..., \"" + siteInfo.getCustCode() + "\", \"" + siteInfo.getCISSiteId() + "\");");
            // get customer and site
            ps = m_connection.prepareStatement(
                   "SELECT DISTINCT v_servicehandle, vse_lupddate, vses_statustypecode        "                                                                
                 + " FROM viw_version_element_attribute v                                     "                                                  
                 + " WHERE v_custhandle = ?                                                   "                                                             
                 + "     AND v_sitehandle = ?                                                 "                                                               
                 + "ORDER BY v_servicehandle, vse_lupddate DESC                               "
            );
            ps.setString(1, siteInfo.getCustCode());
            ps.setString(2, siteInfo.getCISSiteId());
            rs = ps.executeQuery();
            String service="";
            while(rs.next()) {
                String id = rs.getString(1);
                String status=rs.getString(3);
                if(id!=null && id.length()>0) {
                    if(!id.equals(service)) {
                        //processing new service
                        service=id;
                        if(!STATUS_DELETE.equals(status) && !STATUS_DISCONNECT.equals(status)) {
                            //if last status not DELETE or not DISCONNECT - create service
                            ServicesType st = m_objectFactory.createServicesType();
                            st.setServiceName(id);
                            siteInfo.getServices().add(st);                            
                        }
                        
                    }
                }
            }
            if(siteInfo.getServices().size()==0) {
                siteInfo.getServices().add(m_objectFactory.createServicesType());
            }
        } catch (SQLException e) {
            info("Can't perform query in populateServiceListForSite(\"" + siteInfo.getCustCode() + "\", \"" + siteInfo.getCISSiteId() + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in populateServiceListForSite(\"" + siteInfo.getCustCode() + "\", \"" + siteInfo.getCISSiteId() + "\", ...)", e);
        } catch (JAXBException e) {
            info("Can't instantiate JAXB class(es) in populateServiceListForSite(\"" + siteInfo.getCustCode() + "\", \"" + siteInfo.getCISSiteId() + "\", ...)", e);
            throw new CSIRollbackException("Can't instantiate JAXB class(es) in populateServiceListForSite(\"" + siteInfo.getCustCode() + "\", \"" + siteInfo.getCISSiteId() + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return siteInfo;
    }

    public Hashtable getSourceSystem(String orderId) throws CSIException {
        Hashtable ret = new Hashtable();

        PreparedStatement ps = null;
        ResultSet rs = null;

        debug("getSourceSystem(...,\"" + orderId + "\"");

        try {
            // get customer and site
            ps = m_connection.prepareStatement(
                    "SELECT DISTINCT                                                              "
                  + "       se.USID, se.SOURCESYSTEM                                              "
                  + "FROM                                                                         "
                  + "       CVERSION v,                                                           "
                  + "       CVERSIONSERVICEELEMENT vse,                                           "
                  + "       CSERVICEELEMENT se                                                    "
                  + "WHERE                                                                        "
                  + "       v.VERSIONID = vse.VERSIONID                                           "
                  + "  AND                                                                        "
                  + "         vse.SERVICEELEMENTID = se.SERVICEELEMENTID                          "
                  + "  AND                                                                        "
                  + "       v.ORDHANDLE = ?                                                       "
            );
            ps.setString(1, orderId);
            rs = ps.executeQuery();
            while(rs.next()) {
                String sourceSystem = rs.getString(2);
                if(sourceSystem==null) sourceSystem = SYSTEM_TYPE_GOLD;
                ret.put(rs.getString(1), sourceSystem);
            }
        } catch (SQLException e) {
            info("Can't perform query in getSourceSystem(\"" + orderId + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getSourceSystem(\"" + orderId + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return ret;
    }

    public HashMap getUsidBySiteAttribute(
            String customerId,
            String siteId,
            String serviceId,
            String serviceElementClass,
            String attributeName,
            String attributeValue
    ) throws CSIException {
        HashMap ret = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        debug(
                "getUsidBySiteAttribute(\"" + customerId + "\", \"" + siteId + "\", \"" + serviceId
                + "\", \"" + serviceElementClass + "\", \"" + attributeName + "\", \"" + attributeValue + "\") "
        );

        try {
            // get customer and site
            ps = m_connection.prepareStatement(
                    " SELECT "
                  + " se.USID "
                  + " , se.SOURCESYSTEM "
                  + " FROM "
                  + " CVERSION v,  "
                  + " CVERSIONSERVICEELEMENT vse,  "
                  + " CVERSIONSERVICEELEMENTSTS vses,  "
                  + " CSERVICEELEMENT se,  "
                  + " CSERVICECHANGEITEM sci,  "
                  + " CSERVICEATTRIBUTE sa  "
                  + " WHERE  "
                  + " v.VERSIONID = vse.VERSIONID  "
                  + "  AND "
                  + "  vse.SERVICEELEMENTID = se.SERVICEELEMENTID  "
                  + "  AND "
                  + "  vse.VERSIONSERVICEELEMENTID = vses.VERSIONSERVICEELEMENTID "
                  + "  AND "
                  + "  vses.statustypecode NOT IN ('Disconnect', 'Delete')  "
                  + "  AND "
                  + "        v.CUSTHANDLE = ? "
                  + "  AND "
                  + "        v.SITEHANDLE = ? "
                  + "  AND "
                  + "        v.SERVICEHANDLE = ? "
                  + "  AND "
                  + "        se.SERVICEELEMENTCLASS = ?  "
                  + "  AND "
                  + "        sci.VERSIONSERVICEELEMENTID = vse.VERSIONSERVICEELEMENTID "
                  + "  AND "
                  + "        sci.SERVICEATTRIBUTEID = sa.SERVICEATTRIBUTEID  "
                  + "  AND "
                  + "        sa.SERVICEATTRIBUTENAME = ? "
                  + "  AND "
                  + "        UPPER(sa.VALUE) = UPPER(?) "
            );
            ps.setString(1, customerId);
            ps.setString(2, siteId);
            ps.setString(3, serviceId);
            ps.setString(4, serviceElementClass);
            ps.setString(5, attributeName);
            ps.setString(6, attributeValue);

            rs = ps.executeQuery();
            while(rs.next()) {
                if(ret==null) ret = new HashMap();
                ret.put(rs.getString(1), rs.getString(2));
            }
        } catch (SQLException e) {
            info(
                    "Can't perform query in " +
                    "getUsidBySiteAttribute(\"" + customerId + "\", \"" + siteId + "\", \"" + serviceId
                    + "\", \"" + serviceElementClass + "\", \"" + attributeName + "\", \"" + attributeValue + "\" ",
                    e
            );
            throw new CSIRollbackException(
                    "Can't perform query in " +
                    "getUsidBySiteAttribute(\"" + customerId + "\", \"" + siteId + "\", \"" + serviceId
                    + "\", \"" + serviceElementClass + "\", \"" + attributeName + "\", \"" + attributeValue + "\" ",
                    e
            );
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }
        return ret;
    }

    public String getLatestServiceElementStatus(String usid) throws CSIException {
        String ret = null;

        PreparedStatement ps = null;
        ResultSet rs = null;

        debug("getLatestServiceElementStatus(\"" + usid + "\")");

        try {
            // get customer and site
            ps = m_connection.prepareStatement(
                                    "SELECT vses.STATUSTYPECODE                                                      "
                  + "FROM   CSERVICEELEMENT se,                                                      "
                  + "       CVERSIONSERVICEELEMENT vse,                                              "
                  + "       CVERSIONSERVICEELEMENTSTS vses                                           "
                  + "WHERE  se.SERVICEELEMENTID = vse.SERVICEELEMENTID                               "
                  + "  AND  vse.VERSIONSERVICEELEMENTID = vses.VERSIONSERVICEELEMENTID               "
                  + "  AND  vses.statustypecode IN ('InProgress', 'Current', 'Disconnect', 'Delete') "
                  + "  AND  se.USID = ?                                                              "
                  + "ORDER BY vses.LUPDDATE DESC                                                     "
            );
            ps.setString(1, usid);
            rs = ps.executeQuery();

            // we need only latest status (it comes as first)
            if(rs.next()) {
                ret = rs.getString(1);
            }
        } catch (SQLException e) {
            info("Can't perform query in getLatestServiceElementStatus(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getLatestServiceElementStatus(\"" + usid + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return ret;
    }

    public String getLatestServiceElementStatus(String customerId, String siteHandle, String serviceId, String usid) throws CSIException {
        String ret = null;

        PreparedStatement ps = null;
        ResultSet rs = null;

        debug("getLatestServiceElementStatus(\"" + customerId + "\", \"" + siteHandle + "\", " + serviceId + "\", " + usid + "\")");

        try {
            // get customer and site
            ps = m_connection.prepareStatement(
                    "SELECT vses.STATUSTYPECODE                                                      "
                  + "FROM   CVERSION v,                                                              "
                  + "       CSERVICEELEMENT se,                                                      "
                  + "       CVERSIONSERVICEELEMENT vse,                                              "
                  + "       CVERSIONSERVICEELEMENTSTS vses                                           "
                  + "WHERE  v.VERSIONID = vse.VERSIONID                                              "
                  + "  AND  se.SERVICEELEMENTID = vse.SERVICEELEMENTID                               "
                  + "  AND  vse.VERSIONSERVICEELEMENTID = vses.VERSIONSERVICEELEMENTID               "
                  + "  AND  vses.statustypecode IN ('InProgress', 'Current', 'Disconnect', 'Delete') "
                  + "  AND  se.USID = ?                                                              "
                  + "  AND  v.CUSTHANDLE = ?                                                         "
                  + "  AND  v.SITEHANDLE = ?                                                         "
                  + "  AND  v.SERVICEHANDLE = ?                                                      "
                  + "ORDER BY vses.LUPDDATE DESC                                                     "
            );
            ps.setString(1, usid);
            ps.setString(2, customerId);
            ps.setString(3, siteHandle);
            ps.setString(4, serviceId);
            rs = ps.executeQuery();

            // we need only latest status (it comes as first)
            if(rs.next()) {
                ret = rs.getString(1);
            }
        } catch (SQLException e) {
            info("Can't perform query in getLatestServiceElementStatus(\""
                    + customerId + "\", \"" + siteHandle + "\", " + serviceId + "\", " + usid + "\")", e);
            throw new CSIRollbackException("Can't perform query in getLatestServiceElementStatus(\""
                    + customerId + "\", \"" + siteHandle + "\", " + serviceId + "\", " + usid + "\")", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return ret;
    }

    /**
     * Returns CustomerID/SiteHandle/ServiceHandle by given USID (contains only single record, combined to String[3])
     *
     * @param usid  the Service Element USID
     * @return String[3] or null
     */
    public String[] getCustomerInfoByServiceElementUSID(String usid) throws CSIException {
        String[] ret = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            debug("Get getCustomerInfoByServiceElementUSID for " + usid);

            ps = m_connection.prepareStatement(
                    "SELECT v.custhandle                              "
                  + "     , v.sitehandle                              "
                  + "     , v.servicehandle                           "
                  + "FROM CVERSION                  v                 "
                  + "   , CSERVICEELEMENT           se                "
                  + "   , CVERSIONSERVICEELEMENT    vse               "
                  + "WHERE v.versionid = vse.versionid                "
                  + "  AND vse.serviceelementid = se.serviceelementid "
                  + "  AND se.usid = ?                                "
            );

            ps.setString(1, usid);
            rs = ps.executeQuery();
            if(rs.next()) {
                // we have this USID
                ret = new String[3];
                ret[0] = rs.getString(1); /* CustomerId */
                ret[1] = rs.getString(2); /* SiteHandle */
                ret[2] = rs.getString(3); /* ServiceHandle */
            }
        } catch (SQLException e) {
            info("Can't perform query in getCustomerInfoByServiceElementUSID(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getCustomerInfoByServiceElementUSID(\"" + usid + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }
        return ret;
    }

    /**
     *
     * @return List of Service Attribute IDs where Remote Site Code = 'unknown' and USID for that Remote Site
     * @throws CSIException
     */
    public ArrayList getRemoteSiteUSIDs() throws CSIException {
        PreparedStatement ps = null;
        ArrayList ret = null;
        ResultSet rs = null;

        try {
            debug("getRemoteSiteUSIDs: Get getRemoteSiteUSID");

            ps = m_connection.prepareStatement(
                   " select usid.versionserviceelementid, site.serviceattributeid, usid.value                  "
                 + " from (                                                                                    "
                 + " select sci.VERSIONSERVICEELEMENTID, sa.SERVICEATTRIBUTEID, sa.VALUE                       "
                 + " from                                                                                      "
                 + "        CSERVICECHANGEITEM sci,                                                            "
                 + "        CSERVICEATTRIBUTE sa                                                               "
                 + " where                                                                                     "
                 + "        sci.SERVICEATTRIBUTEID = sa.SERVICEATTRIBUTEID                                     "
                 + "   AND  sa.SERVICEATTRIBUTENAME = 'Remote Site Code'                                       "
                 + " ) site, (                                                                                 "
                 + " select sci.VERSIONSERVICEELEMENTID, sa.VALUE                                              "
                 + " from                                                                                      "
                 + "        CSERVICECHANGEITEM sci,                                                            "
                 + "        CSERVICEATTRIBUTE sa                                                               "
                 + " where                                                                                     "
                 + "        sci.SERVICEATTRIBUTEID = sa.SERVICEATTRIBUTEID                                     "
                 + "   AND  sa.SERVICEATTRIBUTENAME = 'Remote Access Connection USID'                          "
                 + " ) usid                                                                                    "
                 + " where site.VERSIONSERVICEELEMENTID(+) = usid.VERSIONSERVICEELEMENTID                      "
                 + "   AND (site.VALUE = 'UNKNOWN' or site.VALUE is null)                                      "
            );

            rs = ps.executeQuery();
            ret = new ArrayList();
            while (rs.next()) {
                RemoteSiteCodeInfo value = new RemoteSiteCodeInfo(rs.getLong(1)   /* VersionServiceElementId */,
                                                                  rs.getLong(2)   /* ServiceAttributeId */,
                                                                  rs.getString(3) /* Value */);
                ret.add(value);
            }
        } catch (SQLException e) {
            info("Can't perform query in getRemoteSiteUSIDs()", e);
            throw new CSIRollbackException("Can't perform query in getRemoteSiteUSIDs()", e);
        } finally {
            carefullyClose(rs, ps); rs = null; ps = null;
        }

        return ret;
    }

    public ServiceIdHolder getCustomerSiteByCISUSID(String usid) throws CSIException {
        ServiceIdHolder ret = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            debug("Get getCustomerSiteByCISUSID(\"" + usid + "\");");

            // get customer and site
            ps = m_connection.prepareStatement(
                    "SELECT v.CUSTHANDLE                                                        "
                  + "     , v.SITEHANDLE                                                        "
                  + "     , v.SERVICEHANDLE                                                     "
                  + "FROM   CSERVICEELEMENT se                                                  "
                  + "     , CVERSIONSERVICEELEMENT vse                                          "
                  + "     , CVERSION v                                                          "
                  + "WHERE  se.SERVICEELEMENTID = vse.SERVICEELEMENTID                          "
                  + "   AND vse.VERSIONID = v.VERSIONID                                         "
                  + "   AND se.SOURCESYSTEM = ?                                                 "
                  + "   AND se.USID = ?                                                         "
            );
            ps.setString(1, CSIConstants.SYSTEM_TYPE_CIS);
            ps.setString(2, usid);
            rs = ps.executeQuery();
            if (rs != null && rs.next()) {
                ret = new ServiceIdHolder(rs.getString(1), rs.getString(2), rs.getString(3));
                debug("getCustomerSiteByCISUSID: Found site code " + ret.siteId);
            } else {
                debug("getCustomerSiteByCISUSID: Site code is not found");
            }
        } catch (SQLException e) {
            info("Can't perform query in getCustomerSiteByUSID(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getCustomerSiteByUSID(\"" + usid + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return ret;
    }

    long getLatestVersion(VersionType version, String statustype) throws CSIException {
        long ret = -1;
        if(version==null || version.getCustomerId() == null || version.getSiteId() == null || version.getServiceId() == null) return -1;

        PreparedStatement ps = null;
        ResultSet rs = null;

        debug("getLatestVersion(\"" + version.getCustomerId() + "\", \"" + version.getSiteId() + "\", " + version.getServiceId() + "\", " + statustype + "\")");

        try {
            // get customer and site
            ps = m_connection.prepareStatement(
                    "SELECT v.VERSIONID                                                                  "
                  + "FROM  CVERSION  v                                                                   "
                  + "    , CVERSIONSERVICEELEMENT    vse                                                 "
                  + "    , CVERSIONSERVICEELEMENTSTS vses                                                "
                  + "WHERE v.versionid = vse.versionid                                                   "
                  + "  AND vse.versionserviceelementid = vses.versionserviceelementid                    "
//                  + "  AND vses.statustypecode IN ('InProgress', 'Current', 'Disconnect', 'Delete')      "
                  + "  AND v.custhandle = ?                                                              "
                  + "  AND v.sitehandle = ?                                                              "
                  + "  AND v.servicehandle = ?                                                           "
                  + "  AND vses.statustypecode = ?                                                       "
                  + "ORDER BY vse.createdate DESC                                                        "
            );
            ps.setString(1, version.getCustomerId());
            ps.setString(2, version.getSiteId());
            ps.setString(3, version.getServiceId());
            ps.setString(4, statustype);
            rs = ps.executeQuery();

            // we need only latest status (it comes as first)
            if(rs.next()) {
                ret = rs.getLong(1);
            }
        } catch (SQLException e) {
            info("Can't perform query in getLatestVersion(\""
                    + version.getCustomerId() + "\", \"" + version.getSiteId() + "\", " + version.getServiceId() + "\", " + statustype + "\")", e);
            throw new CSIRollbackException("Can't perform query in getLatestVersion(\""
                    + version.getCustomerId() + "\", \"" + version.getSiteId() + "\", " + version.getServiceId() + "\", " + statustype + "\")", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return ret;
    }
    
    public long getServiceElementId(String usid) throws CSIException {
        long ret = -1;

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = m_connection.prepareStatement("SELECT ServiceElementId FROM CServiceElement WHERE USID = ? ");
            ps.setString(1, usid);
            rs = ps.executeQuery();
            
            // we need only latest status (it comes as first)
            if(rs.next()) {
                ret = rs.getLong(1);
            }
        } catch (SQLException e) {
            info("Can't perform query in getServiceElementId(\"" + usid + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getServiceElementId(\"" + usid + "\", ...)", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return ret;
    }
    
    public String getOrderStatusForOrderId(String orderId) throws CSIRollbackException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String ret="";
        try {
            ps = m_connection.prepareStatement(
                    "SELECT v.OrderStatus                                                                  "
                  + "FROM  CVERSION  v                                                                     "
                  + "WHERE v.ordhandle = ?                                                                   "
                  + "ORDER BY v.VERSIONID DESC                                                             "
            );
            ps.setString(1, orderId);
            rs = ps.executeQuery();
            if(rs.next()) {
                ret = rs.getString(1);
            }
        }catch(SQLException e) {
            info("Can't perform query in getOrderStatusForOrderId(\"" + orderId + "\", ...)", e);
            throw new CSIRollbackException("Can't perform query in getOrderStatusForOrderId(\"" + orderId + "\", ...)", e);            
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;            
        }
        return ret;
    }
    
    protected Category getLogger() {
        return QueryManager.m_logger;
    }

    /**
     * @param siteInfo
     * @return
     * @throws CSIException 
     */
    public String getCISSiteIDFromTempTable(SiteInformationType siteInfo) throws CSIException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String ret = "";

        if (siteInfo != null && siteInfo.getCustCode() != null && siteInfo.getCustCode().length() > 0) {
            try {
                ps = m_connection.prepareStatement(" SELECT s.SITECODE "
                        + " FROM  CIS_TEMP_SITE s "
                        + " WHERE CUSTCODE = ? "
                        + " AND UPPER(s.POSTALCODE) = ? "
                        + " AND UPPER(s.COUNTRYCODE) = ? "
                        + " AND UPPER(s.CITYNAME) = ? "
                        + " AND UPPER(s.ADDRESS1) = ? ");

                ps.setString(1, siteInfo.getCustCode());
                ps.setString(2, siteInfo.getPostalCode() != null ? siteInfo.getPostalCode().toUpperCase() : "");
                ps.setString(3, siteInfo.getCountryCode() != null ? siteInfo.getCountryCode().toUpperCase() : "");
                ps.setString(4, siteInfo.getCityName() != null ? siteInfo.getCityName().toUpperCase() : "");
                ps.setString(5, siteInfo.getAddress1() != null ? siteInfo.getAddress1().toUpperCase() : "");
                rs = ps.executeQuery();

                int count = 0;
                while (rs.next()) {
                    //searching how many records existed and use the last one found
                    ret = rs.getString(1);
                    count++;
                }
                if (count > 1)
                    m_logger.debug("site temp table has more than one records for the same address:" + ret);
            } catch (SQLException e) {
                throw new CSIException("Can't perform query in getCISSiteIDFromTempTable(\"" + siteInfo.getCustCode() + "\", ...)", e);
            } finally {
                carefullyClose(rs, ps);
                rs = null;
                ps = null;
            }
        }
        return ret;
    }

    /**
     * Gets Order(s) associated to by USID of ServiceElement and its status 
     * @param usid USID of ServiceElement 
     * @param status ServiceElement status
     * @return list of associated orders
     * @throws CSIException in case of any SQL exeptions during work with DB. 
     */
    public List getOrderHandlesByStatus( String usid, String status) throws CSIException {
        List ret = new ArrayList();

        PreparedStatement ps = null;
        ResultSet rs = null;

        debug("getOrderHandlesByStatusStatus(\"" + usid + "\",\"" + status + "\")");

        try {
            // get customer and site
            ps = m_connection.prepareStatement(
                    "SELECT DISTINCT v.ORDHANDLE                                                     "
                  + "FROM   CSERVICEELEMENT se,                                                      "
                  + "       CVERSION v,                                                              "
                  + "       CVERSIONSERVICEELEMENT vse,                                              "
                  + "       CVERSIONSERVICEELEMENTSTS vses                                           "
                  + "WHERE  se.SERVICEELEMENTID = vse.SERVICEELEMENTID                               "
                  + "  AND  vse.VERSIONSERVICEELEMENTID = vses.VERSIONSERVICEELEMENTID               "
                  + "  AND  v.VERSIONID = vse.VERSIONID                                              "
                  + "  AND  vses.statustypecode = ?                                                  "
                  + "  AND  se.USID = ?                                                              "
            );
            ps.setString(1, status);
            ps.setString(2, usid);
            rs = ps.executeQuery();

            // collect all Orders Handles to array 
            while(rs.next()) {
                ret.add(rs.getString(1));
            }
        } catch (SQLException e) {
            info("Can't perform query in getOrderHandlesByStatusStatus(\"" + usid + "\",\"" + status + "\")", e);
            throw new CSIRollbackException("Can't perform query in getOrderHandlesByStatusStatus(\"" + usid + "\",\"" + status + "\")", e);
        } finally {
            carefullyClose(rs, ps); rs=null; ps=null;
        }

        return ret;
    }    
}