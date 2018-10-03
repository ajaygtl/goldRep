/*
 * $Author: sergiy.meshkov $ $Date: 2002/11/19 14:51:22 $
 * $Revision: Kiran P Reddy
 * Property of Equant
 */

package com.equant.csi.client;

import com.equant.csi.utilities.LoggerFactory;
import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Hashtable;

/**
 * This program is for sending messages to the predefined
 * JMS topic. The classes in this package operate on the same topic. Run
 * the classes together with OrderMDBean to observe message being sent and received.  *
 * @author KiranReddy, OrderSystemsDevelopment group
 */
public class OrderClient {
    private static final Category logger = LoggerFactory.getInstance(OrderClient.class.getName());

    /**
     * Defines the JNDI context factory.
     */
    //jonas/jms migration
    //public final static String JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";
    public final static String JNDI_FACTORY = "org.ow2.carol.jndi.spi.IRMIContextWrapperFactory";
    /**
     * Defines the JMS connection factory.
     */
    //jonas/jms migration
    //public final static String JMS_FACTORY = "CsiConnectionFactory";
    public final static String JMS_FACTORY = "CsiConnectionFactory";
    /**
     * Defines the topic.
     */
    //jonas/jms migration
    //public final static String TOPIC = "CsiGoldTopic";
    public final static String QUEUE = "Gold2CsiQueue";
    private static String url = null;
   /* 
    protected TopicConnectionFactory tconFactory;
    protected TopicConnection tcon;
    protected TopicSession tsession;
    protected TopicPublisher tpublisher;
    protected Topic topic;*/
    //jonas/jms migration
    protected QueueConnectionFactory qconFactory;
    protected QueueConnection qcon;
    protected QueueSession qsession;
    protected QueueSender qsender;
    protected Queue queue;
    protected TextMessage msg;
    private static String filename = null;

    /**
     * Creates all the necessary objects for sending
     * messages to a JMS Topic.
     *
     * @param ctx JNDI initial context
     * @param queueName name of queue
     * @exception NamingException if problem occurred with the JNDI context interface
     * @exception JMSException if JMS fails to initialize due to internal error
     *
     */
    public void init(InitialContext ctx, String queueName)
            throws NamingException, JMSException {
        logger.debug("Entering");
        //jonas/jms migration
        /*
		tconFactory = (TopicConnectionFactory) ctx.lookup(JMS_FACTORY);
        tcon = tconFactory.createTopicConnection();
        tsession = tcon.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        topic = (Topic) ctx.lookup(topicName);
        tpublisher = tsession.createPublisher(topic);
        msg = tsession.createTextMessage();
        tcon.start();
		*/
        qconFactory = (QueueConnectionFactory) ctx.lookup(JMS_FACTORY);
        qcon = qconFactory.createQueueConnection();
        qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        queue = (Queue) ctx.lookup(queueName);
        qsender = qsession.createSender(queue);
        msg = qsession.createTextMessage();
        qcon.start();
        logger.debug("leaving");
    }

    /**
     * reads  a message from the file .
     * @return XMLString
     * @throws Exception
     */
    public String readTry() throws Exception {
        logger.debug("Entering...");
        System.out.println("PWD |" + System.getProperty("user.dir") + "|");
        logger.debug("Reading file in weblogic");
        FileReader fr = new FileReader("weblogic/" + filename);
        //FileReader fr = new FileReader("http://localhost:7001/Message.xml");
        BufferedReader br = new BufferedReader(fr);
        String s = "";
        String kir = "";
        while ((s = br.readLine()) != null) {
            kir = kir + "" + s;
        }
        System.out.println(" Total String is  " + kir);
        logger.debug("Leaving...");
        return kir;
    }


    /**
     * Sends a message to a JMS topic.
     * @param message
     * @throws JMSException
     */
    public void send(String message)
            throws JMSException {
        msg.setText(message);
        // jonas/jms migration
        //tpublisher.publish(msg);
        qsender.send(msg);
    }

    /**
     * Closes JMS objects.
     * @exception JMSException if JMS fails to close objects due to internal error
     */
    public void close() throws JMSException {
        logger.debug("Entering");
        // jonas/jms migration
        /*
		tpublisher.close();
        tsession.close();
        tcon.close();
		*/
        qsender.close();
        qsession.close();
        qcon.close();
        logger.debug("leaving");
    }

    /**
     * main() method.
     * @param args WebLogic Server URL
     * @exception Exception if operation fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("PWD |" + System.getProperty("user.dir") + "|");
        PropertyConfigurator.configure("config/Logger.conf.xml");
        logger.debug("Entering...");
        logger.debug("Size of Arguments |" + args.length + "|");

        if ((args.length == 1) && (args[0].endsWith("xml"))) {
            //url = "t3://localhost:7001";
			url = "rmi://localhost:1099";
            filename = args[0];
            logger.info("Assuming that Jonas is Running on localhost.");
            logger.info("Please specify the Jonas Url if not running on localhost");
        } else if ((args.length == 0) || ((args.length == 1) && (args[0].endsWith("7001")))) {
            logger.info("USAGE: java com.equant.csi.client.OrderClient Jonas Url xmlfilename.");
            logger.info("This file should be in the weblogic sub-directory.");
            System.exit(0);
        } else {
            url = args[0];
            filename = args[1];
            logger.debug("Connecting to Weblogic at URL |" + url + "| with filename |" + filename + "|");
        }


        /*
         for (int i=0;i<2;i++)
         {
             logger.debug("|loop number| i=" + i);
             filename = ((i % 2 == 0) ? "fr2-new-release.xml" : "fr2-new-rollback.xml");
             logger.debug("********************************filename is "+filename );
             url = "t3://localhost:7001";
         */
        InitialContext ic = getInitialContext(url);
        OrderClient ts = new OrderClient();
        ts.init(ic, QUEUE);
        readAndSend(ts);
        ts.close();

//     }
        logger.debug("Leaving...");
    }

    /**
     * Prompts, reads, and sends a message.
     *
     * @param ts OrderClient
     * @exception JMSException if JMS fails due to internal error
     */

    public static void readAndSend(OrderClient ts) throws JMSException {
        logger.debug("Entering...");
        //BufferedReader msgStream = new BufferedReader (new InputStreamReader(System.in));
        String line = null;
        //do {
        System.out.print("Enter message (\"quit\" to quit): ");
        try {
            line = ts.readTry();
        } catch (Exception e) {
            System.out.println("error while calling ts.readTry()" + e);
        }

        //line = msgStream.readLine();

        if (line != null && line.trim().length() != 0) {
            ts.send(line);
            System.out.println("JMS Message Sent: " + line + "\n");
        }
        // } while (line != null && ! line.equalsIgnoreCase("quit"));
        logger.debug("Leaving...");
    }

    /**
     * Get initial JNDI context.
     * @param url
     * @return
     * @throws NamingException
     */
    public static InitialContext getInitialContext(String url)
            throws NamingException {
        logger.debug("Entering...");
        //jonas/jms migration
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        env.put(Context.URL_PKG_PREFIXES,"org.ow2.jonas.naming" );
        env.put(Context.PROVIDER_URL, url);
        //env.put("java.naming.security.principal", "cn=user1,ou=People,o=CCEAIDev");
       // env.put("java.naming.security.credentials", "toto");
        
        //env.put("weblogic.jndi.createIntermediateContexts", "true");
        
        logger.debug("Leaving...");
        return new InitialContext(env);
    }

}

