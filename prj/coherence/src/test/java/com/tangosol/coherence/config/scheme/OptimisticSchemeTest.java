/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.EvictionPolicyBuilder;
import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;
import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.coherence.config.unit.Bytes;
import com.tangosol.coherence.config.unit.Units;
import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit Tests for a {@link OptimisticScheme}.
 *
 * @author pfm  2012.06.27
 */
public class OptimisticSchemeTest
    {
    // ----- tests ----------------------------------------------------------

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
        LocalScheme scheme = new LocalScheme();

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
        scheme.setUnitFactor(new LiteralExpression<Integer>(new Integer(nUnitFactor)));
        assertEquals(nUnitFactor, scheme.getUnitFactor(new NullParameterResolver()));

        scheme.setPreLoad(new LiteralExpression<Boolean>(new Boolean(true)));
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
