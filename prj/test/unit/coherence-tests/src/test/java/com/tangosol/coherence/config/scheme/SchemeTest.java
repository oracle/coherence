/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit Tests for a {@link AbstractScheme}.
 *
 * @author pfm  2012.06.28
 */
public class SchemeTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        ConcreteScheme scheme = new ConcreteScheme();

        assertEquals("", scheme.getSchemeName());
        assertTrue(scheme.isAnonymous());
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        ConcreteScheme scheme = new ConcreteScheme();
        scheme.setSchemeName("foo");
        assertEquals("foo", scheme.getSchemeName());
        assertFalse(scheme.isAnonymous());
        }

    // ----- inner class ConcreteClass --------------------------------------

    /**
     * Concrete class for testing.
     */
    public static class ConcreteScheme
            extends AbstractScheme
        {
        }
    }
