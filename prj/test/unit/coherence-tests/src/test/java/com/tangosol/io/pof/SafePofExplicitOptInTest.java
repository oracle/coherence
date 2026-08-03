/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import data.pof.PortablePerson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for the explicit {@code safe-pof} compatibility context.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public class SafePofExplicitOptInTest
    {
    @Test
    public void testExplicitSafePofContextRoundTripsUnregisteredPortableObject()
        {
        SafeConfigurablePofContext context = new SafeConfigurablePofContext();
        context.setEnableAutoTypeDiscovery(false);

        PortablePerson person = PortablePerson.createNoChildren();
        Binary         bin    = ExternalizableHelper.toBinary(person, context);
        Object         result = ExternalizableHelper.fromBinary(bin, context);

        assertTrue(result instanceof PortablePerson);
        assertEquals(person, result);
        }
    }
