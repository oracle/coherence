/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.Dependencies;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.DependenciesHelper;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import java.io.File;
import java.io.IOException;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
* A collection of test-related utilities.
*
* @author dag 2010.01.12
*/
public class TestHelper
    {
    /**
    * Parse the specified xml string into an XmlElement.
    *
    * @param sXml  the xml String to be parsed
    *
    * @return the parsed XmlElement
    */
    public static XmlElement parseXmlString(String sXml)
        {
        try
            {
            return new SimpleParser().parseXml(sXml);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Get the test admin subject
    *
    * @return the test admin subject
    */
    public static Subject getAdminSubject()
        {
        Set setPrincipalAdmin = new HashSet();

        setPrincipalAdmin.add(new X500Principal("CN=admin,OU=IT"));
        return new Subject(false, setPrincipalAdmin, new HashSet(),
                new HashSet());
        }

    /**
    * Get the test user subject
    *
    * @return the test user subject
    */
    public static Subject getUserSubject()
        {
        Set setPrincipalUser   = new HashSet();

        setPrincipalUser.add(new X500Principal("CN=joe,OU=business"));
        return new Subject(false, setPrincipalUser, new HashSet(),
                new HashSet());
        }

    /**
     * Return the backing map for the given cache.
     * NOTE: this code assumes either DCCF or ECCF is the factory.
     *
     * @param cache  the cache which owns the backing map
     *
     * @return the backing map
     */
    public static Map getBackingMap(NamedCache cache)
        {
        BackingMapManager manager = cache.getCacheService().getBackingMapManager();

        return manager instanceof ExtensibleConfigurableCacheFactory.Manager
            ? ((ExtensibleConfigurableCacheFactory.Manager) manager).getBackingMap(cache.getCacheName())
            : ((DefaultConfigurableCacheFactory.Manager) manager).getBackingMap(cache.getCacheName());
        }

    /**
     * Mark the file for delete on exit.  If it is a directory then
     * also mark all of its descendant directories and files.
     *
     * @param sFile  the file or directory relative or absolute name
     */
    public static void markFileDeleteOnExit(String sFile)
        {
        markFileDeleteOnExit(new File(sFile));
        }

    /**
     * Mark the file for delete on exit.  If it is a directory then
     * also mark all of its descendant directories and files.
     *
     * @param f  the file or directory
     */
    public static void markFileDeleteOnExit(File f)
        {
        f.deleteOnExit();
        if (f.isDirectory())
            {
            File[] files = f.listFiles();
            for (File fChild : files)
                {
                if (fChild.isDirectory())
                    {
                    markFileDeleteOnExit(fChild);
                    }
                fChild.deleteOnExit();
                }
            }
        }

    /**
     * get a temp directory.
     * @param dirName  the temp directory base name
     */
    public static String getTempDir(String dirName)
        {
        // get the temp directory. java.io.tmpdir doesn't work on linux so get
        // the directory from a temp file
        String tmpDir;
        try
            {
            File f = File.createTempFile("pre", "suffix");
            f.deleteOnExit();
            tmpDir = f.getParent();
            if (!tmpDir.endsWith(File.separator))
                {
                tmpDir += File.separator;
                }
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        return tmpDir + dirName + Base.getRandom().nextInt();
        }

    /**
     * Validate that the backing map of the cache is a specific type.
     *
     * @param cache  the cache which owns the backing map
     * @param clz    the expected class of the backing map
     */
    public static void validateBackingMapType(NamedCache cache, Class clz)
        {
        assertEquals(clz, getBackingMap(cache).getClass());
        }

    /**
     * Create either a DCCF or ECCF factory, depending on which one is configured.
     *
     * @param xmlConfig  the cache configuration in XML format
     *
     * @return the ConfigurableCacheFactory
     */
    public static ConfigurableCacheFactory instantiateCacheFactory(XmlElement xmlConfig)
        {
        return instantiateCacheFactory(xmlConfig, null);
        }

    /**
     * Create either a DCCF or ECCF factory, depending on which one is configured.
     *
     * @param xmlConfig  the cache configuration in XML format
     * @param loader     the ClassLoader
     *
     * @return the ConfigurableCacheFactory
     */
    public static ConfigurableCacheFactory instantiateCacheFactory(
            final XmlElement xmlConfig, final ClassLoader loader)
        {
        return AccessController.doPrivileged(new PrivilegedAction<ConfigurableCacheFactory>()
            {
            public ConfigurableCacheFactory run()
                {
                XmlElement xml = CacheFactory.getConfigurableCacheFactoryConfig();
                if (xml.toString().contains("DefaultConfigurableCacheFactory"))
                    {
                    return new DefaultConfigurableCacheFactory(xmlConfig, loader);
                    }
                else
                    {
                    Dependencies dependencies = DependenciesHelper.newInstance(xmlConfig, loader);
                    return new ExtensibleConfigurableCacheFactory(dependencies);
                    }
                }
            });
        }

    // ----- constants ------------------------------------------------------

    /**
    * The test admin subject
    */
    public static Subject SUBJECT_ADMIN = getAdminSubject();

    /**
    * The test user subject
    */
    public static Subject SUBJECT_USER = getUserSubject();
    }
