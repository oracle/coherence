/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.bedrock;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests production-mode defaults and explicit member-mode overrides.
 *
 * @author phf  2026.10.23
 * @since 26.10
 */
public class ProductionmodeProfileTest
    {
    @Test
    public void shouldDefaultToProductionMode()
        {
        assertLaunchMode(OptionsByType.of(), "prod");
        }

    @Test
    public void shouldPreserveExplicitDevelopmentMode()
        {
        assertLaunchMode(OptionsByType.of(SystemProperty.of("coherence.mode", "dev")), "dev");
        }

    @Test
    public void shouldPreserveExplicitProductionMode()
        {
        assertLaunchMode(OptionsByType.of(SystemProperty.of("coherence.mode", "prod")), "prod");
        }

    private void assertLaunchMode(OptionsByType options, String sExpected)
        {
        CoherenceClusterMember.MetaClass metaClass = new CoherenceClusterMember.MetaClass();
        metaClass.onLaunching(null, options);
        new ProductionmodeProfile("prod").onLaunching(null, metaClass, options);

        assertEquals(sExpected, options.get(SystemProperties.class).get("coherence.mode").getValue());
        }
    }
