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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Hashtable;

import javax.jms.BytesMessage;
import javax.management.Notification;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Category;

import com.equant.csi.common.CSIConstants;
import com.equant.csi.jaxb.Message;
import com.equant.csi.jaxb.ObjectFactory;
import com.equant.csi.jaxb.ServicesType;
import com.equant.csi.utilities.JAXBUtils;
import com.equant.csi.utilities.LoggerFactory;
import com.sun.xml.bind.marshaller.DataWriter;

/**
 * @author Vasyl Rublyov <vasyl.rublyov@equant.com>
 * @version 1.0 - 25.03.2005
 *
 * TODO
 */
public class TestTimeService extends AbstractTimeService {
    /** Initializing the logger. */
    private static final Category logger = LoggerFactory.getInstance("timeservice.TestTimeService");

    public void init(Date kickoff, Long repeat, Hashtable args) {
        
    }
    
    public boolean allowSimultaneousRun() {
        return true;
    }

    public void run(Notification notification, Hashtable args) {
        logger.debug("Invoked TestTimeService.handleNotification at " + new Date());
        ObjectFactory objectFactory = new ObjectFactory();
        try {
            Message msg = objectFactory.createMessage();
            msg.setSiteInformation(objectFactory.createSiteInformationType());
            msg.getSiteInformation().setSourceSystem(CSIConstants.SYSTEM_TYPE_GOLD);

            ServicesType st = objectFactory.createServicesType();
            //st.setServiceName(rs.getString(1));
            msg.getSiteInformation().getServices().add(st);
            
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            
            JAXBContext ctx = JAXBUtils.getContext();
            Marshaller marshaller = ctx.createMarshaller();

            //DataWriter makes output XML well formatted.
            DataWriter writer = new DataWriter(new OutputStreamWriter(byteStream),
                                               (String)marshaller.getProperty(Marshaller.JAXB_ENCODING));
            writer.setIndentStep(4);

            if (ctx.createValidator().validate(msg)) {
                marshaller.marshal(msg, writer);
            }

            writer.flush();
            
            logger.debug("XML: ");
            logger.debug(byteStream.toString());
            
            byteStream.close();
        } catch (JAXBException e) {
            logger.error(e, e);
        } catch (IOException e) {
            logger.error(e, e);
        }
    }
    
}
