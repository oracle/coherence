/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package partition;


import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.filter.LessEqualsFilter;
import com.tangosol.util.filter.EqualsFilter;
import common.AbstractFunctionalTest;

import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


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
        System.setProperty("tangosol.coherence.distributed.localstorage", "true");
        System.setProperty("tangosol.coherence.distributed.threads.min", "8");

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

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
