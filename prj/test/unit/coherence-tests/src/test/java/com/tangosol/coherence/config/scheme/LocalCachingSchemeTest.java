/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.net.CacheService;
import com.tangosol.net.cache.LocalCache;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;

/**
 * Unit Tests for a {@link AbstractLocalCachingScheme}.
 *
 * @author pfm  2012.06.28
 * @since Coherence 12.1.2
 */
public class LocalCachingSchemeTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        ConcreteScheme scheme = new ConcreteScheme();

        assertEquals(CacheService.TYPE_LOCAL, scheme.getServiceType());
        assertEquals(Collections.EMPTY_LIST, scheme.getEventInterceptorBuilders());
        assertNull(scheme.getCustomBuilder());
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        ConcreteScheme                   scheme    = new ConcreteScheme();

        ParameterizedBuilder<LocalCache> bldrCache = new InstanceBuilder<LocalCache>(LocalCache.class);

        scheme.setCustomBuilder(bldrCache);
        assertEquals(bldrCache, scheme.getCustomBuilder());
        }

    // ----- inner class ConcreteClass --------------------------------------

    /**
     * Concrete class for testing.
     */
    public static class ConcreteScheme
            extends AbstractLocalCachingScheme
        {
        }
    }
