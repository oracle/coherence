/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.io.pof.PortableException;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheStore;

import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.Filter;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.processor.ConditionalRemove;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
* A collection of read-only functional tests for Coherence*Extend.
*
* @author jh  2005.11.29
*/
public class ReadOnlyExtendTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public ReadOnlyExtendTests()
        {
        super(AbstractExtendTests.FILE_CLIENT_CFG_CACHE);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember memberProxy = startCacheServer("ReadOnlyExtendTests", "extend", "server-cache-config-ro.xml");
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("ReadOnlyExtendTests");
        }


    // ----- read-only NamedCache tests -------------------------------------

    /**
    * Invoke {@link NamedCache#addIndex(ValueExtractor, boolean, Comparator)}
    */
    @Test
    public void addIndexReadOnly()
        {
        try
            {
            getNamedCache().addIndex(
                    new ReflectionExtractor("toString"), false, null);
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link NamedCache#removeIndex(ValueExtractor)}
    */
    @Test
    public void removeIndexReadOnly()
        {
        try
            {
            getNamedCache().removeIndex(
                    new ReflectionExtractor("toString"));
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link NamedCache#put(Object, Object)}.
    */
    @Test
    public void putReadOnly()
        {
        try
            {
            getNamedCache().put("Key", "Value");
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link NamedCache#putAll(Map)}
    */
    @Test
    public void putAllReadOnly()
        {
        try
            {
            NamedCache cache = getNamedCache();
            Map        map   = new HashMap();

            map.put("Key", "Value");
            cache.putAll(map);
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link NamedCache#remove(Object)}
    */
    @Test
    public void removeReadOnly()
        {
        try
            {
            getNamedCache().remove("Key");
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link NamedCache#clear()}
    */
    @Test
    public void clearReadOnly()
        {
        try
            {
            getNamedCache().clear();
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link NamedCache#destroy()}
    */
    @Test
    public void destroyReadOnly()
        {
        try
            {
            getNamedCache().destroy();
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link CacheStore#erase(Object)}
    */
    @Test
    public void eraseReadOnly()
        {
        try
            {
            NamedCache cache = getNamedCache();
            CacheStore store = (CacheStore) cache;

            store.erase("Key");
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link CacheStore#eraseAll(Collection)}
    */
    @Test
    public void eraseAllReadOnly()
        {
        try
            {
            NamedCache cache = getNamedCache();
            CacheStore store = (CacheStore) cache;
            List list  = new LinkedList();

            list.add("Key");
            store.eraseAll(list);
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link CacheStore#store(Object, Object)}
    */
    @Test
    public void storeReadOnly()
        {
        try
            {
            NamedCache cache = getNamedCache();
            CacheStore store = (CacheStore) cache;

            store.store("Key", "Value");
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link CacheStore#storeAll(Map)}
    */
    @Test
    public void storeAllReadOnly()
        {
        try
            {
            NamedCache cache = getNamedCache();
            CacheStore store = (CacheStore) cache;
            Map        map   = new HashMap();

            map.put("Key", "Value");
            store.storeAll(map);
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link NamedCache#invoke(Object, EntryProcessor)}.
    */
    @Test
    public void invokeReadOnly()
        {
        try
            {
            getNamedCache().invoke("key",
                    new ConditionalRemove(AlwaysFilter.INSTANCE));
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link NamedCache#invokeAll(Collection, EntryProcessor)}.
    */
    @Test
    public void invokeAllReadOnly()
        {
        try
            {
            getNamedCache().invokeAll(Collections.singleton("key"),
                    new ConditionalRemove(AlwaysFilter.INSTANCE));
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Invoke {@link NamedCache#invokeAll(Filter, EntryProcessor)}.
    */
    @Test
    public void invokeAllFilterReadOnly()
        {
        try
            {
            getNamedCache().invokeAll(AlwaysFilter.INSTANCE,
                    new ConditionalRemove(AlwaysFilter.INSTANCE));
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Attempt to mutate the Set returned from {@link NamedCache#entrySet()}
    */
    @Test
    public void entrySetReadOnly()
        {
        try
            {
            getNamedCache().entrySet().remove(
                    new SimpleMapEntry("Key1", "Value1"));
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Attempt to mutate the Set returned from {@link NamedCache#keySet()}
    */
    @Test
    public void keySetReadOnly()
        {
        try
            {
            getNamedCache().keySet().remove("Key1");
            }
        catch (PortableException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
    * Attempt to mutate the Collection returned from {@link NamedCache#values()}
    */
    @Test
    public void valuesReadOnly()
        {
        try
            {
            getNamedCache().values().iterator().remove();
            }
        catch (IllegalStateException e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the cache used in all test methods.
    *
    * @return the test cache
    */
    protected NamedCache getNamedCache()
        {
        return getNamedCache(AbstractExtendTests.CACHE_DIST_EXTEND_DIRECT);
        }
    }
