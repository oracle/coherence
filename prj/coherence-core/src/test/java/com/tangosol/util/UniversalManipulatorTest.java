/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.processor.NumberIncrementor;
import com.tangosol.util.processor.NumberMultiplier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static org.hamcrest.CoreMatchers.instanceOf;


/**
 * Tests {@link UniversalManipulator} POF registration.
 *
 * @author Vaso Putica  2026.05.19
 */
public class UniversalManipulatorTest
    {
    @Test
    public void shouldRoundTripUniversalManipulatorThroughPof()
        {
        UniversalManipulator manipulator = new UniversalManipulator("age");

        assertEquals(manipulator, roundTrip(manipulator));
        }

    @Test
    public void shouldRoundTripIncrementorWithUniversalManipulatorThroughPof()
        {
        NumberIncrementor processor = new NumberIncrementor(new UniversalManipulator("age"), 1, false);

        NumberIncrementor result = roundTrip(processor);

        // rest-01 Slice C 14.1.1.2206 MVEL POF backport: type 197 keeps DEV/PROD numeric processors portable
        assertThat(result.getValueManipulator(), instanceOf(UniversalManipulator.class));
        assertEquals(1, result.getNumInc());
        assertFalse(result.getPostInc());
        }

    @Test
    public void shouldRoundTripMultiplierWithUniversalManipulatorThroughPof()
        {
        NumberMultiplier processor = new NumberMultiplier(new UniversalManipulator("age"), 2, true);

        NumberMultiplier result = roundTrip(processor);

        assertThat(result.getValueManipulator(), instanceOf(UniversalManipulator.class));
        assertEquals(2, result.getNumFactor());
        assertTrue(result.getPostFactor());
        }

    private static <T> T roundTrip(T value)
        {
        return (T) ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(value, POF_CONTEXT), POF_CONTEXT);
        }

    private static SimplePofContext createPofContext()
        {
        SimplePofContext context = new SimplePofContext();

        context.registerUserType(96, NumberIncrementor.class, new PortableObjectSerializer(96));
        context.registerUserType(97, NumberMultiplier.class,  new PortableObjectSerializer(97));
        context.registerUserType(197, UniversalManipulator.class, new PortableObjectSerializer(197));

        return context;
        }

    private static final SimplePofContext POF_CONTEXT = createPofContext();
    }
