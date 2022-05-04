/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Blocking;

import com.oracle.coherence.common.util.Threads;
import com.sun.management.OperatingSystemMXBean;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.PartitionedFilter;

import com.oracle.coherence.testing.TestCoh15021;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.OutputStream;

import java.lang.management.ManagementFactory;

import java.net.Socket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
* A collection of functional tests for Coherence*Extend that use the
* "dist-extend-direct" cache.
*
* @author jh  2005.11.29
*/
public class DistExtendDirectTests
        extends AbstractExtendTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistExtendDirectTests()
        {
        super(CACHE_DIST_EXTEND_DIRECT);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        setupProps();
        m_cMember = startCacheServerWithProxy("DistExtendDirectTests", "extend", FILE_SERVER_CFG_CACHE);
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistExtendDirectTests");
        }

    // ----- PartitionedFilter test -----------------------------------------

    /**
    * Test the PartitionedFilter from an extend client.
    */
    @Test
    public void testPartitionedFilter()
        {
        NamedCache cache = getNamedCache();
        int cPartitions  = 257;					// default partition size

        // fill the cache with some data
        for (int i = 0; i < 500; i++)
            {
            cache.put(i+"key"+i, "value"+i);
            }

        int cSize               = cache.size();
        int cPartitionSize      = 0;
        Set setKeys             = cache.keySet();
        Set setEntries          = cache.entrySet();
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
            cPartitionSize += cEntries;
            parts.remove(iPartition);
            }

        assertEquals(cSize, cPartitionSize);
        assertEqualKeySet(setKeys, setPartitionKeys);
        assertEqualEntrySet(setEntries, setPartitionEntries);
        }

    /**
    * Test COH-13434 Support Sliding Expiry Out of the Box
    */
    @Test
    public void testExpirySliding()
        {
        long  cExpiry = 3000L;
        long  cSleep  = 1000L;
        int   cSize   = 10;
        Map   map     = new HashMap();
        Set   setKeys = new HashSet();

        for (int i = 0; i < cSize; i++)
            {
            map.put(i, i);
            setKeys.add(i);
            }

        NamedCache cache = getNamedCache("dist-expiry-sliding");
        // key 1's expiry should be extended after the get call
        cache.putAll(map);
        Base.sleep(2 * cSleep);
        cache.get(1);
        Base.sleep(cExpiry - 2 * cSleep + 250L);
        assertEquals(1, cache.size());
        cache.clear();

        // all keys's expiry should be extended after the getAll call
        cache.putAll(map);
        Base.sleep(2 * cSleep);
        cache.getAll(setKeys);
        Base.sleep(cExpiry - 2 * cSleep + 250);
        assertEquals(cache.size(), cSize);
        cache.clear();
        }

    @Test
    public void testIsServiceRunning()
        {
        NamedCache cache = getNamedCache();
        assertFalse(CacheFactory.getCluster().isRunning());
        assertTrue(cache.getCacheService().isRunning());
        }

    /**
    * Test COH-19392 port scan with random data
    */
    @Test
    public void testPortScan() throws Exception
        {
        Eventually.assertThat(m_cMember.isServiceRunning("ExtendTcpProxyService"), is(true));
        double cpuBefore = getCPUAverage();

        try
            {
            Properties props = System.getProperties();
            String     sHost = props.getProperty("test.extend.address.remote");
            int        nPort = Integer.parseInt(props.getProperty("test.extend.port"));

            for (int i = 0; i < 20; i++)
                {
                Socket       socket = new Socket(sHost, nPort);
                OutputStream os     = socket.getOutputStream();

                // test two problem cases to ensure we handle them;
                // randomly generate the rest of the data
                byte[] ab;
                if (i == 5)
                    {
                    ab = Base.parseHex("bfcdd4ec123244cd");
                    }
                else if (i == 10)
                    {
                    ab = Base.parseHex("90dc8ccbc85e95811849c7067eeb2d998dda9b320b7510fddcd0dbe39c");
                    }
                else
                    {
                    ab = new byte[getRandom().nextInt(100)];
                    getRandom().nextBytes(ab);
                    }

                os.write(ab);
                }
            }
        catch (Throwable oops)
            {
            err(oops);
            }

        // wait a while to make sure there is no CPU spike after the test
        sleep(20000);

        double cpuAfter = getCPUAverage();
        double result   = cpuAfter - cpuBefore;

        System.out.println(String.format("testPortScan CPU usages before and after the test: before=%f, after=%f, delta=%f, expected-delta=<.1", cpuBefore, cpuAfter, result));
        if (result > .1)
            {
            System.out.println("testPortScan detected higher CPU usage after the test, gathering thread dumps to check whether thread is busy...");
            int cSuccess = 0;
            for (int i = 0; i < 5; i++)
                {
                String dump     = m_cMember.invoke(new RemoteThreadDump());
                int    cTcpProc = dump.indexOf("Proxy:ExtendTcpProxyService:TcpAcceptor:TcpProcessor");
                int    cDaemon  = dump.indexOf("com.tangosol.coherence.component.util.Daemon.run", cTcpProc);
                int    cDiff    = cDaemon - cTcpProc;

                System.out.println("cDiff: " + cDiff);
                Blocking.sleep(1000);

                // cDiff should be less than 1500 characters; otherwise,
                // the Proxy:ExtendTcpProxyService:TcpAcceptor:TcpProcessor
                // thread is busy doing work.
                if (cDiff < 1500)
                    {
                    cSuccess++;
                    }
                else
                    {
                    System.out.println(String.format("\n====== BEGIN DUMP %s =====\n", i));
                    System.out.println(dump);
                    System.out.println(String.format("\n====== END DUMP %s =====\n", i));
                    }
                }

            // fail the test if found busy thread
            if (cSuccess < 5)
                {
                fail("Detected busy thread. See thread dump(s) for details.");
                }
            }
        }

    @Test
    public void testCoh15021()
        {
        TestCoh15021.testCoh15021(getNamedCache());
        }

    // ----- helper methods -------------------------------------

    protected double getCPUAverage()
        {
        double cpu = 0;
        for (int i = 0; i < 5; i++)
            {
            cpu += m_cMember.invoke(new GetCPU());
            }

        return cpu / 5;
        }

    // ----- inner class: RemoteThreadDump ----------------------------------

    /**
    * Return a thread dump of the invoking member.
    */
    protected static class RemoteThreadDump
        implements RemoteCallable<String>
        {

        // ----- RemoteCallable methods -------------------------------------

        @Override
        public String call() throws Exception
            {
            return Threads.getThreadDump();
            }
        }

    // ----- inner class: GetCPU --------------------------------------------

    /**
    * Return the CPU usage of the current JVM.
    */
    public static class GetCPU
            implements RemoteCallable<Double>
        {
        public GetCPU()
            {
            }

        /**
        * {@inheritDoc}
        */
        @Override
        public Double call()
            {
            try
                {
                OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                return operatingSystemMXBean.getProcessCpuLoad();
                }
            catch (Exception e)
                {
                CacheFactory.err(e);
                throw e;
                }
            }
        }

    // ----- data members ---------------------------------------------------

    private static CoherenceClusterMember m_cMember;
    }
