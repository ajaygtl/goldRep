/*
 * $IonIdea: $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2003 Equant corporation.
 */
package com.equant.csi.utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Hashtable;

import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Category;

import oracle.jdbc.driver.OracleDriver;
import oracle.jdbc.pool.OracleDataSource;

/**
 * This class is used for testing puproses only. Used from com.equant.csi.test package.
 *
 * @author Kostyantyn Yevenko
 */
public class JDBCUtils {

    /** Initializing the logger. */
    private static final Category logger = LoggerFactory.getInstance(JDBCUtils.class.getName());

    /** Defines the DB setup properties */
    private static Properties properties = null;

    static {
        try {
            FileInputStream fis = new FileInputStream("../../../ant.properties");

            properties = new Properties();
            properties.load(fis);

            dumpProperties(properties);
        } catch (IOException e) {
            logger.fatal(e.toString(), e);
        }
    }

    public static DataSource getDataSource(String name) throws NamingException {
        if(logger.isDebugEnabled()) {
            logger.debug("getDataSource(\"" + name + "\")");
        }

        InitialContext ic = new InitialContext();
        return (DataSource) ic.lookup(name);
    }

    /**
     * This method is for testing purposes only.
     * Returns the Oracle DataSource.
     *
     * @return the DataSource instance.
     */
    public static DataSource getDataSource() {
        OracleDataSource ds = null;
        try {
            DriverManager.registerDriver(new OracleDriver());

            ds = new OracleDataSource();

            ds.setURL(properties.getProperty("jdbc.url"));
            ds.setUser(properties.getProperty("jdbc.username"));
            ds.setPassword(properties.getProperty("jdbc.password"));
        } catch (SQLException e) {
            logger.error("SQLException", e);
        }

        return ds;
    }

    /**
     * Dumps the specified properties into the logger output.
     *
     * @param properties the properties to dump
     */
    private static void dumpProperties(Properties properties) {
        if (!logger.isDebugEnabled())
            return;

        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            logger.debug("Property " + name + " = " + properties.getProperty(name));
        }
    }
}