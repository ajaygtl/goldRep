package com.equant.csi.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Vector;
import java.util.Iterator;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Validator;

import org.apache.log4j.Category;

import com.equant.csi.common.TransDate;
import com.equant.csi.database.CreateManager;
import com.equant.csi.database.DeleteManager;
import com.equant.csi.database.QueryManager;
import com.equant.csi.database.RollbackManager;
import com.equant.csi.database.UpdateManager;
import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.Notes;
import com.equant.csi.jaxb.ObjectFactory;
import com.equant.csi.jaxb.SystemType;
import com.equant.csi.jaxb.UserType;
import com.equant.csi.jaxb.Version;
import com.equant.csi.jaxb.VersionType;
import com.equant.csi.jaxb.ServiceElementType;
import com.equant.csi.utilities.JAXBUtils;
import com.equant.csi.utilities.JDBCUtils;
import com.equant.csi.utilities.LoggerFactory;
import com.sun.xml.bind.marshaller.DataWriter;
import junit.framework.TestCase;

/**
 * @version CVS $Id: CSITest.java,v 1.54 2003/01/18 00:52:36 vadim.gritsenko Exp $
 */
public class CSITest extends TestCase {
    private static final Category logger = LoggerFactory.getInstance(CSITest.class.getName());

    public static final String ORDER_PREFIX = "TEST-ORD-00";
    public static final String CUSTOMER_ID  = "1111";
    public static final String SITE_ID      = "Test Site";
    public static final String SERVICE_NAME = "FRAME_RELAY_TEST";

    private static DataSource dataSource;
    private static Validator validator;
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;

    private ObjectFactory objectFactory = null;

    static {
        dataSource = JDBCUtils.getDataSource();
        try {
            JAXBContext context = JAXBUtils.getContext();
            validator = context.createValidator();
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            unmarshaller = context.createUnmarshaller();
            unmarshaller.setValidating(true);
        } catch (JAXBException e) {
            logger.error("Exception", e);
            throw new RuntimeException("Failed to iniialize JAXB: " + e);
        }
    }

    public CSITest(String name) {
        super(name);
        objectFactory = new ObjectFactory();
    }

    public void testJDBC() {
        logger.debug("----------------------------------------- Entering -----------------------------------------");

        Connection con;
        try {
            con = dataSource.getConnection();

            SystemType system = objectFactory.createSystemType();
            UserType user = objectFactory.createUserType();
            user.setName("tester");
            system.setId("GOLD");
            system.setUser(user);

            CreateManager cm = new CreateManager(dataSource,
                                                 new Date(System.currentTimeMillis()-24*60*60*1000),//yesterday
                                                 user.getName(),
                                                 null, //OrderType and OrderStatus are required for
                                                 null);//CreateManager.createVersionServiceElementStatuses method only
            long cSysUsrId = cm.findOrCreateSystemUser(system, con);

            PreparedStatement ps = con.prepareStatement(
                    "SELECT CreateDate,CreatedBy,LupdBy " +
                    "  FROM CSystemUser                 " +
                    " WHERE SystemUserID = ?            "
            );
            ps.setLong(1, cSysUsrId);
            ResultSet rs = ps.executeQuery();

            assertTrue("DB query should return 1 record.", rs.next());

            Date createdOld = rs.getTimestamp(1);
            String createdByOld = rs.getString(2);
            String updatedByOld = rs.getString(3);

            rs.close(); rs = null;
            ps.close(); ps = null;

            Date date = new Date(System.currentTimeMillis() / 1000 * 1000);
            logger.debug("Set update date for tester to: " + date + " (" + date.getTime() + "ms)");
            ps = con.prepareStatement(
                    "UPDATE CSystemUser         " +
                    "   SET LupdDate = ?        " +
                    " WHERE SystemUserID = ?    "
            );
            ps.setTimestamp(1, TransDate.getTimestamp(date));
            ps.setLong(2, cSysUsrId);

            ps.executeUpdate();

            ps.close(); ps = null;

            ps = con.prepareStatement(
                    "SELECT CreateDate,CreatedBy,LupdBy,LupdDate    " +
                    "  FROM CSystemUser                             " +
                    " WHERE SystemUserID = ?                        "
            );
            ps.setLong(1, cSysUsrId);
            rs = ps.executeQuery();

            assertTrue("DB query should return 1 record.", rs.next());

            Date createdNew = rs.getTimestamp(1);
            String createdByNew = rs.getString(2);
            String updatedByNew = rs.getString(3);
            Date updatedNew = rs.getTimestamp(4);

            logger.debug("New update date for tester set to: "+updatedNew + " (" + updatedNew.getTime() + "ms)");
            assertTrue("Date saved into the DB differs from the date obtained from the DB",
                       date.compareTo(updatedNew)==0);
            assertEquals("Created date should not change", createdOld, createdNew);
            assertEquals("Created By should not change", createdByOld, createdByNew);
            assertEquals("Updated By should not change", updatedByOld, updatedByNew);

            rs.close(); rs = null;
            ps.close(); ps = null;
            con.close();
        } catch (Exception e) {
            logger.error("Got exception", e);
            fail("Got exception: " + e);
        }
    }

    public void testUnmarshal() {
        logger.debug("----------------------------------------- Entering -----------------------------------------");
        try {
            // All these has to unmarshal without issues. That's our interface to the GOLD system.
            File[] files = new File("../interfacefiles").listFiles(new FilenameFilter() {
                public boolean accept (File dir, String name)
                {
                    return name.endsWith(".xml");
                }
            });
            for (int i = 0; i < files.length; i++) {
                logger.debug("Unmarshalling file " + files[i]);
                unmarshaller.unmarshal(files[i]);
            }
        } catch (JAXBException e) {
            logger.error("Got exception", e);
            fail("Got exception: " + e);
        }
    }

    public void testClean() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");

        DeleteManager deleteManager = new DeleteManager(dataSource);
        for (int i = 0; i <= 9; i++) {
            // Use special method to delete service elements, including all in satus DELETE or DISCONNECT
            deleteManager.deleteAllVersions("TEST-ORD-00" + i);
        }
        deleteManager.deleteAllVersions("TEST-LONG-FLOAT-001");
    }

    // Database is empty (with regards to test client)
    public void testGetServices0() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");

        long start = System.currentTimeMillis();
        try {
            QueryManager qManager = new QueryManager(dataSource);
            Vector v = qManager.getServices(CUSTOMER_ID, SITE_ID);
            logger.debug("Got services for the customer <" + CUSTOMER_ID
                         + ">, site <" + SITE_ID
                         + "> in " + (System.currentTimeMillis()-start) + "ms");

            assertNotNull("Result of getServices should not be null.", v);
            assertEquals("Result of getServices for should be 0 services.", 0, v.size());
        } finally {
            logger.debug("Leaving");
        }
    }

    // Create first order
    public void testCreate() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");

        Message message = (Message)unmarshaller.unmarshal(new File("../../../config/test/test-1-new-release.xml"));
        VersionType version = (VersionType)message.getVersion().get(0);

        long start = System.currentTimeMillis();

        new CreateManager(dataSource).createVersion(version);

        logger.debug("Created order <" + version.getOrderid()
                     + "> in " + (System.currentTimeMillis()-start) + "ms");

        // Check that it got created
        start = System.currentTimeMillis();
        QueryManager qManager = new QueryManager(dataSource);
        Vector v = qManager.getServices(CUSTOMER_ID, SITE_ID);

        logger.debug("Got services for the customer <" + CUSTOMER_ID
                     + ">, site <" + SITE_ID
                     + "> in " + (System.currentTimeMillis()-start) + "ms");

        assertNotNull("Result of getServices should not be null.", v);
        assertEquals("Result of getServices for should be 1 service.", 1, v.size());
        assertEquals("Result of getServices for should be " + SERVICE_NAME, SERVICE_NAME, v.elementAt(0));
    }

    // Delete order
    public void testDelete() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");

        long start = System.currentTimeMillis();

        new DeleteManager(dataSource).deleteVersion(ORDER_PREFIX + "1");

        logger.debug("Delete Order: " + (System.currentTimeMillis()-start) + "ms");

        // Check that it got deleted
        start = System.currentTimeMillis();
        QueryManager qManager = new QueryManager(dataSource);
        Vector v = qManager.getServices(CUSTOMER_ID, SITE_ID);
        logger.debug("Got services for the customer <" + CUSTOMER_ID
                     + ">, site <" + SITE_ID
                     + "> in " + (System.currentTimeMillis()-start) + "ms");

        assertNotNull("Result of getServices should not be null.", v);
        assertEquals("Result of getServices for should be 0 services.", 0, v.size());
    }

    // TODO
    public void testRollback() throws Exception {
        if (true) return;
        logger.debug("----------------------------------------- Entering -----------------------------------------");

        long start = 0, stop = 0;

        Message message = (Message)unmarshaller.unmarshal(new File("../../../config/test/test-1-new-release.xml"));
        VersionType mVersion = (VersionType)message.getVersion().get(0);
        mVersion.setOrderstatus("Manage");

        new CreateManager(dataSource).createVersion(mVersion);

        message = null; // TODO (Message)unmarshaller.unmarshal(new File("../interfacefiles/fr2-new-rollback.xml"));
        VersionType rVersion = (VersionType)message.getVersion().get(0);

        start = System.currentTimeMillis();
        new RollbackManager(dataSource).rollbackVersion(rVersion);

        logger.debug("Rollback Order: " + (stop-start) + "ms");

        // TODO: make sure that status has been changed.

        new RollbackManager(dataSource).rollbackVersion(rVersion);

        // make sure its gone.
        start = System.currentTimeMillis();
        QueryManager qManager = new QueryManager(dataSource);
        Vector v = qManager.getServices(CUSTOMER_ID, SITE_ID);
        logger.debug("Got services for the customer <" + CUSTOMER_ID
                     + ">, site <" + SITE_ID
                     + "> in " + (System.currentTimeMillis()-start) + "ms");

        assertNotNull("Result of getServices should not be null.", v);
        assertEquals("Result of getServices for should be 0 services.", 0, v.size());
    }

    // TODO
    public void testUpdate() throws Exception {
        if (true) return;
        logger.debug("Update Version Test");
        long start = 0, stop = 0;

        Message message = (Message)unmarshaller.unmarshal(new File("../interfacefiles/fr2-new-manage.xml"));
        VersionType mVersion = (VersionType)message.getVersion().get(0);
        mVersion.setOrderid("CSITEST0001");

        message = (Message)unmarshaller.unmarshal(new File("../interfacefiles/fr2-change-release.xml"));
        VersionType rVersion = (VersionType)message.getVersion().get(0);
        rVersion.setOrderid("CSITEST0002");
        assertEquals("Order Type", "Existing", rVersion.getOrdertype());

//            pm.currentTransaction().begin();
//            new CreateManager(pm).createVersion(mVersion);
//            start = System.currentTimeMillis();
//            new UpdateManager(pm).updateVersion(rVersion);
//            stop = System.currentTimeMillis();
//            pm.currentTransaction().commit();

        // TODO: make sure that status has been changed.

        new DeleteManager(dataSource).deleteVersion(mVersion);
        new DeleteManager(dataSource).deleteVersion(rVersion);

        // make sure its gone.
        start = System.currentTimeMillis();
        QueryManager qManager = new QueryManager(dataSource);
        Vector v = qManager.getServices(CUSTOMER_ID, SITE_ID);
        logger.debug("Got services for the customer <" + CUSTOMER_ID
                     + ">, site <" + SITE_ID
                     + "> in " + (System.currentTimeMillis()-start) + "ms");

        assertNotNull("Result of getServices should not be null.", v);
        assertEquals("Result of getServices for should be 0 services.", 0, v.size());

        logger.debug("Update Order: " + (stop-start) + "ms");
    }

    // Complex test: create, get it, compare with expected results.
    public void testQueryStep1() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");
        doTestQueryStep("test-1-new-release.xml", "test-1-query.xml");
    }

    public void testQueryStep2() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");
        doTestQueryStep("test-2-change-release.xml", "test-2-query.xml");
    }

    public void testQueryStep3() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");
        doTestQueryStep("test-3-change-release.xml", "test-3-query.xml");
    }

    public void testQueryStep4() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");
        doTestQueryStep("test-4-change-release.xml", "test-4-query.xml");
    }

    public void testQueryStep5() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");
        doTestQueryStep("test-5-disconnect-release.xml", "test-5-query.xml");
    }

    public void testQueryStep6() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");
        doTestQueryStep("test-6-change-release.xml", "test-6-query.xml");
    }

    public void testQueryStep7() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");
        doTestQueryStep("test-7-new-manage.xml", "test-7-query.xml");
    }

    // Test the case when amount fields have float value with 50 digits after dot
    public void testLongFloat() throws Exception {
        logger.debug("----------------------------------------- Entering -----------------------------------------");

        doTestQueryStep("test-float-new-release.xml", "test-float-query.xml");
    }

    public void doTestQueryStep(String inFile, String outFile) throws Exception {
        long start = 0, stop = 0;

        // Create Version
        Message message = (Message)unmarshaller.unmarshal(new File("../../../config/test/" + inFile));
        VersionType version = (VersionType)message.getVersion().get(0);

        // 1 or 2 versions.
        if (message.getVersion().size() > 1) {
            new UpdateManager(dataSource).updateVersion(version);
            version = (VersionType)message.getVersion().get(1);
        }
        new CreateManager(dataSource).createVersion(version);

        // Retrieve
        start = System.currentTimeMillis();
        Message messageNew = objectFactory.createMessage();
        Notes notes = objectFactory.createNotes();
        Version versionNew = new QueryManager(dataSource).getServiceDetails(version.getCustomerId(),
                                                                            version.getSiteId(),
                                                                            version.getServiceId(),
                                                                            notes);
        messageNew.getVersion().add(versionNew);
        if (notes.getNote().size() > 0)
            messageNew.setNotes(notes);
        stop = System.currentTimeMillis();
        logger.debug("Get Service Details: " + (stop-start) + "ms");

        File outFileF = new File(outFile);
        // Save result
        if (validator.validate(messageNew)) {
            FileOutputStream fos = new FileOutputStream(outFileF);
            DataWriter d = new DataWriter(new OutputStreamWriter(fos),
                                          (String)marshaller.getProperty(Marshaller.JAXB_ENCODING));
            d.setIndentStep(4);

            marshaller.marshal(messageNew, d);
        } else {
            logger.error("Validation of 'message' failed!");
        }

        // Compare with expected result
        Message messageRef = (Message)unmarshaller.unmarshal(outFileF);
        assertTrue("Result differs from expected", MessageCompare.compare(messageRef, messageNew));

            // Retrieve using legacy implementation
//            start = System.currentTimeMillis();
//            Version versionOld = new QueryManagerLegacy(pm).getServiceDetails(version.getCustomerId(),
//                                                                              version.getSiteId(),
//                                                                              version.getServiceId());
//            Message messageOld = objectFactory.createMessage();
//            messageOld.getVersion().add(versionOld);
//            stop = System.currentTimeMillis();
//            logger.debug("Get Service Details Legacy: " + (stop-start) + "ms");
//
//            // Save result
//            if (validator.validate(messageNew)) {
//                FileOutputStream fos = new FileOutputStream(outFile + ".legacy");
//                marshaller.marshal(messageOld, fos);
//            } else {
//                logger.error("Validation of 'message' failed!");
//            }
//
//            // Compare with legacy result
//            MessageCompare.compare(messageRef, messageOld);
    }
}