/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package trigger;


import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.ValueManipulator;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.FilterTrigger;
import com.tangosol.util.filter.LessFilter;

import com.tangosol.util.processor.NumberIncrementor;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
* A collection of functional tests for the MapTrigger functionality.
*
* @author gg 2008.03.14
*/
public abstract class AbstractMapTriggerTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new AbstractMapTriggerTests that will use the cache with the
    * given name in all test methods.
    *
    * @param sCache  the test cache name
    */
    public AbstractMapTriggerTests(String sCache)
        {
        if (sCache == null || sCache.trim().length() == 0)
            {
            throw new IllegalArgumentException("Invalid cache name");
            }

        m_sCache = sCache.trim();
        }

    @Override
    public void _afterTest()
        {
        getNamedCache().destroy();
        super._afterTest();
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
    * Test of the put operations.
    */
    @Test
    public void testPut()
        {
        NamedCache cache = getNamedCache();
        cache.clear();

        // rollback-trigger
        MapTriggerListener listener = new MapTriggerListener(m_triggerRollback);
        cache.addMapListener(listener);

        cache.put("1", Integer.valueOf(1));
        try
            {
            cache.put("2", Integer.valueOf(2));
            fail("Failed rollback-trigger");
            }
        catch (RuntimeException e)
            {
            // expected exception
            }
        cache.removeMapListener(listener);

        // ignore-trigger
        listener = new MapTriggerListener(m_triggerIgnore);
        cache.addMapListener(listener);

        cache.put("1", Integer.valueOf(11));
        assertTrue("Failed ignore-trigger update", Integer.valueOf(1).equals(cache.get("1")));

        cache.put("2", Integer.valueOf(2));
        assertFalse("Failed ignore-trigger insert", cache.containsKey("2"));

        cache.removeMapListener(listener);

        // no triggers
        cache.put("2", Integer.valueOf(2));
        Integer IValue = (Integer) cache.get("2");
        assertTrue("Failed to remove the trigger", IValue != null && IValue.intValue() == 2);

        // remove-trigger
        listener = new MapTriggerListener(m_triggerRemove);
        cache.addMapListener(listener);

        cache.put("2", Integer.valueOf(2));
        Eventually.assertDeferred(() -> cache.containsKey("2"), is(false));


        cache.removeMapListener(listener);
        }

    /**
    * Test of the putAll operations.
    */
    @Test
    public void testPutAll()
        {
        NamedCache cache = getNamedCache();
        cache.clear();

        // rollback-trigger
        MapTriggerListener listener = new MapTriggerListener(m_triggerRollback);
        cache.addMapListener(listener);

        Map map = new HashMap();
        map.put("1", Integer.valueOf(1));
        map.put("2", Integer.valueOf(2));
        try
            {
            cache.putAll(map);
            fail("Failed trigger");
            }
        catch (RuntimeException e)
            {
            // expected exception
            }
        assertFalse("Failed rollback-trigger", cache.containsKey("2"));
        cache.removeMapListener(listener);

        // ignore-trigger
        listener = new MapTriggerListener(m_triggerIgnore);
        cache.addMapListener(listener);

        cache.putAll(map);
        assertFalse("Failed ignore-trigger", cache.containsKey("2"));

        cache.removeMapListener(listener);

        // no triggers
        cache.putAll(map);
        Integer IValue = (Integer) cache.get("2");
        assertTrue("Failed to remove the trigger", IValue != null && IValue.intValue() == 2);

        // remove-trigger
        listener = new MapTriggerListener(m_triggerRemove);
        cache.addMapListener(listener);

        cache.putAll(map);
        Eventually.assertDeferred(() -> cache.containsKey("2"), is(false));

        cache.removeMapListener(listener);
        }

    /**
    * Test of the remove operations.
    */
    @Test
    public void testRemove()
        {
        NamedCache cache = getNamedCache();
        cache.clear();
        cache.put("1", Integer.valueOf(1));
        cache.put("2", Integer.valueOf(2));

        // rollback-trigger
        MapTriggerListener listener = new MapTriggerListener(m_triggerRollback);
        cache.addMapListener(listener);

        // remove operation is allowed by the trigger
        cache.remove("1");
        cache.remove("2");

        cache.removeMapListener(listener);
        }

    /**
    * Test of the invoke operations.
    */
    @Test
    public void testInvoke()
        {
        NamedCache cache = getNamedCache();
        cache.clear();
        cache.put("0", Integer.valueOf(0));

        // rollback-trigger
        MapTriggerListener listener = new MapTriggerListener(m_triggerRollback);
        cache.addMapListener(listener);

        InvocableMap.EntryProcessor agent = new NumberIncrementor(
            (ValueManipulator) null, Integer.valueOf(1), false);

        cache.invoke("0", agent); // 0 -> 1
        try
            {
            cache.invoke("0", agent); // 1 -> 2
            fail("Failed rollback-trigger");
            }
        catch (RuntimeException e)
            {
            // expected exception
            }
        cache.removeMapListener(listener);

        // ignore-trigger
        listener = new MapTriggerListener(m_triggerIgnore);
        cache.addMapListener(listener);

        Integer IValue = (Integer) cache.invoke("0", agent); // 1 -> 2
        assertTrue("returned " + IValue, IValue != null && IValue.intValue() == 2);
        IValue = (Integer) cache.get("0");
        assertTrue("Failed ignore-trigger", IValue != null && IValue.intValue() == 1);

        cache.removeMapListener(listener);

        // no triggers
        IValue = (Integer) cache.invoke("0", agent); // 1 -> 2
        assertTrue("returned " + IValue, IValue != null && IValue.intValue() == 2);
        IValue = (Integer) cache.get("0");
        assertTrue("Failed ignore-trigger", IValue != null && IValue.intValue() == 2);

        // remove-trigger
        listener = new MapTriggerListener(m_triggerRemove);
        cache.addMapListener(listener);

        IValue = (Integer) cache.invoke("0", agent); // 2 -> 3
        assertTrue("returned " + IValue, IValue != null && IValue.intValue() == 3);

        Eventually.assertDeferred(() -> cache.containsKey("0"), is(false));

        cache.removeMapListener(listener);
        }

    /**
    * Test of the invokeAll operations.
    */
    @Test
    public void testInvokeAll()
        {
        NamedCache cache = getNamedCache();
        cache.clear();
        cache.put("0", Integer.valueOf(0));
        cache.put("1", Integer.valueOf(1));

        // rollback-trigger
        MapTriggerListener listener = new MapTriggerListener(m_triggerRollback);
        cache.addMapListener(listener);

        InvocableMap.EntryProcessor agent = new NumberIncrementor(
            (ValueManipulator) null, Integer.valueOf(1), false);

        try
            {
            cache.invokeAll(cache.keySet(), agent); // 0 -> 1; 1 -> 2
            fail("Failed rollback-trigger");
            }
        catch (RuntimeException e)
            {
            // expected exception
            }

        cache.removeMapListener(listener);

        // no triggers
        Integer IValue = (Integer) cache.get("1");
        assertTrue("returned " + IValue, IValue != null && IValue.intValue() == 1);
        }


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

    protected MapTrigger m_triggerRollback =
        new FilterTrigger(new LessFilter(IdentityExtractor.INSTANCE, Integer.valueOf(2)), FilterTrigger.ACTION_ROLLBACK);
    protected MapTrigger m_triggerIgnore =
        new FilterTrigger(new LessFilter(IdentityExtractor.INSTANCE, Integer.valueOf(2)), FilterTrigger.ACTION_IGNORE);
    protected MapTrigger m_triggerRemove =
        new FilterTrigger(new LessFilter(IdentityExtractor.INSTANCE, Integer.valueOf(2)), FilterTrigger.ACTION_REMOVE);
    }