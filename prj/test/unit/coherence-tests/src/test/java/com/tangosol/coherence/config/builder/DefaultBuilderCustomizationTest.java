/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * {@link DefaultBuilderCustomizationTest} contains unit tests for {@link DefaultBuilderCustomization}s.
 *
 * @author pfm  2012.06.07
 */
public class DefaultBuilderCustomizationTest
    {
    @Test
    public void testAccess()
        {
        ParameterizedBuilder<String> bldrString = new InstanceBuilder<String>();
        DefaultBuilderCustomization<String> bldrCustom = new DefaultBuilderCustomization<String>();
        bldrCustom.setCustomBuilder(bldrString);
        assertEquals(bldrString, bldrCustom.getCustomBuilder());
        }
    }
