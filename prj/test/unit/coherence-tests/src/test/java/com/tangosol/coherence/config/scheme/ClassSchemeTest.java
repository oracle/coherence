/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.MapBuilder;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;

import com.tangosol.util.Base;
import com.tangosol.util.SafeHashMap;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;

/**
 * Unit Tests for a {@link ClassScheme}.
 *
 * @author pfm  2012.06.27
 * @since Coherence 12.1.2
 */
public class ClassSchemeTest
    {
    /**
     * Test RealizeMap.
     */
    @Test
    public void testRealizeMap()
        {
        ClassScheme scheme = new ClassScheme();

        scheme.setCustomBuilder(new InstanceBuilder<Object>(SafeHashMap.class));

        MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(null,
                                                   Mockito.mock(BackingMapManagerContext.class),
                                                   Base.getContextClassLoader(), "TestCache", "");

        Object oMap = scheme.realizeMap(new NullParameterResolver(), dependencies);

        assertNotNull(oMap);

        Object oTest = scheme.realize(new NullParameterResolver(), null, null);

        assertNotNull(oTest);
        }

    /**
     * Test realize.
     */
    @Test
    public void testRealize()
        {
        final String STR_NAME = "TestString";

        ClassScheme  scheme   = new ClassScheme();

        scheme.setCustomBuilder(new InstanceBuilder<Object>(String.class));

        ParameterList params = new ResolvableParameterList();

        params.add(new Parameter("name", STR_NAME));

        String s = (String) scheme.realize(new NullParameterResolver(), null, params);

        assertNotNull(s);
        assertEquals(STR_NAME, s);
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        ClassScheme scheme = new ClassScheme();

        assertEquals(CacheService.TYPE_LOCAL, scheme.getServiceType());
        assertFalse(scheme.isRunningClusterNeeded());
        assertEquals(Collections.EMPTY_LIST, scheme.getEventInterceptorBuilders());
        }

    /**
     * Test validate.
     */
    @Test
    public void testValidate()
        {
        try
            {
            new ClassScheme().validate();
            fail("Expected IllegalArgumentException due to missing Custom map builder");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("custom map builder"));
            }
        }
    }
