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

import com.tangosol.util.MapListener;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit Tests for a {@link AbstractCachingScheme}.
 *
 * @author pfm  2012.06.28
 * @since Coherence 12.1.2
 */
public class CachingSchemeTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        ConcreteScheme scheme = new ConcreteScheme();

        assertNull(scheme.getListenerBuilder());
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        ConcreteScheme                    scheme       = new ConcreteScheme();

        ParameterizedBuilder<MapListener> bldrListener = new InstanceBuilder<MapListener>(MapListener.class);

        scheme.setListenerBuilder(bldrListener);
        assertEquals(bldrListener, scheme.getListenerBuilder());
        }

    /**
     * Test validate.
     */
    @Test
    public void testValidate()
        {
        try
            {
            new ConcreteScheme().validate(null);
            fail("Expected IllegalArgumentException due to missing ParameterResolver");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("resolver"));
            }
        }

    // ----- inner class ConcreteClass --------------------------------------

    /**
     * Concrete class for testing.
     */
    public static class ConcreteScheme
            extends AbstractCachingScheme
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getServiceType()
            {
            return CacheService.TYPE_LOCAL;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isRunningClusterNeeded()
            {
            return false;
            }
        }
    }
