package com.equant.csi.interfaces.cis;

import java.util.Date;
import java.util.Hashtable;

import javax.management.Notification;

import org.apache.log4j.Logger;

import com.equant.csi.utilities.LoggerFactory;

public class ExtractionFull extends AbstractExtraction {
    protected static Logger cis_logger = LoggerFactory.getInstance("cis_extraction.ExtractionFull");

    public void init(Date kickoff, Long repeat, Hashtable args) {
        if(cis_logger.isInfoEnabled()) {
            cis_logger.info("Initilizing ExtractionFull (CIS Full Extraction) at " + kickoff + " and repeat every " + repeat + " msec");
        }
        // Empty method definition
        if(m_productToRunSet!=null) {
            m_productToRunSet.clear();
            m_productToRunSet = null;
        }
        m_productToRun = (String)args.get(CISConstants.ARG_CIS_NAME_PRODUCTLIST);
        setProductToRunHT();

        m_jndiCISDataSourceName = (String)args.get(CISConstants.ARG_CIS_NAME_CISDS);
        m_jndiCSIDataSourceName = (String)args.get(CISConstants.ARG_CIS_NAME_CSIDS);
        m_jndiGOLDDataSourceName = (String)args.get(CISConstants.ARG_CIS_NAME_GOLDDS);

        if(logger.isDebugEnabled()) {
            cis_logger.debug(" - productToRun: " + m_productToRun);
            cis_logger.debug(" - jndiCISDataSourceName: " + m_jndiCISDataSourceName);
            cis_logger.debug(" - jndiGOLDDataSourceName: " + m_jndiGOLDDataSourceName);
            cis_logger.debug(" - m_productToRunSet: " + m_productToRunSet);
        }
    }

    public void run(Notification notification, Hashtable args) {
        if (cis_logger.isDebugEnabled()) {
            cis_logger.debug("Waking up ExtractionFull.run() process.");
            cis_logger.debug("The current runtime is at " + new Date());
        }

        try {
            synchronized (this) {
                cisRun(CISConstants.CIS_EXTRACTTYPE_FULL);
            }
        } catch (Exception e) {
            logger.fatal(e);
        }
    }
}
