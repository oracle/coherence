/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.server;

import com.tangosol.coherence.rest.PassThroughRootResource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ContainerPassThroughResourceConfig}.
 *
 * @author tam  2016.10.11
 */
public class ContainerPassThroughResourceConfigTest
    {
    @Test
    public void testCptr()
        {
        ContainerPassThroughResourceConfig config = new ContainerPassThroughResourceConfig();
        assertEquals(config.isRunningInContainer(), true);
        assertEquals(config.isRegistered(PassThroughRootResource.class), true);
        }
    }
