/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/utilities/LoggerFactory.java,v 1.10 2002/12/07 00:56:46 constantine.evenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.utilities;

import java.io.InputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Initializes the PropertyConfigurator.
 * The property configurator reads the config file for the
 * debug level, file to log to etc.
 *
 * @author kalyan
 * @author kpathuri
 * @author Vadim Gritsenko
 * @version $Revision: 1.10 $
 */
public class LoggerFactory {
    public static final String CONFIGURATION = "Logger.conf.xml";
    
    static {
        init();
    }

    /**
     * Initializes the logger properties.
     *
     */
    private static void init() {
        try {
            InputStream stream;
            ClassLoader loader = LoggerFactory.class.getClassLoader();
            if (loader == null) {
                // The class loader for classes loaded by the system
                // class loader may be represented as null. Yes, this
                // is very dumb and not consistant and needs to be
                // fixed!
                stream = ClassLoader.getSystemResourceAsStream(CONFIGURATION);
            } else {
                stream = loader.getResourceAsStream(CONFIGURATION);
            }
            DOMConfigurator configurator = new DOMConfigurator(); 
            configurator.doConfigure(stream, LogManager.getLoggerRepository());
        } catch (Exception e) {
            System.err.println("The Log4j logger for CSI application is nto initialized.");
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Returns the wrapped log4j logger, where events are being logged.
     *
     * @param c the class attribute to generate the Category
     * @return the Logger Instance
     */
    public static Logger getInstance(Class c) {
        return Logger.getLogger(c); 
    }

    /**
     * Returns the wrapped log4j logger, where events are being logged.
     *
     * @param s the String attribute to generate the Category
     * @return the Logger Instance
     */
    public static Logger getInstance(String s) {
        return Logger.getLogger(s);
    }
}
