package com.equant.csi.interfaces.cis;

import com.equant.csi.exceptions.CISException;
import com.equant.csi.exceptions.CSIException;
import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.Version;
import com.equant.csi.jaxb.ServiceElement;
import com.equant.csi.jaxb.User;
import com.equant.csi.jaxb.SiteInformation;
import com.equant.csi.jaxb.SiteContact;
import com.equant.csi.jaxb.ObjectFactory;
import com.equant.csi.jaxb.ServiceElementStatus;
import com.equant.csi.jaxb.ServiceElementAttribute;
import com.equant.csi.common.TransDate;
import com.equant.csi.ejb.biz.ServiceManagerHome;
import com.equant.csi.ejb.biz.ServiceManager;

import javax.xml.bind.JAXBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.ejb.CreateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.rmi.RemoteException;

/**
 * Created by IntelliJ IDEA.
 * User: szhu
 * Date: Jun 30, 2004
 * Time: 3:48:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestCISConnection {
    public static void main(String[] args) {

        try {

            Properties p = new Properties();
            // jonas/jms migration
            /*
			p.put("java.naming.factory.initial", "weblogic.jndi.WLInitialContextFactory");
            p.put("java.naming.provider.url", "t3://localhost:7000");
            p.put("weblogic.jndi.createIntermediateContexts", "true");
			*/
    		p.put(Context.INITIAL_CONTEXT_FACTORY, "org.ow2.carol.jndi.spi.IRMIContextWrapperFactory");
    		p.put(Context.URL_PKG_PREFIXES,"org.ow2.jonas.naming" );
    		p.put(Context.PROVIDER_URL, "rmi://localhost:1099");

            InitialContext ic = new InitialContext(p);
          //  ServiceManagerHome home = (ServiceManagerHome) ic.lookup(ServiceManagerHome.class.getName());
         // changed as part of csi jonas migration 
            // ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup(ServiceManagerHome.class.getName()), ServiceManagerHome.class);
             ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup("com.equant.csi.ejb.biz.ServiceManagerHome"), ServiceManagerHome.class);
            ServiceManager ejbServiceManager = home.create();
            ejbServiceManager.runCISExtract(2, "ATMPVC");

        } catch (CISException cise) {
            System.out.println(cise.toString());
        } catch (javax.naming.NamingException jnne) {
            System.out.println(jnne.toString());
        } catch (java.rmi.RemoteException jrre) {
            System.out.println(jrre.toString());
        } catch (javax.ejb.CreateException jece) {
            System.out.println(jece.toString());
        }
    }

    /*
    protected Hashtable m_atm_mapping = null;

    protected ObjectFactory m_objectFactory = null;
    protected Message m_message = null;
    protected Version m_version = null;
    protected ServiceElement m_srvElement = null;
    protected ServiceElementStatus m_srvStatus = null;
    protected SiteInformation m_site = null;
    protected SiteContact m_contact = null;
    protected com.equant.csi.jaxb.System m_system = null;
    protected User m_user = null;

    private void cisRun() {
        try {
            //load the sybase jdbc driver
            Class.forName("com.sybase.jdbc2.jdbc.SybDriver");
            // open connection
            Connection sybaseConn = DriverManager.getConnection("jdbc:sybase:Tds:cisyulsrv.tb.yul.equant.com:1616", "csigold", "csigoldete");

            StringBuffer query = new StringBuffer("select siteid, custcode, type, usid, CustomerDefinedCode, LastModifDate, " +
                    "ServiceStatus, ServiceSubStatus, CessationDate, CityCode, StreetName, SellingEntityCrossRef, OrderedLineSpeed " +
                    "From crm_atm_service ");

            String whereDateClause = " where lastmodifdate > ";

            Date lastRunDate = CISMTNProductBase.getLastRunDate(getLocalCSIConnection(), 1, sybaseConn);
            System.out.println("lastRunDate.getTime() = " + lastRunDate.getTime());
            System.out.println("lastRunDate = " + lastRunDate);
            if (lastRunDate != null) {
                String dateStr = CISManagerHelper.getSybaseFormatDateStr(lastRunDate);
                System.out.println("dateStr = " + dateStr);

                query.append(whereDateClause);
                query = CISManagerHelper.appendLiteral(query, dateStr);
                System.out.println("query = " + query);
            }

            Statement dateStatement = sybaseConn.createStatement();
            ResultSet result = dateStatement.executeQuery(query.toString());

            int i = 0;
            initMapping(getLocalCSIConnection());
            initObjectFactory();

            while (result.next()) {
                i++;
                initGeneralObject();
                //set Version properties
                m_version.setCustomerId(result.getString("CUSTCODE"));
                m_version.setServiceId(result.getString("TYPE"));
                TransDate lastModifDate = CISMTNProductBase.convertSQLTimeToTransdate(result.getTimestamp("LastModifDate"));
                m_version.setOrderSentDate(lastModifDate);

                //set service elemet status
                m_srvStatus.setStatus(CISMTNProductBase.setCISStatusToCSIStatus(result.getString("SERVICESTATUS"), result.getString("SERVICESUBSTATUS")));

                //set site
                m_site.setCISSiteId(result.getString("SITEID"));
                m_site.setCityCode(result.getString("CITYCODE"));
                m_site.setAddress1(result.getString("STREETNAME"));


                setServiceElementAndAttr(result, "ACCESSCONNECTION");

            }
            System.out.println("i:" + i);

            Timestamp ts = CISMTNProductBase.getCurrentSybaseTimeStamp();
            CISMTNProductBase.insertExtractLog(getLocalCSIConnection(), CISConstants.CIS_EXTRACTTYPE_DAY,
                    ts, ts, CISConstants.CIS_EXTRACTSTATUS_SUCCESS);


        } catch (ClassNotFoundException cnfe) {
            System.out.println(cnfe.toString());
        } catch (SQLException sqle) {
            System.out.println(sqle.toString());
        } catch (CISException cisex) {
            System.out.println(cisex.toString());
        } catch (JAXBException jaxb) {
            System.out.println(jaxb.toString());
        }
    }

    private void setServiceElementAndAttr(ResultSet result, String classType) throws SQLException, JAXBException {
        //set service element properties
        m_srvElement.setId(result.getString("USID"));
        m_srvElement.setServiceElementClass(classType);
        m_srvElement.setSourceSystem("CIS");

        //set attribute object
        String cisColName;
        String cisValue;
        String csiColName;
        Enumeration keys = m_atm_mapping.keys();

        while (keys.hasMoreElements()) {
            cisColName = (String) keys.nextElement();
            csiColName = (String) m_atm_mapping.get(cisColName);
            System.out.println("csiColName = " + csiColName);
            if (csiColName.indexOf(classType) >= 0)  {
                csiColName = csiColName.substring(csiColName.indexOf(",") + 1);
                System.out.println("csiColName = " + csiColName);
            cisValue = result.getString(cisColName);
            if (cisValue != null && cisValue.length() > 0) {
                ServiceElementAttribute attr = m_objectFactory.createServiceElementAttribute();
                attr.setName(csiColName);
                attr.setValue(cisValue);
                m_srvElement.getServiceElementAttribute().add(attr);
            }
            }

        }
    }

    private void initGeneralObject() throws JAXBException {
        m_message = m_objectFactory.createMessage();
        m_version = m_objectFactory.createVersion();
        m_srvElement = m_objectFactory.createServiceElement();
        m_srvStatus = m_objectFactory.createServiceElementStatus();
        m_site = m_objectFactory.createSiteInformation();
        m_contact = m_objectFactory.createSiteContact();

        m_message.getVersion().add(m_version);
        m_message.setSiteInformation(m_site);

        m_version.setSystem(m_system);
        m_version.getServiceElement().add(m_srvElement);
        m_srvElement.getServiceElementStatus().add(m_srvStatus);

        m_site.getSiteContact().add(m_contact);
    }

    private Connection getLocalCSIConnection() {
        Connection oracleConn = null;
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            oracleConn = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:GOLDDEV", "csiadm", "csiadm");
        } catch (ClassNotFoundException cnfe) {
            System.out.println(cnfe.toString());
        } catch (SQLException sqle) {
            System.out.println(sqle.toString());
        }

        return oracleConn;
    }

    private void initObjectFactory() throws JAXBException {
        if (m_objectFactory == null) {
            m_objectFactory = new ObjectFactory();
        }

        if (m_system == null) {
            m_system = m_objectFactory.createSystem();
            m_system.setId("CIS");
            m_user = m_objectFactory.createUser();
            m_user.setName(CISConstants.CIS_CSI_PROGUSER);
            m_system.setUser(m_user);
        }
    }

    private void initMapping(Connection localConn) throws SQLException {
        PreparedStatement ps = localConn.prepareStatement("SELECT CISCOLUMNNAME, CSISRVELMCLASS, CSISRVELMATTRNAME FROM CCISMAPPING " +
                " WHERE CSISRVELMATTRNAME IS NOT NULL AND STATUS = 0 AND CSISERVICE = ? ");

        String cisColumnName = null;
        String csiColumnName = null;
        String csiServiceClass = null;

        if (m_atm_mapping == null) {
            m_atm_mapping = new Hashtable();
            ps.setString(1, "ATM");
            ResultSet result = ps.executeQuery();
            while (result.next()) {
                cisColumnName = result.getString(1);
                csiServiceClass = result.getString(2);
                csiColumnName = result.getString(3);
                m_atm_mapping.put(cisColumnName, csiServiceClass + "," + csiColumnName);
            }
        }

        ps.close();
    }
    */

}
