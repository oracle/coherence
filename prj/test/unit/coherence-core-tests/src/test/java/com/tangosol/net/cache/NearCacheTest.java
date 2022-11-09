/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;

import com.oracle.coherence.testing.SystemPropertyIsolation;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * NearCache Unit test
 *
 * @author  jf 2022.11.08
 */
public class NearCacheTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        System.setProperty("coherence.distributed.localstorage", "true");
        }

    // ----- tests -----------------------------------------------------------

    /**
     * Regression test for COH-26224.
     * Had to be a unit test to call NearCache protected methods to simulate recreation of complex failure scenario.
     * Original recreator was performing many concurrent getAll with a small high units, failure mode
     * required same key to have unregisterListener called twice (via eviction) within one getAll call.
     */
    @Test
    public void simulateDoubleLockPRESENTOptimizationFailure()
            throws InterruptedException
        {
        String sCacheName = "near-cache1";
        ExtensibleConfigurableCacheFactory eccf =
                (ExtensibleConfigurableCacheFactory) CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE,
                                                                                                                       null);
        NearCache<String,String> m_cache = (NearCache) eccf.ensureCache(sCacheName, null);

        m_cache.put("fooKey", "fooValue");
        m_cache.put("anotherKey", "anotherValue");
        m_cache.get("fooKey");
        m_cache.get("anotherKey");

        // NearCache mapControl key locks are by thread, so run this in a different thread than client thread
        Thread recreateDoubleLock =
            new Thread(()->
                       {
                       // simulate failing scenario via direct calls to protected methods that occurred within in getAll/get
                       // when using PRESENT invalidation strategy.
                       Set<String> setUnregister = m_cache.setKeyHolder();

                       // recreate bug of double lock here by calling unregisterListener twice on same key.
                       // before bug fix the lock on this key would have a count of 2 after these two calls.
                       // fix was to ensure only one lock is outstanding.
                       m_cache.unregisterListener("fooKey");
                       m_cache.unregisterListener("fooKey");

                       m_cache.unregisterListener("anotherKey");
                       assertNotNull("set of keys to unregister listener must be non-null for PRESENT unregister optimization", setUnregister);
                       assertEquals(setUnregister.size(), 2);

                       // this call releases mapControl lock once for each key in setUnregister
                       m_cache.unregisterListeners(setUnregister);
                       m_cache.removeKeyHolder();
                       });

        recreateDoubleLock.start();
        recreateDoubleLock.join();

        if (m_cache.getControlMap().lock("fooKey", 0))
            {
            // succeeded, leak of lock of "fooKey" has been fixed
            m_cache.getControlMap().unlock("fooKey");
            }
        else
            {
            // lock held, regression test has failed
            fail("regression test for COH-26224 has failed, unable to get lock, still held by other thread");
            }
        }

    /**
     * A {@link ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();

    protected static final String FILE_CFG_CACHE = "near-cache-server-config.xml";
    }
