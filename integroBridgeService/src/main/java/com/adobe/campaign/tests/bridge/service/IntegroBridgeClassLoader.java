/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.adobe.campaign.tests.bridge.service.exceptions.NonExistentJavaObjectException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IntegroBridgeClassLoader extends ClassLoader {
    private static final Logger log = LogManager.getLogger();
    private Map<String, Object> callResultCache;

    private Set<String> packagePaths;

    /**
     * Parent ClassLoader passed to this constructor
     * will be used if this ClassLoader can not resolve a
     * particular class.
     */
    public IntegroBridgeClassLoader() {
        super(IntegroBridgeClassLoader.class.getClassLoader());
        setPackagePaths(ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.fetchValue());
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
    private synchronized  Class getClass(String in_classPath) {

        // is this class already loaded?
        Class cls = super.findLoadedClass(in_classPath);
        if (cls != null) {
            log.debug("Class {} has already been loaded.",in_classPath);
            return cls;
        } else {
            log.debug("Class {} has not been loaded. Loading now.", in_classPath);
        }


        // We are getting a name that looks like
        // javablogging.package.ClassToLoad
        // and we have to convert it into the .class file name
        // like javablogging/package/ClassToLoad.class
        String file = in_classPath.replace('.', '/')
                + ".class";

        try {
            // This loads the byte code data from the file
            byte[] b = loadClassData(file);
            // defineClass is inherited from the ClassLoader class
            // and converts the byte array into a Class
            cls = defineClass(in_classPath, b, 0, b.length);
            resolveClass(cls);
            return cls;
        } catch (IOException e) {
            log.error("Encountered IOException ",e);
            return null;
        }
    }

    /**
     * Every request for a class passes through this method. We have three rules that apply here:
     * <ol>
     *     <li>In automatic integrity management, we load the accessed classes when needed.</li>
     *     <li>In manual/semi-manual integrity management, we load the class if it is part of the defined packages</li>
     *     <li>Otherwise, and when the class is in the "java" package, we load it using the system class loader</li>
     * </ol>
     * If not, it will use the super.loadClass() method
     * which in turn will pass the request to the parent.
     *
     * @param in_classFullPath
     *            Full class name
     */
    @Override
    public Class loadClass(String in_classFullPath) {
        log.debug("Preparing class {}", in_classFullPath);

        if (ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.is("manual", "semi-manual")) {
            if (isClassAmongPackagePaths(in_classFullPath)) {
                return getClass(in_classFullPath);
            }
        } else if (!in_classFullPath.startsWith("java")) {
            return getClass(in_classFullPath);
        }

        try {
            return super.loadClass(in_classFullPath);
        } catch (ClassNotFoundException cnfe) {
            throw new NonExistentJavaObjectException(
                    "The given class path " + in_classFullPath + " could not be found.", cnfe);
        }
    }

    /**
     * Loads a given file (presumably .class) into a byte array. The file should be accessible as a resource, for
     * example it could be located on the classpath.
     *
     * @param name File name to load
     * @return Byte array read from the file
     * @throws IOException Is thrown when there was some problem reading the file
     */
    private byte[] loadClassData(String name) throws IOException {
        // Opening the file
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(name);
        if (stream == null) {
            throw new NonExistentJavaObjectException("The given class path " + name + " could not be found.");
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

    public Set<String> getPackagePaths() {
        return packagePaths;
    }

    public void setPackagePaths(Set<String> packagePaths) {
        this.packagePaths = packagePaths;
    }

    public void setPackagePaths(String in_packagePaths) {

        setPackagePaths(in_packagePaths.isEmpty() ? new HashSet<>() : new HashSet<>(
                Arrays.asList(in_packagePaths.split(","))));

    }

    /**
     * Lets us know if the given class path is included in the classes that are to be managed by the class loader
     * @param in_classFullPath A full class path (package + Class name)
     * @return true if the given class path is among the stored package paths
     */
    public boolean isClassAmongPackagePaths(String in_classFullPath) {
        return getPackagePaths().stream().anyMatch(in_classFullPath::startsWith);
    }

    /**
     * Checks if a class is loaded by the class loader
     * @param typeName Is the full name of the class that we want to see if it is loaded
     * @return true if the class has been loaded by the IBSClassLoader
     */
    public boolean isClassLoaded(String typeName) {
        return !(findLoadedClass(typeName)==null);
    }
}
