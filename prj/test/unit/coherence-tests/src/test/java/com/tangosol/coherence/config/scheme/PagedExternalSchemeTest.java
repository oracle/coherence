/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.util.Duration.Magnitude;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.storemanager.BinaryStoreManagerBuilder;
import com.tangosol.coherence.config.builder.storemanager.NioFileManagerBuilder;
import com.tangosol.coherence.config.unit.Seconds;

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

import java.util.Map;

/**
 * Unit Tests for a {@link PagedExternalScheme}.
 *
 * @author pfm  2012.06.28
 * @since Coherence 12.1.2
 */
public class PagedExternalSchemeTest
    {
    /**
     * Test realize.
     */
    @Test
    public void testRealize()
        {
        PagedExternalScheme scheme = new PagedExternalScheme();

        MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(null,
                                                   Mockito.mock(BackingMapManagerContext.class),
                                                   Base.getContextClassLoader(), "TestCache", "");

        scheme.setPageLimit(new LiteralExpression<Integer>(5));
        scheme.setBinaryStoreManagerBuilder(new NioFileManagerBuilder());

        Map map = scheme.realizeMap(new NullParameterResolver(), dependencies);

        assertNotNull(map);
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        PagedExternalScheme scheme = new PagedExternalScheme();

        assertNull(scheme.getBinaryStoreManagerBuilder());

        assertEquals(5, scheme.getPageDurationSeconds(new NullParameterResolver()).as(Magnitude.SECOND));
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        PagedExternalScheme       scheme = new PagedExternalScheme();

        BinaryStoreManagerBuilder bldr   = new NioFileManagerBuilder();

        scheme.setBinaryStoreManagerBuilder(bldr);
        assertEquals(bldr, scheme.getBinaryStoreManagerBuilder());

        scheme.setPageDurationSeconds(new LiteralExpression<Seconds>(new Seconds(10)));
        assertEquals(10, scheme.getPageDurationSeconds(new NullParameterResolver()).as(Magnitude.SECOND));

        scheme.setPageLimit(new LiteralExpression<Integer>(100));
        assertEquals(100, scheme.getPageLimit(new NullParameterResolver()));
        }

    /**
     * Test validate.
     */
    @Test
    public void testValidate()
        {
        MapBuilder.Dependencies realizeContext = new MapBuilder.Dependencies(null,
                                                     Mockito.mock(BackingMapManagerContext.class),
                                                     Base.getContextClassLoader(), "TestCache", "");

        try
            {
            PagedExternalScheme scheme = new PagedExternalScheme();

            scheme.realizeMap(null, null);
            fail("Expected IllegalArgumentException due to missing ParameterResolver");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("resolver"));
            }

        try
            {
            PagedExternalScheme scheme = new PagedExternalScheme();

            scheme.setPageLimit(new LiteralExpression<Integer>(1));
            scheme.realizeMap(new NullParameterResolver(), realizeContext);
            fail("Expected IllegalArgumentException due to invalid Page Limit");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("page limit"));
            }

        try
            {
            PagedExternalScheme scheme = new PagedExternalScheme();

            scheme.setPageLimit(new LiteralExpression<Integer>(3601));
            scheme.realizeMap(new NullParameterResolver(), realizeContext);
            fail("Expected IllegalArgumentException due to invalid Page Limit");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("page limit"));
            }

        try
            {
            PagedExternalScheme scheme = new PagedExternalScheme();

            scheme.setPageLimit(new LiteralExpression<Integer>(5));
            scheme.setPageDurationSeconds(new LiteralExpression<Seconds>(new Seconds(4)));
            scheme.realizeMap(new NullParameterResolver(), realizeContext);
            fail("Expected IllegalArgumentException due to invalid Page Duration Seconds");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("page seconds"));
            }

        try
            {
            PagedExternalScheme scheme = new PagedExternalScheme();

            scheme.setPageLimit(new LiteralExpression<Integer>(5));
            scheme.setPageDurationSeconds(new LiteralExpression<Seconds>(new Seconds(604801)));
            scheme.realizeMap(new NullParameterResolver(), realizeContext);
            fail("Expected IllegalArgumentException due to invalid Page Duration Seconds");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("page seconds"));
            }
        }
    }
