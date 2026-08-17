/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util;

import com.oracle.coherence.common.base.Associated;

import com.oracle.coherence.common.util.AssociationPile;
import com.oracle.coherence.common.util.ConcurrentAssociationPile;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for workload-aware daemon-pool telemetry.
 *
 * @author Aleks Seovic  2026.08.14
 * @since 26.07
 */
public class DaemonPoolTelemetryTest
    {
    @Test
    public void shouldAggregateExecutableAndAssociationDeferredBacklog()
        {
        ConcurrentAssociationPile<Value, Integer> queue = new MetricsPile<>();
        Value first    = new Value(1);
        Value deferred = new Value(1);
        Value available = new Value(null);

        queue.add(first);
        assertThat(queue.poll(), is(first));
        queue.add(deferred);
        queue.add(available);

        TestDaemonPool pool = new TestDaemonPool();
        pool.setTestQueues(new AssociationPile[] {queue});

        assertThat(pool.getBacklog(), is(2));
        assertThat(pool.getAvailableBacklog(), is(1));
        assertThat(pool.getAssociationDeferredBacklog(), is(1));
        assertThat(pool.getAssociationDeferredAddCount(), is(1L));
        assertThat(pool.getActiveAssociationCount(), is(1));
        }

    @Test
    public void shouldRecordAppliedResizeDirectionAndReason()
        {
        TestDaemonPool pool = new TestDaemonPool();

        pool.recordTestResize(5, 8, "executable backlog");
        pool.recordTestResize(8, 7, "idle workers");

        assertThat(pool.getResizeGrowCount(), is(1L));
        assertThat(pool.getResizeShrinkCount(), is(1L));
        assertThat(pool.getLastResizeReason(), is("idle workers"));

        pool.resetStats();

        assertThat(pool.getResizeGrowCount(), is(1L));
        assertThat(pool.getResizeShrinkCount(), is(1L));
        assertThat(pool.getLastResizeReason(), is("idle workers"));
        }

    private static class TestDaemonPool
            extends DaemonPool
        {
        @Override
        protected boolean isWorkloadMetricsEnabled()
            {
            return true;
            }

        private void setTestQueues(AssociationPile[] aQueue)
            {
            setQueues(aQueue);
            }

        private void recordTestResize(int cThreadsOld, int cThreadsNew, String sReason)
            {
            recordResize(cThreadsOld, cThreadsNew, sReason);
            }
        }

    private static class MetricsPile<T, A>
            extends ConcurrentAssociationPile<T, A>
        {
        @Override
        protected boolean isMetricsEnabled()
            {
            return true;
            }
        }

    private static class Value
            implements Associated<Integer>
        {
        private Value(Integer nAssociation)
            {
            m_nAssociation = nAssociation;
            }

        @Override
        public Integer getAssociatedKey()
            {
            return m_nAssociation;
            }

        private final Integer m_nAssociation;
        }
    }
