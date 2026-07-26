/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.processor;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.rest.util.MvelManipulator;

import com.tangosol.util.UniversalManipulator;

import com.tangosol.util.processor.NumberMultiplier;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Tests {@link NumberMultiplierFactory}.
 *
 * @author Vaso Putica  2026.05.19
 */
public class NumberMultiplierFactoryTest
    {
    @After
    public void cleanup()
        {
        CoherenceModeHelper.clear();
        }

    @Test
    public void shouldUseMvelManipulatorInCompatibilityMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            NumberMultiplier processor = (NumberMultiplier) new NumberMultiplierFactory(false).getProcessor("age", "2");

            // compatibility mode keeps the POF-registered REST wire type
            assertThat(processor.getValueManipulator(), instanceOf(MvelManipulator.class));
            }
        }

    @Test
    public void shouldUseUniversalManipulatorInHardenedMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            NumberMultiplier processor = (NumberMultiplier) new NumberMultiplierFactory(false).getProcessor("age", "2");

            assertThat(processor.getValueManipulator(), instanceOf(UniversalManipulator.class));
            }
        }

    }
