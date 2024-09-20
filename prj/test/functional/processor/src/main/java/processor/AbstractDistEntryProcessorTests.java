/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package processor;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.NonBlocking;
import com.oracle.coherence.common.base.Notifier;
import com.oracle.coherence.common.base.SingleWaiterMultiNotifier;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.config.Config;

import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.CompositeKey;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.LiteMap;
import com.tangosol.util.ValueManipulator;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.AsynchronousProcessor;
import com.tangosol.util.processor.NumberIncrementor;
import com.tangosol.util.processor.NumberMultiplier;
import com.tangosol.util.processor.SingleEntryAsynchronousProcessor;
import com.tangosol.util.processor.UpdaterProcessor;

import org.junit.Test;
import org.junit.runner.OrderWith;
import org.junit.runner.manipulation.Alphanumeric;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
* A collection of functional tests for the various
* {@link InvocableMap.EntryProcessor} implementations specific to the
* PartitionedCacheService.
*
* @author gg Mar 17, 2010
*/
@OrderWith(value = Alphanumeric.class)
public abstract class AbstractDistEntryProcessorTests
        extends AbstractEntryProcessorTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new AbstractDistEntryProcessorTests that will use the cache with
    * the given name in all test methods.  Test using this constructor will check
    * that the backing service is stopped as appropriate.
    *
    * @param sCache  the test cache name
    */
    public AbstractDistEntryProcessorTests(String sCache)
        {
        super(sCache);
        }

    // ----- test methods ---------------------------------------------------

    /**
    * Test of the entry processor setting a custom entry expiry.
    */
    @Test
    public void expiry()
        {
        NamedCache cache = getNamedCache();
        cache.clear();
        Eventually.assertDeferred(cache::size, is(0));
        Eventually.assertDeferred("The cache should be empty. Cache contents: " + cache.entrySet(), cache::isEmpty, is(true));

        cache.put("key", "value");
        Eventually.assertDeferred(cache::size, is(1));

        InvocableMap.EntryProcessor processor =
                new ExpiryProcessor(ExpiryProcessor.Mode.UPDATE_NONE, 100L);
        cache.invoke("key", processor);

        Eventually.assertDeferred(cache::isEmpty, is(true));

        // ensure setting the expiry for a non-existent entry is a no-op
        cache.invoke("key2", processor);
        Eventually.assertDeferred(cache::isEmpty, is(true));
        }

    /**
    * Test of the entry processor setting a custom entry expiry after updating
    * the entry value.
    */
    @Test
    public void updateBeforeExpiry()
        {
        NamedCache cache = getNamedCache();
        cache.clear();
        Eventually.assertDeferred("The cache should be empty. Cache contents: " + cache.entrySet(), cache::isEmpty, is(true));
        Eventually.assertDeferred(cache::size, is(0));

        cache.put("key", "value");
        Eventually.assertDeferred(cache::size, is(1));

        InvocableMap.EntryProcessor processor =
                new ExpiryProcessor(ExpiryProcessor.Mode.UPDATE_BEFORE_BIN, 100L);
        cache.invoke("key", processor);

        Eventually.assertDeferred(cache::isEmpty, is(true));

        // ensure setting the value & expiry for a non-existent entry
        // functions as expected
        cache.invoke("key2", new ExpiryProcessor(ExpiryProcessor.Mode.UPDATE_BEFORE, 750L));
        assertEquals("value2", cache.get("key2"));
        Eventually.assertDeferred(cache::isEmpty, is(true));
        }

    /**
    * Test of the entry processor setting a custom entry expiry before updating
    * the entry value.
    */
    @Test
    public void updateAfterExpiry()
        {
        NamedCache cache = getNamedCache();
        cache.clear();
        Eventually.assertDeferred("The cache should be empty. Cache contents: " + cache.entrySet(), cache::isEmpty, is(true));
        Eventually.assertDeferred(cache::size, is(0));

        cache.put("key", "value");
        Eventually.assertDeferred(cache::size, is(1));

        InvocableMap.EntryProcessor processor =
                new ExpiryProcessor(ExpiryProcessor.Mode.UPDATE_AFTER_BIN, 100L);
        cache.invoke("key", processor);

        Eventually.assertDeferred(cache::isEmpty, is(true));

        // ensure setting the expiry & value for a non-existent entry
        // functions as expected
        cache.invoke("key2", new ExpiryProcessor(ExpiryProcessor.Mode.UPDATE_AFTER, 750L));
        assertEquals("value2", cache.get("key2"));
        Eventually.assertDeferred(cache::isEmpty, is(true));
        }

    /**
    * Test of the AsynchronousEntryProcessor.
    */
    @Test
    public void async()
        {
        NamedCache cache = getNamedCache();
        cache.clear();

        final int SIZE = 10;
        final Map RESULT = new HashMap(SIZE);
        for (int i = 0; i < SIZE; i++)
            {
            RESULT.put(i, 10);
            }

        // set the values to 0
        EntryProcessor proc0 = new UpdaterProcessor((ValueUpdater) null, Integer.valueOf(0));
        for (Object oKey : RESULT.keySet())
            {
            cache.invoke(oKey, new SingleEntryAsynchronousProcessor(proc0));
            }

        Eventually.assertDeferred(cache::size, is(10));

        // increment by 2 -> 2
        EntryProcessor proc1 = new NumberIncrementor((ValueManipulator) null, Integer.valueOf(2), false);
        AsynchronousProcessor aproc1 = new AsynchronousProcessor(proc1);
        cache.invokeAll(RESULT.keySet(), aproc1);

        // multiply by 5 -> 10
        EntryProcessor proc2 = new NumberMultiplier((ValueManipulator) null, Integer.valueOf(5), false);
        AsynchronousProcessor aproc2 = new AsynchronousProcessor(proc2);
        cache.invokeAll(AlwaysFilter.INSTANCE, aproc2);

        try
            {
            long ldtSubmit = getSafeTimeMillis();

            Map mapResult = (Map) aproc2.get();

            log(getClass().getSimpleName() +
                ": waited for asynchronous execution to complete for "
                + (getSafeTimeMillis() - ldtSubmit) + " millis");
            if (!RESULT.equals(mapResult))
                {
                assertEquals("Wrong size", SIZE, mapResult.size());
                for (Object oKey : RESULT.keySet())
                    {
                    assertEquals("Wrong value for " + oKey, RESULT.get(oKey), mapResult.get(oKey));
                    }
                }
            }
        catch (Exception e)
            {
            fail(getStackTrace(e));
            }

        // test the non-blocking guarantee
        cache.getCacheService().shutdown();
        assertFalse(cache.isActive());

        try (NonBlocking nb = new NonBlocking())
            {
            AsynchronousProcessor aproc2a = new AsynchronousProcessor(proc2);
            cache.invokeAll(AlwaysFilter.INSTANCE, aproc2a);
            fail("Expected ServiceStoppedException");
            }
        catch (IllegalStateException e)
            {
            // thrown by async processing or an attempt to restart the service
            }

        try
            {
            cache = getNamedCache();

            waitForBalanced(cache.getCacheService());

            cache.invokeAll(AlwaysFilter.INSTANCE, aproc2);

            Map mapResult = (Map) aproc2.get();

            assertEquals(RESULT, mapResult);
            }
        catch (Exception e)
            {
            fail(getStackTrace());
            }
        }

    /**
     * Test for the async backlog notifications with manual flow control.
     */
    @Test
    public void asyncBacklog()
            throws InterruptedException, ExecutionException, TimeoutException
        {
        try (NonBlocking nb = new NonBlocking())
            {
            NamedCache cache = getNamedCache();

            final int COUNT = 1000;
            AsynchronousProcessor[] aAsync = new AsynchronousProcessor[COUNT];
            for (int i = 0; true; i++)
                {
                EntryProcessor proc = new UpdaterProcessor((ValueUpdater) null,
                    Integer.valueOf(Base.getRandom().nextInt()));
                AsynchronousProcessor async = new AsynchronousProcessor(proc, i);

                aAsync[i % COUNT] = async; // last async processor for a given key

                cache.invoke(Integer.valueOf(i % COUNT), async);

                if (async.isDone())
                    {
                    assertTrue(async.get() != null);
                    }
                else if (async.checkBacklog(null))
                    {
                    // we use the AtomicBoolean simply as a value holder
                    final AtomicBoolean atomicFlag = new AtomicBoolean(false);
                    final Notifier      notifier   = new SingleWaiterMultiNotifier();

                    Continuation contNormal = new Continuation()
                        {
                        public synchronized void proceed(Object o)
                            {
                            log("Backlog cleared");

                            atomicFlag.set(true);
                            notifier.signal();
                            }
                        };

                    if (async.checkBacklog(contNormal))
                        {
                        log("Backlog announced " + i);

                        async.flush();

                        notifier.await(5000);

                        Eventually.assertDeferred(atomicFlag::get, is(true));
                        break;
                        }
                    }
                }

            // make sure all the processors have finished
            for (AsynchronousProcessor async : aAsync)
                {
                if (async != null)
                    {
                    assertTrue(async.get(5000l, TimeUnit.SECONDS) != null);
                    }
                }
            }
        }

    /**
     * Test an exception that should be generated to an attempt to offload
     * entry enlistment.
     */
    @Test
    public void offloadEnlist()
        {
        NamedCache cache = getNamedCache();

        try
            {
            cache.invoke(new CompositeKey(0, "ignore"), new OffloadProcessor());

            fail("Execution of offloading processor should have failed");
            }
        catch (Throwable t)
            {
            }
        }

    /**
     * Test an EntryProcessor that stores binary keys as the returned keys from
     * processAll does <b>not</b> deserialize the key.
     */
    @Test
    public void testSkipKeySerialization()
        {
        // NOTE: this test presumes a single storage-enabled cluster member that
        //       is this test to reference count deserialization
        NamedCache cache = getNamedCache();
        cache.clear();
        Eventually.assertDeferred(cache::size, is(0));

        if (!Config.getBoolean("coherence.distributed.localstorage", true) ||
            cache.getCacheService().getInfo().getServiceMembers().size() > 1)
            {
            return; // skip test
            }

        Logger.info("[testSkipKeySerialization] cache instance: " + cache.getClass().getName());

        final int SIZE = 100;
        Set<SerializationCountingKey> setKeys = new HashSet<>(SIZE / 2);

        for (int i = 0; i < SIZE; i++)
            {
            SerializationCountingKey key = new SerializationCountingKey("foo-" + i);
            if ((i & 1) == 0)
                {
                setKeys.add(key);
                }

            cache.put(key, "bar-" + i);
            }

        SafeService      safeService = (SafeService) cache.getCacheService();
        PartitionedCache distService = (PartitionedCache) safeService.getService();
        Eventually.assertDeferred(() -> distService.getOwnershipEnabledMembers().size(), is(1));
        waitForBalanced(cache.getCacheService());
        Eventually.assertDeferred(cache::size, is(SIZE));
        Eventually.assertDeferred(() -> distService.isDistributionStable(), is(true));

        SerializationCountingKey.reset();
        Eventually.assertDeferred(SerializationCountingKey.DESERIALIZATION_COUNTER::get, is(0));
        Eventually.assertDeferred(SerializationCountingKey.SERIALIZATION_COUNTER::get, is(0));

        Map mapResults = cache.invokeAll(setKeys, new OptimizedGetAllProcessor());
        Eventually.assertDeferred(mapResults::size, is(setKeys.size()));

        // we do not expect any deserialization as the processor adds binary
        // keys and there is no reason for PC to deserialize the key
        int cExpected = cache instanceof ContinuousQueryCache
                ? setKeys.size() : 0;

        assertEquals("Deserialization should not occur for cache: " + cache,
                cExpected, SerializationCountingKey.DESERIALIZATION_COUNTER.get());

        // force the key to be deserialized
        for (Map.Entry entry : (Set<Map.Entry>) mapResults.entrySet())
            {
            entry.getKey();
            }

        assertEquals("Deserialization should not occur for cache: " + cache,
                cExpected + setKeys.size(), SerializationCountingKey.DESERIALIZATION_COUNTER.get());

        SerializationCountingKey.reset();
        Eventually.assertDeferred(SerializationCountingKey.DESERIALIZATION_COUNTER::get, is(0));
        Eventually.assertDeferred(SerializationCountingKey.SERIALIZATION_COUNTER::get, is(0));

        cache.clear();
        Eventually.assertDeferred(cache::isEmpty, is(true));
        }

    // ----- inner classes --------------------------------------------------

    public static class OptimizedGetAllProcessor
            implements EntryProcessor
        {

        @Override
        public Object process(Entry entry)
            {
            return entry.getValue();
            }

        @Override
        public Map processAll(Set setEntries)
            {
            Map  mapResults = new LiteMap();

            for (Iterator iter = setEntries.iterator(); iter.hasNext(); )
                {
                BinaryEntry entry = (BinaryEntry) iter.next();

                mapResults.put(entry.getBinaryKey(), process(entry));

                iter.remove();
                }
            return mapResults;
            }
        }

    public static class SerializationCountingKey
            implements PortableObject, ExternalizableLite
        {
        public SerializationCountingKey() {}

        public SerializationCountingKey(String sKey)
            {
            m_sKey = sKey;
            }

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            m_sKey = ExternalizableHelper.readSafeUTF(in);
            DESERIALIZATION_COUNTER.incrementAndGet();
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            ExternalizableHelper.writeSafeUTF(out, m_sKey);
            SERIALIZATION_COUNTER.incrementAndGet();
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_sKey = in.readString(0);
            DESERIALIZATION_COUNTER.incrementAndGet();
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sKey);
            SERIALIZATION_COUNTER.incrementAndGet();
            }

        public static void reset()
            {
            DESERIALIZATION_COUNTER.set(0);
            SERIALIZATION_COUNTER.set(0);
            }

        @Override
        public int hashCode()
            {
            return 31 +
                    (m_sKey == null ? 0 : m_sKey.hashCode());
            }

        @Override
        public boolean equals(Object o)
            {
            return o instanceof SerializationCountingKey &&
                    Base.equals(m_sKey, ((SerializationCountingKey) o).m_sKey);
            }

        public static final AtomicInteger DESERIALIZATION_COUNTER = new AtomicInteger();
        public static final AtomicInteger SERIALIZATION_COUNTER = new AtomicInteger();

        protected String m_sKey;
        }

    public static class ExpiryProcessor
            extends AbstractProcessor
        {
        public ExpiryProcessor(Mode mode, long cMillis)
            {
            m_mode    = mode;
            m_cMillis = cMillis;
            }

        public Object process(InvocableMap.Entry entry)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;

            switch (m_mode)
                {
                case UPDATE_BEFORE:
                    binEntry.setValue("value2");
                    break;
                case UPDATE_BEFORE_BIN:
                    binEntry.updateBinaryValue(new Binary(serialize(binEntry.getSerializer(), "value2")));
                    break;
                }

            binEntry.expire(m_cMillis);

            switch (m_mode)
                {
                case UPDATE_AFTER:
                    binEntry.setValue("value2");
                    break;
                case UPDATE_AFTER_BIN:
                    binEntry.updateBinaryValue(new Binary(serialize(binEntry.getSerializer(), "value2")));
                    break;
                }

            return null;
            }

        // ----- helper methods -------------------------------------------------

        /**
         * Serialize the provided {@link Serializable}
         *
         * @param serializer the {@link Serializer} to use
         * @param serializable the {@link Serializable}
         *
         * @return the serialized bytes
         *
         * @since 12.2.1.4
         */
        @SuppressWarnings("SameParameterValue")
        protected byte[] serialize(Serializer serializer, Serializable serializable)
            {
            ByteArrayWriteBuffer stream = new ByteArrayWriteBuffer(new byte[25]);
            try
                {
                serializer.serialize(stream.getBufferOutput(), serializable);
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            return stream.toByteArray();
            }

        private final Mode m_mode;

        private final long m_cMillis;

        public enum Mode
            {
            UPDATE_NONE,
            UPDATE_BEFORE_BIN,
            UPDATE_BEFORE,
            UPDATE_AFTER,
            UPDATE_AFTER_BIN
            }
        }

    /**
     * An EntryProcessor that offloads work on another thread.
     */
    public static class OffloadProcessor
            extends AbstractProcessor
        {
        @Override
        public Object process(Entry entry)
            {
            BackingMapContext ctx = ((BinaryEntry) entry).getBackingMapContext();
            CompositeKey key = (CompositeKey) entry.getKey();
            Integer IKey = (Integer) key.getPrimaryKey();
            Binary  binKey = (Binary) ctx.getManagerContext()
                                .getKeyToInternalConverter().convert(IKey);

            AtomicReference<RuntimeException> result = new AtomicReference<>();

            Thread thread = new Thread(() ->
                {
                try
                    {
                    ctx.getBackingMapEntry(binKey).setValue(0);
                    }
                catch (RuntimeException e)
                    {
                    result.set(e);
                    }
                });
            thread.start();
            try
                {
                thread.join();
                }
            catch (InterruptedException e) {}

            RuntimeException e = result.get();
            if (e != null)
                {
                throw e;
                }
            return ctx.getBackingMapEntry(binKey).getValue();
            }
        }
    }

