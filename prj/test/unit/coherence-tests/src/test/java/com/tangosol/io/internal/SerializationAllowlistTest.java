/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.tangosol.internal.util.CoherenceModeTestSupport;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectStreamException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import javax.management.Attribute;
import javax.management.BadAttributeValueExpException;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXServiceURL;

import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.LinkRef;
import javax.naming.Reference;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for serialization allowlist filtering.
 *
 * @author Aleks Seovic  2026.04.29
 * @since 26.07
 */
public class SerializationAllowlistTest
    {
    @Test
    public void testDevModeRejectsNonAllowlistedClass()
        {
        withProperties("dev", null, () -> assertEquals(ObjectInputFilter.Status.REJECTED,
                check(java.io.File.class)));
        }

    @Test
    public void testLegacyModeAllowsNonDenylistedClass()
        {
        withProperties("legacy", null, () -> assertEquals(ObjectInputFilter.Status.ALLOWED,
                check(java.io.File.class)));
        }

    @Test
    public void testDenylistWins()
        {
        withProperties("prod", "javax.management.*", () -> assertEquals(ObjectInputFilter.Status.REJECTED,
                check(BadAttributeValueExpException.class)));
        }

    @Test
    public void testLegitimateManagementAndNamingClassesAllowed()
        {
        assertAllowedInDevAndProd(Throwable.class);
        assertAllowedInDevAndProd(Object.class);
        assertAllowedInDevAndProd(Object[].class);
        assertAllowedInDevAndProd(Exception.class);
        assertAllowedInDevAndProd(RuntimeException.class);
        assertAllowedInDevAndProd(IllegalStateException.class);
        assertAllowedInDevAndProd(SecurityException.class);
        assertAllowedInDevAndProd(Error.class);
        assertAllowedInDevAndProd(AssertionError.class);
        assertAllowedInDevAndProd(Enum.class);
        assertAllowedInDevAndProd(StackTraceElement.class);
        assertAllowedInDevAndProd(IOException.class);
        assertAllowedInDevAndProd(ObjectStreamException.class);
        assertAllowedInDevAndProd(InvalidClassException.class);
        assertAllowedInDevAndProd(InetAddress.class);
        assertAllowedInDevAndProd(Inet4Address.class);
        assertAllowedInDevAndProd(Inet6Address.class);
        assertAllowedInDevAndProd(JMXServiceURL.class);
        assertAllowedInDevAndProd(ImmutableDescriptor.class);
        assertAllowedInDevAndProd(DescriptorSupport.class);
        assertAllowedInDevAndProd(MBeanInfo.class);
        assertAllowedInDevAndProd(MBeanAttributeInfo[].class);
        assertAllowedInDevAndProd(MBeanConstructorInfo[].class);
        assertAllowedInDevAndProd(MBeanFeatureInfo.class);
        assertAllowedInDevAndProd(MBeanNotificationInfo[].class);
        assertAllowedInDevAndProd(MBeanOperationInfo[].class);
        assertAllowedInDevAndProd(MBeanParameterInfo[].class);
        assertAllowedInDevAndProd(CompositeDataSupport.class);
        assertAllowedInDevAndProd(Attribute.class);
        assertAllowedInDevAndProd(ObjectName.class);
        assertAllowedInDevAndProd(CompositeName.class);
        assertAllowedInDevAndProd(CompoundName.class);
        }

    @Test
    public void testManagementAndNamingGadgetsDenied()
        {
        assertDeniedInDevAndProd(BadAttributeValueExpException.class);
        assertDeniedInDevAndProd(Reference.class);
        assertDeniedInDevAndProd(LinkRef.class);
        }

    @Test
    public void testProdModeRejectsNonAllowlistedClass()
        {
        withProperties("prod", null, () -> assertEquals(ObjectInputFilter.Status.REJECTED,
                check(java.io.File.class)));
        }

    @Test
    public void testProdModeAcceptsExactConfiguredClass()
        {
        withProperties("prod", java.io.File.class.getName(), () -> assertEquals(ObjectInputFilter.Status.ALLOWED,
                check(java.io.File.class)));
        }

    @Test
    public void testProdModeAcceptsConfiguredPackageWildcard()
        {
        withProperties("prod", "java.io.*", () -> assertEquals(ObjectInputFilter.Status.ALLOWED,
                check(java.io.File.class)));
        }

    @Test
    public void testProdModeAcceptsSyntheticLambdaProxyForAllowedCapturingClass()
        {
        withProperties("prod", "example.TopicTest", () ->
            {
            assertTrue(SerializationAllowlist.isAllowlistedName("example.TopicTest$$Lambda/0x00007800007faa38",
                    true));
            assertTrue(SerializationAllowlist.isAllowlistedName("example.TopicTest$$Lambda$1",
                    true));
            assertFalse(SerializationAllowlist.isAllowlistedName("example.TopicTest$$Lambda/0x00007800007faa38",
                    false));
            assertFalse(SerializationAllowlist.isAllowlistedName("example.OtherTest$$Lambda/0x00007800007faa38",
                    true));
            });
        }

    @Test
    public void testInvalidConfiguredEntryIsDropped()
        {
        withProperties("prod", "not a class name", () -> assertEquals(ObjectInputFilter.Status.REJECTED,
                check(java.io.File.class)));
        }

    private static ObjectInputFilter.Status check(Class<?> clz)
        {
        return DefaultObjectInputFilter.create().checkInput(new TestFilterInfo(clz));
        }

    private static void assertAllowedInDevAndProd(Class<?> clz)
        {
        for (String sMode : new String[] {"dev", "prod"})
            {
            withProperties(sMode, null, () ->
                {
                assertTrue(clz.getName(), SerializationAllowlist.isAllowed(clz));
                assertFalse(clz.getName(), SerializationAllowlist.isDenied(clz));
                assertEquals(clz.getName(), ObjectInputFilter.Status.ALLOWED, check(clz));
                });
            }
        }

    private static void assertDeniedInDevAndProd(Class<?> clz)
        {
        for (String sMode : new String[] {"dev", "prod"})
            {
            withProperties(sMode, clz.getName(), () ->
                {
                assertTrue(clz.getName(), SerializationAllowlist.isDenied(clz));
                assertFalse(clz.getName(), SerializationAllowlist.isAllowed(clz));
                assertEquals(clz.getName(), ObjectInputFilter.Status.REJECTED, check(clz));
                });
            }
        }

    private static void withProperties(String sMode, String sAllowed, Runnable runnable)
        {
        String sModeOld    = System.getProperty("coherence.mode");
        String sAllowedOld = System.getProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        try
            {
            restoreProperty("coherence.mode", sMode);
            CoherenceModeTestSupport.reset();
            restoreProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, sAllowed);
            runnable.run();
            }
        finally
            {
            restoreProperty("coherence.mode", sModeOld);
            CoherenceModeTestSupport.reset();
            restoreProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, sAllowedOld);
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

    private record TestFilterInfo(Class<?> serialClass)
            implements ObjectInputFilter.FilterInfo
        {
        @Override
        public long arrayLength()
            {
            return -1L;
            }

        @Override
        public long depth()
            {
            return 1L;
            }

        @Override
        public long references()
            {
            return 0L;
            }

        @Override
        public long streamBytes()
            {
            return 0L;
            }
        }

    }
