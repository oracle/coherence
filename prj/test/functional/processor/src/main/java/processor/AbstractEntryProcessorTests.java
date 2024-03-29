/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package processor;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LiteMap;
import com.tangosol.util.Processors;
import com.tangosol.util.SafeLinkedList;
import com.tangosol.util.Versionable;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.NeverFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PresentFilter;

import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.CompositeProcessor;
import com.tangosol.util.processor.ConditionalProcessor;
import com.tangosol.util.processor.ConditionalPut;
import com.tangosol.util.processor.ConditionalPutAll;
import com.tangosol.util.processor.ConditionalRemove;
import com.tangosol.util.processor.ExtractorProcessor;
import com.tangosol.util.processor.NumberIncrementor;
import com.tangosol.util.processor.NumberMultiplier;
import com.tangosol.util.processor.PreloadRequest;
import com.tangosol.util.processor.UpdaterProcessor;
import com.tangosol.util.processor.VersionedPut;
import com.tangosol.util.processor.VersionedPutAll;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.coherence.common.internal.util.HeapDump;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;


/**
* A collection of functional tests for the various
* {@link InvocableMap.EntryProcessor} implementations.
*
* @author jh  2005.12.21
*
* @see InvocableMap
*/
public abstract class AbstractEntryProcessorTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new AbstractEntryProcessorTests that will use the cache with
    * the given name in all test methods.
    *
    * @param sCache  the test cache name
    */
    public AbstractEntryProcessorTests(String sCache)
        {
        if (sCache == null || sCache.trim().length() == 0)
            {
            throw new IllegalArgumentException("Invalid cache name");
            }

        m_sCache = sCache.trim();
        }


    // ----- AbstractEntryAggregatorTests methods ---------------------------

    /**
    * Return the cache used in all test methods.
    *
    * @return the test cache
    */
    protected NamedCache getNamedCache()
        {
        return getNamedCache(getCacheName());
        }


    // ----- test methods ---------------------------------------------------

    /**
    * Test of the {@link CompositeProcessor}.
    */
    @Test
    public void composite()
        {
        NamedCache         cache = getNamedCache();
        CompositeProcessor processor;
        Object[]           aoResult;
        Map                mapResult;
        TestValue          value;

        cache.clear();

        try
            {
            value = new TestValue();
            value.setIntegerValue(1);
            cache.put("1", value);
            processor = new CompositeProcessor(new InvocableMap.EntryProcessor[]{});

            aoResult = (Object[]) cache.invoke("1", processor);
            assertTrue("Result=" + aoResult, aoResult.length == 0);
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            mapResult = cache.invokeAll(Collections.singletonList("1"), processor);
            aoResult = (Object[]) mapResult.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, aoResult.length == 0);
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            aoResult = (Object[]) mapResult.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, aoResult.length == 0);
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            processor = (CompositeProcessor) Processors.composite(
                    new InvocableMap.EntryProcessor[]
                            {
                                new NumberIncrementor("IntegerValue", 1, false),
                                new NumberMultiplier("IntegerValue", 2, false)
                            });

            aoResult = (Object[]) cache.invoke("1", processor);
            assertTrue("Result=" + aoResult, aoResult.length == 2);
            assertTrue("Result=" + aoResult[0], equals(aoResult[0], 2));
            assertTrue("Result=" + aoResult[1], equals(aoResult[1], 4));

            mapResult = cache.invokeAll(Collections.singletonList("1"), processor);
            for (Object obj : mapResult.entrySet())
                {
                System.out.println("[Composite Processor] : " + obj);
                }

            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            aoResult = (Object[]) mapResult.get("1");
            assertTrue("Result=" + mapResult, aoResult.length == 2);
            assertTrue("Result=" + aoResult[0], equals(aoResult[0], 5));
            assertTrue("Result=" + aoResult[1], equals(aoResult[1], 10));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            aoResult = (Object[]) mapResult.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, aoResult.length == 2);
            assertTrue("Result=" + aoResult[0], equals(aoResult[0], 11));
            assertTrue("Result=" + aoResult[1], equals(aoResult[1], 22));

            value = (TestValue) cache.get("1");
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 22));

            processor = new CompositeProcessor(
                    new InvocableMap.EntryProcessor[]
                            {
                                new ConditionalRemove(PresentFilter.INSTANCE),
                                new NumberIncrementor("IntegerValue", 1, false),
                            });
            cache.invoke("1", processor);
            value = (TestValue) cache.get("1");
            assertTrue("Expected null; " + value, value == null);

            processor = new CompositeProcessor(
                    new InvocableMap.EntryProcessor[]
                            {
                                new ConditionalPut(new NotFilter(PresentFilter.INSTANCE), new TestValue()),
                                new NumberIncrementor("IntegerValue", 1, false),
                            });
            aoResult = (Object[]) cache.invoke("1", processor);
            assertTrue("Result=" + aoResult[1], equals(aoResult[1], 1));

            value = (TestValue) cache.get("1");
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 1));

            int cSize = SMALL_TEST_SIZE;
            Map mapTemp = generateTestMap(cSize);
            cache.clear();
            cache.putAll(mapTemp);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == cSize);

            mapResult = cache.invokeAll(mapTemp.keySet(), processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == cSize);
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link ConditionalProcessor}.
    */
    @Test
    public void conditional()
        {
        NamedCache           cache = getNamedCache();
        ConditionalProcessor processor;
        Object               oResult;
        Map                  mapResult;
        TestValue            value;

        cache.clear();

        try
            {
            value = new TestValue();
            value.setIntegerValue(1);
            cache.put("1", value);
            processor = (ConditionalProcessor) Processors.conditional(NeverFilter.INSTANCE,
                    new ExtractorProcessor("getIntegerValue"));

            oResult = cache.invoke("1", processor);
            assertTrue("Result=" + oResult, equals(oResult, null));
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            mapResult = cache.invokeAll(Collections.singletonList("1"), processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            processor = new ConditionalProcessor(AlwaysFilter.INSTANCE,
                    new ExtractorProcessor("getIntegerValue"));

            oResult = cache.invoke("1", processor);
            assertTrue("Result=" + oResult, equals(oResult, 1));
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            mapResult = cache.invokeAll(Collections.singletonList("1"), processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 1));
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 1));
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            processor = new ConditionalProcessor(
                    new EqualsFilter("getIntegerValue", 1),
                    new NumberIncrementor("IntegerValue", 1, false));
            oResult = cache.invoke("1", processor);
            assertTrue("Result=" + oResult, equals(oResult, 2));

            oResult = cache.invoke("1", processor);
            assertTrue("Result=" + oResult, equals(oResult, null));

            processor = new ConditionalProcessor(
                    new EqualsFilter("getIntegerValue", 2),
                    new UpdaterProcessor("setIntegerValue", 3));
            oResult = cache.invoke("1", processor);
            assertTrue("Result=" + oResult, equals(oResult, Boolean.TRUE));

            oResult = cache.invoke("1", processor);
            assertTrue("Result=" + oResult, equals(oResult, null));

            value = (TestValue) cache.get("1");
            assertTrue(value.getIntegerValue().toString(), value.getIntegerValue().intValue() == 3);

            // Test that the result map returned from processAll will only
            // include results for the entries that pass the filter check.
            cache.clear();
            cache.put("1", value);
            cache.put("2", value);
            cache.put("3", value);

            Set setEntries = new HashSet();
            setEntries.add("1");
            setEntries.add("2");
            setEntries.add("3");

            processor = new ConditionalProcessor(NeverFilter.INSTANCE,
                    new PreloadRequest());

            mapResult = cache.invokeAll(setEntries, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);

            processor = new ConditionalProcessor(AlwaysFilter.INSTANCE,
                    new PreloadRequest());

            mapResult = cache.invokeAll(setEntries, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 3);
            assertTrue("Key mssing from results",
                    mapResult.containsKey("1") &&
                    mapResult.containsKey("2") &&
                    mapResult.containsKey("3"));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 3);
            assertTrue("Key mssing from results",
            mapResult.containsKey("1") &&
                    mapResult.containsKey("2") &&
                    mapResult.containsKey("3"));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link ExtractorProcessor} on an empty cache.
    */
    @Test
    public void extractNothing()
        {
        NamedCache         cache = getNamedCache();
        ExtractorProcessor extractor;
        Object             oResult;
        Map                mapResult;

        cache.clear();

        try
            {
            // test extraction of a non-existent entry
            extractor = (ExtractorProcessor) Processors.extract("getByteValue");
            oResult = cache.invoke("1", extractor);
            assertTrue("Result=" + oResult, oResult == null);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            mapResult = cache.invokeAll(Collections.singletonList("1"), extractor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, mapResult.get("1") == null);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, extractor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            extractor = new ExtractorProcessor("getByteValue");
            oResult = cache.invoke("1", extractor);
            assertTrue("Result=" + oResult, oResult == null);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            mapResult = cache.invokeAll(Collections.singletonList("1"), extractor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, mapResult.get("1") == null);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, extractor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Size=" + cache.size(), cache.size() == 0);
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link ExtractorProcessor}.
    */
    @Test
    public void extract()
        {
        NamedCache         cache = getNamedCache();
        ExtractorProcessor extractor;
        Object             oResult;
        Map                mapResult;
        TestValue          value;

        cache.clear();

        try
            {
            value = new TestValue();
            cache.put("1", value);
            extractor = new ExtractorProcessor("getIntegerValue");

            oResult = cache.invoke("1", extractor);
            assertTrue("Result=" + oResult, equals(oResult, null));
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            mapResult = cache.invokeAll(Collections.singletonList("1"), extractor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), null));
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, extractor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), null));
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            cache.clear();

            value = new TestValue();
            value.setIntegerValue(1);
            cache.put("1", value);
            extractor = new ExtractorProcessor("getIntegerValue");

            oResult = cache.invoke("1", extractor);
            assertTrue("Result=" + oResult, equals(oResult, 1));
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            mapResult = cache.invokeAll(Collections.singletonList("1"), extractor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 1));
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, extractor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 1));
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), value));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberIncrementor} on an empty cache.
    */
    @Test
    public void incrementNothing()
        {
        NamedCache        cache = getNamedCache();
        NumberIncrementor incrementor;
        Object            oResult;
        Map               mapResult;

        cache.clear();

        try
            {
            // test increment of a non-existent entry
            incrementor = (NumberIncrementor) Processors.increment("IntegerValue", 1, false);
            oResult     = cache.invoke("1", incrementor);
            assertTrue("Result=" + oResult, oResult == null);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, mapResult.get("1") == null);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE,  incrementor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            incrementor = new NumberIncrementor("Value", 1, true);
            oResult     = cache.invoke("1", incrementor);
            assertTrue("Result=" + oResult, oResult == null);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, mapResult.get("1") == null);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE,  incrementor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Size=" + cache.size(), cache.size() == 0);
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberIncrementor} on a Byte property.
    */
    @Test
    public void incrementByte()
        {
        NamedCache        cache = getNamedCache();
        NumberIncrementor incrementor;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-increment of a Byte value
            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("ByteValue", (byte) 1, false);

            oResult = cache.invoke("1", incrementor);
            value   = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, (byte) 1));
            assertTrue("Value=" + value, equals(value.getByteValue(), (byte) 1));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (byte) 2));
            assertTrue("Value=" + value, equals(value.getByteValue(), (byte) 2));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (byte) 3));
            assertTrue("Value=" + value, equals(value.getByteValue(), (byte) 3));

            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("ByteValue", (byte) 1, true);

            oResult = cache.invoke("1", incrementor);
            value   = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, (byte) 0));
            assertTrue("Value=" + value, equals(value.getByteValue(), (byte) 1));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (byte) 1));
            assertTrue("Value=" + value, equals(value.getByteValue(), (byte) 2));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (byte) 2));
            assertTrue("Value=" + value, equals(value.getByteValue(), (byte) 3));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberIncrementor} on a Short property.
    */
    @Test
    public void incrementShort()
        {
        NamedCache        cache = getNamedCache();
        NumberIncrementor incrementor;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-increment of a Short value
            cache.put("1", new TestValue());
            incrementor = (NumberIncrementor) Processors.increment("ShortValue", (short) 1, false);

            oResult = cache.invoke("1", incrementor);
            value   = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, (short) 1));
            assertTrue("Value=" + value, equals(value.getShortValue(), (short) 1));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (short) 2));
            assertTrue("Value=" + value, equals(value.getShortValue(), (short) 2));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (short) 3));
            assertTrue("Value=" + value, equals(value.getShortValue(), (short) 3));

            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("ShortValue", (short) 1, true);

            oResult = cache.invoke("1", incrementor);
            value   = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, (short) 0));
            assertTrue("Value=" + value, equals(value.getShortValue(), (short) 1));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (short) 1));
            assertTrue("Value=" + value, equals(value.getShortValue(), (short) 2));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (short) 2));
            assertTrue("Value=" + value, equals(value.getShortValue(), (short) 3));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberIncrementor} on an Integer property.
    */
    @Test
    public void incrementInteger()
        {
        NamedCache        cache = getNamedCache();
        NumberIncrementor incrementor;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-increment of a Integer value
            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("IntegerValue", 1, false);

            oResult = cache.invoke("1", incrementor);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 1));
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 1));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 2));
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 2));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 3));
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 3));

            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("IntegerValue", 1, true);

            oResult = cache.invoke("1", incrementor);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 0));
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 1));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 1));
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 2));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 2));
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 3));

            int cSize = SMALL_TEST_SIZE;
            Map mapTemp = generateTestMap(cSize);
            cache.clear();
            cache.putAll(mapTemp);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == cSize);
            assertTrue("Result=" + mapResult.get(cSize),
                    equals(mapResult.get(cSize), cSize));

            mapResult = cache.invokeAll(mapTemp.keySet(), incrementor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == cSize);
            assertTrue("Result=" + mapResult.get(1),
                    equals(mapResult.get(1), 2));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberIncrementor} on a Float property.
    */
    @Test
    public void incrementFloat()
        {
        NamedCache        cache = getNamedCache();
        NumberIncrementor incrementor;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-increment of a Float value
            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("FloatValue", 1.0F, false);

            oResult = cache.invoke("1", incrementor);
            value   = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 1.0F));
            assertTrue("Value=" + value, equals(value.getFloatValue(), 1.0F));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 2.0F));
            assertTrue("Value=" + value, equals(value.getFloatValue(), 2.0F));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 3.0F));
            assertTrue("Value=" + value, equals(value.getFloatValue(), 3.0F));

            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("FloatValue", 1.0F, true);

            oResult = cache.invoke("1", incrementor);
            value   = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 0.0F));
            assertTrue("Value=" + value, equals(value.getFloatValue(), 1.0F));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 1.0F));
            assertTrue("Value=" + value, equals(value.getFloatValue(), 2.0F));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 2.0F));
            assertTrue("Value=" + value, equals(value.getFloatValue(), 3.0F));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberIncrementor} on a Long property.
    */
    @Test
    public void incrementLong()
        {
        NamedCache        cache = getNamedCache();
        NumberIncrementor incrementor;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-increment of a Long value
            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("LongValue", 1L, false);

            oResult = cache.invoke("1", incrementor);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 1L));
            assertTrue("Value=" + value, equals(value.getLongValue(), 1L));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value = (TestValue) cache.get("1");

            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 2L));
            assertTrue("Value=" + value, equals(value.getLongValue(), 2L));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 3L));
            assertTrue("Value=" + value, equals(value.getLongValue(), 3L));

            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("LongValue", 1L, true);

            oResult = cache.invoke("1", incrementor);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 0L));
            assertTrue("Value=" + value, equals(value.getLongValue(), 1L));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 1L));
            assertTrue("Value=" + value, equals(value.getLongValue(), 2L));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 2L));
            assertTrue("Value=" + value, equals(value.getLongValue(), 3L));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberIncrementor} on a Double property.
    */
    @Test
    public void incrementDouble()
        {
        NamedCache        cache = getNamedCache();
        NumberIncrementor incrementor;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-increment of a Double value
            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("DoubleValue", 1.0, false);

            oResult = cache.invoke("1", incrementor);
            value   = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 1.0));
            assertTrue("Value=" + value, equals(value.getDoubleValue(), 1.0));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 2.0));
            assertTrue("Value=" + value, equals(value.getDoubleValue(), 2.0));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 3.0));
            assertTrue("Value=" + value, equals(value.getDoubleValue(), 3.0));

            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("DoubleValue", 1.0, true);

            oResult = cache.invoke("1", incrementor);
            value   = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 0.0));
            assertTrue("Value=" + value, equals(value.getDoubleValue(), 1.0));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 1.0));
            assertTrue("Value=" + value, equals(value.getDoubleValue(), 2.0));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 2.0));
            assertTrue("Value=" + value, equals(value.getDoubleValue(), 3.0));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberIncrementor} on a BigInteger property.
    */
    @Test
    public void incrementBigInteger()
        {
        NamedCache        cache = getNamedCache();
        NumberIncrementor incrementor;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-increment of a BigInteger value
            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("BigIntegerValue", new BigInteger("1"), false);

            oResult = cache.invoke("1", incrementor);
            value   = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, new BigInteger("1")));
            assertTrue("Value=" + value, equals(value.getBigIntegerValue(), new BigInteger("1")));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigInteger("2")));
            assertTrue("Value=" + value, equals(value.getBigIntegerValue(), new BigInteger("2")));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigInteger("3")));
            assertTrue("Value=" + value, equals(value.getBigIntegerValue(), new BigInteger("3")));

            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("BigIntegerValue", new BigInteger("1"), true);

            oResult = cache.invoke("1", incrementor);
            value   = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, BigInteger.ZERO));
            assertTrue("Value=" + value, equals(value.getBigIntegerValue(), new BigInteger("1")));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigInteger("1")));
            assertTrue("Value=" + value, equals(value.getBigIntegerValue(), new BigInteger("2")));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigInteger("2")));
            assertTrue("Value=" + value, equals(value.getBigIntegerValue(), new BigInteger("3")));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberIncrementor} on a BigDecimal property.
    */
    @Test
    public void incrementBigDecimal()
        {
        NamedCache        cache = getNamedCache();
        NumberIncrementor incrementor;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-increment of a BigDecimal value
            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("BigDecimalValue", new BigDecimal("1.0"), false);

            oResult = cache.invoke("1", incrementor);
            value   = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, new BigDecimal("1.0")));
            assertTrue("Value=" + value, equals(value.getBigDecimalValue(), new BigDecimal("1.0")));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigDecimal("2.0")));
            assertTrue("Value=" + value, equals(value.getBigDecimalValue(), new BigDecimal("2.0")));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigDecimal("3.0")));
            assertTrue("Value=" + value, equals(value.getBigDecimalValue(), new BigDecimal("3.0")));

            cache.put("1", new TestValue());
            incrementor = new NumberIncrementor("BigDecimalValue", new BigDecimal("1.0"), true);

            oResult = cache.invoke("1", incrementor);
            value   = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, new BigDecimal(BigInteger.ZERO)));
            assertTrue("Value=" + value, equals(value.getBigDecimalValue(), new BigDecimal("1.0")));

            mapResult = cache.invokeAll(Collections.singletonList("1"), incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigDecimal("1.0")));
            assertTrue("Value=" + value, equals(value.getBigDecimalValue(), new BigDecimal("2.0")));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, incrementor);
            value     = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigDecimal("2.0")));
            assertTrue("Value=" + value, equals(value.getBigDecimalValue(), new BigDecimal("3.0")));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberMultiplier} on an empty cache.
    */
    @Test
    public void multiplyNothing()
        {
        NamedCache        cache = getNamedCache();
        NumberMultiplier  multiplier;
        Object            oResult;
        Map               mapResult;

        cache.clear();

        try
            {
            // test increment of a non-existent entry
            multiplier = (NumberMultiplier) Processors.multiply("IntegerValue", 2, false);
            oResult    = cache.invoke("1", multiplier);
            assertTrue("Result=" + oResult, oResult == null);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, mapResult.get("1") == null);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE,  multiplier);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            multiplier = new NumberMultiplier("IntegerValue", 2, false);
            oResult    = cache.invoke("1", multiplier);
            assertTrue("Result=" + oResult, oResult == null);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, mapResult.get("1") == null);
            assertTrue("Size=" + cache.size(), cache.size() == 0);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE,  multiplier);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Size=" + cache.size(), cache.size() == 0);
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberMultiplier} on a Byte property.
    */
    @Test
    public void multiplyByte()
        {
        NamedCache        cache = getNamedCache();
        NumberMultiplier  multiplier;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-multiply of a Byte value
            value = new TestValue();
            multiplier = new NumberMultiplier("ByteValue", (byte) 2, false);
            value.setByteValue((byte) 1);
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, (byte) 2));
            assertTrue("Value=" + value, equals(value.getByteValue(), (byte) 2));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (byte) 4));
            assertTrue("Value=" + value, equals(value.getByteValue(), (byte) 4));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (byte) 8));
            assertTrue("Value=" + value, equals(value.getByteValue(), (byte) 8));

            value = new TestValue();
            multiplier = new NumberMultiplier("ByteValue", (byte) 2, true);
            value.setByteValue((byte) 1);
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, (byte) 1));
            assertTrue("Value=" + value, equals(value.getByteValue(), (byte) 2));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (byte) 2));
            assertTrue("Value=" + value, equals(value.getByteValue(), (byte) 4));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (byte) 4));
            assertTrue("Value=" + value, equals(value.getByteValue(), (byte) 8));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberMultiplier} on a Short property.
    */
    @Test
    public void multiplyShort()
        {
        NamedCache        cache = getNamedCache();
        NumberMultiplier  multiplier;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-multiply of a Short value
            value = new TestValue();
            multiplier = new NumberMultiplier("ShortValue", (short) 2, false);
            value.setShortValue((short) 1);
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, (short) 2));
            assertTrue("Value=" + value, equals(value.getShortValue(), (short) 2));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (short) 4));
            assertTrue("Value=" + value, equals(value.getShortValue(), (short) 4));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (short) 8));
            assertTrue("Value=" + value, equals(value.getShortValue(), (short) 8));

            value = new TestValue();
            multiplier = new NumberMultiplier("ShortValue", (short) 2, true);
            value.setShortValue((short) 1);
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, (short) 1));
            assertTrue("Value=" + value, equals(value.getShortValue(), (short) 2));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (short) 2));
            assertTrue("Value=" + value, equals(value.getShortValue(), (short) 4));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), (short) 4));
            assertTrue("Value=" + value, equals(value.getShortValue(), (short) 8));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberMultiplier} on an Integer property.
    */
    @Test
    public void multiplyInteger()
        {
        NamedCache        cache = getNamedCache();
        NumberMultiplier  multiplier;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-multiply of a Integer value
            value = new TestValue();
            multiplier = new NumberMultiplier("IntegerValue", 2, false);
            value.setIntegerValue(1);
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 2));
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 2));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 4));
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 4));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 8));
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 8));

            value = new TestValue();
            multiplier = new NumberMultiplier("IntegerValue", 2, true);
            value.setIntegerValue(1);
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 1));
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 2));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 2));
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 4));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 4));
            assertTrue("Value=" + value, equals(value.getIntegerValue(), 8));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberMultiplier} on a Long property.
    */
    @Test
    public void multiplyLong()
        {
        NamedCache        cache = getNamedCache();
        NumberMultiplier  multiplier;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-multiply of a Long value
            value = new TestValue();
            multiplier = new NumberMultiplier("LongValue", 2L, false);
            value.setLongValue(1L);
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 2L));
            assertTrue("Value=" + value, equals(value.getLongValue(), 2L));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 4L));
            assertTrue("Value=" + value, equals(value.getLongValue(), 4L));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 8L));
            assertTrue("Value=" + value, equals(value.getLongValue(), 8L));

            value = new TestValue();
            multiplier = new NumberMultiplier("LongValue", 2L, true);
            value.setLongValue(1L);
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 1L));
            assertTrue("Value=" + value, equals(value.getLongValue(), 2L));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 2L));
            assertTrue("Value=" + value, equals(value.getLongValue(), 4L));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 4L));
            assertTrue("Value=" + value, equals(value.getLongValue(), 8L));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberMultiplier} on a Float property.
    */
    @Test
    public void multiplyFloat()
        {
        NamedCache        cache = getNamedCache();
        NumberMultiplier  multiplier;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-multiply of a Float value
            value = new TestValue();
            multiplier = new NumberMultiplier("FloatValue", 2.0F, false);
            value.setFloatValue(1.0F);
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 2.0F));
            assertTrue("Value=" + value, equals(value.getFloatValue(), 2.0F));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 4.0F));
            assertTrue("Value=" + value, equals(value.getFloatValue(), 4.0F));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 8.0F));
            assertTrue("Value=" + value, equals(value.getFloatValue(), 8.0F));

            value = new TestValue();
            multiplier = new NumberMultiplier("FloatValue", 2.0F, true);
            value.setFloatValue(1.0F);
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 1.0F));
            assertTrue("Value=" + value, equals(value.getFloatValue(), 2.0F));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 2.0F));
            assertTrue("Value=" + value, equals(value.getFloatValue(), 4.0F));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 4.0F));
            assertTrue("Value=" + value, equals(value.getFloatValue(), 8.0F));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberMultiplier} on a Double property.
    */
    @Test
    public void multiplyDouble()
        {
        NamedCache        cache = getNamedCache();
        NumberMultiplier  multiplier;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-multiply of a Double value
            value = new TestValue();
            multiplier = new NumberMultiplier("DoubleValue", 2.0, false);
            value.setDoubleValue(1.0);
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 2.0));
            assertTrue("Value=" + value, equals(value.getDoubleValue(), 2.0));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 4.0));
            assertTrue("Value=" + value, equals(value.getDoubleValue(), 4.0));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 8.0));
            assertTrue("Value=" + value, equals(value.getDoubleValue(), 8.0));

            value = new TestValue();
            multiplier = new NumberMultiplier("DoubleValue", 2.0, true);
            value.setDoubleValue(1.0);
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, 1.0));
            assertTrue("Value=" + value, equals(value.getDoubleValue(), 2.0));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 2.0));
            assertTrue("Value=" + value, equals(value.getDoubleValue(), 4.0));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), 4.0));
            assertTrue("Value=" + value, equals(value.getDoubleValue(), 8.0));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberMultiplier} on a BigInteger property.
    */
    @Test
    public void multiplyBigInteger()
        {
        NamedCache        cache = getNamedCache();
        NumberMultiplier  multiplier;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-multiply of a BigInteger value
            value = new TestValue();
            multiplier = new NumberMultiplier("BigIntegerValue", new BigInteger("2"), false);
            value.setBigIntegerValue(new BigInteger("1"));
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, new BigInteger("2")));
            assertTrue("Value=" + value, equals(value.getBigIntegerValue(), new BigInteger("2")));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigInteger("4")));
            assertTrue("Value=" + value, equals(value.getBigIntegerValue(), new BigInteger("4")));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigInteger("8")));
            assertTrue("Value=" + value, equals(value.getBigIntegerValue(), new BigInteger("8")));

            value = new TestValue();
            multiplier = new NumberMultiplier("BigIntegerValue", new BigInteger("2"), true);
            value.setBigIntegerValue(new BigInteger("1"));
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, new BigInteger("1")));
            assertTrue("Value=" + value, equals(value.getBigIntegerValue(), new BigInteger("2")));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigInteger("2")));
            assertTrue("Value=" + value, equals(value.getBigIntegerValue(), new BigInteger("4")));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigInteger("4")));
            assertTrue("Value=" + value, equals(value.getBigIntegerValue(), new BigInteger("8")));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link NumberMultiplier} on a BigDecimal property.
    */
    @Test
    public void multiplyBigDecimal()
        {
        NamedCache        cache = getNamedCache();
        NumberMultiplier  multiplier;
        Object            oResult;
        Map               mapResult;
        TestValue         value;

        cache.clear();

        try
            {
            // test pre and post-multiply of a BigDecimal value
            value = new TestValue();
            multiplier = new NumberMultiplier("BigDecimalValue", new BigDecimal("2.0"), false);
            value.setBigDecimalValue(new BigDecimal("1.0"));
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, new BigDecimal("2.00")));
            assertTrue("Value=" + value, equals(value.getBigDecimalValue(), new BigDecimal("2.00")));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigDecimal("4.000")));
            assertTrue("Value=" + value, equals(value.getBigDecimalValue(), new BigDecimal("4.000")));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigDecimal("8.0000")));
            assertTrue("Value=" + value, equals(value.getBigDecimalValue(), new BigDecimal("8.0000")));

            value = new TestValue();
            multiplier = new NumberMultiplier("BigDecimalValue", new BigDecimal("2.0"), true);
            value.setBigDecimalValue(new BigDecimal("1.0"));
            cache.put("1", value);

            oResult = cache.invoke("1", multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Result=" + oResult, equals(oResult, new BigDecimal("1.0")));
            assertTrue("Value=" + value, equals(value.getBigDecimalValue(), new BigDecimal("2.00")));

            mapResult = cache.invokeAll(Collections.singletonList("1"), multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigDecimal("2.00")));
            assertTrue("Value=" + value, equals(value.getBigDecimalValue(), new BigDecimal("4.000")));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, multiplier);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), new BigDecimal("4.000")));
            assertTrue("Value=" + value, equals(value.getBigDecimalValue(), new BigDecimal("8.0000")));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.release();
        }

    /**
    * Test of the {@link com.tangosol.util.processor.ConditionalRemove}
    */
    @Test
    public void remove()
        {
        NamedCache      cache = getNamedCache();
        TestValue       value;
        Object          oResult;
        ConditionalRemove agentLite  = (ConditionalRemove) Processors.remove(AlwaysFilter.INSTANCE);
        ConditionalRemove agentHeavy = (ConditionalRemove) Processors.remove(AlwaysFilter.INSTANCE, true);

        cache.clear();

        try
            {
            oResult = cache.invoke("1", agentLite);
            assertTrue("Result=" + oResult, oResult == null);

            oResult = cache.invoke("1", agentHeavy);
            assertTrue("Result=" + oResult, oResult == null);

            value = new TestValue();
            value.setLongValue(getRandom().nextLong());

            cache.put("1", value);
            oResult = cache.invoke("1", agentLite);
            assertTrue("Result=" + oResult, oResult == null);
            assertTrue("Not empty=" + cache.size(), cache.size() == 0);

            cache.put("1", value);
            oResult = cache.invoke("1", agentHeavy);
            assertTrue("Result=" + oResult, oResult == null);
            assertTrue("Not empty=" + cache.size(), cache.size() == 0);

            for (int i = 0; i < 100; i++)
                {
                value = new TestValue();
                value.setLongValue(1l);
                cache.put(i, value);
                }

            Map mapResult = cache.invokeAll(AlwaysFilter.INSTANCE,
                    new ConditionalRemove(NeverFilter.INSTANCE, true));
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 100);
            assertTrue("Not full=" + cache.size(), cache.size() == 100);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE,
                    new ConditionalRemove(NeverFilter.INSTANCE));
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Not full=" + cache.size(), cache.size() == 100);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, agentHeavy);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Not empty=" + cache.size(), cache.size() == 0);

            for (int i = 0; i < 100; i++)
                {
                value = new TestValue();
                value.setLongValue(2l);
                cache.put(i, value);
                }

            cache.invokeAll(AlwaysFilter.INSTANCE, agentLite);
            assertTrue("Not empty=" + cache.size(), cache.size() == 0);

            for (int i = 0; i < 100; i++)
                {
                value = new TestValue();
                value.setLongValue(3l);
                cache.put(i, value);
                }

            cache.invokeAll(AlwaysFilter.INSTANCE,
                    new ConditionalRemove(new EqualsFilter("getLongValue", 2l)));
            assertTrue("Not full=" + cache.size(), cache.size() == 100);

            cache.invokeAll((Filter) null,
                    new ConditionalRemove(new EqualsFilter("getLongValue", 3l)));
            assertTrue("Not empty=" + cache.size(), cache.size() == 0);

            for (int i = 0; i < 100; i++)
                {
                value = new TestValue();
                value.setLongValue(4l);
                cache.put(i, value);
                }
            cache.invokeAll(new EqualsFilter("getLongValue", 3l), agentLite);
            assertTrue("Not full=" + cache.size(), cache.size() == 100);

            cache.invokeAll(new EqualsFilter("getLongValue", 4l), agentLite);
            assertTrue("Not empty=" + cache.size(), cache.size() == 0);


            // Test that the result map returned from processAll will only
            // include the values from entries that could not be removed.
            cache.clear();

            Set setEntries = new HashSet();
            setEntries.add("1");
            setEntries.add("3");

            ConditionalRemove processor = new ConditionalRemove(NeverFilter.INSTANCE, true);

            mapResult = cache.invokeAll(setEntries, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 2);
            assertTrue("Key mssing from results",
                    mapResult.containsKey("1") &&
                            mapResult.containsKey("3"));

            cache.put("1", value);
            cache.put("2", value);
            cache.put("3", value);
            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 3);
            assertTrue("Key mssing from results",
                    mapResult.containsKey("1") &&
                            mapResult.containsKey("2") &&
                            mapResult.containsKey("3"));

            processor = new ConditionalRemove(AlwaysFilter.INSTANCE, true);

            mapResult = cache.invokeAll(setEntries, processor);
            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);

            cache.put("1", value);
            cache.put("2", value);
            cache.put("3", value);
            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }
        }

    /**
    * Test of the {@link com.tangosol.util.processor.ConditionalPut}
    */
    @Test
    public void put()
        {
        NamedCache cache = getNamedCache();
        Filter     filter;
        TestValue  value, valueOld, valueCheck;
        Object     oResult;

        cache.clear();

        try
            {
            value = valueOld = new TestValue();
            value.setLongValue(1L);

            // simple puts
            oResult = cache.invoke("1",
                    Processors.put(AlwaysFilter.INSTANCE, value));
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + "!=" + valueOld, equals(value, valueOld));

            value.setLongValue(2L);
            oResult = cache.invoke("1",
                    new ConditionalPut(AlwaysFilter.INSTANCE, value, true));
            assertTrue("Result=" + oResult, oResult == null);

            cache.clear();

            // Test ConditionalPut through invokeAll...
            TestValue valueCurrent = new TestValue();
            valueCurrent.setLongValue(100L);
            cache.put("1", valueCurrent);

            TestValue valueNew = new TestValue();
            valueNew.setLongValue(99L);

            // Use an AlwaysFilter and verify that the put takes place.
            // An empty map should be returned.
            ConditionalPut processor = new ConditionalPut(AlwaysFilter.INSTANCE, valueNew);

            Map mapResult = cache.invokeAll(Collections.singletonList("1"), processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), valueNew));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), valueNew));

            cache.clear();

            // Use an NeverFilter and verify that the put doesn't happen.
            // An empty map should be returned... fReturn = false.
            cache.put("1", valueCurrent);

            processor = new ConditionalPut(NeverFilter.INSTANCE, valueNew);

            mapResult = cache.invokeAll(Collections.singletonList("1"), processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), valueCurrent));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), valueCurrent));

            cache.clear();

            // Use an NeverFilter and verify that the put doesn't happen.
            // An map containing the current value should be returned...
            // fReturn = true.
            cache.put("1", valueCurrent);

            processor = new ConditionalPut(NeverFilter.INSTANCE, valueNew, true);

            mapResult = cache.invokeAll(Collections.singletonList("1"), processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), valueCurrent));
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), valueCurrent));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 1);
            assertTrue("Result=" + mapResult, equals(mapResult.get("1"), valueCurrent));
            assertTrue("Value=" + cache.get("1"), equals(cache.get("1"), valueCurrent));

            cache.clear();

            // putIfAbsent
            value.setLongValue(3L);
            filter = new NotFilter(PresentFilter.INSTANCE);
            cache.invoke("1",
                    new ConditionalPut(filter, value)); // should work
            valueCheck = (TestValue) cache.get("1");
            assertTrue(valueCheck + "!=" + valueOld, equals(valueCheck, value));

            valueOld = ((TestValue) cache.get("1")).cloneValue();
            value = value.cloneValue();
            value.setLongValue(4L);
            oResult = cache.invoke("1",
                    new ConditionalPut(filter, value, true)); // should fail
            assertTrue("Result=" + oResult, equals(oResult, valueOld));
            valueCheck = (TestValue) cache.get("1");
            assertTrue(valueCheck + "!=" + valueOld, equals(valueCheck, valueOld));
            assertTrue(valueCheck + "==" + value, !equals(valueCheck, value));

            // replace
            valueOld = ((TestValue) cache.get("1")).cloneValue();
            value = value.cloneValue();
            value.setLongValue(5L);
            filter = new EqualsFilter(IdentityExtractor.INSTANCE, valueOld);
            cache.invoke("1",
                    new ConditionalPut(filter, value)); // should work
            valueCheck = (TestValue) cache.get("1");
            assertTrue(valueCheck + "!=" + value, equals(valueCheck, value));

            valueOld = ((TestValue) cache.get("1")).cloneValue();
            value = value.cloneValue();
            value.setLongValue(6L);
            filter = new EqualsFilter(IdentityExtractor.INSTANCE, value);
            cache.invoke("1",
                    new ConditionalPut(filter, value)); // should fail
            valueCheck = (TestValue) cache.get("1");
            assertTrue(valueCheck + "!=" + valueOld, equals(valueCheck, valueOld));

            // Test that the result map returned from processAll will only
            // include values from entries that fail the processor's filter check.
            cache.clear();
            cache.put("1", value);
            cache.put("2", value);
            cache.put("3", value);

            Set setEntries = new HashSet();
            setEntries.add("1");
            setEntries.add("2");
            setEntries.add("3");

            valueNew = new TestValue();
            valueNew.setLongValue(99L);

            processor = new ConditionalPut(NeverFilter.INSTANCE, valueNew, true);

            mapResult = cache.invokeAll(setEntries, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 3);
            assertTrue("Key mssing from results",
                    mapResult.containsKey("1") &&
                            mapResult.containsKey("2") &&
                            mapResult.containsKey("3"));

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            value = (TestValue) cache.get("1");
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 3);
            assertTrue("Key mssing from results",
                    mapResult.containsKey("1") &&
                            mapResult.containsKey("2") &&
                            mapResult.containsKey("3"));

            processor = new ConditionalPut(AlwaysFilter.INSTANCE, valueNew, true);

            mapResult = cache.invokeAll(setEntries, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }
        }

    /**
    * Test of the {@link com.tangosol.util.processor.VersionedPut}
    */
    @Test
    public void versionedPut()
        {
        NamedCache cache = getNamedCache();
        TestValue  value, valueOld, valueNew;
        Object     oResult;
        Map        mapResult;

        cache.clear();

        try
            {
            // simple puts
            // allow insert = false
            value = new TestValue();
            value.setLongValue(1L);
            value.setLongValue(100L);

            oResult = cache.invoke("1", Processors.versionedPut(value));
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + "is not null", equals(value, null));

            // allow insert = true, return = false
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(100L);

            valueNew = new TestValue();
            valueNew.setLongValue(1L);
            valueNew.setVersion(101L);

            oResult = cache.invoke("1", new VersionedPut(value, true, false));
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + " != " + valueNew, equals(value, valueNew));

            // no match, allow insert = true, return = true
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(100L);

            valueOld = new TestValue();
            valueOld.setLongValue(1L);
            valueOld.setVersion(101L);

            oResult = cache.invoke("1", Processors.versionedPut(value, true, true));
            assertTrue(value + " != " + valueOld, equals(oResult, valueOld));
            value = (TestValue) cache.get("1");
            assertTrue(value + "!=" + oResult, equals(value, oResult));

            // match, allow insert = true, return = true
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(101L);

            valueNew = new TestValue();
            valueNew.setLongValue(1L);
            valueNew.setVersion(102L);

            oResult = cache.invoke("1", new VersionedPut(value, true, true));
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + " != " + valueNew, equals(value, valueNew));

            cache.clear();

            //invokeAll tests with key set
            // allow insert = false
            value = new TestValue();
            value.setLongValue(1L);
            value.setLongValue(100L);

            mapResult = cache.invokeAll(Collections.singleton("1"), new VersionedPut(value));
            oResult = mapResult.get("1");
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + "is not null", equals(value, null));

            // allow insert = true, return = false
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(100L);

            valueNew = new TestValue();
            valueNew.setLongValue(1L);
            valueNew.setVersion(101L);

            mapResult = cache.invokeAll(Collections.singleton("1"), new VersionedPut(value, true, false));
            oResult = mapResult.get("1");
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + " != " + valueNew, equals(value, valueNew));

            // no match, allow insert = true, return = true
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(100L);

            valueOld = new TestValue();
            valueOld.setLongValue(1L);
            valueOld.setVersion(101L);

            mapResult = cache.invokeAll(Collections.singleton("1"), new VersionedPut(value, true, true));
            oResult = mapResult.get("1");
            assertTrue(value + " != " + valueOld, equals(oResult, valueOld));
            value = (TestValue) cache.get("1");
            assertTrue(value + "!=" + oResult, equals(value, oResult));

            // match, allow insert = true, return = true
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(101L);

            valueNew = new TestValue();
            valueNew.setLongValue(1L);
            valueNew.setVersion(102L);

            mapResult = cache.invokeAll(Collections.singleton("1"), new VersionedPut(value, true, true));
            oResult = mapResult.get("1");
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + " != " + valueNew, equals(value, valueNew));

            cache.clear();

            //invokeAll tests with filter
            // allow insert = false
            value = new TestValue();
            value.setLongValue(1L);
            value.setLongValue(100L);

            cache.put("1", null);
            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, new VersionedPut(value));
            oResult = mapResult.get("1");
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + "is not null", equals(value, null));

            // allow insert = true, return = false
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(100L);

            valueNew = new TestValue();
            valueNew.setLongValue(1L);
            valueNew.setVersion(101L);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, new VersionedPut(value, true, false));
            oResult = mapResult.get("1");
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + " != " + valueNew, equals(value, valueNew));

            // no match, allow insert = true, return = true
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(100L);

            valueOld = new TestValue();
            valueOld.setLongValue(1L);
            valueOld.setVersion(101L);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, new VersionedPut(value, true, true));
            oResult = mapResult.get("1");
            assertTrue(value + " != " + valueOld, equals(oResult, valueOld));
            value = (TestValue) cache.get("1");
            assertTrue(value + "!=" + oResult, equals(value, oResult));

            // match, allow insert = true, return = true
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(101L);

            valueNew = new TestValue();
            valueNew.setLongValue(1L);
            valueNew.setVersion(102L);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, new VersionedPut(value, true, true));
            oResult = mapResult.get("1");
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + " != " + valueNew, equals(value, valueNew));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.clear();
        }

    /**
    * Test of the {@link com.tangosol.util.processor.VersionedPutAll}
    */
    @Test
    public void versionedPutAll()
        {
        NamedCache cache = getNamedCache();
        TestValue  value, valueOld, valueNew;
        Object     oResult;
        Map        mapEntries = new HashMap();
        Map        mapResult;

        cache.clear();

        try
            {
            // simple puts
            // allow insert = false
            value = new TestValue();
            value.setLongValue(1L);
            value.setLongValue(100L);

            mapEntries.put("1", value);
            oResult = cache.invoke("1", Processors.versionedPutAll(mapEntries));
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + "is not null", equals(value, null));

            // allow insert = true, return = false
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(100L);

            valueNew = new TestValue();
            valueNew.setLongValue(1L);
            valueNew.setVersion(101L);

            mapEntries.put("1", value);
            oResult = cache.invoke("1", Processors.versionedPutAll(mapEntries, true, false));
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + " != " + valueNew, equals(value, valueNew));

            // no match, allow insert = true, return = true
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(100L);

            valueOld = new TestValue();
            valueOld.setLongValue(1L);
            valueOld.setVersion(101L);

            mapEntries.put("1", value);
            oResult = cache.invoke("1", new VersionedPutAll(mapEntries, true, true));
            assertTrue(value + " != " + valueOld, equals(oResult, valueOld));
            value = (TestValue) cache.get("1");
            assertTrue(value + "!=" + oResult, equals(value, oResult));

            // match, allow insert = true, return = true
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(101L);

            valueNew = new TestValue();
            valueNew.setLongValue(1L);
            valueNew.setVersion(102L);

            mapEntries.put("1", value);
            oResult = cache.invoke("1", new VersionedPutAll(mapEntries, true, true));
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + " != " + valueNew, equals(value, valueNew));

            cache.clear();

            //invokeAll tests with key set
            // allow insert = false
            value = new TestValue();
            value.setLongValue(1L);
            value.setLongValue(100L);

            mapEntries.put("1", value);
            mapResult = cache.invokeAll(Collections.singleton("1"), new VersionedPutAll(mapEntries));
            oResult = mapResult.get("1");
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + "is not null", equals(value, null));

            // allow insert = true, return = false
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(100L);

            valueNew = new TestValue();
            valueNew.setLongValue(1L);
            valueNew.setVersion(101L);

            mapEntries.put("1", value);
            mapResult = cache.invokeAll(Collections.singleton("1"), new VersionedPutAll(mapEntries, true, false));
            oResult = mapResult.get("1");
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + " != " + valueNew, equals(value, valueNew));

            // no match, allow insert = true, return = true
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(100L);

            valueOld = new TestValue();
            valueOld.setLongValue(1L);
            valueOld.setVersion(101L);

            mapEntries.put("1", value);
            mapResult = cache.invokeAll(Collections.singleton("1"), new VersionedPutAll(mapEntries, true, true));
            oResult = mapResult.get("1");
            assertTrue(value + " != " + valueOld, equals(oResult, valueOld));
            value = (TestValue) cache.get("1");
            assertTrue(value + "!=" + oResult, equals(value, oResult));

            // match, allow insert = true, return = true
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(101L);

            valueNew = new TestValue();
            valueNew.setLongValue(1L);
            valueNew.setVersion(102L);

            mapEntries.put("1", value);
            mapResult = cache.invokeAll(Collections.singleton("1"), new VersionedPutAll(mapEntries, true, true));
            oResult = mapResult.get("1");
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + " != " + valueNew, equals(value, valueNew));

            cache.clear();

            //invokeAll tests with filter
            // allow insert = false
            value = new TestValue();
            value.setLongValue(1L);
            value.setLongValue(100L);

            cache.put("1", null);
            mapEntries.put("1", value);
            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, new VersionedPutAll(mapEntries));
            oResult = mapResult.get("1");
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + "is not null", equals(value, null));

            // allow insert = true, return = false
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(100L);

            valueNew = new TestValue();
            valueNew.setLongValue(1L);
            valueNew.setVersion(101L);

            mapEntries.put("1", value);
            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, new VersionedPutAll(mapEntries, true, false));
            oResult = mapResult.get("1");
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + " != " + valueNew, equals(value, valueNew));

            // no match, allow insert = true, return = true
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(100L);

            valueOld = new TestValue();
            valueOld.setLongValue(1L);
            valueOld.setVersion(101L);

            mapEntries.put("1", value);
            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, new VersionedPutAll(mapEntries, true, true));
            oResult = mapResult.get("1");
            assertTrue(value + " != " + valueOld, equals(oResult, valueOld));
            value = (TestValue) cache.get("1");
            assertTrue(value + "!=" + oResult, equals(value, oResult));

            // match, allow insert = true, return = true
            value = new TestValue();
            value.setLongValue(1L);
            value.setVersion(101L);

            valueNew = new TestValue();
            valueNew.setLongValue(1L);
            valueNew.setVersion(102L);

            mapEntries.put("1", value);
            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, new VersionedPutAll(mapEntries, true, true));
            oResult = mapResult.get("1");
            assertTrue("Result=" + oResult, oResult == null);
            value = (TestValue) cache.get("1");
            assertTrue(value + " != " + valueNew, equals(value, valueNew));
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }

        cache.clear();
        }

    /**
    * Test of the {@link com.tangosol.util.processor.PreloadRequest}
    */
    @Test
    public void preload()
        {
        NamedCache cache = getNamedCache();
        Object     oResult;
        Map        mapResult;

        cache.clear();

        // Not much to assert here... at least make sure that we can
        // exercise the code and get the expected return.

        try
            {
            // invoke
            oResult = cache.invoke("1", Processors.preload());
            assertTrue("Result=" + oResult, oResult == null);

            // invokeAll with key set
            mapResult = cache.invokeAll(Collections.singleton("1"), new PreloadRequest());
            assertTrue("Map size=" + mapResult.size(), mapResult.size() == 0);

            // invokeAll with filter
            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, new PreloadRequest());
            assertTrue("Map size=" + mapResult.size(), mapResult.size() == 0);
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }
        }

    /**
    * Test of the {@link com.tangosol.util.processor.ConditionalPutAll}
    */
    @Test
    public void putAll()
        {
        NamedCache cache   = getNamedCache();
        Map        mapTemp = generateTestMap(2);
        Map        mapResult;
        Filter     filter;
        TestValue  value;

        cache.clear();

        try
            {
            // putAll
            mapResult = cache.invokeAll(mapTemp.keySet(),
                    Processors.putAll(AlwaysFilter.INSTANCE, mapTemp));
            assertTrue(cache.size() + "!=" + 2, cache.size() == 2);
            assertTrue("mapResult=" + mapResult, mapResult == null || mapResult.size() == 0);

            cache.clear();

            // putAllIfAbsent
            filter = new NotFilter(PresentFilter.INSTANCE);
            mapResult = cache.invokeAll(mapTemp.keySet(),
                    new ConditionalPutAll(filter, mapTemp));
            assertTrue(cache.size() + "!=" + 2, cache.size() == 2);
            assertTrue("mapResult=" + mapResult, mapResult == null || mapResult.size() == 0);

            value = new TestValue();
            value.setLongValue((long) -1);
            mapTemp.put(1, value);
            value = new TestValue();
            value.setLongValue((long) -2);
            mapTemp.put(2, value);

            mapResult = cache.invokeAll(mapTemp.keySet(),
                    new ConditionalPutAll(filter, mapTemp));
            value = (TestValue) cache.get(1);
            assertTrue(value.getLongValue() + "!=" + 1, value.getLongValue().longValue() == 1);
            assertTrue("mapResult=" + mapResult, mapResult == null || mapResult.size() == 0);

            // replace
            filter = PresentFilter.INSTANCE;
            value.setLongValue((long) -3);
            mapTemp.put(3, value);

            mapResult = cache.invokeAll(mapTemp.keySet(),
                    new ConditionalPutAll(filter, mapTemp));
            assertTrue(cache.size() + "!=" + 2, cache.size() == 2);
            assertTrue("mapResult=" + mapResult, mapResult == null || mapResult.size() == 0);

            value = (TestValue) cache.get(1);
            assertTrue(value.getLongValue() + "!=" + 1, value.getLongValue().longValue() == -1);


            // Test that the result map returned from processAll is always empty.
            cache.clear();
            cache.put("1", value);
            cache.put("2", value);
            cache.put("3", value);

            Set setEntries = new HashSet();
            setEntries.add("1");
            setEntries.add("2");
            setEntries.add("3");

            mapTemp.clear();
            mapTemp.put("1", value);
            mapTemp.put("2", value);
            mapTemp.put("3", value);

            TestValue valueNew;
            valueNew = new TestValue();
            valueNew.setLongValue(99L);

            ConditionalPutAll processor = new ConditionalPutAll(NeverFilter.INSTANCE, mapTemp);

            mapResult = cache.invokeAll(setEntries, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);

            processor = new ConditionalPutAll(AlwaysFilter.INSTANCE, mapTemp);

            mapResult = cache.invokeAll(setEntries, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);

            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == 0);
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }
        }

    /**
    * Test of a custom processor.
    */
    @Test
    public void custom()
        {
        NamedCache cache = getNamedCache();
        Object     oResult;

        InvocableMap.EntryProcessor agent = new CustomProcessor();

        cache.clear();

        try
            {
            oResult = cache.invoke(0, agent);
            assertTrue("Result=" + oResult, oResult == null);

            int cSize = 100;
            Map mapTemp = generateTestMap(cSize);
            cache.putAll(mapTemp);

            for (int i = 1; i <= cSize; i++)
                {
                oResult = cache.invoke(i, agent);
                assertTrue("Result " + oResult, oResult.equals((double) i));
                }

            agent = new CustomProcessor(true);
            Map mapResult = cache.invokeAll(mapTemp.keySet(), agent);
            assertTrue("Size=" + mapResult.size(), mapResult.size() == cSize);
            }
        catch (Throwable t)
            {
            Base.log("Failed with Throwable " + t.getMessage() + '\n' + Base.printStackTrace(t));
            heapDump();
            throw t;
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Return a new Map of requested size with test data (1-based).
    *
    * @param cSize  the size of the returned Map
    *
    * @return a new Map
    */
    public static Map generateTestMap(int cSize)
        {
        Map map = new HashMap();

        for (int i = 1; i <= cSize; i++)
            {
            TestValue value = new TestValue();

            value.setIntegerValue(i);
            value.setLongValue((long) i);
            value.setByteValue((byte) i);
            value.setDoubleValue((double) i);
            map.put(i, value);
            }

        return map;
        }

    /**
    * Get heapdump from the cluster members.
    */
    public static void heapDump()
        {
        List<CoherenceClusterMember> list = m_listClusterMembers;
        String                       path = System.getProperty("test.project.dir") + File.separatorChar + "target";
        if (list.isEmpty())
            {
            String sDump = HeapDump.dumpHeap(path, true);
            Base.log("Dumping heap for analysis here :\n" + sDump);
            }
        else
            {
            for(CoherenceClusterMember member : m_listClusterMembers)
                {
                member.submit(()-> Base.log("Dumping heap for analysis here : \n" + HeapDump.dumpHeap(path, true)));
                }
            }
        }



    // ----- CustomProcessor inner class ------------------------------------

    public static class CustomProcessor
            extends AbstractProcessor
            implements PortableObject
        {

        public CustomProcessor()
            {
            this(false);
            }

        public CustomProcessor(boolean fOnInvokeAll)
            {
            m_fOnInvokeAll = fOnInvokeAll;
            }

        // ----- EntryProcessor interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public Object process(InvocableMap.Entry entry)
            {
            assertTrue("invokeAll flag is true.", !m_fOnInvokeAll);
            return processEntry(entry);
            }

        /**
        * {@inheritDoc}
        */
        public Map processAll(Set setEntries)
            {
            assertTrue("invokeAll flag is false.", m_fOnInvokeAll);

            Map mapResults = new LiteMap();
            for (Iterator iter = setEntries.iterator(); iter.hasNext(); )
                {
                InvocableMap.Entry entry = (InvocableMap.Entry) iter.next();
                mapResults.put(entry.getKey(), processEntry(entry));
                }
            return mapResults;
            }

        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_fOnInvokeAll = in.readBoolean(0);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeBoolean(0, m_fOnInvokeAll);
            }

        // ----- helper methods -------------------------------------------

        private Object processEntry(InvocableMap.Entry entry)
            {
            if (entry.isPresent())
                {
                Integer   IKey  = (Integer)   entry.getKey();
                TestValue value = (TestValue) entry.getValue();

                assertTrue("Key=" + IKey + "; value=" + value,
                        IKey.intValue() == value.getLongValue().intValue());
                return value.getDoubleValue();
                }
            else
                {
                return null;
                }
            }

        // ----- data members ---------------------------------------------

        /**
        * Invoked through invokeAll.
        */
        private boolean m_fOnInvokeAll;
        }


    // ----- TestValue inner class ------------------------------------------

    /**
    * Test value object with various numerical properties.
    */
    public static class TestValue
            extends Base
            implements PortableObject, Serializable, Cloneable, Versionable
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor.
        */
        public TestValue()
            {
            m_version = 0L;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Byte value associated with this TestValue object.
        *
        * @return  the Byte value
        */
        public Byte getByteValue()
            {
            return m_ByteValue;
            }

        /**
        * Set the Byte value associated with this TestValue object.
        *
        * @param value  the new Byte value
        */
        public void setByteValue(Byte value)
            {
            m_ByteValue = value;
            }

        /**
        * Return the Short value associated with this TestValue object.
        *
        * @return  the Short value
        */
        public Short getShortValue()
            {
            return m_ShortValue;
            }

        /**
        * Set the Short value associated with this TestValue object.
        *
        * @param value  the new Short value
        */
        public void setShortValue(Short value)
            {
            m_ShortValue = value;
            }

        /**
        * Return the Integer value associated with this TestValue object.
        *
        * @return  the Integer value
        */
        public Integer getIntegerValue()
            {
            return m_IntegerValue;
            }

        /**
        * Set the Integer value associated with this TestValue object.
        *
        * @param value  the new Integer value
        */
        public void setIntegerValue(Integer value)
            {
            m_IntegerValue = value;
            }

        /**
        * Return the Long value associated with this TestValue object.
        *
        * @return  the Long value
        */
        public Long getLongValue()
            {
            return m_LongValue;
            }

        /**
        * Set the Long value associated with this TestValue object.
        *
        * @param value  the new Long value
        */
        public void setLongValue(Long value)
            {
            m_LongValue = value;
            }

        /**
        * Return the Float value associated with this TestValue object.
        *
        * @return  the Float value
        */
        public Float getFloatValue()
            {
            return m_FloatValue;
            }

        /**
        * Set the Float value associated with this TestValue object.
        *
        * @param value  the new Float value
        */
        public void setFloatValue(Float value)
            {
            m_FloatValue = value;
            }

        /**
        * Return the Double value associated with this TestValue object.
        *
        * @return  the Double value
        */
        public Double getDoubleValue()
            {
            return m_DoubleValue;
            }

        /**
        * Set the Double value associated with this TestValue object.
        *
        * @param value  the new Double value
        */
        public void setDoubleValue(Double value)
            {
            m_DoubleValue = value;
            }

        /**
        * Return the BigInteger value associated with this TestValue object.
        *
        * @return  the BigInteger value
        */
        public BigInteger getBigIntegerValue()
            {
            return m_BigIntegerValue;
            }

        /**
        * Set the BigInteger value associated with this TestValue object.
        *
        * @param value  the new BigInteger value
        */
        public void setBigIntegerValue(BigInteger value)
            {
            m_BigIntegerValue = value;
            }

        /**
        * Return the BigDecimal value associated with this TestValue object.
        *
        * @return  the BigDecimal value
        */
        public BigDecimal getBigDecimalValue()
            {
            return m_BigDecimalValue;
            }

        /**
        * Set the BigDecimal value associated with this TestValue object.
        *
        * @param value  the new BigDecimal value
        */
        public void setBigDecimalValue(BigDecimal value)
            {
            m_BigDecimalValue = value;
            }

        /**
        * Get the version
        * @return the version
        */
        public Long getVersion()
            {
            return m_version;
            }

        /**
        * Set the version
        * @param version the version
        */
        public void setVersion(Long version)
            {
            m_version = version;
            }

        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_ByteValue        = (Byte) in.readObject(0);
            m_ShortValue       = (Short) in.readObject(1);
            m_IntegerValue     = (Integer) in.readObject(2);
            m_LongValue        = (Long) in.readObject(3);
            m_FloatValue       = (Float) in.readObject(4);
            m_DoubleValue      = (Double) in.readObject(5);
            m_BigIntegerValue  = (BigInteger) in.readObject(6);
            m_BigDecimalValue  = (BigDecimal) in.readObject(7);
            m_version          = (Long) in.readObject(8);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_ByteValue      );
            out.writeObject(1, m_ShortValue     );
            out.writeObject(2, m_IntegerValue   );
            out.writeObject(3, m_LongValue      );
            out.writeObject(4, m_FloatValue     );
            out.writeObject(5, m_DoubleValue    );
            out.writeObject(6, m_BigIntegerValue);
            out.writeObject(7, m_BigDecimalValue);
            out.writeObject(8, m_version        );
            }

        // ----- Versionable methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        public Comparable getVersionIndicator()
            {
            return m_version;
            }

        /**
        * {@inheritDoc}
        */
        public void incrementVersion()
            {
            m_version = m_version.longValue() + 1;
            }

        // ----- Object methods -------------------------------------------

        /**
        * Returns a hash code value for the object.
        *
        * @return a hash code value for this object.
        */
        public int hashCode()
            {
            int nHash = 0;

            if (m_ByteValue != null)
                {
                nHash ^= m_ByteValue.hashCode();
                }

            if (m_ShortValue != null)
                {
                nHash ^= m_ShortValue.hashCode();
                }

            if (m_IntegerValue != null)
                {
                nHash ^= m_IntegerValue.hashCode();
                }

            if (m_LongValue != null)
                {
                nHash ^= m_LongValue.hashCode();
                }

            if (m_FloatValue != null)
                {
                nHash ^= m_FloatValue.hashCode();
                }

            if (m_DoubleValue != null)
                {
                nHash ^= m_DoubleValue.hashCode();
                }

            if (m_BigIntegerValue != null)
                {
                nHash ^= m_BigIntegerValue.hashCode();
                }

            if (m_BigDecimalValue != null)
                {
                nHash ^= m_BigDecimalValue.hashCode();
                }

            nHash ^= m_version.hashCode();

            return nHash;
            }

        /**
        * Indicates whether some other object is "equal to" this one.
        *
        * @param o the reference object with which to compare.
        *
        * @return <code>true</code> if this object is the same as the o
        *         argument; <code>false</code> otherwise.
        */
        public boolean equals(Object o)
            {
            if (o instanceof TestValue)
                {
                TestValue that = (TestValue) o;

                return this == that ||
                    equals(this.m_ByteValue, that.m_ByteValue) &&
                    equals(this.m_ShortValue, that.m_ShortValue) &&
                    equals(this.m_IntegerValue, that.m_IntegerValue) &&
                    equals(this.m_LongValue, that.m_LongValue) &&
                    equals(this.m_FloatValue, that.m_FloatValue) &&
                    equals(this.m_DoubleValue, that.m_DoubleValue) &&
                    equals(this.m_BigIntegerValue, that.m_BigIntegerValue) &&
                    equals(this.m_BigDecimalValue, that.m_BigDecimalValue) &&
                    equals(this.m_version, that.m_version);
                }

            return false;
            }

        /**
        * Returns a string representation of the object.
        *
        * @return a string representation of the object.
        */
        public String toString()
            {
            return new StringBuffer("TestValue(ByteValue=")
                    .append(m_ByteValue)
                    .append(", ShortValue=")
                    .append(m_ShortValue)
                    .append(", IntegerValue=")
                    .append(m_IntegerValue)
                    .append(", LongValue=")
                    .append(m_LongValue)
                    .append(", FloatValue=")
                    .append(m_FloatValue)
                    .append(", DoubleValue=")
                    .append(m_DoubleValue)
                    .append(", BigIntegerValue=")
                    .append(m_BigIntegerValue)
                    .append(", BigDecimalValue=")
                    .append(m_BigDecimalValue)
                    .append(", Version=")
                    .append(m_version)
                    .append(')')
                    .toString();
            }

        /**
        * Clone the TestValue.
        *
        * @return a new TestValue
        */
        public TestValue cloneValue()
            {
            try
                {
                return (TestValue) super.clone();
                }
            catch (CloneNotSupportedException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        // ----- data members ---------------------------------------------

        /**
        * A Byte value.
        */
        private Byte m_ByteValue;

        /**
        * A Short value.
        */
        private Short m_ShortValue;

        /**
        * An Integer value.
        */
        private Integer m_IntegerValue;

        /**
        * A Long value.
        */
        private Long m_LongValue;

        /**
        * A Float value.
        */
        private Float m_FloatValue;

        /**
        * A Double value.
        */
        private Double m_DoubleValue;

        /**
        * A BigInteger value.
        */
        private BigInteger m_BigIntegerValue;

        /**
        * A BigDecimal value.
        */
        private BigDecimal m_BigDecimalValue;

        /**
        * The version number
        */
        private Long m_version;
        }


    // ----- constants ------------------------------------------------------

    private static final int SMALL_TEST_SIZE  = 10;


    // ----- accessors ------------------------------------------------------

    /**
    * Return the name of the cache used in all test methods.
    *
    * @return the name of the cache used in all test methods
    */
    protected String getCacheName()
        {
        return m_sCache;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of the cache used in all test methods.
    */
    protected final String m_sCache;

    /**
    * The list of cluster members.
    */
    protected static List<CoherenceClusterMember> m_listClusterMembers = new SafeLinkedList();
    }
