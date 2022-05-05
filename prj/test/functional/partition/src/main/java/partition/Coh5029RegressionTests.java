/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package partition;


import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.LessFilter;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
 * @author dimitri 4/28/11
 */
public class Coh5029RegressionTests
        extends AbstractFunctionalTest
    {
    public Coh5029RegressionTests()
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
        System.setProperty("coherence.distributed.threads.min", "4");

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    @Test
    public void test()
        {
        try
            {
            final NamedCache cache = getFactory().ensureCache("COH5029", null);

            Map mapData = new HashMap();

            for (long i = 0; i < 2222; i++)
                {
                mapData.put(i, i);
                }

            cache.putAll(mapData);
            mapData = null;

            s_run = true;

            Thread[] aThreadUpdate = new Thread[8];

            for (int i = 0, c = aThreadUpdate.length; i < c; i++)
                {
                aThreadUpdate[i] = new Thread(new Update(cache));
                aThreadUpdate[i].setDaemon(false);
                aThreadUpdate[i].start();
                }

            Thread[] aThreadQuery = new Thread[8];

            for (int i = 0, c = aThreadQuery.length; i < c; i++)
                {
                aThreadQuery[i] = new Thread(new Query(cache));
                aThreadQuery[i].setDaemon(false);
                aThreadQuery[i].start();
                }

            for (int i = 0; i < 10; i++)
                {
                cache.addIndex(IdentityExtractor.INSTANCE, true, null);
                cache.removeIndex(IdentityExtractor.INSTANCE);
                System.out.println(cache.size() + " " + cache.entrySet(s_filterLT).size() + " "
                        + cache.keySet(s_filterLT).size());
                System.out.println(cache.size() + " " + cache.entrySet(s_filterGT).size() + " "
                        + cache.keySet(s_filterGT).size());
                }
            s_run = false;
            }
        catch (Throwable e)
            {
            e.printStackTrace();
            }
        }

    public static void main(String[] asArg)
        {
        new Coh5029RegressionTests().test();
        }

    public static class Decrementor
            implements EntryProcessor
        {
        public Object process(Entry entry)
            {
            if (entry.isPresent())
                {
                Long l = (Long) entry.extract(IdentityExtractor.INSTANCE);
                l = Long.valueOf(l.longValue() - 1);
                entry.setValue(l);
                }
            return null;
            }

        public Map processAll(Set setEntries)
            {
            for (Iterator iter = setEntries.iterator(); iter.hasNext();)
                {
                process((Entry) iter.next());
                }
            return null;
            }

        public static EntryProcessor INSTANCE = new Decrementor();
        }


    public static class Incrementor
            implements EntryProcessor
        {
        public Object process(Entry entry)
            {
            if (entry.isPresent())
                {
                Long l = (Long) entry.extract(IdentityExtractor.INSTANCE);
                l = Long.valueOf(l.longValue() + 1);
                entry.setValue(l);
                }
            return null;
            }

        public Map processAll(Set setEntries)
            {
            for (Iterator iter = setEntries.iterator(); iter.hasNext();)
                {
                process((Entry) iter.next());
                }
            return null;
            }

        public static EntryProcessor INSTANCE = new Incrementor();
        }

    public static class Query
            implements Runnable
        {

        public Query(NamedCache cache)
            {
            m_cache = cache;
            }

        public void run()
            {
            int cEntrySet;
            int cKeySet;

            while (s_run)
                {
                try
                    {
                    ContinuousQueryCache cqc;
                    Filter filter;
                    Set setEntries;
                    Iterator iter;

                    filter = s_filterLT;
                    cqc = new ContinuousQueryCache(m_cache, filter, true);

                    setEntries = cqc.entrySet();
                    for (iter = setEntries.iterator(); iter.hasNext();)
                        {
                        Entry entry = (Entry) iter.next();
                        Object oValue = entry.getValue();
                        assertTrue("filter: " + filter + " value: " + oValue, filter.evaluate(oValue));
                        }

                    cqc.release();

                    filter = s_filterGT;
                    cqc = new ContinuousQueryCache(m_cache, filter, true);

                    setEntries = cqc.entrySet();
                    for (iter = setEntries.iterator(); iter.hasNext();)
                        {
                        Entry entry = (Entry) iter.next();
                        Object oValue = entry.getValue();
                        assertTrue("filter: " + filter + " value: " + oValue, filter.evaluate(oValue));
                        }

                    cqc.release();
                    }
                catch (Throwable e)
                    {
                    System.out.println(e);
                    }
                }
            }

        NamedCache m_cache;
        }

    public static class Update
            implements Runnable
        {
        public Update(NamedCache cache)
            {
            m_cache = cache;
            }

        public void run()
            {
            while (s_run)
                {
                m_cache.invokeAll(s_filterLT, Decrementor.INSTANCE);
                m_cache.invokeAll(s_filterGT, Incrementor.INSTANCE);
                }
            }

        NamedCache m_cache;
        }


    static Filter s_filterLT = new LessFilter(IdentityExtractor.INSTANCE, 22l);

    static Filter s_filterGT = new GreaterFilter(IdentityExtractor.INSTANCE, 22l);

    public volatile static boolean s_run = true;
    }