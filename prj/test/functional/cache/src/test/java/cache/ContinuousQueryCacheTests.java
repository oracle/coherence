/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package cache;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.processor.UpdaterProcessor;

import com.oracle.common.base.Blocking;
import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author jk 2014.08.22
 */
public class ContinuousQueryCacheTests
    {
    /**
     * Regression test for COH-3847
     */
    @Test
    public void testCoh3847()
        {
        int        cItems = 25;
        NamedCache cache  = CacheFactory.getCache("test");
        Map mapInitial    = new HashMap();

        for (int i = 0; i < cItems; i++)
            {
            mapInitial.put(i, new Boolean[] { Boolean.FALSE });
            }
        cache.putAll(mapInitial);

        // start the cache updating thread before creating the CQC
        Thread thdUpdate = new Thread()
            {
            public void run()
                {
                NamedCache cache  = CacheFactory.getCache("test");
                Set keys = cache.keySet();
                for (Object key : keys)
                    {
                    try
                        {
                        // Increase sleep time to 50, problem goes away
                        Blocking.sleep(1);
                        }
                    catch (InterruptedException e)
                        {
                        throw Base.ensureRuntimeException(e);
                        }

                    cache.invoke(key, new UpdaterProcessor(new TestValueUpdater(), Boolean.TRUE));
                    }
                }
            };
        thdUpdate.start();

        // Now create the CQC
        ContinuousQueryCache cqc = new ContinuousQueryCache(
                    cache, new EqualsFilter(new ValueExtractor()
                        {
                        public Object extract(Object oTarget)
                            {
                            return ((Boolean[]) oTarget)[0];
                            }
                        }, Boolean.FALSE));

        // Allow the update thread to complete
        try
            {
            thdUpdate.join();
            }
        catch (InterruptedException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        // The cqc should be completely empty
        assertArrayEquals(new Object[0], cqc.keySet().toArray());
        }

    /**
    * ValueUpdater.
    */
    public static class TestValueUpdater
            implements ValueUpdater, Serializable
        {
        public TestValueUpdater()
            {
            }

        public void update(Object oTarget, Object oValue)
            {
            ((Boolean[]) oTarget)[0] = (Boolean) oValue;
            }
        }
    }
