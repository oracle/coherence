/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.oracle.coherence.testing.util.CoherenceModeHelper;
import com.tangosol.internal.util.security.SecurityConfig;

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
        String sModeOld    = System.getProperty("coherence.mode");
        String sAllowedOld = System.getProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        try
            {
            restoreProperty("coherence.mode", sMode);
            CoherenceModeHelper.reset();
            restoreProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, sAllowed);
            runnable.run();
            }
        finally
            {
            restoreProperty("coherence.mode", sModeOld);
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
