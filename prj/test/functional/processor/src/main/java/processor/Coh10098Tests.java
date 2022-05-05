/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package processor;


import com.oracle.coherence.common.base.Continuation;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.InvocationService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.partition.TransferEvent;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.NullImplementation;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.AsynchronousProcessor;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.is;


/**
 * Regression tests for COH-10098 (Async Invocation ordering issues).
 * <p>
 * These tests validate that the unit-of-order guarantees for asynchronous
 * agent invocation are maintained even during failure (and rescinding) of a
 * primary transfer.
 *
 * @author rhl 2013.08.27
 */
public class Coh10098Tests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public Coh10098Tests()
        {
        super(CACHE_CONFIG);
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testInvokeSvcThread()
        {
        System.setProperty("coherence.distributed.threads", "0");
        System.setProperty("coherence.distributed.transport.reliable", "datagram");

        doTestInvoke("testCoh10098InvokeSvc");
        }
//
//    @Test
//    public void testInvokeWorkers()
//        {
//        System.setProperty("coherence.distributed.threads", "4");
//        System.setProperty("coherence.distributed.transport.reliable", "datagram");
//
//        doTestInvoke("testCoh10098InvokeWorkers");
//        }
//
//    @Test
//    public void testInvokePreprocessing()
//        {
//        System.setProperty("coherence.distributed.threads", "-1");
//        System.setProperty("coherence.distributed.transport.reliable", "tmb");
//
//        doTestInvoke("testCoh10098InvokePreprocessing", props);
//        }

    // ----- test helpers -------------------------------------------------

    /**
     * Return the sum of the specified list of atomic integers.
     *
     * @param aCounters  the array of atomic integers to sum
     *
     * @return the sum
     */
    public int sumCounters(AtomicInteger[] aCounters)
        {
        int cSum = 0;
        for (AtomicInteger counter : aCounters)
            {
            cSum += counter == null ? 0 : counter.get();
            }

        return cSum;
        }

    /**
     * Run the test.
     *
     * @param sTest  the test name
     */
    protected void doTestInvoke(final String sTest)
        {
        final InvocationService serviceInv = (InvocationService)
                getFactory().ensureService("InvocationService");

        CoherenceClusterMember clusterMember = startCacheServer(sTest + "-main", PROJECT, CACHE_CONFIG);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("InvocationService"), is(true));

        serviceInv.query(new ServiceStartInvocable(),
                Collections.singleton(findCacheServer(sTest + "-main")));

        Eventually.assertThat(invoking(clusterMember).isServiceRunning("PartitionedCache"), is(true));

        final int cIters = 10;
        try
            {
            final AtomicInteger   atomicState     = new AtomicInteger();
            final AtomicReference atomicResult    = new AtomicReference();

            final int             cKeys           = 1;  // TODO: when COH-10532 is fixed,
                                                        // cKeys can be increased to >1
            final AtomicInteger[] aSubmitCounters = new AtomicInteger[cKeys + 1];
            final AtomicInteger[] aCommitCounters = new AtomicInteger[cKeys + 1];
            final Set             setKeys         = new HashSet();
            for (int i = 1; i <= cKeys; i++)
                {
                setKeys.add(i);
                aSubmitCounters[i] = new AtomicInteger();
                aCommitCounters[i] = new AtomicInteger();
                }

            final Thread thdClient = new Thread()
                {
                protected AsynchronousProcessor getProcessor(Set<Integer> setKeys)
                    {
                    Map mapExpect = new HashMap();
                    for (int nKey : setKeys)
                        {
                        mapExpect.put(nKey, aSubmitCounters[nKey].incrementAndGet());
                        }

                    return new ContinuationAsyncProcessor(new IncrementProcessor(mapExpect), f_contResult, f_contException);
                    }

                public void run()
                    {
                    try
                        {
                        NamedCache cache = getNamedCache("foo");
                        for (int i = 1 ; i <= cKeys; i++)
                            {
                            cache.put(i, 1);
                            }

                        while (atomicState.get() == 0)
                            {
                            cache.invoke(1, getProcessor(Collections.singleton(1)));
                            cache.invokeAll(AlwaysFilter.INSTANCE, getProcessor(setKeys));
                            cache.invokeAll(setKeys, getProcessor(setKeys));

                            Base.sleep(10L);
                            }

                        int  cSubmitted = sumCounters(aSubmitCounters);
                        Eventually.assertThat(invoking(Coh10098Tests.this).sumCounters(aCommitCounters), is(cSubmitted));

                        atomicResult.compareAndSet(null, Boolean.TRUE);
                        }
                    catch (Throwable t)
                        {
                        CacheFactory.log(Base.printStackTrace(t), 1);
                        atomicResult.set(t);
                        }
                    }

                Continuation<Throwable> f_contException = e ->
                    {
                    CacheFactory.log(Base.printStackTrace(e), 1);
                    atomicResult.set(e);
                    };

                Continuation<Map.Entry> f_contResult = entry ->
                    {
                    try
                        {
                        int nKey    = ((Integer) entry.getKey()).intValue();
                        int nResult = ((Integer) entry.getValue()).intValue();

                        AtomicInteger commitCounter = aCommitCounters[nKey];
                        assertEquals("Wrong value for key " + nKey, commitCounter.get() + 1, nResult);

                        boolean fCasResult = commitCounter.compareAndSet(nResult - 1, nResult);
                        assertTrue("Wrong value for key " + nKey, fCasResult);
                        }
                    catch (Throwable t)
                        {
                        CacheFactory.log(Base.printStackTrace(t), 1);
                        atomicResult.set(t.getStackTrace());
                        }
                    };
                };

            final Thread thdControl = new Thread()
                {
                public void run()
                    {
                    try
                        {
                        for (int i = 0; i < cIters && atomicResult.get() == null; i++)
                            {
                            Base.sleep(1000L);

                            // ungraceful start to avoid validation of process start
                            String sServerName = sTest + "-" + i;
                            CoherenceClusterMember clusterMember = startCacheServer(sServerName, PROJECT, CACHE_CONFIG, null, false);
                            Eventually.assertThat(invoking(clusterMember).isServiceRunning("InvocationService"), is(true));

                            serviceInv.query(new ServiceStartInvocable(),
                                    Collections.singleton(findCacheServer(sServerName)));

                            Eventually.assertThat(invoking(clusterMember).isServiceRunning("PartitionedCache"), is(true));

                            // sleep for a bit to allow distribution to settle and to
                            // let a emptyPipeline of async requests to build up

                            Base.sleep(7000L);
                            }
                        }
                    catch (Throwable t)
                        {
                        CacheFactory.log(Base.printStackTrace(t), 1);
                        atomicResult.set(t.getStackTrace());
                        }
                    finally
                        {
                        atomicState.set(1);
                        }

                    CacheFactory.log("Control Thread terminating: " + atomicState.get(), 2);
                    }
                };

            thdClient.start();
            thdControl.start();

            try
                {
                thdClient.join(300000L);
                if (thdClient.isAlive())
                    {
                    GuardSupport.logStackTraces();
                    }
                }
            catch (InterruptedException ie)
                {
                fail("Test run interrupted");
                }

            assertEquals("submitted=" + sumCounters(aSubmitCounters) + ", completed=" + sumCounters(aCommitCounters), Boolean.TRUE, atomicResult.get());
            }
        finally
            {
            stopAllApplications();

            CacheFactory.shutdown();
            }
        }

    // ----- inner class: ContinuationAsyncProcessor ----------------------

    /**
     * AsynchronousProcessor implementation controlled by continuations.
     */
    public static class ContinuationAsyncProcessor
            extends AsynchronousProcessor
        {
        // ----- constructors ---------------------------------------------

        /**
         * Construct a ContinuationAsyncProcessor.
         *
         * @param agent          the EntryProcessor agent to run asynchronously
         * @param contResult     the result continuation
         * @param contException  the exception continuation
         */
        public ContinuationAsyncProcessor(InvocableMap.EntryProcessor agent,
                                          Continuation<Map.Entry> contResult,
                                          Continuation<Throwable> contException)
            {
            super(agent);

            f_contResult    = contResult    == null ? NullImplementation.<Map.Entry>getContinuation() : contResult;
            f_contException = contException == null ? NullImplementation.<Throwable>getContinuation() : contException;
            }

        // ----- AsynchronousProcessor methods ----------------------------

        /**
         * {@inheritDoc}
         */
        public void onResult(Map.Entry entry)
            {
            f_contResult.proceed(entry);
            }

        public void onComplete()
            {}

        public void onException(Throwable eReason)
            {
            super.onException(eReason);

            f_contException.proceed(eReason);
            }

        // ----- data members ---------------------------------------------

        /**
         * The result continuation.
         */
        protected final Continuation<Map.Entry> f_contResult;

        /**
         * The exception continuation.
         */
        protected final Continuation<Throwable> f_contException;
        }

    // ----- inner class: IncrementProcessor ------------------------------

    /**
     * Processor to increment (and validate) an entry value.
     */
    public static class IncrementProcessor
            extends AbstractProcessor
        {
        public IncrementProcessor(Map mapExpect)
            {
            m_mapExpect = mapExpect;
            }

        // ------ EntryProcessor methods ----------------------------------

        /**
         * {@inheritDoc}
         */
        public Object process(InvocableMap.Entry entry)
            {
            int     nKey      = (Integer) entry.getKey();
            Integer NValueOld = (Integer) entry.getValue();
            int     nValueOld = NValueOld == null ? 0 : NValueOld.intValue();

            int nExpect = (Integer) m_mapExpect.get(nKey);
            if (nValueOld != nExpect)
                {
                BinaryEntry binEntry = (BinaryEntry) entry;
                String      sMsg     = "Wrong value for key " + nKey + " in partition "
                                     + binEntry.getContext().getKeyPartition(binEntry.getBinaryKey())
                                     + "; expected " + nExpect + ", but found " + nValueOld;

                Base.log(sMsg);
                assertEquals(sMsg, nExpect, nValueOld);
                }

            entry.setValue(nValueOld + 1);

            return nValueOld;
            }

        /**
         * {@inheritDoc}
         */
        public String toString()
            {
            return "IncrementProcessor{Expected=" + m_mapExpect + "}";
            }

        // ----- data members ---------------------------------------------

        /**
         * The expected value.
         */
        protected Map m_mapExpect;
        }

    // ----- inner class: TransferInterceptor -------------------------------

    public static class TransferInterceptor
            implements EventInterceptor<TransferEvent>
        {
        /**
         * {@inheritDoc}
         */
        public void onEvent(TransferEvent event)
            {
            int nPIDToKill = calculatePIDToKill(event.getService());
            if (event.getType() == TransferEvent.Type.ARRIVED &&
                event.getPartitionId() == nPIDToKill)
                {
                // Note: 5s is intentionally chosen based on a known wait time
                //       used by testlogic (4s) if it's PID watching process is
                //       unable to execute
                Base.log("Will halt the node in 5 seconds");
                Base.sleep(5000L);
                Runtime.getRuntime().halt(1);
                }
            }

        private int calculatePIDToKill(PartitionedService service)
            {
            int nPID = m_nPID;
            if (nPID == -1)
                {
                nPID = m_nPID = service.getKeyPartitioningStrategy().getKeyPartition(1);
                }

            return nPID;
            }

        private int m_nPID = -1;
        }

    // ----- inner class: ServiceStartInvocable -----------------------------

    public static class ServiceStartInvocable
            extends AbstractInvocable
        {
        public void run()
            {
            CacheFactory.getConfigurableCacheFactory()
                        .ensureService("PartitionedCache");
            }
        }

    // ----- constants ----------------------------------------------------

    /**
     * Cache configuration.
     */
    public static final String CACHE_CONFIG = "coh-10098-cache-config.xml";

    /**
     * Test project.
     */
    public static final String PROJECT      = "processor";
    }
