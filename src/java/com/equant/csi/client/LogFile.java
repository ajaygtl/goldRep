/*
 * Created by IntelliJ IDEA.
 * User: KPR(Kiran P Reddy)
 * Date: Aug 8, 2002
 * Time: 11:36:34 AM
 * This code is for Logging the error cases with error and XML when not in proper format
 */
package com.equant.csi.client;

import com.equant.csi.utilities.LoggerFactory;
import org.apache.log4j.Category;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.text.SimpleDateFormat;

public class LogFile {
    private static final Category logger = LoggerFactory.getInstance(LogFile.class.getName());
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMMyyyy");

    private LogFile() { }

    private static void writeToLogFile(String s, Exception error, String xmlString) {
        String slash = File.separator;
        try {
            FileOutputStream log = new FileOutputStream("config" + slash + "mydomain" + slash + "csilogs" + slash + s + ".log", true);
            PrintStream ps = new PrintStream(log);
            ps.println("-----------------------------------------------------------");
            ps.print("TIME : ");
                ps.println(timeFormat.format(new Date()));
            ps.print("ERROR : ");
                error.printStackTrace(ps);
            ps.print("XML STRING : ");
                ps.println(xmlString);
            ps.println("-----------------------------------------------------------");
            ps.println();
            ps.close();
        } catch (Exception e) {
            logger.error("Exception in logfile", e);
        }
    }

    /**
     * This method is for the situations where XMLString is available
     */
    public static void logErr(Exception error, String xmlString) {
        try {
            LogFile.writeToLogFile("Csi" + dateFormat.format(new Date()), error, xmlString);
        } catch (Exception e) {
            logger.error("Error in logErr method ");
        }
    }
}
