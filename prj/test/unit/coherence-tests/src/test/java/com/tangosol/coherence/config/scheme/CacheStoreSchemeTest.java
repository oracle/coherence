/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Base;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit Tests for a {@link CacheStoreScheme}.
 *
 * @author pfm  2012.06.27
 * @since 12.1.2
 */
public class CacheStoreSchemeTest
    {
    /**
     * Test realize.
     */
    @Test
    public void testRealize()
        {
        CacheStoreScheme scheme = new CacheStoreScheme();

        MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(null,
                                                   Mockito.mock(BackingMapManagerContext.class),
                                                   Base.getContextClassLoader(), "TestCache", "");

        assertNull(scheme.realize(new NullParameterResolver(), dependencies));
        assertNull(scheme.realizeLocal(new NullParameterResolver(), dependencies));

        scheme.setCustomBuilder(new InstanceBuilder<Object>(String.class));
        assertNotNull(scheme.realizeLocal(new NullParameterResolver(), dependencies));
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        CacheStoreScheme scheme = new CacheStoreScheme();

        assertNull(scheme.getBundleManager());
        assertNull(scheme.getCustomBuilder());
        assertNull(scheme.getRemoteCacheScheme());
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        CacheStoreScheme scheme = new CacheStoreScheme();

        BundleManager    mgr    = new BundleManager();

        scheme.setBundleManager(mgr);
        assertEquals(mgr, scheme.getBundleManager());

        ParameterizedBuilder<Object> bldr = new InstanceBuilder<Object>();

        scheme.setCustomBuilder(bldr);
        assertEquals(bldr, scheme.getCustomBuilder());

        RemoteCacheScheme schemeRemote = new RemoteCacheScheme();

        scheme.setRemoteCacheScheme(schemeRemote);
        assertEquals(schemeRemote, scheme.getRemoteCacheScheme());
        }
    }
