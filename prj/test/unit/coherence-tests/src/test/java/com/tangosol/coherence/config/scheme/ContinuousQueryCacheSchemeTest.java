/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.net.CacheService;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link ContinuousQueryCacheScheme}.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public class ContinuousQueryCacheSchemeTest
    {
    // ----- test methods ---------------------------------------------------

    @Test
    public void testDefaults()
        {
        ContinuousQueryCacheScheme filterScheme = new ContinuousQueryCacheScheme();
        assertThat(filterScheme.getServiceType(), is(CacheService.TYPE_LOCAL));
        assertThat(filterScheme.isRunningClusterNeeded(), is(false));
        assertThat(filterScheme.getFilterBuilder(), is(nullValue()));
        assertThat(filterScheme.getTransformerBuilder(), is(nullValue()));
        assertThat(filterScheme.isReadOnly(), is(false));
        assertThat(filterScheme.getReconnectInterval(), is(0L));
        }
    }
