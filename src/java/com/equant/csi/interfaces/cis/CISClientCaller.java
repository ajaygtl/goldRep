package com.equant.csi.interfaces.cis;

/**
 * Created by IntelliJ IDEA.
 * User: szhu
 * Date: Sep 18, 2004
 * Time: 12:25:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class CISClientCaller {

    public static void main(String[] args) {
            System.out.println("TODO!!!!");
//        try {
//
//            if (args == null || (args != null && args.length == 0 ) ) {
//                System.exit(1);
//            }
//
//            String fileName = args[0];
//
//            Properties propertyToRun = new Properties();
//            propertyToRun = PropertiesFileLoader.load(fileName);
//            System.out.println("loadProperties");
//
//            T3ServicesDef t3services = null;
//            Properties p = new Properties();
//            p.put("java.naming.factory.initial", "weblogic.jndi.WLInitialContextFactory");
//            p.put("weblogic.jndi.createIntermediateContexts", "true");
//
//            p.put("java.naming.provider.url", propertyToRun.getProperty("java.naming.provider.url"));
//            p.put(Context.SECURITY_PRINCIPAL, propertyToRun.getProperty(Context.SECURITY_PRINCIPAL));
//            p.put(Context.SECURITY_CREDENTIALS, propertyToRun.getProperty(Context.SECURITY_CREDENTIALS));
//
//            InitialContext ic = new InitialContext(p);
//            t3services = (T3ServicesDef) ic.lookup("weblogic.common.T3Services");
//
//            ParamSet schedParams = new ParamSet();
//            schedParams.setParam(CISConstants.PARAM_CIS_NAME_TIME, propertyToRun.getProperty("cistime"));
//            schedParams.setParam(CISConstants.PARAM_CIS_NAME_SKIPDAY, propertyToRun.getProperty("cisskip"));
//            schedParams.setParam(CISConstants.PARAM_CIS_NAME_SWITCH, propertyToRun.getProperty("cisrunswitch"));
//            schedParams.setParam(CISConstants.PARAM_CIS_NAME_RUNTYPE, propertyToRun.getProperty("cisruntype"));
//
//            Scheduler scheduler = new Scheduler("com.equant.csi.interfaces.cis.CISNewScheduler", schedParams);
//
//            ParamSet triggerParams = new ParamSet();
//            triggerParams.setParam(CISConstants.PARAM_CIS_NAME_RUNTYPE, propertyToRun.getProperty("cisruntype"));
//            triggerParams.setParam(CISConstants.PARAM_CIS_NAME_PRODUCTLIST, propertyToRun.getProperty("cisproducttorun"));
//            triggerParams.setParam(CISConstants.PARAM_CIS_NAME_CISDS, propertyToRun.getProperty("jndiCISDataSourceName"));
//            triggerParams.setParam(CISConstants.PARAM_CIS_NAME_CSIDS, propertyToRun.getProperty("jndiLocalCSIDataSourceName"));
//            triggerParams.setParam(CISConstants.PARAM_CIS_NAME_GOLDDS, propertyToRun.getProperty("jndiGOLDDataSourceName"));
//
//            Trigger trigger = new Trigger("com.equant.csi.interfaces.cis.CISNewTrigger", triggerParams);
//
//
//            ScheduledTriggerDef std = t3services.time().getScheduledTrigger(scheduler, trigger);
//            std.setDaemon(true);
//            std.schedule();
//
//        } catch (Exception e) {
//            System.out.println(e.toString());
//        }
    }

}
