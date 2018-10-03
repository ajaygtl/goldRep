/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/utilities/JAXBUtils.java,v 1.11 2002/12/06 04:49:02 constantine.evenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.utilities;

import com.equant.csi.jaxb.Note;
import com.equant.csi.jaxb.NotesType;
import com.equant.csi.jaxb.ObjectFactory;
import com.equant.csi.jaxb.Message;
import com.sun.xml.bind.marshaller.DataWriter;
import org.apache.log4j.Category;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;

/**
 * Provides useful methods for retrieving the JAXBContext and
 * converting the JAXB objects.
 *
 * @author Vadim Gritsenko
 * @author Vasyl Rublyov
 * @version $Revision: 1.11 $
 */
public class JAXBUtils {

    /** Initializing the logger. */
    private static final Category logger = LoggerFactory.getInstance(JAXBUtils.class.getName());

    private static ObjectFactory objectFactory = new ObjectFactory();

    /**
     * Retrieves the JAXBContext.
     *
     * @return the instance of JAXBContext
     */
    public static JAXBContext getContext() throws JAXBException {
        logger.debug("getting JAXBContext...");
        return JAXBContext.newInstance("com.equant.csi.jaxb");
    }

    /**
     * Helper method. Can be used to easy populate Message with Notes.
     *
     * @param notes the notes holder
     * @param body  the body of note.
     */
    public static void appendNote(NotesType notes, String body) {
        if (notes == null || body == null)
            return;

        try {
            Note note = objectFactory.createNote();
            note.setValue(body);
            notes.getNote().add(note);
        } catch (JAXBException e) {
            logger.warn("Unable to create Note", e);
        }
    }

    public static String validateString(String inp)	{
        StringBuffer retval = new StringBuffer(inp);

        retval = replaceAll(retval, "&",  "&amp;");
        retval = replaceAll(retval, "\"", "&quot;");
        retval = replaceAll(retval, "<",  "&lt;");
        retval = replaceAll(retval, ">",  "&gt;");

        char[] retvalArray = retval.toString().toCharArray();
        for(int i = 0; i < retvalArray.length; i++) {
            Integer intObj = new Integer(retvalArray[i]);
            if(intObj.intValue() > 127) {
                retval = replaceAll(retval, "" + retvalArray[i],  "&#" + intObj.toString() + ";");
                // get the new array
                retvalArray = retval.toString().toCharArray();
            }
        }
        return retval.toString();
    }

    public static StringBuffer replaceAll(StringBuffer string, String token, String replacement) {
        StringBuffer sf = new StringBuffer();
        StringTokenizer st = new StringTokenizer(string.toString(), token, true);
        while (st.hasMoreTokens()) {
            String tmp = st.nextToken();
            if(token.equals(tmp)) {
                sf.append(replacement);
            } else {
                sf.append(tmp);
            }
        }
        return sf;
    }

    public static String marshalMessage(Message msg) throws JAXBException, IOException {
        if(msg!=null) {
	    	JAXBContext ctx = JAXBUtils.getContext();
	        Marshaller marshaller = ctx.createMarshaller();
	
	        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	        //DataWriter makes output XML well formatted.
	        DataWriter writer = new DataWriter(new OutputStreamWriter(byteStream),
	                                           (String)marshaller.getProperty(Marshaller.JAXB_ENCODING));
	        writer.setIndentStep(4);
	
	        if (ctx.createValidator().validate(msg)) {
	            marshaller.marshal(msg, writer);
	        }
	
	        writer.flush();
	
	        String ret = byteStream.toString();
	
	        if (logger.isDebugEnabled()) {
	            logger.debug("XML: \n" + ret);
	        }
	
	        return ret;
        } 
        return null;
    }

    public static Message unmarshalMessage(String xml) throws JAXBException {
        // Unmarshal XML to the JAXB Message object
        byte b[] = xml.getBytes();
        ByteArrayInputStream bis = new ByteArrayInputStream(b, 0, b.length);
        Unmarshaller unmarsh = JAXBUtils.getContext().createUnmarshaller();
        unmarsh.setValidating(true);
        return (Message) unmarsh.unmarshal(bis);
    }

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        String input;
        StringBuffer sb = new StringBuffer();

        while ((input = br.readLine()) != null) {
            System.out.println("[ORIGINAL  LINE]: " + input);
            System.out.println("[VALIDATED LINE]: " + validateString(input));
            sb.append(input).append("\n");
        }

        System.out.println("====================== NOT WHOLE FILE ======================");
        System.out.println("[ORIGINAL  FILE]: \n\n" + sb.toString());
        System.out.println("[VALIDATED FILE]: \n\n" + validateString(sb.toString()));

        br.close();
    }
}