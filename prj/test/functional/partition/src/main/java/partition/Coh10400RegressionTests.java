/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package partition;


import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestPolicyException;
import com.tangosol.net.RequestTimeoutException;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestMapListener;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Regression tests for COH-10400 (single-client negative tests).
 *
 * @author rhl 2013.09.11
 */
public class Coh10400RegressionTests
        extends AbstractFunctionalTest
    {
    public Coh10400RegressionTests()
        {
        super("coherence-cache-config.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be disabled
        System.setProperty("coherence.distributed.localstorage", "false");

        AbstractFunctionalTest._startup();
        }

    // ----- Test methods -------------------------------------------------

    @Test
    public void testSize()
        {
        doTest(new Runnable()
                    {
                    public void run()
                        {
                        NamedCache cache = getNamedCache("dist-test");

                        cache.size();
                        }
                    });
        }

    @Test
    public void testPut()
        {
        doTest(new Runnable()
                    {
                    public void run()
                        {
                        NamedCache cache = getNamedCache("dist-test");

                        cache.put(1, 2);
                        }
                    });
        }

    @Test
    public void testGet()
        {
        doTest(new Runnable()
                    {
                    public void run()
                        {
                        NamedCache cache = getNamedCache("dist-test");

                        cache.get(1);
                        }
                    });
        }

    @Test
    public void testPutAll()
        {
        doTest(new Runnable()
                    {
                    public void run()
                        {
                        NamedCache cache = getNamedCache("dist-test");
                        HashMap map = new HashMap();
                        for (int i = 0; i < 100; i ++)
                            {
                            map.put(i, i);
                            }
                        cache.putAll(map);
                        }
                    });
        }

    @Test
    public void testClear()
        {
        doTest(new Runnable()
                    {
                    public void run()
                        {
                        NamedCache cache = getNamedCache("dist-test");
                        cache.clear();
                        }
                    });
        }

    @Test
    public void testAddIndex()
        {
        doTest(new Runnable()
                    {
                    public void run()
                        {
                        NamedCache cache = getNamedCache("dist-test");

                        cache.addIndex(new ReflectionExtractor("toString"), false, null);
                        }
                    });
        }

    @Test
    public void testAddListener()
        {
        doTest(new Runnable()
                    {
                    public void run()
                        {
                        NamedCache cache = getNamedCache("dist-test");

                        cache.addMapListener(new TestMapListener());
                        }
                    });
        }


    // ----- helper methods -----------------------------------------------

    protected void doTest(final Runnable runnable)
        {
        NamedCache cache = getNamedCache("dist-test"); // fetch the cache to remove
                                                       // cluster-formation time from the test

        final AtomicReference atomicRef = new AtomicReference();
        Thread t = new Thread()
            {
            public void run()
                {
                try
                    {
                    runnable.run();

                    atomicRef.set("expected exception not thrown");
                    }
                catch (RequestTimeoutException rte)
                    {
                    atomicRef.set("unexpected timeout: " + rte);
                    }
                catch (RequestPolicyException rpe)
                    {
                    // expected
                    atomicRef.set(Boolean.TRUE);
                    }
                }
            };

        try
            {
            t.start();
            t.join(5000);
            }
        catch (InterruptedException e)
            {
            fail("Unexpected interrupt" + e);
            }

        assertEquals(Boolean.TRUE, atomicRef.get());
        }
    }
