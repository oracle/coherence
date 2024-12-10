/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package partition;


import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.BackingMapBinaryEntry;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.filter.LessEqualsFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

public class RegressionTests
    extends AbstractFunctionalTest
    {
    public RegressionTests()
        {
        super("cache-config.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.distributed.threads.min", "8");

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    @Test
    public void testPutAllMemory()
            throws Exception
        {
        // test regression for COH-27680

        // force this node to be client to the entire putAll
        AbstractFunctionalTest._shutdown();
        System.setProperty("coherence.distributed.localstorage", "false");
        System.setProperty("coherence.management", "all");
        System.setProperty("coherence.management.remote", "true");
        AbstractFunctionalTest._startup();

        Properties props = new Properties();
        props.setProperty("coherence.management", "all");
        props.setProperty("coherence.management.remote", "true");
        String sServerName = "storage1";
        CoherenceClusterMember clusterMember1 = startCacheServer(sServerName, "testPutAllMemory", "coherence-cache-config.xml", props);
        waitForServer(clusterMember1);

        NamedCache<String, String> cache = CacheFactory.getCache("dist-cache");
        waitForBalanced(cache.getCacheService());
        cache.clear();
        cache.addIndex(IdentityExtractor.INSTANCE);

        Random r = new Random();

        Map<String,String> mapData = new HashMap<>();
        for (int i = 0 ; i < 500; i++)
            {
            // one large value to verify we don't re-use the resulting larger
            // buffer
            mapData.put("Key" + i, i == 0 ? "Value12345678901234567890" : ("Value" + i));
            }

        cache.putAll(mapData);

        cache.invokeAll(entry ->
                        {
                        Binary binKey = ExternalizableHelper.toBinary(entry.getKey());
                        Binary binVal = ExternalizableHelper.toBinary(entry.getValue());

                        BackingMapBinaryEntry b =
                                new BackingMapBinaryEntry(binKey, binVal, binVal, null);

                        // backing map entry value size should match original
                        assertEquals(b.getBinaryValue().length(), entry.asBinaryEntry().getBinaryValue().length());

                        return entry;
                        });

        MBeanServer server = MBeanHelper.findMBeanServer();
        String sName = "Coherence:type=StorageManager,service=" + cache.getCacheService().getInfo().getServiceName()
                       + ",cache=" + cache.getCacheName() + ",nodeId=2";
        final ObjectName name = new ObjectName(sName);

        //CacheFactory.log("IndexTotalUnits: " + indexTotalUnits);
        Eventually.assertDeferred(() ->
                                  {
                                  Long indexTotalUnits = 0L;
                                  try
                                      {
                                      indexTotalUnits = (Long) server.getAttribute(name, "IndexTotalUnits");
                                      }
                                  catch (Exception e)
                                      {
                                      }
                                  return indexTotalUnits;
                                  },    not(0));

        cache.removeIndex(IdentityExtractor.INSTANCE);
        cache.clear();
        }

    @Test
    public void testCoh5575()
            throws InterruptedException
        {
        final NamedCache cache = CacheFactory.getCache("dist-cache");

        for (int i = 0; i < MAXKEY; i++)
            {
            cache.put(i, 50);
            }

        cache.addIndex(IdentityExtractor.INSTANCE, true, null);

        new Thread()
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public void run()
                {

                while (m_fRun)
                    {
                    int val = m_rnd.nextInt(MAXRND);
                    Set<Map.Entry<Integer, Integer>> set = cache.entrySet(new LessEqualsFilter(
                            IdentityExtractor.INSTANCE, val));

                    for (Map.Entry<Integer, Integer> entry : set)
                        {
                        Assert.assertTrue(entry.getValue() <= val);
                        }
                    }
                }
        }.start();

        new Thread()
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public void run()
                {
                while (m_fRun)
                    {
                    int val = m_rnd.nextInt(100);
                    Set<Map.Entry<Integer, Integer>> set = cache.entrySet(new EqualsFilter(IdentityExtractor.INSTANCE,
                            val));
                    for (Map.Entry<Integer, Integer> entry : set)
                        {
                        Assert.assertEquals(val, (int) entry.getValue());
                        }
                    }
                }
        }.start();

        new Thread()
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public void run()
                {
                while (m_fRun)
                    {
                    int val = m_rnd.nextInt(MAXRND);
                    Set<Map.Entry<Integer, Integer>> set = cache.entrySet(new GreaterEqualsFilter(
                            IdentityExtractor.INSTANCE, val));
                    for (Map.Entry<Integer, Integer> entry : set)
                        {
                        Assert.assertTrue(entry.getValue() >= val);
                        }
                    }
                }
        }.start();

        Thread thds[] = new Thread[5];
        for (int i = 0; i < thds.length; i++)
            {
            thds[i] = new Thread()
            {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run()
                    {
                    for (int i = 0; i < 10000; i++)
                        {
                        cache.put(m_rnd.nextInt(MAXKEY), m_rnd.nextInt(MAXRND));
                        cache.remove(m_rnd.nextInt(MAXKEY));
                        }
                    }
            };
            thds[i].start();
            }

        for (int i = 0; i < thds.length; i++)
            {
            thds[i].join();
            }

        m_fRun = false;
        }

    private static final int MAXRND = 100;
    private static final int MAXKEY = MAXRND * 100;

    public Random m_rnd = new Random();
    public volatile boolean m_fRun = true;
    }
