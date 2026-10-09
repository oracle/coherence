/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.oracle.coherence.testing.util.CoherenceModeHelper;
import com.oracle.coherence.ai.search.BaseQueryResult;
import com.oracle.coherence.ai.search.BinaryQueryResult;
import com.oracle.coherence.ai.search.SimpleQueryResult;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.SecurityConfig;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Binary;
import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.function.Remote;
import com.tangosol.util.stream.RemoteCollector;
import com.tangosol.util.stream.RemoteCollectors;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.BadAttributeValueExpException;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXServiceURL;

import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.LinkRef;
import javax.naming.Reference;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
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
    public void testCompatibilityModeAllowsNonDenylistedClass()
        {
        withProperties("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null,
                () -> assertEquals(ObjectInputFilter.Status.ALLOWED,
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
        assertAllowedInDevAndProd(IllegalArgumentException.class);
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
    public void testTriggerRollbackExceptionRoundTrip()
        {
        for (String sMode : new String[] {null, "dev", "prod"})
            {
            for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED})
                {
                withProperties(sMode, sSecurityMode, null, () ->
                    {
                    IllegalArgumentException original = new IllegalArgumentException("Trigger rejected value",
                            new IllegalStateException("rollback"));
                    original.addSuppressed(new IOException("suppressed"));

                    IllegalArgumentException copy = ExternalizableHelper.fromBinary(
                            ExternalizableHelper.toBinary(original));

                    assertEquals(original.getMessage(), copy.getMessage());
                    assertEquals(IllegalStateException.class, copy.getCause().getClass());
                    assertEquals("rollback", copy.getCause().getMessage());
                    assertEquals(1, copy.getSuppressed().length);
                    assertEquals(IOException.class, copy.getSuppressed()[0].getClass());
                    assertEquals("suppressed", copy.getSuppressed()[0].getMessage());
                    assertArrayEquals(original.getStackTrace(), copy.getStackTrace());
                    });
                }
            }
        }

    @Test
    public void testOperationExceptionsRoundTrip()
        {
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED,
                CoherenceMode.SECURITY_MODE_COMPATIBILITY})
            {
            withProperties("dev", sSecurityMode, null, () ->
                {
                UnsupportedOperationException operation = new UnsupportedOperationException(
                        "operation failed", new IllegalStateException("not ready"));
                operation.addSuppressed(new IOException("suppressed operation failure"));
                InvocationTargetException invocation = new InvocationTargetException(operation, "invocation failed");
                invocation.addSuppressed(new IllegalArgumentException("suppressed invocation failure"));

                for (Throwable original : new Throwable[] {operation, invocation})
                    {
                    Throwable copy = ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(original));
                    assertExceptionState(original, copy);
                    }
                });
            }
        }

    @Test
    public void testOperationExceptionGraphsRejectUnregisteredTypes()
        {
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED,
                CoherenceMode.SECURITY_MODE_COMPATIBILITY})
            {
            withProperties("dev", sSecurityMode, null, () ->
                {
                UnsupportedOperationException operation = new UnsupportedOperationException("operation failed");
                operation.addSuppressed(new UnregisteredInvocationTargetException());
                InvocationTargetException invocation = new InvocationTargetException(new IllegalStateException());
                invocation.addSuppressed(new UnregisteredOperationException());

                for (Throwable original : new Throwable[]
                        {
                        new UnregisteredOperationException(), new UnregisteredInvocationTargetException(),
                        new InvocationTargetException(new UnregisteredOperationException()),
                        new UnsupportedOperationException(new UnregisteredInvocationTargetException()),
                        operation, invocation
                        })
                    {
                    s_fUnregisteredExceptionRead = false;
                    Binary binary = ExternalizableHelper.toBinary(original);
                    if (CoherenceMode.SECURITY_MODE_COMPATIBILITY.equals(sSecurityMode))
                        {
                        Throwable copy = ExternalizableHelper.fromBinary(binary);
                        assertEquals(original.getClass(), copy.getClass());
                        assertTrue(s_fUnregisteredExceptionRead);
                        }
                    else
                        {
                        Throwable error = assertThrows(RuntimeException.class,
                                () -> ExternalizableHelper.fromBinary(binary));
                        while (error.getCause() != null)
                            {
                            error = error.getCause();
                            }
                        assertTrue(error.toString(), error instanceof InvalidClassException);
                        assertFalse(s_fUnregisteredExceptionRead);
                        }
                    }
                });
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
        for (int i = 0; i < expected.getSuppressed().length; i++)
            {
            assertExceptionState(expected.getSuppressed()[i], actual.getSuppressed()[i]);
            }
        }

    @Test
    public void testNotificationRoundTrip()
        {
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED,
                CoherenceMode.SECURITY_MODE_COMPATIBILITY})
            {
            withProperties("dev", sSecurityMode, null, () ->
                {
                Notification original = new Notification("snapshot.create.end", "Persistence", 7L,
                        123456L, "Snapshot created");
                original.setUserData(Map.of("snapshot", "snapshot-1", "duration", 42L));

                Notification copy = ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(original));

                assertEquals(Notification.class, copy.getClass());
                assertEquals(original.getType(), copy.getType());
                assertEquals(original.getSource(), copy.getSource());
                assertEquals(original.getSequenceNumber(), copy.getSequenceNumber());
                assertEquals(original.getTimeStamp(), copy.getTimeStamp());
                assertEquals(original.getMessage(), copy.getMessage());
                assertEquals(original.getUserData(), copy.getUserData());
                });
            }
        }

    @Test
    public void testAttributeChangeNotificationRequiresRegistration()
        {
        AttributeChangeNotification original = new AttributeChangeNotification("TestEmitter", 7L,
                123456L, "CacheSize changed", "CacheSize", "int", 25, 75);

        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED,
                CoherenceMode.SECURITY_MODE_COMPATIBILITY})
            {
            withProperties("dev", sSecurityMode, null, () ->
                {
                Binary binary = ExternalizableHelper.toBinary(original);
                if (CoherenceMode.SECURITY_MODE_COMPATIBILITY.equals(sSecurityMode))
                    {
                    assertEquals(AttributeChangeNotification.class,
                            ExternalizableHelper.fromBinary(binary).getClass());
                    }
                else
                    {
                    assertRejectedSerialization(binary);
                    }
                });

            withProperties("dev", sSecurityMode, AttributeChangeNotification.class.getName(), () ->
                {
                AttributeChangeNotification copy = ExternalizableHelper.fromBinary(
                        ExternalizableHelper.toBinary(original));

                assertEquals(original.getType(), copy.getType());
                assertEquals(original.getSource(), copy.getSource());
                assertEquals(original.getSequenceNumber(), copy.getSequenceNumber());
                assertEquals(original.getTimeStamp(), copy.getTimeStamp());
                assertEquals(original.getMessage(), copy.getMessage());
                assertEquals(original.getAttributeName(), copy.getAttributeName());
                assertEquals(original.getAttributeType(), copy.getAttributeType());
                assertEquals(original.getOldValue(), copy.getOldValue());
                assertEquals(original.getNewValue(), copy.getNewValue());
                });
            }
        }

    @Test
    public void testNotificationGraphsRejectUnregisteredTypes()
        {
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED,
                CoherenceMode.SECURITY_MODE_COMPATIBILITY})
            {
            withProperties("dev", sSecurityMode, AttributeChangeNotification.class.getName(), () ->
                {
                Notification source = new Notification("test", new UnregisteredOperationException(), 1L);
                Notification userData = new Notification("test", "source", 2L);
                userData.setUserData(new UnregisteredOperationException());

                for (Notification original : new Notification[]
                        {
                        source, userData,
                        new AttributeChangeNotification("source", 3L, 123456L, "changed", "value", "Object",
                                new UnregisteredOperationException(), "new"),
                        new AttributeChangeNotification("source", 4L, 123456L, "changed", "value", "Object",
                                "old", new UnregisteredOperationException())
                        })
                    {
                    s_fUnregisteredExceptionRead = false;
                    Binary binary = ExternalizableHelper.toBinary(original);
                    if (CoherenceMode.SECURITY_MODE_COMPATIBILITY.equals(sSecurityMode))
                        {
                        Notification copy = ExternalizableHelper.fromBinary(binary);
                        assertEquals(original.getClass(), copy.getClass());
                        assertTrue(s_fUnregisteredExceptionRead);
                        }
                    else
                        {
                        assertRejectedSerialization(binary);
                        assertFalse(s_fUnregisteredExceptionRead);
                        }
                    }
                });
            }
        }

    private static void assertRejectedSerialization(Binary binary)
        {
        Throwable error = assertThrows(RuntimeException.class, () -> ExternalizableHelper.fromBinary(binary));
        while (error.getCause() != null)
            {
            error = error.getCause();
            }
        assertTrue(error.toString(), error instanceof InvalidClassException);
        }

    @Test
    public void testOrderedGroupingResultRoundTrip()
        {
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED,
                CoherenceMode.SECURITY_MODE_COMPATIBILITY})
            {
            withProperties("dev", sSecurityMode, null, () ->
                {
                for (boolean fReverse : new boolean[] {false, true})
                    {
                    Comparator<String> comparator = new ReflectionExtractor<>("length");
                    SortedSet<String> values = new TreeSet<>(fReverse ? comparator.reversed() : comparator);
                    values.addAll(List.of("ccc", "a", "bb"));
                    Map<String, SortedSet<String>> original = new HashMap<>();
                    original.put("group", values);

                    Map<String, SortedSet<String>> copy = ExternalizableHelper.fromBinary(
                            ExternalizableHelper.toBinary(original));
                    SortedSet<String> copiedValues = copy.get("group");
                    copiedValues.add("dddd");
                    assertEquals(fReverse ? List.of("dddd", "ccc", "bb", "a")
                                          : List.of("a", "bb", "ccc", "dddd"),
                            new ArrayList<>(copiedValues));
                    }
                });
            }
        }

    @Test
    public void testGroupingCollectorRoundTrip()
        {
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED,
                CoherenceMode.SECURITY_MODE_COMPATIBILITY})
            {
            withProperties("dev", sSecurityMode, null, () ->
                {
                assertGroupingCollectorRoundTrip(RemoteCollectors.groupingBy(
                        new ReflectionExtractor<String, Integer>("length"),
                        RemoteCollectors.toSortedSet(Remote.Comparator.<String>naturalOrder())));
                });
            }
        }

    @Test
    public void testGroupingCollectorOptionalResultRoundTrip()
        {
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED,
                CoherenceMode.SECURITY_MODE_COMPATIBILITY})
            {
            withProperties("dev", sSecurityMode, null, () ->
                {
                Map<Integer, Optional<String>> original = List.of("a", "bb", "cc").stream().collect(
                        RemoteCollectors.groupingBy(new ReflectionExtractor<String, Integer>("length"),
                                RemoteCollectors.maxBy(Remote.Comparator.<String>naturalOrder())));
                Map<Integer, Optional<String>> copy = ExternalizableHelper.fromBinary(
                        ExternalizableHelper.toBinary(original));

                assertEquals(Map.of(1, Optional.of("a"), 2, Optional.of("cc")), copy);
                });
            }
        }

    @Test
    public void testNotSerializableExceptionRoundTrip()
        {
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED,
                CoherenceMode.SECURITY_MODE_COMPATIBILITY})
            {
            withProperties("dev", sSecurityMode, null, () ->
                {
                NotSerializableException original = new NotSerializableException("java.util.Optional");
                NotSerializableException copy = ExternalizableHelper.fromBinary(
                        ExternalizableHelper.toBinary(original));

                assertEquals(NotSerializableException.class, copy.getClass());
                assertEquals(original.getMessage(), copy.getMessage());
                assertArrayEquals(original.getStackTrace(), copy.getStackTrace());
                });
            }
        }

    @Test
    public void testExtractorSubclassIsNotImplicitlyAllowed()
        {
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED})
            {
            withProperties("dev", sSecurityMode, null, () ->
                {
                assertEquals(ObjectInputFilter.Status.REJECTED, check(UnregisteredExtractor.class));
                RuntimeException error = assertThrows(RuntimeException.class,
                        () -> ExternalizableHelper.fromBinary(
                                ExternalizableHelper.toBinary(new UnregisteredExtractor())));
                Throwable cause = error;
                while (cause.getCause() != null)
                    {
                    cause = cause.getCause();
                    }
                assertTrue(cause.toString(), cause instanceof InvalidClassException);
                });
            }
        }

    @Test
    public void testVectorQueryResultsRoundTrip()
        {
        for (String sMode : new String[] {null, "dev", "prod"})
            {
            for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED})
                {
                withProperties(sMode, sSecurityMode, null, () ->
                    {
                    Binary binary = new Binary(new byte[] {1, 2, 3});
                    List<BaseQueryResult<?, ?>> results = new ArrayList<>();
                    results.add(new BinaryQueryResult(0.25, binary, binary));
                    results.add(new SimpleQueryResult<>(0.5, "key", "value"));
                    List<BaseQueryResult<?, ?>> copy = ExternalizableHelper.fromBinary(
                            ExternalizableHelper.toBinary(results));

                    assertEquals(results.size(), copy.size());
                    for (int i = 0; i < results.size(); i++)
                        {
                        assertEquals(results.get(i).getClass(), copy.get(i).getClass());
                        assertEquals(results.get(i).getDistance(), copy.get(i).getDistance(), 0.0);
                        assertEquals(results.get(i).getKey(), copy.get(i).getKey());
                        assertEquals(results.get(i).getValue(), copy.get(i).getValue());
                        }

                    BaseQueryResult<?, ?>[] array = results.toArray(new BaseQueryResult[0]);
                    BaseQueryResult<?, ?>[] arrayCopy = ExternalizableHelper.fromBinary(
                            ExternalizableHelper.toBinary(array));
                    assertEquals(array.length, arrayCopy.length);
                    assertEquals(binary, arrayCopy[0].getKey());
                    assertEquals("value", arrayCopy[1].getValue());
                    });
                }
            }
        }

    @Test
    public void testRollbackExceptionSubclassIsNotImplicitlyAllowed()
        {
        IllegalArgumentException exception = new NumberFormatException("unregistered");
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_HARDENED})
            {
            withProperties("dev", sSecurityMode, null, () ->
                {
                assertEquals(ObjectInputFilter.Status.REJECTED, check(exception.getClass()));
                RuntimeException error = assertThrows(RuntimeException.class,
                        () -> ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(exception)));
                Throwable cause = error;
                while (cause.getCause() != null)
                    {
                    cause = cause.getCause();
                    }
                assertTrue(cause.toString(), cause instanceof InvalidClassException);
                });
            }
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
    public void testProdModeAcceptsCoherenceGeneratedLambdaForAllowedCapturingClass()
        {
        withProperties("prod", "com.tangosol.util.function.Remote$Function", () ->
            {
            assertTrue(SerializationAllowlist.isAllowlistedName(
                    "com.tangosol.util.function.Remote$Function$lambda$identity$1cd84d30$1$0"
                            + ":275DA13E2DD504E9E1E2763445483A96",
                    true));
            assertFalse(SerializationAllowlist.isAllowlistedName(
                    "com.tangosol.util.function.Remote$Function$lambda$identity$1cd84d30$1$0"
                            + ":275DA13E2DD504E9E1E2763445483A96",
                    false));
            assertFalse(SerializationAllowlist.isAllowlistedName(
                    "example.Other$Function$lambda$identity$1cd84d30$1$0"
                            + ":275DA13E2DD504E9E1E2763445483A96",
                    true));
            });
        }

    @Test
    public void testProdModeAcceptsStreamPipelineGeneratedLambdaFromSecurityConfig()
        {
        withProperties("prod", null, () ->
            {
            assertGeneratedLambdaAllowed("com.tangosol.internal.util.stream.ReferencePipeline");
            assertGeneratedLambdaAllowed("com.tangosol.internal.util.stream.IntPipeline");
            assertGeneratedLambdaAllowed("com.tangosol.internal.util.stream.LongPipeline");
            assertGeneratedLambdaAllowed("com.tangosol.internal.util.stream.DoublePipeline");
            assertGeneratedLambdaAllowed("com.tangosol.internal.util.processor.CacheProcessors");
            assertGeneratedLambdaAllowed("com.tangosol.util.InvocableMap");
            assertGeneratedLambdaAllowed("com.tangosol.util.InvocableMap$Entry");
            assertGeneratedLambdaAllowed("com.tangosol.util.stream.RemoteCollectors");
            assertGeneratedLambdaAllowed("com.tangosol.util.stream.RemoteStream");
            assertGeneratedLambdaAllowed("com.tangosol.util.stream.RemoteIntStream");
            assertGeneratedLambdaAllowed("com.tangosol.util.stream.RemoteLongStream");
            assertGeneratedLambdaAllowed("com.tangosol.util.stream.RemoteDoubleStream");
            });
        }

    @Test
    public void testProdModeAcceptsPublicApiGeneratedLambdaFromSecurityConfig()
        {
        withProperties("prod", null, () ->
            {
            for (String sCapturer : new String[]
                    {
                    "com.tangosol.util.Aggregators",
                    "com.tangosol.util.Extractors",
                    "com.tangosol.util.Filter",
                    "com.tangosol.util.Filters",
                    "com.tangosol.util.Processors",
                    "com.tangosol.util.ValueExtractor",
                    "com.tangosol.util.function.Remote",
                    "com.tangosol.util.function.Remote$BiConsumer",
                    "com.tangosol.util.function.Remote$BiFunction",
                    "com.tangosol.util.function.Remote$BiPredicate",
                    "com.tangosol.util.function.Remote$BinaryOperator",
                    "com.tangosol.util.function.Remote$BooleanSupplier",
                    "com.tangosol.util.function.Remote$Callable",
                    "com.tangosol.util.function.Remote$Comparator",
                    "com.tangosol.util.function.Remote$Consumer",
                    "com.tangosol.util.function.Remote$DoubleBinaryOperator",
                    "com.tangosol.util.function.Remote$DoubleConsumer",
                    "com.tangosol.util.function.Remote$DoubleFunction",
                    "com.tangosol.util.function.Remote$DoublePredicate",
                    "com.tangosol.util.function.Remote$DoubleSupplier",
                    "com.tangosol.util.function.Remote$DoubleToIntFunction",
                    "com.tangosol.util.function.Remote$DoubleToLongFunction",
                    "com.tangosol.util.function.Remote$DoubleUnaryOperator",
                    "com.tangosol.util.function.Remote$Function",
                    "com.tangosol.util.function.Remote$IntBinaryOperator",
                    "com.tangosol.util.function.Remote$IntConsumer",
                    "com.tangosol.util.function.Remote$IntFunction",
                    "com.tangosol.util.function.Remote$IntPredicate",
                    "com.tangosol.util.function.Remote$IntSupplier",
                    "com.tangosol.util.function.Remote$IntToDoubleFunction",
                    "com.tangosol.util.function.Remote$IntToLongFunction",
                    "com.tangosol.util.function.Remote$IntUnaryOperator",
                    "com.tangosol.util.function.Remote$LongBinaryOperator",
                    "com.tangosol.util.function.Remote$LongConsumer",
                    "com.tangosol.util.function.Remote$LongFunction",
                    "com.tangosol.util.function.Remote$LongPredicate",
                    "com.tangosol.util.function.Remote$LongSupplier",
                    "com.tangosol.util.function.Remote$LongToDoubleFunction",
                    "com.tangosol.util.function.Remote$LongToIntFunction",
                    "com.tangosol.util.function.Remote$LongUnaryOperator",
                    "com.tangosol.util.function.Remote$ObjDoubleConsumer",
                    "com.tangosol.util.function.Remote$ObjIntConsumer",
                    "com.tangosol.util.function.Remote$ObjLongConsumer",
                    "com.tangosol.util.function.Remote$Predicate",
                    "com.tangosol.util.function.Remote$Runnable",
                    "com.tangosol.util.function.Remote$Supplier",
                    "com.tangosol.util.function.Remote$ToBigDecimalFunction",
                    "com.tangosol.util.function.Remote$ToComparableFunction",
                    "com.tangosol.util.function.Remote$ToDoubleBiFunction",
                    "com.tangosol.util.function.Remote$ToDoubleFunction",
                    "com.tangosol.util.function.Remote$ToIntBiFunction",
                    "com.tangosol.util.function.Remote$ToIntFunction",
                    "com.tangosol.util.function.Remote$ToLongBiFunction",
                    "com.tangosol.util.function.Remote$ToLongFunction",
                    "com.tangosol.util.function.Remote$UnaryOperator"
                    })
                {
                assertGeneratedLambdaAllowed(sCapturer);
                }
            });
        }

    @Test
    public void testProdModeAcceptsCoherenceGeneratedMethodReferenceForAllowedOwner()
        {
        withProperties("prod", null, () ->
            {
            assertGeneratedMethodReferenceAllowed(
                    "lambda.java.lang.CharSequence$length$1234567890ABCDEF1234567890ABCDEF");
            assertGeneratedMethodReferenceAllowed("lambda.java.lang.Class$cast$1234567890ABCDEF1234567890ABCDEF");
            assertGeneratedMethodReferenceAllowed(
                    "com.tangosol.internal.util.collection.PortableList$<init>$9FCED6064C77BD9F799786EE35CC174C");
            assertGeneratedMethodReferenceAllowed(
                    "com.tangosol.internal.util.collection.PortableMap$<init>$EB2BC68444595CC9B86C2E950997BE8C");
            assertGeneratedMethodReferenceAllowed("lambda.java.lang.Long$sum$EB695C23889E6AE7284515643B47FEAF");
            assertGeneratedMethodReferenceAllowed("lambda.java.lang.Math$max$2C34D546195A7B83B74C368473DAFF71");
            assertGeneratedMethodReferenceAllowed(
                    "lambda.java.util.Optional$ofNullable$1E36B103F62709D17AFE67DC5B8B50FE");
            assertGeneratedMethodReferenceAllowed(
                    "com.tangosol.util.InvocableMap$Entry$getValue$3F46EE932750B15CCCCF9860A154C1BC");
            assertDirectlyAllowed("com.tangosol.util.WrapperCollections$AbstractWrapperCollection");
            assertDirectlyAllowed("com.tangosol.util.WrapperCollections$AbstractWrapperList");
            assertDirectlyAllowed("com.tangosol.util.WrapperCollections$AbstractWrapperMap");
            assertDirectlyAllowed("com.tangosol.util.WrapperCollections$AbstractWrapperSet");
            assertDirectlyAllowed("com.tangosol.util.WrapperCollections$AbstractWrapperSortedMap");
            assertDirectlyAllowed("com.tangosol.util.WrapperCollections$AbstractWrapperSortedSet");
            });
        }

    @Test
    public void testRestGeneratedPartialNamesAreNotAllowlistedByPrefix()
        {
        withProperties("prod", null, () ->
            {
            assertFalse(SerializationAllowlist.isAllowlistedName(
                    "com.tangosol.coherence.rest.util.gen.partial.Person_1234567890",
                    false));
            assertFalse(SerializationAllowlist.isAllowlistedName(
                    "com.tangosol.coherence.rest.util.gen.partial.Person_1234567890",
                    true));
            });
        }

    @Test
    public void testProdModeRejectsCoherenceGeneratedMethodReferenceWithoutAllowedOwner()
        {
        withProperties("prod", "example.Allowed", () ->
            {
            assertTrue(SerializationAllowlist.isAllowlistedName(
                    "example.Allowed$create$1234567890ABCDEF1234567890ABCDEF",
                    true));
            assertFalse(SerializationAllowlist.isAllowlistedName(
                    "example.Allowed$create$1234567890ABCDEF1234567890ABCDEF",
                    false));
            assertFalse(SerializationAllowlist.isAllowlistedName(
                    "example.Other$create$1234567890ABCDEF1234567890ABCDEF",
                    true));
            assertFalse(SerializationAllowlist.isAllowlistedName(
                    "example.Allowed$create$not-a-version",
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

    private static <A> void assertGroupingCollectorRoundTrip(
            RemoteCollector<String, A, Map<Integer, SortedSet<String>>> collector)
        {
        A partial = collector.supplier().get();
        for (String value : List.of("a", "cc", "bb"))
            {
            collector.accumulator().accept(partial, value);
            }

        A copiedPartial = ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(partial));
        collector.accumulator().accept(copiedPartial, "aa");
        Map<Integer, SortedSet<String>> result = collector.finisher().apply(copiedPartial);
        Map<Integer, SortedSet<String>> copy = ExternalizableHelper.fromBinary(
                ExternalizableHelper.toBinary(result));

        assertEquals(2, copy.size());
        assertEquals(List.of("a"), new ArrayList<>(copy.get(1)));
        assertEquals(List.of("aa", "bb", "cc"), new ArrayList<>(copy.get(2)));
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

    private static void assertGeneratedLambdaAllowed(String sCapturingClass)
        {
        assertTrue(sCapturingClass, SecurityConfig.current().contains(sCapturingClass));
        assertTrue(SerializationAllowlist.isAllowlistedName(
                sCapturingClass + "$lambda$unordered$4c7fec84$1$0:393B44F020A1FBFA84C97EDF36061AC0",
                true));
        assertFalse(SerializationAllowlist.isAllowlistedName(
                sCapturingClass + "$lambda$unordered$4c7fec84$1$0:393B44F020A1FBFA84C97EDF36061AC0",
                false));
        }

    private static void assertGeneratedMethodReferenceAllowed(String sName)
        {
        assertTrue(sName, SerializationAllowlist.isAllowlistedName(sName, true));
        assertFalse(sName, SerializationAllowlist.isAllowlistedName(sName, false));
        }

    private static void assertDirectlyAllowed(String sName)
        {
        assertTrue(sName, SerializationAllowlist.isAllowlistedName(sName, false));
        }

    private static void withProperties(String sMode, String sAllowed, Runnable runnable)
        {
        withProperties(sMode, CoherenceMode.SECURITY_MODE_HARDENED, sAllowed, runnable);
        }

    private static void withProperties(String sMode, String sSecurityMode, String sAllowed, Runnable runnable)
        {
        String sModeOld    = System.getProperty("coherence.mode");
        String sSecurityModeOld = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
        String sAllowedOld = System.getProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        try
            {
            restoreProperty("coherence.mode", sMode);
            restoreProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
            CoherenceModeHelper.reset();
            restoreProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, sAllowed);
            runnable.run();
            }
        finally
            {
            restoreProperty("coherence.mode", sModeOld);
            restoreProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityModeOld);
            CoherenceModeHelper.reset();
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

    /**
     * Unregistered subclass used to verify exact extractor allowlisting.
     *
     * @author phf  2026.09.29
     * @since 26.10
     */
    public static class UnregisteredExtractor
            extends AbstractExtractor<String, Integer>
        {
        @Override
        public Integer extract(String value)
            {
            return value.length();
            }
        }

    /**
     * Unregistered operation exception with a deserialization marker.
     *
     * @author phf  2026.09.30
     * @since 26.10
     */
    private static class UnregisteredOperationException
            extends UnsupportedOperationException
        {
        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            s_fUnregisteredExceptionRead = true;
            in.defaultReadObject();
            }
        }

    /**
     * Unregistered invocation exception with a deserialization marker.
     *
     * @author phf  2026.09.30
     * @since 26.10
     */
    private static class UnregisteredInvocationTargetException
            extends InvocationTargetException
        {
        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            s_fUnregisteredExceptionRead = true;
            in.defaultReadObject();
            }
        }

    private static boolean s_fUnregisteredExceptionRead;

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
