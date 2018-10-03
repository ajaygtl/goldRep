/*
 * User: pathurik
 * Date: Oct 4, 2002
 * Time: 3:50:02 PM
 * Property of Equant.
 */
package com.equant.csi.test;

import java.io.ByteArrayOutputStream;
import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.xml.bind.JAXBContext;

import org.apache.log4j.Category;

import com.equant.csi.ejb.api.CsiApi;
import com.equant.csi.ejb.api.CsiApiHome;
import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.utilities.JAXBUtils;
import com.equant.csi.utilities.LoggerFactory;

public class ConnectionFactory {
    private static final Category logger = LoggerFactory.getInstance(ConnectionFactory.class.getName());

    private CsiApi remote;

    /**
     * Defines the JNDI context factory.
     */
    // jonas/jms migration
    //private final static String JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";
   // private final static String JNDI_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    private final static String JNDI_FACTORY = "org.ow2.carol.jndi.spi.IRMIContextWrapperFactory";

    /**
     * Defines the JMS connection factory.
     */
    // jonas/jms migration    
    //private final static String JMS_FACTORY = "CsiConnectionFactory";
    private final static String JMS_FACTORY = "CsiConnectionFactory";

    /**
     * Defines the topic.
     */
    // jonas/jms migration
    //private static final String TOPIC = "CsiGoldTopic";
    private static final String QUEUE = "Gold2CsiQueue";

    /*
	private static final String url = "t3://localhost:7001";

    private TopicConnectionFactory tconFactory;
    private TopicConnection tcon;
    private TopicSession tsession;
    private TopicPublisher tpublisher;
    private Topic topic;
	*/
    private TextMessage msg;
    
    private QueueConnectionFactory qconFactory;
    private QueueConnection qcon;
    private QueueSession qsession;
    private QueueSender qsender;
    private Queue queue;

    private int counter;

    public ConnectionFactory() {
        try {
            /*
			Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
            env.put(Context.PROVIDER_URL, this.url);
            env.put("weblogic.jndi.createIntermediateContexts", "true");
            InitialContext initialContext = new InitialContext(env);
			*/
		//	InitialContext eaiIc = getEaiInitialContext();
            InitialContext ic = getInitialContext();
            setupJmsConnection(ic);
            setupRmiConnection(ic);
            
        } catch (NamingException e) {
            logger.error(e.toString(), e);
        }
    }
    /**
     * Get EAI initial context.
     * @return
     * @throws NamingException
     */
   /* public static InitialContext getEaiInitialContext()
            throws NamingException {
        logger.debug("Entering...");
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        env.put(Context.PROVIDER_URL, "ldap://10.238.22.42/cn=TestGold,o=CCEAIDev");
        env.put("java.naming.security.principal", "cn=user1,ou=People,o=CCEAIDev");
        env.put("java.naming.security.credentials", "toto");
        logger.debug("Leaving...");
        return new InitialContext(env);
    }*/
	
	/**
     * Get initial context.
     * @return
     * @throws NamingException
     */
    public static InitialContext getInitialContext() throws NamingException 
    {
		logger.debug("Entering...");
		Hashtable<String, String> env1 = new Hashtable<String, String>();
		env1.put(Context.INITIAL_CONTEXT_FACTORY, "org.ow2.carol.jndi.spi.IRMIContextWrapperFactory");
		env1.put(Context.URL_PKG_PREFIXES,"org.ow2.jonas.naming" );
		env1.put(Context.PROVIDER_URL, "rmi://localhost:1099");
		logger.debug("Leaving...");
		return new InitialContext(env1);
		}

    public ConnectionFactory(int startCounter) {
        this();
        this.counter = startCounter;
    }

    private void setupJmsConnection(InitialContext initialContext) {
        try {
        	// jonas/jms migration
        	/*
			tconFactory = (TopicConnectionFactory) initialContext.lookup(JMS_FACTORY);
            tcon = tconFactory.createTopicConnection();
            tsession = tcon.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = (Topic) initialContext.lookup(TOPIC);
            tpublisher = tsession.createPublisher(topic);
            msg = tsession.createTextMessage();
            tcon.start();
			*/
            qconFactory = (QueueConnectionFactory) initialContext.lookup(JMS_FACTORY);
            qcon = qconFactory.createQueueConnection();
            qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            queue = (Queue) initialContext.lookup(QUEUE);
            qsender = qsession.createSender(queue);
            msg = qsession.createTextMessage();
            qcon.start();
        } catch (NamingException e) {
            logger.error(e.toString(), e);
        } catch (JMSException e) {
            logger.error(e.toString(), e);
        }
    }

    private void setupRmiConnection(InitialContext initialContext) {
        logger.debug("inside the lookAtBean() method ..... ");
        try {
        	// csi jonas migration 
            //CsiApiHome home = (CsiApiHome) initialContext.lookup("com.equant.csi.ejb.api.CsiApiHome");
        	 CsiApiHome home = (CsiApiHome) PortableRemoteObject.narrow(initialContext.lookup("com.equant.csi.ejb.api.CsiApiHome"), CsiApiHome.class);
            this.remote = home.create();
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
    }

    public CsiApi getCsRemote() {
        return remote;
    }

    public void sendMessage(Message message) {
        try {
            logger.debug("Trying for the |" + counter + "|");

            ((VersionType) message.getVersion().get(0)).setSiteId("MegaPolis_" + counter);
            counter++;

            JAXBContext ctx = JAXBUtils.getContext();
            boolean isValid = ctx.createValidator().validate(message);
            if (isValid) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ctx.createMarshaller().marshal(message, out);

                msg.setText(out.toString());
                // jonas/jms migration
                //tpublisher.publish(msg);
                qsender.send(msg);
            } else {
                logger.error("Validation of 'message' failed.");
            }
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
    }
}