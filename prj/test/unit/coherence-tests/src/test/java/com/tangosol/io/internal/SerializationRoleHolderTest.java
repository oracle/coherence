/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.tangosol.io.SerializationRole;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link SerializationRole}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class SerializationRoleHolderTest
    {
    @Test
    public void testDefaultRoleIsUnclassified()
        {
        assertEquals(SerializationRole.UNCLASSIFIED, SerializationRole.current());
        }

    @Test
    public void testSetAndCloseRestoresPriorRole()
        {
        try (SerializationRole.Scope ignored = SerializationRole.setAndClose(SerializationRole.GRPC))
            {
            assertEquals(SerializationRole.GRPC, SerializationRole.current());
            }

        assertEquals(SerializationRole.UNCLASSIFIED, SerializationRole.current());
        }

    @Test
    public void testNestedSetAndCloseRestoresOuterRole()
        {
        try (SerializationRole.Scope ignored = SerializationRole.setAndClose(SerializationRole.CLUSTER))
            {
            assertEquals(SerializationRole.CLUSTER, SerializationRole.current());
            try (SerializationRole.Scope ignoredNested = SerializationRole.setAndClose(SerializationRole.REST))
                {
                assertEquals(SerializationRole.REST, SerializationRole.current());
                }
            assertEquals(SerializationRole.CLUSTER, SerializationRole.current());
            }

        assertEquals(SerializationRole.UNCLASSIFIED, SerializationRole.current());
        }

    @Test
    public void testThrowingBodyRestoresRole()
        {
        try
            {
            try (SerializationRole.Scope ignored = SerializationRole.setAndClose(SerializationRole.SESSION))
                {
                throw new IllegalStateException("expected");
                }
            }
        catch (IllegalStateException expected)
            {
            // expected
            }

        assertEquals(SerializationRole.UNCLASSIFIED, SerializationRole.current());
        }
    }
