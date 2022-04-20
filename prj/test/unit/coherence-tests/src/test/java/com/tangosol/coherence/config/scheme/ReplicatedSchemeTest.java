/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.net.CacheService;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit Tests for a {@link ReplicatedScheme}.
 *
 * @author pfm  2012.06.28
 */
public class ReplicatedSchemeTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        ReplicatedScheme scheme = new ReplicatedScheme();

        assertEquals(CacheService.TYPE_REPLICATED, scheme.getServiceType());
        assertTrue(scheme.isRunningClusterNeeded());
        assertNull(scheme.getBackingMapScheme());
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        ReplicatedScheme scheme = new ReplicatedScheme();

        BackingMapScheme schemeBM = new BackingMapScheme();
        scheme.setBackingMapScheme(schemeBM);
        assertEquals(schemeBM, scheme.getBackingMapScheme());
        }
    }
