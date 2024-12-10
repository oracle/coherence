/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.partition;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.ConfigurableCacheMap.UnitCalculator;

import com.tangosol.net.cache.LocalCache;
import org.hamcrest.Matchers;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static org.hamcrest.Matchers.is;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * ObservableSplittingBackingCacheTest tests some of the functionality in
 * {@link ObservableSplittingBackingCache}.
 *
 * @author hr  2013.08.27
 */
public class ObservableSplittingBackingCacheTest
    {

    /**
     * Test distribution of units across consumers (CCMs). We stick with 300
     * units and as partitions are created we should observe equal distribution
     * for both high and low units.
     */
    @Test
    public void testRepartition()
        {
        ConfigurableCacheMap[] accm = new ConfigurableCacheMap[] {mock(ConfigurableCacheMap.class), mock(ConfigurableCacheMap.class), mock(ConfigurableCacheMap.class)};
        BackingMapManager mgr = mockBMM();
        ObservableSplittingBackingCache cache = new ObservableSplittingBackingCache(mgr, "test");

        when(mgr.instantiateBackingMap(any(String.class))).thenReturn(accm[0], accm[1], accm[2]);

        cache.setHighUnits(300);
        cache.setLowUnits(100);

        cache.createPartition(0);
        verify(accm[0]).setHighUnits(300);
        verify(accm[0]).setLowUnits(100);

        // due to the double-checked locking the interactions must be accommodate
        // for each value being requested twice
        when(accm[0].getUnits()).thenReturn(0, 0);
        when(accm[0].getHighUnits()).thenReturn(300, 300);

        cache.createPartition(1);
        verify(accm[0]).setHighUnits(150);
        verify(accm[0]).setLowUnits(50);
        verify(accm[1]).setHighUnits(150);
        verify(accm[1]).setLowUnits(50);

        // set the interactions to the results we have just verified
        when(accm[0].getUnits()).thenReturn(0, 0);
        when(accm[0].getHighUnits()).thenReturn(150, 150);
        when(accm[1].getUnits()).thenReturn(0, 0);
        when(accm[1].getHighUnits()).thenReturn(150, 150);

        cache.createPartition(2);
        verify(accm[0]).setHighUnits(100);
        verify(accm[0]).setLowUnits(33);
        verify(accm[1]).setHighUnits(100);
        verify(accm[1]).setLowUnits(33);
        verify(accm[2]).setHighUnits(100);
        verify(accm[2]).setLowUnits(33);
        }

    /**
     * Test the distribution of units across consumers, however test the lazy
     * behaviour when a new consumer arrives and therefore an existing partition
     * becomes over subscribed. The new consumer should get what it can for free
     * and upon requiring anything beyond should result in demanding its entitled
     * units.
     */
    @Test
    public void testRepartitionWithConsumption()
        {
        ConfigurableCacheMap[] accm = new ConfigurableCacheMap[] {mock(ConfigurableCacheMap.class), mock(ConfigurableCacheMap.class), mock(ConfigurableCacheMap.class)};
        BackingMapManager mgr = mockBMM();
        ObservableSplittingBackingCache cache = new ObservableSplittingBackingCache(mgr, "test");

        when(mgr.instantiateBackingMap(any(String.class))).thenReturn(accm[0], accm[1], accm[2]);

        cache.setHighUnits(300);

        cache.createPartition(0);
        verify(accm[0]).setHighUnits(300);

        // due to the double-checked locking the interactions must be accommodate
        // for each value being requested twice
        when(accm[0].getUnits())    .thenReturn(0, 0);
        when(accm[0].getHighUnits()).thenReturn(300, 300);

        cache.createPartition(1);
        verify(accm[0]).setHighUnits(150);
        verify(accm[1]).setHighUnits(150);

        when(accm[0].getUnits())    .thenReturn(130, 130);
        when(accm[0].getHighUnits()).thenReturn(150, 150);
        when(accm[1].getUnits())    .thenReturn(0, 0);
        when(accm[1].getHighUnits()).thenReturn(150, 150);

        cache.createPartition(2);
        verify(accm[0]).setHighUnits(130);
        verify(accm[1]).setHighUnits(100);
        verify(accm[2]).setHighUnits(70);

        // the arrival of partition 2 leaves partition 0 in debt of 30 units;
        // verify that after sufficient store requests targeted to partition 2,
        // it demands its fair share of units from any overdrawn partitions

        String[]       asKey    = new String[] {"quark", "lepton"};
        String[]       asValue  = new String[] {"up"   , "muon"};
        UnitCalculator unitCalc = mock(UnitCalculator.class);

        when(accm[0].getUnits())    .thenReturn(130, 130);
        when(accm[0].getHighUnits()).thenReturn(130, 130);
        when(accm[1].getUnits())    .thenReturn(0, 0);
        when(accm[1].getHighUnits()).thenReturn(100, 100);
        when(accm[2].getUnits())    .thenReturn(0, 0);
        when(accm[2].getHighUnits()).thenReturn(70, 70);
        when(mgr.getContext().getKeyPartition(any(String.class))).thenReturn(2, 2);
        when(accm[2].getUnitCalculator()).thenReturn(unitCalc, unitCalc);
        when(unitCalc.calculateUnits(any(String.class), any(String.class))).thenReturn(40, 40);


        cache.put(asKey[0], asValue[0]);

        when(accm[2].getUnits())    .thenReturn(40, 40);
        when(accm[2].getHighUnits()).thenReturn(70, 70);

        cache.put(asKey[1], asValue[1]);

        verify(accm[0]).setHighUnits(100);
        verify(accm[1]).setHighUnits(100);
        verify(accm[2]).setHighUnits(100);
        }

    /**
     * Test changing the high units value such that the new global quota is
     * honoured by all consumers.
     */
    @Test
    public void testHighUnitChange()
        {
        ConfigurableCacheMap[] accm = new ConfigurableCacheMap[] {mock(ConfigurableCacheMap.class), mock(ConfigurableCacheMap.class), mock(ConfigurableCacheMap.class)};
        BackingMapManager mgr = mockBMM();
        ObservableSplittingBackingCache cache = new ObservableSplittingBackingCache(mgr, "test");

        when(mgr.instantiateBackingMap(any(String.class))).thenReturn(accm[0], accm[1], accm[2]);

        cache.setHighUnits(300);

        cache.createPartition(0);
        verify(accm[0]).setHighUnits(300);

        // due to the double-checked locking the interactions must be accommodate
        // for each value being requested twice
        when(accm[0].getUnits())    .thenReturn(0, 0);
        when(accm[0].getHighUnits()).thenReturn(300, 300);

        cache.createPartition(1);

        when(accm[0].getUnits())    .thenReturn(130, 130);
        when(accm[0].getHighUnits()).thenReturn(150, 150);
        when(accm[1].getUnits())    .thenReturn(0, 0);
        when(accm[1].getHighUnits()).thenReturn(150, 150);

        cache.createPartition(2);

        when(accm[0].getUnits())    .thenReturn(130, 130);
        when(accm[0].getHighUnits()).thenReturn(130, 130);
        when(accm[1].getUnits())    .thenReturn(0, 0);
        when(accm[1].getHighUnits()).thenReturn(100, 100);
        when(accm[2].getHighUnits()).thenReturn(70, 70);
        cache.setHighUnits(303);

        verify(accm[0], atMost(2)).setHighUnits(intThat(new ArgumentMatcher<Integer>()
            {
            @Override
            public boolean matches(Integer value)
                {
                return Matchers.anyOf(is(150), is(130)).matches(value);
                }
            }));
        verify(accm[1], atMost(3)).setHighUnits(intThat(new ArgumentMatcher<Integer>()
            {
            @Override
            public boolean matches(Integer value)
                {
                return Matchers.anyOf(is(150), is(100), is(101)).matches(value);
                }
            }));
        verify(accm[2]).setHighUnits(70);
        verify(accm[2]).setHighUnits(72);
        }

    // ----- helpers --------------------------------------------------------

    protected BackingMapManager mockBMM()
        {
        BackingMapManager bmm = mock(BackingMapManager.class);
        BackingMapManagerContext bmmc = mock(BackingMapManagerContext.class);
        PartitionedService service = mock(PartitionedService.class, withSettings().extraInterfaces(CacheService.class));

        when(bmm.getContext()).thenReturn(bmmc);
        when(bmm.instantiateBackingMap("test$synthetic")).thenReturn(new LocalCache());
        when(bmmc.getCacheService()).thenReturn((CacheService) service);
        when(service.getPartitionCount()).thenReturn(Integer.valueOf(257));

        return bmm;
        }
    }
