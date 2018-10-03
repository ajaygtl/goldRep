package com.equant.csi.test;

import com.equant.csi.utilities.LoggerFactory;
import com.equant.csi.utilities.JDOStartup;
import com.equant.csi.database.QueryManager;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import junit.framework.TestCase;
import org.apache.log4j.Category;

/**
 * @version CVS $Id: CSILoadTest.java,v 1.1 2002/11/22 04:33:25 vadim.gritsenko Exp $
 */
public class CSILoadTest extends TestCase {
    private static final Category logger = LoggerFactory.getInstance(CSILoadTest.class.getName());

    private static final String customerId = "2346";
    private static final String siteId = "3M Dubai";
    private static final String serviceName = "FRAME_RELAY_PURPLE";

    private static PersistenceManagerFactory factory;

    static {
        factory = JDOStartup.getPersistentManagerFactory();
    }

    private synchronized PersistenceManager getJDOManager() {
        return factory.getPersistenceManager();
    }

    private void releaseJDOManager(PersistenceManager pm) {
        if (pm.currentTransaction().isActive()) {
            pm.currentTransaction().rollback();
        }
        pm.close();
    }

    public CSILoadTest(String name) {
        super(name);
    }

    public void testGetServiceDetailsLoad() throws Exception {
        // TODO
    }
}