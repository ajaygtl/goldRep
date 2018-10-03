/*
 * TODO:
 *
 * GOLD: Global Order Lifecycle Delivery
 * (c) Copyright 2005 Equant corporation.
 * 
 * Vasyl Rublyov <vasyl.rublyov@equant.com>
 *
 */
package com.equant.csi.weblogic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;

//import weblogic.common.T3ServicesDef;
//import weblogic.common.T3StartupDef;
//import weblogic.management.timer.Timer;
import javax.management.timer.Timer;

import com.equant.csi.utilities.LoggerFactory;

/**
 * @author Vasyl Rublyov <vasyl.rublyov@equant.com>
 * @version 1.0 - 25.03.2005
 *
 * TODO
 */
public final class TimerMBeanScheduler implements  NotificationListener {
    private static final String SDF_PATTERN = "yyyy.MM.dd HH:mm:ss z";
    
    /** Initializing the logger. */
    private static final Category logger = LoggerFactory.getInstance("timeservice.TimerMBeanScheduler");

    public static final String NOTIFICATION_TYPE_PROPERTY = "propertyFileMonitor";
    public static final String NOTIFICATION_PROPERTY_FILE_NAME = "properties";
    public static final String DAYS_OF_WEEK[][] = {
            { "SUNDAY", "SUN" },        /* 0 */
            { "MONDAY", "MON" },        /* 1 */
            { "TUESDAY", "TUE" },       /* 2 */
            { "WEDNESDAY", "WED" },     /* 3 */
            { "THURSDAY", "THU" },      /* 4 */
            { "FRIDAY", "FRI" },        /* 5 */
            { "SATURDAY", "SAT" }       /* 6 */
    };
    
 //   private transient T3ServicesDef m_servicesDef = null;
    private transient String m_propertiesFileName = null;
    private transient long m_propertiesLastModified = 0;
    private transient Properties m_properties = null;
    private transient ArrayList m_notifications = null;
    
    

    /* (non-Javadoc)
     * @see weblogic.common.T3StartupDef#startup(java.lang.String, java.util.Hashtable)
     */
/*    public String startup(String name, Hashtable ht) throws Exception {
        try {
            logger.debug("Invoked TimerMBeanScheduler.startup(\"" + name + "\", {" + ht + "})");
            if(!ht.containsKey(NOTIFICATION_PROPERTY_FILE_NAME)) {
                logger.error("Usage: com.equant.csi.weblogic.TimerMBeanScheduler " +
                        "properties={propery file name}"
                );
                throw new IllegalArgumentException("Usage: com.equant.csi.weblogic.TimerMBeanScheduler " +
                        "properties={propery file name}");
            }
            m_propertiesFileName = (String)ht.get(NOTIFICATION_PROPERTY_FILE_NAME);
            reloadProperties();
            reinitializeTimeServices();
        } catch(Exception e) {
            logger.error(e);
            throw e;
        }
        return "ok";
    }*/

    // constructor added for startup class
    public TimerMBeanScheduler(String fileName){
    	logger.debug("####Invoked TimerMBeanScheduler.startup file name ##### "+fileName);
    	try{
    	m_propertiesFileName = fileName;
    	reloadProperties();
		} catch (IllegalArgumentException e) {
			logger.error("####IllegalArgumentException while  calling reloadProperties ##### "+fileName);	
		} catch (IOException e) {
			logger.error("####IOException while calling reloadProperties##### "+fileName);
		}
         try {
			reinitializeTimeServices();
		} catch (IllegalArgumentException e) {
			logger.error("####IllegalArgumentException while  calling reinitializeTimeServices ##### "+fileName);
		} catch (ParseException e) {
			logger.error("####ParseException while  calling reinitializeTimeServices ##### "+fileName);
		} catch (InstantiationException e) {
			logger.error("####InstantiationException while  calling reinitializeTimeServices ##### "+fileName);
		} catch (IllegalAccessException e) {
			logger.error("####IllegalAccessException while  calling reinitializeTimeServices ##### "+fileName);
		} catch (ClassNotFoundException e) {
			logger.error("####ClassNotFoundException while  calling reinitializeTimeServices ##### "+fileName);
		}
    	
    	
    }

    /* (non-Javadoc)
     * @see weblogic.common.T3StartupDef#setServices(weblogic.common.T3ServicesDef)
     */
    

    /* (non-Javadoc)
     * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
     */
    public void handleNotification(Notification notification, Object handback) {
        logger.debug("Invoked TimerMBeanScheduler.handleNotification at " + new Date());
        // properties file check
        synchronized(this) {
            File f = new File(m_propertiesFileName);
            if(f.lastModified() != m_propertiesLastModified) {
                logger.info("Property file " + m_propertiesFileName + " has been modified - reinitialize time services");
                try {
                    reloadProperties(); 
                    reinitializeTimeServices();
                } catch(Exception e) {
                    logger.error("Unable load property file or initialize time services.", e);
                }
            }
        }
    }

    /**
     * parsePeriod
     * 
     * @param id
     * @param period
     * @return
     * @throws NumberFormatException
     */
    protected Long parsePeriod(String id, String period) throws NumberFormatException {
        Long ret = null;
        SimpleDateFormat sdf = new SimpleDateFormat(SDF_PATTERN);
        
        try {
            ret = new Long(Long.parseLong(period));
        } catch(NumberFormatException nfe) {
            // try to parse constants
            if("ONE_SECOND".equalsIgnoreCase(period)) {
                ret = new Long(Timer.ONE_SECOND);
            } else if("ONE_MINUTE".equalsIgnoreCase(period)) {
                ret = new Long(Timer.ONE_MINUTE);
            } else if("ONE_HOUR".equalsIgnoreCase(period)) {
                ret = new Long(Timer.ONE_HOUR);
            } else if("ONE_DAY".equalsIgnoreCase(period)) {
                ret = new Long(Timer.ONE_DAY);
            } else if("ONE_WEEK".equalsIgnoreCase(period)) {
                ret = new Long(Timer.ONE_WEEK);
            } else if("NULL".equalsIgnoreCase(period)) {
                ret = null;
            } else {
                logger.error(id + ".TimeService.Period contains wrong value \"" + period + "\"");
                throw nfe;
            }
        }
        return ret;
    }
    
    /**
     * reloadProperties
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     */
    private synchronized void reloadProperties() throws IOException, IllegalArgumentException {
    	logger.debug("####reloadProperties() starts ####");
        File f = new File(m_propertiesFileName);
        if(!f.exists() || !f.isFile() || !f.canRead()) {
            logger.error("property file " + m_propertiesFileName + " does not exist or unreadable.");
            throw new IllegalArgumentException(
                    "property file " + m_propertiesFileName + " does not exist or unreadable.");
        }
        m_propertiesLastModified = f.lastModified();
        if(m_properties!=null) {
            m_properties.clear();
        } else {
            m_properties = new Properties();
        }
        FileInputStream fis = new FileInputStream(m_propertiesFileName);
        m_properties.load(fis);
        fis.close();
        logger.debug("####reloadProperties() ends #####");
    }
    
    private void registerTimeService(String id, String classname, Date date, Long period, 
            NotificationListener listener, Hashtable args) {
    	logger.debug("#####registerTimeService() starts #####");
        String classid = id + "." + classname;
        logger.info("Setting " + classname + " TimeService to execute at " +
                date + " and with period " + period+ " msec");

        // Instantiating the Timer MBean
        logger.debug("Instantiating the Timer MBean for " + classid);
        Timer timer = new Timer();
        
        logger.debug("Registering this class as a listener for " + classid);
        // Registering this class as a listener
        timer.addNotificationListener(listener, null, args);
        Integer notificationid;
        if(period!=null) {
            notificationid = timer.addNotification(classid, null, listener, date, period.longValue());
        } else {
            notificationid = timer.addNotification(classid, null, listener, date);
        }
        m_notifications.add(new NotificationData(classid, notificationid, timer));
        logger.debug("####registerTimeService() ends#####");
    }
    /**
     * setTimeService
     * 
     * @param id
     * @param timer
     * @throws IllegalArgumentException
     * @throws ParseException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    private void setTimeService(String id) throws IllegalArgumentException, ParseException, 
            InstantiationException, IllegalAccessException, ClassNotFoundException {
    	logger.debug("####setTimeService starts ####");
        String classname = (String)m_properties.get(id + ".TimeService.ClassName");
        String date = (String)m_properties.get(id + ".TimeService.Date");
        String period = (String)m_properties.get(id + ".TimeService.Period");
        String daysOfWeek = StringUtils.trimToNull((String)m_properties.get(id + ".TimeService.DaysOfWeek"));
        
        if(classname==null || date==null || period==null) {
            logger.error(id + ".TimeService.ClassName, " + id + ".TimeService.Date, " + id + ".TimeService.Period - " +
                    "are required parameters");
            throw new IllegalArgumentException(id + ".TimeService.ClassName, " + 
                    id + ".TimeService.Date, " + id + ".TimeService.Period - " +
                    "are required parameters");
        }

        Hashtable ht = new Hashtable();
        for(Iterator iter = m_properties.keySet().iterator();iter.hasNext();) {
            String key = ((String)iter.next()).trim();
            if(key.length()>2) {
                StringTokenizer st = new StringTokenizer(key, ".");
                String id2 = null, constant = null, param = null, ext = null;
                if(st.hasMoreTokens()) id2 = st.nextToken();
                if(st.hasMoreTokens()) constant = st.nextToken();
                if(st.hasMoreTokens()) param = st.nextToken();
                if(st.hasMoreTokens()) ext = st.nextToken();
                
                if(id.equalsIgnoreCase(id2) && "Arg".equalsIgnoreCase(param)) {
                    String val = (String)m_properties.get(key);
                    ht.put(ext, val);
                }
            }
        }
        if(logger.isDebugEnabled()) {
            logger.debug("daysOfWeek = " + daysOfWeek);
        }
        BitSet daysToRun = new BitSet(7); 
        if(daysOfWeek==null) {
            daysToRun.set(0,7);
        } else {
            daysToRun.clear();
            parseDaysOfWeekToken(daysOfWeek, daysToRun);
        }
        SimpleDateFormat sdf = new SimpleDateFormat(SDF_PATTERN);
        AbstractTimeService service = (AbstractTimeService)Class.forName(classname).newInstance();
        Date kickoff = sdf.parse(date.trim());
        Long repeat = parsePeriod(id, period.trim());
        
        if(logger.isDebugEnabled()) {
            logger.debug(id + ".TimeService.DaysOfWeek = " + daysToRun.toString());
        }
        ht.put("TimeService.DaysOfWeek", daysToRun);
        
        service.init(kickoff, repeat, ht);
        
        registerTimeService(id, classname.trim(), kickoff, repeat, service, ht);
        
        logger.debug("####setTimeService ends#####");
    }

    private void parseDaysOfWeekToken(String token, BitSet result) throws IllegalArgumentException {
        int index;
        int each=1;
        try {
            if(token.equals("*")) {
                result.set(0,7);
                return;
            }
            index = token.indexOf(",");
            if(index > 0) {
                StringTokenizer tokenizer = new StringTokenizer(token, ",");
                while(tokenizer.hasMoreTokens()) {
                    parseDaysOfWeekToken(tokenizer.nextToken(), result);
                }
                return;
            }
            index = token.indexOf("-");
            if(index > 0) {
                int start = parseDayOfWeekValue(token.substring(0, index));
                int end = parseDayOfWeekValue(token.substring(index + 1));
                if(start>end) {
                    int tmp=end;
                    end=start;
                    start=tmp;
                }
                for(int j=start; j<=end; j+=each) {
                    result.set(j);
                }
                return;
            }
            int iValue = parseDayOfWeekValue(token);
            result.set(iValue);
        } catch(IllegalArgumentException e) {
            logger.error("Smth was wrong with " + token);
            throw e;
        }
    }
    
    private int parseDayOfWeekValue(String value) throws IllegalArgumentException {
        String token = StringUtils.trimToNull(value);
        if(token!=null) {
            try {
                return Integer.parseInt(token);
            } catch(NumberFormatException nfe) {
                for(int i=0;i<7;i++) {
                    if(DAYS_OF_WEEK[i][0].equalsIgnoreCase(token) || DAYS_OF_WEEK[i][1].equalsIgnoreCase(token)) {
                        return i;
                    }
                }
            }
            
        }
        throw new IllegalArgumentException("DayOfWeek " + value + " is not supported.");
    }
    
    /**
     * reinitializeTimeServices
     * 
     * @throws IllegalArgumentException
     * @throws ParseException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    private synchronized void reinitializeTimeServices() throws IllegalArgumentException, ParseException, 
            InstantiationException, IllegalAccessException, ClassNotFoundException {
    	logger.debug("####reinitializeTimeServices() starts ####"+m_properties);
        if(m_notifications!=null && !m_notifications.isEmpty()) {
            for(Iterator iter=m_notifications.iterator();iter.hasNext();) {
                NotificationData data = (NotificationData)iter.next();
                data.stop();
            }
            m_notifications.clear();
        } else {
            m_notifications = new ArrayList();
        }
        
        registerTimeService("X", getClass().getName(), new Date((new Date()).getTime() + 5000L), 
                new Long(Timer.ONE_MINUTE), this, null);
        
        if(m_properties!=null && m_properties.size() > 0) {
            for(Iterator iter = m_properties.keySet().iterator();iter.hasNext();) {
                String key = ((String)iter.next()).trim();
                if(key.length()>2) {
                    StringTokenizer st = new StringTokenizer(key, ".");
                    String id1 = null, constant = null, param = null;
                    if(st.hasMoreTokens()) id1 = st.nextToken();
                    if(st.hasMoreTokens()) constant = st.nextToken();
                    if(st.hasMoreTokens()) param = st.nextToken();
                    
                    if("TimeService".equalsIgnoreCase(constant) && "Active".equalsIgnoreCase(param)) {
                        String val = (String)m_properties.get(key);
                        if("yes".equalsIgnoreCase(val)) {
                            setTimeService(id1);
                        }
                    }
                }
            }
        }
        
        logger.debug("Starting Time Services.");
        for(Iterator iter = m_notifications.iterator();iter.hasNext();) {
            NotificationData d = (NotificationData)iter.next();
            d.start();
        }
        
        logger.debug("####reinitializeTimeServices() ends ####");
    }
    
 public synchronized void stopTimeServices() throws IllegalArgumentException, ParseException, 
    InstantiationException, IllegalAccessException, ClassNotFoundException {
	  logger.debug("#######stopTimeServices###");
  if(m_notifications!=null && !m_notifications.isEmpty()) {
    for(Iterator iter=m_notifications.iterator();iter.hasNext();) {
        NotificationData data = (NotificationData)iter.next();
        logger.debug("###data.stop(###");
        data.stop();
    }
    m_notifications.clear();
} else {
    m_notifications = new ArrayList();
}
    }
    
}
