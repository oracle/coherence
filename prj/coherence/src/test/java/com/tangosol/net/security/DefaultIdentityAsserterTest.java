/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.Subject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * Tests for {@link DefaultIdentityAsserter}.
 *
 * @author OpenAI  2026.05.16
 */
public class DefaultIdentityAsserterTest
    {
    @Test
    public void shouldReturnNullForNullToken()
        {
        assertNull(DefaultIdentityAsserter.INSTANCE.assertIdentity(null, null));
        }

    @Test
    public void shouldRejectRawSubjectToken()
        {
        assertThrows(SecurityException.class,
                () -> DefaultIdentityAsserter.INSTANCE.assertIdentity(new Subject(), null));
        }

    @Test
    public void shouldRejectUnsupportedToken()
        {
        assertThrows(SecurityException.class,
                () -> DefaultIdentityAsserter.INSTANCE.assertIdentity("subject=admin", null));
        }

    @Test
    public void shouldNotUseTokenToStringForDiagnostics()
        {
        ToStringProbe.reset();

        assertThrows(SecurityException.class,
                () -> DefaultIdentityAsserter.INSTANCE.assertIdentity(new ToStringProbe(), null));
        assertFalse(ToStringProbe.wasToStringCalled());
        }

    /**
     * Token whose toString records diagnostic misuse.
     */
    public static class ToStringProbe
        {
        @Override
        public String toString()
            {
            CALLED.set(true);
            throw new AssertionError("toString should not be called");
            }

        static void reset()
            {
            CALLED.set(false);
            }

        static boolean wasToStringCalled()
            {
            return CALLED.get();
            }

        private static final AtomicBoolean CALLED = new AtomicBoolean();
        }
    }
