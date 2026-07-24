/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.tangosol.net.security.SecurityHelper;
import com.tangosol.net.security.UsernameAndPassword;

import io.grpc.Attributes;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.Subject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link GrpcAuthentication} and {@link GrpcSecurityContext}.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.04
 */
class GrpcAuthenticationTest
    {
    @BeforeEach
    void setUp()
        {
        m_sModeOld         = System.getProperty(PROP_COHERENCE_MODE);
        m_sSecurityModeOld = System.getProperty(PROP_SECURITY_MODE);
        }

    @AfterEach
    void restore()
        {
        restoreProperty(PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(PROP_SECURITY_MODE, m_sSecurityModeOld);
        resetMode();
        }

    @Test
    void shouldParseBasicCredentials()
        {
        UsernameAndPassword token = GrpcAuthentication.parseBasicCredentials(headers("alice", "secret"));

        assertEquals("alice", token.getUsername());
        assertArrayEquals("secret".toCharArray(), token.getPassword());
        }

    @Test
    void shouldRejectMissingCredentials()
        {
        assertUnauthenticated(() -> GrpcAuthentication.parseBasicCredentials(new Metadata()));
        }

    @Test
    void shouldRejectMalformedCredentials()
        {
        Metadata headers = new Metadata();
        headers.put(GrpcAuthentication.KEY_AUTHORIZATION, "Basic not-base64");

        assertUnauthenticated(() -> GrpcAuthentication.parseBasicCredentials(headers));
        }

    @Test
    void shouldRejectCredentialsWithoutColon()
        {
        Metadata headers = new Metadata();
        headers.put(GrpcAuthentication.KEY_AUTHORIZATION, "Basic "
                + Base64.getEncoder().encodeToString("alice".getBytes(StandardCharsets.US_ASCII)));

        assertUnauthenticated(() -> GrpcAuthentication.parseBasicCredentials(headers));
        }

    @Test
    void shouldRejectCredentialsWithEmptyPassword()
        {
        assertUnauthenticated(() -> GrpcAuthentication.parseBasicCredentials(headers("alice", "")));
        }

    @Test
    void shouldAssertSubject()
        {
        Subject subject = new Subject();
        Subject result = GrpcAuthentication.assertSubject(
                GrpcAuthentication.parseBasicCredentials(headers("alice", "secret")),
                token ->
                    {
                    UsernameAndPassword credentials = (UsernameAndPassword) token;
                    assertEquals("alice", credentials.getUsername());
                    assertArrayEquals("secret".toCharArray(), credentials.getPassword());
                    return subject;
                    });

        assertSame(subject, result);
        }

    @Test
    void shouldMapRejectedIdentityToUnauthenticated()
        {
        UsernameAndPassword token = GrpcAuthentication.parseBasicCredentials(headers("alice", "secret"));
        assertUnauthenticated(() -> GrpcAuthentication.assertSubject(token,
                ignored -> { throw new SecurityException("denied"); }));
        }

    @Test
    void shouldRejectCleartextBasicWhenHardeningIsEnabled()
        {
        mode("prod");
        securityMode("hardened");
        Attributes attributes = Attributes.newBuilder()
                .set(Grpc.TRANSPORT_ATTR_REMOTE_ADDR, new InetSocketAddress("127.0.0.1", 1234))
                .build();

        assertUnauthenticated(() -> GrpcAuthentication.validateBasicTransport(attributes));
        }

    @Test
    void shouldAllowCleartextBasicWhenHardeningIsDisabled()
        {
        Attributes attributes = Attributes.newBuilder()
                .set(Grpc.TRANSPORT_ATTR_REMOTE_ADDR, new InetSocketAddress("127.0.0.1", 1234))
                .build();

        mode("prod");
        assertDoesNotThrow(() -> GrpcAuthentication.validateBasicTransport(attributes));

        mode("dev");
        assertDoesNotThrow(() -> GrpcAuthentication.validateBasicTransport(attributes));
        }

    @Test
    void shouldPreserveGrpcContextAndRunAsSubjectInExecutor()
        {
        Subject                  subject = new Subject();
        AtomicReference<Subject> refCtx  = new AtomicReference<>();
        AtomicReference<Subject> refSec  = new AtomicReference<>();
        Executor                 executor = GrpcSecurityContext.contextAware(Runnable::run);

        GrpcSecurityContext.withSubject(io.grpc.Context.current(), subject).run(() ->
                executor.execute(() ->
                    {
                    refCtx.set(GrpcSecurityContext.getCurrentSubject());
                    refSec.set(SecurityHelper.getCurrentSubject());
                    }));

        assertSame(subject, refCtx.get());
        assertSame(subject, refSec.get());
        }

    @Test
    void shouldExposeRunAsSubjectInGrpcContext()
        {
        Subject                  subject = new Subject();
        AtomicReference<Subject> refCtx  = new AtomicReference<>();
        AtomicReference<Subject> refSec  = new AtomicReference<>();

        GrpcSecurityContext.runAs(subject, () ->
            {
            refCtx.set(GrpcSecurityContext.getCurrentSubject());
            refSec.set(SecurityHelper.getCurrentSubject());
            });

        assertSame(subject, refCtx.get());
        assertSame(subject, refSec.get());
        }

    @Test
    void shouldExposeSupplyAsSubjectInGrpcContext()
        {
        Subject                  subject = new Subject();
        AtomicReference<Subject> refCtx  = new AtomicReference<>();
        AtomicReference<Subject> refSec  = new AtomicReference<>();
        Object                   result  = new Object();

        assertSame(result, GrpcSecurityContext.supplyAs(subject, () ->
            {
            refCtx.set(GrpcSecurityContext.getCurrentSubject());
            refSec.set(SecurityHelper.getCurrentSubject());
            return result;
            }));

        assertSame(subject, refCtx.get());
        assertSame(subject, refSec.get());
        }

    private static Metadata headers(String sUser, String sPassword)
        {
        Metadata headers = new Metadata();
        headers.put(GrpcAuthentication.KEY_AUTHORIZATION, "Basic "
                + Base64.getEncoder().encodeToString((sUser + ':' + sPassword).getBytes(StandardCharsets.US_ASCII)));
        return headers;
        }

    private static void assertUnauthenticated(ThrowingRunnable action)
        {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, action::run);
        assertEquals(Status.UNAUTHENTICATED.getCode(), e.getStatus().getCode());
        assertEquals("invalid authentication credentials", e.getStatus().getDescription());
        }

    private void mode(String sMode)
        {
        restoreProperty(PROP_COHERENCE_MODE, sMode);
        resetMode();
        }

    private void securityMode(String sSecurityMode)
        {
        restoreProperty(PROP_SECURITY_MODE, sSecurityMode);
        resetMode();
        }

    private static void resetMode()
        {
        try
            {
            Method method = Class.forName(COHERENCE_MODE_CLASS).getDeclaredMethod("resetForTesting");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (ReflectiveOperationException | RuntimeException e)
            {
            throw new IllegalStateException("Unable to reset memoized Coherence mode", e);
            }
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    private static final String PROP_COHERENCE_MODE = "coherence.mode";

    private static final String PROP_SECURITY_MODE = "coherence.security.mode";

    private static final String COHERENCE_MODE_CLASS = "com.tangosol.internal.util.CoherenceMode";

    private String m_sModeOld;

    private String m_sSecurityModeOld;

    @FunctionalInterface
    private interface ThrowingRunnable
        {
        void run();
        }
    }
