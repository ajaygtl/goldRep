package com.equant.csi.interfaces.cis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.PreparedStatement;

import com.equant.csi.jaxb.ObjectFactory;
import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.Version;
import com.equant.csi.jaxb.ServiceElement;
import com.equant.csi.jaxb.SiteInformation;
import com.equant.csi.jaxb.SiteContact;
import com.equant.csi.jaxb.User;
import com.equant.csi.jaxb.ServiceElementAttribute;
import com.equant.csi.jaxb.Type;
import com.equant.csi.exceptions.CISException;
import com.equant.csi.common.TransDate;
import com.equant.csi.common.CSIConstants;
import com.equant.csi.utilities.LoggerFactory;
import com.equant.csi.ejb.biz.ServiceManager;
import com.equant.csi.database.GOLDManager;

import java.util.Hashtable;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import org.apache.log4j.Category;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.JAXBException;

/**
 * Created by IntelliJ IDEA.
 * User: szhu
 * Date: Jul 14, 2004
 * Time: 10:26:40 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class CISProductBase {
    /**
     * Initializing the logger.
     */
    private static final Category cis_logger = LoggerFactory.getInstance("cis_extraction.CISProductBase");
    private static final Category cis_site_logger = LoggerFactory.getInstance("cis_extraction_site.CISProductBase");

    Connection m_sybaseConn = null;
    Connection m_localConn = null;
    Connection m_goldConn = null;
    int m_runType;
    String m_extractLogMsg = "";
    ServiceManager m_serviceManager = null;
    String m_product = "";
    protected final static String m_mapping_delim = "|";
    protected final static int m_max_custcode_length = 40000;

    protected ObjectFactory m_objectFactory = null;


    /**
     * @param sybase
     * @param local
     * @param runType
     * @param goldConn
     */
    public CISProductBase(Connection sybase, Connection local, int runType, ServiceManager serverceManager, Connection goldConn, String product) {
        m_sybaseConn = sybase;
        m_localConn = local;
        m_goldConn = goldConn;
        m_runType = runType;
        m_product = product;
        m_serviceManager = serverceManager;
        m_extractLogMsg = "";
    }

    /**
     * @throws JAXBException
     */
    protected void initObjectFactory() throws JAXBException {
        if (m_objectFactory == null) {
            m_objectFactory = new ObjectFactory();
        }

    }


    /**
     * @throws JAXBException
     */
    protected Message initGeneralObject() throws JAXBException {
        Message m_message = null;
        Version m_version = null;
        SiteInformation m_site = null;
        SiteContact m_contact = null;
        com.equant.csi.jaxb.System m_system = null;
        User m_user = null;

        m_message = m_objectFactory.createMessage();
        m_version = m_objectFactory.createVersion();
        m_site = m_objectFactory.createSiteInformation();
        m_contact = m_objectFactory.createSiteContact();
        m_system = m_objectFactory.createSystem();
        m_user = m_objectFactory.createUser();

        m_message.getVersion().add(m_version);

        m_system.setId(CSIConstants.SYSTEM_TYPE_CIS);
        m_user.setName(CISConstants.CIS_CSI_PROGUSER);
        m_system.setUser(m_user);
        m_version.setSystem(m_system);

        m_message.setSiteInformation(m_site);
        m_site.getSiteContact().add(m_contact);

        return m_message;
    }

    /**
     * @throws JAXBException
     */
    protected ServiceElement initServiceEelment(String category) throws JAXBException {
        ServiceElement m_srvElement = m_objectFactory.createServiceElement();
        Type m_type = m_objectFactory.createType();
        m_type.setCategory(category);
        m_srvElement.setType(m_type);
        return m_srvElement;
    }

    /**
     * @throws JAXBException
     */
    protected void setServiceEelmentToVersion(Version m_version, ServiceElement m_srvElement) throws JAXBException {
        m_version.getServiceElement().add(m_srvElement);
    }

    /**
     * Purpose:  get the sybase server timestamp
     *
     * @return
     * @throws com.equant.csi.exceptions.CISException
     *
     */
    protected Timestamp getCurrentSybaseTimeStamp() throws CISException {
        Timestamp currentTime = null;
        //String query = "select getDate()";
        //cis migration change
		String query = "SELECT SYSDATE FROM DUAL";
        PreparedStatement dateStatement = null;
        ResultSet result = null;
        try {
            dateStatement = m_sybaseConn.prepareStatement(query);
            result = dateStatement.executeQuery();
            if (result.next()) {
                currentTime = result.getTimestamp(1);
            }
        } catch (SQLException sqle) {
            throw new CISException("There is an error to get sybase server current time. ", sqle);
        } finally {
            closeResultSet(dateStatement, result);
            result = null;
            dateStatement = null;
        }
        return currentTime;
    }

    /**
     * @return
     * @throws com.equant.csi.exceptions.CISException
     *
     */
    protected Timestamp getLastRunDate() throws CISException {
        Timestamp lastRunDate = null;
        if (m_runType == CISConstants.CIS_EXTRACTTYPE_FULL) return null;
        PreparedStatement dateStatement = null;
        ResultSet result = null;

        try {
            String cis_last_runtime_query = "SELECT MAX(STARTTIME) FROM CCISEXTRACTLOG WHERE STATUS = " + CISConstants.CIS_EXTRACTSTATUS_SUCCESS
                                              + " AND NOTE like '%" + m_product + "%'";
            dateStatement = m_localConn.prepareStatement(cis_last_runtime_query);
            result = dateStatement.executeQuery();
            if (result.next()) {
                lastRunDate = result.getTimestamp(1);
            }
        } catch (SQLException sqle) {
            throw new CISException("There is an error in getLastRunDate. ", sqle);
        } finally {
            closeResultSet(dateStatement, result);
            result = null;
            dateStatement = null;
        }

        if (lastRunDate == null) {
            Timestamp currentSybaseTimestamp = getCurrentSybaseTimeStamp();
            switch (m_runType) {
                case CISConstants.CIS_EXTRACTTYPE_DAY:
                    lastRunDate = new Timestamp(currentSybaseTimestamp.getTime() - CISConstants.CIS_DAY_DELTA);
                case CISConstants.CIS_EXTRACTTYPE_WEEK:
                    lastRunDate = new Timestamp(currentSybaseTimestamp.getTime() - CISConstants.CIS_WEEK_DELTA);
            }
        }
        return lastRunDate;
    }

    /**
     * @param startTime
     * @param endTimestamp
     * @param status
     * @param logMsg
     */
    protected void insertExtractLog(Timestamp startTime, Timestamp endTimestamp, int status, String logMsg)
            throws CISException {
        PreparedStatement ps = null;
        try {
            ps = null;
            ps = m_localConn.prepareStatement("INSERT INTO CCISExtractLog (ccisextractlogid   " +
                                                " ,extracttype " +
                                                " ,starttime " +
                                                " ,endtime " +
                                                " ,status " +
                                                " ,note " +
                                                " ,createDate " +
                                                " ,createdBy " +
                                                " ,lupdDate " +
                                                " ,lupdBy " +
                                                "   ) VALUES (SEQCCISExtractLog.nextVal" + ",?,?,?,?,?,?,?,?,?) ");

            ps.setLong(1, m_runType);
            ps.setTimestamp(2, startTime);
            ps.setTimestamp(3, endTimestamp);
            ps.setLong(4, status);
            ps.setString(5, logMsg);
            ps.setTimestamp(6, new Timestamp((new Date()).getTime()));
            ps.setString(7, CISConstants.CIS_CSI_PROGUSER);
            ps.setTimestamp(8, new Timestamp((new Date()).getTime()));
            ps.setString(9, CISConstants.CIS_CSI_PROGUSER);

            ps.executeUpdate();
            m_localConn.commit();

        } catch (SQLException sqle) {
            throw new CISException("Error in insert to log file. ", sqle);
        } finally {
            closeResultSet(ps, null);
            ps = null;
        }
    }

    /**
     * convert jdbc result set time to jaxb transdate
     *
     * @param ts
     * @return
     */
    protected TransDate convertSQLTimeToTransdate(Timestamp ts) {
        TransDate date = null;
        if (ts != null) {
            date = new TransDate(ts.getTime());
        }
        return date;
    }

    /**
     * convert jdbc result set time to jaxb transdate
     *
     * @param string
     * @return
     */ 
    //cis migration
    protected TransDate convertSQLTimeStringToTransdate(String time) {
        TransDate date = null;
        if (time != null) {
            date = new TransDate(time);
        }
        return date;
    }    
    
    /**
     * @param result
     * @return
     * @throws SQLException
     */
    protected Timestamp getLastModifDate(ResultSet result) throws SQLException {
        Timestamp tempLastModifDate = null;

        /*
        Timestamp tempLastModifDate = result.getTimestamp("LASTMODIFDATE");
        if (tempLastModifDate == null) {
            tempLastModifDate = result.getTimestamp("CREATIONDATE");
        }*/

        //Assign current time to it to avoid future exception, this should not be the case
        if (tempLastModifDate == null) {
            tempLastModifDate = new Timestamp(new Date().getTime());
        }
        return tempLastModifDate;
    }


    /**
     * @param serviceStatus
     * @param serviceSubStatus
     */
    protected String setCISStatusToCSIStatus(String serviceStatus, String serviceSubStatus, Version m_version) {
        String srvCategory = "";
        if (CISConstants.CIS_STATUS_ORDERED.equals(serviceStatus)) {
            m_version.setOrdertype(CSIConstants.ORDER_TYPE_NEW);
            m_version.setOrderstatus(CSIConstants.STATUS_ORDER_RELEASE);
            srvCategory = CSIConstants.ELEMENT_TYPE_CREATION;
        } else if (CISConstants.CIS_STATUS_ONLINE.equals(serviceStatus)) {
            if (serviceSubStatus != null && CISConstants.CIS_SUB_STATUS_STABLE.equals(serviceSubStatus)) {
                m_version.setOrdertype(CSIConstants.ORDER_TYPE_EXISTING);
                m_version.setOrderstatus(CSIConstants.STATUS_ORDER_MANAGE);
                srvCategory = CSIConstants.ELEMENT_TYPE_MODIFICATION;
            } else if (CISConstants.CIS_SUB_STATUS_MODIF.equals(serviceSubStatus)) {
                m_version.setOrdertype(CSIConstants.ORDER_TYPE_CHANGE);
                m_version.setOrderstatus(CSIConstants.STATUS_ORDER_RELEASE);
                srvCategory = CSIConstants.ELEMENT_TYPE_MODIFICATION;
            } else {
                m_version.setOrdertype(CSIConstants.ORDER_TYPE_CHANGE);
                m_version.setOrderstatus(CSIConstants.STATUS_ORDER_RELEASE);
                srvCategory = CSIConstants.ELEMENT_TYPE_MODIFICATION;
            }
        } else if (CISConstants.CIS_STATUS_CEASED.equals(serviceStatus)) {
            m_version.setOrdertype(CSIConstants.ORDER_TYPE_DISCONNECT);
            m_version.setOrderstatus(CSIConstants.STATUS_ORDER_MANAGE);
            srvCategory = CSIConstants.ELEMENT_TYPE_DISCONNECT;
        }

        return srvCategory;
    }

    /**
     * @throws CISException
     */
    protected void initMapping(boolean hasPVC) throws CISException {
        cis_logger.info("get into initMapping...");
        String cisColumnName = null;
        String csiColumnName = null;
        String csiServiceClass = null;
        String description = null;
        String cisValue = null;
        String csiValue = null;
        PreparedStatement ps = null;
        ResultSet result = null;


        //pop field mapping hash table
        try {
            ps = m_localConn.prepareStatement("SELECT CISCOLUMNNAME, CSISRVELMCLASS, CSISRVELMATTRNAME, DESCRIPTION FROM CCISMAPPING " +
                    " WHERE CSISRVELMATTRNAME IS NOT NULL AND ( CSISERVICE = 'ALL' OR CSISERVICE like '%" + getCSIProductNameFromMappingTable() + "%') AND STATUS = " + CISConstants.CIS_EXTRACTSTATUS_SUCCESS);

            Hashtable mapping = getProductFieldMappingTable();
            String key = "";
            if (mapping == null) {
                mapping = new Hashtable();
                result = ps.executeQuery();
                while (result.next()) {
                    cisColumnName = result.getString(1);
                    csiServiceClass = result.getString(2);
                    csiColumnName = result.getString(3);
                    description = result.getString(4);
                    key = cisColumnName + m_mapping_delim + csiServiceClass + m_mapping_delim + description;
                    if (!mapping.containsKey(key)) {
                        mapping.put(key, csiColumnName);
                    }
                }

                setProductFieldMappingTable(mapping);
            }
        } catch (SQLException sqle) {
            throw new CISException("Error while initiating field mapping: " + sqle);
        } finally {
            closeResultSet(ps, result);
            ps = null;
            result = null;
        }


        //pop value mapping hash table
        try {
            ps = m_localConn.prepareStatement("SELECT CSISERVICE, CSISRVELMCLASS, CSISRVELMATTRNAME, CISVALUE, CSIVALUE FROM CCISVALUEMAPPING " +
                    " WHERE ( CSISERVICE = 'ALL' OR CSISERVICE LIKE '%" + getCSIProductNameFromMappingTable() + "%' ) AND STATUS = " + CISConstants.CIS_EXTRACTSTATUS_SUCCESS);

            Hashtable valueMapping = getProductValueMappingTable();
            if (valueMapping == null) {
                valueMapping = new Hashtable();
                result = ps.executeQuery();
                while (result.next()) {
                    csiServiceClass = result.getString(2);
                    csiColumnName = result.getString(3);
                    cisValue = result.getString(4);
                    csiValue = result.getString(5);
                    valueMapping.put(csiServiceClass + m_mapping_delim + csiColumnName + m_mapping_delim + cisValue, csiValue);
                }

                setProductValueMappingTable(valueMapping);
            }

            if (hasPVC) {
                initRemoteUSIDAttrs(hasPVC, getCustomerConditionString());
            }
        } catch (SQLException sqle) {
            throw new CISException("Error while initiating value mapping: " + sqle);
        } finally {
            closeResultSet(ps, result);
            ps = null;
            result = null;
        }

        cis_logger.info("end initMapping.");
    }

    /**
     *
     * @throws CISException
     */
	 //cis migration change
    protected void initMTNQueryString() throws CISException {
        PreparedStatement ps = null;
        ResultSet result = null;
        Vector custIdVec = getCustomerConditionString();

        try {
        //pop access query
        ps = m_localConn.prepareStatement("SELECT CISCOLUMNNAME FROM CCISMAPPING WHERE CISSERVICE = ? AND CISTABLENAME = ?");

        Vector acqueryVec = getProductACQuery();
        Vector pvcqueryVec = getProductPVCQuery();
        if (acqueryVec == null || (acqueryVec != null && acqueryVec.size() == 0)) {
            acqueryVec = new Vector();
            StringBuffer acquery = new StringBuffer();
            acquery.append("SELECT ");
            ps.setString(1, getCISACProductName());
            ps.setString(2, CISConstants.CIS_TABLENAME_AC);
            result = ps.executeQuery();
            while (result.next()) {
                acquery.append(result.getString(1) + ", ");
            }
            // cis migration change
            // acquery.append(" AccessConnectionName = 'Access Connection' ");
            acquery.append(" 'Access Connection' as AccessConnectionName ");
            acquery.append(" FROM " + CISConstants.CIS_TABLENAME_AC);
            acquery.append(" WHERE S_TYPE = '" + getCISACProductName() + "' ");
            acquery.append(" AND SERVICESTATUS != '" + CISConstants.CIS_STATUS_CANCELLED + "' ");
            acquery.append(" AND PURELAYER2FLAG = 'Yes'");
            acquery.append(getAddressCondition(" "));
            cis_logger.info("Query without custcode:");
            cis_logger.info(acquery.toString());

            formMultipleQuery(custIdVec, acqueryVec, acquery, "CUSTCODE");
            setProductACQuery(acqueryVec);
            result.close();
        }

        //pop pvc query
        if (pvcqueryVec == null || (pvcqueryVec != null && pvcqueryVec.size() == 0)) {
            pvcqueryVec = new Vector();
            StringBuffer pvcquery = new StringBuffer();
            pvcquery.append("SELECT ");
            ps.setString(1, getCISPVCProductName());
            ps.setString(2, CISConstants.CIS_TABLENAME_PVC);
            result = ps.executeQuery();
            while (result.next()) {
            	// cis migration change
            	//pvcquery.append(result.getString(1) + " = p." + result.getString(1) + ", \n");
            	pvcquery.append( " p." + result.getString(1)+" "+result.getString(1) + ", \n");
            }

            //join mtn_access for local access usid
            //cis migration change
            pvcquery.append(" a1.CustCode CustCode, \n");
            pvcquery.append(" a1.Address1 Address1, \n");
            pvcquery.append(" a1.Address2 Address2, \n");
            pvcquery.append(" a1.Address3 Address3, \n");
            pvcquery.append(" a1.CityCode CityCode, \n");
            pvcquery.append(" a1.CityName CityName, \n");
            pvcquery.append(" a1.CountryCode CountryCode, \n");
            pvcquery.append(" a1.PostalCode PostalCode, \n");
            pvcquery.append(" a1.CustomerLastModifDate SiteLastModifDate, \n");
            pvcquery.append(" a1.StateCode StateCode, \n");
            pvcquery.append(" a1.ContactName ContactName, \n");
            pvcquery.append(" a1.EmailAddress EmailAddress, \n");
            pvcquery.append(" a1.FaxNumber FaxNumber, \n");
            pvcquery.append(" a1.TelephoneNumber TelephoneNumber, \n");
            pvcquery.append(" a1.ServiceOrigin ServiceOrigin, \n");

            pvcquery.append(" p.TransportType ConnectionType \n");

            pvcquery.append(" FROM " + CISConstants.CIS_TABLENAME_PVC + " p,  ");
            pvcquery.append(CISConstants.CIS_TABLENAME_AC + " a1 ");
            pvcquery.append(" WHERE p.S_TYPE = '" + getCISPVCProductName() + "' ");
            pvcquery.append(" AND p.LocalAccessConnectionUSID = a1.USID ");
            pvcquery.append("  AND p.SERVICESTATUS != '" + CISConstants.CIS_STATUS_CANCELLED + "' ");
            pvcquery.append(" AND p.PURELAYER2FLAG = 'Yes'");
            pvcquery.append(" AND a1.PURELAYER2FLAG = 'Yes'");
            pvcquery.append(getAddressCondition("a1."));
            cis_logger.info("Query without custcode:");
            cis_logger.info(pvcquery.toString());

            formMultipleQuery(custIdVec, pvcqueryVec, pvcquery, "a1.CUSTCODE");
            setProductPVCQuery(pvcqueryVec);
        }
        }catch (SQLException sqle) {
            throw new CISException("Error while initating MTN queries: " + sqle);
        } finally{
        closeResultSet(ps, result);
        result = null;
        ps = null;
        }

    }

    protected void formMultipleQuery(Vector custIdVec, Vector queryVec, StringBuffer queryWithoutCust, String prefix) {
        for (int i = 0; i < custIdVec.size(); i++) {
            queryVec.add(queryWithoutCust.toString() +  " AND " + prefix + " IN ( " + (String) custIdVec.get(i) + " )");
        }
    }

    protected void initRemoteUSIDAttrs(boolean hasPVC, Vector custIdVec) throws CISException {
        //init remote attribute table
        Hashtable remoteAttrHT = getRemoteUSIDAttrs();
        boolean isMTNProd = false;

        if (hasPVC && remoteAttrHT == null) {

            if (CISConstants.CSI_PRODUCT_ATM_PURPLE.equals(getCSIProductNameFromMappingTable())
                    || CISConstants.CSI_PRODUCT_FR_PURPLE.equals(getCSIProductNameFromMappingTable())) {
                isMTNProd = true;
            }

            long startTime = System.currentTimeMillis();

            remoteAttrHT = new Hashtable();
            StringBuffer remoteQuery = new StringBuffer();
            remoteQuery.append("Select a.USID, a.CustomerDefinedCode, a.UserRepertoryName, a.SellingEntityCrossReference, a.ConnectionAddress ");
            remoteQuery.append(" FROM " + CISConstants.CIS_TABLENAME_AC + " a, ");
            remoteQuery.append(isMTNProd? CISConstants.CIS_TABLENAME_PVC + " p ": CISConstants.CIS_TABLENAME_IP_PVC + " p ");
            remoteQuery.append(" WHERE a.USID = p.RemoteAccessConnectionUSID ");
            remoteQuery.append(" AND p.S_TYPE = '" + getCISPVCProductName() + "' " );
            remoteQuery.append(" AND a.SERVICESTATUS != '" + CISConstants.CIS_STATUS_CANCELLED + "' ");
            if (isMTNProd) {
                remoteQuery.append(" AND a.PURELAYER2FLAG = 'Yes'");
            } else {
                remoteQuery.append(" AND a.PURELAYER2FLAG = 'No'");
            }
            if (cis_logger.isDebugEnabled())
                cis_logger.info("RemoteQuery Without CustCode Condition:" + remoteQuery.toString());

            for (int i = 0; i < custIdVec.size(); i++) {
                String remoteQueryStr = remoteQuery.toString() + " AND a.CUSTCODE in ( " + (String) custIdVec.get(i) + " ) ";

                PreparedStatement remotePS = null;
                ResultSet remoteRS = null;
                try {
                    int remoteAttCount = 0;
                    remotePS = m_sybaseConn.prepareStatement(remoteQueryStr);
                    remoteRS = remotePS.executeQuery();
                    while (remoteRS.next()) {
                        remoteAttCount++;
                        if (cis_logger.isDebugEnabled())
                            cis_logger.debug("USID added|" + remoteRS.getString(1) + "|");
                        String[] strings = {remoteRS.getString(2), remoteRS.getString(3), remoteRS.getString(4), remoteRS.getString(5)};
                        remoteAttrHT.put(remoteRS.getString(1), strings);
                    }

                    if (cis_logger.isDebugEnabled())
                        cis_logger.info("Total Remote USID added: " + remoteAttCount);
                } catch (SQLException sqle) {
                    throw new CISException("Error in init remote attrs arrays: " + sqle);
                } finally {
                    closeResultSet(remotePS, remoteRS);
                    remotePS = null;
                    remoteRS = null;
                }
            }

            setRemoteUSIDAttrs(remoteAttrHT);

            long endTime = System.currentTimeMillis();
            if (cis_logger.isDebugEnabled())
                cis_logger.info("Init Remote Attribute Hashtable took: " + (endTime - startTime)/1000 + " seconds." );
        }
    }

    protected Vector getCustomerConditionString() throws CISException {
        //gold customer id condition
        StringBuffer custId = new StringBuffer();
        ResultSet searchResult = null;
        PreparedStatement ps1 = null;
        Vector custIdVec = new Vector();
        try {
            ps1 = m_goldConn.prepareStatement("select ORGANIZATIONID from sc_organization where status = 0 ");
            ps1.setFetchSize(1000);

            searchResult = ps1.executeQuery();

            while (searchResult.next()) {
                String custCode = searchResult.getString(1).trim();
                custId.append("'" + custCode + "',");
                // cis migration change
                if((searchResult.getRow()%1000)== 0){
                	custId.deleteCharAt(custId.length() - 1);
                    custIdVec.add(custId.toString());
                    custId = new StringBuffer();
                }
                /*if (custId.length() > m_max_custcode_length ) {
                    custId.deleteCharAt(custId.length() - 1);
                    custIdVec.add(custId.toString());
                    custId = new StringBuffer();
                }*/
            }

            searchResult.close();

            if (custId != null && custId.length() > 0) {
                custId.deleteCharAt(custId.length() - 1);
                custIdVec.add(custId.toString());
            }

        } catch (SQLException sqle) {
            throw new CISException("Error while getting customer id list:" + sqle);
        } finally {
            closeResultSet(ps1, searchResult);
            searchResult = null;
            ps1 = null;
        }

        cis_logger.info("Size of Custcode Vec:" + custIdVec.size());
        for (int i=0;i<custIdVec.size();i++) {
            cis_logger.info("Custcode:" + i);
            cis_logger.info((String) custIdVec.get(i));
        }

        cis_logger.info(custId.toString());
        return custIdVec;
    }

    /**
     *
     * @param result
     * @param classType
     * @param category
     * @param m_version
     * @throws SQLException
     * @throws JAXBException
     * @throws CISException
     */
    protected void setServiceElementAndAttr(ResultSet result, String classType, String category, Version m_version) throws SQLException, JAXBException, CISException {
        setServiceElementAndAttr(result, classType, category, m_version, null);
    }


    /**
     * @param result
     * @param classType
     * @throws SQLException
     * @throws JAXBException
     */
    protected void setServiceElementAndAttr(ResultSet result, String classType, String category, Version m_version, String extraCheckingKey) throws SQLException, JAXBException, CISException {
        ServiceElement m_srvElement = initServiceEelment(category);
        //set service element properties
        if (CSIConstants.SECLASS_ACCESSCONNECTION.equals(classType) || CSIConstants.SECLASS_CPE.equals(classType)) {
            m_srvElement.setId(result.getString("USID"));
        } else if (CSIConstants.SECLASS_SERVICEOPTIONS.equals(classType)) {
            m_srvElement.setId("SO:" + result.getString("USID"));
        } else if (CSIConstants.SECLASS_BACKUP.equals(classType)) {
            m_srvElement.setId("BKP::" + result.getString("USID"));
        } else if (CSIConstants.SECLASS_TRANSPORT.equals(classType)) {
            if (CISConstants.CSI_PRODUCT_ATM_PURPLE.equals(getCSIProductNameFromMappingTable()) || CISConstants.CSI_PRODUCT_FR_PURPLE.equals(getCSIProductNameFromMappingTable())) {
                m_srvElement.setId(result.getString("PVCUSID"));
            }else {
                m_srvElement.setId(result.getString("MESHINGUSID"));
            }
        } else if (CSIConstants.SECLASS_NASBACKUP.equals(classType)) {
            m_srvElement.setId("NASBKP: " + result.getString("USID"));  //there is a space after :
        }

        m_srvElement.setServiceElementClass(classType);
        m_srvElement.setDescription(classType);
        m_srvElement.setName(classType);
        m_srvElement.setCreationDate(convertSQLTimeToTransdate(result.getTimestamp("CREATIONDATE")));
        m_srvElement.setSourceSystem(CSIConstants.SYSTEM_TYPE_CIS);

        //set attribute object
        String mappingKey;
        String cisColName;
        String cisValue;
        String csiValue;
        String csiColName;

        Hashtable mapping = getProductFieldMappingTable();
        Enumeration keys = mapping.keys();

        boolean hasAttribute = false;

        while (keys.hasMoreElements()) {
            mappingKey = (String) keys.nextElement();
            if ( (extraCheckingKey == null && mappingKey.indexOf(m_mapping_delim + classType) >= 0) ||
                    (extraCheckingKey != null && mappingKey.indexOf(m_mapping_delim + classType + m_mapping_delim + extraCheckingKey) >= 0)) {
                cisColName = mappingKey.substring(0, mappingKey.indexOf(m_mapping_delim));
                csiColName = (String) mapping.get(mappingKey);

                cisValue = result.getString(cisColName);
                cisValue = cisValue == null ? cisValue : cisValue.trim();

                csiValue = convertCISValue(m_srvElement.getId(), classType, csiColName, cisValue, result);

                if (csiValue != null && csiValue.length() > 0) {
                    if ("BackupOptions".equals(csiColName)) {
                        hasAttribute = setBackupOptions(csiValue, m_srvElement, hasAttribute);
                    } else {
                        m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute(csiColName, csiValue));
                        hasAttribute = true;
                    }

                    //set remote usid related attribute
                    if (CSIConstants.SECLASS_TRANSPORT.equals(classType) && "RemoteAccessConnectionUSID".equals(cisColName)) {
                        handleRemoteAccessConnectionAttr(m_srvElement, cisValue, getRemoteUSIDAttrs());
                    }
                }
            }
        }

        //add more attribute to service element
        if (CSIConstants.SECLASS_ACCESSCONNECTION.equals(classType)) {
            hasAttribute = setExtraACAttributes(result, m_srvElement) || hasAttribute;
        } else if (CSIConstants.SECLASS_TRANSPORT.equals(classType) ) {
            hasAttribute = setExtraPVCAttributes(result, m_srvElement) || hasAttribute;
        } else if (CSIConstants.SECLASS_CPE.equals(classType)) {
            hasAttribute = setExtraCPEAttributes(result, m_srvElement) || hasAttribute;
        } else if (CSIConstants.SECLASS_NASBACKUP.equals(classType)) {
            hasAttribute = setExtraNasBackupAttributes(result, m_srvElement) || hasAttribute;
        }

        if (hasAttribute) {
            setServiceEelmentToVersion(m_version, m_srvElement);
        }
    }

    /**
     * @param result
     * @param m_srvElement
     * @return
     * @throws SQLException
     * @throws JAXBException
     */
    protected boolean setExtraACAttributes(ResultSet result, ServiceElement m_srvElement) throws SQLException, JAXBException {
        boolean hasAttribute = false;
        hasAttribute = setACPortType(result, m_srvElement) || hasAttribute;

        hasAttribute = setACIMALinkGroup(m_srvElement, result) || hasAttribute;

        return hasAttribute;
    }

    private boolean setACIMALinkGroup(ServiceElement m_srvElement, ResultSet result) throws SQLException, JAXBException {
        boolean hasAttribute = false;
        //ATM is for all color products
        if (CISConstants.CSI_PRODUCT_IPVPN.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_INTERNETDIRECT.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_INTRANETCONNECT.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CIS_PRODUCT_ATM_AC.equals(getCISACProductName())) {
            String numIMA = convertCISValue(m_srvElement.getId(), m_srvElement.getServiceElementClass(), "Number of Circuits in IMA", StringUtils.strip(result.getString("NbrOfCirInIMA")), result);
            if (numIMA != null && numIMA.length() > 0) {
                ServiceElementAttribute attrLinkGroups = m_objectFactory.createServiceElementAttribute();
                attrLinkGroups.setName("IMA Link Group");
                attrLinkGroups.setValue(numIMA);
                m_srvElement.getServiceElementAttribute().add(attrLinkGroups);
                hasAttribute = true;
            }
        }
        return hasAttribute;
    }

    private boolean setACPortType(ResultSet result, ServiceElement m_srvElement) throws SQLException, JAXBException {
        boolean hasAttribute = false;
        //handle connectiontype and connectionsubtype
        if (CISConstants.CSI_PRODUCT_LANAS.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_MANAGED_CPE.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_IPVPN.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_INTERNETDIRECT.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_INTRANETCONNECT.equals(getCSIProductNameFromMappingTable())) {
            String connectionType = StringUtils.strip(result.getString("ConnectionType"));
            String connectionSubType = StringUtils.strip(result.getString("ConnectionSubType"));
            String value1 = "";
            String value2 = "";

            if (connectionType != null && connectionType.length() > 0 && connectionSubType != null && connectionSubType.length() > 0) {
                if ("MTN".equals(connectionType)) {
                    if ("ATM".equals(connectionSubType)) {
                        value1 = "ATM";
                    } else if ("FR".equals(connectionSubType)) {
                        value1 = "Frame Relay";
                        value2 = "Frame Relay";
                    }
                } else if ("Direct Access".equals(connectionType)) {
                    if (null == connectionSubType) {
                        value2 = "DDR";
                    } else if ("POS".equals(connectionSubType)) {
                        value1 = "POS";
                    } else {
                        value1 = "Direct Access";
                    }
                } else if ("Ethernet".equals(connectionType)) {
                    value1 = "Ethernet Access";
                } else if ("Third Party".equals(connectionType)) {
                    if ("TDSL".equals(connectionSubType)) {
                        value1 = "DSL";
                    }else if ("ATM".equals(connectionSubType)) {
                        value1 = "ATM";
                    }else if ("FR".equals(connectionSubType)) {
                        value1 = "Frame Relay";
                        value2 = "Frame Relay";
                    }
                } else if ("DSL".equals(connectionType)) {
                    if ("ATM".equals(connectionSubType)) {
                        value1 = "DSL";
                    }
                } else if ("No access".equals(connectionType)) {
                    if ("LAN".equals(connectionSubType)) {
                        value2 = "Cascaded";
                    }
                }

                if (CISConstants.CSI_PRODUCT_LANAS.equals(getCSIProductNameFromMappingTable()) || CISConstants.CSI_PRODUCT_MANAGED_CPE.equals(getCSIProductNameFromMappingTable())) {
                    if (value2 != null && value2.length() > 0) {
                        ServiceElementAttribute attr = m_objectFactory.createServiceElementAttribute();
                        attr.setName("Port Type");
                        attr.setValue(value2);
                        m_srvElement.getServiceElementAttribute().add(attr);
                        hasAttribute = true;
                    }
                } else {
                    if (value1 != null && value1.length() > 0) {
                        ServiceElementAttribute attr = m_objectFactory.createServiceElementAttribute();
                        attr.setName("Port Type");
                        attr.setValue(value1);
                        m_srvElement.getServiceElementAttribute().add(attr);

                        if (CISConstants.CSI_PRODUCT_IPVPN.equals(getCSIProductNameFromMappingTable())
                                || CISConstants.CSI_PRODUCT_INTRANETCONNECT.equals(getCSIProductNameFromMappingTable())) {
                            if (!StringUtils.isBlank(result.getString("ConnectionAddress"))) {
                                ServiceElementAttribute attrDNA = m_objectFactory.createServiceElementAttribute();
                                if ("ATM".equals(value1)) {
                                    attrDNA.setName("Local ATM Type Address");
                                } else {
                                    attrDNA.setName("Local DNA Address");
                                }

                                attrDNA.setValue(result.getString("ConnectionAddress"));
                                m_srvElement.getServiceElementAttribute().add(attrDNA);
                                hasAttribute = true;
                            }
                        }
                    }
                }
            }
        }

        return hasAttribute;
    }

    /**
     * @param result
     * @param m_srvElement
     * @return
     * @throws SQLException
     * @throws JAXBException
     */
    protected boolean setExtraCPEAttributes(ResultSet result, ServiceElement m_srvElement) throws SQLException, JAXBException {
        boolean hasAttribute = false;
        hasAttribute = setCPERouterFunction(result, m_srvElement) ;
        return hasAttribute;
    }

    private boolean setCPERouterFunction(ResultSet result, ServiceElement m_srvElement) throws SQLException, JAXBException {
        boolean hasAttribute = false;
        //FunctionType
        if (CISConstants.CSI_PRODUCT_LANAS.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_MANAGED_CPE.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_IPVPN.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_INTERNETDIRECT.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_INTRANETCONNECT.equals(getCSIProductNameFromMappingTable())) {
            String routerFunctionCisValue = result.getString("FunctionType");
            String csiValue = null;
            String routerFunction = null;
            String managedBy = null;
            String supplier = null;

            String routerCard = StringUtils.strip(result.getString("RouterCards"));
            String routerCardNum = StringUtils.strip(result.getString("CardNumber"));

            if (routerFunctionCisValue != null && routerFunctionCisValue.length() > 0) {
                csiValue = convertCISValue(m_srvElement.getId(), m_srvElement.getServiceElementClass(), "Router Function", routerFunctionCisValue, result);
                int pos1 = csiValue.indexOf('/');
                int pos2 = csiValue.indexOf('/', pos1 + 1);
                if (pos1 > -1 && pos2 > -1) {
                    routerFunction = StringUtils.strip(csiValue.substring(0, pos1));
                    managedBy = StringUtils.strip(csiValue.substring(pos1 + 1, pos2));
                    supplier = StringUtils.strip(csiValue.substring(pos2 + 1));


                    ServiceElementAttribute attrRF = m_objectFactory.createServiceElementAttribute();
                    attrRF.setName("RouterFunction");
                    attrRF.setValue(routerFunction);
                    m_srvElement.getServiceElementAttribute().add(attrRF);

                    ServiceElementAttribute attrMB = m_objectFactory.createServiceElementAttribute();
                    attrMB.setName("Managed By");
                    attrMB.setValue(managedBy);
                    m_srvElement.getServiceElementAttribute().add(attrMB);

                    ServiceElementAttribute attrSP = m_objectFactory.createServiceElementAttribute();
                    attrSP.setName("Supplier");
                    attrSP.setValue(supplier);
                    m_srvElement.getServiceElementAttribute().add(attrSP);

                    hasAttribute = true;
                }
            }

            if (routerCard != null && routerCard.length() > 0 && routerCardNum != null && routerCardNum.length() > 0) {
                ServiceElementAttribute attrRouterCard = m_objectFactory.createServiceElementAttribute();
                attrRouterCard.setName(routerCard);
                attrRouterCard.setValue(routerCardNum);
                m_srvElement.getServiceElementAttribute().add(attrRouterCard);
                hasAttribute = true;
            }
        }
        return hasAttribute;
    }

    /**
     * @param result
     * @param m_srvElement
     * @return
     * @throws SQLException
     * @throws JAXBException
     */
    protected boolean setExtraPVCAttributes(ResultSet result, ServiceElement m_srvElement) throws SQLException, JAXBException {
        boolean hasAttribute = false;
        hasAttribute = setPVCRemoteSiteCode(result, m_srvElement);
        hasAttribute = setPVCBurstOption(result, m_srvElement) || hasAttribute;
        hasAttribute = setPVCConnectionType(result, m_srvElement) || hasAttribute;
        hasAttribute = setPVCMainConnectionType(m_srvElement, result) || hasAttribute;

        //set service type
        /* No need to do for this release
        if (CISConstants.CSI_PRODUCT_IPVPN.equals(getCSIProductNameFromMappingTable())) {
            String serviceType = result.getString("ServiceType");
            String d1 = result.getString("D1");
            String d2 = result.getString("D2");
            String d3 = result.getString("D3");

            String csiValue = null;

            if (serviceType != null && serviceType.length() > 0) {
                csiValue = convertCISValue(m_srvElement.getId(),m_srvElement.getServiceElementClass(), "Service Type", serviceType, result);

                if (serviceType.equals(csiValue)) {
                    csiValue = convertCISValue(m_srvElement.getId(),m_srvElement.getServiceElementClass(), "Service Type", serviceType + "/" + d1 + "/" + d2 + "/" + d3, result);
                }

                ServiceElementAttribute attrST = m_objectFactory.createServiceElementAttribute();
                attrST.setName("Service Type");
                attrST.setValue(csiValue);
                m_srvElement.getServiceElementAttribute().add(attrST);

                hasAttribute = true;
            }
        } */
        return hasAttribute;
    }

    private boolean setPVCMainConnectionType(ServiceElement m_srvElement, ResultSet result) throws SQLException, JAXBException {
        boolean hasAttribute = false;
        if (CISConstants.CSI_PRODUCT_IPVPN.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_INTRANETCONNECT.equals(getCSIProductNameFromMappingTable())) {
            String transportType = convertCISValue(m_srvElement.getId(), m_srvElement.getServiceElementClass(), "Transport Type", StringUtils.strip(result.getString("TransportType")), result);
            if (transportType != null && transportType.length() > 0) {
                ServiceElementAttribute attrConnectionType = m_objectFactory.createServiceElementAttribute();
                attrConnectionType.setName("Connection Type (Main - Shadow)");
                attrConnectionType.setValue(transportType);
                m_srvElement.getServiceElementAttribute().add(attrConnectionType);
                hasAttribute = true;
            }
        }
        return hasAttribute;
    }

    private boolean setPVCConnectionType(ResultSet result, ServiceElement m_srvElement) throws SQLException, JAXBException {
        boolean hasAttribute = false;
        if (CISConstants.CIS_PRODUCT_ATM_PVC.equals(getCISPVCProductName()) ||
                CISConstants.CSI_PRODUCT_LANAS.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_MANAGED_CPE.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_IPVPN.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_INTERNETDIRECT.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_INTRANETCONNECT.equals(getCSIProductNameFromMappingTable())) {
            String localVCI = result.getString("LocalVCI");
            String localVPI = result.getString("LocalVPI");
            String remoteVCI = result.getString("RemoteVCI");
            String remoteVPI = result.getString("RemoteVPI");

            localVCI = localVCI == null ? null : localVCI.trim();
            localVPI = localVPI == null ? null : localVPI.trim();
            remoteVCI = remoteVCI == null ? null : remoteVCI.trim();
            remoteVPI = remoteVPI == null ? null : remoteVPI.trim();

            if (localVCI != null && localVCI.length() > 0 && localVPI != null && localVPI.length() > 0) {
                hasAttribute = true;
                ServiceElementAttribute attr = m_objectFactory.createServiceElementAttribute();
                attr.setName("Connection Type (VCC - VPC)");

                ServiceElementAttribute attr1 = m_objectFactory.createServiceElementAttribute();
                attr1.setName("Local vci Value");

                float localVCIFloat = Float.parseFloat(localVCI);
                float localVPIFloat = Float.parseFloat(localVPI);

                if (localVPIFloat == 0) {
                    attr.setValue("VCC");
                } else {
                    attr.setValue("VPC");
                }

                attr1.setValue(String.valueOf(Math.round(localVPIFloat)) + "." + String.valueOf(Math.round(localVCIFloat)));

                m_srvElement.getServiceElementAttribute().add(attr);
                m_srvElement.getServiceElementAttribute().add(attr1);
            }

            if (remoteVCI != null && remoteVCI.length() > 0 && remoteVPI != null && remoteVPI.length() > 0) {
                hasAttribute = true;
                ServiceElementAttribute attr2 = m_objectFactory.createServiceElementAttribute();
                attr2.setName("Remote vci Value");

                float remoteVCIFloat = Float.parseFloat(remoteVCI);
                float remoteVPIFloat = Float.parseFloat(remoteVPI);

                attr2.setValue(String.valueOf(Math.round(remoteVPIFloat)) + "." + String.valueOf(Math.round(remoteVCIFloat)));
                m_srvElement.getServiceElementAttribute().add(attr2);
            }
        }
        return hasAttribute;
    }

    private boolean setPVCBurstOption(ResultSet result, ServiceElement m_srvElement) throws SQLException, JAXBException {
        boolean hasAttribute = false;
        //for all color of Frame Relay Product
        if (CISConstants.CIS_PRODUCT_FR_PVC.equals(getCISPVCProductName()) ||
                CISConstants.CSI_PRODUCT_LANAS.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_MANAGED_CPE.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_IPVPN.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_INTERNETDIRECT.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_INTRANETCONNECT.equals(getCSIProductNameFromMappingTable())) {
            String cir = result.getString("CIR");
            String eir = result.getString("EIR");
            String remoteCir = result.getString("RemoteToLocalCIR");
            String remoteEir = result.getString("RemoteToLocalEIR");
            if (cir != null && eir != null) {
                hasAttribute = true;
                ServiceElementAttribute attr = m_objectFactory.createServiceElementAttribute();
                attr.setName("Symmetric");
                if (cir.equals(remoteCir) && eir.equals(remoteEir)) {
                    attr.setValue("Symmetric PVC");
                } else {
                    attr.setValue("Asymmetric PVC");
                }
                m_srvElement.getServiceElementAttribute().add(attr);

                ServiceElementAttribute attr1 = m_objectFactory.createServiceElementAttribute();
                attr1.setName("Burst Option");
                float cirFloat = Float.parseFloat(cir);
                float eirFloat = Float.parseFloat(eir);
                if (eirFloat == 0) {
                    attr1.setValue("No Bursting");
                } else if (eirFloat > 0 && eirFloat <= cirFloat / 2 && cirFloat <= 128000) {
                    attr1.setValue("Standard");
                } else {
                    attr1.setValue("Premium");
                }
                m_srvElement.getServiceElementAttribute().add(attr1);
            }
        }
        return hasAttribute;
    }

    private boolean setPVCRemoteSiteCode(ResultSet result, ServiceElement m_srvElement) throws SQLException, JAXBException {
        boolean hasAttribute = false;
        String remoteSiteACUSID = result.getString("RemoteAccessConnectionUSID");
        if (remoteSiteACUSID != null && remoteSiteACUSID.trim().length() > 0) {
            hasAttribute = true;
            m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Remote Site", "UNKNOWN"));

            m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Remote Site Code", "UNKNOWN"));
        }
        return hasAttribute;
    }

    /**
     * @param result
     * @param m_srvElement
     * @return
     * @throws SQLException
     * @throws JAXBException
     */
    protected boolean setExtraNasBackupAttributes(ResultSet result, ServiceElement m_srvElement) throws SQLException, JAXBException {
        boolean hasAttribute = false;
        hasAttribute = setNBKFunctionType(result, m_srvElement);
        return hasAttribute;
    }

    private boolean setNBKFunctionType(ResultSet result, ServiceElement m_srvElement) throws SQLException, JAXBException {
        boolean hasAttribute = false;
        //FunctionType
        if (CISConstants.CSI_PRODUCT_LANAS.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_MANAGED_CPE.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_IPVPN.equals(getCSIProductNameFromMappingTable()) ||
                CISConstants.CSI_PRODUCT_INTRANETCONNECT.equals(getCSIProductNameFromMappingTable())) {
            String nasArchitectureCisValue = result.getString("NASArchitecture");
            if (nasArchitectureCisValue != null && nasArchitectureCisValue.length() > 0) {

                if (nasArchitectureCisValue.indexOf("Primary") >= 0) {
                    hasAttribute = true;
                    ServiceElementAttribute attr = m_objectFactory.createServiceElementAttribute();
                    attr.setName("Function Type NAS");
                    attr.setValue("Primary");
                    m_srvElement.getServiceElementAttribute().add(attr);
                } else if (nasArchitectureCisValue.indexOf("Secondary") >= 0) {
                    hasAttribute = true;
                    ServiceElementAttribute attr = m_objectFactory.createServiceElementAttribute();
                    attr.setName("Function Type NAS");
                    attr.setValue("Secondary");
                    m_srvElement.getServiceElementAttribute().add(attr);
                }
            }
        }
        return hasAttribute;
    }

    /**
     * @param result
     * @return
     * @throws SQLException
     */
    protected String handleCircuitTypeLL(ResultSet result) throws SQLException {
        String csiValue = null;
        String circuitSpeed = result.getString("CIRCUITSPEED");
        try {
            float circuitSpeedInt = Float.parseFloat(circuitSpeed);
            if (circuitSpeedInt < 56000) {
                csiValue = "Analogue Leased Line";
            } else if (circuitSpeedInt < 64000) {
                //TODO: find out more
                csiValue = "Digital Leased Line";
            } else {
                csiValue = "Digital Leased Line";
            }
        } catch (Exception e) {
            csiValue = null;
        }
        return csiValue;
    }

    /**
     * @param csiValue
     * @param m_srvElement
     * @param hasAttribute
     * @return
     * @throws JAXBException
     */
    protected boolean setBackupOptions(String csiValue, ServiceElement m_srvElement, boolean hasAttribute) throws JAXBException {
        String backupType = null;
        String backupSpeed = null;
        boolean hasAttributeLocal = false;
        if (csiValue.indexOf('/') >= 0) {
            backupType = csiValue.substring(0, csiValue.indexOf('/') - 1).trim();
            backupSpeed = csiValue.substring(csiValue.indexOf('/') + 1).trim();
        } else {
            backupType = csiValue.trim();
        }

        if (backupType != null) {
            m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Backup Type", backupType));
            hasAttributeLocal = true;
        }

        if (backupSpeed != null) {
            m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Speed", backupSpeed));
            hasAttributeLocal = true;
        }

        if (hasAttributeLocal) {
            m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Access Connection Name", "Access Connection"));
            m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("In Place", "Using Existing"));
            hasAttribute = true;
        }
        return hasAttribute;
    }

    /**
     * @param query
     * @param whereDateClause
     * @throws CISException
     */
    protected void addLastModifDateCond(StringBuffer query, String whereDateClause) throws CISException {
    	try{
	        Date lastRunDate = getLastRunDate();
	        if (lastRunDate != null) {
	            //String dateStr = CISManagerHelper.getSybaseFormatDateStr(lastRunDate);
	        	SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	        	String dateStr = format.format(lastRunDate);
	            if (query.toString().toUpperCase().indexOf("WHERE") >= 0) {
	            	query.append(" AND " + whereDateClause );
	            } else {
	            	query.append(" WHERE " + whereDateClause);
	            }
	            query.append("TO_DATE('"+dateStr+"', 'MM/DD/YYYY HH24:MI:SS')");
	            
	            //query = CISManagerHelper.appendLiteral(query, dateStr);
	        }
    	}
    	catch(Exception ex){
    		cis_logger.info("Exception in addLastModifDateCond()... "  + ex);
    	}
    }


    /**
     * @throws SQLException
     */
    protected ResultSet getResultSetFromQuery(StringBuffer query, PreparedStatement ps) throws SQLException {
        //cis_logger.info("Query String:" + query);
        ps.setFetchSize(5000);
        long startTime = System.currentTimeMillis();
        ResultSet result = ps.executeQuery();
        long endTime = System.currentTimeMillis();
        cis_logger.info("Query took " + (endTime - startTime) / 1000 + " Seconds.");
        return result; 
    }

    /**
     *
     */
    protected boolean setSiteInformation(ResultSet result, SiteInformation m_site, String errorHeader) throws SQLException {
        m_site.setCustCode(StringUtils.strip(result.getString("CUSTCODE")));
        m_site.setAddress1(StringUtils.strip(result.getString("ADDRESS1")));
        m_site.setAddress2(StringUtils.strip(result.getString("ADDRESS2")));
        m_site.setAddress3(StringUtils.strip(result.getString("ADDRESS3")));
        m_site.setCityCode(StringUtils.strip(result.getString("CITYCODE")));
        m_site.setCountryCode(StringUtils.strip(result.getString("COUNTRYCODE")));

        String postalCode = StringUtils.strip(result.getString("POSTALCODE"));
        //add strip here for GOLD
        if (StringUtils.isNotBlank(postalCode) && !StringUtils.isAlphanumericSpace(postalCode)) {
            cis_site_logger.error(errorHeader + CISConstants.NEWLINE + "PostalCode has invalide characters: " + postalCode);
            return false;
        }

        m_site.setPostalCode(StringUtils.strip(postalCode));
        m_site.setStateCode(StringUtils.strip(result.getString("STATECODE")));
        m_site.setCityName(StringUtils.strip(result.getString("CITYNAME")));
        m_site.setSourceSystem("CIS");
//        m_site.setLastModifDate(convertSQLTimeToTransdate(result.getTimestamp("SITELASTMODIFDATE")));
        SiteContact m_contact = (SiteContact) m_site.getSiteContact().get(0);
        //add trim here for GOLD
        m_contact.setContactName(StringUtils.strip(result.getString("CONTACTNAME")));
        m_contact.setTelephoneNumber(StringUtils.strip(result.getString("TELEPHONENUMBER")));
        //only set faxnumber for mtn proudct, not for ip product.  IP product data is corrupt.
        if (CISConstants.CSI_PRODUCT_ATM_PURPLE.equals(getCSIProductNameFromMappingTable())
                || CISConstants.CSI_PRODUCT_FR_PURPLE.equals(getCSIProductNameFromMappingTable())) {
            m_contact.setFaxNumber(StringUtils.strip(result.getString("FAXNUMBER")));
        }
        m_contact.setEmailAddress(StringUtils.strip(result.getString("EMAILADDRESS")));

        //prepare site block for gold
        GOLDManager.normalizeAddress(m_site, GOLDManager.ADDRESS_LENGTH_MAX);
        return true;
    }

    /**
     *
     */
    protected String convertCISValue(String usid, String classType, String csiAttriName, String cisValue, ResultSet result)
            throws SQLException {

        String csiValue = null;
        Hashtable valueMapping = getProductValueMappingTable();

        if ("Circuit Type".equals(csiAttriName) && ("LL".equals(cisValue) || "LEASED".equals(cisValue))) {
            csiValue = handleCircuitTypeLL(result);
        } else {
            String key = classType + m_mapping_delim + csiAttriName + m_mapping_delim + (cisValue == null ? "Null" : cisValue);
            String defaultKey = classType + m_mapping_delim + csiAttriName + m_mapping_delim + "DEFAULT";
            String formattedKey = classType + m_mapping_delim + csiAttriName + m_mapping_delim + (cisValue == null ? "Null" : formatNumber(cisValue));
            if (valueMapping.containsKey(key)) {
                csiValue = (String) valueMapping.get(key);
            } else if ("AccessConnection".equals(classType)
                    && ("Circuit Speed".equals(csiAttriName) || "Port Speed".equals(csiAttriName))
                    && valueMapping.containsKey(formattedKey)) { //Added for formating the speed/portspeed value - to chop '.000'.
                csiValue = (String) valueMapping.get(formattedKey); //Added for formating the speed/portspeed value.
            } else if (valueMapping.containsKey(defaultKey)) {
                csiValue = (String) valueMapping.get(defaultKey);
            } else if (isIgnoreValue(classType, csiAttriName, cisValue)) {
                csiValue = null;
            } else if (cisValue != null) {
                csiValue = convertSpeedAttribute(classType, csiAttriName, cisValue);
            } else {
                csiValue = cisValue;
            }

            if ("Invalid Value".equals(csiValue)) {
                csiValue = null;
            }

            //for all color ATM product
            if ( (CISConstants.CIS_PRODUCT_ATM_AC.equals(getCISACProductName()) ||
                  CISConstants.CSI_PRODUCT_LANAS.equals(getCSIProductNameFromMappingTable()) ||
                  CISConstants.CSI_PRODUCT_MANAGED_CPE.equals(getCSIProductNameFromMappingTable()) ||
                  CISConstants.CSI_PRODUCT_IPVPN.equals(getCSIProductNameFromMappingTable()) ||
                  CISConstants.CSI_PRODUCT_INTERNETDIRECT.equals(getCSIProductNameFromMappingTable()) ||
                  CISConstants.CSI_PRODUCT_INTRANETCONNECT.equals(getCSIProductNameFromMappingTable()))
                    && CSIConstants.SECLASS_ACCESSCONNECTION.equals(classType)
                    && "Termination Interface".equals(csiAttriName)
                    && "IMA".equals(csiValue)) {
                String imaLinkType = StringUtils.strip(result.getString("IMALinksType"));
                if ("DS1".equals(imaLinkType)) {
                    csiValue = "DS1/T1 - IMA";
                } else if ("E1".equals(imaLinkType)) {
                    csiValue = "E1 - IMA";
                } else {
                    csiValue = null;
                }
            }
        }

        if (cis_logger.isDebugEnabled())
            cis_logger.debug("valuemapping|"+usid+"|"+classType+"|"+csiAttriName+"|"+cisValue+"|"+csiValue);

        return csiValue;
    }

    /**
     * @param classType
     * @param csiAttriName
     * @return
     */
    protected boolean isIgnoreValue(String classType, String csiAttriName, String cisValue) {
        boolean isIgnoreValue = false;
        if (CSIConstants.SECLASS_BACKUP.equals(classType)) {
            isIgnoreValue = true;
        }
        return isIgnoreValue;
    }

    /**
     * @param classType
     * @param csiAttriName
     * @return
     */
    protected String convertSpeedAttribute(String classType, String csiAttriName, String cisValue) {
        String csiValue = null;

        if (CSIConstants.SECLASS_ACCESSCONNECTION.equals(classType) && ("Circuit Speed".equals(csiAttriName) || "Port Speed".equals(csiAttriName))) {
            cisValue = formatNumber(cisValue);
			csiValue = cisValue + CISConstants.CIS_ATTR_SPEED;
        } else if ( (CSIConstants.SECLASS_TRANSPORT.equals(classType) && ("CIR".equals(csiAttriName) || "EIR".equals(csiAttriName)
                || "Remote-to-Local CIR".equals(csiAttriName) || "Remote-to-Local EIR".equals(csiAttriName)))
                || (CSIConstants.SECLASS_ACCESSCONNECTION.equals(classType) && "IP Bandwidth".equals(csiAttriName)) ) {
            try {
                float cisValueFloat = Float.parseFloat(cisValue) / 1000;
                String tempCsiValue = "" + cisValueFloat;
                if (tempCsiValue.endsWith(".0")) tempCsiValue = tempCsiValue.substring(0, tempCsiValue.length() - 2);
                csiValue = tempCsiValue + CISConstants.CIS_ATTR_SPEED;
            } catch (Exception e) {
                cis_logger.fatal(e);
            }
        } else {
            csiValue = cisValue;
        }
        return csiValue;
    }
	
	/**
     * @param inValue
     * @return
     */
	protected String formatNumber(String inValue) {

        if (inValue == null || (inValue != null && inValue.trim().equals(""))) return inValue;
        DecimalFormat nf = new DecimalFormat("######.0##");
        nf.setGroupingUsed(false);

        try {
            float fVal = Float.parseFloat(inValue);
            String formattedVal = nf.format(fVal);
            if (formattedVal.indexOf(".") != -1) {
                if (Integer.parseInt(formattedVal.substring(formattedVal.lastIndexOf(".") + 1, formattedVal.trim().length())) == 0) {
                    return "" + (long) fVal;
                } else {
                    return inValue;
                }
            }
        } catch (Exception e) {

        }
        return inValue;
    }



    /**
     * @param m_srvElement
     * @param remoteAccessUSID
     */
    protected void handleRemoteAccessConnectionAttr(ServiceElement m_srvElement, String remoteAccessUSID) throws CISException {

        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            String queryStr = "Select CustomerDefinedCode, UserRepertoryName, SellingEntityCrossReference, ConnectionAddress From "
                    + CISConstants.CIS_TABLENAME_AC
                    + " WHERE USID = ?";
            statement = m_sybaseConn.prepareStatement(queryStr);
            statement.setString(1, remoteAccessUSID);
            result = statement.executeQuery();
            while (result.next()) {

                String remoteDNA = result.getString("ConnectionAddress");
                String remoteCDC = result.getString("CustomerDefinedCode");
                String remoteURN = result.getString("UserRepertoryName");
                String remoteSelling = result.getString("SellingEntityCrossReference");

                if (remoteDNA != null && remoteDNA.length() > 0) {
                    if (CISConstants.CIS_PRODUCT_ATM_PVC.equals(getCISPVCProductName())) {
                        m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Remote ATM Type Address", remoteDNA));
                    } else if (CISConstants.CIS_PRODUCT_FR_PVC.equals(getCISPVCProductName())) {
                        m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Local DNA Address", remoteDNA));
                    }
                }

                if (remoteCDC != null && remoteCDC.length() > 0) {
                    m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Remote Customer Defined Code", remoteCDC));
                }

                if (remoteURN != null && remoteURN.length() > 0) {
                    m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Remote User Repertory Name", remoteURN));
                }

                if (remoteSelling != null && remoteSelling.length() > 0) {
                    m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Remote Selling Entity Cross Reference", remoteSelling));
                }
            }

        } catch (SQLException sqle) {
            throw new CISException(sqle);
        } catch (javax.xml.bind.JAXBException jxbj) {
            throw new CISException(jxbj);
        } finally {
            closeResultSet(statement, result);
            result = null;
            statement = null;
        }
    }

    /**
     * @param m_srvElement
     * @param remoteAccessUSID
     */
    protected void handleRemoteAccessConnectionAttr(ServiceElement m_srvElement, String remoteAccessUSID, Hashtable remoteAttrs) throws JAXBException {
        if (remoteAttrs != null && remoteAttrs.size() > 0) {
            String[] strs = null;
            String remoteDNA = null;
            String remoteCDC = null;
            String remoteURN = null;
            String remoteSelling = null;

            strs = (String[]) remoteAttrs.get(remoteAccessUSID);
            if (strs != null) {
                remoteDNA = strs[3];
                remoteCDC = strs[0];
                remoteURN = strs[1];
                remoteSelling = strs[2];

                if (remoteDNA != null && remoteDNA.length() > 0) {
                    if (CISConstants.CIS_PRODUCT_ATM_PVC.equals(getCISPVCProductName())) {
                        m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Remote ATM Type Address", remoteDNA));
                    } else if (CISConstants.CIS_PRODUCT_FR_PVC.equals(getCISPVCProductName())) {
                        m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Remote DNA Address", remoteDNA));
                    }
                }

                if (remoteCDC != null && remoteCDC.length() > 0) {
                    m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Remote Customer Defined Code", remoteCDC));
                }

                if (remoteURN != null && remoteURN.length() > 0) {
                    m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Remote User Repertory Name", remoteURN));
                }

                if (remoteSelling != null && remoteSelling.length() > 0) {
                    m_srvElement.getServiceElementAttribute().add(createServiceElementAttribute("Remote Selling Entity Cross Reference", remoteSelling));
                }

            } else {
                if (cis_logger.isDebugEnabled())
                    cis_logger.debug("Remote USID |" + remoteAccessUSID + "| not found in the hashtable.");
            }
        }
    }

    /**
     * @param attrName
     * @param value
     * @return
     * @throws JAXBException
     */
    protected ServiceElementAttribute createServiceElementAttribute(String attrName, String value) throws JAXBException {
        ServiceElementAttribute attr = m_objectFactory.createServiceElementAttribute();
        attr.setName(attrName);
        attr.setValue(value);
        return attr;
    }

     /**
      *
      */
     protected Version addBackupDisconnectVersion(Version m_version, ResultSet result) throws SQLException, JAXBException {
         Version version = m_objectFactory.createVersion();

         version.setEndUserId(m_version.getEndUserId());
         version.setCustomerId(m_version.getCustomerId());
         version.setOrderSentDate(m_version.getOrderSentDate());
         version.setServiceId(m_version.getServiceId());

         com.equant.csi.jaxb.System system = m_objectFactory.createSystem();
         User user = m_objectFactory.createUser();
         system.setId(CSIConstants.SYSTEM_TYPE_CIS);
         user.setName(CISConstants.CIS_CSI_PROGUSER);
         system.setUser(user);
         version.setSystem(system);

         version.setOrdertype(CSIConstants.ORDER_TYPE_DISCONNECT);
         version.setOrderstatus(CSIConstants.STATUS_ORDER_MANAGE);

         ServiceElement srvElement = initServiceEelment(CSIConstants.ELEMENT_TYPE_DISCONNECT);
         srvElement.setServiceElementClass(CSIConstants.SECLASS_BACKUP);
         srvElement.setDescription(CSIConstants.SECLASS_BACKUP);
         srvElement.setName(CSIConstants.SECLASS_BACKUP);
         srvElement.setSourceSystem(CSIConstants.SYSTEM_TYPE_CIS);
         srvElement.setId("BKP::" + result.getString("USID"));

         version.getServiceElement().add(srvElement);

         return version;
     }

    /**
     *
     */
    protected String getAddressCondition(String prefix) {
        return " AND " + prefix + "ADDRESS1 is not null "
            + " AND " + prefix + "CITYNAME is not null "
            + " AND " + prefix + "POSTALCODE is not null "
            + " AND " + prefix + "COUNTRYCODE is not null "
            + " AND LENGTH(" + prefix + "POSTALCODE) <= 9 ";
    }

    /**
     *
     */
    protected void closeResultSet(PreparedStatement ps, ResultSet result) {
         //close result set
        try {
            if (result != null) result.close();
            if (ps != null) ps.close();
        } catch (SQLException e) {
            cis_logger.debug("Error while close resultset and statement.");
        }
    }

    /**
     *
     */
    protected String getSiteErrorMsgHeader(String productToRun, String serviceComponentToRun, String usid) {
        return CISConstants.NEWLINE
             + CISConstants.NEWLINE + "Insufficient information in CIS to create/update site, ignore this record. "
             + CISConstants.NEWLINE + "Product: " + productToRun
             + CISConstants.NEWLINE + "Service Component: " + serviceComponentToRun
             + CISConstants.NEWLINE + "USID: " + usid;
    }

    /**
     * @return
     */
    protected abstract String getCISACProductName();

    /**
     * @return
     */
    protected abstract String getCISPVCProductName();

    /**
     * @return
     */
    protected abstract Hashtable getProductFieldMappingTable();

    /**
     * @param mapping
     */
    protected abstract void setProductFieldMappingTable(Hashtable mapping);

    /**
     *
     */
    protected abstract String run() throws CISException;

    /**
     *
     */
    protected abstract Vector getProductACQuery();

    /**
     *
     */
    protected abstract Vector getProductPVCQuery();

    /**
     *
     */
    protected abstract void setProductACQuery(Vector query);

    /**
     *
     */
    protected abstract void setProductPVCQuery(Vector query);

    /**
     *
     */
    protected abstract Hashtable getProductValueMappingTable();

    /**
     *
     */
    protected abstract void setProductValueMappingTable(Hashtable mapping);

    /**
     *
     */
    protected abstract String getCSIProductNameFromMappingTable();

    /**
     *
     */
    protected abstract String getCSIProductByServiceOrig(String serviceOrig);

    protected Hashtable getRemoteUSIDAttrs() {return null;}

    protected void setRemoteUSIDAttrs(Hashtable hs) {}

}
