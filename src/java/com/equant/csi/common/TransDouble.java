/*
 * $IonIdea: $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2003 Equant corporation.
 */
package com.equant.csi.common;

import com.equant.csi.utilities.LoggerFactory;
import org.apache.log4j.Category;

import java.text.DecimalFormat;
import java.text.FieldPosition;

/**
 * Provides useful methods for translating and formatting the date.
 *
 * @author Kostyantyn Yevenko
 * @version 1.0
 */
public class TransDouble {

    /** Initializing the logger. */
    private static Category logger = LoggerFactory.getInstance(TransDouble.class.getName());

    /**
     * Defines the decimal formatter for formatting double value in the style
     * ###################0.####################
     */
    private static final DecimalFormat df = new DecimalFormat("###################0.####################");

    /**
     * Formats the date.
     *
     * @return the formatted date
     */
    public String toString() {
        return df.format(this);
    }

    /**
     * Parse method as required in JAXB specification.
     * @param input The date in <code>String</code>
     * @return new instance of this class.
     */
    public static double parseString(String input) {
        try {
            return Double.parseDouble(input);
        } catch (Exception e) {
            logger.error(e.toString(), e);
            return -1;
        }
    }

    /**
     * Print method as required in JAXB specification.
     * @param input The date in <code>TransDate</code>
     * @return a <code>String</code> representation of date.
     */
    public static String printToString(double input) {
        StringBuffer result = new StringBuffer();
        df.format(input, result, new FieldPosition(DecimalFormat.INTEGER_FIELD));
        return result.toString();
    }
}

