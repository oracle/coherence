/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

import com.oracle.coherence.common.base.Associated;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for association availability metrics.
 *
 * @author Aleks Seovic  2026.08.14
 * @since 26.07
 */
public class ConcurrentAssociationPileMetricsTest
    {
    @Test
    public void shouldDistinguishAvailableAndAssociationDeferredValues()
        {
        ConcurrentAssociationPile<Value, Integer> pile = new MetricsPile<>();
        Value first       = new Value(1);
        Value deferred    = new Value(1);
        Value unassociated = new Value(null);

        pile.add(first);

        assertThat(pile.size(), is(1));
        assertThat(pile.getAvailableCount(), is(1));
        assertThat(pile.getDeferredCount(), is(0));
        assertThat(pile.getDeferredAddCount(), is(0L));

        assertThat(pile.poll(), is(first));
        pile.add(deferred);
        pile.add(unassociated);

        assertThat(pile.size(), is(2));
        assertThat(pile.getAvailableCount(), is(1));
        assertThat(pile.getDeferredCount(), is(1));
        assertThat(pile.getDeferredAddCount(), is(1L));
        assertThat(pile.getAssociationCount(), is(1));

        assertThat(pile.poll(), is(unassociated));
        pile.release(unassociated);
        pile.release(first);

        assertThat(pile.size(), is(1));
        assertThat(pile.getAvailableCount(), is(1));
        assertThat(pile.getDeferredCount(), is(0));
        assertThat(pile.poll(), is(deferred));

        pile.release(deferred);

        assertThat(pile.size(), is(0));
        assertThat(pile.getAvailableCount(), is(0));
        assertThat(pile.getDeferredCount(), is(0));
        assertThat(pile.getAssociationCount(), is(0));
        }

    @Test
    public void shouldExposeAssociationCountWithoutHotPathMetrics()
        {
        ConcurrentAssociationPile<Value, Integer> pile = new PlainPile<>();
        Value first    = new Value(1);
        Value deferred = new Value(1);

        pile.add(first);
        assertThat(pile.poll(), is(first));
        pile.add(deferred);

        assertThat(pile.getAvailableCount(), is(-1));
        assertThat(pile.getDeferredCount(), is(-1));
        assertThat(pile.getDeferredAddCount(), is(-1L));
        assertThat(pile.getAssociationCount(), is(1));

        pile.release(first);
        assertThat(pile.poll(), is(deferred));
        pile.release(deferred);
        assertThat(pile.getAssociationCount(), is(0));
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

    private static class PlainPile<T, A>
            extends ConcurrentAssociationPile<T, A>
        {
        @Override
        protected boolean isMetricsEnabled()
            {
            return false;
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
