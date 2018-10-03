package com.equant.csi.interfaces.cis;

import com.equant.csi.exceptions.CISException;
import com.equant.csi.ejb.biz.ServiceManager;
import com.equant.csi.utilities.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Category;

/**
 * Created by IntelliJ IDEA.
 * User: szhu
 * Date: Jul 1, 2004
 * Time: 6:23:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class CISManagerHelper {

       protected static final Category cis_logger = LoggerFactory.getInstance("cis_extraction.CISIPProduct");

       /*
        * List contains names of products which are allowed for CIS Extraction  
        */
       protected static final String [] LIST_OF_PRODUCT = {
               CISConstants.CIS_PRODUCT_ATM_AC,
               CISConstants.CIS_PRODUCT_FR_AC,
               CISConstants.CIS_PRODUCT_LANAS,
               CISConstants.CIS_PRODUCT_MANAGED_CPE,
               CISConstants.CIS_PRODUCT_IPVPN,
               CISConstants.CIS_PRODUCT_INTERNETDIRECT,
               CISConstants.CIS_PRODUCT_INTRANETCONNECT
           };

    /**
     * Purpose:  Format a date string for sybase
     *
     * @param p_date
     * @return
     */
    public static String getSybaseFormatDateStr(Date p_date) {
        SimpleDateFormat sdf = new SimpleDateFormat(CISConstants.SYBASE_DATE_FORMAT);
        String dateStr = sdf.format(p_date);
        return dateStr;
    }

    /**
     * Purpose:  Handle '' issue
     *
     * @param stringbuffer
     * @param s
     * @return
     */
    public static StringBuffer appendLiteral(StringBuffer stringbuffer, String s) {
        if (s != null) {
            stringbuffer.append('\'');
            int i = 0;
            for (int j = s.indexOf('\''); j != -1; j = s.indexOf('\'', i)) {
                stringbuffer.append(s.substring(i, j)).append("''");
                i = j + 1;
            }

            return stringbuffer.append(s.substring(i)).append('\'');
        } else {
            return stringbuffer.append("NIL");
        }
    }

    /**
     * @param sybaseConn
     * @param localConn
     * @param runType
     * @param sm
     * @param goldConn
     * @param productToRunSet
     * @param logger
     * @throws CISException
     */
    public static void cisSubRun( Connection sybaseConn, Connection localConn,
                                 int runType, ServiceManager sm, Connection goldConn,
                                 Set productToRunSet, Category logger) throws CISException {


        logger.info("Starting point of CIS extraction job.");
        logger.info("Updating Remote Site information");
        new CISRemoteSiteCodePopulation().run(localConn);
        logger.info("Finished updating Remote Site information");

        //clean the site temp table first
//        logger.info("Clean site temp table");
//        cleanSiteTempTable(localConn);

        Timestamp startTime = null;
        Timestamp endTimestamp = null;

        //TODO: it will be better to have some special class for saving log information into DB
        //but during current step of refactoring I will use CISATMProduct (as it was before) 
        CISProductBase productDBLogHelper = new CISATMProduct(sybaseConn, localConn, runType, sm, goldConn, CISConstants.CIS_PRODUCT_ATM_AC);
        StringBuffer extractLogMsg = new StringBuffer();
        try {
            startTime = productDBLogHelper.getCurrentSybaseTimeStamp();
            logger.info("Start to extract data from CIS at: " + startTime);

            //check if we have something for Run
            if (productToRunSet != null ) {

                //look up each allowed product 
                for (int i = 0; i < LIST_OF_PRODUCT.length; i++) {
                    //and run CIS Extraction for current product if it was choosen
                    String currentProductName = LIST_OF_PRODUCT[i];
                    if ( productToRunSet.contains( currentProductName)) {
                        cisRunForProduct( currentProductName, 
                                sybaseConn, localConn, runType, sm, goldConn, logger, extractLogMsg);
                    } else {
                        logger.debug("Product: " + currentProductName + " was skipped");
                    }
                }
            } else {
                logger.debug("productToRunSet contains nothing");
            }
            
            if (logger.isDebugEnabled()) {
                Runtime runtime = Runtime.getRuntime();
                logger.debug("Finished. Memory:" + runtime.totalMemory() + "|" + runtime.freeMemory());
            }
            
            endTimestamp = productDBLogHelper.getCurrentSybaseTimeStamp();
            productDBLogHelper.insertExtractLog(startTime, endTimestamp, CISConstants.CIS_EXTRACTSTATUS_SUCCESS, (extractLogMsg != null)? extractLogMsg.toString():"");

        } catch (Throwable cise) {
            logger.error("Error in CIS extraction:" + cise);
            logger.debug("Error in CIS extraction:" + cise);
            try {
                endTimestamp = productDBLogHelper.getCurrentSybaseTimeStamp();
                productDBLogHelper.insertExtractLog(startTime, endTimestamp, CISConstants.CIS_EXTRACTSTATUS_FAILED, (extractLogMsg != null)? extractLogMsg.toString():"");
            } catch(Throwable t) {
                logger.error("Error in CIS extraction (2):" + cise);
                logger.debug("Error in CIS extraction (2):" + cise);
            }

            throw new CISException(cise.toString());
            //catch here, not throw out in order to let the scheduler gracefully finish the process
        }

        logger.info("End point of CIS extraction job.");
    }

    /*
     * Makes correct inctance of "runner" class for selected product
     * and run it with logger
     */
    private static void cisRunForProduct (
            String productName,
            Connection sybaseConn, Connection localConn,
            int runType, ServiceManager sm, Connection goldConn,
            Category logger,  StringBuffer extractLogMsg) throws CISException {
        
        CISProductBase product = null;
        //make correct inctance of "runner" class
        if (CISConstants.CIS_PRODUCT_ATM_AC.equals( productName)) {
            product = new CISATMProduct(sybaseConn, localConn, runType, sm, goldConn, CISConstants.CIS_PRODUCT_ATM_AC);

        } else if (CISConstants.CIS_PRODUCT_FR_AC.equals( productName)) {
            product = new CISFRProduct(sybaseConn, localConn, runType, sm, goldConn, CISConstants.CIS_PRODUCT_FR_AC);

        } else if (CISConstants.CIS_PRODUCT_LANAS.equals( productName)) {
            product = new CISIPProduct(sybaseConn, localConn, runType, sm, goldConn, CISConstants.CIS_PRODUCT_LANAS);

        } else if (CISConstants.CIS_PRODUCT_MANAGED_CPE.equals( productName)) {
            product = new CISIPProduct(sybaseConn, localConn, runType, sm, goldConn, CISConstants.CIS_PRODUCT_MANAGED_CPE);

        } else if (CISConstants.CIS_PRODUCT_IPVPN.equals( productName)) {
            product = new CISIPProduct(sybaseConn, localConn, runType, sm, goldConn, CISConstants.CIS_PRODUCT_IPVPN);

        } else if (CISConstants.CIS_PRODUCT_INTERNETDIRECT.equals( productName)) {
            product = new CISIPProduct(sybaseConn, localConn, runType, sm, goldConn, CISConstants.CIS_PRODUCT_INTERNETDIRECT);

        } else if (CISConstants.CIS_PRODUCT_INTRANETCONNECT.equals( productName)) {
            product = new CISIPProduct(sybaseConn, localConn, runType, sm, goldConn, CISConstants.CIS_PRODUCT_INTRANETCONNECT);
        }

        //run
        if (product != null) {
            cisRunWithLog( productName, logger, extractLogMsg, product);
        } else {
            //unknown product
            logger.debug("Unsupported product: " + productName + " was skipped");
        }

    }


    /**
     * Method calls method <code>run<code> of <code>product<code> object 
     * and logs memory size before and after run.  
     */
    private static void cisRunWithLog(String productName, Category logger, StringBuffer extractLogMsg, CISProductBase product) throws CISException {
        Runtime runtime = Runtime.getRuntime();
        if (logger.isDebugEnabled())
            logger.debug( productName + " Product starting. Memory:" + runtime.totalMemory() + "|" + runtime.freeMemory());

        //run
        extractLogMsg.append(product.run());
        
        if (logger.isDebugEnabled())
            logger.debug( productName + " Product end.  Memory:" + runtime.totalMemory() + "|" + runtime.freeMemory());

    }

    /**
     * @param productToRun
     * @return
     */
     public static HashSet parseProductToRun(String productToRun) {
         HashSet productToRunSet = new HashSet();
         String s = "";
         StringTokenizer tn = new StringTokenizer(productToRun, CISConstants.CIS_PRODUCT_DELIM);
         while (tn.hasMoreElements()) {
             s = (String) tn.nextElement();
             productToRunSet.add(s);
         }

         return productToRunSet;
     }

    /**
     *
     */
    protected static void cleanSiteTempTable(Connection localConn) throws CISException {
        PreparedStatement ps = null;
        try {
            ps = localConn.prepareStatement("TRUNCATE TABLE CIS_TEMP_SITE ");
            ps.execute();
        } catch (SQLException e) {
            cis_logger.error("Can't truncate site temp table.", e);
            throw new CISException("Can't perform SQL query.", e);
        } finally {
            try {
                if (ps != null) ps.close();
                ps = null;
            } catch (SQLException e) {
                cis_logger.error("Error close PS.", e);
            }
        }
    }

}
