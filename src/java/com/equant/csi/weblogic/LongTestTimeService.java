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

import java.util.Date;
import java.util.Hashtable;

import javax.management.Notification;

import org.apache.log4j.Category;

import com.equant.csi.utilities.LoggerFactory;

/**
 * @author Vasyl Rublyov <vasyl.rublyov@equant.com>
 * @version 1.0 - 25.03.2005
 *
 * TODO
 */
public class LongTestTimeService extends AbstractTimeService {
    /** Initializing the logger. */
    private static final Category logger = LoggerFactory.getInstance("timeservice.LongTestTimeService");

    public void init(Date kickoff, Long repeat, Hashtable args) {
        
    }
    
    public boolean allowSimultaneousRun() {
        return false;
    }

    public void run(Notification notification, Hashtable args) {
        logger.info("ACTIVATING LongTestTimeService for 1 day!!!");
        for(int i=0; i<86400;i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.info("FINISHING LongTestTimeService for 1 day!!!");
    }
}
