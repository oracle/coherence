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
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for management notification serialization and exact admission.
 *
 * @author phf  2026.10.14
 * @since 26.10
 */
@RunWith(Parameterized.class)
public class NotificationSerializationTest
    {
    @Parameterized.Parameters(name = "security={0}")
    public static Object[] modes()
        {
        return new Object[] {null, "hardened", "compatibility"};
        }

    public NotificationSerializationTest(String sMode)
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
    public void shouldPreserveProductNotificationState()
        {
        Notification original = new Notification("snapshot.create.end", "Persistence", 7L,
                123456L, "Snapshot created");
        Map<String, Object> data = new HashMap<>();
        data.put("snapshot", "snapshot-1");
        data.put("duration", 42L);
        original.setUserData(data);
        Notification copy = roundTrip(original);
        assertEquals(Notification.class, copy.getClass());
        assertNotificationState(original, copy);
        }

    @Test
    public void shouldRequireExplicitAttributeChangeNotificationRegistration()
        {
        AttributeChangeNotification original = new AttributeChangeNotification("TestEmitter", 7L,
                123456L, "CacheSize changed", "CacheSize", "int", 25, 75);
        assertFalse(SerializationAllowlist.isAllowlisted(AttributeChangeNotification.class));
        if (CoherenceMode.isSecurityHardeningEnabled())
            {
            assertRejected(ExternalizableHelper.toBinary(original));
            }
        else
            {
            assertEquals(AttributeChangeNotification.class, roundTrip(original).getClass());
            }

        System.setProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED,
                AttributeChangeNotification.class.getName());
        AttributeChangeNotification copy = (AttributeChangeNotification) roundTrip(original);
        assertNotificationState(original, copy);
        assertEquals(original.getAttributeName(), copy.getAttributeName());
        assertEquals(original.getAttributeType(), copy.getAttributeType());
        assertEquals(original.getOldValue(), copy.getOldValue());
        assertEquals(original.getNewValue(), copy.getNewValue());
        }

    @Test
    public void shouldRejectUnregisteredNotificationGraphsBeforeReadObject()
        {
        System.setProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED,
                AttributeChangeNotification.class.getName());
        Notification source = new Notification("test", new UnregisteredValue(), 1L);
        Notification userData = new Notification("test", "source", 2L);
        userData.setUserData(new UnregisteredValue());
        for (Notification original : new Notification[] {source, userData,
                new AttributeChangeNotification("source", 3L, 123456L, "changed", "value", "Object",
                        new UnregisteredValue(), "new"),
                new AttributeChangeNotification("source", 4L, 123456L, "changed", "value", "Object",
                        "old", new UnregisteredValue()), new UnregisteredNotification()})
            {
            s_fMaterialized = false;
            Binary binary = ExternalizableHelper.toBinary(original);
            if (CoherenceMode.isSecurityHardeningEnabled())
                {
                assertRejected(binary);
                assertFalse(s_fMaterialized);
                }
            else
                {
                Notification copy = ExternalizableHelper.fromBinary(binary);
                assertEquals(original.getClass(), copy.getClass());
                assertTrue(s_fMaterialized);
                }
            }
        }

    private static Notification roundTrip(Notification value)
        {
        return ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(value));
        }

    private static void assertNotificationState(Notification expected, Notification actual)
        {
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getSource(), actual.getSource());
        assertEquals(expected.getSequenceNumber(), actual.getSequenceNumber());
        assertEquals(expected.getTimeStamp(), actual.getTimeStamp());
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getUserData(), actual.getUserData());
        }

    private static void assertRejected(Binary binary)
        {
        Throwable error = assertThrows(RuntimeException.class, () -> ExternalizableHelper.fromBinary(binary));
        while (error.getCause() != null)
            {
            error = error.getCause();
            }
        assertTrue(error.toString(), error instanceof InvalidClassException);
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
     * Unregistered nested payload whose deserialization must remain filtered.
     *
     * @author phf  2026.10.14
     * @since 26.10
     */
    private static class UnregisteredValue extends UnsupportedOperationException
        {
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }
        }

    /**
     * Unregistered notification subtype used to check exact-type admission.
     *
     * @author phf  2026.10.14
     * @since 26.10
     */
    private static class UnregisteredNotification extends Notification
        {
        private UnregisteredNotification()
            {
            super("test", "source", 5L);
            }

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
