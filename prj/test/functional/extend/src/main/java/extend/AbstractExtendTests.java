/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.InvocationService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheStore;
import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Binary;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.EntryAggregator;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.BigDecimalAverage;
import com.tangosol.util.aggregator.BigDecimalMax;
import com.tangosol.util.aggregator.BigDecimalMin;
import com.tangosol.util.aggregator.BigDecimalSum;
import com.tangosol.util.aggregator.ComparableMax;
import com.tangosol.util.aggregator.ComparableMin;
import com.tangosol.util.aggregator.CompositeAggregator;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.DistinctValues;
import com.tangosol.util.aggregator.DoubleAverage;
import com.tangosol.util.aggregator.DoubleMax;
import com.tangosol.util.aggregator.DoubleMin;
import com.tangosol.util.aggregator.DoubleSum;
import com.tangosol.util.aggregator.LongMax;
import com.tangosol.util.aggregator.LongMin;
import com.tangosol.util.aggregator.LongSum;
import com.tangosol.util.aggregator.ReducerAggregator;
import com.tangosol.util.aggregator.TopNAggregator;

import com.tangosol.util.comparator.InverseComparator;
import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EntryFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.FilterTrigger;
import com.tangosol.util.filter.InFilter;
import com.tangosol.util.filter.LessFilter;
import com.tangosol.util.filter.LimitFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.MapEventTransformerFilter;
import com.tangosol.util.filter.NeverFilter;

import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.NumberIncrementor;

import com.tangosol.util.transformer.SemiLiteEventTransformer;

import com.oracle.coherence.testing.CaseInsensitiveComparator;
import com.oracle.coherence.testing.TestMapListener;
import com.oracle.coherence.testing.TestNCDListener;
import com.oracle.coherence.testing.TestSynchronousMapListener;

import data.Person;
import data.Trade;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Arrays;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
* A collection of functional tests for Coherence*Extend.
*
* @author jh  2005.11.29
*/
@SuppressWarnings("unchecked")
public abstract class AbstractExtendTests
        extends AbstractExtendTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new AbstractExtendTest that will use the cache with the given
    * name in all test methods.
    *
    * @param sCache  the test cache name, one of the CACHE_* constants
    */
    public AbstractExtendTests(String sCache)
        {
        this(sCache, FILE_CLIENT_CFG_CACHE);
        }

    /**
    * Create a new AbstractExtendTest that will use the cache with the given
    * name configured by the given cache configuration file in all test
    * methods.
    *
    * @param sCache  the test cache name, one of the CACHE_* constants
    * @param sPath   the configuration resource name or file path
    */
    public AbstractExtendTests(String sCache, String sPath)
        {
        super(sCache, sPath);
        }

    // ----- AbstractExtendTests methods ------------------------------------

    /**
    * Convert the passed in key to a (potentially) different object. This
    * allows classes which extend AbstractExtendTests to use different key
    * classes.
    *
    * @param o  the initial key object
    *
    * @return the key object
    */
    protected Object getKeyObject(Object o)
        {
        return o;
        }

    // ----- NamedCache test methods ----------------------------------------

    /**
    * Test the behavior of {@link NamedCache#clear()},
    * {@link NamedCache#size()}, and {@link NamedCache#isEmpty()}.
    */
    @Test
    public void clear()
        {
        NamedCache cache = getNamedCache();

        cache.put(getKeyObject("Key"), "Value");
        assertTrue(cache.size() > 0);
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.size() == 0);
        assertTrue(cache.isEmpty());
        }

    /**
    * Test the behavior of {@link NamedCache#truncate()},
    * {@link NamedCache#size()}, and {@link NamedCache#isEmpty()}.
    */
    @Test
    public void truncate()
        {
        NamedCache cache = getNamedCache();

        cache.put(getKeyObject("Key"), "Value");
        assertTrue(cache.size() > 0);
        assertFalse(cache.isEmpty());
        cache.truncate();
        assertTrue(cache.size() == 0);
        assertTrue(cache.isEmpty());
        assertTrue(cache.isActive());
        }

    /**
    * Test the behavior of {@link NamedCache#truncate()},
    * {@link NamedCache#size()}, and {@link NamedCache#isEmpty()}.
    */
    @Test
    public void truncateWithListener()
        {
        if (VIEW_EXTEND_DIRECT.equals(getCacheName()) ||
            VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }
        NamedCache         cache = getNamedCache();
        TestNCDListener listener = new TestNCDListener();

        cache.put(getKeyObject("Key"), "Value");
        assertTrue(cache.size() > 0);
        assertFalse(cache.isEmpty());

        cache.addMapListener(listener);
        cache.truncate();

        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
        assertTrue(cache.isActive());

        MapEvent evt = listener.waitForEvent();
        assertNotNull("Event should be not null", evt);
        assertEquals("Event should be of type: " + MapEvent.ENTRY_UPDATED,
                     MapEvent.ENTRY_UPDATED, evt.getId());
        assertThat(evt.getOldValue(), is(nullValue()));
        assertThat(evt.getNewValue(), is(nullValue()));
        assertThat(evt.getKey(), is(nullValue()));
        }

    /**
    * Test the behavior of {@link NamedCache#containsKey(Object)}.
    */
    @Test
    public void containsKey()
        {
        NamedCache cache = getNamedCache();

        cache.clear();
        assertTrue(cache.isEmpty());

        assertFalse(cache.containsKey(getKeyObject("Key")));
        assertFalse(cache.containsKey("Value"));

        cache.put(getKeyObject("Key"), "Value");
        assertTrue(cache.size() == 1);

        assertTrue(cache.containsKey(getKeyObject("Key")));
        assertFalse(cache.containsKey(getKeyObject("Value")));
        }

    /**
    * Test the behavior of {@link NamedCache#containsValue(Object)}.
    */
    @Test
    public void containsValue()
        {
        NamedCache cache = getNamedCache();

        cache.clear();
        assertTrue(cache.isEmpty());

        assertFalse(cache.containsValue(getKeyObject("Key")));
        assertFalse(cache.containsValue("Value"));

        cache.put(getKeyObject("Key"), "Value");
        assertTrue(cache.size() == 1);

        assertFalse(cache.containsValue(getKeyObject("Key")));
        assertTrue(cache.containsValue("Value"));
        }

    /**
    * Test the behavior of {@link NamedCache#put(Object, Object)} and
    * {@link NamedCache#get(Object)}.
    */
    @Test
    public void put()
        {
        NamedCache cache = getNamedCache();

        cache.clear();
        assertTrue(cache.isEmpty());

        assertEquals(null, cache.get(getKeyObject("Key")));
        assertEquals(null, cache.put(getKeyObject("Key"), "Value"));
        assertEquals("Value", cache.get(getKeyObject("Key")));
        assertEquals(1, cache.size());

        if (CACHE_DIST_EXTEND_DIRECT_BUNDLING.equals(getCacheName()))
            {
            assertEquals(null, cache.put(getKeyObject("Key"), "NewValue"));
            }
        else
            {
            assertEquals("Value", cache.put(getKeyObject("Key"), "NewValue"));
            }
        assertEquals("NewValue", cache.get(getKeyObject("Key")));

        cache.clear();
        assertTrue(cache.isEmpty());

        assertEquals(null, cache.get(getKeyObject("Key1")));
        assertEquals(null, cache.put(getKeyObject("Key1"), "Value1"));
        assertEquals("Value1", cache.get(getKeyObject("Key1")));
        assertEquals(null, cache.get(getKeyObject("Key2")));
        assertEquals(null, cache.put(getKeyObject("Key2"), "Value2"));
        assertEquals("Value2", cache.get(getKeyObject("Key2")));
        assertEquals(2, cache.size());
        }

    /**
    * Test the behavior of {@link NamedCache#put(Object, Object)} and
    * {@link NamedCache#get(Object)} when binary pass-through is enabled.
    */
    @Test
    public void putPassThrough()
        {
        if (CACHE_DIST_EXTEND_CLIENT_KEY.equals(getCacheName())      ||
            CACHE_DIST_EXTEND_LOCAL.equals(getCacheName())           ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()))
            {
            return;
            }

        NamedCache   cache     = getNamedCache();
        CacheService service   = cache.getCacheService();
        Serializer   ser       = service.getSerializer();
        NamedCache   cacheBin  = service.ensureCache(getCacheName(), NullImplementation.getClassLoader());
        Binary       binNull   = ExternalizableHelper.toBinary(null, ser);
        Binary       binKey    = ExternalizableHelper.toBinary(getKeyObject("Key"), ser);
        Binary       binKey1   = ExternalizableHelper.toBinary(getKeyObject("Key1"), ser);
        Binary       binValue  = ExternalizableHelper.toBinary("Value", ser);
        Binary       binValue1 = ExternalizableHelper.toBinary("Value1", ser);
        Binary       binValue2 = ExternalizableHelper.toBinary("Value2", ser);

        cache.clear();
        assertTrue(cache.isEmpty());
        assertTrue(cacheBin.isEmpty());

        assertEquals(null, cache.get(getKeyObject("Key")));
        Object o = cacheBin.get(binKey);
        assertTrue(equals(o, null) || equals(o, binNull));
        assertEquals(0, cache.size());

        assertEquals(null, cache.put(getKeyObject("Key"), "Value"));
        assertEquals("Value", cache.get(getKeyObject("Key")));
        assertEquals(binValue, cacheBin.get(binKey));
        assertEquals(1, cache.size());

        o = cacheBin.put(binKey1, binValue1);
        assertTrue(equals(o, null) || equals(o, binNull));
        assertEquals("Value1", cache.get(getKeyObject("Key1")));
        assertEquals(binValue1, cacheBin.get(binKey1));
        assertEquals(2, cache.size());

        if (CACHE_DIST_EXTEND_DIRECT_BUNDLING.equals(getCacheName()))
            {
            assertEquals(null, cache.put(getKeyObject("Key"), "Value2"));
            }
        else
            {
            assertEquals("Value", cache.put(getKeyObject("Key"), "Value2"));
            }
        assertEquals("Value2", cache.get(getKeyObject("Key")));
        assertEquals(binValue2, cacheBin.get(binKey));
        assertEquals(binValue1, cacheBin.put(binKey1, binValue2));
        assertEquals("Value2", cache.get(getKeyObject("Key1")));
        assertEquals(binValue2, cacheBin.get(binKey1));

        cache.clear();
        assertTrue(cache.isEmpty());
        assertTrue(cacheBin.isEmpty());
        }

    /**
    * Test the behavior of {@link NamedCache#putAll(Map)} and
    * {@link NamedCache#getAll(Collection)}.
    */
    @Test
    public void putAll()
        {
        NamedCache cache   = getNamedCache();
        List       listOne = new LinkedList();
        List       listTwo = new LinkedList();
        Map        mapOne  = new HashMap();
        Map        mapTwo  = new HashMap();

        listOne.add(getKeyObject("Key1"));
        listTwo.add(getKeyObject("Key1"));
        listTwo.add(getKeyObject("Key2"));

        mapOne.put(getKeyObject("Key1"), "Value1");
        mapTwo.put(getKeyObject("Key1"), "Value1");
        mapTwo.put(getKeyObject("Key2"), "Value2");

        cache.clear();
        assertTrue(cache.isEmpty());

        Map map = cache.getAll(new LinkedList());
        assertTrue(map.isEmpty());
        cache.putAll(mapOne);
        map = cache.getAll(new LinkedList());
        assertTrue(map.isEmpty());

        cache.clear();
        assertTrue(cache.isEmpty());

        cache.putAll(new HashMap());
        assertTrue(cache.isEmpty());

        map = cache.getAll(listOne);
        assertTrue(map.isEmpty());
        cache.putAll(mapOne);
        assertEquals(1, cache.size());
        map = cache.getAll(listOne);
        assertEquals(mapOne, map);
        map = cache.getAll(listTwo);
        assertEquals(mapOne, map);

        cache.clear();
        assertTrue(cache.isEmpty());

        map = cache.getAll(listTwo);
        assertTrue(map.isEmpty());
        cache.putAll(mapTwo);
        assertEquals(2, cache.size());
        map = cache.getAll(listTwo);
        assertEquals(mapTwo, map);
        map = cache.getAll(listOne);
        assertEquals(mapOne, map);
        }

    /**
    * Test the behavior of {@link NamedCache#remove(Object)}.
    */
    @Test
    public void remove()
        {
        NamedCache cache     = getNamedCache();
        boolean    fBundling = CACHE_DIST_EXTEND_DIRECT_BUNDLING.equals(getCacheName());

        cache.clear();
        assertTrue(cache.isEmpty());

        cache.put(getKeyObject("Key"), "Value");
        assertEquals("Value", cache.get(getKeyObject("Key")));
        if (fBundling)
            {
            assertEquals(null, cache.remove(getKeyObject("Key")));
            }
        else
            {
            assertEquals("Value", cache.remove(getKeyObject("Key")));
            }
        assertEquals(null, cache.get(getKeyObject("Key")));
        assertTrue(cache.isEmpty());

        cache.put(getKeyObject("Key1"), "Value1");
        cache.put(getKeyObject("Key2"), "Value2");
        assertEquals("Value1", cache.get(getKeyObject("Key1")));
        if (fBundling)
            {
            assertEquals(null, cache.remove(getKeyObject("Key1")));
            }
        else
            {
            assertEquals("Value1", cache.remove(getKeyObject("Key1")));
            }
        assertEquals(null, cache.get(getKeyObject("Key1")));
        assertEquals(1, cache.size());
        assertEquals("Value2", cache.get(getKeyObject("Key2")));
        if (fBundling)
            {
            assertEquals(null, cache.remove(getKeyObject("Key2")));
            }
        else
            {
            assertEquals("Value2", cache.remove(getKeyObject("Key2")));
            }
        assertEquals(null, cache.get(getKeyObject("Key2")));
        assertTrue(cache.isEmpty());
        }

    /**
    * Test the behavior of {@link CacheStore#eraseAll(Collection)}.
    */
    @Test
    public void eraseAll()
        {
        if (CACHE_DIST_EXTEND_DIRECT_BUNDLING.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_NEAR_ALL.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_NEAR_PRESENT.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_NEAR_ALL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_NEAR_PRESENT.equals(getCacheName()) ||
            VIEW_EXTEND_DIRECT.equals(getCacheName()) ||
            VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache   = getNamedCache();
        CacheStore store   = (CacheStore) cache;
        List       listOne = new LinkedList();
        List       listTwo = new LinkedList();
        Map        mapOne  = new HashMap();
        Map        mapTwo  = new HashMap();

        listOne.add(getKeyObject("Key1"));
        listTwo.add(getKeyObject("Key1"));
        listTwo.add(getKeyObject("Key2"));

        mapOne.put(getKeyObject("Key2"), "Value2");
        mapTwo.put(getKeyObject("Key1"), "Value1");
        mapTwo.put(getKeyObject("Key2"), "Value2");

        cache.clear();
        assertTrue(cache.isEmpty());

        cache.put(getKeyObject("Key1"), "Value1");
        cache.put(getKeyObject("Key2"), "Value2");
        Map map = cache.getAll(listTwo);
        assertEquals(mapTwo, map);
        store.eraseAll(new LinkedList());
        map = cache.getAll(listTwo);
        assertEquals(mapTwo, map);
        cache.clear();
        assertTrue(cache.isEmpty());

        cache.put(getKeyObject("Key1"), "Value1");
        cache.put(getKeyObject("Key2"), "Value2");
        map = cache.getAll(listTwo);
        assertEquals(mapTwo, map);
        store.eraseAll(listOne);
        map = cache.getAll(listTwo);
        assertEquals(mapOne, map);
        cache.clear();
        assertTrue(cache.isEmpty());

        cache.put(getKeyObject("Key1"), "Value1");
        cache.put(getKeyObject("Key2"), "Value2");
        map = cache.getAll(listTwo);
        assertEquals(mapTwo, map);
        store.eraseAll(listTwo);
        map = cache.getAll(listTwo);
        assertTrue(map.isEmpty());
        assertTrue(cache.isEmpty());
        }

    /**
    * Test the behavior of {@link NamedCache#release()}.
    */
    @Test
    public void release()
        {
        NamedCache cache = getNamedCache();

        assertTrue(cache.isActive());
        cache.release();
        assertFalse(cache.isActive());
        }

    /**
    * Test the behavior of {@link NamedCache#release()} with a deactivation
    * listener.
    */
    @Test
    public void releaseWithListener()
        {
        if (VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }
        NamedCache cache = getNamedCache();

        TestNCDListener listener = new TestNCDListener();
        cache.addMapListener(listener);
        assertTrue(cache.isActive());
        cache.release();
        assertFalse(cache.isActive());

        MapEvent evt = listener.waitForEvent();
        assertNotNull("Event should be not null", evt);
        assertEquals("Event should be of type: " + MapEvent.ENTRY_DELETED,
                MapEvent.ENTRY_DELETED, evt.getId());
        }

    /**
    * Test the behavior of {@link NamedCache#destroy()}.
    */
    @Test
    public void destroy()
        {
        NamedCache cache = getNamedCache();

        assertTrue(cache.isActive());
        cache.destroy();
        assertFalse(cache.isActive());
        }

    /**
    * Test the behavior of {@link NamedCache#destroy()} with a deactivation
    * listener.
    */
    @Test
    public void destroyWithListener()
        {
        if (VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }
        NamedCache cache = getNamedCache();

        TestNCDListener listener = new TestNCDListener();
        cache.addMapListener(listener);
        assertTrue(cache.isActive());
        cache.destroy();
        assertFalse(cache.isActive());

        MapEvent evt = listener.waitForEvent();
        assertNotNull("Event should be not null", evt);
        assertEquals("Event should be of type: " + MapEvent.ENTRY_DELETED,
                MapEvent.ENTRY_DELETED, evt.getId());
        }

    /**
    * Test cache destroy on the server.
    */
    @Test
    public void destroyRemote()
        {
        if (CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()) ||
            VIEW_EXTEND_DIRECT.equals(getCacheName()) ||
            VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache = getNamedCache();
        assertTrue(cache.isActive());

        // destroy cache on server
        destroyRemote(getCacheName());
        assertFalse(cache.isActive());
        }

    /**
     * Test {@link NamedCache#isDestroyed()} when cluster scoped cache is
     * destroyed outside current JVM.
     */
    private void testIsDestroyedDestroyRemote()
        {
        if (CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache = getNamedCache();

        assertTrue(cache.isActive());
        assertFalse(cache.isDestroyed());
        assertFalse(cache.isReleased());

        destroyRemote(getCacheName());
        Eventually.assertThat(invoking(cache).isDestroyed(), is(true));
        }

    /**
     * Test NamedCache.isDestroyed() when cache destroyed locally and remotely.
     */
    @Test
    public void testIsDestroyed()
        {
        if (VIEW_EXTEND_DIRECT.equals(getCacheName()) ||
            VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache = getNamedCache();
        assertTrue(cache.isActive());
        assertFalse(cache.isDestroyed());
        assertFalse(cache.isReleased());

        destroyNamedCache(cache);
        assertTrue(cache.isDestroyed());

        testIsDestroyedDestroyRemote();
        }

    /**
     * Test NamedCache.isReleased().
     */
    @Test
    public void testIsReleased()
        {
        NamedCache cache = getNamedCache();
        assertTrue(cache.isActive());
        assertFalse(cache.isReleased());

        // release cache
        releaseNamedCache(cache);
        assertTrue(cache.isReleased());

        // ensure cache
        cache = getNamedCache();
        assertTrue(cache.isActive());
        assertFalse(cache.isDestroyed());
        assertFalse(cache.isReleased());
        }

    /**
    * Destroy the specified cache using an Invocable to simulate invoking
    * cache.destroy() on the server.
    *
    * @param sName  name of the cache to destroy
    */
    private void destroyRemote(String sName)
        {
        InvocationService service = (InvocationService)
                getFactory().ensureService("ExtendTcpInvocationService");

        CacheDestroyInvocable invocable = new CacheDestroyInvocable();
        invocable.setCacheName(sName);
        service.query(invocable, null);
        }

    /**
    * Test the behavior of {@link NamedCache#addMapListener(MapListener)} and
    * {@link NamedCache#removeMapListener(MapListener)}.
    */
    @Test
    public void addMapListener()
        {
        NamedCache      cache    = getNamedCache();
        TestMapListener listener = new TestMapListener();

        cache.clear();
        assertTrue(cache.isEmpty());
        cache.addMapListener(listener);

        cache.put(getKeyObject("Key"), "Value");
        MapEvent evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_INSERTED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals("Value", evt.getNewValue());

        cache.put(getKeyObject("Key"), "Value1");
        evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_UPDATED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals("Value", evt.getOldValue());
        assertEquals("Value1", evt.getNewValue());

        cache.remove(getKeyObject("Key"));
        evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_DELETED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals("Value1", evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.removeMapListener(listener);
        listener.clearEvent();

        cache.put(getKeyObject("Key"), "Value");
        evt = listener.waitForEvent();
        assertEquals(null, evt);
        }

    /**
    * Test the behavior of
    * {@link NamedCache#addMapListener(MapListener, Object, boolean)} and
    * {@link NamedCache#removeMapListener(MapListener, Object)}.
    */
    @Test
    public void addMapListenerForKey()
        {
        NamedCache      cache    = getNamedCache();
        TestMapListener listener = new TestMapListener();

        cache.clear();
        assertTrue(cache.isEmpty());
        cache.addMapListener(listener, getKeyObject("Key"), false);

        cache.put(getKeyObject("Key"), "Value");
        MapEvent evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_INSERTED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals("Value", evt.getNewValue());

        cache.put(getKeyObject("Key"), "Value1");
        evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_UPDATED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals("Value", evt.getOldValue());
        assertEquals("Value1", evt.getNewValue());

        cache.remove(getKeyObject("Key"));
        evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_DELETED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals("Value1", evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.removeMapListener(listener, getKeyObject("Key"));
        listener.clearEvent();

        cache.put(getKeyObject("Key"), "Value");
        evt = listener.waitForEvent();
        assertEquals(null, evt);
        }

    /**
    * Test the behavior of
    * {@link NamedCache#addMapListener(MapListener, Object, boolean)} and
    * {@link NamedCache#removeMapListener(MapListener, Object)}.
    */
    @Test
    public void addLightMapListenerForKey()
        {
        if (CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()))
            {
            return;
            }

        NamedCache      cache    = getNamedCache();
        TestMapListener listener = new TestMapListener();

        cache.clear();
        assertTrue(cache.isEmpty());
        cache.addMapListener(listener, getKeyObject("Key"), true);

        cache.put(getKeyObject("Key"), "Value");
        MapEvent evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_INSERTED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.put(getKeyObject("Key"), "Value1");
        evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_UPDATED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.remove(getKeyObject("Key"));
        evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_DELETED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.removeMapListener(listener, getKeyObject("Key"));
        listener.clearEvent();

        cache.put(getKeyObject("Key"), "Value");
        evt = listener.waitForEvent();
        assertEquals(null, evt);
        }

    /**
    * Test the behavior of
    * {@link NamedCache#addMapListener(MapListener, Filter, boolean)} and
    * {@link NamedCache#removeMapListener(MapListener, Filter)}.
    */
    @Test
    public void addMapListenerForFilter()
        {
        if (CACHE_DIST_EXTEND_CLIENT_KEY.equals(getCacheName()))
            {
            return;
            }

        NamedCache      cache    = getNamedCache();
        TestMapListener listener = new TestMapListener();

        Filter filter = new EqualsFilter("getKey.toString",
                getKeyObject("Key").toString());

        cache.clear();
        assertTrue(cache.isEmpty());
        cache.addMapListener(listener, filter, false);

        cache.put(getKeyObject("Key"), "Value");
        MapEvent evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_INSERTED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals("Value", evt.getNewValue());

        cache.put(getKeyObject("Key"), "Value1");
        evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_UPDATED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals("Value", evt.getOldValue());
        assertEquals("Value1", evt.getNewValue());

        cache.remove(getKeyObject("Key"));
        evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_DELETED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals("Value1", evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.removeMapListener(listener, filter);
        listener.clearEvent();

        cache.put(getKeyObject("Key"), "Value");
        evt = listener.waitForEvent();
        assertEquals(null, evt);
        }

    /**
    * Test the behavior of
    * {@link NamedCache#addMapListener(MapListener, Filter, boolean)} and
    * {@link NamedCache#removeMapListener(MapListener, Filter)}.
    */
    @Test
    public void addLightMapListenerForFilter()
        {
        if (CACHE_DIST_EXTEND_CLIENT_KEY.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_LOCAL.equals(getCacheName())      ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()))
            {
            return;
            }

        NamedCache      cache    = getNamedCache();
        TestMapListener listener = new TestMapListener();
        Filter          filter   = new EqualsFilter("getKey.toString",
                getKeyObject("Key").toString());

        cache.clear();
        assertTrue(cache.isEmpty());
        cache.addMapListener(listener, filter, true);

        cache.put(getKeyObject("Key"), "Value");
        MapEvent evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_INSERTED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.put(getKeyObject("Key"), "Value1");
        evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_UPDATED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.remove(getKeyObject("Key"));
        evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_DELETED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.removeMapListener(listener, filter);
        listener.clearEvent();

        cache.put(getKeyObject("Key"), "Value");
        evt = listener.waitForEvent();
        assertEquals(null, evt);
        }

    /**
    * Test the behavior of {@link NamedCache#addMapListener(MapListener)} and
    * {@link NamedCache#removeMapListener(MapListener)} with a
    * SynchronousListener.
    */
    @Test
    public void addSynchronousListener()
        {
        NamedCache                 cache    = getNamedCache();
        TestSynchronousMapListener listener = new TestSynchronousMapListener();

        cache.clear();
        assertTrue(cache.isEmpty());
        cache.addMapListener(listener);

        cache.put(getKeyObject("Key"), "Value");
        MapEvent evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_INSERTED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals("Value", evt.getNewValue());

        cache.put(getKeyObject("Key"), "Value1");
        evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_UPDATED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals("Value", evt.getOldValue());
        assertEquals("Value1", evt.getNewValue());

        cache.remove(getKeyObject("Key"));
        evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_DELETED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals("Value1", evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.removeMapListener(listener);
        listener.clearEvent();

        cache.put(getKeyObject("Key"), "Value");
        evt = listener.getEvent();
        assertEquals(null, evt);
        }

    /**
    * Test the behavior of
    * {@link NamedCache#addMapListener(MapListener, Object, boolean)} and
    * {@link NamedCache#removeMapListener(MapListener, Object)} with a
    * SynchronousListener.
    */
    @Test
    public void addSynchronousListenerForKey()
        {
        NamedCache                 cache    = getNamedCache();
        TestSynchronousMapListener listener = new TestSynchronousMapListener();

        cache.clear();
        assertTrue(cache.isEmpty());
        cache.addMapListener(listener, getKeyObject("Key"), false);

        cache.put(getKeyObject("Key"), "Value");
        MapEvent evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_INSERTED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals("Value", evt.getNewValue());

        cache.put(getKeyObject("Key"), "Value1");
        evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_UPDATED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals("Value", evt.getOldValue());
        assertEquals("Value1", evt.getNewValue());

        cache.remove(getKeyObject("Key"));
        evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_DELETED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals("Value1", evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.removeMapListener(listener, getKeyObject("Key"));
        listener.clearEvent();

        cache.put(getKeyObject("Key"), "Value");
        evt = listener.getEvent();
        assertEquals(null, evt);
        }

    /**
    * Test the behavior of
    * {@link NamedCache#addMapListener(MapListener, Object, boolean)} and
    * {@link NamedCache#removeMapListener(MapListener, Object)} with a
    * SynchronousListener.
    */
    @Test
    public void addLightSynchronousListenerForKey()
        {
        if (CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()))
            {
            return;
            }

        NamedCache                 cache    = getNamedCache();
        TestSynchronousMapListener listener = new TestSynchronousMapListener();

        cache.clear();
        assertTrue(cache.isEmpty());
        cache.addMapListener(listener, getKeyObject("Key"), true);

        cache.put(getKeyObject("Key"), "Value");
        MapEvent evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_INSERTED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.put(getKeyObject("Key"), "Value1");
        evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_UPDATED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.remove(getKeyObject("Key"));
        evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_DELETED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.removeMapListener(listener, getKeyObject("Key"));
        listener.clearEvent();

        cache.put(getKeyObject("Key"), "Value");
        evt = listener.getEvent();
        assertEquals(null, evt);
        }

    /**
    * Test the behavior of
    * {@link NamedCache#addMapListener(MapListener, Filter, boolean)} and
    * {@link NamedCache#removeMapListener(MapListener, Filter)} with a
    * SynchronousListener.
    */
    @Test
    public void addSynchronousListenerForFilter()
        {
        if (CACHE_DIST_EXTEND_CLIENT_KEY.equals(getCacheName()))
            {
            return;
            }

        NamedCache                 cache    = getNamedCache();
        TestSynchronousMapListener listener = new TestSynchronousMapListener();

        Filter filter = new EqualsFilter("getKey.toString",
                getKeyObject("Key").toString());

        cache.clear();
        assertTrue(cache.isEmpty());
        cache.addMapListener(listener, filter, false);

        cache.put(getKeyObject("Key"), "Value");
        MapEvent evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_INSERTED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals("Value", evt.getNewValue());

        cache.put(getKeyObject("Key"), "Value1");
        evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_UPDATED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals("Value", evt.getOldValue());
        assertEquals("Value1", evt.getNewValue());

        cache.remove(getKeyObject("Key"));
        evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_DELETED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals("Value1", evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.removeMapListener(listener, filter);
        listener.clearEvent();

        cache.put(getKeyObject("Key"), "Value");
        evt = listener.getEvent();
        assertEquals(null, evt);
        }

    /**
    * Test the behavior of
    * {@link NamedCache#addMapListener(MapListener, Filter, boolean)} and
    * {@link NamedCache#removeMapListener(MapListener, Filter)} with a
    * SynchronousListener.
    */
    @Test
    public void addLightSynchronousListenerForFilter()
        {
        if (CACHE_DIST_EXTEND_CLIENT_KEY.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_LOCAL.equals(getCacheName())      ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()))
            {
            return;
            }

        NamedCache                 cache    = getNamedCache();
        TestSynchronousMapListener listener = new TestSynchronousMapListener();

        Filter filter = new EqualsFilter("getKey.toString",
                getKeyObject("Key").toString());

        cache.clear();
        assertTrue(cache.isEmpty());
        cache.addMapListener(listener, filter, true);

        cache.put(getKeyObject("Key"), "Value");
        MapEvent evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_INSERTED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.put(getKeyObject("Key"), "Value1");
        evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_UPDATED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.remove(getKeyObject("Key"));
        evt = listener.getEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_DELETED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals(null, evt.getNewValue());

        cache.removeMapListener(listener, filter);
        listener.clearEvent();

        cache.put(getKeyObject("Key"), "Value");
        evt = listener.getEvent();
        assertEquals(null, evt);
        }

    /**
    * Test the behavior of {@link MapTriggerListener} when registered and
    * unregistered using {@link NamedCache#addMapListener(MapListener)} and
    * {@link NamedCache#removeMapListener(MapListener)}, respectively.
    */
    @Test
    public void addMapTrigger()
        {
        if (CACHE_LOCAL_EXTEND_DIRECT.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_DIRECT.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_DIRECT_JAVA.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_NEAR_ALL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_NEAR_PRESENT.equals(getCacheName()) ||
            VIEW_EXTEND_DIRECT.equals(getCacheName()) ||
            VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache   = getNamedCache();
        MapTrigger trigger = new FilterTrigger(NeverFilter.INSTANCE,
                FilterTrigger.ACTION_IGNORE);

        MapTriggerListener listener = new MapTriggerListener(trigger);

        cache.clear();
        assertTrue(cache.isEmpty());

        cache.addMapListener(listener);
        cache.put(getKeyObject("Key1"), "Value1");
        assertTrue(cache.isEmpty());

        cache.removeMapListener(listener);
        cache.put(getKeyObject("Key1"), "Value1");
        assertTrue(cache.size() == 1);
        assertEquals("Value1", cache.get(getKeyObject("Key1")));
        }

    /**
    * Test the behavior of {@link MapTriggerListener} when registered and
    * unregistered using
    * {@link NamedCache#addMapListener(MapListener, Object, boolean)} and
    * {@link NamedCache#removeMapListener(MapListener, Object)}, respectively.
    */
    @Test
    public void addMapTriggerForKey()
        {
        if (CACHE_DIST_EXTEND_CLIENT_KEY.equals(getCacheName())      ||
            CACHE_DIST_EXTEND_DIRECT.equals(getCacheName())          ||
            CACHE_DIST_EXTEND_DIRECT_BUNDLING.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_DIRECT_JAVA.equals(getCacheName())     ||
            CACHE_DIST_EXTEND_NEAR_ALL.equals(getCacheName())        ||
            CACHE_DIST_EXTEND_NEAR_PRESENT.equals(getCacheName())    ||
            CACHE_NEAR_EXTEND_DIRECT.equals(getCacheName())          ||
            VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache   = getNamedCache();
        MapTrigger trigger = new FilterTrigger(NeverFilter.INSTANCE,
                FilterTrigger.ACTION_IGNORE);

        MapTriggerListener listener = new MapTriggerListener(trigger);
        cache.addMapListener(listener, getKeyObject("Key1"), true);
        }

    /**
    * Test the behavior of {@link MapTriggerListener} when registered and
    * unregistered using
    * {@link NamedCache#addMapListener(MapListener, Filter, boolean)} and
    * {@link NamedCache#removeMapListener(MapListener, Filter)}, respectively.
    */
    @Test
    public void addMapTriggerForFilter()
        {
        if (CACHE_LOCAL_EXTEND_DIRECT.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_CLIENT_KEY.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_DIRECT.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_DIRECT_BUNDLING.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_DIRECT_JAVA.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_NEAR_ALL.equals(getCacheName()) ||
            CACHE_DIST_EXTEND_NEAR_PRESENT.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_DIRECT.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_DIRECT_JAVA.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_NEAR_ALL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_NEAR_PRESENT.equals(getCacheName()) ||
            CACHE_NEAR_EXTEND_DIRECT.equals(getCacheName()) ||
            VIEW_EXTEND_DIRECT.equals(getCacheName()) ||
            VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache   = getNamedCache();
        MapTrigger trigger = new FilterTrigger(NeverFilter.INSTANCE,
                FilterTrigger.ACTION_IGNORE);

        MapTriggerListener listener = new MapTriggerListener(trigger);

        cache.clear();
        assertTrue(cache.isEmpty());

        cache.addMapListener(listener, null, true);
        cache.put(getKeyObject("Key1"), "Value1");
        assertTrue(cache.isEmpty());

        cache.removeMapListener(listener);
        cache.put(getKeyObject("Key1"), "Value1");
        assertTrue(cache.size() == 1);
        assertEquals("Value1", cache.get(getKeyObject("Key1")));

        cache.addMapListener(listener, AlwaysFilter.INSTANCE, true);
        }

    /**
    * Test the behavior of
    * {@link NamedCache#addIndex(ValueExtractor, boolean, Comparator)} and
    * {@link NamedCache#removeIndex(ValueExtractor)}.
    */
    @Test
    public void addIndex()
        {
        NamedCache cache = getNamedCache();

        ValueExtractor extractor = new ReflectionExtractor("toString");

        cache.addIndex(extractor, true, new CaseInsensitiveComparator());

        cache.put(getKeyObject("Key1"), "Value1");
        cache.put(getKeyObject("Key2"), "Value2");

        cache.removeIndex(extractor);

        cache.clear();
        cache.put(getKeyObject("Key1"), 1);
        cache.put(getKeyObject("Key2"), 2);
        }

    /**
    * Test the behavior of
    * {@link NamedCache#aggregate(Collection, EntryAggregator)}.
    */
    @Test
    public void aggregate()
        {
        if (CACHE_DIST_EXTEND_CLIENT_KEY.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache = getNamedCache();
        Count      agent = new Count();

        cache.clear();

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(getKeyObject(String.valueOf(i)), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertEquals("Result=" + oResult, 0, oResult);

        oResult = cache.aggregate(Collections.singletonList(getKeyObject("1")), agent);
        assertEquals("Result=" + oResult, 1, oResult);

        Collection col = new HashSet();
        col.add(getKeyObject("3"));
        col.add(getKeyObject("4"));

        oResult = cache.aggregate(col, agent);
        assertEquals("Result=" + oResult, 2, oResult);
        }

    /**
    * Test the behavior of
    * {@link NamedCache#aggregate(Filter, EntryAggregator)}.
    */
    @Test
    public void aggregateFilter()
        {
        if (CACHE_DIST_EXTEND_CLIENT_KEY.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache = getNamedCache();
        Count      agent = new Count();

        cache.clear();

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(getKeyObject(String.valueOf(i)), i);
            }

        Object oResult = cache.aggregate(NeverFilter.INSTANCE, agent);
        assertEquals("Result=" + oResult, 0, oResult);

        oResult = cache.aggregate((Filter) null, agent);
        assertEquals("Result=" + oResult, cache.size(), oResult);

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertEquals("Result=" + oResult, cache.size(), oResult);
        }

    /**
    * Test of the {@link DistinctValues} aggregator.
    */
    @Test
    public void distinctValues()
        {
        NamedCache     cache = getNamedCache();
        DistinctValues agent = new DistinctValues(IdentityExtractor.INSTANCE);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Collection setResult = (Collection) cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue(setResult.isEmpty());

        Set setExpected = new HashSet();
        setExpected.add(1);

        setResult = (Collection) cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + setResult, equals(new HashSet(setResult), setExpected));

        setExpected.clear();
        for (int i = 1; i <= 10; ++i)
            {
            setExpected.add(i);
            }

        setResult = (Collection) cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + setResult, equals(new HashSet(setResult), setExpected));

        setResult = (Collection) cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + setResult, equals(new HashSet(setResult), setExpected));

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), 0);
            }

        setExpected.clear();
        setExpected.add(0);

        setResult = (Collection) cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + setResult, equals(new HashSet(setResult), setExpected));

        setResult = (Collection) cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + setResult, equals(new HashSet(setResult), setExpected));

        setResult = (Collection) cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + setResult, equals(new HashSet(setResult), setExpected));
        }

    /**
    * Test of the {@link DoubleAverage} aggregator.
    */
    @Test
    public void doubleAverage()
        {
        NamedCache    cache = getNamedCache();
        DoubleAverage agent = new DoubleAverage(IdentityExtractor.INSTANCE);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1.0D));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 5.5D));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 5.5D));
        }

    /**
    * Test of the {@link DoubleMax} aggregator.
    */
    @Test
    public void doubleMax()
        {
        NamedCache cache = getNamedCache();
        DoubleMax  agent = new DoubleMax(IdentityExtractor.INSTANCE);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1.0D));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 10.0D));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 10.0D));
        }

    /**
    * Test of the {@link DoubleMin} aggregator.
    */
    @Test
    public void doubleMin()
        {
        NamedCache cache = getNamedCache();
        DoubleMin  agent = new DoubleMin(IdentityExtractor.INSTANCE);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1.0D));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 1.0D));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 1.0D));
        }

    /**
    * Test of the {@link DoubleSum} aggregator.
    */
    @Test
    public void doubleSum()
        {
        NamedCache cache = getNamedCache();
        DoubleSum  agent = new DoubleSum(IdentityExtractor.INSTANCE);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1.0D));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 55.0D));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 55.0D));
        }

    /**
    * Test of the {@link LongMax} aggregator.
    */
    @Test
    public void longMax()
        {
        NamedCache cache = getNamedCache();
        LongMax    agent = new LongMax(IdentityExtractor.INSTANCE);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1L));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 10L));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 10L));
        }

    /**
    * Test of the {@link LongMin} aggregator.
    */
    @Test
    public void longMin()
        {
        NamedCache cache = getNamedCache();
        LongMin    agent = new LongMin(IdentityExtractor.INSTANCE);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1L));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 1L));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 1L));
        }

    /**
    * Test of the {@link LongSum} aggregator.
    */
    @Test
    public void longSum()
        {
        NamedCache cache = getNamedCache();
        LongSum    agent = new LongSum(IdentityExtractor.INSTANCE);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1L));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 55L));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 55L));
        }

    /**
    * Test of the {@link BigDecimalAverage} aggregator.
    */
    @Test
    public void bigDecimalAverage()
        {
        NamedCache cache = getNamedCache();
        BigDecimalAverage agent = new BigDecimalAverage(IdentityExtractor.INSTANCE);

        fillBigNumbers(cache, 1, 10);

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(1.0D)));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(5.5D)));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(5.5D)));
        }

    /**
    * Test of the {@link BigDecimalMax} aggregator.
    */
    @Test
    public void bigDecimalMax()
        {
        NamedCache cache = getNamedCache();
        BigDecimalMax agent = new BigDecimalMax(IdentityExtractor.INSTANCE);

        fillBigNumbers(cache, 1, 10);

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(1.0D)));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(10.0D)));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(10.0D)));
        }

    /**
    * Test of the {@link BigDecimalMin} aggregator.
    */
    @Test
    public void bigDecimalMin()
        {
        NamedCache cache = getNamedCache();
        BigDecimalMin agent = new BigDecimalMin(IdentityExtractor.INSTANCE);

        fillBigNumbers(cache, 1, 10);

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(1.0D)));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(1.0D)));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(1.0D)));
        }

    /**
    * Test of the {@link BigDecimalSum} aggregator.
    */
    @Test
    public void bigDecimalSum()
        {
        NamedCache cache = getNamedCache();
        BigDecimalSum agent = new BigDecimalSum(IdentityExtractor.INSTANCE);

        fillBigNumbers(cache, 1, 10);

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(1.0D)));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(55.0D)));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(55.0D)));
        }

    /**
    * Test of {@link ComparableMax} and {@link ComparableMin}
    */
    @Test
    public void comparableAggregator()
        {
        NamedCache cache = getNamedCache();

        // create the aggregators and test with an empty cache

        ComparableMax comparableMax = new ComparableMax("getFirstName");
        ComparableMin comparableMin = new ComparableMin("getFirstName");

        Person.fillRandom(cache, 1000);

        // determine the expected aggregation results

        String[] asFirst = Person.FIRST_NAMES.clone();
        Arrays.sort(asFirst);

        String sExpectedMax = asFirst[asFirst.length - 1];
        String sExpectedMin = asFirst[0];

        // test using Comparable extracted values

        String sMaxFirst = (String) cache.aggregate((Filter)null,
                comparableMax);
        assertTrue("Expected: " + sExpectedMax + ", actual: " + sMaxFirst,
                sExpectedMax.equals(sMaxFirst));

        String sMinFirst = (String) cache.aggregate((Filter)null,
                comparableMin);
        assertTrue("Expected: " + sExpectedMin + ", actual: " + sMinFirst,
                sExpectedMin.equals(sMinFirst));
        }

    /**
    * Test of the {@link CompositeAggregator} aggregator.
    */
    @Test
    public void compositeParallel()
        {
        System.err.println(">>>>>>> In compositeParallel() - Start");

        NamedCache          cache     = getNamedCache();
        ValueExtractor      extrFirst = new ReflectionExtractor("getFirstName");

        CompositeAggregator agent     = CompositeAggregator.createInstance(
            new InvocableMap.EntryAggregator[]
                {
                new ComparableMin("getFirstName"),
                new ComparableMax("getLastName"),
                });

        System.err.println(">>>>>>> In compositeParallel() - Populating cache with 1000 entries " + cache.getCacheName());
        Person.fillRandom(cache, 1000);
        System.err.println(">>>>>>> In compositeParallel() - Populated cache " + cache.getCacheName());

        String[] asFirst = Person.FIRST_NAMES.clone();
        String[] asLast  = Person.LAST_NAMES.clone();
        Arrays.sort(asFirst);
        Arrays.sort(asLast);

        System.err.println(">>>>>>> In compositeParallel() - 1. Running aggregator " + agent);
        List listResult = (List) cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Not null", listResult.get(0) == null && listResult.get(1) == null);

        Object oId       = cache.keySet().iterator().next();
        Person person    = (Person) cache.get(oId);

        System.err.println(">>>>>>> In compositeParallel() - 2. Running aggregator " + agent);
        listResult = (List) cache.aggregate(Collections.singletonList(oId), agent);
        assertTrue("Result=" + listResult.get(0), equals(listResult.get(0), person.getFirstName()));
        assertTrue("Result=" + listResult.get(1), equals(listResult.get(1), person.getLastName()) );

        System.err.println(">>>>>>> In compositeParallel() - 3. Running aggregator " + agent);
        listResult = (List) cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + listResult.get(0), equals(listResult.get(0), asFirst[0]));
        assertTrue("Result=" + listResult.get(1), equals(listResult.get(1), asLast[asLast.length - 1]) );

        System.err.println(">>>>>>> In compositeParallel() - 1. Adding Index");
        cache.addIndex(extrFirst, true, null);

        System.err.println(">>>>>>> In compositeParallel() - 4. Running aggregator " + agent);
        listResult = (List) cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + listResult.get(0), equals(listResult.get(0), asFirst[0]));
        assertTrue("Result=" + listResult.get(1), equals(listResult.get(1), asLast[asLast.length - 1]) );

        System.err.println(">>>>>>> In compositeParallel() - 1. Removing Index");
        cache.removeIndex(extrFirst);

        agent = CompositeAggregator.createInstance(
            new InvocableMap.EntryAggregator[]
                {
                new DistinctValues("getFirstName"),
                new DistinctValues("getLastName"),
                });

        System.err.println(">>>>>>> In compositeParallel() - Clearing cache");
        cache.clear();

        // the composite aggregator's result structures are more complex
        // that covered by the testEmpty() helper
        System.err.println(">>>>>>> In compositeParallel() - 5. Running aggregator " + agent);
        listResult = (List) cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Not empty", ((Collection) listResult.get(0)).isEmpty() && ((Collection) listResult.get(1)).isEmpty());

        System.err.println(">>>>>>> In compositeParallel() - 6. Running aggregator " + agent);
        listResult = (List) cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Not empty", ((Collection) listResult.get(0)).isEmpty() && ((Collection) listResult.get(1)).isEmpty());

        System.err.println(">>>>>>> In compositeParallel() - 7. Running aggregator " + agent);
        listResult = (List) cache.aggregate((Filter) null, agent);
        assertTrue("Not empty", ((Collection) listResult.get(0)).isEmpty() && ((Collection) listResult.get(1)).isEmpty());

        System.err.println(">>>>>>> In compositeParallel() - 8. Running aggregator " + agent);
        listResult = (List) cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Not empty", ((Collection) listResult.get(0)).isEmpty() && ((Collection) listResult.get(1)).isEmpty());

        System.err.println(">>>>>>> In compositeParallel() - Populating cache second time with 1000 entries " + cache.getCacheName());
        Person.fillRandom(cache, 1000);
        System.err.println(">>>>>>> In compositeParallel() - Populated cache second time " + cache.getCacheName());

        System.err.println(">>>>>>> In compositeParallel() - 9. Running aggregator " + agent);
        listResult = (List) cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Not empty", ((Collection) listResult.get(0)).isEmpty() && ((Collection) listResult.get(1)).isEmpty());

        oId    = cache.keySet().iterator().next();
        person = (Person) cache.get(oId);
        Set setFirst = Collections.singleton(person.getFirstName());
        Set setLast  = Collections.singleton(person.getLastName());

        System.err.println(">>>>>>> In compositeParallel() - 10. Running aggregator " + agent);
        listResult = (List) cache.aggregate(Collections.singletonList(oId), agent);
        assertTrue("Result=" + listResult.get(0), equals(new HashSet((Collection) listResult.get(0)), setFirst));
        assertTrue("Result=" + listResult.get(1), equals(new HashSet((Collection) listResult.get(1)), setLast));

        setFirst = new HashSet();
        setLast  = new HashSet();

        for (int i = 0, c = Person.FIRST_NAMES.length; i < c; ++i)
            {
            setFirst.add(Person.FIRST_NAMES[i]);
            }
        for (int i = 0, c = Person.LAST_NAMES.length; i < c; ++i)
            {
            setLast.add(Person.LAST_NAMES[i]);
            }

        System.err.println(">>>>>>> In compositeParallel() - 11. Running aggregator " + agent);
        listResult = (List) cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + listResult.get(0), equals(new HashSet((Collection) listResult.get(0)), setFirst));
        assertTrue("Result=" + listResult.get(1), equals(new HashSet((Collection) listResult.get(1)), setLast));

        System.err.println(">>>>>>> In compositeParallel() - 2. Adding Index");
        cache.addIndex(extrFirst, true, null);

        System.err.println(">>>>>>> In compositeParallel() - 10. Running aggregator " + agent);
        listResult = (List) cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + listResult.get(0), equals(new HashSet((Collection) listResult.get(0)), setFirst));
        assertTrue("Result=" + listResult.get(1), equals(new HashSet((Collection) listResult.get(1)), setLast));

        System.err.println(">>>>>>> In compositeParallel() - End");
        }

    /**
    * Test of the {@link ReducerAggregator} aggregator.
    */
    @Test
    public void reducerAggregator()
        {
        NamedCache        cache = getNamedCache();
        ReducerAggregator agent = new ReducerAggregator(new MultiExtractor("getId,getFirstName,getLastName"));
        Map               m     = (Map) cache.aggregate(AlwaysFilter.INSTANCE, agent);

        assertEquals(m.size(), cache.size());
        cache.clear();

        Person p = new Person("666-22-9999");
        p.setFirstName("David");
        p.setLastName("Person1");
        cache.put("P1", p);

        p = new Person("666-22-1111");
        p.setFirstName("George");
        p.setLastName("Person2");
        cache.put("P2", p);

        m = (Map) cache.aggregate(AlwaysFilter.INSTANCE,agent);

        assertEquals(m.size(), cache.size());

        List results = (List) m.get("P1");
        assertEquals(3, results.size());
        assertEquals("666-22-9999", results.get(0));
        assertEquals("David", results.get(1));
        assertEquals("Person1", results.get(2));

        results = (List) m.get("P2");
        assertEquals(3, results.size());
        assertEquals("666-22-1111", results.get(0));
        assertEquals("George", results.get(1));
        assertEquals("Person2", results.get(2));
        }

    /**
    * Test of the {@link TopNAggregator}.
    */
    @Test
    public void topNAggregator()
        {
        NamedCache     cache = getNamedCache();
        TopNAggregator agent = new TopNAggregator(IdentityExtractor.INSTANCE, SafeComparator.INSTANCE, 10);

        Map map   = new HashMap();
        int cKeys = 10000;
        for (int i = 1; i <= cKeys; ++i)
            {
            map.put(String.valueOf(i), i);
            }
        cache.putAll(map);

        Object[] aoTop10 = new Object[10];
        for (int i = 0; i < 10; i++)
            {
            aoTop10[i] = cKeys - i;
            }

        Object[] oResult = (Object[]) cache.aggregate(NullImplementation.getSet(), agent);
        assertArrayEquals("Result=" + Arrays.toString(oResult), new Object[0], oResult);

        oResult = (Object[]) cache.aggregate(Collections.singletonList("1"), agent);
        assertArrayEquals("Result=" + Arrays.toString(oResult), new Object[] {1}, oResult);

        oResult = (Object[]) cache.aggregate((Filter) null, agent);
        assertArrayEquals("Result=" + Arrays.toString(oResult), aoTop10, oResult);

        oResult = (Object[]) cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertArrayEquals("Result=" + Arrays.toString(oResult), aoTop10, oResult);
        }


    /**
    * Test the behavior of {@link NamedCache#entrySet()}.
    */
    @Test
    public void entrySet()
        {
        if (CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()))
            {
            return;
            }

        NamedCache  cache      = getNamedCache();
        Map         mapOne     = new HashMap();
        Map         mapTwo     = new HashMap();
        Map.Entry   entryOne   = new SimpleMapEntry(getKeyObject("Key1"), "Value1");
        Map.Entry   entryTwo   = new SimpleMapEntry(getKeyObject("Key2"), "Value2");
        Map.Entry   entryThree = new SimpleMapEntry(getKeyObject("Key3"), "Value3");
        Set         setOne     = new HashSet();
        Set         setTwo     = new HashSet();

        mapOne.put(getKeyObject("Key1"), "Value1");
        mapOne.put(getKeyObject("Key2"), "Value2");

        setOne.add(entryOne);
        setOne.add(entryTwo);

        setTwo.add(entryOne);
        setTwo.add(entryTwo);
        setTwo.add(entryThree);

        cache.clear();

        Set setEntries = cache.entrySet();
        assertTrue(setEntries.isEmpty());

        cache.put(getKeyObject("Key1"), "Value1");
        cache.put(getKeyObject("Key2"), "Value2");

        assertTrue(setEntries.size() == cache.size());
        assertTrue(setEntries.contains(entryOne));
        assertFalse(setEntries.contains(entryThree));
        assertTrue(setEntries.containsAll(setOne));
        assertFalse(setEntries.containsAll(setTwo));

        for (Iterator iter = setEntries.iterator(); iter.hasNext(); )
            {
            Map.Entry entry = (Map.Entry) iter.next();
            mapTwo.put(entry.getKey(), entry.getValue());
            }

        assertEquals(mapTwo, mapOne);

        Iterator iter = setEntries.iterator();
        iter.next();
        iter.remove();

        assertTrue(cache.size() == 1);

        cache.put(getKeyObject("Key1"), "Value1");
        cache.put(getKeyObject("Key2"), "Value2");
        cache.put(getKeyObject("Key3"), "Value3");

        assertTrue(setEntries.size() == cache.size());
        assertTrue(setEntries.contains(entryThree));
        assertTrue(setEntries.containsAll(setTwo));

        cache.remove(getKeyObject("Key1"));

        assertTrue(setEntries.size() == cache.size());
        assertFalse(setEntries.contains(entryOne));

        cache.put(getKeyObject("Key1"), "Value1");

        assertTrue(setEntries.retainAll(setOne));
        assertTrue(setEntries.size() == cache.size());
        assertFalse(setEntries.contains(entryThree));
        assertFalse(setEntries.retainAll(setOne));

        try
            {
            setEntries.add(entryThree);
            fail();
            }
        catch (UnsupportedOperationException e)
            {
            }

        try
            {
            setEntries.addAll(setOne);
            fail();
            }
        catch (UnsupportedOperationException e)
            {
            }

        cache.put(getKeyObject("Key3"), "Value3");
        assertTrue(setEntries.remove(entryOne));
        assertFalse(setEntries.remove(entryOne));

        assertTrue(setEntries.size() == cache.size());
        assertFalse(setEntries.contains(entryOne));

        assertTrue(setEntries.removeAll(setTwo));
        assertTrue(setEntries.isEmpty());
        assertFalse(setEntries.removeAll(setTwo));
        assertTrue(cache.isEmpty());

        cache.put(getKeyObject("Key1"), "Value1");
        Object[] ao = setEntries.toArray();

        assertEquals(1, ao.length);
        assertEquals(entryOne, ao[0]);

        setEntries.clear();

        assertTrue(setEntries.isEmpty());
        assertTrue(cache.isEmpty());
        }

    /**
    * Test the behavior of {@link NamedCache#entrySet(Filter)}.
    */
    @Test
    public void entrySetFilter()
        {
        NamedCache cache        = getNamedCache();
        Filter     filterAlways = new AlwaysFilter();
        Filter     filterNever  = new NeverFilter();
        Map.Entry  entryOne     = new SimpleMapEntry(getKeyObject("Key1"), "Value1");
        Map.Entry  entryTwo     = new SimpleMapEntry(getKeyObject("Key2"), "Value2");

        cache.clear();

        Set setEntries = cache.entrySet(filterAlways);
        assertTrue(setEntries.isEmpty());

        setEntries = cache.entrySet(filterNever);
        assertTrue(setEntries.isEmpty());

        cache.put(getKeyObject("Key1"), "Value1");
        cache.put(getKeyObject("Key2"), "Value2");

        setEntries = cache.entrySet(filterAlways);
        assertTrue(setEntries.size() == 2);
        assertTrue(setEntries.contains(entryOne));
        assertTrue(setEntries.contains(entryTwo));

        setEntries = cache.entrySet(filterNever);
        assertTrue(setEntries.isEmpty());
        }

    /**
    * Invoke {@link NamedCache#entrySet(Filter, Comparator)}.
    */
    @Test
    public void entrySetFilterComparator()
        {
        NamedCache cache        = getNamedCache();
        Comparator comparator   = new InverseComparator(new CaseInsensitiveComparator());
        Filter     filterAlways = new AlwaysFilter();
        Filter     filterNever  = new NeverFilter();
        Map.Entry  entryOne     = new SimpleMapEntry(getKeyObject("Key1"), "Value1");
        Map.Entry  entryTwo     = new SimpleMapEntry(getKeyObject("Key2"), "Value2");

        cache.clear();

        Set setEntries = cache.entrySet(filterAlways, comparator);
        assertTrue(setEntries.isEmpty());

        setEntries = cache.entrySet(filterNever, comparator);
        assertTrue(setEntries.isEmpty());

        cache.put(getKeyObject("Key1"), "Value1");
        cache.put(getKeyObject("Key2"), "Value2");

        setEntries = cache.entrySet(filterAlways, null);
        assertTrue(setEntries.size() == 2);
        assertTrue(setEntries.contains(entryOne));
        assertTrue(setEntries.contains(entryTwo));

        Iterator iter = setEntries.iterator();
        assertEquals(entryOne, iter.next());
        assertEquals(entryTwo, iter.next());

        setEntries = cache.entrySet(filterAlways, comparator);
        assertTrue(setEntries.size() == 2);
        assertTrue(setEntries.contains(entryOne));
        assertTrue(setEntries.contains(entryTwo));

        iter = setEntries.iterator();
        assertEquals(entryTwo, iter.next());
        assertEquals(entryOne, iter.next());

        setEntries = cache.entrySet(filterNever, comparator);
        assertTrue(setEntries.isEmpty());
        }

    /**
    * Invoke {@link NamedCache#entrySet(Filter, Comparator)} with a
    * LimitFilter.
    */
    @Test
    public void entrySetLimitFilterComparator()
        {
        NamedCache cache     = getNamedCache();
        HashMap    map       = new HashMap();
        final int  PAGE_SIZE = 10;

        cache.clear();

        for (int i = 0; i < 1000; i++)
            {
            map.put(i, i);
            }

        cache.putAll(map);

        LimitFilter         filterLimit = new LimitFilter(new AlwaysFilter(), PAGE_SIZE);
        Comparator<Integer> comparator  = new IntegerComparator();

        Set<Map.Entry> set = cache.entrySet(filterLimit, comparator);
        assertNotNull("Result set is null", set);
        assertEquals(PAGE_SIZE, set.size());

        int i = 1000;
        for (Map.Entry entry : set)
            {
            i--;
            assertEquals("Expected value=" + i + ", Returned value="
                         + entry.getValue(), i, entry.getValue());
            }
        }

    /**
    * Test the behavior of
    * {@link NamedCache#invoke(Object, EntryProcessor)}.
    */
    @Test
    public void invoke()
        {
        if (CACHE_DIST_EXTEND_CLIENT_KEY.equals(getCacheName()))
            {
            return;
            }

        NamedCache        cache = getNamedCache();
        NumberIncrementor agent = new NumberIncrementor("Lot",
                                                        1, false);

        cache.clear();

        for (int i = 1; i <= 5; ++i)
            {
            cache.put(getKeyObject(String.valueOf(i)),
                    new Trade(i, 0.0, String.valueOf(i), i));
            }

        Object oResult = cache.invoke(getKeyObject("0"), agent);
        assertEquals("Result=" + oResult, null, oResult);

        oResult = cache.invoke(getKeyObject("1"), agent);
        assertEquals("Result=" + oResult, 2, oResult);
        }

    /**
    * Test the behavior of
    * {@link NamedCache#invokeAll(Collection, EntryProcessor)}.
    */
    @Test
    public void invokeAll()
        {
        if (CACHE_DIST_EXTEND_CLIENT_KEY.equals(getCacheName()))
            {
            return;
            }

        NamedCache        cache = getNamedCache();
        NumberIncrementor agent = new NumberIncrementor("Lot",
                                                        1, false);

        cache.clear();

        for (int i = 1; i <= 5; ++i)
            {
            cache.put(getKeyObject(String.valueOf(i)),
                    new Trade(i, 0.0, String.valueOf(i), i));
            }

        Map mapResult = cache.invokeAll(NullImplementation.getSet(), agent);
        assertEquals("Result=" + mapResult, NullImplementation.getMap(), mapResult);

        mapResult = cache.invokeAll(Collections.singleton(getKeyObject("1")), agent);
        assertEquals("Result=" + mapResult, 2, mapResult.get(getKeyObject("1")));

        Collection col = new HashSet();
        col.add(getKeyObject("3"));
        col.add(getKeyObject("4"));

        mapResult = cache.invokeAll(col, agent);
        assertEquals("Result=" + mapResult, 2, mapResult.size());
        assertEquals("Result=" + mapResult, 4, mapResult.get(getKeyObject("3")));
        assertEquals("Result=" + mapResult, 5, mapResult.get(getKeyObject("4")));
        }

    /**
    * Test the behavior of
    * {@link NamedCache#invokeAll(Filter, EntryProcessor)}.
    */
    @Test
    public void invokeAllFilter()
        {
        if (CACHE_DIST_EXTEND_CLIENT_KEY.equals(getCacheName()))
            {
            return;
            }

        NamedCache        cache = getNamedCache();
        NumberIncrementor agent = new NumberIncrementor("Lot",
                                                        1, false);

        cache.clear();

        for (int i = 1; i <= 5; ++i)
            {
            cache.put(getKeyObject(String.valueOf(i)),
                    new Trade(i, 0.0, String.valueOf(i), i));
            }

        Map mapResult = cache.invokeAll(NeverFilter.INSTANCE, agent);
        assertEquals("Result=" + mapResult, NullImplementation.getMap(), mapResult);

        mapResult = cache.invokeAll((Filter) null, agent);
        assertEquals("Result=" + mapResult, 5, mapResult.size());
        assertEquals("Result=" + mapResult, 2, mapResult.get(getKeyObject("1")));
        assertEquals("Result=" + mapResult, 3, mapResult.get(getKeyObject("2")));
        assertEquals("Result=" + mapResult, 4, mapResult.get(getKeyObject("3")));
        assertEquals("Result=" + mapResult, 5, mapResult.get(getKeyObject("4")));
        assertEquals("Result=" + mapResult, 6, mapResult.get(getKeyObject("5")));

        mapResult = cache.invokeAll((Filter) null, agent);
        assertEquals("Result=" + mapResult, 5, mapResult.size());
        assertEquals("Result=" + mapResult, 3, mapResult.get(getKeyObject("1")));
        assertEquals("Result=" + mapResult, 4, mapResult.get(getKeyObject("2")));
        assertEquals("Result=" + mapResult, 5, mapResult.get(getKeyObject("3")));
        assertEquals("Result=" + mapResult, 6, mapResult.get(getKeyObject("4")));
        assertEquals("Result=" + mapResult, 7, mapResult.get(getKeyObject("5")));
        }

    /**
    * Test the behavior of {@link NamedCache#keySet()}.
    */
    @Test
    public void keySet()
        {
        NamedCache  cache       = getNamedCache();
        Object      oKeyOne     = getKeyObject("Key1");
        Object      oKeyTwo     = getKeyObject("Key2");
        Object      oKeyThree   = getKeyObject("Key3");
        Object      oValueOne   = "Value1";
        Object      oValueTwo   = "Value2";
        Object      oValueThree = "Value3";
        Set         setOne      = new HashSet();
        Set         setTwo      = new HashSet();
        Set         setThree    = new HashSet();

        setOne.add(oKeyOne);
        setOne.add(oKeyTwo);

        setTwo.add(oKeyOne);
        setTwo.add(oKeyTwo);
        setTwo.add(oKeyThree);

        cache.clear();

        Set setKeys = cache.keySet();
        assertTrue(setKeys.isEmpty());

        cache.put(oKeyOne, oValueOne);
        cache.put(oKeyTwo, oValueTwo);

        assertTrue(setKeys.size() == cache.size());
        assertTrue(setKeys.contains(oKeyOne));
        assertTrue(setKeys.contains(oKeyTwo));
        assertTrue(setKeys.containsAll(setOne));
        assertFalse(setKeys.containsAll(setTwo));

        for (Iterator iter = setKeys.iterator(); iter.hasNext(); )
            {
            setThree.add(iter.next());
            }

        assertEquals(setThree, setOne);

        Iterator iter = setKeys.iterator();
        iter.next();
        iter.remove();

        assertTrue(cache.size() == 1);

        cache.put(oKeyOne, oValueOne);
        cache.put(oKeyTwo, oValueTwo);
        cache.put(oKeyThree, oValueThree);

        assertTrue(setKeys.size() == cache.size());
        assertTrue(setKeys.contains(oKeyThree));
        assertTrue(setKeys.containsAll(setTwo));

        cache.remove(oKeyOne);

        assertTrue(setKeys.size() == cache.size());
        assertFalse(setKeys.contains(oKeyOne));

        cache.put(oKeyOne, oValueOne);

        assertTrue(setKeys.retainAll(setOne));
        assertTrue(setKeys.size() == cache.size());
        assertFalse(setKeys.contains(oKeyThree));
        assertFalse(setKeys.retainAll(setOne));

        try
            {
            setKeys.add(oKeyThree);
            fail();
            }
        catch (UnsupportedOperationException e)
            {
            }

        try
            {
            setKeys.addAll(setOne);
            fail();
            }
        catch (UnsupportedOperationException e)
            {
            }

        cache.put(oKeyThree, oValueThree);
        assertTrue(setKeys.remove(oKeyOne));
        assertFalse(setKeys.remove(oKeyOne));

        assertTrue(setKeys.size() == cache.size());
        assertFalse(setKeys.contains(oKeyOne));

        assertTrue(setKeys.removeAll(setTwo));
        assertTrue(setKeys.isEmpty());
        assertFalse(setKeys.removeAll(setTwo));

        cache.put(oKeyOne, oValueOne);
        Object[] ao = setKeys.toArray();

        assertTrue(ao.length == 1);
        assertEquals(oKeyOne, ao[0]);

        setKeys.clear();

        assertTrue(setKeys.isEmpty());
        assertTrue(cache.isEmpty());
        }

    /**
    * Test the behavior {@link NamedCache#keySet(Filter)}.
    */
    @Test
    public void keySetFilter()
        {
        NamedCache cache        = getNamedCache();
        Filter     filterAlways = new AlwaysFilter();
        Filter     filterNever  = new NeverFilter();
        Object     oKeyOne      = getKeyObject("Key1");
        Object     oKeyTwo      = getKeyObject("Key2");
        Object     oValueOne    = "Value1";
        Object     oValueTwo    = "Value2";

        cache.clear();

        Set setKeys = cache.keySet(filterAlways);
        assertTrue(setKeys.isEmpty());

        setKeys = cache.keySet(filterNever);
        assertTrue(setKeys.isEmpty());

        cache.put(oKeyOne, oValueOne);
        cache.put(oKeyTwo, oValueTwo);

        setKeys = cache.keySet(filterAlways);
        assertTrue(setKeys.size() == 2);
        assertTrue(setKeys.contains(oKeyOne));
        assertTrue(setKeys.contains(oKeyTwo));

        setKeys = cache.keySet(filterNever);
        assertTrue(setKeys.isEmpty());
        }

    /**
    * Test the behavior of {@link NamedCache#values()}.
    */
    @Test
    public void values()
        {
        NamedCache  cache       = getNamedCache();
        Object      oKeyOne     = getKeyObject("Key1");
        Object      oKeyTwo     = getKeyObject("Key2");
        Object      oKeyThree   = getKeyObject("Key3");
        Object      oValueOne   = "Value1";
        Object      oValueTwo   = "Value2";
        Object      oValueThree = "Value3";
        Set         setOne      = new HashSet();
        Set         setTwo      = new HashSet();
        Set         setThree    = new HashSet();

        setOne.add(oValueOne);
        setOne.add(oValueTwo);

        setTwo.add(oValueOne);
        setTwo.add(oValueTwo);
        setTwo.add(oValueThree);

        cache.clear();

        Collection colValues = cache.values();
        assertTrue(colValues.isEmpty());

        cache.put(oKeyOne, oValueOne);
        cache.put(oKeyTwo, oValueTwo);

        assertTrue(colValues.size() == cache.size());
        assertTrue(colValues.contains(oValueOne));
        assertTrue(colValues.contains(oValueTwo));
        assertTrue(colValues.containsAll(setOne));
        assertFalse(colValues.containsAll(setTwo));

        for (Iterator iter = colValues.iterator(); iter.hasNext(); )
            {
            setThree.add(iter.next());
            }

        assertEquals(setThree, setOne);

        Iterator iter = colValues.iterator();
        iter.next();
        iter.remove();

        assertTrue(cache.size() == 1);

        cache.put(oKeyOne, oValueOne);
        cache.put(oKeyTwo, oValueTwo);
        cache.put(oKeyThree, oValueThree);

        assertTrue(colValues.size() == cache.size());
        assertTrue(colValues.contains(oValueThree));
        assertTrue(colValues.containsAll(setTwo));

        cache.remove(oKeyOne);

        assertTrue(colValues.size() == cache.size());
        assertFalse(colValues.contains(oValueOne));

        cache.put(oKeyOne, oValueOne);

        assertTrue(colValues.retainAll(setOne));
        assertTrue(colValues.size() == cache.size());
        assertFalse(colValues.contains(oValueThree));
        assertFalse(colValues.retainAll(setOne));

        try
            {
            colValues.add(oValueThree);
            fail();
            }
        catch (UnsupportedOperationException e)
            {
            }

        try
            {
            colValues.addAll(setOne);
            fail();
            }
        catch (UnsupportedOperationException e)
            {
            }

        cache.put(oKeyThree, oValueThree);
        assertTrue(colValues.remove(oValueOne));
        assertFalse(colValues.remove(oValueOne));

        assertTrue(colValues.size() == cache.size());
        assertFalse(colValues.contains(oValueOne));

        assertTrue(colValues.removeAll(setTwo));
        assertTrue(colValues.isEmpty());
        assertFalse(colValues.removeAll(setTwo));

        cache.put(oKeyOne, oValueOne);
        Object[] ao = colValues.toArray();

        assertTrue(ao.length == 1);
        assertEquals(oValueOne, ao[0]);

        colValues.clear();

        assertTrue(colValues.isEmpty());
        assertTrue(cache.isEmpty());
        }

    /**
     * Verify that call to {@link NamedCache#values(Filter filter)} uses ConverterCollections to lazily deserialize results returned by the server.
     *
     * {@link NamedCache#values(Filter, Comparator)} will return collection of already deserialized values
     */
    @Test
    public void valuesLazyDeserialization()
        {
        NamedCache<String, MyObject> cache = getNamedCache();

        Set<MyObject> setOne   = new HashSet();
        Set<MyObject> setTwo   = new HashSet();
        MyObject      valueOne = new MyObject(1);
        setOne.add(valueOne);
        setTwo.add(new MyObject(2));
        setTwo.add(new MyObject(3));

        for (int i = 1; i < 4; i++)
            {
            cache.put("key-" + i, new MyObject(i));
            }

        Filter<Integer>      filter = Filters.less(Extractors.extract("value"), 2);
        Collection<MyObject> colVal = cache.values(filter);

        assertThat(getDeserializationCount(), is(0));
        assertThat(colVal.contains(valueOne), is(true));
        assertThat(getDeserializationCount(), is(1));
        assertThat(colVal.size(), is(setOne.size()));
        assertThat(colVal.containsAll(setOne), is(true));
        assertThat(getDeserializationCount(), is(2));

        setDeserializationCount(0);

        filter = Filters.greater(Extractors.extract("value"), 1);
        colVal = cache.values(filter);

        assertThat(getDeserializationCount(), is(0));
        assertThat(colVal.size(), is(setTwo.size()));
        assertThat(colVal.containsAll(setTwo), is(true));
        assertThat(getDeserializationCount(), is(3));

        setDeserializationCount(0);
        }

    /**
     * Test the behavior of {@link NamedCache#values(Filter)}.
     */
    @Test
    public void valuesFilter()
        {
        NamedCache<String, Integer> cache = getNamedCache();

        Set<Integer> setOne = new HashSet();
        Set<Integer> setTwo = new HashSet();
        setOne.add(1);
        setTwo.add(2);
        setTwo.add(3);

        for (int i = 1; i < 4; i++)
            {
            cache.put("key-" + i, i);
            }

        Filter<Integer>     filter = Filters.less(Extractors.identity(), 2);
        Collection<Integer> colVal = cache.values(filter);

        assertThat(colVal.size(), is(setOne.size()));
        assertThat(colVal.contains(1), is(true));
        assertThat(colVal.containsAll(setOne), is(true));

        filter = Filters.greater(Extractors.identity(), 1);
        colVal = cache.values(filter);

        assertThat(colVal.size(), is(setTwo.size()));
        assertThat(colVal.contains(2), is(true));
        assertThat(colVal.contains(3), is(true));
        assertThat(colVal.containsAll(setTwo), is(true));

        filter = Filters.less(Extractors.identity(), 1);
        colVal = cache.values(filter);

        assertThat(colVal.isEmpty(), is(true));
        }

    /**
     * Test the behavior of {@link NamedCache#values(Filter, Comparator)}.
     */
    @Test
    public void valuesFilterComparator()
        {
        NamedCache<String, Integer> cache = getNamedCache();

        Set<Integer> setOne = new HashSet();
        Set<Integer> setTwo = new HashSet();
        setOne.add(1);
        setTwo.add(2);
        setTwo.add(3);

        for (int i = 1; i < 4; i++)
            {
            cache.put("key-" + i, i);
            }

        Comparator<Integer>  comparator = new IntegerComparator();
        Filter<Integer>      filter     = Filters.less(Extractors.identity(), 2);
        Collection<Integer>  colVal     = cache.values(filter, comparator);

        assertThat(colVal.size(), is(setOne.size()));
        assertThat(colVal.contains(1), is(true));
        assertThat(colVal.containsAll(setOne), is(true));

        filter = Filters.greater(Extractors.identity(), 1);
        colVal = cache.values(filter, comparator);

        assertThat(colVal.size(), is(setTwo.size()));
        assertThat(colVal.contains(2), is(true));
        assertThat(colVal.contains(3), is(true));
        assertThat(colVal.containsAll(setTwo), is(true));

        filter = Filters.less(Extractors.identity(), 1);
        colVal = cache.values(filter, comparator);

        assertThat(colVal.isEmpty(), is(true));
        }

    /**
    * Test the behavior of {@link ContinuousQueryCache} when used with
    * Coherence*Extend.
    */
    @Test
    public void continuousQuery()
        {
        if (VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }
        NamedCache cache = getNamedCache();

        Set set = new HashSet();
        set.add("1");
        set.add("2");

        ContinuousQueryCache cqc = new ContinuousQueryCache(cache,
                new InFilter("toString", set));
        try
            {
            assertEquals(0, cqc.size());

            cache.put(getKeyObject("1"), "1");
            assertEquals(1, cqc.size());
            assertEquals("1", cqc.get(getKeyObject("1")));

            cache.put(getKeyObject("2"), "2");
            assertEquals(2, cqc.size());
            assertEquals("1", cqc.get(getKeyObject("1")));
            assertEquals("2", cqc.get(getKeyObject("2")));

            cache.put(getKeyObject("3"), "3");
            assertEquals(2, cqc.size());
            assertEquals("1", cqc.get(getKeyObject("1")));
            assertEquals("2", cqc.get(getKeyObject("2")));
            assertNull(cqc.get(getKeyObject("3")));
            }
        finally
            {
            cqc.release();
            }
        }

    /**
    * Invoke {@link NamedCache#lock(Object)}.
    */
    @Test
    public void lock()
        {
        if (CACHE_LOCAL_EXTEND_DIRECT.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache   = getNamedCache();
        boolean    fLocked = cache.lock(getKeyObject("Key"));
        try
            {
            assertTrue(fLocked);
            }
        finally
            {
            assertTrue(cache.unlock(getKeyObject("Key")));
            }
        }

    /**
    * Invoke {@link NamedCache#lock(Object, long)}.
    */
    @Test
    public void lockTimeout()
        {
        if (CACHE_LOCAL_EXTEND_DIRECT.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache   = getNamedCache();
        boolean    fLocked = cache.lock(getKeyObject("Key"), 1000L);
        try
            {
            assertTrue(fLocked);
            }
        finally
            {
            assertTrue(cache.unlock(getKeyObject("Key")));
            }
        }

    /**
    * Invoke {@link NamedCache#lock(Object)} with
    * {@link ConcurrentMap#LOCK_ALL}.
    */
    @Test
    public void lockAll()
        {
        if (CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()) ||
            VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }

        try
            {
            getNamedCache().lock(ConcurrentMap.LOCK_ALL);
            }
        catch (UnsupportedOperationException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link NamedCache#lock(Object, long)} with
    * {@link ConcurrentMap#LOCK_ALL}.
    */
    @Test
    public void lockAllTimeout()
        {
        if (CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()) ||
            VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }

        try
            {
            getNamedCache().lock(ConcurrentMap.LOCK_ALL, 1000L);
            }
        catch (UnsupportedOperationException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link NamedCache#unlock(Object)} with
    * {@link ConcurrentMap#LOCK_ALL}.
    */
    @Test
    public void unlockAll()
        {
        if (CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()) ||
            VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }

        try
            {
            getNamedCache().unlock(ConcurrentMap.LOCK_ALL);
            }
        catch (UnsupportedOperationException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }


    // ----- TransactionMap tests -------------------------------------------

    /**
    * Test the behavior of {@link CacheFactory#getLocalTransaction(NamedCache)}
    * with a remote NamedCache.
    */
    @Test
    public void getLocalTransaction()
        {
        if (CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()))
            {
            return;
            }

        try
            {
            CacheFactory.getLocalTransaction(getNamedCache());
            }
        catch (RuntimeException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }


    // ----- regression tests -----------------------------------------------

    /**
     * Regression test for COH-9355 event delivery semantics.
     */
    @Test
    public void testCoh9355()
        {
        final AtomicInteger atomicAllUpdate = new AtomicInteger();
        final AtomicInteger atomicAllInsert = new AtomicInteger();
        final AtomicInteger atomicAllDelete = new AtomicInteger();

        final AtomicInteger atomicAllLiteUpdate = new AtomicInteger();
        final AtomicInteger atomicAllLiteInsert = new AtomicInteger();
        final AtomicInteger atomicAllLiteDelete = new AtomicInteger();

        final AtomicInteger atomicKeyUpdate = new AtomicInteger();
        final AtomicInteger atomicKeyInsert = new AtomicInteger();
        final AtomicInteger atomicKeyDelete = new AtomicInteger();

        final AtomicInteger atomicKeyLiteUpdate = new AtomicInteger();
        final AtomicInteger atomicKeyLiteInsert = new AtomicInteger();
        final AtomicInteger atomicKeyLiteDelete = new AtomicInteger();

        final AtomicInteger atomicFilterUpdate = new AtomicInteger();
        final AtomicInteger atomicFilterInsert = new AtomicInteger();
        final AtomicInteger atomicFilterDelete = new AtomicInteger();

        final AtomicInteger atomicFilterLiteUpdate = new AtomicInteger();
        final AtomicInteger atomicFilterLiteInsert = new AtomicInteger();
        final AtomicInteger atomicFilterLiteDelete = new AtomicInteger();

        final AtomicInteger atomicTransformFilterUpdate = new AtomicInteger();
        final AtomicInteger atomicTransformFilterInsert = new AtomicInteger();
        final AtomicInteger atomicTransformFilterDelete = new AtomicInteger();

        final AtomicInteger atomicTransformFilterLiteUpdate = new AtomicInteger();
        final AtomicInteger atomicTransformFilterLiteInsert = new AtomicInteger();
        final AtomicInteger atomicTransformFilterLiteDelete = new AtomicInteger();

        NamedCache cache = getNamedCache();
        cache.addMapListener(new MapListener()
            {
            public void entryUpdated(MapEvent event)
                {
                atomicAllUpdate.incrementAndGet();
                }

            public void entryInserted(MapEvent event)
                {
                atomicAllInsert.incrementAndGet();
                }

            public void entryDeleted(MapEvent event)
                {
                atomicAllDelete.incrementAndGet();
                }
            }, AlwaysFilter.INSTANCE, false);

        cache.addMapListener(new MapListener()
            {
            public void entryUpdated(MapEvent event)
                {
                atomicAllLiteUpdate.incrementAndGet();
                }

            public void entryInserted(MapEvent event)
                {
                atomicAllLiteInsert.incrementAndGet();
                }

            public void entryDeleted(MapEvent event)
                {
                atomicAllLiteDelete.incrementAndGet();
                }
            }, AlwaysFilter.INSTANCE, true);

        cache.addMapListener(new MapListener()
            {
            public void entryUpdated(MapEvent event)
                {
                atomicKeyUpdate.incrementAndGet();
                }

            public void entryInserted(MapEvent event)
                {
                atomicKeyInsert.incrementAndGet();
                }

            public void entryDeleted(MapEvent event)
                {
                atomicKeyDelete.incrementAndGet();
                }
            }, "Key1", false);

        cache.addMapListener(new MapListener()
            {
            public void entryUpdated(MapEvent event)
                {
                atomicKeyLiteUpdate.incrementAndGet();
                }

            public void entryInserted(MapEvent event)
                {
                atomicKeyLiteInsert.incrementAndGet();
                }

            public void entryDeleted(MapEvent event)
                {
                atomicKeyLiteDelete.incrementAndGet();
                }
            }, "Key1", true);

        cache.addMapListener(new MapListener()
            {
            public void entryUpdated(MapEvent event)
                {
                atomicFilterUpdate.incrementAndGet();
                }

            public void entryInserted(MapEvent event)
                {
                atomicFilterInsert.incrementAndGet();
                }

            public void entryDeleted(MapEvent event)
                {
                atomicFilterDelete.incrementAndGet();
                }
            }, new MapEventFilter(MapEventFilter.E_ALL,
                                  new LessFilter(IdentityExtractor.INSTANCE, 50)), false);

        cache.addMapListener(new MapListener()
            {
            public void entryUpdated(MapEvent event)
                {
                atomicFilterLiteUpdate.incrementAndGet();
                }

            public void entryInserted(MapEvent event)
                {
                atomicFilterLiteInsert.incrementAndGet();
                }

            public void entryDeleted(MapEvent event)
                {
                atomicFilterLiteDelete.incrementAndGet();
                }
            }, new MapEventFilter(MapEventFilter.E_ALL,
                                  new LessFilter(IdentityExtractor.INSTANCE, 50)), true);

        cache.addMapListener(new MapListener()
            {
            public void entryUpdated(MapEvent event)
                {
                atomicTransformFilterUpdate.incrementAndGet();
                }

            public void entryInserted(MapEvent event)
                {
                atomicTransformFilterInsert.incrementAndGet();
                }

            public void entryDeleted(MapEvent event)
                {
                atomicTransformFilterDelete.incrementAndGet();
                }
            }, new MapEventTransformerFilter(
                new MapEventFilter(MapEventFilter.E_ALL,
                                   new LessFilter(IdentityExtractor.INSTANCE, 50)),
                SemiLiteEventTransformer.INSTANCE), false);

        cache.addMapListener(new MapListener()
            {
            public void entryUpdated(MapEvent event)
                {
                atomicTransformFilterLiteUpdate.incrementAndGet();
                }

            public void entryInserted(MapEvent event)
                {
                atomicTransformFilterLiteInsert.incrementAndGet();
                }

            public void entryDeleted(MapEvent event)
                {
                atomicTransformFilterLiteDelete.incrementAndGet();
                }
            }, new MapEventTransformerFilter(
                new MapEventFilter(MapEventFilter.E_ALL,
                                   new LessFilter(IdentityExtractor.INSTANCE, 50)),
                SemiLiteEventTransformer.INSTANCE), true);

        // Insert
        for (int i = 0; i < 100; i++)
            {
            cache.put("Key" + i, i);
            }

        // Update
        for (int i = 0; i < 100; i++)
            {
            cache.put("Key" + i, i);
            }

        // Delete
        for (int i = 0; i < 100; i++)
            {
            cache.remove("Key" + i);
            }

        // let the EventDispatcher thread flush the events
        Eventually.assertThat(invoking(this).dereference(atomicAllInsert), is(100));
        Eventually.assertThat(invoking(this).dereference(atomicAllUpdate), is(100));
        Eventually.assertThat(invoking(this).dereference(atomicAllDelete), is(100));

        assertEquals(100, atomicAllLiteInsert.get());
        assertEquals(100, atomicAllLiteUpdate.get());
        assertEquals(100, atomicAllLiteDelete.get());

        assertEquals(1, atomicKeyInsert.get());
        assertEquals(1, atomicKeyUpdate.get());
        assertEquals(1, atomicKeyDelete.get());

        assertEquals(1, atomicKeyLiteInsert.get());
        assertEquals(1, atomicKeyLiteUpdate.get());
        assertEquals(1, atomicKeyLiteDelete.get());

        assertEquals(50, atomicFilterInsert.get());
        assertEquals(50, atomicFilterUpdate.get());
        assertEquals(50, atomicFilterDelete.get());

        assertEquals(50, atomicFilterLiteInsert.get());
        assertEquals(50, atomicFilterLiteUpdate.get());
        assertEquals(50, atomicFilterLiteDelete.get());

        assertEquals(50, atomicTransformFilterInsert.get());
        assertEquals(50, atomicTransformFilterUpdate.get());
        assertEquals(50, atomicTransformFilterDelete.get());

        assertEquals(50, atomicTransformFilterLiteInsert.get());
        assertEquals(50, atomicTransformFilterLiteUpdate.get());
        assertEquals(50, atomicTransformFilterLiteDelete.get());
        }

    public int dereference(AtomicInteger ref)
        {
        return ref.get();
        }

    /**
     * Regression test that EntryProcessor cache update with setValue(..., true) generates cache event with isSynthetic set to true.
     */
    @Test
    public void testCOH13780()
        {
        if (CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_LOCAL_EXTEND_DIRECT.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_DIRECT.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_DIRECT_JAVA.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_NEAR_ALL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_NEAR_PRESENT.equals(getCacheName()) ||
            VIEW_EXTEND_DIRECT.equals(getCacheName()) ||
            VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache = getNamedCache();
        TestMapListener listener = new TestMapListener();

        cache.clear();
        assertTrue(cache.isEmpty());
        cache.addMapListener(listener);

        cache.put(getKeyObject("Key"), "Value");
        MapEvent evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_INSERTED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals("Value", evt.getNewValue());

        cache.invoke(getKeyObject("Key"), new TestEntryProcessor());
        evt = listener.waitForEvent();
        if (evt instanceof CacheEvent)
            {
            CacheEvent cevt = (CacheEvent) evt;
            assertEquals("EPSetValue", cevt.getNewValue());
            assertTrue("testCOH13780: failed assertion that CacheEvent is synthetic", cevt.isSynthetic());
            }
        }

    /**
     * Regression test that server side filter can filter out synthetic events such as
     * EntryProcessor cache update with setValue(..., true)
     */
    @Test
    public void testCOH13780WithFilter()
        {
        if (CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_LOCAL_EXTEND_DIRECT.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_DIRECT.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_DIRECT_JAVA.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_NEAR_ALL.equals(getCacheName()) ||
            CACHE_REPL_EXTEND_NEAR_PRESENT.equals(getCacheName()) ||
            VIEW_EXTEND_DIRECT.equals(getCacheName()) ||
            VIEW_EXTEND_NEAR.equals(getCacheName()))
            {
            return;
            }

        NamedCache cache = getNamedCache();
        TestMapListener listener = new TestMapListener();

        cache.clear();
        assertTrue(cache.isEmpty());
        NonSyntheticEntryFilter filter = new NonSyntheticEntryFilter();
        cache.addMapListener(listener, filter, false);

        cache.put(getKeyObject("Key"), "Value");
        MapEvent evt = listener.waitForEvent();
        assertNotNull(evt);
        assertEquals(MapEvent.ENTRY_INSERTED, evt.getId());
        assertEquals(getKeyObject("Key"), evt.getKey());
        assertEquals(null, evt.getOldValue());
        assertEquals("Value", evt.getNewValue());

        cache.invoke(getKeyObject("Key"), new TestEntryProcessor());
        assertNull(listener.waitForEvent(2000L));
        }

    @Test
    public void testExpiry()
        {
        NamedCache cache = getNamedCache();

        TestMapListener listener = new TestMapListener();
        cache.addMapListener(listener, 1, false);

        cache.put(1, 1);
        listener.waitForEvent();

        // synthetic event
        cache.invoke(1, new TestEntryProcessor(true));
        MapEvent evt = listener.waitForEvent();

        // COH-25041
        if (!CACHE_LOCAL_EXTEND_DIRECT.equals(getCacheName()))
            {
            assertEquals(true, ((CacheEvent) evt).isSynthetic());
            assertEquals(false, ((CacheEvent) evt).isExpired());
            }

        cache.put(1, 1, 2000);
        listener.waitForEvent();

        // wait for synthetic delete due to expiry
        listener.clearEvent();
        if (CACHE_DIST_EXTEND_LOCAL.equals(getCacheName()) ||
            CACHE_LOCAL_EXTEND_DIRECT.equals(getCacheName()))
            {
            // LocalCache does not have an eviction daemon thread
            sleep(3000);
            cache.get(1);
            }
        evt = listener.waitForEvent(3000);

        assertEquals(true, ((CacheEvent) evt).isSynthetic());
        assertEquals(true, ((CacheEvent) evt).isExpired());

        cache.put(1, 1);
        listener.waitForEvent();
        listener.clearEvent();

        cache.remove(1);

        // regular event
        evt = listener.waitForEvent();

        assertEquals(false, ((CacheEvent) evt).isSynthetic());
        assertEquals(false, ((CacheEvent) evt).isExpired());
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Test whether or not two BigDecimal values are equal to each other.
    *
    * @param dec1  the first BigDecimal to compare
    * @param dec2  the second BigDecimal to compare
    *
    * @return true if the two BigDecimal values are equal
    */
    public static boolean equalsDec(BigDecimal dec1, BigDecimal dec2)
        {
        BigDecimal decDiff = dec1.subtract(dec2);
        return decDiff.doubleValue() == 0.0;
        }

    /**
    * Fill the specified map with BigDecimals, BigIntegers and long values.
    *
    * @param cache  the target cache
    * @param iStart the starting key
    * @param cnt    the number of new entries
    */
    public static void fillBigNumbers(Map cache, int iStart, int cnt)
        {
        int nType = 0;
        for (int i = iStart; i <= cnt; ++i)
            {
            Object oVal;
            switch (nType++ % 3)
                {
                default:
                case 0:
                    oVal = BigDecimal.valueOf(i);
                    break;
                case 1:
                    oVal = BigInteger.valueOf(i);
                    break;
                case 2:
                    oVal = (double) i;
                    break;
                }
            cache.put(String.valueOf(i), oVal);
            }
        }

    /**
     * Total number of times a MyObject is deserialized.
     *
     * @return number of times MyObject is deserialized
     */
    public int getDeserializationCount()
        {
        return s_cDeserializationCount;
        }

    /**
     * Reset the number of times a MyObject is deserialized
     *
     * @param cDeserializationCount  the deserialization count
     */
    public void setDeserializationCount(int cDeserializationCount)
        {
        s_cDeserializationCount = cDeserializationCount;
        }

    // ----- inner class: IntegerComparator ---------------------------------

    /**
     * Simple comparator to order integers from highest to lowest.
     */
    public static class IntegerComparator
            implements Comparator, PortableObject, ExternalizableLite
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor
         */
        public IntegerComparator()
            {
            }

        // ----- Compatator interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        public int compare(Object o1, Object o2)
            {
            if (o1 instanceof Map.Entry)
                {
                o1 = ((Map.Entry) o1).getValue();
                }
            if (o2 instanceof Map.Entry)
                {
                o2 = ((Map.Entry) o2).getValue();
                }
            return -((Integer) o1).compareTo((Integer) o2);
            }

        // ----- PortableObject interface -----------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        // ----- ExternalizableLite interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in)
                throws IOException
            {
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            }
        }

    // ----- inner class: NonSyntheticEntryFilter ----------------------------

    /**
     * Server side filter to filter out coherence synthetic events
     */
    static public class NonSyntheticEntryFilter
            implements EntryFilter, PortableObject, ExternalizableLite
        {
        // ----- EntryFilter interface ---------------------------------------

        @Override
        public boolean evaluate(Object o)
            {
            // filter out synthetic events.
            if (o instanceof CacheEvent)
                {
                CacheEvent evt = (CacheEvent) o;

                if (evt.isSynthetic() && evt.getId() == CacheEvent.ENTRY_UPDATED)
                    {
                    return false;
                    }
                }

            return true;
            }

        @Override
        public boolean evaluateEntry(Map.Entry entry)
            {
            return true;
            }

        // ----- PortableObject interface ------------------------------------

        @Override
        public void readExternal(PofReader pofReader)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter pofWriter)
                throws IOException
            {
            }

        // ----- ExternalizableLite interface --------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            }
        }

    // ----- inner class: TestEntryProcessor ---------------------------------


    public static class TestEntryProcessor extends AbstractProcessor implements PortableObject, ExternalizableLite
        {
        public TestEntryProcessor()
            {
            this(false);
            }

        public TestEntryProcessor(boolean fRemove)
            {
            f_fRemoveSynthetic = fRemove;
            }

        @Override
        public Object process(InvocableMap.Entry entry)
            {
            Logger.log("entrytype is " + entry.getClass().getName(), LOG_ALWAYS);
            if (f_fRemoveSynthetic)
                {
                entry.remove(true);
                }
            else
                {
                entry.setValue("EPSetValue", true);
                }
            return "OK";
            }

        // ----- PortableObject interface -----------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        // ----- ExternalizableLite interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in)
                throws IOException
            {
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            }

        // data members
        protected final boolean f_fRemoveSynthetic;
        }
    // ----- inner class: MyObject ---------------------------------------

    public static class MyObject
            implements ExternalizableLite, PortableObject, Comparable
        {

        public MyObject()
            {
            }

        public MyObject(int value)
            {
            m_nValue = value;
            }

        // ----- ExternalizableLite interface -------------------------------------

        public void readExternal(DataInput in) throws IOException
            {
            m_nValue = in.readInt();
            s_cDeserializationCount++;
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeInt(m_nValue);
            }

        // ----- PortableObject interface -----------------------------------------

        public void readExternal(PofReader in) throws IOException
            {
            m_nValue = in.readInt(0);
            s_cDeserializationCount++;
            }

        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeInt(0, m_nValue);
            }

        // ----- Comparable interface ----------------------------------------------

        public int compareTo(Object o)
            {
            MyObject that = (MyObject) o;

            if (m_nValue != that.m_nValue)
                {
                return m_nValue < that.m_nValue ? -1 : 1;
                }
            return 0;
            }

        // ----- Object methods ----------------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }
            MyObject that = (MyObject) o;
            return m_nValue == that.m_nValue;
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_nValue);
            }

        // ----- helpers ----------------------------------------------------------

        public int getValue()
            {
            return m_nValue;
            }

        public void setValue(int value)
            {
            m_nValue = value;
            }

        // ----- data members ----------------------------------------------------

        protected int m_nValue;
        }

    // ----- data members --------------------------------------------------

    protected static int s_cDeserializationCount;

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CLIENT_CFG_CACHE             = "client-cache-config.xml";

    /**
    * The file name of the default cache configuration file used by cache
    * servers launched by this test.
    */
    public static String FILE_SERVER_CFG_CACHE             = "server-cache-config.xml";

    /**
    * Cache name: "local-extend-direct"
    */
    public static String CACHE_LOCAL_EXTEND_DIRECT         = "local-extend-direct";

    /**
    * Cache name: "dist-extend-direct-bundling"
    */
    public static String CACHE_DIST_EXTEND_DIRECT_BUNDLING = "dist-extend-direct-bundling";

    /**
    * Cache name: "dist-extend-client-key"
    */
    public static String CACHE_DIST_EXTEND_CLIENT_KEY      = "dist-extend-client-key";

    /**
    * Cache name: "dist-extend-direct"
    */
    public static String CACHE_DIST_EXTEND_DIRECT          = "dist-extend-direct";

    /**
    * Cache name: "dist-extend-direct-java"
    */
    public static String CACHE_DIST_EXTEND_DIRECT_JAVA     = "dist-extend-direct-java";

    /**
    * Cache name: "dist-extend-local"
    */
    public static String CACHE_DIST_EXTEND_LOCAL           = "dist-extend-local";

    /**
    * Cache name: "dist-extend-near-all"
    */
    public static String CACHE_DIST_EXTEND_NEAR_ALL        = "dist-extend-near-all";

    /**
    * Cache name: "dist-extend-near-present"
    */
    public static String CACHE_DIST_EXTEND_NEAR_PRESENT    = "dist-extend-near-present";

    /**
    * Cache name: "repl-extend-direct"
    */
    public static String CACHE_REPL_EXTEND_DIRECT          = "repl-extend-direct";

    /**
    * Cache name: "repl-extend-direct-java"
    */
    public static String CACHE_REPL_EXTEND_DIRECT_JAVA     = "repl-extend-direct-java";

    /**
    * Cache name: "repl-extend-local"
    */
    public static String CACHE_REPL_EXTEND_LOCAL           = "repl-extend-local";

    /**
    * Cache name: "repl-extend-near-all"
    */
    public static String CACHE_REPL_EXTEND_NEAR_ALL        = "repl-extend-near-all";

    /**
    * Cache name: "repl-extend-near-present"
    */
    public static String CACHE_REPL_EXTEND_NEAR_PRESENT    = "repl-extend-near-present";

    /**
    * Cache name: "near-extend-direct"
    */
    public static String CACHE_NEAR_EXTEND_DIRECT          = "near-extend-direct";

    /**
    * Cache name: "view-extend-direct".
    */
    public static final String VIEW_EXTEND_DIRECT          = "view-extend-direct";

    /**
    * Cache name: "view-extend-near".
    */
    public static final String VIEW_EXTEND_NEAR            = "view-extend-near";
    }
