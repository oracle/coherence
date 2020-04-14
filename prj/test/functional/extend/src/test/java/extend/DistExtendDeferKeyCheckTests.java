/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package extend;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.ClassHelper;

import common.AbstractFunctionalTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
* A collection of functional tests for Coherence*Extend that verify support
* for the defer-key-association-check configuration element
*
* @author phf  2011.09.07
*/
public class DistExtendDeferKeyCheckTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistExtendDeferKeyCheckTests()
        {
        super(FILE_CLIENT_CFG_CACHE);
        }


    // ----- DistExtendDeferKeyCheckTests methods ---------------------------

    /**
    * Return the cache used in all test methods.
    *
    * @return the test cache
    */
    protected NamedCache getNamedCache()
        {
        return getNamedCache(CACHE_DIST_EXTEND_DIRECT);
        }


    // ----- AbstractNamedCacheTest methods ---------------------------------

    /**
    * {@inheritDoc}
    */
    protected NamedCache getNamedCache(String sCacheName, ClassLoader loader)
        {
        ConfigurableCacheFactory factory = getFactory();
        NamedCache               cache   = factory.ensureCache(sCacheName, loader);

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

        return factory.ensureCache(sCacheName, loader);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember memberProxy = startCacheServer("DistExtendDeferKeyCheckTests", "extend", FILE_SERVER_CFG_CACHE);
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistExtendDeferKeyCheckTests");
        }


    // ----- DistExtendDeferKeyCheckTests test methods ----------------------

    /**
    * Verify that put with a simple key class succeeds but with a custom key
    * class throws an exception.
    */
    @Test
    public void put()
        {
        NamedCache cache = getNamedCache();

        cache.clear();
        assertTrue(cache.isEmpty());

        cache.put("key", "value");

        try
            {
            cache.put(new CustomKeyClass("key"), "value");
            }
        catch (Exception e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }


    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CLIENT_CFG_CACHE
            = "client-cache-config-defer-key-check.xml";

    /**
    * The file name of the default cache configuration file used by cache
    * servers launched by this test.
    */
    public static String FILE_SERVER_CFG_CACHE    = "server-cache-config.xml";

    /**
    * Cache name: "dist-extend-direct"
    */
    public static String CACHE_DIST_EXTEND_DIRECT = "dist-extend-direct";
    }
