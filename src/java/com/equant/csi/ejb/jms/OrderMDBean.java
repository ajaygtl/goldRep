/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/ejb/jms/OrderMDBean.java,v 1.18 2002/12/18 15:30:21 vadim.gritsenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.ejb.jms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.apache.log4j.Category;

import com.equant.csi.common.CSIConstants;
import com.equant.csi.ejb.api.CsiApi;
import com.equant.csi.ejb.api.CsiApiHome;
import com.equant.csi.utilities.LoggerFactory;

/**
 * The Order Message Driven Bean. It performs the following actions:
 * <ul>
 * <li>Listen for messages on the <b>CsiGoldTopic</b> topic (see CSIModule-weblogic-ejb-jar.xml)</li>
 * <li>When message arrives, extract XML content from the JMS TextMessage and pass it to CsiApi for processing</li>
 * </ul>
 *
 * @author KiranReddy
 * @author OrderSystemsDevelopment group
 * @author kalyan
 * @author Vadim Gritsenko
 * @version $Revision: 1.18 $
 *
 * @see TextMessage
 * @see CsiApi#handleOrder(java.lang.String)
 */
public class OrderMDBean implements MessageDrivenBean, MessageListener, CSIConstants {

    /** Initializing the logger. */
    private static final Category logger = LoggerFactory.getInstance(OrderMDBean.class.getName());

    /** The remote interface of the CsiApiEJB. */
    private transient CsiApi csi;

    private MessageDrivenContext m_context = null;
    /**
     * Sets the context of the message driven bean.
     *
     * @param context the context to set.
     */
    public void setMessageDrivenContext(MessageDrivenContext context) {
        m_context = context;
    }

    /**
     * Defines bean instance creation sequence in the Container.
     * EJB life cycle starts here.
     *
     * @throws EJBException    if ejb could not be created.
     * @throws CreateException if ejb could not be created.
     */
    public void ejbCreate() throws EJBException, CreateException {
    }

    /**
     * Defines bean instance removal from the Container.
     *
     * @throws EJBException if ejb could not be removed.
     */
    public void ejbRemove() throws EJBException {
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
        if (csi == null) {
            InitialContext ic = new InitialContext();
          //  CsiApiHome home = (CsiApiHome) PortableRemoteObject.narrow(ic.lookup("java:/comp/env/ejb/CsiApi"), CsiApiHome.class);
            CsiApiHome home = (CsiApiHome) PortableRemoteObject.narrow(ic.lookup("com.equant.csi.ejb.api.CsiApiHome"), CsiApiHome.class);
            csi = home.create();
        }
        return csi;
    }

    /**
     * Creates all the necessary objects for receiving
     * messages from a JMS Topic.
     * This listens to the messages on the predefined topic to
     * invoke necessary action.
     *
     * @param msg JNDI <code>javax.jms.Message</code>
     */
    public void onMessage(Message msg) {
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

            if (!(xmlMessage == null || xmlMessage.length() == 0)) {
                getCSI().handleOrder(xmlMessage);
            }
            // else DO NOT rollback this transaction. Failure is fatal, this message can not be processed.
            msg.acknowledge();
        } catch (JMSException e) {
            m_context.setRollbackOnly();
            logger.fatal("Got JMSException in OrerMDBean", e);
        } catch (IOException e) {
            m_context.setRollbackOnly();
            logger.fatal("Got IOException in OrerMDBean", e);
        } catch (NamingException e) {
            m_context.setRollbackOnly();
            logger.fatal("Got NamingException in OrerMDBean", e);
        } catch (CreateException e) {
            m_context.setRollbackOnly();
            logger.fatal("Got CreateException in OrerMDBean", e);
        }
/*
        } catch(Throwable t) {
            logger.fatal("Unhandeled exception occured... PLEASE FIX THIS", t);
        }
*/
    }
}