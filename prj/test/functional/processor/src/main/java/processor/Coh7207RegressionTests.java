/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package processor;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.oracle.coherence.common.base.Blocking;
import com.tangosol.coherence.component.util.Daemon;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.net.Guardian.GuardContext;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.processor.AbstractProcessor;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;


public class Coh7207RegressionTests
             extends AbstractFunctionalTest
    {
    public Coh7207RegressionTests()
        {
        super();
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
        AbstractFunctionalTest._startup();
        }

    /**
    * Test with service thread only
    */
    @Test
    public void testCOH7207S()
        {
        NamedCache cache = getNamedCache("dist-std-tests");
        doTest(cache);
        }

    /**
    * Test with worker threads
    */
    @Test
    public void testCOH7207W()
        {
        NamedCache cache = getNamedCache("dist-pool-test");
        doTest(cache);
        }

    public void doTest(NamedCache cache)
        {
        try
            {
            cache.clear();
            cache.addIndex(IdentityExtractor.INSTANCE, false, null);

            Thread[] aThread = new Thread[22];
            for (int i = 0, c = aThread.length; i < c; i++)
                {
                aThread[i] = new Thread(new RunCoh7207(cache));
                aThread[i].setDaemon(true);
                aThread[i].start();
                }

            int          count    = 0;
            CacheService     service     = cache.getCacheService();
            SafeService      serviceSafe = (SafeService) service;
            PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

            Daemon[] daemons = serviceReal.getDaemonPool().getDaemons();
            while (true)
                {
                if (daemons == null)
                    {
                    GuardContext oGuardCtx = serviceReal.getGuardable().getContext();
                    Base.azzert(oGuardCtx != null,
                                "The service thread was terminated due to Guardian timeout");
                    long cMillis     = oGuardCtx.getTimeoutMillis();
                    long cSoftMillis = oGuardCtx.getSoftTimeoutMillis();
                    Base.azzert(cMillis > 1, "Invalid guardian hard timeout interval");
                    Base.azzert(cSoftMillis > 1, "Invalid guardian soft timeout interval");
                    }
                else
                    {
                    for (Daemon d : daemons)
                        {
                        GuardContext ctx = d.getGuardable().getContext();
                        if (ctx == null)
                            {
                            // the thread has been terminated, most likely
                            // due to a thread pool resize
                            continue;
                            }

                        long cMillis     = ctx.getTimeoutMillis();
                        long cSoftMillis = ctx.getSoftTimeoutMillis();
                        Base.azzert(cMillis > 1, "Invalid guardian hard timeout interval");
                        Base.azzert(cSoftMillis > 1, "Invalid guardian soft timeout interval");
                        }
                    }

                count++;
                if (count >=20)
                    {
                    break;
                    }
                try
                    {
                    Blocking.sleep(2222);
                    }
                catch(Exception e) {};
                }
            }
        finally
            {
            cache.destroy();
            System.out.println("testCOH7207 for cache " + cache.getCacheName() + " done");
            CacheFactory.shutdown();
            }
        }


    public class RunCoh7207 extends Base implements Runnable
        {
        public RunCoh7207(NamedCache mcache)
            {
            cache = mcache;
            }

        public void run()
            {
            Random     random = new Random();
           // NamedCache cache  = CacheFactory.getCache("dist-pool-test");
            cache.clear();
            while (true)
                {
                if (random.nextBoolean())
                    {
                    Map mapBatch = new HashMap();
                    for (int i = 0; i < 2222; i++)
                        {
                        mapBatch.put(Integer.valueOf(random.nextInt(10000)), new Date().toString());
                        }
                    try
                        {
                        cache.putAll(mapBatch);
                        }
                    catch (Throwable e)
                        {
                        }
                    }
                else
                    {
                    Collection colKeys = new ArrayList();
                    for (int i = 0; i < 2222; i++)
                        {
                        colKeys.add(Integer.valueOf(random.nextInt(10000)));
                        }
                    try
                        {
                        cache.invokeAll(colKeys, new MyProcessor());
                        }
                    catch (Throwable e)
                        {
                        }
                    }
                }
            }

        NamedCache cache;
        }

    public static class MyProcessor extends AbstractProcessor
        {
        public Object process(InvocableMap.Entry entry)
            {
            Random random = new Random();
            if (entry.isPresent())
                {
                if (random.nextBoolean())
                    {
                    entry.remove(false);
                    }
                else
                    {
                    entry.setValue(new Date().toString());
                    }
                }
            return Boolean.valueOf(System.currentTimeMillis() % 2 == 0);
            }
        }

    /**
     * Helper for ClassHelper.invoke()
     */
    static protected Object invoke(Object target, String sMeth, Object[] aoArgs)
        {
        try
            {
            return ClassHelper.invoke(target, sMeth, aoArgs);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    public final static Object[] EMPTY_ARGS = new Object[] {};
    }