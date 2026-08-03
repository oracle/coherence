/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json.internal;

import java.io.ObjectInputFilter;

import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for {@link SerializationGate}.
 *
 * @author Aleks Seovic  2026.04.29
 * @since 26.07
 */
public class SerializationGateTest
    {
    @Test
    void shouldUseDefaultFilterWhenProcessWideFilterIsNull()
        {
        assumeTrue(ObjectInputFilter.Config.getSerialFilter() == null);
        assertTrue(SerializationGate.isValid(Date.class));
        assertFalse(SerializationGate.isValid(Runtime.class));
        }
    }
