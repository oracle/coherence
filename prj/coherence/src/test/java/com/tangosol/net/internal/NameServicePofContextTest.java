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
import com.tangosol.io.pof.PortableException;
import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.coherence.component.net.extend.remoteService.RemoteNameService;
import com.tangosol.coherence.component.util.NameService$TcpAcceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator;

import com.tangosol.license.LicenseException;

import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.RequestTimeoutException;

import com.tangosol.net.messaging.ConnectionException;

import com.tangosol.net.security.PermissionInfo;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import org.junit.Test;

import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.security.auth.Subject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
        assertEquals("java.lang.Throwable", NameServicePofContext.INSTANCE.getClassName(0));
        assertEquals("com.tangosol.net.messaging.ConnectionException", NameServicePofContext.INSTANCE.getClassName(3));
        assertEquals("com.tangosol.util.UUID", NameServicePofContext.INSTANCE.getClassName(14));
        assertEquals("com.tangosol.coherence.component.net.Member", NameServicePofContext.INSTANCE.getClassName(160));
        assertEquals("java.security.Principal", NameServicePofContext.INSTANCE.getClassName(900));
        assertEquals("java.net.InetAddress", NameServicePofContext.INSTANCE.getClassName(907));
        assertEquals("java.net.InetSocketAddress", NameServicePofContext.INSTANCE.getClassName(908));
        assertEquals("javax.security.auth.Subject", NameServicePofContext.INSTANCE.getClassName(950));

        assertThrowsIllegalArgument(() -> NameServicePofContext.INSTANCE.getClass(909));
        }

    @Test
    public void shouldPreferExactRegisteredExceptionTypeIds()
        {
        assertEquals(0, NameServicePofContext.INSTANCE.getUserTypeIdentifier(Throwable.class));
        assertEquals(1, NameServicePofContext.INSTANCE.getUserTypeIdentifier(LicenseException.class));
        assertEquals(2, NameServicePofContext.INSTANCE.getUserTypeIdentifier(RequestTimeoutException.class));
        assertEquals(3, NameServicePofContext.INSTANCE.getUserTypeIdentifier(ConnectionException.class));
        assertEquals(4, NameServicePofContext.INSTANCE.getUserTypeIdentifier(RequestIncompleteException.class));
        assertEquals(0, NameServicePofContext.INSTANCE.getUserTypeIdentifier(IllegalStateException.class));
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
    public void shouldRoundTripNameServiceConnectionException()
            throws IOException
        {
        ConnectionException exception = new ConnectionException("connection rejected");

        Object oResult = roundTrip(exception);

        assertTrue(oResult instanceof ConnectionException);
        assertEquals("connection rejected", ((ConnectionException) oResult).getMessage());
        }

    @Test
    public void shouldRoundTripGenericThrowable()
            throws IOException
        {
        Object oResult = roundTrip(new IllegalStateException("connection rejected"));

        assertTrue(oResult instanceof PortableException);
        assertEquals("Portable(" + IllegalStateException.class.getName() + ")", ((PortableException) oResult).getName());
        assertEquals("connection rejected", ((PortableException) oResult).getMessage());
        }

    @Test
    public void shouldReadLegacyConnectionExceptionType()
            throws IOException
        {
        SimplePofContext context = new SimplePofContext();
        context.registerUserType(3, ConnectionException.class, new PortableObjectSerializer(3));

        Object oResult = ExternalizableHelper.fromBinary(
                ExternalizableHelper.toBinary(new ConnectionException("connection rejected"), context),
                NameServicePofContext.INSTANCE);

        assertTrue(oResult instanceof ConnectionException);
        assertEquals("connection rejected", ((ConnectionException) oResult).getMessage());
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
    public void shouldIgnoreRemoteNameServiceIdentityTokens()
        {
        RemoteNameService service   = new RemoteNameService("RemoteNameService", null, false);
        TcpInitiator      initiator = new TcpInitiator("TcpInitiator", null, false);
        PermissionInfo    info      = new PermissionInfo(null, "RemoteNameService", null, null);

        initiator.setParentService(service);

        assertNull(initiator.serializeIdentityToken(info));
        assertNull(service.serializeIdentityToken(info));
        assertNull(service.deserializeIdentityToken(new byte[] {1}));
        }

    @Test
    public void shouldIgnoreServerSideNameServiceIdentityTokenBytes()
        {
        NameService$TcpAcceptor acceptor = new NameService$TcpAcceptor("TcpAcceptor", null, false);
        PermissionInfo          info     = new PermissionInfo(null, "RemoteNameService", null, null);
        byte[]                  abToken  = ExternalizableHelper.toBinary(info).toByteArray();

        assertNull(acceptor.deserializeIdentityToken(abToken));
        assertNull(acceptor.assertIdentityToken(info));
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
