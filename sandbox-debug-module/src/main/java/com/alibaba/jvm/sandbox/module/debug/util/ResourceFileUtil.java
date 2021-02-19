package com.alibaba.jvm.sandbox.module.debug.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ResourceFileUtil {
    public static final String CLASSPATH_URL_PREFIX = "classpath:";
    public static final String ETC_CONFIG_DIR = "/oneapm/etc/alert";

    private ResourceFileUtil() {
        // No constructor
    }

    public static String load(String path) throws URISyntaxException, IOException {
        URI u;
        if (path.startsWith(CLASSPATH_URL_PREFIX)) {
            u = getURL(path).toURI();
        } else {
            String schema = "file:";
            if (!path.startsWith(schema)) {
                path = schema + path;
            }
            u = new URI(path);
        }
        return IOUtils.toString(u);
    }


    public static String getExternalConfigFilePath(String sysConfigDir, String file) {

        File sysConfigDirFile = new File(sysConfigDir);
        if (sysConfigDirFile.exists() && sysConfigDirFile.isDirectory()) {
            File configFile = new File(sysConfigDir + "/" + file);
            if (configFile.exists() && configFile.isFile()) {
                return configFile.getAbsolutePath();
            }
        }

        return getDefaultConfigFilePath() + file;
    }

    /**
     * @return
     */
    public static String getDefaultConfigFilePath() {
        File jarPath = new File(ResourceFileUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        return jarPath.getParentFile().getAbsolutePath() + "/../config/";
    }

    public static String getConfigFile(String sysConfigDir, String file) {
        String propertiesFile = getExternalConfigFilePath(sysConfigDir, file);
        if (!new File(propertiesFile).exists())
            propertiesFile = "classpath:" + file;
        return propertiesFile;
    }

    /**
     * read external configuration files.
     * first, attempt to read config file from ETC_CONFIG_DIR,
     * if not exits in ETC_CONFIG_DIR, then read from INSTALL_DIR/config/
     *
     * @param file
     * @return
     */
    public static String getConfigFile(String file) {
        return getConfigFile(ETC_CONFIG_DIR, file);
    }

    public static URL getURL(String resourceLocation) throws FileNotFoundException {
        if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
            String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
            ClassLoader cl = getDefaultClassLoader();
            URL url = cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path);
            if (url == null) {
                String description = "class path resource [" + path + "]";
                throw new FileNotFoundException(description + " cannot be resolved to URL because it does not exist");
            }
            return url;
        }
        try {
            // try URL
            return new URL(resourceLocation);
        } catch (MalformedURLException ex) {
            // no URL -> treat as file path
            try {
                return new File(resourceLocation).toURI().toURL();
            } catch (MalformedURLException ex2) {
                throw new FileNotFoundException("Resource location [" + resourceLocation + "] is neither a URL not a well-formed file path");
            }
        }
    }

    private static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = ResourceFileUtil.class.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // wft...
                }
            }
        }
        return cl;
    }
}
