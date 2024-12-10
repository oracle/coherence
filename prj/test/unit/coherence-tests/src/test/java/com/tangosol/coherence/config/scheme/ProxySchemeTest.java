/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit Tests for a {@link ProxyScheme}.
 *
 * @author pfm  2012.06.28
 */
public class ProxySchemeTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        ProxyScheme scheme = new ProxyScheme();

        assertEquals("Proxy", scheme.getServiceType());
        assertTrue(scheme.isRunningClusterNeeded());
        }
    }
