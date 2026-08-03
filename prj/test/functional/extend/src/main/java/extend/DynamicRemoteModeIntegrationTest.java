/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.RemoteExecutionMode;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.processor.MethodInvocationProcessor;
import com.tangosol.util.processor.ScriptProcessor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Extend roundtrip coverage for the shared dynamic remote mode gate.
 *
 * @author Aleks Seovic  2026.05.08
 * @since 26.04
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
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        restoreProperty(PROP_COHERENCE_CLUSTER, m_sClusterOld);
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    @Test
    public void mipDevAllowedByDefault()
        {
        assertMipAllowed("dev", null);
        }

    @Test
    public void mipProdDeniedByDefault()
        {
        assertMipRejected("prod", null, "method-invocation-denied-by-mode");
        }

    @Test
    public void mipProdAllowedWhenPropertyAllow()
        {
        assertMipAllowed("prod", "allow");
        }

    @Test
    public void mipLegacyAllowedByDefault()
        {
        assertMipAllowed("legacy", null);
        }

    @Test
    public void spDevAllowedByDefault()
        {
        assertScriptAllowed("dev", null);
        }

    @Test
    public void spProdDeniedByDefault()
        {
        assertScriptRejected("prod", null, "script-eval-denied-by-mode");
        }

    @Test
    public void spProdAllowedWhenPropertyAllow()
        {
        assertScriptAllowed("prod", "allow");
        }

    @Test
    public void spLegacyAllowedByDefault()
        {
        assertScriptAllowed("legacy", null);
        }

    @Test
    public void legacyScriptHostAccessIsHardFloor()
        {
        startProxy("legacy", null);
        NamedCache<String, String> cache = getCache();
        cache.put("key", "value");

        assertRemoteFailure(cache, new ScriptProcessor<String, String, Object>("js", "RuntimeProbe"),
                "java.lang.Runtime");
        }

    private void assertMipAllowed(String sMode, String sDynamicRemote)
        {
        startProxy(sMode, sDynamicRemote);
        NamedCache<String, String> cache = getCache();
        cache.put("key", "value");

        assertEquals(Integer.valueOf(5), cache.invoke("key",
                new MethodInvocationProcessor<String, String, Integer>("length", false)));
        }

    private void assertMipRejected(String sMode, String sDynamicRemote, String sMessage)
        {
        startProxy(sMode, sDynamicRemote);
        NamedCache<String, String> cache = getCache();
        cache.put("key", "value");

        assertRemoteFailure(cache, new MethodInvocationProcessor<String, String, Integer>("length", false), sMessage);
        }

    private void assertScriptAllowed(String sMode, String sDynamicRemote)
        {
        startProxy(sMode, sDynamicRemote);
        NamedCache<String, String> cache = getCache();
        cache.put("key", "value");

        assertEquals("value", cache.invoke("key", new ScriptProcessor<String, String, String>("js", "EntryEcho")));
        }

    private void assertScriptRejected(String sMode, String sDynamicRemote, String sMessage)
        {
        startProxy(sMode, sDynamicRemote);
        NamedCache<String, String> cache = getCache();
        cache.put("key", "value");

        assertRemoteFailure(cache, new ScriptProcessor<String, String, String>("js", "EntryEcho"), sMessage);
        }

    private void startProxy(String sMode, String sDynamicRemote)
        {
        String sCluster = SERVER_NAME + '-' + sMode + '-' + (sDynamicRemote == null ? "default" : sDynamicRemote)
                + '-' + System.nanoTime();

        CoherenceModeHelper.restore(sMode);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
        restoreProperty(PROP_COHERENCE_CLUSTER, sCluster);
        RemoteExecutionMode.resetForTesting();

        Properties props = new Properties();
        props.setProperty("coherence.mode", sMode);
        props.setProperty(PROP_COHERENCE_CLUSTER, sCluster);
        props.setProperty("test.extend.enabled", "true");
        if (sDynamicRemote != null)
            {
            props.setProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
            }

        m_sServerName = sCluster;
        CoherenceClusterMember member = startCacheServer(m_sServerName, "extend",
                AbstractExtendTests.FILE_SERVER_CFG_CACHE, props);
        Eventually.assertThat(invoking(member).isServiceRunning("ExtendTcpProxyService"), is(true));
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
        try
            {
            cache.invoke("key", processor);
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

    private NamedCache<String, String> m_cache;
    private String                     m_sServerName;

    private static final String SERVER_NAME = "DynamicRemoteModeIntegrationTest";

    private static final String PROP_COHERENCE_CLUSTER = "coherence.cluster";

    private String m_sModeOld;
    private String m_sDynamicRemoteOld;
    private String m_sClusterOld;
    }
