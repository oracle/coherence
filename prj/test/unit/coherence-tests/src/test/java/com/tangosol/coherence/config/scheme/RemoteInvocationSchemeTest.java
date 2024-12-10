/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.net.InvocationService;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Unit Tests for a {@link RemoteInvocationScheme}.
 *
 * @author pfm  2012.06.28
 */
public class RemoteInvocationSchemeTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        RemoteInvocationScheme scheme = new RemoteInvocationScheme();

        assertEquals(InvocationService.TYPE_REMOTE, scheme.getServiceType());
        assertFalse(scheme.isRunningClusterNeeded());
        }
    }
