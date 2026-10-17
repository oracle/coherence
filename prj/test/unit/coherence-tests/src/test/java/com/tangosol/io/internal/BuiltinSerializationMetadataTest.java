/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.oracle.coherence.ai.search.BaseQueryResult;
import com.oracle.coherence.ai.search.BinaryQueryResult;
import com.oracle.coherence.ai.search.SimpleQueryResult;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.security.SecurityConfig;
import com.tangosol.io.pof.reflect.SimplePofPath;
import com.tangosol.net.AbstractInvocable;
import com.tangosol.util.Binary;
import com.tangosol.util.CoherenceMode;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.SparseArray;
import com.tangosol.util.aggregator.LongSum;
import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.GreaterFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for exact built-in superclass and state serialization metadata.
 *
 * @author phf  2026.10.14
 * @since 26.10
 */
@RunWith(Parameterized.class)
public class BuiltinSerializationMetadataTest
    {
    @Parameterized.Parameters(name = "security={0}")
    public static Object[] modes()
        {
        return new Object[] {null, "hardened", "compatibility"};
        }

    public BuiltinSerializationMetadataTest(String sMode)
        {
        f_sMode = sMode;
        }

    @Before
    public void setUp()
        {
        m_scope = CoherenceModeHelper.securityMode(f_sMode);
        m_sAllowed = System.getProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        System.setProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, RegisteredInvocable.class.getName());
        }

    @After
    public void tearDown()
        {
        if (m_sAllowed == null)
            {
            System.clearProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
            }
        else
            {
            System.setProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, m_sAllowed);
            }
        m_scope.close();
        }

    @Test
    public void shouldRegisterExactPassiveDescriptors() throws Exception
        {
        for (String sName : new String[]
                {
                "com.oracle.coherence.ai.search.BaseQueryResult",
                "com.tangosol.io.pof.reflect.AbstractPofPath",
                "com.tangosol.net.AbstractInvocable",
                "com.tangosol.util.AbstractLongArray",
                "com.tangosol.util.AbstractSparseArray",
                "com.tangosol.util.AbstractSparseArray$Node",
                "com.tangosol.util.aggregator.AbstractAggregator",
                "com.tangosol.util.aggregator.AbstractLongAggregator",
                "com.tangosol.util.extractor.AbstractExtractor",
                "com.tangosol.util.filter.AbstractQueryRecorderFilter",
                "com.tangosol.util.filter.ComparisonFilter",
                "com.tangosol.util.filter.ExtractorFilter"
                })
            {
            assertTrue(sName, SerializationAllowlist.isExplicitlyAllowlisted(Class.forName(sName)));
            assertFalse(sName, SecurityConfig.current().isExecutable(sName));
            }
        }

    @Test
    public void shouldRoundTripFilterAndAggregatorGraph()
        {
        GreaterFilter<Long, Long> filter = new GreaterFilter<>(IdentityExtractor.INSTANCE(), 10L);
        GreaterFilter<Long, Long> copy = (GreaterFilter<Long, Long>) roundTrip(filter);
        assertTrue(copy.evaluate(11L));
        assertFalse(copy.evaluate(9L));
        assertEquals(LongSum.class, roundTrip(new LongSum<>(IdentityExtractor.INSTANCE())).getClass());
        }

    @Test
    public void shouldRoundTripInvocableAndPofPath()
        {
        assertEquals(RegisteredInvocable.class, roundTrip(new RegisteredInvocable()).getClass());
        SimplePofPath path = new SimplePofPath(new int[] {1, 2});
        assertEquals(path, roundTrip(path));
        }

    @Test
    public void shouldRoundTripPopulatedSparseArray()
        {
        SparseArray<String> values = new SparseArray<>();
        values.set(3L, "three");
        values.set(11L, "eleven");
        SparseArray<?> copy = (SparseArray<?>) roundTrip(values);
        assertEquals("three", copy.get(3L));
        assertEquals("eleven", copy.get(11L));
        assertEquals(2, copy.getSize());
        }

    @Test
    public void shouldPreserveExtractorComparatorState()
        {
        for (boolean fReverse : new boolean[] {false, true})
            {
            Comparator<String> comparator = new ReflectionExtractor<>("length");
            SortedSet<String> values = new TreeSet<>(fReverse ? comparator.reversed() : comparator);
            values.addAll(Arrays.asList("ccc", "a", "bb"));
            SortedSet<String> copy = (SortedSet<String>) roundTrip(values);
            copy.add("dddd");
            assertEquals(fReverse ? Arrays.asList("dddd", "ccc", "bb", "a")
                                  : Arrays.asList("a", "bb", "ccc", "dddd"), new ArrayList<>(copy));
            }
        }

    @Test
    public void shouldRejectUnregisteredSubclassesAndNestedValuesBeforeMaterialization()
        {
        SparseArray<Object> values = new SparseArray<>();
        values.set(3L, new UnregisteredValue());
        for (Object value : new Object[] {new UnregisteredExtractor(), new UnregisteredFilter(),
                new UnregisteredAggregator(), new UnregisteredInvocable(), values})
            {
            s_fMaterialized = false;
            if (CoherenceMode.isSecurityHardeningEnabled())
                {
                Throwable error = assertThrows(RuntimeException.class, () -> roundTrip(value));
                while (error.getCause() != null)
                    {
                    error = error.getCause();
                    }
                assertTrue(error.toString(), error instanceof InvalidClassException);
                assertFalse(s_fMaterialized);
                }
            else
                {
                assertEquals(value.getClass(), roundTrip(value).getClass());
                assertTrue(s_fMaterialized);
                }
            }
        }

    @Test
    public void shouldRoundTripVectorResults()
        {
        Binary binary = new Binary(new byte[] {1, 2, 3});
        List<BaseQueryResult<?, ?>> values = new ArrayList<>();
        values.add(new BinaryQueryResult(0.25, binary, binary));
        values.add(new SimpleQueryResult<>(0.5, "key", "value"));
        List<BaseQueryResult<?, ?>> copy = (List<BaseQueryResult<?, ?>>) roundTrip(values);
        assertEquals(values.size(), copy.size());
        for (int i = 0; i < values.size(); ++i)
            {
            assertEquals(values.get(i).getClass(), copy.get(i).getClass());
            assertEquals(values.get(i).getDistance(), copy.get(i).getDistance(), 0.0);
            assertEquals(values.get(i).getKey(), copy.get(i).getKey());
            assertEquals(values.get(i).getValue(), copy.get(i).getValue());
            }
        BaseQueryResult<?, ?>[] array = values.toArray(new BaseQueryResult[0]);
        BaseQueryResult<?, ?>[] arrayCopy = (BaseQueryResult<?, ?>[]) roundTrip(array);
        assertEquals(binary, arrayCopy[0].getKey());
        assertEquals("value", arrayCopy[1].getValue());
        }

    private Object roundTrip(Object value)
        {
        // a Java-serialized map exercises descriptors for the complete nested graph
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("value", value);
        Map<?, ?> copy = ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(envelope));
        return copy.get("value");
        }

    /**
     * Explicitly registered application invocable used for superclass admission.
     *
     * @author phf  2026.10.14
     * @since 26.10
     */
    public static class RegisteredInvocable extends AbstractInvocable
        {
        @Override
        public void run()
            {
            }
        }

    private static class UnregisteredInvocable extends RegisteredInvocable
        {
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }
        }

    private static class UnregisteredExtractor extends AbstractExtractor<String, Integer>
        {
        @Override
        public Integer extract(String value)
            {
            return value.length();
            }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }
        }

    private static class UnregisteredFilter extends GreaterFilter<Long, Long>
        {
        private UnregisteredFilter()
            {
            super(IdentityExtractor.INSTANCE(), 10L);
            }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }
        }

    private static class UnregisteredAggregator extends LongSum<Long>
        {
        private UnregisteredAggregator()
            {
            super(IdentityExtractor.INSTANCE());
            }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }
        }

    private static class UnregisteredValue implements Serializable
        {
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }
        }

    private final String f_sMode;
    private CoherenceModeHelper.ModeScope m_scope;
    private String m_sAllowed;
    private static boolean s_fMaterialized;
    }
