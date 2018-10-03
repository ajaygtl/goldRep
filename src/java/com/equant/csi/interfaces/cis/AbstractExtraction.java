package com.equant.csi.interfaces.cis;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.ejb.CreateException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;

import org.apache.log4j.Category;

import com.equant.csi.ejb.biz.ServiceManager;
import com.equant.csi.ejb.biz.ServiceManagerHome;
import com.equant.csi.exceptions.CISException;
import com.equant.csi.utilities.LoggerFactory;
import com.equant.csi.weblogic.AbstractTimeService;

public abstract class AbstractExtraction extends AbstractTimeService {
    protected static Category logger = LoggerFactory.getInstance(AbstractExtraction.class);
    protected static final Category cis_logger = LoggerFactory.getInstance("cis_extraction.CISIPProduct");


    protected String m_productToRun;
    protected Set m_productToRunSet = null;

    protected static String m_jndiCISDataSourceName;
    protected static String m_jndiCSIDataSourceName;
    protected static String m_jndiGOLDDataSourceName;

    private DataSource m_cisDataSource;
    protected DataSource m_csiDataSource;
    protected DataSource m_goldDataSource;
    protected ServiceManager m_serviceManager;

    public boolean allowSimultaneousRun() {
        return false;
    }

    /**
     * help metho
     */
    protected void setProductToRunHT() {
        if (m_productToRunSet == null && m_productToRun != null) {
            m_productToRunSet = CISManagerHelper.parseProductToRun(m_productToRun);
        }
    }

    /**
     * @param runType
     * @throws com.equant.csi.exceptions.CISException
     *
     */
    protected void cisRun(int runType) throws CISException {

        Connection sybaseConn = null;
        Connection localConn = null;
        Connection goldConn = null;
        ServiceManager sm = null;
        try {
            sybaseConn = getCISDataSource().getConnection();
            localConn = getLocalCSIDataSource().getConnection();
            goldConn = getGOLDDataSource().getConnection();
            sm = getServiceManager();
            CISManagerHelper.cisSubRun(sybaseConn, localConn, runType, sm, goldConn, m_productToRunSet, cis_logger);
        } catch (NamingException ne) {
            logger.fatal(ne);
        } catch (CISException cise) {
            logger.fatal(cise);
        } catch (Exception e) {
            logger.fatal(e);
        } finally {
            try {
                logger.debug("close connection");
                localConn.close();
                sybaseConn.close();
                goldConn.close();

            } catch (SQLException e) {
                logger.fatal(e);
            }
        }
    }

    /**
     * Lookup for CIS datasource and return it
     *
     * @return CIS DataSource object
     * @throws NamingException
     */
    DataSource getCISDataSource() throws NamingException {
        if (m_cisDataSource == null) {
            InitialContext ic = new InitialContext();
            m_cisDataSource = (DataSource) ic.lookup(m_jndiCISDataSourceName);
        }
        return m_cisDataSource;
    }

    /**
     * Lookup for local CSI datasource and return it
     *
     * @return CSI DataSource object
     * @throws NamingException
     */
    DataSource getLocalCSIDataSource() throws NamingException {
        if (m_csiDataSource == null) {
            InitialContext ic = new InitialContext();
            m_csiDataSource = (DataSource) ic.lookup(m_jndiCSIDataSourceName);
        }
        return m_csiDataSource;
    }

    /**
     * @return
     * @throws NamingException
     */
    DataSource getGOLDDataSource() throws NamingException {
        if (m_goldDataSource == null) {
            InitialContext ic = new InitialContext();
            m_goldDataSource = (DataSource) ic.lookup(m_jndiGOLDDataSourceName);
        }
        return m_goldDataSource;
    }

    /**
     * Creates and returns an instance of ServiceManager EJB.
     *
     * @return the instance of ServiceManager EJB
     * @throws NamingException           thrown for any error during retrieving
     *                                   the object with JNDI
     * @throws javax.ejb.CreateException the creation of the ServiceManager object failed
     * @throws java.rmi.RemoteException  the communication-related exceptions that may occur
     *                                   during the execution of a remote method call
     */
    ServiceManager getServiceManager()
            throws NamingException, CreateException, RemoteException {

        if (m_serviceManager == null) {
            InitialContext ic = new InitialContext();
            // changed as part of gold infra. change 
           // ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup(ServiceManagerHome.class.getName()), ServiceManagerHome.class);
            ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup("com.equant.csi.ejb.biz.ServiceManagerHome"), ServiceManagerHome.class);
            m_serviceManager = home.create();
        }
        return m_serviceManager;
    }
}
