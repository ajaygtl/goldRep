package com.equant.csi.weblogic;

/** New class added as a substitue of weblogic start up class which will take input from web.xml "file name" and instantiate TimerMBeanScheduler */

import java.text.ParseException;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import org.apache.log4j.Category;

import com.equant.csi.common.JMSMessageConsumer;
import com.equant.csi.utilities.LoggerFactory;
public class MBeanRegistrationListener implements ServletContextListener{
    
	/** Initializing the logger. */
    private static final Category logger = LoggerFactory.getInstance("MBeanRegistrationListener");
    /** start up class. */
    TimerMBeanScheduler timerMBeanScheduler;
    /** listener as replacement of MDB added as part of jonas migration project */
    JMSMessageConsumer messageConsumer;
	
	public void contextInitialized(ServletContextEvent arg0)  {
		
	  logger.debug("####contextInitialized####");
	  if(arg0.getServletContext().getInitParameter("filename")==null || (arg0.getServletContext().getInitParameter("filename").equalsIgnoreCase("")))
		  {
		  
		  logger.debug("######context init param for listener is null or empty #####");
		  throw new IllegalArgumentException("context init param for listener is null or empty ");
		  
		  }else{
		String fileNames =
		arg0.getServletContext().getInitParameter("filename");
		timerMBeanScheduler=new TimerMBeanScheduler(fileNames);
		// commented listener as we are using mdb now  
		/*try {
			// Instantiating listener 
			 messageConsumer=new JMSMessageConsumer();
		} catch (NamingException e) {
			logger.debug("NamingException while instansiating JMSMessageConsumer "+e);
			logger.error("NamingException while instansiating JMSMessageConsumer "+e);
		} catch (JMSException e) {
			logger.debug("JMSException while instansiating JMSMessageConsumer "+e);
			logger.error("JMSException while instansiating JMSMessageConsumer "+e);
		}*/
		}
			}
	

	public void contextDestroyed(ServletContextEvent arg0) {
		logger.debug("contextDestroyed");
		
		try {
			timerMBeanScheduler.stopTimeServices();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
			}
	

}
