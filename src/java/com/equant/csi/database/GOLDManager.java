package com.equant.csi.database;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.equant.csi.jaxb.ObjectFactory;
import com.equant.csi.jaxb.ServiceElementStatusType;
import com.equant.csi.jaxb.ServiceElementType;
import com.equant.csi.jaxb.SiteInformation;
import com.equant.csi.jaxb.SiteInformationType;
import com.equant.csi.jaxb.SiteContactType;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.exceptions.CSIException;
import com.equant.csi.utilities.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: szhu
 * Date: Jun 13, 2005
 * Time: 4:24:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class GOLDManager {
    protected Connection m_csi_connection = null;
    protected Connection m_gold_connection = null;
    public static final int ADDRESS_LENGTH_MAX = 50;
    public static final String SITE_FIELD = "CIS";
    public static final int CIS_SITE_CODE_LEN = 7;
    public static final String GOLD_STATUS_CLOSED = "Closed";

    protected static final Category cis_logger = LoggerFactory.getInstance("cis_extraction.GOLDManager");

    /**
     * Constructor
     *
     * @param csiconnection
     * @param goldconnection
     */
    public GOLDManager(Connection csiconnection, Connection goldconnection) {
        m_csi_connection = csiconnection;
        m_gold_connection = goldconnection;
    }

    //helper methods

    /**
     * Checks the address length. If address longer than <code>maxLen</code>, value will be
     * broken up into <code>maxLen</code> characters blocks using the space nearest to the end
     * of each block or any commas in the value as a separator.
     *
     * @param siteInfo
     * @param maxLen
     */
    public static final void normalizeAddress(SiteInformation siteInfo, int maxLen) {
        if (siteInfo == null) return;

        String[] res = {null, null, null};
        if (siteInfo.getAddress1() != null && siteInfo.getAddress1().length() > maxLen) {
            int pos = 0;
            String address = siteInfo.getAddress1() + " " +
                    StringUtils.defaultString(siteInfo.getAddress2()) + " " +
                    StringUtils.defaultString(siteInfo.getAddress3());
            for (int i = 0; i < 3; i++) {
                int endPos = findTokenEnd(address, pos, maxLen);
                res[i] = StringUtils.strip(StringUtils.substring(address, pos, endPos));
                pos = endPos;
            }

            siteInfo.setAddress1(res[0]);
            siteInfo.setAddress2(res[1]);
            siteInfo.setAddress3(res[2]);
        }
    }

    private static final int findTokenEnd(String str, int pos, int maxLen) {
        int tokenSpaceLen = StringUtils.lastIndexOf(str, ' ', pos + maxLen - 1);
        int tokenCommaLen = StringUtils.lastIndexOf(str, ',', pos + maxLen - 1) + 1;

        int res = (tokenSpaceLen > tokenCommaLen ? tokenSpaceLen : tokenCommaLen);
        return (res <= pos) ? pos + maxLen : res;
    }

    /**
     * Reference to GOLD: siteExists@SiteHelper.java
     *
     * @param siteInfo
     * @return
     */
    public String findSiteID(SiteInformationType siteInfo) throws CSIException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String ret = "";

        if (siteInfo != null && siteInfo.getCustCode() != null && siteInfo.getCustCode().length() > 0) {
            try {
                ps = m_gold_connection.prepareStatement(" SELECT t.SITECODE "
                        + " FROM  EQ_SITE t, SC_ADDRESS s, SC_ORGANIZATION o "
                        + " WHERE t.SITEADDRESS = s.TRIL_GID "
                        + " AND t.EQ_SITEOF = o.TRIL_GID "
                        + " AND o.ORGANIZATIONID = ?"
                        + " AND UPPER(s.ZIPCODE) = ? "
                        + " AND UPPER(s.COUNTRY) = ? "
                        + " AND UPPER(s.CITY) = ? "
                        + " AND UPPER(s.STREET1) = ? ");
                ps.setString(1, siteInfo.getCustCode() != null ? siteInfo.getCustCode().toUpperCase() : "");
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
                    cis_logger.debug("GOLD Site search has more than one records for the same address:" + ret);
            } catch (SQLException e) {
                throw new CSIException("Can't perform query in getCISSiteIDFromTempTable(\"" + siteInfo.getCustCode() + "\", ...)", e);
            } finally {
                carefullyClose(rs, ps);
            }
        }
        return ret;

    }

    /**
     * Populate SiteInformation by siteID from GOLD
     *
     * @param siteInfo
     * @return
     */
    public SiteInformationType getSiteInfoByID(String siteID) throws CSIException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        SiteInformationType siteInfo = null;
        
        if (StringUtils.isBlank(siteID)) {
            return null;
        }
        
        try {
            ps = m_gold_connection.prepareStatement(
                      " SELECT SITECODE, ZIPCODE , COUNTRY, CITY, STREET1, ORGANIZATIONID "
                    + " FROM  EQ_SITE t, SC_ADDRESS s, SC_ORGANIZATION o "
                    + " WHERE t.SITEADDRESS = s.TRIL_GID "
                    + " AND t.EQ_SITEOF = o.TRIL_GID "
                    + " AND UPPER(t.SITECODE) = ? ");
            ps.setString(1, siteID.toUpperCase());
            rs = ps.executeQuery();

            if (rs.next()) {
                //searching how many records existed and use the last one found
                siteInfo = (new ObjectFactory()).createSiteInformationType();
                siteInfo.setAddress1(rs.getString("STREET1"));
                siteInfo.setPostalCode(rs.getString("ZIPCODE"));
                siteInfo.setCountryCode(rs.getString("COUNTRY"));
                siteInfo.setCityName(rs.getString("CITY"));
                siteInfo.setCustCode(rs.getString("ORGANIZATIONID"));
                siteInfo.setCISSiteId(siteID);
            }else {
                cis_logger.debug("getSiteInfoByID: Can't find GOLD site by siteId = "+siteID);
            }
        } catch (SQLException e) {
            throw new CSIException("Can't perform query in getSiteInfoByID: (\"" + siteID + "\", ...)", e);
        } catch (JAXBException e) {
            throw new CSIException("getSiteInfoByID: JAXB error!", e);
        } finally {
            carefullyClose(rs, ps);
            rs = null;
            ps = null;
        }
        
        return siteInfo;
    }

    /**
     * Reference to GOLD: isContactValid@CSISiteValidationHandler.java
     *
     * @param siteInfo:
     * @return
     */
    public boolean isContactValid(SiteInformationType siteInfo) {
        boolean result = true;
        if (siteInfo.getSiteContact() == null || siteInfo.getSiteContact().size() == 0) {
            result = false;
        } else {
            SiteContactType siteContact = (SiteContactType) siteInfo.getSiteContact().iterator().next();
            if (StringUtils.isBlank(siteContact.getContactName())) {
                result = false;
            }
            if (StringUtils.isBlank(siteContact.getTelephoneNumber())) {
                result = false;
            }
        }
        return result;
    }

    /**
     * @param siteInfo
     * @return
     */
    public String generateSiteID(SiteInformationType siteInfo) {
        String siteID = "";
        if (siteInfo != null) {
            if (StringUtils.isBlank(siteInfo.getCISSiteId())) {
                siteID = getNextSiteCode(getNextSiteIDSEQ());
            } else {
                siteID = siteInfo.getCISSiteId();
            }
        }

        return siteID;
    }

    protected void carefullyClose(ResultSet rs, Statement ps) {
        try {
            if (rs != null) rs.close();
        } catch (Exception e) {
            cis_logger.error("ResultSet was not closed successfully", e);
        }

        try {
            if (ps != null) ps.close();
        } catch (Exception e) {
            cis_logger.error("Statement was not closed succesfully", e);
        }
    }

    /**
     * Reference to GOLD: getNextId@EQIDNumberGenerator.java
     * @return
     */
    protected String getNextSiteIDSEQ() {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String ret = "";
        try {
            ps = m_csi_connection.prepareStatement(" SELECT SEQSITECODE.nextval FROM DUAL  ");
            rs = ps.executeQuery();

            if (rs.next()) {
                ret = rs.getString(1);
            }
        } catch (SQLException e) {
            cis_logger.error("Can't perform query in getNextSiteID.", e);
        } finally {
            carefullyClose( rs, ps);
        }
        return ret;
    }

    /**
     * Reference to GOLD: getCISSiteCode@EQIDNumberGenerator.java
     * @param siteCodeSeq
     * @return
     */
    protected String getNextSiteCode(String siteCodeSeq) {
        String siteCode = StringUtils.leftPad(siteCodeSeq, CIS_SITE_CODE_LEN, '0');
        if (siteCode.length() > CIS_SITE_CODE_LEN) {
            cis_logger.error("CIS Site Code has become bigger than " + CIS_SITE_CODE_LEN + " characters.");
        }
        return SITE_FIELD + siteCode;
    }
    
    /**
     * Returns map of not closed orders with their statuses
     * @param orderList
     * @return map of not closed orders with their statuses: key=orderNumber, value=GOLD.EQStatus
     * @throws CSIException
     */
    public Map getNotClosedOrders(List orderList) throws CSIException {

        Map result = new HashMap();

        PreparedStatement ps=null;
        ResultSet rs=null;

        Iterator orders = orderList.iterator();
        if (! orders.hasNext()) {
            return result;
        }

        String orderListQueryPart = '\'' + StringUtils.join( orders, "', '") + '\'';

        try {
            ps = m_gold_connection.prepareStatement(
                    "SELECT DISTINCT s.EQ_STATUS, q.QUOTENUMBER FROM SC_QUOTE q, EQ_ORDERSTATUS s "
                  + " WHERE q.QUOTENUMBER IN ( " + orderListQueryPart + " ) "
                  + " AND q.TRIL_GID=s.EQ_ORDERGID"
            );

            rs = ps.executeQuery();

            //Get statuses of all assigned and not closed orders
            while (rs.next()) {
                String status = rs.getString(1);
                if (!GOLD_STATUS_CLOSED.equals( status)) {
                    result.put( rs.getString(2), status);
                }
            }
            
        } catch (SQLException e) {
            throw new CSIException(e);
        } finally {
            carefullyClose(rs,ps);
        }

        return result;
    }    

    /**
     * Check existance of input orders in the GOLD. 
     * 
     * @param orderList orders IDs to be cheked.
     * @return intersection of input set and existed orders in GOLD db.
     * @throws CSIException
     */
    public List getExistedOrdersIds( List orderList) throws CSIException {

        if( orderList.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        List result = new ArrayList();

        PreparedStatement ps=null;
        ResultSet rs=null;

        String orderListQueryPart = '\'' +
            StringUtils.join( orderList.iterator(), "', '") + '\'';

        try {
            ps = m_gold_connection.prepareStatement(
                    "SELECT DISTINCT quoteNumber FROM sc_quote "
                  + " WHERE quoteNumber IN ( " + orderListQueryPart + " )");

            rs = ps.executeQuery();

            //Get statuses of all assigned and not closed orders
            while (rs.next()) {
                result.add( rs.getString( 1));
            }
        } catch( SQLException e) {
            throw new CSIException( e);
        } finally {
            carefullyClose( rs, ps);
        }

        return result;
    }    
}
