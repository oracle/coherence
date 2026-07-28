/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.NamedCache;
import com.tangosol.net.security.StorageAccessAuthorizer;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.processor.UpdaterProcessor;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * StorageAccessAuthorizer routed operation coverage.
 *
 * @author OpenAI  2026.05.16
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class StorageAccessAuthorizerRouteTests
        extends AbstractFunctionalTest
    {
    public StorageAccessAuthorizerRouteTests()
        {
        super(FILE_CFG_CACHE);
        }

    @BeforeClass
    public static void _startup()
        {
        if (System.getProperty("coherence.cluster") == null)
            {
            System.setProperty("coherence.cluster", "StorageAccessAuthorizerRouteTests-" + System.currentTimeMillis());
            }
        System.setProperty("coherence.override", FILE_CFG_OVERRIDE);
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE);
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.wka", "127.0.0.1");

        AbstractFunctionalTest._startup();

        Properties props = new Properties();
        props.setProperty("coherence.override", FILE_CFG_OVERRIDE);
        props.setProperty("coherence.localhost", "127.0.0.1");
        props.setProperty("coherence.wka", "127.0.0.1");

        s_member = startCacheServer("StorageAccessAuthorizerRouteTests", "security", FILE_CFG_CACHE, props);
        Eventually.assertThat(invoking(s_member).isServiceRunning("StorageAuthorizerRouteService"), is(true));
        Eventually.assertThat(invoking(s_member).isServiceRunning("StorageAuthorizerDefaultService"), is(true));
        }

    @AfterClass
    public static void stopServer()
        {
        stopCacheServer("StorageAccessAuthorizerRouteTests");
        }

    @Before
    public void resetAuthorizer()
        {
        s_member.invoke(new ResetAuthorizer());
        }

    @Test
    public void shouldAllowRoutesWithoutConfiguredAuthorizer()
        {
        NamedCache cache = getNamedCache("default-routes");
        cache.clear();

        cache.put("key", "value");
        cache.addIndex(IdentityExtractor.INSTANCE, false, null);
        cache.removeIndex(IdentityExtractor.INSTANCE);
        cache.invoke("key", new UpdaterProcessor((ValueUpdater) null, "updated"));

        assertTrue(cache.lock("lock-key", 0L));
        assertTrue(cache.unlock("lock-key"));

        assertThat(getEvents().isEmpty(), is(true));
        cache.destroy();
        }

    @Test
    public void shouldCaptureConfiguredAuthorizerIndexRoutes()
        {
        NamedCache cache = getNamedCache("authorized-index");
        cache.clear();

        cache.addIndex(IdentityExtractor.INSTANCE, false, null);
        assertContainsEvent(StorageAccessAuthorizer.REASON_INDEX_ADD, "writeAny", "authorized-index", null);

        resetAuthorizer();
        cache.removeIndex(IdentityExtractor.INSTANCE);
        assertContainsEvent(StorageAccessAuthorizer.REASON_INDEX_REMOVE, "writeAny", "authorized-index", null);

        cache.destroy();
        }

    @Test
    public void shouldCaptureConfiguredAuthorizerSingleKeyInvokeRoute()
        {
        NamedCache cache = getNamedCache("authorized-invoke");
        cache.clear();
        cache.put("key", "value");
        resetAuthorizer();

        cache.invoke("key", new UpdaterProcessor((ValueUpdater) null, "updated"));

        assertContainsEvent(StorageAccessAuthorizer.REASON_INVOKE, "writeAny", "authorized-invoke", null);
        cache.destroy();
        }

    @Test
    public void shouldCaptureConfiguredAuthorizerLockRoutes()
        {
        NamedCache cache = getNamedCache("authorized-lock");
        cache.clear();

        assertTrue(cache.lock("key", 0L));
        assertContainsEvent(StorageAccessAuthorizer.REASON_LOCK, "write", "authorized-lock", "key");

        resetAuthorizer();
        assertTrue(cache.unlock("key"));
        assertContainsEvent(StorageAccessAuthorizer.REASON_UNLOCK, "write", "authorized-lock", "key");

        cache.destroy();
        }

    @Test
    public void shouldRejectLockBeforeStateMutation()
        {
        NamedCache cache = getNamedCache("authorized-deny-lock");
        cache.clear();

        s_member.invoke(new SetDeniedReasons(Collections.singleton(StorageAccessAuthorizer.REASON_LOCK)));

        try
            {
            cache.lock("key", 0L);
            fail("lock should be rejected by the configured authorizer");
            }
        catch (RuntimeException e)
            {
            assertContainsCause(e, SecurityException.class);
            }

        s_member.invoke(new SetDeniedReasons(Collections.emptySet()));

        assertTrue(cache.lock("key", 0L));
        assertTrue(cache.unlock("key"));
        cache.destroy();
        }

    @Test
    public void shouldExposeStableReasonStrings()
        {
        assertThat(StorageAccessAuthorizer.REASON_INTERCEPTOR_REMOVE, is(17));
        assertThat(StorageAccessAuthorizer.REASON_LOCK, is(18));
        assertThat(StorageAccessAuthorizer.REASON_UNLOCK, is(19));
        assertThat(StorageAccessAuthorizer.reasonToString(StorageAccessAuthorizer.REASON_LOCK), is("lock"));
        assertThat(StorageAccessAuthorizer.reasonToString(StorageAccessAuthorizer.REASON_UNLOCK), is("unlock"));
        }

    protected List<CapturingAuthorizer.Event> getEvents()
        {
        return s_member.invoke(new GetEvents());
        }

    protected void assertContainsEvent(int nReason, String sOperation, String sCacheName, Object oKey)
        {
        List<CapturingAuthorizer.Event> listEvents = getEvents();
        assertFalse("expected captured authorizer events", listEvents.isEmpty());

        for (CapturingAuthorizer.Event event : listEvents)
            {
            if (event.getReason() == nReason
                    && sOperation.equals(event.getOperation())
                    && sCacheName.equals(event.getCacheName())
                    && (oKey == null || oKey.equals(event.getKey())))
                {
                return;
                }
            }

        fail("missing event reason=" + StorageAccessAuthorizer.reasonToString(nReason)
                + ", operation=" + sOperation + ", cache=" + sCacheName + ", key=" + oKey);
        }

    protected void assertContainsCause(Throwable thrown, Class<?> clzCause)
        {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause())
            {
            if (clzCause.isInstance(cause))
                {
                return;
                }
            }

        fail("missing cause " + clzCause.getName() + " in " + thrown);
        }

    // ----- inner class: ResetAuthorizer ------------------------------------

    public static class ResetAuthorizer
            implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            CapturingAuthorizer.clear();
            return null;
            }
        }

    // ----- inner class: SetDeniedReasons -----------------------------------

    public static class SetDeniedReasons
            implements RemoteCallable<Void>
        {
        public SetDeniedReasons(Set<Integer> setReasons)
            {
            m_setReasons = setReasons;
            }

        @Override
        public Void call()
            {
            CapturingAuthorizer.setDeniedReasons(m_setReasons);
            return null;
            }

        private final Set<Integer> m_setReasons;
        }

    // ----- inner class: GetEvents ------------------------------------------

    public static class GetEvents
            implements RemoteCallable<List<CapturingAuthorizer.Event>>
        {
        @Override
        public List<CapturingAuthorizer.Event> call()
            {
            return CapturingAuthorizer.getEvents();
            }
        }

    // ----- constants -------------------------------------------------------

    private static final String FILE_CFG_CACHE = "storage-authorizer-cache-config.xml";

    private static final String FILE_CFG_OVERRIDE = "storage-authorizer-override.xml";

    // ----- data members ---------------------------------------------------

    private static CoherenceClusterMember s_member;
    }
