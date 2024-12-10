/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util;

import com.oracle.coherence.testing.SystemPropertyResource;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.component.net.Cluster;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import org.junit.Test;

import org.junit.After;
import org.junit.BeforeClass;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * The {@link ClusterConfigTest} class contains test to verify
 * configuration of Cluster.
 *
 * @author jf 2022.12.15
 */
public class ClusterConfigTest
    {
    @After
    public void cleanup()
        {
        CacheFactory.shutdown();
        }

    @Test
    public void validateDefaultShutdownTimeout()
        {
        Cluster    cluster = (Cluster) ((SafeCluster) CacheFactory.ensureCluster()).getInternalCluster();

        assertThat(cluster.getShutdownTimeout(), is(new Duration("2m").as(Duration.Magnitude.MILLI)));
        }

    @Test
    public void validateShutdownTimeoutProperty()
        {
        String sTimeout = "8m 40s";

        try (SystemPropertyResource resource = new SystemPropertyResource("coherence.shutdown.timeout", sTimeout))
            {
            Cluster cluster =  (Cluster) ((SafeCluster) CacheFactory.ensureCluster()).getInternalCluster();
            assertThat(cluster.getShutdownTimeout(), is(new Duration(sTimeout).as(Duration.Magnitude.MILLI)));
            }
        }
    }