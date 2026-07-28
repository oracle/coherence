/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional coverage for lambda bytecode deny-listing through Extend.
 *
 * @author Aleks Seovic  2026.04.30
 * @since 26.04
 */
public class LambdaBytecodeDenyListTests
        extends AbstractFunctionalTest
    {
    public LambdaBytecodeDenyListTests()
        {
        super(AbstractExtendTests.FILE_CLIENT_CFG_CACHE);
        }

    @BeforeClass
    public static void startup()
        {
        System.setProperty("coherence.cluster", System.getProperty("coherence.cluster",
                "LambdaBytecodeDenyListTests-" + System.currentTimeMillis()));

        CoherenceClusterMember memberProxy = startCacheServer("LambdaBytecodeDenyListTests", "extend",
                AbstractExtendTests.FILE_SERVER_CFG_CACHE);
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    @After
    public void cleanup()
        {
        if (m_cache != null)
            {
            releaseNamedCache(m_cache);
            m_cache = null;
            }
        }

    @AfterClass
    public static void shutdown()
        {
        CacheFactory.shutdown();
        stopCacheServer("LambdaBytecodeDenyListTests");
        }

    @Test
    public void testRuntimeLambdaRejected()
        {
        NamedCache<String, String> cache = getNamedCache();

        try
            {
            cache.invoke("key", (InvocableMap.EntryProcessor<String, String, Object>) entry ->
                {
                try
                    {
                    Runtime.getRuntime().exec("ls");
                    }
                catch (Exception e)
                    {
                    throw new RuntimeException(e);
                    }
                return null;
                });
            fail("Expected Runtime lambda to be rejected");
            }
        catch (RuntimeException e)
            {
            assertTrue(containsSecurityException(e));
            }
        }

    @Test
    public void testOrdinaryLambdaAccepted()
        {
        NamedCache<String, String> cache = getNamedCache();
        cache.put("key", "value");

        String sValue = cache.invoke("key",
                (InvocableMap.EntryProcessor<String, String, String>) InvocableMap.Entry::getValue);

        assertEquals("value", sValue);
        }

    private static boolean containsSecurityException(Throwable t)
        {
        while (t != null)
            {
            if (t instanceof SecurityException)
                {
                return true;
                }
            t = t.getCause();
            }
        return false;
        }

    private NamedCache<String, String> getNamedCache()
        {
        m_cache = getNamedCache(AbstractExtendTests.CACHE_DIST_EXTEND_DIRECT);
        return m_cache;
        }

    private NamedCache<String, String> m_cache;
    }
