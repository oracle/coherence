/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.tests.invoke;

import com.tangosol.internal.util.invoke.AbstractRemotable;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.WrapperNamedCache;
import com.tangosol.util.InvocableMap;
import data.Trade;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author as  2015.08.22
 */
public abstract class AbstractRemotableTest
    {
    protected NamedCache<Integer, Trade> getCache()
        {
        return new WrapperNamedCache<>("test");
        }

    @Test
    public void testRemotableLambda()
        {
        Trade before = new Trade(1, 40.0, "ORCL", 1000);

        NamedCache<Integer, Trade> cache = getCache();
        cache.put(1, before);

        Trade after = cache.invoke(1, SplitProcessor(5));
        assertEquals(5000, after.getLot());
        assertEquals(8.0, after.getPrice(), 0.01);

        after = cache.get(1);
        assertEquals(5000, after.getLot());
        assertEquals(8.0, after.getPrice(), 0.01);
        }

    @Test
    public void testRemotableClass()
        {
        Trade before = new Trade(1, 40.0, "ORCL", 1000);

        NamedCache<Integer, Trade> cache = getCache();
        cache.put(1, before);

        Trade after = cache.invoke(1, new SplitProcessor(5));
        assertEquals(5000, after.getLot());
        assertEquals(8.0, after.getPrice(), 0.01);

        after = cache.get(1);
        assertEquals(5000, after.getLot());
        assertEquals(8.0, after.getPrice(), 0.01);
        }

    // ---- lambda: SplitProcessor ------------------------------------------

    public static InvocableMap.EntryProcessor<Integer, Trade, Trade> SplitProcessor(int nFactor)
        {
        return entry ->
            {
            Trade trade = entry.getValue();
            trade.setLot(trade.getLot() * nFactor);
            trade.setPrice(trade.getPrice() / nFactor);
            entry.setValue(trade, false);

            return trade;
            };
        }

    // ---- inner class: SplitProcessor -------------------------------------

    public static class SplitProcessor
            extends AbstractRemotable
            implements InvocableMap.EntryProcessor<Integer, Trade, Trade>
        {
        public SplitProcessor(int nFactor)
            {
            super(nFactor);

            m_nFactor = nFactor;
            }

        public Trade process(InvocableMap.Entry<Integer, Trade> entry)
            {
            Trade trade = entry.getValue();
            trade.setLot(trade.getLot() * m_nFactor);
            trade.setPrice(trade.getPrice() / m_nFactor);
            entry.setValue(trade, false);

            return trade;
            }

        // ---- data members ------------------------------------------------

        private int m_nFactor;
        }
    }
