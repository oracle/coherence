/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi;

import com.oracle.coherence.micrometer.CoherenceMicrometerMetrics;
import com.tangosol.coherence.osgi.metricsosgitest.CoherenceMetricsOsgiTestBundleActivator;

import com.tangosol.net.CacheFactory;

import org.junit.Test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * MetricsOsgiTest tests the deployment and execution of the
 * <tt>coherence-micrometer.jar</tt> within an OSGi container.
 *
 * @since 15.1.2.0
 */
public class MetricsOsgiTest
        extends AbstractOsgiTest
    {
    @Test
    public void testDeployBundle() throws BundleException
        {
        Container container = m_container;
        String    sPackage  = CoherenceMetricsOsgiTestBundleActivator.class.getPackage().getName().replace('.', '/');

        listBundles();

        // deploy dependencies
        libDeploy("micrometer-core.bnd");
        deployDependency(CacheFactory.class);
        deployDependency(CoherenceMicrometerMetrics.class);

        // deploy this test as a bundle
        container.packageAndDeploy("/" + sPackage + "/coherence-metrics-osgi-test.properties");

        listBundles();

        // ensure all known bundles are deployed and active
        assertThat(container.getBundle("Coherence"),              hasState(Bundle.ACTIVE));
        assertThat(container.getBundle("Coherence Micrometer"),   hasState(Bundle.ACTIVE));
        assertThat(container.getBundle("CoherenceMetricsOsgiTest"), hasState(Bundle.ACTIVE));
        }
    }
