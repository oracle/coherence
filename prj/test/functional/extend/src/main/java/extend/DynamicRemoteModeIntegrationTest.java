/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.RemoteExecutionMode;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.function.Remote;

import com.tangosol.util.processor.MethodInvocationProcessor;
import com.tangosol.util.processor.ScriptProcessor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Extend roundtrip coverage for the shared dynamic remote mode gate.
 *
 * @author Aleks Seovic  2026.05.08
 * @since 26.07
 */
public class DynamicRemoteModeIntegrationTest
        extends AbstractFunctionalTest
    {
    public DynamicRemoteModeIntegrationTest()
        {
        super(AbstractExtendTests.FILE_CLIENT_CFG_CACHE);
        }

    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sSecurityModeOld  = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
        m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
        m_sClusterOld       = System.getProperty(PROP_COHERENCE_CLUSTER);
        }

    @After
    public void cleanup()
        {
        if (m_cache != null)
            {
            releaseNamedCache(m_cache);
            m_cache = null;
            }
        CacheFactory.shutdown();
        setFactory(null);
        if (m_sServerName != null)
            {
            stopCacheServer(m_sServerName);
            m_sServerName = null;
        }
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, m_sSecurityModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        restoreProperty(PROP_COHERENCE_CLUSTER, m_sClusterOld);
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    @Test
    public void mipDevCompatibilityAllowedByDefault()
        {
        assertMipAllowed("dev", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);
        }

    @Test
    public void mipCompatibilityRejectedWhenPropertyDeny()
        {
        assertMipRejected("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "deny",
                "method-invocation-denied-by-mode");
        }

    @Test
    public void absentMipSupplierDeniedBeforeExecutionInProd()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "deny");
        NamedCache<String, String> cache = getCache();
        String sKey = "absent-mip-supplier";
        m_memberProxy.invoke(new ResetObservableSupplierCalls());

        assertFalse(cache.containsKey(sKey));

        assertRemoteFailure(cache, sKey,
                new MethodInvocationProcessor<String, String, Integer>(
                        new ObservableSupplier(), "length", false),
                "method-invocation-denied-by-mode");

        assertEquals(Integer.valueOf(0), m_memberProxy.invoke(new GetObservableSupplierCalls()));
        assertFalse(cache.containsKey(sKey));
        }

    @Test
    public void mipCompatibilityAllowedWhenPropertyAllow()
        {
        assertMipAllowed("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "allow");
        }

    @Test
    public void mipCompatibilityAllowedByDefault()
        {
        assertMipAllowed("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);
        }

    @Test
    public void spDevCompatibilityAllowedByDefault()
        {
        assertScriptAllowed("dev", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);
        }

    @Test
    public void spCompatibilityRejectedWhenPropertyDeny()
        {
        assertScriptRejected("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "deny",
                "script-eval-denied-by-mode");
        }

    @Test
    public void spCompatibilityAllowedWhenPropertyAllow()
        {
        assertScriptAllowed("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "allow");
        }

    @Test
    public void spCompatibilityAllowedByDefault()
        {
        assertScriptAllowed("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);
        }

    @Test
    public void compatibilityScriptHostAccessIsHardFloor()
        {
        startProxy("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);
        NamedCache<String, String> cache = getCache();
        cache.put("key", "value");

        assertRemoteFailure(cache, new ScriptProcessor<String, String, Object>("js", "RuntimeProbe"),
                "java.lang.Runtime");
        }

    private void assertMipAllowed(String sMode, String sSecurityMode, String sDynamicRemote)
        {
        startProxy(sMode, sSecurityMode, sDynamicRemote);
        NamedCache<String, String> cache = getCache();
        cache.put("key", "value");

        assertEquals(Integer.valueOf(5), cache.invoke("key",
                new MethodInvocationProcessor<String, String, Integer>("length", false)));
        }

    private void assertMipRejected(String sMode, String sSecurityMode, String sDynamicRemote, String sMessage)
        {
        startProxy(sMode, sSecurityMode, sDynamicRemote);
        NamedCache<String, String> cache = getCache();
        cache.put("key", "value");

        assertRemoteFailure(cache, new MethodInvocationProcessor<String, String, Integer>("length", false), sMessage);
        }

    private void assertScriptAllowed(String sMode, String sSecurityMode, String sDynamicRemote)
        {
        startProxy(sMode, sSecurityMode, sDynamicRemote);
        NamedCache<String, String> cache = getCache();
        cache.put("key", "value");

        assertEquals("value", cache.invoke("key", new ScriptProcessor<String, String, String>("js", "EntryEcho")));
        }

    private void assertScriptRejected(String sMode, String sSecurityMode, String sDynamicRemote, String sMessage)
        {
        startProxy(sMode, sSecurityMode, sDynamicRemote);
        NamedCache<String, String> cache = getCache();
        cache.put("key", "value");

        assertRemoteFailure(cache, new ScriptProcessor<String, String, String>("js", "EntryEcho"), sMessage);
        }

    private void startProxy(String sMode, String sSecurityMode, String sDynamicRemote)
        {
        String sCluster = SERVER_NAME + '-' + sMode + '-' + (sDynamicRemote == null ? "default" : sDynamicRemote)
                + '-' + System.nanoTime();

        CoherenceModeHelper.restore(sMode);
        CoherenceModeHelper.restoreSecurityMode(sSecurityMode);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
        restoreProperty(PROP_COHERENCE_CLUSTER, sCluster);
        RemoteExecutionMode.resetForTesting();

        Properties props = new Properties();
        props.setProperty("coherence.mode", sMode);
        props.setProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
        props.setProperty(PROP_COHERENCE_CLUSTER, sCluster);
        props.setProperty("test.extend.enabled", "true");
        if (sDynamicRemote != null)
            {
            props.setProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
            }

        m_sServerName = sCluster;
        m_memberProxy = startCacheServer(m_sServerName, "extend",
                AbstractExtendTests.FILE_SERVER_CFG_CACHE, props);
        Eventually.assertThat(invoking(m_memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    private NamedCache<String, String> getCache()
        {
        m_cache = getNamedCache(AbstractExtendTests.CACHE_DIST_EXTEND_DIRECT);
        return m_cache;
        }

    private static void assertRemoteFailure(NamedCache<String, String> cache,
                                            com.tangosol.util.InvocableMap.EntryProcessor<String, String, ?> processor,
                                            String sMessage)
        {
        assertRemoteFailure(cache, "key", processor, sMessage);
        }

    private static void assertRemoteFailure(NamedCache<String, String> cache,
                                            String sKey,
                                            com.tangosol.util.InvocableMap.EntryProcessor<String, String, ?> processor,
                                            String sMessage)
        {
        try
            {
            cache.invoke(sKey, processor);
            fail("Expected remote processor to fail");
            }
        catch (RuntimeException e)
            {
            assertTrue(String.valueOf(e), containsMessage(e, sMessage));
            }
        }

    private static boolean containsMessage(Throwable t, String sMessage)
        {
        while (t != null)
            {
            if (String.valueOf(t.getMessage()).contains(sMessage))
                {
                return true;
                }
            t = t.getCause();
            }
        return false;
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    public static class ObservableSupplier
            implements Remote.Supplier<String>, PortableObject
        {
        @Override
        public String get()
            {
            CALLS.incrementAndGet();
            return "value";
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        static void reset()
            {
            CALLS.set(0);
            }

        static int calls()
            {
            return CALLS.get();
            }

        private static final AtomicInteger CALLS = new AtomicInteger();
        }

    public static class ResetObservableSupplierCalls
            implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            ObservableSupplier.reset();
            return null;
            }
        }

    public static class GetObservableSupplierCalls
            implements RemoteCallable<Integer>
        {
        @Override
        public Integer call()
            {
            return ObservableSupplier.calls();
            }
        }

    private NamedCache<String, String> m_cache;
    private String                     m_sServerName;
    private CoherenceClusterMember     m_memberProxy;

    private static final String SERVER_NAME = "DynamicRemoteModeIntegrationTest";

    private static final String PROP_COHERENCE_CLUSTER = "coherence.cluster";

    private String m_sModeOld;
    private String m_sSecurityModeOld;
    private String m_sDynamicRemoteOld;
    private String m_sClusterOld;
    }
