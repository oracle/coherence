/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.security;

import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;

import java.io.IOException;

import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests for {@link SecurityManagerWrapperImpl} and
 * {@link DoAsAction}.
 */
class SecurityManagerWrapperImplTest
    {
    @Test
    void shouldPreserveCheckedExceptionContract()
        {
        SecurityManagerWrapper wrapper   = new TestSecurityManagerWrapper();
        IOException            exception = new IOException("boom");

        PrivilegedActionException thrown = assertThrows(PrivilegedActionException.class,
                () -> wrapper.doPrivileged((PrivilegedExceptionAction<Void>) () ->
                    {
                    throw exception;
                    }));

        assertSame(exception, thrown.getException());
        }

    @Test
    void shouldPreserveRuntimeExceptionContract()
        {
        SecurityManagerWrapper wrapper   = new TestSecurityManagerWrapper();
        IllegalStateException  exception = new IllegalStateException("boom");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> wrapper.doPrivileged((PrivilegedAction<Void>) () ->
                    {
                    throw exception;
                    }));

        assertSame(exception, thrown);
        }

    @Test
    void shouldPreserveErrorContract()
        {
        SecurityManagerWrapper wrapper = new TestSecurityManagerWrapper();
        AssertionError         error   = new AssertionError("boom");

        AssertionError thrown = assertThrows(AssertionError.class,
                () -> wrapper.doPrivileged((PrivilegedAction<Void>) () ->
                    {
                    throw error;
                    }));

        assertSame(error, thrown);
        }

    @Test
    void shouldPreserveDoAsActionRuntimeExceptionContract()
        {
        IllegalStateException exception = new IllegalStateException("boom");
        DoAsAction<Void>      action    = new DoAsAction<>(new Subject(), () ->
            {
            throw exception;
            });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, action::run);

        assertSame(exception, thrown);
        }

    @Test
    void shouldRunDoAsSpecifiedSubject()
        {
        Subject             subject = new Subject();
        DoAsAction<Subject> action  = new DoAsAction<>(subject, Subject::current);

        assertSame(subject, action.run());
        }

    private static class TestSecurityManagerWrapper
            extends SecurityManagerWrapperImpl
        {
        @Override
        public Subject getCurrentSubject()
            {
            return new Subject();
            }
        }
    }
