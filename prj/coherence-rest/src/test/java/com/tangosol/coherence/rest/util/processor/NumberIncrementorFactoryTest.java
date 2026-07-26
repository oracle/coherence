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

import com.tangosol.util.processor.NumberIncrementor;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Tests {@link NumberIncrementorFactory}.
 *
 * @author Vaso Putica  2026.05.19
 */
public class NumberIncrementorFactoryTest
    {
    @After
    public void cleanup()
        {
        CoherenceModeHelper.clear();
        }

    @Test
    public void shouldUseLegacyMvelManipulatorInLegacyMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            NumberIncrementor processor = (NumberIncrementor) new NumberIncrementorFactory(false).getProcessor("age", "1");

            // rest-01 Slice C 14.1.1.2206 MVEL POF backport: LEGACY keeps the POF-registered REST wire type
            assertThat(processor.getValueManipulator(), instanceOf(MvelManipulator.class));
            }
        }

    @Test
    public void shouldUseUniversalManipulatorInDevMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.dev())
            {
            NumberIncrementor processor = (NumberIncrementor) new NumberIncrementorFactory(false).getProcessor("age", "1");

            assertThat(processor.getValueManipulator(), instanceOf(UniversalManipulator.class));
            }
        }

    @Test
    public void shouldUseUniversalManipulatorInProdMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            NumberIncrementor processor = (NumberIncrementor) new NumberIncrementorFactory(false).getProcessor("age", "1");

            assertThat(processor.getValueManipulator(), instanceOf(UniversalManipulator.class));
            }
        }
    }
