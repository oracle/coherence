/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;

import com.tangosol.net.management.Registry;

import com.tangosol.net.metrics.MetricsRegistryAdapter;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Mockito.mock;

/**
 * @author jk  2019.06.24
 */
public class MetricSupportTest
    {
    @Test
    public void shouldNotHaveRegistries()
        {
        Supplier<Registry>           supplier = () -> null;
        List<MetricsRegistryAdapter> loader   = Collections.emptyList();
        MetricSupport                support  = new MetricSupport(supplier, loader);

        assertThat(support.hasRegistries(), is(false));
        }

    @Test
    public void shouldHaveRegistries()
        {
        Supplier<Registry>           supplier = () -> null;
        MetricsRegistryAdapter       adapter  = mock(MetricsRegistryAdapter.class);
        List<MetricsRegistryAdapter> loader   = Collections.singletonList(adapter);
        MetricSupport                support  = new MetricSupport(supplier, loader);

        assertThat(support.hasRegistries(), is(true));
        }

    }
