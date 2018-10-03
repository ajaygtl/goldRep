package com.equant.csi.test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.naming.InitialContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import junit.framework.TestCase;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

//import weblogic.management.AdminServer;
import org.ow2.jonas.commands.admin.ClientAdmin;
import com.equant.csi.common.ServiceIdHolder;
import com.equant.csi.ejb.api.CsiApi;
import com.equant.csi.ejb.api.CsiApiHome;
import com.equant.csi.ejb.biz.ServiceManager;
import com.equant.csi.ejb.biz.ServiceManagerHome;
import com.equant.csi.jaxb.Message;
import com.equant.csi.utilities.JAXBUtils;
import com.equant.csi.utilities.LoggerFactory;

/**
 * @version $Id: CSIDataTest.java,v 1.2 2005/01/19 09:32:40 artem.shevchenko Exp $
 */
public class CSIDataTest extends TestCase {
    private static final Category logger = LoggerFactory.getInstance(CSIDataTest.class.getName()); 
    Properties properties = null;
    private ArrayList services = new ArrayList(); 
    
    public static final int ITERATOR = 4000;
    
    private Hashtable m_logLevels = new Hashtable();

    public CSIDataTest(String name) {
        super(name);
        
        // load services data... we should convert this to load from flat file
        services.add(new ServiceIdHolder("9503", "KONE Escalators Keighley", "IP_VPN"));
        services.add(new ServiceIdHolder("9338", "ACE-Frankfurt", "IP_VPN"));
        services.add(new ServiceIdHolder("9319", "KN High Tech Center", "LAN_ACCESS"));
        services.add(new ServiceIdHolder("4233", "4233_LA Cons", "FRAME_RELAY_PURPLE"));
        services.add(new ServiceIdHolder("28358", "SLF", "IP_VPN"));
        services.add(new ServiceIdHolder("6402", "VIEB6402-1", "LAN_ACCESS"));
    }

    public void testStart() {
        logger.debug("Starting WebLogic Server");
        try {
           // weblogic.Server.main(new String[]{});
        //  weblogic.Server.main(new String[]{});
            // code for starting jonas server 
           	 FileInputStream fis;
           	 fis = new FileInputStream("../../../ant.properties");
    			 properties.load(fis);
    			 System.setProperty("jonas.root", properties.getProperty("jonas.root"));
    		     System.setProperty("jonas.name", "jonas");
    		     System.setProperty("jonas.base", properties.getProperty("jonas.base"));
    		     String[] arg={"-start"};
    		     ClientAdmin.main(arg);
            Thread.sleep(20000);
        } catch (Exception e) {
            logger.error("Exception", e);
            fail("Got exception: " + e);
        }
    }

    public void testWait() {
        logger.debug("Wait for WebLogic Server startup");
        int counter = 20;
        try {
            Thread.sleep(5000);
        } catch (Exception ee) {
        }
        while(true) {
            counter--;
            if (counter < 0) {
                fail("Could not startup weblogic in 100 seconds");
            }
            try {
                new InitialContext().lookup("com.equant.csi.ejb.api.CsiApiHome");
                new InitialContext().lookup("com.equant.csi.ejb.biz.ServiceManagerHome");
            } catch (Throwable e) {
                logger.debug("EJBs are not deployed yet. Got exception: " + e);
                try { Thread.sleep(5000); } catch (Exception ee) { }
                continue;
            }
            break;
        }

        try { Thread.sleep(1000); } catch (Exception ee) { }
    }

    /* The first - we have to test that all beans are up and running */

    public void testCsiApiBean() {
    	//if(true) return;
    	
        logger.debug("Test CsiApi Bean");
        try {
            CsiApiHome home = (CsiApiHome)new InitialContext().lookup(CsiApiHome.class.getName());
            CsiApi csi = home.create();

            assertNotNull("CsiApi has not been initialized", csi);
            logger.debug("CsiApi has been initialized");
            csi.remove();
        } catch (Exception e) {
            logger.error("Exception", e);
            fail("Got exception " + e);
        }
    }

    public void testServiceManagerBean() {
    	//if(true) return;
    	
        logger.debug("Test ServiceManager Bean");
        try {
            ServiceManagerHome home = (ServiceManagerHome)new InitialContext().lookup(ServiceManagerHome.class.getName());
            ServiceManager manager = home.create();

            assertNotNull("ServiceManager has not been initialized", manager);
            logger.debug("ServiceManager has been initialized");
            manager.remove();
        } catch (Exception e) {
            logger.error("Exception", e);
            fail("Got exception " + e);
        }
    }

    public void testGetServices() throws Exception {
        try {
        	long max = Long.MIN_VALUE, min = Long.MAX_VALUE, maxLocal = Long.MIN_VALUE, minLocal = Long.MAX_VALUE, total = 0;
        	// disabling loggin for Log4j, so it does not print a lot.
        	disableLoggers();
            CsiApiHome home = (CsiApiHome)new InitialContext().lookup("com.equant.csi.ejb.api.CsiApiHome");
            CsiApi csi = home.create();
            
            for(int i = 0;i < ITERATOR; i++) {
                for(Iterator iter = services.iterator();iter.hasNext(); ) {
                    long start = System.currentTimeMillis();
                    ServiceIdHolder id = (ServiceIdHolder)iter.next();
                    Vector v = csi.getServices(id.customerId, id.siteId, null, null);
                    long diff = System.currentTimeMillis() - start; 
                    if(maxLocal < diff) maxLocal = diff; 
                    if(minLocal > diff) minLocal = diff;
                    total+=diff;
                    
                    if (i > 0 && i % 100 == 0) {
                        String si = "" + i;
                        while (si.length() < 7) si = " " + si;
                        logger.debug(
                        		"Iteration " + si + ": used " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) +
    							": CsiApi.getServices(\"" + id.customerId + "\", \"" + id.siteId + "\"): MAX: " + maxLocal + " msec, MIN: " + minLocal + " msec, AVERAGE: " + (total/(i+1)) + " msec" 
                        );
                        if(max<maxLocal) max=maxLocal;
                        if(min>minLocal) min=minLocal;
                        maxLocal = Long.MIN_VALUE;
                        minLocal = Long.MAX_VALUE;
                    }
                }
            }
            logger.debug("CsiApi.getServices(...): MAX: " + max + " msec, MIN: " + min + " msec, AVERAGE: " + (total/ITERATOR) + " msec");
        } catch (Throwable e) {
            logger.error("Exception", e);
            fail("Exception " + e);
        } finally {
        	enableLoggers();
        }
    }

    public void testGetServiceDetails() throws Exception {
        try {
        	long max = Long.MIN_VALUE, min = Long.MAX_VALUE, maxLocal = Long.MIN_VALUE, minLocal = Long.MAX_VALUE, total = 0;
        	// disabling loggin for Log4j, so it does not print a lot.
        	disableLoggers();
            CsiApiHome home = (CsiApiHome)new InitialContext().lookup("com.equant.csi.ejb.api.CsiApiHome");
            CsiApi csi = home.create();
            JAXBContext ctx = JAXBUtils.getContext();
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            unmarshaller.setValidating(true);
            for(int i = 0;i < ITERATOR; i++) {
                for(Iterator iter = services.iterator();iter.hasNext(); ) {
                    long start = System.currentTimeMillis();
                    ServiceIdHolder id = (ServiceIdHolder)iter.next();
                    String messageText = csi.getServiceDetails(id.customerId, id.siteId, null, null, id.servicerId);
                    if(messageText!=null) {
                    	Message messageDB = (Message)unmarshaller.unmarshal(new ByteArrayInputStream(messageText.getBytes()));
                    }
                    long diff = System.currentTimeMillis() - start; 
                    if(maxLocal < diff) maxLocal = diff; 
                    if(minLocal > diff) minLocal = diff;
                    total+=diff;
    
                    if (i > 0 && i % 100 == 0) {
                        String si = "" + i;
                        while (si.length() < 7) si = " " + si;
                        logger.debug(
                        		"Iteration " + si + ": used " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) +
    							": CsiApi.getServiceDetails(\"" + id.customerId + "\", \"" + id.siteId + "\", \"" + id.servicerId + "\"): MAX: " + maxLocal + " msec, MIN: " + minLocal + " msec, AVERAGE: " + (total/(i+1)) + " msec" 
    					);
                        if(max<maxLocal) max=maxLocal;
                        if(min>minLocal) min=minLocal;
                        maxLocal = Long.MIN_VALUE;
                        minLocal = Long.MAX_VALUE;
                    }
                }
            }
            logger.debug("CsiApi.getServiceDetails(...): MAX: " + max + " msec, MIN: " + min + " msec, AVERAGE: " + (total/ITERATOR) + " msec");
        } catch (Throwable e) {
            logger.error("Exception", e);
            fail("Exception " + e);
        } finally {
        	enableLoggers();
        }
    }

    public void testStop() {
        logger.debug("Stop jonas Server");
        try {
           // AdminServer.getAdminServer().getServer().stop();
        	 FileInputStream fis;
           	 fis = new FileInputStream("../../../ant.properties");
    			 properties.load(fis);
    			 System.setProperty("jonas.root", properties.getProperty("jonas.root"));
    		     System.setProperty("jonas.name", "jonas");
    		     System.setProperty("jonas.base", properties.getProperty("jonas.base"));
    		     String[] arg={"-stop"};
    		     ClientAdmin.main(arg);
        } catch (Exception e) {
            logger.error("Got exception", e);
            fail("Got exception: " + e);
        }
    }
    
    // helpers
    private void disableLoggers() {
    	Enumeration loggers = LogManager.getCurrentLoggers();
    	if(loggers!=null) {
    		while(loggers.hasMoreElements()) {
    			Logger l = (Logger)loggers.nextElement();
				String name = l.getName();
			    Level level = l.getLevel();
    			if(getClass().getName().equals(name)) {
        			logger.debug("Got logger \"" + name + "\" which has \"" + (level==null?"NULL?":level.toString()) + "\" level - skipping own logger");
    			} else {
    				if(level==null) {
    					level = Logger.getRootLogger().getLevel();
    				}
        			logger.debug("Got logger \"" + name + "\" which has \"" + (level==null?"NULL?":level.toString()) + "\" level - disabling output, except ERROR");
    				l.setLevel(Level.ERROR);
  					m_logLevels.put(name, level);
    			}
    		}
    	}
    }

    private void enableLoggers() {
    	Enumeration loggers = LogManager.getCurrentLoggers();
    	if(loggers!=null) {
    		while(loggers.hasMoreElements()) {
    			Logger l = (Logger)loggers.nextElement();
				String name = l.getName();
			    Level level = (Level)m_logLevels.get(name);
			    if(level!=null) {
        			logger.debug("Restoring logger \"" + name + "\" for \"" + level.toString() + "\".");
        			l.setLevel(level);
			    }
    		}
    	}
    }
}
