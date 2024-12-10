/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.scheme.LocalScheme;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit Tests for a {@link NearScheme}.
 *
 * @author pfm  2012.06.27
 */
public class NearSchemeTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        NearScheme scheme = new NearScheme();

        scheme.setBackScheme(new LocalScheme());

        assertNotNull(scheme.getServiceBuilder());
        assertEquals("auto", scheme.getInvalidationStrategy(new NullParameterResolver()));
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        NearScheme scheme = new NearScheme();

        LocalScheme schemeBack = new LocalScheme();
        scheme.setBackScheme(schemeBack);
        assertEquals(schemeBack, scheme.getBackScheme());

        LocalScheme schemeFront = new LocalScheme();
        scheme.setFrontScheme(schemeFront);
        assertEquals(schemeFront, scheme.getFrontScheme());

        scheme.setInvalidationStrategy(new LiteralExpression<String>("present"));
        assertEquals("present", scheme.getInvalidationStrategy(new NullParameterResolver()));
        }
    }
