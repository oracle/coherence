/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.util.Binary;
import com.tangosol.util.SimpleMapEntry;

import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.aggregator.LongSum;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.GreaterFilter;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ContinuousAggregationPersistence}.
 *
 * @author Aleks Seovic  2026.09.11
 * @since 26.10
 */
public class ContinuousAggregationPersistenceTest
    {
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
    }
