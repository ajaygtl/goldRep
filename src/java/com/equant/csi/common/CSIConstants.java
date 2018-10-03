/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/common/CSIConstants.java,v 1.10 2002/12/05 15:57:38 victoria.ovdenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.common;

import java.util.List;

/**
 * Class defines the constant values of order types,
 * order statuses and states of the <code>VersionServiceElement</code>
 * JDO object which can be accepted by the system.
 *
 * @author pathurik
 * @author Vadim Gritsenko
 * @version $Revision: 1.10 $
 */
public interface CSIConstants {

    /** The constant defines the 'Modification' element type. */
    public final static String ELEMENT_TYPE_MODIFICATION = "Modification";

    /** The constant defines the 'Creation' element type. */
    public final static String ELEMENT_TYPE_CREATION = "Creation";

    /** The constant defines the 'Disconnect' element type. */
    public final static String ELEMENT_TYPE_DISCONNECT = "Disconnect";

    //
    // Order types which can be accepted by the system
    //

    /** The constant defines the 'New' order type. */
    public final static String ORDER_TYPE_NEW = "New";

    /** The constant defines the 'Change' order type. */
    public final static String ORDER_TYPE_CHANGE = "Change";

    /** The constant defines the 'Disconnect' order type. */
    public final static String ORDER_TYPE_DISCONNECT = "Disconnect";

    /** The constant defines the 'Existing' order type. */
    public final static String ORDER_TYPE_EXISTING = "Existing";



    //
    // Order statuses which can be accepted by the system
    //

    /** The constant defines the 'Release' order status. */
    public final static String STATUS_ORDER_RELEASE = "ReleaseOrder";

    /** The constants defines the 'Manage' order status. */
    public final static String STATUS_ORDER_MANAGE = "ManageOrder";

    /** The constants defines the 'Cancel' order status. */
    public final static String STATUS_ORDER_CANCEL = "CancelOrder";

    /** The constants defines the 'Un-Cancel' order status. */
    public final static String STATUS_ORDER_UNCANCEL = "UnCancelOrder";

    /** The constants defines the 'Rollback' order status. */
    public final static String STATUS_ORDER_ROLLBACK = "Rollback";

    /** The constants defines the 'Modify with Price Impact' order status. */
    public final static String STATUS_MODIFY_WITH_PRICE_IMPACT = "ModifyPriceImpact";

    /** The constants defines the 'Modify without Price Impact' order status. */
    public final static String STATUS_MODIFY_WITHOUT_PRICE_IMPACT = "ModifyWithoutPriceImpact";

    /** The constants defines the 'Customer Accepts Service' order status. */
    public final static String STATUS_CUSTOMER_ACCEPTS_SERVICE = "CustomerAcceptsService";

    //
    // States of the VersionServiceElement
    //

    /**
     * The constant defines the 'In Progress' state of the
     * <code>VersionServiceElement</code> JDO object.
     *
     */
    public final static String STATUS_INPROGRESS = "InProgress";

    /**
     * The constant defines the 'In Progress No Current' state of the
     * <code>VersionServiceElement</code> JDO object.
     *
     */
    public final static String STATUS_INPRONOCURR = "InProgressNoCurrent";

    /**
     * The constant defines the 'Current' state of the
     * <code>VersionServiceElement</code> JDO object.
     *
     */
    public final static String STATUS_CURRENT = "Current";

    /**
     * The constant defines the 'Disconnect' state of the
     * <code>VersionServiceElement</code> JDO object.
     *
     */
    public final static String STATUS_DISCONNECT = "Disconnect";

    /**
     * The constant defines the 'Delete' state of the
     * <code>VersionServiceElement</code> JDO object.
     *
     */
    public final static String STATUS_DELETE = "Delete";


    //
    // Types of Service Elemet
    //
    public final static String SERVICE_ELEMENT_SO = "ServiceOptions";

    /** Types of Service element" */
    public final static String SERVICE_ELEMENT_CLASS_AC = "AccessConnection";

    //
    // Constants for Sequences.
    //

    /**
     * Sequence name for table cVersionServiceElement
     */
    public final static String SEQ_VERSION_SERVICE_ELEMENT = "seqVersionServiceElement";

    /**
     * Sequence name for table cVersionServiceElementSts
     */
    public final static String SEQ_VERSION_SERVICE_ELEMENT_STS = "seqVersionServiceElementSts";

    /**
     * Sequence name for table cChargeChangeItem
     */
    public final static String SEQ_CHARGE_CHANGE_ITEM = "seqChargeChangeItem";

    /**
     * Sequence name for table cUsageCharge
     */
    public final static String SEQ_USAGE_CHARGE = "seqUsageCharge";

    /**
     * Sequence name for table cRecurringCharge
     */
    public final static String SEQ_RECURRING_CHARGE = "seqRecurringCharge";

    /**
     * Sequence name for table cServiceElement
     */
    public final static String SEQ_SERVICE_ELEMENT = "seqServiceElement";

    /**
     * Sequence name for table cOnceOffCharge
     */
    public final static String SEQ_ONCE_OFF_CHARGE = "seqOnceOffCharge";

    /**
     * Sequence name for table cVersion
     */
    public final static String SEQ_VERSION = "seqVersion";

    /**
     * Sequence name for table cServiceAttribute
     */
    public final static String SEQ_SERVICE_ATTRIBUTE = "seqServiceAttribute";

    /**
     * Sequence name for table cServiceChangeItem
     */
    public final static String SEQ_SERVICE_CHANGE_ITEM = "seqServiceChangeItem";

    /**
     * Sequence name for table cRefType
     */
    public final static String SEQ_REF_TYPE = "seqRefType";

    /**
     * Sequence name for table cSystemUser
     */
    public final static String SEQ_SYSTEM_USER = "seqSystemUser";

    /**
     * CSP Product name
     */
    public static final String CONFIG_CSP_PURPLE = "CSP_PURPLE";

    /**
     * CSP Class name for component
     */
    public static final String CONFIG_CSP_SERVICE_CLASS = "CSP";
    public static final String CONFIG_CSP_SERVICE = "Global CSP Service";


    /**
     * CSP Global <-> Site level mapping
     */
    public static final String GLOBAL_CSP_FM = "Global CSP Fault Management";
    public static final String GLOBAL_CSP_SM = "Global CSP Service Management";
    public static final String GLOBAL_CSP_PM = "Global CSP Project Management";

    public static final String SITE_CSP_FM = "CSP Fault Management";
    public static final String SITE_CSP_SM = "CSP Service Management";
    public static final String SITE_CSP_PM = "CSP Project Management";

    /**
     * CSP Service value <-> Service handle mapping
     */
    public static final String CSP_PURPLE_ATM = "ATM";
    public static final String CSP_PURPLE_CONTACT_CENTER = "Contact Center";
    public static final String CSP_PURPLE_FR = "Frame Relay";
    public static final String CSP_PURPLE_IP_VPN = "IP VPN";
    public static final String CSP_PURPLE_IP_VIDEO = "IP Videoconferencing";
    public static final String CSP_PURPLE_LAN_ACCESS = "LAN Access";
    public static final String CSP_PURPLE_VOICE_VPN = "Voice VPN";
    public static final String CSP_PURPLE_INTERNET_DIRECT = "Internet Direct";
    public static final String CSP_PURPLE_INTRANET_CONNECT = "Intranet Connect";

    public static final String SITE_HANDLE_ATM = "ATM_PURPLE";
    public static final String SITE_HANDLE_CONTACT_CENTER = "Contact Center";
    public static final String SITE_HANDLE_FR = "FRAME_RELAY_PURPLE";
    public static final String SITE_HANDLE_IP_VPN = "IP_VPN";
    public static final String SITE_HANDLE_IP_VIDEO = "IP Videoconferencing";
    public static final String SITE_HANDLE_LAN_ACCESS = "MANAGED_ROUTER_CPE_LANAS";
    public static final String SITE_HANDLE_VOICE_VPN = "Voice VPN";

    /**
     * "Blue" <-> "Purple" products mapping (for CSP)
     */

    public static final String CSP_COLOR_BLUE_ATM = "ATM";
    public static final String CSP_COLOR_BLUE_FRAME_RELAY = "FRAME_RELAY";
    public static final String CSP_COLOR_BLUE_LAN_ACCESS = "LAN_ACCESS";

    public static final String CSP_COLOR_PURPLE_ATM = "ATM_PURPLE";
    public static final String CSP_COLOR_PURPLE_FRAME_RELAY = "FRAME_RELAY_PURPLE";
    public static final String CSP_COLOR_PURPLE_LAN_ACCESS = "MANAGED_ROUTER_CPE_LANAS";

    /**
     * CSP global update (flag), should update all site level ServiceElements
     */
    public static final String CONFIG_CSP_GLOBAL_UPDATE_FLAG = "Global CSP Site Update Flag";

    //Constants for CIS to CSI:
    //System type for Service Components
    public static final String SYSTEM_TYPE_GOLD="GOLD";
    public static final String SYSTEM_TYPE_CIS="CIS";

//    public static final int CSI_EXCEPTION_REPORT_LIMIT = 30;
    public static final int CSI_EXCEPTION_REPORT_LIMIT = 30000;

    public static final String CSI_EXCEPTION_REPORT_EXCEPTIONREPORT ="ExceptionReport";
    public static final String CSI_EXCEPTION_REPORT_REGULARREPORT ="RegularReport";

    public static final int BYTES_BUFFER_SIZE  = 2048;

    public static final String SECLASS_ACCESSCONNECTION = "AccessConnection";
    public static final String SECLASS_TRANSPORT = "Transport";
    public static final String SECLASS_CPE = "CPE";
    public static final String SECLASS_BACKUP = "BackupOptions";
    public static final String SECLASS_SERVICEOPTIONS = "ServiceOptions";
    public static final String SECLASS_NASBACKUP = "NASBackup";

    public static final String ARG_CSI_EXCETION_REPORT_STORAGE = "STORAGE";

}