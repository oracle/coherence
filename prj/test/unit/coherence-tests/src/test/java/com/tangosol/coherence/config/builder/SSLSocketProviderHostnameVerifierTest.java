/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import org.junit.Test;

import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for mode-aware SSL hostname verifier defaults.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 26.04
 */
public class SSLSocketProviderHostnameVerifierTest
    {
    @Test
    public void shouldAllowNullVerifierInLegacy()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            provider(null).ensureSessionValidity(rejectingSession(), peerSocket());
            }
        }

    @Test
    public void shouldRejectNullVerifierInDev()
            throws Exception
        {
        assertNullVerifierRejected(CoherenceModeHelper.securityHardened());
        }

    @Test
    public void shouldRejectNullVerifierInProd()
            throws Exception
        {
        assertNullVerifierRejected(CoherenceModeHelper.securityHardened());
        }

    @Test
    public void shouldPreserveCustomVerifier()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            HostnameVerifier verifier = (sHost, session) -> true;

            SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder builder =
                    new SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder();
            builder.setBuilder((resolver, loader, list) -> verifier);

            assertThat(builder.realize(null, null, null), is(sameInstance(verifier)));
            }
        }

    @Test
    public void shouldAllowActionAllowInLegacy()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            HostnameVerifier verifier = allowActionBuilder(false).realize(null, null, null);

            assertThat(verifier.verify("not-a-cert-name.example.com", rejectingSession()), is(true));
            }
        }

    @Test
    public void shouldRejectExplicitActionAllowInDev()
        {
        assertExplicitAllowRejected(CoherenceModeHelper.securityHardened());
        }

    @Test
    public void shouldRejectExplicitActionAllowInProd()
        {
        assertExplicitAllowRejected(CoherenceModeHelper.securityHardened());
        }

    @Test
    public void shouldTreatSystemPropertyDefaultActionAllowAsDefaultInDev()
        {
        assertSystemPropertyDefaultIsDefaultVerifier(CoherenceModeHelper.securityHardened());
        }

    @Test
    public void shouldTreatSystemPropertyDefaultActionAllowAsDefaultInProd()
        {
        assertSystemPropertyDefaultIsDefaultVerifier(CoherenceModeHelper.securityHardened());
        }

    private void assertNullVerifierRejected(CoherenceModeHelper.ModeScope scope)
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = scope)
            {
            try
                {
                provider(null).ensureSessionValidity(rejectingSession(), peerSocket());
                fail("Expected SSLException");
                }
            catch (SSLException expected)
                {
                // expected
                }
            }
        }

    private void assertExplicitAllowRejected(CoherenceModeHelper.ModeScope scope)
        {
        try (CoherenceModeHelper.ModeScope ignored = scope)
            {
            try
                {
                allowActionBuilder(false).realize(null, null, null);
                fail("Expected IllegalArgumentException");
                }
            catch (IllegalArgumentException expected)
                {
                // expected
                }
            }
        }

    private void assertSystemPropertyDefaultIsDefaultVerifier(CoherenceModeHelper.ModeScope scope)
        {
        try (CoherenceModeHelper.ModeScope ignored = scope)
            {
            HostnameVerifier verifier = allowActionBuilder(true).realize(null, null, null);

            assertThat(verifier, instanceOf(SSLSocketProviderDependenciesBuilder.DefaultHostnameVerifier.class));
            assertThat(verifier.verify("not-a-cert-name.example.com", rejectingSession()), is(false));
            }
        }

    private SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder allowActionBuilder(boolean fDefault)
        {
        SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder builder =
                new SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder();
        builder.setAction(SSLSocketProviderDependenciesBuilder.ACTION_ALLOW);
        builder.setSystemPropertyDefault(fDefault);
        return builder;
        }

    private SSLSocketProvider provider(HostnameVerifier verifier)
        {
        return new SSLSocketProvider(new SSLSocketProvider.DefaultDependencies().setHostnameVerifier(verifier));
        }

    private Socket peerSocket()
            throws Exception
        {
        Socket socket = mock(Socket.class);
        when(socket.getInetAddress()).thenReturn(InetAddress.getByName("203.0.113.10"));
        return socket;
        }

    private SSLSession rejectingSession()
        {
        try
            {
            SSLSession session = mock(SSLSession.class);
            when(session.getPeerCertificates()).thenThrow(new SSLPeerUnverifiedException("peer not verified"));
            when(session.getCipherSuite()).thenReturn("TLS_FAKE_WITH_NULL_NULL");
            return session;
            }
        catch (SSLPeerUnverifiedException e)
            {
            throw new IllegalStateException(e);
            }
        }
    }
