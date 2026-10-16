/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.oracle.coherence.testing.util.CoherenceModeHelper;
import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for supported remote exception serialization.
 *
 * @author phf  2026.10.14
 * @since 26.10
 */
@RunWith(Parameterized.class)
public class OperationExceptionSerializationTest
    {
    @Parameterized.Parameters(name = "security={0}")
    public static Object[] modes()
        {
        return new Object[] {null, "hardened", "compatibility"};
        }

    public OperationExceptionSerializationTest(String sMode)
        {
        f_sMode = sMode;
        }

    @Before
    public void setUp()
        {
        m_scope = CoherenceModeHelper.dev();
        m_sSecurityMode = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
        m_sAllowed = System.getProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, f_sMode);
        System.clearProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        CoherenceModeHelper.reset();
        }

    @After
    public void tearDown()
        {
        restoreProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, m_sAllowed);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, m_sSecurityMode);
        m_scope.close();
        }

    @Test
    public void shouldPreserveSupportedExceptionGraphs()
        {
        UnsupportedOperationException operation = new UnsupportedOperationException(
                "operation failed", new IllegalStateException("not ready"));
        operation.addSuppressed(new IOException("suppressed operation failure"));
        InvocationTargetException invocation = new InvocationTargetException(operation, "invocation failed");
        invocation.addSuppressed(new IllegalArgumentException("suppressed invocation failure"));

        for (Throwable original : new Throwable[] {new IllegalArgumentException("argument failed"),
                operation, invocation, new ReflectiveOperationException("reflection failed"),
                new NotSerializableException("application.Value")})
            {
            Throwable copy = ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(original));
            assertExceptionState(original, copy);
            }
        }

    @Test
    public void shouldRejectUnregisteredExceptionGraphsBeforeReadObject()
        {
        UnsupportedOperationException operation = new UnsupportedOperationException("operation failed");
        operation.addSuppressed(new UnregisteredInvocationTargetException());
        InvocationTargetException invocation = new InvocationTargetException(new IllegalStateException());
        invocation.addSuppressed(new UnregisteredOperationException());

        for (Throwable original : new Throwable[] {new UnregisteredOperationException(),
                new UnregisteredInvocationTargetException(),
                new InvocationTargetException(new UnregisteredOperationException()),
                new UnsupportedOperationException(new UnregisteredInvocationTargetException()),
                operation, invocation})
            {
            s_fMaterialized = false;
            Binary binary = ExternalizableHelper.toBinary(original);
            if (CoherenceMode.isSecurityHardeningEnabled())
                {
                Throwable error = assertThrows(RuntimeException.class,
                        () -> ExternalizableHelper.fromBinary(binary));
                while (error.getCause() != null)
                    {
                    error = error.getCause();
                    }
                assertTrue(error.toString(), error instanceof InvalidClassException);
                assertFalse(s_fMaterialized);
                }
            else
                {
                Throwable copy = ExternalizableHelper.fromBinary(binary);
                assertEquals(original.getClass(), copy.getClass());
                assertTrue(s_fMaterialized);
                }
            }
        }

    private static void assertExceptionState(Throwable expected, Throwable actual)
        {
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected.getMessage(), actual.getMessage());
        assertArrayEquals(expected.getStackTrace(), actual.getStackTrace());
        if (expected.getCause() == null)
            {
            assertEquals(null, actual.getCause());
            }
        else
            {
            assertExceptionState(expected.getCause(), actual.getCause());
            }
        assertEquals(expected.getSuppressed().length, actual.getSuppressed().length);
        for (int i = 0; i < expected.getSuppressed().length; ++i)
            {
            assertExceptionState(expected.getSuppressed()[i], actual.getSuppressed()[i]);
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

    /**
     * Unregistered operation exception with a deserialization marker.
     *
     * @author phf  2026.10.14
     * @since 26.10
     */
    private static class UnregisteredOperationException extends UnsupportedOperationException
        {
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }
        }

    /**
     * Unregistered invocation exception with a deserialization marker.
     *
     * @author phf  2026.10.14
     * @since 26.10
     */
    private static class UnregisteredInvocationTargetException extends InvocationTargetException
        {
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }
        }

    private final String f_sMode;
    private CoherenceModeHelper.ModeScope m_scope;
    private String m_sSecurityMode;
    private String m_sAllowed;
    private static boolean s_fMaterialized;
    }
