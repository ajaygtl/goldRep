/*
 * TODO:
 *
 * GOLD: Global Order Lifecycle Delivery
 * (c) Copyright 2005 Equant corporation.
 * 
 * Vasyl Rublyov <vasyl.rublyov@equant.com>
 *
 */
package com.equant.csi.weblogic;

import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.apache.log4j.Category;

import com.equant.csi.utilities.LoggerFactory;
//import com.sun.rsasign.h;

/**
 * @author Vasyl Rublyov <vasyl.rublyov@equant.com>
 * @version 1.0 - 25.03.2005
 *
 * TODO
 */
public abstract class AbstractTimeService implements NotificationListener {
    /** Initializing the logger. */
    private static final Category logger = LoggerFactory.getInstance("timeservice.AbstractTimeService");

    private static boolean isActive = false;

    /* (non-Javadoc)
     * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
     */
    public void handleNotification(Notification notification, Object handback) {
        try {
            if(!allowSimultaneousRun()) {
                synchronized(this) {
                    if(isActive) {
                        logger.debug(getClass().getName() + " is currently running, skip this run");
                        return;
                    }
                    isActive = true;
                }
            }
            Hashtable ht = (Hashtable)handback;
            if(ht!=null && ht.get("TimeService.DaysOfWeek")!=null) {
                BitSet bs = (BitSet)ht.get("TimeService.DaysOfWeek");
                if(!bs.get(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1)) {
                    logger.debug("Skipping because the calendar.");
                } else {
                    logger.debug("Running because the calendar.");
                    run(notification, ht);
                }
            }
        } catch(Throwable t) {
            logger.error("Error occurred during time service run.", t);
        } finally {
            isActive = false;
        }
    }

    public abstract void init(Date kickoff, Long repeat, Hashtable args);
    public abstract void run(Notification notification, Hashtable args);
    public abstract boolean allowSimultaneousRun();
}
