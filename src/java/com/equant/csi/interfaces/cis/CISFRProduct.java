package com.equant.csi.interfaces.cis;

import com.equant.csi.common.TransDate;
import com.equant.csi.common.CSIConstants;
import com.equant.csi.exceptions.CISException;
import com.equant.csi.ejb.biz.ServiceManager;
import com.equant.csi.utilities.LoggerFactory;
import com.equant.csi.utilities.JAXBUtils;
import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.Version;
import com.equant.csi.jaxb.SiteInformation;
import com.equant.csi.jaxb.ServiceElement;
import com.equant.csi.jaxb.User;

import javax.xml.bind.JAXBException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Category;

/**
 * Created by IntelliJ IDEA.
 * User: szhu
 * Date: Jul 14, 2004
 * Time: 11:54:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class CISFRProduct extends CISProductBase {
    protected Hashtable m_fr_mapping = null;
    protected Hashtable m_fr_value_mapping = null;
    protected Vector m_frac_query;
    protected Vector m_frpvc_query;
    protected Hashtable m_fr_remoteattrs;

    /**
     * Initializing the logger.
     */
    private static final Category cis_logger = LoggerFactory.getInstance("cis_extraction.CISFRProduct");

    public CISFRProduct(Connection sybase, Connection local, int runType, ServiceManager serviceManager, Connection goldConn, String product) {
        super(sybase, local, runType, serviceManager, goldConn, product);
    }

    /**
     * @return 
     */
    protected String getCISACProductName() {
        return CISConstants.CIS_PRODUCT_FR_AC;
    }

    protected String getCISPVCProductName() {
        return CISConstants.CIS_PRODUCT_FR_PVC;
    }

    /**
     * @return 
     */
    protected String getCSIProductNameFromMappingTable() {
        return CISConstants.CSI_PRODUCT_FR_PURPLE;
    }

    /**
     * @return 
     */
    protected Hashtable getProductFieldMappingTable() {
        return m_fr_mapping;
    }

    /**
     * @param mapping 
     */
    protected void setProductFieldMappingTable(Hashtable mapping) {
        m_fr_mapping = mapping;
    }

    /**
     *
     */
    protected String run() throws CISException {
        m_extractLogMsg = "";
        m_extractLogMsg = frACRun();
        m_extractLogMsg += frPVCRun();

         //clean up the hashtable
         Hashtable hs = getRemoteUSIDAttrs();
         if (hs != null) {
             hs.clear();
         }
        return m_extractLogMsg;
    }

    /**
     * @return 
     */
    protected Vector getProductACQuery() {
        return m_frac_query;
    }

    /**
     * @return 
     */
    protected Vector getProductPVCQuery() {
        return m_frpvc_query;
    }

    /**
     * @param query 
     */
    protected void setProductPVCQuery(Vector query) {
        m_frpvc_query = query;
    }

    /**
     * @param query
     */
    protected void setProductACQuery(Vector query) {
        m_frac_query = query;
    }

    /**
     * @return 
     */
    protected Hashtable getProductValueMappingTable() {
        return m_fr_value_mapping;
    }

    /**
     * @param mapping 
     */
    protected void setProductValueMappingTable(Hashtable mapping) {
        m_fr_value_mapping = mapping;

    }

    /**
     *
     * @param serviceOrig
     * @return
     */
    protected String getCSIProductByServiceOrig(String serviceOrig) {
       if (serviceOrig == null || CISConstants.CIS_PRODUCT_ORIG_PURPLE.equals(serviceOrig)) {
           return CISConstants.CSI_PRODUCT_FR_PURPLE;
       }else if (CISConstants.CIS_PRODUCT_ORIG_GREEN.equals(serviceOrig)) {
           return CISConstants.CSI_PRODUCT_FR_GREEN;
       }else if (CISConstants.CIS_PRODUCT_ORIG_BLUE.equals(serviceOrig)) {
           return CISConstants.CSI_PRODUCT_FR_BLUE;
       }
       return null;
    }

    protected Hashtable getRemoteUSIDAttrs() {
        return m_fr_remoteattrs;
    }

    protected void setRemoteUSIDAttrs(Hashtable hs) {
        m_fr_remoteattrs = hs;
    }

    /**
     *
     */
    protected String frACRun() throws CISException {
        cis_logger.info("starting frACRun...");
        m_extractLogMsg = "";
        long startTime = System.currentTimeMillis();
        int correctRecord;
        int errorRecord;
        ResultSet result = null;
        PreparedStatement ps = null;
        String usid = "";

        try {
            initMapping(false);
            initMTNQueryString();
            correctRecord = 0;
            errorRecord = 0;

            for (int i = 0; i < m_frac_query.size(); i++) {
                cis_logger.debug("Query loop:" + i);
                StringBuffer tmpQueryStrBuffer = new StringBuffer();
                tmpQueryStrBuffer.append((String) m_frac_query.get(i));

                String whereDateClause = " lastmodifdate > ";
                addLastModifDateCond(tmpQueryStrBuffer, whereDateClause);

                ps = m_sybaseConn.prepareStatement(tmpQueryStrBuffer.toString());
                result = getResultSetFromQuery(tmpQueryStrBuffer, ps);

                String status = null;
                String subStatus = null;

                initObjectFactory();

                while (result.next()) {
                    //ignore the current records if there is error
                    try {
                        usid = result.getString("USID");
                        cis_logger.debug("Retrieved Record|" + m_product + "|AccessConnection|USID|" + usid);
                        Message m_message = initGeneralObject();

                        //set Version properties
                        Version m_version = (Version) m_message.getVersion().get(0); //only one version for each message
                        String custId = result.getString("CUSTCODE");
                        m_version.setEndUserId(custId);
                        m_version.setCustomerId(custId);
                        m_version.setServiceId(getCSIProductByServiceOrig(result.getString("SERVICEORIGIN")));
                        TransDate lastModifDate = convertSQLTimeToTransdate(getLastModifDate(result));
                        m_version.setOrderSentDate(lastModifDate);

                        //set service elemet status
                        status = result.getString("SERVICESTATUS");
                        subStatus = result.getString("SERVICESUBSTATUS");

                        if (null == status || CISConstants.CIS_STATUS_CANCELLED.equals(status)) continue; //do nothing for this record
                        String category = setCISStatusToCSIStatus(status, subStatus, m_version);

                        //set site
                        SiteInformation m_site = (SiteInformation) m_message.getSiteInformation();
                        if (!setSiteInformation(result, m_site, getSiteErrorMsgHeader(m_product, CSIConstants.SECLASS_ACCESSCONNECTION, usid))) continue;
                        // cis migration
                        //m_site.setLastModifDate(convertSQLTimeToTransdate(result.getTimestamp("CUSTOMERLASTMODIFDATE")));
                        m_site.setLastModifDate(convertSQLTimeStringToTransdate(result.getString("CUSTOMERLASTMODIFDATE")));
                        
                        //now for service element 1: access connection
                        setServiceElementAndAttr(result, CSIConstants.SECLASS_ACCESSCONNECTION, category, m_version);

                        //now for service element 2:  CSP
                        setServiceElementAndAttr(result, CSIConstants.SECLASS_SERVICEOPTIONS, category, m_version);

                        //now for service element 3: BACKUP
                        String backupService = result.getString("ServiceSupplements");

                        if (backupService != null && backupService.length() > 0) {
                            setServiceElementAndAttr(result, CSIConstants.SECLASS_BACKUP, category, m_version);
                        } else {
                            //add new version to disconnect it
                            Version version = addBackupDisconnectVersion(m_version, result);
                            m_message.getVersion().add(version);
                        }

                        //TODO:  call ServiceManager's method to pass on
                        //TODO:  Pass String in test session
                        //TODO:  Pass Message in weblogic container
//                    printMessageObjectXML(m_message);
//                    String msg = JAXBUtils.marshalMessage(m_message);
//                    cis_logger.debug("message:" + msg);
                        m_serviceManager.processMessageFromCis(m_message);
                        correctRecord++;
                    } catch (Exception e) {
                        errorRecord++;
                        cis_logger.info("Error while handle FR AC records:" + usid);
                        cis_logger.info("Detail errors:", e);
                    }
                }
            }
            cis_logger.info("Number of FR AC records successed:" + correctRecord + " records failed:" + errorRecord);
        } catch (SQLException sqle) {
            if (cis_logger.isDebugEnabled()) cis_logger.fatal("SQL Exception", sqle);
            throw new CISException("SQL Exception:", sqle);
        } catch (CISException cisex) {
            if (cis_logger.isDebugEnabled()) cis_logger.fatal("CISException", cisex);
            throw new CISException("SQL Exception:", cisex);
        } catch (JAXBException jaxb) {
            if (cis_logger.isDebugEnabled()) cis_logger.fatal("JAXBException", jaxb);
            throw new CISException("JAXBException:", jaxb);
        } finally {
            closeResultSet(ps, result);
            result = null;
            ps = null;
        }

        long endTime = System.currentTimeMillis();
        m_extractLogMsg = CISConstants.CIS_EXTRACTMSG_SEPARATER + CISConstants.CIS_PRODUCT_FR_AC + " AC records successed:" + correctRecord + " FR records failed:" + errorRecord
                + ". It took: " + (endTime - startTime) / 1000 + " Seconds.";

        cis_logger.info("end FRAC Run.  It took: " + (endTime - startTime) / 1000 + " Seconds.");
        return m_extractLogMsg;
    }

    /**
     *
     */
    protected String frPVCRun() throws CISException {
        cis_logger.info("starting frPVCRun...");
        m_extractLogMsg = "";
        long startTime = System.currentTimeMillis();
        int correctRecord;
        int errorRecord;
        ResultSet result = null;
        PreparedStatement ps = null;
        String usid = "";

        try {
            initMapping(true);
            initMTNQueryString();
            correctRecord = 0;
            errorRecord = 0;

            for (int i = 0; i < m_frpvc_query.size(); i++) {

                cis_logger.debug("Query loop:" + i);
                StringBuffer tmpQueryStrBuffer = new StringBuffer();
                tmpQueryStrBuffer.append((String) m_frpvc_query.get(i));
                String whereDateClause = " p.lastmodifdate > ";
                addLastModifDateCond(tmpQueryStrBuffer, whereDateClause);

                ps = m_sybaseConn.prepareStatement(tmpQueryStrBuffer.toString());
                result = getResultSetFromQuery(tmpQueryStrBuffer, ps);

                String status = null;
                String subStatus = null;

                initObjectFactory();

                while (result.next()) {
                    //ignore the current records if there is error
                    try {
                        usid = result.getString("PVCUSID");
                        cis_logger.debug("Retrieved Record|" + m_product + "|Transport|USID|" + usid);

                        Message m_message = initGeneralObject();

                        //set Version properties
                        Version m_version = (Version) m_message.getVersion().get(0); //only one version for each message
                        //set Version properties
                        String custId = result.getString("CUSTCODE");
                        m_version.setEndUserId(custId);
                        m_version.setCustomerId(custId);
                        m_version.setServiceId(getCSIProductByServiceOrig(result.getString("SERVICEORIGIN")));
                        TransDate lastModifDate = convertSQLTimeToTransdate(getLastModifDate(result));
                        m_version.setOrderSentDate(lastModifDate);

                        //set service elemet status
                        status = result.getString("SERVICESTATUS");
                        subStatus = result.getString("SERVICESUBSTATUS");

                        if (null == status || CISConstants.CIS_STATUS_CANCELLED.equals(status)) continue; //do nothing for this record
                        String category = setCISStatusToCSIStatus(status, subStatus, m_version);

                        //set site
                        SiteInformation m_site = (SiteInformation) m_message.getSiteInformation();
                        if (!setSiteInformation(result, m_site, getSiteErrorMsgHeader(m_product, CSIConstants.SECLASS_TRANSPORT, usid))) continue;
                        // cis migration
                        //m_site.setLastModifDate(convertSQLTimeToTransdate(result.getTimestamp("SITELASTMODIFDATE")));
                        m_site.setLastModifDate(convertSQLTimeStringToTransdate(result.getString("SITELASTMODIFDATE")));
                        //now for service element 1: access connection
                        setServiceElementAndAttr(result, CSIConstants.SECLASS_TRANSPORT, category, m_version);

                        //TODO:  call ServiceManager's method to pass on
//                    printMessageObjectXML(m_message);
//                    String msg = JAXBUtils.marshalMessage(m_message);
                        m_serviceManager.processMessageFromCis(m_message);
                        correctRecord++;
                    } catch (Exception e) {
                        errorRecord++;
                        cis_logger.info("Error while handle FR PVC records:" + usid);
                        cis_logger.info("Detail errors:", e);
                    }
                }
            }
            cis_logger.info("Number of FR PVC records successed:" + correctRecord + " records failed:" + errorRecord);
        } catch (SQLException sqle) {
            if (cis_logger.isDebugEnabled()) cis_logger.fatal("SQL Exception", sqle);
            throw new CISException("SQL Exception:", sqle);
        } catch (CISException cisex) {
            if (cis_logger.isDebugEnabled()) cis_logger.fatal("CISException", cisex);
            throw new CISException("SQL Exception:", cisex);
        } catch (JAXBException jaxb) {
            if (cis_logger.isDebugEnabled()) cis_logger.fatal("JAXBException", jaxb);
            throw new CISException("JAXBException:", jaxb);
        } finally {
            closeResultSet(ps, result);
            result = null;
            ps = null;
        }

        long endTime = System.currentTimeMillis();
        m_extractLogMsg = CISConstants.CIS_EXTRACTMSG_SEPARATER + CISConstants.CIS_PRODUCT_FR_PVC + " records successed:" + correctRecord + " PVC records failed:" + errorRecord
                + ". It took: " + (endTime - startTime) / 1000 + " Seconds.";

        cis_logger.info("end FR PVC Run.  It took: " + (endTime - startTime) / 1000 + " Seconds.");
        return m_extractLogMsg;
    }

}
