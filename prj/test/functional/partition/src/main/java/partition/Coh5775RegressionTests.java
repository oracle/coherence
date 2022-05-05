/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package partition;


import com.tangosol.net.NamedCache;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.filter.LessEqualsFilter;
import com.tangosol.util.filter.EqualsFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class Coh5775RegressionTests
        extends AbstractFunctionalTest
    {
    public Coh5775RegressionTests()
        {
        super("regression-cache-config.xml");
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
    public void test()
            throws InterruptedException
        {
        final NamedCache cache = getFactory().ensureCache("COH5775", null);

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
                try
                    {
                    while (m_fRun)
                        {
                        int val = 50;
                        Set<Map.Entry<Integer, Integer>> set = cache.entrySet(new LessEqualsFilter(
                                IdentityExtractor.INSTANCE, val));

                        for (Map.Entry<Integer, Integer> entry : set)
                            {
                            int value = entry.getValue();
                            Assert.assertTrue("Expected: " + value + " <= " + val, value <= val);
                            }
                        }
                    }
                catch (AssertionError e)
                    {
                    m_assert = e;
                    }
                }
        };

        new Thread()
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public void run()
                {
                try
                    {
                    while (m_fRun)
                        {
                        int val = 50;
                        Set<Map.Entry<Integer, Integer>> set = cache.entrySet(new EqualsFilter(
                                IdentityExtractor.INSTANCE, val));
                        for (Map.Entry<Integer, Integer> entry : set)
                            {
                            Assert.assertEquals(val, (int) entry.getValue());
                            }
                        }
                    }
                catch (AssertionError e)
                    {
                    m_assert = e;
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
                try
                    {
                while (m_fRun)
                    {
                    int val = m_rnd.nextInt(MAXRND);
                    Set<Map.Entry<Integer, Integer>> set = cache.entrySet(new GreaterEqualsFilter(
                            IdentityExtractor.INSTANCE, val));
                    for (Map.Entry<Integer, Integer> entry : set)
                        {
                        int value = entry.getValue();
                        Assert.assertTrue("Expected " + value + " >= " + val, value >= val);
                        }
                    }
                    }
                catch (AssertionError e)
                    {
                    m_assert = e;
                    }
                }
        };

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
                    for (int i = 0; i < 60000 && m_assert == null; i++)
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

        if (m_assert != null)
            {
            throw m_assert;
            }

        }

    private static final int MAXRND = 100;
    private static final int MAXKEY = MAXRND * 100;

    public Random m_rnd = new Random();
    public volatile boolean m_fRun = true;
    public volatile AssertionError m_assert = null;
    }
