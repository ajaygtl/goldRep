package com.equant.csi.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import junit.framework.TestCase;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

//import weblogic.management.AdminServer;

import org.ow2.jonas.commands.admin.ClientAdmin;

import com.equant.csi.ejb.api.CsiApi;
import com.equant.csi.ejb.api.CsiApiHome;
import com.equant.csi.ejb.biz.ServiceManager;
import com.equant.csi.ejb.biz.ServiceManagerHome;
import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.utilities.JAXBUtils;
import com.equant.csi.utilities.LoggerFactory;

/**
 * @version CVS $Id: CSIBeansTest.java,v 1.11 2003/01/09 23:07:24 constantine.evenko Exp $
 */
public class CSIBeansTest extends TestCase {
    private static final Category logger = LoggerFactory.getInstance(CSIBeansTest.class.getName());

    public static final String SITE_ID = "Test Site";
    public static final String SERVICE_NAME = "FRAME_RELAY_TEST";
    public static final int ITERATOR = 4000;
    Properties properties = null;
    private Hashtable m_logLevels = new Hashtable();

    public CSIBeansTest(String name) {
        super(name);
    }

    public void testStart() {
        logger.debug("Starting jonas Server");
        try {
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

    public void testCsiApiBeanExceptionHandling() {
    	//if(true) return;
    	
        logger.debug("Test Exception Handling");
        try {
            CsiApiHome home = (CsiApiHome)new InitialContext().lookup(CsiApiHome.class.getName());
            CsiApi csi = home.create();

            logger.debug("Preparing incorectly formed message...");
            // This XML should cause exception in the CSI.
            String incorrectXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<Message>\n\t<incorrect-message-body-here/>\n</Message>";
            // This should throw an exception.

            logger.debug("Sending for process (should result an error)...");
            csi.handleOrder(incorrectXML);
        } catch (Exception e) {
            logger.debug("Got Exception, it's expected: " + e);
        }
    }

    public void testOrderBean() {
    	//if(true) return;
    	
        logger.debug("Test Order Bean");
        try {
        	//Jonas Migration
        	InitialContext ic = getInitialContext();
            JAXBContext ctx = JAXBUtils.getContext();
            Marshaller marshaller = ctx.createMarshaller();
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            unmarshaller.setValidating(true);

            Message message = (Message)unmarshaller.unmarshal(new File("../../../config/test/test-jms-new-manage.xml"));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(message, baos);
			// Jonas Migration
            /*
			TopicConnectionFactory tFactory = (TopicConnectionFactory)new InitialContext().lookup("CsiConnectionFactory");
            Topic topic = (Topic)new InitialContext().lookup("CsiGoldTopic");

            TopicConnection tConnection = tFactory.createTopicConnection();
            TopicSession tSession = tConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            TopicPublisher tPublisher = tSession.createPublisher(topic);
            tConnection.start();
			*/
            QueueConnectionFactory qFactory = (QueueConnectionFactory) ic.lookup("cn=CsiConnectionFactory");
            Queue queue = (Queue) ic.lookup("cn=Gold2CsiQueue");

            QueueConnection qConnection = qFactory.createQueueConnection();
            QueueSession qSession = qConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            QueueSender qsender = qSession.createSender(queue);
            qConnection.start();

            javax.jms.TextMessage msg = qSession.createTextMessage();
            msg.setText(baos.toString());

            logger.debug("Sending order message <change manage order>");
            //tPublisher.publish(msg);
            qsender.send(msg);

            qConnection.close();

            logger.debug("Waiting for 10 sec (yield server backgroud operations)");
            Thread.sleep(10000);

            CsiApiHome home = (CsiApiHome)new InitialContext().lookup("com.equant.csi.ejb.api.CsiApiHome");
            CsiApi csi = home.create();

            Vector v = csi.getServices("2222", SITE_ID, null, null);
            assertNotNull("Result of getServices should not be null.", v);
            assertEquals("Result of getServices should be 1.", v.size(), 1);

            if (v.size()==1) {
                logger.debug("Service is created. Comparing...");
                String serviceName = (String)v.elementAt(0);
                assertEquals("Result of getServices for should be " + SERVICE_NAME, SERVICE_NAME, serviceName);

                String serviceDetails = csi.getServiceDetails("2222", SITE_ID, null, null, serviceName);
                Message messageDB = (Message)unmarshaller.unmarshal(new ByteArrayInputStream(serviceDetails.getBytes()));

                logger.debug("Check if we get correct result.");
                MessageCompare.compare(message, messageDB);
            }
        } catch (Exception e) {
            logger.debug("Got Exception, it's expected: " + e);
            fail("Got exception " + e);
        }
    }
	// Jonas Migration
    public static InitialContext getInitialContext()
		    throws NamingException {
		logger.debug("Entering...");
		Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://10.238.22.42/cn=TestGold,o=CCEAIDev");
        env.put("java.naming.security.principal", "cn=user1,ou=People,o=CCEAIDev");
        env.put("java.naming.security.credentials", "toto");
		return new InitialContext(env);
		}

    public void testOrderProcess() {
    	//if(true) return;
    	
        logger.debug("Test Order Process");
        String[] testFile = new String[]{
            "fr2-new-cancel.xml",
            "fr2-new-manage.xml",
            "fr2-new-mpi.xml",
            "fr2-new-release.xml",
            "fr2-new-rollback.xml",
            "fr2-new-uncancel.xml",
            "fr2-change-cancel.xml",
            "fr2-change-manage.xml",
            "fr2-change-mpi.xml",
            "fr2-change-release.xml",
            "fr2-change-release2.xml",
            "fr2-change-rollback.xml",
            "fr2-change-uncancel.xml",
            "fr2-disconnect-manage.xml",
            "fr2-disconnect-release.xml"
        };
        try {
            JAXBContext ctx = JAXBUtils.getContext();
            Marshaller marshaller = ctx.createMarshaller();
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            unmarshaller.setValidating(true);
            // Jonas Migration
            InitialContext ic = getInitialContext();
            QueueConnectionFactory qFactory = (QueueConnectionFactory) ic.lookup("cn=CsiConnectionFactory");
            Queue queue = (Queue) ic.lookup("cn=Gold2CsiQueue");
            /*
			TopicConnectionFactory tFactory = (TopicConnectionFactory)new InitialContext().lookup("CsiConnectionFactory");
            Topic topic = (Topic)new InitialContext().lookup("CsiGoldTopic");
			*/

            CsiApiHome home = (CsiApiHome)new InitialContext().lookup("com.equant.csi.ejb.api.CsiApiHome");

            for (int i=0; i<testFile.length; i++) {
                /*
				TopicConnection tConnection = tFactory.createTopicConnection();
                TopicSession tSession = tConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

                TopicPublisher tPublisher = tSession.createPublisher(topic);
                tConnection.start();
				*/
                QueueConnection qConnection = qFactory.createQueueConnection();
                QueueSession qSession = qConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

                QueueSender qsender = qSession.createSender(queue);
                qConnection.start();

                Message message = (Message)unmarshaller.unmarshal(new File("../../../config/interfacefiles/"+testFile[i]));

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                marshaller.marshal(message, baos);

                javax.jms.TextMessage msg = qSession.createTextMessage();
                msg.setText(baos.toString());

                VersionType version = (VersionType)message.getVersion().get(0);
                logger.debug("Sending order message <"+version.getOrdertype()+" "+version.getOrderstatus()+">");
                /*
				tPublisher.publish(msg);

                tConnection.close();
				*/
                qsender.send(msg);

                qConnection.close();

                logger.debug("Waiting for 5 sec (yield server backgroud operations)");
                Thread.sleep(5000);

                CsiApi csi = home.create();

                Vector v = csi.getServices(version.getCustomerId(), version.getSiteId(), null, null);
                assertNotNull("Result of getServices should not be null.", v);

                if (v.size()==1) {
                    logger.debug("Service is created. Getting current version...");
                    String serviceName = (String)v.elementAt(0);

                    String serviceDetails = csi.getServiceDetails(version.getCustomerId(), version.getSiteId(), null, null, serviceName);
                    File result = new File("result-" + testFile[i]);
                    FileWriter writer = new FileWriter(result);
                    writer.write(serviceDetails);
                    writer.flush();

                    Message messageDB = (Message)unmarshaller.unmarshal(new ByteArrayInputStream(serviceDetails.getBytes()));

                    logger.debug("Result saved into "+result.getAbsolutePath());
                    MessageCompare.compare(message, messageDB);
                }
            }
        } catch (Exception e) {
            logger.debug("Got Exception, it's expected: " + e);
            fail("Got exception " + e);
        }
    }

    public void testLeak1() throws Exception {
        try {
        	long max = Long.MIN_VALUE, min = Long.MAX_VALUE, maxLocal = Long.MIN_VALUE, minLocal = Long.MAX_VALUE, total = 0;
        	// disabling loggin for Log4j, so it does not print a lot.
        	disableLoggers();
            CsiApiHome home = (CsiApiHome)new InitialContext().lookup("com.equant.csi.ejb.api.CsiApiHome");
            CsiApi csi = home.create();
            
            for(int i = 0;i < ITERATOR; i++) {
                long start = System.currentTimeMillis();
                Vector v = csi.getServices("2222", SITE_ID, null, null);
                long diff = System.currentTimeMillis() - start; 
                if(maxLocal < diff) maxLocal = diff; 
                if(minLocal > diff) minLocal = diff;
                total+=diff;
                
                if (i > 0 && i % 1000 == 0) {
                    String si = "" + i;
                    while (si.length() < 7) si = " " + si;
                    logger.debug(
                    		"Iteration " + si + ": used " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) +
							": CsiApi.getServices(...): MAX: " + maxLocal + " msec, MIN: " + minLocal + " msec, AVERAGE: " + (total/(i+1)) + " msec" 
                    );
                    if(max<maxLocal) max=maxLocal;
                    if(min>minLocal) min=minLocal;
                    maxLocal = Long.MIN_VALUE;
                    minLocal = Long.MAX_VALUE;
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

    public void testLeak2() throws Exception {
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
                long start = System.currentTimeMillis();
                String messageText = csi.getServiceDetails("2222", SITE_ID, null, null, SERVICE_NAME);
                if(messageText!=null) {
                	Message messageDB = (Message)unmarshaller.unmarshal(new ByteArrayInputStream(messageText.getBytes()));
                }
                long diff = System.currentTimeMillis() - start; 
                if(maxLocal < diff) maxLocal = diff; 
                if(minLocal > diff) minLocal = diff;
                total+=diff;

                if (i > 0 && i % 1000 == 0) {
                    String si = "" + i;
                    while (si.length() < 7) si = " " + si;
                    logger.debug(
                    		"Iteration " + si + ": used " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) +
							": CsiApi.getServiceDetails(...): MAX: " + maxLocal + " msec, MIN: " + minLocal + " msec, AVERAGE: " + (total/(i+1)) + " msec" 
					);
                    if(max<maxLocal) max=maxLocal;
                    if(min>minLocal) min=minLocal;
                    maxLocal = Long.MIN_VALUE;
                    minLocal = Long.MAX_VALUE;
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
        logger.debug("Stop jonas  Server");
        try {
            //AdminServer.getAdminServer().getServer().stop();
        	 //  weblogic.Server.main(new String[]{});
            // code for starting jonas server 
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
