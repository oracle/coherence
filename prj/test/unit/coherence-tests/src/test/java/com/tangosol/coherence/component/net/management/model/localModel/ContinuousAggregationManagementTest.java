/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.management.model.localModel;

import com.tangosol.coherence.component.manageable.modelAdapter.StorageManagerMBean;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage;

import com.tangosol.internal.net.ContinuousAggregationSupport;
import com.tangosol.internal.net.MessageComponent;
import com.tangosol.internal.util.VersionHelper;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.WrapperBufferInput;
import com.tangosol.io.WrapperBufferOutput;

import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntPredicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for continuous aggregation StorageManager metrics.
 *
 * @author Aleks Seovic  2026.09.15
 * @since 26.10
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ContinuousAggregationManagementTest
    {
    @Test
    public void shouldExposeStorageStateAndResettableWorkCounters()
        {
        Storage storage = mock(Storage.class);
        when(storage.getContinuousAggregationRegistrationCount()).thenReturn(1);
        when(storage.getContinuousAggregationStateCount(
                com.tangosol.internal.net.ContinuousAggregationState.Status.READY))
                .thenReturn(2);
        when(storage.getContinuousAggregationStateCount(
                com.tangosol.internal.net.ContinuousAggregationState.Status.BUILDING))
                .thenReturn(3);
        when(storage.getContinuousAggregationStaleStateCount()).thenReturn(4);
        when(storage.getStatsContinuousAggregationMutations())
                .thenReturn(new AtomicLong(11L));
        when(storage.getStatsContinuousAggregationFallbacks())
                .thenReturn(new AtomicLong(12L));
        when(storage.getStatsContinuousAggregationRebuilds())
                .thenReturn(new AtomicLong(13L));
        when(storage.getStatsContinuousAggregationDirty())
                .thenReturn(new AtomicLong(14L));

        StorageManagerModel model = new StorageManagerModel();
        model.set_Storage(storage);

        assertEquals(1, model.getContinuousAggregationRegistrationCount());
        assertEquals(2, model.getContinuousAggregationReadyPartitionCount());
        assertEquals(3, model.getContinuousAggregationBuildingPartitionCount());
        assertEquals(4, model.getContinuousAggregationStalePartitionCount());
        assertEquals(11L, model.getContinuousAggregationMutationCount());
        assertEquals(12L, model.getContinuousAggregationFallbackCount());
        assertEquals(13L, model.getContinuousAggregationRebuildCount());
        assertEquals(14L, model.getContinuousAggregationDirtyCount());

        model.resetStatistics();
        verify(storage).resetStats();
        }

    @Test
    public void shouldPublishAllContinuousAggregationAttributes()
        {
        Map attributes = new ExposedStorageManagerMBean().attributes();

        assertTrue(attributes.containsKey("ContinuousAggregationRegistrationCount"));
        assertTrue(attributes.containsKey("ContinuousAggregationReadyPartitionCount"));
        assertTrue(attributes.containsKey("ContinuousAggregationBuildingPartitionCount"));
        assertTrue(attributes.containsKey("ContinuousAggregationStalePartitionCount"));
        assertTrue(attributes.containsKey("ContinuousAggregationMutationCount"));
        assertTrue(attributes.containsKey("ContinuousAggregationFallbackCount"));
        assertTrue(attributes.containsKey("ContinuousAggregationRebuildCount"));
        assertTrue(attributes.containsKey("ContinuousAggregationDirtyCount"));
        }

    @Test
    public void shouldApplyExactCompatibilityPredicateToVersionAwareStreams()
        {
        assertVersionAwareStreamsCompatible(VersionHelper.encodeVersion(26, 0, 0, 0, 0), false);
        assertVersionAwareStreamsCompatible(VersionHelper.encodeVersion(26, 1, 0, 0, 0), true);
        assertVersionAwareStreamsCompatible(VersionHelper.encodeVersion(26, 3, 0), false);
        assertVersionAwareStreamsCompatible(VersionHelper.encodeVersion(26, 10, 0), true);
        }

    private static void assertVersionAwareStreamsCompatible(int nVersion, boolean fExpected)
        {
        MessageComponent message = mock(MessageComponent.class);
        when(message.isSenderCompatible(org.mockito.ArgumentMatchers.any(IntPredicate.class)))
                .thenAnswer(invocation -> invocation.<IntPredicate>getArgument(0).test(nVersion));
        when(message.isRecipientCompatible(org.mockito.ArgumentMatchers.any(IntPredicate.class)))
                .thenAnswer(invocation -> invocation.<IntPredicate>getArgument(0).test(nVersion));

        WrapperBufferInput.VersionAwareBufferInput in =
                new WrapperBufferInput.VersionAwareBufferInput(
                        new ByteArrayReadBuffer(new byte[0]).getBufferInput(), null, message);
        WrapperBufferOutput.VersionAwareBufferOutput out =
                new WrapperBufferOutput.VersionAwareBufferOutput(
                        new ByteArrayWriteBuffer(1).getBufferOutput(), message);

        IntPredicate predicate = ContinuousAggregationSupport::isVersionCompatible;
        if (fExpected)
            {
            assertTrue(ExternalizableHelper.isVersionCompatible(in, predicate));
            assertTrue(ExternalizableHelper.isVersionCompatible(out, predicate));
            }
        else
            {
            assertFalse(ExternalizableHelper.isVersionCompatible(in, predicate));
            assertFalse(ExternalizableHelper.isVersionCompatible(out, predicate));
            }
        }

    private static class ExposedStorageManagerMBean
            extends StorageManagerMBean
        {
        Map attributes()
            {
            return get_PropertyInfo();
            }
        }
    }
