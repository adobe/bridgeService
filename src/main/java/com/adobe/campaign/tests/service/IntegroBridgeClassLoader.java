package com.adobe.campaign.tests.service;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.adobe.campaign.tests.service.exceptions.NonExistantJavaObjectException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IntegroBridgeClassLoader extends ClassLoader {
    private static final Logger log = LogManager.getLogger();
    private Map<String, Object> callResultCache;

    /**
     * Parent ClassLoader passed to this constructor
     * will be used if this ClassLoader can not resolve a
     * particular class.
     *
     * @param
     */
    public IntegroBridgeClassLoader() {
        super(IntegroBridgeClassLoader.class.getClassLoader());
        this.setCallResultCache(new HashMap<>());
    }

    /**
     * Loads a given class from .class file just like
     * the default ClassLoader. This method could be
     * changed to load the class over network from some
     * other server or from the database.
     *
     * @param name Full class name
     */
    private synchronized  Class getClass(String name)
            throws ClassNotFoundException {

        // is this class already loaded?
        Class cls = findLoadedClass(name);
        if (cls != null) {
            log.debug("class {} has been loaded.",name);
            return cls;
        } else {
            log.debug("class {} has not been loaded. Loading now.", name);
        }


        // We are getting a name that looks like
        // javablogging.package.ClassToLoad
        // and we have to convert it into the .class file name
        // like javablogging/package/ClassToLoad.class
        String file = name.replace('.', '/')
                + ".class";
        byte[] b = null;
        try {
            // This loads the byte code data from the file
            b = loadClassData(file);
            // defineClass is inherited from the ClassLoader class
            // and converts the byte array into a Class
            cls = defineClass(name, b, 0, b.length);
            resolveClass(cls);
            return cls;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Every request for a class passes through this method.
     * If the requested class is in "javablogging" package,
     * it will load it using the
     *  method.
     * If not, it will use the super.loadClass() method
     * which in turn will pass the request to the parent.
     *
     * @param name
     *            Full class name
     */
    @Override
    public Class loadClass(String name)
            throws ClassNotFoundException {
        log.debug("loading class {}",name);

        if (name.startsWith("com.adobe.campaign.") || name.startsWith("utils.") || name.startsWith("testhelper.")) {

            return getClass(name);
        }
        return super.loadClass(name);
    }

    /**
     * Loads a given file (presumably .class) into a byte array.
     * The file should be accessible as a resource, for example
     * it could be located on the classpath.
     *
     * @param name File name to load
     * @return Byte array read from the file
     * @throws IOException Is thrown when there
     *               was some problem reading the file
     */
    private byte[] loadClassData(String name) throws IOException {
        // Opening the file
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(name);
        if (stream==null) {
            throw new NonExistantJavaObjectException("The given class path "+name+" could not be found.");
        }
        int size = stream.available();
        byte buff[] = new byte[size];
        DataInputStream in = new DataInputStream(stream);
        // Reading the binary data
        in.readFully(buff);
        in.close();
        return buff;
    }

    public Map<String, Object> getCallResultCache() {
        return callResultCache;
    }

    public void setCallResultCache(Map<String, Object> callResultCache) {
        this.callResultCache = callResultCache;
    }
}
