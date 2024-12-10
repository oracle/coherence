/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.caffeine.CaffeineCache;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;
import com.tangosol.coherence.config.unit.Seconds;
import com.tangosol.coherence.config.unit.Units;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Base;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit Tests for a {@link CaffeineScheme}.
 *
 * @author Aleks Seovic  2022.05.17
 * @since 22.06
 */
public class CaffeineSchemeTest
    {
    /**
     * Test realize.
     */
    @Test
    public void testRealize()
        {
        CaffeineScheme scheme = new CaffeineScheme();

        MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(null,
                                                   Mockito.mock(BackingMapManagerContext.class),
                                                   Base.getContextClassLoader(), "TestCache", "");

        CaffeineCache cache = scheme.realizeMap(new NullParameterResolver(), dependencies);

        assertNotNull(cache);
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        CaffeineScheme scheme = new CaffeineScheme();

        assertNull(scheme.getUnitCalculatorBuilder());

        assertEquals(0, scheme.getExpiryDelay(new NullParameterResolver()).getNanos());
        assertEquals(0, scheme.getHighUnits(new NullParameterResolver()).getUnitCount());
        assertEquals(1, scheme.getUnitFactor(new NullParameterResolver()));
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        CaffeineScheme scheme = new CaffeineScheme();

        UnitCalculatorBuilder bldrCalculator = new UnitCalculatorBuilder();

        scheme.setUnitCalculatorBuilder(bldrCalculator);
        assertEquals(bldrCalculator, scheme.getUnitCalculatorBuilder());

        Seconds secs = new Seconds(10);

        scheme.setExpiryDelay(new LiteralExpression<>(secs));
        assertEquals(secs, scheme.getExpiryDelay(new NullParameterResolver()));

        int cHighUnits = 1000;

        scheme.setHighUnits(new LiteralExpression<>(new Units(cHighUnits)));
        assertEquals(cHighUnits, scheme.getHighUnits(new NullParameterResolver()).getUnitCount());

        int nUnitFactor = 10;

        scheme.setUnitFactor(new LiteralExpression<>(nUnitFactor));
        assertEquals(nUnitFactor, scheme.getUnitFactor(new NullParameterResolver()));
        }

    /**
     * Test validate.
     */
    @Test
    public void testValidate()
        {
        try
            {
            new CaffeineScheme().realizeMap(null, null);
            fail("Expected IllegalArgumentException due to missing ParameterResolver");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("resolver"));
            }
        }
    }
