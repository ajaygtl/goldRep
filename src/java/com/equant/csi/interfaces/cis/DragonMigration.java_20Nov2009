package com.equant.csi.interfaces.cis;

import com.equant.csi.database.GOLDManager;
import com.equant.csi.database.QueryManager;
import com.equant.csi.database.UpdateManager;
import com.equant.csi.ejb.biz.ServiceManager;
import com.equant.csi.ejb.biz.ServiceManagerHome;
import com.equant.csi.exceptions.CSIException;
import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.ObjectFactory;
import com.equant.csi.jaxb.SiteInformationType;
import com.equant.csi.utilities.LoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.relique.jdbc.csv.CsvReader;

import javax.ejb.CreateException;
import javax.ejb.RemoveException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;
import javax.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


/**
 * Dragon migration script.
 * @author Vadim Gulyakin
 */
public class DragonMigration {
    class ServiceComponent{
        public String customerCode;
        public String oldRouterName;
        public String newRouterName;
        public String oldAccessConnectionUsid;
        public String newAccessConnectionUsid;
        public String oldRouterUsid;
        public String newRouterUsid;
        public String oldBackupUsid;
        public String newBackupUsid;
        public String oldNasBackupUsid;
        public String newNasBackupUsid;

        public String toString() {
            return "|" + customerCode + "|"+oldRouterName + "|" + newRouterName + "|"+oldAccessConnectionUsid + "|"+newAccessConnectionUsid + "|";
        }
    }

    class PVC{
        public String customerCode;
        public String oldPVC;
        public String newPVC;

        public String toString() {
            return "|" + customerCode + "|" + oldPVC + "|" + newPVC + "|";
        }
    }

    protected static final char SEPARATOR = ',';                      // separator for csv files
    protected static final String CSV_DIR = "dragon";                 // direcory name where uploaded csv files are placed
    protected static final String BACKUP_USID_PREFIX = "BKP::";
    protected static final String NASBACKUP_USID_PREFIX = "NASBKP: ";
    protected static final String jndiGOLDDataSourceName = "GOLDDataSource";
    protected static final String jndiCSIDataSourceName = "CsiDataSource";

    protected static final String BOLD_SEPARATOR = "=============================================";
    protected static final String NORM_SEPARATOR = "---------------------------------------------";

    protected final Logger dragon_logger = LoggerFactory.getInstance("dragon_logger");
    protected String file_1 = null;                                 //1-st file name (service components)
    protected String file_2 = null;                                 //1-st file name (PVCs)
    protected ArrayList serviceComponents = new ArrayList();        //list of ServiceComponents
    protected ArrayList pVCs = new ArrayList();                     //list of PVCs
    protected String appPath = null;                                //application path
    protected DataSource sciDataSource = null;
    protected DataSource goldDataSource = null;
    protected Connection csiConnection = null;
    protected Connection goldConnection = null;
    protected FileAppender fileAppender = null;                     // appender for each migration
    protected String fileAppenderName = null;                       // file name of appender for each migration
    protected final DateFormat dtf = new SimpleDateFormat("dd.MMM.yyyy HH:mm:ss");
    protected String currentTimeString = "";                        //dd_MM_yyyy_HH_mm_ss for file names suffix
    protected HashMap m_serviceOptions = null;                        //HashMap for ServiceOption usids accumulating 
    protected HashMap m_migratedSites = null;                        //Set to store migrated sites 

    protected QueryManager m_queryManager;

    public DragonMigration() {
        currentTimeString = dtf.format(Calendar.getInstance().getTime());
        currentTimeString = currentTimeString.replaceAll("[.: /\\-]", "_");
    }


    /**
     * Creates separate log file for each migration.
     */
    public void initLogger() {
        fileAppenderName = getImportPath() + "dragon.log_" + currentTimeString;

        //and appent current date to it name
        try {
            fileAppender = new FileAppender(new PatternLayout("%d (%p) - %m%n"), fileAppenderName);
        } catch (IOException e) {
            dragon_logger.error("Can`t create separate log for new migration. All events will be logged to current file.");
        }

        dragon_logger.addAppender(fileAppender);
        dragon_logger.info(BOLD_SEPARATOR);
        dragon_logger.info("Dragon migration started");
        dragon_logger.info(BOLD_SEPARATOR);
    }

    /**
     * Initializes migration, open database connections.
     * @return <code>true</code> if initialization succeed
     */
    public boolean init(){
        initLogger();
        m_serviceOptions=new HashMap();
        m_migratedSites=new HashMap();

        //get datasources and connections
        try {
            InitialContext ic = new InitialContext();

            goldDataSource = (DataSource) ic.lookup(jndiGOLDDataSourceName);
            sciDataSource = (DataSource) ic.lookup(jndiCSIDataSourceName);
            goldConnection = goldDataSource.getConnection();
            csiConnection = sciDataSource.getConnection();

            m_queryManager = new QueryManager( csiConnection);
        } catch (NamingException e) {
            dragon_logger.fatal(e.toString());
            return false;
        } catch (SQLException e) {
            dragon_logger.fatal(e.toString());
            return false;
        }
        return true;
    }

    /**
     * Load migration records from two .CSV files.
     * @return <code>true</code> if records loaded from files successfully <code>false</code> otherwise
     */
    public boolean getAllDragonMigrationRecords() {
        try {
            String dirToCSVFiles = getImportPath();

            //load ServiceElements
            CsvReader reader = new CsvReader(dirToCSVFiles + file_1, SEPARATOR, false);
            if (reader.getColumnNames().length != 5) {
                dragon_logger.error("ServiceComponents file should contain 5 columns. Migration failed.");
                return false;
            }

            dragon_logger.info("Loading ServiceComponents");
            int row = 0;
            while (reader.next()) {
                try {
                    row++;
                    ServiceComponent sc = new ServiceComponent();
                    sc.customerCode = reader.getColumn(0).trim();
                    sc.oldRouterName = reader.getColumn(1).trim();
                    sc.newRouterName = reader.getColumn(2).trim();
                    sc.oldAccessConnectionUsid = reader.getColumn(3).trim();
                    sc.oldBackupUsid = BACKUP_USID_PREFIX + sc.oldAccessConnectionUsid;
                    sc.newAccessConnectionUsid = reader.getColumn(4).trim();
                    sc.newBackupUsid = BACKUP_USID_PREFIX + sc.newAccessConnectionUsid;
                    dragon_logger.info(sc.toString());
                    serviceComponents.add(sc);
                } catch (ArrayIndexOutOfBoundsException ex) {  //fix CSVLoader bug
                    dragon_logger.error("Error parsing line " + row + " in ServiceComponents file");
                }
            }
            dragon_logger.info(NORM_SEPARATOR);

            if (file_2==null) {
                dragon_logger.info("PVCs file was not supplied for this migration.");
                dragon_logger.info(NORM_SEPARATOR);
                return true;
            }
            //load PVCs
            reader = new CsvReader(dirToCSVFiles + file_2, SEPARATOR, false);
            if (reader.getColumnNames().length != 3) {
                dragon_logger.error("PVCs file should contain 3 columns. Migration failed.");
                return false;
            }
            dragon_logger.info("Loading PVCs");
            row = 0;
            while (reader.next()) {
                try {
                    row++;
                    PVC pvc = new PVC();
                    pvc.customerCode = reader.getColumn(0).trim();
                    pvc.oldPVC = reader.getColumn(1).trim();
                    pvc.newPVC = reader.getColumn(2).trim();
                    dragon_logger.info(pvc.toString());
                    pVCs.add(pvc);
                } catch (ArrayIndexOutOfBoundsException ex) { //fix CSVLoader bug
                    dragon_logger.error("Error parsing line " + row + " in PVCs file");
                }
            }
            dragon_logger.info(NORM_SEPARATOR);

        } catch (Exception e) {
            dragon_logger.fatal("Can`t load input data");
            dragon_logger.fatal(e.toString(), e);
            return false;
        }
        return true;
    }

    /**
     * Tries to find Usid for each router by its name in CSI database.
     * If Usid not found for router or more then one Usid found for router (old or new name)
     * router will not be migrated.
     */
    public void fillUSIDs() {
        for (int i = 0; i < serviceComponents.size(); i++) {
            ServiceComponent sc = (ServiceComponent) serviceComponents.get(i);
            if (StringUtils.isEmpty(sc.oldRouterName) || StringUtils.isEmpty(sc.newRouterName)) {
                continue;
            }

            sc.oldRouterUsid = getRouterUsidByName(sc.oldRouterName);
            if (StringUtils.isNotEmpty(sc.oldRouterUsid)) {
                if (sc.oldRouterUsid.indexOf(',') == -1) { //only one Usid found
                    sc.oldNasBackupUsid = NASBACKUP_USID_PREFIX + sc.oldRouterUsid;
                    sc.newRouterUsid = getRouterUsidByName(sc.newRouterName);
                    if (StringUtils.isNotEmpty(sc.newRouterUsid)) {
                        if (sc.newRouterUsid.indexOf(',') == -1) { //only one Usid found
                            sc.newNasBackupUsid = NASBACKUP_USID_PREFIX + sc.newRouterUsid;
                        } else { //more than one Usid found
                            logErrorMessageForRouterName("End migration of", sc, "NOT migrated.");
                            sc.newRouterUsid = null;
                        }
                    } else { //no Usid found
                        logErrorMessageForRouterName("", sc, "new component not in CSI.");
                        logErrorMessageForRouterName("End migration of", sc, "NOT migrated.");
                    }
                } else { //more than one Usid found
                    logErrorMessageForRouterName("End migration of", sc, "NOT migrated.");
                    sc.oldRouterUsid = null;
                }
            } else { //no Usid found
                logErrorMessageForRouterName("", sc, "old component not in CSI.");
                logErrorMessageForRouterName("End migration of", sc, "NOT migrated.");
            }
        }
    }

    private void logErrorMessageForRouterName(String prefixMessage, ServiceComponent sc, String suffixMessage) {
        dragon_logger.error(prefixMessage + " Router (Cust code=" + sc.customerCode + ", old Name=" + sc.oldRouterName + ", new Name=" + sc.newRouterName + ") - " + suffixMessage);
    }


    /**
     * Seeks for router USID by its name.
     * @param routerName router name
     * @return router Usid or comma separated list of Usids if multiple found
     */
    public String getRouterUsidByName(String routerName){
        PreparedStatement pstmt = null;
        Statement stmt = null;
        ResultSet rs = null;
        String routerUsid = null;
        String qry1 = "select SERVICEATTRIBUTEID from CSERVICEATTRIBUTE WHERE serviceattributename='Router Name' AND value=?";

        try {
            //I did not do it in single query because such query works too slow
            //first we should find SERVICEATTRIBUTEID list for given router name
            pstmt = csiConnection.prepareStatement(qry1);
            pstmt.setString(1, routerName);
            rs = pstmt.executeQuery();

            //then prepare string for IN statement
            if (! rs.next()) {
                return null;
            }

            StringBuffer inStmt = new StringBuffer('\'' + rs.getString(1) +'\'');
            while (rs.next()) {
                inStmt.append(", '");
                inStmt.append(rs.getString(1));
                inStmt.append('\'');
            }

            rs.close();

            String qry2 = " select DISTINCT se.USID                                               " +
                    " from CSERVICECHANGEITEM ch, CVERSIONSERVICEELEMENT v, CSERVICEELEMENT se    " +
                    " WHERE  ch.SERVICEATTRIBUTEID IN (" + inStmt + ") AND                        " +
                    " ch.VERSIONSERVICEELEMENTID = v.VERSIONSERVICEELEMENTID  AND                 " +
                    " v.SERVICEELEMENTID = se.SERVICEELEMENTID AND                                " +
                    " se.SERVICEELEMENTCLASS = 'CPE'                                              ";

            stmt = csiConnection.createStatement();
            rs = stmt.executeQuery(qry2);
            if (rs.next()) {
                routerUsid = rs.getString(1);
                dragon_logger.info("USID " + routerUsid + " found for router " + routerName);
                //check if we have only one usid
                while (rs.next()) {
                    dragon_logger.error("More than one USID found for router " + routerName + " next USID is " + rs.getString(1));
                    routerUsid = routerUsid + "," + rs.getString(1);
                }
            } else {
                dragon_logger.error("USID NOT found for router " + routerName);
                return null;
            }
        } catch (SQLException e) {
            dragon_logger.fatal(e.toString(), e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                    rs = null;
                }
                if (pstmt != null) {
                    pstmt.close();
                    pstmt = null;
                }
                if (stmt != null) {
                    stmt.close();
                    pstmt = null;
                }
            } catch (SQLException e) {
                dragon_logger.debug(e.toString());
            }
            ;
        }
        return routerUsid;
    }

    /**
     * Calls migarteServiceElement for each element of each record.
     */
    public void doMigrate() {
        UpdateManager um = new UpdateManager(csiConnection);
        for (int i = 0; i < serviceComponents.size(); i++) {
            ServiceComponent sc = (ServiceComponent) serviceComponents.get(i);
            migarteServiceElement(um, sc.oldAccessConnectionUsid, sc.newAccessConnectionUsid, "AccessConnection");
            migarteServiceElement(um, sc.oldBackupUsid, sc.newBackupUsid, "Backup");
            migarteServiceElement(um, sc.oldRouterUsid, sc.newRouterUsid, "Router");
            migarteServiceElement(um, sc.oldNasBackupUsid, sc.newNasBackupUsid, "NAS Backup");
        }
        for (int i = 0; i < pVCs.size(); i++) {
            PVC pvc = (PVC) pVCs.get(i);
            migarteServiceElement(um, pvc.oldPVC, pvc.newPVC, "PVC");
        }

        migrateServiceOptions( um);
        
        updateSiteServiceListInGOLD();
    }


    protected void migrateServiceOptions( UpdateManager um) {

        String oldUSID = null;
        String newUSID = null;
        try {
            for(Iterator i=m_serviceOptions.keySet().iterator();i.hasNext();) {
                oldUSID = (String)i.next();
                newUSID = (String)m_serviceOptions.get( oldUSID);

                List activeElements = m_queryManager.getActiveServiceElements( oldUSID);
                if( activeElements.isEmpty()) {
                    migarteServiceElement( um, oldUSID, newUSID, "ServiceOptions");
                } else {
                    dragon_logger.error( "Service Option (old USID=" + oldUSID + ", new USID=" + newUSID + ") - NOT migrated. Exists active compinents at site:");

                    for( int j=0; j<activeElements.size(); j++) {
                        dragon_logger.error( "Component USID=" + activeElements.get( j));
                    }
                }
            }
        } catch( CSIException e) {
            dragon_logger.error( "Service Option (old USID=" + oldUSID + ", new USID=" + newUSID + ") - NOT migrated. Reason: " + e.toString());
        }
    }

    /**
     * Calls <code>UpdateManager.migrateServiceElementsForDragon</code>.
     * @param um  - Update manager instance
     * @param oldUsid - old Usid
     * @param newUsid - new Usid
     * @param elementType - String describing element type (for logging only)
     */
    public void migarteServiceElement(UpdateManager um, String oldUsid, String newUsid, String elementType) {
        if (StringUtils.isEmpty(oldUsid) || StringUtils.isEmpty(newUsid)) return;
        dragon_logger.info(NORM_SEPARATOR);
        dragon_logger.info("Start migration of service element");

        boolean migrated = false;
        try {
            migrated = um.migrateServiceElementsForDragon(oldUsid, newUsid, m_serviceOptions, m_migratedSites, dragon_logger, goldConnection);
        } catch (CSIException e) {
            dragon_logger.error(elementType + " (old USID=" + oldUsid + ", new USID=" + newUsid + ") - NOT migrated. Reason: " + e.toString());
        }

        if (migrated) {
            dragon_logger.info("End migration of " + elementType + " (old USID=" + oldUsid + ", new USID=" + newUsid + ") was successfully migrated.");
        } else {
            dragon_logger.error("End migration of " + elementType + " (old USID=" + oldUsid + ", new USID=" + newUsid + ") - NOT migrated. ");
        }
    }

    /**
     * Update service list for sites in GOLD.
     */
    public void updateSiteServiceListInGOLD() {
        dragon_logger.info("Start updating sites");
        
        if( m_migratedSites.isEmpty()) {
            dragon_logger.info( "List of migrated sites is empty");
            return;
        }

        GOLDManager goldManager = null; 
        ObjectFactory objectFactory = new ObjectFactory();
        try {
            goldManager = new GOLDManager(csiConnection,goldConnection); 

            ServiceManager serviceManager = getServiceManager();
            for(Iterator i=m_migratedSites.keySet().iterator();i.hasNext();) {
                String siteID = (String)i.next();

                //retrieve customerId (index 0 in the array) and serviceId(index 1 in the array)
                String[] siteData = (String[]) m_migratedSites.get( siteID);
                List activeElements = m_queryManager.getActiveServiceElements(
                    siteData[0], siteID, siteData[1]);
                if( activeElements.isEmpty()) {
                    SiteInformationType siteInfo=goldManager.getSiteInfoByID(siteID);
                    if(siteInfo!=null) {
                        Message msg = objectFactory.createMessage();
                        msg.setSiteInformation( m_queryManager.populateServiceListForSite( siteInfo));
                        serviceManager.publishMessage(msg);
                        dragon_logger.debug( "Message for updating service list was sent for site with ID = " + siteID);
                    }else {
                        dragon_logger.debug("SiteInfo is null ID = " + siteID);                    
                    }
                } else {
                    dragon_logger.error( "Message for updating service list for site with ID = " + siteID +
                        " was not sent. Reason: there exist some active components on the site:");

                    for( int j=0; j<activeElements.size(); j++) {
                        dragon_logger.error( "Component USID=" + activeElements.get( j));
                    }
                }
            }
            
            //to enforce call of deinitializeJMS method of ServiceManagerEJB which
            //is called from the ejbRemove method
            serviceManager.remove();
        } catch (CSIException e) {
            dragon_logger.error("Exception occurred while site was updating!",e);
        } catch (JAXBException e) {
            dragon_logger.error("JAXB Exception occurred",e);
        } catch (RemoteException e) {
            dragon_logger.error("Exception occurred while site was updating!",e);
        } catch (RemoveException e) {
            dragon_logger.error("Exception occurred while site was updating!",e);
        }
    }

    private ServiceManager getServiceManager() throws CSIException {
        InitialContext ic;
        try {
            ic = new InitialContext();
            ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup(ServiceManagerHome.class.getName()), ServiceManagerHome.class);
            return home.create();
        } catch (NamingException e) {
            throw new CSIException( e);
        } catch (RemoteException e) {
            throw new CSIException( e);
        } catch (CreateException e) {
            throw new CSIException( e);
        }
    }
    
    
    /**
     * Sets input (uploaded) files names.
     *
     * @param file1Name - ServiceComponents file name
     * @param file2Name - PVCs file name
     */
    public void setFileNames(String file1Name, String file2Name) {
        file_1 = file1Name;
        file_2 = file2Name;
    }

    /**
     * @return absolute path where uploaded files will be saved
     */
    public String getImportPath() {
        String importPath = "";
        FileAppender fa = (FileAppender) dragon_logger.getAppender("dragonlog");
        if (fa != null) {
            File logDir = new File(fa.getFile()).getParentFile();
            if (logDir==null)
                importPath = CSV_DIR + File.separator;
            else
                importPath = logDir.getAbsolutePath() + File.separator + CSV_DIR + File.separator;;

        }
        return importPath;
    }

    /**
     * @return current migration startup time in format dd_MM_yyyy_HH_mm_ss
     */
    public String getCurrentTimeString() {
        return currentTimeString;
    }

    /**
     * @return file name of the <code>FileAppender</code> for current migration
     */
    public String getFileAppenderName() {
        File logFile = new File(fileAppenderName);
        if (logFile.canRead())
            return logFile.getAbsolutePath();
        else
            return null;
    }

    /**
     * Main method for running migration.
     * @return <code>true</code> if migration was successfull <code>false</code> otherwise
     */
    public boolean run() {
        try {
            if (!init()) return false;
            if (!getAllDragonMigrationRecords()) return false;
            fillUSIDs();
            doMigrate();
            dragon_logger.info(BOLD_SEPARATOR);
            dragon_logger.info("Dragon migration completed");
            dragon_logger.info(BOLD_SEPARATOR);
            return true;
        } catch (Exception e) {
            dragon_logger.error("An exception occured during migration", e);
            return false;
        } finally {
            try {
                if (goldConnection != null) goldConnection.close();
                if (csiConnection != null) csiConnection.close();
            } catch (SQLException e) {
                dragon_logger.error("SQLException during close connection: " + e);
            } finally {
                if (fileAppender != null) {
                    dragon_logger.removeAppender(fileAppender);
                    fileAppender.close();
                }
            }
        }
    }


    public static void main(String[] args) {
        DragonMigration dragonMigration = new DragonMigration();
        dragonMigration.run();
    }

}
