package com.equant.csi.interfaces.cis;

import com.equant.csi.common.TransDate;
import com.equant.csi.common.CSIConstants;
import com.equant.csi.exceptions.CISException;
import com.equant.csi.ejb.biz.ServiceManager;
import com.equant.csi.utilities.LoggerFactory;
import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.Version;
import com.equant.csi.jaxb.SiteInformation;

import javax.xml.bind.JAXBException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Category;

/**
 * Created by IntelliJ IDEA.
 * User: szhu
 * Date: April 12, 2005
 * Time: 11:54:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class CISIPProduct extends CISProductBase {
    protected Hashtable m_ip_mapping = null;
    protected Hashtable m_ip_value_mapping = null;

    protected Vector m_ip_ac_query;
    protected Vector m_ip_pvc_query;
    protected Vector m_ip_router_query;
    protected Vector m_ip_nasbackup_query;
    protected Vector m_ip_router_query_2;
    protected Vector m_ip_nasbackup_query_2;

    protected String m_csi_product_name = "";

    protected final static String QUERYTYPE_AC = "ACQUERY";
    protected final static String QUERYTYPE_MESHING = "PVCQUERY";
    protected final static String QUERYTYPE_ROUTER = "ROUTERQUERY";
    protected final static String QUERYTYPE_NASBACKUP = "NASBACKUPQUERY";

    /**
     * Initializing the logger.
     */
    protected static final Category cis_logger = LoggerFactory.getInstance("cis_extraction.CISIPProduct");


    public CISIPProduct(Connection sybase, Connection local, int runType, ServiceManager serviceManager, Connection goldConn, String ipProduct) {
        super(sybase, local, runType, serviceManager, goldConn, ipProduct);
    }

    /**
     * @return
     */
    protected String getCISACProductName() {
        return m_product;
    }

    protected String getCISPVCProductName() {
        return m_product;
    }

    protected String getCISRouterProductName() {
        return m_product;
    }

    protected String getCISNasBackupProductName() {
        return m_product;
    }

    /**
     * @return
     */
    protected Hashtable getProductFieldMappingTable() {
        return m_ip_mapping;
    }

    /**
     * @param mapping
     */
    protected void setProductFieldMappingTable(Hashtable mapping) {
        m_ip_mapping = mapping;
    }

    /**
     *  valid router/nasbackup status
     */
    protected final static Hashtable validRouterStatus = new Hashtable();

    static {
        validRouterStatus.put(CISConstants.CIS_ROUTER_STATUS_MIG, CISConstants.CIS_ROUTER_STATUS_MIG);
        validRouterStatus.put(CISConstants.CIS_ROUTER_STATUS_ONHOLD, CISConstants.CIS_ROUTER_STATUS_ONHOLD);
        validRouterStatus.put(CISConstants.CIS_ROUTER_STATUS_PLA, CISConstants.CIS_ROUTER_STATUS_PLA);
        validRouterStatus.put(CISConstants.CIS_ROUTER_STATUS_CFG, CISConstants.CIS_ROUTER_STATUS_CFG);
        validRouterStatus.put(CISConstants.CIS_ROUTER_STATUS_INT, CISConstants.CIS_ROUTER_STATUS_INT);
        validRouterStatus.put(CISConstants.CIS_ROUTER_STATUS_OL, CISConstants.CIS_ROUTER_STATUS_OL);
        validRouterStatus.put(CISConstants.CIS_ROUTER_STATUS_OUT, CISConstants.CIS_ROUTER_STATUS_OUT);
    }

    /**
     * @return
     * @throws CISException
     */
    protected String run() throws CISException {
        m_extractLogMsg = "";
        try {
            setCSIProductNameFromMappingTable();
            initMapping(true);
            initIPQueryString();
            m_extractLogMsg += runServiceEelement(CSIConstants.SECLASS_ACCESSCONNECTION);
            m_extractLogMsg += runServiceEelement(CSIConstants.SECLASS_CPE);
            //changed the second part qurey to do all
            m_extractLogMsg += runServiceEelement(CSIConstants.SECLASS_CPE, true);
            m_extractLogMsg += runServiceEelement(CSIConstants.SECLASS_TRANSPORT);
            if (!CISConstants.CSI_PRODUCT_INTERNETDIRECT.equals(getCSIProductByServiceOrig(null))) {
                m_extractLogMsg += runServiceEelement(CSIConstants.SECLASS_NASBACKUP);
                //changed the second part query to do all
                m_extractLogMsg += runServiceEelement(CSIConstants.SECLASS_NASBACKUP, true);
            }

            //clean up the hashtable
            Hashtable hs = getRemoteUSIDAttrs();
            if (hs != null) {
                hs.clear();
            }

        } catch (java.sql.SQLException sqle) {
            if (cis_logger.isDebugEnabled()) cis_logger.fatal(sqle);
        }
        return m_extractLogMsg;
    }

    /**
     * @return
     */
    protected Vector getProductACQuery() {
        return m_ip_ac_query;
    }

    /**
     * @return
     */
    protected Vector getProductPVCQuery() {
        return m_ip_pvc_query;
    }

    /**
     * @return
     */
    protected Vector getProductRouterQuery() {
        return m_ip_router_query;
    }

     /**
     * @return
     */
    protected Vector getProductRouterQuery2() {
        return m_ip_router_query_2;
    }

    /**
     * @return
     */
    protected Vector getProductNasBackupQuery() {
        return m_ip_nasbackup_query;
    }

    /**
     * @return
     */
    protected Vector getProductNasBackupQuery2() {
        return m_ip_nasbackup_query_2;
    }


    /**
     * @param query
     */
    protected void setProductPVCQuery(Vector query) {
        m_ip_pvc_query = query;
    }

    /**
     * @param query
     */
    protected void setProductRouterQuery(Vector query) {
        m_ip_router_query = query;
    }

    /**
     * @param query
     */
    protected void setProductRouterQuery2(Vector query) {
        m_ip_router_query_2 = query;
    }

    /**
     * @param query
     */
    protected void setProductNasBackupQuery(Vector query) {
        m_ip_nasbackup_query = query;
    }

    /**
     * @param query
     */
    protected void setProductNasBackupQuery2(Vector query) {
        m_ip_nasbackup_query_2 = query;
    }



    /**
     * @return
     */
    protected Hashtable getProductValueMappingTable() {
        return m_ip_value_mapping;
    }

    /**
     * @param mapping
     */
    protected void setProductValueMappingTable(Hashtable mapping) {
        m_ip_value_mapping = mapping;

    }

    /**
     * @return
     */
    protected String getCSIProductNameFromMappingTable() {
        return m_csi_product_name;
    }

    /**
     *
     */
    protected void setCSIProductNameFromMappingTable() {
        if (CISConstants.CIS_PRODUCT_LANAS.equals(m_product)) {
            m_csi_product_name = CISConstants.CSI_PRODUCT_LANAS;
        } else if (CISConstants.CIS_PRODUCT_MANAGED_CPE.equals(m_product)) {
            m_csi_product_name = CISConstants.CSI_PRODUCT_MANAGED_CPE;
        } else if (CISConstants.CIS_PRODUCT_INTERNETDIRECT.equals(m_product)) {
            m_csi_product_name = CISConstants.CSI_PRODUCT_INTERNETDIRECT;
        } else if (CISConstants.CIS_PRODUCT_INTRANETCONNECT.equals(m_product)) {
            m_csi_product_name = CISConstants.CSI_PRODUCT_INTRANETCONNECT;
        } else {   //default to IPVPN
            m_csi_product_name = CISConstants.CSI_PRODUCT_IPVPN;
        }
    }

    /**
     * @param serviceOrig
     * @return
     */
    protected String getCSIProductByServiceOrig(String serviceOrig) {
        //ignore the service origin mapping
        return getCSIProductNameFromMappingTable();
    }

    /**
     * @param query
     */
    protected void setProductACQuery(Vector query) {
        m_ip_ac_query = query;
    }

   /**
    *
    */
   protected String runServiceEelement(String serviceElementRun) throws CISException {
       return runServiceEelement(serviceElementRun, false);
   }
    /**
     * @param serviceElementRun
     * @param isSecondPart:  true for router and nasbackup sencond part
     */
    protected String runServiceEelement(String serviceElementRun, boolean isSecondPart) throws CISException {
        cis_logger.info("starting run for " + m_product + " " + serviceElementRun + " ...");
        m_extractLogMsg = "";
        long startTime = System.currentTimeMillis();
        int correctRecord;
        int errorRecord;
        ResultSet result = null;
        PreparedStatement ps = null;

        try {
            String status = null;
            String subStatus = null;
            String category = null;
            correctRecord = 0;
            errorRecord = 0;
            String usid = "";

            Vector queryVec = getQueryBuffer(serviceElementRun, isSecondPart);

            for (int i = 0; i < queryVec.size(); i++) {
                cis_logger.info("Query loop:" + i);
                StringBuffer queryBuffer = new StringBuffer();
                queryBuffer.append((String) queryVec.get(i));

                ps = m_sybaseConn.prepareStatement(queryBuffer.toString());
                result = getResultSetFromQuery(queryBuffer, ps);

                initObjectFactory();

                while (result.next()) {
                    try {
                        usid = getUSIDFromResultSet(serviceElementRun, result);

                        cis_logger.info("Retrieved FROM CIS|" + m_product + "|" + serviceElementRun + "|" + "USID|" + usid);

                        Message m_message = initGeneralObject();
                        //set Version properties
                        Version m_version = (Version) m_message.getVersion().get(0); //only one version for each message
                        String custId = result.getString("CUSTCODE");
                        m_version.setEndUserId(custId);
                        m_version.setCustomerId(custId);
                        m_version.setServiceId(getCSIProductByServiceOrig(null));

                        TransDate lastModifDate = convertSQLTimeToTransdate(getLastModifDate(result));
                        m_version.setOrderSentDate(lastModifDate);

                        if (CSIConstants.SECLASS_CPE.equals(serviceElementRun) || CSIConstants.SECLASS_NASBACKUP.equals(serviceElementRun)) {
                            status = result.getString("ConfigStatus");
                            if (status != null && validRouterStatus.containsKey(status)) {
                                category = setCISStatusToCSIStatusForRouter(status, m_version);
                            } else {
                                cis_logger.info(m_product + "|" + serviceElementRun + "|record:|" + usid + "|has invalid ConfigStatus|" + status + "|. Stop to handle this record.");
                                continue;
                            }
                        } else {
                            //set service elemet status
                            status = result.getString("SERVICESTATUS");
                            subStatus = result.getString("SERVICESUBSTATUS");
                            if (null == status || CISConstants.CIS_STATUS_CANCELLED.equals(status)) continue; //do nothing for this record
                            category = setCISStatusToCSIStatus(status, subStatus, m_version);
                        }

                        //set site
                        SiteInformation m_site = (SiteInformation) m_message.getSiteInformation();
                        if (!setSiteInformation(result, m_site, getSiteErrorMsgHeader(m_product, serviceElementRun, usid))) continue;
                        // cis migration
                        //m_site.setLastModifDate(convertSQLTimeToTransdate(result.getTimestamp("CUSTOMERLASTMODIFDATE")));
                        m_site.setLastModifDate(convertSQLTimeStringToTransdate(result.getString("CUSTOMERLASTMODIFDATE")));

                        setServiceElementAndAttr(result, serviceElementRun, category, m_version);

                        //Backup
                        if (CSIConstants.SECLASS_ACCESSCONNECTION.equals(serviceElementRun)) {
                            String backupService = result.getString("ServiceSupplements");
                            if (backupService != null && backupService.length() > 0) {
                                setServiceElementAndAttr(result, CSIConstants.SECLASS_BACKUP, category, m_version);
                            } else {
                                //add new version to disconnect it
                                Version version = addBackupDisconnectVersion(m_version, result);
                                m_message.getVersion().add(version);
                            }
                        }

                        //SO
                        if (CSIConstants.SECLASS_ACCESSCONNECTION.equals(serviceElementRun)) {
                            setServiceElementAndAttr(result, CSIConstants.SECLASS_SERVICEOPTIONS, category, m_version, QUERYTYPE_AC);
                        } else if (CSIConstants.SECLASS_CPE.equals(serviceElementRun)) {  //run for all records
                            setServiceElementAndAttr(result, CSIConstants.SECLASS_SERVICEOPTIONS, category, m_version, QUERYTYPE_ROUTER);
                        }

                        Runtime runtime = Runtime.getRuntime();

                        if (cis_logger.isDebugEnabled())
                            cis_logger.debug("Before Call ServiceManagerEJB. Memory:" + runtime.totalMemory() + "|" + runtime.freeMemory());
//                    String msg = JAXBUtils.marshalMessage(m_message);
//                    cis_logger.debug("message:" + msg);
                        m_serviceManager.processMessageFromCis(m_message);
                        if (cis_logger.isDebugEnabled())
                            cis_logger.debug("After Call ServiceManagerEJB. Memory:" + runtime.totalMemory() + "|" + runtime.freeMemory());

                        correctRecord++;
                    } catch (Exception e) {
                        errorRecord++;
                        cis_logger.error("Error while handle " + m_product + " " + serviceElementRun + " records:" + usid);
                        cis_logger.error("Detail errors:", e);
                    }
                }
            }

            cis_logger.info("Number of " + m_product + " " + serviceElementRun + " records successed:" + correctRecord + " records failed:" + errorRecord);
        } catch (SQLException sqle) {
            if (cis_logger.isDebugEnabled()) cis_logger.fatal("SQL Exception", sqle);
            throw new CISException("SQL Exception:", sqle);
        } catch (JAXBException jaxb) {
            if (cis_logger.isDebugEnabled()) cis_logger.fatal("JAXBException", jaxb);
            throw new CISException("JAXBException:", jaxb);
        } finally {
            closeResultSet(ps, result);
            result = null;
            ps = null;
        }

        long endTime = System.currentTimeMillis();

        m_extractLogMsg = CISConstants.CIS_EXTRACTMSG_SEPARATER + m_product + " " + serviceElementRun + " records successed:" + correctRecord + " records failed:" + errorRecord
                + ". It took:" + (endTime - startTime) / 1000 + " Seconds.";
        cis_logger.info("end " + m_product + " " + serviceElementRun + " Run.  It took: " + (endTime - startTime) / 1000 + " Seconds.");
        return m_extractLogMsg;
    }

    protected String getUSIDFromResultSet(String serviceElementRun, ResultSet result) throws SQLException {
        String usid = "";
        if (CSIConstants.SECLASS_TRANSPORT.equals(serviceElementRun)) {
            usid = result.getString("MESHINGUSID");
        } else {
            usid = result.getString("USID");
        }
        return usid;
    }

    protected Vector getQueryBuffer(String serviceElementRun, boolean isSecondQuery) {
        Vector queryVec = new Vector();
        if (CSIConstants.SECLASS_ACCESSCONNECTION.equals(serviceElementRun)) {
            queryVec = getProductACQuery();
        } else if (CSIConstants.SECLASS_TRANSPORT.equals(serviceElementRun)) {
            queryVec = getProductPVCQuery();
        } else if (CSIConstants.SECLASS_CPE.equals(serviceElementRun)) {
            if (isSecondQuery) {
                queryVec = getProductRouterQuery2();
            } else {
                queryVec = getProductRouterQuery();
            }
        } else if (CSIConstants.SECLASS_NASBACKUP.equals(serviceElementRun)) {
            if (isSecondQuery) {
                queryVec = getProductNasBackupQuery2();
            } else {
                queryVec = getProductNasBackupQuery();
            }
        }
        return queryVec;
    }

    /**
     * @throws SQLException
     */
    protected void initIPQueryString() throws SQLException, CISException {
        Vector custId = getCustomerConditionString();
        initIPACQuery(custId);
        initIPPVCQuery(custId);
        initIPRouterQuery(custId);
        initIPNasBackupQuery(custId);
    }

    /**
     * @param custId
     * @throws SQLException
     */
	 //cis migration change
    protected void initIPPVCQuery(Vector custId) throws SQLException, CISException {
        if (cis_logger.isDebugEnabled())
            cis_logger.info("get into initIPPVCQuery...");

        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            Vector pvcqueryVec = getProductPVCQuery();
            ps = m_localConn.prepareStatement("SELECT CISTABLENAME, CISCOLUMNNAME FROM CCISMAPPING WHERE (CSISERVICE = 'ALL' OR CSISERVICE like '%" + getCSIProductNameFromMappingTable() + "%') AND DESCRIPTION = ? AND CISTABLENAME IS NOT NULL");

            if (pvcqueryVec == null || (pvcqueryVec != null && pvcqueryVec.isEmpty())) {
                pvcqueryVec = new Vector();
                StringBuffer pvcquery = new StringBuffer();
                pvcquery.append("\n SELECT ");
                ps.setString(1, QUERYTYPE_MESHING);
                result = ps.executeQuery();
                String shortTableName = "";
                String tableName = "";
                int columnCount = 0;

                while (result.next()) {
                    tableName = result.getString(1);
                    if (tableName.equals(CISConstants.CIS_TABLENAME_IP_PVC)) {
                        shortTableName = "p.";
                    } else if (tableName.equals(CISConstants.CIS_TABLENAME_IP_AC)) {
                        shortTableName = "a.";
                    } else {
                        shortTableName = "p1.";
                    }
                    if (columnCount == 0) {
                        pvcquery.append("  " + shortTableName + result.getString(2) + " \n");
                    } else {
                        pvcquery.append(", " + shortTableName + result.getString(2) + " \n");
                    }

                    columnCount++;
                }

                pvcquery.append(" FROM " + CISConstants.CIS_TABLENAME_IP_PVC + " p, " + CISConstants.CIS_TABLENAME_IP_AC + " a, " + CISConstants.CIS_TABLENAME_PVC + " p1 ");
                pvcquery.append(" WHERE a.S_TYPE = '" + getCISPVCProductName() + "' ");
                pvcquery.append(" AND a.SERVICESTATUS != '" + CISConstants.CIS_STATUS_CANCELLED + "' ");
                pvcquery.append(" AND a.SERVICEORIGIN != 'Multiservice' ");
                pvcquery.append(" AND a.SERVICEORIGIN is not null ");
                pvcquery.append(" AND p.LocalAccessConnectionUSID = a.USID ");
                pvcquery.append(" AND p.MESHINGUSID = p1.PVCUSID (+) ");
                pvcquery.append(" AND p1.PURELAYER2FLAG = 'No'");
                pvcquery.append(" AND LENGTH(p.MESHINGUSID) >= 10 ");
                pvcquery.append(getAddressCondition("a."));
                String whereDateClause = " p.lastmodifdate > ";
                addLastModifDateCond(pvcquery, whereDateClause);
                if (cis_logger.isDebugEnabled()) {
                    cis_logger.info("Total PVC Columns:" + columnCount);
                    cis_logger.info("PVC Query String Without Custcode Condition:" + pvcquery.toString());
                }
                formMultipleQuery(custId, pvcqueryVec, pvcquery, "a.CUSTCODE");
                setProductPVCQuery(pvcqueryVec);
            }
            closeResultSet(ps, result);
            if (cis_logger.isDebugEnabled())
                cis_logger.info("end initIPPVCQuery.");
        } finally {
            closeResultSet(ps, result);
            result = null;
            ps = null;                        
        }
    }

    /**
     * @param custId
     * @throws SQLException
     */
	 //cis migration change
    protected void initIPACQuery(Vector custId) throws SQLException, CISException {
        if (cis_logger.isDebugEnabled())
            cis_logger.info("get into initIPACQuery...");

        PreparedStatement ps = null;
        ResultSet result = null;

        try {
            //pop access query
            ps = m_localConn.prepareStatement("SELECT CISTABLENAME, CISCOLUMNNAME FROM CCISMAPPING WHERE (CSISERVICE = 'ALL' OR CSISERVICE like '%" + getCSIProductNameFromMappingTable() + "%') AND DESCRIPTION = ? AND CISTABLENAME IS NOT NULL");


            Vector acqueryVec = getProductACQuery();
            if (acqueryVec == null || (acqueryVec != null && acqueryVec.isEmpty())) {
                acqueryVec = new Vector();
                StringBuffer acquery = new StringBuffer();
                acquery = new StringBuffer();
                acquery.append("\nSELECT DISTINCT ");
                ps.setString(1, QUERYTYPE_AC);
                result = ps.executeQuery();
                String tableShortName = "";
                String tableFullName = "";
                int columnCount = 0;
                boolean needJoinMeshing = false;

                while (result.next()) {
                    tableFullName = result.getString(1);
                    if (CISConstants.CIS_TABLENAME_IP_AC.equals(tableFullName)) {
                        tableShortName = "a1.";
                    } else if (CISConstants.CIS_TABLENAME_AC.equals(tableFullName)) {
                        tableShortName = "a2.";
                    } else if (CISConstants.CIS_TABLENAME_IP_PVC.equals(tableFullName)) {
                        tableShortName = "p.";
                        needJoinMeshing = true;
                    }
                    if (columnCount == 0) {
                        acquery.append("  " + tableShortName + result.getString(2) + " \n");
                    } else {
                        acquery.append(",  " + tableShortName + result.getString(2) + " \n");
                    }
                    columnCount++;
                }

				//cis migration
                // acquery.append(",  AccessConnectionName = 'Access Connection' ");
                // acquery.append(",  TerminationInterface = IsNULL(a1.TerminationInterface, a2.TerminationInterface)");
                acquery.append(", 'Access Connection' as AccessConnectionName ");
                acquery.append(",  NVL(a1.TerminationInterface, a2.TerminationInterface) As TerminationInterface ");
                
                if (needJoinMeshing) {
                    acquery.append(" FROM " + CISConstants.CIS_TABLENAME_IP_AC + " a1, " + CISConstants.CIS_TABLENAME_AC + " a2, " + CISConstants.CIS_TABLENAME_IP_PVC + " p ");
                } else {
                    acquery.append(" FROM " + CISConstants.CIS_TABLENAME_IP_AC + " a1, " + CISConstants.CIS_TABLENAME_AC + " a2 ");
                }
                acquery.append(" WHERE a1.S_TYPE = '" + getCISACProductName() + "' ");
                acquery.append(" AND a1.SERVICESTATUS != '" + CISConstants.CIS_STATUS_CANCELLED + "' ");
                acquery.append(" AND a1.SERVICEORIGIN != 'Multiservice' ");
                acquery.append(" AND a1.SERVICEORIGIN is not null ");
                acquery.append(" AND a1.USID = a2.USID (+) ");
                if (needJoinMeshing) {
                    acquery.append(" AND a1.USID = p.LocalAccessConnectionUSID (+) ");
                }
                acquery.append(" AND a2.PURELAYER2FLAG = 'No' ");
                acquery.append(getAddressCondition("a1."));
                String whereDateClause = " a1.lastmodifdate > ";
                addLastModifDateCond(acquery, whereDateClause);
                if (cis_logger.isDebugEnabled())
                    cis_logger.debug("AC Query String Without Custcode Condition:" + acquery.toString());
                formMultipleQuery(custId, acqueryVec, acquery, "a1.CUSTCODE");
                setProductACQuery(acqueryVec);
            }

            if (cis_logger.isDebugEnabled())
                cis_logger.info("end initIPACQuery.");
        } finally {
            closeResultSet(ps, result);
            result = null;
            ps = null;
        }
    }

    /**
     * @param custId
     * @throws SQLException
     */
	 //cis migration change
    protected void initIPRouterQuery(Vector custId) throws SQLException, CISException {
        if (cis_logger.isDebugEnabled())
            cis_logger.info("get into initIPRouterQuery...");

        PreparedStatement ps = null;
        ResultSet result = null;
        
        try {
            ps = m_localConn.prepareStatement("SELECT CISTABLENAME, CISCOLUMNNAME FROM CCISMAPPING WHERE (CSISERVICE = 'ALL' OR CSISERVICE like '%" + getCSIProductNameFromMappingTable() + "%') AND DESCRIPTION = ? AND CISTABLENAME IS NOT NULL");

            Vector routerQueryVec = getProductRouterQuery();
            Vector routerQuery2Vec = getProductNasBackupQuery2();
            if (routerQueryVec == null || routerQuery2Vec == null || (routerQueryVec !=null && routerQueryVec.isEmpty()) || (routerQuery2Vec != null && routerQuery2Vec.isEmpty())) {
                routerQueryVec = new Vector();
                routerQuery2Vec = new Vector();
                StringBuffer routerQuery = new StringBuffer();
                StringBuffer routerQuery2 = new StringBuffer();
                routerQuery.append("\n SELECT DISTINCT ");
                ps.setString(1, QUERYTYPE_ROUTER);
                result = ps.executeQuery();
                String tableShortName = "";
                int columnCount = 0;

                while (result.next()) {
                    tableShortName = result.getString(1).equals(CISConstants.CIS_TABLENAME_IP_ROUTER) ? "r." : "c.";
                    if (columnCount == 0) {
                        routerQuery.append("  " + tableShortName + result.getString(2) + " \n");
                    } else {
                        routerQuery.append(",  " + tableShortName + result.getString(2) + " \n");
                    }
                    columnCount++;
                }

                routerQuery2.append(routerQuery.toString());

                //router query 1
                routerQuery.append(", a.S_Type ");
                routerQuery.append(" FROM " + CISConstants.CIS_TABLENAME_IP_ROUTER + " r, " + CISConstants.CIS_TABLENAME_IP_CHASSIS + " c, " + CISConstants.CIS_TABLENAME_IP_AC + " a ");
                routerQuery.append(" WHERE a.S_Type = '" + getCISRouterProductName() + "' ");
                routerQuery.append(" AND r.SERVICESTATUS != '" + CISConstants.CIS_STATUS_CANCELLED + "' ");
                routerQuery.append(" AND r.SERVICEORIGIN != 'Multiservice' ");
                routerQuery.append(" AND r.SERVICEORIGIN is not null ");
                routerQuery.append(" AND r.USID = c.USID (+) ");
                routerQuery.append(" AND r.RouterName = a.AccessConnectionRouterName ");
                routerQuery.append(getAddressCondition("r."));
                String whereDateClause = " r.lastmodifdate > ";
                addLastModifDateCond(routerQuery, whereDateClause);
                if (cis_logger.isDebugEnabled())
                    cis_logger.info("Router Query String Without Custcode Condition: " + routerQuery.toString());
                formMultipleQuery(custId, routerQueryVec, routerQuery, "r.CUSTCODE");
                setProductRouterQuery(routerQueryVec);

                //router query 2
                routerQuery2.append(", r.S_Type ");
                routerQuery2.append(" FROM " + CISConstants.CIS_TABLENAME_IP_ROUTER + " r, " + CISConstants.CIS_TABLENAME_IP_CHASSIS + " c ");
                routerQuery2.append(" WHERE r.S_Type = '" + getCISRouterProductName() + "' ");
                routerQuery2.append(" AND r.SERVICESTATUS != '" + CISConstants.CIS_STATUS_CANCELLED + "' ");
                routerQuery2.append(" AND r.SERVICEORIGIN != 'Multiservice' ");
                routerQuery2.append(" AND r.SERVICEORIGIN is not null ");
                routerQuery2.append(" AND r.USID = c.USID (+) ");
                routerQuery2.append(" AND r.RouterName not in (SELECT AccessConnectionRouterName FROM " + CISConstants.CIS_TABLENAME_IP_AC + " ) " );
                routerQuery2.append(getAddressCondition("r."));
                String whereDateClause1 = " r.lastmodifdate > ";
                addLastModifDateCond(routerQuery2, whereDateClause1);
                if (cis_logger.isDebugEnabled())
                    cis_logger.info("Router Query2 String Without Custcode Condition: " + routerQuery2.toString());
                formMultipleQuery(custId, routerQuery2Vec, routerQuery2, "r.CUSTCODE");
                setProductRouterQuery2(routerQuery2Vec);

            }

            if (cis_logger.isDebugEnabled())
                cis_logger.info("end initIPRouterQuery.");
        } finally {
            closeResultSet(ps, result);
            result = null;
            ps = null;            
        }
    }

    /**
     * @param custId
     * @throws SQLException
     */
	 //cis migration change
    protected void initIPNasBackupQuery(Vector custId) throws SQLException, CISException {
        if (cis_logger.isDebugEnabled())
            cis_logger.info("get into initIPNasBackupQuery...");

        PreparedStatement ps = null;
        ResultSet result = null;
        
        try {
            ps = m_localConn.prepareStatement("SELECT CISTABLENAME, CISCOLUMNNAME FROM CCISMAPPING WHERE (CSISERVICE = 'ALL' OR CSISERVICE like '%" + getCSIProductNameFromMappingTable() + "%') AND DESCRIPTION = ? AND CISTABLENAME IS NOT NULL");

            Vector nasbackupQueryVec = getProductNasBackupQuery();
            Vector nasbackupQuery2Vec = getProductNasBackupQuery2();
            if (nasbackupQueryVec == null || nasbackupQuery2Vec == null || (nasbackupQueryVec != null && nasbackupQueryVec.isEmpty()) || (nasbackupQuery2Vec != null && nasbackupQuery2Vec.isEmpty())) {
                nasbackupQueryVec = new Vector();
                nasbackupQuery2Vec = new Vector();
                StringBuffer nasbackupQuery = new StringBuffer();
                StringBuffer nasbackupQuery2 = new StringBuffer();
                nasbackupQuery.append("\n SELECT DISTINCT ");
                ps.setString(1, QUERYTYPE_NASBACKUP);
                result = ps.executeQuery();
                String tableShortName = "";
                int columnCount = 0;

                while (result.next()) {
                    tableShortName = result.getString(1).equals(CISConstants.CIS_TABLENAME_IP_ROUTER) ? "r." : "n.";
                    if (columnCount == 0) {
                        nasbackupQuery.append("  " + tableShortName + result.getString(2) + " \n");
                    } else {
                        nasbackupQuery.append(",  " + tableShortName + result.getString(2) + " \n");
                    }
                    columnCount++;
                }

                nasbackupQuery2 = nasbackupQuery2.append(nasbackupQuery.toString());

                //nasbackupQuery 1
                nasbackupQuery.append(" , a.S_Type ");
                nasbackupQuery.append(" FROM " + CISConstants.CIS_TABLENAME_IP_ROUTER + " r, " + CISConstants.CIS_TABLENAME_IP_NASBACKUP + " n, " + CISConstants.CIS_TABLENAME_IP_AC + " a ");
                nasbackupQuery.append(" WHERE a.S_Type = '" + getCISRouterProductName() + "' ");
                nasbackupQuery.append(" AND r.SERVICESTATUS != '" + CISConstants.CIS_STATUS_CANCELLED + "' ");
                nasbackupQuery.append(" AND r.SERVICEORIGIN != 'Multiservice' ");
                nasbackupQuery.append(" AND r.SERVICEORIGIN is not null ");
                nasbackupQuery.append(" AND r.USID = n.USID ");
                nasbackupQuery.append(" AND r.RouterName = a.AccessConnectionRouterName ");
                nasbackupQuery.append(" AND n.NASBackupType = 'NAS Backup' ");
                nasbackupQuery.append(getAddressCondition("r."));
                String whereDateClause = " n.lastmodifdate > ";
                addLastModifDateCond(nasbackupQuery, whereDateClause);
                if (cis_logger.isDebugEnabled())
                    cis_logger.info("Nasbackup Query String Without Custcode Condition: " + nasbackupQuery.toString());
                formMultipleQuery(custId, nasbackupQueryVec, nasbackupQuery, "r.CUSTCODE");
                setProductNasBackupQuery(nasbackupQueryVec);

                 //nasbackupQuery 2
                nasbackupQuery2.append(" , r.S_Type ");
                nasbackupQuery2.append(" FROM " + CISConstants.CIS_TABLENAME_IP_ROUTER + " r, " + CISConstants.CIS_TABLENAME_IP_NASBACKUP + " n " );
                nasbackupQuery2.append(" WHERE r.S_Type = '" + getCISRouterProductName() + "' ");
                nasbackupQuery2.append(" AND r.SERVICESTATUS != '" + CISConstants.CIS_STATUS_CANCELLED + "' ");
                nasbackupQuery2.append(" AND r.SERVICEORIGIN != 'Multiservice' ");
                nasbackupQuery2.append(" AND r.SERVICEORIGIN is not null ");
                nasbackupQuery2.append(" AND r.USID = n.USID ");
                nasbackupQuery2.append(" AND r.RouterName not in ( SELECT AccessConnectionRouterName FROM " + CISConstants.CIS_TABLENAME_IP_AC + " ) ");
                nasbackupQuery2.append(" AND n.NASBackupType = 'NAS Backup' ");
                nasbackupQuery2.append(getAddressCondition("r."));
                String whereDateClause1 = " n.lastmodifdate > ";
                addLastModifDateCond(nasbackupQuery2, whereDateClause1);
                if (cis_logger.isDebugEnabled())
                    cis_logger.info("Nasbackup Query2 String Without Custcode Condition: " + nasbackupQuery2.toString());
                formMultipleQuery(custId, nasbackupQuery2Vec, nasbackupQuery2, "r.CUSTCODE");
                setProductNasBackupQuery2(nasbackupQuery2Vec);
            }

            if (cis_logger.isDebugEnabled())
                cis_logger.info("end initIPNasBackupQuery.");
        } finally {
            closeResultSet(ps, result);
            result = null;
            ps = null;
        }
    }

    /**
     * @param serviceStatus
     * @param m_version
     * @return
     */
    protected String setCISStatusToCSIStatusForRouter(String serviceStatus, Version m_version) {
        String srvCategory = "";
        if (CISConstants.CIS_ROUTER_STATUS_MIG.equals(serviceStatus) || CISConstants.CIS_ROUTER_STATUS_ONHOLD.equals(serviceStatus)
                || CISConstants.CIS_ROUTER_STATUS_PLA.equals(serviceStatus)) {
            m_version.setOrdertype(CSIConstants.ORDER_TYPE_NEW);
            m_version.setOrderstatus(CSIConstants.STATUS_ORDER_RELEASE);
            srvCategory = CSIConstants.ELEMENT_TYPE_CREATION;
        } else if (CISConstants.CIS_ROUTER_STATUS_CFG.equals(serviceStatus) || CISConstants.CIS_ROUTER_STATUS_INT.equals(serviceStatus)
                || CISConstants.CIS_ROUTER_STATUS_OL.equals(serviceStatus)) {
            m_version.setOrdertype(CSIConstants.ORDER_TYPE_EXISTING);
            m_version.setOrderstatus(CSIConstants.STATUS_ORDER_MANAGE);
            srvCategory = CSIConstants.ELEMENT_TYPE_MODIFICATION;
        } else if (CISConstants.CIS_ROUTER_STATUS_OUT.equals(serviceStatus)) {
            m_version.setOrdertype(CSIConstants.ORDER_TYPE_DISCONNECT);
            m_version.setOrderstatus(CSIConstants.STATUS_ORDER_MANAGE);
            srvCategory = CSIConstants.ELEMENT_TYPE_DISCONNECT;
        }
        return srvCategory;
    }

}


