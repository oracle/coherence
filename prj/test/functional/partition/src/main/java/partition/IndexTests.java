/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package partition;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.safeService.safeCacheService.SafeDistributedCacheService;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.SimpleMemoryCalculator;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.tangosol.util.filter.AlwaysFilter;

/**
 * @author coh 2010.09.15
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class IndexTests
        extends AbstractFunctionalTest
    {
    public IndexTests()
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

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    /**
     * Basic functional test for adding an index after data has already been added.
     */
    @Test
    public void testIndexAfterData()
        {
        final NamedCache cache = getNamedCache("index-test");
        cache.clear();

        int cKeys   = 500;
        Map mapData = new HashMap();
        for (int i = 0; i < cKeys; i++)
            {
            mapData.put(i, i);
            }
        cache.putAll(mapData);

        ReflectionExtractor extractor = new ReflectionExtractor("intValue");
        cache.addIndex(extractor, true, null);

        Assert.assertEquals(500, cache.keySet(AlwaysFilter.INSTANCE).size());

        Map<ValueExtractor, MapIndex> indexMap = getIndexMap(cache);
        Assert.assertEquals(1, indexMap.size());
        MapIndex index = getIndex(extractor, indexMap);
        Assert.assertNotNull(index);
        Assert.assertEquals(cKeys, index.getIndexContents().size());

        cache.removeIndex(extractor);
        Assert.assertTrue(indexMap.isEmpty());
        }

    /**
     * To test the difference between the actual index cost with the calculated index cost.
     */
    @Test
    public void testIndexSize() throws InterruptedException
        {
        final NamedCache cache = getNamedCache("index-size");
        cache.clear();

        int    cKeys  = 500;
        Random random = new Random();
        Map map       = new HashMap();

        for (int i = 0; i < 10; i++)
            {
            for (int j = 1; j <= 100000; j++)
                {
                map.put(Integer.toString(i * 100000 + j), random.nextInt(cKeys));
                }

            cache.putAll(map);
            map.clear();
            }
        map = null;

        IdentityExtractor extractor = IdentityExtractor.INSTANCE;

        System.gc();
        long beforeIndex = getMemoryUsage();

        System.out.println("Creating index...");
        cache.addIndex(extractor, false, null);

        Map<ValueExtractor, MapIndex> indexMap = getIndexMap(cache);
        Assert.assertEquals(1, indexMap.size());
        MapIndex index = getIndex(extractor, indexMap);
        Assert.assertNotNull(index);

        System.gc();
        long afterIndex = getMemoryUsage();

        long   overhead = afterIndex - beforeIndex;
        double variance = Math.abs(overhead - index.getUnits()) * 1.0 / overhead;

        System.out.println("Overhead=" + Base.toMemorySizeString(overhead, false));
        System.out.println(index);

        long keySizes = 0L;

        SimpleMemoryCalculator calc = new SimpleMemoryCalculator();

        for (int k = 0; k < cKeys; k++)
            {
            for (int i = 0; i < ((DistributedCacheService) cache.getCacheService()).getPartitionCount(); i++)
                {
                Set<Binary> setKeys = (Set<Binary>) getPartitionedIndexMap(cache, i).get(extractor).getIndexContents().get(k);
                keySizes += setKeys == null ? 0 : setKeys.stream().mapToLong(calc::sizeOf).sum();
                }
            }

        System.out.printf("\nInverse index keys: %s (%,d)", Base.toMemorySizeString(keySizes, false), keySizes);
        System.out.println();

        Assert.assertTrue("The variance percentage is " + variance + ". Real memory Cost for index: " +
                    overhead + "; but calculated memory cost: " + index.getUnits(), variance < 0.1);
        }

    //@Test
    public void testInteger()
        {
        final NamedCache cache = getNamedCache("index-test");
        cache.clear();

        ReflectionExtractor extractor = new ReflectionExtractor("intValue");
        cache.addIndex(extractor, true, null);

        Map<ValueExtractor, MapIndex> indexMap = getIndexMap(cache);
        Assert.assertEquals(1, indexMap.size());
        MapIndex index = getIndex(extractor, indexMap);
        Assert.assertNotNull(index);

        int valueSize = s_calculator.sizeOf(10);
        int expectedSize = 0;

        checkSize(index, expectedSize);
        cache.put("A", 2);

        checkSize(index, expectedSize += valueSize );
        cache.put("B", 3);

        checkSize(index, expectedSize += valueSize );
        cache.put("B", 3);

        checkSize(index, expectedSize);
        cache.put("B", 3);

        checkSize(index, expectedSize);
        cache.put("C", 3);

        checkSize(index, expectedSize);
        cache.put("B", 65765);

        checkSize(index, expectedSize += valueSize);
        cache.put("A", 323);

        checkSize(index, expectedSize);
        cache.remove("A");

        checkSize(index, expectedSize -= valueSize);
        cache.remove("B");

        checkSize(index, expectedSize -= valueSize);

        cache.removeIndex(extractor);

        Assert.assertNull(indexMap.get(extractor));
        Assert.assertEquals(0, indexMap.size());
        }

    /**
     * Return the used memory of the JVM.
     *
     * @return the used memory of the JVM
     */
    public static long getMemoryUsage()
        {
        long current = 0;
        long previous = Long.MAX_VALUE;

        try{
            do
                {
                Runtime runtime = Runtime.getRuntime();
                runtime.gc();

                current = runtime.totalMemory() - runtime.freeMemory();

                Thread.sleep(1000);
                if (Math.abs(previous - current) < 100)
                    {
                    break;
                    }
                else
                    {
                    previous = current;
                    current = 0;
                    }

                }
            while (true);
        }
        catch (Exception e) {}

        return current;
        }

    /**
     * @param index
     * @param expectedSize
     */
    private void checkSize(MapIndex index, int expectedSize)
        {
        Assert.assertEquals(expectedSize, index.getUnits());
        }

    // @Test
    public void testString()
        {
        final NamedCache cache = getNamedCache("index-string-test");

        ValueExtractor extractor = new ReflectionExtractor("toString");
        cache.addIndex(extractor, true, null);

        Map<ValueExtractor, MapIndex> indexMap = getIndexMap(cache);
        Assert.assertEquals(1, indexMap.size());
        MapIndex index = getIndex(extractor, indexMap);
        Assert.assertNotNull(index);

        int expectedSize = 0;
        checkSize(index, expectedSize);

        String value = "AAAA";
        int valueSize = s_calculator.sizeOf(value);

        cache.put("A", value);
        checkSize(index, expectedSize += valueSize);

        value = "ABCDEFGHIJKLMNOPQRSTUVXYZ";
        cache.put("B", value);
        checkSize(index, expectedSize += s_calculator.sizeOf(value));
        expectedSize -= s_calculator.sizeOf(value);
        value = "ABCDEFGHIJKLMNOPQRSTUVXYZ123456789abcdefghijklmnopqrstuvxyz";
        cache.put("B", value);

        expectedSize += s_calculator.sizeOf(value);

        checkSize(index, expectedSize);

        value = (String) cache.remove("B");
        checkSize(index, expectedSize -= s_calculator.sizeOf(value));

        value = (String) cache.remove("A");
        checkSize(index, expectedSize -= s_calculator.sizeOf(value));

        cache.removeIndex(extractor);
        Assert.assertNull(indexMap.get(extractor));
        Assert.assertEquals(0, indexMap.size());
        }

    // @Test
    public void testCustom()
        {
//        IndexCalculator.addCalculator(Result.class, new UnitCalculator()
//            {
//                public String getName()
//                    {
//                    return "Type unit calculator";
//                    }
//
//                public int calculateUnits(Object oObject)
//                    {
//                    Result result = (Result) oObject;
//                    return calculator.sizeOf(result.m_name)
//                            + calculator.sizeOf(result.m_val);
//                    }
//
//                public int calculateUnits(Object oKey, Object oValue)
//                    {
//                    return 0;
//                    }
//
//                private SimpleMemoryCalculator calculator = new SimpleMemoryCalculator();
//            });

        final NamedCache cache = getNamedCache("index-custom-test");

        ValueExtractor extractor = new ReflectionExtractor("getValue");
        cache.addIndex(extractor, true, null);

        Map<ValueExtractor, MapIndex> indexMap = getIndexMap(cache);
        MapIndex index = getIndex(extractor, indexMap);

        checkSize(index, 0);
        Type bar = new Type("bar", 1);
        cache.put("Foo", bar);
        //checkSize(index, index.getCalculator().calculateUnits(null, bar.getValue()));
        }

    public static class Type implements ExternalizableLite
        {
        public Type()
            {
            }

        public Type(String name, int val)
            {
            m_result = new Result(name, val);
            }

        private Result m_result;

        public Result getValue()
            {
            return m_result;
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            Result result = new Result();
            result.readExternal(in);
            m_result = result;
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            m_result.writeExternal(out);
            }
        }

    public static class Result implements ExternalizableLite
        {
        private String m_name;
        private int    m_val;

        public Result(String name, int val)
            {
            m_name = name;
            m_val = val;
            }

        public Result()
            {
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_val = in.readInt();
            m_name = in.readUTF();
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeInt(m_val);
            out.writeUTF(m_name);
            }
        }

    /**
     * @param extractor
     * @param indexMap
     * @return
     */
    private MapIndex getIndex(ValueExtractor extractor,
                              Map<ValueExtractor, MapIndex> indexMap)
        {
        return indexMap.get(extractor);
        }

    /**
     * @param cache
     * @return
     */
    private Map<ValueExtractor, MapIndex> getIndexMap(
            final NamedCache cache)
        {
        SafeDistributedCacheService service = (SafeDistributedCacheService) cache
                .getCacheService().getCluster()
                .getService(cache.getCacheService().getInfo().getServiceName());
        PartitionedCache distCache = (PartitionedCache) service.getService();
        return distCache.getStorage(cache.getCacheName()).getIndexMap();
        }

    private Map<ValueExtractor, MapIndex> getPartitionedIndexMap(
            final NamedCache cache, int nPart)
        {
        SafeDistributedCacheService service = (SafeDistributedCacheService) cache
                .getCacheService().getCluster()
                .getService(cache.getCacheService().getInfo().getServiceName());
        PartitionedCache distCache = (PartitionedCache) service.getService();
        return distCache.getStorage(cache.getCacheName()).getPartitionIndexMap(nPart);
        }

    private static final SimpleMemoryCalculator s_calculator = new SimpleMemoryCalculator();
    }
