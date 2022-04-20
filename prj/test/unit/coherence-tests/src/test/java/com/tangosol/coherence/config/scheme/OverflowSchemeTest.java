/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit Tests for a {@link OverflowScheme}.
 *
 * @author pfm  2012.06.27
 */
public class OverflowSchemeTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        OverflowScheme    scheme   = new OverflowScheme();
        ParameterResolver resolver = new NullParameterResolver();

        scheme.setBackScheme(new LocalScheme());

        assertNull(scheme.getMissCacheScheme());
        assertEquals(0, scheme.getExpiryDelay(resolver).getNanos());
        assertFalse(scheme.isExpiryEnabled(resolver));
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        OverflowScheme scheme = new OverflowScheme();

        LocalScheme schemeBack = new LocalScheme();
        scheme.setBackScheme(schemeBack);
        assertEquals(schemeBack, scheme.getBackScheme());

        LocalScheme schemeFront = new LocalScheme();
        scheme.setFrontScheme(schemeFront);
        assertEquals(schemeFront, scheme.getFrontScheme());

        LocalScheme schemeMissCache = new LocalScheme();
        scheme.setMissCacheScheme(schemeMissCache);
        assertEquals(schemeMissCache, scheme.getMissCacheScheme());

        Seconds secs = new Seconds(10);
        scheme.setExpiryDelay(new LiteralExpression<Seconds>(secs));
        assertEquals(secs, scheme.getExpiryDelay(new NullParameterResolver()));

        scheme.setExpiryEnabled(new LiteralExpression<Boolean>(true));
        assertTrue(scheme.isExpiryEnabled(new NullParameterResolver()));
        }
    }
