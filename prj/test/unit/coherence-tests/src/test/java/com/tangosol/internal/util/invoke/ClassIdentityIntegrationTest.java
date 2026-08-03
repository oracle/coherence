/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Wire-level tests for {@link ClassIdentity} validation.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public class ClassIdentityIntegrationTest
    {
    @Test
    public void testLegitimateClassIdentityRoundTrip()
            throws IOException
        {
        ClassIdentity identity = new ClassIdentity(ClassIdentityIntegrationTest.class);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(0);
        identity.writeExternal(buffer.getBufferOutput());

        ClassIdentity result = new ClassIdentity();
        result.readExternal(new ByteArrayReadBuffer(buffer.toByteArray()).getBufferInput());

        assertEquals(identity, result);
        }

    @Test
    public void testDisallowedPackageRejected()
            throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(0);
        new ClassIdentity("com/evil", "Payload", "1").writeExternal(buffer.getBufferOutput());

        ClassIdentity identity = new ClassIdentity();
        IOException     e        = assertThrows(IOException.class,
                () -> identity.readExternal(new ByteArrayReadBuffer(buffer.toByteArray()).getBufferInput()));

        assertEquals("ClassIdentity package is not allowed: com/evil", e.getMessage());
        }
    }
