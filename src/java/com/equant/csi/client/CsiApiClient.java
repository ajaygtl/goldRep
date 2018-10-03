/**
 * $Author: vasyl.rublyov $ $Date: 2002/11/24 02:52:45 $
 * $Revision: 1.9 $ $RCSfile: CsiApiClient.java,v $
 * Copyright Equant.
 */
package com.equant.csi.client;

import com.equant.csi.ejb.api.CsiApiHome;
import com.equant.csi.jdo.CServiceChangeItem;
import com.equant.csi.jdo.CServiceElement;
import com.equant.csi.jdo.CVersionServiceElement;
import com.equant.csi.utilities.LoggerFactory;
import org.apache.log4j.Category;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

public class CsiApiClient implements Runnable {

    private static final Category logger = LoggerFactory.getInstance(CsiApiClient.class.getName());
    private com.equant.csi.ejb.api.CsiApi CsRemote;
    private com.equant.csi.ejb.api.CsiApiHome CsHome;
    private static String url = null;
    private static String siteId = "3M Dubai";
    private static String customerId = "2346";
    private static String serviceId = "FRAME_RELAY_PURPLE";
    private boolean funcFlag = false;

    public CsiApiClient(boolean flag) {
        try {
            funcFlag = flag;
			// jonas/jms migration
            //url = "t3://localhost:7001";
            url = "rmi://localhost:1099";
            lookAtBean();
        } catch (Exception e) {
        }
    }

    public static void main(String[] args) throws Exception {
        Thread thread1 = new Thread(new CsiApiClient(false), "Thread1");
        Thread thread2 = new Thread(new CsiApiClient(false), "Thread2");
        Thread thread3 = new Thread(new CsiApiClient(true), "Thread3");
        Thread thread4 = new Thread(new CsiApiClient(true), "Thread4");
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        logger.debug("Leaving...");
    }

    public void getServices(String custId, String siteId, String coreSiteId, String addressId) {
//      logger.debug("Entering...");
        try {
            long startTime = System.currentTimeMillis();
            Vector serviceNameVec = CsRemote.getServices(custId, siteId, coreSiteId, addressId);
            logger.debug(Thread.currentThread().getName() + ": Call To getServices took |" + (System.currentTimeMillis() - startTime) + "| ms");
            logger.debug(Thread.currentThread().getName() + ": Services |" + serviceNameVec + "|");

        } catch (RemoteException e) {
            logger.error(e.toString(), e);
        }
    }

    //This is test method to get the service element's details
    public void getServiceDetails(String custId, String siteId, String coreSiteId, String addressId, String serviceName) {
//       String serviceXML = null;
        try {
            long startTime = System.currentTimeMillis();
//          serviceXML = (String) CsRemote.getServiceDetails(custId, siteId, serviceName);
            CsRemote.getServiceDetails(custId, siteId, coreSiteId, addressId, serviceName);
            logger.debug(Thread.currentThread().getName() + ": Call To getServiceDetails()  took |" + (System.currentTimeMillis() - startTime) + "| ms");
        } catch (RemoteException e) {
            logger.error(e.toString(), e);
        }
    }

    public void printServiceElementVec(java.util.Vector vec) {
        logger.debug("Entering...");
        if (vec == null) {
            logger.debug("VECTOR IS NULL");
        } else {
            for (Enumeration e = vec.elements(); e.hasMoreElements();) {
                Object obj = e.nextElement();
                if (obj instanceof java.lang.Integer) {
                    logger.debug("Got Integer");
                    java.lang.Integer intg = (Integer) obj;
                    logger.debug("Integer Value |" + intg.toString() + "|");
                } else if (obj instanceof CServiceElement) {
                    CServiceElement sce = (CServiceElement) obj;
                    if (sce != null) {
                        logger.debug("Service Element Id       |" + sce.getServiceElementId() + "|");
                        logger.debug("Service Element Desc     |" + sce.getDescription() + "|");
                        logger.debug("Service Element Crt Date |" + sce.getServiceElementCreationDate() + "|");
                        logger.debug("Service Element Prod Id  |" + sce.getProdId() + "|");
                    }
                } else {
                    logger.error("No more service elements");
                }
            }
        }

        logger.debug("Leaving...");
    }

    public void testApis(String siteId) {
        CServiceElement topse = new CServiceElement();
        //Vector seDetailVector = null;
        logger.debug("Entering...");
        java.util.Vector newVec = null;
        java.util.Vector v2 = null;
        Collection topvsecc = null;
        try {
            //Double val = CsRemote.getUseId ();
            //logger.debug ("New UseId |" + val.doubleValue () + "|");
            logger.debug("Calling getServices");
            newVec = CsRemote.getServices(siteId, "site");
            logger.debug("Done Calling getServices");
            if (newVec != null) {
                printServiceElementVec(newVec);
                logger.debug("Total elements:" + newVec.size());
                for (int j = 0; j < newVec.size(); j++) {
                    topse = (CServiceElement) newVec.elementAt(j);
                    if (topse != null) {
                        logger.debug("Element not null #:" + j + " USEID=" + topse.getServiceElementId());
                        topvsecc = topse.getCVerServiceElementColl();
                        if (topvsecc != null)
                            logger.debug("Size of each vse: " + topvsecc.size());
                        topvsecc = topse.getCParentSrvChangeItemColl();
                        if (topvsecc != null)
                            logger.debug("Size of each sci: " + topvsecc.size());
                    }
                }
            }

            {
                logger.debug("Calling getServiceDetails");
//               long useIdParam = 102046; // 102036
                //OLD
                //v2 = (java.util.Vector) CsRemote.getServiceDetails (useIdParam);
                //***NEW This above method's return type has been changed ..change it accordingly
                if (v2 != null && v2.size() > 0) {
                    logger.debug("SE Vector size after call to getServiceDetails:" + v2.size());
                    topse = (CServiceElement) v2.firstElement();
                    logger.debug("Call to viewVSE from client - begin -  -  -  -  -  -  -  -  -");
                    viewVSE(topse);
                    logger.debug("Call to viewVSE from client - end   -  -  -  -  -  -  -  -  -");
                }
            }
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
        logger.debug("Leaving...");

    }

    public void viewSCI(CVersionServiceElement vse) throws ClassNotFoundException {
        Collection c;
        Iterator i;
        c = vse.getCServiceChangeItemColl();
        if (c != null) {
            logger.debug("size of return sci=" + c.size());
            for (i = c.iterator(); i.hasNext();) {
                CServiceChangeItem sci = (CServiceChangeItem) i.next();
                if (sci != null) {
                    logger.debug("id=" + sci.getServiceChangeItemId());
                    logger.debug("created by=" + sci.getCreatedBy());
                    logger.debug("create date=" + sci.getCreatedDate());
                }
            }
        } else
            logger.debug("sci is null!!");
    }

    public void viewVSE(CServiceElement s) throws ClassNotFoundException {
        Collection c;
        Iterator i;
        logger.debug("SE Id=" + s.getServiceElementId());
        logger.debug("SE Id=" + s.getCreatedDate());
        c = s.getCVerServiceElementColl();
        if (c != null) {
            logger.debug("size of return vse=" + c.size());
            for (i = c.iterator(); i.hasNext();) {
                CVersionServiceElement vse = (CVersionServiceElement) i.next();
                if (vse != null) {
                    logger.debug("id=" + vse.getVersionServiceElementId());
                    logger.debug("created by=" + vse.getCreatedBy());
                    logger.debug("create date=" + vse.getCreatedDate());
                    logger.debug("sci date - start");
                    viewSCI(vse);
                    logger.debug("sci date - end");
                }
            }
        } else
            logger.debug("vse is null!!");
    }

    public void lookAtBean() throws Exception {
        logger.debug("inside the lookAtBean() method ..... ");
        try {
            javax.naming.Context Ctx = getInitialContext();
            CsHome = (CsiApiHome) Ctx.lookup("com.equant.csi.ejb.api.CsiApiHome");
            logger.debug("CsiSession Home interface found ");
            CsRemote = CsHome.create();
            //insert your method calls here .........
        } catch (Exception e) {
            logger.error(" kiran error occured while context look up" + e);
            logger.error(e.toString(), e);
        }
    }

    /**
     * get the Initial Context
     */
    public static Context getInitialContext() throws Exception {
        logger.debug("Entering...)");
        Properties P = new Properties();
        // jonas/jms migration
        /*
		P.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
        P.put(Context.PROVIDER_URL, url);
		*/
        P.put(Context.INITIAL_CONTEXT_FACTORY, "org.ow2.carol.jndi.spi.IRMIContextWrapperFactory");
		P.put(Context.URL_PKG_PREFIXES,"org.ow2.jonas.naming" );
		P.put(Context.PROVIDER_URL, url);
        return new InitialContext(P);
    }

    public void run() {
        for (int i = 0; i < 75; i++) {
            logger.debug(Thread.currentThread().getName() + ": Counter |" + i + "|");
            if (funcFlag == true)
                getServiceDetails(customerId, siteId, null, null, serviceId);
            else
                getServices(customerId, siteId, null, null);
            try {
                Thread.currentThread().sleep(300);
            } catch (InterruptedException e) {
            }
        }
    }
}