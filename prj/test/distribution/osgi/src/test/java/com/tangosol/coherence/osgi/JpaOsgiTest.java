/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi;

import com.oracle.coherence.jpa.JpaCacheLoader;

import com.tangosol.coherence.osgi.jpaosgitest.CoherenceJpaOsgiTestBundleActivator;

import com.tangosol.net.CacheFactory;

import jakarta.persistence.Persistence;

import org.junit.Ignore;
import org.junit.Test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * JpaOsgiTest tests the deployment and execution of the
 * <tt>coherence-jpa.jar</tt> within an OSGi container.
 *
 * @author hr  2012.01.29
 * @since Coherence 12.1.2
 */
public class JpaOsgiTest
        extends AbstractOsgiTest
    {
    @Test
    public void testDeployBundle() throws BundleException
        {
        Container container = m_container;
        String    sPackage  = CoherenceJpaOsgiTestBundleActivator.class.getPackage().getName().replace('.','/');

        listBundles();

        // deploy dependencies
        deployDependency(CacheFactory.class);
        deployDependency(Persistence.class);
        deployDependency(JpaCacheLoader.class);

        // deploy this test as a bundle
        container.packageAndDeploy("/" + sPackage + "/coherence-jpa-osgi-test.properties");

        listBundles();

        // ensure all known bundles are deployed and active
        assertThat(container.getBundle("Coherence"),                   hasState(Bundle.ACTIVE));
        assertThat(container.getBundle("Jakarta Persistence API jar"), hasState(Bundle.ACTIVE));
        assertThat(container.getBundle("Coherence JPA Integration"),   hasState(Bundle.ACTIVE));
        assertThat(container.getBundle("CoherenceJpaOsgiTest"),        hasState(Bundle.ACTIVE));
        }
    }
