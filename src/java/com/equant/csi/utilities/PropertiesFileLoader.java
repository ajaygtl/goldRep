/*
 * $IonIdea: eqt/csi/src/java/com/equant/csi/utilities/PropertiesFileLoader.java,v 1.9 2002/12/05 15:57:39 victoria.ovdenko Exp $
 *
 * CSI: Customer Service Inventory project.
 * (c) Copyright 2002 Equant corporation.
 */
package com.equant.csi.utilities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads a properties file.
 * It executed by finding it in the class path of the given or assumed ClassLoader.
 *
 * @author Vadim Gritsenko
 * @version $Revision: 1.9 $
 */
public class PropertiesFileLoader {

    /**
     * Loads the specified properties file. Conveniance method that
     * can be used if there are no special ClassLoader considerations.
     * This works, if the PropertyFileLoader class is loaded with the
     * same ClassLoader as the client using this method.
     *
     * @param file the name of the properties file
     * @return the loaded properties
     *
     * @throws IOException if the properties file couldn't be loaded
     */
    public static Properties load(String file)
            throws IOException {

        return load(file, PropertiesFileLoader.class.getClassLoader(), null);
    }

    /**
     * Loads the specified properties file by finding it along the
     * class path of the given ClassLoader.
     *
     * @param file     the name of the properties file
     * @param loader   the class loader used to find the properties file
     * @param defaults default properties used for creating the
     *                 returned properties. If <code>null</code>,
     *                 there are no defaults.
     * @return the properties
     *
     * @throws IOException if the properties file couldn't be loaded
     */
    public static Properties load(String file, ClassLoader loader, Properties defaults)
            throws IOException {

        InputStream stream;
        Properties props = null;

        if (loader == null) {
            // The class loader for classes loaded by the system
            // class loader may be represented as null. Yes, this
            // is very dumb and not consistant and needs to be
            // fixed!
            stream = ClassLoader.getSystemResourceAsStream(file);
        } else {
            stream = loader.getResourceAsStream(file);
        }
        if (stream == null) {
            throw new FileNotFoundException("Unable to locate properties file. (" + file + ")");
        } else {
            if (defaults == null) {
                props = new Properties();
            } else {
                props = new Properties(defaults);
            }
            props.load(stream);
        }

        return props;
    }
}
