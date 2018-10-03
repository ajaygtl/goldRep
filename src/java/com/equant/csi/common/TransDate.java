/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/common/TransDate.java,v 1.11 2002/12/05 15:57:38 victoria.ovdenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.common;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Category;

import com.equant.csi.utilities.LoggerFactory;

/**
 * Provides useful methods for translating and formatting the date.
 *
 * @author Vadim Gritsenko
 * @version $Revision: 1.11 $
 *
 * @see java.util.Date
 */
public class TransDate extends Date {

    /** Initializing the logger. */
    private static Category logger = LoggerFactory.getInstance(TransDate.class.getName());

    /**
     * Default constructor.
     *
     */
    public TransDate() {
        super();
    }

    /**
     * Constructs the class with specified date given in milliseconds.
     *
     * @param date the date in milliseconds
     */
    public TransDate(long date) {
        super(date);
    }

    /**
     * Constructs the class with specified date given as <code>Date</code>.
     *
     * @param d the given <code>Date</code>
     */
    public TransDate(Date d) {
        super(d.getTime());
    }

    /**
     * Constructs the class with specified date represented by
     * <code>String</code>.
     *
     * @param s the string representation of the date
     */
    public TransDate(String s) {
        this(parseDate(s));
    }

    /**
     * Formats the date.
     *
     * @return the formatted date
     */
    public String toString() {
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df1.format(this);
    }

    /**
     * Converts the <code>java.util.Date</code> to
     * <code>java.sql.Timestamp</code>.
     * It is used to obtain a thin wrapper around <code>java.util.Date</code>
     * that allows the JDBC API to identify this as an SQL TIMESTAMP value.
     *
     * @param d the given <code>Date</code>
     * @return the <code>Timestamp</code> constructed on the base of
     *         given <code>Date</code>. Returns current datatime if
     *         input date is null.
     */
    public static Timestamp getTimestamp(Date d) {
        return (d == null ?
                new Timestamp(new Date().getTime()) :
                new Timestamp(d.getTime())
                );
    }

    /**
     * Returns the <code>java.sql.Timestamp</code> value.
     * It is used to obtain a thin wrapper around <code>java.util.Date</code>
     * that allows the JDBC API to identify this as an SQL TIMESTAMP value.
     *
     * @return the <code>Timestamp</code> constructed on the base of
     *         internal <code>Date</code>. Returns current datatime if
     *         input date is null.
     */
    public Timestamp getTimestamp() {
        return getTimestamp(this);
    }

    /**
     * Allocates a <code>Date</code> object and initializes it so that
     * it represents the date and time indicated by the string d.
     *
     * @param d a string to be parsed as a date.
     * @return the <code>Date</code> constructed on the base of String
     *         representation.
     */
    private static Date parseDate(String d) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return df.parse(d);
        } catch (java.text.ParseException pe1) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                return df.parse(d);
            } catch (java.text.ParseException pe2) {
            	try {
                    SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yy");
                    return df.parse(d);
                } catch (java.text.ParseException pe3) {
                	logger.error(pe3.toString(), pe3);
                }
            }
            logger.error(pe1.toString(), pe1);
            return new Date();
        }
    }

    /**
     * Parse method as required in JAXB specification.
     * @param input The date in <code>String</code>
     * @return new instance of this class.
     */
    public static TransDate parseString(String input) {
        return new TransDate(input);
    }

    /**
     * Print method as required in JAXB specification.
     * @param input The date in <code>TransDate</code>
     * @return a <code>String</code> representation of date.
     */
    public static String printToString(TransDate input) {
        return (input==null ? "" : input.toString());
    }
}

