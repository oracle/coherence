/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

import com.oracle.coherence.testing.AbstractTestInfrastructure;

import java.io.File;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

/**
 * A base class for Coherence*Extend test classes which provides facilities
 * for starting Proxy servers and using a separate client configuration for
 * NamedCaches (in a Coherence version independent / neutral manner).
 *
 * @author jh/phf  2014.11.07
 */
public abstract class AbstractExtendTest
    extends AbstractTestInfrastructure
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new AbstractExtendTest that will use the cache with the given
     * name configured by the given cache configuration file in all test
     * methods.
     *
     * @param sCache  the test cache name, one of the CACHE_* constants
     * @param sPath   the configuration resource name or file path
     */
    public AbstractExtendTest(String sCache, String sPath)
        {
        super(sPath);
        if (sCache == null || sCache.trim().length() == 0)
            {
            throw new IllegalArgumentException("Invalid cache name");
            }

        m_sCache = sCache.trim();
        }

    // ----- AbstractExtendTest methods ------------------------------------

    /**
     * Return the cache used in all test methods.
     *
     * @return the test cache
     */
    protected NamedCache getNamedCache()
        {
        return getNamedCache(getCacheName(), getClass().getClassLoader());
        }

    protected NamedCache getNamedCache(String sCacheName)
        {
        return getNamedCache(sCacheName, getClass().getClassLoader());
        }

    protected NamedCache getNamedCache(String sCacheName, ClassLoader loader)
        {
        NamedCache cache = getFactory().ensureCache(sCacheName, loader);

        // release any previous state
        if (cache.getCacheService().getInfo().getServiceType().equals(
            CacheService.TYPE_LOCAL))
            {
            try
                {
                Object o = cache;
                o = ClassHelper.invoke(o, "getNamedCache",  ClassHelper.VOID);
                o = ClassHelper.invoke(o, "getActualMap",   ClassHelper.VOID);
                o = ClassHelper.invoke(o, "getCacheLoader", ClassHelper.VOID);
                    ClassHelper.invoke(o, "destroy",        ClassHelper.VOID);
                }
            catch (Exception e)
                {
                // ignore
                }
            }

        cache.destroy();

        // For Older releases that do not support isDestroyed(), sleep for a second.
        try
            {
            if (!(cache instanceof ContinuousQueryCache))
                {
                Eventually.assertDeferred(() -> cache.isDestroyed(), is(true));
                }
            }
        catch (Throwable e)
            {
            Base.sleep(1000);
            }

        return getFactory().ensureCache(sCacheName, loader);
        }

    /**
     * Release a cache and its associated resources.
     *
     * @param cache  the cache to be released
     */
    protected void releaseNamedCache(NamedCache cache)
        {
        getFactory().releaseCache(cache);
        }

    /**
     * Destroy a cache.
     *
     * @param cache  the cache to be destroyed
     */
    protected void destroyNamedCache(NamedCache cache)
        {
        getFactory().destroyCache(cache);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the name of the cache used in all test methods.
     *
     * @return the name of the cache used in all test methods
     */
    protected String getCacheName()
        {
        return m_sCache;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Start a cache server with a proxy service and verify the proxy is running.
     *
     * @param sServer      the name of the cache server to start; this name should be used to stop the cache server when
     *                     it is no longer needed
     * @param sCacheConfig the name of the cache configuration file that will be used by the cache server; this file
     *                     must reside within the specified test project's directory
     */
    protected static CoherenceClusterMember startCacheServerWithProxy(String sServer, String sCacheConfig)
        {
        return startCacheServerWithProxy(sServer, "extend", sCacheConfig);
        }

    /**
     * Start a cache server with a proxy service and verify the proxy is running.
     *
     * @param sServer      the name of the cache server to start; this name should be used to stop the cache server when
     *                     it is no longer needed
     * @param sProject     the test project to start the cache server for
     * @param sCacheConfig the name of the cache configuration file that will be used by the cache server; this file
     *                     must reside within the specified test project's directory
     */
    protected static CoherenceClusterMember startCacheServerWithProxy(String sServer, String sProject, String sCacheConfig)
        {
        return startCacheServerWithProxy(sServer, sProject, "build.xml", sCacheConfig, null, true);
        }

    /**
     * Start a cache server with a proxy service and verify the proxy is running.
     *
     * @param sServer      the name of the cache server to start; this name should be used to stop the cache server when
     *                     it is no longer needed
     * @param sProject     the test project to start the cache server for
     * @param sCacheConfig the name of the cache configuration file that will be used by the cache server; this file
     *                     must reside within the specified test project's directory
     * @param props        the optional system properties to pass to the cache server JVM
     */
    protected static CoherenceClusterMember startCacheServerWithProxy(String sServer, String sProject,
                                                                      String sCacheConfig, Properties props)
        {
        return startCacheServerWithProxy(sServer, sProject, "build.xml", sCacheConfig, props, true, null);
        }

    /**
     * Start a cache server with a proxy service and verify the proxy is running.
     *
     * @param sServer      the name of the cache server to start; this name should be used to stop the cache server when
     *                     it is no longer needed
     * @param sProject     the test project to start the cache server for
     * @param sBuild       the name of the test project's Ant build file that contains the "start.test.server" target
     * @param sCacheConfig the name of the cache configuration file that will be used by the cache server; this file
     *                     must reside within the specified test project's directory
     * @param props        the optional system properties to pass to the cache server JVM
     * @param fGraceful    if true, a "graceful" startup of the cache server will be performed
     */
    protected static CoherenceClusterMember startCacheServerWithProxy(String sServer, String sProject,
                                                                      String sBuild, String sCacheConfig, Properties props, boolean fGraceful)
        {
        return startCacheServerWithProxy(sServer, sProject, sBuild, sCacheConfig, props, fGraceful, null);
        }

    /**
     * Start a cache server with a proxy service and verify the proxy is running.
     *
     * @param sServer      the name of the cache server to start; this name should be used to stop the cache server when
     *                     it is no longer needed
     * @param sProject     the test project to start the cache server for
     * @param sBuild       the name of the test project's Ant build file that contains the "start.test.server" target
     * @param sCacheConfig the name of the cache configuration file that will be used by the cache server; this file
     *                     must reside within the specified test project's directory
     * @param props        the optional system properties to pass to the cache server JVM
     * @param fGraceful    if true, a "graceful" startup of the cache server will be performed
     * @param sClassPath   the optional classpath to use for the server being started
     */
    protected static CoherenceClusterMember startCacheServerWithProxy(String sServer, String sProject,
            String sBuild, String sCacheConfig, Properties props, boolean fGraceful, String sClassPath)
        {
        CoherenceClusterMember member = startCacheServer(sServer, sProject, sCacheConfig,
            props, fGraceful, sClassPath);

        Eventually.assertThat(member, new AreAllProxyServicesRunning(), is(true));
        return member;
        }

    protected static void assertCoherenceJarExists()
        {
        File fileCoherence = new File(CURRENT_COHERENCE_JAR);
        assertThat(fileCoherence + " does not exists. Run mvn -s settings.xml process-resources",
            fileCoherence.exists(), is(true));
        }

    protected static String getCurrentVersionClassPath()
        {
        assertCoherenceJarExists();
        return CURRENT_COHERENCE_JAR + File.pathSeparator + System.getProperty("java.class.path");
        }

    // ----- constants ------------------------------------------------------

    /**
     * The location of the current version of Coherence.jar used by Extend tests
     * that test backward compatibility.
     */
    public static final String CURRENT_COHERENCE_JAR  = "target/lib/coherence.jar";

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_CLIENT_SIMPLE_CFG_CACHE = "client-cache-config-simple.xml";

    /**
     * The file name of the default cache configuration file used by cache
     * servers launched by this test.
     */
    public static String FILE_SERVER_SIMPLE_CFG_CACHE = "server-cache-config-simple.xml";

    // ----- data members ---------------------------------------------------

    /**
     * The name of the cache used in all test methods.
     */
    protected final String m_sCache;
    }
