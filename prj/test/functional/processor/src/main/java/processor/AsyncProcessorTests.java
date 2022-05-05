/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package processor;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.coherence.common.base.Continuation;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.ServiceStoppedException;

import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.NullImplementation;

import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.AsynchronousProcessor;

import com.oracle.coherence.testing.AbstractRollingRestartTest;

import java.util.Map;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Tests for async entry-processor functionality during membership change.
 *
 * @author rhl 2013.08.29
 */
public class AsyncProcessorTests
        extends AbstractRollingRestartTest
    {
    // ----- AbstractRollingRestartTest methods -----------------------------

    /**
     * {@inheritDoc}
     */
    public String getCacheConfigPath()
        {
        return s_sCacheConfig;
        }

    /**
     * {@inheritDoc}
     */
    public String getBuildPath()
        {
        return s_sBuild;
        }

    /**
     * {@inheritDoc}
     */
    public String getProjectName()
        {
        return s_sProject;
        }

    // ----- test methods ---------------------------------------------------


    /**
     * Test of async EP through suspend/resume.
     */
    @Test
    public void testAsyncSuspendResume()
        {
        final String sServer = "testAsyncSuspendResume";

        CoherenceClusterMember clusterMember = startCacheServer(sServer, s_sProject, s_sCacheConfig);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("Partitioned"), is(true));
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("PartitionedWithPool"), is(true));

        NamedCache cache = getNamedCache("dist-std-test-async");
        cache.put("foo", 1);

        CacheService service = cache.getCacheService();
        CacheFactory.getCluster().suspendService(service.getInfo().getServiceName());

        final AtomicReference atomicResult = new AtomicReference();

        final Continuation continuation = new Continuation<Object>()
            {
            @Override
            public void proceed(Object o)
                {
                synchronized (atomicResult)
                    {
                    atomicResult.set(o);
                    atomicResult.notifyAll();
                    }
                }
            };

        cache.invoke("foo", new ContinuationAsyncProcessor(new IncrementProcessor(1), continuation, continuation));

        Base.sleep(10L);

        // assert that the EP did not execute
        assertNull(atomicResult.get());

        CacheFactory.getCluster().resumeService(service.getInfo().getServiceName());

        Eventually.assertThat(invoking(this).dereference(atomicResult), is(notNullValue()));
        assertEquals(1, (((Map) atomicResult.get()).get("foo")));

        stopCacheServer(sServer, true);
        }

    /**
     * Test to verify that an exception raised during a key-based request results
     * in the set of failed keys being propagated to the client; identical to
     * sync processors.
     */
    @Test
    public void testException()
        {
        startCacheServer("testException-1", s_sProject, s_sCacheConfig);

        try
            {
            NamedCache cache = getNamedCache("dist-std-test-kaboom");

            for (int i = 10; i > 0; --i)
                {
                cache.put(i, i);
                }

            AtomicReference<Throwable> result = new AtomicReference<>();
            AsynchronousProcessor      proc1 = new AsynchronousProcessor(
                entry -> { throw new RuntimeException("kaboom"); })
                {
                public void onException(Throwable t)
                    {
                    result.set(t);
                    }
                };

            cache.invokeAll(cache.keySet(), proc1);
            try
                {
                proc1.get();
                }
            catch (Throwable t)
                {
                fail("Execution of async processor failed:" + t.getMessage() +
                        "\n" + Base.printStackTrace(t));
                }
            Throwable t1 = result.get();
            assertTrue(t1 instanceof RequestIncompleteException);

            Set setKeys = (Set) ((RequestIncompleteException) t1).getPartialResult();
            assertEquals(10, setKeys.size());

            // test the service stop behavior
            result.set(null);

            AsynchronousProcessor proc2 = new AsynchronousProcessor(
                entry -> { Base.sleep(10000); return null;})
                {
                public void onException(Throwable t)
                    {
                    result.set(t);
                    }
                };
            cache.invoke(1, proc2);
            cache.getCacheService().stop();

            try
                {
                proc2.get();
                }
            catch (Throwable t)
                {
                fail("Execution of async processor failed:" + t.getMessage() +
                        "\n" + Base.printStackTrace(t));
                }

            assertTrue(result.get() instanceof ServiceStoppedException);
            }
        finally
            {
            stopCacheServer("testException-1");
            }
        }

    /**
     * Test unit-of-order guarantees during graceful membership change.
     */
    @Test
    public void testAsyncGraceful()
        {
        final String sServer = "testAsyncGraceful";

        final AtomicInteger   atomicIdx       = new AtomicInteger();
        final AtomicReference atomicResult    = new AtomicReference();
        final NamedCache      cache           = getNamedCache("dist-std-test-async");
        final DistributedCacheService service = (DistributedCacheService) cache.getCacheService();

        class ClientThread
                extends Thread
            {
            public ClientThread(Object oKey)
                {
                f_oKey = oKey;
                }

            public void run()
                {
                try
                    {
                    Continuation<Throwable> contException = new Continuation<Throwable>()
                        {
                        @Override
                        public void proceed(Throwable e)
                            {
                            atomicResult.set(e);
                            }
                        };

                    cache.put(f_oKey, 1);

                    int i = 1;
                    while (atomicResult.get() == null)
                        {
                        cache.invoke(f_oKey, new ContinuationAsyncProcessor(new IncrementProcessor(i++), null, contException));
                        }
                    }
                catch (Throwable t)
                    {
                    atomicResult.set(t);
                    }
                }

            final Object f_oKey;
            }

        final Thread thdControl = new Thread()
            {
            public void run()
                {
                while (atomicResult.get() == null)
                    {
                    int i = atomicIdx.incrementAndGet();

                    startCacheServer(sServer + "-" + i, s_sProject, s_sCacheConfig);

                    waitForNodeSafe(service);

                    stopCacheServer(sServer + "-" + (i - 1), true);
                    }
                }
            };

        try
            {
            startCacheServer(sServer + "-" + atomicIdx.incrementAndGet(),
                s_sProject, s_sCacheConfig);
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(1));

            ClientThread[] aThreads = new ClientThread[10];
            int            cThreads = 8;
            for (int i = 0; i < cThreads; i++)
                {
                aThreads[i] = new ClientThread(i);
                aThreads[i].start();
                }

            thdControl.start();

            try
                {
                long cWait = 30000L;
                for (int i = 0; i < cThreads; i++)
                    {
                    long ldtStart = System.currentTimeMillis();
                    aThreads[i].join(cWait);
                    cWait = Math.max(1, cWait - (System.currentTimeMillis() - ldtStart));
                    }

                atomicResult.compareAndSet(null, Boolean.TRUE);
                }
            catch (InterruptedException e)
                {
                atomicResult.set(e);
                }

            try
                {
                thdControl.join();
                }
            catch (InterruptedException e)
                {
                }

            assertEquals(Boolean.TRUE, atomicResult.get());
            }
        finally
            {
            stopCacheServer(sServer + "-" + atomicIdx.get());
            }
        }

    // ----- helpers --------------------------------------------------------

    public Object dereference(AtomicReference ref)
        {
        return ref.get();
        }

    // ----- inner class: ContinuationAsyncProcessor ------------------------

    /**
     * AsynchronousProcessor implementation controlled by continuations.
     */
    public static class ContinuationAsyncProcessor
            extends AsynchronousProcessor
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a ContinuationAsyncProcessor.
         *
         * @param agent          the EntryProcessor agent to run asynchronously
         * @param contResult     the result continuation
         * @param contException  the exception continuation
         */
        public ContinuationAsyncProcessor(InvocableMap.EntryProcessor agent,
                                          Continuation<Object> contResult,
                                          Continuation<Throwable> contException)
            {
            super(agent);

            f_contResult    = contResult    == null ? NullImplementation.getContinuation() : contResult;
            f_contException = contException == null ? NullImplementation.<Throwable>getContinuation() : contException;
            }

        // ----- AsynchronousProcessor methods ------------------------------

        @Override
        public void onComplete()
            {
            super.onComplete();

            if (!isCompletedExceptionally())
                {
                f_contResult.proceed(getResult());
                }
            }

        @Override
        public void onException(Throwable eReason)
            {
            super.onException(eReason);

            f_contException.proceed(eReason);
            }

        // ----- data members -----------------------------------------------

        /**
         * The result continuation.
         */
        protected final Continuation<Object> f_contResult;

        /**
         * The exception continuation.
         */
        protected final Continuation<Throwable> f_contException;
        }

    // ----- inner class: IncrementProcessor --------------------------------

    /**
     * An EntryProcessor which validates the expected value of a cache entry
     * and increments it.
     */
    public static class IncrementProcessor
            extends AbstractProcessor
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an IncrementProcessor with the specified expected value.
         *
         * @param nExpect  the expected value
         */
        public IncrementProcessor(int nExpect)
            {
            m_nExpect = nExpect;
            }

        // ----- EntryProcessor methods -------------------------------------

        /**
         * {@inheritDoc}
         */
        public Object process(InvocableMap.Entry entry)
            {
            Integer NValueOld = (Integer) entry.getValue();
            int     nValueOld = NValueOld == null ? 0 : NValueOld.intValue();

            if (nValueOld != m_nExpect)
                {
                Base.log("FAILURE for key " + entry.getKey() + ": expected value " + m_nExpect + " but was " + nValueOld);
                assertEquals(m_nExpect, nValueOld);
                }

            entry.setValue(nValueOld + 1);

            return nValueOld;
            }

        /**
         * {@inheritDoc}
         */
        public String toString()
            {
            return "IncrementProcessor{Expected=" + m_nExpect + "}";
            }

        // ----- data members -----------------------------------------------

        /**
         * The expected value.
         */
        protected int m_nExpect;
        }


    // ----- constants and data members -------------------------------------

    /**
     * The path to the cache configuration.
     */
    public final static String s_sCacheConfig = "coherence-cache-config.xml";

    /**
     * The path to the Ant build script.
     */
    public final static String s_sBuild       = "build.xml";

    /**
     * The project name.
     */
    public final static String s_sProject     = "processor";
    }
