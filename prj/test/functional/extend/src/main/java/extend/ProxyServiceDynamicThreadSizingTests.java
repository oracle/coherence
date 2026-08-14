/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.component.net.Cluster;

import com.tangosol.coherence.component.util.SafeCluster;

import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.internal.util.DaemonPoolSizing;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.InvocationService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Filter;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.PartitionedFilter;

import java.io.IOException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
* A collection of functional tests for ProxyService dynamic thread pool sizing that use the
* "dist-extend-direct" cache.
*
* @author lh  2012.01.17
*/
public class ProxyServiceDynamicThreadSizingTests
        extends AbstractExtendTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public ProxyServiceDynamicThreadSizingTests()
        {
        super(CACHE_DIST_EXTEND_DIRECT);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        m_fCheckInitialThreadCount = true;
        System.setProperty("coherence.daemonpool.adjust.period", "500");
        System.setProperty("coherence.daemonpool.max.period", "1000");
        System.setProperty("coherence.daemonpool.min.period", "100");
        System.setProperty("coherence.daemonpool.grow.percentage", "0.5");
        System.setProperty("coherence.daemonpool.debug", "true");

        startCacheServerWithProxy("ProxyServiceThreadSizingTests", "extend", "server-cache-config-threadpool.xml");
        ensureRunningService("ProxyServiceThreadSizingTests", "ExtendTcpProxyService");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("ProxyServiceThreadSizingTests");
        System.clearProperty("coherence.daemonpool.adjust.period");
        System.clearProperty("coherence.daemonpool.max.period");
        System.clearProperty("coherence.daemonpool.min.period");
        System.clearProperty("coherence.daemonpool.grow.percentage");
        System.clearProperty("coherence.daemonpool.debug");
        }


    // ----- ProxyService ThreadPoolSizingStrategy test ---------------------

    /**
    * Verify that a proxy worker pool with no configured maximum receives a
    * finite derived maximum.
    */
    @Test
    public void shouldCoordinateCustomerShapedUnboundedPools()
        {
        int cProxyMax = getSizingValue("UnboundedTcpProxyService", ThreadCountInvocable.THREAD_COUNT_MAX);
        int cCacheMax = getSizingValue("DistributedCache", ThreadCountInvocable.SERVICE_THREAD_COUNT_MAX);
        int cBudget   = getSizingValue(null, ThreadCountInvocable.WORKER_BUDGET);
        int cAllocated = getSizingValue(null, ThreadCountInvocable.ALLOCATED_WORKER_MAX);
        int fOvercommitted = getSizingValue(null, ThreadCountInvocable.OVERCOMMITTED);
        int cProxyAllocations = getSizingValue("UnboundedTcpProxyService",
                ThreadCountInvocable.MATCHING_ALLOCATION_COUNT);

        assertThat(cProxyMax, greaterThanOrEqualTo(50));
        assertThat(cProxyMax, lessThan(Integer.MAX_VALUE));
        assertThat(cCacheMax, greaterThanOrEqualTo(50));
        assertThat(cCacheMax, lessThan(Integer.MAX_VALUE));
        assertThat(cProxyMax, greaterThanOrEqualTo(cCacheMax));
        assertThat(cProxyAllocations, is(1));

        if (fOvercommitted == 0)
            {
            assertThat(cAllocated <= cBudget, is(true));
            }
        else
            {
            assertThat(cProxyMax, is(50));
            assertThat(cCacheMax, is(50));
            }
        }

    /**
    * Test DefaultProxyServiceThreadPoolSizing using PartitionedFilter from an extend client.
    */
    // @Test
    public void testPartitionedFilter()
            throws InterruptedException
        {
        final NamedCache cache = getNamedCache();

        if (m_fCheckInitialThreadCount)
            {
            assertEquals(1, getThreadCount());
            m_fCheckInitialThreadCount = false;
            }

        // spawn a separate thread to check the thread count periodically
        ThreadCountChecker threadCountChecker = new ThreadCountChecker();
        Thread             checkerThread      = new Thread(threadCountChecker);
        checkerThread.start();

        Thread thds[] = new Thread[20];
        for (m_index = 0; m_index < thds.length; m_index++)
            {
            thds[m_index] = new Thread()
                {
                @Override
                public void run()
                    {
                    testPartitionedFilterPart(m_index, cache);
                    }
                };
            thds[m_index].start();
            }

        for (int i = 0; i < thds.length; i++)
            {
            thds[i].join();
            }

        Eventually.assertThat(invoking(threadCountChecker).getMaxThreadCount(), greaterThan(1));

        threadCountChecker.stop();
        checkerThread.join();

        Thread.sleep(1000);
        int threadCount = getThreadCount();
        int count = 0;
        while (threadCount > 1)
            {
            if (count++ > 20)
                break;
            for (int i = 0; i < 20; i++)
                {
                testPartitionedFilterPart(i, cache);
                Thread.sleep(400);
                }

            Thread.sleep(1000);
            int lastThreadCount = threadCount;
            threadCount = getThreadCount();
            if (threadCount < lastThreadCount)
                {
                threadCount = 1;
                break;
                }
            }

        assertEquals(1, threadCount);
        }

    protected void testPartitionedFilterPart(int index, NamedCache cache)
        {
        int cPartitions  = 257;                     // default partition size

        // fill the cache with some data
        for (int i = index; i < index + 500; i++)
            {
            cache.put(i + "key" + i, "value" + i);
            }

        Set setPartitionKeys    = new HashSet();
        Set setPartitionEntries = new HashSet();

        PartitionSet parts = new PartitionSet(cPartitions);

        // run the query for a single partition at a time
        for (int iPartition = 0; iPartition < cPartitions; iPartition++)
            {
            parts.add(iPartition);
            Filter filter = new PartitionedFilter(AlwaysFilter.INSTANCE, parts);

            Set setTest;
            int cKeys, cEntries;

            setTest = cache.keySet(filter);
            cKeys   = setTest.size();
            setPartitionKeys.addAll(setTest);

            setTest  = cache.entrySet(filter);
            cEntries = setTest.size();
            setPartitionEntries.addAll(setTest);

            assertEquals(cKeys + "!=" + cEntries, cEntries, cKeys);
            parts.remove(iPartition);
            }
        }

    /**
    * Test the DefaultProxyServiceThreadPoolSizing with Invocation Service,
    * {@link com.tangosol.net.InvocationService#query(com.tangosol.net.Invocable task, Set setMembers)}.
    *
    * Comment out the test because it is no longer reliable.
    */
//    @Test
    public void testQuery()
            throws InterruptedException
        {
        final InvocationService service = (InvocationService)
                getFactory().ensureService(InvocationExtendTests.INVOCATION_SERVICE_NAME);
        try
            {
            OperationalContext ctx = (OperationalContext) CacheFactory.getCluster();

            assertNotNull(ctx);
            assertNotNull(ctx.getSocketProviderFactory());
            assertNotNull(ctx.getLocalMember());

            if (m_fCheckInitialThreadCount)
                {
                assertEquals(1, getThreadCount());
                m_fCheckInitialThreadCount = false;
                }

            // spawn a separate thread to check the thread count periodically
            ThreadCountChecker threadCountChecker = new ThreadCountChecker();
            Thread             checkerThread      = new Thread(threadCountChecker);
            checkerThread.start();

            Thread thds[] = new Thread[200];
            for (m_index = 0; m_index < thds.length; m_index++)
                {
                thds[m_index] = new Thread()
                    {
                    @Override
                    public void run()
                        {
                        for (int i = 0; i < 400; i++)
                            {
                            query(service);
                            }
                        }
                    };
                thds[m_index].start();
                }

            for (int i = 0; i < thds.length; i++)
                {
                thds[i].join();
                }

            int maxCount = threadCountChecker.getMaxThreadCount();
            int count    = 0;
            while (maxCount <= 1)
                {
                if (count++ > 20)
                    break;
                Thread.sleep(500);
                maxCount = threadCountChecker.getMaxThreadCount();
                }

            threadCountChecker.stop();
            checkerThread.join();
            assertThat(maxCount, is(greaterThan(1)));

            Thread.sleep(1000);
            int threadCount = getThreadCount();
            count = 0;
            while (threadCount > 1)
                {
                if (count++ > 20)
                    break;
                for (int i = 0; i < 20; i++)
                    {
                    query(service);
                    Thread.sleep(400);
                    }

                Thread.sleep(1000);
                int lastThreadCount = threadCount;
                threadCount = getThreadCount();
                if (threadCount < lastThreadCount)
                    {
                    threadCount = 1;
                    break;
                    }
                }

            assertEquals(1, threadCount);
            }
        catch (InterruptedException e)
            {}
        finally
            {
            service.shutdown();
            }
        }

    protected void query(InvocationService service)
        {
        InvocationExtendTests.TestInvocable task = new InvocationExtendTests.TestInvocable();
        task.setValue(6);
        Map map = service.query(task, null);

        assertTrue(map != null);
        assertTrue(map.size() == 1);

        Object oMember = map.keySet().iterator().next();
        assertTrue(equals(oMember, service.getCluster().getLocalMember()));

        Object oResult = map.values().iterator().next();
        assertTrue(oResult instanceof Integer);
        assertTrue(((Integer) oResult).intValue() == 7);
        }

    public int getThreadCount()
        {
        InvocationService service = (InvocationService)
                getFactory().ensureService(InvocationExtendTests.INVOCATION_SERVICE_NAME);

        ThreadCountInvocable task = new ThreadCountInvocable("ExtendTcpProxyService");
        task.setValue(0);
        Map map = service.query(task, null);

        assertTrue(map != null);
        assertTrue(map.size() == 1);

        Object oMember = map.keySet().iterator().next();
        assertTrue(equals(oMember, service.getCluster().getLocalMember()));

        Object oResult = map.values().iterator().next();
        assertTrue(oResult instanceof Integer);
        return ((Integer) oResult).intValue();
        }

    public int getThreadCountMax()
        {
        return getSizingValue("UnboundedTcpProxyService", ThreadCountInvocable.THREAD_COUNT_MAX);
        }

    public int getSizingValue(String sServiceName, int nMetric)
        {
        InvocationService service = (InvocationService)
                getFactory().ensureService(InvocationExtendTests.INVOCATION_SERVICE_NAME);

        ThreadCountInvocable task = new ThreadCountInvocable(sServiceName);
        task.setValue(nMetric);
        Map map = service.query(task, null);

        assertTrue(map != null);
        assertTrue(map.size() == 1);

        Object oResult = map.values().iterator().next();
        assertTrue(oResult instanceof Integer);
        return ((Integer) oResult).intValue();
        }

    // ----- inner class: ThreadCountInvocable ------------------------------

    /**
    * Invocable implementation that gets the thread count of the proxy service
    * thread pool.
    */
    public static class ThreadCountInvocable
            extends AbstractInvocable
            implements PortableObject
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor.
        */
        public ThreadCountInvocable()
            {
            }

        /**
        * Default constructor.
        */
        public ThreadCountInvocable(String sServiceName)
            {
            m_sServiceName = sServiceName;
            }

        // ----- Invocable interface --------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            SafeCluster  safeCluster = (com.tangosol.coherence.component.util.SafeCluster) CacheFactory.ensureCluster();
            Cluster      cluster     = (Cluster) safeCluster.getCluster();

            if (m_nValue == WORKER_BUDGET)
                {
                m_nValue = DaemonPoolSizing.getSnapshot().getBudget();
                return;
                }
            if (m_nValue == ALLOCATED_WORKER_MAX)
                {
                m_nValue = DaemonPoolSizing.getSnapshot().getAllocatedWorkerMax();
                return;
                }
            if (m_nValue == OVERCOMMITTED)
                {
                m_nValue = DaemonPoolSizing.getSnapshot().isOvercommitted() ? 1 : 0;
                return;
                }
            if (m_nValue == MATCHING_ALLOCATION_COUNT)
                {
                m_nValue = (int) DaemonPoolSizing.getSnapshot().getAllocations().keySet().stream()
                        .filter(sName -> sName.contains(m_sServiceName))
                        .count();
                return;
                }
            if (m_nValue == SERVICE_THREAD_COUNT_MAX)
                {
                Service service = (Service) cluster.getService(m_sServiceName);
                m_nValue = service == null ? 0 : service.getDaemonPool().getDaemonCountMax();
                return;
                }

            ProxyService service     = (ProxyService) cluster.getService(
                    m_sServiceName == null ? "ExtendTcpProxyService" : m_sServiceName);
            if (service != null)
                {
                com.tangosol.coherence.component.util.DaemonPool pool =
                        ((Acceptor) service.getAcceptor()).getDaemonPool();
                m_nValue = m_nValue == THREAD_COUNT_MAX
                        ? pool.getDaemonCountMax()
                        : pool.getDaemonCount();
                }
            }

        /**
        * {@inheritDoc}
        */
        public Object getResult()
            {
            return m_nValue;
            }

        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_nValue       = in.readInt(0);
            m_sServiceName = in.readString(1);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeInt(0, m_nValue);
            out.writeString(1, m_sServiceName);
            }

        // ----- accessors ------------------------------------------------

        /**
        * Set the integer value to increment.
        *
        * @param nValue  the value to increment
        */
        public void setValue(int nValue)
            {
            m_nValue = nValue;
            }

        // ----- data members ---------------------------------------------

        /**
        * The integer value to increment.
        */
        private int m_nValue;

        /**
        * The proxy service name.
        */
        private String m_sServiceName;

        /**
        * Request value used to return the configured maximum thread count.
        */
        public static final int THREAD_COUNT_MAX = -1;

        /** Request value used to return a service worker-pool maximum. */
        public static final int SERVICE_THREAD_COUNT_MAX = -2;

        /** Request value used to return the JVM-wide worker budget. */
        public static final int WORKER_BUDGET = -3;

        /** Request value used to return the aggregate allocated maximum. */
        public static final int ALLOCATED_WORKER_MAX = -4;

        /** Request value used to return the budget overcommit flag. */
        public static final int OVERCOMMITTED = -5;

        /** Request value used to count matching live-pool allocations. */
        public static final int MATCHING_ALLOCATION_COUNT = -6;
        }

    // ----- inner class: ThreadCountChecker --------------------------------

    /**
    * Runnable that checks whether the proxy service thread count has
    * exceeded 1, or is told to stop.
    */
    public class ThreadCountChecker
            implements Runnable
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        public ThreadCountChecker()
            {
            this(2);
            }

        /**
        * Constructor with threshold value.
        *
        * @param cThreshold  Stop when thread count reaches this value.
        */
        public ThreadCountChecker(int cThreshold)
            {
            m_cThreshold      = cThreshold;
            m_cMaxThreadCount = 0;
            m_fStop           = false;
            }

        // ----- Runnable interface -------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        public void run()
            {
            while (!m_fStop && m_cMaxThreadCount < m_cThreshold)
                {
                m_cMaxThreadCount = Math.max(m_cMaxThreadCount, getThreadCount());
                try
                    {
                    Thread.sleep(100);
                    }
                catch(InterruptedException e)
                    { }
                }
            }

        // ----- ThreadCountChecker interface ---------------------------

        /**
        * The maximum observed thread count.
        *
        * @return the maximum observed thread count
        */
        public int getMaxThreadCount()
            {
            return m_cMaxThreadCount;
            }

        /**
        * Signal the thread to stop checking the thread count.
        */
        public void stop()
            {
            m_fStop = true;
            }

        // ----- data members -------------------------------------------

        /**
        * Stop checking the thread count once this value is observed.
        */
        private final int m_cThreshold;

        /**
        * The maximum observed thread count.
        */
        private volatile int m_cMaxThreadCount;

        /**
        * Stop checking the thread count if this flag is set.
        */
        private volatile boolean m_fStop;
        }

    // ----- data members ---------------------------------------------------

    public int m_index;

    /**
    * check the initial thread count if this flag is set.
    */
    private static boolean m_fCheckInitialThreadCount;
    }
