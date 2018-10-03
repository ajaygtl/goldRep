package com.equant.csi.interfaces.cis;

/**
 * Created by IntelliJ IDEA.
 * User: szhu
 * Date: Jul 9, 2004
 * Time: 11:11:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class CISConstants {
    public static final int CIS_EXTRACTTYPE_DAY = 0;
    public static final int CIS_EXTRACTTYPE_WEEK = 1;
    public static final int CIS_EXTRACTTYPE_FULL = 2;

    public static final int CIS_EXTRACTSTATUS_SUCCESS = 0;
    public static final int CIS_EXTRACTSTATUS_FAILED = 1;
    public static final String CIS_EXTRACTMSG_SEPARATER = "|";

    public static final long CIS_DAY_DELTA = 24 * 60 * 60 * 1000;
    public static final long CIS_WEEK_DELTA = 7 * 24 * 60 * 60 * 1000;

    public static final String CIS_CSI_PROGUSER = "CISADM";
    // cis migration
	//public static final String SYBASE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.S";
    public static final String SYBASE_DATE_FORMAT = "MM/dd/yyyy HH:mm:ss";

    //cis status
    public static final String CIS_STATUS_ORDERED = "Ordered";
    public static final String CIS_STATUS_ONLINE = "Online";
    public static final String CIS_STATUS_CEASED = "Ceased";
    public static final String CIS_STATUS_CANCELLED = "Cancelled";
    public static final String CIS_SUB_STATUS_STABLE = "Stable";
    public static final String CIS_SUB_STATUS_MODIF = "Modifying";
    public static final String CIS_SUB_STATUS_CEASING = "Ceasing";

    //cis router status
    public static final String CIS_ROUTER_STATUS_MIG = "MIG";
    public static final String CIS_ROUTER_STATUS_ONHOLD = "ON-HOLD";
    public static final String CIS_ROUTER_STATUS_PLA = "PLA";
    public static final String CIS_ROUTER_STATUS_CFG = "CFG";
    public static final String CIS_ROUTER_STATUS_INT = "INT";
    public static final String CIS_ROUTER_STATUS_OL = "O/L";
    public static final String CIS_ROUTER_STATUS_OUT = "OUT";

    //use for mapping load
    public static final String CIS_CSV_DRIVER = "org.relique.jdbc.csv.CsvDriver";
    public static final String CIS_CSV_URL_PREFIX = "jdbc:relique:csv:";
    public static final String CIS_ORACLE_DRIVER = "oracle.jdbc.driver.OracleDriver";
    public static final String CIS_ORACLE_URL_PREFIX = "jdbc:oracle:thin:@";

    public static final String CIS_PRODUCT_ATM_AC = "ATM";
    public static final String CIS_PRODUCT_ATM_PVC = "ATMPVC";
    public static final String CIS_PRODUCT_FR_AC = "FR";
    public static final String CIS_PRODUCT_FR_PVC = "FRPVC";
    public static final String CIS_PRODUCT_LANAS = "LANAS";
    public static final String CIS_PRODUCT_MANAGED_CPE = "Managed CPE";
    public static final String CIS_PRODUCT_IPVPN = "IP VPN";
    public static final String CIS_PRODUCT_INTERNETDIRECT = "Internet Direct";
    public static final String CIS_PRODUCT_INTRANETCONNECT = "IntranetConnect";

    public static final String CSI_PRODUCT_ATM_PURPLE = "ATM_PURPLE";
    public static final String CSI_PRODUCT_ATM_GREEN = "ATM_GREEN";
    public static final String CSI_PRODUCT_ATM_BLUE = "ATM";
    public static final String CSI_PRODUCT_FR_PURPLE = "FRAME_RELAY_PURPLE";
    public static final String CSI_PRODUCT_FR_GREEN = "FRAME_RELAY_GREEN";
    public static final String CSI_PRODUCT_FR_BLUE = "FRAME_RELAY";
    public static final String CSI_PRODUCT_LANAS = "LAN_ACCESS";
    public static final String CSI_PRODUCT_MANAGED_CPE = "MANAGED_ROUTER_CPE_LANAS";
    public static final String CSI_PRODUCT_IPVPN = "IP_VPN";
    public static final String CSI_PRODUCT_INTERNETDIRECT = "INTERNET_DIRECT_PURPLE";
    public static final String CSI_PRODUCT_INTRANETCONNECT = "INTRANET_CONNECT";

    public static final String CIS_PRODUCT_ORIG_PURPLE = "Equant";
    public static final String CIS_PRODUCT_ORIG_BLUE = "Ex_Equant";
    public static final String CIS_PRODUCT_ORIG_GREEN = "Ex_GO";

    public static final String CIS_TABLENAME_AC = "MTN_ACCESS";
    public static final String CIS_TABLENAME_PVC = "MTN_MESHING";
    public static final String CIS_TABLENAME_IP_AC = "IP_ACCESS";
    public static final String CIS_TABLENAME_IP_PVC = "IP_MESHING";
    public static final String CIS_TABLENAME_IP_ROUTER = "IP_ROUTER";
    public static final String CIS_TABLENAME_IP_CHASSIS = "IP_CHASSIS_MODULE";
    public static final String CIS_TABLENAME_IP_NASBACKUP = "IP_NAS_BACKUP";

    public static final String CIS_PRODUCT_DELIM = "|" ;

    public static final String IDS_CIS_DATASOURCE_JNDI_KEY = "jndiCISDataSourceName";
    public static final String IDS_LOCAL_CSI_DATASOURCE_JNDI_KEY= "jndiLocalCSIDataSourceName";
    public static final String IDS_GOLD_DATASOURCE_JNDI_KEY = "jndiGOLDDataSourceName";
    public static final String IDS_CIS_DAY_SWITCH = "cisdayrunswitch";
    public static final String IDS_CIS_DAY_INITIAL_TIME = "cisdaytime";
    public static final String IDS_CIS_DAY_RUNTYPE = "cisdayruntype";
    public static final String IDS_CIS_DAY_SKIP = "cisdayskip";
    public static final String IDS_CIS_WEEK_SWITCH = "cisweekrunswitch";
    public static final String IDS_CIS_WEEK_INITIAL_TIME = "cisweektime";
    public static final String IDS_CIS_WEEK_RUNTYPE = "cisweekruntype";
    public static final String IDS_CIS_WEEK_SKIP = "cisweekskip";
    public static final String IDS_CIS_DAY_PRODUCT_TO_RUN = "cisdayproducttorun";
    public static final String IDS_CIS_WEEK_PRODUCT_TO_RUN = "cisweekproducttorun";
    public static final String IDS_CIS_DAY_DELTA = "cisdaydelta";
    public static final String IDS_CIS_WEEK_DELTA = "cisweekdelta";

    //public static final String PARAM_CIS_NAME_RUNTYPE = "runtype";
    //public static final String PARAM_CIS_NAME_PRODUCTLIST = "productlist";
    //public static final String PARAM_CIS_NAME_TIME = "time";
    //public static final String PARAM_CIS_NAME_SKIPDAY = "skipday";
    //public static final String PARAM_CIS_NAME_SWITCH = "switch";
    //public static final String PARAM_CIS_NAME_DELTA = "delta";
    //public static final String PARAM_CIS_NAME_CISDS = "cisdatasource";
    //public static final String PARAM_CIS_NAME_CSIDS = "csidatasource";
    //public static final String PARAM_CIS_NAME_GOLDDS = "golddatasource";
    
    public static final String ARG_CIS_NAME_PRODUCTLIST = "PRODUCT_LIST";
    public static final String ARG_CIS_NAME_CISDS = "CIS_DATA_SOURCE";
    public static final String ARG_CIS_NAME_CSIDS = "CSI_DATA_SOURCE";
    public static final String ARG_CIS_NAME_GOLDDS = "GOLD_DATA_SOURCE";

    public static final String CIS_ATTR_SPEED = " Kbps";
    public static final String NEWLINE = "\r\n";

}
