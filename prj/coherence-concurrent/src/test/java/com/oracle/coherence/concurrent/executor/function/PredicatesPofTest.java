/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.function;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.function.Remote.Predicate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests for the POF serialization of {@link Predicates} implementations.
 *
 * @author phf 2026.09.21
 * @since 26.10
 */
public class PredicatesPofTest
    {
    /**
     * COH-33859: Ensure that AndPredicate remains registered for strict POF serialization.
     */
    @Test
    public void shouldRoundTripAndPredicateWithStrictPof()
        {
        ConfigurablePofContext context   = new ConfigurablePofContext("coherence-concurrent-pof-config.xml");
        Predicate<String>      predicate = Predicates.and(Predicates.always(), Predicates.never());

        Binary            binary = ExternalizableHelper.toBinary(predicate, context);
        Predicate<String> result = ExternalizableHelper.fromBinary(binary, context);

        assertInstanceOf(Predicates.AndPredicate.class, result);
        assertEquals(898, context.getUserTypeIdentifier(result));
        assertFalse(result.test("value"));
        }
    }
