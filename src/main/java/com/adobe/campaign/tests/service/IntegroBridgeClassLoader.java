package com.adobe.campaign.tests.service;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.adobe.campaign.tests.service.exceptions.NonExistantJavaObjectException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IntegroBridgeClassLoader extends ClassLoader {
    private static final Logger log = LogManager.getLogger();
    private Map<String, Object> callResultCache;

    private String[] packagePaths;

    /**
     * Parent ClassLoader passed to this constructor
     * will be used if this ClassLoader can not resolve a
     * particular class.
     */
    public IntegroBridgeClassLoader() {
        super(IntegroBridgeClassLoader.class.getClassLoader());
        setPackagePaths(ConfigValueHandler.STATIC_INTEGRITY_PACKAGES.fetchValue());
        this.setCallResultCache(new HashMap<>());
    }

    /**
     * Loads a given class from .class file just like
     * the default ClassLoader. This method could be
     * changed to load the class over network from some
     * other server or from the database.
     *
     * @param in_classPath Full class name
     */
    private synchronized  Class getClass(String in_classPath)
            throws ClassNotFoundException {

        // is this class already loaded?
        Class cls = findLoadedClass(in_classPath);
        if (cls != null) {
            log.debug("class {} has been loaded.",in_classPath);
            return cls;
        } else {
            log.debug("class {} has not been loaded. Loading now.", in_classPath);
        }


        // We are getting a name that looks like
        // javablogging.package.ClassToLoad
        // and we have to convert it into the .class file name
        // like javablogging/package/ClassToLoad.class
        String file = in_classPath.replace('.', '/')
                + ".class";
        byte[] b = null;
        try {
            // This loads the byte code data from the file
            b = loadClassData(file);
            // defineClass is inherited from the ClassLoader class
            // and converts the byte array into a Class
            cls = defineClass(in_classPath, b, 0, b.length);
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
     * @param in_classFullPath
     *            Full class name
     */
    @Override
    public Class loadClass(String in_classFullPath)
            throws ClassNotFoundException {
        log.debug("loading class {}",in_classFullPath);

        if (isClassAmongPackagePaths(in_classFullPath)) {
            return getClass(in_classFullPath);
        }

        return super.loadClass(in_classFullPath);
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

    public String[] getPackagePaths() {
        return packagePaths;
    }

    public void setPackagePaths(String[] packagePaths) {
        this.packagePaths = packagePaths;
    }

    public void setPackagePaths(String in_packagePaths) {
        setPackagePaths(in_packagePaths.contains(",") ? in_packagePaths.split(",") : new String[]{});
    }

    /**
     * Lets us know if the given class path is included in the classes that are to be managed by the class loader
     * @param in_classFullPath
     * @return true if the given class path is among the stored package paths
     */
    public boolean isClassAmongPackagePaths(String in_classFullPath) {
        return Arrays.stream(getPackagePaths()).anyMatch(z -> in_classFullPath.startsWith(z));
    }
}
