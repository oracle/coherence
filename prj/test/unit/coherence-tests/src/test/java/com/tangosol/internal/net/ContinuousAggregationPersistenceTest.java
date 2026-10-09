/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.internal.SerializationAllowlist;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.SimpleMapEntry;

import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.aggregator.LongSum;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.GreaterFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;

import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ContinuousAggregationPersistence}.
 *
 * @author Aleks Seovic  2026.09.11
 * @since 26.10
 */
@RunWith(Parameterized.class)
public class ContinuousAggregationPersistenceTest
    {
    @Parameterized.Parameters(name = "security={0}")
    public static Object[] modes()
        {
        return new Object[] {"compatibility", "hardened"};
        }

    public ContinuousAggregationPersistenceTest(String sSecurityMode)
        {
        f_sSecurityMode = sSecurityMode;
        }

    @Before
    public void setUp()
        {
        m_scope = CoherenceModeHelper.securityMode(f_sSecurityMode);
        m_sAllowed = System.getProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        System.clearProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        UnregisteredFilter.s_fMaterialized = false;
        UnregisteredAggregator.s_fMaterialized = false;
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
    public void shouldRoundTripDefinitionAndVersionedState()
        {
        Serializer serializer = new DefaultSerializer();
        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(
                        new GreaterFilter<>(IdentityExtractor.INSTANCE(), 10L),
                        new LongSum<>(IdentityExtractor.INSTANCE()));

        Binary binDefinition = ContinuousAggregationPersistence
                .toDefinitionBinary(definition, serializer);
        assertEquals(definition, ContinuousAggregationPersistence
                .fromDefinitionBinary(binDefinition, serializer));

        Binary binState = ContinuousAggregationPersistence.toStateBinary(
                binDefinition, 7, 11L, 13L,
                new Object[] {Integer.valueOf(2), Long.valueOf(31L)}, serializer);
        ContinuousAggregationPersistence.StateRecord record =
                ContinuousAggregationPersistence.fromStateBinary(
                        binState, binDefinition, 7, 11L, 13L, serializer);
        assertEquals(11L, record.getOwnershipVersion());
        assertEquals(13L, record.getDataVersion());
        assertEquals(2, ((Number) ((Object[]) record.getState())[0]).intValue());
        assertEquals(31L, ((Number) ((Object[]) record.getState())[1]).longValue());
        }

    @Test
    public void shouldRejectMismatchedStateIdentity()
        {
        Serializer serializer    = new DefaultSerializer();
        Binary     binDefinition = new Binary(new byte[] {1, 2, 3});
        Binary     binState      = ContinuousAggregationPersistence.toStateBinary(
                binDefinition, 7, 11L, 13L, Integer.valueOf(2), serializer);

        assertThrows(IllegalArgumentException.class,
                () -> ContinuousAggregationPersistence.fromStateBinary(
                        binState, binDefinition, 8, 11L, 13L, serializer));
        assertThrows(IllegalArgumentException.class,
                () -> ContinuousAggregationPersistence.fromStateBinary(
                        binState, binDefinition, 7, 12L, 13L, serializer));
        assertThrows(IllegalArgumentException.class,
                () -> ContinuousAggregationPersistence.fromStateBinary(
                        binState, binDefinition, 7, 11L, 14L, serializer));
        assertThrows(IllegalArgumentException.class,
                () -> ContinuousAggregationPersistence.fromStateBinary(
                        binState, new Binary(), 7, 11L, 13L, serializer));
        }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shouldRoundTripNestedMaintenanceStateThroughTheServiceSerializer()
        {
        Serializer serializer = new DefaultSerializer();
        GroupAggregator<Integer, Long, Long, Long, Integer> group =
                GroupAggregator.createInstance(IdentityExtractor.INSTANCE(), new Count<>());
        group.accumulate(new SimpleMapEntry<>(1, 10L));
        group.accumulate(new SimpleMapEntry<>(2, 10L));
        group.accumulate(new SimpleMapEntry<>(3, 20L));

        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(null, group);
        Binary binDefinition = ContinuousAggregationPersistence
                .toDefinitionBinary(definition, serializer);
        Binary binState = ContinuousAggregationPersistence.toStateBinary(
                binDefinition, 7, 11L, 13L, group.snapshotState(), serializer);
        Object state = ContinuousAggregationPersistence.fromStateBinary(
                binState, binDefinition, 7, 11L, 13L, serializer).getState();

        ContinuousAggregationState restored = new ContinuousAggregationState();
        assertTrue(restored.restoreState(
                ContinuousAggregationPersistence.fromDefinitionBinary(
                        binDefinition, serializer), state));
        assertEquals(Map.of(10L, 2, 20L, 1), restored.getPartialResult());
        }

    @Test
    public void shouldRejectUnsupportedFormatVersions()
        {
        Serializer serializer    = new DefaultSerializer();
        Binary     binDefinition = ExternalizableHelper.toBinary(new Object[]
            {
            ContinuousAggregationPersistence.DEFINITION_FORMAT_VERSION + 1,
            new GreaterFilter<>(IdentityExtractor.INSTANCE(), 10L),
            new Count<>()
            }, serializer);

        assertThrows(IllegalArgumentException.class,
                () -> ContinuousAggregationPersistence.fromDefinitionBinary(binDefinition, serializer));

        Binary binState = ExternalizableHelper.toBinary(new Object[]
            {
            ContinuousAggregationPersistence.STATE_FORMAT_VERSION + 1,
            binDefinition, 7, 11L, 13L, 1
            }, serializer);

        assertThrows(IllegalArgumentException.class,
                () -> ContinuousAggregationPersistence.fromStateBinary(
                        binState, binDefinition, 7, 11L, 13L, serializer));
        }

    @Test
    public void shouldApplySecurityModeBeforeReadingUnregisteredFilter()
        {
        ContinuousAggregationDefinition definition = new ContinuousAggregationDefinition(
                new UnregisteredFilter(), new Count<>());
        assertDefinitionAdmission(definition, () -> UnregisteredFilter.s_fMaterialized);
        }

    @Test
    public void shouldApplySecurityModeBeforeReadingUnregisteredAggregator()
        {
        ContinuousAggregationDefinition definition = new ContinuousAggregationDefinition(
                null, new UnregisteredAggregator());
        assertDefinitionAdmission(definition, () -> UnregisteredAggregator.s_fMaterialized);
        }

    private void assertDefinitionAdmission(ContinuousAggregationDefinition definition, BooleanSupplier materialized)
        {
        Serializer serializer    = new DefaultSerializer();
        Binary     binDefinition = ContinuousAggregationPersistence.toDefinitionBinary(definition, serializer);

        if ("hardened".equals(f_sSecurityMode))
            {
            // admitting superclass descriptors must not admit an unregistered subclass
            Throwable error = assertThrows(RuntimeException.class,
                    () -> ContinuousAggregationPersistence.fromDefinitionBinary(binDefinition, serializer));
            while (error.getCause() != null)
                {
                error = error.getCause();
                }
            assertTrue(error instanceof InvalidClassException);
            assertFalse(materialized.getAsBoolean());
            }
        else
            {
            assertEquals(definition, ContinuousAggregationPersistence.fromDefinitionBinary(binDefinition, serializer));
            assertTrue(materialized.getAsBoolean());
            }
        }

    /**
     * Unregistered filter whose Java deserialization hook must not run in hardened mode.
     *
     * @author phf  2026.09.30
     * @since 26.10
     */
    private static class UnregisteredFilter
            extends GreaterFilter<Long, Long>
        {
        private UnregisteredFilter()
            {
            super(IdentityExtractor.INSTANCE(), 10L);
            }

        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }

        private static boolean s_fMaterialized;

        private static final long serialVersionUID = 1L;
        }

    /**
     * Unregistered aggregator whose Java deserialization hook must not run in hardened mode.
     *
     * @author phf  2026.09.30
     * @since 26.10
     */
    private static class UnregisteredAggregator
            extends LongSum<Long>
        {
        private UnregisteredAggregator()
            {
            super(IdentityExtractor.INSTANCE());
            }

        @Override
        public InvocableMap.StreamingAggregator<Object, Object, Object, Long> supply()
            {
            return new UnregisteredAggregator();
            }

        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }

        private static boolean s_fMaterialized;

        private static final long serialVersionUID = 1L;
        }

    private CoherenceModeHelper.ModeScope m_scope;

    private String m_sAllowed;

    private final String f_sSecurityMode;
    }
