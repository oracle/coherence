/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.MapBuilder;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.net.cache.LocalCache;
import com.tangosol.util.Base;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

/**
 * Unit Tests for a {@link BackingMapScheme}.
 *
 * @author pfm  2012.06.27
 * @since Coherence 12.1.2
 */
public class BackingMapSchemeTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test realize.
     */
    @Test
    public void testRealize()
        {
        BackingMapScheme scheme = new BackingMapScheme();

        scheme.setInnerScheme(new LocalScheme());

        MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(null,
                                                   Mockito.mock(BackingMapManagerContext.class),
                                                   Base.getContextClassLoader(), "TestCache", "");

        Map map = scheme.realizeMap(new NullParameterResolver(), dependencies);

        assertNotNull(map);
        assert(map.getClass().equals(LocalCache.class));
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        BackingMapScheme scheme = new BackingMapScheme();

        assertNull(scheme.getInnerScheme());

        scheme.setInnerScheme(new LocalScheme());
        assertFalse(scheme.isPartitioned(new NullParameterResolver(), false));
        assertTrue(scheme.isPartitioned(new NullParameterResolver(), true));
        assertFalse(scheme.isTransient(new NullParameterResolver()));
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        BackingMapScheme scheme = new BackingMapScheme();

        scheme.setInnerScheme(new LocalScheme());

        scheme.setPartitioned(new LiteralExpression<String>("t"));
        assertTrue(scheme.isPartitioned(new NullParameterResolver(), false));

        scheme.setPartitioned(new LiteralExpression<String>("f"));
        assertFalse(scheme.isPartitioned(new NullParameterResolver(), true));

        scheme.setTransient(new LiteralExpression<Boolean>(Boolean.TRUE));
        assertTrue(scheme.isTransient(new NullParameterResolver()));

        scheme.setTransient(new LiteralExpression<Boolean>(Boolean.FALSE));
        assertFalse(scheme.isTransient(new NullParameterResolver()));
        }

    /**
     * Test validate.
     */
    @Test
    public void testValidate()
        {
        BackingMapScheme scheme = new BackingMapScheme();

        MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(null,
                                                   Mockito.mock(BackingMapManagerContext.class),
                                                   Base.getContextClassLoader(), "TestCache", "");

        try
            {
            assertFalse(scheme.isPartitioned(new NullParameterResolver(), false));
            fail("Expected IllegalArgumentException due to missing inner scheme");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("inner scheme"));
            }

        try
            {
            assertFalse(scheme.isPartitioned(new NullParameterResolver(), false));
            fail("Expected IllegalArgumentException due to missing inner scheme");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("inner scheme"));
            }

        try
            {
            scheme.realizeMap(new NullParameterResolver(), dependencies);
            fail("Expected IllegalArgumentException due to missing inner scheme");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("inner scheme"));
            }

        try
            {
            scheme.setInnerScheme(new LocalScheme());
            scheme.realizeMap(null, null);
            fail("Expected IllegalArgumentException due to missing ParameterResolver");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("resolver"));
            }
        }
    }
