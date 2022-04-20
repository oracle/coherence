/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.EvictionPolicyBuilder;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;
import com.tangosol.coherence.config.unit.Seconds;
import com.tangosol.coherence.config.unit.Units;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.cache.LocalCache;

import com.tangosol.util.Base;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit Tests for a {@link LocalScheme}.
 *
 * @author pfm  2012.06.27
 * @since Coherence 12.1.2
 */
public class LocalSchemeTest
    {
    /**
     * Test realize.
     */
    @Test
    public void testRealize()
        {
        LocalScheme scheme = new LocalScheme();

        MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(null,
                                                   Mockito.mock(BackingMapManagerContext.class),
                                                   Base.getContextClassLoader(), "TestCache", "");

        LocalCache cache = scheme.realizeMap(new NullParameterResolver(), dependencies);

        assertNotNull(cache);
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        LocalScheme scheme = new LocalScheme();

        assertNull(scheme.getCacheStoreScheme());
        assertNull(scheme.getEvictionPolicyBuilder());
        assertNull(scheme.getUnitCalculatorBuilder());

        assertEquals(0, scheme.getExpiryDelay(new NullParameterResolver()).getNanos());
        assertEquals(0, scheme.getHighUnits(new NullParameterResolver()).getUnitCount());
        assertEquals(0, scheme.getLowUnits(new NullParameterResolver()).getUnitCount());
        assertEquals(1, scheme.getUnitFactor(new NullParameterResolver()));
        assertFalse(scheme.isPreLoad(new NullParameterResolver()));
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        LocalScheme      scheme           = new LocalScheme();

        CacheStoreScheme schemeCacheStore = new CacheStoreScheme();

        scheme.setCacheStoreScheme(schemeCacheStore);
        assertEquals(schemeCacheStore, scheme.getCacheStoreScheme());

        EvictionPolicyBuilder bldrEviction = new EvictionPolicyBuilder();

        scheme.setEvictionPolicyBuilder(bldrEviction);
        assertEquals(bldrEviction, scheme.getEvictionPolicyBuilder());

        UnitCalculatorBuilder bldrCalculator = new UnitCalculatorBuilder();

        scheme.setUnitCalculatorBuilder(bldrCalculator);
        assertEquals(bldrCalculator, scheme.getUnitCalculatorBuilder());

        Seconds secs = new Seconds(10);

        scheme.setExpiryDelay(new LiteralExpression<Seconds>(secs));
        assertEquals(secs, scheme.getExpiryDelay(new NullParameterResolver()));

        int cHighUnits = 1000;

        scheme.setHighUnits(new LiteralExpression<Units>(new Units(cHighUnits)));
        assertEquals(cHighUnits, scheme.getHighUnits(new NullParameterResolver()).getUnitCount());

        int cLowUnits = 500;

        scheme.setLowUnits(new LiteralExpression<Units>(new Units(cLowUnits)));
        assertEquals(cLowUnits, scheme.getLowUnits(new NullParameterResolver()).getUnitCount());

        int nUnitFactor = 10;

        scheme.setUnitFactor(new LiteralExpression<Integer>(nUnitFactor));
        assertEquals(nUnitFactor, scheme.getUnitFactor(new NullParameterResolver()));

        scheme.setPreLoad(new LiteralExpression<Boolean>(Boolean.TRUE));
        assertTrue(scheme.isPreLoad(new NullParameterResolver()));
        }

    /**
     * Test validate.
     */
    @Test
    public void testValidate()
        {
        try
            {
            new LocalScheme().realizeMap(null, null);
            fail("Expected IllegalArgumentException due to missing ParameterResolver");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("resolver"));
            }
        }
    }
