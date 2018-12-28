package com.equant.csi.client;

/* Adding one more comment to check if integration on Jenkins working.
 * Hello
 * $Author: sergiy.meshkov $ $Date: 2002/11/19 14:38:43 $
 *
 *
 * Created on Nov 01, 2001, 9:15 AM
 */
import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 *
 * This program establishes a connection to and receive
 * messages from Error topic. The classes in this package operate
 * on the same JMS topic. Run the classes together to observe messages
 * being sent and received. This class is used to receive messages from
 * the topic.
 * @author kiran Reddy
 */
public class ErrorReceive implements MessageListener {
    private static final Category logger = Category.getInstance(ErrorReceive.class.getName());
    /**
     * Defines the JNDI context factory.
     */


  //  public final static String JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";
    // changed as part of csi jonas migration
    public final static String JNDI_FACTORY = "org.ow2.carol.jndi.spi.IRMIContextWrapperFactory";
    /**
     * Defines the JMS connection factory for the topic.
     */
   public final static String JMS_FACTORY = "CsiConnectionFactory";
    /**
     * Defines the topic.
     */
 // changed for weblogic jms to eai migration
 //   public final static String TOPIC = "CsiGoldErrorTopic";

 //   private TopicConnectionFactory tconFactory;
 //   private TopicConnection tcon;
 //   private TopicSession tsession;
 //   private TopicSubscriber tsubscriber;
 //   private Topic topic;
    public final static String QUEUE = "CsiGoldErrorQueue";
    private ConnectionFactory conFactory;
    private Connection con;
    private Session session;
    private MessageConsumer consumer;
    private Queue queue;
    private boolean quit = false;
    //private ErrorListener errorListener = new ErrorListener ();
    private ErrorFrame errorFrame;

    public ErrorReceive() {
        errorFrame = new ErrorFrame();
        errorFrame.show();
    }

    /**
     * Message listener interface.
     * @param msg message
     *
     */
    // MessageListener interface
    public void onMessage(Message msg) {
        try {
            String msgText;
            String msg11 = "";

            if (msg instanceof TextMessage) {
                msgText = ((TextMessage) msg).getText();
                msg11 = ((TextMessage) msg).getStringProperty("author");
                errorFrame.setXml(msgText);
                errorFrame.setLabelText();
            } else {
                msgText = msg.toString();
            }

            logger.debug("JMS eMessage Received: " + msgText + "Error is " + msg11);

            if (msgText.equalsIgnoreCase("quit")) {
                synchronized (this) {
                    quit = true;
                    this.notifyAll(); // Notify main thread to quit
                }
            }
        } catch (JMSException jmse) {
            logger.error(jmse.toString(), jmse);
        }
    }

    /**
     * Creates all the necessary objects for sending
     * messages to  ErrorTopic.
     *
     * @param  ctx  JNDI initial context
     * @param topicName name of topic
     * @exception NamingException if problem occurred with JNDI context interface
     * @exception JMSException if JMS fails to initialize due to internal error
     */
    public void init(Context ctx, String queueName) throws NamingException, JMSException {
    	// changed for weblogic to eai and topic to queue migration
        conFactory = (ConnectionFactory) ctx.lookup(JMS_FACTORY);
        con = conFactory.createConnection();
        session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        queue = (Queue) ctx.lookup(queueName);
        //tsubscriber=tsession.createDurableSubscriber(topic,"kpr") ;
        consumer = session.createConsumer(queue);
        consumer.setMessageListener(this);
        con.start();
    }

    /**
     * Closes JMS objects.
     *
     * @exception JMSException if JMS fails to close objects due to internal error
     */
    public void close() throws JMSException {
        consumer.close();
        session.close();
        con.close();
    }

    /**
     * main() method.
     *
     * @param args WebLogic Server URL
     * @exception Exception if execution fails
     */

    public static void main(String[] args) throws Exception {
        PropertyConfigurator.configure("config/Logger.conf");
        if (args.length != 1) {
            logger.debug("Usage: java examples.jms.topic.ErrorReceive WebLogicURL");
            return;
        }
        InitialContext ic = getInitialContext(args[0]);
        ErrorReceive tr = new ErrorReceive();
        tr.init(ic, QUEUE);

        logger.debug("JMS Ready To Receive Messages (To quit, send a \"quit\" message).");

        // Wait until a "quit" message has been received.
        synchronized (tr) {
            while (!tr.quit) {
                try {
                    tr.wait();
                } catch (InterruptedException ie) {
                    logger.error(ie.toString(), ie);
                }
            }
        }
        tr.close();
    }

    private static InitialContext getInitialContext(String url) throws NamingException {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        env.put(Context.URL_PKG_PREFIXES,"org.ow2.jonas.naming" );
        env.put(Context.PROVIDER_URL, url);
       // env.put("weblogic.jndi.createIntermediateContexts", "true");
        return new InitialContext(env);
    }

}



