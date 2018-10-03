package com.equant.csi.test;

import java.io.File;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Category;

import com.equant.csi.database.CreateManager;
import com.equant.csi.database.DeleteManager;
import com.equant.csi.database.QueryManager;
import com.equant.csi.database.UpdateManager;
import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.Notes;
import com.equant.csi.jaxb.ObjectFactory;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.utilities.JAXBUtils;
import com.equant.csi.utilities.JDBCUtils;
import com.equant.csi.utilities.LoggerFactory;
import junit.framework.TestCase;

/**
 * @version CVS $Id: CSIPerformanceTest.java,v 1.12 2003/01/09 23:07:24 constantine.evenko Exp $
 */
public class CSIPerformanceTest extends TestCase {
    private static final Category logger = LoggerFactory.getInstance(CSIPerformanceTest.class.getName());

    private static final int CLIENTS    =  5;
    private static final int SITES      =  5;
    private static final int SERVICES   =  5;
    private static final int STEPS      =  4;

    private static DataSource dataSource;
    private static Unmarshaller unmarshaller;

    private ObjectFactory objectFactory = null;

    static {
        dataSource = JDBCUtils.getDataSource();
        try {
            JAXBContext context = JAXBUtils.getContext();
            unmarshaller = context.createUnmarshaller();
            unmarshaller.setValidating(true);
        } catch (JAXBException e) {
            logger.error("Exception", e);
            throw new RuntimeException("Failed to iniialize JAXB: " + e);
        }
    }

    public CSIPerformanceTest(String name) {
        super(name);
        objectFactory = new ObjectFactory();
    }

//    public void testLeak() throws Exception {
//        if (true) return;
//        PersistenceManager pm = null;
//        try {
//            pm = getJDOManager();
//
//            for (int i = 0; i < 200000; i++) {
//                pm.currentTransaction().begin();
//
//                Query query = pm.newQuery(CVersionServiceElement.class, "cServiceElement == id");
//                query.declareParameters("String id");
//                try {
//                    // Execute query
//                    Collection result = (Collection) query.execute(new Long(i).toString());
//                    // Read results
//                    Collection copy = new ArrayList(result.size());
//                    copy.addAll(result);
//                    copy.clear();
//                } finally {
//                    try { query.closeAll(); } catch (Exception e) { }
//                }
//                pm.evictAll();
//
//                pm.currentTransaction().commit();
//
//                if (i % 1000 == 0) {
//                    String si = "" + i;
//                    while (si.length() < 7) si = " " + si;
//                    logger.debug("Iteration " + si + ": " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
//                }
//            }
//
//            pm.close();
//            pm = null;
//        } catch (Throwable e) {
//            logger.error("Exception", e);
//            fail("Exception " + e);
//        }
//    }

    public void testDelete() throws Exception {
        // if (true) return;
        logger.debug("----------------------------------------- Entering -----------------------------------------");

        try {
            long start = System.currentTimeMillis();
            for (int i = 0; i < CLIENTS; i++) {
                String si = "" + i;
                while (si.length() < 2) si = "0" + si;

                for (int j = 0; j < SITES; j++) {
                    String sj = "" + j;
                    while (sj.length() < 2) sj = "0" + sj;

                    for (int k = 0; k < SERVICES; k++) {
                        String sk = "" + k;
                        while (sk.length() < 2) sk = "0" + sk;

                        for (int l = 0; l < STEPS; l++) {
                            String sl = "" + l;
                            while (sl.length() < 2) sl = "0" + sl;

                            // Use special method to delete service elements, including all in satus DELETE or DISCONNECT
                            new DeleteManager(dataSource).deleteAllVersions("TEST-ORD-" + si + "-" + sj + "-" + sk + "-" + sl);
                        }
                    }

                    logger.debug("Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
                }
            }

            logger.warn("Delete: Done " + (CLIENTS*SITES*SERVICES*STEPS) + " orders in " + (System.currentTimeMillis()-start) + "ms");
            logger.warn("Delete: " + (CLIENTS*SITES*SERVICES*STEPS*1000f/(System.currentTimeMillis()-start)) + " calls per second");
        } catch (Throwable e) {
            logger.error("Exception", e);
            fail("Exception " + e);
        }
    }

    public void testCreate() throws Exception {
        // if (true) return;
        logger.debug("----------------------------------------- Entering -----------------------------------------");

        try {
            Message[] messages = new Message[STEPS];
            for (int i = 1; i <= STEPS; i++) {
                messages[i-1] = (Message)unmarshaller.unmarshal(new File("../../../config/test/perf/test-" + i + ".xml"));
            }

            long start = System.currentTimeMillis();
            for (int i = 0; i < CLIENTS; i++) {
                String si = "" + i;
                while (si.length() < 2) si = "0" + si;

                for (int j = 0; j < SITES; j++) {
                    String sj = "" + j;
                    while (sj.length() < 2) sj = "0" + sj;

                    for (int k = 0; k < SERVICES; k++) {
                        String sk = "" + k;
                        while (sk.length() < 2) sk = "0" + sk;

                        for (int l = 0; l < STEPS; l++) {
                            String sl = "" + l;
                            while (sl.length() < 2) sl = "0" + sl;

                            VersionType version = (VersionType)messages[l].getVersion().get(0);
                            version.setOrderid("TEST-ORD-" + si + "-" + sj + "-" + sk + "-" + sl);
                            version.setCustomerId("CUSTOMER" + si);
                            version.setSiteId("SITE" + sj);
                            version.setServiceId("FRAME_RELAY_TEST" + sk);

                            if (messages[l].getVersion().size() > 1) {
                                new UpdateManager(dataSource).updateVersion(version);
                                version = (VersionType)messages[l].getVersion().get(1);
                                version.setOrderid("TEST-ORD-" + si + "-" + sj + "-" + sk + "-" + sl);
                                version.setCustomerId("CUSTOMER" + si);
                                version.setSiteId("SITE" + sj);
                                version.setServiceId("FRAME_RELAY_TEST" + sk);
                            }
                            new CreateManager(dataSource).createVersion(version);
                        }
                    }

                    logger.debug("Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
                }
            }

            logger.warn("Create: Done " + (CLIENTS*SITES*SERVICES*STEPS) + " orders in " + (System.currentTimeMillis()-start) + "ms");
            logger.warn("Create: " + (CLIENTS*SITES*SERVICES*STEPS*1000f/(System.currentTimeMillis()-start)) + " calls per second");
        } catch (Throwable e) {
            logger.error("Exception", e);
            fail("Exception " + e);
        }
    }

    public void testQuery() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");

        int count = 0;
        long time = System.currentTimeMillis();

        try {
            Notes notes = objectFactory.createNotes();

            for (int i = 0; i < CLIENTS; i++) {
                String si = "" + i;
                while (si.length() < 2) si = "0" + si;

                for (int j = 0; j < SITES; j++) {
                    String sj = "" + j;
                    while (sj.length() < 2) sj = "0" + sj;

                    int k = 0;
                    String sk = "" + k;
                    while (sk.length() < 2) sk = "0" + sk;

                    notes.getNote().clear();

                    long start = System.currentTimeMillis();
                    new QueryManager(dataSource).getServiceDetails("CUSTOMER" + si,
                                                                   "SITE" + sj,
                                                                   "FRAME_RELAY_TEST" + sk,
                                                                   notes);
                    logger.warn("Query " + count + ": Done in " + (System.currentTimeMillis()-start) + "ms. "
                                + "Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
                    count ++;
                }
            }

            logger.warn("Average: " + ((System.currentTimeMillis()-time)/count) + "ms per query on " + count + " queries");
        } catch (Throwable e) {
            logger.error("Exception", e);
            fail("Exception " + e);
        } finally {
        }
    }

//    public void testQueryLegacy() throws Exception {
//        logger.debug("----------------------------------------- Entering -----------------------------------------");
//
//        int count = 0;
//        long time = System.currentTimeMillis();
//
//        PersistenceManager pm = null;
//        try {
//            pm = getJDOManager();
//
//            for (int i = 0; i < CLIENTS; i++) {
//                String si = "" + i;
//                while (si.length() < 2) si = "0" + si;
//
//                for (int j = 0; j < SITES; j++) {
//                    String sj = "" + j;
//                    while (sj.length() < 2) sj = "0" + sj;
//
//                    int k = 0;
//                    String sk = "" + k;
//                    while (sk.length() < 2) sk = "0" + sk;
//
//                    long start = System.currentTimeMillis();
//                    new QueryManagerLegacy(pm).getServiceDetails("CUSTOMER" + si,
//                                                                 "SITE" + sj,
//                                                                 "FRAME_RELAY_TEST" + sk);
//                    logger.warn("Query " + count + ": Done in " + (System.currentTimeMillis()-start) + "ms. "
//                                + "Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
//                    count ++;
//                }
//            }
//
//            pm.close();
//            pm = null;
//
//            logger.warn("Average: " + ((System.currentTimeMillis()-time)/count) + " ms per query on " + count + " queries");
//        } catch (Throwable e) {
//            logger.error("Exception", e);
//            fail("Exception " + e);
//        } finally {
//            if (pm != null) {
//                if (pm.currentTransaction().isActive()) pm.currentTransaction().rollback();
//                pm.close();
//            }
//        }
//    }
}