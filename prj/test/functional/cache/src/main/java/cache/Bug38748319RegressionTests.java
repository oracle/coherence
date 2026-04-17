/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.safeService.SafeCacheService;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.MultiBufferWriteBuffer;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.util.Binary;
import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Regression coverage for BUG-38748319.
 */
public class Bug38748319RegressionTests
    {
    @Test
    public void shouldClonePendingMapEventBeforeOnIntervalResend()
        {
        CoherenceCluster       cluster       = s_cluster.getCluster();
        CoherenceClusterMember memberStore   = cluster.get("storage-1");
        CoherenceClusterMember memberClient  = cluster.get("client-1");
        String                 sCacheName    = "bug-38748319-resend-" + System.nanoTime();

        Eventually.assertDeferred(cluster::getClusterSize, is(4));

        // Ensure the storage-disabled member has joined the cache service so the
        // synthetic resend targets the same kind of recipient as the reported bug.
        memberClient.invoke(new EnsureListener(sCacheName));

        int nRecipientId = memberClient.invoke(new GetLocalMemberId());

        assertThat(memberStore.invoke(new TriggerOnIntervalResend(sCacheName, nRecipientId)), is(true));
        }

    @Test
    public void shouldKeepDeliveringMapEventsWhenAnotherListenerMemberLeaves() throws Exception
        {
        CoherenceCluster       cluster       = s_cluster.getCluster();
        CoherenceClusterMember memberStore1  = cluster.get("storage-1");
        CoherenceClusterMember memberStore2  = cluster.get("storage-2");
        CoherenceClusterMember memberClient1 = cluster.get("client-1");
        CoherenceClusterMember memberClient2 = cluster.get("client-2");
        String                 sCacheName    = "bug-38748319-" + System.nanoTime();
        String                 sKeyPrefix    = sCacheName + "-key-";
        int                    cEvents       = 4096;
        int                    cbValue       = 32768;

        Eventually.assertDeferred(cluster::getClusterSize, is(4));

        memberStore1.invoke(new ClearCache(sCacheName));
        memberClient1.invoke(new ResetListener(sCacheName));
        memberClient2.invoke(new ResetListener(sCacheName));
        memberClient1.invoke(new EnsureListener(sCacheName));
        memberClient2.invoke(new EnsureListener(sCacheName));

        // Keep a large event stream in flight so that closing a listener member
        // exercises the resend path for pending MapEvents.
        CompletableFuture<Integer> futurePublish =
                memberStore1.submit(new PutSequence(sCacheName, sKeyPrefix, cEvents, cbValue));

        Eventually.assertDeferred(() -> memberClient1.invoke(new GetInsertCount(sCacheName)), is(greaterThan(32)));
        Eventually.assertDeferred(() -> memberClient2.invoke(new GetInsertCount(sCacheName)), is(greaterThan(32)));

        assertThat("publisher completed before the listener member was closed", futurePublish.isDone(), is(false));

        memberClient2.close();

        Eventually.assertDeferred(cluster::getClusterSize, is(3));
        assertThat(futurePublish.get(5, TimeUnit.MINUTES), is(cEvents));

        Eventually.assertDeferred(() -> memberClient1.invoke(new GetInsertCount(sCacheName)), is(cEvents));
        Eventually.assertDeferred(() -> memberClient1.invoke(new GetCacheSize(sCacheName)), is(cEvents));
        Eventually.assertDeferred(() -> memberStore1.invoke(new GetCacheSize(sCacheName)), is(cEvents));
        Eventually.assertDeferred(() -> memberStore2.invoke(new GetCacheSize(sCacheName)), is(cEvents));
        Eventually.assertDeferred(() -> memberStore1.invoke(new GetClusterSize()), is(3));
        Eventually.assertDeferred(() -> memberStore2.invoke(new GetClusterSize()), is(3));
        }

    // ----- helper classes -------------------------------------------------

    /**
     * Per-member listener registry used by the remote callables.
     */
    protected static class ListenerRegistry
        {
        static void ensureRegistered(String sCacheName)
            {
            LISTENERS.computeIfAbsent(sCacheName, name ->
                {
                NamedCache<String, byte[]> cache    = CacheFactory.getCache(name);
                CountingMapListener        listener = new CountingMapListener();

                cache.addMapListener(listener);
                return listener;
                });
            }

        static void reset(String sCacheName)
            {
            CountingMapListener listener = LISTENERS.remove(sCacheName);
            if (listener != null)
                {
                CacheFactory.getCache(sCacheName).removeMapListener(listener);
                }
            }

        static int getInsertCount(String sCacheName)
            {
            CountingMapListener listener = LISTENERS.get(sCacheName);
            return listener == null ? 0 : listener.getInsertCount();
            }

        private static final Map<String, CountingMapListener> LISTENERS = new ConcurrentHashMap<>();
        }

    /**
     * Listener used on remote client members.
     */
    protected static class CountingMapListener
            extends AbstractMapListener
        {
        @Override
        public void entryInserted(MapEvent evt)
            {
            m_cInsert.incrementAndGet();
            }

        protected int getInsertCount()
            {
            return m_cInsert.get();
            }

        private final AtomicInteger m_cInsert = new AtomicInteger();
        }

    public static class EnsureListener
            implements RemoteCallable<Integer>, Serializable
        {
        public EnsureListener(String sCacheName)
            {
            f_sCacheName = sCacheName;
            }

        @Override
        public Integer call()
            {
            ListenerRegistry.ensureRegistered(f_sCacheName);
            return ListenerRegistry.getInsertCount(f_sCacheName);
            }

        private final String f_sCacheName;
        }

    public static class GetLocalMemberId
            implements RemoteCallable<Integer>, Serializable
        {
        @Override
        public Integer call()
            {
            return CacheFactory.getCluster().getLocalMember().getId();
            }
        }

    public static class ResetListener
            implements RemoteCallable<Void>, Serializable
        {
        public ResetListener(String sCacheName)
            {
            f_sCacheName = sCacheName;
            }

        @Override
        public Void call()
            {
            ListenerRegistry.reset(f_sCacheName);
            return null;
            }

        private final String f_sCacheName;
        }

    public static class GetInsertCount
            implements RemoteCallable<Integer>, Serializable
        {
        public GetInsertCount(String sCacheName)
            {
            f_sCacheName = sCacheName;
            }

        @Override
        public Integer call()
            {
            return ListenerRegistry.getInsertCount(f_sCacheName);
            }

        private final String f_sCacheName;
        }

    public static class TriggerOnIntervalResend
            implements RemoteCallable<Boolean>, Serializable
        {
        public TriggerOnIntervalResend(String sCacheName, int nRecipientId)
            {
            f_sCacheName   = sCacheName;
            f_nRecipientId = nRecipientId;
            }

        @Override
        public Boolean call()
            {
            NamedCache<String, String> cache = CacheFactory.getTypedCache(f_sCacheName, TypeAssertion.withTypes(String.class, String.class));
            PartitionedCache service = getPartitionedCache(cache);
            Object           storage = getStorage(service, f_sCacheName);
            Member           member  = service.getServiceMemberSet().getMember(f_nRecipientId);

            assertThat("storage must exist for the synthetic resend test", storage, is(notNullValue()));
            assertThat("recipient must be part of the cache service", member, is(notNullValue()));

            Binary binKey   = ExternalizableHelper.toBinary(f_sCacheName + "-key", cache.getCacheService().getSerializer());
            Binary binValue = ExternalizableHelper.toBinary(f_sCacheName + "-value", cache.getCacheService().getSerializer());
            long   lEvent   = service.getSUID(PartitionedCache.SUID_EVENT);

            // Seed PendingEvents with a single oldest event that already carries
            // the same MultiBufferWriteBuffer controller that caused the bug.
            PartitionedCache.MapEvent msgEvent = (PartitionedCache.MapEvent) service.instantiateMessage(68);
            msgEvent.addToMember(member);
            msgEvent.setCacheId(getCacheId(storage));
            msgEvent.setEventSUID(lEvent);
            msgEvent.setEventType(MapEvent.ENTRY_INSERTED);
            msgEvent.setKey(binKey);
            msgEvent.setNewValue(binValue);
            msgEvent.setOldestPendingEventSUID(lEvent);
            msgEvent.setPartition(service.getKeyPartition(binKey));
            msgEvent.setVersion(1L);
            // Recreate the stale controller shape from the bug: an already
            // serialized message still holding a MultiBufferWriteBuffer.
            msgEvent.setBufferController(new MultiBufferWriteBuffer(new ByteArrayWriteBuffer.Allocator(1024)), 1);

            synchronized (service.getPendingEvents())
                {
                service.getPendingEvents().set(lEvent, msgEvent);
                }

            // Advance the event counter far enough that the seeded event looks
            // "stuck" to the onInterval resend logic.
            for (int i = 0; i < 101; i++)
                {
                service.getSUID(PartitionedCache.SUID_EVENT);
                }

            service.setOldestEventResendNextMillis(0L);
            invokeOnInterval(service);

            service.setOldestEventResendNextMillis(0L);
            invokeOnInterval(service);

            return service.isRunning();
            }

        /**
         * Extract the internal PartitionedCache service from the public cache.
         */
        private static PartitionedCache getPartitionedCache(NamedCache<?, ?> cache)
            {
            Service service = ((SafeCacheService) cache.getCacheService()).getService();

            assertThat("expected an internal PartitionedCache service", service instanceof PartitionedCache, is(true));
            return (PartitionedCache) service;
            }

        /**
         * Resolve the generated internal Storage object without depending on its
         * generated nested type name in the test source.
         */
        private static Object getStorage(PartitionedCache service, String sCacheName)
            {
            try
                {
                Method method = PartitionedCache.class.getMethod("getStorage", String.class);

                return method.invoke(service, sCacheName);
                }
            catch (ReflectiveOperationException e)
                {
                throw new RuntimeException("failed to resolve PartitionedCache storage", e);
                }
            }

        /**
         * Read the cache id from the generated Storage object used by the service.
         */
        private static long getCacheId(Object storage)
            {
            try
                {
                Method method = storage.getClass().getMethod("getCacheId");

                return ((Number) method.invoke(storage)).longValue();
                }
            catch (ReflectiveOperationException e)
                {
                throw new RuntimeException("failed to read cache id from storage", e);
                }
            }

        /**
         * Invoke the protected onInterval hook so the test can drive the resend path
         * without waiting for the daemon scheduler.
         */
        private static void invokeOnInterval(PartitionedCache service)
            {
            try
                {
                Method method = PartitionedCache.class.getDeclaredMethod("onInterval");

                method.setAccessible(true);
                method.invoke(service);
                }
            catch (ReflectiveOperationException e)
                {
                throw new RuntimeException("failed to invoke PartitionedCache.onInterval()", e);
                }
            }

        private final String f_sCacheName;
        private final int    f_nRecipientId;
        }

    public static class ClearCache
            implements RemoteCallable<Void>, Serializable
        {
        public ClearCache(String sCacheName)
            {
            f_sCacheName = sCacheName;
            }

        @Override
        public Void call()
            {
            CacheFactory.getCache(f_sCacheName).clear();
            return null;
            }

        private final String f_sCacheName;
        }

    public static class GetCacheSize
            implements RemoteCallable<Integer>, Serializable
        {
        public GetCacheSize(String sCacheName)
            {
            f_sCacheName = sCacheName;
            }

        @Override
        public Integer call()
            {
            return CacheFactory.getCache(f_sCacheName).size();
            }

        private final String f_sCacheName;
        }

    public static class GetClusterSize
            implements RemoteCallable<Integer>, Serializable
        {
        @Override
        public Integer call()
            {
            return CacheFactory.getCluster().getMemberSet().size();
            }
        }

    public static class PutSequence
            implements RemoteCallable<Integer>, Serializable
        {
        public PutSequence(String sCacheName, String sKeyPrefix, int cEvents, int cbValue)
            {
            f_sCacheName = sCacheName;
            f_sKeyPrefix = sKeyPrefix;
            f_cEvents    = cEvents;
            f_cbValue    = cbValue;
            }

        @Override
        public Integer call()
            {
            NamedCache<String, byte[]> cache      = CacheFactory.getCache(f_sCacheName);
            byte[]                     abTemplate = new byte[f_cbValue];

            Arrays.fill(abTemplate, (byte) 1);

            for (int i = 0; i < f_cEvents; i++)
                {
                byte[] abValue = abTemplate.clone();

                abValue[0] = (byte) i;
                abValue[1] = (byte) (i >>> 8);

                cache.put(f_sKeyPrefix + i, abValue);
                }

            return f_cEvents;
            }

        private final String f_sCacheName;
        private final String f_sKeyPrefix;
        private final int    f_cEvents;
        private final int    f_cbValue;
        }

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static final TestLogs s_testLogs = new TestLogs(Bug38748319RegressionTests.class);

    @Rule
    public final CoherenceClusterResource s_cluster =
            new CoherenceClusterResource()
                    .with(ClusterName.of(Bug38748319RegressionTests.class.getSimpleName()),
                          CacheConfig.of("coherence-cache-config.xml"),
                          // Keep the test on the TCMP/packet-publisher path while
                          // shrinking the minimum UDP buffer requirement enough for
                          // constrained local environments.
                          OperationalOverride.of("bug-38748319-coherence-override.xml"),
                          LocalHost.only(),
                          WellKnownAddress.loopback(),
                          IPv4Preferred.yes(),
                          s_testLogs,
                          StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()))
                    .include(2, CoherenceClusterMember.class,
                             DisplayName.of("storage"),
                             IPv4Preferred.yes(),
                             LocalStorage.enabled())
                    .include(2, CoherenceClusterMember.class,
                             DisplayName.of("client"),
                             IPv4Preferred.yes(),
                             LocalStorage.disabled());
    }
