/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.unit.Millis;
import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.ReadWriteBackingMap;
import com.tangosol.util.Base;
import com.tangosol.util.ObservableMap;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

/**
 * Unit Tests for a {@link ReadWriteBackingMapScheme}.
 *
 * @author pfm  2012.06.27
 * @since Coherence 12.1.2
 */
public class ReadWriteBackingMapSchemeTest
    {
    /**
     * Test realize.
     */
    @Test
    public void testRealize()
        {
        ReadWriteBackingMapScheme scheme = new ReadWriteBackingMapScheme();

        scheme.setInternalScheme(new LocalScheme());

        MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(null,
                                                   Mockito.mock(BackingMapManagerContext.class),
                                                   Base.getContextClassLoader(), "TestCache", "");

        Map map = scheme.realizeMap(new NullParameterResolver(), dependencies);

        assertNotNull(map);
        assert(map.getClass().equals(ReadWriteBackingMap.class));
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        ReadWriteBackingMapScheme scheme = new ReadWriteBackingMapScheme();

        assertNull(scheme.getCacheStoreScheme());
        assertNull(scheme.getInternalScheme());
        assertNull(scheme.getInternalMap());
        assertNull(scheme.getMissCacheScheme());

        assertEquals(0, scheme.getCacheStoreTimeout(new NullParameterResolver()).getNanos());
        assertTrue(0 == scheme.getRefreshAheadFactor(new NullParameterResolver()));
        assertTrue(0 == scheme.getWriteBatchFactor(new NullParameterResolver()));
        assertEquals(0, scheme.getWriteDelay(new NullParameterResolver()).getNanos());
        assertEquals(0, scheme.getWriteDelaySeconds(new NullParameterResolver()));
        assertEquals(128, scheme.getWriteMaxBatchSize(new NullParameterResolver()));
        assertEquals(0, scheme.getWriteRequeueThreshold(new NullParameterResolver()));
        assertFalse(scheme.isReadOnly(new NullParameterResolver()));
        assertTrue(scheme.isRollbackCacheStoreFailures(new NullParameterResolver()));
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        ReadWriteBackingMapScheme scheme           = new ReadWriteBackingMapScheme();

        CacheStoreScheme          schemeCacheStore = new CacheStoreScheme();

        scheme.setCacheStoreScheme(schemeCacheStore);
        assertEquals(schemeCacheStore, scheme.getCacheStoreScheme());

        LocalScheme schemeInternal = new LocalScheme();

        scheme.setInternalScheme(schemeInternal);
        assertEquals(schemeInternal, scheme.getInternalScheme());

        ObservableMap mapInternal = Mockito.mock(ObservableMap.class);

        scheme.setInternalMap(mapInternal);
        assertEquals(schemeInternal, scheme.getInternalScheme());

        LocalScheme schemeMissing = new LocalScheme();

        scheme.setMissCacheScheme(schemeMissing);
        assertEquals(schemeMissing, scheme.getMissCacheScheme());

        Millis mills = new Millis("10");

        scheme.setCacheStoreTimeout(new LiteralExpression<Millis>(mills));
        assertEquals(mills, scheme.getCacheStoreTimeout(new NullParameterResolver()));

        double dflRefreshAhead = 0.5;

        scheme.setRefreshAheadFactor(new LiteralExpression<Double>(dflRefreshAhead));
        assertTrue(dflRefreshAhead == scheme.getRefreshAheadFactor(new NullParameterResolver()));

        double dflWriteBatch = 0.5;

        scheme.setWriteBatchFactor(new LiteralExpression<Double>(dflWriteBatch));
        assertTrue(dflWriteBatch == scheme.getWriteBatchFactor(new NullParameterResolver()));

        Seconds secsWriteDelay = new Seconds(20);

        scheme.setWriteDelay(new LiteralExpression<Seconds>(secsWriteDelay));
        assertEquals(secsWriteDelay, scheme.getWriteDelay(new NullParameterResolver()));

        scheme.setWriteDelaySeconds(new LiteralExpression<Integer>(30));
        assertEquals(30, scheme.getWriteDelaySeconds(new NullParameterResolver()));

        scheme.setWriteMaxBatchSize(new LiteralExpression<Integer>(40));
        assertEquals(40, scheme.getWriteMaxBatchSize(new NullParameterResolver()));

        scheme.setWriteRequeueThreshold(new LiteralExpression<Integer>(50));
        assertEquals(50, scheme.getWriteRequeueThreshold(new NullParameterResolver()));

        scheme.setReadOnly(new LiteralExpression<Boolean>(true));
        assertTrue(scheme.isReadOnly(new NullParameterResolver()));

        scheme.setRollbackCacheStoreFailures(new LiteralExpression<Boolean>(false));
        assertFalse(scheme.isRollbackCacheStoreFailures(new NullParameterResolver()));
        }

    /**
     * Test validate.
     */
    @Test
    public void testValidate()
        {
        ReadWriteBackingMapScheme scheme = new ReadWriteBackingMapScheme();

        MapBuilder.Dependencies realizeContext = new MapBuilder.Dependencies(null,
                                                     Mockito.mock(BackingMapManagerContext.class),
                                                     Base.getContextClassLoader(), "TestCache", "");

        try
            {
            scheme.realizeMap(new NullParameterResolver(), realizeContext);
            fail("Expected IllegalArgumentException due to missing internal scheme");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("internal scheme"));
            }

        try
            {
            scheme.setInternalScheme(new LocalScheme());
            scheme.realizeMap(null, null);
            fail("Expected IllegalArgumentException due to missing ParameterResolver");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("resolver"));
            }
        }
    }
