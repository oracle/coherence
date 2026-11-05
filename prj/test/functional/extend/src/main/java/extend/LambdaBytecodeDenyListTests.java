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

import com.tangosol.internal.util.invoke.RemotableSupport;
import com.tangosol.internal.util.invoke.lambda.StaticLambdaInfo;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;

import java.lang.reflect.Field;

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
 * @since 26.07
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
        InvocableMap.EntryProcessor<String, String, Runtime> processor = entry -> Runtime.getRuntime();

        try
            {
            // bytecode policy is independent of the configured lambda wire format
            RemotableSupport.get(getClass().getClassLoader()).createRemoteConstructor(processor);
            fail("Expected Runtime bytecode to be rejected");
            }
        catch (SecurityException e)
            {
            assertTrue(e.getMessage().contains("reason=bytecode-references-gadget"));
            assertTrue(e.getMessage().contains("java.lang.Runtime"));
            }
        }

    @Test
    public void testRuntimeCapturingClassRejectedThroughExtend() throws Exception
        {
        InvocableMap.EntryProcessor<String, String, String> ordinary = InvocableMap.Entry::getValue;
        StaticLambdaInfo<?> info = new StaticLambdaInfo(ordinary.getClass(), ordinary);
        Field field = StaticLambdaInfo.class.getDeclaredField("m_sCapturingClass");
        field.setAccessible(true);
        field.set(info, "java/lang/Runtime");

        try
            {
            getNamedCache().invoke("key", (InvocableMap.EntryProcessor<String, String, Object>) entry -> info);
            fail("Expected a deny-listed static capturing class to be rejected");
            }
        catch (RuntimeException e)
            {
            assertTrue(containsRejection(e, "class-name-on-denylist", "java.lang.Runtime"));
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

    private static boolean containsRejection(Throwable t, String reason, String denied)
        {
        while (t != null)
            {
            String message = t.getMessage();
            if (message != null && message.contains("reason=" + reason) && message.contains("denied=" + denied))
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
