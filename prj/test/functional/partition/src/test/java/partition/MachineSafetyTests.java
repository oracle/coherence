/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package partition;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;
import com.tangosol.coherence.component.util.safeService.safeCacheService.SafeDistributedCacheService;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.SegmentedConcurrentMap;
import com.tangosol.util.ValueManipulator;
import com.tangosol.util.processor.NumberIncrementor;

import common.AbstractMachineRollingRestartTest;

import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Tests for machine-safety.
 */
public class MachineSafetyTests
        extends AbstractMachineRollingRestartTest
    {

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    * <p/>
    * This method is a no-op, since each test starts and stops the cluster.
    */
    @BeforeClass
    public static void _startup()
        {
        // no-op
        }

    /**
    * Shutdown the test class.
    * <p/>
    * This method is a no-op, since each test starts and stops the cluster.
    */
    @AfterClass
    public static void _shutdown()
        {
        // no-op
        }

    // ----- Test methods -------------------------------------------------

    @Test
    public void noopPlaceHolder()
        {
        // no-op
        }

    /**
     * Test for 2 machines, with 3 JVMs each; backup-count=1.
     */
    @Test
    public void testTwoByThreeBC1()
        {
        doTest("simple-assignment", "test2x3-BC1-",
               /*cBackups*/ 1, new int[] {3, 3}, /*cRestart*/5);
        }

    /**
     * Test for 3 machines, with 1, 1 and 2 JVMs respectively; backup-count=1.
     */
    @Test
    public void test1_1_2BC1()
        {
        doTest("simple-assignment", "test1_1_2-BC1-",
               /*cBackups*/ 1, new int[] {1, 1, 2}, /*cRestart*/5);
        }

    /**
     * Test for 4 machines, with 2 JVMs each; backup-count=2.
     */
    @Test
    public void testFourByTwoBC2()
        {
        doTest("simple-assignment", "test4x2-BC2-",
               /*cBackups*/ 2, new int[] {2, 2, 2, 2}, /*cRestart*/5);
        }

    /**
     * Test for 4 machines, with 3 JVMs each; backup-count=2.
     */
    @Test
    public void testFourByThreeBC2()
        {
        doTest("simple-assignment", "test4x3-BC2-",
               /*cBackups*/ 2, new int[] {3, 3, 3, 3}, /*cRestart*/5);
        }

    /**
     * Test for 5 machines, with 2 JVMs each; backup-count=2.
     */
    @Test
    public void testFiveByTwoBC2()
        {
        doTest("simple-assignment", "test5x2-BC2-",
               /*cBackups*/ 2, new int[] {2, 2, 2, 2, 2}, /*cRestart*/5);
        }


    // ----- MachineSafetyTests methods -----------------------------------

    /**
     * Run the multi-machine safety tests with the specified pseudo-topology.
     *
     * @param sCacheName  the cache name
     * @param sTestName   the test name
     * @param cBackups    the backup-count to use
     * @param anMembers   the array of members (per machine) in the topology
     * @param cRestarts   the number of restarts to perform
     */
    protected void doTest(String sCacheName, String sTestName,
                          int cBackups, int[] anMembers, int cRestarts)
        {
        Properties props = new Properties();

        System.setProperty("tangosol.coherence.log.level", "9");
        System.setProperty("tangosol.coherence.distributed.backupcount", "" + cBackups);
        props.put("tangosol.coherence.distributed.backupcount", "" + cBackups);

        // start the cluster
        AbstractMachineRollingRestartTest._startup();

        Cluster                  cluster = CacheFactory.ensureCluster();
        final NamedCache         cache   = getNamedCache(sCacheName);
        final PartitionedService service = (PartitionedService) ((SafeDistributedCacheService) cache.getCacheService()).getService();

        final MemberHandler memberHandler = new MemberHandler(
                cluster, "MultiMachine-" + sTestName,
                /*fExternalKill*/true, /*fGraceful*/false,
                props, cBackups);

        try
            {
            final int cKeys    = 1000;
            int       cThreads = 4;
            int       cClusterSize = 1;

            // setup the initial topology
            for (int i = 0, cMachines = anMembers.length; i < cMachines; i++)
                {
                for (int j = 0, nMembers = anMembers[i]; j < nMembers; j++)
                    {
                    memberHandler.addServer("machine-" + i);
                    cClusterSize++;
                    }
                }

            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(cClusterSize));
            Eventually.assertThat(invoking(cache.getCacheService()).
                getInfo().getServiceMembers().size(), is(cClusterSize));

            // pre-load the cache
            Map map = new HashMap(cKeys);
            for (int i = 0; i < cKeys; i++)
                {
                map.put(i, 0);
                }
            cache.putAll(map);

            // start the client (load) threads
            final boolean[]     afExiting   = new boolean[1];
            final Object[]      aoException = new Object[1];
            Thread[]            aThreads    = new Thread[cThreads];
            final int[]         aExpected   = new int[cKeys];
            final ConcurrentMap mapLock     = new SegmentedConcurrentMap();

            for (int i = 0; i < cThreads; i++)
                {
                aThreads[i] = new Thread()
                    {
                    public void run()
                        {
                        EntryProcessor processor = new NumberIncrementor((ValueManipulator) null, 1, false);
                        Random         random    = new Random();
                        try
                            {
                            while (!afExiting[0])
                                {
                                int iKey = random.nextInt(cKeys);

                                mapLock.lock(iKey, -1);
                                try
                                    {
                                    Integer IResult = (Integer) cache.invoke(iKey, processor);
                                    assertTrue("Missing key " + iKey + "belonging to partition " + service.getKeyPartitioningStrategy().getKeyPartition(iKey), IResult != null);

                                    aExpected[iKey]++;
                                    assertEquals("Wrong value for key " + iKey, aExpected[iKey], IResult.intValue());
                                    }
                                finally
                                    {
                                    mapLock.unlock(iKey);
                                    }
                                }
                            }
                        catch (Throwable t)
                            {
                            synchronized (aoException)
                                {
                                aoException[0] = t;
                                }

                            throw Base.ensureRuntimeException(t);
                            }
                        }
                    };
                aThreads[i].start();
                }

            // start the rolling restart
            final int nSafeClusterSize = cClusterSize;
            Runnable runSafe = new Runnable()
                {
                @Override
                public void run()
                    {
                    // rolling restart will kill oldest server, before doing so we ensure that that member has seen
                    // prior deaths (by asserting cluster size), and that it believes things to be machine safe
                    CoherenceClusterMember memberControl = findApplication(memberHandler.getOldestMember().getRoleName());

                    Eventually.assertThat(invoking(memberControl).getClusterSize(), is(nSafeClusterSize));
                    Eventually.assertThat(invoking(MachineSafetyTests.this)
                            .getServiceStatus(memberControl,cache.getCacheService().getInfo().getServiceName()),
                            is(ServiceStatus.MACHINE_SAFE.name()), within(5, TimeUnit.MINUTES));
                    }

                };
            doRollingRestart(memberHandler, cRestarts, runSafe);

            Eventually.assertThat(invoking(cache.getCacheService()).
                getInfo().getServiceMembers().size(), is(cClusterSize));

            // stop the client load
            afExiting[0] = true;
            for (int i = 0; i < cThreads; i++)
                {
                try
                    {
                    aThreads[i].join();
                    }
                catch (InterruptedException e)
                    {}
                }

            synchronized (aoException)
                {
                assertNull(aoException[0]);
                }
            }
        finally
            {
            try
                {
                memberHandler.dispose();
                }
            finally
                {
                // stop the cluster, as we may be changing the backup-count between tests
                AbstractMachineRollingRestartTest._shutdown();
                }
            }
        }

    public String getServiceStatus(CoherenceClusterMember member, String sService)
        {
        return member.getServiceStatus(sService).name();
        }


    // ----- AbstractMachineRollingRestartTest methods --------------------

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


    // ----- inner class: MemberHandler -----------------------------------

    /**
     * Specialization of MemberHandler to handle "machine-level" bounces.
     */
    public class MemberHandler
            extends AbstractMachineRollingRestartTest.MemberHandler
        {
        /**
        * Create and register a MemberHandler for the specified cluster. If
        * fExternal is not specified, an invocable will execute on the member
        * to be killed which will ensure that isRemoteKill() will evaluate true.
        *
        * @param cluster        the cluster to register a MemberHandler for
        * @param sPrefix        the string prefix to use for created members
        * @param fExternalKill  true iff members killed by the handler should
        *                       be killed "externally" (e.g. kill -9)
        * @param fGracefulKill  true iff the members should be added or killed
        *                       by the handler gracefully
        * @param props          the default set of properties to pass to
        *                       servers started by this member handler
        * @param cBackups       the backup-count (# of machines to kill simultaneously)
        */
        public MemberHandler(Cluster cluster, String sPrefix,
                             boolean fExternalKill, boolean fGracefulKill,
                             Properties props, int cBackups)
            {
            super(cluster, sPrefix, fExternalKill, fGracefulKill, props);

            m_cBackups = cBackups;
            }


        // ----- MemberHandler methods ------------------------------------

        /**
         * {@inheritDoc}
         */
        public void bounce()
            {
            // kill cBackups machines at a time
            int   cBackups   = m_cBackups;
            int[] anMembers  = new int[cBackups];

            // find all of the machines to kill
            List listMachines = new ArrayList(cBackups);
            int  cKill        = 0;
            for (Iterator iter = m_listServers.iterator(); cKill < cBackups && iter.hasNext(); )
                {
                Member member   = (Member) iter.next();
                String sMachine = member.getMachineName();

                if (!listMachines.contains(sMachine))
                    {
                    listMachines.add(sMachine);
                    cKill++;
                    }
                }

            // kill all of the machines first
            String[] asMachines = (String[]) listMachines.toArray(new String[listMachines.size()]);
            for (int i = 0; i < cKill; i++)
                {
                anMembers[i] = killAll(asMachines[i]);
                }

            // restart all of the members on the killed machines
            for (int i = 0; i < cKill; i++)
                {
                String sMachine = asMachines[i];
                for (int j = 0, nMembers = anMembers[i]; j < nMembers; j++)
                    {
                    addServer(sMachine);
                    }
                }
            }

        // ----- data members ---------------------------------------------

        /**
         * The backup-count.
         */
        protected int m_cBackups;
        }


    // ----- constants and data members -----------------------------------

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
    public final static String s_sProject     = "partition";
    }