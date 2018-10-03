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

import javax.management.InstanceNotFoundException;

import org.apache.log4j.Category;

import javax.management.timer.Timer;

import com.equant.csi.utilities.LoggerFactory;

/**
 * @author Vasyl Rublyov <vasyl.rublyov@equant.com>
 * @version 1.0 - 25.03.2005
 *
 * TODO
 */
class NotificationData {
    /** Initializing the logger. */
    private static final Category logger = LoggerFactory.getInstance("timeservice.NotificationData");

    public String name;
    public Integer id;
    public Timer timer;
    
    /**
     * NotificationData
     * 
     * @param name
     * @param id
     * @param timer
     * @param args
     */
    public NotificationData(String name, Integer id, Timer timer) {
        this.name = name;
        this.id = id;
        this.timer = timer;
    }
    
    /**
     * 
     */
    public String toString() {
        return name;
    }
    
    /**
     * 
     *
     */
    public void start() {
        if(timer!=null) {
            logger.debug("-= Starting " + name + " time service.");
            timer.start();
            logger.debug("#####Timer service is active or not #####"+timer.isActive()) ;
        }
    }

    /**
     * 
     *
     */
    public void stop() {
        if(timer!=null) {
            timer.stop();
            if(id!=null) {
                try {
                    timer.removeNotification(id);
                } catch(InstanceNotFoundException e) {
                    logger.error("Unable remove notifications from " + name + " time service", e);
                }
            }
        }
        name = null;
        id = null;
        timer = null;
   }
}
