/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit Tests for a {@link AbstractCompositeScheme}.
 *
 * @author pfm  2012.06.27
 */
public class CompositeSchemeTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        ConcreteScheme scheme = new ConcreteScheme();

        assertNull(scheme.getBackScheme());
        assertNull(scheme.getFrontScheme());
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        ConcreteScheme scheme = new ConcreteScheme();

        LocalScheme schemeBack = new LocalScheme();
        scheme.setBackScheme(schemeBack);
        assertEquals(schemeBack, scheme.getBackScheme());

        LocalScheme schemeFront = new LocalScheme();
        scheme.setFrontScheme(schemeFront);
        assertEquals(schemeFront, scheme.getFrontScheme());
        }

    /**
     * Test validate.
     */
    @Test
    public void testValidate()
        {
        ParameterResolver resolver = new NullParameterResolver();

        ConcreteScheme scheme = new ConcreteScheme();
        scheme.setBackScheme(new LocalScheme());
        scheme.setFrontScheme(new LocalScheme());
        scheme.validate(resolver);

        try
            {
            scheme.setBackScheme(new LocalScheme());
            scheme.setFrontScheme(null);
            scheme.validate(resolver);
            fail("Expected IllegalArgumentException due to missing FrontScheme");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("front"));
            }

        try
            {
            scheme.setBackScheme(null);
            scheme.setFrontScheme(new LocalScheme());
            scheme.validate(resolver);
            fail("Expected IllegalArgumentException due to missing BackScheme");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("back"));
            }

        }

    // ----- inner class ConcreteClass --------------------------------------

    /*
     * Concrete class used to test AbstractCompositeScheme.
     */
    public class ConcreteScheme
            extends OverflowScheme
        {
        }
    }
