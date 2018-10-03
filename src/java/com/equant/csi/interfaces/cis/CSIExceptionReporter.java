/**
 * $Equant$
 * 
 * (c) Equant: GOLD 4.3 [Global Order Lifecycle Delivery] 
 * Date: Jun 30, 2004
 */
package com.equant.csi.interfaces.cis;

import java.util.Date;
import java.util.Hashtable;

import javax.management.Notification;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import org.apache.log4j.Category;

import com.equant.csi.common.CSIConstants;
import com.equant.csi.ejb.biz.ServiceManager;
import com.equant.csi.ejb.biz.ServiceManagerHome;
import com.equant.csi.utilities.LoggerFactory;
import com.equant.csi.weblogic.AbstractTimeService;

/**
 * $Comment
 * 
 * @author Vasyl Rublyov
 * @version $Revision$
 */
public class CSIExceptionReporter extends AbstractTimeService {
    /** Initializing the logger. */
    private final static Category logger = LoggerFactory.getInstance(CSIExceptionReporter.class.getName());

    protected boolean m_isIdleWeekly;
    protected boolean m_isIdleMonthly;
    protected Object synchronizeWeekly = new Object();
    protected Object synchronizeMonthly = new Object();

    protected long m_weeklyRun;
    protected long m_monthlyRun;

    protected String m_storageLocation;

    public void init(Date kickoff, Long repeat, Hashtable args) {
        if(logger.isInfoEnabled()) {
            logger.info("Initilizing ExtractionFull (CIS Full Extraction) at " + kickoff + " and repeat every " + repeat + " msec");
        }
        m_storageLocation = (String)args.get(CSIConstants.ARG_CSI_EXCETION_REPORT_STORAGE);

        if(logger.isDebugEnabled()) {
            logger.debug(" - storageLocation: " + m_storageLocation);
        }
    }
    
    public void run(Notification notification, Hashtable args) {
        new GoRunner(false).go();
    }
    
    public boolean allowSimultaneousRun() {
        return false;
    }


    protected class GoRunner implements Runnable {
        protected boolean m_isMonthlyRun;

        public GoRunner(boolean isMonthlyRun) {
            m_isMonthlyRun = isMonthlyRun;
        }

        public void go() {
//            Thread t = new Thread(GoRunner.this);
//            t.start();
            run();
        }

        public void run() {
            if(logger.isDebugEnabled()) {
                logger.debug("We are in " + (m_isMonthlyRun?"mothly":"weekly") +" run thread...");
            }
            try {
                InitialContext ic = new InitialContext();
             //   ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup(ServiceManagerHome.class.getName()), ServiceManagerHome.class);
                // changed as part of csi jonas migration 
                // ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup(ServiceManagerHome.class.getName()), ServiceManagerHome.class);
                 ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup("com.equant.csi.ejb.biz.ServiceManagerHome"), ServiceManagerHome.class);
                ServiceManager serviceManager  = home.create();
                if(serviceManager!=null) {
                    serviceManager.runExceptionReport(m_isMonthlyRun, m_storageLocation);
                } else {
                    logger.error("CSIExceptionReporter: can't get ServiceManager EJB remote inteface.");
                }
            } catch(Throwable t) {
                logger.error(t);
            }

            if(m_isMonthlyRun) m_isIdleWeekly = true; else m_isIdleMonthly = true;
        }
    }
}
