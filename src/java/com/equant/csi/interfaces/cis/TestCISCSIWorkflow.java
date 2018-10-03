package com.equant.csi.interfaces.cis;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import com.equant.csi.ejb.biz.ServiceManager;
import com.equant.csi.ejb.biz.ServiceManagerHome;
import com.equant.csi.exceptions.CISException;

/**
 * Created by IntelliJ IDEA.
 * User: szhu
 * Date: Jun 30, 2004
 * Time: 3:48:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestCISCSIWorkflow {
    
    public static String getFileContent(String fileName) {
        StringBuffer result=new StringBuffer();        
        byte[] buf=new byte[500];
        try {
            FileInputStream io=new FileInputStream( fileName);
            for(int l=io.read(buf);l>0;l=io.read(buf)) {
                result.append(new String(buf,0,l));                
            }
        } catch (FileNotFoundException e) {
            System.err.println("File "+ fileName+" not found!");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return result.toString();
    }
    public static void main(String[] args) {

        try {

            if(args.length < 2) {
                System.err.println("You should specify incoming files and host!!!!");
                return;
            }
            Properties p = new Properties();
            // jonas/jms migration
            /*
			p.put("java.naming.factory.initial", "weblogic.jndi.WLInitialContextFactory");
            p.put("java.naming.provider.url", "t3://"+args[0]+":7000");
            p.put("weblogic.jndi.createIntermediateContexts", "true");
			*/
    		p.put(Context.INITIAL_CONTEXT_FACTORY, "org.ow2.carol.jndi.spi.IRMIContextWrapperFactory");
    		p.put(Context.URL_PKG_PREFIXES,"org.ow2.jonas.naming" );
    		p.put(Context.PROVIDER_URL, "rmi://"+args[0]+":1099");

            InitialContext ic = new InitialContext(p);
           //// jonas/jms migration 
          //  ServiceManagerHome home = (ServiceManagerHome) ic.lookup(ServiceManagerHome.class.getName());
            ServiceManagerHome home = (ServiceManagerHome) PortableRemoteObject.narrow(ic.lookup("com.equant.csi.ejb.biz.ServiceManagerHome"), ServiceManagerHome.class);
            ServiceManager ejbServiceManager = home.create();
            for( int i=1; i<args.length;i++){
                String incomingXML=getFileContent(args[i]);
                if(incomingXML.length()>0) {
                    ejbServiceManager.processMessageFromCis(incomingXML);                
                }
            }

        } catch (CISException cise) {
            System.out.println(cise.toString());
        } catch (javax.naming.NamingException jnne) {
            System.out.println(jnne.toString());
        } catch (java.rmi.RemoteException jrre) {
            System.out.println(jrre.toString());
        } catch (javax.ejb.CreateException jece) {
            System.out.println(jece.toString());
        }
    }
}
