package com.equant.csi.common;

import java.io.ByteArrayOutputStream;
import java.rmi.RemoteException;
import java.util.Hashtable;
import javax.ejb.CreateException;
import javax.jms.BytesMessage;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.apache.log4j.Category;

import com.equant.csi.ejb.api.CsiApi;
import com.equant.csi.ejb.api.CsiApiHome;
import com.equant.csi.utilities.LoggerFactory;
import com.equant.csi.common.CSIConstants;
import com.equant.csi.ejb.api.CsiApi;
import com.equant.csi.ejb.api.CsiApiHome;

/**
 *  JMS message consumer
 * @author  Devendra Gupta
 * @see MessageListener
 */
public class JMSMessageConsumer implements MessageListener, ExceptionListener{

	private static final Category logger = LoggerFactory.getInstance(JMSMessageConsumer.class.getName());
	private final static String JNDI_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
	private final static String JMS_FACTORY = "cn=CsiConnectionFactory";
	private static final String QUEUE = "cn=Gold2CsiQueue";
    public static final int BYTES_BUFFER_SIZE  = 2048;
    private InitialContext ctx = null;
	
    /** The remote interface of the CsiApiEJB. */
    private transient CsiApi csi;
    
    protected String m_messageType = null;
    protected String m_textMessage = null;
    // dev 
    protected QueueConnectionFactory qconFactory;
    protected QueueConnection qcon;
    protected QueueSession qsession;
    protected Queue queue;
    protected QueueReceiver qreceiver;
    //
    /**
     * Constructor. Establishes the JMS subscriber.
     * @param env Properties
     * @param topicName  Topic name
     * @throws NamingException  if an exception occurs
     * @throws JMSException  if an exception occurs
     */
    public JMSMessageConsumer() throws NamingException, JMSException {
        
        // Obtains a JNDI connection
    	ctx = getEaiInitialContext();
    	logger.debug("ctx..."+ctx);
    	// Looks up a JMS connection factory
    	qconFactory = (QueueConnectionFactory) ctx.lookup(JMS_FACTORY);
    	logger.debug("qconFactory..."+qconFactory);
    	// Creates a JMS connection
    	qcon = qconFactory.createQueueConnection();
    	logger.debug("qcon..."+qcon);
    	// making the session transacted
    	qsession = qcon.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
    	logger.debug("qsession..."+qsession);
    	// Looks up a JMS topic
    	queue = (Queue) ctx.lookup(QUEUE);
    	logger.debug("queue..."+queue);
    	qreceiver = qsession.createReceiver(queue);
    	logger.debug("qreceiver..."+qreceiver);
    	// Sets a JMS message listener
    	qreceiver.setMessageListener(this);
    	// set an asynchronous exception listener on the connection
    	qcon.setExceptionListener(this);
    	// Starts the JMS connection; allows messages to be delivered
    	qcon.start();
    	logger.debug("qcon.started ...");
    	
    }
    
    /**
     * Get Initial context.
     * @return
     * @throws NamingException
     */
    public static InitialContext getEaiInitialContext()
            throws NamingException {
        logger.debug("Entering...");
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        env.put(Context.PROVIDER_URL, "ldap://10.238.22.42/cn=TestGold,o=CCEAIDev");
        env.put("java.naming.security.principal", "cn=user1,ou=People,o=CCEAIDev");
        env.put("java.naming.security.credentials", "toto");
        logger.debug("Leaving...");
        return new InitialContext(env);
    }

    
    /**
     * Creates and returns an instance of CsiApi EJB.
     *
     * @return CsiApi the instance of CsiApi EJB or null if exception happened.
     * @throws NamingException thrown for any error during retrieving
     *                         the JNDI initial context
     * @throws CreateException if ejb could not be created.
     * @throws RemoteException the communication-related exceptions that may occur
     *                         during the execution of a remote method call
     */
    private CsiApi getCSI() throws NamingException, RemoteException, CreateException {
    	logger.debug("Entering getCSI()...");
        if (csi == null) {
           
           InitialContext ic = new InitialContext();
           logger.debug("getCSI() ic..."+ic);
           
	     /*   Context ctx = (Context)new InitialContext().lookup("java:comp/env");
	       listContext(ctx, "");
	        *** Recursively exhaust the JNDI tree*//*
	      NamingEnumeration list = ic.listBindings("");
	        while (list.hasMore()) {   
	        Binding item = (Binding) list.next(); 
	        String className = item.getClassName();
	      String name = item.getName();   
	       System.out.println("##############"+name);
	       logger.log(Level.INFO, indent + className + " " + name); 
	       Object o = item.getObject();      
	        if (o instanceof javax.naming.Context) { 
	        listContext((Context) o, indent + " ");  
	        }  
          Object obj = ic.lookup("CsiApi");
            System.out.println("######################obj#####################"+obj);
         Object obj1 = PortableRemoteObject.narrow(obj,CsiApiHome.class);
          System.out.println("######################obj1#####################"+obj1);
          CsiApiHome home =(CsiApiHome)obj1;
         System.out.println("######################home#####################"+home);

           CsiApiHome home = (CsiApiHome) PortableRemoteObject.narrow(ic.lookup("java:/comp/env/ejb/CsiApi"), CsiApiHome.class);
*/            CsiApiHome home = (CsiApiHome) PortableRemoteObject.narrow(ic.lookup("com.equant.csi.ejb.api.CsiApiHome"), CsiApiHome.class);
            logger.debug("getCSI() home..."+home);
            csi = home.create();
            logger.debug("getCSI() csi..."+csi);
            
        }
        return csi;
    }
    
    
    /**
     * Get initial context.
     * @return
     * @throws NamingException
     */
  /*  public static InitialContext getInitialContext() throws NamingException 
    {
		logger.debug("Entering...");
		Hashtable<String, String> env1 = new Hashtable<String, String>();
		env1.put(Context.INITIAL_CONTEXT_FACTORY, "org.ow2.carol.jndi.spi.IRMIContextWrapperFactory");
		env1.put(Context.URL_PKG_PREFIXES,"org.ow2.jonas.lib.naming" );
		env1.put(Context.PROVIDER_URL, "rmi://localhost:1099");
		logger.debug("Leaving...");
		return new InitialContext(env1);
		}*/
    /**
     * Creates all the necessary objects for receiving
     * messages from a JMS Queue.
     * This listens to the messages on the predefined Queue to
     * invoke necessary action.
     * @param msg JNDI <code>javax.jms.Message</code>
     */
    public void onMessage(Message msg) {
    	logger.debug("onMessage..." );
    	
        String xmlMessage = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Message received (ID: " + msg.getJMSMessageID() + " send date: " + new java.util.Date(msg.getJMSTimestamp()) + ")");
            }

            if(msg==null) {
                // DO NOT rollback this transaction. Failure is fatal, this message can not be processed.
            } else if(msg instanceof TextMessage) {
                xmlMessage = ((TextMessage)msg).getText();
            } else if(msg instanceof BytesMessage) {
                // Got BytesMessage
                ByteArrayOutputStream byteInputStream = new ByteArrayOutputStream();
                byte buf[] = new byte[BYTES_BUFFER_SIZE];
                int len;
                while((len=((BytesMessage)msg).readBytes(buf, BYTES_BUFFER_SIZE-1))!=-1) {
                    byteInputStream.write(buf, 0, len);
                }
                byteInputStream.flush();
                xmlMessage = byteInputStream.toString();
            }
            System.out.println("receive: msg#" + ((TextMessage)msg).getText());

            if (!(xmlMessage == null || xmlMessage.length() == 0)) {
                getCSI().handleOrder(xmlMessage);
            }
            // else DO NOT rollback this transaction. Failure is fatal, this message can not be processed.
            msg.acknowledge();
        } catch(Exception e) {
        	logger.error("onMessage - got exception", e);
        	logger.debug("onMessage - got exception"+e);

            try {
            	qsession.rollback(); /* rollback the transation back to JMS */
            } catch(JMSException e1) {
            	logger.error("onMessage - exception when rolled back", e1);
            	logger.debug("onMessage - exception when rolled back", e1);
            	
            }
        } 
 
    }
   
    /**
    This method is called asynchronously by JMS when some error occurs.
    When using an asynchronous message listener it is recommended to use
    an exception listener also since JMS have no way to report errors
    otherwise.
    @param exception A JMS exception.
  */
	 public void onException(JMSException exception)
		 {
		    logger.fatal("An error occurred: ", exception);
		    logger.debug("An error occurred: ", exception);
		    
		 }

}
