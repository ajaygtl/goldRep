package com.equant.csi.utilities;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Properties;
import java.io.IOException;

import javax.jms.Message;

import org.apache.log4j.Category;

/**
 *
 * @author Kostyantyn Yevenko
 * @version 1.0
 */
public class JMSMessageStore {
    /** Initializing the logger. */
    private static final Category logger = LoggerFactory.getInstance(JMSMessageStore.class.getName());

    private static int TIMER_UPDATE_PERIOD = 60000; //1 minute

    private static Timer timer;
    private static Hashtable messages;
    // When the Message expires
    private static long expirationPeriod;
    // When the Expired Message should be removed;
    private static long expirationPeriod2;
    private static int maxCount;

    static {
        try {
            Properties props = PropertiesFileLoader.load("csi.conf");
            String tmp = props.getProperty("com.equant.csi.jms.message.couter.ttl");
            expirationPeriod = Integer.parseInt(tmp) * 60 * 1000; //Conver in Milliseconds

            tmp = props.getProperty("com.equant.csi.jms.message.couter.maxvalue");
            maxCount = Integer.parseInt(tmp);
        } catch (Exception e) {
            logger.error("Can't load properties. Will use defaults.");
            // Use default harcoded values
            expirationPeriod = 4 * 60 * 60 * 1000; //4 hours
            maxCount = 10; //10 messages
        }

        expirationPeriod2 = expirationPeriod*2;

        messages = new Hashtable();
        timer = new Timer(true);
        TimerTask task = new TimerTask() {
            public void run() {
                if (messages.isEmpty())
                    return;

                Iterator keys = messages.keySet().iterator();
                try {
                    long currentTime = System.currentTimeMillis();
                    while (keys.hasNext()) {
                        String key = (String)keys.next();
                        JMSMessageAttrubute attr = (JMSMessageAttrubute)messages.get(key);
                        if (attr.isExpired() && currentTime>(attr.getLastReceivedDate() + expirationPeriod2)) {
                            if (logger.isDebugEnabled())
                                logger.debug("Removing message: " + key);
                            keys.remove();
                        } else if (!attr.isExpired() && currentTime>(attr.getLastReceivedDate() + expirationPeriod)) {
                            if (logger.isDebugEnabled())
                                logger.debug("Set message status to EXPIRED.");
                            attr.setExpired();
                        }
                    }
                } catch (Exception e) { //This is expected exception in some cases.
                    if (logger.isDebugEnabled())
                        logger.debug("The JMS message store was modified concurrently: ", e);
                }
            }
        };
        timer.schedule(task, 1000, TIMER_UPDATE_PERIOD);
    }

    public static boolean isMaxCountExceededOrExpired(Message message) {
        //NOTE (KY): If time interval between JMS calls greater than expirationTime*2
        //           interval, then it falls into indefinite loop !!!
        try {
            String messageId = message.getJMSMessageID();
            if (message!=null && messageId!=null) {
                JMSMessageAttrubute attr = (JMSMessageAttrubute)messages.get(messageId);

                //If container do not provide the send time, we take the time of
                //method call.
                long timestamp = message.getJMSTimestamp();
                if (timestamp==0)
                    timestamp = System.currentTimeMillis();

                if (attr==null) {
                    attr = new JMSMessageAttrubute(timestamp);
                    messages.put(messageId, attr);

                    if (logger.isDebugEnabled())
                        logger.debug("New counter created for message: " + messageId);
                } else if (!attr.isExpired()) {
                    synchronized (attr) {
                        attr.increaseCount(timestamp);
                    }
                    if (logger.isDebugEnabled())
                        logger.debug("Updated counter to: " + attr.getCount() + " for message: " + messageId);
                }

                if (attr.isExpired() || attr.getCount()>maxCount) {
                    Object obj = messages.remove(messageId);
                    obj = null;

                    if (logger.isDebugEnabled())
                        logger.debug("Counter for message: " + messageId + " exceeded maximum value of: " + maxCount + " or expired and was removed.");

                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error ocurred while handling JMS message.", e);
        }

        return false;
    }

//    private static Properties
}

class JMSMessageAttrubute {
    private long lastReceivedDate = 0;
    private int count = 0;
    private boolean isExpired = false;

    public JMSMessageAttrubute(long received) {
        this.lastReceivedDate = received;
    }

    public long getLastReceivedDate() {
        return lastReceivedDate;
    }
    public void setLastReceivedDate(long lastReceivedDate) {
        this.lastReceivedDate = lastReceivedDate;
    }

    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }

    public boolean isExpired() {
        return isExpired;
    }
    public void setExpired() {
        isExpired = true;
    }

    public void increaseCount(long newReceived) {
        this.lastReceivedDate = newReceived;
        this.count++;
    }
}