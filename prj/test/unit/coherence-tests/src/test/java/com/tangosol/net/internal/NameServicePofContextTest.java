/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.PofPrincipal;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import org.junit.Test;

import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.security.auth.Subject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for the exact NameService POF registry.
 *
 * @author Aleks Seovic  2026.05.17
 *
 * @since 15.1.2.0
 */
public class NameServicePofContextTest
    {
    @Test
    public void shouldUseExactSimplePofContext()
        {
        assertTrue(NameServicePofContext.INSTANCE instanceof SimplePofContext);
        assertEquals("com.tangosol.util.UUID", NameServicePofContext.INSTANCE.getClassName(14));
        assertEquals("com.tangosol.coherence.component.net.Member", NameServicePofContext.INSTANCE.getClassName(160));
        assertEquals("java.security.Principal", NameServicePofContext.INSTANCE.getClassName(900));
        assertEquals("java.net.InetAddress", NameServicePofContext.INSTANCE.getClassName(907));
        assertEquals("java.net.InetSocketAddress", NameServicePofContext.INSTANCE.getClassName(908));
        assertEquals("javax.security.auth.Subject", NameServicePofContext.INSTANCE.getClassName(950));

        assertThrowsIllegalArgument(() -> NameServicePofContext.INSTANCE.getClass(0));
        assertThrowsIllegalArgument(() -> NameServicePofContext.INSTANCE.getClass(909));
        }

    @Test
    public void shouldRoundTripNameServiceValues()
            throws IOException
        {
        InetAddress       address       = InetAddress.getByName("127.0.0.1");
        InetSocketAddress socketAddress = new InetSocketAddress(address, 7574);
        UUID              uuid          = new UUID();

        assertEquals(address, roundTrip(address));
        assertEquals(socketAddress, roundTrip(socketAddress));
        assertEquals(uuid, roundTrip(uuid));
        assertEquals("NameService/string", roundTrip("NameService/string"));
        }

    @Test
    public void shouldRoundTripIdentityTokenSubject()
            throws IOException
        {
        Subject subject = new Subject();
        subject.getPrincipals().add(new PofPrincipal("CN=Manager,OU=MyUnit"));

        Subject subjectResult = (Subject) roundTrip(subject);
        assertEquals(1, subjectResult.getPrincipals().size());
        assertEquals("CN=Manager,OU=MyUnit", subjectResult.getPrincipals().iterator().next().getName());
        }

    @Test
    public void shouldRejectUnknownUserTypes()
            throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(16);
        WriteBuffer.BufferOutput out = buffer.getBufferOutput();

        out.writePackedInt(1234);
        out.writePackedInt(0);
        out.writePackedInt(-1);

        try
            {
            NameServicePofContext.INSTANCE.deserialize(buffer.getReadBuffer().getBufferInput());
            fail("expected unknown user type rejection");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    private static Object roundTrip(Object oValue)
            throws IOException
        {
        return ExternalizableHelper.fromBinary(
                ExternalizableHelper.toBinary(oValue, NameServicePofContext.INSTANCE),
                NameServicePofContext.INSTANCE);
        }

    private static void assertThrowsIllegalArgument(Runnable runnable)
        {
        try
            {
            runnable.run();
            fail("expected IllegalArgumentException");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }
    }
