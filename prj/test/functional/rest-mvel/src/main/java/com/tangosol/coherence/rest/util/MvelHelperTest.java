/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;


import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Unit test when explicit mvel2 dependency provided for backwards compatibilty testing.
 */
public class MvelHelperTest
    {
    @Test
    public void validateEnabled()
        {
        assertTrue(MvelHelper.isEnabled());
        }

    @Test
    public void validateMvelParserContext()
        {
        assertNotNull(MvelHelper.getMvelParserContext());
        }
    }
